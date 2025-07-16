package com.plp.attendance.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.domain.model.Leave
import com.plp.attendance.domain.model.LeaveStatus
import com.plp.attendance.domain.model.LeaveType
import com.plp.attendance.domain.repository.LeaveRepository
import com.plp.attendance.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaveUiState())
    val uiState: StateFlow<LeaveUiState> = _uiState.asStateFlow()

    val myLeaves = leaveRepository.getCachedLeaves()
    val pendingLeaves = leaveRepository.getCachedLeavesByStatus(LeaveStatus.PENDING)

    init {
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = user != null,
                    currentUser = user
                )
                // Only show error if we've explicitly checked and there's no user
                // Don't show error immediately on initialization
                if (user == null && _uiState.value.error == null) {
                    _uiState.value = _uiState.value.copy(
                        error = null // Don't show error immediately
                    )
                }
            }
        }
    }

    fun submitLeaveRequest(
        leaveType: LeaveType,
        startDate: Long,
        endDate: Long,
        reason: String,
        attachmentPath: String? = null
    ) {
        viewModelScope.launch {
            if (!_uiState.value.isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "No authentication token found"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            leaveRepository.submitLeaveRequest(
                leaveType = leaveType,
                startDate = startDate,
                endDate = endDate,
                reason = reason,
                attachmentFile = null // TODO: Convert attachmentPath to File if needed
            ).fold(
                onSuccess = { leave ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        submitSuccess = true,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun approveLeave(leaveId: String, comments: String? = null) {
        viewModelScope.launch {
            if (!_uiState.value.isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "No authentication token found"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            leaveRepository.approveLeaveRequest(leaveId, comments).fold(
                onSuccess = { leave ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        approvalSuccess = true,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun rejectLeave(leaveId: String, comments: String? = null) {
        viewModelScope.launch {
            if (!_uiState.value.isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "No authentication token found"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            leaveRepository.rejectLeaveRequest(leaveId, comments).fold(
                onSuccess = { leave ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        approvalSuccess = true,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun cancelLeave(leaveId: String) {
        viewModelScope.launch {
            if (!_uiState.value.isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "No authentication token found"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            leaveRepository.cancelLeaveRequest(leaveId).fold(
                onSuccess = { leave ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        cancelSuccess = true,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(
            submitSuccess = false,
            approvalSuccess = false,
            cancelSuccess = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class LeaveUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val submitSuccess: Boolean = false,
    val approvalSuccess: Boolean = false,
    val cancelSuccess: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: com.plp.attendance.domain.model.User? = null
)