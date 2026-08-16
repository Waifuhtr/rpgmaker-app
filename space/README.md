---
title: PixelStore APK Builder
emoji: 🕹️
colorFrom: indigo
colorTo: yellow
sdk: docker
app_port: 7860
pinned: false
license: mit
short_description: RPG Maker tarzı piksel uygulama mağazası - APK derleyici ve canlı demo
---

# PixelStore — Space

Bu Space iki iş yapar:

1. **APK derler.** Docker imajı kurulurken Android SDK indirilir ve `./gradlew assembleDebug`
   çalıştırılır. Üretilen `app-debug.apk` açılış sayfasından indirilir.
2. **Canlı demo sunar.** Uygulamanın arayüzü tamamen HTML/CSS/JS olduğu için aynı arayüz
   `/demo/` altında tarayıcıda da çalışır. Demo modunda köprü, native yerine tarayıcı yedeğini
   kullanır (veriler `localStorage`'da tutulur).

## Demo hesaplar

| Rol | Kullanıcı | Parola |
|---|---|---|
| Yönetici | `admin` | `admin123` |
| Kullanıcı | `user` | `user123` |

Yönetim sekmesi yalnızca yönetici oturumunda oluşturulur. APK'da yetki kararı Kotlin tarafında
verilir; kullanıcı rolünde yönetim uçları veri değil hata döndürür.

## Yapı

```
Dockerfile      derleme + çalışma zamanı imajı
server.py       açılış sayfası ve dosya sunucusu (yalnızca Python standart kütüphanesi)
project/        Android (Kotlin + WebView) kaynak kodunun tamamı
```

## İlk derleme ne kadar sürer?

Android SDK indirmesi ve Gradle bağımlılıkları nedeniyle ilk imaj derlemesi tipik olarak
10–20 dakika sürer. Sonraki derlemelerde Docker katman önbelleği devreye girer.

Derleme başarısız olursa Space yine de açılır ve açılış sayfasında `build-log.txt` bağlantısı
gösterilir.

## Yerelde derlemek

```bash
export ANDROID_HOME=/path/to/android-sdk
cd project
./gradlew assembleDebug
# çıktı: app/build/outputs/apk/debug/app-debug.apk
```
