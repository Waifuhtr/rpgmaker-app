# PixelStore — Senaryo (v2)

Bu belge uygulamanın kurgusunu, ekran akışını, veri modelini ve rol ayrımını tarif eder.
Uygulanan hâli `app/src/main/` ve `wordpress-plugin/` altındadır.

> **v1 → v2 değişikliği.** v1'de arayüz WebView içinde HTML/CSS/JS idi ve veri cihazdaydı.
> v2'de WebView tamamen kaldırıldı: her ekran Jetpack Compose ile yerel olarak çiziliyor ve
> veritabanı olarak WordPress kullanılabiliyor. Arayüz okunabilirlik için baştan tasarlandı.

## 1. Konsept

PixelStore, bir uygulama mağazasının işlevlerini RPG Maker estetiğinde sunan bir vitrindir.
Gerçek bir dağıtım altyapısı değildir. Amaç, "mağaza" fikrini 8-bit bir arayüzle tam olarak kurmak
ve üzerine gerçek yetki ayrımı olan bir yönetim paneli eklemektir.

**Görsel dil**

- Press Start 2P piksel yazı tipi yalnızca kısa metinlerde; paragraflar sistem yazı tipinde
  (piksel yazı tipinde uzun metin okunmuyor — v1'in en büyük sorunuydu)
- Koyu lacivert üç yüzey kademesi + altın/nane/gök/gül/mor vurgular
- Yuvarlak köşe yok: çerçeveler `drawBehind` ile çizilen pahlı piksel kenarlar
- Düğmelerde kabartma (üst kenarda ışık, alt kenarda gölge)
- Sürekli dolgu yerine **segment çubuklar** (8-bit hissi + oran gözle sayılabiliyor)
- Tüm ikon ve ekran görüntüleri tohumdan üretilen piksel çizimleri
- Sesler `AudioTrack` ile üretilen kare/üçgen dalgalar; ses dosyası yok

## 2. Veri kaynağı seçimi

Uygulama iki kipte çalışır ve kip üst çubukta rozet olarak görünür.

**Yerel kip (varsayılan).** Katalog `assets/catalog_seed.json` içinden tohumlanıp cihazın özel
dizinine kopyalanır; yazma atomiktir. Hesaplar gömülüdür (`admin`/`user`). İnternet gerekmez —
APK kurulduğu anda çalışır.

**WordPress kipi.** Profil → Bağlantı ayarları'na site adresi girilir. Kayıtlar WordPress'te
`pixelstore_app` özel yazı tipinde, türler `pixelstore_category` taksonomisinde tutulur. Hesaplar
sitenin WordPress kullanıcılarıdır.

Kaynak değiştiğinde oturum sıfırlanır: iki kaynağın hesapları farklıdır.

## 3. Roller

| Rol | Yerel kip | WordPress kipi |
|---|---|---|
| Yönetici | `admin` / `admin123` | `manage_options` veya `edit_others_posts` yeteneği olan kullanıcı |
| Kullanıcı | `user` / `user123` | diğer tüm kullanıcılar |

Kural: **yönetim yalnızca yöneticide görünür, kullanıcıda hiç yoktur.** Üç katmanda uygulanır:

1. **Veri** — kullanıcı rolünde katalog yükü taslak kayıtları ve `published`/`createdAt`
   alanlarını hiç içermez.
2. **Kaynak** — `saveApp`, `deleteApp`, `setPublished`, `users`, `stats`, `resetCatalog` yönetici
   değilse veri değil hata döner. Yerel kipte `LocalCatalogSource`, WordPress kipinde sunucudaki
   yetenek denetimi karar verir.
3. **Arayüz** — Yönetim sekmesi kullanıcı rolünde hiç oluşturulmaz; `openTab`/`push` da `adminOnly`
   ekranları reddeder ve her yönetim ekranı kendi başına rolü bir kez daha doğrular.

Oturum sürdürülürken rol, kalıcı tercihlerden değil hesap tablosundan (veya sunucudan) okunur;
tercihler kurcalansa bile yetki yükseltilemez.

## 4. Ekran akışı

```
Açılış perdesi
   │
   ├─ oturum yoksa ──► Giriş
   │                     ├─ demo hesap kartları (yerel kipte)
   │                     ├─ manuel form
   │                     └─ sunucu / bağlantı paneli (açılır-kapanır)
   │
   └─ oturum varsa ───► Mağaza
                          ├─ Haftanın oyunu afişi
                          ├─ Arama · tür çipleri · sıralama
                          └─ Kart ──► Kayıt detayı
                                        ├─ İndirme akışı (segment çubuk)
                                        ├─ Galeri ──► tam ekran görüntüleyici
                                        └─ [yalnız admin] "Bu kaydı düzenle"

Sekmeler: Mağaza · Türler · Profil · [yalnız admin] Yönetim
Profil → Bağlantı ayarları → WordPress adresi / sınama / kurulum adımları
```

**Geri tuşu:** `BackHandler` → `StoreViewModel.back()`. Yığında bir adım geri gider; kök ekranda
değilse mağazaya döner; mağazadayken `false` döner ve Activity kapanır.

## 5. Ekranlar

### Giriş
Arma (başlatıcı ikonuyla aynı çizim), aktif veri kaynağı rozeti, form, hata paneli. Yerel kipte iki
demo hesap kartı (tohumdan üretilen avatarlarla); WordPress kipinde bunun yerine açıklama paneli.
Altta açılır bağlantı paneli — kullanıcı oturum açmadan da sunucu adresini girebilmeli.

### Mağaza
- **Haftanın oyunu:** en yüksek `puan × log(indirme)` skorlu yayındaki kayıt, geniş afiş oranında
- **Arama:** başlık, geliştirici, tür ve etiketlerde eşleşme
- **Tür çipleri:** yatay kaydırmalı, piksel simgeli, ikinci dokunuşta temizlenir
- **Sıralama:** Öne çıkan · İndirme · Puan · Yeni
- **Kart:** 64dp ikon + dört bilgi kademesi (başlık, geliştirici, açıklama, ölçüler) + rozetler

### Kayıt detayı
88dp ikon, tür ve yaş rozetleri, üçlü ölçü şeridi (puan/indirme/boyut, dikey çizgilerle ayrılmış),
indirme düğmesi + segment ilerleme, ekran görüntüsü şeridi (dokununca tam ekran görüntüleyici,
ileri/geri), paragraflara ayrılmış açıklama, etiketler, bilgi tablosu. Yöneticiye ek olarak
"bu kaydı düzenle" düğmesi.

### Türler
Tür kutuları: glif, ad, kayıt sayısı ve dağılım çubuğu. Seçim mağazayı o türe filtreler.

### Profil
Avatar, ad, rol rozeti; kütüphane; ses ve titreşim anahtarları; bağlantı ayarlarına geçiş;
paket/sürüm/kaynak bilgileri; onaylı çıkış.

### Yönetim — yalnızca admin
- **Özet kartları:** toplam kayıt, yayında, taslak, toplam indirme, ortalama puan
- **Tür dağılımı:** segment çubuklar
- **Hızlı işlemler:** yeni kayıt · kullanıcılar · katalogu sıfırla (onaylı)
- **Katalog yönetimi:** her satırda düzenle / yayın durumu / sil (silme onaylı)
- **Kayıt düzenleyici:** ikon tohumu ve palet **canlı önizlemeli**; künye alanları; yayın anahtarı;
  metinler; ekran görüntüsü editörü (sahne, cihaz tipi, başlık, varyasyon üretme, kaldırma —
  en fazla 8)
- **Kullanıcılar:** hesap listesi ve roller. Parola bilgisi arayüze hiç gönderilmez.

## 6. Veri modeli

```jsonc
{
  "id": "ay-kalesi-gunlukleri",   // WordPress'te post_name (slug)
  "title": "Ay Kalesi Günlükleri",
  "developer": "Kuzey Fener Stüdyo",
  "category": "RPG",              // WordPress'te pixelstore_category terimi
  "version": "2.4.1",
  "sizeMb": 148.0,
  "rating": 4.7,
  "ratingCount": 18240,
  "installs": 412300,             // türetilmiş: forma yazılamaz, /install artırır
  "contentRating": "12+",
  "published": true,              // admin-only alan; WP'de post_status
  "updatedAt": "2026-07-02",
  "iconSeed": "ay-kalesi-01",     // ikon bu tohumdan çizilir
  "palette": "azure",             // emerald | amber | crimson | azure | violet | slate | mint
  "tags": ["Sıra tabanlı", "Hikâye"],
  "shortDescription": "…",
  "longDescription": "…",         // WordPress'te post_content
  "downloadUrl": "",
  "screenshots": [
    { "seed": "ay-1", "kind": "phone", "scene": "title", "caption": "Açılış ekranı" }
  ]
}
```

`scene`: `title` · `field` · `battle` · `town` · `cave` · `menu`
`kind`: `phone` (108×192) · `tablet` (176×132) — ayrıca yalnızca afiş için `banner` (208×94)

Aynı JSON şeması iki kaynakta da kullanılır, bu yüzden `Json` nesnesindeki tek ayrıştırıcı ikisine
de hizmet eder. Tohum katalogda 8 kayıt vardır; biri (`Gölge Loncası`) bilinçli olarak
**yayınlanmamıştır** — rol ayrımını canlı göstermek için: kullanıcı 7 kayıt görür, yönetici 8.

## 7. WordPress arka ucu

`wordpress-plugin/pixelstore-api` eklentisi:

- `pixelstore_app` özel yazı tipi (public=false; kayıtlar ön yüzde sayfa açmaz) + `pixelstore_category`
- Uygulama alanları meta olarak (`_ps_*`), ekran görüntüleri ve etiketler JSON meta olarak
- Jeton tabanlı oturum: `wp_authenticate()` ile doğrulama, jetonun yalnızca SHA-256 özeti kullanıcı
  metasında, 30 gün ömür, kullanıcı başına 5 cihaz
- `/wp-json/pixelstore/v1/`: `health`, `auth/login`, `auth/me`, `auth/logout`, `catalog`,
  `apps`, `apps/{id}`, `apps/{id}/install`, `apps/{id}/published`, `users`, `stats`, `seed`
- wp-admin → PixelStore ekranı: bağlanacak adres, durum tablosu, demo katalog kurulumu, uç listesi,
  sorun giderme (Authorization başlığı düşen sunucular için `CGIPassAuth On` notu)

## 8. Derleme ve dağıtım

- **Yerel:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- **Depoda hazır APK:** `release/PixelStore-debug.apk` her sürümde güncellenir
- **Tasarımı görmek:** `./gradlew :app:testDebugUnitTest --tests '*DesignScreenshotTest'` →
  `app/build/screenshots/*.png` (emülatör gerekmez; Robolectric yerel grafik kipi)
- **Hugging Face Space:** `scripts/make-space-zip.sh` → Docker SDK'lı Space APK derler, açılış
  sayfasında APK + kaynak zip + WordPress eklentisi zip + build günlüğü sunar
- **İmzalama:** `keystore.properties` varsa release imzalanır; yoksa imzasız çıkar

## 9. Bilinçli sınırlar

- "İndir" gerçek bir APK indirmez; sayacı artırıp kaydı kütüphaneye ekler. Kayda `downloadUrl`
  yazılırsa detayda ayrı bir düğme çıkar.
- Uygulamadan kullanıcı eklenip silinemez; yönetici listeyi görür. WordPress kipinde kullanıcı
  yönetimi wp-admin'in işi.
- Yorum/puan yazma akışı yok; puanlar katalog verisinden gelir.
- Uzak görsel desteği yok: tüm görseller cihazda üretilir.

## 10. Sonraki adımlar (isteğe bağlı)

- Gerçek APK yükleme: yöneticinin dosya seçip kayda iliştirmesi (SAF + WordPress medya kütüphanesi)
- Yorum/puan akışı ve moderasyon kuyruğu
- Çevrimdışı önbellek: WordPress kipinde son katalogu cihazda tutup ağ yokken göstermek
- Release imzası + Play Store paketleme (AAB)
