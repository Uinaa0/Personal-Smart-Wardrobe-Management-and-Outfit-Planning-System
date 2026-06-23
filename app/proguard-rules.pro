# =============================================================================
# ProGuard Rules — Code Shrinking & Obfuscation Configuration
# =============================================================================
# These rules tell R8/ProGuard which classes to keep when minification is
# enabled. Room entities and Gson-serialized classes must be preserved
# because they rely on reflection.
# =============================================================================

# Keep Room entity classes (they use reflection for column mapping)
-keep class com.aiman.smartwardrobe.data.entity.** { *; }

# Keep Gson-serialized model classes
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit2 rules
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**

# Glide rules
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
