package com.mezo.biometric

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mezo.biometric.data.VaultRepository
import com.mezo.biometric.security.BiometricPromptManager
import com.mezo.biometric.security.CryptoManager
import com.mezo.biometric.ui.theme.BiometricFeatureTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val biometricPromptManager by lazy {
        BiometricPromptManager(this)
    }

    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Biometric Results
        lifecycleScope.launch {
            biometricPromptManager.promptResults.collectLatest { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                        viewModel.onEvent(MainEvent.BiometricAuthSuccess(result.result))
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                        viewModel.onEvent(MainEvent.BiometricAuthError(result.error))
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                        viewModel.onEvent(MainEvent.BiometricAuthError("Authentication Failed"))
                    }
                    is BiometricPromptManager.BiometricResult.HardwareUnavailable -> {
                        viewModel.onEvent(MainEvent.BiometricAuthError("Biometric Hardware Unavailable"))
                    }
                    is BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                        viewModel.onEvent(MainEvent.BiometricAuthError("Biometric Feature Unavailable"))
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                        viewModel.onEvent(MainEvent.BiometricAuthError("Biometric Not Set"))
                    }
                }
            }
        }

        setContent {
            val state by viewModel.state.collectAsState()
            BiometricFeatureTheme(dynamicColor = state.isDynamicColor) {
                val snackbarHostState = remember { SnackbarHostState() }

                // Trigger Biometric Prompt
                LaunchedEffect(state.biometricPromptData) {
                    state.biometricPromptData?.let { data ->
                        biometricPromptManager.showBiometricPrompt(
                            title = data.title,
                            subtitle = data.subtitle,
                            description = data.description,
                            cryptoObject = data.cryptoObject
                        )
                        viewModel.consumeBiometricPrompt()
                    }
                }

                // Show Snackbar Messages
                LaunchedEffect(state.message) {
                    state.message?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.onEvent(MainEvent.ClearMessage)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    VaultScreen(
                        state = state,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier
                            .padding(innerPadding)
                            .imePadding() // Handles keyboard
                    )
                }
            }
        }
    }
}

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val prefs = context.getSharedPreferences("secure_vault", Context.MODE_PRIVATE)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                CryptoManager(),
                VaultRepository(prefs),
                prefs
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun VaultScreen(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Cartoony Shapes
    val bigRoundShape = RoundedCornerShape(32.dp)
    val inputShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = bigRoundShape
                ),
            shape = bigRoundShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Styling
                val isUnlocked = state.decryptedData != null
                
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (isUnlocked) "VAULT UNLOCKED" else "VAULT LOCKED",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (state.decryptedData != null) {
                    // Decrypted State - Jewel Box Style
                    Text(
                        text = "DECRYPTED MESSAGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                         Text(
                            text = state.decryptedData,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(28.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    // Input State - Clean & Minimal
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { 
                            Text(
                                "SECRET DATA", 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isVisible = !isVisible }) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        shape = inputShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Action Buttons - High Definition
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { 
                            keyboardController?.hide()
                            onEvent(MainEvent.EncryptData(textInput)) 
                            textInput = "" 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        enabled = textInput.isNotBlank() && state.decryptedData == null,
                        shape = bigRoundShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text("ENCRYPT", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp))
                    }

                    Button(
                        onClick = { 
                            keyboardController?.hide()
                            onEvent(MainEvent.DecryptData) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        enabled = state.encryptedDataStored,
                        shape = bigRoundShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 0.dp
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Text("DECRYPT", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp))
                    }
                }
            }
        }
        
        // Clear Button (Floating below or separate)
        if (state.encryptedDataStored || state.decryptedData != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    keyboardController?.hide()
                    onEvent(MainEvent.ClearData) 
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = bigRoundShape
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Vault", fontWeight = FontWeight.SemiBold)
            }
        }
        
        // Theme Toggle (Centered at the bottom)
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dynamic Theme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.material3.Switch(
                checked = state.isDynamicColor,
                onCheckedChange = { onEvent(MainEvent.ToggleDynamicColor(it)) }
            )
        }
    }
}
