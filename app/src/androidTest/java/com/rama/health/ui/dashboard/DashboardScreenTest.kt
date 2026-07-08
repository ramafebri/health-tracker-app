package com.rama.health.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.rama.health.ui.theme.HealthTrackerAppTheme
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionsNotGranted_showsPermissionRequestCard() {
        composeTestRule.setContent {
            HealthTrackerAppTheme {
                DashboardContent(
                    uiState = DashboardUiState(
                        todaySteps = 1000,
                        dailyGoal = 10000,
                        hasActivityRecognitionPermission = false,
                        hasNotificationPermission = false,
                    ),
                    onGrantPermissionClick = {},
                    onGoalChanged = {},
                    onNavigateToHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Grant Permission").assertIsDisplayed()
    }

    @Test
    fun permissionsGranted_hidesPermissionRequestCardAndShowsStepCount() {
        composeTestRule.setContent {
            HealthTrackerAppTheme {
                DashboardContent(
                    uiState = DashboardUiState(
                        todaySteps = 6543,
                        dailyGoal = 10000,
                        hasActivityRecognitionPermission = true,
                        hasNotificationPermission = true,
                    ),
                    onGrantPermissionClick = {},
                    onGoalChanged = {},
                    onNavigateToHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Grant Permission").assertDoesNotExist()
        composeTestRule.onNodeWithText("6543", substring = true).assertIsDisplayed()
    }
}
