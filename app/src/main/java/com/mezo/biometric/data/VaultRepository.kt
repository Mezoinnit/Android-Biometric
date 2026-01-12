package com.mezo.biometric.data

import android.content.SharedPreferences

class VaultRepository(private val prefs: SharedPreferences) {

    fun saveEncryptedData(ciphertext: ByteArray, iv: ByteArray) {
        prefs.edit()
            .putString("ciphertext", ciphertext.joinToString(separator = ",") { it.toString() })
            .putString("iv", iv.joinToString(separator = ",") { it.toString() })
            .apply()
    }

    fun getEncryptedData(): Pair<ByteArray, ByteArray>? {
        val ciphertextString = prefs.getString("ciphertext", null)
        val ivString = prefs.getString("iv", null)

        if (ciphertextString == null || ivString == null) return null

        val ciphertext = ciphertextString.split(",").map { it.toByte() }.toByteArray()
        val iv = ivString.split(",").map { it.toByte() }.toByteArray()

        return Pair(ciphertext, iv)
    }

    fun clearData() {
        prefs.edit().clear().apply()
    }
}
