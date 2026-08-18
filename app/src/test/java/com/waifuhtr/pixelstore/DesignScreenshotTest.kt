package com.waifuhtr.pixelstore

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waifuhtr.pixelstore.data.Badge
import com.waifuhtr.pixelstore.data.GameDetail
import com.waifuhtr.pixelstore.data.GameSummary
import com.waifuhtr.pixelstore.data.Requirements
import com.waifuhtr.pixelstore.data.Review
import com.waifuhtr.pixelstore.data.Role
import com.waifuhtr.pixelstore.data.Screenshot
import com.waifuhtr.pixelstore.data.Session
import com.waifuhtr.pixelstore.data.SpecSet
import com.waifuhtr.pixelstore.data.Stats
import com.waifuhtr.pixelstore.data.Taxonomies
import com.waifuhtr.pixelstore.data.Term
import com.waifuhtr.pixelstore.data.UserAccount
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.NoopFeedback
import com.waifuhtr.pixelstore.ui.screens.PixelStoreApp
import com.waifuhtr.pixelstore.ui.theme.PixelStoreTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Tasarım doğrulama testi.
 *
 * Emülatör olmadan arayüzün gerçekte nasıl göründüğünü görebilmek için ekranlar Robolectric'in
 * yerel grafik kipinde çizilip PNG'ye alınır. Ağ yok, bu yüzden gerçek kapaklar yerine yer tutucu
 * piksel ikon görünür — kullanıcının görsel yüklenene kadar gördüğü hâlin aynısı.
 *
 * Çalıştırma: ./gradlew :app:testDebugUnitTest --tests '*DesignScreenshotTest'
 * Çıktı:      app/build/screenshots klasörüne PNG olarak
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "tr-rTR-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val outDir: File by lazy {
        File(System.getProperty("pixelstore.screenshotDir") ?: "build/screenshots").apply { mkdirs() }
    }

    private val application: Application get() = ApplicationProvider.getApplicationContext()

    /* ---- Kurgu veri -------------------------------------------------------------------------- */

    private val adminSession = Session(
        username = "rias",
        displayName = "Rias",
        role = Role.ADMIN,
        avatarUrl = "",
        joinedAt = "2024-03-11",
        favoriteCount = 12,
        reviewCount = 4,
        badges = listOf(Badge("Usta Oyuncu", "#f59e0b"), Badge("Oyun Meraklısı", "#8b5cf6"))
    )

    private val userSession = adminSession.copy(
        username = "gezgin",
        displayName = "Gezgin Kaya",
        role = Role.USER,
        badges = listOf(Badge("Çaylak", "#3b82f6")),
        favoriteCount = 3,
        reviewCount = 1
    )

    private fun summary(
        id: String,
        title: String,
        developer: String,
        platform: String,
        size: String,
        rating: Double,
        downloads: Int,
        published: Boolean = true,
        featured: Boolean = false,
        excerpt: String = ""
    ) = GameSummary(
        id = id,
        postId = id.hashCode(),
        title = title,
        subtitle = "",
        excerpt = excerpt,
        coverUrl = "",
        developer = developer,
        publisher = "Riaslink",
        platform = platform,
        language = "Türkçe",
        status = if (published) "Tamamlandı" else "Beta",
        genres = listOf("RPG"),
        version = "v1.3",
        sizeLabel = size,
        rating = rating,
        ratingCount = (rating * 40).toInt(),
        downloadCount = downloads,
        viewCount = downloads * 8,
        featured = featured,
        editorsChoice = featured,
        published = published,
        updatedAt = "2026-07-02",
        favorited = id == "golge-vadisi"
    )

    private val games = listOf(
        summary(
            "golge-vadisi", "Gölge Vadisi", "Kuzey Fener Stüdyo", "Android", "2.4 GB",
            4.6, 41200, featured = true,
            excerpt = "Karanlık bir vadide geçen, tamamı Türkçeye çevrilmiş uzun soluklu bir hikâye."
        ),
        summary(
            "kirik-fener", "Kırık Fener", "Demirhane Kolektif", "PC", "780 MB",
            4.2, 18900,
            excerpt = "Zamanlama üzerine kurulu, tek başparmakla oynanan hızlı bir aksiyon."
        ),
        summary(
            "kristal-sarmal", "Kristal Sarmal", "Yedi Kule", "Android", "42 MB",
            4.8, 25940,
            excerpt = "120 el yapımı bulmaca; süre yok, can yok, reklam yok."
        ),
        summary(
            "gizli-proje", "Gizli Proje", "Kuzey Fener Stüdyo", "PC", "88 MB",
            0.0, 0, published = false,
            excerpt = "Yayın öncesi taslak kayıt — yalnızca yöneticide görünür."
        )
    )

    private val detail = GameDetail(
        summary = games[0],
        description = "Gölge Vadisi, klasik sıra tabanlı savaş sistemini modern bir tempoyla " +
            "birleştiren uzun soluklu bir piksel RPG'dir.\n\nYedi bölgeye yayılan haritada 40 " +
            "saatlik ana hikâye, 60'tan fazla yan görev ve dört ayrı final sizi bekliyor.",
        changelog = "v1.3 — Çeviri düzeltmeleri ve kayıt hatası giderildi.\nv1.2 — Yeni bölge eklendi.",
        installGuide = "APK'yı kur, arşivi OBB klasörüne çıkar, oyunu başlat.",
        heroUrl = "",
        screenshots = listOf(
            Screenshot(1, "", ""),
            Screenshot(2, "", ""),
            Screenshot(3, "", "")
        ),
        trailerUrl = "https://youtu.be/ornek",
        downloadUrl = "https://riaslink.fun/dl/golge.apk",
        mirrorUrl = "https://mirror.example/golge.apk",
        archivePassword = "riaslink",
        releaseDate = "2026-05-01",
        ageRating = "+16",
        licenseType = "Ücretsiz",
        multiplayer = false,
        controller = true,
        features = listOf("Tam Türkçe", "Çevrimdışı"),
        platforms = listOf("Android", "PC"),
        languages = listOf("Türkçe", "İngilizce"),
        tags = listOf("gerilim", "hikâye"),
        permalink = "https://riaslink.fun/golge-vadisi/",
        requirements = Requirements(
            minimum = SpecSet("Android 8", "Snapdragon 660", "3 GB", "Adreno 512", "4 GB"),
            recommended = SpecSet("Android 12", "Snapdragon 8 Gen 1", "8 GB", "Adreno 730", "6 GB"),
            hasMinimum = true,
            hasRecommended = true
        ),
        userRating = 4,
        reviewCount = 2,
        hasReviewed = false
    )

    private val reviews = listOf(
        Review(
            id = 1, author = "Gezgin Kaya", authorId = 2, avatarUrl = "",
            content = "Çeviri gerçekten iyi olmuş, akıcı oynanıyor. Yan görevler de boş değil.",
            recommended = true, playtime = "12 saat", date = "2026-06-14",
            upvotes = 7, downvotes = 1, voted = false, mine = false,
            badges = listOf(Badge("Usta Oyuncu", "#f59e0b"))
        ),
        Review(
            id = 2, author = "Rias", authorId = 1, avatarUrl = "",
            content = "Son bölümde bir kayıt hatası var, onun dışında sorunsuz.",
            recommended = false, playtime = "3 saat", date = "2026-06-02",
            upvotes = 2, downvotes = 0, voted = true, mine = true,
            badges = emptyList()
        )
    )

    private val taxonomies = Taxonomies(
        genres = listOf(
            Term("rpg", "RPG", 24, "", ""),
            Term("aksiyon", "Aksiyon", 18, "", ""),
            Term("bulmaca", "Bulmaca", 9, "", ""),
            Term("macera", "Macera", 6, "", "")
        ),
        platforms = listOf(
            Term("android", "Android", 31, "🤖", ""),
            Term("pc", "PC", 22, "🖥", "")
        ),
        languages = listOf(Term("turkce", "Türkçe", 40, "", ""), Term("ingilizce", "İngilizce", 12, "", "")),
        statuses = listOf(Term("tamamlandi", "Tamamlandı", 28, "", ""), Term("beta", "Beta", 4, "", ""))
    )

    private val stats = Stats(
        totalGames = 52, published = 48, drafts = 4,
        totalDownloads = 184300, totalViews = 962400, averageRating = 4.5,
        reviewCount = 214, reportCount = 3, userCount = 1870,
        perGenre = listOf("RPG" to 24, "Aksiyon" to 18, "Bulmaca" to 9, "Macera" to 6)
    )

    private val users = listOf(
        UserAccount("rias", "Rias", Role.ADMIN, "administrator", "", "2024-03-11", 12, adminSession.badges),
        UserAccount("gezgin", "Gezgin Kaya", Role.USER, "subscriber", "", "2025-01-08", 3, userSession.badges)
    )

    private fun state(
        session: Session?,
        screen: Screen = Screen.Store,
        includeDrafts: Boolean = false
    ) = UiState(
        booting = false,
        session = session,
        games = if (includeDrafts) games else games.filter { it.published },
        totalGames = if (includeDrafts) 52 else 48,
        totalPages = 3,
        taxonomies = taxonomies,
        detail = detail,
        reviews = reviews,
        wishlist = games.take(2),
        stats = stats,
        users = users,
        backStack = listOf(screen)
    )

    /* ---- Çizim ------------------------------------------------------------------------------- */

    private fun render(name: String, content: @Composable () -> Unit) {
        rule.setContent {
            PixelStoreTheme {
                CompositionLocalProvider(LocalFeedback provides NoopFeedback) { content() }
            }
        }
        rule.waitForIdle()

        val root: View = rule.activity.window.decorView
        if (root.width == 0 || root.height == 0) {
            root.measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT_PX, View.MeasureSpec.EXACTLY)
            )
            root.layout(0, 0, WIDTH_PX, HEIGHT_PX)
            rule.waitForIdle()
        }

        val bitmap = Bitmap.createBitmap(
            root.width.coerceAtLeast(1),
            root.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        root.draw(Canvas(bitmap))

        val file = File(outDir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("screenshot -> ${file.absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    private fun renderApp(name: String, state: UiState) {
        val viewModel = StoreViewModel(application)
        render(name) { PixelStoreApp(viewModel = viewModel, state = state) }
    }

    @Test
    fun login() = renderApp("01-login", state(session = null))

    @Test
    fun storeUser() = renderApp("02-store", state(userSession))

    @Test
    fun storeAdmin() = renderApp("03-store-admin", state(adminSession, includeDrafts = true))

    @Test
    fun detailScreen() = renderApp("04-detail", state(userSession, Screen.Detail("golge-vadisi")))

    @Test
    fun reviewsScreen() = renderApp("05-reviews", state(userSession, Screen.Reviews("golge-vadisi")))

    @Test
    fun categories() = renderApp("06-categories", state(userSession, Screen.Categories))

    @Test
    fun wishlist() = renderApp("07-wishlist", state(userSession, Screen.Wishlist))

    @Test
    fun profile() = renderApp("08-profile", state(userSession, Screen.Profile))

    @Test
    fun adminDashboard() = renderApp("09-admin", state(adminSession, Screen.Admin, includeDrafts = true))

    @Test
    fun adminUsers() = renderApp("10-admin-users", state(adminSession, Screen.AdminUsers))

    @Test
    fun adminEditor() =
        renderApp("11-admin-editor", state(adminSession, Screen.AdminEditor("golge-vadisi"), includeDrafts = true))

    private companion object {
        const val WIDTH_PX = 822
        const val HEIGHT_PX = 1782
    }
}
