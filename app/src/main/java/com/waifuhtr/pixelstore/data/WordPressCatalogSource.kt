package com.waifuhtr.pixelstore.data

import org.json.JSONObject

/**
 * WordPress kaynağı — veritabanı olarak WordPress kullanır.
 *
 * Sunucu tarafı `wordpress-plugin/pixelstore-api` eklentisidir. Kayıtlar `pixelstore_app` özel
 * yazı tipinde tutulur, uçlar `/wp-json/pixelstore/v1/...` altındadır.
 *
 * Yetki kararı tamamen sunucudadır: jeton WordPress kullanıcısına bağlıdır ve yönetim uçları
 * `manage_options`/`edit_posts` yeteneklerini kontrol eder. İstemci "ben adminim" diyerek veri
 * alamaz; buradaki rol yalnızca arayüzün ne göstereceğini belirler.
 */
class WordPressCatalogSource(
    private val settings: Settings
) : CatalogSource {

    override val kind = SourceKind.WORDPRESS

    @Volatile
    private var session: Session? = null

    private val base: String
        get() {
            val url = settings.wordpressUrl
            if (url.isBlank()) throw PixelStoreException("WordPress adresi ayarlanmamış.")
            return "$url/wp-json/$NAMESPACE"
        }

    private val token: String
        get() = settings.wordpressToken.ifBlank {
            throw PixelStoreException("Oturum açılmamış.")
        }

    override fun currentSession(): Session? = session

    override suspend fun restoreSession(): Session? {
        if (settings.wordpressToken.isBlank()) return null
        return try {
            val response = HttpJson.request("$base/auth/me", token = settings.wordpressToken)
            Json.sessionFrom(response.getJSONObject("session")).also { session = it }
        } catch (e: PixelStoreException) {
            // Jeton geçersiz veya sunucu erişilemez; kullanıcıyı giriş ekranına düşür.
            settings.clearWordPressSession()
            session = null
            null
        }
    }

    override suspend fun login(username: String, password: String): Session {
        val body = JSONObject()
            .put("username", username.trim())
            .put("password", password)
        val response = HttpJson.request("$base/auth/login", method = "POST", body = body)
        val newToken = response.optString("token")
        if (newToken.isBlank()) throw PixelStoreException("Sunucu jeton döndürmedi.")
        settings.wordpressToken = newToken
        return Json.sessionFrom(response.getJSONObject("session")).also { session = it }
    }

    override suspend fun logout() {
        val saved = settings.wordpressToken
        session = null
        settings.clearWordPressSession()
        if (saved.isNotBlank()) {
            // Jetonu sunucuda da iptal et; başarısız olsa bile yerel oturum kapanmış olur.
            runCatching { HttpJson.request("$base/auth/logout", method = "POST", token = saved) }
        }
    }

    override suspend fun catalog(): Catalog =
        Json.catalogFrom(HttpJson.request("$base/catalog", token = token))

    override suspend fun app(id: String): AppRecord {
        val response = HttpJson.request("$base/apps/${encode(id)}", token = token)
        return Json.appFrom(response.getJSONObject("app"))
    }

    override suspend fun recordInstall(id: String): Int {
        val response = HttpJson.request(
            "$base/apps/${encode(id)}/install", method = "POST", token = token
        )
        return response.optInt("installs")
    }

    override suspend fun saveApp(record: AppRecord): AppRecord {
        val body = Json.appTo(record)
        val response = if (record.id.isBlank()) {
            HttpJson.request("$base/apps", method = "POST", body = body, token = token)
        } else {
            HttpJson.request("$base/apps/${encode(record.id)}", method = "PUT", body = body, token = token)
        }
        return Json.appFrom(response.getJSONObject("app"))
    }

    override suspend fun deleteApp(id: String) {
        HttpJson.request("$base/apps/${encode(id)}", method = "DELETE", token = token)
    }

    override suspend fun setPublished(id: String, published: Boolean) {
        HttpJson.request(
            "$base/apps/${encode(id)}/published",
            method = "POST",
            body = JSONObject().put("published", published),
            token = token
        )
    }

    override suspend fun users(): List<UserAccount> {
        val response = HttpJson.request("$base/users", token = token)
        val arr = response.optJSONArray("users") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { Json.userFrom(it) }
        }
    }

    override suspend fun stats(): Stats =
        Json.statsFrom(HttpJson.request("$base/stats", token = token).getJSONObject("stats"))

    override suspend fun resetCatalog() {
        HttpJson.request("$base/seed", method = "POST", token = token)
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        private const val NAMESPACE = "pixelstore/v1"

        /**
         * Bağlantı sınaması. Kayıtlı ayarlara dokunmaz, verilen adresi doğrudan dener; bu yüzden
         * "Bağlantıyı sına" düğmesi mevcut kipi bozmadan çalışır.
         */
        suspend fun probe(normalizedUrl: String): String {
            if (normalizedUrl.isBlank()) {
                throw PixelStoreException("Adres boş. Boş bırakılırsa uygulama yerel kipte çalışır.")
            }
            val response = HttpJson.request("$normalizedUrl/wp-json/$NAMESPACE/health")
            val plugin = response.optString("plugin").ifBlank { "?" }
            val apps = response.optInt("apps", -1)
            val site = response.optString("site")
            return buildString {
                appendLine("Bağlantı başarılı.")
                appendLine("Eklenti sürümü: $plugin")
                if (apps >= 0) appendLine("Sunucudaki kayıt: $apps")
                if (site.isNotBlank()) append("Site: $site")
            }.trim()
        }
    }
}
