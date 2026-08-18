package com.waifuhtr.pixelstore.ui.art

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tohumdan üretilen piksel görseller.
 *
 * Görseller düşük çözünürlüklü bir [Bitmap]'e piksel piksel yazılır, sonra
 * [FilterQuality.None] ile büyütülerek çizilir. Böylece hiçbir ikili görsel dosya taşınmaz,
 * katalog büyüdükçe APK büyümez ve kenarlar her ekran yoğunluğunda keskin kalır.
 *
 * Üretilen bitmap'ler LRU önbellekte tutulur; aynı tohum ikinci kez çizilirken yeniden
 * hesaplanmaz (liste kaydırmada önemli).
 */
object PixelArt {

    /* ---- Paletler --------------------------------------------------------------------------- */

    data class Palette(
        val deep: Int,
        val dark: Int,
        val mid: Int,
        val light: Int,
        val accent: Int,
        val sky: Int
    )

    private fun hex(value: Long): Int = value.toInt()

    val palettes: Map<String, Palette> = linkedMapOf(
        "emerald" to Palette(hex(0xFF0D2A24), hex(0xFF14503F), hex(0xFF2F9E6A), hex(0xFF7EE2A8), hex(0xFFFFC93C), hex(0xFF1D3B52)),
        "amber" to Palette(hex(0xFF2B1A08), hex(0xFF7A4A12), hex(0xFFD08B2C), hex(0xFFF7D489), hex(0xFFFF8F5C), hex(0xFF3A2A12)),
        "crimson" to Palette(hex(0xFF2A0D16), hex(0xFF7A1F33), hex(0xFFD2384F), hex(0xFFFF8FA3), hex(0xFFFFC93C), hex(0xFF3B1420)),
        "azure" to Palette(hex(0xFF0B1C33), hex(0xFF1C3F6E), hex(0xFF3D7FC4), hex(0xFF8FD0FF), hex(0xFFFFC93C), hex(0xFF132A47)),
        "violet" to Palette(hex(0xFF1D1030), hex(0xFF43206B), hex(0xFF8149C9), hex(0xFFC9A5F7), hex(0xFF57E8A0), hex(0xFF241640)),
        "slate" to Palette(hex(0xFF14161F), hex(0xFF333A4D), hex(0xFF697291), hex(0xFFB6BED6), hex(0xFFFFC93C), hex(0xFF1C2030)),
        "mint" to Palette(hex(0xFF0C231F), hex(0xFF175048), hex(0xFF33A394), hex(0xFF8EF0DD), hex(0xFFFFD166), hex(0xFF12312C))
    )

    val paletteNames: List<String> = palettes.keys.toList()

    fun palette(name: String?): Palette = palettes[name] ?: palettes.getValue("slate")

    private val INK = hex(0xFF090910)
    private val WHITE = hex(0xFFFFFFFF)
    private const val TRANSPARENT = 0

    /* ---- Deterministik rastgelelik ---------------------------------------------------------- */

    private fun hashSeed(text: String): Int {
        var h = -2128831035 // 2166136261 imzalı karşılığı
        for (ch in text) {
            h = h xor ch.code
            h *= 16777619
        }
        return h
    }

    /** xorshift32 — aynı tohum her zaman aynı görseli üretir. */
    private class Rng(seed: Int) {
        private var state: Int = if (seed == 0) 1 else seed
        fun next(): Float {
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            return (state.toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
        }
        fun nextInt(bound: Int): Int = if (bound <= 0) 0 else (next() * bound).toInt().coerceIn(0, bound - 1)
    }

    /* ---- Piksel tuvali --------------------------------------------------------------------- */

    private class Buf(val w: Int, val h: Int) {
        val px = IntArray(w * h)

        fun rect(x: Int, y: Int, rw: Int, rh: Int, color: Int) {
            if (rw <= 0 || rh <= 0) return
            val x0 = x.coerceIn(0, w)
            val y0 = y.coerceIn(0, h)
            val x1 = (x + rw).coerceIn(0, w)
            val y1 = (y + rh).coerceIn(0, h)
            for (yy in y0 until y1) {
                val row = yy * w
                for (xx in x0 until x1) px[row + xx] = color
            }
        }

        fun toBitmap(): ImageBitmap =
            Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    /* ---- İkon ------------------------------------------------------------------------------- */

    private const val ICON_GRID = 16

    /**
     * 16x16 simetrik "yaratık" ikonu: sol yarı tohumdan üretilir, sağ yarı aynalanır, gözler ve
     * ağız sabit konumdadır. Böylece her tohum farklı ama daima bir karakter gibi okunur.
     */
    private fun buildIcon(seed: String, paletteName: String): ImageBitmap {
        val pal = palette(paletteName)
        val rng = Rng(hashSeed(seed))
        val n = ICON_GRID
        val buf = Buf(n, n)

        buf.rect(0, 0, n, n, pal.deep)
        // Pahlı köşeler
        buf.rect(0, 0, 1, 1, TRANSPARENT)
        buf.rect(n - 1, 0, 1, 1, TRANSPARENT)
        buf.rect(0, n - 1, 1, 1, TRANSPARENT)
        buf.rect(n - 1, n - 1, 1, 1, TRANSPARENT)
        buf.rect(1, 0, n - 2, 1, INK)
        buf.rect(1, n - 1, n - 2, 1, INK)
        buf.rect(0, 1, 1, n - 2, INK)
        buf.rect(n - 1, 1, 1, n - 2, INK)

        val body = intArrayOf(pal.mid, pal.light, pal.dark, pal.accent)
        for (y in 3..12) {
            for (x in 3..7) {
                val density = if (x >= 5) 0.72f else 0.48f
                if (rng.next() > density) continue
                val color = body[rng.nextInt(body.size)]
                buf.rect(x, y, 1, 1, color)
                buf.rect(n - 1 - x, y, 1, 1, color)
            }
        }

        buf.rect(5, 7, 2, 2, INK)
        buf.rect(n - 7, 7, 2, 2, INK)
        buf.rect(5, 7, 1, 1, WHITE)
        buf.rect(n - 7, 7, 1, 1, WHITE)
        buf.rect(6, 10, n - 12, 1, INK)
        buf.rect(3, 12, n - 6, 1, pal.deep)

        return buf.toBitmap()
    }

    /* ---- Sahneler --------------------------------------------------------------------------- */

    enum class Scene(val id: String, val label: String) {
        TITLE("title", "Açılış"),
        FIELD("field", "Ova"),
        BATTLE("battle", "Savaş"),
        TOWN("town", "Kasaba"),
        CAVE("cave", "Mağara"),
        MENU("menu", "Menü");

        companion object {
            fun from(id: String?): Scene = entries.firstOrNull { it.id == id } ?: FIELD
        }
    }

    enum class ShotKind(val id: String, val label: String, val w: Int, val h: Int) {
        PHONE("phone", "Telefon", 108, 192),
        TABLET("tablet", "Tablet", 176, 132),
        /** Yalnızca "haftanın oyunu" afişi için; kayıt düzenleyicide seçilemez. */
        BANNER("banner", "Afiş", 208, 94);

        companion object {
            fun from(id: String?): ShotKind = when (id) {
                "tablet" -> TABLET
                "banner" -> BANNER
                else -> PHONE
            }

            /** Kayıt düzenleyicide sunulan cihaz tipleri. */
            val selectable = listOf(PHONE, TABLET)
        }
    }

    /** RPG Maker tarzı pencere çerçevesi. */
    private fun window(buf: Buf, x: Int, y: Int, w: Int, h: Int, pal: Palette) {
        if (w < 4 || h < 4) return
        buf.rect(x, y, w, h, INK)
        buf.rect(x + 1, y + 1, w - 2, h - 2, pal.deep)
        buf.rect(x + 1, y + 1, w - 2, 1, pal.light)
        buf.rect(x + 1, y + h - 2, w - 2, 1, pal.dark)
        buf.rect(x + 1, y + 1, 1, h - 2, pal.light)
        buf.rect(x + w - 2, y + 1, 1, h - 2, pal.dark)
        buf.rect(x + 1, y + 1, 1, 1, pal.accent)
        buf.rect(x + w - 2, y + 1, 1, 1, pal.accent)
        buf.rect(x + 1, y + h - 2, 1, 1, pal.accent)
        buf.rect(x + w - 2, y + h - 2, 1, 1, pal.accent)
    }

    /** Piksel ölçeğinde gerçek yazı okunmaz; metin yerine sahte satırlar çizilir. */
    private fun fakeText(buf: Buf, x: Int, y: Int, width: Int, color: Int, rng: Rng) {
        var cursor = x
        while (cursor < x + width) {
            var len = 1 + rng.nextInt(3)
            if (cursor + len > x + width) len = x + width - cursor
            if (len <= 0) break
            buf.rect(cursor, y, len, 1, color)
            cursor += len + 1
        }
    }

    private fun sprite(buf: Buf, x: Int, y: Int, pal: Palette, rng: Rng, tall: Boolean) {
        val h = if (tall) 8 else 6
        buf.rect(x, y, 4, h, pal.mid)
        buf.rect(x, y, 4, 3, pal.light)
        buf.rect(x + 1, y + 1, 1, 1, INK)
        buf.rect(x + 2, y + 1, 1, 1, INK)
        buf.rect(x, y + h - 1, 1, 1, INK)
        buf.rect(x + 3, y + h - 1, 1, 1, INK)
        if (rng.next() > 0.5f) buf.rect(x + 4, y + 3, 1, 3, pal.accent)
    }

    private fun ground(buf: Buf, y: Int, w: Int, h: Int, pal: Palette, rng: Rng) {
        buf.rect(0, y, w, h, pal.dark)
        var i = 0
        while (i < w) {
            buf.rect(i, y, 1, h, pal.deep)
            if (rng.next() > 0.6f) buf.rect(i + 2, y + 1, 1, 1, pal.mid)
            i += 4
        }
        buf.rect(0, y, w, 1, pal.mid)
    }

    private fun buildScene(seed: String, scene: Scene, paletteName: String, kind: ShotKind): ImageBitmap {
        val pal = palette(paletteName)
        val rng = Rng(hashSeed(seed))
        val w = kind.w
        val h = kind.h
        val buf = Buf(w, h)

        when (scene) {
            Scene.TITLE -> {
                buf.rect(0, 0, w, h, pal.deep)
                repeat(40) {
                    buf.rect(rng.nextInt(w), rng.nextInt((h * 0.6f).toInt()), 1, 1,
                        if (rng.next() > 0.7f) pal.accent else pal.light)
                }
                for (x in 0 until w) {
                    val peak = (h * 0.62f + sin(x / 7f) * 5f + sin(x / 3f) * 2f).toInt()
                    buf.rect(x, peak, 1, h - peak, pal.dark)
                }
                buf.rect(0, h - 6, w, 6, INK)
                window(buf, (w * 0.12f).toInt(), (h * 0.2f).toInt(), (w * 0.76f).toInt(), 18, pal)
                fakeText(buf, (w * 0.18f).toInt(), (h * 0.2f).toInt() + 6, (w * 0.64f).toInt(), pal.accent, rng)
                fakeText(buf, (w * 0.24f).toInt(), (h * 0.2f).toInt() + 11, (w * 0.5f).toInt(), pal.light, rng)
                window(buf, (w * 0.28f).toInt(), h - 26, (w * 0.44f).toInt(), 14, pal)
                fakeText(buf, (w * 0.33f).toInt(), h - 21, (w * 0.34f).toInt(), pal.light, rng)
            }

            Scene.FIELD -> {
                buf.rect(0, 0, w, h, pal.sky)
                repeat(4) {
                    val cx = rng.nextInt(w)
                    val cy = 4 + rng.nextInt((h * 0.22f).toInt())
                    buf.rect(cx, cy, 7, 2, pal.light)
                    buf.rect(cx + 1, cy - 1, 4, 1, pal.light)
                }
                val horizon = (h * 0.42f).toInt()
                ground(buf, horizon, w, h - horizon, pal, rng)
                buf.rect((w * 0.42f).toInt(), horizon, (w * 0.16f).toInt(), h - horizon, pal.mid)
                repeat(5) {
                    val tx = rng.nextInt(w - 8)
                    val ty = horizon + 2 + rng.nextInt((h - horizon - 12).coerceAtLeast(1))
                    buf.rect(tx + 2, ty + 5, 2, 3, pal.deep)
                    buf.rect(tx, ty, 6, 5, pal.light)
                    buf.rect(tx + 1, ty + 1, 4, 3, pal.mid)
                }
                sprite(buf, (w * 0.47f).toInt(), horizon + ((h - horizon) * 0.45f).toInt(), pal, rng, true)
                window(buf, 3, 3, (w * 0.42f).toInt(), 12, pal)
                fakeText(buf, 6, 8, (w * 0.34f).toInt(), pal.accent, rng)
            }

            Scene.BATTLE -> {
                buf.rect(0, 0, w, h, pal.deep)
                var i = 0
                while (i < w) { buf.rect(i, 0, 1, h, pal.dark); i += 2 }
                buf.rect(0, (h * 0.55f).toInt(), w, (h * 0.45f).toInt(), pal.dark)
                val ew = (w * 0.34f).toInt()
                val ex = (w - ew) / 2
                val ey = (h * 0.18f).toInt()
                buf.rect(ex, ey, ew, (h * 0.3f).toInt(), pal.mid)
                buf.rect(ex + 2, ey + 3, ew - 4, 4, INK)
                buf.rect(ex + 4, ey + 4, 3, 2, pal.accent)
                buf.rect(ex + ew - 7, ey + 4, 3, 2, pal.accent)
                buf.rect(ex - 2, ey + (h * 0.12f).toInt(), 2, 8, pal.dark)
                buf.rect(ex + ew, ey + (h * 0.12f).toInt(), 2, 8, pal.dark)
                sprite(buf, (w * 0.18f).toInt(), (h * 0.6f).toInt(), pal, rng, false)
                sprite(buf, (w * 0.32f).toInt(), (h * 0.64f).toInt(), pal, rng, false)
                window(buf, 2, h - 30, (w * 0.4f).toInt(), 28, pal)
                for (r in 0 until 3) {
                    fakeText(buf, 6, h - 25 + r * 6, (w * 0.3f).toInt(), if (r == 0) pal.accent else pal.light, rng)
                }
                window(buf, (w * 0.44f).toInt(), h - 30, (w * 0.54f).toInt(), 28, pal)
                for (b in 0 until 3) {
                    val by = h - 25 + b * 6
                    val len = (w * 0.3f * (0.4f + rng.next() * 0.6f)).toInt()
                    buf.rect((w * 0.48f).toInt(), by, len, 2, if (b == 0) pal.light else pal.accent)
                }
            }

            Scene.TOWN -> {
                buf.rect(0, 0, w, h, pal.sky)
                val groundY = (h * 0.6f).toInt()
                repeat(4) {
                    val bw = 10 + rng.nextInt(12)
                    val bh = 14 + rng.nextInt(16)
                    val bx = rng.nextInt((w - bw).coerceAtLeast(1))
                    val by = groundY - bh
                    buf.rect(bx, by, bw, bh, pal.dark)
                    buf.rect(bx, by, bw, 3, pal.mid)
                    buf.rect(bx + 1, by - 2, bw - 2, 2, pal.mid)
                    var wy = by + 5
                    while (wy < groundY - 4) {
                        var wx = bx + 2
                        while (wx < bx + bw - 3) {
                            buf.rect(wx, wy, 2, 2, if (rng.next() > 0.4f) pal.accent else pal.deep)
                            wx += 5
                        }
                        wy += 5
                    }
                }
                ground(buf, groundY, w, h - groundY, pal, rng)
                sprite(buf, (w * 0.3f).toInt(), groundY + 4, pal, rng, false)
                sprite(buf, (w * 0.62f).toInt(), groundY + 8, pal, rng, false)
                window(buf, 3, h - 22, w - 6, 19, pal)
                fakeText(buf, 7, h - 17, w - 16, pal.light, rng)
                fakeText(buf, 7, h - 12, w - 24, pal.light, rng)
            }

            Scene.CAVE -> {
                buf.rect(0, 0, w, h, INK)
                var y = 0
                while (y < h) {
                    var x = 0
                    while (x < w) {
                        if (rng.next() > 0.55f) buf.rect(x, y, 3, 3, pal.deep)
                        x += 3
                    }
                    y += 3
                }
                val cx = w / 2
                for (yy in 0 until h) {
                    val half = (w * 0.28f + sin(yy / 6f) * 6f).toInt()
                    buf.rect(cx - half, yy, half * 2, 1, pal.dark)
                }
                for (t in 0 until 3) {
                    val tx = if (t % 2 == 0) cx - (w * 0.22f).toInt() else cx + (w * 0.18f).toInt()
                    val ty = 12 + t * (h * 0.26f).toInt()
                    buf.rect(tx, ty, 2, 4, pal.mid)
                    buf.rect(tx, ty - 3, 2, 3, pal.accent)
                    buf.rect(tx - 1, ty - 2, 4, 2, pal.accent)
                }
                sprite(buf, cx - 2, (h * 0.62f).toInt(), pal, rng, true)
                window(buf, 2, 2, (w * 0.5f).toInt(), 12, pal)
                fakeText(buf, 5, 7, (w * 0.42f).toInt(), pal.accent, rng)
            }

            Scene.MENU -> {
                buf.rect(0, 0, w, h, pal.deep)
                var i = 0
                while (i < h) { buf.rect(0, i, w, 1, pal.dark); i += 4 }
                window(buf, 2, 2, (w * 0.44f).toInt(), h - 4, pal)
                for (r in 0 until 7) {
                    val y2 = 8 + r * 9
                    if (y2 > h - 10) break
                    if (r == 1) buf.rect(4, y2 - 2, (w * 0.4f).toInt(), 7, pal.dark)
                    fakeText(buf, 6, y2, (w * 0.34f).toInt(), if (r == 1) pal.accent else pal.light, rng)
                }
                window(buf, (w * 0.48f).toInt(), 2, (w * 0.5f).toInt(), (h * 0.55f).toInt(), pal)
                sprite(buf, (w * 0.53f).toInt(), 8, pal, rng, true)
                for (s in 0 until 4) {
                    fakeText(buf, (w * 0.62f).toInt(), 8 + s * 7, (w * 0.3f).toInt(), pal.light, rng)
                }
                window(buf, (w * 0.48f).toInt(), (h * 0.6f).toInt(), (w * 0.5f).toInt(), (h * 0.36f).toInt(), pal)
                for (g in 0 until 3) {
                    val gy = (h * 0.6f).toInt() + 6 + g * 7
                    val len = (w * 0.4f * (0.35f + rng.next() * 0.65f)).toInt()
                    buf.rect((w * 0.52f).toInt(), gy, len, 3, if (g == 0) pal.accent else pal.mid)
                }
            }
        }

        buf.rect(0, 0, w, 1, INK)
        buf.rect(0, h - 1, w, 1, INK)
        buf.rect(0, 0, 1, h, INK)
        buf.rect(w - 1, 0, 1, h, INK)
        return buf.toBitmap()
    }

    /* ---- Glifler ---------------------------------------------------------------------------- */

    private val glyphs: Map<String, Array<String>> = mapOf(
        "sword" to arrayOf("..#..", "..#..", ".###.", "..#..", "..#.."),
        "flame" to arrayOf("..#..", ".#.#.", ".###.", "#####", ".###."),
        "gem" to arrayOf(".###.", "#####", "#####", ".###.", "..#.."),
        "potion" to arrayOf("..#..", ".###.", ".#.#.", "#####", ".###."),
        // "Türler": 2x2 kutu ızgarası — kategori listesi olarak okunuyor.
        "grid" to arrayOf("##.##", "##.##", ".....", "##.##", "##.##"),
        "map" to arrayOf("#####", "##.##", "#.#.#", "##.##", "#####"),
        // Ayarlar: dişli. Eski çapraz çizgi "eğik çizgi" gibi görünüyordu.
        "gear" to arrayOf(".#.#.", "#####", ".###.", "#####", ".#.#."),
        "wrench" to arrayOf(".#.#.", "#####", ".###.", "#####", ".#.#."),
        // Profil: insan silüeti.
        "hero" to arrayOf(".###.", ".###.", "#####", "..#..", ".#.#."),
        // Puan göstergesi: 5x5'te gerçek yıldız okunmuyor, elmas/pırlanta kullanılıyor.
        "pip" to arrayOf("..#..", ".###.", "#####", ".###.", "..#.."),
        "star" to arrayOf("..#..", ".###.", "#####", ".###.", "..#.."),
        "shield" to arrayOf("#####", "#####", ".###.", ".###.", "..#.."),
        "bag" to arrayOf(".#.#.", "#####", "#####", "#####", ".###."),
        "search" to arrayOf(".###.", "#...#", "#...#", ".###.", "....#"),
        "pencil" to arrayOf("....#", "...##", "..##.", ".##..", "##..."),
        "cross" to arrayOf("#...#", ".#.#.", "..#..", ".#.#.", "#...#"),
        "plus" to arrayOf("..#..", "..#..", "#####", "..#..", "..#.."),
        "dotOn" to arrayOf(".###.", "#####", "#####", "#####", ".###."),
        "dotOff" to arrayOf(".###.", "#...#", "#...#", "#...#", ".###."),
        "left" to arrayOf("...#.", "..##.", ".###.", "..##.", "...#."),
        "right" to arrayOf(".#...", ".##..", ".###.", ".##..", ".#..."),
        "check" to arrayOf("....#", "...##", "#.##.", "###..", ".#..."),
        // İstek listesi
        "heart" to arrayOf(".#.#.", "#####", "#####", ".###.", "..#.."),
        // İndirme: aşağı ok + taban çizgisi
        "download" to arrayOf("..#..", "#####", ".###.", "..#..", "#####"),
        // İnceleme oyları
        "up" to arrayOf("..#..", ".###.", "#####", ".....", "....."),
        "down" to arrayOf(".....", ".....", "#####", ".###.", "..#.."),
        // Uyarı / hata bildirimi
        "warn" to arrayOf("..#..", "..#..", "..#..", ".....", "..#..")
    )

    /**
     * Uygulama arması — başlatıcı ikonuyla aynı çizim (çanta + mücevher).
     *
     * Üst çubukta ve giriş ekranında tohumdan üretilen rastgele bir yaratık yerine bu sabit
     * çizim kullanılır; marka kimliği başlatıcı ikonuyla tutarlı olsun.
     */
    private val crestArt = arrayOf(
        "..................",
        ".......CCCC.......",
        "......C....C......",
        "......C....C......",
        "......C....C......",
        "......C....C......",
        "..##############..",
        "..#GGGGGGGGGGGG#..",
        "..#GGGGGDDGGGGG#..",
        "..#GGGGDDDDGGGG#..",
        "..#GGGDDDDDDGGG#..",
        "..#GGDDDDDDDDGG#..",
        "..#gggDDDDDDggg#..",
        "..#ggggDDDDgggg#..",
        "..#gggggDDggggg#..",
        "..#gggggggggggg#..",
        "..##############..",
        ".................."
    )

    private fun buildCrest(): ImageBitmap {
        val buf = Buf(18, 18)
        val colors = mapOf(
            '#' to INK,
            'G' to hex(0xFFFFC93C),
            'g' to hex(0xFFB8801C),
            'C' to hex(0xFF57E8A0),
            'D' to hex(0xFF1D1D2E)
        )
        for (y in crestArt.indices) {
            for (x in crestArt[y].indices) {
                colors[crestArt[y][x]]?.let { buf.rect(x, y, 1, 1, it) }
            }
        }
        return buf.toBitmap()
    }

    val glyphNames: List<String> = glyphs.keys.toList()

    private fun buildGlyph(name: String, argb: Int): ImageBitmap {
        val art = glyphs[name] ?: glyphs.getValue("star")
        val buf = Buf(5, 5)
        for (y in art.indices) {
            for (x in art[y].indices) {
                if (art[y][x] == '#') buf.rect(x, y, 1, 1, argb)
            }
        }
        return buf.toBitmap()
    }

    /* ---- Önbellek --------------------------------------------------------------------------- */

    private val cache = LruCache<String, ImageBitmap>(220)

    fun icon(seed: String, paletteName: String?): ImageBitmap {
        val key = "i|$seed|$paletteName"
        cache[key]?.let { return it }
        return buildIcon(seed, paletteName ?: "slate").also { cache.put(key, it) }
    }

    fun shot(seed: String, scene: Scene, paletteName: String?, kind: ShotKind): ImageBitmap {
        val key = "s|$seed|${scene.id}|$paletteName|${kind.id}"
        cache[key]?.let { return it }
        return buildScene(seed, scene, paletteName ?: "slate", kind).also { cache.put(key, it) }
    }

    fun glyph(name: String, color: Color): ImageBitmap {
        val argb = color.toArgb()
        val key = "g|$name|$argb"
        cache[key]?.let { return it }
        return buildGlyph(name, argb).also { cache.put(key, it) }
    }

    fun crest(): ImageBitmap {
        val key = "crest"
        cache[key]?.let { return it }
        return buildCrest().also { cache.put(key, it) }
    }

    fun clearCache() = cache.evictAll()

    /** Bitmap'i tam olarak hedef alana, piksel yumuşatma olmadan çizer. */
    fun DrawScope.drawPixelImage(image: ImageBitmap) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}

/* ---- Composable sarmalayıcılar ------------------------------------------------------------- */

@Composable
fun PixelIconImage(seed: String, palette: String?, modifier: Modifier = Modifier) {
    val image = PixelArt.icon(seed, palette)
    Canvas(modifier = modifier) {
        with(PixelArt) { drawPixelImage(image) }
    }
}

@Composable
fun PixelShotImage(
    seed: String,
    scene: PixelArt.Scene,
    palette: String?,
    kind: PixelArt.ShotKind,
    modifier: Modifier = Modifier
) {
    val image = PixelArt.shot(seed, scene, palette, kind)
    Canvas(modifier = modifier) {
        with(PixelArt) { drawPixelImage(image) }
    }
}

@Composable
fun PixelGlyphImage(name: String, tint: Color, modifier: Modifier = Modifier) {
    val image = PixelArt.glyph(name, tint)
    Canvas(modifier = modifier) {
        with(PixelArt) { drawPixelImage(image) }
    }
}

/** Uygulama arması: başlatıcı ikonuyla aynı çizim. */
@Composable
fun PixelCrestImage(modifier: Modifier = Modifier) {
    val image = PixelArt.crest()
    Canvas(modifier = modifier) {
        with(PixelArt) { drawPixelImage(image) }
    }
}
