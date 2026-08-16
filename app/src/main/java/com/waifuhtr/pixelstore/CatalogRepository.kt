package com.waifuhtr.pixelstore

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Katalog deposu.
 *
 * Tohum veri `assets/www/data/catalog.json` içindedir ve ilk açılışta uygulamanın özel dizinine
 * kopyalanır. Sonraki tüm yazma işlemleri (admin paneli) bu kopyada yapılır; asset dosyası
 * salt-okunurdur.
 *
 * Yazma işlemleri atomiktir: önce `.tmp` dosyasına yazılır, sonra rename edilir. Böylece yarım
 * yazılmış bir dosya kataloğu bozmaz.
 */
class CatalogRepository(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)
    private val lock = Any()
    private val loaded = AtomicBoolean(false)

    private var root: JSONObject = JSONObject()

    private fun ensureLoaded() {
        if (loaded.get()) return
        synchronized(lock) {
            if (loaded.get()) return
            root = readFromDisk() ?: readSeed()
            loaded.set(true)
        }
    }

    private fun readFromDisk(): JSONObject? {
        if (!file.exists()) return null
        return runCatching { JSONObject(file.readText(Charsets.UTF_8)) }
            .onFailure { Log.w(TAG, "Kayıtlı katalog okunamadı, tohum veriye dönülüyor: ${it.message}") }
            .getOrNull()
    }

    private fun readSeed(): JSONObject {
        val text = appContext.assets.open(SEED_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val seed = JSONObject(text)
        persist(seed)
        return seed
    }

    private fun persist(value: JSONObject) {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(value.toString(), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            // renameTo bazı dosya sistemlerinde hedef varsa başarısız olur.
            file.delete()
            if (!tmp.renameTo(file)) {
                throw IllegalStateException("Katalog kaydedilemedi: ${file.absolutePath}")
            }
        }
    }

    private fun apps(): JSONArray {
        ensureLoaded()
        if (!root.has(KEY_APPS)) root.put(KEY_APPS, JSONArray())
        return root.getJSONArray(KEY_APPS)
    }

    private fun indexOf(id: String): Int {
        val list = apps()
        for (i in 0 until list.length()) {
            if (list.getJSONObject(i).optString("id") == id) return i
        }
        return -1
    }

    /** Kullanıcı görünümü: yalnızca yayınlanmış kayıtlar, yönetim alanları olmadan. */
    fun publicApps(): JSONArray = synchronized(lock) {
        val list = apps()
        val out = JSONArray()
        for (i in 0 until list.length()) {
            val app = list.getJSONObject(i)
            if (!app.optBoolean("published", true)) continue
            out.put(stripAdminFields(app))
        }
        out
    }

    /** Admin görünümü: taslaklar ve yönetim alanları dahil her şey. */
    fun allApps(): JSONArray = synchronized(lock) {
        val list = apps()
        val out = JSONArray()
        for (i in 0 until list.length()) out.put(JSONObject(list.getJSONObject(i).toString()))
        out
    }

    private fun stripAdminFields(app: JSONObject): JSONObject {
        val copy = JSONObject(app.toString())
        ADMIN_ONLY_FIELDS.forEach { copy.remove(it) }
        return copy
    }

    fun categories(): JSONArray = synchronized(lock) {
        ensureLoaded()
        root.optJSONArray(KEY_CATEGORIES) ?: JSONArray()
    }

    fun findPublic(id: String): JSONObject? = synchronized(lock) {
        val idx = indexOf(id)
        if (idx < 0) return null
        val app = apps().getJSONObject(idx)
        if (!app.optBoolean("published", true)) return null
        stripAdminFields(app)
    }

    fun findForAdmin(id: String): JSONObject? = synchronized(lock) {
        val idx = indexOf(id)
        if (idx < 0) return null
        JSONObject(apps().getJSONObject(idx).toString())
    }

    /** Yeni kayıt ekler veya mevcut id'yi günceller. Sadece admin köprüsünden çağrılır. */
    fun upsert(incoming: JSONObject): JSONObject = synchronized(lock) {
        val list = apps()
        val id = incoming.optString("id").ifBlank { generateId(incoming.optString("title")) }
        val sanitized = sanitize(incoming, id)
        val idx = indexOf(id)
        if (idx >= 0) {
            val existing = list.getJSONObject(idx)
            // İndirme sayacı gibi türetilmiş alanlar formdan gelen veriyle ezilmemeli.
            sanitized.put("installs", existing.optInt("installs", 0))
            sanitized.put("createdAt", existing.optString("createdAt", sanitized.optString("updatedAt")))
            list.put(idx, sanitized)
        } else {
            sanitized.put("installs", 0)
            sanitized.put("createdAt", sanitized.optString("updatedAt"))
            list.put(sanitized)
        }
        persist(root)
        JSONObject(sanitized.toString())
    }

    fun delete(id: String): Boolean = synchronized(lock) {
        val idx = indexOf(id)
        if (idx < 0) return false
        apps().remove(idx)
        persist(root)
        true
    }

    fun setPublished(id: String, published: Boolean): Boolean = synchronized(lock) {
        val idx = indexOf(id)
        if (idx < 0) return false
        apps().getJSONObject(idx).put("published", published)
        persist(root)
        true
    }

    /** "İndir" düğmesi sayaç artırır; her rol çağırabilir. */
    fun recordInstall(id: String): Int = synchronized(lock) {
        val idx = indexOf(id)
        if (idx < 0) return -1
        val app = apps().getJSONObject(idx)
        if (!app.optBoolean("published", true)) return -1
        val next = app.optInt("installs", 0) + 1
        app.put("installs", next)
        persist(root)
        next
    }

    /** Admin istatistik kartları. */
    fun stats(): JSONObject = synchronized(lock) {
        val list = apps()
        var installs = 0L
        var published = 0
        var drafts = 0
        var ratingSum = 0.0
        var ratingCount = 0
        val perCategory = mutableMapOf<String, Int>()
        for (i in 0 until list.length()) {
            val app = list.getJSONObject(i)
            installs += app.optInt("installs", 0)
            if (app.optBoolean("published", true)) published++ else drafts++
            val rating = app.optDouble("rating", 0.0)
            if (rating > 0) {
                ratingSum += rating
                ratingCount++
            }
            val category = app.optString("category", "Diğer")
            perCategory[category] = (perCategory[category] ?: 0) + 1
        }
        JSONObject().apply {
            put("totalApps", list.length())
            put("published", published)
            put("drafts", drafts)
            put("totalInstalls", installs)
            put("averageRating", if (ratingCount > 0) Math.round(ratingSum / ratingCount * 10.0) / 10.0 else 0.0)
            put("perCategory", JSONObject(perCategory as Map<*, *>))
        }
    }

    /** Tohum veriye geri döner. Admin panelinden onaylı olarak çağrılır. */
    fun resetToSeed(): Boolean = synchronized(lock) {
        file.delete()
        loaded.set(false)
        ensureLoaded()
        true
    }

    /**
     * Formdan gelen veriyi şemaya oturtur: bilinmeyen alanlar düşer, metin uzunlukları sınırlanır,
     * sayısal alanlar aralığa çekilir. Böylece köprüden gelen JSON kataloğu bozamaz.
     */
    private fun sanitize(input: JSONObject, id: String): JSONObject {
        val out = JSONObject()
        out.put("id", id)
        out.put("title", input.optString("title").trim().take(60).ifBlank { "İsimsiz Kayıt" })
        out.put("developer", input.optString("developer").trim().take(60).ifBlank { "Bilinmeyen Stüdyo" })
        out.put("category", input.optString("category").trim().take(40).ifBlank { "Diğer" })
        out.put("version", input.optString("version").trim().take(20).ifBlank { "1.0.0" })
        out.put("shortDescription", input.optString("shortDescription").trim().take(120))
        out.put("longDescription", input.optString("longDescription").trim().take(4000))
        out.put("iconSeed", input.optString("iconSeed").trim().take(40).ifBlank { id })
        out.put("palette", input.optString("palette").trim().take(24).ifBlank { "emerald" })
        out.put("sizeMb", input.optDouble("sizeMb", 12.0).coerceIn(0.1, 4096.0))
        out.put("rating", Math.round(input.optDouble("rating", 4.0).coerceIn(0.0, 5.0) * 10.0) / 10.0)
        out.put("ratingCount", input.optInt("ratingCount", 0).coerceIn(0, 100_000_000))
        out.put("published", input.optBoolean("published", true))
        out.put("contentRating", input.optString("contentRating").trim().take(12).ifBlank { "7+" })
        out.put("updatedAt", input.optString("updatedAt").trim().take(24).ifBlank { today() })

        val tags = JSONArray()
        input.optJSONArray("tags")?.let { src ->
            for (i in 0 until minOf(src.length(), MAX_TAGS)) {
                val tag = src.optString(i).trim().take(20)
                if (tag.isNotEmpty()) tags.put(tag)
            }
        }
        out.put("tags", tags)

        val shots = JSONArray()
        input.optJSONArray("screenshots")?.let { src ->
            for (i in 0 until minOf(src.length(), MAX_SCREENSHOTS)) {
                val raw = src.optJSONObject(i) ?: continue
                val shot = JSONObject()
                shot.put("seed", raw.optString("seed").trim().take(40).ifBlank { "$id-$i" })
                shot.put("kind", if (raw.optString("kind") == "tablet") "tablet" else "phone")
                shot.put("scene", raw.optString("scene").trim().take(20).ifBlank { "field" })
                shot.put("caption", raw.optString("caption").trim().take(60))
                // Uzak görsel isteğe bağlı; yalnızca https kabul edilir.
                val url = raw.optString("url").trim()
                if (url.startsWith("https://") && url.length <= MAX_URL) shot.put("url", url)
                shots.put(shot)
            }
        }
        out.put("screenshots", shots)

        val download = input.optString("downloadUrl").trim()
        if ((download.startsWith("https://") || download.startsWith("http://")) && download.length <= MAX_URL) {
            out.put("downloadUrl", download)
        } else {
            out.put("downloadUrl", "")
        }
        return out
    }

    private fun today(): String {
        val cal = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    private fun generateId(title: String): String {
        val slug = title.lowercase()
            .map { if (it in 'a'..'z' || it in '0'..'9') it else '-' }
            .joinToString("")
            .trim('-')
            .take(24)
            .ifBlank { "kayit" }
        var candidate = slug
        var n = 2
        while (indexOf(candidate) >= 0) {
            candidate = "$slug-$n"
            n++
        }
        return candidate
    }

    companion object {
        private const val TAG = "CatalogRepository"
        private const val FILE_NAME = "catalog.json"
        private const val SEED_ASSET = "www/data/catalog.json"
        private const val KEY_APPS = "apps"
        private const val KEY_CATEGORIES = "categories"
        private const val MAX_SCREENSHOTS = 8
        private const val MAX_TAGS = 6
        private const val MAX_URL = 512

        /** Kullanıcı yükünden çıkarılan yönetim alanları. */
        private val ADMIN_ONLY_FIELDS = listOf("published", "createdAt", "internalNote")
    }
}
