# Keep data models serialized by Gson
-keep class com.gunjan.stickpick.data.model.** { *; }
-keep class com.gunjan.stickpick.whatsapp.** { *; }
-keep class com.gunjan.stickpick.conversion.** { *; }

# Keep native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}
