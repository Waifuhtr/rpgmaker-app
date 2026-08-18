# PixelStore — Senaryo (v3)

Bu belge uygulamanın kurgusunu, ekran akışını, veri modelini ve rol ayrımını tarif eder.
Uygulanan hâli `app/src/main/` ve `wordpress-plugin/pixelstore-bridge/` altındadır.

> **v2 → v3 değişikliği.** v2'de iki veri kaynağı vardı (cihazdaki tohum katalog + isteğe bağlı
> WordPress) ve iki demo hesap gömülüydü. v3'te **tek kaynak** var: riaslink.fun. Demo bölümü
> tamamen kaldırıldı, kullanıcı veritabanı sitenin WordPress kullanıcı tablosu oldu, eklenti
> temanın kendi kayıt tipini okuduğu için sitede yayımlanmış tüm eski oyunlar kendiliğinden
> göründü. Uygulamaya istek listesi, puanlama, yorum, profil fotoğrafı ve canlı arama eklendi.

## 1. Konsept

PixelStore, riaslink.fun'da yayımlanan oyun kütüphanesini RPG Maker estetiğinde sunan bir Android
uygulamasıdır. Sitenin bir kopyası değil, **aynı veritabanının ikinci arayüzüdür**: uygulamada
yapılan her işlem sitede, sitede yapılan her işlem uygulamada görünür.

**Görsel dil**

- Press Start 2P piksel yazı tipi yalnızca kısa metinlerde; paragraflar sistem yazı tipinde
  (piksel yazı tipinde uzun metin okunmuyor — v1'in en büyük sorunuydu)
- Koyu lacivert üç yüzey kademesi + altın/nane/gök/gül/mor vurgular
- Yuvarlak köşe yok: çerçeveler `drawBehind` ile çizilen pahlı piksel kenarlar
- Düğmelerde kabartma (üst kenarda ışık, alt kenarda gölge); pasif düğme nötr griye düşer
- Sürekli dolgu yerine **segment çubuklar** (8-bit hissi + oran gözle sayılabiliyor)
- Kapak ve ekran görüntüleri **gerçek** — WordPress medya kütüphanesinden gelir. Görsel yoksa
  tohumdan üretilen piksel sahne yer tutucu olur (boş gri kutu görünmez)
- Sesler `AudioTrack` ile üretilen kare/üçgen dalgalar; ses dosyası yok

## 2. Veri kaynağı

Tek kaynak: `https://riaslink.fun/wp-json/pixelstore/v2`

Adres `app/build.gradle.kts` içinde `BuildConfig.API_BASE` olarak gömülüdür. Uygulamada sunucu
alanı **yoktur** — kullanıcı adres girmez, değiştiremez. Giriş ekranındaki "sunucu bağlantısını
sına" düğmesi yalnızca `/health` ucunu yoklar.

Cihazda tutulan tek veri: oturum jetonu, ses tercihi, titreşim tercihi. Parola saklanmaz.

## 3. Roller

| Rol | Karşılığı |
|---|---|
| Yönetici | `manage_options` **veya** `edit_others_posts` yeteneği olan WordPress kullanıcısı |
| Kullanıcı | sitedeki diğer tüm kullanıcılar |

Kural: **yönetim yalnızca yöneticide görünür, kullanıcıda hiç yoktur.** Üç katmanda uygulanır:

1. **Veri** — kullanıcı rolünde sorgu taslak (`draft`) kayıtları hiç döndürmez; `PSB_Query::games()`
   yayın durumunu `$is_admin` bayrağına göre kurar.
2. **Uç** — yönetim uçları `permission_admin` ile korunur: kullanıcı jetonuyla çağrılırsa 403
   döner. Puan, indirme ve görüntülenme sayaçları yazma ucundan hiç kabul edilmez (yalnızca
   kendi uçları artırır).
3. **Arayüz** — Yönetim sekmesi kullanıcı rolünde hiç oluşturulmaz; `openTab`/`push` `adminOnly`
   ekranları reddeder ve her yönetim ekranı kendi başına rolü bir kez daha doğrular.

Rol her istekte sunucudan okunur, cihazdaki tercihlerden değil. Tercihler kurcalansa bile yetki
yükseltilemez.

## 4. Oturum

```
Giriş  ─► POST /auth/login {username, password}
          └─ wp_authenticate() → jeton: "<kullanıcı_id>.<48 hex>"
             sunucuda yalnızca sha256(jeton) saklanır (_psb_tokens kullanıcı metası)
             30 gün ömür · kullanıcı başına en çok 5 cihaz (en eskisi düşer)

Açılış ─► GET /auth/me   (Authorization: Bearer <jeton>)
          ├─ geçerli  → doğrudan mağaza
          └─ geçersiz → jeton silinir, giriş ekranı

Çıkış  ─► POST /auth/logout → o jetonun özeti listeden silinir
```

## 5. Ekran akışı

```
Açılış perdesi
   │
   ├─ jeton yoksa/geçersizse ──► Giriş (site hesabı · demo yok)
   │
   └─ jeton geçerliyse ────────► Mağaza
                                  ├─ Canlı arama (yazarken sonuç açılır)
                                  ├─ Öne çıkan oyun afişi
                                  ├─ Katlanabilir filtreler + sıralama
                                  └─ Kart ──► Oyun sayfası
                                                ├─ İndirme (geri sayım + arşiv parolası)
                                                ├─ Puanlama 1–5
                                                ├─ Galeri ──► tam ekran görüntüleyici
                                                ├─ Yorumlar ──► yorum yaz / oyla / sil
                                                ├─ Hata bildir
                                                └─ [yalnız admin] "Bu oyunu düzenle"

Sekmeler: Mağaza · Türler · Listem · Profil · [yalnız admin] Yönetim
```

**Geri tuşu:** `BackHandler` → `StoreViewModel.back()`. Yığında bir adım geri gider; kök ekranda
değilse mağazaya döner; mağazadayken `false` döner ve Activity kapanır.

## 6. Ekranlar

### Giriş
Arma (başlatıcı ikonuyla aynı çizim), kullanıcı adı/e-posta + parola, hata paneli, sunucu sınama
düğmesi, gömülü site adresi. Demo hesap kartı yok.

### Mağaza
- **Canlı arama:** 320 ms gecikmeli, iptal edilebilir; sunucudan hafif yük (`/search`, en az 2
  karakter) çekip listenin üstünde açılır. Klavyedeki arama tuşu tam listeye geçer.
- **Öne çıkan:** sitede öne çıkan/editörün seçimi işaretli kayıt, 16:9 kapakla
- **Filtreler:** varsayılan kapalı tek satır; açılınca tür · platform · dil · durum çip satırları.
  Kapalıyken seçili filtreler rozet olarak görünür.
- **Sıralama:** En yeni · İndirme · Puan · A-Z
- **Kart:** 74×104 gerçek kapak + dört bilgi kademesi (başlık, geliştirici, tanıtım, ölçüler) +
  rozetler + istek listesi kalbi
- **Sonsuz kaydırma:** son üç karta yaklaşınca sonraki sayfa istenir

### Oyun sayfası
Kahraman görseli (üstten şeffaf, alta doğru koyulaşan perde) + 86×120 kapak, ölçü şeridi
(puan/oy · indirme · boyut), indirme düğmesi + istek listesi, alternatif link, puanlama paneli
(5 dokunulabilir kutu, site ortalaması altta), ekran görüntüsü şeridi (dokununca tam ekran),
açıklama · değişiklik günlüğü · kurulum rehberi, sistem gereksinimleri (minimum + önerilen),
bilgi tablosu, etiketler, yorumlar düğmesi, fragman, hata bildirimi. Yöneticiye ek olarak
"bu oyunu düzenle".

**İndirme akışı:** onay → 3 saniye geri sayım → arşiv parolası varsa gösterilir → "hemen aç".
Sayaç sunucuda artar (`game_download_count`), yani sitedeki sayı da artar.

### Yorumlar
Tavsiye ediyor/etmiyor seçimi + metin (en az 3 karakter). Bir kullanıcı bir oyuna bir kez yorum
yazabilir (temanın kuralı). Yorum kartında yazarın rozetleri, tavsiye durumu, faydalı/faydasız
oyları; kendi yorumunu silebilir, kendi yorumuna oy veremez.

### Türler
Dört taksonominin karoları: tür · platform · dil · durum. Karoda sitede terime atanmış emoji
varsa o, yoksa terimin baş harfi çizilir (5×5 nesne glifleri bu boyutta birbirinden ayırt
edilemiyordu). Kayıt sayısı ve dağılım çubuğu her karoda. Dokununca mağaza o filtreyle açılır.

### Listem
İstek listesi — sitedeki `sl_favorites` ile aynı liste. Sekmeye her girişte tazelenir, çünkü
site tarafından da değişebilir.

### Profil
Profil fotoğrafı (dokununca sistem görsel seçici; depolama izni istemez), ad, rol rozeti,
rozetler/başarımlar, istek listesi ve yorum sayacı, ses ve titreşim anahtarları, sunucu/katılım/
sürüm bilgileri, onaylı çıkış. Kendi yüklediği fotoğraf varsa "fotoğrafı kaldır" da görünür.

### Yönetim — yalnızca admin
- **Özet karoları:** toplam oyun, yayında, taslak, indirme, görüntülenme, ortalama puan, yorum,
  kullanıcı, açık hata raporu
- **Tür dağılımı:** segment çubuklar
- **Katalog yönetimi:** her satırda düzenle / yayın durumu / sil (çöp kutusuna, kalıcı değil)
- **Kullanıcılar:** site kullanıcıları, rolleri, katılım tarihi, istek sayısı. Parola bilgisi
  arayüze hiç gönderilmez; kullanıcı ekleme/silme wp-admin'in işi.
- **Oyun düzenleyici:** kapak yükleme, ekran görüntüsü ekleme/kaldırma, künye alanları,
  taksonomi seçimi (olmayan terim yazılırsa oluşturulur), metinler, indirme linkleri, sistem
  gereksinimleri. Kaydedilen her alan doğrudan siteye yazılır.

## 7. Veri modeli

Uygulama kendi şemasını dayatmaz; temanın alanlarını okur. `PSB_Mapper` tek sözleşmedir.

```jsonc
{
  "id": "golge-vadisi",            // post_name (slug)
  "title": "Gölge Vadisi",
  "subtitle": "Tam Türkçe",
  "developer": "Kuzey Fener Stüdyo",
  "publisher": "…",
  "version": "v1.3",
  "sizeLabel": "2.4 GB",           // game_size — serbest metin, sayı değil
  "releaseDate": "2026-05-01",
  "ageRating": "+16",
  "genre": "RPG",                  // game_genre terimi
  "platform": "Android",           // game_platform terimi
  "language": "Türkçe",            // game_language terimi
  "status": "Tamamlandı",          // game_status terimi
  "rating": 4.6,                   // sl_user_rating_avg
  "ratingCount": 184,
  "downloadCount": 41200,          // game_download_count
  "viewCount": 96240,
  "published": true,               // post_status; taslak yalnızca yöneticide
  "featured": false,
  "editorsChoice": true,
  "favorited": false,              // isteği yapan kullanıcıya göre
  "userRating": 4,                 // isteği yapan kullanıcının oyu
  "coverUrl": "https://…",         // öne çıkan görsel (steamlike-cover)
  "heroUrl": "https://…",
  "screenshots": [ { "id": 812, "url": "…", "fullUrl": "…" } ],
  "excerpt": "…",
  "description": "…",             // düz metne çevrilir, madde imleri korunur
  "changelog": "…",
  "installGuide": "…",
  "tags": ["Sıra tabanlı", "Hikâye"],
  "downloadUrl": "…",
  "mirrorUrl": "…",
  "archivePassword": "…",          // yalnızca indirme ucundan döner
  "trailerUrl": "…",
  "requirements": {
    "hasMinimum": true, "hasRecommended": true,
    "minimum":     { "os": "…", "cpu": "…", "ram": "…", "gpu": "…", "storage": "…" },
    "recommended": { "os": "…", "cpu": "…", "ram": "…", "gpu": "…", "storage": "…" }
  }
}
```

Açıklama alanları HTML'den düz metne çevrilir: paragraf araları `\n\n`, liste maddeleri `• `
olur. `the_content` filtresi bilinçli olarak **uygulanmaz** — tema kısa kodları uygulamada
işlenemez, çıktıya ham kod düşmesin.

## 8. WordPress arka ucu

`wordpress-plugin/pixelstore-bridge` eklentisi:

| Dosya | Görevi |
|---|---|
| `pixelstore-bridge.php` | önyükleme, sabitler, tema var mı denetimi |
| `class-psb-auth.php` | jeton üret/doğrula/sil, oturum yükü, rozetler, avatar adresi |
| `class-psb-mapper.php` | app ↔ site alan eşlemesi (tek sözleşme) |
| `class-psb-query.php` | listeleme, sayfalama, arama, filtre, sıralama, taksonomi, istatistik |
| `class-psb-social.php` | istek listesi, puanlama, yorum, yorum oyu, indirme/görüntülenme, rapor |
| `class-psb-write.php` | yönetici oluşturma/güncelleme/silme, terim atama |
| `class-psb-media.php` | görsel yükleme (avatar, kapak, ekran görüntüsü) |
| `class-psb-rest.php` | uçlar ve izin geri çağrıları |
| `class-psb-admin.php` | wp-admin durum ekranı (Ayarlar → PixelStore) |

Kendi kayıt tipi tanımlamaz. `uninstall.php` yalnızca eklentinin kendi ürettiği verileri
(`_psb_tokens`, `psb_avatar_attachment`, `psb_rating_voters`, `psb_review_voters`) siler —
temanın verisine dokunmaz.

### Testler

`wordpress-plugin/tests/` altında WordPress kurulumu gerektirmeyen bir test takımı vardır:
`wp-stubs.php` küçük bir sahte WordPress (post/kullanıcı/yorum/terim tabloları, `WP_Query`,
`WP_REST_Request`) kurar, `bridge-test.php` 130 doğrulama koşar — oturum ve yetki, alan eşleme,
taslak görünürlüğü, filtre/sıralama/arama, istek listesi, puanlama, yorumlar ve oylar, sayaçlar,
hata raporu, yönetim yazma, istatistik, profil fotoğrafı.

```bash
php wordpress-plugin/tests/bridge-test.php   # 130 geçti, 0 başarısız
```

## 9. Tema

`wordpress-theme/steamlike/` temanın **değiştirilmemiş** kopyasıdır; ileride karşılaştırma
gerekirse diye depoda tutulur. Eklenti temanın alanlarını olduğu gibi kullandığı için tema
üzerinde değişiklik gerekmedi — güncellemeniz gereken bir tema zip'i yok.

## 10. Derleme ve dağıtım

- **Yerel:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- **Depoda hazır APK:** `release/PixelStore-debug.apk` her sürümde güncellenir
- **Tasarımı görmek:** `./gradlew :app:testDebugUnitTest --tests '*DesignScreenshotTest'` →
  `app/build/screenshots/*.png` (emülatör gerekmez; Robolectric yerel grafik kipi)
- **Hugging Face Space:** `scripts/make-space-zip.sh` → Docker SDK'lı Space APK derler, eklenti
  testini koşar, açılış sayfasında APK + eklenti zip + tema zip + kaynak zip + günlükler sunar
- **İmzalama:** `keystore.properties` varsa release imzalanır; yoksa imzasız çıkar. İmza dosyaları
  depoya hiç girmez.

## 11. Bilinçli sınırlar

- Uygulamadan kullanıcı eklenip silinemez; yönetici listeyi görür. Kullanıcı yönetimi wp-admin'in
  işi.
- Yorum silmeyi kullanıcı yalnızca kendi yorumu için yapabilir; moderasyon kuyruğu wp-admin'de.
- Hata raporları uygulamada listelenmez; yönetim panelinde sayısı görünür, içerik wp-admin'de
  okunur (`sl_report` kayıtları).
- Çevrimdışı katalog yok: veri her açılışta siteden gelir. Görseller Coil ile disk önbelleğinde
  tutulur, ağ yokken en son görülen kapaklar görünür ama liste boş gelir.

## 12. Sonraki adımlar (isteğe bağlı)

- Yeni oyun yayımlandığında bildirim (FCM veya periyodik yoklama)
- Çevrimdışı önbellek: son listeyi cihazda tutup ağ yokken göstermek
- Release imzası + Play Store paketleme (AAB)
- Uygulama içinden hata raporu yanıtlama (yönetici)
