# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Cambodia Education Attendance System - an Android app built with Kotlin and Jetpack Compose. It's a comprehensive attendance tracking system supporting a 7-level hierarchical structure from national administrators down to individual teachers.

## Essential Commands

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install debug build on device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing Commands
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run all tests with coverage report
./gradlew createDebugCoverageReport

# Run specific test class
./gradlew test --tests "com.plp.attendance.YourTestClass"
```

### Development Setup
Note: No gradlew wrapper exists yet. You'll need to generate it:
```bash
gradle wrapper --gradle-version 8.1.1
```

## Architecture Overview

The app follows **Clean Architecture with MVVM**, organized into three main layers:

### Domain Layer (`domain/`)
- **Entities**: Core business models (User, Attendance, Leave, Mission, School)
- **Repository Interfaces**: Not yet implemented
- **Use Cases**: Not yet implemented

### Data Layer (`data/`)
Currently empty, needs implementation:
- **Remote**: API services, DTOs, remote data sources
- **Local**: Room database, DAOs, local data sources
- **Repositories**: Repository implementations

### Presentation Layer (`presentation/`)
- **MainActivity**: Main entry point with Compose navigation
- **Screens**: Individual feature screens (not yet implemented)
- **ViewModels**: State management (not yet implemented)
- **Navigation**: Role-based navigation routing defined in `Screen.kt`

## Key Technologies & Dependencies

- **Kotlin 1.9.0** with JDK 17
- **Jetpack Compose** (BOM 2023.10.01) for UI
- **Hilt 2.48** for dependency injection
- **Retrofit 2.9.0** for networking
- **Room 2.6.0** for local database
- **Navigation Compose 2.7.5** for navigation
- **Google Maps** for location services
- **WorkManager** for background tasks
- **Biometric 1.1.0** for fingerprint/face authentication
- **DataStore Preferences** for settings storage

## User Role Hierarchy

The app supports 7 hierarchical levels:
1. **Administrator** (National level)
2. **Zone** (Multi-province oversight)
3. **Provincial** (Province management)
4. **Department** (District/Department level)
5. **Cluster Head** (Multi-school management)
6. **Director** (School management)
7. **Teacher** (End user)

Each role has specific navigation routes and permissions defined in `navigation/Screen.kt`.

## Current Implementation Status

**Implemented:**
- Project structure and dependencies
- Domain entities with business logic
- Basic navigation setup with role-based routing
- Hilt configuration in Application and MainActivity
- Complete offline-first architecture with Room database
- Network layer with Retrofit and error handling
- Location services with GPS tracking and geofencing
- Mission tracking with route recording
- Leave management with approval workflows
- Role-based analytics dashboards
- Biometric authentication system with fingerprint/face support
- Comprehensive settings management with DataStore

**Major Features Completed:**
- **Attendance System**: GPS-verified check-in/out with photo capture
- **Leave Management**: Multi-level approval workflow with calendar integration
- **Mission Tracking**: Real-time GPS route recording with foreground service
- **Analytics Dashboard**: Role-specific metrics and performance tracking
- **Biometric Security**: Fingerprint/face authentication for secure actions
- **Offline Support**: Complete offline-first architecture with sync queue
- **Background Sync**: Automatic data synchronization with WorkManager

## Development Guidelines

1. **Clean Architecture**: Maintain strict layer separation. Domain layer should have no Android dependencies.

2. **Dependency Flow**: 
   - Presentation → Domain ← Data
   - Use interfaces in domain layer, implementations in data layer

3. **State Management**: Use ViewModels with StateFlow for UI state

4. **Coroutines**: Use for all async operations

5. **Error Handling**: Implement Result types or sealed classes for error states

6. **Testing**: Write unit tests for ViewModels and use cases, integration tests for repositories

## Important Files

- `PLPApplication.kt`: Hilt application class with WorkManager setup
- `MainActivity.kt`: Entry point with navigation setup
- `Screen.kt`: All navigation destinations for different user roles
- Domain entities in `domain/entities/`: Core business models

## API Integration

When implementing API services:
1. Add API key to `local.properties`: `API_BASE_URL_DEV=https://api-dev.plp-attendance.gov.kh`
2. Create Retrofit interfaces in `data/remote/api/`
3. Implement repository pattern for data access
4. Handle offline scenarios with Room caching

## GPS & Location Features

The app requires location services for:
- Attendance check-in/out with geofencing
- Mission tracking with start/end locations
- School location verification

Remember to handle location permissions properly in the UI layer.

## Biometric Authentication

The app includes comprehensive biometric authentication support:

### Features:
- **Fingerprint Recognition**: Primary biometric method
- **Face Recognition**: Secondary biometric method on supported devices
- **PIN Fallback**: Optional fallback when biometric fails
- **Configurable Requirements**: Separate settings for attendance and sensitive actions
- **Timeout Management**: Configurable re-authentication intervals

### Key Files:
- `BiometricAuthManager.kt`: Core biometric authentication service
- `BiometricPreferences.kt`: DataStore-based settings storage
- `BiometricAuthDialog.kt`: Compose UI components for authentication
- `BiometricSettingsScreen.kt`: Settings UI for biometric configuration

### Configuration:
Biometric authentication can be configured for:
1. **Attendance Actions**: Require biometric for check-in/check-out
2. **Sensitive Actions**: Require biometric for data export, admin functions
3. **Fallback Options**: Allow PIN authentication when biometric fails
4. **Timeout Settings**: Re-authenticate after 1, 5, 15, 30, or 60 minutes

### Permissions Required:
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

### Implementation Notes:
- Uses AndroidX Biometric library for compatibility
- Gracefully handles devices without biometric hardware
- Provides clear status messages for various biometric states
- Integrates with existing MVI architecture pattern

## Real-time Notifications System

The app includes a comprehensive real-time notifications system for user engagement and workflow management:

### Features:
- **Multi-channel Notifications**: Separate channels for attendance, approvals, missions, system alerts
- **Smart Scheduling**: Automated reminders based on work schedules and user preferences
- **Rich Notifications**: Action buttons, big text, progress indicators, and badges
- **Offline Support**: Local notification storage with sync capabilities
- **In-app Management**: Complete notification center with filtering and search

### Key Components:
- `NotificationService.kt`: Core notification display service with channel management
- `NotificationScheduler.kt`: WorkManager-based scheduling for recurring notifications
- `NotificationScreen.kt`: Complete in-app notification center UI
- `NotificationEntity.kt`: Room database entity for notification persistence
- `NotificationRepository.kt`: Repository pattern for notification data management

### Notification Types:
1. **Attendance Reminders**: Check-in/check-out reminders with location awareness
2. **Approval Notifications**: Real-time alerts for approval requests and responses
3. **Mission Updates**: Start/end notifications with tracking status
4. **System Alerts**: Important system messages and compliance notifications
5. **Sync Status**: Background synchronization progress and errors

### Smart Features:
- **Geofencing Integration**: Location-aware attendance reminders
- **Work Schedule Awareness**: Timing based on school hours and user role
- **Priority Management**: Urgent, high, normal, and low priority levels
- **Quiet Hours**: Automatic notification silencing during configured hours
- **Batch Processing**: Efficient handling of multiple notifications

### Automation:
- **Daily Attendance Reminders**: Scheduled based on work start time
- **Check-out Reminders**: End-of-day notifications with working hours summary
- **Weekly Reports**: Management-level summary notifications
- **Mission Alerts**: Pre-mission reminders and tracking updates
- **Approval Workflows**: Instant notifications for approval chain management

### Configuration:
```kotlin
// Schedule daily reminders
notificationScheduler.scheduleDailyAttendanceReminder(
    reminderTime = LocalTime.of(8, 0), // 8:00 AM
    reminderMinutesBefore = 15
)

// Create custom notifications
notificationService.showApprovalRequest(
    type = "leave",
    requesterName = "John Doe", 
    details = "Sick leave request for 2 days",
    requestId = "req-123"
)
```

### Permissions Required:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## Report Generation and Export System

The app includes a comprehensive report generation and export system for data analysis and compliance:

### Features:
- **Multiple Report Types**: Attendance, leave, mission, performance, and compliance reports
- **Flexible Export Formats**: PDF, Excel (XLSX), CSV, and JSON export capabilities
- **Configurable Parameters**: Date ranges, entity scope, grouping options, and detail levels
- **Scheduled Reports**: Automated generation with email distribution
- **Rich PDF Generation**: Professional layouts with charts, tables, and summary statistics
- **Excel Integration**: Full spreadsheet generation with formatting and formulas

### Key Components:
- `ReportRepository.kt`: Complete interface for all report operations
- `ReportGenerationService.kt`: Core service for PDF, Excel, and CSV generation
- `ReportsScreen.kt`: Comprehensive UI for report management and generation
- `ReportGeneratorDialog.kt`: Configuration dialog for report parameters
- `ReportsViewModel.kt`: Complete state management for report operations

### Report Types:
1. **Attendance Reports**: 
   - Summary statistics with attendance rates and punctuality metrics
   - Daily/weekly/monthly breakdowns with trend analysis
   - User details with individual performance metrics
   - Department rankings and comparisons

2. **Leave Reports**:
   - Leave utilization statistics and balance tracking
   - Approval workflow analysis with processing times
   - Seasonal patterns and usage trends
   - Balance reports by leave type and employee

3. **Mission Reports**:
   - Efficiency metrics with distance and time analysis
   - Geographical coverage analysis
   - Route optimization suggestions
   - Performance rankings by user and department

4. **Performance Reports**:
   - Overall performance scores with benchmarking
   - Multi-metric analysis (attendance, punctuality, mission success)
   - Trend analysis with improvement recommendations
   - Comparative rankings and progress tracking

5. **Compliance Reports**:
   - Policy violation tracking and analysis
   - Risk assessment with mitigation recommendations
   - Audit trails and compliance scoring
   - Regulatory compliance monitoring

### Export Capabilities:
- **PDF Generation**: Professional reports with Android's PdfDocument API
- **Excel Export**: Rich spreadsheets using Apache POI library with charts and formatting
- **CSV Export**: Structured data for external analysis and integration
- **Batch Processing**: Multiple reports with progress tracking

### Configuration Options:
```kotlin
// Configure attendance report
val config = ReportGenerationConfig(
    reportType = ReportType.ATTENDANCE,
    entityType = "department", // user, department, school, province
    startDate = "2023-01-01",
    endDate = "2023-12-31",
    format = ExportFormat.PDF,
    includeDetails = true,
    groupBy = GroupBy.MONTHLY
)

// Generate and export
viewModel.generateReport(config)
```

### Key Features:
- **Quick Actions**: Pre-configured report templates for common use cases
- **Analytics Overview**: Summary metrics and recent report statistics
- **Report History**: Complete history with download tracking and file management
- **Scheduled Reports**: Automated generation with configurable frequency
- **Progress Tracking**: Real-time progress updates during generation
- **File Management**: Share, download, and delete generated reports

### Dependencies Added:
```kotlin
// Apache POI for Excel generation
implementation("org.apache.poi:poi:5.2.4")
implementation("org.apache.poi:poi-ooxml:5.2.4")
```

### Future Enhancements:
- Email integration for scheduled report delivery
- Advanced charting with custom visualizations
- Dashboard widgets for quick metrics
- Export templates for branded reports
- Integration with external BI tools

## Multi-Language Support (Khmer/English)

The app includes comprehensive multi-language support for both Khmer and English languages, essential for Cambodia's educational system:

### Features:
- **Dual Language Support**: Complete English and Khmer (ភាសាខ្មែរ) localization
- **Dynamic Language Switching**: Change language without app restart for UI elements
- **Persistent Language Settings**: User preferences saved in DataStore
- **System Language Detection**: Automatic detection and fallback to device language
- **Localized Content**: All UI text, status messages, and user-facing content
- **Cultural Adaptation**: Proper text direction and layout for both languages

### Key Components:
- `LocalizationManager.kt`: Core service for language management and context creation
- `StringResourceProvider.kt`: Utility for dynamic string resource access
- `LocalizedText.kt`: Compose components for localized text rendering
- `LanguageSettingsScreen.kt`: Complete UI for language configuration
- `LanguageSwitcher.kt`: Various language switcher components for different UI contexts

### String Resources:
- **English** (`values/strings.xml`): 265+ localized strings covering all app features
- **Khmer** (`values-km/strings.xml`): Complete Khmer translations with proper Unicode support
- **Comprehensive Coverage**: All UI elements, error messages, notifications, and content

### Language Management:
```kotlin
// Initialize localization in Application
localizationManager.setLanguage("km") // Switch to Khmer
localizationManager.setLanguage("en") // Switch to English

// Get localized strings
stringResourceProvider.getString(R.string.attendance)
stringResourceProvider.attendanceStatus("present") // Returns localized status
```

### UI Components:
```kotlin
// Use localized text in Compose
LocalizedText(textRes = R.string.welcome_back)

// Dynamic status localization
LocalizedAttendanceStatus(status = "present")
LocalizedUserRole(role = "teacher")

// Language switcher components
LanguageSwitcher(onLanguageChanged = { /* handle change */ })
CompactLanguageSwitcher() // For toolbar/app bar
LanguageToggleChips() // For settings screens
```

### Integration Points:
- **MainActivity**: Language context application and configuration changes
- **PLPApplication**: System-wide locale initialization and management
- **All Screens**: Use LocalizedText components for proper text rendering
- **Notifications**: Localized notification content based on user preference
- **Reports**: Multi-language report generation with localized headers and content

### Features Implemented:
1. **Complete String Localization**: Every user-facing string has both English and Khmer versions
2. **Smart Language Detection**: Auto-detects device language with fallback options
3. **Seamless Language Switching**: Change language dynamically with immediate UI updates
4. **Persistent Settings**: Language preference saved and restored across app sessions
5. **Localized Components**: Specialized components for status messages, roles, and dynamic content
6. **Configuration Management**: Proper handling of configuration changes and context creation

### Technical Implementation:
- **DataStore Integration**: Language preferences stored using AndroidX DataStore
- **Context Management**: Proper localized context creation for Activities and Services
- **Compose Integration**: CompositionLocal pattern for accessing localization throughout UI
- **Resource Management**: Efficient string resource loading with caching
- **Unicode Support**: Proper Khmer text rendering with correct fonts and layout

### Language Coverage:
**Localized Categories:**
- Authentication and login flows
- Dashboard and navigation elements
- Attendance tracking terminology
- Leave management vocabulary
- Mission tracking language
- Report generation content
- Settings and preferences
- Biometric authentication prompts
- Notification messages
- Error and status messages
- Date and time formatting
- User roles and hierarchies
- School administrative terms

This implementation ensures the Cambodia Education Attendance System is fully accessible to both English and Khmer speakers, supporting the diverse linguistic needs of Cambodia's educational institutions.