package com.rama.health.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.rama.health.MainActivity
import com.rama.health.ui.theme.HealthTrackerAppTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class NavGraphTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Test
    fun navigateToHistoryAndBack() {
        composeRule.onNodeWithText("View History", substring = true).performClick()

        composeRule.onNodeWithText("Step History").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Step Counter", substring = true).assertIsDisplayed()
    }

    @Test
    fun navigateToWorkoutListAndBack() {
        composeRule.onNodeWithText("Workout Logger", substring = true).performClick()

        composeRule.onNodeWithText("Workout Logger").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Step Counter", substring = true).assertIsDisplayed()
    }
}

class WorkoutNavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigateWorkoutListBackToDashboard() {
        composeRule.setContent {
            HealthTrackerAppTheme {
                val navController = rememberNavController()
                NavGraph(navController)
                LaunchedEffect(Unit) {
                    navController.navigate(NavRoutes.WORKOUT_LIST)
                }
            }
        }

        composeRule.onNodeWithText("Workout Logger").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Step Counter", substring = true).assertIsDisplayed()
    }

    @Test
    fun navigateActiveWorkoutScreen() {
        composeRule.setContent {
            HealthTrackerAppTheme {
                val navController = rememberNavController()
                NavGraph(navController)
                LaunchedEffect(Unit) {
                    navController.navigate(NavRoutes.ACTIVE_WORKOUT)
                }
            }
        }

        composeRule.onNodeWithText("New Workout").assertIsDisplayed()
        composeRule.onNodeWithText("Start Workout").assertIsDisplayed()
    }
}
