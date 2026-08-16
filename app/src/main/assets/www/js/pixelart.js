/*
 * PixelArt — tohumdan (seed) üretilen piksel görseller.
 *
 * Mağazadaki her ikon ve ekran görüntüsü burada canvas üzerine çizilir. Böylece uygulama tek bir
 * ikili görsel dosya taşımaz; katalog büyüdükçe APK boyutu artmaz ve admin yeni kayıt eklerken
 * hazır görsel aramak zorunda kalmaz.
 *
 * Çizim her zaman düşük çözünürlükte (ör. 16x16) yapılır ve CSS `image-rendering: pixelated` ile
 * büyütülür; bu yüzden piksel kenarları her ekran yoğunluğunda keskin kalır.
 */
(function (global) {
  'use strict';

  /* ---- Deterministik rastgelelik ---------------------------------------------------------- */

  function hashSeed(text) {
    var h = 2166136261 >>> 0;
    var str = String(text || 'seed');
    for (var i = 0; i < str.length; i++) {
      h ^= str.charCodeAt(i);
      h = Math.imul(h, 16777619) >>> 0;
    }
    return h >>> 0;
  }

  // xorshift32: aynı tohum her zaman aynı görseli üretir.
  function makeRandom(seed) {
    var s = hashSeed(seed) || 1;
    return function () {
      s ^= s << 13; s >>>= 0;
      s ^= s >>> 17;
      s ^= s << 5; s >>>= 0;
      return s / 4294967296;
    };
  }

  /* ---- Paletler ---------------------------------------------------------------------------- */

  var PALETTES = {
    emerald: { deep: '#0d2a24', dark: '#14503f', mid: '#2f9e6a', light: '#7ee2a8', accent: '#f2c14e', sky: '#1d3b52' },
    amber:   { deep: '#2b1a08', dark: '#7a4a12', mid: '#d08b2c', light: '#f7d489', accent: '#ff8f5c', sky: '#3a2a12' },
    crimson: { deep: '#2a0d16', dark: '#7a1f33', mid: '#d2384f', light: '#ff8fa3', accent: '#f2c14e', sky: '#3b1420' },
    azure:   { deep: '#0b1c33', dark: '#1c3f6e', mid: '#3d7fc4', light: '#8fd0ff', accent: '#f2c14e', sky: '#132a47' },
    violet:  { deep: '#1d1030', dark: '#43206b', mid: '#8149c9', light: '#c9a5f7', accent: '#6be3a0', sky: '#241640' },
    slate:   { deep: '#14161f', dark: '#333a4d', mid: '#697291', light: '#b6bed6', accent: '#f2c14e', sky: '#1c2030' },
    mint:    { deep: '#0c231f', dark: '#175048', mid: '#33a394', light: '#8ef0dd', accent: '#ffd166', sky: '#12312c' }
  };

  var INK = '#0a0a12';

  function palette(name) {
    return PALETTES[name] || PALETTES.slate;
  }

  function paletteNames() {
    return Object.keys(PALETTES);
  }

  /* ---- Canvas yardımcıları ----------------------------------------------------------------- */

  function makeCanvas(w, h, cssClass) {
    var canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    canvas.className = cssClass || 'px-canvas';
    return canvas;
  }

  function rect(ctx, x, y, w, h, color) {
    ctx.fillStyle = color;
    ctx.fillRect(x, y, w, h);
  }

  /** RPG Maker tarzı pencere çerçevesi: koyu dolgu, açık kenar, köşe vurguları. */
  function windowBox(ctx, x, y, w, h, pal) {
    rect(ctx, x, y, w, h, INK);
    rect(ctx, x + 1, y + 1, w - 2, h - 2, pal.deep);
    rect(ctx, x + 1, y + 1, w - 2, 1, pal.light);
    rect(ctx, x + 1, y + h - 2, w - 2, 1, pal.dark);
    rect(ctx, x + 1, y + 1, 1, h - 2, pal.light);
    rect(ctx, x + w - 2, y + 1, 1, h - 2, pal.dark);
    rect(ctx, x + 1, y + 1, 1, 1, pal.accent);
    rect(ctx, x + w - 2, y + 1, 1, 1, pal.accent);
    rect(ctx, x + 1, y + h - 2, 1, 1, pal.accent);
    rect(ctx, x + w - 2, y + h - 2, 1, 1, pal.accent);
  }

  /** Metin yerine geçen sahte satırlar — piksel ölçeğinde gerçek yazı okunmaz. */
  function fakeText(ctx, x, y, width, color, rand) {
    var cursor = x;
    while (cursor < x + width) {
      var wordLen = 1 + Math.floor((rand ? rand() : 0.5) * 3);
      if (cursor + wordLen > x + width) wordLen = x + width - cursor;
      rect(ctx, cursor, y, wordLen, 1, color);
      cursor += wordLen + 1;
    }
  }

  /* ---- İkon ------------------------------------------------------------------------------- */

  var ICON_SIZE = 16;

  /**
   * 16x16 simetrik "yaratık" ikonu. Sol yarı rastgele üretilir, sağ yarı aynalanır; gözler sabit
   * konumdadır, böylece her tohum farklı ama daima bir karakter gibi okunur.
   */
  function drawIcon(canvas, seed, paletteName) {
    var ctx = canvas.getContext('2d');
    var pal = palette(paletteName);
    var rand = makeRandom(seed);
    var n = ICON_SIZE;

    ctx.clearRect(0, 0, n, n);
    rect(ctx, 0, 0, n, n, pal.deep);
    // Köşeleri kırp: kare ikonlar yerine pahlı bir kartuş görünümü.
    ctx.clearRect(0, 0, 1, 1);
    ctx.clearRect(n - 1, 0, 1, 1);
    ctx.clearRect(0, n - 1, 1, 1);
    ctx.clearRect(n - 1, n - 1, 1, 1);
    rect(ctx, 1, 0, n - 2, 1, INK);
    rect(ctx, 1, n - 1, n - 2, 1, INK);
    rect(ctx, 0, 1, 1, n - 2, INK);
    rect(ctx, n - 1, 1, 1, n - 2, INK);

    var body = [pal.mid, pal.light, pal.dark, pal.accent];
    for (var y = 3; y <= 12; y++) {
      for (var x = 3; x <= 7; x++) {
        // Merkeze yakın hücreler daha dolu; silüet dağılmasın.
        var density = x >= 5 ? 0.72 : 0.48;
        if (rand() > density) continue;
        var color = body[Math.floor(rand() * body.length)];
        rect(ctx, x, y, 1, 1, color);
        rect(ctx, n - 1 - x, y, 1, 1, color);
      }
    }

    // Gözler ve ağız: ikonu her zaman "bakan" bir şeye çevirir.
    rect(ctx, 5, 7, 2, 2, INK);
    rect(ctx, n - 7, 7, 2, 2, INK);
    rect(ctx, 5, 7, 1, 1, '#ffffff');
    rect(ctx, n - 7, 7, 1, 1, '#ffffff');
    rect(ctx, 6, 10, n - 12, 1, INK);

    // Alt gölge, ikona hacim verir.
    rect(ctx, 3, 12, n - 6, 1, pal.deep);
    return canvas;
  }

  function iconElement(seed, paletteName, cssClass) {
    var canvas = makeCanvas(ICON_SIZE, ICON_SIZE, cssClass || 'px-icon');
    drawIcon(canvas, seed, paletteName);
    return canvas;
  }

  /* ---- Ekran görüntüsü sahneleri ----------------------------------------------------------- */

  function sprite(ctx, x, y, pal, rand, tall) {
    var h = tall ? 8 : 6;
    rect(ctx, x, y, 4, h, pal.mid);          // gövde
    rect(ctx, x, y, 4, 3, pal.light);        // baş
    rect(ctx, x + 1, y + 1, 1, 1, INK);      // gözler
    rect(ctx, x + 2, y + 1, 1, 1, INK);
    rect(ctx, x, y + h - 1, 1, 1, INK);      // ayaklar
    rect(ctx, x + 3, y + h - 1, 1, 1, INK);
    if (rand() > 0.5) rect(ctx, x + 4, y + 3, 1, 3, pal.accent); // elde bir şey
  }

  function tiledGround(ctx, y, w, h, pal, rand) {
    rect(ctx, 0, y, w, h, pal.dark);
    for (var i = 0; i < w; i += 4) {
      rect(ctx, i, y, 1, h, pal.deep);
      if (rand() > 0.6) rect(ctx, i + 2, y + 1, 1, 1, pal.mid);
    }
    rect(ctx, 0, y, w, 1, pal.mid);
  }

  var SCENES = {
    title: function (ctx, w, h, pal, rand) {
      rect(ctx, 0, 0, w, h, pal.deep);
      for (var i = 0; i < 40; i++) {
        rect(ctx, Math.floor(rand() * w), Math.floor(rand() * h * 0.6), 1, 1, rand() > 0.7 ? pal.accent : pal.light);
      }
      // Silüet dağlar
      for (var x = 0; x < w; x++) {
        var peak = Math.floor(h * 0.62 + Math.sin(x / 7) * 5 + Math.sin(x / 3) * 2);
        rect(ctx, x, peak, 1, h - peak, pal.dark);
      }
      rect(ctx, 0, h - 6, w, 6, INK);
      windowBox(ctx, Math.floor(w * 0.12), Math.floor(h * 0.2), Math.floor(w * 0.76), 18, pal);
      fakeText(ctx, Math.floor(w * 0.18), Math.floor(h * 0.2) + 6, Math.floor(w * 0.64), pal.accent, rand);
      fakeText(ctx, Math.floor(w * 0.24), Math.floor(h * 0.2) + 11, Math.floor(w * 0.5), pal.light, rand);
      windowBox(ctx, Math.floor(w * 0.28), h - 26, Math.floor(w * 0.44), 14, pal);
      fakeText(ctx, Math.floor(w * 0.33), h - 21, Math.floor(w * 0.34), pal.light, rand);
    },

    field: function (ctx, w, h, pal, rand) {
      rect(ctx, 0, 0, w, h, pal.sky);
      for (var c = 0; c < 4; c++) {
        var cx = Math.floor(rand() * w);
        var cy = 4 + Math.floor(rand() * (h * 0.22));
        rect(ctx, cx, cy, 7, 2, pal.light);
        rect(ctx, cx + 1, cy - 1, 4, 1, pal.light);
      }
      var horizon = Math.floor(h * 0.42);
      tiledGround(ctx, horizon, w, h - horizon, pal, rand);
      // Yol
      rect(ctx, Math.floor(w * 0.42), horizon, Math.floor(w * 0.16), h - horizon, pal.mid);
      // Ağaçlar
      for (var t = 0; t < 5; t++) {
        var tx = Math.floor(rand() * (w - 8));
        var ty = horizon + 2 + Math.floor(rand() * (h - horizon - 12));
        rect(ctx, tx + 2, ty + 5, 2, 3, pal.deep);
        rect(ctx, tx, ty, 6, 5, pal.light);
        rect(ctx, tx + 1, ty + 1, 4, 3, pal.mid);
      }
      sprite(ctx, Math.floor(w * 0.47), horizon + Math.floor((h - horizon) * 0.45), pal, rand, true);
      windowBox(ctx, 3, 3, Math.floor(w * 0.42), 12, pal);
      fakeText(ctx, 6, 8, Math.floor(w * 0.34), pal.accent, rand);
    },

    battle: function (ctx, w, h, pal, rand) {
      rect(ctx, 0, 0, w, h, pal.deep);
      for (var i = 0; i < w; i += 2) {
        rect(ctx, i, 0, 1, h, pal.dark);
      }
      rect(ctx, 0, Math.floor(h * 0.55), w, Math.floor(h * 0.45), pal.dark);
      // Düşman
      var ew = Math.floor(w * 0.34);
      var ex = Math.floor((w - ew) / 2);
      var ey = Math.floor(h * 0.18);
      rect(ctx, ex, ey, ew, Math.floor(h * 0.3), pal.mid);
      rect(ctx, ex + 2, ey + 3, ew - 4, 4, INK);
      rect(ctx, ex + 4, ey + 4, 3, 2, pal.accent);
      rect(ctx, ex + ew - 7, ey + 4, 3, 2, pal.accent);
      rect(ctx, ex - 2, ey + Math.floor(h * 0.12), 2, 8, pal.dark);
      rect(ctx, ex + ew, ey + Math.floor(h * 0.12), 2, 8, pal.dark);
      // Ekip
      sprite(ctx, Math.floor(w * 0.18), Math.floor(h * 0.6), pal, rand, false);
      sprite(ctx, Math.floor(w * 0.32), Math.floor(h * 0.64), pal, rand, false);
      // Komut ve durum pencereleri
      windowBox(ctx, 2, h - 30, Math.floor(w * 0.4), 28, pal);
      for (var r = 0; r < 3; r++) {
        fakeText(ctx, 6, h - 25 + r * 6, Math.floor(w * 0.3), r === 0 ? pal.accent : pal.light, rand);
      }
      windowBox(ctx, Math.floor(w * 0.44), h - 30, Math.floor(w * 0.54), 28, pal);
      for (var b = 0; b < 3; b++) {
        var by = h - 25 + b * 6;
        rect(ctx, Math.floor(w * 0.48), by, Math.floor(w * 0.3 * (0.4 + rand() * 0.6)), 2, b === 0 ? pal.light : pal.accent);
      }
    },

    town: function (ctx, w, h, pal, rand) {
      rect(ctx, 0, 0, w, h, pal.sky);
      var ground = Math.floor(h * 0.6);
      for (var b = 0; b < 4; b++) {
        var bw = 10 + Math.floor(rand() * 12);
        var bh = 14 + Math.floor(rand() * 16);
        var bx = Math.floor(rand() * (w - bw));
        var by = ground - bh;
        rect(ctx, bx, by, bw, bh, pal.dark);
        rect(ctx, bx, by, bw, 3, pal.mid);          // çatı
        rect(ctx, bx + 1, by - 2, bw - 2, 2, pal.mid);
        for (var wy = by + 5; wy < ground - 4; wy += 5) {
          for (var wx = bx + 2; wx < bx + bw - 3; wx += 5) {
            rect(ctx, wx, wy, 2, 2, rand() > 0.4 ? pal.accent : pal.deep);
          }
        }
      }
      tiledGround(ctx, ground, w, h - ground, pal, rand);
      sprite(ctx, Math.floor(w * 0.3), ground + 4, pal, rand, false);
      sprite(ctx, Math.floor(w * 0.62), ground + 8, pal, rand, false);
      windowBox(ctx, 3, h - 22, w - 6, 19, pal);
      fakeText(ctx, 7, h - 17, w - 16, pal.light, rand);
      fakeText(ctx, 7, h - 12, w - 24, pal.light, rand);
    },

    cave: function (ctx, w, h, pal, rand) {
      rect(ctx, 0, 0, w, h, INK);
      // Duvar dokusu
      for (var y = 0; y < h; y += 3) {
        for (var x = 0; x < w; x += 3) {
          if (rand() > 0.55) rect(ctx, x, y, 3, 3, pal.deep);
        }
      }
      // Mağara boşluğu
      var cx = Math.floor(w / 2);
      for (var yy = 0; yy < h; yy++) {
        var half = Math.floor(w * 0.28 + Math.sin(yy / 6) * 6);
        rect(ctx, cx - half, yy, half * 2, 1, pal.dark);
      }
      // Meşaleler
      for (var t = 0; t < 3; t++) {
        var tx = t % 2 === 0 ? cx - Math.floor(w * 0.22) : cx + Math.floor(w * 0.18);
        var ty = 12 + t * Math.floor(h * 0.26);
        rect(ctx, tx, ty, 2, 4, pal.mid);
        rect(ctx, tx, ty - 3, 2, 3, pal.accent);
        rect(ctx, tx - 1, ty - 2, 4, 2, pal.accent);
      }
      sprite(ctx, cx - 2, Math.floor(h * 0.62), pal, rand, true);
      windowBox(ctx, 2, 2, Math.floor(w * 0.5), 12, pal);
      fakeText(ctx, 5, 7, Math.floor(w * 0.42), pal.accent, rand);
    },

    menu: function (ctx, w, h, pal, rand) {
      rect(ctx, 0, 0, w, h, pal.deep);
      for (var i = 0; i < h; i += 4) {
        rect(ctx, 0, i, w, 1, pal.dark);
      }
      windowBox(ctx, 2, 2, Math.floor(w * 0.44), h - 4, pal);
      for (var r = 0; r < 7; r++) {
        var y = 8 + r * 9;
        if (y > h - 10) break;
        if (r === 1) rect(ctx, 4, y - 2, Math.floor(w * 0.4), 7, pal.dark);
        fakeText(ctx, 6, y, Math.floor(w * 0.34), r === 1 ? pal.accent : pal.light, rand);
      }
      windowBox(ctx, Math.floor(w * 0.48), 2, Math.floor(w * 0.5), Math.floor(h * 0.55), pal);
      sprite(ctx, Math.floor(w * 0.53), 8, pal, rand, true);
      for (var s = 0; s < 4; s++) {
        var sy = 8 + s * 7;
        fakeText(ctx, Math.floor(w * 0.62), sy, Math.floor(w * 0.3), pal.light, rand);
      }
      windowBox(ctx, Math.floor(w * 0.48), Math.floor(h * 0.6), Math.floor(w * 0.5), Math.floor(h * 0.36), pal);
      for (var g = 0; g < 3; g++) {
        var gy = Math.floor(h * 0.6) + 6 + g * 7;
        rect(ctx, Math.floor(w * 0.52), gy, Math.floor(w * 0.4 * (0.35 + rand() * 0.65)), 3, g === 0 ? pal.accent : pal.mid);
      }
    }
  };

  var SCREEN_SIZES = {
    phone: { w: 108, h: 192 },
    tablet: { w: 176, h: 132 }
  };

  function drawScreenshot(canvas, options) {
    var pal = palette(options.palette);
    var rand = makeRandom(options.seed);
    var ctx = canvas.getContext('2d');
    var scene = SCENES[options.scene] || SCENES.field;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    scene(ctx, canvas.width, canvas.height, pal, rand);
    // Ekran kenarı
    rect(ctx, 0, 0, canvas.width, 1, INK);
    rect(ctx, 0, canvas.height - 1, canvas.width, 1, INK);
    rect(ctx, 0, 0, 1, canvas.height, INK);
    rect(ctx, canvas.width - 1, 0, 1, canvas.height, INK);
    return canvas;
  }

  function screenshotElement(shot, paletteName, cssClass) {
    var size = SCREEN_SIZES[shot.kind === 'tablet' ? 'tablet' : 'phone'];
    var canvas = makeCanvas(size.w, size.h, cssClass || 'px-shot');
    canvas.dataset.kind = shot.kind === 'tablet' ? 'tablet' : 'phone';
    drawScreenshot(canvas, {
      seed: shot.seed,
      scene: shot.scene,
      palette: paletteName
    });
    return canvas;
  }

  /* ---- Kategori simgeleri ------------------------------------------------------------------ */

  var GLYPHS = {
    sword: ['..#..', '.###.', '.###.', '#####', '..#..'],
    flame: ['..#..', '.###.', '#####', '#.#.#', '.###.'],
    gem:   ['.###.', '#####', '#####', '.###.', '..#..'],
    potion:['..#..', '.###.', '.#.#.', '#####', '.###.'],
    map:   ['#####', '#...#', '#.#.#', '#...#', '#####'],
    wrench:['##...', '###..', '.###.', '..###', '...##'],
    star:  ['..#..', '.###.', '#####', '.#.#.', '#...#'],
    shield:['#####', '#####', '.###.', '.###.', '..#..'],
    // Simge düğmeleri için: yazı tipinde bulunmayan sembolleri piksel çizimiyle karşılarız.
    pencil:['....#', '...##', '..##.', '.##..', '##...'],
    cross: ['#...#', '.#.#.', '..#..', '.#.#.', '#...#'],
    plus:  ['..#..', '..#..', '#####', '..#..', '..#..'],
    dotOn: ['.###.', '#####', '#####', '#####', '.###.'],
    dotOff:['.###.', '#...#', '#...#', '#...#', '.###.'],
    left:  ['...#.', '..##.', '.###.', '..##.', '...#.'],
    right: ['.#...', '.##..', '.###.', '.##..', '.#...']
  };

  function glyphElement(name, color, cssClass) {
    var art = GLYPHS[name] || GLYPHS.star;
    var canvas = makeCanvas(5, 5, cssClass || 'px-glyph');
    var ctx = canvas.getContext('2d');
    for (var y = 0; y < art.length; y++) {
      for (var x = 0; x < art[y].length; x++) {
        if (art[y][x] === '#') rect(ctx, x, y, 1, 1, color || '#f2c14e');
      }
    }
    return canvas;
  }

  /* ---- Avatar ------------------------------------------------------------------------------ */

  function avatarElement(seed, isAdmin, cssClass) {
    return iconElement(seed, isAdmin ? 'amber' : 'azure', cssClass || 'px-avatar');
  }

  global.PixelArt = {
    iconElement: iconElement,
    screenshotElement: screenshotElement,
    glyphElement: glyphElement,
    avatarElement: avatarElement,
    paletteNames: paletteNames,
    palette: palette,
    sceneNames: function () { return Object.keys(SCENES); }
  };
})(window);
