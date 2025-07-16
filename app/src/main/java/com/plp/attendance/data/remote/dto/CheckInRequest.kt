package com.plp.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CheckInRequest(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("address")
    val address: String? = null,
    
    @SerializedName("notes")
    val notes: String? = null
)