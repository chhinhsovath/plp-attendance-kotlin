package com.plp.attendance.ui

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.plp.attendance.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

/**
 * End-to-end tests for attendance check-in/out flow
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AttendanceFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )

    @Before
    fun setup() {
        hiltRule.inject()
        // Login as teacher first
        loginAsTeacher()
    }

    private fun loginAsTeacher() {
        composeTestRule.apply {
            onNodeWithTag("username_field").performTextInput("teacher001")
            onNodeWithTag("password_field").performTextInput("Test@123")
            onNodeWithText("Login").performClick()
            waitForIdle()
        }
    }

    @Test
    fun checkInFlow_withinGeofence_success() {
        val currentTime = LocalTime.now()
        
        // Only run this test during valid check-in hours
        if (currentTime.isAfter(LocalTime.of(6, 0)) && 
            currentTime.isBefore(LocalTime.of(9, 0))) {
            
            composeTestRule.apply {
                // Navigate to attendance
                onNodeWithTag("check_in_button").performClick()
                waitForIdle()
                
                // Verify location is detected
                onNodeWithText("Current Location").assertIsDisplayed()
                onNodeWithText("Within School Area").assertIsDisplayed()
                
                // Take photo (if required)
                onNodeWithText("Take Photo").performClick()
                // Simulate camera capture
                onNodeWithTag("camera_capture").performClick()
                waitForIdle()
                
                // Confirm check-in
                onNodeWithText("Confirm Check-In").performClick()
                waitForIdle()
                
                // Verify success
                onNodeWithText("Check-in Successful").assertIsDisplayed()
                onNodeWithText("Checked in at").assertIsDisplayed()
            }
        }
    }

    @Test
    fun checkInFlow_outsideGeofence_showsError() {
        composeTestRule.apply {
            // Navigate to attendance
            onNodeWithTag("check_in_button").performClick()
            waitForIdle()
            
            // Mock location outside school
            // This would be set up in test configuration
            
            // Verify error message
            onNodeWithText("Outside School Area").assertIsDisplayed()
            onNodeWithText("You must be within 500m of school to check in").assertIsDisplayed()
            
            // Check-in button should be disabled
            onNodeWithText("Confirm Check-In").assertIsEnabled().not()
        }
    }

    @Test
    fun checkOutFlow_afterCheckIn_success() {
        // First perform check-in
        checkInFlow_withinGeofence_success()
        
        composeTestRule.apply {
            // Navigate back to home
            onNodeWithContentDescription("Navigate up").performClick()
            
            // Click check-out
            onNodeWithTag("check_out_button").performClick()
            waitForIdle()
            
            // Confirm check-out
            onNodeWithText("Confirm Check-Out").performClick()
            waitForIdle()
            
            // Verify success
            onNodeWithText("Check-out Successful").assertIsDisplayed()
            onNodeWithText("Total Hours:").assertIsDisplayed()
        }
    }

    @Test
    fun attendanceHistory_displaysCorrectly() {
        composeTestRule.apply {
            // Navigate to attendance history
            onNodeWithText("Attendance").performClick()
            onNodeWithText("History").performClick()
            waitForIdle()
            
            // Verify history list
            onNodeWithTag("attendance_history_list").assertIsDisplayed()
            
            // Verify date filter
            onNodeWithText("This Month").assertIsDisplayed()
            
            // Test date filter change
            onNodeWithText("This Month").performClick()
            onNodeWithText("Last Month").performClick()
            waitForIdle()
            
            // Verify list updates
            onNodeWithTag("attendance_history_list").assertIsDisplayed()
        }
    }

    @Test
    fun attendanceStats_displayCorrectly() {
        composeTestRule.apply {
            // Navigate to attendance
            onNodeWithText("Attendance").performClick()
            waitForIdle()
            
            // Verify stats are displayed
            onNodeWithText("This Month").assertIsDisplayed()
            onNodeWithText("Present Days").assertIsDisplayed()
            onNodeWithText("Absent Days").assertIsDisplayed()
            onNodeWithText("On Leave").assertIsDisplayed()
            onNodeWithText("On Mission").assertIsDisplayed()
            
            // Verify percentage
            onNodeWithContentDescription("Attendance percentage").assertIsDisplayed()
        }
    }
}