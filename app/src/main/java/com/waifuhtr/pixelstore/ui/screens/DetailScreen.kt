package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.AppRecord
import com.waifuhtr.pixelstore.ui.Format
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.art.PixelIconImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.InfoRow
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelDivider
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.components.StarRow
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(viewModel: StoreViewModel, state: UiState, appId: String) {
    val app = state.app(appId)
    if (app == null) {
        EmptyState("Kayıt bulunamadı.")
        return
    }

    val feedback = LocalFeedback.current
    val installed = state.installed.contains(app.id)
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }

    // Sahte indirme akışı: ilerleme çubuğu dolduktan sonra sayaç sunucuda/depoda artırılır.
    if (progress != null) {
        androidx.compose.runtime.LaunchedEffect(app.id) {
            var value = 0f
            while (value < 1f) {
                delay(120)
                value = (value + 0.09f + Math.random().toFloat() * 0.08f).coerceAtMost(1f)
                progress = value
            }
            delay(140)
            viewModel.install(app.id) { progress = null }
        }
    }

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
        item { DetailHeader(app) }
        item { MetricsStrip(app) }

        item {
            val running = progress != null
            Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                PixelButton(
                    text = when {
                        running -> "İNDİRİLİYOR"
                        installed -> "KALDIR"
                        else -> "İNDİR"
                    },
                    onClick = {
                        if (running) return@PixelButton
                        if (installed) {
                            feedback.tap(Sfx.CANCEL)
                            viewModel.uninstall(app.id)
                        } else {
                            feedback.tap(Sfx.OPEN)
                            progress = 0f
                        }
                    },
                    tone = if (installed) PixelButtonTone.GHOST else PixelButtonTone.PRIMARY,
                    enabled = !running,
                    glyph = if (installed) "cross" else "check",
                    modifier = Modifier.fillMaxWidth()
                )
                if (running) {
                    SegmentBar(
                        ratio = progress ?: 0f,
                        segments = 16,
                        onColor = PixelColors.Mint,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (app.downloadUrl.isNotBlank()) {
            item {
                PixelButton(
                    text = "GELİŞTİRİCİ SAYFASI",
                    onClick = {
                        feedback.tap()
                        viewModel.toast("Bağlantı: ${app.downloadUrl}")
                    },
                    tone = PixelButtonTone.GHOST,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (app.screenshots.isNotEmpty()) {
            item { SectionHeader("Ekran görüntüleri", glyph = "gem") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    itemsIndexed(app.screenshots) { index, shot ->
                        Column(
                            Modifier
                                .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Outline)
                                .clickablePixel({
                                    feedback.tap(Sfx.OPEN)
                                    viewerIndex = index
                                }, shot.caption.ifBlank { "Ekran görüntüsü ${index + 1}" })
                                .padding(PixelSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)
                        ) {
                            ShotFrame(shot = shot, palette = app.palette, scale = 1.25f)
                            Text(
                                text = shot.caption.ifBlank { "${shot.kind.label} · ${shot.scene.label}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = PixelColors.TextTertiary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width((shot.kind.w * 1.25f).dp)
                            )
                        }
                    }
                }
            }
        }

        if (app.longDescription.isNotBlank()) {
            item { SectionHeader("Açıklama", glyph = "grid") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    app.longDescription.split("\n").filter { it.isNotBlank() }.forEach { line ->
                        Text(
                            text = line.trim(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PixelColors.TextPrimary
                        )
                    }
                }
            }
        }

        if (app.tags.isNotEmpty()) {
            item { SectionHeader("Etiketler", glyph = "pip") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                    itemsIndexed(app.tags) { _, tag ->
                        PixelBadge(tag, BadgeTone.NEUTRAL)
                    }
                }
            }
        }

        item { SectionHeader("Bilgiler", glyph = "gear") }
        item {
            PixelPanel(padding = PixelSpacing.medium) {
                InfoRow("Sürüm", app.version)
                PixelDivider()
                InfoRow("Güncelleme", app.updatedAt.ifBlank { "—" })
                PixelDivider()
                InfoRow("Tür", app.category)
                PixelDivider()
                InfoRow("İçerik yaşı", app.contentRating)
                PixelDivider()
                InfoRow("Paket", app.id)
            }
        }

        if (state.isAdmin) {
            item {
                PixelButton(
                    text = "YÖNETİCİ: BU KAYDI DÜZENLE",
                    onClick = {
                        feedback.tap()
                        viewModel.push(Screen.AdminEditor(app.id))
                    },
                    tone = PixelButtonTone.ADMIN,
                    glyph = "pencil",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    viewerIndex?.let { index ->
        ShotViewer(
            title = app.title,
            shots = app.screenshots,
            palette = app.palette,
            startIndex = index,
            onDismiss = { viewerIndex = null }
        )
    }
}

@Composable
private fun DetailHeader(app: AppRecord) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        PixelIconImage(
            app.iconSeed,
            app.palette,
            Modifier
                .size(88.dp)
                .pixelFrame(
                    fill = Color.Transparent,
                    border = PixelColors.Outline,
                    corner = PixelColors.Backdrop,
                    thickness = 2.dp
                )
        )
        Spacer(Modifier.width(PixelSpacing.large))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
            Text(
                app.title,
                style = MaterialTheme.typography.displayMedium,
                color = PixelColors.Gold
            )
            Text(
                app.developer,
                style = MaterialTheme.typography.bodyMedium,
                color = PixelColors.Sky
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                PixelBadge(app.category, BadgeTone.CATEGORY)
                PixelBadge(app.contentRating, BadgeTone.NEUTRAL)
                if (!app.published) PixelBadge("TASLAK", BadgeTone.DRAFT)
            }
        }
    }
}

/** Üç ölçü tek şeritte: puan, indirme, boyut. Dikey çizgilerle net ayrılır. */
@Composable
private fun MetricsStrip(app: AppRecord) {
    Row(
        Modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Outline)
            .padding(vertical = PixelSpacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetricCell(
            modifier = Modifier.weight(1f),
            value = if (app.rating > 0) Format.rating(app.rating) else "—",
            label = "${Format.count(app.ratingCount)} oy",
            accent = PixelColors.Gold
        ) {
            StarRow(app.rating, starSize = 10.dp)
        }
        VerticalRule()
        MetricCell(
            modifier = Modifier.weight(1f),
            value = Format.count(app.installs),
            label = "indirme",
            accent = PixelColors.Mint
        )
        VerticalRule()
        MetricCell(
            modifier = Modifier.weight(1f),
            value = Format.size(app.sizeMb),
            label = "boyut",
            accent = PixelColors.Sky
        )
    }
}

@Composable
private fun MetricCell(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: Color,
    topContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.tiny)
    ) {
        if (topContent != null) {
            topContent()
            Spacer(Modifier.height(2.dp))
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = PixelColors.TextTertiary)
    }
}

@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .width(2.dp)
            .height(44.dp)
            .background(PixelColors.OutlineSoft)
    )
}
