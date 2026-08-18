package com.waifuhtr.pixelstore.ui.screens

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.Review
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.EmptyState
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelChip
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.components.RemoteImage
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

/**
 * Yorumlar / incelemeler.
 *
 * Temanın `comment_type = review` kayıtlarıyla aynı listedir: uygulamadan yazılan yorum sitedeki
 * inceleme sayfasında, sitede yazılan yorum burada görünür. Kullanıcı bir oyuna bir kez yorum
 * yazabilir (temanın kuralı).
 */
@Composable
fun ReviewsScreen(viewModel: StoreViewModel, state: UiState, gameId: String) {
    val feedback = LocalFeedback.current
    var content by remember { mutableStateOf("") }
    var recommended by remember { mutableStateOf<Boolean?>(null) }
    var pendingDelete by remember { mutableStateOf<Review?>(null) }

    LaunchedEffect(gameId) { viewModel.loadReviews(gameId) }

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
        // Oyun adı üst çubukta yazıyor; burada tekrar edilmez.
        if (!state.hasReviewed) {
            item {
                PixelPanel {
                    Text(
                        "YORUM YAZ",
                        style = MaterialTheme.typography.labelMedium,
                        color = PixelColors.Gold
                    )
                    Text(
                        "Bu oyunu başkalarına tavsiye eder misin?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelColors.TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                        PixelChip(
                            text = "Evet, tavsiye ederim",
                            selected = recommended == true,
                            glyph = "check",
                            onClick = {
                                feedback.tap(Sfx.MOVE, 8)
                                recommended = true
                            }
                        )
                        PixelChip(
                            text = "Hayır",
                            selected = recommended == false,
                            glyph = "cross",
                            onClick = {
                                feedback.tap(Sfx.MOVE, 8)
                                recommended = false
                            }
                        )
                    }
                    PixelTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = "Bu oyun hakkında ne düşünüyorsun?",
                        singleLine = false,
                        minLines = 4,
                        imeAction = ImeAction.Default
                    )
                    PixelButton(
                        text = if (state.busy) "GÖNDERİLİYOR" else "YORUMU YAYINLA",
                        onClick = {
                            feedback.tap(Sfx.COIN)
                            viewModel.addReview(gameId, content, recommended == true) {
                                content = ""
                                recommended = null
                            }
                        },
                        enabled = !state.busy && recommended != null && content.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (recommended == null) {
                        Text(
                            "Önce tavsiye edip etmediğini seç.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PixelColors.TextTertiary
                        )
                    }
                }
            }
        } else {
            item {
                PixelPanel(border = PixelColors.MintDeep) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelGlyphImage("check", PixelColors.Mint, Modifier.size(14.dp))
                        Spacer(Modifier.width(PixelSpacing.small))
                        Text(
                            "Bu oyun için yorumunu yazdın.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelColors.TextPrimary
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                if (state.reviews.isEmpty()) "Yorumlar" else "${state.reviews.size} yorum",
                glyph = "pip"
            )
        }

        if (state.reviews.isEmpty()) {
            item {
                EmptyState(
                    if (state.reviewsLoading) "Yükleniyor…" else "Henüz yorum yok. İlk yorumu sen yaz.",
                    glyph = "hero"
                )
            }
        } else {
            items(state.reviews, key = { it.id }) { review ->
                ReviewCard(
                    review = review,
                    onVote = { up ->
                        feedback.tap(Sfx.MOVE, 8)
                        viewModel.voteReview(review.id, up)
                    },
                    onDelete = {
                        feedback.tap(Sfx.CANCEL)
                        pendingDelete = review
                    }
                )
            }
        }
    }

    pendingDelete?.let { review ->
        PixelModal(
            title = "Yorumu sil",
            message = "Bu yorum kalıcı olarak silinecek. Devam edilsin mi?",
            confirmLabel = "Sil",
            onConfirm = {
                pendingDelete = null
                viewModel.deleteReview(review.id, gameId)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    onVote: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .pixelFrame(
                fill = PixelColors.Surface,
                border = if (review.recommended) PixelColors.MintDeep else PixelColors.RoseDeep
            )
            .padding(PixelSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                url = review.avatarUrl,
                fallbackSeed = review.author,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(PixelSpacing.medium))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    review.author,
                    style = MaterialTheme.typography.titleSmall,
                    color = PixelColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    review.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelColors.TextTertiary
                )
            }
            PixelBadge(
                if (review.recommended) "TAVSİYE EDİYOR" else "TAVSİYE ETMİYOR",
                if (review.recommended) BadgeTone.INSTALLED else BadgeTone.WARN
            )
        }

        if (review.badges.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                review.badges.take(4).forEach { badge ->
                    PixelBadge(badge.name.uppercase(), BadgeTone.ADMIN)
                }
            }
        }

        Text(review.content, style = MaterialTheme.typography.bodyLarge, color = PixelColors.TextPrimary)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (review.voted) "Değerlendirdin" else "Faydalı oldu mu?",
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextTertiary
            )
            Spacer(Modifier.width(PixelSpacing.medium))
            VoteButton("up", review.upvotes, PixelColors.Mint, enabled = !review.voted && !review.mine) {
                onVote(true)
            }
            Spacer(Modifier.width(PixelSpacing.small))
            VoteButton("down", review.downvotes, PixelColors.Rose, enabled = !review.voted && !review.mine) {
                onVote(false)
            }
            Spacer(Modifier.weight(1f))
            if (review.mine) {
                PixelButton(
                    text = "SİL",
                    onClick = onDelete,
                    tone = PixelButtonTone.DANGER,
                    glyph = "cross"
                )
            }
        }
    }
}

@Composable
private fun VoteButton(
    glyph: String,
    count: Int,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .background(PixelColors.SurfaceHigh)
            .then(if (enabled) Modifier.clickablePixel(onClick) else Modifier)
            .padding(horizontal = PixelSpacing.small, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelGlyphImage(
            glyph,
            if (enabled) accent else PixelColors.OutlineSoft,
            Modifier.size(12.dp)
        )
        if (count > 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) accent else PixelColors.TextTertiary
            )
        }
    }
}
