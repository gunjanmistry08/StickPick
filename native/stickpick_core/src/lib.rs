use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;

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
