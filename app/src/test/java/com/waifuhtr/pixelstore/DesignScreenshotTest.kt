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
import com.waifuhtr.pixelstore.data.AppRecord
import com.waifuhtr.pixelstore.data.Catalog
import com.waifuhtr.pixelstore.data.Json
import com.waifuhtr.pixelstore.data.Role
import com.waifuhtr.pixelstore.data.Session
import com.waifuhtr.pixelstore.data.SourceKind
import com.waifuhtr.pixelstore.data.Stats
import com.waifuhtr.pixelstore.data.UserAccount
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.NoopFeedback
import com.waifuhtr.pixelstore.ui.screens.PixelStoreApp
import com.waifuhtr.pixelstore.ui.theme.PixelStoreTheme
import org.json.JSONObject
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
 * Emülatör olmadan da arayüzün gerçekte nasıl göründüğünü görebilmek için ekranlar Robolectric'in
 * yerel grafik kipinde çizilip PNG olarak kaydedilir. Amaç doğrulama değil gözle denetim: çıktı
 * `app/build/screenshots/` altına yazılır.
 *
 * Çalıştırma: ./gradlew :app:testDebugUnitTest --tests '*DesignScreenshotTest'
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

    /** Tohum katalog: testler gerçek veriyle çizilsin. */
    private val catalog: Catalog by lazy {
        val text = application.assets.open("catalog_seed.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json.catalogFrom(JSONObject(text))
    }

    private val adminSession = Session("admin", "Yönetici Yamato", Role.ADMIN, "warden-01")
    private val userSession = Session("user", "Gezgin Kaya", Role.USER, "wanderer-07")

    private fun baseState(
        session: Session?,
        screen: Screen = Screen.Store,
        includeDrafts: Boolean = false
    ): UiState {
        val apps = if (includeDrafts) catalog.apps else catalog.apps.filter { it.published }
        return UiState(
            booting = false,
            session = session,
            sourceKind = SourceKind.LOCAL,
            apps = apps,
            categories = catalog.categories,
            installed = setOf(catalog.apps.first().id),
            backStack = listOf(screen),
            stats = stats(apps),
            users = listOf(
                UserAccount("admin", "Yönetici Yamato", Role.ADMIN, "warden-01", "2024-03-11", "Katalog yöneticisi"),
                UserAccount("user", "Gezgin Kaya", Role.USER, "wanderer-07", "2025-01-08", "Standart oyuncu")
            )
        )
    }

    private fun stats(apps: List<AppRecord>) = Stats(
        totalApps = catalog.apps.size,
        published = catalog.apps.count { it.published },
        drafts = catalog.apps.count { !it.published },
        totalInstalls = catalog.apps.sumOf { it.installs.toLong() },
        averageRating = 4.5,
        perCategory = catalog.apps.groupBy { it.category }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
    )

    /**
     * Compose ağacını çizip PNG'ye yazar.
     *
     * `captureToImage()` Robolectric'te pencere kopyalamasına (PixelCopy) dayandığı için zaman
     * aşımına uğruyor; bunun yerine kök görünüm doğrudan bir [Canvas] üzerine çizilir. Yerel
     * grafik kipinde bu gerçek pikseller üretir.
     */
    private fun render(name: String, content: @Composable () -> Unit) {
        rule.setContent {
            PixelStoreTheme {
                CompositionLocalProvider(LocalFeedback provides NoopFeedback) {
                    content()
                }
            }
        }
        rule.waitForIdle()

        val root: View = rule.activity.window.decorView
        if (root.width == 0 || root.height == 0) {
            // Robolectric bazen yerleşimi kendiliğinden tetiklemiyor; ölçüyü zorla.
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
        file.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        println("screenshot -> ${file.absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    private fun renderApp(name: String, state: UiState) {
        val viewModel = StoreViewModel(application)
        render(name) { PixelStoreApp(viewModel = viewModel, state = state) }
    }

    @Test
    fun login() = renderApp("01-login", baseState(session = null))

    @Test
    fun storeUser() = renderApp("02-store-user", baseState(userSession))

    @Test
    fun storeAdmin() = renderApp("03-store-admin", baseState(adminSession, includeDrafts = true))

    @Test
    fun detail() = renderApp(
        "04-detail",
        baseState(userSession, Screen.Detail(catalog.apps.first().id))
    )

    @Test
    fun categories() = renderApp("05-categories", baseState(userSession, Screen.Categories))

    @Test
    fun profile() = renderApp("06-profile", baseState(userSession, Screen.Profile))

    @Test
    fun adminDashboard() =
        renderApp("07-admin", baseState(adminSession, Screen.Admin, includeDrafts = true))

    @Test
    fun adminUsers() = renderApp("08-admin-users", baseState(adminSession, Screen.AdminUsers))

    @Test
    fun adminEditor() = renderApp(
        "09-admin-editor",
        baseState(adminSession, Screen.AdminEditor(catalog.apps.first().id), includeDrafts = true)
    )

    @Test
    fun connection() = renderApp("10-connection", baseState(userSession, Screen.Connection))

    private companion object {
        // qualifiers'taki 411dp x 891dp @ xhdpi (2x) karşılığı.
        const val WIDTH_PX = 822
        const val HEIGHT_PX = 1782
    }
}
