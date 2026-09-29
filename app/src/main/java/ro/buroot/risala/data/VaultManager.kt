package ro.buroot.risala.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted store for the hidden vault. Conversations moved into the vault are
 * removed from the SMS provider and kept here, encrypted with an AES key held
 * in the Android Keystore and gated behind biometric unlock in the UI.
 *
 * The key never leaves the Keystore; the vault file is meaningless without it.
 */
object VaultManager {

    private const val KEY_ALIAS = "risala_vault_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val VAULT_FILE = "vault.enc"

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    fun save(ctx: Context, plaintextJson: String) {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintextJson.toByteArray())
        val file = File(ctx.filesDir, VAULT_FILE)
        file.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(encrypted)
        }
    }

    fun load(ctx: Context): String {
        val file = File(ctx.filesDir, VAULT_FILE)
        if (!file.exists()) return "[]"
        file.inputStream().use { inp ->
            val ivLen = inp.read()
            val iv = ByteArray(ivLen).also { inp.read(it) }
            val encrypted = inp.readBytes()
            val cipher = Cipher.getInstance(TRANSFORM).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            }
            return String(cipher.doFinal(encrypted))
        }
    }
}
