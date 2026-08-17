package com.waifuhtr.pixelstore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waifuhtr.pixelstore.R

/**
 * PixelStore renk paleti.
 *
 * v1'deki arayüz "boğuk" bulunmuştu: zemin ile yüzey arasındaki fark azdı, her yazı aynı küçük
 * piksel yazı tipindeydi ve üstte tarama çizgisi katmanı vardı. Bu palette:
 *  - zemin/yüzey/yükseltilmiş yüzey arasında belirgin üç kademe var
 *  - metin kontrastı yükseltildi (ana metin neredeyse beyaz, ikincil metin açık lila)
 *  - vurgular parlatıldı, her anlam için ayrı renk ayrıldı
 */
object PixelColors {
    val Backdrop = Color(0xFF12121C)
    val Surface = Color(0xFF1D1D2E)
    val SurfaceHigh = Color(0xFF2A2A44)
    val SurfaceSunken = Color(0xFF14141F)
    val Ink = Color(0xFF090910)

    val Outline = Color(0xFF46466F)
    val OutlineSoft = Color(0xFF303052)

    val Gold = Color(0xFFFFC93C)
    val GoldDeep = Color(0xFFB8801C)
    val Mint = Color(0xFF57E8A0)
    val MintDeep = Color(0xFF1F7C56)
    val Sky = Color(0xFF62CBFF)
    val SkyDeep = Color(0xFF215E85)
    val Rose = Color(0xFFFF7A94)
    val RoseDeep = Color(0xFF8E2340)
    val Violet = Color(0xFFB98CFF)
    val VioletDeep = Color(0xFF4E2E86)
    val Amber = Color(0xFFFF9D5C)

    val TextPrimary = Color(0xFFF4F4FF)
    val TextSecondary = Color(0xFFB4B4DC)
    val TextTertiary = Color(0xFF8C8CB4)
    val OnGold = Color(0xFF1A1206)
}

/** Piksel ızgarası: tüm boşluklar ve çerçeve kalınlıkları bunun katlarıdır. */
object PixelSpacing {
    val frame = 3.dp
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xlarge = 24.dp
    val gutter = 16.dp
}

val PixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))

/**
 * İki yazı tipi kullanılır:
 *  - Press Start 2P yalnızca kısa metinlerde (başlık, etiket, düğme, sayı)
 *  - uzun metinler sistem yazı tipinde (paragraflar piksel yazı tipinde okunmuyordu)
 */
private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val PixelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PixelFont, fontSize = 22.sp, lineHeight = 32.sp, letterSpacing = 1.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PixelFont, fontSize = 17.sp, lineHeight = 26.sp, letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PixelFont, fontSize = 14.sp, lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PixelFont, fontSize = 12.sp, lineHeight = 19.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PixelFont, fontSize = 10.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PixelFont, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PixelFont, fontSize = 9.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PixelFont, fontSize = 8.sp, lineHeight = 12.sp, letterSpacing = 0.5.sp
    ),
    // Uzun metinler: sistem yazı tipi, geniş satır aralığı.
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 23.sp,
        lineHeightStyle = tightLineHeight
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontSize = 13.5.sp, lineHeight = 20.sp,
        lineHeightStyle = tightLineHeight
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp,
        lineHeightStyle = tightLineHeight
    )
)

private val PixelColorScheme = darkColorScheme(
    primary = PixelColors.Gold,
    onPrimary = PixelColors.OnGold,
    secondary = PixelColors.Mint,
    onSecondary = PixelColors.Ink,
    tertiary = PixelColors.Sky,
    background = PixelColors.Backdrop,
    onBackground = PixelColors.TextPrimary,
    surface = PixelColors.Surface,
    onSurface = PixelColors.TextPrimary,
    surfaceVariant = PixelColors.SurfaceHigh,
    onSurfaceVariant = PixelColors.TextSecondary,
    outline = PixelColors.Outline,
    error = PixelColors.Rose,
    onError = PixelColors.Ink
)

@Composable
fun PixelStoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PixelColorScheme,
        typography = PixelTypography,
        content = content
    )
}

/* ---- Piksel çerçeve çizimi ---------------------------------------------------------------- */

/**
 * Pahlı köşeli piksel çerçevesi. Tüm çizim düğümün sınırları içinde kalır (kardeş bileşenlerin
 * üstüne taşmaz); köşe kareleri [corner] rengiyle doldurularak "kesik köşe" görünümü verilir.
 */
fun Modifier.pixelFrame(
    fill: Color,
    border: Color,
    corner: Color = PixelColors.Backdrop,
    thickness: Dp = PixelSpacing.frame
) = this.drawBehind {
    val t = thickness.toPx()
    val w = size.width
    val h = size.height
    if (w <= 2 * t || h <= 2 * t) {
        drawRect(fill)
        return@drawBehind
    }
    drawRect(fill)
    drawRect(border, Offset(t, 0f), Size(w - 2 * t, t))
    drawRect(border, Offset(t, h - t), Size(w - 2 * t, t))
    drawRect(border, Offset(0f, t), Size(t, h - 2 * t))
    drawRect(border, Offset(w - t, t), Size(t, h - 2 * t))
    drawRect(corner, Offset(0f, 0f), Size(t, t))
    drawRect(corner, Offset(w - t, 0f), Size(t, t))
    drawRect(corner, Offset(0f, h - t), Size(t, t))
    drawRect(corner, Offset(w - t, h - t), Size(t, t))
}

/** Üst kenarında ışık, alt kenarında gölge olan kabartılmış piksel yüzeyi (düğmeler için). */
fun Modifier.pixelRaised(
    fill: Color,
    highlight: Color,
    shadow: Color,
    corner: Color = PixelColors.Backdrop,
    thickness: Dp = PixelSpacing.frame
) = this.drawBehind {
    val t = thickness.toPx()
    val w = size.width
    val h = size.height
    if (w <= 2 * t || h <= 2 * t) {
        drawRect(fill)
        return@drawBehind
    }
    drawRect(fill)
    drawRect(highlight, Offset(t, 0f), Size(w - 2 * t, t))
    drawRect(highlight, Offset(0f, t), Size(t, h - 2 * t))
    drawRect(shadow, Offset(t, h - t), Size(w - 2 * t, t))
    drawRect(shadow, Offset(w - t, t), Size(t, h - 2 * t))
    drawRect(corner, Offset(0f, 0f), Size(t, t))
    drawRect(corner, Offset(w - t, 0f), Size(t, t))
    drawRect(corner, Offset(0f, h - t), Size(t, t))
    drawRect(corner, Offset(w - t, h - t), Size(t, t))
}
