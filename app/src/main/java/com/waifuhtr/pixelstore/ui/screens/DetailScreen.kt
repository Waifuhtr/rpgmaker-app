package com.waifuhtr.pixelstore.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.MessageTone
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.DownloadTicket
import com.waifuhtr.pixelstore.data.GameDetail
import com.waifuhtr.pixelstore.data.SpecSet
import com.waifuhtr.pixelstore.ui.Format
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.InfoRow
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelDivider
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.RemoteImage
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.components.StarRow
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(viewModel: StoreViewModel, state: UiState, gameId: String) {
    val detail = state.detail

    // Ekran yeniden oluşturulduysa (süreç geri geldi) kaydı tekrar çek.
    LaunchedEffect(gameId) {
        if (detail == null || detail.id != gameId) viewModel.loadDetail(gameId)
    }

    if (detail == null || detail.id != gameId) {
        EmptyState(if (state.detailLoading) "Yükleniyor…" else "Oyun bulunamadı.")
        return
    }

    val context = LocalContext.current
    val feedback = LocalFeedback.current
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDownload by remember { mutableStateOf<DownloadTicket?>(null) }
    var countdownMirror by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            viewModel.toast("Bağlantıyı açacak uygulama bulunamadı.", MessageTone.ERROR)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.large)
    ) {
        item { HeroHeader(detail) }

        item {
            Column(
                Modifier.padding(horizontal = PixelSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(PixelSpacing.large)
            ) {
                MetricsStrip(detail)

                // İndirme + istek listesi
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    PixelButton(
                        text = "İNDİR",
                        onClick = {
                            feedback.tap(Sfx.OPEN)
                            countdownMirror = false
                            viewModel.requestDownload(detail.id, false) { pendingDownload = it }
                        },
                        glyph = "download",
                        enabled = detail.downloadUrl.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .size(48.dp)
                            .pixelFrame(
                                fill = if (detail.summary.favorited) PixelColors.RoseDeep else PixelColors.SurfaceHigh,
                                border = if (detail.summary.favorited) PixelColors.Rose else PixelColors.Outline
                            )
                            .clickablePixel({
                                feedback.tap(Sfx.COIN)
                                viewModel.toggleFavorite(detail.id)
                            }, if (detail.summary.favorited) "İstek listesinden çıkar" else "İstek listesine ekle"),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelGlyphImage(
                            "heart",
                            if (detail.summary.favorited) PixelColors.Rose else PixelColors.TextTertiary,
                            Modifier.size(20.dp)
                        )
                    }
                }

                if (detail.mirrorUrl.isNotBlank()) {
                    PixelButton(
                        text = "ALTERNATİF LİNK",
                        onClick = {
                            feedback.tap()
                            countdownMirror = true
                            viewModel.requestDownload(detail.id, true) { pendingDownload = it }
                        },
                        tone = PixelButtonTone.GHOST,
                        glyph = "download",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (detail.downloadUrl.isBlank()) {
                    Text(
                        "Bu kayıt için henüz indirme bağlantısı eklenmemiş.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelColors.Amber
                    )
                }

                RatingPanel(detail) { rating ->
                    feedback.tap(Sfx.COIN)
                    viewModel.rate(detail.id, rating)
                }
            }
        }

        if (detail.screenshots.isNotEmpty()) {
            item { Padded { SectionHeader("Ekran görüntüleri", glyph = "gem") } }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PixelSpacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
                ) {
                    itemsIndexed(detail.screenshots) { index, shot ->
                        RemoteImage(
                            url = shot.url,
                            fallbackSeed = "${detail.id}-$index",
                            contentDescription = "Ekran görüntüsü ${index + 1}",
                            modifier = Modifier
                                .size(width = 240.dp, height = 135.dp)
                                .pixelFrame(
                                    fill = Color.Transparent,
                                    border = PixelColors.Outline,
                                    corner = PixelColors.Backdrop,
                                    thickness = 2.dp
                                )
                                .clickablePixel({
                                    feedback.tap(Sfx.OPEN)
                                    viewerIndex = index
                                }, "Ekran görüntüsünü büyüt")
                        )
                    }
                }
            }
        }

        if (detail.description.isNotBlank()) {
            item { Padded { SectionHeader("Oyun hakkında", glyph = "grid") } }
            item {
                Padded {
                    Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                        detail.description.split("\n").filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                line.trim(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = PixelColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (detail.changelog.isNotBlank()) {
            item { Padded { SectionHeader("Değişiklik günlüğü", glyph = "pencil") } }
            item {
                Padded {
                    PixelPanel(border = PixelColors.MintDeep) {
                        detail.changelog.split("\n").filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                line.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PixelColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (detail.installGuide.isNotBlank()) {
            item { Padded { SectionHeader("Kurulum rehberi", glyph = "gear") } }
            item {
                Padded {
                    PixelPanel(border = PixelColors.SkyDeep) {
                        detail.installGuide.split("\n").filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                line.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PixelColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (detail.requirements.hasMinimum) {
            item { Padded { SectionHeader("Sistem gereksinimleri", glyph = "grid") } }
            item {
                Padded {
                    Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                        SpecCard("MİNİMUM", detail.requirements.minimum, PixelColors.Outline)
                        if (detail.requirements.hasRecommended) {
                            SpecCard("ÖNERİLEN", detail.requirements.recommended, PixelColors.MintDeep)
                        }
                    }
                }
            }
        }

        item { Padded { SectionHeader("Bilgiler", glyph = "gear") } }
        item {
            Padded {
                PixelPanel(padding = PixelSpacing.medium) {
                    InfoRowIf("Sürüm", detail.summary.version)
                    InfoRowIf("Boyut", detail.summary.sizeLabel)
                    InfoRowIf("Platform", detail.platforms.joinToString(", "))
                    InfoRowIf("Dil", detail.languages.joinToString(", "))
                    InfoRowIf("Geliştirici", detail.summary.developer)
                    InfoRowIf("Yayıncı", detail.summary.publisher)
                    InfoRowIf("Durum", detail.summary.status)
                    InfoRowIf("Çıkış", detail.releaseDate)
                    InfoRowIf("Yaş sınırı", detail.ageRating)
                    InfoRowIf("Lisans", detail.licenseType)
                    InfoRowIf("Güncelleme", detail.summary.updatedAt)
                    InfoRowIf("Arşiv şifresi", detail.archivePassword)
                    if (detail.multiplayer || detail.controller) {
                        PixelDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                            if (detail.multiplayer) PixelBadge("ÇOK OYUNCULU", BadgeTone.INSTALLED)
                            if (detail.controller) PixelBadge("KUMANDA", BadgeTone.CATEGORY)
                        }
                    }
                }
            }
        }

        if (detail.tags.isNotEmpty() || detail.features.isNotEmpty()) {
            item { Padded { SectionHeader("Etiketler", glyph = "pip") } }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PixelSpacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)
                ) {
                    itemsIndexed(detail.features + detail.tags) { _, label ->
                        PixelBadge(label, BadgeTone.NEUTRAL)
                    }
                }
            }
        }

        item {
            Padded {
                Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    PixelButton(
                        text = if (detail.reviewCount > 0) "YORUMLAR (${detail.reviewCount})" else "İLK YORUMU SEN YAZ",
                        onClick = {
                            feedback.tap()
                            viewModel.openReviews(detail.id)
                        },
                        tone = PixelButtonTone.MINT,
                        glyph = "hero",
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (detail.trailerUrl.isNotBlank()) {
                        PixelButton(
                            text = "FRAGMANI İZLE",
                            onClick = {
                                feedback.tap()
                                openUrl(detail.trailerUrl)
                            },
                            tone = PixelButtonTone.GHOST,
                            glyph = "right",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    PixelButton(
                        text = "SORUN / HATA BİLDİR",
                        onClick = {
                            feedback.tap(Sfx.CANCEL)
                            showReport = true
                        },
                        tone = PixelButtonTone.DANGER,
                        glyph = "warn",
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.isAdmin) {
                        PixelButton(
                            text = "YÖNETİCİ: BU OYUNU DÜZENLE",
                            onClick = {
                                feedback.tap()
                                viewModel.push(Screen.AdminEditor(detail.id))
                            },
                            tone = PixelButtonTone.ADMIN,
                            glyph = "pencil",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    viewerIndex?.let { index ->
        ScreenshotViewer(
            title = detail.title,
            urls = detail.screenshots.map { it.fullUrl },
            startIndex = index,
            onDismiss = { viewerIndex = null }
        )
    }

    pendingDownload?.let { ticket ->
        DownloadCountdown(
            title = detail.title,
            ticket = ticket,
            mirror = countdownMirror,
            onOpen = {
                openUrl(ticket.url)
                pendingDownload = null
            },
            onDismiss = { pendingDownload = null }
        )
    }

    if (showReport) {
        PixelPromptModal(
            title = "Hata bildir",
            description = "İndirme, kurulum veya oynanış sorununu yaz. Rapor doğrudan yönetime iletilir.",
            placeholder = "Sorunu detaylıca anlat…",
            confirmLabel = "GÖNDER",
            onConfirm = { message ->
                viewModel.report(detail.id, message) { showReport = false }
            },
            onDismiss = { showReport = false }
        )
    }
}

/* ---- Parçalar ---------------------------------------------------------------------------------- */

@Composable
private fun Padded(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = PixelSpacing.gutter)) { content() }
}

@Composable
private fun HeroHeader(detail: GameDetail) {
    Box(Modifier.fillMaxWidth()) {
        // Arka plan: sitedeki arka plan görseli veya tam boy kapak.
        RemoteImage(
            url = detail.heroUrl.ifBlank { detail.summary.coverUrl },
            fallbackSeed = detail.id,
            palette = "azure",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        // Okunabilirlik için alta doğru koyulaşan perde: görselin üstü açık kalır, başlık bloğu
        // neredeyse düz zemine oturur. Düz yarı saydam kaplama, hareketli kapaklarda yazıyı
        // okunmaz bırakıyordu.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Brush.verticalGradient(
                        0f to PixelColors.Backdrop.copy(alpha = 0.10f),
                        0.40f to PixelColors.Backdrop.copy(alpha = 0.42f),
                        0.72f to PixelColors.Backdrop.copy(alpha = 0.88f),
                        1f to PixelColors.Backdrop
                    )
                )
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(PixelSpacing.gutter),
            verticalAlignment = Alignment.Bottom
        ) {
            RemoteImage(
                url = detail.summary.coverUrl,
                fallbackSeed = detail.id,
                contentDescription = detail.title,
                modifier = Modifier
                    .size(width = 86.dp, height = 120.dp)
                    .pixelFrame(
                        fill = Color.Transparent,
                        border = PixelColors.Gold,
                        corner = Color.Transparent,
                        thickness = 2.dp
                    )
            )
            Spacer(Modifier.width(PixelSpacing.medium))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    detail.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = PixelColors.Gold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.summary.subtitle.isNotBlank()) {
                    Text(
                        detail.summary.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelColors.TextPrimary
                    )
                }
                if (detail.summary.developer.isNotBlank()) {
                    Text(
                        detail.summary.developer,
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelColors.Sky
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (detail.summary.status.isNotBlank()) {
                        PixelBadge(detail.summary.status, BadgeTone.INSTALLED)
                    }
                    if (detail.summary.version.isNotBlank()) {
                        PixelBadge(detail.summary.version, BadgeTone.CATEGORY)
                    }
                    if (!detail.summary.published) PixelBadge("TASLAK", BadgeTone.DRAFT)
                }
            }
        }
    }
}

@Composable
private fun MetricsStrip(detail: GameDetail) {
    Row(
        Modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Outline)
            .padding(vertical = PixelSpacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetricCell(
            modifier = Modifier.weight(1f),
            value = if (detail.summary.rating > 0) Format.rating(detail.summary.rating) else "—",
            label = "${Format.count(detail.summary.ratingCount)} oy",
            accent = PixelColors.Gold
        )
        VerticalRule()
        MetricCell(
            modifier = Modifier.weight(1f),
            value = Format.count(detail.summary.downloadCount),
            label = "indirme",
            accent = PixelColors.Mint
        )
        VerticalRule()
        MetricCell(
            modifier = Modifier.weight(1f),
            value = detail.summary.sizeLabel.ifBlank { "—" },
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
    accent: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.tiny)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = PixelColors.TextTertiary)
    }
}

@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .width(2.dp)
            .height(40.dp)
            .background(PixelColors.OutlineSoft)
    )
}

/** Kullanıcı puanı: dokunulan yıldıza kadar oy verilir, mevcut oy vurgulanır. */
@Composable
private fun RatingPanel(detail: GameDetail, onRate: (Int) -> Unit) {
    PixelPanel(border = if (detail.userRating > 0) PixelColors.GoldDeep else PixelColors.Outline) {
        Text(
            if (detail.userRating > 0) "PUANIN: ${detail.userRating}/5" else "BU OYUNU PUANLA",
            style = MaterialTheme.typography.labelMedium,
            color = PixelColors.Gold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
            for (star in 1..5) {
                val filled = star <= detail.userRating
                Box(
                    Modifier
                        .size(40.dp)
                        .pixelFrame(
                            fill = if (filled) PixelColors.SurfaceHigh else PixelColors.SurfaceSunken,
                            border = if (filled) PixelColors.Gold else PixelColors.OutlineSoft,
                            thickness = 2.dp
                        )
                        .clickablePixel({ onRate(star) }, "$star yıldız ver"),
                    contentAlignment = Alignment.Center
                ) {
                    PixelGlyphImage(
                        "pip",
                        if (filled) PixelColors.Gold else PixelColors.OutlineSoft,
                        Modifier.size(16.dp)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarRow(detail.summary.rating, starSize = 11.dp)
            Spacer(Modifier.width(PixelSpacing.small))
            Text(
                "site ortalaması ${Format.rating(detail.summary.rating)}",
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextTertiary
            )
        }
    }
}

@Composable
private fun SpecCard(title: String, specs: SpecSet, border: Color) {
    val rows = specs.rows()
    if (rows.isEmpty()) return
    PixelPanel(border = border, padding = PixelSpacing.medium) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = PixelColors.Gold)
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) PixelDivider()
            InfoRow(label, value)
        }
    }
}

@Composable
private fun InfoRowIf(label: String, value: String) {
    if (value.isBlank()) return
    InfoRow(label, value)
}

/**
 * İndirme geri sayımı.
 *
 * Sitede indirme ayrı bir yönlendirme sayfasından geçiyor (5 saniye bekleme). Uygulamada aynı akış
 * yerel olarak taklit edilir: sayaç artırılır, geri sayım biter ve bağlantı sistem tarayıcısında
 * açılır. Kullanıcı beklemeden de açabilir.
 */
@Composable
private fun DownloadCountdown(
    title: String,
    ticket: DownloadTicket,
    mirror: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    var remaining by remember { mutableStateOf(3) }
    LaunchedEffect(ticket.url) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        onOpen()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(PixelSpacing.xlarge)
                .fillMaxWidth()
                .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Gold, corner = PixelColors.Backdrop)
                .padding(PixelSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
        ) {
            Text(
                if (mirror) "ALTERNATİF İNDİRME" else "İNDİRME HAZIRLANIYOR",
                style = MaterialTheme.typography.titleMedium,
                color = PixelColors.Gold
            )
            Text(title, style = MaterialTheme.typography.bodyLarge, color = PixelColors.TextPrimary, maxLines = 2)
            Text(
                if (remaining > 0) "$remaining" else "AÇILIYOR",
                style = MaterialTheme.typography.displayLarge,
                color = PixelColors.Mint
            )
            SegmentBar(
                ratio = (3 - remaining) / 3f,
                segments = 12,
                onColor = PixelColors.Mint,
                modifier = Modifier.fillMaxWidth()
            )
            if (ticket.password.isNotBlank()) {
                PixelPanel(border = PixelColors.SkyDeep, padding = PixelSpacing.medium) {
                    Text("ARŞİV ŞİFRESİ", style = MaterialTheme.typography.labelSmall, color = PixelColors.Sky)
                    Text(
                        ticket.password,
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelColors.TextPrimary
                    )
                }
            }
            Text(
                "Toplam indirme: ${Format.count(ticket.downloadCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextTertiary
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                PixelButton("VAZGEÇ", onDismiss, tone = PixelButtonTone.GHOST, modifier = Modifier.weight(1f))
                PixelButton("HEMEN AÇ", onOpen, modifier = Modifier.weight(1f))
            }
        }
    }
}
