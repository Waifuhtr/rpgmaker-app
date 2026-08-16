#!/usr/bin/env python3
"""
PixelStore launcher ikonlarını üretir.

İkon 18x18'lik bir piksel ızgarasından nearest-neighbour ölçeklenerek çizilir; böylece her
yoğunlukta kenarlar keskin kalır ve depoda ikili görsel düzenleme aracına ihtiyaç olmaz.

Kullanım:  python3 scripts/generate_icons.py
Çıktı:     app/src/main/res/mipmap-*/ic_launcher.png ve ic_launcher_foreground.png
"""

import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

# 18x18 piksel ızgarası: alışveriş çantası + mücevher.
GRID = [
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
    "..................",
]

PALETTE = {
    ".": (0, 0, 0, 0),
    "#": (0x0A, 0x0A, 0x12, 0xFF),   # kontur
    "G": (0xF2, 0xC1, 0x4E, 0xFF),   # altın gövde
    "g": (0xC9, 0x93, 0x2F, 0xFF),   # altın gölge
    "C": (0x6B, 0xE3, 0xA0, 0xFF),   # nane sap
    "D": (0x1B, 0x1B, 0x2F, 0xFF),   # mücevher
}

# Legacy (mipmap) boyutları ve adaptive-icon foreground boyutları (108dp tuval).
LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FOREGROUND_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

# Adaptive ikonlarda dış %25'lik bant kırpılabilir; sanatı güvenli alana sığdır.
FOREGROUND_ART_RATIO = 0.62
LEGACY_ART_RATIO = 0.94


def write_png(path, width, height, pixels):
    """pixels: (x, y) -> (r, g, b, a) döndüren fonksiyon."""
    rows = []
    for y in range(height):
        row = bytearray()
        row.append(0)  # filter type 0 (None)
        for x in range(width):
            row.extend(pixels(x, y))
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(tag, data):
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    blob = b"\x89PNG\r\n\x1a\n"
    blob += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    blob += chunk(b"IDAT", zlib.compress(raw, 9))
    blob += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(blob)


def render(path, canvas, art_ratio, background=None):
    grid_size = len(GRID)
    art_px = max(grid_size, int(canvas * art_ratio) // grid_size * grid_size)
    scale = art_px // grid_size
    art_px = scale * grid_size
    offset = (canvas - art_px) // 2

    def pixel(x, y):
        gx = (x - offset) // scale
        gy = (y - offset) // scale
        if 0 <= gx < grid_size and 0 <= gy < grid_size and offset <= x and offset <= y:
            color = PALETTE[GRID[gy][gx]]
            if color[3] != 0:
                return color
        return background if background else (0, 0, 0, 0)

    write_png(path, canvas, canvas, pixel)


def main():
    ink = (0x1B, 0x1B, 0x2F, 0xFF)
    for density, size in LEGACY_SIZES.items():
        render(
            os.path.join(RES, f"mipmap-{density}", "ic_launcher.png"),
            size,
            LEGACY_ART_RATIO,
            background=ink,
        )
    for density, size in FOREGROUND_SIZES.items():
        render(
            os.path.join(RES, f"mipmap-{density}", "ic_launcher_foreground.png"),
            size,
            FOREGROUND_ART_RATIO,
        )
    print("İkonlar üretildi:", ", ".join(sorted(LEGACY_SIZES)))


if __name__ == "__main__":
    main()
