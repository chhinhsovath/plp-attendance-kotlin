package com.plp.attendance.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.domain.repository.AuthRepository
import com.plp.attendance.data.preferences.DeveloperPreferences
import com.plp.attendance.domain.model.User
import com.plp.attendance.utils.ErrorHandler
import com.plp.attendance.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val errorHandler: ErrorHandler,
    private val developerPreferences: DeveloperPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn = authRepository.getCurrentUser().map { it != null }

    fun login(email: String, password: String) {
        // Validate inputs first
        val emailError = ValidationUtils.getEmailError(email)
        val passwordError = ValidationUtils.getPasswordError(password)
        
        if (emailError != null || passwordError != null) {
            _uiState.value = _uiState.value.copy(
                error = emailError ?: passwordError
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.login(email, password).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorHandler.getErrorMessage(exception)
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            authRepository.logout().fold(
                onSuccess = {
                    _uiState.value = AuthUiState()
                },
                onFailure = {
                    // Still clear UI state even if logout fails
                    _uiState.value = AuthUiState()
                }
            )
        }
    }
    
    fun toggleDeveloperMode() {
        viewModelScope.launch {
            val currentMode = developerPreferences.useMockAuth.first()
            developerPreferences.setUseMockAuth(!currentMode)
            _uiState.value = _uiState.value.copy(
                error = if (!currentMode) "Switched to Local Mode" else "Switched to API Mode"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // Direct login for test users without validation
    fun directLogin(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.login(username, password).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorHandler.getErrorMessage(exception)
                    )
                }
            )
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)