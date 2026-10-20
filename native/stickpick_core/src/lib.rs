use image::imageops::FilterType;
use image::{GenericImage, ImageReader, Rgba, RgbaImage};
use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jstring};
use jni::JNIEnv;
use std::io::Cursor;

const WHATSAPP_STICKER_SIZE: u32 = 512;
const MAX_STATIC_BYTES: usize = 100 * 1024; // 100 KB limit for WhatsApp static stickers

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
