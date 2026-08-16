package com.waifuhtr.pixelstore

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader

/**
 * PixelStore kabuğu.
 *
 * Arayüzün tamamı `assets/www` içindeki HTML/CSS/JS'tir ve [WebViewAssetLoader] sayesinde sabit bir
 * https origin'inden sunulur. Sabit origin, LocalStorage'daki kullanıcı tercihlerinin sürüm
 * güncellemeleri arasında korunmasını sağlar; `file://` erişimi hiç açılmaz.
 */
class MainActivity : ComponentActivity(), StoreBridge.Host {

    private lateinit var webView: WebView
    private lateinit var accounts: AccountManager
    private lateinit var catalog: CatalogRepository

    /**
     * Köprü, WebView'in JavaBridge thread'inden okunur; sayfa geçişlerinde ana thread'de yazılır.
     * Yerel origin dışına çıkılırsa köprü kendini kapatır.
     */
    @Volatile
    private var trustedOrigin = false

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Geri tuşunu önce arayüze soralım: kendi yığınında geri gidebiliyorsa orada kalır.
            webView.evaluateJavascript(JS_HANDLE_BACK) { result ->
                if (result != "true") finish()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        accounts = AccountManager(this)
        catalog = CatalogRepository(this)

        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain(APP_DOMAIN)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Piksel arayüz kendi zeminini çizer; geçiş anında beyaz flaş olmasın.
            setBackgroundColor(BACKDROP_COLOR)
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                // Ses/titreşim/tema tercihleri LocalStorage'da tutulur.
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                // Sistem yazı tipi ölçeği piksel ızgarasını bozmasın.
                textZoom = 100
                useWideViewPort = false
                loadWithOverviewMode = false
            }

            webViewClient = StoreWebViewClient(assetLoader)
            webChromeClient = StoreChromeClient()
            addJavascriptInterface(
                StoreBridge(accounts, catalog, this@MainActivity),
                StoreBridge.NAME
            )
        }

        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, backCallback)

        if (BuildConfig.WEBVIEW_DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // Süreç yeniden oluşturulduysa sayfayı ikinci kez yükleme; aksi halde arayüz iki kez başlar.
            return
        }
        webView.loadUrl(START_URL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
        // Arka plana giderken arayüzdeki basılı/animasyonlu durumları serbest bırak.
        webView.evaluateJavascript(JS_ON_PAUSE, null)
    }

    override fun onResume() {
        super.onResume()
        webView.resumeTimers()
        webView.onResume()
    }

    override fun onDestroy() {
        // WebView'i view ağacından çıkarıp yok et; aksi halde Activity sızar.
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.removeJavascriptInterface(StoreBridge.NAME)
        webView.destroy()
        super.onDestroy()
    }

    // --- StoreBridge.Host ---------------------------------------------------------------------

    override fun isTrustedOrigin(): Boolean = trustedOrigin

    override fun openExternal(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        if (uri.host.isNullOrBlank()) return false
        // user:pass@host biçimli kimlik bilgisi taşıyan adresleri açma.
        if (uri.userInfo != null) return false

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Kendi uygulamamıza geri dönmesin.
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Bağlantıyı açacak uygulama yok: ${e.message}")
            false
        }
    }

    override fun vibrate(durationMs: Int) {
        runOnUiThread {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VibratorManager::class.java))?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            } ?: return@runOnUiThread
            if (!vibrator.hasVibrator()) return@runOnUiThread
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    override fun exitApp() {
        runOnUiThread { finish() }
    }

    // --- WebView istemcileri ------------------------------------------------------------------

    private inner class StoreWebViewClient(
        private val assetLoader: WebViewAssetLoader
    ) : WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val url = request.url
            if (url.scheme == "https" && url.host == APP_DOMAIN) return false

            // Mağaza yerel asset alanından dışarı çıkmaz. Dış bağlantı yalnızca kullanıcı
            // dokunuşuyla ve doğrulamadan geçerek sistem tarayıcısına gider.
            if (request.hasGesture()) {
                openExternal(url.toString())
            } else {
                Log.w(TAG, "Kullanıcı etkileşimi olmadan dış gezinme engellendi: ${url.scheme}")
            }
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            trustedOrigin = isAppUrl(url)
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            trustedOrigin = isAppUrl(url)
        }

        private fun isAppUrl(url: String?): Boolean =
            url != null && url.startsWith("https://$APP_DOMAIN/")
    }

    private class StoreChromeClient : WebChromeClient() {
        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            if (!BuildConfig.WEBVIEW_DEBUG) return true
            Log.d(TAG, "[web] ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
            return true
        }
    }

    companion object {
        private const val TAG = "PixelStore"
        private const val APP_DOMAIN = "appassets.androidplatform.net"
        private const val START_URL = "https://$APP_DOMAIN/assets/www/index.html"
        private const val BACKDROP_COLOR = 0xFF10101C.toInt()

        private const val JS_HANDLE_BACK =
            "(function(){return !!(window.PixelStore && PixelStore.handleBack && PixelStore.handleBack());})()"
        private const val JS_ON_PAUSE =
            "(function(){if(window.PixelStore && PixelStore.onHostPause){PixelStore.onHostPause();}})()"
    }
}
