package com.plp.attendance.data.remote.interceptors

import com.plp.attendance.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip auth for login and registration endpoints
        val path = originalRequest.url.encodedPath
        if (path.contains("/auth/login") || 
            path.contains("/auth/register") ||
            path.contains("/auth/forgot-password")) {
            return chain.proceed(originalRequest)
        }

        // Get token from session manager
        val token = runBlocking {
            sessionManager.getAuthTokenSuspend()
        }

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}