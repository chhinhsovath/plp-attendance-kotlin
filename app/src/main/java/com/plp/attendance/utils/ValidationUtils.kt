package com.plp.attendance.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.isNotBlank() && 
               phoneNumber.length >= 8 && 
               phoneNumber.length <= 15 &&
               phoneNumber.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
    }
    
    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && 
               name.trim().length >= 2 && 
               name.trim().length <= 50
    }
    
    fun isValidReason(reason: String): Boolean {
        return reason.isNotBlank() && 
               reason.trim().length >= 5 && 
               reason.trim().length <= 500
    }
    
    fun isValidDateRange(startDate: Long, endDate: Long): Boolean {
        return startDate > 0 && endDate > 0 && endDate >= startDate
    }
    
    fun isValidLocation(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
    
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("[<>\"'&]"), "") // Remove potentially dangerous characters
            .take(1000) // Limit length
    }
    
    fun getEmailError(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !isValidEmail(email) -> "Please enter a valid email address"
            else -> null
        }
    }
    
    fun getPasswordError(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            !isValidPassword(password) -> "Password must be at least 6 characters"
            else -> null
        }
    }
    
    fun getNameError(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            !isValidName(name) -> "Name must be between 2 and 50 characters"
            else -> null
        }
    }
    
    fun getReasonError(reason: String): String? {
        return when {
            reason.isBlank() -> "Reason is required"
            !isValidReason(reason) -> "Reason must be between 5 and 500 characters"
            else -> null
        }
    }
    
    fun getDateRangeError(startDate: Long, endDate: Long): String? {
        return when {
            startDate <= 0 -> "Start date is required"
            endDate <= 0 -> "End date is required"
            !isValidDateRange(startDate, endDate) -> "End date must be after start date"
            else -> null
        }
    }
}