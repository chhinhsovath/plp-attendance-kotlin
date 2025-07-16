#!/bin/bash

set -e  # Exit on any error

echo "🧹 Preparing fresh build environment..."

# Function to check if adb device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        echo "❌ No Android device/emulator connected!"
        echo "Please connect a device or start an emulator, then run 'adb devices' to verify."
        exit 1
    fi
    echo "✅ Android device/emulator detected"
}

# Function to clean build artifacts
clean_build() {
    echo "🧽 Cleaning previous build artifacts..."
    ./gradlew clean
    
    # Remove any cached/temp files
    rm -rf .gradle/caches/
    rm -rf app/build/
    rm -rf build/
    
    echo "✅ Build environment cleaned"
}

# Function to sync dependencies
sync_dependencies() {
    echo "📦 Syncing dependencies..."
    ./gradlew --refresh-dependencies
    echo "✅ Dependencies synced"
}

# Function to build APK
build_apk() {
    echo "🔨 Building fresh debug APK from latest codebase..."
    ./gradlew assembleDebug --info
    
    # Verify APK was created
    if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        echo "❌ APK file not found after build!"
        exit 1
    fi
    
    echo "✅ Fresh APK built successfully!"
}

# Function to install APK
install_apk() {
    echo "📱 Installing fresh APK on device..."
    
    # Force stop any running instance
    adb shell am force-stop com.plp.attendance.debug 2>/dev/null || true
    adb shell am force-stop com.plp.attendance 2>/dev/null || true
    
    # Uninstall existing app completely for clean install
    echo "🗑️ Removing existing app installation..."
    adb uninstall com.plp.attendance.debug 2>/dev/null || true
    adb uninstall com.plp.attendance 2>/dev/null || true
    
    # Clear any cached data
    adb shell pm clear com.plp.attendance.debug 2>/dev/null || true
    adb shell pm clear com.plp.attendance 2>/dev/null || true
    
    # Install the fresh APK
    echo "📲 Installing fresh APK..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    echo "✅ Fresh APK installed successfully!"
}

# Function to launch app
launch_app() {
    echo "🚀 Launching fresh app..."
    
    # Wait a moment for installation to settle
    sleep 2
    
    # Launch the app (try both possible package names)
    if adb shell am start -n com.plp.attendance.debug/com.plp.attendance.MainActivity 2>/dev/null; then
        echo "✅ App launched with debug package name"
    elif adb shell am start -n com.plp.attendance/.MainActivity 2>/dev/null; then
        echo "✅ App launched with release package name"
    else
        echo "⚠️ Could not auto-launch app. Please launch manually from device."
    fi
}

# Main execution
echo "🚀 Starting fresh build and install process..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check prerequisites
check_device

# Execute build pipeline
clean_build
sync_dependencies
build_apk
install_apk
launch_app

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎉 Fresh build and install completed successfully!"
echo "📱 The latest version of the app is now running on your device."
echo ""
echo "💡 APK location: app/build/outputs/apk/debug/app-debug.apk"
echo "🔍 To debug: run 'adb logcat | grep PLP' for app logs"