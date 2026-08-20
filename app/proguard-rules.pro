# ============================================================
# 基础属性保留
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod
-keepattributes Exceptions

# ============================================================
# AndroidX 全家桶（WorkManager / Room / Lifecycle / Navigation 等）
# ============================================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ============================================================
# Google Material / Play Services
# ============================================================
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================================
# OkHttp / Okio
# ============================================================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okhttp3.internal.annotations.**
-dontwarn okhttp3.logging.**
-keep class okio.** { *; }
-keep interface okio.** { *; }
-dontwarn okio.**

# ============================================================
# BouncyCastle / Conscrypt / OpenJSSE（SSL 相关）
# ============================================================
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ============================================================
# ReLinker / Sigmob WindAd
# ============================================================
-dontwarn com.getkeepsafe.relinker.**
-dontwarn com.sigmob.windad.**

# ============================================================
# UniApp / DCloud SDK
# ============================================================
-keep class io.dcloud.** { *; }
-keep interface io.dcloud.** { *; }
-dontwarn io.dcloud.**

# ============================================================
# Gson
# ============================================================
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================
# WebView / JS Bridge
# ============================================================
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
    public void *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ============================================================
# 你自己的业务代码（兜底保护）
# ============================================================
-keep class com.shinian.pay.** { *; }
-keep interface com.shinian.pay.** { *; }

# ============================================================
#