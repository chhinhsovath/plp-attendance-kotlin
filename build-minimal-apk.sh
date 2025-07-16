#!/bin/bash

echo "🔧 Building minimal APK without complex dependencies..."

# Backup original build file
cp app/build.gradle.kts app/build.gradle.kts.backup

# Use minimal build file
cp app/build-minimal.gradle.kts app/build.gradle.kts

# Create simple MainActivity
mkdir -p app/src/main/java/com/plp/attendance

cat > app/src/main/java/com/plp/attendance/MainActivity.kt << 'EOF'
package com.plp.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AttendanceScreen()
                }
            }
        }
    }
}

@Composable
fun AttendanceScreen() {
    var checkedIn by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cambodia Education",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Attendance System",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Version 1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (checkedIn) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (checkedIn) "Checked In ✓" else "Not Checked In",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { checkedIn = !checkedIn },
            modifier = Modifier.fillMaxWidth(),
            enabled = !checkedIn
        ) {
            Text(if (checkedIn) "Already Checked In" else "Check In")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { checkedIn = false },
            modifier = Modifier.fillMaxWidth(),
            enabled = checkedIn
        ) {
            Text("Check Out")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Server: 137.184.109.21",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
EOF

# Create simple Application class
cat > app/src/main/java/com/plp/attendance/PLPApplication.kt << 'EOF'
package com.plp.attendance

import android.app.Application

class PLPApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
EOF

# Create strings.xml
mkdir -p app/src/main/res/values
cat > app/src/main/res/values/strings.xml << 'EOF'
<resources>
    <string name="app_name">PLP Attendance</string>
</resources>
EOF

# Build
echo "📦 Building APK..."
./gradlew clean assembleDebug

# Check result
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Build successful!"
    
    # Create output directory
    mkdir -p minimal_apk_output
    cp app/build/outputs/apk/debug/app-debug.apk minimal_apk_output/PLP_Attendance_v1.0_Debug.apk
    
    # Get size
    size=$(du -h minimal_apk_output/PLP_Attendance_v1.0_Debug.apk | cut -f1)
    echo "📱 APK created: minimal_apk_output/PLP_Attendance_v1.0_Debug.apk"
    echo "📊 Size: $size"
    
    # Create info file
    cat > minimal_apk_output/README.txt << 'EOF'
PLP Attendance System - Minimal Build
=====================================

This is a minimal working version of the PLP Attendance app.

Features:
- Basic UI with Material Design 3
- Check In/Check Out functionality (UI only)
- Shows server connection info

To install:
1. Enable "Unknown Sources" in Android settings
2. Transfer APK to device
3. Open APK to install

Server: 137.184.109.21

This is a demonstration build. Full features require the complete build with all dependencies.
EOF
    
    echo ""
    echo "📂 Output saved to: minimal_apk_output/"
    echo ""
    echo "To install on device:"
    echo "  adb install minimal_apk_output/PLP_Attendance_v1.0_Debug.apk"
    
else
    echo "❌ Build failed"
fi

# Restore original build file
mv app/build.gradle.kts.backup app/build.gradle.kts

echo "✅ Done!"