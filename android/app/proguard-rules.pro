# ML Kit ships consumer rules; keep the public translate entry points used via reflection.
-keep class com.google.mlkit.nl.translate.** { *; }
-dontwarn com.google.mlkit.**

# WebView JavaScript bridges, if any are added later.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
