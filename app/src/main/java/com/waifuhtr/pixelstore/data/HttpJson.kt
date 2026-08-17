package com.waifuhtr.pixelstore.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Küçük JSON HTTP istemcisi.
 *
 * Ek bir ağ kütüphanesi getirmemek için [HttpURLConnection] kullanılır: tek uç noktalı,
 * kısa gövdeli bir REST API için yeterli ve APK'ya boyut eklemiyor.
 */
object HttpJson {

    private const val TAG = "PixelStoreHttp"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val MAX_BODY_BYTES = 2 * 1024 * 1024

    suspend fun request(
        url: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PixelStore-Android")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }

            if (body != null) {
                connection.outputStream.use { stream ->
                    stream.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()

            if (text.length > MAX_BODY_BYTES) {
                throw PixelStoreException("Sunucu yanıtı beklenenden büyük.")
            }

            val json = runCatching { JSONObject(text) }.getOrNull()

            if (status !in 200..299) {
                // WordPress hataları {code, message, data:{status}} biçiminde döner.
                val message = json?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: when (status) {
                        401, 403 -> "Yetki reddedildi (HTTP $status)."
                        404 -> "Uç bulunamadı. PixelStore eklentisi etkin mi?"
                        else -> "Sunucu hatası (HTTP $status)."
                    }
                throw PixelStoreException(stripTags(message))
            }

            json ?: throw PixelStoreException("Sunucu geçerli JSON döndürmedi.")
        } catch (e: PixelStoreException) {
            throw e
        } catch (e: UnknownHostException) {
            throw PixelStoreException("Sunucuya ulaşılamadı. Adresi ve interneti kontrol et.")
        } catch (e: SocketTimeoutException) {
            throw PixelStoreException("Sunucu zaman aşımına uğradı.")
        } catch (e: IOException) {
            Log.w(TAG, "İstek başarısız: ${e.javaClass.simpleName}")
            throw PixelStoreException("Bağlantı hatası: ${e.javaClass.simpleName}")
        } finally {
            connection?.disconnect()
        }
    }

    /** WordPress hata mesajları HTML içerebilir; kullanıcıya düz metin gösterilir. */
    private fun stripTags(value: String): String =
        value.replace(Regex("<[^>]*>"), "").trim().take(200)
}
