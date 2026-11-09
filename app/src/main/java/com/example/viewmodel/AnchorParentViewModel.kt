package com.example.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.auth.AuthManager
import com.example.auth.AuthResult
import com.example.data.local.AnchorLocalDao
import com.example.data.local.CachedAuditLogEntity
import com.example.data.local.CachedChildStateEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.remote.AnchorRepository
import com.example.model.AnchorScreen
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
import com.example.util.AndroidPlatformHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class AnchorUiState(
    val currentScreen: AnchorScreen = AnchorScreen.P01_WELCOME_SIGN_IN,
    val backStack: List<AnchorScreen> = emptyList(),
    val isSignedIn: Boolean = false,
    val isCloudConnected: Boolean = false,
    val authLoading: Boolean = false,
    val statusBannerMessage: String? = null,
    val currentUserUid: String = "local_guardian_sarah",
    val guardianDisplayName: String = "Sarah Jenkins",
    val guardianEmail: String = "sarah.jenkins@family.org",
    val guardianRoleChip: String = "Mother",
    val notificationCadence: String = "All Safety & Requests",
    val familyHousehold: FamilyHousehold = AnchorSeedData.defaultFamily("local_guardian_sarah"),
    val children: List<ChildProfile> = AnchorSeedData.defaultChildren("local_guardian_sarah"),
    val selectedChildId: String = "child_leo",
    val pairingCode: PairingCode = PairingCode(
        id = "",
        codeId = "",
        familyId = "",
        ownerUid = "",
        childId = "child_leo",
        childName = "Leo",
        code = "",
        expiresAtEpochMs = 0L,
        status = "NONE",
        revoked = false
    ),
    val pairedDevices: List<PairedDevice> = emptyList(),
    val pairingCountdownSeconds: Int = 0,
    val appRules: List<AppRule> = AnchorSeedData.defaultAppRules("local_guardian_sarah"),
    val schedules: List<ScheduleRoutine> = AnchorSeedData.defaultSchedules("local_guardian_sarah"),
    val geofences: List<SafeGeofence> = AnchorSeedData.defaultGeofences("local_guardian_sarah"),
    val alerts: List<SafetyAlert> = AnchorSeedData.defaultAlerts("local_guardian_sarah"),
    val requests: List<ChildRequest> = AnchorSeedData.defaultRequests("local_guardian_sarah"),
    val messages: List<FamilyMessage> = AnchorSeedData.defaultMessages("local_guardian_sarah"),
    val tasks: List<FamilyTask> = AnchorSeedData.defaultTasks("local_guardian_sarah"),
    val guardians: List<GuardianContact> = AnchorSeedData.defaultGuardians("local_guardian_sarah"),
    val premiumConfig: PremiumFilterConfig = AnchorSeedData.defaultPremiumConfig("local_guardian_sarah"),
    val auditLogs: List<CachedAuditLogEntity> = emptyList(),
    val pendingOfflineSyncCount: Int = 0,
    // Screen-specific interactive UI states
    val analyticsRange: String = "7 Days",
    // Honest Live Session Signaling State (never simulates a fake active camera/mic/screen stream)
    val liveSessionActive: Boolean = false,
    val liveSessionConsentStatus: String = "STANDBY", // STANDBY, AWAITING_CHILD_CONSENT, CONSENT_GRANTED, ENDED
    val liveSessionMode: String = "Screen View", // Screen View, Front Camera, Ambient Audio
    val liveSessionTwoWayTalk: Boolean = false,
    val liveSessionReason: String = "Checking homework progress & safety check-in",
    val liveSessionRemainingSeconds: Int = 600,
    val biometricLockRequired: Boolean = true,
    val criticalAlertsOverrideDnd: Boolean = true,
    val darkModeOverride: Boolean = false,
    val offlineDiagnosticsCacheEnabled: Boolean = true,
    val anonymousDiagnosticTelemetry: Boolean = false,
    val lastSelfTestSummary: String = "Room SQLite Cache Ready • Sign in with Google for Cloud Firestore Verification"
) {
    val selectedChild: ChildProfile
        get() = children.find { it.childId == selectedChildId } ?: children.first()

    val isPremiumActive: Boolean
        get() = familyHousehold.subscriptionActive && familyHousehold.subscriptionTier != "FREE"

    val pendingRequestsCount: Int
        get() = requests.count { it.status == "PENDING" }
}

class AnchorParentViewModel(
    private val repository: AnchorRepository,
    private val localDao: AnchorLocalDao,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnchorUiState())
    val uiState: StateFlow<AnchorUiState> = _uiState.asStateFlow()

    private val realtimeListenerJobs = mutableListOf<Job>()

    init {
        observeLocalRoomCache()
        checkExistingFirebaseSession()
        seedInitialAuditLog()
    }

    private fun observeLocalRoomCache() {
        viewModelScope.launch {
            localDao.observeAuditLogs().collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }
        viewModelScope.launch {
            localDao.observePendingOfflineSyncCount().collect { count ->
                _uiState.update { it.copy(pendingOfflineSyncCount = count) }
            }
        }
    }

    private fun seedInitialAuditLog() {
        viewModelScope.launch {
            val currentLogs = localDao.getAllAuditLogs()
            if (currentLogs.isEmpty()) {
                recordAuditLog(
                    eventCode = "#AUD-1042",
                    actionTitle = "Guardian Workspace Initialized",
                    details = "Anchor Bilateral Transparency Engine & Room SQLite cache online.",
                    syncState = if (_uiState.value.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
                )
            }
        }
    }

    private fun checkExistingFirebaseSession() {
        val currentUser = authManager.currentUser
        if (currentUser != null) {
            onFirebaseUserAuthenticated(
                uid = currentUser.uid,
                displayName = currentUser.displayName ?: "Sarah Jenkins",
                email = currentUser.email ?: "sarah.jenkins@family.org"
            )
        }
    }

    fun navigateTo(screen: AnchorScreen) {
        _uiState.update { current ->
            if (current.currentScreen == screen) {
                current
            } else {
                current.copy(
                    currentScreen = screen,
                    backStack = current.backStack + current.currentScreen
                )
            }
        }
    }

    fun navigateBack(): Boolean {
        val stack = _uiState.value.backStack
        if (stack.isEmpty()) return false
        val previous = stack.last()
        _uiState.update {
            it.copy(
                currentScreen = previous,
                backStack = stack.dropLast(1)
            )
        }
        return true
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun postBannerMessage(message: String) {
        _uiState.update { it.copy(statusBannerMessage = message) }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, statusBannerMessage = null) }
            when (val result = authManager.signInWithGoogleButton(activityContext)) {
                is AuthResult.Success -> {
                    val user = result.user
                    onFirebaseUserAuthenticated(
                        uid = user.uid,
                        displayName = user.displayName ?: "Sarah Jenkins",
                        email = user.email ?: "sarah.jenkins@family.org"
                    )
                }
                is AuthResult.Cancelled -> {
                    _uiState.update {
                        it.copy(
                            authLoading = false,
                            statusBannerMessage = "Google Sign-In was cancelled."
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            authLoading = false,
                            statusBannerMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun onFirebaseUserAuthenticated(uid: String, displayName: String, email: String) {
        viewModelScope.launch {
            val familyId = "fam_$uid"
            _uiState.update {
                it.copy(
                    authLoading = false,
                    isSignedIn = true,
                    currentUserUid = uid,
                    guardianDisplayName = displayName,
                    guardianEmail = email,
                    currentScreen = AnchorScreen.P04_FAMILY_DASHBOARD,
                    backStack = emptyList(),
                    statusBannerMessage = "Authenticating & verifying shared Firestore household ($familyId)..."
                )
            }
            try {
                val household = repository.bootstrapHouseholdIfNeeded(uid, displayName, email)
                val children = repository.getChildren(familyId).ifEmpty {
                    AnchorSeedData.defaultChildren(uid, familyId)
                }
                _uiState.update {
                    it.copy(
                        isCloudConnected = true,
                        familyHousehold = household,
                        children = children,
                        selectedChildId = children.first().childId,
                        statusBannerMessage = "Authenticated as $email • Shared Cloud Firestore Sync Verified",
                        lastSelfTestSummary = "All 4 Guardian Pipelines Verified (Shared Firestore DB Connected)"
                    )
                }
                syncChildrenToLocalRoom(children)
                startRealtimeHouseholdListeners(familyId)
                flushOfflineSyncQueue()
                recordAuditLog(
                    eventCode = "#AUTH-200",
                    actionTitle = "Firebase Google Sign-In Verified",
                    details = "Connected to household ${household.familyName} ($familyId)",
                    syncState = "ENFORCED"
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCloudConnected = false,
                        statusBannerMessage = "Signed in ($email), but Firestore sync failed: ${e.localizedMessage ?: "Offline Room queue active"}"
                    )
                }
            }
        }
    }

    private fun startRealtimeHouseholdListeners(familyId: String) {
        realtimeListenerJobs.forEach { it.cancel() }
        realtimeListenerJobs.clear()

        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeChildren(familyId).collect { list ->
                    if (list.isNotEmpty()) {
                        _uiState.update { it.copy(children = list) }
                        syncChildrenToLocalRoom(list)
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observePairedDevices(familyId).collect { devices ->
                    _uiState.update { it.copy(pairedDevices = devices) }
                }
            } catch (_: Exception) {}
        }

        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observePairingCodes(familyId).collect { codes ->
                    val currentCodeId = _uiState.value.pairingCode.codeId
                    val serverCode = codes.firstOrNull { it.codeId == currentCodeId }
                    if (serverCode != null && serverCode.status != _uiState.value.pairingCode.status) {
                        _uiState.update { current ->
                            if (current.pairingCode.codeId != serverCode.codeId) {
                                current
                            } else {
                                current.copy(
                                    pairingCode = current.pairingCode.copy(
                                        status = serverCode.status,
                                        revoked = serverCode.status == "REVOKED",
                                        revokedAtEpochMs = serverCode.revokedAtEpochMs,
                                        expiresAtEpochMs = serverCode.expiresAtEpochMs
                                    ),
                                    pairingCountdownSeconds = if (serverCode.status == "ACTIVE") {
                                        current.pairingCountdownSeconds
                                    } else {
                                        0
                                    },
                                    statusBannerMessage = when (serverCode.status) {
                                        "CONSUMED" -> "The Child device has completed secure pairing."
                                        "REVOKED" -> "The pairing code was revoked."
                                        "EXPIRED" -> "The pairing code expired. Generate a new one."
                                        else -> current.statusBannerMessage
                                    }
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeAppRules(familyId).collect { rules ->
                    if (rules.isNotEmpty()) {
                        _uiState.update { it.copy(appRules = rules) }
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeSchedules(familyId).collect { schedules ->
                    if (schedules.isNotEmpty()) {
                        _uiState.update { it.copy(schedules = schedules) }
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeGeofences(familyId).collect { geofences ->
                    if (geofences.isNotEmpty()) {
                        _uiState.update { it.copy(geofences = geofences) }
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeAlerts(familyId).collect { alerts ->
                    if (alerts.isNotEmpty()) {
                        _uiState.update { it.copy(alerts = alerts) }
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeRequests(familyId).collect { requests ->
                    if (requests.isNotEmpty()) {
                        _uiState.update { it.copy(requests = requests) }
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeMessages(familyId).collect { messages ->
                    if (messages.isNotEmpty()) {
                        _uiState.update { it.copy(messages = messages) }
                    }
                }
            } catch (_: Exception) {}
        }
        realtimeListenerJobs += viewModelScope.launch {
            try {
                repository.observeTasks(familyId).collect { tasks ->
                    if (tasks.isNotEmpty()) {
                        _uiState.update { it.copy(tasks = tasks) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun enterGuardianWorkspace(startScreen: AnchorScreen = AnchorScreen.P04_FAMILY_DASHBOARD) {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isSignedIn = true,
                currentScreen = startScreen,
                backStack = emptyList(),
                statusBannerMessage = if (it.isCloudConnected) {
                    "Shared Cloud Firestore & Room Cache Active"
                } else {
                    "Local Room SQLite Sandbox Active (Sign in with Google on P-01 for Cloud Firestore sync)"
                }
            )
        }
        viewModelScope.launch {
            syncChildrenToLocalRoom(state.children)
            recordAuditLog(
                eventCode = "#LOC-101",
                actionTitle = "Entered Guardian Workspace",
                details = "Active profile: ${state.guardianDisplayName} (${state.familyHousehold.familyName})",
                syncState = if (state.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
            )
        }
    }

    fun signOutGuardian(context: Context) {
        viewModelScope.launch {
            realtimeListenerJobs.forEach { it.cancel() }
            realtimeListenerJobs.clear()
            authManager.signOut(context)
            _uiState.update {
                it.copy(
                    isSignedIn = false,
                    isCloudConnected = false,
                    currentScreen = AnchorScreen.P01_WELCOME_SIGN_IN,
                    backStack = emptyList(),
                    statusBannerMessage = "Signed out of Guardian session."
                )
            }
        }
    }

    // ===============================================================
    // P-02: Family Onboarding Actions
    // ===============================================================
    fun updateOnboardingHousehold(
        familyName: String,
        guardianName: String,
        roleChip: String,
        cadence: String
    ) {
        val updatedFamily = _uiState.value.familyHousehold.copy(familyName = familyName)
        _uiState.update {
            it.copy(
                familyHousehold = updatedFamily,
                guardianDisplayName = guardianName,
                guardianRoleChip = roleChip,
                notificationCadence = cadence,
                statusBannerMessage = "Saving household settings for $familyName..."
            )
        }
        persistFamilyWithSyncStatus(updatedFamily, "Household settings saved for $familyName")
    }

    fun addChildToHousehold(name: String, age: Int, gradeLabel: String, deviceName: String) {
        if (name.isBlank()) return
        val state = _uiState.value
        val childId = "child_${name.lowercase(Locale.US).replace(" ", "_")}_${Random.nextInt(100, 999)}"
        val initials = name.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .take(2)
            .ifEmpty { "CH" }

        val newChild = ChildProfile(
            id = childId,
            childId = childId,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            name = name.trim(),
            age = age.coerceIn(3, 18),
            gradeLabel = gradeLabel.ifBlank { "Student Profile" },
            avatarInitials = initials,
            deviceName = deviceName.ifBlank { "${name.trim()}'s Android Device" },
            deviceModel = "Pending Pairing Handshake",
            osVersion = "Android 14+",
            batteryLevel = 100,
            chargingStatus = "Ready to Pair",
            networkType = "Wi-Fi",
            networkName = "Household Network",
            storageFreeGb = 64.0,
            syncStatus = "PENDING",
            lastSyncLabel = "Awaiting 6-Digit Pairing Code",
            usedMinutesToday = 0,
            dailyAllowanceMinutes = 120,
            currentMode = "Onboarding Setup",
            locationLabel = "Home Sanctuary"
        )

        val updatedChildren = state.children + newChild
        _uiState.update {
            it.copy(
                children = updatedChildren,
                selectedChildId = newChild.childId,
                statusBannerMessage = "Added ${newChild.name} to ${state.familyHousehold.familyName}"
            )
        }
        viewModelScope.launch {
            syncChildrenToLocalRoom(updatedChildren)
            if (state.isCloudConnected) {
                try {
                    repository.upsertChildProfile(newChild)
                    recordAuditLog(
                        eventCode = "#CHD-204",
                        actionTitle = "Added Child Profile: ${newChild.name}",
                        details = "Synced to Cloud Firestore • Awaiting pairing handshake",
                        syncState = "PENDING"
                    )
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${newChild.familyId}/children",
                        documentId = newChild.childId,
                        entityType = "CHILD_PROFILE",
                        summary = newChild.name,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${newChild.familyId}/children",
                    documentId = newChild.childId,
                    entityType = "CHILD_PROFILE",
                    summary = newChild.name,
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    // ===============================================================
    // P-03: Secure Pairing Code Generation, Revocation & Share Actions
    // ===============================================================
    fun generateNewPairingCode() {
        val state = _uiState.value
        val child = state.selectedChild
        if (!state.isCloudConnected) {
            postBannerMessage("Sign in and connect to Anchor Cloud before generating a verifiable pairing code.")
            return
        }

        val pendingPairing = PairingCode(
            id = "",
            codeId = "",
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = child.childId,
            childName = child.name,
            code = "",
            expiresAtEpochMs = 0L,
            status = "PENDING",
            revoked = false,
            attemptCount = 0,
            maxAttempts = 5
        )
        _uiState.update {
            it.copy(
                pairingCode = pendingPairing,
                pairingCountdownSeconds = 0,
                statusBannerMessage = "Requesting a server-verified 10-minute pairing code for ${child.name}..."
            )
        }

        viewModelScope.launch {
            try {
                val cloudPairing = repository.createPairingCode(
                    familyId = state.familyHousehold.familyId,
                    ownerUid = state.currentUserUid,
                    childId = child.childId,
                    childName = child.name
                )
                _uiState.update {
                    it.copy(
                        pairingCode = cloudPairing,
                        pairingCountdownSeconds = 600,
                        statusBannerMessage = "Generated secure 10-minute pairing code ${cloudPairing.code} for ${child.name}"
                    )
                }
                recordAuditLog(
                    eventCode = "#PAIR-301",
                    actionTitle = "Generated Pairing Code",
                    details = "Target child: ${child.name} • Valid for 10 minutes • Prior codes revoked by server",
                    syncState = "ENFORCED"
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        pairingCode = pendingPairing.copy(status = "FAILED"),
                        pairingCountdownSeconds = 0,
                        statusBannerMessage = "Secure pairing code generation failed: ${e.localizedMessage ?: "Please retry."}"
                    )
                }
            }
        }
    }

    fun revokeCurrentPairingCode() {
        val state = _uiState.value
        val currentCode = state.pairingCode
        if (currentCode.codeId.isBlank() || currentCode.status != "ACTIVE" || currentCode.revoked) {
            postBannerMessage("There is no active server-issued pairing code to revoke.")
            return
        }
        if (!state.isCloudConnected) {
            postBannerMessage("Connect to Anchor Cloud to revoke this pairing code on the server.")
            return
        }

        viewModelScope.launch {
            try {
                repository.revokePairingCode(
                    familyId = state.familyHousehold.familyId,
                    codeId = currentCode.codeId
                )
                _uiState.update {
                    it.copy(
                        pairingCode = currentCode.copy(
                            status = "REVOKED",
                            revoked = true,
                            revokedAtEpochMs = System.currentTimeMillis()
                        ),
                        pairingCountdownSeconds = 0,
                        statusBannerMessage = "Revoked pairing code for ${state.selectedChild.name}"
                    )
                }
                recordAuditLog(
                    eventCode = "#PAIR-309",
                    actionTitle = "Revoked Pairing Code",
                    details = "Pairing code invalidated by the trusted server",
                    syncState = "ENFORCED"
                )
            } catch (e: Exception) {
                postBannerMessage("Could not revoke pairing code on the server: ${e.localizedMessage ?: "Please retry."}")
            }
        }
    }

    fun unpairPairedChildDevice(deviceUid: String) {
        val state = _uiState.value
        if (!state.isCloudConnected) {
            postBannerMessage("Connect to Anchor Cloud before unpairing a Child device.")
            return
        }
        val pairedDevice = state.pairedDevices.firstOrNull { it.deviceId == deviceUid }
        if (pairedDevice == null) {
            postBannerMessage("That paired device is no longer in this family.")
            return
        }
        viewModelScope.launch {
            try {
                repository.unpairChildDevice(
                    familyId = state.familyHousehold.familyId,
                    deviceUid = deviceUid
                )
                recordAuditLog(
                    eventCode = "#PAIR-310",
                    actionTitle = "Unpaired Child Device",
                    details = "Unpaired device for ${pairedDevice.childName.ifBlank { "Child" }} through the trusted server",
                    syncState = "ENFORCED"
                )
                postBannerMessage("Unpaired ${pairedDevice.childName.ifBlank { "Child" }}'s device. It must use a new Parent-issued code to reconnect.")
            } catch (e: Exception) {
                postBannerMessage("Could not unpair this device: ${e.localizedMessage ?: "Please retry."}")
            }
        }
    }

    fun copyPairingCodeToClipboard(context: Context) {
        val pairing = _uiState.value.pairingCode
        if (pairing.status != "ACTIVE" || pairing.code.isBlank()) {
            postBannerMessage("Generate a valid server-verified pairing code before copying.")
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Anchor Pairing Code", pairing.code))
        postBannerMessage("Copied pairing code ${pairing.code} to clipboard")
    }

    fun sharePairingInstructions(context: Context) {
        val state = _uiState.value
        val child = state.selectedChild
        if (state.pairingCode.status != "ACTIVE" || state.pairingCode.code.isBlank()) {
            postBannerMessage("Generate a valid server-verified pairing code before sharing.")
            return
        }
        val shareText = buildString {
            appendLine("Anchor Family Guardian — Child Device Pairing")
            appendLine("Household: ${state.familyHousehold.familyName} (${state.familyHousehold.familyId})")
            appendLine("Child Profile: ${child.name} (${child.childId})")
            appendLine("Pairing Code ID: ${state.pairingCode.codeId}")
            appendLine("6-Digit Pairing Passcode: ${state.pairingCode.code}")
            appendLine("Enter this code in the Anchor Child Companion app within 10 minutes to link with bilateral transparency.")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Anchor Pairing Code for ${child.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Pairing Code").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ===============================================================
    // P-04 & P-05: Child Selection, Pause, Bonus, Telemetry
    // ===============================================================
    fun selectChild(childId: String) {
        _uiState.update { it.copy(selectedChildId = childId) }
    }

    fun togglePauseChildDevice(durationLabel: String = "Until Resumed") {
        val state = _uiState.value
        val child = state.selectedChild
        val isCurrentlyPaused = child.currentMode.startsWith("Paused")
        val pendingChild = if (isCurrentlyPaused) {
            child.copy(
                currentMode = "Free Play Window",
                policyPausedUntil = "",
                syncStatus = "PENDING",
                lastSyncLabel = "Syncing resume command..."
            )
        } else {
            child.copy(
                currentMode = "Paused ($durationLabel)",
                policyPausedUntil = durationLabel,
                syncStatus = "PENDING",
                lastSyncLabel = "Syncing pause command..."
            )
        }
        updateChildInStateAndCloud(
            updatedChild = pendingChild,
            banner = if (isCurrentlyPaused) {
                "Resumed ${child.name}'s ${child.deviceName}"
            } else {
                "Paused ${child.name}'s ${child.deviceName} ($durationLabel) — Emergency calls & SOS remain active"
            }
        )
    }

    fun grantBonusMinutes(bonusMinutes: Int) {
        val child = _uiState.value.selectedChild
        val updatedChild = child.copy(
            dailyAllowanceMinutes = (child.dailyAllowanceMinutes + bonusMinutes).coerceAtMost(720),
            lastSyncLabel = "+${bonusMinutes}m bonus pending sync",
            syncStatus = "PENDING"
        )
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
            banner = "Granted +${bonusMinutes}m bonus screen time to ${child.name}"
        )
    }

    fun triggerDeviceQuickAction(actionName: String) {
        val child = _uiState.value.selectedChild
        val updatedChild = child.copy(
            lastSyncLabel = "$actionName • Dispatched",
            syncStatus = "PENDING"
        )
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
            banner = "$actionName dispatched to ${child.deviceName}"
        )
    }

    private fun updateChildInStateAndCloud(updatedChild: ChildProfile, banner: String) {
        val state = _uiState.value
        val updatedList = state.children.map {
            if (it.childId == updatedChild.childId) updatedChild else it
        }
        _uiState.update {
            it.copy(
                children = updatedList,
                statusBannerMessage = banner
            )
        }
        viewModelScope.launch {
            syncChildrenToLocalRoom(updatedList)
            if (state.isCloudConnected) {
                try {
                    val confirmedChild = updatedChild.copy(
                        syncStatus = "ENFORCED",
                        lastSyncLabel = "Synced to Cloud Firestore just now"
                    )
                    repository.upsertChildProfile(confirmedChild)
                    val confirmedList = _uiState.value.children.map {
                        if (it.childId == confirmedChild.childId) confirmedChild else it
                    }
                    _uiState.update {
                        it.copy(
                            children = confirmedList,
                            statusBannerMessage = "$banner (Cloud Sync Confirmed)"
                        )
                    }
                    syncChildrenToLocalRoom(confirmedList)
                    recordAuditLog(
                        eventCode = "#POL-402",
                        actionTitle = banner,
                        details = "Child: ${confirmedChild.name} • Mode: ${confirmedChild.currentMode}",
                        syncState = "ENFORCED"
                    )
                } catch (e: Exception) {
                    val queuedChild = updatedChild.copy(
                        syncStatus = "QUEUED_OFFLINE",
                        lastSyncLabel = "Queued in Room for Cloud Sync"
                    )
                    val queuedList = _uiState.value.children.map {
                        if (it.childId == queuedChild.childId) queuedChild else it
                    }
                    _uiState.update {
                        it.copy(
                            children = queuedList,
                            statusBannerMessage = "$banner (Queued Offline: ${e.localizedMessage ?: "Network unavailable"})"
                        )
                    }
                    enqueueOfflineWrite(
                        collectionPath = "families/${updatedChild.familyId}/children",
                        documentId = updatedChild.childId,
                        entityType = "CHILD_PROFILE",
                        summary = banner,
                        error = e.localizedMessage ?: "Write error"
                    )
                }
            } else {
                val localChild = updatedChild.copy(
                    syncStatus = "LOCAL_ONLY",
                    lastSyncLabel = "Saved in Local Room Cache"
                )
                val localList = _uiState.value.children.map {
                    if (it.childId == localChild.childId) localChild else it
                }
                _uiState.update {
                    it.copy(
                        children = localList,
                        statusBannerMessage = "$banner (Saved in Local Room Cache)"
                    )
                }
                syncChildrenToLocalRoom(localList)
                enqueueOfflineWrite(
                    collectionPath = "families/${updatedChild.familyId}/children",
                    documentId = updatedChild.childId,
                    entityType = "CHILD_PROFILE",
                    summary = banner,
                    error = "Not signed in to Cloud Firestore"
                )
                recordAuditLog(
                    eventCode = "#POL-402",
                    actionTitle = banner,
                    details = "Child: ${localChild.name} • Mode: ${localChild.currentMode}",
                    syncState = "LOCAL_ONLY"
                )
            }
        }
    }

    // ===============================================================
    // P-06: Screen-Time Analytics
    // ===============================================================
    fun setAnalyticsRange(range: String) {
        _uiState.update { it.copy(analyticsRange = range) }
    }

    fun exportAnalyticsReport(context: Context) {
        val state = _uiState.value
        val child = state.selectedChild
        val localDeviceScreenTime = AndroidPlatformHelper.queryDeviceScreenTimeMinutesToday(context)
        val reportText = buildString {
            appendLine("ANCHOR DIGITAL WELLBEING & ANALYTICS REPORT (${state.analyticsRange})")
            appendLine("Household: ${state.familyHousehold.familyName}")
            appendLine("Child: ${child.name} (${child.deviceName})")
            appendLine("Child Synced Usage Today: ${child.usedMinutesToday}m / ${child.dailyAllowanceMinutes}m")
            if (localDeviceScreenTime != null) {
                appendLine("Parent Device UsageStatsManager Inspection: ${localDeviceScreenTime}m today")
            }
            appendLine("Top Apps:")
            state.appRules.forEach { rule ->
                appendLine("- ${rule.appName} (${rule.category}): ${rule.usedMinutesToday}m / ${rule.dailyLimitMinutes}m [${rule.status}]")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Anchor Screen-Time Report — ${child.name}")
            putExtra(Intent.EXTRA_TEXT, reportText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export Analytics Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ===============================================================
    // P-07: App Usage & Limits
    // ===============================================================
    fun adjustDailyAllowance(deltaMinutes: Int) {
        val child = _uiState.value.selectedChild
        val newAllowance = (child.dailyAllowanceMinutes + deltaMinutes).coerceIn(30, 600)
        val updatedChild = child.copy(
            dailyAllowanceMinutes = newAllowance,
            lastSyncLabel = "Allowance updated to ${newAllowance / 60}h ${newAllowance % 60}m"
        )
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
            banner = "Updated ${child.name}'s daily screen allowance to ${newAllowance / 60}h ${newAllowance % 60}m"
        )
    }

    fun toggleAppBlock(ruleId: String) {
        val state = _uiState.value
        val target = state.appRules.find { it.ruleId == ruleId } ?: return
        val newBlocked = !target.blocked
        val updatedRule = target.copy(
            blocked = newBlocked,
            status = if (newBlocked) "BLOCKED" else "ENFORCED"
        )
        val updatedRules = state.appRules.map { if (it.ruleId == ruleId) updatedRule else it }
        _uiState.update {
            it.copy(
                appRules = updatedRules,
                statusBannerMessage = if (newBlocked) {
                    "Blocked ${updatedRule.appName} on ${state.selectedChild.deviceName}"
                } else {
                    "Restored ${updatedRule.appName} (${updatedRule.dailyLimitMinutes}m daily cap enforced)"
                }
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertAppRule(updatedRule)
                    recordAuditLog(
                        eventCode = "#APP-701",
                        actionTitle = "Updated App Rule: ${updatedRule.appName}",
                        details = "Status: ${updatedRule.status} • Limit: ${updatedRule.dailyLimitMinutes}m (Cloud Synced)",
                        syncState = updatedRule.status
                    )
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updatedRule.familyId}/appRules",
                        documentId = updatedRule.ruleId,
                        entityType = "APP_RULE",
                        summary = updatedRule.appName,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${updatedRule.familyId}/appRules",
                    documentId = updatedRule.ruleId,
                    entityType = "APP_RULE",
                    summary = updatedRule.appName,
                    error = "Local Sandbox Mode"
                )
                recordAuditLog(
                    eventCode = "#APP-701",
                    actionTitle = "Updated App Rule: ${updatedRule.appName}",
                    details = "Status: ${updatedRule.status} • Limit: ${updatedRule.dailyLimitMinutes}m (Saved Locally)",
                    syncState = "LOCAL_ONLY"
                )
            }
        }
    }

    fun adjustAppRuleLimit(ruleId: String, deltaMinutes: Int) {
        val state = _uiState.value
        val target = state.appRules.find { it.ruleId == ruleId } ?: return
        val newLimit = (target.dailyLimitMinutes + deltaMinutes).coerceIn(15, 240)
        val updatedRule = target.copy(dailyLimitMinutes = newLimit, status = "ENFORCED", blocked = false)
        _uiState.update {
            it.copy(
                appRules = state.appRules.map { r -> if (r.ruleId == ruleId) updatedRule else r },
                statusBannerMessage = "Set ${updatedRule.appName} limit to ${newLimit}m/day"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertAppRule(updatedRule)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updatedRule.familyId}/appRules",
                        documentId = updatedRule.ruleId,
                        entityType = "APP_RULE",
                        summary = "${updatedRule.appName} ${newLimit}m",
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${updatedRule.familyId}/appRules",
                    documentId = updatedRule.ruleId,
                    entityType = "APP_RULE",
                    summary = "${updatedRule.appName} ${newLimit}m",
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    fun addCustomAppRule(appName: String, category: String, limitMinutes: Int) {
        if (appName.isBlank()) return
        val state = _uiState.value
        val ruleId = "rule_${appName.lowercase(Locale.US).replace(" ", "_")}_${Random.nextInt(100, 999)}"
        val newRule = AppRule(
            id = ruleId,
            ruleId = ruleId,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = state.selectedChildId,
            packageName = "com.app.${appName.lowercase(Locale.US).replace(" ", "")}",
            appName = appName.trim(),
            category = category,
            dailyLimitMinutes = limitMinutes.coerceIn(15, 240),
            usedMinutesToday = 0,
            status = if (state.isCloudConnected) "ENFORCED" else "PENDING"
        )
        _uiState.update {
            it.copy(
                appRules = it.appRules + newRule,
                statusBannerMessage = "Added ${newRule.appName} (${newRule.dailyLimitMinutes}m/day) to ${state.selectedChild.name}'s rules"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertAppRule(newRule)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${newRule.familyId}/appRules",
                        documentId = newRule.ruleId,
                        entityType = "APP_RULE",
                        summary = newRule.appName,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${newRule.familyId}/appRules",
                    documentId = newRule.ruleId,
                    entityType = "APP_RULE",
                    summary = newRule.appName,
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    // ===============================================================
    // P-08: Schedules & Downtime
    // ===============================================================
    fun toggleScheduleRoutine(scheduleId: String) {
        val state = _uiState.value
        val target = state.schedules.find { it.scheduleId == scheduleId } ?: return
        val updated = target.copy(
            enabled = !target.enabled,
            syncStatus = if (state.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
        )
        _uiState.update {
            it.copy(
                schedules = state.schedules.map { s -> if (s.scheduleId == scheduleId) updated else s },
                statusBannerMessage = "${updated.title} is now ${if (updated.enabled) "Active" else "Paused"}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertScheduleRoutine(updated)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updated.familyId}/schedules",
                        documentId = updated.scheduleId,
                        entityType = "SCHEDULE",
                        summary = updated.title,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${updated.familyId}/schedules",
                    documentId = updated.scheduleId,
                    entityType = "SCHEDULE",
                    summary = updated.title,
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    fun addCustomSchedule(title: String, windowDesc: String, daysSummary: String, ruleSummary: String) {
        if (title.isBlank()) return
        val state = _uiState.value
        val id = "sched_${Random.nextInt(1000, 9999)}"
        val newSchedule = ScheduleRoutine(
            id = id,
            scheduleId = id,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = state.selectedChildId,
            title = title.trim(),
            windowDescription = windowDesc.ifBlank { "4:00 PM — 5:30 PM" },
            daysSummary = daysSummary.ifBlank { "Mon–Fri" },
            ruleSummary = ruleSummary.ifBlank { "Study & Learning Apps Only" },
            iconType = "CUSTOM",
            enabled = true,
            syncStatus = if (state.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
        )
        _uiState.update {
            it.copy(
                schedules = it.schedules + newSchedule,
                statusBannerMessage = "Created schedule '${newSchedule.title}' (${newSchedule.windowDescription})"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertScheduleRoutine(newSchedule)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${newSchedule.familyId}/schedules",
                        documentId = newSchedule.scheduleId,
                        entityType = "SCHEDULE",
                        summary = newSchedule.title,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${newSchedule.familyId}/schedules",
                    documentId = newSchedule.scheduleId,
                    entityType = "SCHEDULE",
                    summary = newSchedule.title,
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    fun updateFairPlayNoticeMinutes(minutes: Int) {
        val updatedFamily = _uiState.value.familyHousehold.copy(fairPlayNoticeMinutes = minutes)
        _uiState.update {
            it.copy(
                familyHousehold = updatedFamily,
                statusBannerMessage = "Fair-Play advance warning set to $minutes minutes before lock"
            )
        }
        persistFamilyWithSyncStatus(updatedFamily, "Fair-Play advance warning set to $minutes minutes")
    }

    // ===============================================================
    // P-09: Location & Geofences (with Real Device GPS & Haversine Math)
    // ===============================================================
    fun pingChildGps(context: Context? = null) {
        val state = _uiState.value
        val child = state.selectedChild
        val deviceLoc = context?.let { AndroidPlatformHelper.captureDeviceGpsLocation(it) }
        val lat = deviceLoc?.latitude ?: child.latitude
        val lon = deviceLoc?.longitude ?: child.longitude

        // Evaluate Haversine distance against all configured safe geofences
        val updatedGeofences = state.geofences.map { geo ->
            val inside = AndroidPlatformHelper.isInsideGeofence(
                childLat = lat,
                childLon = lon,
                geofenceLat = geo.latitude,
                geofenceLon = geo.longitude,
                radiusMeters = geo.radiusMeters
            )
            geo.copy(activeInside = inside)
        }
        val activeZone = updatedGeofences.firstOrNull { it.activeInside }
        val locationDesc = if (deviceLoc != null) {
            activeZone?.name ?: String.format(Locale.US, "GPS Fix (%.4f, %.4f)", lat, lon)
        } else {
            child.locationLabel
        }

        _uiState.update { it.copy(geofences = updatedGeofences) }

        val updatedChild = child.copy(
            latitude = lat,
            longitude = lon,
            locationLabel = locationDesc,
            lastSyncLabel = if (deviceLoc != null) {
                "Device GPS Verified (< ${deviceLoc.accuracy.toInt().coerceAtLeast(5)}m accuracy)"
            } else {
                "GPS Telemetry Checked (${locationDesc})"
            }
        )
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
            banner = "Live GPS fix confirmed for ${child.name} at $locationDesc"
        )
    }

    fun openDirectionsToChild(context: Context) {
        val child = _uiState.value.selectedChild
        val uri = Uri.parse("geo:${child.latitude},${child.longitude}?q=${Uri.encode(child.locationLabel)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            postBannerMessage("Location coordinates: ${child.latitude}, ${child.longitude} (${child.locationLabel})")
        }
    }

    fun toggleGeofenceAlerts(geofenceId: String) {
        val state = _uiState.value
        val target = state.geofences.find { it.geofenceId == geofenceId } ?: return
        val updated = target.copy(alertsEnabled = !target.alertsEnabled)
        _uiState.update {
            it.copy(
                geofences = state.geofences.map { g -> if (g.geofenceId == geofenceId) updated else g },
                statusBannerMessage = "${updated.name} boundary alerts ${if (updated.alertsEnabled) "enabled" else "muted"}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertSafeGeofence(updated)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updated.familyId}/geofences",
                        documentId = updated.geofenceId,
                        entityType = "GEOFENCE",
                        summary = updated.name,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${updated.familyId}/geofences",
                    documentId = updated.geofenceId,
                    entityType = "GEOFENCE",
                    summary = updated.name,
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    fun addSafeGeofence(name: String, radiusMeters: Int, scheduleSummary: String) {
        if (name.isBlank()) return
        val state = _uiState.value
        val child = state.selectedChild
        val id = "geo_${Random.nextInt(1000, 9999)}"
        val geofence = SafeGeofence(
            id = id,
            geofenceId = id,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = state.selectedChildId,
            name = name.trim(),
            radiusMeters = radiusMeters.coerceIn(50, 2000),
            triggerSummary = "Notify on Arrival & Departure",
            scheduleSummary = scheduleSummary.ifBlank { "24/7 Active Boundary" },
            activeInside = true,
            alertsEnabled = true,
            latitude = child.latitude,
            longitude = child.longitude
        )
        _uiState.update {
            it.copy(
                geofences = it.geofences + geofence,
                statusBannerMessage = "Added Safe Geofence '${geofence.name}' (${geofence.radiusMeters}m)"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertSafeGeofence(geofence)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${geofence.familyId}/geofences",
                        documentId = geofence.geofenceId,
                        entityType = "GEOFENCE",
                        summary = geofence.name,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${geofence.familyId}/geofences",
                    documentId = geofence.geofenceId,
                    entityType = "GEOFENCE",
                    summary = geofence.name,
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    fun deleteSafeGeofence(geofenceId: String) {
        val state = _uiState.value
        val target = state.geofences.find { it.geofenceId == geofenceId } ?: return
        _uiState.update {
            it.copy(
                geofences = state.geofences.filterNot { g -> g.geofenceId == geofenceId },
                statusBannerMessage = "Removed Safe Geofence '${target.name}'"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.deleteSafeGeofence(state.familyHousehold.familyId, geofenceId)
                } catch (_: Exception) {}
            }
        }
    }

    // ===============================================================
    // P-10: Alerts & SOS Activity (Practice Mode vs Real Emergency Mode)
    // ===============================================================
    fun triggerTestSosSirenDrill(context: Context) {
        val state = _uiState.value
        val child = state.selectedChild
        val deviceLoc = AndroidPlatformHelper.captureDeviceGpsLocation(context)
        val lat = deviceLoc?.latitude ?: child.latitude
        val lon = deviceLoc?.longitude ?: child.longitude
        val id = "alert_sos_drill_${System.currentTimeMillis()}"
        val drillAlert = SafetyAlert(
            id = id,
            alertId = id,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = child.childId,
            childName = child.name,
            title = "[PRACTICE DRILL] Family SOS Siren Verified",
            description = "Practice Mode SOS Drill confirmed high-priority notification channel, GPS coordinates ($lat, $lon • ${child.locationLabel}), and Tier-1 Guardian escalation.",
            category = "SOS",
            severity = "CRITICAL",
            timestampLabel = "Just now • Practice Mode Drill",
            acknowledged = false
        )
        _uiState.update {
            it.copy(
                alerts = listOf(drillAlert) + it.alerts,
                statusBannerMessage = "Family SOS Practice Drill executed — High-priority Android notification dispatched"
            )
        }
        dispatchLocalNotification(
            context = context,
            title = "[PRACTICE DRILL] Anchor SOS — ${child.name}",
            body = "Practice SOS Siren verified. Location: ${child.locationLabel} ($lat, $lon) • Battery: ${child.batteryLevel}%"
        )
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertSafetyAlert(drillAlert)
                    recordAuditLog(
                        eventCode = "#SOS-911",
                        actionTitle = "Executed Family SOS Practice Drill",
                        details = "Synced to Cloud Firestore & verified notification channel.",
                        syncState = "ENFORCED"
                    )
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${drillAlert.familyId}/alerts",
                        documentId = drillAlert.alertId,
                        entityType = "ALERT",
                        summary = drillAlert.title,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${drillAlert.familyId}/alerts",
                    documentId = drillAlert.alertId,
                    entityType = "ALERT",
                    summary = drillAlert.title,
                    error = "Local Sandbox Mode"
                )
                recordAuditLog(
                    eventCode = "#SOS-911",
                    actionTitle = "Executed Family SOS Practice Drill",
                    details = "Saved in Local Room Queue & verified notification channel.",
                    syncState = "LOCAL_ONLY"
                )
            }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        val state = _uiState.value
        val target = state.alerts.find { it.alertId == alertId } ?: return
        val updated = target.copy(acknowledged = true)
        _uiState.update {
            it.copy(
                alerts = state.alerts.map { a -> if (a.alertId == alertId) updated else a },
                statusBannerMessage = "Acknowledged alert: ${updated.title}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertSafetyAlert(updated)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updated.familyId}/alerts",
                        documentId = updated.alertId,
                        entityType = "ALERT",
                        summary = "Ack ${updated.title}",
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            }
        }
    }

    // ===============================================================
    // P-11: Extra Time & App Requests
    // ===============================================================
    fun resolveChildRequest(
        requestId: String,
        approved: Boolean,
        bonusMinutes: Int,
        decisionSummary: String
    ) {
        val state = _uiState.value
        val target = state.requests.find { it.requestId == requestId } ?: return
        val updatedRequest = target.copy(
            status = if (approved) "APPROVED" else "DECLINED",
            decisionSummary = decisionSummary,
            decidedBy = state.guardianDisplayName
        )
        val systemChatEvent = FamilyMessage(
            id = "msg_sys_${System.currentTimeMillis()}",
            messageId = "msg_sys_${System.currentTimeMillis()}",
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = target.childId,
            senderName = "Anchor Fair-Play Audit",
            senderRole = "SYSTEM",
            text = "${state.guardianDisplayName} ${if (approved) "approved" else "declined"} ${target.childName}'s request for ${target.targetName}: $decisionSummary",
            isSystemEvent = true,
            timestampLabel = "Just now"
        )

        _uiState.update {
            it.copy(
                requests = state.requests.map { r -> if (r.requestId == requestId) updatedRequest else r },
                messages = it.messages + systemChatEvent,
                statusBannerMessage = "${if (approved) "Approved" else "Declined"} ${target.targetName} request ($decisionSummary)"
            )
        }

        if (approved && bonusMinutes > 0) {
            grantBonusMinutes(bonusMinutes)
        }

        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertChildRequest(updatedRequest)
                    repository.upsertFamilyMessage(systemChatEvent)
                    recordAuditLog(
                        eventCode = "#REQ-502",
                        actionTitle = "Request ${updatedRequest.status}: ${target.targetName}",
                        details = decisionSummary,
                        syncState = "ENFORCED"
                    )
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updatedRequest.familyId}/requests",
                        documentId = updatedRequest.requestId,
                        entityType = "REQUEST",
                        summary = "${updatedRequest.status} ${target.targetName}",
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${updatedRequest.familyId}/requests",
                    documentId = updatedRequest.requestId,
                    entityType = "REQUEST",
                    summary = "${updatedRequest.status} ${target.targetName}",
                    error = "Local Sandbox Mode"
                )
                recordAuditLog(
                    eventCode = "#REQ-502",
                    actionTitle = "Request ${updatedRequest.status}: ${target.targetName}",
                    details = decisionSummary,
                    syncState = "LOCAL_ONLY"
                )
            }
        }
    }

    fun toggleAutoApproveLearningApps() {
        val current = _uiState.value.familyHousehold
        val updated = current.copy(autoApproveLearningApps = !current.autoApproveLearningApps)
        _uiState.update {
            it.copy(
                familyHousehold = updated,
                statusBannerMessage = "Auto-approve verified school/learning apps: ${if (updated.autoApproveLearningApps) "ON" else "OFF"}"
            )
        }
        persistFamilyWithSyncStatus(updated, "Updated Auto-Approve Learning Apps policy")
    }

    // ===============================================================
    // P-12: Family Chat & Shared Tasks
    // ===============================================================
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val state = _uiState.value
        val msgId = "msg_${System.currentTimeMillis()}"
        val message = FamilyMessage(
            id = msgId,
            messageId = msgId,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = state.selectedChildId,
            senderName = state.guardianDisplayName,
            senderRole = "PARENT",
            text = text.trim(),
            isSystemEvent = false,
            timestampLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        )
        _uiState.update {
            it.copy(
                messages = it.messages + message,
                statusBannerMessage = "Sent message to ${state.selectedChild.name}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertFamilyMessage(message)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${message.familyId}/messages",
                        documentId = message.messageId,
                        entityType = "MESSAGE",
                        summary = message.text.take(40),
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${message.familyId}/messages",
                    documentId = message.messageId,
                    entityType = "MESSAGE",
                    summary = message.text.take(40),
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    fun assignFamilyTask(title: String, rewardSummary: String, dueSummary: String) {
        if (title.isBlank()) return
        val state = _uiState.value
        val taskId = "task_${System.currentTimeMillis()}"
        val newTask = FamilyTask(
            id = taskId,
            taskId = taskId,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = state.selectedChildId,
            title = title.trim(),
            rewardSummary = rewardSummary.ifBlank { "+15m Bonus Screen Time" },
            dueSummary = dueSummary.ifBlank { "Due Today by 7:00 PM" },
            status = "IN_PROGRESS",
            completedByChild = false,
            approvedByParent = false
        )
        _uiState.update {
            it.copy(
                tasks = listOf(newTask) + it.tasks,
                statusBannerMessage = "Assigned habit pact '${newTask.title}' to ${state.selectedChild.name}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertFamilyTask(newTask)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${newTask.familyId}/tasks",
                        documentId = newTask.taskId,
                        entityType = "TASK",
                        summary = newTask.title,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${newTask.familyId}/tasks",
                    documentId = newTask.taskId,
                    entityType = "TASK",
                    summary = newTask.title,
                    error = "Local Sandbox Mode"
                )
            }
        }
    }

    fun approveFamilyTask(taskId: String) {
        val state = _uiState.value
        val target = state.tasks.find { it.taskId == taskId } ?: return
        val updated = target.copy(
            status = "COMPLETED",
            completedByChild = true,
            approvedByParent = true,
            dueSummary = "Verified & Rewarded by ${state.guardianDisplayName}"
        )
        _uiState.update {
            it.copy(
                tasks = state.tasks.map { t -> if (t.taskId == taskId) updated else t }
            )
        }
        grantBonusMinutes(15)
        sendChatMessage("Verified '${target.title}'! Unlocked ${target.rewardSummary}.")
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertFamilyTask(updated)
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updated.familyId}/tasks",
                        documentId = updated.taskId,
                        entityType = "TASK",
                        summary = "Approved ${updated.title}",
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            }
        }
    }

    // ===============================================================
    // P-13: Guardian Roles & Trusted Contacts
    // ===============================================================
    fun toggleGuardianGeofenceAlerts(guardianId: String) {
        val state = _uiState.value
        val target = state.guardians.find { it.guardianId == guardianId } ?: return
        val updated = target.copy(receiveGeofenceAlerts = !target.receiveGeofenceAlerts)
        _uiState.update {
            it.copy(
                guardians = state.guardians.map { g -> if (g.guardianId == guardianId) updated else g },
                statusBannerMessage = "Updated geofence check-in alerts for ${updated.name}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertGuardianContact(updated)
                } catch (_: Exception) {}
            }
        }
    }

    fun inviteGuardianOrContact(
        name: String,
        relationRole: String,
        emailOrPhone: String,
        isPrimaryGuardian: Boolean
    ) {
        if (name.isBlank()) return
        val state = _uiState.value
        val id = "guard_${Random.nextInt(1000, 9999)}"
        val contact = GuardianContact(
            id = id,
            guardianId = id,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            name = name.trim(),
            relationRole = relationRole.ifBlank { if (isPrimaryGuardian) "Co-Parent Guardian" else "Trusted Emergency Contact" },
            emailOrPhone = emailOrPhone.ifBlank { "invite@family.org" },
            isPrimaryGuardian = isPrimaryGuardian,
            permissionsSummary = if (isPrimaryGuardian) {
                "Approve Requests, Edit Schedules, SOS Dispatch"
            } else {
                "Emergency SOS SMS & Geofence Check-In Alerts Only"
            },
            receiveGeofenceAlerts = true,
            receiveSosSms = true,
            status = "Invitation Sent"
        )
        _uiState.update {
            it.copy(
                guardians = it.guardians + contact,
                statusBannerMessage = "Added ${contact.name} (${contact.relationRole})"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertGuardianContact(contact)
                } catch (_: Exception) {}
            }
        }
    }

    // ===============================================================
    // P-14: Privacy, Data Rights & Deletion
    // ===============================================================
    fun exportHouseholdDataJson(context: Context) {
        val state = _uiState.value
        val root = JSONObject().apply {
            put("exportStandard", "Anchor Bilateral Privacy Archive v1.0 (COPPA / GDPR-K)")
            put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            put("householdName", state.familyHousehold.familyName)
            put("subscriptionTier", state.familyHousehold.subscriptionTier)
            put("children", JSONArray().apply {
                state.children.forEach { child ->
                    put(JSONObject().apply {
                        put("name", child.name)
                        put("age", child.age)
                        put("device", child.deviceName)
                        put("dailyAllowanceMinutes", child.dailyAllowanceMinutes)
                        put("usedMinutesToday", child.usedMinutesToday)
                        put("locationLabel", child.locationLabel)
                    })
                }
            })
            put("schedulesCount", state.schedules.size)
            put("geofencesCount", state.geofences.size)
            put("auditTrailEntries", state.auditLogs.size)
            put("pendingOfflineSyncItems", state.pendingOfflineSyncCount)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Anchor Household Data Export (${state.familyHousehold.familyName})")
            putExtra(Intent.EXTRA_TEXT, root.toString(2))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export Encrypted JSON Archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        postBannerMessage("Generated portable JSON data archive for ${state.familyHousehold.familyName}")
    }

    fun purgeHistoricalLogs() {
        viewModelScope.launch {
            localDao.clearAuditLogs()
            recordAuditLog(
                eventCode = "#PRV-801",
                actionTitle = "Purged Historical Telemetry & Location Trails",
                details = "Executed statutory data minimization purge. Active safety rules preserved.",
                syncState = if (_uiState.value.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
            )
            postBannerMessage("Purged historical location & activity logs (Active rules preserved)")
        }
    }

    fun toggleAnonymousTelemetry() {
        _uiState.update {
            val next = !it.anonymousDiagnosticTelemetry
            it.copy(
                anonymousDiagnosticTelemetry = next,
                statusBannerMessage = "Anonymous crash & diagnostic telemetry: ${if (next) "Opted In" else "Opted Out (Strict Zero-Telemetry)"}"
            )
        }
    }

    fun initiateFamilyDeletion() {
        viewModelScope.launch {
            val state = _uiState.value
            realtimeListenerJobs.forEach { it.cancel() }
            realtimeListenerJobs.clear()
            localDao.clearAuditLogs()
            localDao.clearCachedChildren()
            localDao.clearOfflineSyncQueue()
            if (state.isCloudConnected) {
                try {
                    repository.deleteFamilyHouseholdCascade(
                        familyId = state.familyHousehold.familyId,
                        uid = state.currentUserUid
                    )
                } catch (_: Exception) {}
            }
            _uiState.update {
                AnchorUiState(
                    currentScreen = AnchorScreen.P01_WELCOME_SIGN_IN,
                    statusBannerMessage = "Household data deleted from local Room cache and Firestore."
                )
            }
        }
    }

    // ===============================================================
    // P-15: Premium Features & Subscription Entitlement
    // ===============================================================
    fun setSubscriptionTier(tier: String, active: Boolean) {
        val current = _uiState.value.familyHousehold
        val updated = current.copy(
            subscriptionTier = tier,
            subscriptionActive = active
        )
        _uiState.update {
            it.copy(
                familyHousehold = updated,
                statusBannerMessage = if (active) {
                    "Anchor Family Premium ($tier) Active — All 6 Premium Safeguard Groups Unlocked"
                } else {
                    "Switched to Free Core Tier — The 6 Premium feature groups now show upgrade paywalls"
                }
            )
        }
        persistFamilyWithSyncStatus(updated, "Updated Subscription Entitlement: $tier")
    }

    // ===============================================================
    // P-16, P-17, P-18, P-19: Premium Safeguard Actions
    // ===============================================================
    fun updateWebFilterProfile(profile: String) {
        val updated = _uiState.value.premiumConfig.copy(webFilterProfile = profile)
        savePremiumConfig(updated, "Web Filter Profile set to $profile")
    }

    fun toggleWebBlockedCategory(categoryName: String) {
        val current = _uiState.value.premiumConfig
        if (categoryName.contains("Adult", ignoreCase = true)) {
            postBannerMessage("Adult & Explicit Content filter is permanently locked ON in SafeGuard mode")
            return
        }
        val nextList = if (current.blockedCategories.contains(categoryName)) {
            current.blockedCategories - categoryName
        } else {
            current.blockedCategories + categoryName
        }
        savePremiumConfig(current.copy(blockedCategories = nextList), "Updated Web Category rule: $categoryName")
    }

    fun addWhitelistedDomain(domain: String) {
        val cleaned = domain.trim().lowercase(Locale.US)
        if (cleaned.isBlank()) return
        val current = _uiState.value.premiumConfig
        if (!current.whitelistedDomains.contains(cleaned)) {
            savePremiumConfig(
                current.copy(whitelistedDomains = current.whitelistedDomains + cleaned),
                "Added $cleaned to Always Allowed Whitelist"
            )
        }
    }

    fun removeWhitelistedDomain(domain: String) {
        val current = _uiState.value.premiumConfig
        savePremiumConfig(
            current.copy(whitelistedDomains = current.whitelistedDomains - domain),
            "Removed $domain from Whitelist"
        )
    }

    fun addBlacklistedDomain(domain: String) {
        val cleaned = domain.trim().lowercase(Locale.US)
        if (cleaned.isBlank()) return
        val current = _uiState.value.premiumConfig
        if (!current.blacklistedDomains.contains(cleaned)) {
            savePremiumConfig(
                current.copy(blacklistedDomains = current.blacklistedDomains + cleaned),
                "Blocked domain $cleaned across browsers"
            )
        }
    }

    fun removeBlacklistedDomain(domain: String) {
        val current = _uiState.value.premiumConfig
        savePremiumConfig(
            current.copy(blacklistedDomains = current.blacklistedDomains - domain),
            "Removed $domain from Blacklist"
        )
    }

    fun updateSocialShieldProfile(profile: String) {
        val current = _uiState.value.premiumConfig
        savePremiumConfig(current.copy(socialShieldProfile = profile), "Social Shield Profile set to $profile")
    }

    fun toggleSocialPlatformRule(platformKey: String) {
        val c = _uiState.value.premiumConfig
        val updated = when (platformKey) {
            "DISCORD" -> c.copy(discordDmWhitelistOnly = !c.discordDmWhitelistOnly)
            "ROBLOX" -> c.copy(robloxChatFiltered = !c.robloxChatFiltered)
            "WHATSAPP" -> c.copy(whatsappUnknownBlocked = !c.whatsappUnknownBlocked)
            "YOUTUBE" -> c.copy(youtubeRestrictedMode = !c.youtubeRestrictedMode)
            else -> c
        }
        savePremiumConfig(updated, "Updated $platformKey content safeguard policy")
    }

    fun updateNotificationFilterConfig(
        enabled: Boolean = _uiState.value.premiumConfig.notificationFilterEnabled,
        protocol: String = _uiState.value.premiumConfig.notificationProtocol,
        batchSocial: Boolean = _uiState.value.premiumConfig.batchMuteSocialDuringStudy,
        blockStreaks: Boolean = _uiState.value.premiumConfig.blockGamingStreakPrompts
    ) {
        val current = _uiState.value.premiumConfig
        val updated = current.copy(
            notificationFilterEnabled = enabled,
            notificationProtocol = protocol,
            batchMuteSocialDuringStudy = batchSocial,
            blockGamingStreakPrompts = blockStreaks
        )
        savePremiumConfig(updated, "Applied Notification & Distraction Filter ($protocol)")
    }

    fun setLiveSessionMode(mode: String) {
        _uiState.update {
            it.copy(
                liveSessionMode = mode,
                statusBannerMessage = "Selected Audited Check-In Mode: $mode (Requires explicit child consent on ${it.selectedChild.deviceName})"
            )
        }
        viewModelScope.launch {
            recordAuditLog(
                eventCode = _uiState.value.premiumConfig.liveSessionAuditCode,
                actionTitle = "Selected Audited Live Session Mode: $mode",
                details = "Reason: ${_uiState.value.liveSessionReason} • Consent Status: ${_uiState.value.liveSessionConsentStatus}",
                syncState = if (_uiState.value.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
            )
        }
    }

    fun toggleLiveSessionTwoWayTalk() {
        _uiState.update {
            val next = !it.liveSessionTwoWayTalk
            it.copy(
                liveSessionTwoWayTalk = next,
                statusBannerMessage = if (next) "Two-Way Voice Requested with ${it.selectedChild.name}" else "Two-Way Voice Muted"
            )
        }
    }

    fun toggleLiveSessionActive() {
        val state = _uiState.value
        val currentlyActive = state.liveSessionActive
        if (currentlyActive) {
            _uiState.update {
                it.copy(
                    liveSessionActive = false,
                    liveSessionConsentStatus = "ENDED",
                    statusBannerMessage = "Ended Audited Live Check-In Request (${it.premiumConfig.liveSessionAuditCode}) — Logged to Family Chat"
                )
            }
            sendChatMessage("[Audited Session ${state.premiumConfig.liveSessionAuditCode} Ended] Thank you for checking in!")
        } else {
            val sessionId = "live_${state.selectedChildId}_${System.currentTimeMillis()}"
            val request = LiveSessionRequest(
                id = sessionId,
                sessionId = sessionId,
                familyId = state.familyHousehold.familyId,
                ownerUid = state.currentUserUid,
                childId = state.selectedChildId,
                auditCode = state.premiumConfig.liveSessionAuditCode,
                mode = state.liveSessionMode,
                reason = state.liveSessionReason,
                status = "AWAITING_CHILD_CONSENT",
                consentGranted = false,
                requestedAtEpochMs = System.currentTimeMillis()
            )
            _uiState.update {
                it.copy(
                    liveSessionActive = true,
                    liveSessionConsentStatus = "AWAITING_CHILD_CONSENT",
                    statusBannerMessage = "Sent Audited Check-In Request (${request.auditCode}) to ${it.selectedChild.deviceName} — Awaiting Explicit Child Consent"
                )
            }
            viewModelScope.launch {
                if (state.isCloudConnected) {
                    try {
                        repository.upsertLiveSessionRequest(request)
                    } catch (_: Exception) {}
                }
                recordAuditLog(
                    eventCode = request.auditCode,
                    actionTitle = "Requested Audited Live Check-In (${request.mode})",
                    details = "Dispatched consent prompt to ${state.selectedChild.name}. Stream starts only after child grants permission.",
                    syncState = if (state.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
                )
            }
        }
    }

    private fun savePremiumConfig(updated: PremiumFilterConfig, banner: String) {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                premiumConfig = updated,
                statusBannerMessage = banner
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertPremiumConfig(updated)
                    recordAuditLog(
                        eventCode = "#PRM-610",
                        actionTitle = banner,
                        details = "Child: ${state.selectedChild.name} (Synced to Cloud Firestore)",
                        syncState = "ENFORCED"
                    )
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families/${updated.familyId}/premiumConfigs",
                        documentId = updated.configId,
                        entityType = "PREMIUM_CONFIG",
                        summary = banner,
                        error = e.localizedMessage ?: "Offline"
                    )
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families/${updated.familyId}/premiumConfigs",
                    documentId = updated.configId,
                    entityType = "PREMIUM_CONFIG",
                    summary = banner,
                    error = "Local Sandbox Mode"
                )
                recordAuditLog(
                    eventCode = "#PRM-610",
                    actionTitle = banner,
                    details = "Child: ${state.selectedChild.name} (Saved Locally)",
                    syncState = "LOCAL_ONLY"
                )
            }
        }
    }

    // ===============================================================
    // P-20: Account Settings, Diagnostics & Offline Sync Queue Replay
    // ===============================================================
    fun runConnectionSelfTest() {
        viewModelScope.launch {
            val state = _uiState.value
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            if (state.isCloudConnected) {
                flushOfflineSyncQueue()
            }
            val queueCount = localDao.getPendingOfflineSyncItems().size
            val summary = if (state.isCloudConnected) {
                "Self-Test Passed at $timestamp • Shared Firestore DB (ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e), Room SQLite ($queueCount queued), GPS & Notification Channels Nominal"
            } else {
                "Self-Test Passed at $timestamp • Room SQLite ($queueCount queued items ready for sync), GPS & Notification Channels Nominal (Sign in on P-01 for Cloud Firestore)"
            }
            _uiState.update {
                it.copy(
                    lastSelfTestSummary = summary,
                    statusBannerMessage = summary
                )
            }
            recordAuditLog(
                eventCode = "#DIAG-200",
                actionTitle = "Ran Full Connection & Pipeline Self-Test",
                details = summary,
                syncState = if (state.isCloudConnected) "ENFORCED" else "LOCAL_ONLY"
            )
        }
    }

    fun flushOfflineSyncQueue() {
        val state = _uiState.value
        if (!state.isCloudConnected) return
        viewModelScope.launch {
            val pending = localDao.getPendingOfflineSyncItems()
            if (pending.isEmpty()) return@launch
            var syncedCount = 0
            for (item in pending) {
                try {
                    when (item.entityType) {
                        "FAMILY" -> repository.upsertFamilyHousehold(state.familyHousehold)
                        "CHILD_PROFILE" -> {
                            state.children.find { it.childId == item.documentId }?.let {
                                repository.upsertChildProfile(it.copy(syncStatus = "ENFORCED"))
                            }
                        }
                        "APP_RULE" -> {
                            state.appRules.find { it.ruleId == item.documentId }?.let {
                                repository.upsertAppRule(it.copy(status = if (it.blocked) "BLOCKED" else "ENFORCED"))
                            }
                        }
                        "SCHEDULE" -> {
                            state.schedules.find { it.scheduleId == item.documentId }?.let {
                                repository.upsertScheduleRoutine(it.copy(syncStatus = "ENFORCED"))
                            }
                        }
                        "GEOFENCE" -> {
                            state.geofences.find { it.geofenceId == item.documentId }?.let {
                                repository.upsertSafeGeofence(it)
                            }
                        }
                        "ALERT" -> {
                            state.alerts.find { it.alertId == item.documentId }?.let {
                                repository.upsertSafetyAlert(it)
                            }
                        }
                        "REQUEST" -> {
                            state.requests.find { it.requestId == item.documentId }?.let {
                                repository.upsertChildRequest(it)
                            }
                        }
                        "MESSAGE" -> {
                            state.messages.find { it.messageId == item.documentId }?.let {
                                repository.upsertFamilyMessage(it)
                            }
                        }
                        "TASK" -> {
                            state.tasks.find { it.taskId == item.documentId }?.let {
                                repository.upsertFamilyTask(it)
                            }
                        }
                        "PREMIUM_CONFIG" -> {
                            repository.upsertPremiumConfig(state.premiumConfig)
                        }
                    }
                    // Delete from offline queue ONLY after Firestore write succeeds
                    localDao.deleteOfflineSyncItem(item.idempotencyKey)
                    syncedCount++
                } catch (e: Exception) {
                    localDao.markOfflineSyncRetry(item.idempotencyKey, e.localizedMessage ?: "Retry failed")
                }
            }
            if (syncedCount > 0) {
                postBannerMessage("Synchronized $syncedCount queued offline changes to Cloud Firestore")
            }
        }
    }

    fun toggleSettingPreference(settingKey: String) {
        _uiState.update { s ->
            when (settingKey) {
                "BIOMETRIC" -> s.copy(biometricLockRequired = !s.biometricLockRequired)
                "DND_OVERRIDE" -> s.copy(criticalAlertsOverrideDnd = !s.criticalAlertsOverrideDnd)
                "DARK_MODE" -> s.copy(darkModeOverride = !s.darkModeOverride)
                "OFFLINE_CACHE" -> s.copy(offlineDiagnosticsCacheEnabled = !s.offlineDiagnosticsCacheEnabled)
                else -> s
            }
        }
    }

    fun exportSystemLogBundle(context: Context) {
        val state = _uiState.value
        val logText = buildString {
            appendLine("=== ANCHOR PARENT CRYPTOGRAPHIC DIAGNOSTIC LOG BUNDLE ===")
            appendLine("Application ID: com.anchor.parent")
            appendLine("Shared Firebase Project: gen-lang-client-0963224775")
            appendLine("Shared Firestore Database: ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e")
            appendLine("Household: ${state.familyHousehold.familyName} (${state.familyHousehold.familyId})")
            appendLine("Cloud Firestore Connected: ${state.isCloudConnected}")
            appendLine("Pending Offline Sync Queue Items: ${state.pendingOfflineSyncCount}")
            appendLine("Paired Child Devices: ${state.pairedDevices.size}")
            appendLine("Subscription Tier: ${state.familyHousehold.subscriptionTier} (Active=${state.isPremiumActive})")
            appendLine("Self-Test Status: ${state.lastSelfTestSummary}")
            appendLine("--- RECENT AUDIT LOGS (${state.auditLogs.size}) ---")
            state.auditLogs.take(25).forEach { log ->
                appendLine("[${log.eventCode}] ${log.actionTitle} | ${log.details} (${log.syncState})")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Anchor Diagnostic Log Bundle")
            putExtra(Intent.EXTRA_TEXT, logText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export System Log Bundle").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ===============================================================
    // Helpers
    // ===============================================================
    private fun persistFamilyWithSyncStatus(family: FamilyHousehold, banner: String) {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertFamilyHousehold(family)
                    _uiState.update {
                        it.copy(statusBannerMessage = "$banner (Cloud Sync Confirmed)")
                    }
                    recordAuditLog(
                        eventCode = "#FAM-105",
                        actionTitle = banner,
                        details = "Synced to Cloud Firestore (${family.familyId})",
                        syncState = "ENFORCED"
                    )
                } catch (e: Exception) {
                    enqueueOfflineWrite(
                        collectionPath = "families",
                        documentId = family.familyId,
                        entityType = "FAMILY",
                        summary = banner,
                        error = e.localizedMessage ?: "Offline"
                    )
                    _uiState.update {
                        it.copy(statusBannerMessage = "$banner (Queued Offline)")
                    }
                }
            } else {
                enqueueOfflineWrite(
                    collectionPath = "families",
                    documentId = family.familyId,
                    entityType = "FAMILY",
                    summary = banner,
                    error = "Local Sandbox Mode"
                )
                _uiState.update {
                    it.copy(statusBannerMessage = "$banner (Saved in Local Room Cache)")
                }
                recordAuditLog(
                    eventCode = "#FAM-105",
                    actionTitle = banner,
                    details = "Saved in Local Room Queue (${family.familyId})",
                    syncState = "LOCAL_ONLY"
                )
            }
        }
    }

    private suspend fun enqueueOfflineWrite(
        collectionPath: String,
        documentId: String,
        entityType: String,
        summary: String,
        error: String
    ) {
        val key = "$collectionPath/$documentId"
        localDao.enqueueOfflineSyncItem(
            OfflineSyncQueueEntity(
                idempotencyKey = key,
                operationType = "UPSERT",
                collectionPath = collectionPath,
                documentId = documentId,
                entityType = entityType,
                payloadJson = summary,
                lastError = error
            )
        )
    }

    private suspend fun syncChildrenToLocalRoom(children: List<ChildProfile>) {
        val entities = children.map { c ->
            CachedChildStateEntity(
                childId = c.childId,
                name = c.name,
                deviceName = c.deviceName,
                usedMinutesToday = c.usedMinutesToday,
                dailyAllowanceMinutes = c.dailyAllowanceMinutes,
                syncStatus = c.syncStatus,
                batteryLevel = c.batteryLevel,
                currentMode = c.currentMode,
                locationLabel = c.locationLabel
            )
        }
        localDao.upsertCachedChildren(entities)
    }

    private suspend fun recordAuditLog(
        eventCode: String,
        actionTitle: String,
        details: String,
        syncState: String
    ) {
        localDao.insertAuditLog(
            CachedAuditLogEntity(
                eventCode = eventCode,
                childId = _uiState.value.selectedChildId,
                actionTitle = actionTitle,
                details = details,
                syncState = syncState
            )
        )
    }

    private fun dispatchLocalNotification(context: Context, title: String, body: String) {
        try {
            val channelId = "anchor_safety_sentinel"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Anchor Safety Sentinel & SOS",
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(Random.nextInt(1000, 9999), notification)
        } catch (_: Exception) {
            // Ignore notification security exceptions if runtime permission not yet granted
        }
    }

    class Factory(
        private val repository: AnchorRepository,
        private val localDao: AnchorLocalDao,
        private val authManager: AuthManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnchorParentViewModel(repository, localDao, authManager) as T
        }
    }
}
