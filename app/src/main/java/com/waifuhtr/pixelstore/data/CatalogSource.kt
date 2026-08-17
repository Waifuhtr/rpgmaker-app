package com.waifuhtr.pixelstore.data

/** Veri kaynağının kimliği; arayüzde "YEREL" / "WORDPRESS" göstergesi olarak kullanılır. */
enum class SourceKind(val label: String) {
    LOCAL("YEREL"),
    WORDPRESS("WORDPRESS")
}

/**
 * Katalog ve oturum kaynağı.
 *
 * İki uygulaması var: cihaz içi [LocalCatalogSource] ve [WordPressCatalogSource].
 * Yetki kararı her iki uygulamada da kaynağın kendisinde verilir — arayüz asla "ben adminim"
 * diyerek veri alamaz. WordPress kipinde karar sunucudaki yetenek (capability) kontrolüdür.
 */
interface CatalogSource {

    val kind: SourceKind

    /** Uygulama açılışında sürdürülebilen oturum; yoksa null. */
    suspend fun restoreSession(): Session?

    fun currentSession(): Session?

    suspend fun login(username: String, password: String): Session

    suspend fun logout()

    suspend fun catalog(): Catalog

    suspend fun app(id: String): AppRecord

    /** İndirme sayacını artırır, yeni değeri döner. */
    suspend fun recordInstall(id: String): Int

    // --- Yalnızca yönetici ---------------------------------------------------------------------

    suspend fun saveApp(record: AppRecord): AppRecord

    suspend fun deleteApp(id: String)

    suspend fun setPublished(id: String, published: Boolean)

    suspend fun users(): List<UserAccount>

    suspend fun stats(): Stats

    suspend fun resetCatalog()
}
