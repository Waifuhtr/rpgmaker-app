package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing

/**
 * İstek listesi. Temanın `sl_favorites` verisiyle aynı listedir: sitede eklenen oyun burada,
 * burada eklenen oyun sitedeki profil sayfasında görünür.
 */
@Composable
fun WishlistScreen(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current

    // Sekmeye her girişte tazele: site tarafından da değişebilir.
    LaunchedEffect(Unit) { viewModel.loadWishlist() }

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
        // Başlık üst çubukta yazıyor; burada yalnızca listenin site ile ortak olduğu belirtilir.
        item {
            Text(
                "Bu liste site ile ortak: riaslink.fun profilindeki istek listesinin aynısı." +
                    if (state.wishlist.isNotEmpty()) " ${state.wishlist.size} oyun." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelColors.TextSecondary
            )
        }

        if (state.wishlist.isEmpty()) {
            item {
                EmptyState(
                    if (state.wishlistLoading) "Yükleniyor…" else "İstek listen boş. Oyun kartındaki kalbe dokun.",
                    glyph = "heart"
                )
            }
        } else {
            items(state.wishlist, key = { it.id }) { game ->
                GameCard(
                    game = game.copy(favorited = true),
                    onClick = {
                        feedback.tap()
                        viewModel.openGame(game.id)
                    },
                    onFavorite = {
                        feedback.tap(Sfx.CANCEL)
                        viewModel.toggleFavorite(game.id)
                    }
                )
            }
        }
    }
}
