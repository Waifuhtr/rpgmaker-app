package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.BuildConfig
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.SourceKind
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelIconImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.InfoRow
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelDivider
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.PixelSwitchRow
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

@Composable
fun ProfileScreen(viewModel: StoreViewModel, state: UiState) {
    val session = state.session ?: return
    val feedback = LocalFeedback.current
    var confirmLogout by remember { mutableStateOf(false) }
    val library = state.apps.filter { state.installed.contains(it.id) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = PixelSpacing.gutter,
            end = PixelSpacing.gutter,
            top = PixelSpacing.large,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.large)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pixelFrame(
                        fill = PixelColors.Surface,
                        border = if (session.isAdmin) PixelColors.Gold else PixelColors.Sky
                    )
                    .padding(PixelSpacing.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelIconImage(
                    session.avatarSeed,
                    if (session.isAdmin) "amber" else "azure",
                    Modifier.size(72.dp)
                )
                Spacer(Modifier.width(PixelSpacing.large))
                Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                    Text(
                        session.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = PixelColors.TextPrimary
                    )
                    Text(
                        "@${session.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelColors.TextTertiary
                    )
                    PixelBadge(
                        session.role.label,
                        if (session.isAdmin) BadgeTone.ADMIN else BadgeTone.USER
                    )
                }
            }
        }

        item { SectionHeader("Kütüphanem", glyph = "bag") }
        if (library.isEmpty()) {
            item { EmptyState("Henüz bir şey indirmedin.", glyph = "bag") }
        } else {
            items(library, key = { it.id }) { app ->
                AppCard(
                    app = app,
                    installed = true,
                    onClick = {
                        feedback.tap()
                        viewModel.push(Screen.Detail(app.id))
                    }
                )
            }
        }

        item { SectionHeader("Ayarlar", glyph = "gear") }
        item {
            PixelPanel {
                PixelSwitchRow(
                    label = "Ses efektleri",
                    description = "8-bit tık sesleri (cihazda üretilir)",
                    checked = state.soundEnabled,
                    onCheckedChange = viewModel::setSound
                )
                PixelDivider()
                PixelSwitchRow(
                    label = "Titreşim",
                    description = "Dokunuşlarda kısa dokunsal geri bildirim",
                    checked = state.hapticsEnabled,
                    onCheckedChange = viewModel::setHaptics
                )
            }
        }

        item {
            PixelButton(
                text = "BAĞLANTI AYARLARI",
                onClick = {
                    feedback.tap(Sfx.OPEN)
                    viewModel.push(Screen.Connection)
                },
                tone = PixelButtonTone.GHOST,
                glyph = "gear",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { SectionHeader("Hakkında", glyph = "pip") }
        item {
            PixelPanel(padding = PixelSpacing.medium) {
                InfoRow("Veri kaynağı", if (state.sourceKind == SourceKind.WORDPRESS) "WordPress" else "Yerel (cihaz)")
                PixelDivider()
                if (state.sourceKind == SourceKind.WORDPRESS) {
                    InfoRow("Sunucu", state.wordpressUrl)
                    PixelDivider()
                }
                InfoRow("Paket", BuildConfig.APPLICATION_ID)
                PixelDivider()
                InfoRow("Sürüm", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                PixelDivider()
                InfoRow("Arayüz", "Jetpack Compose · yerel çizim")
            }
        }

        item {
            PixelButton(
                text = "ÇIKIŞ YAP",
                onClick = {
                    feedback.tap(Sfx.CANCEL)
                    confirmLogout = true
                },
                tone = PixelButtonTone.DANGER,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (confirmLogout) {
        PixelModal(
            title = "Çıkış",
            message = "Oturumu kapatmak istiyor musun?",
            confirmLabel = "Çıkış yap",
            onConfirm = {
                confirmLogout = false
                viewModel.logout()
            },
            onDismiss = { confirmLogout = false }
        )
    }
}
