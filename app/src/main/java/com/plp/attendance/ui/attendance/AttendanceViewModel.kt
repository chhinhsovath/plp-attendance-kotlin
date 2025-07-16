package com.plp.attendance.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.domain.model.Attendance
import com.plp.attendance.domain.model.AttendanceStatus
import com.plp.attendance.domain.repository.AttendanceRepository
import com.plp.attendance.domain.repository.AuthRepository
import com.plp.attendance.services.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.location.Location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()
    
    private var locationUpdateJob: Job? = null
    private var wasOutsideGeofence = true // Track if user was previously outside

    init {
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = user != null,
                    currentUser = user,
                    schoolName = user?.schoolName,
                    schoolLatitude = user?.schoolLatitude,
                    schoolLongitude = user?.schoolLongitude
                )
                // Only show error if we've explicitly checked and there's no user
                // Don't show error immediately on initialization
                if (user == null && _uiState.value.error == null) {
                    // Set a flag to indicate we've checked authentication
                    _uiState.value = _uiState.value.copy(
                        error = null // Don't show error immediately
                    )
                }
            }
        }
    }

    fun loadTodayAttendance() {
        viewModelScope.launch {
            if (!_uiState.value.isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "Not authenticated. Please log in."
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            attendanceRepository.getTodayStatus().fold(
                onSuccess = { statusData ->
                    // Check both the old structure and the new structure for backward compatibility
                    val record = statusData.status?.record
                    val hasCheckedIn = statusData.status?.hasCheckedIn ?: statusData.hasCheckedIn
                    val hasCheckedOut = statusData.status?.hasCheckedOut ?: statusData.hasCheckedOut
                    
                    val todayAttendance = if (record != null && hasCheckedIn) {
                        // Parse from the new status.record structure
                        Attendance(
                            id = record.id,
                            userId = _uiState.value.currentUser?.id ?: "",
                            checkInTime = try {
                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }.parse(record.checkInTime ?: "")?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            },
                            checkOutTime = record.checkOutTime?.let {
                                try {
                                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).apply {
                                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    }.parse(it)?.time
                                } catch (e: Exception) {
                                    null
                                }
                            },
                            checkInLatitude = record.latitude?.toDoubleOrNull() ?: 0.0,
                            checkInLongitude = record.longitude?.toDoubleOrNull() ?: 0.0,
                            checkOutLatitude = null,
                            checkOutLongitude = null,
                            checkInPhotoUrl = null,
                            checkOutPhotoUrl = null,
                            status = when (record.status.lowercase()) {
                                "present" -> com.plp.attendance.domain.model.AttendanceStatus.PRESENT
                                "late" -> com.plp.attendance.domain.model.AttendanceStatus.LATE
                                "absent" -> com.plp.attendance.domain.model.AttendanceStatus.ABSENT
                                else -> com.plp.attendance.domain.model.AttendanceStatus.PRESENT
                            },
                            notes = record.notes,
                            isSynced = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else if (statusData.attendance != null) {
                        // Fall back to old structure if available
                        // ... existing mapping code ...
                        null
                    } else {
                        null
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        todayAttendance = todayAttendance,
                        error = null
                    )
                    
                    // SECURITY: Validate geofencing for already checked-in users
                    if (todayAttendance != null && todayAttendance.checkInTime != null && todayAttendance.checkOutTime == null) {
                        validateGeofencingForCheckedInUser()
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load attendance"
                    )
                }
            )
        }
    }

    fun checkIn() {
        viewModelScope.launch {
            if (!_uiState.value.isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "Not authenticated. Please log in."
                )
                return@launch
            }
            
            if (!_uiState.value.locationEnabled) {
                _uiState.value = _uiState.value.copy(
                    error = "Location permission is required for check-in"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current location
                val location = locationService.getCurrentLocation()
                if (location == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Unable to get current location"
                    )
                    return@launch
                }
                
                // Check if within geofence
                val schoolLat = _uiState.value.schoolLatitude
                val schoolLon = _uiState.value.schoolLongitude
                
                if (schoolLat != null && schoolLon != null) {
                    val isWithinGeofence = locationService.isWithinGeofence(
                        location.latitude, location.longitude,
                        schoolLat, schoolLon,
                        LocationService.DEFAULT_GEOFENCE_RADIUS
                    )
                    
                    if (!isWithinGeofence) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "អ្នកនៅក្រៅតំបន់អនុញ្ញាតសម្រាប់ចូលការងារ។ សូមចូលទៅជិតសាលារៀន (១០០ម៉ែត្រ) ដើម្បីចូលការងារ។"
                        )
                        return@launch
                    }
                }
                
                attendanceRepository.checkIn(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    photoFile = null, // TODO: Add camera functionality
                    notes = null
                ).fold(
                    onSuccess = { attendance ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            todayAttendance = attendance,
                            error = null
                        )
                        // Reload វត្ថមានសម្រាប់ថ្ងៃនេះ to ensure UI is in sync
                        loadTodayAttendance()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to check in"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to get location"
                )
            }
        }
    }

    fun checkOut() {
        viewModelScope.launch {
            if (!_uiState.value.isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "មិនទាន់បានបញ្ជាក់អត្តសញ្ញាណទេ។ សូមចូលគណនីមួយម្ដងទៀត។"
                )
                return@launch
            }
            
            if (!_uiState.value.locationEnabled) {
                _uiState.value = _uiState.value.copy(
                    error = "ដើម្បីធ្វើការចាកចេញ (Check-out) ត្រឹមត្រូវ តើអ្នកអាចអនុញ្ញាតឲ្យប្រព័ន្ធប្រើទីតាំងរបស់អ្នកបានទេ?"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current location
                val location = locationService.getCurrentLocation()
                if (location == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "មិនអាចទទួលបានទីតាំងបច្ចុប្បន្នបានទេ។ សូមពិនិត្យការភ្ជាប់ GPS ឬអនុញ្ញាតការប្រើប្រាស់ទីតាំង។"
                    )
                    return@launch
                }
                
                // Check if within geofence
                val schoolLat = _uiState.value.schoolLatitude
                val schoolLon = _uiState.value.schoolLongitude
                
                if (schoolLat != null && schoolLon != null) {
                    val isWithinGeofence = locationService.isWithinGeofence(
                        location.latitude, location.longitude,
                        schoolLat, schoolLon,
                        LocationService.DEFAULT_GEOFENCE_RADIUS
                    )
                    
                    if (!isWithinGeofence) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "អ្នកនៅក្រៅតំបន់អនុញ្ញាតសម្រាប់ចេញពីការងារ។ សូមចូលទៅជិតសាលារៀន (១០០ម៉ែត្រ) ដើម្បីចេញពីការងារ។"
                        )
                        return@launch
                    }
                }
                
                attendanceRepository.checkOut(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    photoFile = null, // TODO: Add camera functionality
                    notes = null
                ).fold(
                    onSuccess = { attendance ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            todayAttendance = attendance,
                            error = null
                        )
                        // Reload វត្ថមានសម្រាប់ថ្ងៃនេះ to ensure UI is in sync
                        loadTodayAttendance()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "ការចាកចេញបរាជ័យ។ សូមព្យាយាមម្តងទៀត។"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "មិនអាចទទួលបានទីតាំងបានទេ។ សូមពិនិត្យការភ្ជាប់ GPS និងអនុញ្ញាតឲ្យប្រើប្រាស់ទីតាំង។"
                )
            }
        }
    }

    fun updateLocationPermission(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(locationEnabled = enabled)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun startLocationUpdates() {
        locationUpdateJob?.cancel()
        locationUpdateJob = viewModelScope.launch {
            while (true) {
                try {
                    val location = locationService.getCurrentLocation()
                    _uiState.value = _uiState.value.copy(currentLocation = location)
                    
                    // Check for automatic check-in
                    checkForAutomaticCheckIn(location)
                } catch (e: Exception) {
                    // Ignore location update errors silently
                }
                delay(15000) // Update every 15 seconds to reduce UI flickering
            }
        }
    }
    
    fun stopLocationUpdates() {
        locationUpdateJob?.cancel()
        locationUpdateJob = null
    }
    
    private suspend fun checkForAutomaticCheckIn(location: android.location.Location?) {
        if (location == null) return
        
        val schoolLat = _uiState.value.schoolLatitude
        val schoolLon = _uiState.value.schoolLongitude
        
        if (schoolLat != null && schoolLon != null) {
            val isWithinGeofence = locationService.isWithinGeofence(
                location.latitude, location.longitude,
                schoolLat, schoolLon,
                LocationService.DEFAULT_GEOFENCE_RADIUS
            )
            
            // If user just entered the geofence and hasn't checked in today
            if (isWithinGeofence && wasOutsideGeofence && 
                _uiState.value.todayAttendance?.checkInTime == null &&
                _uiState.value.isAuthenticated &&
                _uiState.value.locationEnabled) {
                
                // Perform automatic check-in when user enters geofence
                performAutomaticCheckIn(location)
            }
            
            wasOutsideGeofence = !isWithinGeofence
        }
    }
    
    private suspend fun performAutomaticCheckIn(location: android.location.Location) {
        _uiState.value = _uiState.value.copy(
            isAutomaticallyCheckingIn = true,
            error = null
        )
        
        attendanceRepository.checkIn(
            latitude = location.latitude,
            longitude = location.longitude,
            photoFile = null,
            notes = "ចុះវត្តមានដោយស្វ័យប្រវត្តិ"
        ).fold(
            onSuccess = { attendance ->
                _uiState.value = _uiState.value.copy(
                    isAutomaticallyCheckingIn = false,
                    todayAttendance = attendance,
                    error = null
                )
                // Reload វត្ថមានសម្រាប់ថ្ងៃនេះ to ensure UI is in sync
                loadTodayAttendance()
            },
            onFailure = { error ->
                // Silently fail automatic check-in
                _uiState.value = _uiState.value.copy(
                    isAutomaticallyCheckingIn = false
                )
            }
        )
    }
    
    /**
     * SECURITY: Validate geofencing for users who are already checked in
     * This prevents users from staying checked in while outside the geofence
     */
    private suspend fun validateGeofencingForCheckedInUser() {
        try {
            val location = locationService.getCurrentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    error = "ការទទួលបានទីតាំងបរាជ័យ។ សូមបើកទីតាំង GPS។"
                )
                return
            }
            
            val schoolLat = _uiState.value.schoolLatitude
            val schoolLon = _uiState.value.schoolLongitude
            
            if (schoolLat != null && schoolLon != null) {
                val isWithinGeofence = locationService.isWithinGeofence(
                    location.latitude, location.longitude,
                    schoolLat, schoolLon,
                    LocationService.DEFAULT_GEOFENCE_RADIUS
                )
                
                if (!isWithinGeofence) {
                    val distance = locationService.calculateDistance(
                        location.latitude, location.longitude,
                        schoolLat, schoolLon
                    )
                    
                    // SECURITY: Clear the check-in status and force check-out
                    _uiState.value = _uiState.value.copy(
                        error = "🚨 សុវត្ថិភាព: អ្នកនៅក្រៅតំបន់សាលា (${String.format("%.1f", distance)}ម៉ែត្រ)។ សម្រាប់សុវត្ថិភាព អ្នកត្រូវចេញពីការងារ។",
                        isLoading = false,
                        todayAttendance = null,  // Clear the attendance status
                        isValidatedWithinGeofence = false  // Mark as invalid
                    )
                    
                    // Automatically check out the user for security
                    performSecurityCheckOut(location)
                } else {
                    // User is within geofence, mark as validated
                    _uiState.value = _uiState.value.copy(
                        isValidatedWithinGeofence = true
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AttendanceViewModel", "មានបញ្ហាក្នុងការផ្ទៀងផ្ទាត់ទីតាំង", e)
        }
    }
    
    /**
     * SECURITY: Automatically check out user when they leave the geofence
     */
    private suspend fun performSecurityCheckOut(location: android.location.Location) {
        android.util.Log.i("AttendanceViewModel", "កំពុងអនុវត្តការចាកចេញវត្ថមានសម្រាប់អ្នកប្រើដែលនៅខាងក្រៅដែនទីតាំង")
        
        attendanceRepository.checkOut(
            latitude = location.latitude,
            longitude = location.longitude,
            photoFile = null,
            notes = "អ្នកកំពុងនៅខាងក្រៅដែនទីតាំង (Geofence) ដែលបានកំណត់ សម្រាប់ការចាកចេញ"
        ).fold(
            onSuccess = { attendance ->
                android.util.Log.i("AttendanceViewModel", "ការចាកចេញវត្ថមានបានជោគជ័យ")
                _uiState.value = _uiState.value.copy(
                    todayAttendance = null,  // Clear attendance since user is outside geofence
                    error = "🚨 វត្ថមាន: អ្នកត្រូវបានចេញពីការងារដោយស្វ័យប្រវត្តិ ពីព្រោះអ្នកនៅក្រៅតំបន់សាលា។",
                    isValidatedWithinGeofence = false  // Mark as outside geofence
                )
                // IMPORTANT: Reload today's attendance to ensure UI is in sync
                loadTodayAttendance()
            },
            onFailure = { error ->
                android.util.Log.e("AttendanceViewModel", "ការចាកចេញវត្ថមានបានបរាជ័យ: ${error.message}")
                _uiState.value = _uiState.value.copy(
                    todayAttendance = null,  // Clear attendance since user is outside geofence
                    error = "🚨 កំហុសសុវត្ថិភាព: ${error.message}",
                    isValidatedWithinGeofence = false  // Mark as outside geofence
                )
                // IMPORTANT: Even if check-out fails, reload to get current state
                loadTodayAttendance()
            }
        )
    }
}

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val todayAttendance: Attendance? = null,
    val error: String? = null,
    val locationEnabled: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: com.plp.attendance.domain.model.User? = null,
    val schoolName: String? = null,
    val schoolLatitude: Double? = null,
    val schoolLongitude: Double? = null,
    val currentLocation: Location? = null,
    val isValidatedWithinGeofence: Boolean = true,  // Default to true, will be set to false if user is outside geofence
    val isAutomaticallyCheckingIn: Boolean = false  // Track if app is trying to check in automatically
)