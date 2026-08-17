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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.waifuhtr.pixelstore.Screen
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.MessageTone
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.SourceKind
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelArt
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.art.PixelCrestImage
import com.waifuhtr.pixelstore.ui.art.PixelIconImage
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.SegmentBar
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

/** Sekme tanımı. Yönetim sekmesi yalnızca yönetici oturumunda listeye girer. */
private data class Tab(
    val screen: Screen,
    val label: String,
    val glyph: String,
    val adminOnly: Boolean = false
)

private val tabs = listOf(
    Tab(Screen.Store, "Mağaza", "bag"),
    Tab(Screen.Categories, "Türler", "grid"),
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
                Screen.Profile -> ProfileScreen(viewModel, state)
                Screen.Connection -> ConnectionScreen(viewModel, state)
                is Screen.Detail -> DetailScreen(viewModel, state, screen.id)
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
                text = state.current.title,
                style = MaterialTheme.typography.titleLarge,
                color = PixelColors.Gold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PixelBadge(
                text = if (state.sourceKind == SourceKind.WORDPRESS) "WP" else "YEREL",
                tone = if (state.sourceKind == SourceKind.WORDPRESS) BadgeTone.INSTALLED else BadgeTone.NEUTRAL
            )
            Spacer(Modifier.width(PixelSpacing.small))
            val session = state.session
            if (session != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickablePixel({
                        feedback.tap()
                        viewModel.openTab(Screen.Profile)
                    }, "Profil")
                ) {
                    PixelIconImage(
                        session.avatarSeed,
                        if (session.isAdmin) "amber" else "azure",
                        Modifier
                            .size(32.dp)
                            .pixelFrame(
                                fill = Color.Transparent,
                                border = if (session.isAdmin) PixelColors.Gold else PixelColors.Sky,
                                corner = PixelColors.Ink,
                                thickness = 2.dp
                            )
                    )
                }
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

/* ---- Alt sekmeler ---------------------------------------------------------------------------- */

@Composable
private fun BottomTabs(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    // Yönetim sekmesi kullanıcı rolünde hiç oluşturulmaz; gizlenmiş bir düğüm bırakılmaz.
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
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PixelGlyphImage(
                        tab.glyph,
                        if (selected) PixelColors.Gold else PixelColors.TextTertiary,
                        Modifier.size(18.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) PixelColors.Gold else PixelColors.TextTertiary
                    )
                    Box(
                        Modifier
                            .width(24.dp)
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
        Text("PIXELSTORE", style = MaterialTheme.typography.displayLarge, color = PixelColors.Gold)
        Spacer(Modifier.height(PixelSpacing.small))
        Text(
            "Piksel dünyanın uygulama mağazası",
            style = MaterialTheme.typography.bodyMedium,
            color = PixelColors.TextSecondary
        )
        Spacer(Modifier.height(PixelSpacing.xlarge))
        SegmentBar(
            ratio = 0.6f,
            segments = 8,
            onColor = PixelColors.Mint,
            modifier = Modifier.width(160.dp)
        )
    }
}

/** İşlem sürerken üstte akan ince şerit: "bir şey oluyor" bilgisini engelsiz verir. */
@Composable
private fun BusyStrip(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PixelColors.Ink)
            .padding(horizontal = PixelSpacing.gutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "YÜKLENİYOR",
            style = MaterialTheme.typography.labelSmall,
            color = PixelColors.Mint
        )
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
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val tone = message?.tone ?: MessageTone.INFO
            val border = when (tone) {
                MessageTone.SUCCESS -> PixelColors.Mint
                MessageTone.ERROR -> PixelColors.Rose
                MessageTone.INFO -> PixelColors.Outline
            }
            Row(
                Modifier
                    .padding(PixelSpacing.gutter)
                    .padding(bottom = 72.dp)
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

/* ---- Onay penceresi -------------------------------------------------------------------------- */

/**
 * Piksel temalı onay penceresi. Material AlertDialog yerine kendi çerçevesini çizer;
 * yuvarlak köşe ve gölge kullanmıyoruz.
 */
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(PixelSpacing.xlarge)
                .fillMaxWidth()
                .pixelFrame(
                    fill = PixelColors.Surface,
                    border = PixelColors.Gold,
                    corner = PixelColors.Backdrop
                )
                .padding(PixelSpacing.large),
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = PixelColors.Gold)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = PixelColors.TextPrimary
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
            ) {
                PixelButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    tone = PixelButtonTone.GHOST,
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    tone = confirmTone,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Ekran görüntüsü tam ekran görüntüleyici. */
@Composable
fun ShotViewer(
    title: String,
    shots: List<com.waifuhtr.pixelstore.data.Screenshot>,
    palette: String,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    if (shots.isEmpty()) return
    var index by remember { mutableStateOf(startIndex.coerceIn(0, shots.lastIndex)) }
    val feedback = LocalFeedback.current
    val shot = shots[index]

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(PixelSpacing.large)
                .fillMaxWidth()
                .pixelFrame(fill = PixelColors.Surface, border = PixelColors.Gold)
                .padding(PixelSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PixelColors.Gold)
            ShotFrame(shot = shot, palette = palette, scale = 2.4f)
            Text(
                text = "${index + 1} / ${shots.size}" +
                    if (shot.caption.isNotBlank()) " — ${shot.caption}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.large)) {
                PixelButton("ÖNCEKİ", {
                    feedback.tap(Sfx.MOVE, 8)
                    index = (index - 1 + shots.size) % shots.size
                }, tone = PixelButtonTone.GHOST, glyph = "left")
                PixelButton("SONRAKİ", {
                    feedback.tap(Sfx.MOVE, 8)
                    index = (index + 1) % shots.size
                }, tone = PixelButtonTone.GHOST, glyph = "right")
            }
            PixelButton("KAPAT", onDismiss, tone = PixelButtonTone.PRIMARY, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Ekran görüntüsünü doğru en/boy oranıyla, tam sayı ölçekle çizer. */
@Composable
fun ShotFrame(
    shot: com.waifuhtr.pixelstore.data.Screenshot,
    palette: String,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val kind = shot.kind
    com.waifuhtr.pixelstore.ui.art.PixelShotImage(
        seed = shot.seed,
        scene = shot.scene,
        palette = palette,
        kind = kind,
        modifier = modifier
            .size(width = (kind.w * scale).dp, height = (kind.h * scale).dp)
    )
}

/** Sahne adını arayüzde göstermek için. */
val PixelArt.Scene.turkishLabel: String get() = label
