package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.SortMode
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.AppRecord
import com.waifuhtr.pixelstore.ui.Format
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelArt
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.art.PixelIconImage
import com.waifuhtr.pixelstore.ui.art.PixelShotImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelChip
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.StarRow
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

@Composable
fun StoreScreen(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    val apps = state.visibleApps

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = PixelSpacing.gutter,
            end = PixelSpacing.gutter,
            top = PixelSpacing.large,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.large)
    ) {
        item {
            SearchBar(state.query, viewModel::setQuery)
        }

        val featured = state.featured
        if (featured != null && state.query.isBlank() && state.category == null) {
            item {
                FeaturedCard(featured) {
                    feedback.tap()
                    viewModel.push(Screen.Detail(featured.id))
                }
            }
        }

        item {
            SectionHeader("Türler", glyph = "grid")
        }
        item {
            CategoryChips(state, viewModel)
        }

        item {
            SectionHeader(
                title = if (state.category != null) state.category else "Tüm kayıtlar",
                glyph = "bag"
            )
        }
        item {
            SortRow(state.sort) {
                feedback.tap(Sfx.MOVE, 8)
                viewModel.setSort(it)
            }
        }

        if (apps.isEmpty()) {
            item { EmptyState("Aramanla eşleşen kayıt yok.") }
        } else {
            items(apps, key = { it.id }) { app ->
                AppCard(
                    app = app,
                    installed = state.installed.contains(app.id),
                    onClick = {
                        feedback.tap()
                        viewModel.push(Screen.Detail(app.id))
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryChips(state: UiState, viewModel: StoreViewModel) {
    val feedback = LocalFeedback.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
        item {
            PixelChip(
                text = "Tümü",
                selected = state.category == null,
                glyph = "star",
                onClick = {
                    feedback.tap(Sfx.MOVE, 8)
                    viewModel.setCategory(null)
                }
            )
        }
        items(state.categories, key = { it.id }) { category ->
            val selected = state.category == category.name
            PixelChip(
                text = category.name,
                selected = selected,
                glyph = category.glyph,
                onClick = {
                    feedback.tap(Sfx.MOVE, 8)
                    viewModel.setCategory(if (selected) null else category.name)
                }
            )
        }
    }
}

@Composable
private fun SortRow(current: SortMode, onSelect: (SortMode) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
        items(SortMode.entries.toList()) { mode ->
            val selected = mode == current
            Column(
                modifier = Modifier.clickablePixel({ onSelect(mode) }, mode.label),
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

/** Haftanın oyunu kartı: geniş görsel + ayrı bilgi şeridi. */
@Composable
private fun FeaturedCard(app: AppRecord, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Gold)
            .clickablePixel(onClick, app.title)
    ) {
        PixelShotImage(
            seed = "${app.iconSeed}-hero",
            scene = PixelArt.Scene.TITLE,
            palette = app.palette,
            kind = PixelArt.ShotKind.BANNER,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PixelArt.ShotKind.BANNER.w.toFloat() / PixelArt.ShotKind.BANNER.h)
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
                    "HAFTANIN OYUNU",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelColors.Mint
                )
                Text(
                    app.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = PixelColors.Gold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.developer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextSecondary
                )
            }
            Spacer(Modifier.width(PixelSpacing.medium))
            PixelGlyphImage("right", PixelColors.Gold, Modifier.size(18.dp))
        }
    }
}

/**
 * Mağaza kartı.
 *
 * Bilgi düzeni bilinçli olarak dört kademeye ayrıldı: başlık, geliştirici, açıklama, ölçüler.
 * v1'de her satır aynı boyut ve renkteydi ve "neyin ne olduğu" okunmuyordu.
 */
@Composable
fun AppCard(
    app: AppRecord,
    installed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pixelFrame(
                fill = PixelColors.Surface,
                border = if (app.published) PixelColors.Outline else PixelColors.Amber
            )
            .clickablePixel(onClick, app.title)
            .padding(PixelSpacing.large),
        verticalAlignment = Alignment.Top
    ) {
        PixelIconImage(
            app.iconSeed,
            app.palette,
            Modifier
                .size(64.dp)
                .pixelFrame(
                    fill = Color.Transparent,
                    border = PixelColors.OutlineSoft,
                    corner = PixelColors.Surface,
                    thickness = 2.dp
                )
        )
        Spacer(Modifier.width(PixelSpacing.large))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)
        ) {
            Text(
                app.title,
                style = MaterialTheme.typography.titleMedium,
                color = PixelColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                app.developer,
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.Sky
            )
            if (app.shortDescription.isNotBlank()) {
                Text(
                    app.shortDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRow(app.rating, starSize = 11.dp)
                Spacer(Modifier.width(PixelSpacing.small))
                Text(
                    text = if (app.rating > 0) Format.rating(app.rating) else "Yeni",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelColors.Gold
                )
                Spacer(Modifier.width(PixelSpacing.medium))
                Text(
                    text = "${Format.count(app.installs)} indirme",
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.TextTertiary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.small)) {
                PixelBadge(app.category, BadgeTone.CATEGORY)
                PixelBadge(Format.size(app.sizeMb), BadgeTone.NEUTRAL)
                if (!app.published) PixelBadge("TASLAK", BadgeTone.DRAFT)
                if (installed) PixelBadge("YÜKLÜ", BadgeTone.INSTALLED)
            }
        }
    }
}
