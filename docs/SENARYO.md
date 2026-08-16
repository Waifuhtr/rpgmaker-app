# PixelStore — Senaryo

Bu belge uygulamanın kurgusunu, ekran akışını, veri modelini ve rol ayrımını tarif eder.
Uygulanan hâli `app/src/main/` altındadır.

## 1. Konsept

PixelStore, bir uygulama mağazasının işlevlerini RPG Maker estetiğinde sunan bir vitrindir.
Gerçek bir dağıtım altyapısı değildir: katalog cihazda saklanır, dış sunucuya veri gönderilmez.
Amaç, "mağaza" fikrini 8-bit bir arayüzle tam olarak kurmak ve üzerine gerçek yetki ayrımı olan
bir yönetim paneli eklemektir.

**Görsel dil**

- Press Start 2P piksel yazı tipi (OFL-1.1, pakete gömülü — internet gerekmez)
- NES/SNES'e yakın koyu lacivert zemin + altın/nane/gül vurgular
- Yuvarlak köşe yok; çerçeveler `box-shadow` ile çizilen pahlı piksel kenarlar
- CRT tarama çizgileri (profil ekranından kapatılabilir)
- WebAudio ile üretilen kare dalga "tık" sesleri (ses dosyası taşınmaz, kapatılabilir)
- Tüm ikon ve ekran görüntüleri tohumdan üretilen canvas çizimleri

## 2. Roller

| Rol | Kullanıcı | Parola |
|---|---|---|
| Yönetici | `admin` | `admin123` |
| Kullanıcı | `user` | `user123` |

Kural: **yönetim yalnızca yöneticide görünür, kullanıcıda hiç yoktur.**

Bu üç katmanda birden uygulanır:

1. **Veri katmanı** — kullanıcı rolünde katalog yükü `publicApps()` ile üretilir. Yayınlanmamış
   kayıtlar ve `published`, `createdAt`, `internalNote` alanları yüke hiç konmaz.
2. **Köprü katmanı** — `StoreBridge`'deki `adminSaveApp`, `adminDeleteApp`, `adminSetPublished`,
   `adminListUsers`, `adminGetStats`, `adminResetCatalog` uçları `requireAdmin()` ile korunur.
   Kullanıcı rolünde bunlar veri değil hata döner.
3. **Arayüz katmanı** — Yönetim sekmesi kullanıcı rolünde DOM'a eklenmez; `navigate()` de
   `adminOnly` rotaları reddeder.

Rolü her zaman native taraf belirler. Oturum yeniden açıldığında rol, kalıcı tercihlerden değil
hesap tablosundan okunur; böylece tercihler kurcalansa bile yetki yükseltilemez.

## 3. Ekran akışı

```
Açılış perdesi
   │
   ├─ oturum yoksa ──► Giriş
   │                     ├─ "Yönetici olarak gir"  (admin/admin123)
   │                     ├─ "Kullanıcı olarak gir" (user/user123)
   │                     └─ manuel form
   │
   └─ oturum varsa ───► Mağaza
                          ├─ Haftanın oyunu (büyük banner)
                          ├─ Arama · tür çipleri · sıralama
                          └─ Kart ──► Kayıt detayı
                                        ├─ İndirme akışı (segment çubuk)
                                        ├─ Ekran görüntüsü galerisi ──► tam ekran görüntüleyici
                                        └─ [yalnız admin] "Bu kaydı düzenle"

Sekmeler: Mağaza · Türler · Profil · [yalnız admin] Yönetim
```

**Geri tuşu:** açık modal varsa kapanır; değilse yığında bir adım geri gidilir; ana sekmede
değilsek mağazaya dönülür; oradaki geri uygulamadan çıkar. Karar JS'te verilir, native yalnızca
`PixelStore.handleBack()` sonucunu okur.

## 4. Ekranlar

### Giriş
Piksel arma, iki demo hesap kartı (avatarları da tohumdan üretilir) ve manuel form. Hatalı giriş
kırmızı uyarı + hata sesi.

### Mağaza
- **Haftanın oyunu:** en yüksek `puan × log(indirme)` skoruna sahip yayındaki kayıt, tablet
  oranında bir "açılış ekranı" görseliyle.
- **Arama:** başlık, geliştirici, tür ve etiketlerde Türkçe duyarlı eşleşme.
- **Tür çipleri:** yatay kaydırmalı, piksel simgeli, çift dokunuşla temizlenir.
- **Sıralama:** Öne çıkanlar · En çok indirilen · En yüksek puan · En yeni.
- **Kart:** ikon, başlık, geliştirici, tek satır tanıtım, yıldızlar, boyut, tür rozeti,
  "YÜKLÜ" / "TASLAK" rozetleri.

### Kayıt detayı
Büyük ikon, tür ve yaş rozetleri, üçlü istatistik şeridi (puan/indirme/boyut), indirme düğmesi,
ekran görüntüsü şeridi (telefon ve tablet oranları bir arada; dokununca tam ekran görüntüleyici,
ileri/geri gezinme), paragraflara ayrılmış uzun açıklama, etiketler, bilgi tablosu.
İndirme sahte bir ilerleme çubuğuyla tamamlanır, indirme sayacını artırır ve kayıt "kütüphanem"e
girer.

### Türler
Tür kutuları ve her türdeki kayıt sayısı. Seçim mağazayı o türe filtreler.

### Profil
Avatar, ad, rol rozeti; kütüphane listesi; ses / titreşim / tarama çizgisi anahtarları
(LocalStorage'da kalıcı); paket adı ve sürüm satırı; çıkış (onay ister).

### Yönetim — yalnızca admin
- **Özet kartları:** toplam kayıt, yayında, taslak, toplam indirme, ortalama puan.
- **Tür dağılımı:** segment çubuklarla.
- **Hızlı işlemler:** yeni kayıt · kullanıcılar · katalogu sıfırla (onaylı).
- **Katalog yönetimi:** her satırda düzenle / yayın durumu / sil.
- **Kayıt düzenleyici:** başlık, geliştirici, tür, sürüm, boyut, puan, oy sayısı, yaş sınırı,
  yayın anahtarı, kısa/uzun açıklama, etiketler, geliştirici bağlantısı; ikon tohumu ve palet
  seçimi **canlı önizlemeli**; ekran görüntüsü düzenleyici (sahne, cihaz tipi, başlık, varyasyon
  üretme, silme — en fazla 8 adet).
- **Kullanıcılar:** hesap tablosu, roller ve katılım tarihleri. Parola özetleri arayüze gönderilmez.

## 5. Veri modeli

```jsonc
{
  "id": "ay-kalesi-gunlukleri",
  "title": "Ay Kalesi Günlükleri",
  "developer": "Kuzey Fener Stüdyo",
  "category": "RPG",
  "version": "2.4.1",
  "sizeMb": 148.0,
  "rating": 4.7,
  "ratingCount": 18240,
  "installs": 412300,          // türetilmiş: forma yazılamaz, indirmelerle artar
  "contentRating": "12+",
  "published": true,           // admin-only alan
  "updatedAt": "2026-07-02",
  "createdAt": "2024-01-15",   // admin-only alan
  "iconSeed": "ay-kalesi-01",  // ikon bu tohumdan çizilir
  "palette": "azure",          // emerald | amber | crimson | azure | violet | slate | mint
  "tags": ["Sıra tabanlı", "Hikâye"],
  "shortDescription": "…",
  "longDescription": "…",
  "downloadUrl": "",
  "screenshots": [
    { "seed": "ay-1", "kind": "phone", "scene": "title", "caption": "Açılış ekranı" }
  ]
}
```

`scene` değerleri: `title` · `field` · `battle` · `town` · `cave` · `menu`.
`kind` değerleri: `phone` (108×192) · `tablet` (176×132).

Katalog `assets/www/data/catalog.json` içinde tohumlanır, ilk açılışta uygulamanın özel dizinine
kopyalanır. Sonraki tüm yazma işlemleri bu kopyada olur; yazma atomiktir ve gelen JSON şemaya göre
budanır (metin uzunlukları kırpılır, sayılar aralığa çekilir, yalnızca `https` ekran görüntüsü
adresleri kabul edilir).

Tohum katalogda 8 kayıt vardır; biri (`Gölge Loncası`) bilinçli olarak **yayınlanmamıştır** —
rol ayrımını canlı göstermek için: kullanıcı 7 kayıt görür, yönetici 8.

## 6. Derleme ve dağıtım

- **Yerel:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- **Hugging Face Space:** `scripts/make-space-zip.sh` ile üretilen zip, Docker SDK'lı bir Space'e
  yüklenir. İmaj derlenirken Android SDK iner ve APK üretilir; açılış sayfası APK'yı, kaynak
  zip'ini ve build günlüğünü sunar, `/demo/` altında arayüzün tarayıcı sürümü çalışır.
- **İmzalama:** `keystore.properties` varsa release imzalanır; yoksa imzasız çıkar. Bu dosya
  depoya girmez.

## 7. Bilinçli sınırlar

- Backend yok. Katalog cihazda, tercihler LocalStorage'da.
- "İndir" gerçek bir APK indirmez; sayaç artırıp kaydı kütüphaneye ekler. Kayda `downloadUrl`
  yazılırsa detay ekranında sistem tarayıcısını açan ayrı bir düğme çıkar.
- Hesap tablosu sabittir; kayıt olma akışı yoktur. Yönetici kullanıcı listesini görür ama
  kullanıcı ekleyip silemez.
- Uzak görsel desteği vardır (`screenshots[].url`, yalnız `https`) ama tohum katalog tamamen
  çevrimdışıdır.

## 8. Sonraki adımlar (isteğe bağlı)

- Gerçek APK yükleme: yöneticinin cihazdan dosya seçip kayda iliştirmesi (SAF + FileProvider).
- Gerçek backend: katalog senkronizasyonu ve çok cihazlı yönetim.
- Yorum/puan akışı ve moderasyon kuyruğu.
- Release imzası + Play Store paketleme (AAB).
