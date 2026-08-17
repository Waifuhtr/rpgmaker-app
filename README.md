# PixelStore

RPG Maker estetiğinde, piksel sanatıyla çizilmiş bir **uygulama mağazası**. Play Store'un
işlevlerini (başlık, ikon, açıklama, ekran görüntüleri, türler, puan, indirme) 8-bit bir vitrinde
toplar ve rol tabanlı bir **yönetim paneli** içerir.

**Tamamen yerel Android uygulaması** — WebView yok, HTML/CSS/JS yok. Arayüzün her pikseli
Jetpack Compose ile çizilir. Veritabanı olarak cihazı veya **WordPress**'i kullanabilir.

## Ne yapar

| Ekran | İçerik |
|---|---|
| Giriş | Demo hesap kartları, manuel form, sunucu/bağlantı paneli |
| Mağaza | Haftanın oyunu afişi, arama, tür çipleri, 4 sıralama kipi, uygulama kartları |
| Türler | Piksel simgeli tür kutuları, kayıt sayısı ve dağılım çubuğu |
| Kayıt detayı | İkon, puan/indirme/boyut şeridi, indirme akışı, ekran görüntüsü galerisi + tam ekran görüntüleyici, açıklama, etiketler, bilgi tablosu |
| Profil | Kütüphane, ses/titreşim ayarları, bağlantı bilgisi, çıkış |
| **Yönetim** | İstatistik kartları, tür dağılımı, katalog CRUD, canlı önizlemeli kayıt düzenleyici, ekran görüntüsü editörü, kullanıcı listesi, katalog sıfırlama |
| Bağlantı | WordPress adresi, bağlantı sınaması, kurulum adımları |

## İki veri kaynağı

Uygulama tek başına da çalışır, WordPress'e de bağlanır. Aktif kip üst çubukta rozet olarak yazar.

| | **Yerel kip** (varsayılan) | **WordPress kipi** |
|---|---|---|
| Katalog | cihazın özel dizininde `catalog.json` | `pixelstore_app` özel yazı tipi |
| Hesaplar | gömülü `admin` / `user` | sitenin WordPress kullanıcıları |
| Yönetici kararı | Kotlin hesap tablosu | WordPress yetenekleri (`manage_options` / `edit_others_posts`) |
| İnternet | gerekmez | gerekir |
| Geçiş | Profil → Bağlantı ayarları'nda adresi boş bırak | adresi gir, kaydet |

## Demo hesaplar (yerel kip)

| Rol | Kullanıcı | Parola | Gördüğü |
|---|---|---|---|
| Yönetici | `admin` | `admin123` | Mağaza + Yönetim sekmesi + taslak kayıtlar |
| Kullanıcı | `user` | `user123` | Yalnızca mağaza |

## Yetki ayrımı nasıl çalışır

Rol kararı **veri kaynağında** verilir, arayüzde değil:

- **Veri katmanı** — kullanıcı rolünde katalog yükü taslak kayıtları ve `published`/`createdAt`
  alanlarını hiç içermez.
- **Kaynak katmanı** — `saveApp`, `deleteApp`, `setPublished`, `users`, `stats`, `resetCatalog`
  çağrıları yönetici değilse veri değil hata döner. Yerel kipte bunu `LocalCatalogSource`,
  WordPress kipinde sunucudaki yetenek denetimi yapar.
- **Arayüz katmanı** — Yönetim sekmesi kullanıcı rolünde hiç oluşturulmaz; `push()`/`openTab()`
  de `adminOnly` ekranları reddeder.

## Mimari

```
app/src/main/
├── java/com/waifuhtr/pixelstore/
│   ├── MainActivity.kt              Compose host, geri tuşu, ses/titreşim sağlayıcı
│   ├── StoreViewModel.kt            UI durumu, gezinme yığını, tüm eylemler
│   ├── data/
│   │   ├── Models.kt                Veri modelleri + JSON dönüşümleri (tek ayrıştırıcı)
│   │   ├── CatalogSource.kt         Kaynak arayüzü
│   │   ├── LocalCatalogSource.kt    Cihaz içi katalog, atomik yazma, hesap tablosu
│   │   ├── WordPressCatalogSource.kt REST istemcisi
│   │   ├── HttpJson.kt              HttpURLConnection tabanlı küçük JSON istemcisi
│   │   └── Settings.kt              Tercihler, jeton, kurulu kayıtlar
│   └── ui/
│       ├── theme/Theme.kt           Renkler, iki yazı tipli tipografi, piksel çerçeve modifier'ları
│       ├── art/PixelArt.kt          Tohumdan üretilen ikon/sahne/glif çizimi + LRU önbellek
│       ├── components/Components.kt Düğme, alan, rozet, segment çubuk, panel, anahtar…
│       ├── Feedback.kt              8-bit sesler (AudioTrack ile üretilir) + titreşim
│       └── screens/                 Her ekran tek dosya
├── res/font/press_start_2p.ttf      Piksel yazı tipi (OFL-1.1)
└── assets/catalog_seed.json         Tohum katalog

wordpress-plugin/pixelstore-api/     WordPress eklentisi (veritabanı + REST)
space/                               Hugging Face Space (APK derler, çıktıları sunar)
```

### Görseller nereden geliyor

Depoda **tek bir oyun görseli yok**. Her ikon ve ekran görüntüsü `iconSeed`/`seed` değerinden
deterministik olarak bir `Bitmap`'e piksel piksel yazılır, sonra `FilterQuality.None` ile
büyütülerek çizilir. Üretilenler LRU önbellekte tutulur, liste kaydırmada yeniden hesaplanmaz.
Altı sahne tipi vardır: açılış, ova, savaş, kasaba, mağara, menü.

Sesler de dosya değil: kare/üçgen dalga PCM tamponları çalışma zamanında üretilip `AudioTrack`
ile çalınır.

### Okunabilirlik kararları

v1'in arayüzü "boğuk" bulunmuştu. v2'de:

- **İki yazı tipi**: Press Start 2P yalnızca kısa metinlerde (başlık, etiket, düğme, sayı);
  paragraflar sistem yazı tipinde, geniş satır aralığıyla. Piksel yazı tipinde uzun metin okunmuyor.
- **Üç yüzey kademesi** (zemin / yüzey / yükseltilmiş yüzey) ve yükseltilmiş metin kontrastı.
- **Tarama çizgisi katmanı kaldırıldı** — murk'un ana kaynağıydı.
- Bölüm başlıkları glif + başlık + çizgi; kartlarda dört bilgi kademesi (başlık, geliştirici,
  açıklama, ölçüler) ayrı boyut ve renkte.

## Derleme

### Yerelde

```bash
export ANDROID_HOME=/path/to/android-sdk   # platforms;android-35, build-tools;35.0.0
./gradlew assembleDebug
# çıktı: app/build/outputs/apk/debug/app-debug.apk
```

Sürüm matrisi: Gradle 8.14.3 · AGP 8.11.1 · Kotlin 2.1.21 · Compose BOM 2025.06.00 · JDK 17 hedefi
· compileSdk 35 · minSdk 24.

Derlenmiş APK her sürümde `release/PixelStore-debug.apk` altında depoya konur.

### Tasarımı görmek (emülatörsüz)

Ekranlar Robolectric'in yerel grafik kipinde JVM üzerinde çizilip PNG'ye alınabilir:

```bash
./gradlew :app:testDebugUnitTest --tests '*DesignScreenshotTest'
# çıktı: app/build/screenshots/*.png
```

### Hugging Face Space üzerinde

```bash
scripts/make-space-zip.sh
# dist/pixelstore-hf-space.zip          -> Space deposuna
# dist/pixelstore-wordpress-plugin.zip  -> wp-admin'e
```

Zip'in içindeki `pixelstore-space/` klasörünün **içeriğini** Space deposunun köküne koyup push
edin. Space (Docker SDK, port 7860) imaj derlenirken Android SDK indirir, `assembleDebug`
çalıştırır ve açılış sayfasında APK, kaynak zip'i, WordPress eklentisi zip'i ile build günlüğünü
sunar. Derleme başarısız olsa bile Space ayağa kalkar.

## WordPress'i veritabanı yapmak

1. `wordpress-plugin/pixelstore-api` klasörünü zip'leyip WordPress'e kur ve etkinleştir
   (ya da `dist/pixelstore-wordpress-plugin.zip` dosyasını kullan).
2. wp-admin → **PixelStore** → "Demo kataloğu kur".
3. Aynı ekranda yazan site adresini uygulamada **Profil → Bağlantı ayarları**'na gir,
   "Bağlantıyı sına" ile doğrula, kaydet.
4. WordPress kullanıcı adın ve parolanla giriş yap.

Eklenti ayrıntıları: [`wordpress-plugin/pixelstore-api/readme.txt`](wordpress-plugin/pixelstore-api/readme.txt)

Uçlar `/wp-json/pixelstore/v1/` altındadır: `health`, `auth/login`, `auth/me`, `auth/logout`,
`catalog`, `apps/{id}`, `apps/{id}/install`, `apps/{id}/published`, `users`, `stats`, `seed`.

## Güvenlik notları

- **Parolalar**: yerel kipte kaynak kodda düz metin değil, sabit tuzlu SHA-256 özeti; karşılaştırma
  sabit zamanlı. WordPress kipinde parola doğrulaması `wp_authenticate()` ile sitenin kendi
  kullanıcı tablosuna karşı yapılır, eklenti parola saklamaz.
- **Jetonlar**: WordPress kipinde rastgele jeton üretilir; sunucuda yalnızca SHA-256 özeti kullanıcı
  metasında durur, 30 gün sonra düşer, kullanıcı başına en fazla 5 cihaz. Çıkış yalnızca o cihazın
  jetonunu iptal eder.
- **Rol yükseltme**: oturum sürdürülürken rol kalıcı tercihlerden değil hesap tablosundan /
  sunucudan okunur; tercihler kurcalansa bile yetki yükseltilemez.
- **Sunucu tarafı doğrulama**: uygulamadan gelen JSON şemaya göre budanır (metin uzunlukları,
  sayı aralıkları, yalnızca `http(s)` bağlantılar, en fazla 8 ekran görüntüsü). İndirme sayacı
  istemciden yazılamaz, yalnızca `/install` ucu artırır.
- **Yerel yazma**: katalog yazımı atomiktir (`.tmp` + rename); yarım dosya kataloğu bozamaz.
- **İmzalama**: `keystore.properties` depoda yoktur. Varsa release otomatik imzalanır, yoksa
  imzasız çıkar. R8 açıktır.

## Test

```bash
./gradlew :app:testDebugUnitTest      # Compose ekran render testi (PNG üretir)
php wordpress-plugin/tests/auth-smoke.php   # jeton üretimi/doğrulama/iptali (WordPress gerekmez)
```

## Lisans

Kod MIT. Press Start 2P yazı tipi SIL Open Font License 1.1 ile gelir
(`app/src/main/assets/OFL-PressStart2P.txt`).
