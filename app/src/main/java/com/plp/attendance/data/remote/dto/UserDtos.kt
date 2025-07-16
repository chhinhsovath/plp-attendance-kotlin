package com.plp.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserListResponse(
    val success: Boolean,
    val data: UserListData?,
    val error: ErrorData?
)

data class UserListData(
    val users: List<UserData>,
    val pagination: PaginationData
)

data class PaginationData(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

data class CreateUserRequest(
    val email: String,
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val phoneNumber: String? = null,
    val organizationId: String? = null,
    val permissions: List<String> = emptyList()
)

data class UpdateUserRequest(
    val email: String? = null,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: String? = null,
    val phoneNumber: String? = null,
    val organizationId: String? = null,
    val permissions: List<String>? = null,
    val isActive: Boolean? = null
)

data class ResetPasswordRequest(
    val newPassword: String
)

data class BulkUserRequest(
    val userIds: List<String>
)

data class BulkOperationResponse(
    val success: Boolean,
    val data: BulkOperationData?,
    val error: ErrorData?
)

data class BulkOperationData(
    val processed: Int,
    val failed: Int,
    val errors: List<String>
)