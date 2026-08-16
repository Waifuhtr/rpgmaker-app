# Teslim dosyaları

| Dosya | Ne işe yarar |
|---|---|
| `pixelstore-hf-space.zip` | Hugging Face Space paketi. Aç, içindeki `pixelstore-space/` klasörünün **içeriğini** Space deposunun köküne koy ve push et. |
| `PixelStore-debug.apk` | Derlenmiş APK (debug imzalı). Telefona doğrudan kurulabilir. |

Bu iki dosya `scripts/make-space-zip.sh` ve `./gradlew assembleDebug` ile yeniden üretilebilir;
depoda tutulmalarının tek nedeni doğrudan indirilebilir olmaları.

## Space kurulumu

1. huggingface.co/new-space → **SDK: Docker** → Blank
2. Zip'i aç; `pixelstore-space/` içindeki `Dockerfile`, `README.md`, `server.py`,
   `.dockerignore` ve `project/` klasörünü Space deposunun köküne kopyala
3. Push et

Space imajı derlenirken Android SDK iner ve `./gradlew assembleDebug` çalışır. İlk derleme
tipik olarak 10–20 dakika sürer. Açılış sayfası APK'yı, kaynak zip'ini ve build günlüğünü sunar;
`/demo/` altında arayüzün tarayıcı sürümü çalışır.

## APK kurulumu

Debug imzalıdır — Play Store dağıtımı için release imzası gerekir (bkz. ana `README.md`,
"Release imzalama"). Telefonda "bilinmeyen kaynaklardan kuruluma izin ver" gerekir.

Demo hesaplar: `admin` / `admin123` (yönetici) · `user` / `user123` (kullanıcı)
