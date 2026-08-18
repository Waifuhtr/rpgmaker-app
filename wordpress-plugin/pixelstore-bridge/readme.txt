=== PixelStore Bridge ===
Contributors: pixelstore
Tags: rest-api, android, steamlike, games
Requires at least: 6.0
Tested up to: 6.7
Requires PHP: 7.4
Stable tag: 1.0.0
License: MIT

SteamLike temasındaki oyunları PixelStore Android uygulamasına açar.

== Description ==

Bu eklenti **kendi veri şemasını oluşturmaz**. SteamLike temasının zaten kullandığı `game` kayıt
tipini, taksonomilerini ve meta alanlarını okur ve yazar. Sonuç: sitede daha önce yayınlanmış tüm
oyunlar hiçbir taşıma yapılmadan uygulamada görünür; uygulamadan eklenen kayıt da sitede normal bir
oyun olur.

Uygulama ile site şu verileri paylaşır:

* Oyunlar — `game` kayıt tipi, öne çıkan görsel, `game_screenshots` galerisi
* Tür / platform / dil / durum / geliştirici / yayıncı / özellik — `game_*` taksonomileri
* Sistem gereksinimleri — `minimum_*` ve `recommended_*` meta alanları
* İstek listesi — `sl_favorites` (kullanıcı meta)
* Puanlama — `sl_user_rating_sum` / `_count` / `_avg`
* İnceleme ve yorumlar — `wp_comments`, `comment_type = review`
* İndirme ve görüntülenme sayaçları — `game_download_count`, `game_view_count`
* Profil fotoğrafı — `sl_custom_avatar`
* Hata raporları — `sl_report` kayıt tipi
* Rozetler — temanın `sl_get_user_badges()` fonksiyonu

== Installation ==

1. Eklentiyi zip olarak yükleyip etkinleştir.
2. Ayarlar → PixelStore Bridge ekranından durumu doğrula (tema algılandı mı, kaç oyun var).
3. Uygulamayı kur ve WordPress kullanıcı adın ile parolanla giriş yap.

Daha önce `pixelstore-api` eklentisini kurduysan **sil**: artık gerekmiyor ve kendi
`pixelstore_app` kayıtlarını taşıyor.

== Frequently Asked Questions ==

= Uygulama sunucu adresini nereden biliyor? =

Adres uygulamanın içine gömülüdür; kullanıcı değiştiremez. Bu eklenti tarafında ayar gerekmez.

= Parolalar nasıl saklanıyor? =

Eklenti parola saklamaz. Giriş `wp_authenticate()` ile sitenin kendi kullanıcı tablosuna karşı
doğrulanır. Başarılı girişte rastgele bir jeton üretilir; sunucuda yalnızca jetonun SHA-256 özeti
kullanıcı metasında durur, 30 gün sonra düşer, kullanıcı başına en fazla 5 cihaz tutulur.

= Kullanıcı rolündeki biri yönetim verisine erişebilir mi? =

Hayır. Yönetim uçları yetenek kontrolünden geçer ve 403 döner. Taslak kayıtlar kullanıcı rolünün
liste yükünde hiç yer almaz.

= Puanlar site ile uygulamada tutarlı mı? =

Evet. Uygulama temanın `sl_user_rating_*` anahtarlarını güncelleyerek oy verir. Fark: tema tarayıcı
çerezine güveniyor, uygulamada oy WordPress kullanıcısına bağlanır (`psb_rating_voters`), bu yüzden
oy değiştirilebilir ve ortalama bozulmaz.

= Uygulamadan silinen oyun kaybolur mu? =

Hayır, çöp kutusuna taşınır. wp-admin'den geri alınabilir.

= 401 alıyorum ama parola doğru =

Bazı sunucular `Authorization` başlığını PHP'ye geçirmez. Uygulama yedek olarak
`X-PixelStore-Token` başlığı da gönderir. Yine olmuyorsa `.htaccess` dosyasına `CGIPassAuth On`
ekleyin.

== Endpoints ==

Tümü `/wp-json/pixelstore/v2/` altındadır.

Açık: `GET health`, `POST auth/login`, `POST auth/logout`

Oturum: `GET auth/me`, `POST|DELETE auth/avatar`, `GET games`, `GET games/{id}`, `GET search`,
`GET taxonomies`, `GET favorites`, `POST games/{id}/download`, `POST games/{id}/view`,
`POST games/{id}/favorite`, `POST games/{id}/rate`, `GET|POST games/{id}/reviews`,
`POST games/{id}/report`, `DELETE reviews/{id}`, `POST reviews/{id}/vote`

Yönetici: `POST games`, `PUT games/{id}`, `DELETE games/{id}`, `POST games/{id}/published`,
`POST games/{id}/cover`, `POST games/{id}/screenshots`, `DELETE games/{id}/screenshots/{attachment}`,
`GET stats`, `GET users`

== Changelog ==

= 1.0.0 =
* İlk sürüm: SteamLike `game` kayıt tipi köprüsü, jeton tabanlı oturum, istek listesi, puanlama,
  incelemeler ve oylar, hata raporu, profil fotoğrafı yükleme, canlı arama, yönetim CRUD'u ve
  kapak/ekran görüntüsü yükleme.
