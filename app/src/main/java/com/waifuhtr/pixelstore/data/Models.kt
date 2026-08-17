package com.waifuhtr.pixelstore.data

import com.waifuhtr.pixelstore.ui.art.PixelArt
import org.json.JSONArray
import org.json.JSONObject

/** Kullanıcı bekleyen, mesajı doğrudan gösterilebilir hata. */
class PixelStoreException(message: String) : Exception(message)

enum class Role(val wire: String, val label: String) {
    ADMIN("admin", "YÖNETİCİ"),
    USER("user", "KULLANICI");

    companion object {
        fun from(value: String?): Role = if (value == "admin") ADMIN else USER
    }
}

data class Session(
    val username: String,
    val displayName: String,
    val role: Role,
    val avatarSeed: String
) {
    val isAdmin: Boolean get() = role == Role.ADMIN
}

data class UserAccount(
    val username: String,
    val displayName: String,
    val role: Role,
    val avatarSeed: String,
    val joinedAt: String,
    val note: String
)

data class Screenshot(
    val seed: String,
    val kind: PixelArt.ShotKind,
    val scene: PixelArt.Scene,
    val caption: String
)

data class AppRecord(
    val id: String,
    val title: String,
    val developer: String,
    val category: String,
    val version: String,
    val sizeMb: Double,
    val rating: Double,
    val ratingCount: Int,
    val installs: Int,
    val contentRating: String,
    val published: Boolean,
    val updatedAt: String,
    val iconSeed: String,
    val palette: String,
    val tags: List<String>,
    val shortDescription: String,
    val longDescription: String,
    val downloadUrl: String,
    val screenshots: List<Screenshot>
) {
    /** Öne çıkarma skoru: puan ile indirme sayısını birlikte tartar. */
    val featureScore: Double
        get() = rating * kotlin.math.log10(installs + 10.0)
}

data class Category(
    val id: String,
    val name: String,
    val glyph: String
)

data class Catalog(
    val apps: List<AppRecord>,
    val categories: List<Category>
)

data class Stats(
    val totalApps: Int,
    val published: Int,
    val drafts: Int,
    val totalInstalls: Long,
    val averageRating: Double,
    val perCategory: List<Pair<String, Int>>
)

/**
 * JSON dönüşümleri.
 *
 * Yerel tohum dosyası ile WordPress REST yanıtı aynı şemayı kullanır, bu yüzden tek bir
 * ayrıştırıcı iki kaynağa da hizmet eder.
 */
object Json {

    fun appFrom(obj: JSONObject): AppRecord {
        val id = obj.optString("id").ifBlank { "kayit-${obj.optString("title").hashCode()}" }
        return AppRecord(
            id = id,
            title = obj.optString("title").ifBlank { "İsimsiz Kayıt" },
            developer = obj.optString("developer").ifBlank { "Bilinmeyen Stüdyo" },
            category = obj.optString("category").ifBlank { "Diğer" },
            version = obj.optString("version").ifBlank { "1.0.0" },
            sizeMb = obj.optDouble("sizeMb", 12.0).coerceIn(0.1, 4096.0),
            rating = obj.optDouble("rating", 0.0).coerceIn(0.0, 5.0),
            ratingCount = obj.optInt("ratingCount", 0).coerceAtLeast(0),
            installs = obj.optInt("installs", 0).coerceAtLeast(0),
            contentRating = obj.optString("contentRating").ifBlank { "7+" },
            published = obj.optBoolean("published", true),
            updatedAt = obj.optString("updatedAt"),
            iconSeed = obj.optString("iconSeed").ifBlank { id },
            palette = obj.optString("palette").ifBlank { "slate" },
            tags = obj.optJSONArray("tags").toStringList(6),
            shortDescription = obj.optString("shortDescription"),
            longDescription = obj.optString("longDescription"),
            downloadUrl = obj.optString("downloadUrl"),
            screenshots = obj.optJSONArray("screenshots").toScreenshots(id)
        )
    }

    fun appTo(record: AppRecord): JSONObject = JSONObject().apply {
        put("id", record.id)
        put("title", record.title)
        put("developer", record.developer)
        put("category", record.category)
        put("version", record.version)
        put("sizeMb", record.sizeMb)
        put("rating", record.rating)
        put("ratingCount", record.ratingCount)
        put("installs", record.installs)
        put("contentRating", record.contentRating)
        put("published", record.published)
        put("updatedAt", record.updatedAt)
        put("iconSeed", record.iconSeed)
        put("palette", record.palette)
        put("tags", JSONArray(record.tags))
        put("shortDescription", record.shortDescription)
        put("longDescription", record.longDescription)
        put("downloadUrl", record.downloadUrl)
        put("screenshots", JSONArray().apply {
            record.screenshots.forEach { shot ->
                put(JSONObject().apply {
                    put("seed", shot.seed)
                    put("kind", shot.kind.id)
                    put("scene", shot.scene.id)
                    put("caption", shot.caption)
                })
            }
        })
    }

    fun categoryFrom(obj: JSONObject): Category = Category(
        id = obj.optString("id").ifBlank { obj.optString("name").lowercase() },
        name = obj.optString("name").ifBlank { "Diğer" },
        glyph = obj.optString("glyph").ifBlank { "star" }
    )

    fun sessionFrom(obj: JSONObject): Session = Session(
        username = obj.optString("username"),
        displayName = obj.optString("displayName").ifBlank { obj.optString("username") },
        role = Role.from(obj.optString("role")),
        avatarSeed = obj.optString("avatarSeed").ifBlank { obj.optString("username") }
    )

    fun userFrom(obj: JSONObject): UserAccount = UserAccount(
        username = obj.optString("username"),
        displayName = obj.optString("displayName").ifBlank { obj.optString("username") },
        role = Role.from(obj.optString("role")),
        avatarSeed = obj.optString("avatarSeed").ifBlank { obj.optString("username") },
        joinedAt = obj.optString("joinedAt"),
        note = obj.optString("note")
    )

    fun statsFrom(obj: JSONObject): Stats {
        val per = obj.optJSONObject("perCategory")
        val list = mutableListOf<Pair<String, Int>>()
        if (per != null) {
            val keys = per.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                list += key to per.optInt(key)
            }
        }
        return Stats(
            totalApps = obj.optInt("totalApps"),
            published = obj.optInt("published"),
            drafts = obj.optInt("drafts"),
            totalInstalls = obj.optLong("totalInstalls"),
            averageRating = obj.optDouble("averageRating", 0.0),
            perCategory = list.sortedByDescending { it.second }
        )
    }

    fun catalogFrom(obj: JSONObject): Catalog {
        val apps = mutableListOf<AppRecord>()
        obj.optJSONArray("apps")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { apps += appFrom(it) }
            }
        }
        val categories = mutableListOf<Category>()
        obj.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { categories += categoryFrom(it) }
            }
        }
        return Catalog(apps, categories)
    }

    private fun JSONArray?.toStringList(limit: Int): List<String> {
        if (this == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until minOf(length(), limit)) {
            val value = optString(i).trim()
            if (value.isNotEmpty()) out += value
        }
        return out
    }

    private fun JSONArray?.toScreenshots(appId: String): List<Screenshot> {
        if (this == null) return emptyList()
        val out = mutableListOf<Screenshot>()
        for (i in 0 until minOf(length(), 8)) {
            val obj = optJSONObject(i) ?: continue
            out += Screenshot(
                seed = obj.optString("seed").ifBlank { "$appId-$i" },
                kind = PixelArt.ShotKind.from(obj.optString("kind")),
                scene = PixelArt.Scene.from(obj.optString("scene")),
                caption = obj.optString("caption")
            )
        }
        return out
    }
}
