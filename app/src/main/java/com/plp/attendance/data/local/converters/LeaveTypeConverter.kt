package com.plp.attendance.data.local.converters

import androidx.room.TypeConverter
import com.plp.attendance.domain.model.LeaveType
import com.plp.attendance.domain.model.LeaveStatus

class LeaveTypeConverter {
    @TypeConverter
    fun fromLeaveType(leaveType: LeaveType): String {
        return leaveType.name
    }

    @TypeConverter
    fun toLeaveType(leaveTypeString: String): LeaveType {
        return LeaveType.valueOf(leaveTypeString)
    }

    @TypeConverter
    fun fromLeaveStatus(status: LeaveStatus): String {
        return status.name
    }

    @TypeConverter
    fun toLeaveStatus(statusString: String): LeaveStatus {
        return LeaveStatus.valueOf(statusString)
    }
}