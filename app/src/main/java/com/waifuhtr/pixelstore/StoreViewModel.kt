package com.waifuhtr.pixelstore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waifuhtr.pixelstore.data.AppRecord
import com.waifuhtr.pixelstore.data.Catalog
import com.waifuhtr.pixelstore.data.CatalogSource
import com.waifuhtr.pixelstore.data.Category
import com.waifuhtr.pixelstore.data.LocalCatalogSource
import com.waifuhtr.pixelstore.data.PixelStoreException
import com.waifuhtr.pixelstore.data.Session
import com.waifuhtr.pixelstore.data.Settings
import com.waifuhtr.pixelstore.data.SourceKind
import com.waifuhtr.pixelstore.data.Stats
import com.waifuhtr.pixelstore.data.UserAccount
import com.waifuhtr.pixelstore.data.WordPressCatalogSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Uygulama içi ekranlar. Yönetim ekranlarına yalnızca yönetici oturumunda girilebilir. */
sealed interface Screen {
    val adminOnly: Boolean get() = false
    val title: String

    data object Store : Screen {
        override val title = "PIXELSTORE"
    }

    data object Categories : Screen {
        override val title = "TÜRLER"
    }

    data object Profile : Screen {
        override val title = "PROFİL"
    }

    data object Connection : Screen {
        override val title = "BAĞLANTI"
    }

    data class Detail(val id: String) : Screen {
        override val title = "KAYIT"
    }

    data object Admin : Screen {
        override val adminOnly = true
        override val title = "YÖNETİM"
    }

    data object AdminUsers : Screen {
        override val adminOnly = true
        override val title = "KULLANICILAR"
    }

    data class AdminEditor(val id: String?) : Screen {
        override val adminOnly = true
        override val title = if (id == null) "YENİ KAYIT" else "KAYIT DÜZENLE"
    }
}

enum class SortMode(val label: String) {
    FEATURED("Öne çıkan"),
    INSTALLS("İndirme"),
    RATING("Puan"),
    RECENT("Yeni")
}

enum class MessageTone { INFO, SUCCESS, ERROR }

data class UiMessage(val id: Long, val text: String, val tone: MessageTone)

data class UiState(
    val booting: Boolean = true,
    val busy: Boolean = false,
    val session: Session? = null,
    val sourceKind: SourceKind = SourceKind.LOCAL,
    val wordpressUrl: String = "",
    val apps: List<AppRecord> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val category: String? = null,
    val sort: SortMode = SortMode.FEATURED,
    val installed: Set<String> = emptySet(),
    val stats: Stats? = null,
    val users: List<UserAccount> = emptyList(),
    val message: UiMessage? = null,
    val loginError: String? = null,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val backStack: List<Screen> = listOf(Screen.Store)
) {
    val current: Screen get() = backStack.lastOrNull() ?: Screen.Store
    val canGoBack: Boolean get() = backStack.size > 1
    val isAdmin: Boolean get() = session?.isAdmin == true

    /** Arama, tür filtresi ve sıralama uygulanmış liste. */
    val visibleApps: List<AppRecord>
        get() {
            val needle = query.trim().lowercase()
            val filtered = apps.filter { app ->
                (category == null || app.category == category) &&
                    (needle.isEmpty() || listOf(
                        app.title, app.developer, app.category, app.tags.joinToString(" ")
                    ).joinToString(" ").lowercase().contains(needle))
            }
            return when (sort) {
                SortMode.INSTALLS -> filtered.sortedByDescending { it.installs }
                SortMode.RATING -> filtered.sortedByDescending { it.rating }
                SortMode.RECENT -> filtered.sortedByDescending { it.updatedAt }
                SortMode.FEATURED -> filtered.sortedByDescending { it.featureScore }
            }
        }

    val featured: AppRecord?
        get() = apps.filter { it.published }.maxByOrNull { it.featureScore }

    fun app(id: String): AppRecord? = apps.firstOrNull { it.id == id }
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = Settings(application)
    private var source: CatalogSource = buildSource()

    private val _state = MutableStateFlow(
        UiState(
            sourceKind = source.kind,
            wordpressUrl = settings.wordpressUrl,
            installed = settings.installedIds,
            soundEnabled = settings.soundEnabled,
            hapticsEnabled = settings.hapticsEnabled
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        boot()
    }

    private fun buildSource(): CatalogSource =
        if (settings.usesWordPress) {
            WordPressCatalogSource(settings)
        } else {
            LocalCatalogSource(getApplication(), settings)
        }

    private fun boot() {
        viewModelScope.launch {
            val session = runCatching { source.restoreSession() }.getOrNull()
            if (session != null) {
                _state.update { it.copy(session = session) }
                loadCatalog(showBusy = false)
            }
            _state.update { it.copy(booting = false, sourceKind = source.kind) }
        }
    }

    /* ---- Oturum ------------------------------------------------------------------------------ */

    fun login(username: String, password: String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, loginError = null) }
            try {
                val session = source.login(username, password)
                _state.update {
                    it.copy(
                        session = session,
                        backStack = listOf(Screen.Store),
                        busy = false
                    )
                }
                loadCatalog(showBusy = false)
                toast("Hoş geldin, ${session.displayName}", MessageTone.SUCCESS)
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false, loginError = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { source.logout() }
            _state.update {
                it.copy(
                    session = null,
                    apps = emptyList(),
                    categories = emptyList(),
                    stats = null,
                    users = emptyList(),
                    query = "",
                    category = null,
                    backStack = listOf(Screen.Store)
                )
            }
        }
    }

    /* ---- Katalog ----------------------------------------------------------------------------- */

    fun refresh() = loadCatalog(showBusy = true)

    private fun loadCatalog(showBusy: Boolean) {
        viewModelScope.launch {
            if (showBusy) _state.update { it.copy(busy = true) }
            try {
                val catalog: Catalog = source.catalog()
                _state.update {
                    it.copy(
                        apps = catalog.apps,
                        categories = catalog.categories,
                        busy = false
                    )
                }
                if (_state.value.isAdmin) loadAdminData()
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Katalog yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    private suspend fun loadAdminData() {
        val stats = runCatching { source.stats() }.getOrNull()
        val users = runCatching { source.users() }.getOrElse { emptyList() }
        _state.update { it.copy(stats = stats, users = users) }
    }

    fun setQuery(value: String) = _state.update { it.copy(query = value) }

    fun setCategory(value: String?) = _state.update { it.copy(category = value) }

    fun setSort(value: SortMode) = _state.update { it.copy(sort = value) }

    /* ---- İndirme ----------------------------------------------------------------------------- */

    fun install(id: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val installs = source.recordInstall(id)
                settings.addInstalled(id)
                _state.update { current ->
                    current.copy(
                        installed = settings.installedIds,
                        apps = current.apps.map { if (it.id == id) it.copy(installs = installs) else it }
                    )
                }
                toast("${_state.value.app(id)?.title ?: "Kayıt"} yüklendi", MessageTone.SUCCESS)
                onDone()
            } catch (e: PixelStoreException) {
                toast(e.message ?: "İndirme başarısız.", MessageTone.ERROR)
                onDone()
            }
        }
    }

    fun uninstall(id: String) {
        settings.removeInstalled(id)
        _state.update { it.copy(installed = settings.installedIds) }
        toast("Kayıt kaldırıldı", MessageTone.INFO)
    }

    /* ---- Yönetim ----------------------------------------------------------------------------- */

    fun saveApp(record: AppRecord, onSaved: () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val saved = source.saveApp(record)
                _state.update { it.copy(busy = false) }
                toast("\"${saved.title}\" kaydedildi", MessageTone.SUCCESS)
                loadCatalog(showBusy = false)
                onSaved()
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Kaydedilemedi.", MessageTone.ERROR)
            }
        }
    }

    fun deleteApp(id: String) {
        viewModelScope.launch {
            try {
                source.deleteApp(id)
                toast("Kayıt silindi", MessageTone.SUCCESS)
                loadCatalog(showBusy = false)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Silinemedi.", MessageTone.ERROR)
            }
        }
    }

    fun setPublished(id: String, published: Boolean) {
        viewModelScope.launch {
            try {
                source.setPublished(id, published)
                toast(if (published) "Yayına alındı" else "Yayından kaldırıldı", MessageTone.SUCCESS)
                loadCatalog(showBusy = false)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Güncellenemedi.", MessageTone.ERROR)
            }
        }
    }

    fun resetCatalog() {
        viewModelScope.launch {
            try {
                source.resetCatalog()
                toast("Katalog sıfırlandı", MessageTone.SUCCESS)
                loadCatalog(showBusy = false)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Sıfırlanamadı.", MessageTone.ERROR)
            }
        }
    }

    /* ---- Bağlantı ---------------------------------------------------------------------------- */

    /**
     * Veri kaynağını değiştirir. Adres boşsa yerel kipe döner.
     * Kaynak değiştiğinde oturum sıfırlanır: iki kaynağın hesapları farklıdır.
     */
    fun applyWordPressUrl(rawUrl: String) {
        viewModelScope.launch {
            val normalized = Settings.normalizeUrl(rawUrl)
            val changed = normalized != settings.wordpressUrl
            settings.wordpressUrl = normalized
            settings.clearWordPressSession()
            settings.localSessionUser = ""
            source = buildSource()
            _state.update {
                it.copy(
                    session = null,
                    apps = emptyList(),
                    categories = emptyList(),
                    stats = null,
                    users = emptyList(),
                    sourceKind = source.kind,
                    wordpressUrl = normalized,
                    backStack = listOf(Screen.Store)
                )
            }
            if (changed) {
                toast(
                    if (normalized.isBlank()) "Yerel kipe geçildi" else "WordPress kipine geçildi",
                    MessageTone.SUCCESS
                )
            }
        }
    }

    /** Adresi kayıtlı ayarlara dokunmadan dener; mevcut kip bozulmaz. */
    fun testConnection(rawUrl: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val normalized = Settings.normalizeUrl(rawUrl)
            val result = runCatching { WordPressCatalogSource.probe(normalized) }
            onResult(result.getOrElse { it.message ?: "Bağlantı kurulamadı." })
        }
    }

    /* ---- Tercihler --------------------------------------------------------------------------- */

    fun setSound(enabled: Boolean) {
        settings.soundEnabled = enabled
        _state.update { it.copy(soundEnabled = enabled) }
    }

    fun setHaptics(enabled: Boolean) {
        settings.hapticsEnabled = enabled
        _state.update { it.copy(hapticsEnabled = enabled) }
    }

    /* ---- Gezinme ----------------------------------------------------------------------------- */

    fun openTab(screen: Screen) {
        if (screen.adminOnly && !_state.value.isAdmin) {
            toast("Bu bölüm için yönetici yetkisi gerekir.", MessageTone.ERROR)
            return
        }
        _state.update { it.copy(backStack = listOf(screen)) }
    }

    fun push(screen: Screen) {
        if (screen.adminOnly && !_state.value.isAdmin) {
            toast("Bu bölüm için yönetici yetkisi gerekir.", MessageTone.ERROR)
            return
        }
        _state.update { it.copy(backStack = it.backStack + screen) }
    }

    /** Geri gidildiyse true; false ise uygulamadan çıkılır. */
    fun back(): Boolean {
        val current = _state.value
        if (!current.canGoBack) {
            if (current.current != Screen.Store && current.session != null) {
                _state.update { it.copy(backStack = listOf(Screen.Store)) }
                return true
            }
            return false
        }
        _state.update { it.copy(backStack = it.backStack.dropLast(1)) }
        return true
    }

    /* ---- Bildirim ---------------------------------------------------------------------------- */

    fun toast(text: String, tone: MessageTone = MessageTone.INFO) {
        _state.update { it.copy(message = UiMessage(System.nanoTime(), text, tone)) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun clearLoginError() = _state.update { it.copy(loginError = null) }
}
