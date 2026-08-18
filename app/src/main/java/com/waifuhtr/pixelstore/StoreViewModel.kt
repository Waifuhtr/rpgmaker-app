package com.waifuhtr.pixelstore

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waifuhtr.pixelstore.data.DownloadTicket
import com.waifuhtr.pixelstore.data.GameDetail
import com.waifuhtr.pixelstore.data.GameForm
import com.waifuhtr.pixelstore.data.GameSummary
import com.waifuhtr.pixelstore.data.ImagePicker
import com.waifuhtr.pixelstore.data.PixelStoreException
import com.waifuhtr.pixelstore.data.Review
import com.waifuhtr.pixelstore.data.RiasApi
import com.waifuhtr.pixelstore.data.SearchHit
import com.waifuhtr.pixelstore.data.Session
import com.waifuhtr.pixelstore.data.Settings
import com.waifuhtr.pixelstore.data.Stats
import com.waifuhtr.pixelstore.data.Taxonomies
import com.waifuhtr.pixelstore.data.UserAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Uygulama ekranları. Yönetim ekranlarına yalnızca yönetici girebilir. */
sealed interface Screen {
    val adminOnly: Boolean get() = false
    val title: String

    data object Store : Screen {
        override val title = "RIASLINK"
    }

    data object Categories : Screen {
        override val title = "TÜRLER"
    }

    data object Wishlist : Screen {
        override val title = "İSTEK LİSTEM"
    }

    data object Profile : Screen {
        override val title = "PROFİL"
    }

    data class Detail(val id: String) : Screen {
        override val title = "OYUN"
    }

    data class Reviews(val id: String) : Screen {
        override val title = "YORUMLAR"
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
        override val title = if (id == null) "YENİ OYUN" else "OYUNU DÜZENLE"
    }
}

enum class SortMode(val wire: String, val label: String) {
    NEWEST("newest", "En yeni"),
    DOWNLOADS("downloads", "İndirme"),
    RATING("rating", "Puan"),
    TITLE("title", "A-Z")
}

enum class MessageTone { INFO, SUCCESS, ERROR }

data class UiMessage(val id: Long, val text: String, val tone: MessageTone)

/** Filtre seçimi: taksonomi terim slug'ları. */
data class Filters(
    val genre: String? = null,
    val platform: String? = null,
    val language: String? = null,
    val status: String? = null
) {
    val active: Int get() = listOfNotNull(genre, platform, language, status).size
}

data class UiState(
    val booting: Boolean = true,
    val busy: Boolean = false,
    val loadingMore: Boolean = false,
    val session: Session? = null,
    val games: List<GameSummary> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalGames: Int = 0,
    val taxonomies: Taxonomies = Taxonomies(),
    val query: String = "",
    val filters: Filters = Filters(),
    val sort: SortMode = SortMode.NEWEST,
    val searchHits: List<SearchHit> = emptyList(),
    val searching: Boolean = false,
    val detail: GameDetail? = null,
    val detailLoading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val reviewsLoading: Boolean = false,
    val hasReviewed: Boolean = false,
    val wishlist: List<GameSummary> = emptyList(),
    val wishlistLoading: Boolean = false,
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
    val hasMorePages: Boolean get() = page < totalPages
    val featured: GameSummary? get() = games.firstOrNull { it.featured && it.published } ?: games.firstOrNull()
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = Settings(application)
    private val api = RiasApi(settings)

    private val _state = MutableStateFlow(
        UiState(
            soundEnabled = settings.soundEnabled,
            hapticsEnabled = settings.hapticsEnabled
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Canlı arama işi: her tuş vuruşunda öncekini iptal eder. */
    private var searchJob: Job? = null
    private var listJob: Job? = null

    init {
        viewModelScope.launch {
            val session = runCatching { api.restoreSession() }.getOrNull()
            if (session != null) {
                _state.update { it.copy(session = session) }
                loadTaxonomies()
                reloadGames()
            }
            _state.update { it.copy(booting = false) }
        }
    }

    /* ---- Oturum ------------------------------------------------------------------------------ */

    fun login(username: String, password: String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, loginError = null) }
            try {
                val session = api.login(username, password)
                _state.update { it.copy(session = session, busy = false, backStack = listOf(Screen.Store)) }
                loadTaxonomies()
                reloadGames()
                toast("Hoş geldin, ${session.displayName}", MessageTone.SUCCESS)
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false, loginError = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { api.logout() }
            _state.update {
                UiState(
                    booting = false,
                    soundEnabled = it.soundEnabled,
                    hapticsEnabled = it.hapticsEnabled
                )
            }
        }
    }

    fun testConnection(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { api.health() }
            onResult(result.getOrElse { it.message ?: "Bağlantı kurulamadı." })
        }
    }

    /* ---- Katalog ----------------------------------------------------------------------------- */

    private fun loadTaxonomies() {
        viewModelScope.launch {
            runCatching { api.taxonomies() }
                .onSuccess { taxonomies -> _state.update { it.copy(taxonomies = taxonomies) } }
        }
    }

    /** Listeyi baştan yükler. Filtre/sıralama/arama değişiminde çağrılır. */
    fun reloadGames() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val current = _state.value
            try {
                val page = api.games(
                    page = 1,
                    search = current.query.takeIf { it.isNotBlank() },
                    genre = current.filters.genre,
                    platform = current.filters.platform,
                    language = current.filters.language,
                    status = current.filters.status,
                    sort = current.sort.wire
                )
                _state.update {
                    it.copy(
                        games = page.games,
                        page = page.page,
                        totalPages = page.pages,
                        totalGames = page.total,
                        busy = false
                    )
                }
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Oyunlar yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    /** Sonsuz kaydırma: liste sonuna gelindiğinde sonraki sayfayı ekler. */
    fun loadMore() {
        val current = _state.value
        if (current.loadingMore || current.busy || !current.hasMorePages) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            try {
                val next = api.games(
                    page = current.page + 1,
                    search = current.query.takeIf { it.isNotBlank() },
                    genre = current.filters.genre,
                    platform = current.filters.platform,
                    language = current.filters.language,
                    status = current.filters.status,
                    sort = current.sort.wire
                )
                _state.update { state ->
                    // Aynı kaydın iki kez eklenmesini engelle (sunucuda sıra değişmiş olabilir).
                    val existing = state.games.map { it.id }.toSet()
                    state.copy(
                        games = state.games + next.games.filter { it.id !in existing },
                        page = next.page,
                        totalPages = next.pages,
                        loadingMore = false
                    )
                }
            } catch (e: PixelStoreException) {
                _state.update { it.copy(loadingMore = false) }
                toast(e.message ?: "Sonraki sayfa yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
        scheduleLiveSearch(value)
    }

    /**
     * Canlı arama. Her tuş vuruşunda istek atmamak için 320 ms beklenir; kullanıcı yazmaya devam
     * ederse önceki iş iptal edilir.
     */
    private fun scheduleLiveSearch(value: String) {
        searchJob?.cancel()
        val needle = value.trim()
        if (needle.length < 2) {
            _state.update { it.copy(searchHits = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(320)
            _state.update { it.copy(searching = true) }
            try {
                val hits = api.search(needle)
                _state.update { it.copy(searchHits = hits, searching = false) }
            } catch (e: PixelStoreException) {
                _state.update { it.copy(searching = false) }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", searchHits = emptyList(), searching = false) }
        reloadGames()
    }

    /** Arama kutusundan tam listeye geçiş (klavyedeki ara tuşu). */
    fun submitSearch() {
        _state.update { it.copy(searchHits = emptyList()) }
        reloadGames()
    }

    fun setFilter(update: (Filters) -> Filters) {
        _state.update { it.copy(filters = update(it.filters)) }
        reloadGames()
    }

    fun clearFilters() {
        _state.update { it.copy(filters = Filters()) }
        reloadGames()
    }

    fun setSort(value: SortMode) {
        if (_state.value.sort == value) return
        _state.update { it.copy(sort = value) }
        reloadGames()
    }

    /* ---- Detay ------------------------------------------------------------------------------- */

    fun openGame(id: String) {
        push(Screen.Detail(id))
        loadDetail(id)
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(detailLoading = true, detail = null) }
            try {
                val detail = api.game(id)
                _state.update { it.copy(detail = detail, detailLoading = false) }
                api.recordView(id)
            } catch (e: PixelStoreException) {
                _state.update { it.copy(detailLoading = false) }
                toast(e.message ?: "Oyun açılamadı.", MessageTone.ERROR)
            }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            try {
                val (favorited, count) = api.toggleFavorite(id)
                _state.update { state ->
                    state.copy(
                        games = state.games.map { if (it.id == id) it.copy(favorited = favorited) else it },
                        detail = state.detail?.let { detail ->
                            if (detail.id == id) {
                                detail.copy(summary = detail.summary.copy(favorited = favorited))
                            } else detail
                        },
                        session = state.session?.copy(favoriteCount = count),
                        // Listeden çıkarıldıysa istek listesi ekranı da güncellensin.
                        wishlist = if (favorited) state.wishlist else state.wishlist.filterNot { it.id == id }
                    )
                }
                toast(
                    if (favorited) "İstek listene eklendi" else "İstek listenden çıkarıldı",
                    MessageTone.SUCCESS
                )
            } catch (e: PixelStoreException) {
                toast(e.message ?: "İşlem başarısız.", MessageTone.ERROR)
            }
        }
    }

    fun rate(id: String, rating: Int) {
        viewModelScope.launch {
            try {
                val (average, count, mine) = api.rate(id, rating)
                _state.update { state ->
                    state.copy(
                        games = state.games.map {
                            if (it.id == id) it.copy(rating = average, ratingCount = count) else it
                        },
                        detail = state.detail?.let { detail ->
                            if (detail.id == id) {
                                detail.copy(
                                    summary = detail.summary.copy(rating = average, ratingCount = count),
                                    userRating = mine
                                )
                            } else detail
                        }
                    )
                }
                toast("Puanın kaydedildi", MessageTone.SUCCESS)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Puan kaydedilemedi.", MessageTone.ERROR)
            }
        }
    }

    /** İndirme: sayacı artırır ve açılacak adresi döner. Tarayıcıyı arayüz katmanı açar. */
    fun requestDownload(id: String, mirror: Boolean, onReady: (DownloadTicket) -> Unit) {
        viewModelScope.launch {
            try {
                val ticket = api.download(id, mirror)
                _state.update { state ->
                    state.copy(
                        games = state.games.map {
                            if (it.id == id) it.copy(downloadCount = ticket.downloadCount) else it
                        },
                        detail = state.detail?.let { detail ->
                            if (detail.id == id) {
                                detail.copy(summary = detail.summary.copy(downloadCount = ticket.downloadCount))
                            } else detail
                        }
                    )
                }
                onReady(ticket)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "İndirme bağlantısı alınamadı.", MessageTone.ERROR)
            }
        }
    }

    fun report(id: String, message: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.report(id, message)
                toast("Raporun yönetime iletildi", MessageTone.SUCCESS)
                onDone()
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Rapor gönderilemedi.", MessageTone.ERROR)
            }
        }
    }

    /* ---- Yorumlar ---------------------------------------------------------------------------- */

    fun openReviews(id: String) {
        push(Screen.Reviews(id))
        loadReviews(id)
    }

    fun loadReviews(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(reviewsLoading = true) }
            try {
                val (reviews, hasReviewed) = api.reviews(id)
                _state.update { it.copy(reviews = reviews, hasReviewed = hasReviewed, reviewsLoading = false) }
            } catch (e: PixelStoreException) {
                _state.update { it.copy(reviewsLoading = false) }
                toast(e.message ?: "Yorumlar yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    fun addReview(id: String, content: String, recommended: Boolean, onDone: () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val reviews = api.addReview(id, content, recommended)
                _state.update { it.copy(reviews = reviews, hasReviewed = true, busy = false) }
                toast("Yorumun yayınlandı", MessageTone.SUCCESS)
                onDone()
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Yorum gönderilemedi.", MessageTone.ERROR)
            }
        }
    }

    fun voteReview(reviewId: Int, up: Boolean) {
        viewModelScope.launch {
            try {
                api.voteReview(reviewId, up)
                _state.update { state ->
                    state.copy(
                        reviews = state.reviews.map { review ->
                            if (review.id != reviewId) review
                            else review.copy(
                                upvotes = review.upvotes + if (up) 1 else 0,
                                downvotes = review.downvotes + if (up) 0 else 1,
                                voted = true
                            )
                        }
                    )
                }
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Oy verilemedi.", MessageTone.ERROR)
            }
        }
    }

    fun deleteReview(reviewId: Int, gameId: String) {
        viewModelScope.launch {
            try {
                api.deleteReview(reviewId)
                _state.update {
                    it.copy(reviews = it.reviews.filterNot { review -> review.id == reviewId }, hasReviewed = false)
                }
                toast("Yorumun silindi", MessageTone.SUCCESS)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Yorum silinemedi.", MessageTone.ERROR)
            }
        }
    }

    /* ---- İstek listesi ----------------------------------------------------------------------- */

    fun loadWishlist() {
        viewModelScope.launch {
            _state.update { it.copy(wishlistLoading = true) }
            try {
                val games = api.favorites()
                _state.update { it.copy(wishlist = games, wishlistLoading = false) }
            } catch (e: PixelStoreException) {
                _state.update { it.copy(wishlistLoading = false) }
                toast(e.message ?: "İstek listesi yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    /* ---- Profil fotoğrafı -------------------------------------------------------------------- */

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val image = ImagePicker.prepareAvatar(getApplication(), uri)
                val session = api.uploadAvatar(image.fileName, image.contentType, image.bytes)
                _state.update { it.copy(session = session, busy = false) }
                toast("Profil fotoğrafın güncellendi", MessageTone.SUCCESS)
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Fotoğraf yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            try {
                val session = api.clearAvatar()
                _state.update { it.copy(session = session) }
                toast("Profil fotoğrafı kaldırıldı", MessageTone.INFO)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "İşlem başarısız.", MessageTone.ERROR)
            }
        }
    }

    /* ---- Yönetim ----------------------------------------------------------------------------- */

    fun loadAdminData() {
        viewModelScope.launch {
            runCatching { api.stats() }.onSuccess { stats -> _state.update { it.copy(stats = stats) } }
            runCatching { api.users() }.onSuccess { users -> _state.update { it.copy(users = users) } }
        }
    }

    fun saveGame(form: GameForm, existingId: String?, onSaved: (GameDetail) -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val saved = api.saveGame(form, existingId)
                _state.update { it.copy(busy = false, detail = saved) }
                toast("\"${saved.title}\" kaydedildi", MessageTone.SUCCESS)
                reloadGames()
                loadAdminData()
                onSaved(saved)
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Kaydedilemedi.", MessageTone.ERROR)
            }
        }
    }

    fun deleteGame(id: String) {
        viewModelScope.launch {
            try {
                api.deleteGame(id)
                toast("Oyun çöp kutusuna taşındı", MessageTone.SUCCESS)
                reloadGames()
                loadAdminData()
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Silinemedi.", MessageTone.ERROR)
            }
        }
    }

    fun setPublished(id: String, published: Boolean) {
        viewModelScope.launch {
            try {
                api.setPublished(id, published)
                _state.update { state ->
                    state.copy(games = state.games.map { if (it.id == id) it.copy(published = published) else it })
                }
                toast(if (published) "Yayına alındı" else "Yayından kaldırıldı", MessageTone.SUCCESS)
                loadAdminData()
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Güncellenemedi.", MessageTone.ERROR)
            }
        }
    }

    fun uploadCover(id: String, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val image = ImagePicker.prepareCover(getApplication(), uri)
                val coverUrl = api.uploadCover(id, image.fileName, image.contentType, image.bytes)
                _state.update { state ->
                    state.copy(
                        busy = false,
                        games = state.games.map { if (it.id == id) it.copy(coverUrl = coverUrl) else it },
                        detail = state.detail?.let { detail ->
                            if (detail.id == id) detail.copy(summary = detail.summary.copy(coverUrl = coverUrl))
                            else detail
                        }
                    )
                }
                toast("Kapak görseli yüklendi", MessageTone.SUCCESS)
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Kapak yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    fun uploadScreenshot(id: String, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val image = ImagePicker.prepareScreenshot(getApplication(), uri)
                val detail = api.uploadScreenshot(id, image.fileName, image.contentType, image.bytes)
                _state.update { it.copy(busy = false, detail = detail) }
                toast("Ekran görüntüsü eklendi", MessageTone.SUCCESS)
            } catch (e: PixelStoreException) {
                _state.update { it.copy(busy = false) }
                toast(e.message ?: "Ekran görüntüsü yüklenemedi.", MessageTone.ERROR)
            }
        }
    }

    fun deleteScreenshot(id: String, attachmentId: Int) {
        viewModelScope.launch {
            try {
                val detail = api.deleteScreenshot(id, attachmentId)
                _state.update { it.copy(detail = detail) }
                toast("Ekran görüntüsü kaldırıldı", MessageTone.INFO)
            } catch (e: PixelStoreException) {
                toast(e.message ?: "Kaldırılamadı.", MessageTone.ERROR)
            }
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
        when (screen) {
            Screen.Wishlist -> loadWishlist()
            Screen.Admin -> loadAdminData()
            else -> Unit
        }
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
