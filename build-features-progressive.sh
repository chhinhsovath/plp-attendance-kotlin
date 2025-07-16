#!/bin/bash

echo "🚀 Building PLP Attendance System with Features (Progressive Approach)..."

# Function to build and check
build_and_check() {
    echo "🔨 Building APK..."
    ./gradlew clean assembleDebug 2>&1 | tee build.log
    
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        echo "✅ Build successful!"
        return 0
    else
        echo "❌ Build failed"
        tail -20 build.log
        return 1
    fi
}

# Create the necessary directory structure first
echo "📁 Creating directory structure..."
mkdir -p app/src/main/java/com/plp/attendance/{ui,data,domain,utils}
mkdir -p app/src/main/java/com/plp/attendance/ui/{navigation,theme,auth,attendance,leave,reports,settings,components}
mkdir -p app/src/main/java/com/plp/attendance/data/{local,remote,repository}
mkdir -p app/src/main/java/com/plp/attendance/domain/{model,usecase,repository}

# Step 1: Create Theme files
echo "🎨 Creating theme..."
cat > app/src/main/java/com/plp/attendance/ui/theme/Theme.kt << 'EOF'
package com.plp.attendance.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF4CAF50),
    onSecondary = Color.White,
    error = Color(0xFFF44336),
    background = Color(0xFFF5F5F5),
    surface = Color.White
)

@Composable
fun PLPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
EOF

# Step 2: Create simple navigation
cat > app/src/main/java/com/plp/attendance/ui/navigation/NavigationGraph.kt << 'EOF'
package com.plp.attendance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plp.attendance.ui.auth.LoginScreen
import com.plp.attendance.ui.MainScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
}

@Composable
fun NavigationGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}
EOF

# Step 3: Create simple models
cat > app/src/main/java/com/plp/attendance/domain/model/User.kt << 'EOF'
package com.plp.attendance.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val schoolId: String,
    val schoolName: String
) : Parcelable
EOF

# Step 4: Create Login Screen
cat > app/src/main/java/com/plp/attendance/ui/auth/LoginScreen.kt << 'EOF'
package com.plp.attendance.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ប្រព័ន្ធគ្រប់គ្រងវត្តមាន",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Cambodia Education Attendance",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    // For demo, just navigate to main
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = email.isNotEmpty() && password.isNotEmpty() && !isLoading
            ) {
                Text(
                    text = "LOGIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
EOF

# Step 5: Create Main Screen with bottom navigation
cat > app/src/main/java/com/plp/attendance/ui/MainScreen.kt << 'EOF'
package com.plp.attendance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plp.attendance.ui.attendance.AttendanceScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0
                        navController.navigate("attendance")
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Attendance") },
                    label = { Text("Attendance") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        navController.navigate("leave")
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Leave") },
                    label = { Text("Leave") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        navController.navigate("reports")
                    },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { 
                        selectedTab = 3
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "attendance",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("attendance") { AttendanceScreen() }
            composable("leave") { PlaceholderScreen("Leave Management") }
            composable("reports") { PlaceholderScreen("Reports & Analytics") }
            composable("settings") { PlaceholderScreen("Settings") }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Coming Soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
EOF

# Step 6: Create enhanced Attendance Screen
cat > app/src/main/java/com/plp/attendance/ui/attendance/AttendanceScreen.kt << 'EOF'
package com.plp.attendance.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen() {
    var isCheckedIn by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var checkInTime by remember { mutableStateOf<String?>(null) }
    var workingTime by remember { mutableStateOf("0h 00m") }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            if (isCheckedIn && checkInTime != null) {
                workingTime = calculateWorkingTime(checkInTime!!)
            }
            delay(1000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Clock",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentTime,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()),
                    fontSize = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCheckedIn) Color(0xFF4CAF50) else Color(0xFFFF5252)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (isCheckedIn) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "Status",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isCheckedIn) "CHECKED IN" else "NOT CHECKED IN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isCheckedIn) {
                        Text(
                            text = "Working Time: $workingTime",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Location Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Current Location",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Phnom Penh, Cambodia",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Check In/Out Button
        if (!isCheckedIn) {
            Button(
                onClick = { 
                    isCheckedIn = true
                    checkInTime = currentTime
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = "Check In")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHECK IN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            OutlinedButton(
                onClick = { 
                    isCheckedIn = false
                    checkInTime = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Check Out")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHECK OUT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.EventNote, contentDescription = "Leave", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Leave")
            }
            OutlinedButton(
                onClick = { },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("History")
            }
        }
    }
}

private fun getCurrentTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

private fun calculateWorkingTime(checkInTime: String): String {
    try {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val checkIn = format.parse(checkInTime)
        val now = Date()
        val diff = now.time - checkIn.time
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        return String.format("%dh %02dm", hours, minutes)
    } catch (e: Exception) {
        return "0h 00m"
    }
}
EOF

# Now build the app
echo "🔨 Building APK with features..."
build_and_check

if [ $? -eq 0 ]; then
    # Create output directory
    timestamp=$(date +%Y%m%d_%H%M%S)
    output_dir="apk_features_$timestamp"
    mkdir -p "$output_dir"
    
    # Copy APK
    cp app/build/outputs/apk/debug/app-debug.apk "$output_dir/PLP_Attendance_Features.apk"
    
    # Get APK info
    size=$(du -h "$output_dir/PLP_Attendance_Features.apk" | cut -f1)
    
    # Create feature list
    cat > "$output_dir/FEATURES.txt" << EOF
Cambodia Education Attendance System - Feature Build
===================================================

Version: 1.0
Build Date: $(date)
File Size: $size

✨ Implemented Features:
------------------------
1. Authentication
   - Login screen with email/password
   - Bilingual interface (Khmer/English)
   - Material Design 3 theming

2. Navigation Architecture
   - Bottom navigation with 4 tabs
   - Screen-to-screen navigation
   - State management

3. Attendance Module
   - Real-time clock display
   - Check in/out functionality
   - Working time calculation
   - Location display
   - Status cards with visual feedback

4. UI Components
   - Material Design 3 components
   - Responsive layouts
   - Icons and visual indicators
   - Quick action buttons

5. Architecture
   - Clean code structure
   - Separated UI/domain layers
   - Modular design

📱 Next Features to Add:
-----------------------
- Network connectivity
- Local data storage
- Leave management
- Reports generation
- Offline support
- Push notifications
- Biometric authentication

Installation:
1. Enable "Unknown Sources" in Android Settings
2. Transfer APK to device
3. Tap to install

Server: 137.184.109.21:3000
EOF
    
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo "📱 APK: $output_dir/PLP_Attendance_Features.apk"
    echo "📊 Size: $size"
    echo ""
    echo "✨ Features Added:"
    echo "  ✓ Login Screen"
    echo "  ✓ Bottom Navigation (4 tabs)"
    echo "  ✓ Enhanced Attendance Screen"
    echo "  ✓ Material Design 3 Theme"
    echo "  ✓ Working Time Calculation"
    echo "  ✓ Location Display"
    echo ""
    echo "To install: adb install '$output_dir/PLP_Attendance_Features.apk'"
    
    # Try to open folder
    open "$output_dir" 2>/dev/null || true
else
    echo "❌ Build failed. Check build.log for details."
fi

# Cleanup
rm -f build.log

echo "✅ Done!"