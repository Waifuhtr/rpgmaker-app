package com.waifuhtr.pixelstore.data

import com.waifuhtr.pixelstore.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * riaslink.fun REST istemcisi.
 *
 * Sunucu adresi derleme sırasında gömülür ([BuildConfig.API_BASE]); kullanıcı arayüzünden
 * değiştirilemez ve uygulamada demo/yerel kip yoktur — tek veri kaynağı sitedir.
 *
 * Sunucu tarafı: `wordpress-plugin/pixelstore-bridge`. Her uç `{ "ok": true, ... }` döner;
 * hata durumunda [HttpJson] bunu [PixelStoreException]'a çevirir.
 */
class RiasApi(private val settings: Settings) {

    private val base: String get() = BuildConfig.API_BASE.trimEnd('/')

    private val token: String
        get() = settings.token.ifBlank { throw PixelStoreException("Oturum açılmamış.") }

    private fun url(path: String, params: Map<String, String?> = emptyMap()) =
        base + path + HttpJson.query(params)

    /* ---- Oturum ------------------------------------------------------------------------------ */

    suspend fun health(): String {
        val res = HttpJson.json(url("/health"))
        return buildString {
            appendLine("Bağlantı başarılı.")
            appendLine("Site: ${res.optString("site")}")
            appendLine("Köprü sürümü: ${res.optString("plugin")}")
            appendLine("Yayında oyun: ${res.optInt("games")}")
            val notice = res.optString("notice")
            if (notice.isNotBlank()) append(notice)
        }.trim()
    }

    suspend fun login(username: String, password: String): Session {
        val body = JSONObject().put("username", username.trim()).put("password", password)
        val res = HttpJson.json(url("/auth/login"), method = "POST", body = body)
        val newToken = res.optString("token")
        if (newToken.isBlank()) throw PixelStoreException("Sunucu oturum jetonu döndürmedi.")
        settings.token = newToken
        return Json.session(res.getJSONObject("session"))
    }

    /** Kayıtlı jetonla oturumu sürdürür. Jeton geçersizse temizlenir ve null döner. */
    suspend fun restoreSession(): Session? {
        if (settings.token.isBlank()) return null
        return try {
            Json.session(HttpJson.json(url("/auth/me"), token = settings.token).getJSONObject("session"))
        } catch (e: PixelStoreException) {
            settings.token = ""
            null
        }
    }

    suspend fun logout() {
        val saved = settings.token
        settings.token = ""
        if (saved.isNotBlank()) {
            // Sunucuda da iptal et; başarısız olsa bile yerel oturum kapanmış olur.
            runCatching { HttpJson.json(url("/auth/logout"), method = "POST", token = saved) }
        }
    }

    suspend fun uploadAvatar(fileName: String, contentType: String, bytes: ByteArray): Session {
        val res = HttpJson.upload(url("/auth/avatar"), fileName, contentType, bytes, token)
        return Json.session(res.getJSONObject("session"))
    }

    suspend fun clearAvatar(): Session {
        val res = HttpJson.json(url("/auth/avatar"), method = "DELETE", token = token)
        return Json.session(res.getJSONObject("session"))
    }

    /* ---- Katalog ----------------------------------------------------------------------------- */

    suspend fun games(
        page: Int = 1,
        perPage: Int = 20,
        search: String? = null,
        genre: String? = null,
        platform: String? = null,
        language: String? = null,
        status: String? = null,
        sort: String? = null,
        featuredOnly: Boolean = false
    ): GamePage {
        val params = mapOf(
            "page" to page.toString(),
            "per_page" to perPage.toString(),
            "search" to search,
            "game_genre" to genre,
            "game_platform" to platform,
            "game_language" to language,
            "game_status" to status,
            "sort" to sort,
            "featured" to if (featuredOnly) "1" else null
        )
        return Json.gamePage(HttpJson.json(url("/games", params), token = token))
    }

    suspend fun game(id: String): GameDetail =
        Json.detail(HttpJson.json(url("/games/${HttpJson.encode(id)}"), token = token).getJSONObject("game"))

    suspend fun search(query: String, limit: Int = 8): List<SearchHit> {
        val res = HttpJson.json(url("/search", mapOf("q" to query, "limit" to limit.toString())), token = token)
        val arr = res.optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<SearchHit>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out += Json.searchHit(it) }
        }
        return out
    }

    suspend fun taxonomies(): Taxonomies =
        Json.taxonomies(HttpJson.json(url("/taxonomies"), token = token).optJSONObject("taxonomies"))

    /* ---- Etkileşim --------------------------------------------------------------------------- */

    suspend fun recordView(id: String) {
        runCatching { HttpJson.json(url("/games/${HttpJson.encode(id)}/view"), method = "POST", token = token) }
    }

    suspend fun download(id: String, mirror: Boolean): DownloadTicket {
        val res = HttpJson.json(
            url("/games/${HttpJson.encode(id)}/download"),
            method = "POST",
            body = JSONObject().put("mirror", mirror),
            token = token
        )
        return DownloadTicket(
            url = res.optString("url"),
            downloadCount = res.optInt("downloadCount"),
            password = res.optString("password")
        )
    }

    /** @return yeni favori durumu ve listedeki toplam kayıt sayısı. */
    suspend fun toggleFavorite(id: String): Pair<Boolean, Int> {
        val res = HttpJson.json(url("/games/${HttpJson.encode(id)}/favorite"), method = "POST", token = token)
        return res.optBoolean("favorited") to res.optInt("count")
    }

    suspend fun favorites(): List<GameSummary> =
        Json.games(HttpJson.json(url("/favorites"), token = token))

    /** @return ortalama puan, oy sayısı ve kullanıcının kendi oyu. */
    suspend fun rate(id: String, rating: Int): Triple<Double, Int, Int> {
        val res = HttpJson.json(
            url("/games/${HttpJson.encode(id)}/rate"),
            method = "POST",
            body = JSONObject().put("rating", rating),
            token = token
        )
        return Triple(res.optDouble("rating", 0.0), res.optInt("ratingCount"), res.optInt("userRating"))
    }

    suspend fun reviews(id: String): Pair<List<Review>, Boolean> {
        val res = HttpJson.json(url("/games/${HttpJson.encode(id)}/reviews"), token = token)
        return parseReviews(res) to res.optBoolean("hasReviewed")
    }

    suspend fun addReview(id: String, content: String, recommended: Boolean): List<Review> {
        val res = HttpJson.json(
            url("/games/${HttpJson.encode(id)}/reviews"),
            method = "POST",
            body = JSONObject().put("content", content).put("recommended", recommended),
            token = token
        )
        return parseReviews(res)
    }

    suspend fun deleteReview(reviewId: Int) {
        HttpJson.json(url("/reviews/$reviewId"), method = "DELETE", token = token)
    }

    suspend fun voteReview(reviewId: Int, up: Boolean) {
        HttpJson.json(
            url("/reviews/$reviewId/vote"),
            method = "POST",
            body = JSONObject().put("direction", if (up) "up" else "down"),
            token = token
        )
    }

    suspend fun report(id: String, message: String) {
        HttpJson.json(
            url("/games/${HttpJson.encode(id)}/report"),
            method = "POST",
            body = JSONObject().put("message", message),
            token = token
        )
    }

    private fun parseReviews(res: JSONObject): List<Review> {
        val arr: JSONArray = res.optJSONArray("reviews") ?: return emptyList()
        val out = mutableListOf<Review>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out += Json.review(it) }
        }
        return out
    }

    /* ---- Yönetim ----------------------------------------------------------------------------- */

    suspend fun stats(): Stats =
        Json.stats(HttpJson.json(url("/stats"), token = token).getJSONObject("stats"))

    suspend fun users(): List<UserAccount> {
        val res = HttpJson.json(url("/users"), token = token)
        val arr = res.optJSONArray("users") ?: return emptyList()
        val out = mutableListOf<UserAccount>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out += Json.user(it) }
        }
        return out
    }

    suspend fun saveGame(form: GameForm, existingId: String?): GameDetail {
        val body = form.toJson()
        val res = if (existingId.isNullOrBlank()) {
            HttpJson.json(url("/games"), method = "POST", body = body, token = token)
        } else {
            HttpJson.json(url("/games/${HttpJson.encode(existingId)}"), method = "PUT", body = body, token = token)
        }
        return Json.detail(res.getJSONObject("game"))
    }

    suspend fun deleteGame(id: String) {
        HttpJson.json(url("/games/${HttpJson.encode(id)}"), method = "DELETE", token = token)
    }

    suspend fun setPublished(id: String, published: Boolean) {
        HttpJson.json(
            url("/games/${HttpJson.encode(id)}/published"),
            method = "POST",
            body = JSONObject().put("published", published),
            token = token
        )
    }

    suspend fun uploadCover(id: String, fileName: String, contentType: String, bytes: ByteArray): String {
        val res = HttpJson.upload(
            url("/games/${HttpJson.encode(id)}/cover"), fileName, contentType, bytes, token
        )
        return res.optString("coverUrl")
    }

    suspend fun uploadScreenshot(id: String, fileName: String, contentType: String, bytes: ByteArray): GameDetail {
        val res = HttpJson.upload(
            url("/games/${HttpJson.encode(id)}/screenshots"), fileName, contentType, bytes, token
        )
        return Json.detail(res.getJSONObject("game"))
    }

    suspend fun deleteScreenshot(id: String, attachmentId: Int): GameDetail {
        val res = HttpJson.json(
            url("/games/${HttpJson.encode(id)}/screenshots/$attachmentId"),
            method = "DELETE",
            token = token
        )
        return Json.detail(res.getJSONObject("game"))
    }
}

/**
 * Yönetim düzenleyicisinin gönderdiği form.
 *
 * Alan adları sunucudaki meta anahtarlarıyla aynı tutulur; böylece köprüde ek eşleme tablosu
 * gerekmez ve uygulamadan yazılan değer wp-admin'de aynı kutuda görünür.
 */
data class GameForm(
    val title: String,
    val description: String,
    val excerpt: String,
    val subtitle: String,
    val version: String,
    val sizeLabel: String,
    val releaseDate: String,
    val ageRating: String,
    val licenseType: String,
    val changelog: String,
    val installGuide: String,
    val downloadUrl: String,
    val mirrorUrl: String,
    val archivePassword: String,
    val trailerUrl: String,
    val genres: List<String>,
    val platforms: List<String>,
    val languages: List<String>,
    val developer: String,
    val publisher: String,
    val status: String,
    val tags: List<String>,
    val featured: Boolean,
    val editorsChoice: Boolean,
    val multiplayer: Boolean,
    val controller: Boolean,
    val published: Boolean,
    val minimum: SpecSet,
    val recommended: SpecSet
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("description", description)
        put("excerpt", excerpt)
        put("game_subtitle", subtitle)
        put("game_version", version)
        put("game_size", sizeLabel)
        put("game_release_date", releaseDate)
        put("game_age_rating", ageRating)
        put("game_license_type", licenseType)
        put("game_changelog", changelog)
        put("game_installation_guide", installGuide)
        put("game_download_url", downloadUrl)
        put("game_download_mirror", mirrorUrl)
        put("game_download_password", archivePassword)
        put("game_trailer_url", trailerUrl)
        put("genres", JSONArray(genres))
        put("platforms", JSONArray(platforms))
        put("languages", JSONArray(languages))
        put("developer", developer)
        put("publisher", publisher)
        put("status", status)
        put("tags", JSONArray(tags))
        put("game_featured", featured)
        put("game_editors_choice", editorsChoice)
        put("game_multiplayer_support", multiplayer)
        put("game_controller_support", controller)
        put("published", published)
        put("minimum_os", minimum.os)
        put("minimum_cpu", minimum.cpu)
        put("minimum_ram", minimum.ram)
        put("minimum_gpu", minimum.gpu)
        put("minimum_storage", minimum.storage)
        put("recommended_os", recommended.os)
        put("recommended_cpu", recommended.cpu)
        put("recommended_ram", recommended.ram)
        put("recommended_gpu", recommended.gpu)
        put("recommended_storage", recommended.storage)
    }

    companion object {
        fun from(detail: GameDetail) = GameForm(
            title = detail.title,
            description = detail.description,
            excerpt = detail.summary.excerpt,
            subtitle = detail.summary.subtitle,
            version = detail.summary.version,
            sizeLabel = detail.summary.sizeLabel,
            releaseDate = detail.releaseDate,
            ageRating = detail.ageRating,
            licenseType = detail.licenseType,
            changelog = detail.changelog,
            installGuide = detail.installGuide,
            downloadUrl = detail.downloadUrl,
            mirrorUrl = detail.mirrorUrl,
            archivePassword = detail.archivePassword,
            trailerUrl = detail.trailerUrl,
            genres = detail.summary.genres,
            platforms = detail.platforms,
            languages = detail.languages,
            developer = detail.summary.developer,
            publisher = detail.summary.publisher,
            status = detail.summary.status,
            tags = detail.tags,
            featured = detail.summary.featured,
            editorsChoice = detail.summary.editorsChoice,
            multiplayer = detail.multiplayer,
            controller = detail.controller,
            published = detail.summary.published,
            minimum = detail.requirements.minimum,
            recommended = detail.requirements.recommended
        )

        fun empty() = GameForm(
            title = "", description = "", excerpt = "", subtitle = "", version = "",
            sizeLabel = "", releaseDate = "", ageRating = "", licenseType = "",
            changelog = "", installGuide = "", downloadUrl = "", mirrorUrl = "",
            archivePassword = "", trailerUrl = "", genres = emptyList(),
            platforms = emptyList(), languages = emptyList(), developer = "",
            publisher = "", status = "", tags = emptyList(), featured = false,
            editorsChoice = false, multiplayer = false, controller = false,
            published = true,
            minimum = SpecSet("", "", "", "", ""),
            recommended = SpecSet("", "", "", "", "")
        )
    }
}
