package com.plp.attendance.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.data.local.TestDataSeederV2
import com.plp.attendance.domain.repository.UserRepository
import com.plp.attendance.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserSelectionUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UserSelectionViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val testDataSeeder: TestDataSeederV2
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserSelectionUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        checkAndSeedTestData()
    }
    
    private fun checkAndSeedTestData() {
        viewModelScope.launch {
            try {
                // Check if there are any users in the database
                val hasData = testDataSeeder.hasTestData()
                if (!hasData) {
                    // Seed test data if database is empty
                    testDataSeeder.seedTestData()
                }
                // Load users after checking/seeding
                loadUsers()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to initialize data: ${e.message}"
                )
            }
        }
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            userRepository.getCachedUsers()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to load users"
                    )
                }
                .collect { users: List<User> ->
                    _uiState.value = UserSelectionUiState(
                        users = users.sortedWith(
                            compareBy<User>(
                                { it.role.ordinal }, // Sort by role hierarchy
                                { it.name } // Then by name alphabetically
                            )
                        ),
                        isLoading = false
                    )
                }
        }
    }
}