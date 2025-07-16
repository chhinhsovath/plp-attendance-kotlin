# Cambodia Education Attendance System - Android App
https://github.com/chhinhsovath/plp-attendance-kotlin.git
A comprehensive attendance tracking system for the Cambodia Ministry of Education, supporting a 7-level hierarchical structure from national administrators down to individual teachers.

## 🏗️ Architecture

This Android application is built using:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Local Database**: Room
- **Navigation**: Navigation Compose

## 📱 Features

### Core Functionality
- **GPS-based Attendance**: Check-in/out with geofencing
- **Mission Management**: Track official travel with start/end locations
- **Leave Management**: Request and approve leave with documentation
- **Offline Support**: Queue actions when offline, sync when connected
- **Multi-language**: Khmer and English support

### Role-Based Features
1. **Teacher**: Daily attendance, mission tracking, leave requests
2. **Director**: Staff management, approval workflows, school reports
3. **Cluster Head**: Multi-school oversight, director performance tracking
4. **Department/Provincial/Zone**: Hierarchical data aggregation and reporting
5. **Administrator**: System configuration, user management, comprehensive analytics

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Google Maps API Key

### Setup
1. Clone the repository
```bash
git clone https://github.com/plp-cambodia/attendance-app-kotlin.git
cd attendance-app-kotlin
```

2. Add API keys to `local.properties`
```properties
MAPS_API_KEY=your_google_maps_api_key
API_BASE_URL_DEV=https://api-dev.plp-attendance.gov.kh
```

3. Build and run
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## 📋 Documentation

- [User Interaction Scenarios](documentation/USER_INTERACTION_SCENARIOS.md) - Detailed user flows and validations
- [API Testing Plan](documentation/API_TESTING_PLAN.md) - Comprehensive API endpoint testing
- [Testing Guide](documentation/TESTING_GUIDE.md) - Unit, integration, and UI testing strategies
- [Build & Deployment](documentation/BUILD_AND_DEPLOYMENT.md) - CI/CD and release procedures

## 🧪 Testing

### Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# All tests with coverage
./gradlew createDebugCoverageReport
```

## 📊 Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/plp/attendance/
│   │   │   ├── data/          # Data layer (API, DB, repositories)
│   │   │   ├── domain/        # Business logic and entities
│   │   │   ├── presentation/  # UI layer (screens, viewmodels)
│   │   │   └── di/           # Dependency injection modules
│   │   └── res/              # Resources (layouts, strings, etc.)
│   ├── test/                 # Unit tests
│   └── androidTest/          # UI tests
└── build.gradle.kts
```

## 🔑 Key Technologies

### Dependencies
- **Compose BOM**: 2023.10.01
- **Hilt**: 2.48
- **Retrofit**: 2.9.0
- **Room**: 2.6.0
- **Navigation Compose**: 2.7.5
- **Maps Compose**: 3.1.0

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is proprietary software for the Cambodia Ministry of Education.

## 📞 Support

- Technical Support: support@plp-attendance.gov.kh
- Documentation: [Wiki](https://github.com/plp-cambodia/attendance-app-kotlin/wiki)
- Issue Tracking: [GitHub Issues](https://github.com/plp-cambodia/attendance-app-kotlin/issues)# plp-attendance-kotlin
