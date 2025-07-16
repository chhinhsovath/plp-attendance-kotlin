package com.plp.attendance.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.domain.model.AppError
import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.usecase.auth.LoginUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.rules.TestRule

@ExperimentalCoroutinesApi
class LoginViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel
    
    private val testUser = User(
        id = "user-123",
        username = "teacher@school.edu.kh",
        name = "Test Teacher",
        email = "teacher@school.edu.kh",
        phone = "+855123456789",
        role = "TEACHER",
        department = "Primary",
        schoolId = "school-1",
        schoolName = "Demo School",
        profilePhotoPath = null,
        isActive = true
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        loginUseCase = mockk()
        sessionManager = mockk(relaxed = true)
        viewModel = LoginViewModel(loginUseCase, sessionManager)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is correct`() = runTest {
        // When
        val state = viewModel.uiState.first()
        
        // Then
        Assert.assertFalse(state.isLoading)
        Assert.assertNull(state.error)
        Assert.assertFalse(state.isLoggedIn)
        Assert.assertNull(state.emailError)
        Assert.assertNull(state.passwordError)
    }
    
    @Test
    fun `login with valid credentials updates state correctly`() = runTest {
        // Given
        val email = "teacher@school.edu.kh"
        val password = "password123"
        
        coEvery { loginUseCase(email, password) } returns Result.success(testUser)
        
        // When
        viewModel.login(email, password)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        Assert.assertFalse(state.isLoading)
        Assert.assertNull(state.error)
        Assert.assertTrue(state.isLoggedIn)
        
        val navigation = viewModel.navigationEvent.value
        Assert.assertEquals(LoginViewModel.NavigationEvent.NavigateToTeacherDashboard, navigation)
        
        coVerify(exactly = 1) { loginUseCase(email, password) }
    }
    
    @Test
    fun `login with empty email shows validation error`() = runTest {
        // Given
        val email = ""
        val password = "password123"
        
        // When
        viewModel.login(email, password)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        Assert.assertFalse(state.isLoading)
        Assert.assertEquals("Email is required", state.emailError)
        Assert.assertNull(state.passwordError)
        Assert.assertFalse(state.isLoggedIn)
        
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }
    
    @Test
    fun `login with invalid email format shows validation error`() = runTest {
        // Given
        val email = "notanemail"
        val password = "password123"
        
        // When
        viewModel.login(email, password)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        Assert.assertEquals("Invalid email format", state.emailError)
        
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }
    
    @Test
    fun `login with short password shows validation error`() = runTest {
        // Given
        val email = "teacher@school.edu.kh"
        val password = "12345"
        
        // When
        viewModel.login(email, password)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        Assert.assertEquals("Password must be at least 6 characters", state.passwordError)
        
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }
    
    @Test
    fun `login with invalid credentials shows error`() = runTest {
        // Given
        val email = "teacher@school.edu.kh"
        val password = "wrongpassword"
        
        coEvery { loginUseCase(email, password) } returns Result.failure(AppError.Auth.InvalidCredentials)
        
        // When
        viewModel.login(email, password)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        Assert.assertFalse(state.isLoading)
        Assert.assertEquals("Invalid email or password", state.error)
        Assert.assertFalse(state.isLoggedIn)
    }
    
    @Test
    fun `login with network error shows appropriate message`() = runTest {
        // Given
        val email = "teacher@school.edu.kh"
        val password = "password123"
        
        coEvery { loginUseCase(email, password) } returns Result.failure(AppError.Network.NoConnection)
        
        // When
        viewModel.login(email, password)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        Assert.assertEquals("No internet connection. Please check your network", state.error)
    }
    
    @Test
    fun `login navigates based on user role`() = runTest {
        // Test different roles
        val roles = mapOf(
            "ADMINISTRATOR" to LoginViewModel.NavigationEvent.NavigateToAdminDashboard,
            "ZONE_MANAGER" to LoginViewModel.NavigationEvent.NavigateToZoneDashboard,
            "PROVINCIAL" to LoginViewModel.NavigationEvent.NavigateToProvincialDashboard,
            "DEPARTMENT" to LoginViewModel.NavigationEvent.NavigateToDepartmentDashboard,
            "CLUSTER_HEAD" to LoginViewModel.NavigationEvent.NavigateToClusterDashboard,
            "DIRECTOR" to LoginViewModel.NavigationEvent.NavigateToDirectorDashboard,
            "TEACHER" to LoginViewModel.NavigationEvent.NavigateToTeacherDashboard
        )
        
        roles.forEach { (role, expectedNavigation) ->
            // Given
            val user = testUser.copy(role = role)
            coEvery { loginUseCase(any(), any()) } returns Result.success(user)
            
            // When
            viewModel.login("test@test.com", "password123")
            advanceUntilIdle()
            
            // Then
            Assert.assertEquals(expectedNavigation, viewModel.navigationEvent.value)
            
            // Clear for next iteration
            viewModel.clearNavigationEvent()
        }
    }
    
    @Test
    fun `clearError clears error state`() = runTest {
        // Given
        viewModel.login("", "")
        advanceUntilIdle()
        
        // When
        viewModel.clearError()
        
        // Then
        val state = viewModel.uiState.value
        Assert.assertNull(state.error)
    }
}