#!/bin/bash

# Simple APK build script for quick build without all features

echo "Creating simplified APK build..."

# Create a simple MainActivity
cat > app/src/main/java/com/plp/attendance/MainActivity.kt << 'EOF'
package com.plp.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PLPAttendanceApp()
        }
    }
}

@Composable
fun PLPAttendanceApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Cambodia Education",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Attendance System",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(onClick = { }) {
                        Text("Check In")
                    }
                    
                    Button(onClick = { }) {
                        Text("Check Out")
                    }
                }
            }
        }
    }
}
EOF

# Create simple Application class
cat > app/src/main/java/com/plp/attendance/CambodiaEducationApp.kt << 'EOF'
package com.plp.attendance

import android.app.Application

class CambodiaEducationApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
EOF

# Create temporary simplified AndroidManifest.xml
cp app/src/main/AndroidManifest.xml app/src/main/AndroidManifest.xml.backup

cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".CambodiaEducationApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="PLP Attendance"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PLPApp"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.PLPApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>

</manifest>
EOF

# Build the APK
echo "Building simplified debug APK..."
./gradlew clean assembleDebug

# Check if build succeeded
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Build succeeded!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
    
    # Copy to output directory
    mkdir -p simple_apk_output
    cp app/build/outputs/apk/debug/app-debug.apk simple_apk_output/PLP_Attendance_Simple_Debug.apk
    echo "Copied to: simple_apk_output/PLP_Attendance_Simple_Debug.apk"
    
    # Get APK size
    size=$(du -h simple_apk_output/PLP_Attendance_Simple_Debug.apk | cut -f1)
    echo "APK size: $size"
else
    echo "❌ Build failed!"
fi

# Restore original AndroidManifest.xml
mv app/src/main/AndroidManifest.xml.backup app/src/main/AndroidManifest.xml

echo "Done!"