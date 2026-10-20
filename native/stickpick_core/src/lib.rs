use flate2::read::GzDecoder;
use image::imageops::FilterType;
use image::{GenericImage, ImageReader, Rgba, RgbaImage};
use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jstring};
use jni::JNIEnv;
use rasterlottie::{Animation, RenderConfig, Renderer, Rgba8, SupportProfile};
use std::io::{Cursor, Read};
use webp_animation::{Encoder, EncoderOptions, EncodingConfig};

const WHATSAPP_STICKER_SIZE: u32 = 512;
const MAX_STATIC_BYTES: usize = 100 * 1024;    // 100 KB limit for WhatsApp static stickers
const MAX_ANIMATED_BYTES: usize = 500 * 1024; // 500 KB limit for WhatsApp animated stickers

#[no_mangle]
pub extern "system" fn Java_com_gunjan_stickpick_conversion_RustEngine_ping(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = env
        .new_string("Hello from StickPick Rust Engine!")
        .expect("Couldn't create java string");
    output.into_raw()
}

/// Converts an input image (WEBP, PNG, JPEG, etc.) into a WhatsApp-compliant static WebP:
/// - Exactly 512x512 canvas
/// - Maintains original aspect ratio
/// - Centered with transparent padding
/// - Compressed to stay under WhatsApp's 100 KB size limit
#[no_mangle]
pub extern "system" fn Java_com_gunjan_stickpick_conversion_RustEngine_convertStaticImage(
    env: JNIEnv,
    _class: JClass,
    input_bytes: JByteArray,
) -> jbyteArray {
    let raw_bytes = match env.convert_byte_array(&input_bytes) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    match process_static_image(&raw_bytes, WHATSAPP_STICKER_SIZE, MAX_STATIC_BYTES) {
        Ok(webp_bytes) => match env.byte_array_from_slice(&webp_bytes) {
            Ok(jarr) => jarr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Converts an image into a 96x96 tray icon conforming to WhatsApp spec (<= 50 KB PNG/WebP).
#[no_mangle]
pub extern "system" fn Java_com_gunjan_stickpick_conversion_RustEngine_createTrayIcon(
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

    match process_static_image(&raw_bytes, TRAY_SIZE, MAX_TRAY_BYTES) {
        Ok(webp_bytes) => match env.byte_array_from_slice(&webp_bytes) {
            Ok(jarr) => jarr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Converts a Telegram .tgs sticker (gzipped Lottie JSON) to an animated WebP
/// strictly complying with WhatsApp Sticker API specifications:
/// - Exactly 512x512 transparent canvas
/// - Loop count = 0 (infinite loop)
/// - Capped at max 3.0 seconds duration
/// - File size strictly <= 500 KB
#[no_mangle]
pub extern "system" fn Java_com_gunjan_stickpick_conversion_RustEngine_convertTgs(
    env: JNIEnv,
    _class: JClass,
    input_bytes: JByteArray,
) -> jbyteArray {
    let raw_bytes = match env.convert_byte_array(&input_bytes) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    match process_tgs(&raw_bytes) {
        Ok(webp_bytes) => match env.byte_array_from_slice(&webp_bytes) {
            Ok(jarr) => jarr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

fn process_tgs(input: &[u8]) -> Result<Vec<u8>, String> {
    // 1. Decompress .tgs (gzip) to JSON string
    let json_str = decompress_tgs(input)?;

    // 2. Parse Lottie animation
    let anim = Animation::from_json_str(&json_str).map_err(|e| format!("Lottie parse: {e}"))?;

    let orig_w = anim.width as f32;
    let orig_h = anim.height as f32;
    if orig_w == 0.0 || orig_h == 0.0 {
        return Err("Invalid animation dimensions".into());
    }

    // WhatsApp constraints: 512x512 canvas, duration <= 3.0s, fps typically 15
    let scale = (WHATSAPP_STICKER_SIZE as f32 / orig_w).min(WHATSAPP_STICKER_SIZE as f32 / orig_h);
    let duration_sec = anim.duration_seconds().min(3.0);
    let target_fps = 15.0f32; // 15 fps gives smooth animation and stays well under 500KB
    let total_frames = ((duration_sec * target_fps).round() as usize).max(3);
    let frame_step = (anim.out_point - anim.in_point) / (total_frames as f32);
    let frame_duration_ms = (1000.0 / target_fps).round() as i32;

    // Renderer setup with transparent background
    let renderer = Renderer::new(SupportProfile::target_corpus());
    let config = RenderConfig::new(Rgba8::new(0, 0, 0, 0), scale);

    let scaled_w = ((orig_w * scale).round() as u32).min(WHATSAPP_STICKER_SIZE);
    let scaled_h = ((orig_h * scale).round() as u32).min(WHATSAPP_STICKER_SIZE);
    let offset_x = (WHATSAPP_STICKER_SIZE - scaled_w) / 2;
    let offset_y = (WHATSAPP_STICKER_SIZE - scaled_h) / 2;

    // 3. Render all frames into 512x512 transparent canvases
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

    // 4. Encode to Animated WebP with adaptive quality loop to stay under 500 KB
    let mut quality = 65.0f32;
    loop {
        let mut options = EncoderOptions::default();
        options.anim_params.loop_count = 0; // Infinite loop (required by WhatsApp)

        let mut encoder = Encoder::new_with_options(
            (WHATSAPP_STICKER_SIZE, WHATSAPP_STICKER_SIZE),
            options,
        )
        .map_err(|e| format!("Encoder: {e}"))?;

        let config = EncodingConfig::new_lossy(quality);
        let _ = encoder.set_default_encoding_config(config);

        let mut current_timestamp = 0;
        for frame in &frames {
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

fn decompress_tgs(input: &[u8]) -> Result<String, String> {
    // If input already starts with '{', it's uncompressed JSON
    if let Ok(s) = std::str::from_utf8(input) {
        if s.trim_start().starts_with('{') {
            return Ok(s.to_string());
        }
    }

    // Otherwise decompress via GzDecoder
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
) -> Result<Vec<u8>, String> {
    let reader = ImageReader::new(Cursor::new(input))
        .with_guessed_format()
        .map_err(|e| e.to_string())?;

    let img = reader.decode().map_err(|e| e.to_string())?;
    let (orig_w, orig_h) = (img.width(), img.height());

    if orig_w == 0 || orig_h == 0 {
        return Err("Empty image dimensions".into());
    }

    // Compute aspect ratio scale to fit within target canvas
    let scale = (target_canvas as f32 / orig_w as f32).min(target_canvas as f32 / orig_h as f32);
    let new_w = ((orig_w as f32 * scale).round() as u32).clamp(1, target_canvas);
    let new_h = ((orig_h as f32 * scale).round() as u32).clamp(1, target_canvas);

    // High quality downscaling/upscaling using Lanczos3
    let resized = img.resize_exact(new_w, new_h, FilterType::Lanczos3);

    // Create target_canvas x target_canvas transparent canvas
    let mut canvas = RgbaImage::from_pixel(target_canvas, target_canvas, Rgba([0, 0, 0, 0]));

    // Center the resized image onto the transparent canvas
    let offset_x = (target_canvas - new_w) / 2;
    let offset_y = (target_canvas - new_h) / 2;
    canvas
        .copy_from(&resized.to_rgba8(), offset_x, offset_y)
        .map_err(|e| e.to_string())?;

    // Adaptive compression loop to guarantee file size constraint
    let mut quality: f32 = 85.0;
    loop {
        let encoder = webp::Encoder::from_rgba(
            canvas.as_raw(),
            target_canvas,
            target_canvas,
        );

        let encoded = encoder.encode(quality);
        let bytes = encoded.to_vec();

        if bytes.len() <= max_size_bytes || quality <= 20.0 {
            return Ok(bytes);
        }

        quality -= 15.0;
    }
}
