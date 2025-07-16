# Testing Guide - PLP Attendance App

## Testing Strategy

### 1. Unit Testing (70% coverage target)
- Domain logic and business rules
- ViewModels and state management
- Data transformations
- Utility functions

### 2. Integration Testing (20% coverage)
- API integration with mock servers
- Database operations
- Repository layer testing
- Navigation flows

### 3. UI Testing (10% coverage)
- Critical user journeys
- Screen rendering
- User interactions
- Accessibility

## Running Tests

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test with Coverage
```bash
./gradlew createDebugCoverageReport
```

## Test Data Setup

### Mock Users
```kotlin
object TestUsers {
    val teacher = User(
        id = "test_teacher_001",
        username = "teacher001",
        email = "teacher001@plp.gov.kh",
        firstName = "Sophea",
        lastName = "Kim",
        role = UserRole.TEACHER,
        schoolId = "school_001"
    )
    
    val director = User(
        id = "test_director_001",
        username = "director001",
        role = UserRole.DIRECTOR,
        schoolId = "school_001"
    )
}
```

### Mock Locations
```kotlin
object TestLocations {
    val insideSchool = Location(
        latitude = 11.5564,
        longitude = 104.9282,
        accuracy = 10f
    )
    
    val outsideSchool = Location(
        latitude = 11.5800,
        longitude = 104.9500,
        accuracy = 10f
    )
}
```

## Screenshot Testing

### Setup
```kotlin
@get:Rule
val screenshotRule = ScreenshotTestRule()

@Test
fun loginScreen_screenshot() {
    composeTestRule.apply {
        onRoot().captureToImage().assertAgainstGolden("login_screen")
    }
}
```

### Updating Screenshots
```bash
./gradlew updateDebugScreenshots
```

## Performance Testing

### App Startup Time
```kotlin
@Test
fun measureAppStartupTime() {
    val benchmarkRule = MacrobenchmarkRule()
    
    benchmarkRule.measureRepeated(
        packageName = "com.plp.attendance",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}
```

### Critical User Journey Performance
- Login → Home: < 2 seconds
- Check-in flow: < 3 seconds
- Report generation: < 5 seconds

## Debugging Failed Tests

### 1. Enable Test Logging
```kotlin
@Before
fun setup() {
    Timber.plant(Timber.DebugTree())
}
```

### 2. Capture Test Artifacts
```kotlin
@After
fun tearDown() {
    if (testFailed) {
        takeScreenshot("failure_${testName}")
        dumpViewHierarchy("hierarchy_${testName}")
        saveLogcat("logs_${testName}")
    }
}
```

### 3. Network Request Debugging
```kotlin
val mockWebServer = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            println("Request: ${request.path}")
            println("Body: ${request.body.readUtf8()}")
            // Return mock response
        }
    }
}
```

## Continuous Integration

### GitHub Actions Workflow
```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        
    - name: Run Unit Tests
      run: ./gradlew test
      
    - name: Run Instrumented Tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 33
        script: ./gradlew connectedAndroidTest
        
    - name: Upload Test Reports
      uses: actions/upload-artifact@v3
      with:
        name: test-reports
        path: app/build/reports/tests/
```

## Test Checklist Before Release

- [ ] All unit tests passing
- [ ] Critical UI flows tested
- [ ] Performance benchmarks met
- [ ] No memory leaks detected
- [ ] Offline mode functionality verified
- [ ] Different screen sizes tested
- [ ] Accessibility tests passing
- [ ] Security tests completed
- [ ] Edge cases covered
- [ ] Regression tests passing