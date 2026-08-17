package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.data.SourceKind
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.components.BadgeTone
import com.waifuhtr.pixelstore.ui.components.LabeledField
import com.waifuhtr.pixelstore.ui.components.PixelBadge
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.components.SectionHeader
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

@Composable
fun ConnectionScreen(viewModel: StoreViewModel, state: UiState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PixelSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.large)
    ) {
        SectionHeader("Veri kaynağı")
        ConnectionPanel(viewModel, state)

        SectionHeader("WordPress kurulumu")
        PixelPanel {
            StepRow(1, "wordpress-plugin/pixelstore-api klasörünü zip olarak WordPress'e kur ve etkinleştir.")
            StepRow(2, "wp-admin → PixelStore ekranından \"Demo kataloğu kur\" düğmesine bas.")
            StepRow(3, "Aynı ekranda yazan site adresini yukarıya gir ve \"Bağlantıyı sına\"ya bas.")
            StepRow(4, "WordPress kullanıcı adın ve parolanla giriş yap. Yönetici yetkisi WordPress rolünden gelir.")
        }

        PixelPanel(border = PixelColors.OutlineSoft) {
            Text(
                "Adres boş bırakılırsa uygulama yerel kipte çalışır: katalog cihazda saklanır ve " +
                    "gömülü demo hesaplar (admin/user) geçerli olur. İnternet gerekmez.",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelColors.TextSecondary
            )
        }
    }
}

/**
 * Bağlantı paneli. Giriş ekranı ve Bağlantı ekranı aynı bileşeni kullanır: kullanıcı henüz
 * oturum açmadan da sunucu adresini girebilmeli.
 */
@Composable
fun ConnectionPanel(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    var url by remember(state.wordpressUrl) { mutableStateOf(state.wordpressUrl) }
    var probeResult by remember { mutableStateOf<String?>(null) }
    var probing by remember { mutableStateOf(false) }

    PixelPanel(
        border = if (state.sourceKind == SourceKind.WORDPRESS) PixelColors.Mint else PixelColors.Outline
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "AKTİF KİP",
                style = MaterialTheme.typography.labelMedium,
                color = PixelColors.TextTertiary
            )
            Spacer(Modifier.width(PixelSpacing.small))
            PixelBadge(
                text = if (state.sourceKind == SourceKind.WORDPRESS) "WORDPRESS" else "YEREL",
                tone = if (state.sourceKind == SourceKind.WORDPRESS) BadgeTone.INSTALLED else BadgeTone.NEUTRAL
            )
        }

        LabeledField(
            label = "WORDPRESS SİTE ADRESİ",
            hint = "Örnek: https://siteniz.com — /wp-json eklemene gerek yok. Boş bırakırsan yerel kip."
        ) {
            PixelTextField(
                value = url,
                onValueChange = {
                    url = it
                    probeResult = null
                },
                placeholder = "https://siteniz.com",
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(PixelSpacing.medium)) {
            PixelButton(
                text = if (probing) "SINANIYOR" else "BAĞLANTIYI SINA",
                onClick = {
                    feedback.tap(Sfx.SELECT)
                    probing = true
                    viewModel.testConnection(url) {
                        probeResult = it
                        probing = false
                    }
                },
                tone = PixelButtonTone.GHOST,
                enabled = !probing,
                modifier = Modifier.weight(1f)
            )
            PixelButton(
                text = "KAYDET",
                onClick = {
                    feedback.tap(Sfx.COIN)
                    viewModel.applyWordPressUrl(url)
                    probeResult = null
                },
                tone = PixelButtonTone.PRIMARY,
                modifier = Modifier.weight(1f)
            )
        }

        val result = probeResult
        if (result != null) {
            val ok = result.startsWith("Bağlantı başarılı")
            Column(
                Modifier
                    .fillMaxWidth()
                    .pixelFrame(
                        fill = if (ok) PixelColors.MintDeep else PixelColors.RoseDeep,
                        border = if (ok) PixelColors.Mint else PixelColors.Rose
                    )
                    .padding(PixelSpacing.medium)
            ) {
                Text(
                    result,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun StepRow(step: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "$step.",
            style = MaterialTheme.typography.titleSmall,
            color = PixelColors.Gold,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = PixelColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
