package com.plp.attendance.data.remote.api

import com.plp.attendance.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    
    // User CRUD operations
    @GET("api/users")
    suspend fun getUsers(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("role") role: String? = null,
        @Query("search") search: String? = null
    ): Response<UserListResponse>
    
    @GET("api/users/{id}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<UserResponse>
    
    @POST("api/users")
    suspend fun createUser(
        @Header("Authorization") token: String,
        @Body request: CreateUserRequest
    ): Response<UserResponse>
    
    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>
    
    @DELETE("api/users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<Unit>
    
    @PUT("api/users/{id}/activate")
    suspend fun activateUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<UserResponse>
    
    @PUT("api/users/{id}/deactivate")
    suspend fun deactivateUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<UserResponse>
    
    @POST("api/users/{id}/reset-password")
    suspend fun resetUserPassword(
        @Header("Authorization") token: String,
        @Path("id") userId: String,
        @Body request: ResetPasswordRequest
    ): Response<Unit>
    
    @GET("api/users/by-role/{role}")
    suspend fun getUsersByRole(
        @Header("Authorization") token: String,
        @Path("role") role: String
    ): Response<UserListResponse>
    
    @GET("api/users/by-department/{departmentId}")
    suspend fun getUsersByDepartment(
        @Header("Authorization") token: String,
        @Path("departmentId") departmentId: String
    ): Response<UserListResponse>
}