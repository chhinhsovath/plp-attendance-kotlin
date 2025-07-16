@file:OptIn(ExperimentalMaterial3Api::class)

package com.plp.attendance.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.plp.attendance.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)
    
    // Predefined test users
    val testUsers = listOf(
        TestCredential(
            username = "admin_national",
            password = "password123",
            role = "Administrator",
            email = "admin@plp.gov.kh",
            description = "National Administrator - Full system access",
            khmerName = "សុខ សុភាព (Sok Sopheap)",
            position = "Administrator"
        ),
        TestCredential(
            username = "zone_north",
            password = "password123",
            role = "Zone Manager",
            email = "zone.north@plp.gov.kh",
            description = "Zone Manager - Northern Provinces",
            khmerName = "ចាន់ ដារ៉ា (Chan Dara)",
            position = "Zone Manager"
        ),
        TestCredential(
            username = "zone_south",
            password = "password123",
            role = "Zone Manager",
            email = "zone.south@plp.gov.kh",
            description = "Zone Manager - Southern Provinces",
            khmerName = "លី សុខា (Ly Sokha)",
            position = "Zone Manager"
        ),
        TestCredential(
            username = "provincial_pp",
            password = "password123",
            role = "Provincial Manager",
            email = "provincial.pp@plp.gov.kh",
            description = "Provincial Manager - Phnom Penh",
            khmerName = "ហេង សម៉ាត (Heng Samat)",
            position = "Provincial Manager"
        ),
        TestCredential(
            username = "provincial_sr",
            password = "password123",
            role = "Provincial Manager",
            email = "provincial.sr@plp.gov.kh",
            description = "Provincial Manager - Siem Reap",
            khmerName = "ពៅ វិសាល (Pov Visal)",
            position = "Provincial Manager"
        ),
        TestCredential(
            username = "department_pp",
            password = "password123",
            role = "Department Manager",
            email = "department.pp@plp.gov.kh",
            description = "Department Manager - Education Phnom Penh",
            khmerName = "មាស សុផល (Meas Sophal)",
            position = "Department Manager"
        ),
        TestCredential(
            username = "department_sr",
            password = "password123",
            role = "Department Manager",
            email = "department.sr@plp.gov.kh",
            description = "Department Manager - Education Siem Reap",
            khmerName = "សុខ ពិសិទ្ធ (Sok Piseth)",
            position = "Department Manager"
        ),
        TestCredential(
            username = "cluster_pp01",
            password = "password123",
            role = "Cluster Head",
            email = "cluster.pp01@plp.gov.kh",
            description = "Cluster Head - Phnom Penh District 1",
            khmerName = "រស់ បុប្ផា (Ros Bopha)",
            position = "Cluster Head"
        ),
        TestCredential(
            username = "cluster_sr01",
            password = "password123",
            role = "Cluster Head",
            email = "cluster.sr01@plp.gov.kh",
            description = "Cluster Head - Siem Reap District 1",
            khmerName = "នួន សុខលី (Nuon Sokhly)",
            position = "Cluster Head"
        ),
        TestCredential(
            username = "director_pp001",
            password = "password123",
            role = "Director",
            email = "director.pp001@plp.gov.kh",
            description = "School Director - PP Primary School 001",
            khmerName = "ខៀវ សំណាង (Khiev Samnang)",
            position = "Director"
        ),
        TestCredential(
            username = "director_pp002",
            password = "password123",
            role = "Director",
            email = "director.pp002@plp.gov.kh",
            description = "School Director - PP Primary School 002",
            khmerName = "ទេព មករា (Tep Makara)",
            position = "Director"
        ),
        TestCredential(
            username = "director_sr001",
            password = "password123",
            role = "Director",
            email = "director.sr001@plp.gov.kh",
            description = "School Director - SR Primary School 001",
            khmerName = "សាន់ វណ្ណា (San Vanna)",
            position = "Director"
        ),
        TestCredential(
            username = "teacher_pp001",
            password = "password123",
            role = "Teacher",
            email = "teacher.pp001@plp.gov.kh",
            description = "Teacher - PP Primary School 001",
            khmerName = "លឹម សុភាព (Lim Sopheap)",
            position = "Teacher"
        ),
        TestCredential(
            username = "teacher_pp002",
            password = "password123",
            role = "Teacher",
            email = "teacher.pp002@plp.gov.kh",
            description = "Teacher - PP Primary School 001",
            khmerName = "ឈួន លីដា (Chhoun Lida)",
            position = "Teacher"
        ),
        TestCredential(
            username = "teacher_pp003",
            password = "password123",
            role = "Teacher",
            email = "teacher.pp003@plp.gov.kh",
            description = "Teacher - PP Primary School 002",
            khmerName = "គឹម សុខហេង (Kim Sokhheng)",
            position = "Teacher"
        ),
        TestCredential(
            username = "teacher_sr001",
            password = "password123",
            role = "Teacher",
            email = "teacher.sr001@plp.gov.kh",
            description = "Teacher - SR Primary School 001",
            khmerName = "យិន សុវណ្ណាវ (Yin Sovannarv)",
            position = "Teacher"
        ),
        TestCredential(
            username = "plp_office",
            password = "password123",
            role = "Administrator",
            email = "plp@plp.gov.kh",
            description = "PLP Office Administrator - Central Office",
            khmerName = "PLP Administrator",
            position = "Administrator"
        )
    )
    
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }
    
    LaunchedEffect(uiState.user) {
        if (uiState.user != null) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.select_user_role_login),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (uiState.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Error message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            }
        }
        
        // User roles list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val groupedCredentials = testUsers.groupBy { it.position }
            val orderedPositions = listOf(
                "Administrator", 
                "Zone Manager", 
                "Provincial Manager", 
                "Department Manager", 
                "Cluster Head", 
                "Director", 
                "Teacher"
            )
            
            orderedPositions.forEach { position ->
                val credentials = groupedCredentials[position] ?: emptyList()
                if (credentials.isNotEmpty()) {
                    item {
                        Text(
                            text = position,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    items(credentials) { credential ->
                        UserRoleCard(
                            credential = credential,
                            isLoading = uiState.isLoading,
                            onClick = {
                                viewModel.directLogin(credential.email, credential.password)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserRoleCard(
    credential: TestCredential,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!isLoading) onClick() },
        enabled = !isLoading,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = when (credential.role) {
                    "Administrator" -> Color(0xFFDC2626)
                    "Zone Manager" -> Color(0xFFEA580C)
                    "Provincial Manager" -> Color(0xFF059669)
                    "Department Manager" -> Color(0xFF0284C7)
                    "Cluster Head" -> Color(0xFF7C3AED)
                    "Director" -> Color(0xFFDB2777)
                    "Teacher" -> Color(0xFF16A34A)
                    else -> MaterialTheme.colorScheme.secondary
                }
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }
            
            // User info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = credential.khmerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = credential.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = credential.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Arrow indicator
            if (!isLoading) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}