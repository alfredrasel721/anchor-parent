package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.AnchorRepository
import com.example.model.ChildProfile
import com.example.model.FamilyHousehold
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

    private suspend fun signInTestGuardian(email: String): String {
        val result = try {
            auth.createUserWithEmailAndPassword(email, "password123").await()
        } catch (_: Exception) {
            auth.signInWithEmailAndPassword(email, "password123").await()
        }
        return result.user!!.uid
    }

    @Test
    fun bootstrapAndReadHousehold_succeedsForAuthenticatedGuardian() = runFirestoreTest {
        val uid = signInTestGuardian("sarah.guardian@family.org")
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
        signInTestGuardian("sarah.guardian2@family.org")
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
    fun updateChildProfile_succeedsForOwner() = runFirestoreTest {
        val uid = signInTestGuardian("sarah.guardian3@family.org")
        val familyId = "fam_$uid"
        repository.upsertFamilyHousehold(
            FamilyHousehold(
                id = familyId,
                familyId = familyId,
                familyName = "Jenkins Household",
                ownerUid = uid,
                subscriptionTier = "FREE",
                subscriptionActive = false
            )
        )
        val child = ChildProfile(
            id = "child_leo",
            childId = "child_leo",
            familyId = familyId,
            ownerUid = uid,
            name = "Leo",
            age = 11,
            dailyAllowanceMinutes = 180,
            usedMinutesToday = 90
        )
        repository.upsertChildProfile(child)
        val children = repository.getChildren(familyId)
        assertEquals(1, children.size)
        assertEquals(180, children.first().dailyAllowanceMinutes)
    }
}
