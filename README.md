# PixelStore

RPG Maker estetiğinde, piksel sanatıyla çizilmiş bir **uygulama mağazası** — Kotlin kabuk + WebView
arayüz. Play Store'un işlevlerini (başlık, ikon, açıklama, ekran görüntüleri, türler, puan, indirme)
8-bit bir vitrinde toplar ve rol tabanlı bir **yönetim paneli** içerir.

<!-- Ekran görüntüleri uygulamanın kendisi tarafından üretilir; depoda ikili görsel taşınmaz. -->

## Ne yapar

| Ekran | İçerik |
|---|---|
| Giriş | İki demo hesap düğmesi + manuel form |
| Mağaza | Haftanın oyunu, arama, tür filtreleri, 4 sıralama kipi, uygulama kartları |
| Tür listesi | Piksel simgeli tür kutuları, kayıt sayıları |
| Kayıt detayı | İkon, puan/indirme/boyut, indirme akışı, ekran görüntüsü galerisi, açıklama, etiketler, bilgi tablosu |
| Profil | Kütüphane, ses/titreşim/tarama-çizgisi ayarları, sürüm bilgisi, çıkış |
| **Yönetim** | İstatistik kartları, tür dağılımı, katalog CRUD, yayın durumu, kullanıcı listesi, katalog sıfırlama |

## Demo hesaplar

| Rol | Kullanıcı | Parola | Gördüğü |
|---|---|---|---|
| Yönetici | `admin` | `admin123` | Mağaza + Yönetim sekmesi + taslak kayıtlar |
| Kullanıcı | `user` | `user123` | Yalnızca mağaza |

## Yetki ayrımı nasıl çalışır

Rol kararı **native tarafta** verilir, arayüzde değil:

- `AccountManager` oturumu Kotlin tarafında tutar. JS'ten gelen "ben adminim" iddiası okunmaz.
- `StoreBridge`'deki `admin*` uçları `requireAdmin()` ile korunur; kullanıcı rolünde veri değil
  `{"ok":false,"error":"…"}` döner.
- Kullanıcı rolünde katalog yükü `publicApps()` ile üretilir: yayınlanmamış kayıtlar ve
  `published` / `createdAt` gibi yönetim alanları yüke **hiç konmaz**.
- Yönetim sekmesi kullanıcı rolünde DOM'a eklenmez — CSS ile gizlenmiş bir düğüm bırakılmaz.

Yani DevTools ile arayüz kurcalansa bile yönetim verisi elde edilemez. Bu davranış otomatik
tarayıcı testiyle doğrulanmıştır (kullanıcı rolünde 7 kayıt / yönetici rolünde 8 kayıt, taslak
rozeti 0, üç admin ucunun üçü de reddedildi).

## Mimari

```
app/src/main/
├── java/com/waifuhtr/pixelstore/
│   ├── MainActivity.kt        WebView kabuğu, gezinme kilidi, yaşam döngüsü, geri tuşu
│   ├── StoreBridge.kt         @JavascriptInterface köprüsü (dar yüzey, rol korumalı)
│   ├── Accounts.kt            Hesap tablosu, oturum, SHA-256 parola özetleri
│   └── CatalogRepository.kt   Atomik JSON deposu, şema doğrulama, istatistik
└── assets/www/                Arayüzün tamamı (HTML/CSS/JS, framework yok)
    ├── index.html             CSP + kabuk iskeleti
    ├── css/pixel.css          Piksel tema (box-shadow çerçeveler, tarama çizgileri)
    ├── fonts/                 Press Start 2P (OFL-1.1)
    ├── data/catalog.json      Tohum katalog
    └── js/
        ├── bridge.js          Native köprü + tarayıcı yedeği (Space demosu için)
        ├── pixelart.js        Tohumdan üretilen ikon/ekran görüntüsü/simge çizimi
        ├── ui.js              DOM, ses (WebAudio), bildirim, modal, segment çubuk
        ├── views.js           Kullanıcı ekranları
        ├── admin.js           Yönetim ekranları
        └── app.js             Router, oturum akışı, native geri çağrıları
```

### Neden WebView

Arayüz tamamen HTML/CSS/JS olduğu için aynı kod hem APK içinde hem tarayıcıda çalışır; Hugging Face
Space'teki canlı demo bunu kullanır. Assetler `WebViewAssetLoader` ile sabit bir `https://` origin'i
üzerinden sunulur — `file://` erişimi hiç açılmaz ve LocalStorage'daki tercihler sürüm
güncellemeleri arasında korunur.

### Görseller nereden geliyor

Depoda **tek bir oyun görseli yok**. Her ikon ve ekran görüntüsü, kaydın `iconSeed`/`seed`
değerinden deterministik olarak canvas üzerine çizilir (`pixelart.js`). Katalog büyüdükçe APK
boyutu artmaz, yönetici yeni kayıt eklerken hazır görsel aramak zorunda kalmaz. Altı sahne tipi
vardır: açılış, ova, savaş, kasaba, mağara, menü.

## Derleme

### Yerelde

```bash
export ANDROID_HOME=/path/to/android-sdk   # platforms;android-35, build-tools;35.0.0
./gradlew assembleDebug
# çıktı: app/build/outputs/apk/debug/app-debug.apk
```

Sürüm matrisi: Gradle 8.14.3 · AGP 8.11.1 · Kotlin 2.1.21 · JDK 17 hedefi · compileSdk 35 · minSdk 24.

`preBuild` öncesinde `verifyWebAssets` görevi çalışır: `index.html` yoksa veya `www` içinde
büyük/küçük harf çakışması varsa build açık hatayla durur (Android dosya adlarında harf
duyarlıdır).

### Hugging Face Space üzerinde

```bash
scripts/make-space-zip.sh          # dist/pixelstore-hf-space.zip üretir
```

Zip'in içindeki `pixelstore-space/` klasörünün **içeriğini** Space deposunun köküne koyup push edin.
Space (Docker SDK, port 7860):

1. İmaj derlenirken Android SDK iner ve `./gradlew assembleDebug` çalışır.
2. Açılış sayfası APK'yı, kaynak zip'ini ve build günlüğünü indirilebilir yapar.
3. `/demo/` altında arayüzün tarayıcı sürümü çalışır.

Derleme başarısız olsa bile Space ayağa kalkar ve günlüğü gösterir.

### Release imzalama

`keystore.properties` **depoda yoktur ve olmamalıdır**. Varsa otomatik okunur:

```properties
storeFile=release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Dosya yoksa `assembleRelease` imzasız çıktı üretir. R8 açıktır; `@JavascriptInterface` metotları
`proguard-rules.pro` ile korunur.

## Güvenlik notları

- **Gezinme kilidi:** `shouldOverrideUrlLoading` yalnızca uygulama origin'ine izin verir. Dış
  bağlantı yalnızca kullanıcı dokunuşuyla, şema/host doğrulamasından geçtikten sonra sistem
  tarayıcısına gider (`user:pass@host` biçimli adresler reddedilir).
- **Köprü yüzeyi dar:** genel amaçlı `eval`, `readFile`, `openUrl(any)` yok. Her çağrı origin
  kontrolünden geçer; WebView yerel origin dışına çıkarsa köprü kapanır.
- **CSP:** `script-src 'self'; style-src 'self'` — inline script ve inline stil yasaktır.
  (İlerleme çubukları bu yüzden inline `width` yerine segment bloklarıyla çizilir.)
- **Parolalar:** kaynak kodda düz metin değil, sabit tuzlu SHA-256 özeti olarak durur;
  karşılaştırma sabit zamanlıdır. Özetler arayüze hiç gönderilmez.
- **Depolama:** `allowFileAccess = false`, `allowContentAccess = false`. Katalog yazımı atomiktir
  (`.tmp` + rename), gelen JSON şemaya göre budanır ve sınırlanır.
- **Debug ayrımı:** WebView remote debugging yalnızca debug build'de açıktır.

## Lisans

Kod MIT. Press Start 2P yazı tipi SIL Open Font License 1.1 ile gelir
(`app/src/main/assets/www/fonts/OFL.txt`).
