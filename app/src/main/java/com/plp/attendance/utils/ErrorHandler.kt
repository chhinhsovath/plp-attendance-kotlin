package com.plp.attendance.utils

import android.content.Context
import com.plp.attendance.R
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {
    
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> getHttpErrorMessage(throwable)
            is IOException -> getNetworkErrorMessage(throwable)
            is SecurityException -> getSecurityErrorMessage(throwable)
            is IllegalStateException -> throwable.message ?: "Invalid operation"
            is IllegalArgumentException -> throwable.message ?: "Invalid input"
            else -> throwable.message ?: "An unexpected error occurred"
        }
    }
    
    private fun getHttpErrorMessage(exception: HttpException): String {
        return when (exception.code()) {
            400 -> "Bad request. Please check your input."
            401 -> "Authentication failed. Please login again."
            403 -> "Access denied. You don't have permission for this action."
            404 -> "Resource not found. Please try again later."
            408 -> "Request timeout. Please check your connection and try again."
            409 -> "Conflict. The resource already exists or is in use."
            422 -> "Invalid data. Please check your input and try again."
            429 -> "សំណើច្រើនពេក. Please wait a moment and try again."
            500 -> "Server error. Please try again later."
            502 -> "Bad gateway. Please try again later."
            503 -> "Service unavailable. Please try again later."
            504 -> "Gateway timeout. Please try again later."
            else -> "Network error (${exception.code()}). Please try again."
        }
    }
    
    private fun getNetworkErrorMessage(exception: IOException): String {
        return when (exception) {
            is ConnectException -> "Unable to connect to server. Please check your internet connection."
            is SocketTimeoutException -> "Connection timeout. Please check your internet connection and try again."
            is UnknownHostException -> "Unable to reach server. Please check your internet connection."
            else -> "Network error. Please check your connection and try again."
        }
    }
    
    private fun getSecurityErrorMessage(exception: SecurityException): String {
        return when {
            exception.message?.contains("location", ignoreCase = true) == true -> 
                "Location permission is required for attendance tracking."
            exception.message?.contains("camera", ignoreCase = true) == true -> 
                "Camera permission is required to take attendance photos."
            exception.message?.contains("storage", ignoreCase = true) == true -> 
                "Storage permission is required to save files."
            else -> "Permission denied. Please grant the required permissions."
        }
    }
    
    fun getErrorType(throwable: Throwable): ErrorType {
        return when (throwable) {
            is HttpException -> when (throwable.code()) {
                401 -> ErrorType.AUTHENTICATION
                403 -> ErrorType.AUTHORIZATION
                404 -> ErrorType.NOT_FOUND
                408, 504 -> ErrorType.TIMEOUT
                429 -> ErrorType.RATE_LIMIT
                in 500..599 -> ErrorType.SERVER
                else -> ErrorType.NETWORK
            }
            is IOException -> ErrorType.NETWORK
            is SecurityException -> ErrorType.PERMISSION
            is IllegalStateException -> ErrorType.VALIDATION
            is IllegalArgumentException -> ErrorType.VALIDATION
            else -> ErrorType.UNKNOWN
        }
    }
    
    fun shouldRetry(throwable: Throwable): Boolean {
        return when (getErrorType(throwable)) {
            ErrorType.NETWORK, ErrorType.TIMEOUT, ErrorType.SERVER -> true
            ErrorType.RATE_LIMIT -> false // Should implement exponential backoff
            else -> false
        }
    }
    
    fun getRetryDelay(throwable: Throwable, attemptCount: Int): Long {
        return when (getErrorType(throwable)) {
            ErrorType.NETWORK, ErrorType.TIMEOUT -> minOf(1000L * (1 shl attemptCount), 30000L)
            ErrorType.SERVER -> minOf(5000L * (1 shl attemptCount), 60000L)
            ErrorType.RATE_LIMIT -> minOf(10000L * (1 shl attemptCount), 120000L)
            else -> 0L
        }
    }
}

enum class ErrorType {
    NETWORK,
    AUTHENTICATION,
    AUTHORIZATION,
    NOT_FOUND,
    TIMEOUT,
    RATE_LIMIT,
    SERVER,
    PERMISSION,
    VALIDATION,
    UNKNOWN
}