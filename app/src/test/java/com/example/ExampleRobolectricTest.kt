package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun parentApp_displaysWelcomeAndNavigatesToDashboardAndPremium() {
        // Verify P-01 Welcome & Sign In screen loads with Anchor Parent branding
        composeTestRule.onNodeWithText("Welcome & Sign In").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()

        // Enter Guardian Workspace (P-04 Family Dashboard)
        composeTestRule.onNodeWithTag("enter_guardian_workspace_button")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Family Dashboard").assertIsDisplayed()

        // Navigate from Dashboard Command Center to P-16 Web & Content Filters (Premium)
        composeTestRule.onNodeWithTag("quick_link_P-16")
            .performScrollTo()
            .performClick()

        // Verify Free tier shows the Premium Paywall Banner on P-16
        composeTestRule.onNodeWithTag("premium_paywall_banner")
            .performScrollTo()
            .assertIsDisplayed()

        // Unlock 14-Day Free Trial and verify Premium becomes active
        composeTestRule.onNodeWithTag("instant_unlock_premium_trial_btn")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("PREMIUM").assertIsDisplayed()
    }
}
