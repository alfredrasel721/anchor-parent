package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.model.AnchorSeedData
import com.example.model.AppRule
import com.example.model.ChildProfile
import com.example.model.ChildRequest
import com.example.model.FamilyHousehold
import com.example.model.FamilyMessage
import com.example.model.FamilyTask
import com.example.model.GuardianContact
import com.example.model.LiveSessionRequest
import com.example.model.PairedDevice
import com.example.model.PairingCode
import com.example.model.PremiumFilterConfig
import com.example.model.SafeGeofence
import com.example.model.SafetyAlert
import com.example.model.ScheduleRoutine
import com.example.model.UserProfile
import com.example.util.AndroidPlatformHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AnchorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val PAIRING_CODE_TTL_MS = 10 * 60 * 1000L // 10 minutes

        fun create(context: Context): AnchorRepository {
            val databaseId = context.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(databaseId)
            return AnchorRepository(firestore, FirebaseAuth.getInstance())
        }
    }

    suspend fun upsertUserProfile(profile: UserProfile) {
        val path = "users/${profile.uid}"
        try {
            db.document(path).set(profile).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val path = "users/$uid"
        return try {
            val snap = db.document(path).get().await()
            snap.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun upsertFamilyHousehold(family: FamilyHousehold) {
        val path = "families/${family.familyId}"
        try {
            db.document(path).set(family).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getFamilyHousehold(familyId: String): FamilyHousehold? {
        val path = "families/$familyId"
        return try {
            val snap = db.document(path).get().await()
            snap.toObject(FamilyHousehold::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun upsertChildProfile(child: ChildProfile) {
        val path = "families/${child.familyId}/children/${child.childId}"
        try {
            db.document(path).set(child).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getChildren(familyId: String): List<ChildProfile> {
        val path = "families/$familyId/children"
        return try {
            val snap = db.collection(path).get().await()
            snap.toObjects(ChildProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    // ===============================================================
    // Secure Parent-Child Pairing Operations
    // ===============================================================
    suspend fun createPairingCode(
        familyId: String,
        ownerUid: String,
        childId: String,
        childName: String
    ): PairingCode {
        require(auth.currentUser?.uid == ownerUid) { "Sign in as the family owner before generating a pairing code." }
        val data = AnchorWorkerApi.post(
            auth,
            "pairing/create",
            mapOf(
                "familyId" to familyId,
                "childId" to childId,
                "childName" to childName
            )
        )
        return PairingCode(
            id = data.optString("codeId"),
            codeId = data.optString("codeId"),
            familyId = data.optString("familyId", familyId),
            ownerUid = data.optString("ownerUid", ownerUid),
            childId = data.optString("childId", childId),
            childName = data.optString("childName", childName),
            code = data.optString("code"),
            expiresAtEpochMs = data.optLong("expiresAtEpochMs"),
            status = data.optString("status", "ACTIVE"),
            revoked = false,
            attemptCount = 0,
            maxAttempts = 5
        )
    }

    suspend fun revokePairingCode(familyId: String, codeId: String) {
        require(auth.currentUser != null) { "Sign in before revoking a pairing code." }
        AnchorWorkerApi.post(auth, "pairing/revoke", mapOf("familyId" to familyId, "codeId" to codeId))
    }

    suspend fun unpairChildDevice(familyId: String, deviceUid: String) {
        require(auth.currentUser != null) { "Sign in before unpairing a Child device." }
        require(deviceUid.isNotBlank()) { "A Child device ID is required." }
        AnchorWorkerApi.post(auth, "pairing/revoke", mapOf("familyId" to familyId, "deviceUid" to deviceUid))
    }

    suspend fun subscribeToSosNotifications(familyId: String) {
        require(auth.currentUser != null) { "Sign in before registering SOS notifications." }
        val response = AnchorWorkerApi.post(auth, "notifications/topic", mapOf("familyId" to familyId))
        val topic = response.optString("topic")
        require(topic.isNotBlank()) { "SOS notification registration returned an empty topic." }
        FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
    }

    suspend fun unsubscribeFromSosNotifications(familyId: String) {
        if (auth.currentUser == null || familyId.isBlank()) return
        val response = AnchorWorkerApi.post(auth, "notifications/topic", mapOf("familyId" to familyId))
        val topic = response.optString("topic")
        if (topic.isNotBlank()) FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
    }

    /**
     * Pairing documents are server-managed. Keeping this method prevents old callers
     * from silently writing raw pairing secrets to Firestore.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun upsertPairingCode(pairingCode: PairingCode) {
        throw UnsupportedOperationException("Pairing codes can only be created by the trusted pairing service.")
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun redeemPairingCode(
        familyId: String,
        codeId: String,
        enteredCode: String,
        childUid: String = auth.currentUser?.uid ?: "",
        deviceName: String = "Android Child Device",
        deviceModel: String = "Android",
        osVersion: String = "Android"
    ): PairedDevice {
        require(auth.currentUser?.uid == childUid && childUid.isNotBlank()) {
            "Sign in on the Child device before redeeming a pairing code."
        }
        val digits = AndroidPlatformHelper.normalizePairingCode(enteredCode)
            ?: throw IllegalArgumentException("Pairing code must contain six digits.")
        val response = functions.getHttpsCallable("redeemPairingCode")
            .call(
                mapOf(
                    "code" to digits,
                    "deviceName" to deviceName,
                    "deviceModel" to deviceModel,
                    "osVersion" to osVersion
                )
            ).await()
        val data = response.data as? Map<*, *>
            ?: throw IllegalStateException("Pairing service returned an invalid response.")
        return PairedDevice(
            id = data["deviceId"] as? String ?: childUid,
            deviceId = data["deviceId"] as? String ?: childUid,
            childUid = data["deviceId"] as? String ?: childUid,
            familyId = data["familyId"] as? String ?: familyId,
            ownerUid = data["ownerUid"] as? String ?: "",
            childId = data["childId"] as? String ?: "",
            childName = data["childName"] as? String ?: "",
            codeId = data["codeId"] as? String ?: codeId,
            redeemedCode = "",
            deviceName = deviceName,
            deviceModel = deviceModel,
            osVersion = osVersion,
            status = "PAIRED",
            pairedAtEpochMs = (data["pairedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            lastHeartbeatEpochMs = System.currentTimeMillis()
        )
    }

    suspend fun getPairedDevices(familyId: String): List<PairedDevice> {
        val path = "families/$familyId/pairedDevices"
        return try {
            val snap = db.collection(path).get().await()
            snap.toObjects(PairedDevice::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    // ===============================================================
    // Parental Policy & Family Subcollection Operations
    // ===============================================================
    suspend fun upsertAppRule(rule: AppRule) {
        val path = "families/${rule.familyId}/appRules/${rule.ruleId}"
        try {
            db.document(path).set(rule).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getAppRules(familyId: String): List<AppRule> {
        val path = "families/$familyId/appRules"
        return try {
            db.collection(path).get().await().toObjects(AppRule::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertScheduleRoutine(schedule: ScheduleRoutine) {
        val path = "families/${schedule.familyId}/schedules/${schedule.scheduleId}"
        try {
            db.document(path).set(schedule).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getSchedules(familyId: String): List<ScheduleRoutine> {
        val path = "families/$familyId/schedules"
        return try {
            db.collection(path).get().await().toObjects(ScheduleRoutine::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertSafeGeofence(geofence: SafeGeofence) {
        val path = "families/${geofence.familyId}/geofences/${geofence.geofenceId}"
        try {
            db.document(path).set(geofence).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getGeofences(familyId: String): List<SafeGeofence> {
        val path = "families/$familyId/geofences"
        return try {
            db.collection(path).get().await().toObjects(SafeGeofence::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun deleteSafeGeofence(familyId: String, geofenceId: String) {
        val path = "families/$familyId/geofences/$geofenceId"
        try {
            db.document(path).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
        }
    }

    suspend fun upsertSafetyAlert(alert: SafetyAlert) {
        val path = "families/${alert.familyId}/alerts/${alert.alertId}"
        try {
            db.document(path).set(alert).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getAlerts(familyId: String): List<SafetyAlert> {
        val path = "families/$familyId/alerts"
        return try {
            db.collection(path).get().await().toObjects(SafetyAlert::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertChildRequest(request: ChildRequest) {
        val path = "families/${request.familyId}/requests/${request.requestId}"
        try {
            db.document(path).set(request).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getRequests(familyId: String): List<ChildRequest> {
        val path = "families/$familyId/requests"
        return try {
            db.collection(path).get().await().toObjects(ChildRequest::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertFamilyMessage(message: FamilyMessage) {
        val path = "families/${message.familyId}/messages/${message.messageId}"
        try {
            db.document(path).set(message).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getMessages(familyId: String): List<FamilyMessage> {
        val path = "families/$familyId/messages"
        return try {
            db.collection(path).get().await().toObjects(FamilyMessage::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertFamilyTask(task: FamilyTask) {
        val path = "families/${task.familyId}/tasks/${task.taskId}"
        try {
            db.document(path).set(task).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getTasks(familyId: String): List<FamilyTask> {
        val path = "families/$familyId/tasks"
        return try {
            db.collection(path).get().await().toObjects(FamilyTask::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertGuardianContact(guardian: GuardianContact) {
        val path = "families/${guardian.familyId}/guardians/${guardian.guardianId}"
        try {
            db.document(path).set(guardian).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getGuardians(familyId: String): List<GuardianContact> {
        val path = "families/$familyId/guardians"
        return try {
            db.collection(path).get().await().toObjects(GuardianContact::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertPremiumConfig(config: PremiumFilterConfig) {
        val path = "families/${config.familyId}/premiumConfigs/${config.configId}"
        try {
            db.document(path).set(config).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertLiveSessionRequest(session: LiveSessionRequest) {
        val path = "families/${session.familyId}/liveSessions/${session.sessionId}"
        try {
            db.document(path).set(session).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    // ===============================================================
    // Paired Child Companion Telemetry & Sync Operations
    // ===============================================================
    suspend fun submitChildTelemetryUpdate(
        familyId: String,
        childId: String,
        usedMinutesToday: Int,
        batteryLevel: Int,
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        lastSyncLabel: String = "Synced from Child Device just now"
    ) {
        val path = "families/$familyId/children/$childId"
        try {
            db.document(path).update(
                mapOf(
                    "usedMinutesToday" to usedMinutesToday,
                    "batteryLevel" to batteryLevel,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "locationLabel" to locationLabel,
                    "lastSyncLabel" to lastSyncLabel,
                    "syncStatus" to "ENFORCED"
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun submitChildTaskCompletion(familyId: String, taskId: String) {
        val path = "families/$familyId/tasks/$taskId"
        try {
            db.document(path).update(
                mapOf(
                    "completedByChild" to true,
                    "status" to "READY_FOR_REVIEW"
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun deleteFamilyHouseholdCascade(familyId: String, uid: String) {
        val familyPath = "families/$familyId"
        try {
            db.document(familyPath).delete().await()
            db.document("users/$uid").delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, familyPath)
        }
    }

    // ===============================================================
    // Real-Time Firestore Listeners (addSnapshotListener)
    // ===============================================================
    fun observeChildren(familyId: String): Flow<List<ChildProfile>> = callbackFlow {
        val path = "families/$familyId/children"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(ChildProfile::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observePairingCodes(familyId: String): Flow<List<PairingCode>> = callbackFlow {
        val path = "families/$familyId/pairingCodes"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(PairingCode::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observePairedDevices(familyId: String): Flow<List<PairedDevice>> = callbackFlow {
        val path = "families/$familyId/pairedDevices"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(PairedDevice::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeAppRules(familyId: String): Flow<List<AppRule>> = callbackFlow {
        val path = "families/$familyId/appRules"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(AppRule::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeSchedules(familyId: String): Flow<List<ScheduleRoutine>> = callbackFlow {
        val path = "families/$familyId/schedules"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(ScheduleRoutine::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeGeofences(familyId: String): Flow<List<SafeGeofence>> = callbackFlow {
        val path = "families/$familyId/geofences"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(SafeGeofence::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeAlerts(familyId: String): Flow<List<SafetyAlert>> = callbackFlow {
        val path = "families/$familyId/alerts"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(SafetyAlert::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeRequests(familyId: String): Flow<List<ChildRequest>> = callbackFlow {
        val path = "families/$familyId/requests"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(ChildRequest::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeMessages(familyId: String): Flow<List<FamilyMessage>> = callbackFlow {
        val path = "families/$familyId/messages"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(FamilyMessage::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeTasks(familyId: String): Flow<List<FamilyTask>> = callbackFlow {
        val path = "families/$familyId/tasks"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(FamilyTask::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun bootstrapHouseholdIfNeeded(
        uid: String,
        displayName: String,
        email: String
    ): FamilyHousehold {
        val familyId = "fam_$uid"
        val existingUser = getUserProfile(uid)
        if (existingUser == null) {
            upsertUserProfile(
                UserProfile(
                    id = uid,
                    uid = uid,
                    displayName = displayName.ifBlank { "Sarah Jenkins" },
                    email = email.ifBlank { "sarah.jenkins@family.org" },
                    role = "PRIMARY_GUARDIAN",
                    familyId = familyId,
                    notificationCadence = "ALL_EVENTS"
                )
            )
        }
        val existingFamily = getFamilyHousehold(familyId)
        if (existingFamily != null) {
            return existingFamily
        }

        val seededFamily = AnchorSeedData.defaultFamily(uid, familyId)
        upsertFamilyHousehold(seededFamily)
        AnchorSeedData.defaultChildren(uid, familyId).forEach { upsertChildProfile(it) }
        AnchorSeedData.defaultAppRules(uid, familyId, "child_leo").forEach { upsertAppRule(it) }
        AnchorSeedData.defaultSchedules(uid, familyId, "child_leo").forEach { upsertScheduleRoutine(it) }
        AnchorSeedData.defaultGeofences(uid, familyId, "child_leo").forEach { upsertSafeGeofence(it) }
        AnchorSeedData.defaultAlerts(uid, familyId).forEach { upsertSafetyAlert(it) }
        AnchorSeedData.defaultRequests(uid, familyId).forEach { upsertChildRequest(it) }
        AnchorSeedData.defaultMessages(uid, familyId).forEach { upsertFamilyMessage(it) }
        AnchorSeedData.defaultTasks(uid, familyId).forEach { upsertFamilyTask(it) }
        AnchorSeedData.defaultGuardians(uid, familyId).forEach { upsertGuardianContact(it) }
        upsertPremiumConfig(AnchorSeedData.defaultPremiumConfig(uid, familyId, "child_leo"))
        return seededFamily
    }
}
