package com.plp.attendance.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plp.attendance.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for Login Screen using Espresso and Compose Testing
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun loginScreen_displaysCorrectly() {
        // Verify login screen components are displayed
        composeTestRule.apply {
            onNodeWithTag("login_logo").assertIsDisplayed()
            onNodeWithText("Cambodia Education Attendance").assertIsDisplayed()
            onNodeWithTag("username_field").assertIsDisplayed()
            onNodeWithTag("password_field").assertIsDisplayed()
            onNodeWithText("Login").assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_emptyFields_showsError() {
        // Click login without entering credentials
        composeTestRule.apply {
            onNodeWithText("Login").performClick()
            
            // Verify error messages
            onNodeWithText("Username is required").assertIsDisplayed()
            onNodeWithText("Password is required").assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_invalidCredentials_showsError() {
        // Enter invalid credentials
        composeTestRule.apply {
            onNodeWithTag("username_field").performTextInput("invalid_user")
            onNodeWithTag("password_field").performTextInput("wrong_pass")
            onNodeWithText("Login").performClick()
            
            // Wait for API response
            waitForIdle()
            
            // Verify error message
            onNodeWithText("Invalid username or password").assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_validTeacherCredentials_navigatesToTeacherHome() {
        // Enter valid teacher credentials
        composeTestRule.apply {
            onNodeWithTag("username_field").performTextInput("teacher001")
            onNodeWithTag("password_field").performTextInput("Test@123")
            onNodeWithText("Login").performClick()
            
            // Wait for navigation
            waitForIdle()
            
            // Verify navigation to teacher home
            onNodeWithText("Teacher Dashboard").assertIsDisplayed()
            onNodeWithTag("check_in_button").assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_validDirectorCredentials_navigatesToDirectorHome() {
        // Enter valid director credentials
        composeTestRule.apply {
            onNodeWithTag("username_field").performTextInput("director001")
            onNodeWithTag("password_field").performTextInput("Test@123")
            onNodeWithText("Login").performClick()
            
            // Wait for navigation
            waitForIdle()
            
            // Verify navigation to director home
            onNodeWithText("School Dashboard").assertIsDisplayed()
            onNodeWithText("Staff Overview").assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_passwordVisibilityToggle_works() {
        // Test password visibility toggle
        composeTestRule.apply {
            onNodeWithTag("password_field").performTextInput("mypassword")
            
            // Password should be hidden initially
            onNode(hasText("mypassword") and hasImeAction(ImeAction.Done))
                .assertDoesNotExist()
            
            // Click visibility toggle
            onNodeWithTag("password_visibility_toggle").performClick()
            
            // Password should now be visible
            onNodeWithText("mypassword").assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_forgotPassword_navigatesToForgotPassword() {
        // Click forgot password link
        composeTestRule.apply {
            onNodeWithText("Forgot Password?").performClick()
            
            // Verify navigation
            onNodeWithText("Reset Password").assertIsDisplayed()
            onNodeWithText("Enter your email to reset password").assertIsDisplayed()
        }
    }
}