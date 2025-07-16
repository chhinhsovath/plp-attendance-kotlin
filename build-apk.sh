#!/bin/bash

# Build script for Cambodia Education Attendance System APK

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_section() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

# Check if keystore exists
check_keystore() {
    if [ ! -f "release-keystore.jks" ]; then
        log_warn "Keystore not found. Creating new keystore..."
        
        # Generate keystore
        keytool -genkey -v \
            -keystore release-keystore.jks \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -alias plp-attendance-key \
            -dname "CN=PLP Education, OU=Development, O=PLP Cambodia, L=Phnom Penh, S=Phnom Penh, C=KH" \
            -storepass "plp@ttend@nce2024" \
            -keypass "plp@ttend@nce2024"
        
        log_info "Keystore created successfully!"
    else
        log_info "Keystore found."
    fi
}

# Clean previous builds
clean_build() {
    log_section "Cleaning Previous Builds"
    ./gradlew clean
    rm -rf app/build/outputs/apk/
    log_info "Clean completed"
}

# Build Debug APK
build_debug() {
    log_section "Building Debug APK"
    ./gradlew assembleDebug
    
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        log_info "Debug APK built successfully!"
        log_info "Location: app/build/outputs/apk/debug/app-debug.apk"
        
        # Get APK size
        debug_size=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
        log_info "Debug APK size: $debug_size"
    else
        log_error "Debug APK build failed!"
        exit 1
    fi
}

# Build Release APK
build_release() {
    log_section "Building Release APK"
    
    # Check keystore
    check_keystore
    
    ./gradlew assembleRelease
    
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        log_info "Release APK built successfully!"
        log_info "Location: app/build/outputs/apk/release/app-release.apk"
        
        # Get APK size
        release_size=$(du -h app/build/outputs/apk/release/app-release.apk | cut -f1)
        log_info "Release APK size: $release_size"
    else
        log_error "Release APK build failed!"
        exit 1
    fi
}

# Build Staging APK
build_staging() {
    log_section "Building Staging APK"
    ./gradlew assembleStaging
    
    if [ -f "app/build/outputs/apk/staging/app-staging.apk" ]; then
        log_info "Staging APK built successfully!"
        log_info "Location: app/build/outputs/apk/staging/app-staging.apk"
        
        # Get APK size
        staging_size=$(du -h app/build/outputs/apk/staging/app-staging.apk | cut -f1)
        log_info "Staging APK size: $staging_size"
    else
        log_error "Staging APK build failed!"
        exit 1
    fi
}

# Build App Bundle (AAB) for Play Store
build_bundle() {
    log_section "Building App Bundle (AAB)"
    
    # Check keystore
    check_keystore
    
    ./gradlew bundleRelease
    
    if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
        log_info "App Bundle built successfully!"
        log_info "Location: app/build/outputs/bundle/release/app-release.aab"
        
        # Get AAB size
        bundle_size=$(du -h app/build/outputs/bundle/release/app-release.aab | cut -f1)
        log_info "App Bundle size: $bundle_size"
    else
        log_error "App Bundle build failed!"
        exit 1
    fi
}

# Copy APKs to output directory
copy_outputs() {
    log_section "Copying Output Files"
    
    # Create output directory with timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)
    output_dir="apk_output_$timestamp"
    mkdir -p "$output_dir"
    
    # Copy APKs
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        cp app/build/outputs/apk/debug/app-debug.apk "$output_dir/PLP_Attendance_Debug_v1.0.apk"
        log_info "Copied: PLP_Attendance_Debug_v1.0.apk"
    fi
    
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        cp app/build/outputs/apk/release/app-release.apk "$output_dir/PLP_Attendance_Release_v1.0.apk"
        log_info "Copied: PLP_Attendance_Release_v1.0.apk"
    fi
    
    if [ -f "app/build/outputs/apk/staging/app-staging.apk" ]; then
        cp app/build/outputs/apk/staging/app-staging.apk "$output_dir/PLP_Attendance_Staging_v1.0.apk"
        log_info "Copied: PLP_Attendance_Staging_v1.0.apk"
    fi
    
    if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
        cp app/build/outputs/bundle/release/app-release.aab "$output_dir/PLP_Attendance_PlayStore_v1.0.aab"
        log_info "Copied: PLP_Attendance_PlayStore_v1.0.aab"
    fi
    
    # Create README
    cat > "$output_dir/README.txt" << EOF
Cambodia Education Attendance System - Build Output
===================================================

Build Date: $(date)
Version: 1.0
Build Number: 1

Files:
------
1. PLP_Attendance_Debug_v1.0.apk
   - For development and testing
   - Includes debug logging
   - Can be installed alongside other variants

2. PLP_Attendance_Release_v1.0.apk
   - Production-ready APK
   - Optimized and minified
   - For distribution outside Play Store

3. PLP_Attendance_Staging_v1.0.apk
   - For staging/testing environment
   - Between debug and release

4. PLP_Attendance_PlayStore_v1.0.aab
   - App Bundle for Google Play Store submission
   - Smaller download size for users
   - Recommended for Play Store

Installation Instructions:
-------------------------
1. Enable "Install from Unknown Sources" in device settings
2. Transfer APK to device
3. Open APK file to install

Keystore Information:
--------------------
Alias: plp-attendance-key
Validity: 10000 days
Organization: PLP Cambodia

API Endpoints:
-------------
Debug: https://dev-api.attendance.edu.kh/
Staging: https://staging-api.attendance.edu.kh/
Production: https://api.attendance.edu.kh/

Support:
--------
Email: support@plp.edu.kh
EOF
    
    log_info "All files copied to: $output_dir"
    
    # Open output directory
    open "$output_dir" 2>/dev/null || true
}

# Show build summary
show_summary() {
    log_section "Build Summary"
    
    echo "Build completed successfully!"
    echo ""
    echo "Generated files:"
    
    if [ -f "$output_dir/PLP_Attendance_Debug_v1.0.apk" ]; then
        echo "✅ Debug APK: $(du -h "$output_dir/PLP_Attendance_Debug_v1.0.apk" | cut -f1)"
    fi
    
    if [ -f "$output_dir/PLP_Attendance_Release_v1.0.apk" ]; then
        echo "✅ Release APK: $(du -h "$output_dir/PLP_Attendance_Release_v1.0.apk" | cut -f1)"
    fi
    
    if [ -f "$output_dir/PLP_Attendance_Staging_v1.0.apk" ]; then
        echo "✅ Staging APK: $(du -h "$output_dir/PLP_Attendance_Staging_v1.0.apk" | cut -f1)"
    fi
    
    if [ -f "$output_dir/PLP_Attendance_PlayStore_v1.0.aab" ]; then
        echo "✅ App Bundle: $(du -h "$output_dir/PLP_Attendance_PlayStore_v1.0.aab" | cut -f1)"
    fi
    
    echo ""
    echo "Output directory: $output_dir"
}

# Main build process
main() {
    log_section "Cambodia Education Attendance System - APK Builder"
    
    # Check if we're in the right directory
    if [ ! -f "gradlew" ]; then
        log_error "gradlew not found. Please run this script from the project root directory."
        exit 1
    fi
    
    # Make gradlew executable
    chmod +x gradlew
    
    case "${1:-all}" in
        "debug")
            clean_build
            build_debug
            copy_outputs
            ;;
        "release")
            clean_build
            build_release
            copy_outputs
            ;;
        "staging")
            clean_build
            build_staging
            copy_outputs
            ;;
        "bundle")
            clean_build
            build_bundle
            copy_outputs
            ;;
        "all")
            clean_build
            build_debug
            build_release
            build_staging
            build_bundle
            copy_outputs
            show_summary
            ;;
        "clean")
            clean_build
            ;;
        *)
            echo "Usage: $0 [debug|release|staging|bundle|all|clean]"
            echo ""
            echo "Options:"
            echo "  debug    - Build debug APK only"
            echo "  release  - Build release APK only"
            echo "  staging  - Build staging APK only"
            echo "  bundle   - Build app bundle (AAB) only"
            echo "  all      - Build all variants (default)"
            echo "  clean    - Clean build directories"
            exit 1
            ;;
    esac
    
    log_info "Build process completed!"
}

# Run main function
main "$@"