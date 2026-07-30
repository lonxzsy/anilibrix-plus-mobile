package com.anilibrix.plus.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Шифрование секретов, которые лежат в DataStore.
 *
 * До этого токен авторизации хранился открытым текстом: любой, у кого есть
 * доступ к каталогу приложения (root, adb backup на отладочной сборке,
 * вредонос с той же uid), забирал живую сессию как есть.
 *
 * Ключ живёт в Android Keystore и **не покидает его** — наружу отдаётся
 * только результат шифрования. Взят AES/GCM: он даёт не только
 * конфиденциальность, но и аутентификацию, поэтому подменённый шифротекст
 * не расшифруется молча в мусор, а честно бросит исключение.
 *
 * `androidx.security:security-crypto` намеренно не используется: библиотека
 * так и осталась в alpha и фактически заморожена. Здесь нужен один ключ и
 * две операции — своя реализация проще и не тянет заброшенную зависимость.
 */
@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /**
     * Шифрует строку. IV кладётся в начало результата — иначе расшифровать
     * будет нечем: GCM требует тот же IV, а генерируется он каждый раз новый
     * (переиспользование IV в GCM ломает безопасность режима полностью).
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        iv.copyInto(combined)
        encrypted.copyInto(combined, iv.size)
        return PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Расшифровывает строку, ранее полученную из [encrypt].
     *
     * Возвращает `null`, если расшифровать не удалось. Это не проглатывание
     * ошибки, а нужное поведение: ключ Keystore пропадает при сбросе блокировки
     * экрана, переустановке и восстановлении из бэкапа. Единственный разумный
     * ответ на «ключа больше нет» — считать, что сессии нет, и попросить войти
     * заново.
     */
    fun decrypt(cipherText: String): String? {
        if (!isEncrypted(cipherText)) return null
        return runCatching {
            val combined = Base64.decode(cipherText.removePrefix(PREFIX), Base64.NO_WRAP)
            if (combined.size <= IV_SIZE) return null
            val iv = combined.copyOfRange(0, IV_SIZE)
            val payload = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(payload), Charsets.UTF_8)
        }.getOrNull()
    }

    /**
     * Признак того, что значение уже зашифровано.
     *
     * Нужен для одноразовой миграции: у существующих пользователей в DataStore
     * лежит plaintext-токен, и его надо отличить от нового формата, не сломав
     * им сессию при обновлении приложения.
     */
    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Без биометрии: токен нужен фоновым синхронизациям и воркерам,
                // которые работают при заблокированном экране.
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "anilibrix_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFIX = "enc:v1:"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
