package com.waifuhtr.pixelstore

import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject

/**
 * WebView ile native taraf arasındaki tek köprü.
 *
 * Tasarım kuralları:
 *  - Yüzey dar ve isimlendirilmiştir; genel amaçlı `eval`, `readFile`, `openUrl(any)` yoktur.
 *  - Rol kararını yalnızca native taraf verir. JS'ten gelen "ben adminim" iddiası dikkate alınmaz;
 *    admin uçları [requireAdmin] ile korunur ve yetkisiz çağrıya veri değil hata döner.
 *  - Kullanıcı rolündeyken admin verisi (taslak kayıtlar, kullanıcı listesi, istatistik) hiç
 *    üretilmez; DevTools ile de erişilemez.
 *  - Köprü metotları WebView'in JavaBridge thread'inde çalışır, bu yüzden UI'ya dokunan işler
 *    [host] üzerinden ana thread'e devredilir.
 */
class StoreBridge(
    private val accounts: AccountManager,
    private val catalog: CatalogRepository,
    private val host: Host
) {

    /** Köprünün ihtiyaç duyduğu, Activity tarafından sağlanan yetenekler. */
    interface Host {
        /** Yalnızca doğrulanmış http/https adresleri için sistem tarayıcısını açar. */
        fun openExternal(url: String): Boolean
        fun vibrate(durationMs: Int)
        fun exitApp()

        /**
         * WebView hâlâ paketlenmiş yerel origin'de mi? Dış bir sayfaya gidilmişse köprü kapanır.
         * Ana thread'de güncellenen volatile bir bayrak okur.
         */
        fun isTrustedOrigin(): Boolean
    }

    // --- Oturum ------------------------------------------------------------------------------

    @JavascriptInterface
    fun login(username: String?, password: String?): String = guarded {
        val session = accounts.login(username.orEmpty(), password.orEmpty())
            ?: return@guarded fail("Kullanıcı adı veya parola hatalı.")
        Log.i(TAG, "Oturum açıldı: rol=${session.role.wire}")
        ok(JSONObject().put("session", session.toJson()))
    }

    @JavascriptInterface
    fun logout(): String = guarded {
        accounts.logout()
        ok(JSONObject())
    }

    @JavascriptInterface
    fun getSession(): String = guarded {
        val session = accounts.session ?: return@guarded ok(JSONObject().put("session", JSONObject.NULL))
        ok(JSONObject().put("session", session.toJson()))
    }

    // --- Mağaza (her rol) --------------------------------------------------------------------

    @JavascriptInterface
    fun getCatalog(): String = guarded {
        val session = accounts.session ?: return@guarded fail(ERR_AUTH)
        val apps = if (session.isAdmin) catalog.allApps() else catalog.publicApps()
        ok(
            JSONObject()
                .put("apps", apps)
                .put("categories", catalog.categories())
                .put("role", session.role.wire)
        )
    }

    @JavascriptInterface
    fun getApp(id: String?): String = guarded {
        val session = accounts.session ?: return@guarded fail(ERR_AUTH)
        val appId = id.orEmpty().take(64)
        val app = (if (session.isAdmin) catalog.findForAdmin(appId) else catalog.findPublic(appId))
            ?: return@guarded fail("Kayıt bulunamadı.")
        ok(JSONObject().put("app", app))
    }

    @JavascriptInterface
    fun recordInstall(id: String?): String = guarded {
        accounts.session ?: return@guarded fail(ERR_AUTH)
        val installs = catalog.recordInstall(id.orEmpty().take(64))
        if (installs < 0) return@guarded fail("Kayıt bulunamadı.")
        ok(JSONObject().put("installs", installs))
    }

    @JavascriptInterface
    fun openExternalUrl(url: String?): String = guarded {
        accounts.session ?: return@guarded fail(ERR_AUTH)
        val target = url.orEmpty().trim()
        if (target.length > MAX_URL) return@guarded fail("Bağlantı çok uzun.")
        if (!target.startsWith("https://") && !target.startsWith("http://")) {
            return@guarded fail("Yalnızca http/https bağlantıları açılabilir.")
        }
        if (host.openExternal(target)) ok(JSONObject()) else fail("Bağlantı açılamadı.")
    }

    // --- Admin uçları ------------------------------------------------------------------------

    @JavascriptInterface
    fun adminSaveApp(payload: String?): String = guarded {
        requireAdmin()?.let { return@guarded it }
        val json = runCatching { JSONObject(payload.orEmpty()) }.getOrNull()
            ?: return@guarded fail("Geçersiz kayıt verisi.")
        val saved = runCatching { catalog.upsert(json) }.getOrElse {
            Log.w(TAG, "Kayıt yazılamadı: ${it.message}")
            return@guarded fail("Kayıt kaydedilemedi.")
        }
        ok(JSONObject().put("app", saved))
    }

    @JavascriptInterface
    fun adminDeleteApp(id: String?): String = guarded {
        requireAdmin()?.let { return@guarded it }
        if (!catalog.delete(id.orEmpty().take(64))) return@guarded fail("Kayıt bulunamadı.")
        ok(JSONObject())
    }

    @JavascriptInterface
    fun adminSetPublished(id: String?, published: Boolean): String = guarded {
        requireAdmin()?.let { return@guarded it }
        if (!catalog.setPublished(id.orEmpty().take(64), published)) {
            return@guarded fail("Kayıt bulunamadı.")
        }
        ok(JSONObject().put("published", published))
    }

    @JavascriptInterface
    fun adminListUsers(): String = guarded {
        requireAdmin()?.let { return@guarded it }
        val list = JSONArray()
        accounts.directory().forEach { account ->
            list.put(
                JSONObject()
                    .put("username", account.username)
                    .put("displayName", account.displayName)
                    .put("role", account.role.wire)
                    .put("avatarSeed", account.avatarSeed)
                    .put("joinedAt", account.joinedAt)
                    .put("note", account.note)
            )
        }
        ok(JSONObject().put("users", list))
    }

    @JavascriptInterface
    fun adminGetStats(): String = guarded {
        requireAdmin()?.let { return@guarded it }
        ok(JSONObject().put("stats", catalog.stats()))
    }

    @JavascriptInterface
    fun adminResetCatalog(): String = guarded {
        requireAdmin()?.let { return@guarded it }
        catalog.resetToSeed()
        Log.i(TAG, "Katalog tohum veriye döndürüldü.")
        ok(JSONObject())
    }

    // --- Cihaz yardımcıları ------------------------------------------------------------------

    @JavascriptInterface
    fun vibrate(durationMs: Int) {
        if (!host.isTrustedOrigin()) return
        host.vibrate(durationMs.coerceIn(MIN_VIBRATE_MS, MAX_VIBRATE_MS))
    }

    @JavascriptInterface
    fun exitApp() {
        if (!host.isTrustedOrigin()) return
        host.exitApp()
    }

    @JavascriptInterface
    fun getBuildInfo(): String = guarded {
        ok(
            JSONObject()
                .put("versionName", BuildConfig.VERSION_NAME)
                .put("versionCode", BuildConfig.VERSION_CODE)
                .put("applicationId", BuildConfig.APPLICATION_ID)
                .put("bridgeApi", BRIDGE_API_VERSION)
                .put("debug", BuildConfig.DEBUG)
        )
    }

    // --- Ortak yardımcılar -------------------------------------------------------------------

    /**
     * Her köprü çağrısını sarar: origin kontrolü yapar ve beklenmedik hatanın WebView'e ham
     * exception olarak sızmasını engeller.
     */
    private inline fun guarded(block: () -> String): String {
        if (!host.isTrustedOrigin()) {
            Log.w(TAG, "Köprü çağrısı güvenilmeyen origin'den geldi, reddedildi.")
            return fail("Köprü bu sayfada kullanılamaz.")
        }
        return runCatching(block).getOrElse {
            Log.e(TAG, "Köprü çağrısı başarısız: ${it.javaClass.simpleName}")
            fail("Beklenmeyen bir hata oluştu.")
        }
    }

    /** Admin değilse hazır hata yükünü döner, adminse null. */
    private fun requireAdmin(): String? {
        val session = accounts.session ?: return fail(ERR_AUTH)
        if (!session.isAdmin) {
            Log.w(TAG, "Yetkisiz admin çağrısı engellendi.")
            return fail(ERR_FORBIDDEN)
        }
        return null
    }

    private fun Session.toJson() = JSONObject()
        .put("username", username)
        .put("displayName", displayName)
        .put("role", role.wire)
        .put("avatarSeed", avatarSeed)
        .put("isAdmin", isAdmin)

    private fun ok(data: JSONObject): String = data.put("ok", true).toString()

    private fun fail(message: String): String =
        JSONObject().put("ok", false).put("error", message).toString()

    companion object {
        private const val TAG = "StoreBridge"
        const val NAME = "PixelNative"
        const val BRIDGE_API_VERSION = 1
        private const val ERR_AUTH = "Oturum açılmamış."
        private const val ERR_FORBIDDEN = "Bu işlem için yönetici yetkisi gerekir."
        private const val MAX_URL = 512
        private const val MIN_VIBRATE_MS = 5
        private const val MAX_VIBRATE_MS = 80
    }
}
