package com.plp.attendance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plp.attendance.services.BiometricAuthManager
import com.plp.attendance.services.BiometricStatus
import com.plp.attendance.utils.Result
import kotlinx.coroutines.launch

@Composable
fun BiometricAuthDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    title: String = "Biometric Authentication",
    subtitle: String = "Use your biometric to authenticate",
    biometricAuthManager: BiometricAuthManager,
    activity: FragmentActivity
) {
    val scope = rememberCoroutineScope()
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    
                    authError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isAuthenticating = true
                            authError = null
                            
                            when (val result = biometricAuthManager.authenticateForSensitiveAction(
                                activity = activity,
                                title = title,
                                subtitle = subtitle
                            )) {
                                is Result.Success -> {
                                    isAuthenticating = false
                                    onSuccess()
                                }
                                is Result.Error -> {
                                    isAuthenticating = false
                                    authError = result.message
                                    onError(result.message)
                                }
                                is Result.Loading -> {
                                    // Handle loading state
                                }
                            }
                        }
                    },
                    enabled = !isAuthenticating
                ) {
                    Text(if (isAuthenticating) "Authenticating..." else "Authenticate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isAuthenticating
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BiometricStatusCard(
    biometricAuthManager: BiometricAuthManager,
    modifier: Modifier = Modifier
) {
    val biometricStatus = remember { biometricAuthManager.isBiometricAvailable() }
    val statusMessage = remember { biometricAuthManager.getStatusMessage(biometricStatus) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (biometricStatus) {
                BiometricStatus.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer
                BiometricStatus.NONE_ENROLLED -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Status",
                    tint = when (biometricStatus) {
                        BiometricStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                        BiometricStatus.NONE_ENROLLED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                
                Text(
                    text = "Biometric Authentication",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = when (biometricStatus) {
                    BiometricStatus.AVAILABLE -> MaterialTheme.colorScheme.onPrimaryContainer
                    BiometricStatus.NONE_ENROLLED -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

@Composable
fun BiometricToggleCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isAvailable: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                enabled = isAvailable
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricTimeoutSelector(
    selectedTimeout: Int,
    onTimeoutChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeoutOptions = listOf(1, 5, 15, 30, 60)
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Re-authentication Timeout",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "How long before requiring biometric authentication again",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = "${selectedTimeout} minutes",
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    timeoutOptions.forEach { timeout ->
                        DropdownMenuItem(
                            text = { Text("${timeout} minutes") },
                            onClick = {
                                onTimeoutChange(timeout)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}