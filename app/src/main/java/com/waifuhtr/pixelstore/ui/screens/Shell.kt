package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.waifuhtr.pixelstore.MessageTone
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelCrestImage
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.components.RemoteImage
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

private data class Tab(
    val screen: Screen,
    val label: String,
    val glyph: String,
    val adminOnly: Boolean = false
)

private val tabs = listOf(
    Tab(Screen.Store, "Mağaza", "bag"),
    Tab(Screen.Categories, "Türler", "grid"),
    Tab(Screen.Wishlist, "Listem", "heart"),
    Tab(Screen.Profile, "Profil", "hero"),
    Tab(Screen.Admin, "Yönetim", "gear", adminOnly = true)
)

@Composable
fun PixelStoreApp(viewModel: StoreViewModel, state: UiState) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PixelColors.Backdrop)
    ) {
        when {
            state.booting -> SplashScreen()
            state.session == null -> LoginScreen(viewModel, state)
            else -> MainShell(viewModel, state)
        }
        ToastOverlay(state, viewModel::consumeMessage)
    }
}

@Composable
private fun MainShell(viewModel: StoreViewModel, state: UiState) {
    Column(Modifier.fillMaxSize()) {
        TopBar(viewModel, state)
        Box(Modifier.weight(1f)) {
            when (val screen = state.current) {
                Screen.Store -> StoreScreen(viewModel, state)
                Screen.Categories -> CategoriesScreen(viewModel, state)
                Screen.Wishlist -> WishlistScreen(viewModel, state)
                Screen.Profile -> ProfileScreen(viewModel, state)
                is Screen.Detail -> DetailScreen(viewModel, state, screen.id)
                is Screen.Reviews -> ReviewsScreen(viewModel, state, screen.id)
                Screen.Admin -> AdminScreen(viewModel, state)
                Screen.AdminUsers -> AdminUsersScreen(state)
                is Screen.AdminEditor -> AdminEditorScreen(viewModel, state, screen.id)
            }
            if (state.busy) BusyStrip(Modifier.align(Alignment.TopCenter))
        }
        BottomTabs(viewModel, state)
    }
}

/* ---- Üst çubuk ------------------------------------------------------------------------------- */

@Composable
private fun TopBar(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(PixelColors.Ink)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = PixelSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.canGoBack) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clickablePixel({
                            feedback.tap(Sfx.CANCEL)
                            viewModel.back()
                        }, "Geri"),
                    contentAlignment = Alignment.Center
                ) {
                    PixelGlyphImage("left", PixelColors.Mint, Modifier.size(18.dp))
                }
            } else {
                PixelCrestImage(Modifier.size(30.dp))
            }
            Spacer(Modifier.width(PixelSpacing.medium))
            Text(
                text = topBarTitle(state),
                style = MaterialTheme.typography.titleLarge,
                color = PixelColors.Gold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            val session = state.session
            if (session != null) {
                RemoteImage(
                    url = session.avatarUrl,
                    fallbackSeed = session.username,
                    palette = if (session.isAdmin) "amber" else "azure",
                    contentDescription = "Profil",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickablePixel({
                            feedback.tap()
                            viewModel.openTab(Screen.Profile)
                        }, "Profil")
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(PixelColors.GoldDeep)
        )
    }
}

/**
 * Üst çubuk başlığı.
 *
 * Oyun ekranlarında sabit "OYUN" yerine oyunun kendi adı yazılır; kullanıcı hangi kayda baktığını
 * geri tuşuna basmadan görür. Kayıt henüz yüklenmediyse ekranın varsayılan başlığı kalır.
 */
private fun topBarTitle(state: UiState): String {
    val detail = state.detail
    return when (val screen = state.current) {
        is Screen.Detail ->
            if (detail?.id == screen.id && detail.title.isNotBlank()) detail.title else screen.title
        is Screen.Reviews ->
            if (detail?.id == screen.id && detail.title.isNotBlank()) detail.title else screen.title
        else -> screen.title
    }
}

/* ---- Alt sekmeler ---------------------------------------------------------------------------- */

@Composable
private fun BottomTabs(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    // Yönetim sekmesi kullanıcı rolünde hiç oluşturulmaz.
    val visible = tabs.filter { !it.adminOnly || state.isAdmin }
    val activeRoot = state.backStack.firstOrNull()

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(PixelColors.Outline)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(PixelColors.Ink)
                .navigationBarsPadding()
        ) {
            visible.forEach { tab ->
                val selected = activeRoot == tab.screen
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) PixelColors.Surface else Color.Transparent)
                        .clickablePixel({
                            feedback.tap(Sfx.MOVE, 8)
                            viewModel.openTab(tab.screen)
                        }, tab.label)
                        .padding(vertical = PixelSpacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PixelGlyphImage(
                        tab.glyph,
                        if (selected) PixelColors.Gold else PixelColors.TextTertiary,
                        Modifier.size(17.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) PixelColors.Gold else PixelColors.TextTertiary,
                        maxLines = 1
                    )
                    Box(
                        Modifier
                            .width(22.dp)
                            .height(2.dp)
                            .background(if (selected) PixelColors.Gold else Color.Transparent)
                    )
                }
            }
        }
    }
}

/* ---- Açılış perdesi -------------------------------------------------------------------------- */

@Composable
private fun SplashScreen() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PixelCrestImage(Modifier.size(96.dp))
        Spacer(Modifier.height(PixelSpacing.large))
        Text("RIASLINK", style = MaterialTheme.typography.displayLarge, color = PixelColors.Gold)
        Spacer(Modifier.height(PixelSpacing.small))
        Text(
            "Oyun kütüphanesi yükleniyor",
            style = MaterialTheme.typography.bodyMedium,
            color = PixelColors.TextSecondary
        )
        Spacer(Modifier.height(PixelSpacing.xlarge))
        SegmentBar(ratio = 0.6f, segments = 8, onColor = PixelColors.Mint, modifier = Modifier.width(160.dp))
    }
}

@Composable
private fun BusyStrip(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PixelColors.Ink)
            .padding(horizontal = PixelSpacing.gutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("YÜKLENİYOR", style = MaterialTheme.typography.labelSmall, color = PixelColors.Mint)
        Spacer(Modifier.width(PixelSpacing.small))
        SegmentBar(ratio = 1f, segments = 12, onColor = PixelColors.MintDeep, height = 6.dp)
    }
}

/* ---- Bildirim -------------------------------------------------------------------------------- */

@Composable
private fun ToastOverlay(state: UiState, onConsume: () -> Unit) {
    val message = state.message
    LaunchedEffect(message?.id) {
        if (message != null) {
            kotlinx.coroutines.delay(2600)
            onConsume()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
            val tone = message?.tone ?: MessageTone.INFO
            val border = when (tone) {
                MessageTone.SUCCESS -> PixelColors.Mint
                MessageTone.ERROR -> PixelColors.Rose
                MessageTone.INFO -> PixelColors.Outline
            }
            Row(
                Modifier
                    .padding(PixelSpacing.gutter)
                    .padding(bottom = 76.dp)
                    .fillMaxWidth()
                    .pixelFrame(fill = PixelColors.Surface, border = border)
                    .padding(PixelSpacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelGlyphImage(
                    when (tone) {
                        MessageTone.SUCCESS -> "check"
                        MessageTone.ERROR -> "cross"
                        MessageTone.INFO -> "dotOn"
                    },
                    border,
                    Modifier.size(14.dp)
                )
                Spacer(Modifier.width(PixelSpacing.medium))
                Text(
                    text = message?.text.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextPrimary
                )
            }
        }
    }
}

/* ---- Ortak pencereler ------------------------------------------------------------------------ */

@Composable
fun PixelModal(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmTone: PixelButtonTone = PixelButtonTone.DANGER,
    dismissLabel: String = "Vazgeç"
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(PixelSpacing.xlarge)
                .fillMaxWidth()
                .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Gold, corner = PixelColors.Backdrop)
                .padding(PixelSpacing.large),
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = PixelColors.Gold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = PixelColors.TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                PixelButton(dismissLabel, onDismiss, tone = PixelButtonTone.GHOST, modifier = Modifier.weight(1f))
                PixelButton(confirmLabel, onConfirm, tone = confirmTone, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Serbest metin isteyen pencere (hata bildirimi gibi). */
@Composable
fun PixelPromptModal(
    title: String,
    description: String,
    placeholder: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(PixelSpacing.large)
                .fillMaxWidth()
                .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Rose, corner = PixelColors.Backdrop)
                .padding(PixelSpacing.large),
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = PixelColors.Rose)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = PixelColors.TextSecondary)
            PixelTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = placeholder,
                singleLine = false,
                minLines = 4,
                imeAction = ImeAction.Default
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
                PixelButton("Vazgeç", onDismiss, tone = PixelButtonTone.GHOST, modifier = Modifier.weight(1f))
                PixelButton(
                    text = confirmLabel,
                    onClick = { onConfirm(value) },
                    tone = PixelButtonTone.DANGER,
                    enabled = value.trim().length >= 5,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Tam ekran görsel görüntüleyici (ekran görüntüleri). */
@Composable
fun ScreenshotViewer(
    title: String,
    urls: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    if (urls.isEmpty()) return
    var index by remember { mutableStateOf(startIndex.coerceIn(0, urls.lastIndex)) }
    val feedback = LocalFeedback.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(PixelSpacing.medium)
                .fillMaxWidth()
                .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Gold)
                .padding(PixelSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PixelColors.Gold, maxLines = 1)
            RemoteImage(
                url = urls[index],
                fallbackSeed = title,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
            Text(
                "${index + 1} / ${urls.size}",
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            if (urls.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.large)) {
                    PixelButton("ÖNCEKİ", {
                        feedback.tap(Sfx.MOVE, 8)
                        index = (index - 1 + urls.size) % urls.size
                    }, tone = PixelButtonTone.GHOST, glyph = "left")
                    PixelButton("SONRAKİ", {
                        feedback.tap(Sfx.MOVE, 8)
                        index = (index + 1) % urls.size
                    }, tone = PixelButtonTone.GHOST, glyph = "right")
                }
            }
            PixelButton("KAPAT", onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
