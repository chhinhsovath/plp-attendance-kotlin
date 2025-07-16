package com.plp.attendance.ui.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.data.local.TestAccount
import com.plp.attendance.data.local.TestDataSeeder
import com.plp.attendance.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestLoginUiState(
    val testAccounts: List<TestAccount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggingInEmail: String? = null,
    val loginSuccess: Boolean = false,
    val defaultPassword: String = TestDataSeeder.DEFAULT_PASSWORD
)

@HiltViewModel
class TestLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val testDataSeeder: TestDataSeeder
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TestLoginUiState())
    val uiState: StateFlow<TestLoginUiState> = _uiState.asStateFlow()
    
    init {
        checkAndLoadTestAccounts()
    }
    
    private fun checkAndLoadTestAccounts() {
        viewModelScope.launch {
            val hasData = testDataSeeder.hasTestData()
            if (!hasData) {
                // No data exists, show empty state
                _uiState.update { 
                    it.copy(
                        testAccounts = emptyList(),
                        isLoading = false
                    )
                }
            } else {
                loadTestAccounts()
            }
        }
    }
    
    private fun loadTestAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val accounts = testDataSeeder.getTestAccounts()
                _uiState.update { 
                    it.copy(
                        testAccounts = accounts,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load test accounts: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun loginWithTestAccount(account: TestAccount) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    loggingInEmail = account.email,
                    error = null
                )
            }
            
            authRepository.login(
                email = account.email,
                password = account.password
            ).fold(
                onSuccess = { user ->
                    _uiState.update { 
                        it.copy(
                            loggingInEmail = null,
                            loginSuccess = true,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            loggingInEmail = null,
                            error = "ចូលប្រព័ន្ធបរាជ័យ: ${exception.message}"
                        )
                    }
                }
            )
        }
    }
    
    fun seedTestData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                testDataSeeder.seedTestData()
                // Add a small delay to ensure database writes complete
                kotlinx.coroutines.delay(500)
                loadTestAccounts()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to seed test data: ${e.message}"
                    )
                }
            }
        }
    }
}