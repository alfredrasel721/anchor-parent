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
    fun parentApp_displaysWelcomePairsDeviceNavigatesToDashboardAndUnlocksPremium() {
        // 1. Verify P-01 Welcome & Sign In screen loads with Anchor Parent branding
        composeTestRule.onNodeWithText("Welcome & Sign In").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()

        // 2. Open P-03 Pairing Screen from P-01 and test pairing code revocation & regeneration
        composeTestRule.onNodeWithTag("quick_pair_child_button")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Add Child & Pair Device").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pairing_code_display")
            .performScrollTo()
            .assertIsDisplayed()

        // Generate a local demo code first; only an ACTIVE code can be revoked.
        composeTestRule.onNodeWithTag("refresh_pairing_code_btn")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("revoke_pairing_code_btn")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("refresh_pairing_code_btn")
            .performScrollTo()
            .performClick()

        // 3. Proceed from P-03 Pairing Screen to P-04 Family Dashboard
        composeTestRule.onNodeWithTag("go_to_dashboard_from_pairing_btn")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Family Dashboard").assertIsDisplayed()

        // 4. Navigate from Dashboard Command Center to P-16 Web & Content Filters (Premium)
        composeTestRule.onNodeWithTag("quick_link_P-16")
            .performScrollTo()
            .performClick()

        // 5. Verify Free tier shows the Premium Paywall Banner on P-16
        composeTestRule.onNodeWithTag("premium_paywall_banner")
            .performScrollTo()
            .assertIsDisplayed()

        // 6. Unlock 14-Day Free Trial and verify Premium becomes active
        composeTestRule.onNodeWithTag("instant_unlock_premium_trial_btn")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("PREMIUM").assertIsDisplayed()
    }
}
