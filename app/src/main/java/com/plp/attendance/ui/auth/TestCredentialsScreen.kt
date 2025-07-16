package com.plp.attendance.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TestCredential(
    val username: String,
    val password: String,
    val role: String,
    val email: String,
    val description: String,
    val khmerName: String,
    val position: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCredentialsScreen(
    onBackClick: () -> Unit,
    onCredentialSelected: (String, String) -> Unit,
    onDirectLogin: (String, String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    val testCredentials = listOf(
        // Administrator
        TestCredential(
            username = "admin_national",
            password = "password",
            role = "Administrator",
            email = "admin@plp.gov.kh",
            description = "National Administrator - Full system access",
            khmerName = "សុខ សុភាព (Sok Sopheap)",
            position = "Administrator"
        ),
        
        // Zone Managers
        TestCredential(
            username = "zone_north",
            password = "password",
            role = "Zone Manager",
            email = "zone.north@plp.gov.kh",
            description = "Zone Manager - Northern Provinces",
            khmerName = "ចាន់ ដារ៉ា (Chan Dara)",
            position = "Zone Manager"
        ),
        TestCredential(
            username = "zone_south",
            password = "password",
            role = "Zone Manager",
            email = "zone.south@plp.gov.kh",
            description = "Zone Manager - Southern Provinces",
            khmerName = "លី សុខា (Ly Sokha)",
            position = "Zone Manager"
        ),
        
        // Provincial Managers
        TestCredential(
            username = "provincial_pp",
            password = "password",
            role = "Provincial Manager",
            email = "provincial.pp@plp.gov.kh",
            description = "Provincial Manager - Phnom Penh",
            khmerName = "ហេង សម៉ាត (Heng Samat)",
            position = "Provincial Manager"
        ),
        TestCredential(
            username = "provincial_sr",
            password = "password",
            role = "Provincial Manager",
            email = "provincial.sr@plp.gov.kh",
            description = "Provincial Manager - Siem Reap",
            khmerName = "ពៅ វិសាល (Pov Visal)",
            position = "Provincial Manager"
        ),
        
        // Department Managers
        TestCredential(
            username = "department_pp",
            password = "password",
            role = "Department Manager",
            email = "department.pp@plp.gov.kh",
            description = "Department Manager - Education Phnom Penh",
            khmerName = "មាស សុផល (Meas Sophal)",
            position = "Department Manager"
        ),
        TestCredential(
            username = "department_sr",
            password = "password",
            role = "Department Manager",
            email = "department.sr@plp.gov.kh",
            description = "Department Manager - Education Siem Reap",
            khmerName = "សុខ ពិសិទ្ធ (Sok Piseth)",
            position = "Department Manager"
        ),
        
        // Cluster Heads
        TestCredential(
            username = "cluster_pp01",
            password = "password",
            role = "Cluster Head",
            email = "cluster.pp01@plp.gov.kh",
            description = "Cluster Head - Phnom Penh District 1",
            khmerName = "រស់ បុប្ផា (Ros Bopha)",
            position = "Cluster Head"
        ),
        TestCredential(
            username = "cluster_sr01",
            password = "password",
            role = "Cluster Head",
            email = "cluster.sr01@plp.gov.kh",
            description = "Cluster Head - Siem Reap District 1",
            khmerName = "នួន សុខលី (Nuon Sokhly)",
            position = "Cluster Head"
        ),
        
        // Directors
        TestCredential(
            username = "director_pp001",
            password = "password",
            role = "Director",
            email = "director.pp001@plp.gov.kh",
            description = "School Director - PP Primary School 001",
            khmerName = "ខៀវ សំណាង (Khiev Samnang)",
            position = "Director"
        ),
        TestCredential(
            username = "director_pp002",
            password = "password",
            role = "Director",
            email = "director.pp002@plp.gov.kh",
            description = "School Director - PP Primary School 002",
            khmerName = "ទេព មករា (Tep Makara)",
            position = "Director"
        ),
        TestCredential(
            username = "director_sr001",
            password = "password",
            role = "Director",
            email = "director.sr001@plp.gov.kh",
            description = "School Director - SR Primary School 001",
            khmerName = "សាន់ វណ្ណា (San Vanna)",
            position = "Director"
        ),
        
        // Teachers
        TestCredential(
            username = "teacher_pp001",
            password = "password",
            role = "Teacher",
            email = "teacher.pp001@plp.gov.kh",
            description = "Teacher - PP Primary School 001",
            khmerName = "លឹម សុភាព (Lim Sopheap)",
            position = "Teacher"
        ),
        TestCredential(
            username = "teacher_pp002",
            password = "password",
            role = "Teacher",
            email = "teacher.pp002@plp.gov.kh",
            description = "Teacher - PP Primary School 001",
            khmerName = "ឈួន លីដា (Chhoun Lida)",
            position = "Teacher"
        ),
        TestCredential(
            username = "teacher_pp003",
            password = "password",
            role = "Teacher",
            email = "teacher.pp003@plp.gov.kh",
            description = "Teacher - PP Primary School 002",
            khmerName = "គឹម សុខហេង (Kim Sokhheng)",
            position = "Teacher"
        ),
        TestCredential(
            username = "teacher_sr001",
            password = "password",
            role = "Teacher",
            email = "teacher.sr001@plp.gov.kh",
            description = "Teacher - SR Primary School 001",
            khmerName = "យិន សុវណ្ណាវ (Yin Sovannarv)",
            position = "Teacher"
        )
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Credentials") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Test Database Credentials",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "These credentials are from the API server database. Click on any credential to auto-fill the login form.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            val groupedCredentials = testCredentials.groupBy { it.position }
            val orderedPositions = listOf("Administrator", "Zone Manager", "Provincial Manager", "Department Manager", "Cluster Head", "Director", "Teacher")
            
            orderedPositions.forEach { position ->
                val credentials = groupedCredentials[position] ?: emptyList()
                if (credentials.isNotEmpty()) {
                    item {
                        Text(
                            text = position,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(credentials) { credential ->
                        TestCredentialCard(
                            credential = credential,
                            onCredentialClick = { onDirectLogin(credential.username, credential.password) },
                            onCopyClick = { text ->
                                clipboardManager.setText(AnnotatedString(text))
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
fun TestCredentialCard(
    credential: TestCredential,
    onCredentialClick: () -> Unit,
    onCopyClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onCredentialClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = credential.khmerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = credential.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = when (credential.role) {
                        "Administrator" -> Color(0xFFDC2626)
                        "Zone Manager" -> Color(0xFFEA580C)
                        "Provincial Manager" -> Color(0xFF059669)
                        "Department Manager" -> Color(0xFF0284C7)
                        "Cluster Head" -> Color(0xFF7C3AED)
                        "Director" -> Color(0xFFDB2777)
                        "Teacher" -> Color(0xFF16A34A)
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = credential.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Username: ${credential.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Password: ${credential.password}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Email: ${credential.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column {
                    IconButton(
                        onClick = { onCopyClick(credential.username) }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Username",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Click to login instantly",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}