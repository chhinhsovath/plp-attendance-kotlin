package com.plp.attendance.ui.management

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.plp.attendance.R
import com.plp.attendance.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    userId: String,
    navController: NavController,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Load user data
    LaunchedEffect(userId) {
        viewModel.loadUserById(userId)
    }
    
    // Form state - initialized with existing user data
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var organizationId by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var showRoleDropdown by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }
    
    // Update form when user data is loaded
    LaunchedEffect(uiState.selectedUser) {
        uiState.selectedUser?.let { user ->
            // Parse name into first and last name
            val nameParts = user.name.split(" ", limit = 2)
            email = user.email
            username = user.email.substringBefore("@") // Assuming username from email
            firstName = nameParts.getOrNull(0) ?: ""
            lastName = nameParts.getOrNull(1) ?: ""
            phoneNumber = user.phoneNumber ?: ""
            selectedRole = user.role
            organizationId = user.departmentId ?: ""
            isActive = user.isActive
        }
    }
    
    // Track changes
    LaunchedEffect(email, username, firstName, lastName, phoneNumber, selectedRole, organizationId, isActive) {
        uiState.selectedUser?.let { user ->
            val nameParts = user.name.split(" ", limit = 2)
            hasChanges = email != user.email ||
                    username != user.email.substringBefore("@") ||
                    firstName != (nameParts.getOrNull(0) ?: "") ||
                    lastName != (nameParts.getOrNull(1) ?: "") ||
                    phoneNumber != (user.phoneNumber ?: "") ||
                    selectedRole != user.role ||
                    organizationId != (user.departmentId ?: "") ||
                    isActive != user.isActive
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_user)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoadingDetail -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.detailError != null -> {
                ErrorContent(
                    message = uiState.detailError!!,
                    onRetry = { viewModel.loadUserById(userId) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.selectedUser != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Personal Information Section
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Divider()
                    
                    // Account Information Section
                    Text(
                        text = "Account Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                        supportingText = {
                            if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                Text("Invalid email format")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Divider()
                    
                    // Role & Organization Section
                    Text(
                        text = "Role & Organization",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Role Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showRoleDropdown,
                        onExpandedChange = { showRoleDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRole?.displayName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("User Role") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRoleDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showRoleDropdown,
                            onDismissRequest = { showRoleDropdown = false }
                        ) {
                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.displayName) },
                                    onClick = {
                                        selectedRole = role
                                        showRoleDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = organizationId,
                        onValueChange = { organizationId = it },
                        label = { Text("Organization ID") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Account Status
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Account Status",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (isActive) "User can access the system" else "User cannot access the system",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                viewModel.updateUser(
                                    userId = userId,
                                    email = email.trim(),
                                    username = username.trim(),
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    role = selectedRole?.name,
                                    phoneNumber = phoneNumber.trim().takeIf { it.isNotBlank() },
                                    organizationId = organizationId.trim().takeIf { it.isNotBlank() },
                                    isActive = isActive
                                )
                            },
                            enabled = hasChanges && !uiState.isUpdating && 
                                     email.isNotBlank() && 
                                     android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save Changes")
                            }
                        }
                    }
                    
                    // Error display
                    uiState.updateError?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Handle success
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            viewModel.clearSuccessFlags()
            navController.navigateUp()
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}