---
title: PixelStore APK Builder
emoji: 🕹️
colorFrom: indigo
colorTo: yellow
sdk: docker
app_port: 7860
pinned: false
license: mit
short_description: riaslink.fun oyun kütüphanesi için piksel tarzı Android uygulaması - APK derleyici
---

# PixelStore — Space

Bu Space **riaslink.fun** oyun kütüphanesinin Android uygulamasını derler ve indirilebilir hale
getirir.

İmaj kurulurken sırayla:

1. Android SDK indirilir ve `./gradlew assembleDebug` çalıştırılır → `PixelStore-debug.apk`.
2. WordPress eklentisi zip'lenir → `pixelstore-wordpress-plugin.zip`.
3. Eklentinin test takımı çalıştırılır (sahte WordPress ile, gerçek kurulum gerekmez) →
   `plugin-test-log.txt`. Sonuç açılış sayfasında geçti/başarısız olarak görünür.
4. Temanın depodaki kopyası zip'lenir → `steamlike-theme.zip`.

Hepsi açılış sayfasından indirilir. Derleme başarısız olsa bile Space açılır ve `build-log.txt`
bağlantısı gösterilir.

## Kurulum sırası

1. **Eklenti:** `pixelstore-wordpress-plugin.zip` → `riaslink.fun/wp-admin` → Eklentiler →
   Yeni ekle → Yükle → Etkinleştir.
2. **Uygulama:** `PixelStore-debug.apk` → telefonda "bilinmeyen kaynaklara izin ver" ile kur.
3. **Giriş:** Sitedeki kendi hesabınla giriş yap.

Demo hesap yoktur — kullanıcı veritabanı sitenin WordPress kullanıcı tablosudur. Yönetici sekmesi
yalnızca WordPress rolü yönetici olan hesapta oluşturulur; kullanıcı rolünde arayüzde hiç yer
almaz ve sunucudaki yönetim uçları da veri döndürmez.

Sunucu adresi APK içine gömülüdür (`BuildConfig.API_BASE`), uygulamada ayarlanmaz.

## Sitede yayımlanmış eski oyunlar

Eklenti kendi kayıt tipini tanımlamaz; temanın var olan `game` kayıt tipini ve `game_*`
taksonomilerini okur. Bu yüzden sitede daha önce yayımlanmış **tüm oyunlar** eklenti
etkinleştirildiği anda uygulamada görünür. Veri taşımak veya kayıtları tek tek yeniden yüklemek
gerekmez.

## Yapı

```
Dockerfile      derleme + çalışma zamanı imajı
server.py       açılış sayfası ve dosya sunucusu (yalnızca Python standart kütüphanesi)
project/        Android (Kotlin + Jetpack Compose) kaynak kodu, eklenti ve tema
```

Arayüzün tamamı Jetpack Compose ile yerel olarak çizilir. Projede WebView yoktur; HTML/CSS/JS
yalnızca bu açılış sayfasında kullanılır.

## İlk derleme ne kadar sürer?

Android SDK indirmesi ve Gradle bağımlılıkları nedeniyle ilk imaj derlemesi tipik olarak
10–20 dakika sürer. Sonraki derlemelerde Docker katman önbelleği devreye girer.

## Yerelde derlemek

```bash
export ANDROID_HOME=/path/to/android-sdk
cd project
./gradlew assembleDebug
# çıktı: app/build/outputs/apk/debug/app-debug.apk

# eklenti testleri (WordPress kurulumu gerekmez)
php wordpress-plugin/tests/bridge-test.php
```
