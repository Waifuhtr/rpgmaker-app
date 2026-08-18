package com.waifuhtr.pixelstore.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

/**
 * Küçük HTTP/JSON istemcisi.
 *
 * Ek bir ağ kütüphanesi getirilmiyor: uçlar az sayıda ve gövdeler küçük, [HttpURLConnection]
 * yeterli. Görsel yükleme için elle multipart gövdesi kurulur.
 */
object HttpJson {

    private const val TAG = "PixelStoreHttp"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 25_000
    private const val UPLOAD_TIMEOUT_MS = 60_000
    private const val MAX_BODY_CHARS = 4 * 1024 * 1024

    suspend fun json(
        url: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = open(url, method, token, READ_TIMEOUT_MS)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            read(connection)
        } catch (e: PixelStoreException) {
            throw e
        } catch (e: Exception) {
            throw translate(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Tek dosyalık multipart yükleme (profil fotoğrafı, oyun kapağı, ekran görüntüsü).
     * Gövde bellekte kurulmaz; doğrudan akışa yazılır.
     */
    suspend fun upload(
        url: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
        token: String?
    ): JSONObject = withContext(Dispatchers.IO) {
        val boundary = "----PixelStore" + System.nanoTime().toString(16)
        var connection: HttpURLConnection? = null
        try {
            connection = open(url, "POST", token, UPLOAD_TIMEOUT_MS)
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setFixedLengthStreamingMode(
                multipartLength(boundary, fileName, contentType, bytes.size)
            )
            connection.outputStream.use { stream ->
                stream.writeAscii("--$boundary\r\n")
                stream.writeAscii(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n"
                )
                stream.writeAscii("Content-Type: $contentType\r\n\r\n")
                stream.write(bytes)
                stream.writeAscii("\r\n--$boundary--\r\n")
            }
            read(connection)
        } catch (e: PixelStoreException) {
            throw e
        } catch (e: Exception) {
            throw translate(e)
        } finally {
            connection?.disconnect()
        }
    }

    fun query(params: Map<String, String?>): String {
        val pairs = params.entries
            .filter { !it.value.isNullOrBlank() }
            .map { "${encode(it.key)}=${encode(it.value!!)}" }
        return if (pairs.isEmpty()) "" else "?" + pairs.joinToString("&")
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    /* ---- İç yardımcılar ---------------------------------------------------------------------- */

    private fun open(url: String, method: String, token: String?, readTimeout: Int): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            this.readTimeout = readTimeout
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PixelStore-Android")
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
                // Authorization başlığını PHP'ye geçirmeyen sunucular için yedek.
                setRequestProperty("X-PixelStore-Token", token)
            }
        }

    private fun read(connection: HttpURLConnection): JSONObject {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
        if (text.length > MAX_BODY_CHARS) {
            throw PixelStoreException("Sunucu yanıtı beklenenden büyük.")
        }

        val json = runCatching { JSONObject(text) }.getOrNull()

        if (status !in 200..299) {
            // WordPress hataları {code, message, data:{status}} biçiminde döner.
            val message = json?.optString("message")?.takeIf { it.isNotBlank() }
                ?: when (status) {
                    401 -> "Oturum geçersiz, yeniden giriş yap."
                    403 -> "Bu işlem için yetkin yok."
                    404 -> "İstenen kayıt bulunamadı."
                    413 -> "Dosya çok büyük."
                    503 -> "Sunucu şu an hizmet veremiyor."
                    else -> "Sunucu hatası (HTTP $status)."
                }
            throw PixelStoreException(stripTags(message), )
        }

        json ?: throw PixelStoreException("Sunucu geçerli JSON döndürmedi.")
        if (!json.optBoolean("ok", true)) {
            throw PixelStoreException(json.optString("error").ifBlank { "İşlem başarısız." })
        }
        return json
    }

    private fun translate(e: Exception): PixelStoreException = when (e) {
        is UnknownHostException -> PixelStoreException("Sunucuya ulaşılamadı. İnternet bağlantını kontrol et.")
        is SocketTimeoutException -> PixelStoreException("Sunucu zaman aşımına uğradı.")
        is IOException -> {
            Log.w(TAG, "İstek başarısız: ${e.javaClass.simpleName}")
            PixelStoreException("Bağlantı hatası: ${e.javaClass.simpleName}")
        }
        else -> {
            Log.e(TAG, "Beklenmeyen hata: ${e.javaClass.simpleName}")
            PixelStoreException("Beklenmeyen bir hata oluştu.")
        }
    }

    private fun multipartLength(boundary: String, fileName: String, contentType: String, size: Int): Long {
        val header = "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n" +
            "Content-Type: $contentType\r\n\r\n"
        val footer = "\r\n--$boundary--\r\n"
        return header.toByteArray(Charsets.UTF_8).size.toLong() +
            size.toLong() +
            footer.toByteArray(Charsets.UTF_8).size.toLong()
    }

    private fun OutputStream.writeAscii(value: String) = write(value.toByteArray(Charsets.UTF_8))

    /** Sunucu mesajları HTML içerebilir; kullanıcıya düz metin gösterilir. */
    private fun stripTags(value: String): String =
        value.replace(Regex("<[^>]*>"), "").trim().take(240)
}
