# PixelStore — riaslink.fun oyun kütüphanesi

RPG Maker / 8-bit piksel tarzında bir Android uygulaması. Veritabanı **riaslink.fun** sitesidir:
sitede yayımlanan her oyun, her üye, her yorum, her puan ve her istek listesi doğrudan uygulamada
görünür — ve tersi de geçerlidir.

Arayüzün tamamı **Jetpack Compose** ile yerel olarak çizilir. Projede WebView yoktur.

```
┌──────────────────────────┐        ┌──────────────────────────────────┐
│  PixelStore (Android)    │        │  riaslink.fun (WordPress)         │
│  Kotlin + Compose        │◄──────►│  SteamLike teması                 │
│  BuildConfig.API_BASE    │  HTTPS │  + PixelStore Bridge eklentisi    │
│  (adres kodda gömülü)    │  JSON  │  game CPT · game_* taksonomileri  │
└──────────────────────────┘        └──────────────────────────────────┘
```

## Neden veri taşımak gerekmiyor

Eklenti **kendi kayıt tipini tanımlamaz.** Temanın var olan `game` kayıt tipini, `game_*`
taksonomilerini ve `sl_*` post meta alanlarını okur/yazar. Bu yüzden eklenti etkinleştirildiği
anda sitede **daha önce yayımlanmış tüm oyunlar** uygulamada görünür. Tek tek yeniden yükleme,
içe aktarma veya eşitleme adımı yoktur.

Aynı nedenle uygulama iki yönlü çalışır:

| Uygulamadaki özellik | Sitedeki karşılığı |
|---|---|
| İstek listesi | `sl_favorites` kullanıcı meta |
| Puanlama (1–5) | `sl_user_rating_sum` / `_count` / `_avg` |
| Yorum / inceleme | `wp_comments`, `comment_type = review` |
| Yorum oyları | `sl_upvotes` / `sl_downvotes` |
| İndirme sayacı | `game_download_count` |
| Görüntülenme | `game_view_count` |
| Profil fotoğrafı | `sl_custom_avatar` |
| Rozetler | temanın `sl_get_user_badges()` işlevi |
| Hata bildirimi | `sl_report` kayıt tipi |
| Yönetici düzenlemesi | `game` kaydının kendisi + medya kütüphanesi |

Puanlamada tek fark: tema oyu çerezle sayar, uygulamada çerez yoktur. Eklenti bu yüzden kişi
başına oyu `psb_rating_voters` içinde tutar — kullanıcı oyunu değiştirdiğinde toplam düzeltilir,
oy sayısı şişmez. Temanın gördüğü ortalama alanları aynı kalır.

## Kurulum

**1. Eklenti** — `release/pixelstore-wordpress-plugin.zip`

```
riaslink.fun/wp-admin → Eklentiler → Yeni ekle → Eklenti yükle → Zip'i seç → Etkinleştir
```

Etkinleştirdikten sonra `Ayarlar → PixelStore` sayfası temanın bulunup bulunmadığını, kaç oyun
ve kaç kullanıcı göründüğünü yazar.

**2. Uygulama** — `release/PixelStore-debug.apk`

Telefonda "bilinmeyen kaynaklardan kuruluma izin ver" gerekir (debug imzalı).

**3. Giriş**

Sitedeki kendi kullanıcı adı/e-posta ve parolanla giriş yap. Demo hesap yoktur.

## Yetki

Yönetici kararını **sunucu** verir: `manage_options` veya `edit_others_posts` yeteneği olan
WordPress kullanıcısı yöneticidir.

- Yönetim sekmesi kullanıcı rolünde arayüzde **hiç oluşturulmaz** (koşullu render, gizleme değil).
- Yönetim uçları `permission_admin` ile korunur: kullanıcı rolündeki bir jetonla çağrılırsa
  403 döner.
- Taslak (`draft`) kayıtlar kullanıcı rolündeki listelere hiç girmez.

Yani arayüzü atlayıp doğrudan uca istek atmak da işe yaramaz.

## Oturum

Parola cihazda **saklanmaz.** Giriş başarılı olduğunda sunucu `"<kullanıcı_id>.<48 hex>"`
biçiminde bir jeton üretir; sunucuda yalnızca `sha256` özeti tutulur. Jeton 30 gün geçerlidir ve
kullanıcı başına en çok 5 cihaz saklanır (en eskisi düşer). Uygulama açılışta jetonu doğrular,
geçersizse siler ve giriş ekranına döner.

## Uygulama ekranları

| Sekme | İçerik |
|---|---|
| **Mağaza** | Canlı arama, öne çıkan oyun, katlanabilir filtreler (tür/platform/dil/durum), sıralama (en yeni · indirme · puan · A-Z), sonsuz kaydırma |
| **Türler** | Dört taksonominin karoları; birine dokununca mağaza o filtreyle açılır |
| **Listem** | İstek listesi (site ile ortak) |
| **Profil** | Profil fotoğrafı yükleme/kaldırma, rozetler, ses/titreşim ayarı, çıkış |
| **Yönetim** | Yalnızca yönetici: site özeti, tür dağılımı, katalog yönetimi, kullanıcı listesi, oyun düzenleyici |

Oyun sayfasında: kahraman görseli, ölçü şeridi (puan · indirme · boyut), indirme (3 saniyelik
geri sayım + arşiv parolası), alternatif link, 1–5 puanlama, ekran görüntüsü galerisi, açıklama /
değişiklik günlüğü / kurulum rehberi, sistem gereksinimleri (minimum + önerilen), etiketler,
yorumlar, fragman ve hata bildirimi.

Yönetici oyun düzenleyicisinde kapak ve ekran görüntüsü yükleme, tüm künye alanları, taksonomi
seçimi (yeni terim ekleme dahil), metinler, indirme linkleri ve sistem gereksinimleri vardır.
Kaydedilen her alan doğrudan siteye yazılır.

## Görseller

Kapak ve ekran görüntüleri WordPress medya kütüphanesinden gelir (Coil ile indirilir, bellek ve
disk önbelleği açık). Bir kayıtta görsel yoksa tohumdan üretilen piksel sahne yer tutucu olarak
çizilir — boş gri kutu görünmez. Yer tutucu deterministiktir: aynı oyun her yerde aynı görünür.

Arayüz çerçeveleri, glifler ve rozetler tamamen kod tarafından çizilir (yuvarlak köşe yoktur,
pah kırılmış piksel kenar kullanılır). Ses efektleri cihazda `AudioTrack` ile üretilen kare/üçgen
dalgalardır; ses dosyası yoktur.

## REST uçları

Taban: `https://riaslink.fun/wp-json/pixelstore/v2`

| Uç | Yöntem | Yetki |
|---|---|---|
| `/health` | GET | açık |
| `/auth/login` · `/auth/logout` | POST | açık |
| `/auth/me` | GET | oturum |
| `/auth/avatar` | POST · DELETE | oturum |
| `/games` | GET | oturum |
| `/games` | POST | yönetici |
| `/games/{id}` | GET | oturum |
| `/games/{id}` | PUT · PATCH · DELETE | yönetici |
| `/games/{id}/published` | POST | yönetici |
| `/games/{id}/download` · `/view` · `/favorite` · `/rate` · `/report` | POST | oturum |
| `/games/{id}/reviews` | GET · POST | oturum |
| `/games/{id}/cover` · `/screenshots` | POST | yönetici |
| `/games/{id}/screenshots/{attachment}` | DELETE | yönetici |
| `/reviews/{review}` | DELETE | oturum (kendi yorumu) |
| `/reviews/{review}/vote` | POST | oturum |
| `/search` · `/taxonomies` · `/favorites` | GET | oturum |
| `/stats` · `/users` | GET | yönetici |

Jeton `Authorization: Bearer <jeton>` veya `X-PixelStore-Token` başlığıyla gönderilir.

## Yükleme sınırları

Görsel yüklemede en çok 8 MB; yalnızca JPEG, PNG, WebP ve GIF kabul edilir ve dosya adına değil
**içeriğe** bakılarak doğrulanır (`wp_check_filetype_and_ext`). Uygulama yüklemeden önce görseli
küçültür: profil fotoğrafı en uzun kenar 512 px, kapak 1600 px, JPEG kalite 88. Bir oyunda en çok
24 ekran görüntüsü tutulur. Yazma uçları yalnızca `http`/`https` adresleri kabul eder.

## Depo yapısı

```
app/                              Android uygulaması (Kotlin + Compose)
  src/main/java/.../data/         RiasApi, Models, HttpJson, Settings, ImagePicker
  src/main/java/.../ui/           tema, bileşenler, ekranlar, piksel çizim motoru
  src/test/java/.../              tasarım ekran görüntüsü testi (Robolectric)
wordpress-plugin/
  pixelstore-bridge/              WordPress eklentisi
  tests/                          WordPress kurulumu gerektirmeyen test takımı
wordpress-theme/steamlike/        temanın değiştirilmemiş kopyası (karşılaştırma için)
space/                            Hugging Face Space (Dockerfile + açılış sayfası)
release/                          derlenmiş APK ve zip'ler
docs/SENARYO.md                   ürün senaryosu
```

## Derleme

```bash
export ANDROID_HOME=/path/to/android-sdk   # veya local.properties içinde sdk.dir
./gradlew assembleDebug                    # app/build/outputs/apk/debug/app-debug.apk
```

Sunucu adresi `app/build.gradle.kts` içinde gömülüdür:

```kotlin
buildConfigField("String", "API_BASE", "\"https://riaslink.fun/wp-json/pixelstore/v2\"")
buildConfigField("String", "SITE_URL", "\"https://riaslink.fun\"")
```

### Testler

```bash
# Eklenti: 130 doğrulama, WordPress kurulumu gerekmez (sahte WordPress ile çalışır)
php wordpress-plugin/tests/bridge-test.php

# Arayüz: her ekranı PNG olarak çizer, emülatör gerektirmez
./gradlew :app:testDebugUnitTest --tests '*DesignScreenshotTest'
# çıktı: app/build/screenshots/*.png
```

### Hugging Face Space paketi

```bash
scripts/make-space-zip.sh          # dist/ altına üç zip üretir
```

Space imajı kurulurken APK derlenir, eklenti test takımı çalıştırılır ve hepsi Space'in açılış
sayfasından indirilebilir olur.

## Sürüm

`3.0.0` (versionCode 3) · minSdk 24 · targetSdk 35 · Kotlin 2.1.21 · AGP 8.11.1
