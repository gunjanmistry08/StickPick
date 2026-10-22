# Keep data models serialized by Gson
-keep class com.gunjan.teletowa.data.model.** { *; }
-keep class com.gunjan.teletowa.whatsapp.** { *; }
-keep class com.gunjan.teletowa.conversion.** { *; }

# Keep native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}
