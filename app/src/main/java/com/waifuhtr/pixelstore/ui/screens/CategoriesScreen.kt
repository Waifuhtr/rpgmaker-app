package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.Term
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

/**
 * Türler ekranı. Terimler siteden gelir (`game_genre`, `game_platform`, `game_language`,
 * `game_status`); sayılar sitedeki gerçek kayıt sayılarıdır. Bir karo seçilince mağaza sekmesi o
 * filtreyle açılır.
 */
@Composable
fun CategoriesScreen(viewModel: StoreViewModel, state: UiState) {
    val tax = state.taxonomies
    if (tax.genres.isEmpty() && tax.platforms.isEmpty() &&
        tax.languages.isEmpty() && tax.statuses.isEmpty()
    ) {
        EmptyState("Tür listesi alınamadı.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = PixelSpacing.gutter,
            end = PixelSpacing.gutter,
            top = PixelSpacing.large,
            bottom = 96.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(
                    "Bir karo seç; mağaza o filtreyle açılır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextSecondary
                )
                Spacer(Modifier.height(PixelSpacing.small))
            }
        }

        group(
            title = "Türler",
            headerGlyph = "grid",
            terms = tax.genres,
            accent = PixelColors.Gold,
            onPick = { term ->
                viewModel.setFilter { it.copy(genre = term.slug, platform = null) }
                viewModel.openTab(Screen.Store)
            }
        )
        group(
            title = "Platformlar",
            headerGlyph = "gem",
            terms = tax.platforms,
            accent = PixelColors.Sky,
            onPick = { term ->
                viewModel.setFilter { it.copy(platform = term.slug, genre = null) }
                viewModel.openTab(Screen.Store)
            }
        )
        group(
            title = "Diller",
            headerGlyph = "hero",
            terms = tax.languages,
            accent = PixelColors.Mint,
            onPick = { term ->
                viewModel.setFilter { it.copy(language = term.slug) }
                viewModel.openTab(Screen.Store)
            }
        )
        group(
            title = "Durum",
            headerGlyph = "check",
            terms = tax.statuses,
            accent = PixelColors.Violet,
            onPick = { term ->
                viewModel.setFilter { it.copy(status = term.slug) }
                viewModel.openTab(Screen.Store)
            }
        )
    }
}

/** Bir taksonomi bölümü: başlık satırı + iki kolonlu karolar. Terim yoksa hiç eklenmez. */
private fun LazyGridScope.group(
    title: String,
    headerGlyph: String,
    terms: List<Term>,
    accent: Color,
    onPick: (Term) -> Unit
) {
    if (terms.isEmpty()) return
    item(span = { GridItemSpan(2) }) {
        Column {
            SectionHeader(title, glyph = headerGlyph)
            Spacer(Modifier.height(PixelSpacing.small))
        }
    }
    val max = (terms.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    items(terms, key = { "$title-${it.slug}" }) { term ->
        TermTile(term, max, accent, onClick = { onPick(term) })
    }
}

@Composable
private fun TermTile(
    term: Term,
    maxCount: Int,
    accent: Color,
    onClick: () -> Unit
) {
    val feedback = LocalFeedback.current
    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Outline)
            .clickablePixel({
                feedback.tap()
                onClick()
            }, term.name)
            .padding(PixelSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)
    ) {
        // Sitede terime emoji atanmışsa onu göster. Yoksa terimin baş harfi çizilir: 5x5 nesne
        // glifleri (kılıç, kalkan) bu boyutta birbirinden ayırt edilemiyor; harf hem okunur hem
        // terime özgü.
        Box(
            Modifier
                .size(38.dp)
                .pixelFrame(fill = PixelColors.SurfaceHigh, border = accent, thickness = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (term.icon.isNotBlank()) {
                Text(term.icon, style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    term.name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = accent
                )
            }
        }
        Text(
            term.name,
            style = MaterialTheme.typography.titleSmall,
            color = PixelColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${term.count} oyun",
            style = MaterialTheme.typography.bodySmall,
            color = PixelColors.TextTertiary
        )
        SegmentBar(
            ratio = term.count.toFloat() / maxCount,
            segments = 6,
            onColor = accent,
            height = 6.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
