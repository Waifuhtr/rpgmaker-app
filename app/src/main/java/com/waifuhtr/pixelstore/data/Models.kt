package com.waifuhtr.pixelstore.data

import org.json.JSONArray
import org.json.JSONObject

/** Kullanıcıya doğrudan gösterilebilir hata. */
class PixelStoreException(message: String) : Exception(message)

enum class Role(val wire: String, val label: String) {
    ADMIN("admin", "YÖNETİCİ"),
    USER("user", "KULLANICI");

    companion object {
        fun from(value: String?): Role = if (value == "admin") ADMIN else USER
    }
}

data class Badge(val name: String, val color: String)

data class Session(
    val username: String,
    val displayName: String,
    val role: Role,
    val avatarUrl: String,
    val joinedAt: String,
    val favoriteCount: Int,
    val reviewCount: Int,
    val badges: List<Badge>
) {
    val isAdmin: Boolean get() = role == Role.ADMIN
}

data class UserAccount(
    val username: String,
    val displayName: String,
    val role: Role,
    val roleLabel: String,
    val avatarUrl: String,
    val joinedAt: String,
    val favoriteCount: Int,
    val badges: List<Badge>
)

/** Terim (tür, platform, dil, durum) — filtre çipleri bunlardan üretilir. */
data class Term(
    val slug: String,
    val name: String,
    val count: Int,
    val icon: String,
    val iconUrl: String
)

data class Taxonomies(
    val genres: List<Term> = emptyList(),
    val platforms: List<Term> = emptyList(),
    val languages: List<Term> = emptyList(),
    val statuses: List<Term> = emptyList()
)

/** Liste kartı için hafif kayıt. */
data class GameSummary(
    val id: String,
    val postId: Int,
    val title: String,
    val subtitle: String,
    val excerpt: String,
    val coverUrl: String,
    val developer: String,
    val publisher: String,
    val platform: String,
    val language: String,
    val status: String,
    val genres: List<String>,
    val version: String,
    val sizeLabel: String,
    val rating: Double,
    val ratingCount: Int,
    val downloadCount: Int,
    val viewCount: Int,
    val featured: Boolean,
    val editorsChoice: Boolean,
    val published: Boolean,
    val updatedAt: String,
    val favorited: Boolean
)

data class Screenshot(val id: Int, val url: String, val fullUrl: String)

data class SpecSet(
    val os: String,
    val cpu: String,
    val ram: String,
    val gpu: String,
    val storage: String
) {
    /** Bilgi satırlarına dökülecek dolu alanlar. */
    fun rows(): List<Pair<String, String>> = listOfNotNull(
        ("İşletim sistemi" to os).takeIf { os.isNotBlank() },
        ("İşlemci" to cpu).takeIf { cpu.isNotBlank() },
        ("Bellek" to ram).takeIf { ram.isNotBlank() },
        ("Ekran kartı" to gpu).takeIf { gpu.isNotBlank() },
        ("Depolama" to storage).takeIf { storage.isNotBlank() }
    )
}

data class Requirements(
    val minimum: SpecSet,
    val recommended: SpecSet,
    val hasMinimum: Boolean,
    val hasRecommended: Boolean
)

/** Detay ekranı ve yönetim düzenleyicisi için tam kayıt. */
data class GameDetail(
    val summary: GameSummary,
    val description: String,
    val changelog: String,
    val installGuide: String,
    val heroUrl: String,
    val screenshots: List<Screenshot>,
    val trailerUrl: String,
    val downloadUrl: String,
    val mirrorUrl: String,
    val archivePassword: String,
    val releaseDate: String,
    val ageRating: String,
    val licenseType: String,
    val multiplayer: Boolean,
    val controller: Boolean,
    val features: List<String>,
    val platforms: List<String>,
    val languages: List<String>,
    val tags: List<String>,
    val permalink: String,
    val requirements: Requirements,
    val userRating: Int,
    val reviewCount: Int,
    val hasReviewed: Boolean
) {
    val id: String get() = summary.id
    val title: String get() = summary.title
}

data class Review(
    val id: Int,
    val author: String,
    val authorId: Int,
    val avatarUrl: String,
    val content: String,
    val recommended: Boolean,
    val playtime: String,
    val date: String,
    val upvotes: Int,
    val downvotes: Int,
    val voted: Boolean,
    val mine: Boolean,
    val badges: List<Badge>
)

data class SearchHit(
    val id: String,
    val title: String,
    val coverUrl: String,
    val platform: String,
    val rating: Double
)

data class Stats(
    val totalGames: Int,
    val published: Int,
    val drafts: Int,
    val totalDownloads: Int,
    val totalViews: Int,
    val averageRating: Double,
    val reviewCount: Int,
    val reportCount: Int,
    val userCount: Int,
    val perGenre: List<Pair<String, Int>>
)

data class GamePage(
    val games: List<GameSummary>,
    val page: Int,
    val pages: Int,
    val total: Int
)

data class DownloadTicket(
    val url: String,
    val downloadCount: Int,
    val password: String
)

/**
 * JSON → model dönüşümleri.
 *
 * Sunucu tarafındaki `class-psb-mapper.php` ile birebir eşlenir; alan adları orada da aynıdır.
 * Eksik alanlar sessizce boş/sıfır olur: sitede yarım doldurulmuş kayıt uygulamayı çökertmesin.
 */
object Json {

    fun session(obj: JSONObject) = Session(
        username = obj.optString("username"),
        displayName = obj.optString("displayName").ifBlank { obj.optString("username") },
        role = Role.from(obj.optString("role")),
        avatarUrl = obj.optString("avatarUrl"),
        joinedAt = obj.optString("joinedAt"),
        favoriteCount = obj.optInt("favoriteCount"),
        reviewCount = obj.optInt("reviewCount"),
        badges = badges(obj.optJSONArray("badges"))
    )

    fun user(obj: JSONObject) = UserAccount(
        username = obj.optString("username"),
        displayName = obj.optString("displayName").ifBlank { obj.optString("username") },
        role = Role.from(obj.optString("role")),
        roleLabel = obj.optString("roleLabel"),
        avatarUrl = obj.optString("avatarUrl"),
        joinedAt = obj.optString("joinedAt"),
        favoriteCount = obj.optInt("favoriteCount"),
        badges = badges(obj.optJSONArray("badges"))
    )

    private fun badges(arr: JSONArray?): List<Badge> {
        if (arr == null) return emptyList()
        val out = mutableListOf<Badge>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("name")
            if (name.isNotBlank()) out += Badge(name, obj.optString("color").ifBlank { "#3b82f6" })
        }
        return out
    }

    fun summary(obj: JSONObject) = GameSummary(
        id = obj.optString("id"),
        postId = obj.optInt("postId"),
        title = obj.optString("title").ifBlank { "İsimsiz kayıt" },
        subtitle = obj.optString("subtitle"),
        excerpt = obj.optString("excerpt"),
        coverUrl = obj.optString("coverUrl"),
        developer = obj.optString("developer"),
        publisher = obj.optString("publisher"),
        platform = obj.optString("platform"),
        language = obj.optString("language"),
        status = obj.optString("status"),
        genres = strings(obj.optJSONArray("genres")),
        version = obj.optString("version"),
        sizeLabel = obj.optString("sizeLabel"),
        rating = obj.optDouble("rating", 0.0).coerceIn(0.0, 5.0),
        ratingCount = obj.optInt("ratingCount"),
        downloadCount = obj.optInt("downloadCount"),
        viewCount = obj.optInt("viewCount"),
        featured = obj.optBoolean("featured"),
        editorsChoice = obj.optBoolean("editorsChoice"),
        published = obj.optBoolean("published", true),
        updatedAt = obj.optString("updatedAt"),
        favorited = obj.optBoolean("favorited")
    )

    fun detail(obj: JSONObject) = GameDetail(
        summary = summary(obj),
        description = obj.optString("description"),
        changelog = obj.optString("changelog"),
        installGuide = obj.optString("installGuide"),
        heroUrl = obj.optString("heroUrl"),
        screenshots = screenshots(obj.optJSONArray("screenshots")),
        trailerUrl = obj.optString("trailerUrl"),
        downloadUrl = obj.optString("downloadUrl"),
        mirrorUrl = obj.optString("mirrorUrl"),
        archivePassword = obj.optString("archivePassword"),
        releaseDate = obj.optString("releaseDate"),
        ageRating = obj.optString("ageRating"),
        licenseType = obj.optString("licenseType"),
        multiplayer = obj.optBoolean("multiplayer"),
        controller = obj.optBoolean("controller"),
        features = strings(obj.optJSONArray("features")),
        platforms = strings(obj.optJSONArray("platforms")),
        languages = strings(obj.optJSONArray("languages")),
        tags = strings(obj.optJSONArray("tags")),
        permalink = obj.optString("permalink"),
        requirements = requirements(obj.optJSONObject("requirements")),
        userRating = obj.optInt("userRating"),
        reviewCount = obj.optInt("reviewCount"),
        hasReviewed = obj.optBoolean("hasReviewed")
    )

    private fun requirements(obj: JSONObject?): Requirements {
        val min = specs(obj?.optJSONObject("minimum"))
        val rec = specs(obj?.optJSONObject("recommended"))
        return Requirements(
            minimum = min,
            recommended = rec,
            hasMinimum = obj?.optBoolean("hasMinimum") ?: false,
            hasRecommended = obj?.optBoolean("hasRecommended") ?: false
        )
    }

    private fun specs(obj: JSONObject?) = SpecSet(
        os = obj?.optString("os").orEmpty(),
        cpu = obj?.optString("cpu").orEmpty(),
        ram = obj?.optString("ram").orEmpty(),
        gpu = obj?.optString("gpu").orEmpty(),
        storage = obj?.optString("storage").orEmpty()
    )

    private fun screenshots(arr: JSONArray?): List<Screenshot> {
        if (arr == null) return emptyList()
        val out = mutableListOf<Screenshot>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.optString("url")
            if (url.isBlank()) continue
            out += Screenshot(
                id = obj.optInt("id"),
                url = url,
                fullUrl = obj.optString("fullUrl").ifBlank { url }
            )
        }
        return out
    }

    fun review(obj: JSONObject) = Review(
        id = obj.optInt("id"),
        author = obj.optString("author").ifBlank { "Oyuncu" },
        authorId = obj.optInt("authorId"),
        avatarUrl = obj.optString("avatarUrl"),
        content = obj.optString("content"),
        recommended = obj.optBoolean("recommended"),
        playtime = obj.optString("playtime"),
        date = obj.optString("date"),
        upvotes = obj.optInt("upvotes"),
        downvotes = obj.optInt("downvotes"),
        voted = obj.optBoolean("voted"),
        mine = obj.optBoolean("mine"),
        badges = badges(obj.optJSONArray("badges"))
    )

    fun searchHit(obj: JSONObject) = SearchHit(
        id = obj.optString("id"),
        title = obj.optString("title"),
        coverUrl = obj.optString("coverUrl"),
        platform = obj.optString("platform"),
        rating = obj.optDouble("rating", 0.0)
    )

    fun term(obj: JSONObject) = Term(
        slug = obj.optString("slug"),
        name = obj.optString("name"),
        count = obj.optInt("count"),
        icon = obj.optString("icon"),
        iconUrl = obj.optString("iconUrl")
    )

    fun taxonomies(obj: JSONObject?): Taxonomies {
        if (obj == null) return Taxonomies()
        fun list(key: String): List<Term> {
            val arr = obj.optJSONArray(key) ?: return emptyList()
            val out = mutableListOf<Term>()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { out += term(it) }
            }
            return out.filter { it.name.isNotBlank() }
        }
        return Taxonomies(
            genres = list("game_genre"),
            platforms = list("game_platform"),
            languages = list("game_language"),
            statuses = list("game_status")
        )
    }

    fun stats(obj: JSONObject): Stats {
        val per = mutableListOf<Pair<String, Int>>()
        obj.optJSONObject("perGenre")?.let { genres ->
            val keys = genres.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                per += key to genres.optInt(key)
            }
        }
        return Stats(
            totalGames = obj.optInt("totalGames"),
            published = obj.optInt("published"),
            drafts = obj.optInt("drafts"),
            totalDownloads = obj.optInt("totalDownloads"),
            totalViews = obj.optInt("totalViews"),
            averageRating = obj.optDouble("averageRating", 0.0),
            reviewCount = obj.optInt("reviewCount"),
            reportCount = obj.optInt("reportCount"),
            userCount = obj.optInt("userCount"),
            perGenre = per.sortedByDescending { it.second }
        )
    }

    fun gamePage(obj: JSONObject): GamePage {
        val arr = obj.optJSONArray("games")
        val games = mutableListOf<GameSummary>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { games += summary(it) }
            }
        }
        return GamePage(
            games = games,
            page = obj.optInt("page", 1),
            pages = obj.optInt("pages", 1),
            total = obj.optInt("total", games.size)
        )
    }

    fun games(obj: JSONObject, key: String = "games"): List<GameSummary> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        val out = mutableListOf<GameSummary>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out += summary(it) }
        }
        return out
    }

    private fun strings(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val value = arr.optString(i).trim()
            if (value.isNotEmpty()) out += value
        }
        return out
    }
}
