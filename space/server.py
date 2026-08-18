#!/usr/bin/env python3
"""
PixelStore Space sunucusu.

Yalnızca Python standart kütüphanesini kullanır. Görevi:
  - Derleme çıktılarını (APK, kaynak zip, build günlüğü) indirilebilir yapmak
  - WordPress eklentisini ve tema kopyasını indirilebilir zip olarak sunmak
  - Eklenti testinin sonucunu ve derleme durumunu piksel temalı bir sayfada göstermek

Uygulamanın veritabanı riaslink.fun'dır; adres APK içine gömülüdür, burada ayarlanmaz.

Hugging Face Spaces uygulamayı $PORT (varsayılan 7860) üzerinden bekler.
"""

import http.server
import os
import socketserver
import string

ROOT = os.path.dirname(os.path.abspath(__file__))
ARTIFACTS = os.path.join(ROOT, "artifacts")
ASSETS = os.path.join(ROOT, "assets")
PORT = int(os.environ.get("PORT", "7860"))

APK_NAME = "PixelStore-debug.apk"
SOURCE_NAME = "pixelstore-source.zip"
PLUGIN_NAME = "pixelstore-wordpress-plugin.zip"
THEME_NAME = "steamlike-theme.zip"
LOG_NAME = "build-log.txt"
PLUGIN_LOG_NAME = "plugin-test-log.txt"

# Uygulamanın gömülü sunucu adresi; sayfada bilgi olarak gösterilir.
SITE = "riaslink.fun"


def artifact_path(name):
    return os.path.join(ARTIFACTS, name)


def human_size(path):
    try:
        size = os.path.getsize(path)
    except OSError:
        return None
    for unit in ("B", "KB", "MB", "GB"):
        if size < 1024 or unit == "GB":
            return ("%.1f %s" % (size, unit)).replace(".0 ", " ")
        size /= 1024.0
    return None


PAGE = string.Template("""<!DOCTYPE html>
<html lang="tr">
<head>
<meta charset="utf-8">
<title>PixelStore — Space</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="dark">
<style>
@font-face {
  font-family: 'Press Start 2P';
  src: url('/assets/press_start_2p.ttf') format('truetype');
  font-display: swap;
}
:root {
  --bg:#10101c; --panel:#1b1b2f; --panel2:#232342; --line:#3a3a63; --ink:#0a0a12;
  --gold:#f2c14e; --mint:#6be3a0; --rose:#ff6b8b; --sky:#57c7ff; --violet:#a77bf3;
  --text:#e8e8f5; --muted:#9a9ac2;
}
* { box-sizing: border-box; }
body {
  margin:0; padding:32px 16px 64px; background:var(--bg); color:var(--text);
  font-family:'Press Start 2P','Courier New',monospace; font-size:10px; line-height:2;
}
body::after {
  content:''; position:fixed; inset:0; pointer-events:none;
  background:repeating-linear-gradient(to bottom, rgba(0,0,0,.16) 0, rgba(0,0,0,.16) 1px, transparent 1px, transparent 3px);
  opacity:.5;
}
.wrap { max-width:640px; margin:0 auto; display:flex; flex-direction:column; gap:20px; }
.frame {
  background:var(--panel); padding:18px;
  box-shadow:0 -4px 0 0 var(--line),0 4px 0 0 var(--line),-4px 0 0 0 var(--line),4px 0 0 0 var(--line);
}
h1 { font-size:18px; color:var(--gold); margin:0; letter-spacing:2px; text-align:center; }
h2 { font-size:11px; color:var(--mint); margin:0 0 12px; }
p  { margin:0 0 10px; color:#cfcfe6; }
.muted { color:var(--muted); font-size:8px; }
.crest { width:72px; height:72px; margin:0 auto; display:block; image-rendering:pixelated; }
.status { text-align:center; font-size:9px; padding:10px; }
.status--ok   { background:#17402f; color:var(--mint);
  box-shadow:0 -4px 0 0 #2f7f5a,0 4px 0 0 #2f7f5a,-4px 0 0 0 #2f7f5a,4px 0 0 0 #2f7f5a; }
.status--fail { background:#46182a; color:var(--rose);
  box-shadow:0 -4px 0 0 #8a2c47,0 4px 0 0 #8a2c47,-4px 0 0 0 #8a2c47,4px 0 0 0 #8a2c47; }
.links { display:flex; flex-direction:column; gap:14px; }
a.btn {
  display:flex; align-items:center; justify-content:space-between; gap:10px;
  padding:14px; text-decoration:none; font-size:9px; letter-spacing:1px;
  background:var(--panel2); color:var(--text);
  box-shadow:0 -4px 0 0 var(--line),0 4px 0 0 var(--line),-4px 0 0 0 var(--line),4px 0 0 0 var(--line);
}
a.btn:hover { transform:translateY(-2px); }
a.btn--gold { background:var(--gold); color:var(--ink);
  box-shadow:0 -4px 0 0 #c9932f,0 4px 0 0 #c9932f,-4px 0 0 0 #c9932f,4px 0 0 0 #c9932f; }
a.btn--mint { background:#17402f; color:var(--mint);
  box-shadow:0 -4px 0 0 #2f7f5a,0 4px 0 0 #2f7f5a,-4px 0 0 0 #2f7f5a,4px 0 0 0 #2f7f5a; }
a.btn[aria-disabled="true"] { opacity:.45; pointer-events:none; }
a.btn small { font-size:8px; opacity:.8; }
table { width:100%; border-collapse:collapse; font-size:8px; }
th, td { text-align:left; padding:8px 6px; border-bottom:2px solid #2a2a48; }
th { color:var(--gold); }
code { color:var(--sky); }
.tag { display:inline-block; background:var(--panel2); color:var(--violet); padding:3px 6px; font-size:7px; }
ul { margin:0; padding-left:18px; }
li { margin-bottom:8px; color:#cfcfe6; }
</style>
</head>
<body>
<div class="wrap">

  <div>
    <svg class="crest" viewBox="0 0 18 18" xmlns="http://www.w3.org/2000/svg" shape-rendering="crispEdges">
      <rect x="7" y="1" width="4" height="1" fill="#6be3a0"/>
      <rect x="6" y="2" width="1" height="4" fill="#6be3a0"/>
      <rect x="11" y="2" width="1" height="4" fill="#6be3a0"/>
      <rect x="2" y="6" width="14" height="11" fill="#0a0a12"/>
      <rect x="3" y="7" width="12" height="5" fill="#f2c14e"/>
      <rect x="3" y="12" width="12" height="4" fill="#c9932f"/>
      <rect x="8" y="8" width="2" height="1" fill="#1b1b2f"/>
      <rect x="7" y="9" width="4" height="1" fill="#1b1b2f"/>
      <rect x="6" y="10" width="6" height="1" fill="#1b1b2f"/>
      <rect x="5" y="11" width="8" height="1" fill="#1b1b2f"/>
      <rect x="6" y="12" width="6" height="1" fill="#1b1b2f"/>
      <rect x="7" y="13" width="4" height="1" fill="#1b1b2f"/>
      <rect x="8" y="14" width="2" height="1" fill="#1b1b2f"/>
    </svg>
    <h1>PIXELSTORE</h1>
    <p class="muted" style="text-align:center">$site oyun kütüphanesi · Kotlin + Jetpack Compose</p>
  </div>

  <div class="status status--$status_class">$status_text</div>
  <div class="status status--$plugin_status_class">$plugin_status_text</div>

  <div class="frame">
    <h2>İNDİRMELER</h2>
    <div class="links">
      <a class="btn btn--gold" href="/$apk_name" $apk_disabled download>
        <span>APK İNDİR</span><small>$apk_size</small>
      </a>
      <a class="btn btn--mint" href="/$plugin_name" $plugin_disabled download>
        <span>WORDPRESS EKLENTİSİ (ZIP)</span><small>$plugin_size</small>
      </a>
      <a class="btn" href="/$theme_name" $theme_disabled download>
        <span>STEAMLIKE TEMA (ZIP)</span><small>$theme_size</small>
      </a>
      <a class="btn" href="/$source_name" $source_disabled download>
        <span>KAYNAK KODU (ZIP)</span><small>$source_size</small>
      </a>
      <a class="btn" href="/$log_name" $log_disabled>
        <span>BUILD GÜNLÜĞÜ</span><small>$log_size</small>
      </a>
      <a class="btn" href="/$plugin_log_name" $plugin_log_disabled>
        <span>EKLENTİ TEST GÜNLÜĞÜ</span><small>$plugin_log_size</small>
      </a>
    </div>
  </div>

  <div class="frame">
    <h2>KURULUM</h2>
    <ul>
      <li><strong>1.</strong> <em>WORDPRESS EKLENTİSİ</em> zip'ini indir →
          <code>$site/wp-admin</code> → Eklentiler → Yeni ekle → Yükle → Etkinleştir.</li>
      <li><strong>2.</strong> <em>APK</em>'yı indir, telefonda "bilinmeyen kaynaklara izin ver" ile kur.</li>
      <li><strong>3.</strong> Uygulamayı aç ve <strong>sitedeki kendi hesabınla</strong> giriş yap.
          Yetki WordPress rolünden okunur; yönetici sekmesi yalnızca yöneticide oluşur.</li>
    </ul>
    <p class="muted" style="margin-top:12px">
      Demo hesap yoktur: kullanıcı veritabanı sitenin WordPress kullanıcı tablosudur. Sunucu adresi
      APK içinde gömülüdür, uygulamada ayarlanmaz. Sitede yayımlanmış <strong>tüm eski oyunlar</strong>
      uygulamada kendiliğinden görünür — eklenti temanın <code>game</code> kayıt tipini okur, kendi
      şeması yoktur, bu yüzden veri taşımak gerekmez.
    </p>
  </div>

  <div class="frame">
    <h2>ORTAK VERİ</h2>
    <table>
      <tr><th>ÖZELLİK</th><th>SİTEDEKİ KARŞILIĞI</th></tr>
      <tr><td>İstek listesi</td><td><code>sl_favorites</code></td></tr>
      <tr><td>Puanlama</td><td><code>sl_user_rating_*</code></td></tr>
      <tr><td>Yorum / inceleme</td><td><code>comment_type = review</code></td></tr>
      <tr><td>İndirme sayacı</td><td><code>game_download_count</code></td></tr>
      <tr><td>Profil fotoğrafı</td><td><code>sl_custom_avatar</code></td></tr>
      <tr><td>Hata bildirimi</td><td><code>sl_report</code> kayıt tipi</td></tr>
      <tr><td colspan="2" class="muted">Uygulamada yapılan her işlem sitede, sitede yapılan her işlem uygulamada görünür.</td></tr>
    </table>
  </div>

  <p class="muted" style="text-align:center">min SDK 24 · target SDK 35 · Jetpack Compose · WebView yok</p>
</div>
</body>
</html>
""")


class Handler(http.server.SimpleHTTPRequestHandler):
    """Açılış sayfasını üretir, geri kalan her şeyi statik dosya olarak sunar."""

    extensions_map = dict(http.server.SimpleHTTPRequestHandler.extensions_map)
    extensions_map.update({
        ".apk": "application/vnd.android.package-archive",
        ".zip": "application/zip",
        ".ttf": "font/ttf",
        ".json": "application/json",
        ".txt": "text/plain; charset=utf-8",
    })

    def do_GET(self):
        if self.path in ("/", "/index.html"):
            self.send_landing()
            return
        # Çıktı dosyaları kök altından sunulur; diğer her şey dosya sisteminden.
        stripped = self.path.lstrip("/").split("?", 1)[0]
        if stripped in (APK_NAME, SOURCE_NAME, PLUGIN_NAME, THEME_NAME, LOG_NAME, PLUGIN_LOG_NAME):
            self.path = "/artifacts/" + stripped
        super().do_GET()

    def send_landing(self):
        apk = artifact_path(APK_NAME)
        source = artifact_path(SOURCE_NAME)
        plugin = artifact_path(PLUGIN_NAME)
        theme = artifact_path(THEME_NAME)
        log = artifact_path(LOG_NAME)
        plugin_log = artifact_path(PLUGIN_LOG_NAME)
        failed = os.path.exists(artifact_path("BUILD_FAILED")) or not os.path.exists(apk)
        plugin_failed = os.path.exists(artifact_path("PLUGIN_TEST_FAILED"))

        if not os.path.exists(plugin_log):
            plugin_status_class, plugin_status_text = "fail", "EKLENTİ TESTİ ÇALIŞMADI"
        elif plugin_failed:
            plugin_status_class, plugin_status_text = "fail", "EKLENTİ TESTİ BAŞARISIZ — GÜNLÜĞE BAK"
        else:
            plugin_status_class, plugin_status_text = "ok", "EKLENTİ TESTİ GEÇTİ"

        body = PAGE.substitute(
            site=SITE,
            status_class="fail" if failed else "ok",
            status_text=(
                "DERLEME BAŞARISIZ — BUILD GÜNLÜĞÜNE BAK"
                if failed else "DERLEME BAŞARILI — APK HAZIR"
            ),
            plugin_status_class=plugin_status_class,
            plugin_status_text=plugin_status_text,
            apk_name=APK_NAME,
            source_name=SOURCE_NAME,
            log_name=LOG_NAME,
            plugin_name=PLUGIN_NAME,
            theme_name=THEME_NAME,
            plugin_log_name=PLUGIN_LOG_NAME,
            apk_size=human_size(apk) or "yok",
            source_size=human_size(source) or "yok",
            plugin_size=human_size(plugin) or "yok",
            theme_size=human_size(theme) or "yok",
            log_size=human_size(log) or "yok",
            plugin_log_size=human_size(plugin_log) or "yok",
            apk_disabled="" if os.path.exists(apk) else 'aria-disabled="true"',
            source_disabled="" if os.path.exists(source) else 'aria-disabled="true"',
            plugin_disabled="" if os.path.exists(plugin) else 'aria-disabled="true"',
            theme_disabled="" if os.path.exists(theme) else 'aria-disabled="true"',
            log_disabled="" if os.path.exists(log) else 'aria-disabled="true"',
            plugin_log_disabled="" if os.path.exists(plugin_log) else 'aria-disabled="true"',
        ).encode("utf-8")

        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        # Space günlüğünü sadeleştir: yalnızca hata kodlarını yaz.
        if args and str(args[1]).startswith(("4", "5")):
            super().log_message(fmt, *args)


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


def main():
    os.makedirs(ARTIFACTS, exist_ok=True)
    os.chdir(ROOT)
    print("PixelStore Space -> http://0.0.0.0:%d" % PORT)
    print("  artifacts:", sorted(os.listdir(ARTIFACTS)))
    print("  assets:", "var" if os.path.isdir(ASSETS) else "yok")
    with Server(("0.0.0.0", PORT), Handler) as httpd:
        httpd.serve_forever()


if __name__ == "__main__":
    main()
