#!/bin/bash

echo "🚀 Creating minimal APK by temporarily moving complex files..."

# Create backup directory
mkdir -p app_backup

# Move all kotlin files except MainActivity and PLPApplication
echo "📦 Moving complex files temporarily..."
find app/src/main/java/com/plp/attendance -name "*.kt" ! -name "MainActivity.kt" ! -name "PLPApplication.kt" -exec mv {} app_backup/ \; 2>/dev/null

# Build APK
echo "🔨 Building APK..."
./gradlew clean assembleDebug

# Check if build succeeded
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Build successful!"
    
    # Create output directory with timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)
    output_dir="apk_output_minimal_$timestamp"
    mkdir -p "$output_dir"
    
    # Copy APK
    cp app/build/outputs/apk/debug/app-debug.apk "$output_dir/PLP_Attendance_Minimal_v1.0.apk"
    
    # Get APK info
    size=$(du -h "$output_dir/PLP_Attendance_Minimal_v1.0.apk" | cut -f1)
    
    echo ""
    echo "📱 APK Details:"
    echo "   File: $output_dir/PLP_Attendance_Minimal_v1.0.apk"
    echo "   Size: $size"
    echo ""
    
    # Create README
    cat > "$output_dir/README.txt" << EOF
Cambodia Education Attendance System - Minimal APK
=================================================

Version: 1.0 (Minimal)
Build Date: $(date)
Size: $size

This is a minimal version of the PLP Attendance app with basic UI only.

Features:
- Material Design 3 UI
- Check In/Check Out buttons (UI only)
- Shows server connection info

Installation:
1. Enable "Unknown Sources" in Android Settings
2. Transfer APK to your Android device
3. Open the APK file to install

Server Configuration:
- API Server: 137.184.109.21
- Port: 3000

Note: This is a demonstration build. The full-featured version requires
all dependencies to be properly configured.

To install via ADB:
adb install PLP_Attendance_Minimal_v1.0.apk

Support: support@plp.edu.kh
EOF
    
    echo "📂 Output saved to: $output_dir/"
    echo ""
    echo "To install on device:"
    echo "   adb install $output_dir/PLP_Attendance_Minimal_v1.0.apk"
    
    # Open output directory
    open "$output_dir" 2>/dev/null || true
    
else
    echo "❌ Build failed"
fi

# Restore files
echo "♻️ Restoring original files..."
find app_backup -name "*.kt" -exec mv {} app/src/main/java/com/plp/attendance/ \; 2>/dev/null
rmdir app_backup 2>/dev/null

echo "✅ Done!"