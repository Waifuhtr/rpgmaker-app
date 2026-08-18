package com.waifuhtr.pixelstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.waifuhtr.pixelstore.BuildConfig
import com.waifuhtr.pixelstore.StoreViewModel
import com.waifuhtr.pixelstore.UiState
import com.waifuhtr.pixelstore.ui.LocalFeedback
import com.waifuhtr.pixelstore.ui.Sfx
import com.waifuhtr.pixelstore.ui.art.PixelCrestImage
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.components.LabeledField
import com.waifuhtr.pixelstore.ui.components.PixelButton
import com.waifuhtr.pixelstore.ui.components.PixelButtonTone
import com.waifuhtr.pixelstore.ui.components.PixelPanel
import com.waifuhtr.pixelstore.ui.components.PixelTextField
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame

/**
 * Giriş ekranı.
 *
 * Demo hesap yoktur: kullanıcı veritabanı riaslink.fun'ın WordPress kullanıcı tablosudur.
 * Sunucu adresi uygulamada gömülü olduğu için burada sunucu alanı da yoktur.
 */
@Composable
fun LoginScreen(viewModel: StoreViewModel, state: UiState) {
    val feedback = LocalFeedback.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var probe by remember { mutableStateOf<String?>(null) }
    var probing by remember { mutableStateOf(false) }

    fun submit() {
        if (username.isBlank() || password.isBlank()) {
            viewModel.toast("Kullanıcı adı ve parola gerekli.", com.waifuhtr.pixelstore.MessageTone.ERROR)
            return
        }
        feedback.tap(Sfx.SELECT)
        viewModel.login(username, password)
    }

    // Klavye açıldığında kaydırılabilir kalır; kapalıyken içerik dikeyde ortalanır, böylece kısa
    // formun altında büyük bir boşluk kalmaz. İçeriğin en az ekran yüksekliği kadar yer kaplaması
    // (heightIn) ortalamayı mümkün kılar.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewport)
                .padding(horizontal = PixelSpacing.gutter, vertical = PixelSpacing.xlarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixelSpacing.large, Alignment.CenterVertically)
        ) {
            PixelCrestImage(Modifier.size(88.dp))
            Text("RIASLINK", style = MaterialTheme.typography.displayLarge, color = PixelColors.Gold)
            Text(
                "Oyun kütüphanesi",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            PixelPanel {
                Text(
                    "Sitedeki hesabınla giriş yap. Hesabın yoksa riaslink.fun üzerinden kayıt olabilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelColors.TextSecondary
                )
                LabeledField("KULLANICI ADI VEYA E-POSTA") {
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
                        onImeAction = { submit() }
                    )
                }
                if (state.loginError != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pixelFrame(fill = PixelColors.RoseDeep, border = PixelColors.Rose)
                            .padding(PixelSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelGlyphImage("warn", PixelColors.Rose, Modifier.size(14.dp))
                        Spacer(Modifier.size(PixelSpacing.medium))
                        Text(
                            state.loginError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelColors.TextPrimary
                        )
                    }
                }
                PixelButton(
                    text = if (state.busy) "GİRİŞ YAPILIYOR" else "GİRİŞ YAP",
                    onClick = { submit() },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(PixelSpacing.small))

            PixelButton(
                text = if (probing) "SINANIYOR" else "SUNUCU BAĞLANTISINI SINA",
                onClick = {
                    feedback.tap()
                    probing = true
                    viewModel.testConnection {
                        probe = it
                        probing = false
                    }
                },
                tone = PixelButtonTone.GHOST,
                glyph = "gear",
                enabled = !probing,
                modifier = Modifier.fillMaxWidth()
            )

            val result = probe
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
                    Text(result, style = MaterialTheme.typography.bodyMedium, color = PixelColors.TextPrimary)
                }
            }

            Text(
                BuildConfig.SITE_URL.removePrefix("https://"),
                style = MaterialTheme.typography.labelSmall,
                color = PixelColors.TextTertiary
            )
        }
    }
}
