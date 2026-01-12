package com.mezo.biometric

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mezo.biometric.data.VaultRepository
import com.mezo.biometric.security.BiometricPromptManager
import com.mezo.biometric.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class MainViewModel(
    private val cryptoManager: CryptoManager,
    private val vaultRepository: VaultRepository,
    private val prefs: android.content.SharedPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(MainState(
        isDynamicColor = prefs.getBoolean("dynamic_color", true)
    ))
    val state = _state.asStateFlow()

    init {
        // Load initial state (check if data exists)
        val encryptedData = vaultRepository.getEncryptedData()
        if (encryptedData != null) {
            _state.update { it.copy(encryptedDataStored = true) }
        }
    }

    private fun initializeDefaultData() {
        try {
            val defaultText = "تبقي حبيبتي يملك ؟"
            val cipher = cryptoManager.getEncryptCipher()
            val ciphertext = cipher.doFinal(defaultText.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            vaultRepository.saveEncryptedData(ciphertext, iv)
            _state.update { it.copy(encryptedDataStored = true) }
        } catch (e: Exception) {
            // If init fails (e.g. key issue), we just start empty.
            e.printStackTrace()
        }
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.ToggleDynamicColor -> {
                val newValue = event.enabled
                prefs.edit().putBoolean("dynamic_color", newValue).apply()
                _state.update { it.copy(isDynamicColor = newValue) }
            }
            is MainEvent.EncryptData -> {
                encryptData(event.data)
            }
            is MainEvent.DecryptData -> {
                decryptData()
            }
            is MainEvent.BiometricAuthSuccess -> {
                // Auth succeeded. Now we can access the key (valid for x seconds).
                if (state.value.operation == Operation.ENCRYPT) {
                    finalizeEncryption(state.value.tempDataToEncrypt)
                } else if (state.value.operation == Operation.DECRYPT) {
                    finalizeDecryption()
                }
            }
            is MainEvent.BiometricAuthError -> {
                _state.update { it.copy(message = "Auth Error: ${event.error}") }
            }
            MainEvent.ClearMessage -> {
                _state.update { it.copy(message = null) }
            }
            MainEvent.ClearData -> {
                vaultRepository.clearData()
                _state.update {
                    it.copy(
                        encryptedDataStored = false,
                        decryptedData = null,
                        message = "Vault Cleared",
                        tempDataToEncrypt = null
                    )
                }
            }
        }
    }

    private fun encryptData(data: String) {
        // Trigger Auth (No CryptoObject passed)
        _state.update {
            it.copy(
                biometricPromptData = BiometricPromptData(
                    cryptoObject = null,
                    title = "Unlock Secure Vault",
                    subtitle = "Identity Verification",
                    description = "To view your secret data, strict authentication is required. Please confirm your identity to proceed."
                ),
                operation = Operation.ENCRYPT,
                tempDataToEncrypt = data
            )
        }
    }

    private fun decryptData() {
        val encryptedData = vaultRepository.getEncryptedData()
        if (encryptedData == null) {
            _state.update { it.copy(message = "No data to decrypt") }
            return
        }

        // Trigger Auth (No CryptoObject passed)
        _state.update {
            it.copy(
                biometricPromptData = BiometricPromptData(
                    cryptoObject = null,
                    title = "Access Secret Data",
                    subtitle = "Identity Verification",
                    description = "To view your secret data, strict authentication is required. Please confirm your identity to proceed."
                ),
                operation = Operation.DECRYPT
            )
        }
    }

    private fun finalizeEncryption(data: String?) {
        if (data == null) return
        try {
            // Get Cipher NOW (Auth just happened)
            val cipher = cryptoManager.getEncryptCipher()
            val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            vaultRepository.saveEncryptedData(ciphertext, iv)
            _state.update {
                it.copy(
                    encryptedDataStored = true,
                    message = "Data Encrypted & Saved!",
                    biometricPromptData = null,
                    tempDataToEncrypt = null,
                    decryptedData = null // Hide previous data
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(message = "Encryption Failed: ${e.message}") }
        }
    }

    private fun finalizeDecryption() {
        val encryptedData = vaultRepository.getEncryptedData() ?: return
        val (ciphertext, iv) = encryptedData
        
        try {
            // Get Cipher NOW (Auth just happened)
            val cipher = cryptoManager.getDecryptCipher(iv)
            val plaintext = cipher.doFinal(ciphertext)
            _state.update {
                it.copy(
                    decryptedData = String(plaintext, Charsets.UTF_8),
                    message = "Decryption Successful",
                    biometricPromptData = null
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(message = "Decryption Failed: ${e.message}") }
        }
    }

    fun consumeBiometricPrompt() {
        _state.update { it.copy(biometricPromptData = null) }
    }
}

data class MainState(
    val encryptedDataStored: Boolean = false,
    val decryptedData: String? = null,
    val message: String? = null,
    val biometricPromptData: BiometricPromptData? = null,
    val operation: Operation = Operation.NONE,
    val tempDataToEncrypt: String? = null,
    val isDynamicColor: Boolean = true
)

data class BiometricPromptData(
    val cryptoObject: BiometricPrompt.CryptoObject?,
    val title: String,
    val subtitle: String,
    val description: String
)

enum class Operation {
    NONE, ENCRYPT, DECRYPT
}

sealed interface MainEvent {
    data class ToggleDynamicColor(val enabled: Boolean) : MainEvent
    data class EncryptData(val data: String) : MainEvent
    data object DecryptData : MainEvent
    data class BiometricAuthSuccess(val result: BiometricPrompt.AuthenticationResult) : MainEvent
    data class BiometricAuthError(val error: String) : MainEvent
    data object ClearMessage : MainEvent
    data object ClearData : MainEvent
}