package com.waifuhtr.pixelstore.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.Filters
import com.waifuhtr.pixelstore.SortMode
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.GameSummary
import com.waifuhtr.pixelstore.data.Term
import com.waifuhtr.pixelstore.ui.Format
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelChip
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.components.RemoteImage
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.components.StarRow
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

@Composable
fun StoreScreen(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    val listState = rememberLazyListState()

    // Filtreler kapalı başlar; liste hemen ekranın üstünde görünsün. Durum LazyColumn dışında
    // tutulur, yoksa satır ekrandan çıkınca açık/kapalı bilgisi kaybolur.
    var filtersOpen by rememberSaveable { mutableStateOf(false) }

    // Sonsuz kaydırma: son üç karta yaklaşınca sonraki sayfa istenir.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 3
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }.collect { if (it) viewModel.loadMore() }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = PixelSpacing.gutter,
                end = PixelSpacing.gutter,
                top = PixelSpacing.large,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.large)
        ) {
            item {
                SearchBar(
                    query = state.query,
                    onQueryChange = viewModel::setQuery,
                    onClear = {
                        feedback.tap(Sfx.CANCEL)
                        viewModel.clearSearch()
                    },
                    onSubmit = {
                        feedback.tap()
                        viewModel.submitSearch()
                    }
                )
            }

            val featured = state.featured
            if (featured != null && state.query.isBlank() && state.filters.active == 0) {
                item {
                    FeaturedCard(featured) {
                        feedback.tap()
                        viewModel.openGame(featured.id)
                    }
                }
            }

            if (state.taxonomies.genres.isNotEmpty() || state.taxonomies.platforms.isNotEmpty()) {
                item {
                    FilterSection(
                        viewModel = viewModel,
                        state = state,
                        expanded = filtersOpen,
                        onToggle = {
                            feedback.tap(Sfx.MOVE, 8)
                            filtersOpen = !filtersOpen
                        }
                    )
                }
            }

            item {
                SectionHeader(
                    title = if (state.totalGames > 0) "${Format.count(state.totalGames)} oyun" else "Oyunlar",
                    glyph = "bag"
                )
            }
            item { SortRow(state.sort) { viewModel.setSort(it) } }

            if (state.games.isEmpty() && !state.busy) {
                item { EmptyState("Aramanla eşleşen oyun yok.") }
            } else {
                items(state.games, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onClick = {
                            feedback.tap()
                            viewModel.openGame(game.id)
                        },
                        onFavorite = {
                            feedback.tap(Sfx.COIN)
                            viewModel.toggleFavorite(game.id)
                        }
                    )
                }
                if (state.loadingMore) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            SegmentBar(
                                ratio = 1f,
                                segments = 10,
                                onColor = PixelColors.MintDeep,
                                height = 6.dp,
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
        }

        // Canlı arama sonuçları listenin üstünde açılır.
        if (state.searchHits.isNotEmpty()) {
            LiveSearchOverlay(state, viewModel)
        }
    }
}

/* ---- Arama ----------------------------------------------------------------------------------- */

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(44.dp)
                .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Outline),
            contentAlignment = Alignment.Center
        ) {
            PixelGlyphImage("search", PixelColors.Mint, Modifier.size(18.dp))
        }
        Spacer(Modifier.width(PixelSpacing.small))
        PixelTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "oyun, geliştirici, etiket ara",
            imeAction = ImeAction.Search,
            onImeAction = onSubmit,
            modifier = Modifier.weight(1f)
        )
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(PixelSpacing.small))
            Box(
                Modifier
                    .size(44.dp)
                    .pixelFrame(fill = PixelColors.SurfaceHigh, border = PixelColors.RoseDeep)
                    .clickablePixel(onClear, "Aramayı temizle"),
                contentAlignment = Alignment.Center
            ) {
                PixelGlyphImage("cross", PixelColors.Rose, Modifier.size(14.dp))
            }
        }
    }
}

/**
 * Canlı arama katmanı: yazarken sunucudan gelen ilk sonuçları gösterir. Tam listeye geçmek için
 * klavyedeki arama tuşu kullanılır.
 */
@Composable
private fun LiveSearchOverlay(state: UiState, viewModel: StoreViewModel) {
    val feedback = LocalFeedback.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = PixelSpacing.gutter,
                end = PixelSpacing.gutter,
                top = 76.dp
            )
            .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Mint)
            .padding(PixelSpacing.small)
    ) {
        Text(
            "CANLI SONUÇLAR",
            style = MaterialTheme.typography.labelSmall,
            color = PixelColors.Mint,
            modifier = Modifier.padding(PixelSpacing.small)
        )
        state.searchHits.take(6).forEach { hit ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickablePixel({
                        feedback.tap()
                        viewModel.openGame(hit.id)
                    }, hit.title)
                    .padding(PixelSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteImage(
                    url = hit.coverUrl,
                    fallbackSeed = hit.id,
                    modifier = Modifier.size(width = 34.dp, height = 46.dp)
                )
                Spacer(Modifier.width(PixelSpacing.medium))
                Column(Modifier.weight(1f)) {
                    Text(
                        hit.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PixelColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hit.platform.isNotBlank()) {
                        Text(
                            hit.platform,
                            style = MaterialTheme.typography.bodySmall,
                            color = PixelColors.Sky
                        )
                    }
                }
                if (hit.rating > 0) {
                    Text(
                        Format.rating(hit.rating),
                        style = MaterialTheme.typography.labelMedium,
                        color = PixelColors.Gold
                    )
                }
            }
        }
    }
}

/* ---- Filtreler -------------------------------------------------------------------------------- */

/**
 * Katlanabilir filtre bölümü.
 *
 * Kapalıyken tek satır yer kaplar, böylece oyun listesi ekranın üstünde kalır. Seçili filtreler
 * kapalıyken de rozet olarak görünür; kullanıcı neyin süzüldüğünü açmadan görebilir.
 */
@Composable
private fun FilterSection(
    viewModel: StoreViewModel,
    state: UiState,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val feedback = LocalFeedback.current
    val active = state.filters.active

    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(
                fill = PixelColors.Surface,
                border = if (active > 0) PixelColors.Mint else PixelColors.Outline
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickablePixel(onToggle, if (expanded) "Filtreleri kapat" else "Filtreleri aç")
                .padding(horizontal = PixelSpacing.medium, vertical = PixelSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelGlyphImage("grid", PixelColors.Mint, Modifier.size(14.dp))
            Spacer(Modifier.width(PixelSpacing.small))
            Text(
                if (active > 0) "FİLTRE ($active)" else "FİLTRELE",
                style = MaterialTheme.typography.labelMedium,
                color = if (active > 0) PixelColors.Mint else PixelColors.TextSecondary
            )
            Spacer(Modifier.weight(1f))
            if (active > 0) {
                Row(
                    Modifier
                        .clickablePixel({
                            feedback.tap(Sfx.CANCEL)
                            viewModel.clearFilters()
                        }, "Filtreleri temizle")
                        .padding(horizontal = PixelSpacing.small, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixelGlyphImage("cross", PixelColors.Rose, Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "TEMİZLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PixelColors.Rose
                    )
                }
                Spacer(Modifier.width(PixelSpacing.small))
            }
            PixelGlyphImage(
                if (expanded) "up" else "down",
                PixelColors.Gold,
                Modifier.size(14.dp)
            )
        }

        if (!expanded && active > 0) {
            // Kapalı özet: hangi terimlerin seçili olduğu.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PixelSpacing.medium,
                        end = PixelSpacing.medium,
                        bottom = PixelSpacing.small
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeFilterLabels(state).forEach { label ->
                    PixelBadge(label, BadgeTone.CATEGORY)
                }
            }
        }

        if (expanded) {
            Column(
                Modifier.padding(
                    start = PixelSpacing.medium,
                    end = PixelSpacing.medium,
                    bottom = PixelSpacing.medium
                ),
                verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)
            ) {
                TermRow(
                    label = "Tür",
                    terms = state.taxonomies.genres,
                    selected = state.filters.genre,
                    onSelect = { slug -> viewModel.setFilter { it.copy(genre = slug) } }
                )
                TermRow(
                    label = "Platform",
                    terms = state.taxonomies.platforms,
                    selected = state.filters.platform,
                    onSelect = { slug -> viewModel.setFilter { it.copy(platform = slug) } }
                )
                TermRow(
                    label = "Dil",
                    terms = state.taxonomies.languages,
                    selected = state.filters.language,
                    onSelect = { slug -> viewModel.setFilter { it.copy(language = slug) } }
                )
                TermRow(
                    label = "Durum",
                    terms = state.taxonomies.statuses,
                    selected = state.filters.status,
                    onSelect = { slug -> viewModel.setFilter { it.copy(status = slug) } }
                )
            }
        }
    }
}

/** Seçili filtrelerin okunabilir adları; slug yerine terimin gerçek adı gösterilir. */
private fun activeFilterLabels(state: UiState): List<String> {
    val labels = mutableListOf<String>()
    fun add(terms: List<Term>, slug: String?) {
        val chosen = slug ?: return
        labels += terms.firstOrNull { it.slug == chosen }?.name ?: chosen
    }
    add(state.taxonomies.genres, state.filters.genre)
    add(state.taxonomies.platforms, state.filters.platform)
    add(state.taxonomies.languages, state.filters.language)
    add(state.taxonomies.statuses, state.filters.status)
    return labels
}

@Composable
private fun TermRow(
    label: String,
    terms: List<Term>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    if (terms.isEmpty()) return
    val feedback = LocalFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = PixelColors.TextTertiary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
            items(terms, key = { it.slug }) { term ->
                val isSelected = selected == term.slug
                PixelChip(
                    text = if (term.icon.isNotBlank()) "${term.icon} ${term.name}" else "${term.name} (${term.count})",
                    selected = isSelected,
                    onClick = {
                        feedback.tap(Sfx.MOVE, 8)
                        onSelect(if (isSelected) null else term.slug)
                    }
                )
            }
        }
    }
}

@Composable
private fun SortRow(current: SortMode, onSelect: (SortMode) -> Unit) {
    val feedback = LocalFeedback.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
        items(SortMode.entries.toList()) { mode ->
            val selected = mode == current
            Column(
                modifier = Modifier.clickablePixel({
                    feedback.tap(Sfx.MOVE, 8)
                    onSelect(mode)
                }, mode.label),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) PixelColors.Gold else PixelColors.TextTertiary,
                    modifier = Modifier.padding(horizontal = PixelSpacing.small, vertical = 6.dp)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (selected) PixelColors.Gold else Color.Transparent)
                )
            }
        }
    }
}

/* ---- Kartlar ---------------------------------------------------------------------------------- */

/** Öne çıkan oyun: geniş kapak + bilgi şeridi. */
@Composable
private fun FeaturedCard(game: GameSummary, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Gold)
            .clickablePixel(onClick, game.title)
    ) {
        RemoteImage(
            url = game.coverUrl,
            fallbackSeed = game.id,
            palette = "azure",
            contentDescription = game.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(PixelColors.Ink)
                .padding(PixelSpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PixelSpacing.tiny)) {
                Text(
                    if (game.editorsChoice) "EDİTÖRÜN SEÇİMİ" else "ÖNE ÇIKAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelColors.Mint
                )
                Text(
                    game.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = PixelColors.Gold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (game.developer.isNotBlank()) {
                    Text(
                        game.developer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelColors.TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(PixelSpacing.medium))
            PixelGlyphImage("right", PixelColors.Gold, Modifier.size(18.dp))
        }
    }
}

/**
 * Oyun kartı.
 *
 * Bilgi dört kademeye ayrılır: başlık, geliştirici, tanıtım, ölçüler. Kapak gerçek görseldir;
 * dikey oyun kapağı oranı (2:3) korunur.
 */
@Composable
fun GameCard(
    game: GameSummary,
    onClick: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pixelFrame(
                fill = PixelColors.Surface,
                border = if (game.published) PixelColors.Outline else PixelColors.Amber
            )
            .clickablePixel(onClick, game.title)
            .padding(PixelSpacing.medium),
        verticalAlignment = Alignment.Top
    ) {
        RemoteImage(
            url = game.coverUrl,
            fallbackSeed = game.id,
            contentDescription = game.title,
            modifier = Modifier
                .size(width = 74.dp, height = 104.dp)
                .pixelFrame(
                    fill = Color.Transparent,
                    border = PixelColors.OutlineSoft,
                    corner = PixelColors.Surface,
                    thickness = 2.dp
                )
        )
        Spacer(Modifier.width(PixelSpacing.medium))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                game.title,
                style = MaterialTheme.typography.titleMedium,
                color = PixelColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (game.developer.isNotBlank()) {
                Text(game.developer, style = MaterialTheme.typography.bodySmall, color = PixelColors.Sky)
            }
            if (game.excerpt.isNotBlank()) {
                Text(
                    game.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (game.rating > 0) {
                    StarRow(game.rating, starSize = 10.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Format.rating(game.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = PixelColors.Gold
                    )
                    Spacer(Modifier.width(PixelSpacing.medium))
                }
                Text(
                    "${Format.count(game.downloadCount)} indirme",
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.TextTertiary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (game.platform.isNotBlank()) PixelBadge(game.platform, BadgeTone.CATEGORY)
                if (game.sizeLabel.isNotBlank()) PixelBadge(game.sizeLabel, BadgeTone.NEUTRAL)
                if (!game.published) PixelBadge("TASLAK", BadgeTone.DRAFT)
            }
        }
        if (onFavorite != null) {
            Spacer(Modifier.width(PixelSpacing.small))
            Box(
                Modifier
                    .size(36.dp)
                    .clickablePixel(onFavorite, if (game.favorited) "İstek listesinden çıkar" else "İstek listesine ekle"),
                contentAlignment = Alignment.Center
            ) {
                PixelGlyphImage(
                    "heart",
                    if (game.favorited) PixelColors.Rose else PixelColors.OutlineSoft,
                    Modifier.size(18.dp)
                )
            }
        }
    }
}
