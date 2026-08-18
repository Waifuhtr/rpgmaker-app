package com.waifuhtr.pixelstore.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.MessageTone
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.GameForm
import com.waifuhtr.pixelstore.data.GameSummary
import com.waifuhtr.pixelstore.data.SpecSet
import com.waifuhtr.pixelstore.ui.Format
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
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
import com.waifuhtr.pixelstore.ui.components.RemoteImage
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

/* ---- Yönetim panosu -------------------------------------------------------------------------- */

@Composable
fun AdminScreen(viewModel: StoreViewModel, state: UiState) {
    if (!state.isAdmin) {
        EmptyState("Bu bölüm için yönetici yetkisi gerekir.")
        return
    }
    val feedback = LocalFeedback.current
    var pendingDelete by remember { mutableStateOf<GameSummary?>(null) }
    val stats = state.stats

    LaunchedEffect(Unit) { viewModel.loadAdminData() }

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
            Column(
                Modifier
                    .fillMaxWidth()
                    .pixelFrame(fill = PixelColors.VioletDeep, border = PixelColors.Violet)
                    .padding(PixelSpacing.large),
                verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)
            ) {
                PixelBadge("YÖNETİCİ MODU", BadgeTone.ADMIN)
                Text(
                    "Buradan eklediğin veya düzenlediğin her şey doğrudan siteye yazılır. " +
                        "Yetki WordPress rolünden gelir; kullanıcı rolünde bu sekme hiç oluşturulmaz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE9DEFF)
                )
            }
        }

        item { SectionHeader("Site özeti", glyph = "gem") }
        if (stats == null) {
            item { EmptyState("İstatistik alınamadı.") }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    MetricTile(stats.totalGames.toString(), "TOPLAM OYUN", PixelColors.Gold, Modifier.weight(1f))
                    MetricTile(stats.published.toString(), "YAYINDA", PixelColors.Mint, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    MetricTile(stats.drafts.toString(), "TASLAK", PixelColors.Amber, Modifier.weight(1f))
                    MetricTile(Format.count(stats.totalDownloads), "İNDİRME", PixelColors.Sky, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    MetricTile(Format.count(stats.totalViews), "GÖRÜNTÜLENME", PixelColors.Violet, Modifier.weight(1f))
                    MetricTile(Format.rating(stats.averageRating), "ORT. PUAN", PixelColors.Gold, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    MetricTile(Format.count(stats.reviewCount), "YORUM", PixelColors.Mint, Modifier.weight(1f))
                    MetricTile(Format.count(stats.userCount), "KULLANICI", PixelColors.Sky, Modifier.weight(1f))
                }
            }
            if (stats.reportCount > 0) {
                item {
                    MetricTile(
                        Format.count(stats.reportCount),
                        "AÇIK HATA RAPORU (wp-admin)",
                        PixelColors.Rose,
                        Modifier.fillMaxWidth()
                    )
                }
            }
            if (stats.perGenre.isNotEmpty()) {
                item { SectionHeader("Türlere göre dağılım", glyph = "grid") }
                item {
                    val max = stats.perGenre.maxOf { it.second }.coerceAtLeast(1)
                    PixelPanel {
                        stats.perGenre.forEach { (name, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelColors.TextSecondary,
                                    modifier = Modifier.width(96.dp),
                                    maxLines = 1
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
                    text = "YENİ OYUN EKLE",
                    onClick = {
                        feedback.tap()
                        viewModel.push(Screen.AdminEditor(null))
                    },
                    glyph = "plus",
                    modifier = Modifier.fillMaxWidth()
                )
                PixelButton(
                    text = "KULLANICILAR",
                    onClick = {
                        feedback.tap()
                        viewModel.push(Screen.AdminUsers)
                    },
                    tone = PixelButtonTone.GHOST,
                    glyph = "hero",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { SectionHeader("Katalog yönetimi", glyph = "bag") }
        item {
            Text(
                "Liste mağaza sekmesindeki filtreyi izler; taslaklar da burada görünür.",
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextTertiary
            )
        }
        if (state.games.isEmpty()) {
            item { EmptyState("Oyun listesi boş.") }
        } else {
            items(state.games, key = { it.id }) { game ->
                AdminGameRow(
                    game = game,
                    onEdit = {
                        feedback.tap()
                        viewModel.push(Screen.AdminEditor(game.id))
                    },
                    onTogglePublish = {
                        feedback.tap(Sfx.MOVE, 8)
                        viewModel.setPublished(game.id, !game.published)
                    },
                    onDelete = {
                        feedback.tap(Sfx.CANCEL)
                        pendingDelete = game
                    }
                )
            }
        }
    }

    pendingDelete?.let { game ->
        PixelModal(
            title = "Oyunu sil",
            message = "\"${game.title}\" çöp kutusuna taşınacak. wp-admin'den geri alabilirsin.",
            confirmLabel = "Çöpe taşı",
            onConfirm = {
                pendingDelete = null
                viewModel.deleteGame(game.id)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun AdminGameRow(
    game: GameSummary,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(
                fill = PixelColors.Surface,
                border = if (game.published) PixelColors.Outline else PixelColors.Amber
            )
            .padding(PixelSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                url = game.coverUrl,
                fallbackSeed = game.id,
                modifier = Modifier.size(width = 40.dp, height = 56.dp)
            )
            Spacer(Modifier.width(PixelSpacing.medium))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = PixelColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    listOfNotNull(
                        game.platform.takeIf { it.isNotBlank() },
                        game.version.takeIf { it.isNotBlank() },
                        "${Format.count(game.downloadCount)} indirme"
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.TextTertiary
                )
                Text(
                    game.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.OutlineSoft,
                    maxLines = 1
                )
            }
            PixelBadge(
                if (game.published) "YAYINDA" else "TASLAK",
                if (game.published) BadgeTone.INSTALLED else BadgeTone.DRAFT
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
            PixelIconButton("pencil", PixelColors.Gold, onEdit, contentDescription = "Düzenle")
            PixelIconButton(
                glyph = if (game.published) "dotOn" else "dotOff",
                tint = if (game.published) PixelColors.Mint else PixelColors.TextTertiary,
                onClick = onTogglePublish,
                border = if (game.published) PixelColors.MintDeep else PixelColors.Outline,
                contentDescription = if (game.published) "Yayından kaldır" else "Yayına al"
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
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = PixelSpacing.gutter,
            end = PixelSpacing.gutter,
            top = PixelSpacing.large,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        // Ekran adı üst çubukta yazıyor; tekrar edilmez.
        item {
            Text(
                "Liste sitedeki WordPress kullanıcılarıdır. Parola bilgisi hiçbir zaman uygulamaya " +
                    "gönderilmez; kullanıcı ekleme/silme wp-admin'den yapılır.",
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
                        .padding(PixelSpacing.medium),
                    // Rozet üst hizada durur: alt satır iki satıra taştığında adla çakışmaz.
                    verticalAlignment = Alignment.Top
                ) {
                    RemoteImage(
                        url = user.avatarUrl,
                        fallbackSeed = user.username,
                        palette = if (user.role.wire == "admin") "amber" else "azure",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(PixelSpacing.medium))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                user.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = PixelColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(PixelSpacing.small))
                            PixelBadge(
                                user.role.label,
                                if (user.role.wire == "admin") BadgeTone.ADMIN else BadgeTone.USER
                            )
                        }
                        Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = PixelColors.Sky)
                        Text(
                            listOfNotNull(
                                user.roleLabel.takeIf { it.isNotBlank() },
                                user.joinedAt.takeIf { it.isNotBlank() }?.let { "katılım $it" },
                                "${user.favoriteCount} istek"
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = PixelColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

/* ---- Oyun düzenleyici ------------------------------------------------------------------------ */

@Composable
fun AdminEditorScreen(viewModel: StoreViewModel, state: UiState, gameId: String?) {
    if (!state.isAdmin) {
        EmptyState("Bu bölüm için yönetici yetkisi gerekir.")
        return
    }
    val feedback = LocalFeedback.current

    // Düzenlemede kaydı tam haliyle çek; yeni kayıtta boş formla başla.
    LaunchedEffect(gameId) {
        if (gameId != null && state.detail?.id != gameId) viewModel.loadDetail(gameId)
    }

    val existing = state.detail?.takeIf { it.id == gameId }
    if (gameId != null && existing == null) {
        EmptyState(if (state.detailLoading) "Yükleniyor…" else "Kayıt bulunamadı.")
        return
    }

    // Form durumu yalnızca kayıt değiştiğinde sıfırlanır; yazarken kaybolmaz.
    var form by remember(existing?.id ?: "new") {
        mutableStateOf(existing?.let { GameForm.from(it) } ?: GameForm.empty())
    }

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && gameId != null) viewModel.uploadCover(gameId, uri)
    }
    val pickScreenshot = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && gameId != null) viewModel.uploadScreenshot(gameId, uri)
    }

    fun save() {
        if (form.title.isBlank()) {
            viewModel.toast("Başlık boş olamaz.", MessageTone.ERROR)
            return
        }
        feedback.tap(Sfx.COIN)
        viewModel.saveGame(form, gameId) { viewModel.back() }
    }

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
        // Ekran adı üst çubukta yazıyor; burada tekrar yerine ne olacağı anlatılır.
        item {
            Text(
                if (gameId == null) {
                    "Kaydettiğinde site üzerinde yeni bir oyun kaydı oluşur."
                } else {
                    "Değiştirdiğin her alan kaydettiğinde doğrudan siteye yazılır."
                },
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextTertiary
            )
        }

        // Görseller yalnızca kayıt oluşturulduktan sonra yüklenebilir (sunucuda bir kayda bağlanır).
        item {
            PixelPanel {
                Text("GÖRSELLER", style = MaterialTheme.typography.labelMedium, color = PixelColors.Gold)
                if (gameId == null) {
                    Text(
                        "Kapak ve ekran görüntülerini yüklemek için kaydı önce oluştur, sonra tekrar düzenle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelColors.TextTertiary
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(
                            url = existing?.summary?.coverUrl.orEmpty(),
                            fallbackSeed = gameId,
                            modifier = Modifier.size(width = 74.dp, height = 104.dp)
                        )
                        Spacer(Modifier.width(PixelSpacing.medium))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                            Text(
                                "Kapak görseli",
                                style = MaterialTheme.typography.titleSmall,
                                color = PixelColors.TextPrimary
                            )
                            Text(
                                "Yüklenen görsel sitenin medya kütüphanesine girer ve öne çıkan görsel olur.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PixelColors.TextTertiary
                            )
                            PixelButton(
                                text = "KAPAK SEÇ",
                                onClick = {
                                    feedback.tap(Sfx.OPEN)
                                    pickCover.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                tone = PixelButtonTone.GHOST,
                                glyph = "pencil",
                                enabled = !state.busy
                            )
                        }
                    }

                    PixelDivider()
                    Text(
                        "Ekran görüntüleri (${existing?.screenshots?.size ?: 0})",
                        style = MaterialTheme.typography.titleSmall,
                        color = PixelColors.TextPrimary
                    )
                    if (!existing?.screenshots.isNullOrEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                            itemsIndexed(existing!!.screenshots) { index, shot ->
                                // Kaldırma düğmesi küçük bir köşe işareti; küçük önizlemenin
                                // altında tam boy düğme, görselden daha çok yer kaplıyordu.
                                Box {
                                    RemoteImage(
                                        url = shot.url,
                                        fallbackSeed = "$gameId-$index",
                                        modifier = Modifier.size(width = 108.dp, height = 61.dp)
                                    )
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .size(22.dp)
                                            .pixelFrame(
                                                fill = PixelColors.RoseDeep,
                                                border = PixelColors.Rose,
                                                thickness = 2.dp
                                            )
                                            .clickablePixel({
                                                feedback.tap(Sfx.CANCEL)
                                                viewModel.deleteScreenshot(gameId, shot.id)
                                            }, "Ekran görüntüsünü kaldır"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PixelGlyphImage("cross", Color.White, Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                    PixelButton(
                        text = "EKRAN GÖRÜNTÜSÜ EKLE",
                        onClick = {
                            feedback.tap(Sfx.OPEN)
                            pickScreenshot.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        tone = PixelButtonTone.GHOST,
                        glyph = "plus",
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item { SectionHeader("Künye", glyph = "bag") }
        item {
            PixelPanel {
                LabeledField("BAŞLIK") {
                    PixelTextField(form.title, { form = form.copy(title = it) }, placeholder = "Oyun adı")
                }
                LabeledField("ALT BAŞLIK") {
                    PixelTextField(form.subtitle, { form = form.copy(subtitle = it) }, placeholder = "Tam Türkçe")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    LabeledField("SÜRÜM", Modifier.weight(1f)) {
                        PixelTextField(form.version, { form = form.copy(version = it) }, placeholder = "v1.0")
                    }
                    LabeledField("BOYUT", Modifier.weight(1f)) {
                        PixelTextField(form.sizeLabel, { form = form.copy(sizeLabel = it) }, placeholder = "2.4 GB")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                    LabeledField("ÇIKIŞ TARİHİ", Modifier.weight(1f)) {
                        PixelTextField(form.releaseDate, { form = form.copy(releaseDate = it) }, placeholder = "2026-01-01")
                    }
                    LabeledField("YAŞ SINIRI", Modifier.weight(1f)) {
                        PixelTextField(form.ageRating, { form = form.copy(ageRating = it) }, placeholder = "+18")
                    }
                }
                LabeledField("GELİŞTİRİCİ") {
                    PixelTextField(form.developer, { form = form.copy(developer = it) }, placeholder = "Stüdyo adı")
                }
                LabeledField("YAYINCI") {
                    PixelTextField(form.publisher, { form = form.copy(publisher = it) }, placeholder = "Yayıncı")
                }
                LabeledField("DURUM") {
                    PixelTextField(form.status, { form = form.copy(status = it) }, placeholder = "Tamamlandı")
                }
                LabeledField("LİSANS") {
                    PixelTextField(form.licenseType, { form = form.copy(licenseType = it) }, placeholder = "Ücretsiz")
                }
                PixelDivider()
                PixelSwitchRow(
                    label = "Sitede yayında",
                    description = "Kapalıysa taslak olarak kaydedilir, kullanıcılar göremez.",
                    checked = form.published,
                    onCheckedChange = { form = form.copy(published = it) }
                )
                PixelSwitchRow(
                    label = "Öne çıkan",
                    description = "Mağaza başındaki büyük kartta gösterilir.",
                    checked = form.featured,
                    onCheckedChange = { form = form.copy(featured = it) }
                )
                PixelSwitchRow(
                    label = "Editörün seçimi",
                    checked = form.editorsChoice,
                    onCheckedChange = { form = form.copy(editorsChoice = it) }
                )
                PixelSwitchRow(
                    label = "Çok oyunculu",
                    checked = form.multiplayer,
                    onCheckedChange = { form = form.copy(multiplayer = it) }
                )
                PixelSwitchRow(
                    label = "Kumanda desteği",
                    checked = form.controller,
                    onCheckedChange = { form = form.copy(controller = it) }
                )
            }
        }

        item { SectionHeader("Sınıflandırma", glyph = "grid") }
        item {
            PixelPanel {
                TermPicker(
                    label = "TÜRLER",
                    available = state.taxonomies.genres.map { it.name },
                    selected = form.genres,
                    onChange = { form = form.copy(genres = it) }
                )
                TermPicker(
                    label = "PLATFORMLAR",
                    available = state.taxonomies.platforms.map { it.name },
                    selected = form.platforms,
                    onChange = { form = form.copy(platforms = it) }
                )
                TermPicker(
                    label = "DİLLER",
                    available = state.taxonomies.languages.map { it.name },
                    selected = form.languages,
                    onChange = { form = form.copy(languages = it) }
                )
                LabeledField("ETİKETLER", hint = "Virgülle ayır. Sitede yoksa oluşturulur.") {
                    PixelTextField(
                        value = form.tags.joinToString(", "),
                        onValueChange = { text ->
                            form = form.copy(
                                tags = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        },
                        placeholder = "gerilim, bulmaca"
                    )
                }
            }
        }

        item { SectionHeader("Metinler", glyph = "pencil") }
        item {
            PixelPanel {
                LabeledField("KISA TANITIM", hint = "Kartlarda görünen özet.") {
                    PixelTextField(
                        form.excerpt, { form = form.copy(excerpt = it) },
                        placeholder = "Tek cümlelik tanıtım",
                        singleLine = false,
                        minLines = 2,
                        imeAction = ImeAction.Default
                    )
                }
                LabeledField("AÇIKLAMA") {
                    PixelTextField(
                        form.description, { form = form.copy(description = it) },
                        placeholder = "Oyun hakkında",
                        singleLine = false,
                        minLines = 5,
                        imeAction = ImeAction.Default
                    )
                }
                LabeledField("DEĞİŞİKLİK GÜNLÜĞÜ") {
                    PixelTextField(
                        form.changelog, { form = form.copy(changelog = it) },
                        placeholder = "Bu sürümde neler değişti",
                        singleLine = false,
                        minLines = 3,
                        imeAction = ImeAction.Default
                    )
                }
                LabeledField("KURULUM REHBERİ") {
                    PixelTextField(
                        form.installGuide, { form = form.copy(installGuide = it) },
                        placeholder = "Kurulum adımları",
                        singleLine = false,
                        minLines = 3,
                        imeAction = ImeAction.Default
                    )
                }
            }
        }

        item { SectionHeader("İndirme", glyph = "download") }
        item {
            PixelPanel {
                LabeledField("ANA İNDİRME LİNKİ", hint = "https:// ile başlamalı.") {
                    PixelTextField(
                        form.downloadUrl, { form = form.copy(downloadUrl = it) },
                        placeholder = "https://…",
                        keyboardType = KeyboardType.Uri
                    )
                }
                LabeledField("ALTERNATİF LİNK") {
                    PixelTextField(
                        form.mirrorUrl, { form = form.copy(mirrorUrl = it) },
                        placeholder = "https://…",
                        keyboardType = KeyboardType.Uri
                    )
                }
                LabeledField("ARŞİV ŞİFRESİ") {
                    PixelTextField(form.archivePassword, { form = form.copy(archivePassword = it) }, placeholder = "şifre")
                }
                LabeledField("FRAGMAN (YOUTUBE)") {
                    PixelTextField(
                        form.trailerUrl, { form = form.copy(trailerUrl = it) },
                        placeholder = "https://youtu.be/…",
                        keyboardType = KeyboardType.Uri
                    )
                }
            }
        }

        item { SectionHeader("Sistem gereksinimleri", glyph = "gear") }
        item {
            PixelPanel {
                Text("MİNİMUM", style = MaterialTheme.typography.labelMedium, color = PixelColors.Gold)
                SpecFields(form.minimum) { form = form.copy(minimum = it) }
                PixelDivider()
                Text("ÖNERİLEN", style = MaterialTheme.typography.labelMedium, color = PixelColors.Mint)
                SpecFields(form.recommended) { form = form.copy(recommended = it) }
            }
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

/**
 * Terim seçici: sitede var olan terimler çip olarak listelenir, çoklu seçim yapılır.
 * Sitede olmayan bir terim eklemek gerekirse alttaki serbest alan kullanılır.
 */
@Composable
private fun TermPicker(
    label: String,
    available: List<String>,
    selected: List<String>,
    onChange: (List<String>) -> Unit
) {
    val feedback = LocalFeedback.current
    var custom by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = PixelColors.Gold)
        if (available.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                items(available, key = { it }) { name ->
                    val isSelected = selected.contains(name)
                    PixelChip(
                        text = name,
                        selected = isSelected,
                        onClick = {
                            feedback.tap(Sfx.MOVE, 8)
                            onChange(if (isSelected) selected - name else selected + name)
                        }
                    )
                }
            }
        }
        // Seçili ama listede olmayanlar (yeni eklenen terimler) de görünsün.
        val extras = selected.filterNot { available.contains(it) }
        if (extras.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                items(extras, key = { it }) { name ->
                    PixelChip(text = "$name ✕", selected = true, onClick = { onChange(selected - name) })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelTextField(
                value = custom,
                onValueChange = { custom = it },
                placeholder = "yeni terim ekle",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(PixelSpacing.small))
            PixelIconButton(
                glyph = "plus",
                tint = PixelColors.Mint,
                onClick = {
                    val clean = custom.trim()
                    if (clean.isNotEmpty() && !selected.contains(clean)) {
                        onChange(selected + clean)
                        custom = ""
                    }
                },
                border = PixelColors.MintDeep,
                contentDescription = "Terim ekle"
            )
        }
    }
}

@Composable
private fun SpecFields(specs: SpecSet, onChange: (SpecSet) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
        LabeledField("İŞLETİM SİSTEMİ") {
            PixelTextField(specs.os, { onChange(specs.copy(os = it)) }, placeholder = "Android 9 / Windows 10")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
            LabeledField("İŞLEMCİ", Modifier.weight(1f)) {
                PixelTextField(specs.cpu, { onChange(specs.copy(cpu = it)) }, placeholder = "i5 / Snapdragon")
            }
            LabeledField("BELLEK", Modifier.weight(1f)) {
                PixelTextField(specs.ram, { onChange(specs.copy(ram = it)) }, placeholder = "4 GB")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
            LabeledField("EKRAN KARTI", Modifier.weight(1f)) {
                PixelTextField(specs.gpu, { onChange(specs.copy(gpu = it)) }, placeholder = "GTX 1050")
            }
            LabeledField("DEPOLAMA", Modifier.weight(1f)) {
                PixelTextField(specs.storage, { onChange(specs.copy(storage = it)) }, placeholder = "5 GB")
            }
        }
    }
}
