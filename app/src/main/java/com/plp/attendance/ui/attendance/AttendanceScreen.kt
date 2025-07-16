package com.plp.attendance.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.plp.attendance.ui.components.LocationPermissionHandler
import com.plp.attendance.ui.components.LocationStatus
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.plp.attendance.R
import android.location.Location
import androidx.compose.ui.graphics.Color as ComposeColor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onLogout: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Don't load attendance data immediately, wait for location permission
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationUpdates()
        }
    }

    LocationPermissionHandler(
        onPermissionGranted = { 
            viewModel.updateLocationPermission(true)
            // Load attendance data and start location updates after permission granted
            viewModel.loadTodayAttendance()
            viewModel.startLocationUpdates()
        },
        onPermissionDenied = { 
            viewModel.updateLocationPermission(false)
        }
    ) {
        AttendanceContent(
            uiState = uiState,
            onCheckIn = { viewModel.checkIn() },
            onCheckOut = { viewModel.checkOut() },
            onLogout = onLogout
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceContent(
    uiState: AttendanceUiState,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar with user info and logout
        TopAppBar(
            title = { 
                Column {
                    Text(
                        text = uiState.currentUser?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.schoolName ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.currentUser?.role != null) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getKhmerRoleName(uiState.currentUser.role.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            actions = {
                if (!uiState.locationEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.location_required),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = stringResource(R.string.logout))
                }
            }
        )
        
        // Google Map showing school location (1/3 of screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.33f)
        ) {
            SchoolLocationMap(
                schoolLatitude = uiState.schoolLatitude,
                schoolLongitude = uiState.schoolLongitude,
                schoolName = uiState.schoolName,
                currentLocation = uiState.currentLocation
            )
        }
        
        // Rest of the content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        
        // Date and time display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getKhmerDateString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = getKhmerTimeString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Attendance status - Only show if validated within geofence
        if (uiState.isValidatedWithinGeofence && uiState.todayAttendance != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.todays_status),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    uiState.todayAttendance?.checkOutTime != null -> stringResource(R.string.checked_out)
                                    uiState.todayAttendance?.checkInTime != null -> stringResource(R.string.checked_in)
                                    else -> stringResource(R.string.absent)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    uiState.todayAttendance?.checkOutTime != null -> Color.Blue
                                    uiState.todayAttendance?.checkInTime != null -> Color.Green
                                    else -> Color.Gray
                                }
                            )
                        }
                        
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.attendance_status_icon),
                            tint = when {
                                uiState.todayAttendance?.checkOutTime != null -> Color.Blue
                                uiState.todayAttendance?.checkInTime != null -> Color.Green
                                else -> Color.Gray
                            }
                        )
                    }
                    
                    if (uiState.todayAttendance?.checkInTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.check_in)}: ${convertToKhmerNumbers(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(uiState.todayAttendance!!.checkInTime)))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (uiState.todayAttendance?.checkOutTime != null) {
                        Text(
                            text = "${stringResource(R.string.check_out)}: ${convertToKhmerNumbers(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(uiState.todayAttendance!!.checkOutTime!!)))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // Action buttons - Always show if user is authenticated and location is enabled
        // Show loading state within buttons to prevent flickering
        if (uiState.isAuthenticated && uiState.locationEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Case 1: User has successfully checked in and is within geofence
                // Show only Check Out button
                if (uiState.todayAttendance?.checkInTime != null && 
                    uiState.todayAttendance?.checkOutTime == null &&
                    uiState.isValidatedWithinGeofence) {
                    
                    Button(
                        onClick = onCheckOut,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isLoading) {
                                stringResource(R.string.loading)
                            } else {
                                stringResource(R.string.check_out)
                            }
                        )
                    }
                }
                // Case 2: User has checked out - show both buttons disabled
                else if (uiState.todayAttendance?.checkInTime != null && 
                         uiState.todayAttendance?.checkOutTime != null) {
                    
                    Button(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.check_in))
                    }
                    
                    Button(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.check_out))
                    }
                }
                // Case 3: Default - User not checked in OR automatic check-in failed
                // Show only Check In button
                else {
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && !uiState.isAutomaticallyCheckingIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isLoading && !uiState.isAutomaticallyCheckingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                uiState.isAutomaticallyCheckingIn -> stringResource(R.string.automatic_check_in_trying)
                                uiState.isLoading -> stringResource(R.string.loading)
                                else -> stringResource(R.string.check_in)
                            }
                        )
                    }
                }
            }
        }
        
        // Authentication/Error status card - Single card for all errors
        if (!uiState.isAuthenticated || uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8D7DA) // Light pink error background
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Exclamation icon in circle background
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color(0xFF842029), // Dark red for icon background
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = stringResource(R.string.error_icon),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    !uiState.isAuthenticated -> Icons.Default.Person
                                    uiState.error?.contains("តំបន់", ignoreCase = true) == true -> Icons.Default.LocationOn
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = Color(0xFFDC3545), // Red color for icon
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    !uiState.isAuthenticated -> stringResource(R.string.not_authenticated)
                                    uiState.error != null -> uiState.error
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF842029) // Dark red text
                            )
                        }
                        
                        // Add helpful hint for location errors
                        if (uiState.error != null && (isOutsideSchoolAreaError(uiState.error) || 
                            uiState.error.contains("អ្នកនៅក្រៅតំបន់", ignoreCase = true))) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.move_closer_hint),
                                color = Color(0xFF842029).copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        }
    }
}

@Composable
fun SchoolLocationMap(
    schoolLatitude: Double?,
    schoolLongitude: Double?,
    schoolName: String?,
    currentLocation: Location? = null
) {
    val defaultLocation = LatLng(11.551481374849613, 104.92816726562374) // Real school location
    val schoolLocation = if (schoolLatitude != null && schoolLongitude != null) {
        LatLng(schoolLatitude, schoolLongitude)
    } else {
        defaultLocation
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(schoolLocation, 15f)
    }
    
    // Function to move camera to current location
    val moveToCurrentLocation: () -> Unit = {
        currentLocation?.let { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            cameraPositionState.move(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f)
            )
        }
    }
    
    // Function to center both school and current location with proper bounds
    val centerBothLocations: () -> Unit = {
        currentLocation?.let { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            val boundsBuilder = LatLngBounds.builder()
            boundsBuilder.include(schoolLocation)
            boundsBuilder.include(currentLatLng)
            
            val bounds = boundsBuilder.build()
            val padding = 200 // Padding from edges in pixels
            
            cameraPositionState.move(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, padding)
            )
        }
    }
    
    // Auto-center both locations when current location is available
    LaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            centerBothLocations()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false,
            mapType = MapType.NORMAL
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = false
        )
    ) {
        // Show school location with label
        if (schoolLatitude != null && schoolLongitude != null) {
            val schoolMarkerState = rememberMarkerState(position = schoolLocation)
            
            // Force show info window
            LaunchedEffect(schoolMarkerState) {
                schoolMarkerState.showInfoWindow()
            }
            
            MarkerInfoWindow(
                state = schoolMarkerState,
                title = schoolName ?: stringResource(R.string.school),
                snippet = stringResource(R.string.school_khmer)
            ) { marker ->
                // Custom info window content
                Card(
                    modifier = Modifier,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = schoolName ?: stringResource(R.string.school_khmer),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Text(
                            text = stringResource(R.string.school_khmer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Add a circle to show geofence area (100m radius)
            Circle(
                center = schoolLocation,
                radius = 100.0, // 100 meters
                fillColor = Color(0x220000FF),
                strokeColor = Color.Blue,
                strokeWidth = 2f
            )
        }
        
        // Show current location with label
        currentLocation?.let { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            val currentMarkerState = rememberMarkerState(position = currentLatLng)
            
            // Force show info window
            LaunchedEffect(currentMarkerState) {
                currentMarkerState.showInfoWindow()
            }
            
            MarkerInfoWindow(
                state = currentMarkerState,
                title = stringResource(R.string.current_location_khmer),
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                snippet = stringResource(R.string.your_location)
            ) { marker ->
                // Custom info window content
                Card(
                    modifier = Modifier,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.current_location_khmer),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.your_location),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        }
        
        // Center both locations button - positioned at top to avoid zoom controls
        if (currentLocation != null) {
            FloatingActionButton(
                onClick = centerBothLocations,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp), // 50% smaller than default (80dp -> 40dp)
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = "Center both locations",
                    modifier = Modifier.size(20.dp) // Smaller icon to match button size
                )
            }
        }
    }
}

@Composable
fun getKhmerDateString(): String {
    val khmerMonths = listOf(
        "មករា", "កុម្ភៈ", "មីនា", "មេសា", "ឧសភា", "មិថុនា",
        "កក្កដា", "សីហា", "កញ្ញា", "តុលា", "វិច្ឆិកា", "ធ្នូ"
    )
    
    val khmerDays = listOf(
        "អាទិត្យ", "ច័ន្ទ", "អង្គារ", "ពុធ", "ព្រហស្បតិ៍", "សុក្រ", "សៅរ៍"
    )
    
    val calendar = Calendar.getInstance()
    val dayOfWeek = khmerDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = khmerMonths[calendar.get(Calendar.MONTH)]
    val year = calendar.get(Calendar.YEAR)
    
    return "$dayOfWeek, ${convertToKhmerNumbers(day.toString())} $month ${convertToKhmerNumbers(year.toString())}"
}

@Composable
fun getKhmerTimeString(): String {
    val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
    return convertToKhmerNumbers(time)
}

fun calculateDistanceToSchool(
    currentLat: Double, currentLon: Double,
    schoolLat: Double, schoolLon: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(currentLat, currentLon, schoolLat, schoolLon, results)
    return results[0]
}

fun convertToKhmerNumbers(input: String): String {
    val khmerNumbers = mapOf(
        '0' to '០', '1' to '១', '2' to '២', '3' to '៣',
        '4' to '៤', '5' to '៥', '6' to '៦', '7' to '៧',
        '8' to '៨', '9' to '៩'
    )
    
    return input.map { char ->
        khmerNumbers[char] ?: char
    }.joinToString("")
        .replace("AM", "ព្រឹក") // ព្រឹក (morning)
        .replace("PM", "ល្ងាច") // ល្ងាច (evening)
}

@Composable
fun getKhmerRoleName(role: String): String {
    return when (role.lowercase()) {
        "administrator" -> stringResource(R.string.administrator)
        "zone" -> stringResource(R.string.zone_manager)
        "provincial" -> stringResource(R.string.provincial_manager)
        "department" -> stringResource(R.string.department_manager)
        "cluster_head" -> stringResource(R.string.cluster_head)
        "director" -> stringResource(R.string.director)
        "teacher" -> stringResource(R.string.teacher)
        else -> role
    }
}

@Composable
fun isOutsideSchoolAreaError(error: String): Boolean {
    return error.contains(stringResource(R.string.outside_school_area), ignoreCase = true)
}

@Composable
fun isAlreadyCheckedError(error: String): Boolean {
    return error.contains(stringResource(R.string.already_checked), ignoreCase = true)
}
