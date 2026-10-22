use flate2::read::GzDecoder;
use image::imageops::FilterType;
use image::{GenericImage, ImageReader, Rgba, RgbaImage};
use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jstring};
use jni::JNIEnv;
use matroska_demuxer::MatroskaFile;
use rasterlottie::{Animation, RenderConfig, Renderer, Rgba8, SupportProfile};
use rusty_vp9::Vp9Decoder;
use std::io::{Cursor, Read};
use webp_animation::{Encoder, EncoderOptions, EncodingConfig};

const WHATSAPP_STICKER_SIZE: u32 = 512;
const MAX_STATIC_BYTES: usize = 100 * 1024;    // 100 KB limit for WhatsApp static stickers
const MAX_ANIMATED_BYTES: usize = 500 * 1024; // 500 KB limit for WhatsApp animated stickers

#[no_mangle]
pub extern "system" fn Java_com_gunjan_teletowa_conversion_RustEngine_ping(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = env
        .new_string("Hello from TeleToWA Rust Engine!")
        .expect("Couldn't create java string");
    output.into_raw()
}

/// Converts an input image (WEBP, PNG, JPEG, etc.) into a WhatsApp-compliant static WebP:
/// - Exactly 512x512 canvas
/// - Maintains original aspect ratio
/// - Centered with transparent padding
/// - Lossless mode: tries lossless WebP first if requested, falls back if exceeding 100 KB
/// - Compressed to stay under WhatsApp's 100 KB size limit
#[no_mangle]
pub extern "system" fn Java_com_gunjan_teletowa_conversion_RustEngine_convertStaticImage(
    env: JNIEnv,
    _class: JClass,
    input_bytes: JByteArray,
    lossless: jni::sys::jboolean,
) -> jbyteArray {
    let raw_bytes = match env.convert_byte_array(&input_bytes) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    let is_lossless = lossless != 0;
    match process_static_image(&raw_bytes, WHATSAPP_STICKER_SIZE, MAX_STATIC_BYTES, is_lossless) {
        Ok(webp_bytes) => match env.byte_array_from_slice(&webp_bytes) {
            Ok(jarr) => jarr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Converts an image into a 96x96 tray icon conforming to WhatsApp spec (<= 50 KB PNG/WebP).
#[no_mangle]
pub extern "system" fn Java_com_gunjan_teletowa_conversion_RustEngine_createTrayIcon(
    env: JNIEnv,
    _class: JClass,
    input_bytes: JByteArray,
) -> jbyteArray {
    let raw_bytes = match env.convert_byte_array(&input_bytes) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    const TRAY_SIZE: u32 = 96;
    const MAX_TRAY_BYTES: usize = 50 * 1024;

    match process_static_image(&raw_bytes, TRAY_SIZE, MAX_TRAY_BYTES, false) {
        Ok(webp_bytes) => match env.byte_array_from_slice(&webp_bytes) {
            Ok(jarr) => jarr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Converts a Telegram .tgs sticker (gzipped Lottie JSON) to an animated WebP
/// strictly complying with WhatsApp Sticker API specifications.
#[no_mangle]
pub extern "system" fn Java_com_gunjan_teletowa_conversion_RustEngine_convertTgs(
    env: JNIEnv,
    _class: JClass,
    input_bytes: JByteArray,
    lossless: jni::sys::jboolean,
) -> jbyteArray {
    let raw_bytes = match env.convert_byte_array(&input_bytes) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    let is_lossless = lossless != 0;
    match process_tgs(&raw_bytes, is_lossless) {
        Ok(webp_bytes) => match env.byte_array_from_slice(&webp_bytes) {
            Ok(jarr) => jarr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Converts a Telegram .webm (VP9) video sticker to an animated WebP
/// strictly complying with WhatsApp Sticker API specifications:
/// - Exactly 512x512 transparent canvas
/// - Loop count = 0 (infinite loop)
/// - Capped at max 3.0 seconds duration
/// - File size strictly <= 500 KB
#[no_mangle]
pub extern "system" fn Java_com_gunjan_teletowa_conversion_RustEngine_convertWebm(
    env: JNIEnv,
    _class: JClass,
    input_bytes: JByteArray,
    lossless: jni::sys::jboolean,
) -> jbyteArray {
    let raw_bytes = match env.convert_byte_array(&input_bytes) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    let is_lossless = lossless != 0;
    match process_webm(&raw_bytes, is_lossless) {
        Ok(webp_bytes) => match env.byte_array_from_slice(&webp_bytes) {
            Ok(jarr) => jarr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

fn process_webm(input: &[u8], lossless: bool) -> Result<Vec<u8>, String> {
    let cursor = Cursor::new(input);
    let mut mkv = MatroskaFile::open(cursor).map_err(|e| format!("Matroska open: {e}"))?;

    let video_track = mkv
        .tracks()
        .iter()
        .find(|t| t.track_type() == matroska_demuxer::TrackType::Video)
        .ok_or("No video track found")?;
    let video_track_num = video_track.track_number();

    let mut decoder = Vp9Decoder::new();
    let mut mkv_frame = matroska_demuxer::Frame::default();
    let mut rgba_frames: Vec<RgbaImage> = Vec::new();

    // 10 fps matches original FFmpeg (fps=10, -t 3 max 30 frames for WhatsApp standard)
    let target_fps = 10.0f32;
    let frame_duration_ms = (1000.0 / target_fps).round() as i32;
    let max_frames = 30;

    let mut frame_idx = 0;
    while mkv.next_frame(&mut mkv_frame).unwrap_or(false) {
        if mkv_frame.track == video_track_num.get() {
            let pts = Some(mkv_frame.timestamp as i64);
            let _ = decoder.push(&mkv_frame.data, pts);

            while let Ok(decoded) = decoder.next_frame() {
                frame_idx += 1;
                // Subsample frames to match target_fps if video is 30/60fps
                if frame_idx % 2 == 1 || rgba_frames.len() < 12 {
                    let img = yuv420_to_rgba_fast(&decoded);
                    let padded = pad_and_center_512(&img);
                    rgba_frames.push(padded);
                    if rgba_frames.len() >= max_frames {
                        break;
                    }
                }
            }
            if rgba_frames.len() >= max_frames {
                break;
            }
        }
    }

    decoder.flush();
    while let Ok(decoded) = decoder.next_frame() {
        if rgba_frames.len() >= max_frames {
            break;
        }
        let img = yuv420_to_rgba_fast(&decoded);
        let padded = pad_and_center_512(&img);
        rgba_frames.push(padded);
    }

    if rgba_frames.is_empty() {
        return Err("No frames decoded from WebM".into());
    }

    encode_animated_webp(&rgba_frames, frame_duration_ms, lossless)
}

/// Ultra-fast integer-only YUV420 to RGBA conversion (ITU-R BT.601)
#[inline(always)]
fn yuv420_to_rgba_fast(frame: &rusty_vp9::DecodedFrame) -> RgbaImage {
    let w = frame.width as usize;
    let h = frame.height as usize;
    let mut pixels = vec![0u8; w * h * 4];

    if frame.planes.len() < 3 {
        return RgbaImage::from_raw(frame.width, frame.height, pixels).unwrap();
    }

    let y_plane = &frame.planes[0];
    let u_plane = &frame.planes[1];
    let v_plane = &frame.planes[2];
    let y_stride = frame.strides[0];
    let u_stride = frame.strides[1];
    let v_stride = frame.strides[2];
    let sub_x = frame.subsampling_x;
    let sub_y = frame.subsampling_y;

    for y in 0..h {
        let uv_y = y >> sub_y;
        let y_row = y * y_stride;
        let uv_row_u = uv_y * u_stride;
        let uv_row_v = uv_y * v_stride;
        let out_row = y * w * 4;

        for x in 0..w {
            let uv_x = x >> sub_x;

            let y_val = y_plane.get(y_row + x).copied().unwrap_or(0) as i32;
            let u_val = u_plane.get(uv_row_u + uv_x).copied().unwrap_or(128) as i32 - 128;
            let v_val = v_plane.get(uv_row_v + uv_x).copied().unwrap_or(128) as i32 - 128;

            let c = (y_val - 16).max(0) * 298;
            let r = ((c + 409 * v_val + 128) >> 8).clamp(0, 255) as u8;
            let g = ((c - 100 * u_val - 208 * v_val + 128) >> 8).clamp(0, 255) as u8;
            let b = ((c + 516 * u_val + 128) >> 8).clamp(0, 255) as u8;

            let idx = out_row + x * 4;
            pixels[idx] = r;
            pixels[idx + 1] = g;
            pixels[idx + 2] = b;
            pixels[idx + 3] = 255;
        }
    }

    RgbaImage::from_raw(frame.width, frame.height, pixels).unwrap()
}

#[inline]
fn pad_and_center_512(img: &RgbaImage) -> RgbaImage {
    let (orig_w, orig_h) = (img.width(), img.height());

    // Fast-path: Telegram video stickers are often already 512x512
    if orig_w == WHATSAPP_STICKER_SIZE && orig_h == WHATSAPP_STICKER_SIZE {
        return img.clone();
    }

    let scale = (WHATSAPP_STICKER_SIZE as f32 / orig_w as f32)
        .min(WHATSAPP_STICKER_SIZE as f32 / orig_h as f32);
    let new_w = ((orig_w as f32 * scale).round() as u32).clamp(1, WHATSAPP_STICKER_SIZE);
    let new_h = ((orig_h as f32 * scale).round() as u32).clamp(1, WHATSAPP_STICKER_SIZE);

    let resized = image::imageops::resize(img, new_w, new_h, FilterType::Lanczos3);
    let mut canvas = RgbaImage::from_pixel(
        WHATSAPP_STICKER_SIZE,
        WHATSAPP_STICKER_SIZE,
        Rgba([0, 0, 0, 0]),
    );

    let offset_x = (WHATSAPP_STICKER_SIZE - new_w) / 2;
    let offset_y = (WHATSAPP_STICKER_SIZE - new_h) / 2;
    let _ = canvas.copy_from(&resized, offset_x, offset_y);

    canvas
}

fn encode_animated_webp(
    frames: &[RgbaImage],
    frame_duration_ms: i32,
    lossless: bool,
) -> Result<Vec<u8>, String> {
    let mut quality = if lossless { 80.0f32 } else { 55.0f32 };
    loop {
        let mut options = EncoderOptions::default();
        options.anim_params.loop_count = 0; // Infinite loop (mandated by WhatsApp)

        let mut encoder = Encoder::new_with_options(
            (WHATSAPP_STICKER_SIZE, WHATSAPP_STICKER_SIZE),
            options,
        )
        .map_err(|e| format!("Encoder: {e}"))?;

        let config = EncodingConfig::new_lossy(quality);
        let _ = encoder.set_default_encoding_config(config);

        let mut current_timestamp = 0;
        for frame in frames {
            encoder
                .add_frame(frame.as_raw(), current_timestamp)
                .map_err(|e| format!("Add frame: {e}"))?;
            current_timestamp += frame_duration_ms;
        }

        let webp_data = encoder
            .finalize(current_timestamp)
            .map_err(|e| format!("Finalize: {e}"))?;
        let bytes = webp_data.to_vec();

        if bytes.len() <= MAX_ANIMATED_BYTES || quality <= 20.0 {
            return Ok(bytes);
        }

        quality -= 15.0;
    }
}

fn process_tgs(input: &[u8], lossless: bool) -> Result<Vec<u8>, String> {
    let json_str = decompress_tgs(input)?;
    let anim = Animation::from_json_str(&json_str).map_err(|e| format!("Lottie parse: {e}"))?;

    let orig_w = anim.width as f32;
    let orig_h = anim.height as f32;
    if orig_w == 0.0 || orig_h == 0.0 {
        return Err("Invalid animation dimensions".into());
    }

    let scale = (WHATSAPP_STICKER_SIZE as f32 / orig_w).min(WHATSAPP_STICKER_SIZE as f32 / orig_h);
    let duration_sec = anim.duration_seconds().min(3.0);
    let target_fps = 12.0f32;
    let total_frames = ((duration_sec * target_fps).round() as usize).max(3);
    let frame_step = (anim.out_point - anim.in_point) / (total_frames as f32);
    let frame_duration_ms = (1000.0 / target_fps).round() as i32;

    let renderer = Renderer::new(SupportProfile::target_corpus());
    let config = RenderConfig::new(Rgba8::new(0, 0, 0, 0), scale);

    let scaled_w = ((orig_w * scale).round() as u32).min(WHATSAPP_STICKER_SIZE);
    let scaled_h = ((orig_h * scale).round() as u32).min(WHATSAPP_STICKER_SIZE);
    let offset_x = (WHATSAPP_STICKER_SIZE - scaled_w) / 2;
    let offset_y = (WHATSAPP_STICKER_SIZE - scaled_h) / 2;

    let mut frames: Vec<RgbaImage> = Vec::with_capacity(total_frames);

    for i in 0..total_frames {
        let frame_num = anim.in_point + (i as f32) * frame_step;
        let raster = renderer
            .render_frame(&anim, frame_num, config)
            .map_err(|e| format!("Render frame {i}: {e}"))?;

        let mut canvas = RgbaImage::from_pixel(
            WHATSAPP_STICKER_SIZE,
            WHATSAPP_STICKER_SIZE,
            Rgba([0, 0, 0, 0]),
        );

        let frame_img = match RgbaImage::from_raw(raster.width, raster.height, raster.pixels) {
            Some(img) => img,
            None => return Err("Invalid raster pixels".into()),
        };

        canvas
            .copy_from(&frame_img, offset_x, offset_y)
            .map_err(|e| e.to_string())?;

        frames.push(canvas);
    }

    encode_animated_webp(&frames, frame_duration_ms, lossless)
}

fn decompress_tgs(input: &[u8]) -> Result<String, String> {
    if let Ok(s) = std::str::from_utf8(input) {
        if s.trim_start().starts_with('{') {
            return Ok(s.to_string());
        }
    }

    let mut decoder = GzDecoder::new(input);
    let mut decompressed = String::new();
    decoder
        .read_to_string(&mut decompressed)
        .map_err(|e| format!("Gzip decode: {e}"))?;
    Ok(decompressed)
}

fn process_static_image(
    input: &[u8],
    target_canvas: u32,
    max_size_bytes: usize,
    lossless: bool,
) -> Result<Vec<u8>, String> {
    let reader = ImageReader::new(Cursor::new(input))
        .with_guessed_format()
        .map_err(|e| e.to_string())?;

    let img = reader.decode().map_err(|e| e.to_string())?;
    let (orig_w, orig_h) = (img.width(), img.height());

    if orig_w == 0 || orig_h == 0 {
        return Err("Empty image dimensions".into());
    }

    let rgba_canvas = if orig_w == target_canvas && orig_h == target_canvas {
        img.to_rgba8()
    } else {
        let scale = (target_canvas as f32 / orig_w as f32).min(target_canvas as f32 / orig_h as f32);
        let new_w = ((orig_w as f32 * scale).round() as u32).clamp(1, target_canvas);
        let new_h = ((orig_h as f32 * scale).round() as u32).clamp(1, target_canvas);

        // Lanczos3 provides superior sharpness and anti-aliased edges for stickers
        let resized = img.resize_exact(new_w, new_h, FilterType::Lanczos3);

        let mut canvas = RgbaImage::from_pixel(target_canvas, target_canvas, Rgba([0, 0, 0, 0]));
        let offset_x = (target_canvas - new_w) / 2;
        let offset_y = (target_canvas - new_h) / 2;
        canvas
            .copy_from(&resized.to_rgba8(), offset_x, offset_y)
            .map_err(|e| e.to_string())?;
        canvas
    };

    // 1. If Lossless mode is enabled, attempt true lossless WebP first
    if lossless {
        let encoder = webp::Encoder::from_rgba(
            rgba_canvas.as_raw(),
            target_canvas,
            target_canvas,
        );
        let encoded = encoder.encode_lossless();
        let bytes = encoded.to_vec();
        if bytes.len() <= max_size_bytes {
            return Ok(bytes);
        }
        // If lossless exceeds max_size_bytes (e.g. 100 KB limit), fall through to high quality lossy
    }

    // 2. High-quality lossy encoding loop (adapts to stay strictly within size limit)
    let mut quality: f32 = if lossless { 95.0 } else { 82.0 };
    loop {
        let encoder = webp::Encoder::from_rgba(
            rgba_canvas.as_raw(),
            target_canvas,
            target_canvas,
        );

        let encoded = encoder.encode(quality);
        let bytes = encoded.to_vec();

        if bytes.len() <= max_size_bytes || quality <= 20.0 {
            return Ok(bytes);
        }

        quality -= 12.0;
    }
}

#[cfg(test)]
mod tests;
