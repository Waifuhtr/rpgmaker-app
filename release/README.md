# Hazır dosyalar

Depodan doğrudan indirilebilir. Her sürümde yenilenir.

| Dosya | Boyut | Ne işe yarar |
|---|---|---|
| `PixelStore-debug.apk` | 11M | Telefona kurulacak uygulama |
| `pixelstore-wordpress-plugin.zip` | 28K | riaslink.fun'a kurulacak eklenti |
| `steamlike-theme.zip` | 212K | Temanın değiştirilmemiş kopyası (yeniden kurmak gerekirse) |
| `pixelstore-hf-space.zip` | 464K | Hugging Face Space paketi (kendin derlemek istersen) |

## Kurulum sırası

**1. Eklenti**

```
riaslink.fun/wp-admin → Eklentiler → Yeni ekle → Eklenti yükle
→ pixelstore-wordpress-plugin.zip → Şimdi kur → Etkinleştir
```

Etkinleştirdikten sonra `Ayarlar → PixelStore` sayfası temanın bulunduğunu, kaç oyun ve kaç
kullanıcı göründüğünü yazar. Burada oyun sayısı doğru görünüyorsa uygulama da aynı listeyi
görecek demektir.

**2. Uygulama**

`PixelStore-debug.apk` dosyasını telefona indir ve kur. Android "bilinmeyen kaynaklardan
kuruluma izin ver" onayını isteyecek (dosya debug imzalı olduğu için).

**3. Giriş**

Sitedeki kendi kullanıcı adın (veya e-postan) ve parolanla giriş yap. **Demo hesap yoktur** —
kullanıcı veritabanı sitenin WordPress kullanıcı tablosudur.

Yönetici sekmesi yalnızca WordPress rolü yönetici olan hesapta oluşur. Kullanıcı rolünde
arayüzde hiç yer almaz ve sunucudaki yönetim uçları da veri döndürmez.

## Sitede yayımlanmış eski oyunlar

Hepsi kendiliğinden görünür. Eklenti kendi kayıt tipini tanımlamaz; temanın var olan `game`
kayıt tipini ve `game_*` taksonomilerini okur. Tek tek yeniden yükleme, içe aktarma veya
eşitleme adımı yoktur.

## Notlar

- Sunucu adresi APK içinde gömülüdür (`https://riaslink.fun/wp-json/pixelstore/v2`); uygulamada
  sunucu ayarı yoktur.
- Parola cihazda saklanmaz. Girişte alınan jeton 30 gün geçerlidir, sunucuda yalnızca SHA-256
  özeti tutulur, kullanıcı başına en çok 5 cihaz kaydedilir.
- APK debug imzalıdır: kurulabilir ama Play Store'a yüklenemez. Play Store dağıtımı için
  `keystore.properties` ile release imzası gerekir (imza dosyaları depoya girmez).
- Tema üzerinde **değişiklik yapılmadı**; `steamlike-theme.zip` yalnızca yedek kopyadır,
  kurman gerekmiyor.
