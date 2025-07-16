package com.plp.attendance.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.R
import com.plp.attendance.utils.NetworkConnectivityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NetworkStatusViewModel @Inject constructor(
    networkConnectivityManager: NetworkConnectivityManager
) : ViewModel() {
    val isNetworkAvailable = networkConnectivityManager.isNetworkAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
}

@Composable
fun NetworkStatusIndicator(
    modifier: Modifier = Modifier,
    viewModel: NetworkStatusViewModel = hiltViewModel()
) {
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    
    AnimatedVisibility(
        visible = true, // Always visible to show status
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isNetworkAvailable) 
                    Color(0xFFE8F5E9) // Light green background
                else 
                    Color(0xFFFFF3E0) // Light amber background
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isNetworkAvailable) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isNetworkAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isNetworkAvailable) 
                        stringResource(R.string.online)
                    else 
                        stringResource(R.string.offline),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isNetworkAvailable) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
fun CompactNetworkIndicator(
    modifier: Modifier = Modifier,
    viewModel: NetworkStatusViewModel = hiltViewModel()
) {
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    
    Box(
        modifier = modifier
            .size(8.dp)
            .background(
                color = if (isNetworkAvailable) Color(0xFF4CAF50) else Color(0xFFFF5252),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}