package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.AnchorRepository
import com.example.model.AppRule
import com.example.model.ChildProfile
import com.example.model.ChildRequest
import com.example.model.FamilyHousehold
import com.example.model.FamilyMessage
import com.example.model.FamilyTask
import com.example.model.SafetyAlert
import com.example.model.ScheduleRoutine
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
class AnchorRepositoryRuleTest {

    companion object {
        private var emulatorsConfigured = false
        private const val DATABASE_ID = "ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e"
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: AnchorRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("demo-test")
                    .setApplicationId("1:1234567890:android:abcdef")
                    .setApiKey("fake-api-key-for-emulator")
                    .build()
            )
        }

        val clearUrl = URL(
            "http://127.0.0.1:8085/emulator/v1/projects/demo-test/databases/$DATABASE_ID/documents"
        )
        (clearUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            responseCode
            disconnect()
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID)

        if (!emulatorsConfigured) {
            try {
                auth.useEmulator("127.0.0.1", 9099)
            } catch (_: IllegalStateException) {}
            try {
                db.useEmulator("127.0.0.1", 8085)
                db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            } catch (_: IllegalStateException) {}
            emulatorsConfigured = true
        }

        repository = AnchorRepository(db, auth)
    }

    @org.junit.After
    fun tearDown() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
    }

    private fun runFirestoreTest(block: suspend CoroutineScope.() -> Unit) {
        val errorRef = AtomicReference<Throwable?>(null)
        val done = AtomicBoolean(false)
        val worker = Thread {
            try {
                runBlocking { block() }
            } catch (t: Throwable) {
                errorRef.set(t)
            } finally {
                done.set(true)
            }
        }
        worker.start()
        while (!done.get()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        worker.join()
        errorRef.get()?.let { throw it }
    }

    private suspend fun signInTestUser(email: String): String {
        val result = try {
            auth.createUserWithEmailAndPassword(email, "password123").await()
        } catch (_: Exception) {
            auth.signInWithEmailAndPassword(email, "password123").await()
        }
        return result.user!!.uid
    }

    @Test
    fun bootstrapAndReadHousehold_succeedsForAuthenticatedGuardian() = runFirestoreTest {
        val uid = signInTestUser("sarah.guardian@family.org")
        val household = repository.bootstrapHouseholdIfNeeded(
            uid = uid,
            displayName = "Sarah Jenkins",
            email = "sarah.guardian@family.org"
        )
        assertEquals("fam_$uid", household.familyId)
        assertEquals("FREE", household.subscriptionTier)

        val children = repository.getChildren("fam_$uid")
        assertTrue(children.size >= 2)
        assertNotNull(children.find { it.name == "Leo" })
    }

    @Test
    fun createHousehold_failsWhenOwnerUidMismatchesAuthenticatedUser() = runFirestoreTest {
        signInTestUser("sarah.guardian2@family.org")
        val spoofedFamily = FamilyHousehold(
            id = "fam_spoofed",
            familyId = "fam_spoofed",
            familyName = "Spoofed Household",
            ownerUid = "different_uid_not_matching_auth",
            subscriptionTier = "FREE",
            subscriptionActive = false
        )
        try {
            repository.upsertFamilyHousehold(spoofedFamily)
            fail("Expected Firestore security rules to reject mismatched ownerUid")
        } catch (e: RuntimeException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED") || e.message!!.contains("error"))
        }
    }

    @Test
    fun fullEndToEndParentChildPairingAndBidirectionalSyncWorkflow() = runFirestoreTest {
        // 1. Parent signs in and creates household & child profile
        val parentUid = signInTestUser("sarah.e2e@family.org")
        val familyId = "fam_$parentUid"
        repository.upsertFamilyHousehold(
            FamilyHousehold(
                id = familyId,
                familyId = familyId,
                familyName = "The Jenkins Family",
                ownerUid = parentUid,
                subscriptionTier = "FREE",
                subscriptionActive = false
            )
        )
        val leo = ChildProfile(
            id = "child_leo",
            childId = "child_leo",
            familyId = familyId,
            ownerUid = parentUid,
            name = "Leo",
            age = 11,
            dailyAllowanceMinutes = 150,
            usedMinutesToday = 45
        )
        repository.upsertChildProfile(leo)

        // 2. Parent generates a pairing code and revokes it, then generates a fresh active code
        val firstCode = repository.createPairingCode(familyId, parentUid, "child_leo", "Leo")
        repository.revokePairingCode(familyId, firstCode.codeId)
        val activeCode = repository.createPairingCode(familyId, parentUid, "child_leo", "Leo")
        assertEquals("ACTIVE", activeCode.status)
        assertFalse(activeCode.revoked)

        // 3. Child signs in and attempts to redeem revoked code -> fails
        val childUid = signInTestUser("leo.child.e2e@family.org")
        try {
            repository.redeemPairingCode(
                familyId = familyId,
                codeId = firstCode.codeId,
                enteredCode = firstCode.code,
                childUid = childUid
            )
            fail("Expected revoked pairing code redemption to fail")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("revoked"))
        }

        // 4. Child redeems active pairing code -> succeeds and creates pairedDevices membership
        val pairedDevice = repository.redeemPairingCode(
            familyId = familyId,
            codeId = activeCode.codeId,
            enteredCode = activeCode.code,
            childUid = childUid,
            deviceName = "Leo Galaxy A54 5G"
        )
        assertEquals("PAIRED", pairedDevice.status)
        assertEquals("child_leo", pairedDevice.childId)

        // 5. Second attempt to reuse the consumed pairing code -> fails
        try {
            repository.redeemPairingCode(
                familyId = familyId,
                codeId = activeCode.codeId,
                enteredCode = activeCode.code,
                childUid = childUid
            )
            fail("Expected reused pairing code redemption to fail")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("consumed"))
        }

        // 6. Paired Child submits telemetry update (screen time + GPS location)
        repository.submitChildTelemetryUpdate(
            familyId = familyId,
            childId = "child_leo",
            usedMinutesToday = 95,
            batteryLevel = 76,
            latitude = 37.7755,
            longitude = -122.4180,
            locationLabel = "Oakridge Middle School"
        )

        // 7. Paired Child submits extra-time request, chat message, and SOS alert
        repository.upsertChildRequest(
            ChildRequest(
                id = "req_e2e_1",
                requestId = "req_e2e_1",
                familyId = familyId,
                ownerUid = parentUid,
                childId = "child_leo",
                childName = "Leo",
                requestType = "EXTRA_TIME",
                targetName = "Minecraft",
                requestedBonusMinutes = 15,
                status = "PENDING"
            )
        )
        repository.upsertFamilyMessage(
            FamilyMessage(
                id = "msg_e2e_child",
                messageId = "msg_e2e_child",
                familyId = familyId,
                ownerUid = parentUid,
                childId = "child_leo",
                senderName = "Leo",
                senderRole = "CHILD",
                text = "Finished my science project! Can I have +15m?"
            )
        )
        repository.upsertSafetyAlert(
            SafetyAlert(
                id = "alert_e2e_sos",
                alertId = "alert_e2e_sos",
                familyId = familyId,
                ownerUid = parentUid,
                childId = "child_leo",
                childName = "Leo",
                title = "Child SOS Check-In",
                description = "Picked up after soccer practice",
                category = "SOS",
                severity = "CRITICAL",
                acknowledged = false
            )
        )

        // 8. Paired Child attempts to tamper with dailyAllowanceMinutes -> rejected by Firestore rules
        try {
            repository.upsertChildProfile(leo.copy(dailyAllowanceMinutes = 900))
            fail("Expected Firestore security rules to block child from modifying dailyAllowanceMinutes")
        } catch (e: RuntimeException) {
            assertTrue(e.message!!.contains("PERMISSION_DENIED") || e.message!!.contains("error"))
        }

        // 9. Switch back to Parent session and verify Parent receives all Child updates,
        // approves the request, assigns & approves a task, and updates app rules/schedules
        signInTestUser("sarah.e2e@family.org")

        val syncedChildren = repository.getChildren(familyId)
        assertEquals(95, syncedChildren.first().usedMinutesToday)
        assertEquals(76, syncedChildren.first().batteryLevel)

        val pairedDevices = repository.getPairedDevices(familyId)
        assertEquals(1, pairedDevices.size)
        assertEquals(childUid, pairedDevices.first().childUid)

        val requests = repository.getRequests(familyId)
        assertEquals(1, requests.size)
        assertEquals("PENDING", requests.first().status)

        // Parent approves request
        repository.upsertChildRequest(
            requests.first().copy(
                status = "APPROVED",
                decisionSummary = "Approved +15m Bonus",
                decidedBy = "Sarah Jenkins"
            )
        )

        // Parent creates app rule, schedule, and shared task
        repository.upsertAppRule(
            AppRule(
                id = "rule_mc",
                ruleId = "rule_mc",
                familyId = familyId,
                ownerUid = parentUid,
                childId = "child_leo",
                packageName = "com.mojang.minecraftpe",
                appName = "Minecraft",
                category = "Games & Entertainment",
                dailyLimitMinutes = 60,
                usedMinutesToday = 30,
                status = "ENFORCED"
            )
        )
        repository.upsertScheduleRoutine(
            ScheduleRoutine(
                id = "sched_bed",
                scheduleId = "sched_bed",
                familyId = familyId,
                ownerUid = parentUid,
                childId = "child_leo",
                title = "Bedtime Routine",
                windowDescription = "8:30 PM — 7:00 AM",
                enabled = true
            )
        )
        repository.upsertFamilyTask(
            FamilyTask(
                id = "task_e2e_1",
                taskId = "task_e2e_1",
                familyId = familyId,
                ownerUid = parentUid,
                childId = "child_leo",
                title = "Read 20 pages",
                rewardSummary = "+15m Bonus Time",
                status = "IN_PROGRESS",
                completedByChild = false,
                approvedByParent = false
            )
        )

        // 10. Switch to Child, verify Child reads updated rules/schedules/requests, and marks task completed
        signInTestUser("leo.child.e2e@family.org")
        val childRules = repository.getAppRules(familyId)
        assertEquals(1, childRules.size)
        assertEquals(60, childRules.first().dailyLimitMinutes)

        val childRequests = repository.getRequests(familyId)
        assertEquals("APPROVED", childRequests.first().status)

        repository.submitChildTaskCompletion(familyId, "task_e2e_1")

        // 11. Switch to Parent and approve completed task
        signInTestUser("sarah.e2e@family.org")
        val tasks = repository.getTasks(familyId)
        assertEquals("READY_FOR_REVIEW", tasks.first().status)
        assertTrue(tasks.first().completedByChild)
    }
}
