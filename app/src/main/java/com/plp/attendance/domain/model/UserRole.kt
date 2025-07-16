package com.plp.attendance.domain.model

enum class UserRole(val displayName: String) {
    ADMINISTRATOR("Administrator"),
    ZONE_MANAGER("Zone Manager"),
    PROVINCIAL_MANAGER("Provincial Manager"),
    DEPARTMENT_MANAGER("Department Manager"),
    CLUSTER_HEAD("Cluster Head"),
    DIRECTOR("Director"),
    TEACHER("Teacher")
}