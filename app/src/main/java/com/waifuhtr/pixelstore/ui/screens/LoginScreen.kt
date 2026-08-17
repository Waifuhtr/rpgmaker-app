package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.SourceKind
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelCrestImage
import com.waifuhtr.pixelstore.ui.art.PixelIconImage
import com.waifuhtr.pixelstore.ui.components.LabeledField
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.components.clickablePixel
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

@Composable
fun LoginScreen(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showConnection by remember { mutableStateOf(false) }

    fun submit(user: String, pass: String) {
        feedback.tap(Sfx.SELECT)
        viewModel.login(user, pass)
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PixelSpacing.gutter, vertical = PixelSpacing.xlarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.large)
    ) {
        PixelCrestImage(Modifier.size(88.dp))
        Text("PIXELSTORE", style = MaterialTheme.typography.displayLarge, color = PixelColors.Gold)
        Text(
            "Piksel dünyanın uygulama mağazası",
            style = MaterialTheme.typography.bodyMedium,
            color = PixelColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        // Hangi veri kaynağına giriş yapıldığı baştan belli olsun.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "VERİ KAYNAĞI",
                style = MaterialTheme.typography.labelSmall,
                color = PixelColors.TextTertiary
            )
            Spacer(Modifier.width(PixelSpacing.small))
            PixelBadge(
                text = if (state.sourceKind == SourceKind.WORDPRESS) "WORDPRESS" else "YEREL DEMO",
                tone = if (state.sourceKind == SourceKind.WORDPRESS) BadgeTone.INSTALLED else BadgeTone.NEUTRAL
            )
        }

        PixelPanel {
            LabeledField("KULLANICI") {
                PixelTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        viewModel.clearLoginError()
                    },
                    placeholder = "kullanıcı adı",
                    imeAction = ImeAction.Next
                )
            }
            LabeledField("PAROLA") {
                PixelTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.clearLoginError()
                    },
                    placeholder = "parola",
                    isPassword = true,
                    imeAction = ImeAction.Go,
                    onImeAction = { submit(username, password) }
                )
            }
            if (state.loginError != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pixelFrame(fill = PixelColors.RoseDeep, border = PixelColors.Rose)
                        .padding(PixelSpacing.medium)
                ) {
                    Text(
                        state.loginError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelColors.TextPrimary
                    )
                }
            }
            PixelButton(
                text = if (state.busy) "GİRİŞ YAPILIYOR" else "GİRİŞ YAP",
                onClick = { submit(username, password) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.sourceKind == SourceKind.LOCAL) {
            SectionHeader("Demo hesaplar")
            DemoAccountCard(
                label = "Yönetici olarak gir",
                username = "admin",
                password = "admin123",
                seed = "warden-01",
                palette = "amber",
                accent = PixelColors.Gold,
                note = "Mağaza + yönetim paneli"
            ) { submit("admin", "admin123") }
            DemoAccountCard(
                label = "Kullanıcı olarak gir",
                username = "user",
                password = "user123",
                seed = "wanderer-07",
                palette = "azure",
                accent = PixelColors.Sky,
                note = "Yalnızca mağaza"
            ) { submit("user", "user123") }
        } else {
            PixelPanel(border = PixelColors.SkyDeep) {
                Text(
                    "WordPress kipindesin. Sitendeki WordPress kullanıcı adı ve parolanla giriş yap. " +
                        "Yönetici yetkisi, WordPress rolünden gelir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(PixelSpacing.small))

        PixelButton(
            text = if (showConnection) "BAĞLANTIYI KAPAT" else "SUNUCU / BAĞLANTI AYARLARI",
            onClick = {
                feedback.tap(Sfx.OPEN)
                showConnection = !showConnection
            },
            tone = PixelButtonTone.GHOST,
            glyph = "gear",
            modifier = Modifier.fillMaxWidth()
        )

        if (showConnection) {
            ConnectionPanel(viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun DemoAccountCard(
    label: String,
    username: String,
    password: String,
    seed: String,
    palette: String,
    accent: androidx.compose.ui.graphics.Color,
    note: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.Surface, border = accent)
            .clickablePixel(onClick, label)
            .padding(PixelSpacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelIconImage(seed, palette, Modifier.size(48.dp))
        Spacer(Modifier.width(PixelSpacing.large))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PixelSpacing.tiny)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = PixelColors.TextPrimary)
            Text(
                "$username / $password",
                style = MaterialTheme.typography.bodySmall,
                color = accent
            )
            Text(note, style = MaterialTheme.typography.bodySmall, color = PixelColors.TextTertiary)
        }
        Box(
            Modifier
                .size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            com.waifuhtr.pixelstore.ui.art.PixelGlyphImage("right", accent, Modifier.size(16.dp))
        }
    }
}
