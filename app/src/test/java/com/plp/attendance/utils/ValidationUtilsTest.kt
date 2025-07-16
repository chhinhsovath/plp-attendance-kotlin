package com.plp.attendance.utils

import org.junit.Test
import org.junit.Assert.*

class ValidationUtilsTest {

    @Test
    fun `isValidEmail should return true for valid emails`() {
        // Given
        val validEmails = listOf(
            "user@example.com",
            "test.email@domain.co.uk",
            "user123@test-domain.org",
            "firstname.lastname@company.edu"
        )

        // When & Then
        validEmails.forEach { email ->
            assertTrue("$email should be valid", ValidationUtils.isValidEmail(email))
        }
    }

    @Test
    fun `isValidEmail should return false for invalid emails`() {
        // Given
        val invalidEmails = listOf(
            "",
            "invalid.email",
            "@domain.com",
            "user@",
            "user@.com",
            "user..name@domain.com",
            "user@domain.",
            "user name@domain.com"
        )

        // When & Then
        invalidEmails.forEach { email ->
            assertFalse("$email should be invalid", ValidationUtils.isValidEmail(email))
        }
    }

    @Test
    fun `isValidPhoneNumber should return true for valid phone numbers`() {
        // Given
        val validPhoneNumbers = listOf(
            "+85512345678",     // Cambodia format
            "012345678",        // Local Cambodia format
            "+1234567890",      // International format
            "0123456789"        // 10 digit format
        )

        // When & Then
        validPhoneNumbers.forEach { phoneNumber ->
            assertTrue("$phoneNumber should be valid", ValidationUtils.isValidPhoneNumber(phoneNumber))
        }
    }

    @Test
    fun `isValidPhoneNumber should return false for invalid phone numbers`() {
        // Given
        val invalidPhoneNumbers = listOf(
            "",
            "123",              // Too short
            "12345678901234567890", // Too long
            "abc123456",        // Contains letters
            "+",                // Just plus sign
            "123-456-7890"      // Contains dashes
        )

        // When & Then
        invalidPhoneNumbers.forEach { phoneNumber ->
            assertFalse("$phoneNumber should be invalid", ValidationUtils.isValidPhoneNumber(phoneNumber))
        }
    }

    @Test
    fun `isValidPassword should return true for strong passwords`() {
        // Given
        val strongPasswords = listOf(
            "Password123!",
            "MySecure@Pass1",
            "ComplexP@ssw0rd",
            "Str0ng!Password"
        )

        // When & Then
        strongPasswords.forEach { password ->
            assertTrue("$password should be strong", ValidationUtils.isValidPassword(password))
        }
    }

    @Test
    fun `isValidPassword should return false for weak passwords`() {
        // Given
        val weakPasswords = listOf(
            "",                 // Empty
            "password",         // No uppercase, numbers, or special chars
            "PASSWORD",         // No lowercase, numbers, or special chars
            "12345678",         // No letters or special chars
            "Pass123",          // No special characters
            "Pass!",            // Too short
            "password123"       // No uppercase or special chars
        )

        // When & Then
        weakPasswords.forEach { password ->
            assertFalse("$password should be weak", ValidationUtils.isValidPassword(password))
        }
    }

    @Test
    fun `isValidDateFormat should return true for valid date formats`() {
        // Given
        val validDates = listOf(
            "2024-01-15",
            "2024-12-31",
            "2023-02-28",
            "2024-02-29"        // Leap year
        )

        // When & Then
        validDates.forEach { date ->
            assertTrue("$date should be valid", ValidationUtils.isValidDateFormat(date))
        }
    }

    @Test
    fun `isValidDateFormat should return false for invalid date formats`() {
        // Given
        val invalidDates = listOf(
            "",
            "2024/01/15",       // Wrong separator
            "15-01-2024",       // Wrong order
            "2024-13-01",       // Invalid month
            "2024-01-32",       // Invalid day
            "2023-02-29",       // Not a leap year
            "24-01-15",         // Short year
            "invalid-date"
        )

        // When & Then
        invalidDates.forEach { date ->
            assertFalse("$date should be invalid", ValidationUtils.isValidDateFormat(date))
        }
    }

    @Test
    fun `isValidTimeFormat should return true for valid time formats`() {
        // Given
        val validTimes = listOf(
            "09:00",
            "23:59",
            "00:00",
            "12:30"
        )

        // When & Then
        validTimes.forEach { time ->
            assertTrue("$time should be valid", ValidationUtils.isValidTimeFormat(time))
        }
    }

    @Test
    fun `isValidTimeFormat should return false for invalid time formats`() {
        // Given
        val invalidTimes = listOf(
            "",
            "25:00",            // Invalid hour
            "12:60",            // Invalid minute
            "9:00",             // Missing leading zero
            "12:5",             // Missing leading zero for minute
            "12:00:00",         // Includes seconds
            "invalid-time"
        )

        // When & Then
        invalidTimes.forEach { time ->
            assertFalse("$time should be invalid", ValidationUtils.isValidTimeFormat(time))
        }
    }

    @Test
    fun `isValidGPSCoordinates should return true for valid coordinates`() {
        // Given
        val validCoordinates = listOf(
            Pair(11.5564, 104.9282),     // Phnom Penh
            Pair(0.0, 0.0),              // Equator/Prime Meridian
            Pair(-90.0, -180.0),         // South Pole/Date Line
            Pair(90.0, 180.0),           // North Pole/Date Line
            Pair(40.7128, -74.0060)      // New York
        )

        // When & Then
        validCoordinates.forEach { (lat, lng) ->
            assertTrue("($lat, $lng) should be valid", ValidationUtils.isValidGPSCoordinates(lat, lng))
        }
    }

    @Test
    fun `isValidGPSCoordinates should return false for invalid coordinates`() {
        // Given
        val invalidCoordinates = listOf(
            Pair(91.0, 0.0),             // Latitude > 90
            Pair(-91.0, 0.0),            // Latitude < -90
            Pair(0.0, 181.0),            // Longitude > 180
            Pair(0.0, -181.0),           // Longitude < -180
            Pair(Double.NaN, 0.0),       // NaN latitude
            Pair(0.0, Double.NaN),       // NaN longitude
            Pair(Double.POSITIVE_INFINITY, 0.0), // Infinite latitude
            Pair(0.0, Double.NEGATIVE_INFINITY)  // Infinite longitude
        )

        // When & Then
        invalidCoordinates.forEach { (lat, lng) ->
            assertFalse("($lat, $lng) should be invalid", ValidationUtils.isValidGPSCoordinates(lat, lng))
        }
    }

    @Test
    fun `isNotEmpty should return true for non-empty strings`() {
        // Given
        val nonEmptyStrings = listOf(
            "text",
            "a",
            "   ", // Whitespace only
            "123"
        )

        // When & Then
        nonEmptyStrings.forEach { str ->
            assertTrue("'$str' should not be empty", ValidationUtils.isNotEmpty(str))
        }
    }

    @Test
    fun `isNotEmpty should return false for empty strings`() {
        // Given
        val emptyString = ""

        // When & Then
        assertFalse("Empty string should be empty", ValidationUtils.isNotEmpty(emptyString))
    }

    @Test
    fun `isValidLength should return true for strings within range`() {
        // Given
        val text = "Hello World"
        val minLength = 5
        val maxLength = 15

        // When & Then
        assertTrue(ValidationUtils.isValidLength(text, minLength, maxLength))
    }

    @Test
    fun `isValidLength should return false for strings outside range`() {
        // Given
        val shortText = "Hi"
        val longText = "This is a very long text that exceeds the maximum length"
        val minLength = 5
        val maxLength = 15

        // When & Then
        assertFalse("Short text should be invalid", ValidationUtils.isValidLength(shortText, minLength, maxLength))
        assertFalse("Long text should be invalid", ValidationUtils.isValidLength(longText, minLength, maxLength))
    }

    @Test
    fun `isNumeric should return true for numeric strings`() {
        // Given
        val numericStrings = listOf(
            "123",
            "0",
            "123456789",
            "007"
        )

        // When & Then
        numericStrings.forEach { str ->
            assertTrue("'$str' should be numeric", ValidationUtils.isNumeric(str))
        }
    }

    @Test
    fun `isNumeric should return false for non-numeric strings`() {
        // Given
        val nonNumericStrings = listOf(
            "",
            "abc",
            "123abc",
            "12.34",     // Decimal point
            "-123",      // Negative sign
            "+123"       // Plus sign
        )

        // When & Then
        nonNumericStrings.forEach { str ->
            assertFalse("'$str' should not be numeric", ValidationUtils.isNumeric(str))
        }
    }

    @Test
    fun `sanitizeInput should remove dangerous characters`() {
        // Given
        val dangerousInput = "<script>alert('xss')</script>Test Input"
        val expected = "alert('xss')Test Input"

        // When
        val sanitized = ValidationUtils.sanitizeInput(dangerousInput)

        // Then
        assertEquals(expected, sanitized)
    }

    @Test
    fun `sanitizeInput should handle normal input correctly`() {
        // Given
        val normalInput = "Hello World 123"

        // When
        val sanitized = ValidationUtils.sanitizeInput(normalInput)

        // Then
        assertEquals(normalInput, sanitized)
    }

    @Test
    fun `isValidWorkingHours should return true for valid working hours`() {
        // Given
        val validHours = listOf(0.0, 8.0, 8.5, 12.0, 16.0, 24.0)

        // When & Then
        validHours.forEach { hours ->
            assertTrue("$hours should be valid working hours", ValidationUtils.isValidWorkingHours(hours))
        }
    }

    @Test
    fun `isValidWorkingHours should return false for invalid working hours`() {
        // Given
        val invalidHours = listOf(-1.0, 25.0, Double.NaN, Double.POSITIVE_INFINITY)

        // When & Then
        invalidHours.forEach { hours ->
            assertFalse("$hours should be invalid working hours", ValidationUtils.isValidWorkingHours(hours))
        }
    }
}