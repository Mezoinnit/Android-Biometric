# Biometric Secure Vault Playground

This module demonstrates a high-security architecture for storing sensitive data using Android's Biometric Authentication and Keystore system.

## Features

*   **Time-Bound Secure Keys:** Cryptographic keys are generated in the Android Keystore and require user authentication. Once authenticated, the key remains valid for **10 seconds**, allowing multiple operations if needed.
*   **Flexible Authentication:** Supports **Face ID**, **Touch ID**, and **Device Credentials (PIN/Pattern/Password)**.
*   **Secure Storage:** Data is encrypted using AES-256 (CBC mode) and stored safely. The raw data is never stored, only the ciphertext and IV.
*   **Playground UI:** A simple interface to:
    1.  Enter a secret.
    2.  Encrypt it (Triggers Biometric Prompt).
    3.  Decrypt it (Triggers Biometric Prompt).

## Key Components

*   `CryptoManager.kt`: Handles the low-level Keystore operations.
*   `BiometricPromptManager.kt`: Manages the Biometric Prompt dialog and callbacks.
*   `MainViewModel.kt`: Orchestrates the flow between UI, Crypto, and Storage.
*   `MainActivity.kt`: The Compose UI.

## How to Test

1.  Run the app on a device or emulator with Biometrics (Fingerprint/Face) enabled.
    *   *Note: On Emulator, configure a Fingerprint in Settings > Security first.*
2.  Enter text in the field.
3.  Click **Encrypt & Save**.
    *   Authenticate when prompted.
    *   Toast confirms success. Text clears.
4.  Click **Decrypt**.
    *   Authenticate when prompted.
    *   The original secret is revealed.
