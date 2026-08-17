# Compose ve Kotlin için AGP'nin varsayılan kuralları yeterli; buraya yalnızca projeye özgü
# gereksinimler yazılır.

# Katalog JSON'u org.json ile elle ayrıştırılıyor (yansıma yok), bu yüzden model sınıfları için
# keep kuralı gerekmiyor. Yine de veri sınıflarının adları kilitlenmez; yalnızca uyarılar susturulur.
-dontwarn org.json.**

# Robolectric ekran testleri release paketine girmez.
-dontwarn org.robolectric.**

# Kotlin coroutines / metadata: AGP kuralları kapsıyor, ek uyarıları sustur.
-dontwarn kotlinx.coroutines.**
