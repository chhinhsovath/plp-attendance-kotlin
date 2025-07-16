package com.plp.attendance.services

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.plp.attendance.data.preferences.AppPreferences
import com.plp.attendance.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    
    private val keyStore by lazy { KeyStore.getInstance("AndroidKeyStore") }
    private val keyAlias = "plp_biometric_key"
    
    init {
        keyStore.load(null)
        generateSecretKey()
    }
    
    fun isBiometricAvailable(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.UNKNOWN
            else -> BiometricStatus.UNKNOWN
        }
    }
    
    fun isBiometricSetup(): Boolean {
        return isBiometricAvailable() == BiometricStatus.AVAILABLE
    }
    
    suspend fun isBiometricEnabledForAttendance(): Boolean {
        return appPreferences.isBiometricEnabledForAttendance.first()
    }
    
    suspend fun isBiometricEnabledForSensitiveActions(): Boolean {
        return appPreferences.isBiometricEnabledForSensitiveActions.first()
    }
    
    suspend fun setBiometricEnabledForAttendance(enabled: Boolean) {
        appPreferences.setBiometricEnabledForAttendance(enabled)
    }
    
    suspend fun setBiometricEnabledForSensitiveActions(enabled: Boolean) {
        appPreferences.setBiometricEnabledForSensitiveActions(enabled)
    }
    
    suspend fun authenticateForAttendance(
        activity: FragmentActivity,
        title: String = "Attendance Authentication",
        subtitle: String = "Use your biometric to mark attendance"
    ): Result<Boolean> {
        if (!isBiometricEnabledForAttendance()) {
            return Result.Success(true) // Skip if not enabled
        }
        
        return performBiometricAuthentication(activity, title, subtitle)
    }
    
    suspend fun authenticateForSensitiveAction(
        activity: FragmentActivity,
        title: String = "Secure Action",
        subtitle: String = "Use your biometric to confirm this action"
    ): Result<Boolean> {
        if (!isBiometricEnabledForSensitiveActions()) {
            return Result.Success(true) // Skip if not enabled
        }
        
        return performBiometricAuthentication(activity, title, subtitle)
    }
    
    private suspend fun performBiometricAuthentication(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            val biometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        continuation.resume(Result.Success(true))
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        continuation.resume(Result.Error(errString.toString()))
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        continuation.resume(Result.Error("Authentication failed"))
                    }
                }
            )
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            
            try {
                val cipher = getCipher()
                val cryptoObject = BiometricPrompt.CryptoObject(cipher)
                biometricPrompt.authenticate(promptInfo, cryptoObject)
            } catch (e: Exception) {
                continuation.resume(Result.Error("Failed to initialize biometric authentication: ${e.message}"))
            }
            
            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }
        }
    }
    
    private fun generateSecretKey() {
        if (keyStore.containsAlias(keyAlias)) {
            return // Key already exists
        }
        
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    private fun getCipher(): Cipher {
        val cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }
    
    fun getBiometricSettings(): Flow<BiometricSettings> {
        return appPreferences.biometricSettings
    }
    
    suspend fun updateBiometricSettings(settings: BiometricSettings) {
        appPreferences.updateBiometricSettings(settings)
    }
    
    fun getStatusMessage(status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.AVAILABLE -> "Biometric authentication is available"
            BiometricStatus.NO_HARDWARE -> "No biometric hardware available"
            BiometricStatus.HARDWARE_UNAVAILABLE -> "Biometric hardware is unavailable"
            BiometricStatus.NONE_ENROLLED -> "No biometric credentials enrolled"
            BiometricStatus.SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricStatus.UNSUPPORTED -> "Biometric authentication is not supported"
            BiometricStatus.UNKNOWN -> "Biometric status unknown"
        }
    }
}

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}

data class BiometricSettings(
    val isEnabledForAttendance: Boolean = false,
    val isEnabledForSensitiveActions: Boolean = false,
    val allowFallbackToPin: Boolean = true,
    val reAuthenticationTimeoutMinutes: Int = 15
)