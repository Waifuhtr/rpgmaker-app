package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

@Composable
fun CategoriesScreen(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    val counts = state.categories.associate { category ->
        category.name to state.apps.count { it.category == category.name }
    }
    val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)

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
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column {
                SectionHeader("Türler", glyph = "grid")
                Spacer(Modifier.height(PixelSpacing.small))
                Text(
                    "Bir tür seç; mağaza o türe göre filtrelenir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextSecondary
                )
                Spacer(Modifier.height(PixelSpacing.small))
            }
        }

        items(state.categories, key = { it.id }) { category ->
            val count = counts[category.name] ?: 0
            Column(
                Modifier
                    .fillMaxWidth()
                    .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Outline)
                    .clickablePixel({
                        feedback.tap()
                        viewModel.setCategory(category.name)
                        viewModel.openTab(Screen.Store)
                    }, category.name)
                    .padding(PixelSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixelSpacing.small)
            ) {
                PixelGlyphImage(category.glyph, PixelColors.Gold, Modifier.size(32.dp))
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = PixelColors.TextPrimary
                )
                Text(
                    "$count kayıt",
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.TextTertiary
                )
                SegmentBar(
                    ratio = count.toFloat() / max,
                    segments = 6,
                    onColor = PixelColors.Sky,
                    height = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
