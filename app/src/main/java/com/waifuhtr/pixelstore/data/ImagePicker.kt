package com.waifuhtr.pixelstore.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Yükleme için hazırlanmış görsel. */
data class PreparedImage(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
) {
    // ByteArray içeren data class'ta equals/hashCode dizinin kimliğine bakar; içeriğe göre karşılaştır.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PreparedImage) return false
        return fileName == other.fileName &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int =
        (fileName.hashCode() * 31 + contentType.hashCode()) * 31 + bytes.contentHashCode()
}

/**
 * Galeriden seçilen görseli yüklemeye hazırlar.
 *
 * Telefon kameraları 4000 piksel genişliğinde JPEG üretebiliyor; bunu olduğu gibi göndermek hem
 * kullanıcının verisini hem sunucunun sınırını zorlar. Görsel bu yüzden önce ölçeklenip yeniden
 * kodlanır.
 */
object ImagePicker {

    private const val AVATAR_MAX_EDGE = 512
    private const val COVER_MAX_EDGE = 1600
    private const val JPEG_QUALITY = 88

    suspend fun prepareAvatar(context: Context, uri: Uri): PreparedImage =
        prepare(context, uri, AVATAR_MAX_EDGE, "avatar")

    suspend fun prepareCover(context: Context, uri: Uri): PreparedImage =
        prepare(context, uri, COVER_MAX_EDGE, "kapak")

    suspend fun prepareScreenshot(context: Context, uri: Uri): PreparedImage =
        prepare(context, uri, COVER_MAX_EDGE, "ekran")

    private suspend fun prepare(
        context: Context,
        uri: Uri,
        maxEdge: Int,
        namePrefix: String
    ): PreparedImage = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Geçiş: yalnızca boyutları oku, tam bitmap'i belleğe almadan örnekleme oranını hesapla.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw PixelStoreException("Görsel okunamadı.")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw PixelStoreException("Görsel biçimi tanınamadı.")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw PixelStoreException("Görsel çözümlenemedi.")

        val scaled = scaleToFit(decoded, maxEdge)
        if (scaled !== decoded) decoded.recycle()

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        scaled.recycle()

        PreparedImage(
            fileName = "$namePrefix-${System.currentTimeMillis()}.jpg",
            contentType = "image/jpeg",
            bytes = out.toByteArray()
        )
    }

    /** İki kuvvetine yuvarlanan örnekleme oranı: BitmapFactory yalnızca bunları kullanır. */
    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxEdge) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleToFit(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
