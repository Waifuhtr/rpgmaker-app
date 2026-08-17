package com.waifuhtr.pixelstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.PixelFeedback
import com.waifuhtr.pixelstore.ui.screens.PixelStoreApp
import com.waifuhtr.pixelstore.ui.theme.PixelStoreTheme

/**
 * Tek Activity, tamamı Jetpack Compose ile çizilen yerel arayüz.
 *
 * v1'de arayüz WebView içinde HTML/CSS/JS idi; v2'de WebView tamamen kaldırıldı. Ekranlar,
 * piksel çerçeveler, ikonlar ve ekran görüntüleri Compose Canvas üzerine çizilir.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: StoreViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Ses/titreşim tercihleri anlık okunur; ayar değişince yeniden oluşturmak gerekmez.
            val feedback = remember {
                PixelFeedback(
                    context = this,
                    soundEnabled = { viewModel.state.value.soundEnabled },
                    hapticsEnabled = { viewModel.state.value.hapticsEnabled }
                )
            }
            DisposableEffect(Unit) {
                onDispose { feedback.release() }
            }

            PixelStoreTheme {
                CompositionLocalProvider(LocalFeedback provides feedback) {
                    // Donanım geri tuşu: ekran yığınını yönetir, kök ekranda uygulamadan çıkar.
                    BackHandler(enabled = true) {
                        if (!viewModel.back()) finish()
                    }
                    PixelStoreApp(viewModel = viewModel, state = state)
                }
            }
        }
    }
}
