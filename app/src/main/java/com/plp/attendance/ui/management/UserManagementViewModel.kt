package com.plp.attendance.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.model.UserRole
import com.plp.attendance.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadUsers()
    }
    
    fun loadUsers(
        page: Int = 1,
        search: String? = null,
        role: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            userRepository.getUsers(
                page = page,
                limit = 20,
                search = search,
                role = role
            ).fold(
                onSuccess = { users ->
                    _uiState.update { 
                        it.copy(
                            users = if (page == 1) users else it.users + users,
                            isLoading = false,
                            currentPage = page,
                            hasMorePages = users.size == 20
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load users"
                        )
                    }
                }
            )
        }
    }
    
    fun searchUsers(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadUsers(page = 1, search = query, role = _uiState.value.selectedRole?.name)
    }
    
    fun filterByRole(role: UserRole?) {
        _uiState.update { it.copy(selectedRole = role) }
        loadUsers(page = 1, search = _uiState.value.searchQuery, role = role?.name)
    }
    
    fun loadMoreUsers() {
        if (!_uiState.value.isLoading && _uiState.value.hasMorePages) {
            val nextPage = _uiState.value.currentPage + 1
            loadUsers(
                page = nextPage,
                search = _uiState.value.searchQuery,
                role = _uiState.value.selectedRole?.name
            )
        }
    }
    
    fun loadUserById(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, detailError = null) }
            
            userRepository.getUserById(userId).fold(
                onSuccess = { user ->
                    _uiState.update { 
                        it.copy(
                            selectedUser = user,
                            isLoadingDetail = false
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            isLoadingDetail = false,
                            detailError = throwable.message ?: "Failed to load user details"
                        )
                    }
                }
            )
        }
    }
    
    fun createUser(
        email: String,
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String,
        phoneNumber: String?,
        organizationId: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            
            userRepository.createUser(
                email = email,
                username = username,
                password = password,
                firstName = firstName,
                lastName = lastName,
                role = role,
                phoneNumber = phoneNumber,
                organizationId = organizationId
            ).fold(
                onSuccess = { user ->
                    _uiState.update { 
                        it.copy(
                            isCreating = false,
                            createSuccess = true
                        )
                    }
                    // Reload users to show the new user
                    loadUsers(
                        page = 1,
                        search = _uiState.value.searchQuery,
                        role = _uiState.value.selectedRole?.name
                    )
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            isCreating = false,
                            createError = throwable.message ?: "Failed to create user"
                        )
                    }
                }
            )
        }
    }
    
    fun updateUser(
        userId: String,
        email: String? = null,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        role: String? = null,
        phoneNumber: String? = null,
        organizationId: String? = null,
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateError = null) }
            
            userRepository.updateUser(
                userId = userId,
                email = email,
                username = username,
                firstName = firstName,
                lastName = lastName,
                role = role,
                phoneNumber = phoneNumber,
                organizationId = organizationId,
                isActive = isActive
            ).fold(
                onSuccess = { user ->
                    _uiState.update { 
                        it.copy(
                            isUpdating = false,
                            updateSuccess = true,
                            selectedUser = user
                        )
                    }
                    // Update the user in the list
                    _uiState.update { state ->
                        state.copy(
                            users = state.users.map { 
                                if (it.id == userId) user else it 
                            }
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            isUpdating = false,
                            updateError = throwable.message ?: "Failed to update user"
                        )
                    }
                }
            )
        }
    }
    
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            
            userRepository.deleteUser(userId).fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            deleteSuccess = true,
                            users = it.users.filter { user -> user.id != userId }
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            deleteError = throwable.message ?: "Failed to delete user"
                        )
                    }
                }
            )
        }
    }
    
    fun toggleUserStatus(userId: String, isActive: Boolean) {
        viewModelScope.launch {
            if (isActive) {
                userRepository.deactivateUser(userId)
            } else {
                userRepository.activateUser(userId)
            }.fold(
                onSuccess = { user ->
                    // Update the user in the list
                    _uiState.update { state ->
                        state.copy(
                            users = state.users.map { 
                                if (it.id == userId) user else it 
                            },
                            selectedUser = if (state.selectedUser?.id == userId) user else state.selectedUser
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            error = throwable.message ?: "Failed to update user status"
                        )
                    }
                }
            )
        }
    }
    
    fun resetUserPassword(userId: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResettingPassword = true, passwordResetError = null) }
            
            userRepository.resetUserPassword(userId, newPassword).fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isResettingPassword = false,
                            passwordResetSuccess = true
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            isResettingPassword = false,
                            passwordResetError = throwable.message ?: "Failed to reset password"
                        )
                    }
                }
            )
        }
    }
    
    fun clearErrors() {
        _uiState.update { 
            it.copy(
                error = null,
                detailError = null,
                createError = null,
                updateError = null,
                deleteError = null,
                passwordResetError = null
            )
        }
    }
    
    fun clearSuccessFlags() {
        _uiState.update { 
            it.copy(
                createSuccess = false,
                updateSuccess = false,
                deleteSuccess = false,
                passwordResetSuccess = false
            )
        }
    }
}

data class UserManagementUiState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val isResettingPassword: Boolean = false,
    val error: String? = null,
    val detailError: String? = null,
    val createError: String? = null,
    val updateError: String? = null,
    val deleteError: String? = null,
    val passwordResetError: String? = null,
    val createSuccess: Boolean = false,
    val updateSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val passwordResetSuccess: Boolean = false,
    val searchQuery: String = "",
    val selectedRole: UserRole? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
)