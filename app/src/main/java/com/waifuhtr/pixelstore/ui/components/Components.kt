package com.waifuhtr.pixelstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.waifuhtr.pixelstore.ui.art.PixelGlyphImage
import com.waifuhtr.pixelstore.ui.theme.PixelColors
import com.waifuhtr.pixelstore.ui.theme.PixelSpacing
import com.waifuhtr.pixelstore.ui.theme.pixelFrame
import com.waifuhtr.pixelstore.ui.theme.pixelRaised

/* ---- Yüzeyler ------------------------------------------------------------------------------- */

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    fill: Color = PixelColors.Surface,
    border: Color = PixelColors.Outline,
    padding: Dp = PixelSpacing.large,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pixelFrame(fill = fill, border = border)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium),
        content = content
    )
}

/** Bölüm başlığı: glif + başlık + sağa uzanan ince çizgi. Ekranı gözle bölmeyi kolaylaştırır. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    glyph: String = "right",
    color: Color = PixelColors.Mint,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelGlyphImage(glyph, PixelColors.Gold, Modifier.size(10.dp))
        Spacer(Modifier.width(PixelSpacing.small))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Spacer(Modifier.width(PixelSpacing.medium))
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(PixelColors.OutlineSoft)
        )
        if (trailing != null) {
            Spacer(Modifier.width(PixelSpacing.medium))
            trailing()
        }
    }
}

/* ---- Düğmeler ------------------------------------------------------------------------------- */

enum class PixelButtonTone { PRIMARY, GHOST, DANGER, ADMIN, MINT }

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PixelButtonTone = PixelButtonTone.PRIMARY,
    enabled: Boolean = true,
    glyph: String? = null
) {
    val fill: Color
    val label: Color
    val highlight: Color
    val shadow: Color
    when (tone) {
        PixelButtonTone.PRIMARY -> {
            fill = PixelColors.Gold; label = PixelColors.OnGold
            highlight = Color(0xFFFFE59A); shadow = PixelColors.GoldDeep
        }
        PixelButtonTone.MINT -> {
            fill = PixelColors.MintDeep; label = PixelColors.Mint
            highlight = Color(0xFF34A776); shadow = Color(0xFF0E3F2B)
        }
        PixelButtonTone.GHOST -> {
            fill = PixelColors.SurfaceHigh; label = PixelColors.TextPrimary
            highlight = Color(0xFF3C3C60); shadow = Color(0xFF1B1B2C)
        }
        PixelButtonTone.DANGER -> {
            fill = PixelColors.RoseDeep; label = Color(0xFFFFD3DC)
            highlight = Color(0xFFB4304F); shadow = Color(0xFF541425)
        }
        PixelButtonTone.ADMIN -> {
            fill = PixelColors.VioletDeep; label = Color(0xFFE3D2FF)
            highlight = Color(0xFF6B41AE); shadow = Color(0xFF2C1A4C)
        }
    }
    val alpha = if (enabled) 1f else 0.45f

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .pixelRaised(
                fill = fill.copy(alpha = alpha),
                highlight = highlight.copy(alpha = alpha),
                shadow = shadow.copy(alpha = alpha)
            )
            .then(if (enabled) Modifier.clickablePixel(onClick) else Modifier)
            .padding(horizontal = PixelSpacing.large, vertical = PixelSpacing.medium),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            PixelGlyphImage(glyph, label.copy(alpha = alpha), Modifier.size(12.dp))
            Spacer(Modifier.width(PixelSpacing.small))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = label.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Simge düğmesi: 44dp dokunma alanı, piksel glif içerik. */
@Composable
fun PixelIconButton(
    glyph: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: Color = PixelColors.Outline,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .pixelFrame(fill = PixelColors.SurfaceHigh, border = border)
            .clickablePixel(onClick, contentDescription),
        contentAlignment = Alignment.Center
    ) {
        PixelGlyphImage(glyph, tint, Modifier.size(16.dp))
    }
}

/* ---- Metin alanı ---------------------------------------------------------------------------- */

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) PixelColors.Mint else PixelColors.OutlineSoft

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pixelFrame(fill = PixelColors.SurfaceSunken, border = border)
            .padding(horizontal = PixelSpacing.medium, vertical = PixelSpacing.medium)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = PixelColors.TextTertiary
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (singleLine) 20.dp else (minLines * 20).dp)
                .onFocusChanged { focused = it.isFocused },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PixelColors.TextPrimary),
            singleLine = singleLine,
            minLines = minLines,
            cursorBrush = SolidColor(PixelColors.Gold),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction?.invoke() },
                onGo = { onImeAction?.invoke() },
                onSend = { onImeAction?.invoke() }
            )
        )
    }
}

@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PixelColors.Gold
        )
        Spacer(Modifier.height(PixelSpacing.small))
        content()
        if (hint != null) {
            Spacer(Modifier.height(PixelSpacing.tiny))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = PixelColors.TextTertiary
            )
        }
    }
}

/* ---- Rozet, yıldız, çubuk -------------------------------------------------------------------- */

enum class BadgeTone { NEUTRAL, CATEGORY, DRAFT, INSTALLED, ADMIN, USER, WARN }

@Composable
fun PixelBadge(text: String, tone: BadgeTone = BadgeTone.NEUTRAL, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        BadgeTone.NEUTRAL -> PixelColors.SurfaceHigh to PixelColors.TextSecondary
        BadgeTone.CATEGORY -> PixelColors.SkyDeep to Color(0xFFD6F0FF)
        BadgeTone.DRAFT -> Color(0xFF5A2E12) to PixelColors.Amber
        BadgeTone.INSTALLED -> PixelColors.MintDeep to Color(0xFFD2FFE8)
        BadgeTone.ADMIN -> Color(0xFF5A4412) to PixelColors.Gold
        BadgeTone.USER -> PixelColors.SkyDeep to Color(0xFFD6F0FF)
        BadgeTone.WARN -> PixelColors.RoseDeep to Color(0xFFFFD3DC)
    }
    Box(
        modifier = modifier
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}

/** Puan yıldızları. Yarım yıldız iki dikdörtgenle çizilir; piksel estetiğine uygun. */
@Composable
fun StarRow(rating: Double, modifier: Modifier = Modifier, starSize: Dp = 12.dp) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            val tint = when {
                rating >= i -> PixelColors.Gold
                rating >= i - 0.5 -> PixelColors.GoldDeep
                else -> PixelColors.OutlineSoft
            }
            PixelGlyphImage("pip", tint, Modifier.size(starSize))
        }
    }
}

/**
 * Segmentli çubuk. Sürekli bir dolgu yerine ayrık bloklar kullanılır: 8-bit çubuklara benziyor
 * ve oran gözle sayılabiliyor.
 */
@Composable
fun SegmentBar(
    ratio: Float,
    modifier: Modifier = Modifier,
    segments: Int = 10,
    onColor: Color = PixelColors.Mint,
    offColor: Color = PixelColors.SurfaceSunken,
    height: Dp = 10.dp
) {
    val filled = (ratio.coerceIn(0f, 1f) * segments).toInt().coerceIn(0, segments)
    Row(modifier = modifier.height(height), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(segments) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .background(if (index < filled) onColor else offColor)
            )
        }
    }
}

/* ---- Çip ve anahtar --------------------------------------------------------------------------- */

@Composable
fun PixelChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: String? = null
) {
    val fill = if (selected) PixelColors.Gold else PixelColors.Surface
    val border = if (selected) PixelColors.GoldDeep else PixelColors.Outline
    val label = if (selected) PixelColors.OnGold else PixelColors.TextPrimary

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .pixelFrame(fill = fill, border = border)
            .clickablePixel(onClick)
            .padding(horizontal = PixelSpacing.medium, vertical = PixelSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            PixelGlyphImage(glyph, label, Modifier.size(11.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = label, maxLines = 1)
    }
}

@Composable
fun PixelSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickablePixel(onClick = { onCheckedChange(!checked) })
            .padding(vertical = PixelSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = PixelColors.TextPrimary)
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = PixelColors.TextTertiary)
            }
        }
        Spacer(Modifier.width(PixelSpacing.medium))
        Box(
            Modifier
                .width(46.dp)
                .height(24.dp)
                .pixelFrame(
                    fill = if (checked) PixelColors.MintDeep else PixelColors.SurfaceSunken,
                    border = if (checked) PixelColors.Mint else PixelColors.Outline,
                    thickness = 2.dp
                ),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = 16.dp, height = 16.dp)
                    .background(if (checked) PixelColors.Mint else PixelColors.TextTertiary)
            )
        }
    }
}

/* ---- Durum göstergeleri ----------------------------------------------------------------------- */

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, glyph: String = "map") {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.medium)
    ) {
        PixelGlyphImage(glyph, PixelColors.OutlineSoft, Modifier.size(40.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = PixelColors.TextSecondary
        )
    }
}

/** Büyük sayı + küçük etiket: yönetim özet kartları. */
@Composable
fun MetricTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .pixelFrame(fill = PixelColors.Surface, border = accent)
            .padding(PixelSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PixelSpacing.tiny)
    ) {
        Text(value, style = MaterialTheme.typography.displayMedium, color = accent, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = PixelColors.TextSecondary)
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PixelSpacing.small),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PixelColors.TextTertiary,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = PixelColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PixelDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(PixelColors.OutlineSoft)
    )
}

/* ---- Yardımcılar ---------------------------------------------------------------------------- */

/**
 * Tıklama davranışı. Ripple (dalga) efekti kapatılır: piksel arayüzde dalga yersiz duruyor,
 * bunun yerine düğmelerin kabartma çerçevesi geri bildirimi taşır.
 */
@Composable
fun Modifier.clickablePixel(
    onClick: () -> Unit,
    contentDescription: String? = null
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    var chain = this.clickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick
    )
    if (contentDescription != null) {
        chain = chain.semantics { this.contentDescription = contentDescription }
    }
    return chain
}
