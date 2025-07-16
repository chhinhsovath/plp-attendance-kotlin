package com.plp.attendance.domain.usecase.auth

import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LoginUseCaseTest {
    
    private lateinit var userRepository: UserRepository
    private lateinit var loginUseCase: LoginUseCase
    
    @Before
    fun setup() {
        userRepository = mockk()
        loginUseCase = LoginUseCase(userRepository)
    }
    
    @Test
    fun `login with valid credentials returns success`() = runTest {
        // Given
        val email = "teacher@school.edu.kh"
        val password = "password123"
        val expectedUser = User(
            id = "user-123",
            username = email,
            name = "Test Teacher",
            email = email,
            phone = "+855123456789",
            role = "TEACHER",
            department = "Primary",
            schoolId = "school-1",
            schoolName = "Demo School",
            profilePhotoPath = null,
            isActive = true
        )
        
        coEvery { userRepository.login(email, password) } returns Result.success(expectedUser)
        
        // When
        val result = loginUseCase(email, password)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        
        coVerify(exactly = 1) { userRepository.login(email, password) }
    }
    
    @Test
    fun `login with empty username returns validation error`() = runTest {
        // Given
        val email = ""
        val password = "password123"
        
        // When
        val result = loginUseCase(email, password)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { userRepository.login(any(), any()) }
    }
    
    @Test
    fun `login with empty password returns validation error`() = runTest {
        // Given
        val email = "teacher@school.edu.kh"
        val password = ""
        
        // When
        val result = loginUseCase(email, password)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Password cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { userRepository.login(any(), any()) }
    }
    
    @Test
    fun `login with blank username returns validation error`() = runTest {
        // Given
        val email = "   "
        val password = "password123"
        
        // When
        val result = loginUseCase(email, password)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { userRepository.login(any(), any()) }
    }
    
    @Test
    fun `login propagates repository error`() = runTest {
        // Given
        val email = "teacher@school.edu.kh"
        val password = "wrongpassword"
        val expectedError = Exception("Invalid credentials")
        
        coEvery { userRepository.login(email, password) } returns Result.failure(expectedError)
        
        // When
        val result = loginUseCase(email, password)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedError, result.exceptionOrNull())
        
        coVerify(exactly = 1) { userRepository.login(email, password) }
    }
}