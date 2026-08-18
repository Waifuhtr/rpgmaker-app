package com.waifuhtr.pixelstore.data

import android.content.Context

/**
 * Kalıcı tercihler.
 *
 * Sunucu adresi burada TUTULMAZ: derleme sırasında gömülür ve değiştirilemez. Burada yalnızca
 * oturum jetonu ve arayüz tercihleri durur.
 */
class Settings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Sunucudan alınan oturum jetonu. Yalnızca uygulamanın özel alanında saklanır. */
    var token: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) {
            prefs.edit().apply {
                if (value.isBlank()) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }.apply()
        }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    private companion object {
        const val PREFS = "pixelstore_settings"
        const val KEY_TOKEN = "session_token"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_HAPTICS = "haptics_enabled"
    }
}
