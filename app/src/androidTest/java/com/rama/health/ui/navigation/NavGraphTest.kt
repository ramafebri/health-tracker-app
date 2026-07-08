package com.rama.health.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rama.health.MainActivity
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
}
