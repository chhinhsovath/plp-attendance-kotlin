package com.plp.attendance

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Use Case Tests
    com.plp.attendance.domain.usecase.auth.LoginUseCaseTest::class,
    com.plp.attendance.domain.usecase.attendance.CheckInUseCaseTest::class,
    
    // ViewModel Tests
    com.plp.attendance.ui.auth.LoginViewModelTest::class,
    
    // Existing Tests (to be updated)
    // com.plp.attendance.domain.usecases.AttendanceUseCaseTest::class,
    // com.plp.attendance.data.repositories.AttendanceRepositoryTest::class,
    // com.plp.attendance.presentation.viewmodels.AttendanceViewModelTest::class,
    // com.plp.attendance.utils.ValidationUtilsTest::class,
    // com.plp.attendance.security.EncryptionManagerTest::class,
    // com.plp.attendance.monitoring.PerformanceMonitorTest::class
)
class TestRunner