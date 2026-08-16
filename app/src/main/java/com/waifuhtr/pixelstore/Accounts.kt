package com.waifuhtr.pixelstore

import android.content.Context
import java.security.MessageDigest

/**
 * Yetki seviyesi. Rolü her zaman native taraf belirler; JS tarafından gelen bir rol iddiasına
 * asla güvenilmez.
 */
enum class Role(val wire: String) {
    ADMIN("admin"),
    USER("user");

    companion object {
        fun fromWire(value: String?): Role? = entries.firstOrNull { it.wire == value }
    }
}

data class Account(
    val username: String,
    val displayName: String,
    val role: Role,
    val passwordHash: String,
    val avatarSeed: String,
    val joinedAt: String,
    val note: String
)

data class Session(
    val username: String,
    val displayName: String,
    val role: Role,
    val avatarSeed: String
) {
    val isAdmin: Boolean get() = role == Role.ADMIN
}

/**
 * Demo hesap tablosu ve oturum durumu.
 *
 * Parolalar kaynak kodda düz metin tutulmaz; sabit bir uygulama tuzuyla SHA-256 özetleri saklanır.
 * (Demo parolaları README'de belgelidir, giriş ekranındaki demo düğmeleri de bunları kullanır.)
 */
class AccountManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var current: Session? = null

    init {
        // Uygulama yeniden açıldığında oturumu sürdür. Rol prefs'ten değil, her zaman hesap
        // tablosundan okunur; böylece prefs kurcalansa bile yetki yükseltilemez.
        val saved = prefs.getString(KEY_USER, null)
        current = saved?.let { name -> ACCOUNTS.firstOrNull { it.username == name }?.toSession() }
    }

    val session: Session? get() = current

    fun login(username: String, password: String): Session? {
        val cleanUser = username.trim().lowercase()
        if (cleanUser.isEmpty() || cleanUser.length > MAX_FIELD || password.length > MAX_FIELD) {
            return null
        }
        val account = ACCOUNTS.firstOrNull { it.username == cleanUser } ?: return null
        if (!constantTimeEquals(account.passwordHash, hash(cleanUser, password))) return null

        val session = account.toSession()
        current = session
        prefs.edit().putString(KEY_USER, account.username).apply()
        return session
    }

    fun logout() {
        current = null
        prefs.edit().remove(KEY_USER).apply()
    }

    /** Admin panelindeki kullanıcı listesi. Parola özetleri asla dışarı verilmez. */
    fun directory(): List<Account> = ACCOUNTS

    private fun Account.toSession() = Session(username, displayName, role, avatarSeed)

    private fun hash(username: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$SALT|$username|$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    companion object {
        private const val PREFS_NAME = "pixelstore_session"
        private const val KEY_USER = "session_user"
        private const val SALT = "pixelstore.v1"
        private const val MAX_FIELD = 64

        /**
         * Demo hesaplar:
         *  - admin / admin123  -> tüm admin paneli görünür
         *  - user  / user123   -> yalnızca mağaza
         */
        val ACCOUNTS = listOf(
            Account(
                username = "admin",
                displayName = "Yönetici Yamato",
                role = Role.ADMIN,
                passwordHash = "bcaac04b31d7ace3394cb7fb59f44d007ee543e55560979e11e0cfc8adcd2314",
                avatarSeed = "warden-01",
                joinedAt = "2024-03-11",
                note = "Katalog yöneticisi"
            ),
            Account(
                username = "user",
                displayName = "Gezgin Kaya",
                role = Role.USER,
                passwordHash = "6adab157f57a5f88d11676a7eada1b876ccaa2b8e5876fc0230fe45532c076a5",
                avatarSeed = "wanderer-07",
                joinedAt = "2025-01-08",
                note = "Standart oyuncu"
            )
        )
    }
}
