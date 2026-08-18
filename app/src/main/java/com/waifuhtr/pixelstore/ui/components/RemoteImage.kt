package com.waifuhtr.pixelstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.waifuhtr.pixelstore.ui.art.PixelArt
import com.waifuhtr.pixelstore.ui.art.PixelIconImage
import com.waifuhtr.pixelstore.ui.art.PixelShotImage
import com.waifuhtr.pixelstore.ui.theme.PixelColors

/**
 * Siteden gelen gerçek görsel.
 *
 * Kapaklar ve ekran görüntüleri WordPress medya kütüphanesinden gelir; bu yüzden yumuşak ölçekleme
 * kullanılır (piksel motoru yalnızca arayüz çerçevesi için). Görsel yoksa veya yüklenemezse
 * tohumdan üretilen piksel sahne yer tutucu olarak çizilir — boş gri kutu görünmez.
 */
@Composable
fun RemoteImage(
    url: String,
    fallbackSeed: String,
    modifier: Modifier = Modifier,
    palette: String = "slate",
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    if (url.isBlank()) {
        PixelFallback(fallbackSeed, palette, modifier)
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            // Liste kaydırmada aynı kapak tekrar indirilmesin.
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { PixelFallback(fallbackSeed, palette, Modifier.fillMaxSize()) },
        error = { PixelFallback(fallbackSeed, palette, Modifier.fillMaxSize()) }
    )
}

/**
 * Yer tutucu: kutunun tamamını dolduran piksel çizim.
 *
 * Kare kutular (profil fotoğrafı) tohumdan üretilen yaratık ikonunu alır. Dikdörtgen kutular —
 * kapaklar, kahraman görseli, ekran görüntüleri — oran uyan bir sahne alır; böylece 16:9 alanda
 * kenarlarda siyah bant kalmaz. Seçim tohumdan türetilir, yani aynı oyun her yerde aynı görünür.
 */
@Composable
private fun PixelFallback(
    seed: String,
    palette: String,
    modifier: Modifier = Modifier
) {
    val key = seed.ifBlank { "pixelstore" }
    BoxWithConstraints(modifier = modifier.background(PixelColors.SurfaceSunken)) {
        val ratio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
        if (ratio > 0.82f && ratio < 1.22f) {
            PixelIconImage(key, palette, Modifier.fillMaxSize())
        } else {
            PixelShotImage(
                seed = key,
                scene = sceneFor(key),
                palette = palette,
                kind = if (ratio >= 1f) PixelArt.ShotKind.BANNER else PixelArt.ShotKind.PHONE,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Tohumdan sabit bir sahne seçer: aynı oyun her açılışta aynı yer tutucuyu alır.
 *
 * MENU sahnesi listede kullanılmaz; arayüz taslağı gibi göründüğü için oyun kapağı yerine
 * geçmiyor.
 */
private val fallbackScenes = listOf(
    PixelArt.Scene.TITLE,
    PixelArt.Scene.FIELD,
    PixelArt.Scene.BATTLE,
    PixelArt.Scene.TOWN,
    PixelArt.Scene.CAVE
)

private fun sceneFor(seed: String): PixelArt.Scene {
    var hash = 0
    for (ch in seed) hash = hash * 31 + ch.code
    return fallbackScenes[((hash % fallbackScenes.size) + fallbackScenes.size) % fallbackScenes.size]
}
