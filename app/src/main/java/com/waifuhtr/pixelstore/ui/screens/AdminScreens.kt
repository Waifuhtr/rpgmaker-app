package com.waifuhtr.pixelstore.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.AppRecord
import com.waifuhtr.pixelstore.data.Screenshot
import com.waifuhtr.pixelstore.ui.Format
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelArt
import com.waifuhtr.pixelstore.ui.art.PixelIconImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.LabeledField
import com.waifuhtr.pixelstore.ui.components.MetricTile
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelChip
import com.waifuhtr.pixelstore.ui.components.PixelDivider
import com.waifuhtr.pixelstore.ui.components.PixelIconButton
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.PixelSwitchRow
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame
import kotlin.random.Random

/* ---- Yönetim panosu -------------------------------------------------------------------------- */

@Composable
fun AdminScreen(viewModel: StoreViewModel, state: UiState) {
    if (!state.isAdmin) {
        EmptyState("Bu bölüm için yönetici yetkisi gerekir.")
        return
    }
    val feedback = LocalFeedback.current
    var confirmReset by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<AppRecord?>(null) }
    val stats = state.stats

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
            Column(
                Modifier
                    .fillMaxWidth()
                    .pixelFrame(fill = PixelColors.VioletDeep, border = PixelColors.Violet)
                    .padding(PixelSpacing.large),
                verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)
            ) {
                PixelBadge("YÖNETİCİ MODU", BadgeTone.ADMIN)
                Text(
                    "Bu bölüm yalnızca yönetici oturumunda oluşturulur. Kullanıcı rolünde sekme " +
                        "hiç eklenmez ve veri kaynağı yönetim isteklerini reddeder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE9DEFF)
                )
            }
        }

        item { SectionHeader("Özet", glyph = "gem") }
        if (stats == null) {
            item { EmptyState("İstatistik alınamadı.") }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    MetricTile(stats.totalApps.toString(), "TOPLAM KAYIT", PixelColors.Gold, Modifier.weight(1f))
                    MetricTile(stats.published.toString(), "YAYINDA", PixelColors.Mint, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    MetricTile(stats.drafts.toString(), "TASLAK", PixelColors.Amber, Modifier.weight(1f))
                    MetricTile(Format.count(stats.totalInstalls), "İNDİRME", PixelColors.Sky, Modifier.weight(1f))
                }
            }
            item {
                MetricTile(
                    Format.rating(stats.averageRating),
                    "ORTALAMA PUAN",
                    PixelColors.Gold,
                    Modifier.fillMaxWidth()
                )
            }

            if (stats.perCategory.isNotEmpty()) {
                item { SectionHeader("Türlere göre dağılım", glyph = "grid") }
                item {
                    val max = stats.perCategory.maxOf { it.second }.coerceAtLeast(1)
                    PixelPanel {
                        stats.perCategory.forEach { (name, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelColors.TextSecondary,
                                    modifier = Modifier.width(96.dp)
                                )
                                SegmentBar(
                                    ratio = count.toFloat() / max,
                                    segments = 10,
                                    onColor = PixelColors.Violet,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(PixelSpacing.small))
                                Text(
                                    count.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PixelColors.Gold
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Hızlı işlemler", glyph = "gear") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                PixelButton(
                    text = "YENİ KAYIT EKLE",
                    onClick = {
                        feedback.tap()
                        viewModel.push(Screen.AdminEditor(null))
                    },
                    glyph = "plus",
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    PixelButton(
                        text = "KULLANICILAR",
                        onClick = {
                            feedback.tap()
                            viewModel.push(Screen.AdminUsers)
                        },
                        tone = PixelButtonTone.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "SIFIRLA",
                        onClick = {
                            feedback.tap(Sfx.CANCEL)
                            confirmReset = true
                        },
                        tone = PixelButtonTone.DANGER,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item { SectionHeader("Katalog yönetimi", glyph = "bag") }
        if (state.apps.isEmpty()) {
            item { EmptyState("Katalog boş.") }
        } else {
            items(state.apps.sortedBy { it.title }, key = { it.id }) { app ->
                AdminAppRow(
                    app = app,
                    onEdit = {
                        feedback.tap()
                        viewModel.push(Screen.AdminEditor(app.id))
                    },
                    onTogglePublish = {
                        feedback.tap(Sfx.MOVE, 8)
                        viewModel.setPublished(app.id, !app.published)
                    },
                    onDelete = {
                        feedback.tap(Sfx.CANCEL)
                        pendingDelete = app
                    }
                )
            }
        }
    }

    if (confirmReset) {
        PixelModal(
            title = "Katalogu sıfırla",
            message = "Tüm düzenlemeler silinip fabrika kataloğu geri yüklenecek. Devam edilsin mi?",
            confirmLabel = "Sıfırla",
            onConfirm = {
                confirmReset = false
                viewModel.resetCatalog()
            },
            onDismiss = { confirmReset = false }
        )
    }

    pendingDelete?.let { app ->
        PixelModal(
            title = "Kaydı sil",
            message = "\"${app.title}\" kalıcı olarak silinsin mi?",
            confirmLabel = "Sil",
            onConfirm = {
                pendingDelete = null
                viewModel.deleteApp(app.id)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun AdminAppRow(
    app: AppRecord,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(
                fill = PixelColors.Surface,
                border = if (app.published) PixelColors.Outline else PixelColors.Amber
            )
            .padding(PixelSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelIconImage(app.iconSeed, app.palette, Modifier.size(44.dp))
            Spacer(Modifier.width(PixelSpacing.medium))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    app.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = PixelColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    "${app.category} · v${app.version} · ${Format.count(app.installs)} indirme",
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.TextTertiary
                )
                Text(
                    "id: ${app.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.OutlineSoft
                )
            }
            PixelBadge(
                if (app.published) "YAYINDA" else "TASLAK",
                if (app.published) BadgeTone.INSTALLED else BadgeTone.DRAFT
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
            PixelIconButton("pencil", PixelColors.Gold, onEdit, contentDescription = "Düzenle")
            PixelIconButton(
                glyph = if (app.published) "dotOn" else "dotOff",
                tint = if (app.published) PixelColors.Mint else PixelColors.TextTertiary,
                onClick = onTogglePublish,
                border = if (app.published) PixelColors.MintDeep else PixelColors.Outline,
                contentDescription = if (app.published) "Yayından kaldır" else "Yayına al"
            )
            PixelIconButton(
                glyph = "cross",
                tint = PixelColors.Rose,
                onClick = onDelete,
                border = PixelColors.RoseDeep,
                contentDescription = "Sil"
            )
        }
    }
}

/* ---- Kullanıcılar ---------------------------------------------------------------------------- */

@Composable
fun AdminUsersScreen(state: UiState) {
    if (!state.isAdmin) {
        EmptyState("Bu bölüm için yönetici yetkisi gerekir.")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = PixelSpacing.gutter,
            end = PixelSpacing.gutter,
            top = PixelSpacing.large,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        item { SectionHeader("Kullanıcılar", glyph = "hero") }
        item {
            Text(
                "Parolalar hiçbir zaman arayüze gönderilmez. Yerel kipte hesap tablosu sabittir; " +
                    "WordPress kipinde liste sitedeki kullanıcılardan gelir.",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelColors.TextSecondary
            )
        }
        if (state.users.isEmpty()) {
            item { EmptyState("Kullanıcı listesi alınamadı.") }
        } else {
            items(state.users, key = { it.username }) { user ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pixelFrame(
                            fill = PixelColors.Surface,
                            border = if (user.role.wire == "admin") PixelColors.Gold else PixelColors.Outline
                        )
                        .padding(PixelSpacing.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixelIconImage(
                        user.avatarSeed,
                        if (user.role.wire == "admin") "amber" else "azure",
                        Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(PixelSpacing.large))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            user.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = PixelColors.TextPrimary
                        )
                        Text(
                            "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PixelColors.Sky
                        )
                        if (user.joinedAt.isNotBlank()) {
                            Text(
                                "katılım ${user.joinedAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PixelColors.TextTertiary
                            )
                        }
                        if (user.note.isNotBlank()) {
                            Text(
                                user.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = PixelColors.TextTertiary
                            )
                        }
                    }
                    PixelBadge(
                        user.role.label,
                        if (user.role.wire == "admin") BadgeTone.ADMIN else BadgeTone.USER
                    )
                }
            }
        }
    }
}

/* ---- Kayıt düzenleyici ----------------------------------------------------------------------- */

private val contentRatings = listOf("3+", "7+", "12+", "16+", "18+")

@Composable
fun AdminEditorScreen(viewModel: StoreViewModel, state: UiState, appId: String?) {
    if (!state.isAdmin) {
        EmptyState("Bu bölüm için yönetici yetkisi gerekir.")
        return
    }
    val feedback = LocalFeedback.current
    val existing = appId?.let { state.app(it) }
    val categoryNames = state.categories.map { it.name }.ifEmpty { listOf("RPG", "Aksiyon", "Diğer") }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var developer by remember { mutableStateOf(existing?.developer ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: categoryNames.first()) }
    var version by remember { mutableStateOf(existing?.version ?: "1.0.0") }
    var sizeMb by remember { mutableStateOf((existing?.sizeMb ?: 25.0).toString()) }
    var rating by remember { mutableStateOf((existing?.rating ?: 4.0).toString()) }
    var ratingCount by remember { mutableStateOf((existing?.ratingCount ?: 0).toString()) }
    var contentRating by remember { mutableStateOf(existing?.contentRating ?: "7+") }
    var published by remember { mutableStateOf(existing?.published ?: true) }
    var shortDescription by remember { mutableStateOf(existing?.shortDescription ?: "") }
    var longDescription by remember { mutableStateOf(existing?.longDescription ?: "") }
    var tags by remember { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var downloadUrl by remember { mutableStateOf(existing?.downloadUrl ?: "") }
    var iconSeed by remember { mutableStateOf(existing?.iconSeed ?: "yeni-${Random.nextInt(1000, 9999)}") }
    var palette by remember { mutableStateOf(existing?.palette ?: "emerald") }
    val shots = remember { (existing?.screenshots ?: emptyList()).toMutableStateList() }

    fun save() {
        if (title.isBlank()) {
            viewModel.toast("Başlık boş olamaz", com.waifuhtr.pixelstore.MessageTone.ERROR)
            return
        }
        feedback.tap(Sfx.COIN)
        viewModel.saveApp(
            AppRecord(
                id = existing?.id ?: "",
                title = title.trim(),
                developer = developer.trim().ifBlank { "Bilinmeyen Stüdyo" },
                category = category,
                version = version.trim().ifBlank { "1.0.0" },
                sizeMb = sizeMb.toDoubleOrNull() ?: 25.0,
                rating = rating.toDoubleOrNull()?.coerceIn(0.0, 5.0) ?: 0.0,
                ratingCount = ratingCount.toIntOrNull() ?: 0,
                installs = existing?.installs ?: 0,
                contentRating = contentRating,
                published = published,
                updatedAt = existing?.updatedAt.orEmpty(),
                iconSeed = iconSeed.trim().ifBlank { "yeni" },
                palette = palette,
                tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(6),
                shortDescription = shortDescription.trim(),
                longDescription = longDescription.trim(),
                downloadUrl = downloadUrl.trim(),
                screenshots = shots.toList()
            )
        ) { viewModel.back() }
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
        item { SectionHeader(if (existing == null) "Yeni kayıt" else "Kaydı düzenle", glyph = "pencil") }

        item {
            PixelPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelIconImage(
                        iconSeed,
                        palette,
                        Modifier
                            .size(80.dp)
                            .pixelFrame(
                                fill = Color.Transparent,
                                border = PixelColors.Outline,
                                corner = PixelColors.Surface,
                                thickness = 2.dp
                            )
                    )
                    Spacer(Modifier.width(PixelSpacing.large))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                        Text(
                            "İKON ÖNİZLEME",
                            style = MaterialTheme.typography.labelMedium,
                            color = PixelColors.Gold
                        )
                        Text(
                            "İkon tohumdan çizilir; aynı tohum her zaman aynı ikonu verir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PixelColors.TextTertiary
                        )
                        PixelButton(
                            text = "RASTGELE",
                            onClick = {
                                feedback.tap(Sfx.MOVE, 8)
                                iconSeed = "seed-${Random.nextInt(10000, 99999)}"
                            },
                            tone = PixelButtonTone.GHOST,
                            glyph = "star"
                        )
                    }
                }
                LabeledField("İKON TOHUMU") {
                    PixelTextField(iconSeed, { iconSeed = it }, placeholder = "tohum")
                }
                LabeledField("PALET") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                        items(PixelArt.paletteNames) { name ->
                            PixelChip(
                                text = name,
                                selected = palette == name,
                                onClick = {
                                    feedback.tap(Sfx.MOVE, 8)
                                    palette = name
                                }
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Künye", glyph = "bag") }
        item {
            PixelPanel {
                LabeledField("BAŞLIK") {
                    PixelTextField(title, { title = it }, placeholder = "Oyun adı")
                }
                LabeledField("GELİŞTİRİCİ") {
                    PixelTextField(developer, { developer = it }, placeholder = "Stüdyo adı")
                }
                LabeledField("TÜR") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                        items(categoryNames) { name ->
                            PixelChip(name, category == name, {
                                feedback.tap(Sfx.MOVE, 8)
                                category = name
                            })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    LabeledField("SÜRÜM", Modifier.weight(1f)) {
                        PixelTextField(version, { version = it }, placeholder = "1.0.0")
                    }
                    LabeledField("BOYUT (MB)", Modifier.weight(1f)) {
                        PixelTextField(
                            sizeMb, { sizeMb = it },
                            placeholder = "25",
                            keyboardType = KeyboardType.Decimal
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    LabeledField("PUAN (0-5)", Modifier.weight(1f)) {
                        PixelTextField(
                            rating, { rating = it },
                            placeholder = "4.5",
                            keyboardType = KeyboardType.Decimal
                        )
                    }
                    LabeledField("OY SAYISI", Modifier.weight(1f)) {
                        PixelTextField(
                            ratingCount, { ratingCount = it },
                            placeholder = "0",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
                LabeledField("İÇERİK YAŞI") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                        items(contentRatings) { value ->
                            PixelChip(value, contentRating == value, {
                                feedback.tap(Sfx.MOVE, 8)
                                contentRating = value
                            })
                        }
                    }
                }
                PixelDivider()
                PixelSwitchRow(
                    label = "Mağazada yayında",
                    description = "Kapalıysa kayıt yalnızca yönetici oturumunda görünür.",
                    checked = published,
                    onCheckedChange = {
                        feedback.tap(Sfx.MOVE, 8)
                        published = it
                    }
                )
            }
        }

        item { SectionHeader("Metinler", glyph = "grid") }
        item {
            PixelPanel {
                LabeledField("KISA AÇIKLAMA", hint = "Kartlarda görünen tek cümle.") {
                    PixelTextField(shortDescription, { shortDescription = it }, placeholder = "Tek cümlelik tanıtım")
                }
                LabeledField("UZUN AÇIKLAMA", hint = "Paragraflar için satır atlayabilirsin.") {
                    PixelTextField(
                        longDescription, { longDescription = it },
                        placeholder = "Detaylı açıklama",
                        singleLine = false,
                        minLines = 5
                    )
                }
                LabeledField("ETİKETLER", hint = "Virgülle ayır, en fazla 6 etiket.") {
                    PixelTextField(tags, { tags = it }, placeholder = "Sıra tabanlı, Hikâye")
                }
                LabeledField("GELİŞTİRİCİ BAĞLANTISI", hint = "İsteğe bağlı, https ile başlamalı.") {
                    PixelTextField(
                        downloadUrl, { downloadUrl = it },
                        placeholder = "https://…",
                        keyboardType = KeyboardType.Uri
                    )
                }
            }
        }

        item {
            SectionHeader("Ekran görüntüleri", glyph = "gem")
        }
        item {
            Text(
                "En fazla 8 görsel. Her görsel sahne + cihaz tipinden çizilir; \"varyasyon\" düğmesi " +
                    "tohumu değiştirip yeni bir çizim üretir.",
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextTertiary
            )
        }
        itemsIndexed(shots) { index, shot ->
            ScreenshotEditorRow(
                shot = shot,
                palette = palette,
                onChange = { shots[index] = it },
                onRemove = {
                    feedback.tap(Sfx.CANCEL)
                    shots.removeAt(index)
                }
            )
        }
        item {
            PixelButton(
                text = if (shots.size >= 8) "SINIRA ULAŞILDI (8)" else "EKRAN GÖRÜNTÜSÜ EKLE",
                onClick = {
                    if (shots.size >= 8) return@PixelButton
                    feedback.tap()
                    shots.add(
                        Screenshot(
                            seed = "$iconSeed-${Random.nextInt(1000, 9999)}",
                            kind = PixelArt.ShotKind.PHONE,
                            scene = PixelArt.Scene.FIELD,
                            caption = ""
                        )
                    )
                },
                tone = PixelButtonTone.GHOST,
                glyph = "plus",
                enabled = shots.size < 8,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                PixelButton(
                    text = "VAZGEÇ",
                    onClick = {
                        feedback.tap(Sfx.CANCEL)
                        viewModel.back()
                    },
                    tone = PixelButtonTone.GHOST,
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = if (state.busy) "KAYDEDİLİYOR" else "KAYDET",
                    onClick = ::save,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScreenshotEditorRow(
    shot: Screenshot,
    palette: String,
    onChange: (Screenshot) -> Unit,
    onRemove: () -> Unit
) {
    val feedback = LocalFeedback.current
    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Outline)
            .padding(PixelSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ShotFrame(shot = shot, palette = palette, scale = 0.72f)
            Spacer(Modifier.width(PixelSpacing.medium))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                Text(
                    "SAHNE",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelColors.Gold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                    items(PixelArt.Scene.entries.toList()) { scene ->
                        PixelChip(scene.label, shot.scene == scene, {
                            feedback.tap(Sfx.MOVE, 8)
                            onChange(shot.copy(scene = scene))
                        })
                    }
                }
                Text(
                    "CİHAZ",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelColors.Gold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                    PixelArt.ShotKind.selectable.forEach { kind ->
                        PixelChip(kind.label, shot.kind == kind, {
                            feedback.tap(Sfx.MOVE, 8)
                            onChange(shot.copy(kind = kind))
                        })
                    }
                }
            }
        }
        PixelTextField(
            value = shot.caption,
            onValueChange = { onChange(shot.copy(caption = it)) },
            placeholder = "başlık (isteğe bağlı)"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
            PixelButton(
                text = "VARYASYON",
                onClick = {
                    feedback.tap(Sfx.MOVE, 8)
                    onChange(shot.copy(seed = shot.seed.substringBefore("#") + "#" + Random.nextInt(1000, 9999)))
                },
                tone = PixelButtonTone.GHOST,
                glyph = "star",
                modifier = Modifier.weight(1f)
            )
            PixelButton(
                text = "KALDIR",
                onClick = onRemove,
                tone = PixelButtonTone.DANGER,
                glyph = "cross",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
