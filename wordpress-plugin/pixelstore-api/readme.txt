=== PixelStore API ===
Contributors: pixelstore
Tags: rest-api, android, catalog
Requires at least: 6.0
Tested up to: 6.7
Requires PHP: 7.4
Stable tag: 1.0.0
License: MIT

PixelStore Android uygulamasının veritabanı ve REST arka ucu.

== Description ==

Bu eklenti WordPress'i PixelStore uygulamasının veritabanı olarak kullanılabilir hale getirir:

* Uygulama kayıtları `pixelstore_app` özel yazı tipinde tutulur (wp-admin'den de düzenlenebilir)
* Türler `pixelstore_category` taksonomisindedir
* `/wp-json/pixelstore/v1/...` altında jeton tabanlı bir REST API sunar
* Yetki kararı WordPress rollerinden gelir: `manage_options` veya `edit_others_posts`
  yeteneği olan kullanıcılar uygulamada yönetim panelini görür

Uygulama, adres girilmediğinde cihaz içi yerel katalogla çalışmaya devam eder; bu eklenti
isteğe bağlı bir arka uçtur.

== Installation ==

1. `pixelstore-api` klasörünü zip'leyip Eklentiler → Yeni ekle → Eklenti yükle ile kur.
2. Etkinleştir.
3. wp-admin → PixelStore ekranına git, "Demo kataloğu kur" düğmesine bas.
4. Aynı ekranda yazan site adresini uygulamadaki Profil → Bağlantı ayarları alanına gir.
5. WordPress kullanıcı adın ve parolanla giriş yap.

== Frequently Asked Questions ==

= Parolam nasıl saklanıyor? =

Eklenti parola saklamaz. Giriş `wp_authenticate()` ile WordPress'in kendi kullanıcı tablosuna
karşı doğrulanır. Başarılı girişte rastgele bir jeton üretilir; sunucuda yalnızca jetonun
SHA-256 özeti kullanıcı metasında durur ve 30 gün sonra düşer.

= Kullanıcı rolündeki biri yönetim verisine erişebilir mi? =

Hayır. Yönetim uçları yetenek kontrolünden geçer ve 403 döner. Katalog yükünde taslak kayıtlar
ile `published`/`createdAt` alanları kullanıcı rolüne hiç gönderilmez.

= 401 alıyorum ama parola doğru =

Bazı sunucular `Authorization` başlığını PHP'ye geçirmez. Uygulama yedek olarak
`X-PixelStore-Token` başlığını da gönderir. Yine olmuyorsa `.htaccess` dosyasına
`CGIPassAuth On` satırını ekleyin.

== Changelog ==

= 1.0.0 =
* İlk sürüm: özel yazı tipi, taksonomi, jeton tabanlı oturum, katalog/istatistik/kullanıcı uçları,
  demo katalog kurulumu ve wp-admin durum ekranı.
