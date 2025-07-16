package com.plp.attendance.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plp.attendance.presentation.screens.attendance.AttendanceScreen
import com.plp.attendance.presentation.theme.PLPAttendanceTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AttendanceScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun attendanceScreen_displaysCorrectly() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Attendance").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Check In").assertIsDisplayed()
    }

    @Test
    fun checkInButton_isClickable() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // When
        composeTestRule.onNodeWithContentDescription("Check In").performClick()

        // Then - Should show loading or check in confirmation
        composeTestRule.waitForIdle()
        // Additional assertions can be added based on expected behavior
    }

    @Test
    fun attendanceScreen_showsLocationInfo() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Current Location").assertIsDisplayed()
    }

    @Test
    fun attendanceScreen_showsTodayStatus() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Today's Attendance").assertIsDisplayed()
    }

    @Test
    fun checkOutButton_appearsAfterCheckIn() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // When - Simulate check in
        composeTestRule.onNodeWithContentDescription("Check In").performClick()
        composeTestRule.waitForIdle()

        // Then - Check out button should appear (this depends on the actual implementation)
        // This test would need to be adjusted based on the actual UI behavior
    }

    @Test
    fun attendanceScreen_displaysRecentAttendance() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Recent Attendance").assertIsDisplayed()
    }

    @Test
    fun attendanceScreen_handlesRefresh() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // When
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()

        // Then - Should show loading or updated content
        composeTestRule.waitForIdle()
    }

    @Test
    fun attendanceScreen_showsErrorState() {
        // This test would need mock data or dependency injection to simulate error state
        // Given an error state in the ViewModel
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Simulate error condition and verify error message is displayed
        // This would require setting up the ViewModel with error state
    }

    @Test
    fun attendanceScreen_respondsToSwipeRefresh() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // When - Perform swipe to refresh gesture
        composeTestRule.onRoot().performTouchInput {
            swipeDown()
        }

        // Then - Should trigger refresh
        composeTestRule.waitForIdle()
    }

    @Test
    fun attendanceScreen_navigatesCorrectly() {
        // Given
        var navigatedToReports = false
        
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen(
                    onNavigateToReports = { navigatedToReports = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("View Reports").performClick()

        // Then
        assert(navigatedToReports)
    }

    @Test
    fun attendanceScreen_handlesLocationPermission() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // When location permission is needed
        // This test would need to simulate permission request scenarios
        
        // Then - Should show appropriate UI for location permission
        composeTestRule.onNodeWithText("Location Permission").assertExists()
    }

    @Test
    fun attendanceScreen_displaysCorrectTime() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Then - Should display current time information
        composeTestRule.onNodeWithText("Current Time").assertIsDisplayed()
    }

    @Test
    fun attendanceScreen_handlesNetworkConnectivity() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Test would simulate network connectivity changes
        // and verify appropriate UI responses
    }

    @Test
    fun attendanceScreen_accessibilityLabels() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // Then - Verify accessibility labels are present
        composeTestRule.onNodeWithContentDescription("Check In Button").assertExists()
        composeTestRule.onNodeWithContentDescription("Current Location").assertExists()
        composeTestRule.onNodeWithContentDescription("Attendance Status").assertExists()
    }

    @Test
    fun attendanceScreen_scrollsBehavior() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // When - Scroll down to see more content
        composeTestRule.onRoot().performTouchInput {
            swipeUp()
        }

        // Then - Should be able to scroll and see more content
        composeTestRule.waitForIdle()
    }

    @Test
    fun attendanceScreen_handlesOrientationChange() {
        // Given
        composeTestRule.setContent {
            PLPAttendanceTheme {
                AttendanceScreen()
            }
        }

        // This test would simulate device rotation
        // and verify UI adapts correctly
        
        // Then - UI should adapt to new orientation
        composeTestRule.onNodeWithText("Attendance").assertIsDisplayed()
    }
}