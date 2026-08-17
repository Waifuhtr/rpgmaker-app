# Teslim dosyaları

| Dosya | Ne işe yarar |
|---|---|
| `PixelStore-debug.apk` | Derlenmiş APK (debug imzalı). Telefona doğrudan kurulabilir. |
| `pixelstore-wordpress-plugin.zip` | WordPress eklentisi. wp-admin → Eklentiler → Yeni ekle → Eklenti yükle. |
| `pixelstore-hf-space.zip` | Hugging Face Space paketi. Aç, `pixelstore-space/` klasörünün **içeriğini** Space deposunun köküne koy. |

Üçü de `scripts/make-space-zip.sh` ve `./gradlew assembleDebug` ile yeniden üretilebilir;
depoda tutulmalarının tek nedeni doğrudan indirilebilir olmaları.

## APK kurulumu

1. `PixelStore-debug.apk` dosyasını telefona indir
2. "Bilinmeyen kaynaklardan kuruluma izin ver" onayını ver
3. Kur ve aç

Kurulumdan sonra uygulama **yerel kipte** çalışır: katalog cihazda saklanır, internet gerekmez.

Demo hesaplar: `admin` / `admin123` (yönetici) · `user` / `user123` (kullanıcı)

Debug imzalıdır — Play Store dağıtımı için release imzası gerekir (bkz. ana `README.md`).

## WordPress'i veritabanı yapmak

1. `pixelstore-wordpress-plugin.zip` dosyasını wp-admin'den kur ve etkinleştir
2. wp-admin → **PixelStore** → "Demo kataloğu kur"
3. Aynı ekranda yazan site adresini uygulamada **Profil → Bağlantı ayarları**'na gir
4. "Bağlantıyı sına" ile doğrula, "Kaydet"e bas
5. WordPress kullanıcı adın ve parolanla giriş yap

Yönetim paneli, WordPress'te `manage_options` veya `edit_others_posts` yeteneği olan
kullanıcılara açılır (yönetici ve editör).

## Space kurulumu

1. huggingface.co/new-space → **SDK: Docker** → Blank
2. `pixelstore-hf-space.zip` içindeki `pixelstore-space/` klasörünün içeriğini (Dockerfile,
   README.md, server.py, .dockerignore, project/) Space deposunun köküne kopyala
3. Push et

Space imajı derlenirken Android SDK iner ve `./gradlew assembleDebug` çalışır. İlk derleme
tipik olarak 10–20 dakika sürer. Açılış sayfası APK'yı, kaynak zip'ini, WordPress eklentisi
zip'ini ve build günlüğünü sunar.
