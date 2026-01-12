package com.mezo.biometric.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val encryptCipher = Cipher.getInstance(TRANSFORMATION)
    private val decryptCipher = Cipher.getInstance(TRANSFORMATION)

    fun getEncryptCipher(): Cipher {
        val secretKey = getKey()
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return encryptCipher
    }

    fun getDecryptCipher(iv: ByteArray): Cipher {
        val secretKey = getKey()
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return decryptCipher
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    // Security Downgrade: Removed setUserAuthenticationParameters to allow Weak Biometrics (Face ID)
                    // The key is now accessible without strict Keystore Auth, relying on the App's UI Prompt instead.
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    companion object {
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val KEY_ALIAS = "biometric_vault_key_v3"
    }
}
