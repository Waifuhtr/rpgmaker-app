package com.waifuhtr.pixelstore.data

import android.content.Context

/**
 * Kalıcı tercihler ve bağlantı ayarları.
 *
 * WordPress adresi boşsa uygulama yerel kipte çalışır (cihazdaki katalog + gömülü demo hesaplar).
 * Adres girilirse veri kaynağı WordPress eklentisinin REST ucu olur.
 */
class Settings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var wordpressUrl: String
        get() = prefs.getString(KEY_WP_URL, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_WP_URL, normalizeUrl(value)).apply()
        }

    /** WordPress oturum jetonu. Yalnızca uygulamanın özel alanında saklanır. */
    var wordpressToken: String
        get() = prefs.getString(KEY_WP_TOKEN, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_WP_TOKEN, value).apply()
        }

    /** Yerel kipte açık olan oturumun kullanıcı adı. Rol buradan değil hesap tablosundan okunur. */
    var localSessionUser: String
        get() = prefs.getString(KEY_LOCAL_USER, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_LOCAL_USER, value).apply()
        }

    /** Kullanıcının "indirdiği" kayıtlar. Kütüphane listesi bundan üretilir. */
    val installedIds: Set<String>
        get() = prefs.getStringSet(KEY_INSTALLED, emptySet())?.toSet() ?: emptySet()

    fun addInstalled(id: String) {
        prefs.edit().putStringSet(KEY_INSTALLED, installedIds + id).apply()
    }

    fun removeInstalled(id: String) {
        prefs.edit().putStringSet(KEY_INSTALLED, installedIds - id).apply()
    }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    val usesWordPress: Boolean get() = wordpressUrl.isNotBlank()

    fun clearWordPressSession() {
        prefs.edit().remove(KEY_WP_TOKEN).apply()
    }

    companion object {
        private const val PREFS = "pixelstore_settings"
        private const val KEY_WP_URL = "wordpress_url"
        private const val KEY_WP_TOKEN = "wordpress_token"
        private const val KEY_LOCAL_USER = "local_session_user"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_INSTALLED = "installed_ids"

        /** Sonundaki eğik çizgileri ve /wp-json kuyruğunu temizler, şema yoksa https ekler. */
        fun normalizeUrl(raw: String): String {
            var url = raw.trim()
            if (url.isEmpty()) return ""
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
            url = url.trimEnd('/')
            listOf("/wp-json/pixelstore/v1", "/wp-json/pixelstore", "/wp-json").forEach { suffix ->
                if (url.endsWith(suffix, ignoreCase = true)) {
                    url = url.dropLast(suffix.length).trimEnd('/')
                }
            }
            return url
        }
    }
}
