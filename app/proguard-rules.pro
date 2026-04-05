# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**
-keep class kotlinx.** { *; }
-dontwarn kotlinx.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class dagger.Lazy
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-dontwarn dagger.hilt.**

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep class okhttp3.**$** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Serialization
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class ** { @kotlinx.serialization.SerialName *; }

# Application code
-keep class com.aria2.downloader.** { *; }
-keepclasseswithmembernames class com.aria2.downloader.** {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep data classes
-keep class com.aria2.downloader.domain.model.** { *; }
-keep class com.aria2.downloader.data.local.** { *; }

# Remove logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
