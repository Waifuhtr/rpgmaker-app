package com.waifuhtr.pixelstore.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.BuildConfig
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.ui.Format
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.InfoRow
import com.waifuhtr.pixelstore.ui.components.MetricTile
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelDivider
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.PixelSwitchRow
import com.waifuhtr.pixelstore.ui.components.RemoteImage
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

@Composable
fun ProfileScreen(viewModel: StoreViewModel, state: UiState) {
    val session = state.session ?: return
    val feedback = LocalFeedback.current
    var confirmLogout by remember { mutableStateOf(false) }

    // Sistem fotoğraf seçici: depolama izni istemez, yalnızca seçilen görsele erişim verir.
    val pickAvatar = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadAvatar(uri) }

    LazyColumn(
        Modifier.fillMaxWidth(),
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
                Box(contentAlignment = Alignment.BottomEnd) {
                    RemoteImage(
                        url = session.avatarUrl,
                        fallbackSeed = session.username,
                        palette = if (session.isAdmin) "amber" else "azure",
                        contentDescription = "Profil fotoğrafı",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickablePixel({
                                feedback.tap(Sfx.OPEN)
                                pickAvatar.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }, "Profil fotoğrafını değiştir")
                    )
                    // Dokunulabilir olduğunu gösteren küçük kalem işareti.
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(PixelColors.Ink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelGlyphImage("pencil", PixelColors.Gold, Modifier.size(12.dp))
                    }
                }
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

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                MetricTile(
                    Format.count(session.favoriteCount),
                    "İSTEK LİSTESİ",
                    PixelColors.Rose,
                    Modifier.weight(1f)
                )
                MetricTile(
                    Format.count(session.reviewCount),
                    "YORUM",
                    PixelColors.Mint,
                    Modifier.weight(1f)
                )
            }
        }

        if (session.badges.isNotEmpty()) {
            item { SectionHeader("Başarımlar", glyph = "pip") }
            item {
                PixelPanel {
                    session.badges.forEach { badge ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rozet rengi sitedeki başarım ayarından gelir.
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(parseColor(badge.color, PixelColors.Gold))
                            )
                            Spacer(Modifier.width(PixelSpacing.medium))
                            Text(
                                badge.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = PixelColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            PixelButton(
                text = "PROFİL FOTOĞRAFI SEÇ",
                onClick = {
                    feedback.tap(Sfx.OPEN)
                    pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                tone = PixelButtonTone.GHOST,
                glyph = "pencil",
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (session.avatarUrl.contains("/wp-content/")) {
            // Yalnızca kendi yüklediği görsel varsa kaldırma seçeneği anlamlı.
            item {
                PixelButton(
                    text = "FOTOĞRAFI KALDIR",
                    onClick = {
                        feedback.tap(Sfx.CANCEL)
                        viewModel.clearAvatar()
                    },
                    tone = PixelButtonTone.DANGER,
                    glyph = "cross",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { SectionHeader("İstek listem", glyph = "heart") }
        item {
            PixelButton(
                text = "İSTEK LİSTEMİ AÇ (${session.favoriteCount})",
                onClick = {
                    feedback.tap()
                    viewModel.openTab(Screen.Wishlist)
                },
                tone = PixelButtonTone.MINT,
                glyph = "heart",
                modifier = Modifier.fillMaxWidth()
            )
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

        item { SectionHeader("Hakkında", glyph = "pip") }
        item {
            PixelPanel(padding = PixelSpacing.medium) {
                InfoRow("Sunucu", BuildConfig.SITE_URL.removePrefix("https://"))
                PixelDivider()
                InfoRow("Katılım", session.joinedAt.ifBlank { "—" })
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

/** Sitedeki rozet rengi "#rrggbb" biçiminde gelir; okunamazsa varsayılana düşer. */
private fun parseColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6) return fallback
    val value = clean.toLongOrNull(16) ?: return fallback
    return Color(0xFF000000 or value)
}
