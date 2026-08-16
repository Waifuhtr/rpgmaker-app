#!/usr/bin/env bash
#
# Hugging Face Space paketi üretir.
#
# Çıktı zip'inin kökü doğrudan Space deposunun kökü olacak biçimde düzenlenir:
#
#   Dockerfile      -> Space'in derleme tarifi
#   README.md       -> HF frontmatter (sdk: docker, app_port: 7860)
#   server.py       -> açılış sayfası ve dosya sunucusu
#   project/        -> Android kaynak kodunun tamamı
#
# Kullanım: scripts/make-space-zip.sh [çıktı-dizini]

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${1:-$ROOT/dist}"
STAGE="$(mktemp -d)"
PKG="$STAGE/pixelstore-space"

cleanup() { rm -rf "$STAGE"; }
trap cleanup EXIT

echo "==> Space paketi hazırlanıyor"
mkdir -p "$PKG/project"

# Space kökü
cp "$ROOT/space/Dockerfile" "$PKG/Dockerfile"
cp "$ROOT/space/README.md" "$PKG/README.md"
cp "$ROOT/space/server.py" "$PKG/server.py"
cp "$ROOT/space/.dockerignore" "$PKG/.dockerignore"

# Android projesi. Build çıktıları ve yerel ayarlar dışarıda bırakılır; imzalama sırrı zaten
# depoda yok ama kopyalanmadığından emin olalım.
copy_item() {
  local item="$1"
  [ -e "$ROOT/$item" ] || return 0
  cp -R "$ROOT/$item" "$PKG/project/"
}

for item in app gradle gradlew gradlew.bat settings.gradle.kts build.gradle.kts \
            gradle.properties scripts docs README.md LICENSE .gitignore; do
  copy_item "$item"
done

rm -rf "$PKG/project/app/build" "$PKG/project/build" "$PKG/project/.gradle"
rm -f  "$PKG/project/local.properties" "$PKG/project/keystore.properties"
find "$PKG/project" -name '*.jks' -delete
find "$PKG/project" -name '*.keystore' -delete

if [ ! -f "$PKG/project/app/src/main/assets/www/index.html" ]; then
  echo "HATA: assets/www/index.html pakette yok, Space boş ekran açar." >&2
  exit 1
fi

chmod +x "$PKG/project/gradlew"

mkdir -p "$OUT_DIR"
ZIP="$OUT_DIR/pixelstore-hf-space.zip"
rm -f "$ZIP"
(cd "$STAGE" && zip -q -r "$ZIP" "pixelstore-space")

echo "==> Hazır: $ZIP ($(du -h "$ZIP" | cut -f1))"
echo
echo "Yükleme:"
echo "  1) huggingface.co/new-space -> SDK: Docker -> Blank"
echo "  2) Zip'i aç, 'pixelstore-space' klasörünün İÇİNDEKİLERİ Space deposunun köküne koy"
echo "  3) Push et; Space imajı derlerken APK da üretilir"
