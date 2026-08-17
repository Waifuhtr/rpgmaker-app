package com.waifuhtr.pixelstore.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Calendar

/**
 * Cihaz içi kaynak — WordPress adresi girilmediğinde kullanılır.
 *
 * Katalog `assets/catalog_seed.json` içinden tohumlanır ve uygulamanın özel dizinine kopyalanır;
 * sonraki tüm yazmalar bu kopyada olur. Yazma atomiktir (`.tmp` + rename), böylece yarım yazılmış
 * dosya kataloğu bozamaz.
 *
 * Parolalar kaynak kodda düz metin tutulmaz; sabit tuzlu SHA-256 özeti saklanır ve karşılaştırma
 * sabit zamanlıdır.
 */
class LocalCatalogSource(
    context: Context,
    private val settings: Settings
) : CatalogSource {

    override val kind = SourceKind.LOCAL

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)
    private val mutex = Mutex()

    @Volatile
    private var session: Session? = null

    @Volatile
    private var root: JSONObject? = null

    override fun currentSession(): Session? = session

    override suspend fun restoreSession(): Session? {
        val saved = settings.localSessionUser
        // Rol prefs'ten değil hesap tablosundan okunur: prefs kurcalansa bile yetki yükseltilemez.
        session = ACCOUNTS.firstOrNull { it.username == saved }?.toSession()
        return session
    }

    override suspend fun login(username: String, password: String): Session {
        val clean = username.trim().lowercase()
        if (clean.isEmpty() || clean.length > MAX_FIELD || password.length > MAX_FIELD) {
            throw PixelStoreException("Kullanıcı adı veya parola hatalı.")
        }
        val account = ACCOUNTS.firstOrNull { it.username == clean }
            ?: throw PixelStoreException("Kullanıcı adı veya parola hatalı.")
        if (!constantTimeEquals(account.passwordHash, hash(clean, password))) {
            throw PixelStoreException("Kullanıcı adı veya parola hatalı.")
        }
        val newSession = account.toSession()
        session = newSession
        settings.localSessionUser = account.username
        Log.i(TAG, "Yerel oturum açıldı: rol=${newSession.role.wire}")
        return newSession
    }

    override suspend fun logout() {
        session = null
        settings.localSessionUser = ""
    }

    override suspend fun catalog(): Catalog = mutex.withLock {
        val active = requireSession()
        val data = load()
        val apps = mutableListOf<AppRecord>()
        val arr = data.optJSONArray(KEY_APPS) ?: JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val record = Json.appFrom(obj)
            // Kullanıcı rolünde taslak kayıtlar listeye hiç girmez.
            if (!record.published && !active.isAdmin) continue
            apps += record
        }
        val categories = mutableListOf<Category>()
        data.optJSONArray(KEY_CATEGORIES)?.let { cats ->
            for (i in 0 until cats.length()) {
                cats.optJSONObject(i)?.let { categories += Json.categoryFrom(it) }
            }
        }
        Catalog(apps, categories)
    }

    override suspend fun app(id: String): AppRecord = mutex.withLock {
        val active = requireSession()
        val obj = findApp(load(), id) ?: throw PixelStoreException("Kayıt bulunamadı.")
        val record = Json.appFrom(obj)
        if (!record.published && !active.isAdmin) throw PixelStoreException("Kayıt bulunamadı.")
        record
    }

    override suspend fun recordInstall(id: String): Int = mutex.withLock {
        requireSession()
        val data = load()
        val obj = findApp(data, id) ?: throw PixelStoreException("Kayıt bulunamadı.")
        if (!obj.optBoolean("published", true)) throw PixelStoreException("Kayıt bulunamadı.")
        val next = obj.optInt("installs", 0) + 1
        obj.put("installs", next)
        persist(data)
        next
    }

    override suspend fun saveApp(record: AppRecord): AppRecord = mutex.withLock {
        requireAdmin()
        val data = load()
        val arr = data.optJSONArray(KEY_APPS) ?: JSONArray().also { data.put(KEY_APPS, it) }
        val id = record.id.ifBlank { generateId(arr, record.title) }
        val incoming = Json.appTo(record.copy(id = id, updatedAt = today()))
        val index = indexOf(arr, id)
        if (index >= 0) {
            val existing = arr.getJSONObject(index)
            // Türetilmiş alanlar formdan gelen veriyle ezilmemeli.
            incoming.put("installs", existing.optInt("installs", 0))
            incoming.put("createdAt", existing.optString("createdAt", today()))
            arr.put(index, incoming)
        } else {
            incoming.put("installs", 0)
            incoming.put("createdAt", today())
            arr.put(incoming)
        }
        persist(data)
        Json.appFrom(incoming)
    }

    override suspend fun deleteApp(id: String) = mutex.withLock {
        requireAdmin()
        val data = load()
        val arr = data.optJSONArray(KEY_APPS) ?: throw PixelStoreException("Kayıt bulunamadı.")
        val index = indexOf(arr, id)
        if (index < 0) throw PixelStoreException("Kayıt bulunamadı.")
        arr.remove(index)
        persist(data)
    }

    override suspend fun setPublished(id: String, published: Boolean) = mutex.withLock {
        requireAdmin()
        val data = load()
        val obj = findApp(data, id) ?: throw PixelStoreException("Kayıt bulunamadı.")
        obj.put("published", published)
        persist(data)
    }

    override suspend fun users(): List<UserAccount> {
        requireAdmin()
        return ACCOUNTS.map {
            UserAccount(it.username, it.displayName, it.role, it.avatarSeed, it.joinedAt, it.note)
        }
    }

    override suspend fun stats(): Stats = mutex.withLock {
        requireAdmin()
        val arr = load().optJSONArray(KEY_APPS) ?: JSONArray()
        var installs = 0L
        var published = 0
        var drafts = 0
        var ratingSum = 0.0
        var ratingCount = 0
        val per = linkedMapOf<String, Int>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            installs += obj.optInt("installs", 0)
            if (obj.optBoolean("published", true)) published++ else drafts++
            val rating = obj.optDouble("rating", 0.0)
            if (rating > 0) {
                ratingSum += rating
                ratingCount++
            }
            val category = obj.optString("category").ifBlank { "Diğer" }
            per[category] = (per[category] ?: 0) + 1
        }
        Stats(
            totalApps = arr.length(),
            published = published,
            drafts = drafts,
            totalInstalls = installs,
            averageRating = if (ratingCount > 0) Math.round(ratingSum / ratingCount * 10.0) / 10.0 else 0.0,
            perCategory = per.toList().sortedByDescending { it.second }
        )
    }

    override suspend fun resetCatalog() = mutex.withLock {
        requireAdmin()
        withContext(Dispatchers.IO) { file.delete() }
        root = null
        load()
        Unit
    }

    /* ---- İç yardımcılar ---------------------------------------------------------------------- */

    private fun requireSession(): Session =
        session ?: throw PixelStoreException("Oturum açılmamış.")

    private fun requireAdmin(): Session {
        val active = requireSession()
        if (!active.isAdmin) throw PixelStoreException("Bu işlem için yönetici yetkisi gerekir.")
        return active
    }

    private suspend fun load(): JSONObject {
        root?.let { return it }
        return withContext(Dispatchers.IO) {
            val loaded = readFile() ?: readSeed()
            root = loaded
            loaded
        }
    }

    private fun readFile(): JSONObject? {
        if (!file.exists()) return null
        return runCatching { JSONObject(file.readText(Charsets.UTF_8)) }
            .onFailure { Log.w(TAG, "Kayıtlı katalog okunamadı, tohum veriye dönülüyor.") }
            .getOrNull()
    }

    private fun readSeed(): JSONObject {
        val text = appContext.assets.open(SEED_ASSET)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val seed = JSONObject(text)
        writeAtomic(seed)
        return seed
    }

    private suspend fun persist(value: JSONObject) = withContext(Dispatchers.IO) {
        writeAtomic(value)
    }

    private fun writeAtomic(value: JSONObject) {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(value.toString(), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.delete()
            if (!tmp.renameTo(file)) {
                throw PixelStoreException("Katalog kaydedilemedi.")
            }
        }
    }

    private fun findApp(data: JSONObject, id: String): JSONObject? {
        val arr = data.optJSONArray(KEY_APPS) ?: return null
        val index = indexOf(arr, id)
        return if (index >= 0) arr.getJSONObject(index) else null
    }

    private fun indexOf(arr: JSONArray, id: String): Int {
        for (i in 0 until arr.length()) {
            if (arr.optJSONObject(i)?.optString("id") == id) return i
        }
        return -1
    }

    private fun generateId(arr: JSONArray, title: String): String {
        val slug = title.lowercase()
            .map { if (it in 'a'..'z' || it in '0'..'9') it else '-' }
            .joinToString("")
            .trim('-')
            .take(24)
            .ifBlank { "kayit" }
        var candidate = slug
        var n = 2
        while (indexOf(arr, candidate) >= 0) {
            candidate = "$slug-$n"
            n++
        }
        return candidate
    }

    private fun today(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun hash(username: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$SALT|$username|$password".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private data class LocalAccount(
        val username: String,
        val displayName: String,
        val role: Role,
        val passwordHash: String,
        val avatarSeed: String,
        val joinedAt: String,
        val note: String
    ) {
        fun toSession() = Session(username, displayName, role, avatarSeed)
    }

    companion object {
        private const val TAG = "LocalCatalogSource"
        private const val FILE_NAME = "catalog.json"
        private const val SEED_ASSET = "catalog_seed.json"
        private const val KEY_APPS = "apps"
        private const val KEY_CATEGORIES = "categories"
        private const val SALT = "pixelstore.v1"
        private const val MAX_FIELD = 64

        /** Demo hesaplar: admin/admin123 ve user/user123 */
        private val ACCOUNTS = listOf(
            LocalAccount(
                username = "admin",
                displayName = "Yönetici Yamato",
                role = Role.ADMIN,
                passwordHash = "bcaac04b31d7ace3394cb7fb59f44d007ee543e55560979e11e0cfc8adcd2314",
                avatarSeed = "warden-01",
                joinedAt = "2024-03-11",
                note = "Katalog yöneticisi"
            ),
            LocalAccount(
                username = "user",
                displayName = "Gezgin Kaya",
                role = Role.USER,
                passwordHash = "6adab157f57a5f88d11676a7eada1b876ccaa2b8e5876fc0230fe45532c076a5",
                avatarSeed = "wanderer-07",
                joinedAt = "2025-01-08",
                note = "Standart oyuncu"
            )
        )
    }
}
