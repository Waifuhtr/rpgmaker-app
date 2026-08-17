package com.waifuhtr.pixelstore.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.PI
import kotlin.math.sin

/** Arayüz sesleri ve dokunsal geri bildirim. */
enum class Sfx { MOVE, SELECT, CANCEL, ERROR, COIN, OPEN }

interface Feedback {
    fun play(sfx: Sfx)
    fun haptic(durationMs: Int = 12)

    /** Ses + titreşim birlikte: düğme dokunuşlarının standart tepkisi. */
    fun tap(sfx: Sfx = Sfx.SELECT, hapticMs: Int = 12) {
        play(sfx)
        haptic(hapticMs)
    }
}

/** Testlerde ve önizlemelerde kullanılan sessiz uygulama. */
object NoopFeedback : Feedback {
    override fun play(sfx: Sfx) = Unit
    override fun haptic(durationMs: Int) = Unit
}

val LocalFeedback = staticCompositionLocalOf<Feedback> { NoopFeedback }

/**
 * 8-bit sesleri çalışma zamanında üretir — depoda ses dosyası taşınmaz.
 *
 * Her efekt için kısa bir kare/üçgen dalga PCM tamponu bir kez hazırlanır ve statik moddaki
 * [AudioTrack] ile tekrar tekrar çalınır; her dokunuşta yeni tampon ayırmak gerekmez.
 */
class PixelFeedback(
    context: Context,
    private val soundEnabled: () -> Boolean,
    private val hapticsEnabled: () -> Boolean
) : Feedback {

    private val appContext = context.applicationContext
    private val tracks = mutableMapOf<Sfx, AudioTrack>()

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }
    }

    override fun play(sfx: Sfx) {
        if (!soundEnabled()) return
        runCatching {
            val track = tracks.getOrPut(sfx) { buildTrack(sfx) }
            track.stop()
            track.reloadStaticData()
            track.play()
        }
        // Ses başarısız olursa arayüz çalışmaya devam eder; kullanıcıya hata göstermeye değmez.
    }

    override fun haptic(durationMs: Int) {
        if (!hapticsEnabled()) return
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching {
            device.vibrate(
                VibrationEffect.createOneShot(
                    durationMs.coerceIn(5, 80).toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    fun release() {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }

    private fun buildTrack(sfx: Sfx): AudioTrack {
        val spec = specs.getValue(sfx)
        val samples = generate(spec)
        val bytes = samples.size * 2
        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bytes,
                AudioTrack.MODE_STATIC
            )
        }
        track.write(samples, 0, samples.size)
        track.setVolume(spec.volume)
        return track
    }

    private fun generate(spec: Spec): ShortArray {
        val count = (SAMPLE_RATE * spec.durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val progress = i.toFloat() / count
            val freq = spec.startHz + (spec.endHz - spec.startHz) * progress
            val phase = 2.0 * PI * freq * i / SAMPLE_RATE
            val raw = when (spec.wave) {
                Wave.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
                Wave.TRIANGLE -> 2.0 / PI * kotlin.math.asin(sin(phase))
                Wave.SAW -> 2.0 * ((freq * i / SAMPLE_RATE) % 1.0) - 1.0
            }
            // Üstel sönüm: tık sesinin kuyruğu kesilmiş gibi durmasın.
            val envelope = (1.0 - progress) * (1.0 - progress)
            out[i] = (raw * envelope * Short.MAX_VALUE * 0.35).toInt().toShort()
        }
        return out
    }

    private enum class Wave { SQUARE, TRIANGLE, SAW }

    private data class Spec(
        val startHz: Double,
        val endHz: Double,
        val durationSec: Double,
        val wave: Wave,
        val volume: Float
    )

    private companion object {
        const val SAMPLE_RATE = 22050

        val specs = mapOf(
            Sfx.MOVE to Spec(440.0, 440.0, 0.045, Wave.SQUARE, 0.5f),
            Sfx.SELECT to Spec(660.0, 780.0, 0.070, Wave.SQUARE, 0.6f),
            Sfx.CANCEL to Spec(300.0, 180.0, 0.090, Wave.SQUARE, 0.5f),
            Sfx.ERROR to Spec(180.0, 110.0, 0.160, Wave.SAW, 0.5f),
            Sfx.COIN to Spec(880.0, 1320.0, 0.110, Wave.SQUARE, 0.6f),
            Sfx.OPEN to Spec(320.0, 660.0, 0.090, Wave.TRIANGLE, 0.6f)
        )
    }
}
