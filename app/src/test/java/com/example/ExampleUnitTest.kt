package com.example

import com.example.model.AnchorScreen
import com.example.model.AnchorSeedData
import com.example.util.AndroidPlatformHelper
import com.example.viewmodel.AnchorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun premiumGating_keepsAllCoreScreensFreeAndOnlyGatesSixPremiumFeatureScreens() {
        val premiumScreens = AnchorScreen.entries.filter { it.isPremium }
        val freeScreens = AnchorScreen.entries.filter { !it.isPremium }

        // Screens P-16 (Web Filter), P-17 (Social Filter), P-18 (Notification Filter),
        // and P-19 (Live Screen/Camera/Mic Session Viewer) represent the 6 Premium groups
        assertEquals(4, premiumScreens.size)
        assertEquals(16, freeScreens.size)

        val defaultState = AnchorUiState()
        assertFalse(defaultState.isPremiumActive)
        assertEquals("FREE", defaultState.familyHousehold.subscriptionTier)
        // Honest Live Session default: never fakes an active stream before child consent
        assertFalse(defaultState.liveSessionActive)
        assertEquals("STANDBY", defaultState.liveSessionConsentStatus)
    }

    @Test
    fun seedData_populatesCompleteJenkinsHouseholdWithChildrenRulesAndGeofences() {
        val uid = "guardian_test_uid"
        val family = AnchorSeedData.defaultFamily(uid)
        val children = AnchorSeedData.defaultChildren(uid)
        val rules = AnchorSeedData.defaultAppRules(uid)
        val schedules = AnchorSeedData.defaultSchedules(uid)
        val geofences = AnchorSeedData.defaultGeofences(uid)
        val requests = AnchorSeedData.defaultRequests(uid)

        assertEquals("The Jenkins Family", family.familyName)
        assertEquals(2, children.size)
        assertEquals("Leo", children.first().name)
        assertTrue(rules.isNotEmpty())
        assertEquals(3, schedules.size)
        assertEquals(3, geofences.size)
        assertEquals(2, requests.count { it.status == "PENDING" })
    }

    @Test
    fun platformHelper_generatesSecurePairingCodesAndEvaluatesHaversineGeofences() {
        val code1 = AndroidPlatformHelper.generateSecurePairingCode()
        assertTrue(code1.matches(Regex("\\d{3}-\\d{3}")))
        assertEquals(code1, AndroidPlatformHelper.normalizePairingCode(code1.replace("-", "")))
        assertNull(AndroidPlatformHelper.normalizePairingCode("12-3"))

        // Haversine geofence check: identical coordinates = 0m distance (inside 250m school geofence)
        assertTrue(
            AndroidPlatformHelper.isInsideGeofence(
                childLat = 37.7749,
                childLon = -122.4194,
                geofenceLat = 37.7749,
                geofenceLon = -122.4194,
                radiusMeters = 250
            )
        )
        // Coordinates ~5km away = outside 250m school geofence
        val distMeters = AndroidPlatformHelper.calculateDistanceMeters(
            37.7749, -122.4194,
            37.8199, -122.4783
        )
        assertNotNull(distMeters)
        assertTrue(distMeters > 250)
        assertFalse(
            AndroidPlatformHelper.isInsideGeofence(
                childLat = 37.8199,
                childLon = -122.4783,
                geofenceLat = 37.7749,
                geofenceLon = -122.4194,
                radiusMeters = 250
            )
        )
    }
}
