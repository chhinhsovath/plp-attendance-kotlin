package com.plp.attendance.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.plp.attendance.ui.navigation.Screen
import com.plp.attendance.ui.components.LocalizedText
import com.plp.attendance.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.plp.attendance.data.preferences.DeveloperPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import android.content.Context

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val developerPreferences = remember { DeveloperPreferences(context) }
    var isDeveloperModeEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        developerPreferences.isDeveloperModeEnabled.collect { enabled ->
            isDeveloperModeEnabled = enabled
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { LocalizedText(textRes = R.string.settings) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Profile Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ListItem(
                    headlineContent = { LocalizedText(textRes = R.string.profile) },
                    supportingContent = { Text(stringResource(R.string.view_and_edit_profile)) },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }
            
            // Settings Options
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Language Settings
                ListItem(
                    headlineContent = { LocalizedText(textRes = R.string.language) },
                    supportingContent = { Text(stringResource(R.string.change_app_language)) },
                    leadingContent = {
                        Icon(Icons.Default.Language, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.Language.route)
                    }
                )
                
                Divider()
                
                // Notification Settings
                ListItem(
                    headlineContent = { LocalizedText(textRes = R.string.notifications) },
                    supportingContent = { Text(stringResource(R.string.manage_notification_preferences)) },
                    leadingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
                
                Divider()
                
                // Biometric Settings
                ListItem(
                    headlineContent = { Text(stringResource(R.string.biometric_settings)) },
                    supportingContent = { Text(stringResource(R.string.configure_biometric_auth)) },
                    leadingContent = {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        // Navigate to biometric settings
                    }
                )
                
                if (isDeveloperModeEnabled) {
                    Divider()
                    
                    // Developer Settings
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.developer_settings)) },
                        supportingContent = { Text(stringResource(R.string.advanced_testing_options)) },
                        leadingContent = {
                            Icon(Icons.Default.Code, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.DeveloperSettings.route)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                LocalizedText(textRes = R.string.logout)
            }
            
            // Version Info
            Text(
                text = stringResource(R.string.version_format, "1.0.0"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }
    }
}