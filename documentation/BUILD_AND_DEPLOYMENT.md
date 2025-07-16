# Build and Deployment Guide - PLP Attendance App

## Prerequisites

### Development Environment
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Kotlin 1.9.0+
- Git

### API Keys and Credentials
```bash
# Create local.properties file (not tracked in git)
MAPS_API_KEY=your_google_maps_api_key
API_BASE_URL_DEV=https://api-dev.plp-attendance.gov.kh
API_BASE_URL_PROD=https://api.plp-attendance.gov.kh
```

## Build Configuration

### 1. Clone Repository
```bash
git clone https://github.com/plp-cambodia/attendance-app-kotlin.git
cd attendance-app-kotlin
```

### 2. Install Dependencies
```bash
# Sync project with Gradle files
./gradlew build
```

### 3. Build Variants

#### Debug Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

#### Bundle for Play Store
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

## Signing Configuration

### 1. Generate Keystore
```bash
keytool -genkey -v -keystore plp-release-key.keystore \
  -alias plp-attendance -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Configure Signing
```kotlin
// app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file("../plp-release-key.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "plp-attendance"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## Version Management

### Semantic Versioning
```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        versionCode = 1  // Increment for each release
        versionName = "1.0.0"  // Major.Minor.Patch
    }
}
```

### Version Code Strategy
- Production: 1000000 + (major * 10000) + (minor * 100) + patch
- Example: v2.3.1 = 1020301

## Build Optimization

### ProGuard Rules
```proguard
# app/proguard-rules.pro
-keep class com.plp.attendance.domain.entities.** { *; }
-keep class com.plp.attendance.data.remote.dto.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
```

### Build Performance
```kotlin
// gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
kotlin.code.style=official
```

## Continuous Integration/Deployment

### GitHub Actions Workflow
```yaml
# .github/workflows/android-ci.yml
name: Android CI/CD

on:
  push:
    branches: [ main, develop ]
    tags: [ 'v*' ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Decode Keystore
      env:
        KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
      run: |
        echo $KEYSTORE_BASE64 | base64 -d > plp-release-key.keystore
    
    - name: Build Release APK
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      run: |
        ./gradlew assembleRelease
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-release
        path: app/build/outputs/apk/release/app-release.apk
    
    - name: Create Release
      if: startsWith(github.ref, 'refs/tags/')
      uses: softprops/action-gh-release@v1
      with:
        files: app/build/outputs/apk/release/app-release.apk
```

## Deployment Process

### 1. Internal Testing
```bash
# Build and deploy to internal testers
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Or use App Center / Firebase App Distribution
fastlane deploy_internal
```

### 2. Beta Testing
```bash
# Build beta version
./gradlew assembleBeta

# Upload to Play Store Internal Testing
fastlane beta
```

### 3. Production Release

#### Pre-release Checklist
- [ ] Update version code and name
- [ ] Run all tests: `./gradlew test connectedAndroidTest`
- [ ] Check ProGuard rules
- [ ] Update changelog
- [ ] Test on multiple devices
- [ ] Verify API endpoints point to production
- [ ] Check analytics and crash reporting

#### Release Steps
1. Create release branch
```bash
git checkout -b release/v1.0.0
```

2. Build release bundle
```bash
./gradlew bundleRelease
```

3. Upload to Play Store
```bash
fastlane deploy
```

4. Tag release
```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

## Monitoring and Analytics

### Crash Reporting (Firebase Crashlytics)
```kotlin
// Automatically included with Firebase
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

### Performance Monitoring
```kotlin
dependencies {
    implementation("com.google.firebase:firebase-perf-ktx")
}
```

### Analytics Events
```kotlin
// Track key events
Analytics.logEvent("check_in_completed") {
    param("location_accuracy", accuracy)
    param("time_of_day", timeCategory)
}
```

## Rollback Procedure

### Emergency Rollback
1. Halt rollout in Play Console
2. Upload previous stable version
3. Notify users of known issues
4. Deploy hotfix

### Database Migration Rollback
```kotlin
// Always test migrations
@Database(
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = Migration1To2::class)
    ]
)
```

## Security Checklist

- [ ] API keys not hardcoded
- [ ] ProGuard enabled for release
- [ ] Certificate pinning implemented
- [ ] Sensitive data encrypted
- [ ] No debug logs in release
- [ ] Permissions minimized
- [ ] Deep links validated

## Post-Deployment

### Monitor Key Metrics
- Crash-free rate > 99.5%
- ANR rate < 0.5%
- App startup time < 2s
- API success rate > 99%

### User Feedback Channels
- In-app feedback form
- Play Store reviews
- Support email: support@plp-attendance.gov.kh

### Update Communication
- In-app update prompts for critical updates
- Release notes in Khmer and English
- Email notifications for administrators