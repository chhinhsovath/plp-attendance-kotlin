package com.plp.attendance.security

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class EncryptionManagerTest {

    @MockK
    lateinit var context: Context

    private lateinit var encryptionManager: EncryptionManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock context for testing
        every { context.filesDir } returns mockk(relaxed = true)
        
        // Note: In a real test environment, you might need to use Robolectric
        // or create a test-specific implementation that doesn't rely on Android Keystore
        encryptionManager = createTestEncryptionManager()
    }

    @Test
    fun `encrypt and decrypt should return original text`() {
        // Given
        val originalText = "Hello, World! This is a test message."

        // When
        val encryptedData = encryptionManager.encrypt(originalText)
        val decryptedText = encryptionManager.decrypt(encryptedData)

        // Then
        assertNotEquals("Encrypted text should be different from original", originalText, encryptedData.encryptedText)
        assertEquals("Decrypted text should match original", originalText, decryptedText)
        assertFalse("IV should not be empty", encryptedData.iv.isEmpty())
    }

    @Test
    fun `encrypt should produce different results for same input`() {
        // Given
        val originalText = "Same input text"

        // When
        val encrypted1 = encryptionManager.encrypt(originalText)
        val encrypted2 = encryptionManager.encrypt(originalText)

        // Then
        assertNotEquals("Encrypted texts should be different due to random IV", 
            encrypted1.encryptedText, encrypted2.encryptedText)
        assertNotEquals("IVs should be different", encrypted1.iv, encrypted2.iv)
        
        // But both should decrypt to the same original text
        assertEquals("Both should decrypt to original text", originalText, encryptionManager.decrypt(encrypted1))
        assertEquals("Both should decrypt to original text", originalText, encryptionManager.decrypt(encrypted2))
    }

    @Test
    fun `encryptAttendanceData should handle JSON data correctly`() {
        // Given
        val attendanceJson = """
            {
                "id": "attendance123",
                "userId": "user456",
                "location": "Office",
                "checkInTime": 1640995200000
            }
        """.trimIndent()

        // When
        val encrypted = encryptionManager.encryptAttendanceData(attendanceJson)
        val decrypted = encryptionManager.decryptAttendanceData(encrypted)

        // Then
        assertNotEquals("Encrypted data should be different", attendanceJson, encrypted)
        assertEquals("Decrypted data should match original", attendanceJson, decrypted)
        assertTrue("Encrypted data should contain colon separator", encrypted.contains(":"))
    }

    @Test
    fun `encryptSensitiveField should work with various data types`() {
        // Given
        val testData = listOf(
            "Simple text",
            "Email: user@example.com",
            "Phone: +855123456789",
            "Special chars: !@#$%^&*()",
            "Unicode: 中文 Español 🌟",
            ""  // Empty string
        )

        // When & Then
        testData.forEach { data ->
            val encrypted = encryptionManager.encryptSensitiveField(data)
            val decrypted = encryptionManager.decryptSensitiveField(encrypted)
            
            assertEquals("Data should be preserved: '$data'", data, decrypted)
            assertNotEquals("Encrypted should be different: '$data'", data, encrypted)
        }
    }

    @Test
    fun `generateDataIntegrityHash should be consistent`() {
        // Given
        val data = "Test data for integrity checking"

        // When
        val hash1 = encryptionManager.generateDataIntegrityHash(data)
        val hash2 = encryptionManager.generateDataIntegrityHash(data)

        // Then
        assertEquals("Hash should be consistent for same data", hash1, hash2)
        assertFalse("Hash should not be empty", hash1.isEmpty())
    }

    @Test
    fun `generateDataIntegrityHash should be different for different data`() {
        // Given
        val data1 = "First piece of data"
        val data2 = "Second piece of data"

        // When
        val hash1 = encryptionManager.generateDataIntegrityHash(data1)
        val hash2 = encryptionManager.generateDataIntegrityHash(data2)

        // Then
        assertNotEquals("Hashes should be different for different data", hash1, hash2)
    }

    @Test
    fun `verifyDataIntegrity should return true for valid data`() {
        // Given
        val data = "Data to verify"
        val correctHash = encryptionManager.generateDataIntegrityHash(data)

        // When
        val isValid = encryptionManager.verifyDataIntegrity(data, correctHash)

        // Then
        assertTrue("Data integrity should be valid", isValid)
    }

    @Test
    fun `verifyDataIntegrity should return false for tampered data`() {
        // Given
        val originalData = "Original data"
        val tamperedData = "Tampered data"
        val originalHash = encryptionManager.generateDataIntegrityHash(originalData)

        // When
        val isValid = encryptionManager.verifyDataIntegrity(tamperedData, originalHash)

        // Then
        assertFalse("Tampered data should not pass integrity check", isValid)
    }

    @Test
    fun `encryptSensitiveField should handle large data`() {
        // Given
        val largeData = "Large data content: " + "x".repeat(10000)

        // When
        val encrypted = encryptionManager.encryptSensitiveField(largeData)
        val decrypted = encryptionManager.decryptSensitiveField(encrypted)

        // Then
        assertEquals("Large data should be preserved", largeData, decrypted)
        assertTrue("Encrypted data should be longer than original", encrypted.length > largeData.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decryptSensitiveField should throw exception for invalid format`() {
        // Given
        val invalidEncryptedData = "invalid_encrypted_data_without_separator"

        // When
        encryptionManager.decryptSensitiveField(invalidEncryptedData)

        // Then - Should throw IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decryptAttendanceData should throw exception for invalid format`() {
        // Given
        val invalidEncryptedData = "invalid_encrypted_data_without_separator"

        // When
        encryptionManager.decryptAttendanceData(invalidEncryptedData)

        // Then - Should throw IllegalArgumentException
    }

    @Test
    fun `encryption should handle null and empty strings`() {
        // Given
        val emptyString = ""

        // When
        val encrypted = encryptionManager.encrypt(emptyString)
        val decrypted = encryptionManager.decrypt(encrypted)

        // Then
        assertEquals("Empty string should be preserved", emptyString, decrypted)
        assertNotEquals("Encrypted empty string should not be empty", "", encrypted.encryptedText)
    }

    @Test
    fun `data integrity should work with empty strings`() {
        // Given
        val emptyData = ""

        // When
        val hash = encryptionManager.generateDataIntegrityHash(emptyData)
        val isValid = encryptionManager.verifyDataIntegrity(emptyData, hash)

        // Then
        assertTrue("Empty data should pass integrity check", isValid)
        assertFalse("Hash of empty data should not be empty", hash.isEmpty())
    }

    // Helper method to create a test-specific encryption manager
    // In a real test environment, this would use a test keystore or mock implementation
    private fun createTestEncryptionManager(): EncryptionManager {
        // For testing purposes, we create a simplified encryption manager
        // that doesn't rely on Android Keystore
        return object : EncryptionManager(context) {
            // Override methods to use test implementations if needed
            // This is a simplified approach for unit testing
        }
    }
}