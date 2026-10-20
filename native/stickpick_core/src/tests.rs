use crate::*;

#[test]
fn test_static_image_conversion() {
    let icon_bytes = std::fs::read("../../Assets/icon.png")
        .or_else(|_| std::fs::read("../../Assets/logo.png"))
        .expect("Should read sample asset");

    let converted = process_static_image(&icon_bytes, WHATSAPP_STICKER_SIZE, MAX_STATIC_BYTES)
        .expect("Should convert image");

    // Verify valid WebP header
    assert!(converted.len() >= 12);
    assert_eq!(&converted[0..4], b"RIFF");
    assert_eq!(&converted[8..12], b"WEBP");

    // Verify size constraint <= 100 KB
    assert!(converted.len() <= MAX_STATIC_BYTES);

    // Verify tray icon conversion <= 50 KB
    let tray = process_static_image(&icon_bytes, 96, 50 * 1024)
        .expect("Should convert tray");
    assert!(tray.len() <= 50 * 1024);
    assert_eq!(&tray[0..4], b"RIFF");
    assert_eq!(&tray[8..12], b"WEBP");
}

#[test]
fn test_minimal_tgs_conversion() {
    let lottie_json = r#"{
        "v": "5.5.2",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "nm": "test",
        "ddd": 0,
        "assets": [],
        "layers": []
    }"#;

    use flate2::write::GzEncoder;
    use flate2::Compression;
    use std::io::Write;

    let mut encoder = GzEncoder::new(Vec::new(), Compression::default());
    encoder.write_all(lottie_json.as_bytes()).unwrap();
    let tgs_bytes = encoder.finish().unwrap();

    let webp_bytes = process_tgs(&tgs_bytes).expect("Should convert TGS to animated WebP");

    assert!(webp_bytes.len() >= 12);
    assert_eq!(&webp_bytes[0..4], b"RIFF");
    assert_eq!(&webp_bytes[8..12], b"WEBP");
    assert!(webp_bytes.len() <= MAX_ANIMATED_BYTES);
}
