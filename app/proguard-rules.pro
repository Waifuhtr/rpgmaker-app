# WebView'e @JavascriptInterface ile açılan köprü metotları release'de isim değiştirmemeli,
# aksi halde JS tarafındaki çağrılar sessizce kaybolur.
-keepclassmembers class com.waifuhtr.pixelstore.StoreBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# WebView içinden çağrılan geri dönüş noktaları
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}

-dontwarn android.webkit.**
