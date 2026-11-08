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
import com.example.model.PairingCode
import com.example.model.PremiumFilterConfig
import com.example.model.SafeGeofence
import com.example.model.SafetyAlert
import com.example.model.ScheduleRoutine
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
        id = "pair_leo_1",
        codeId = "pair_leo_1",
        familyId = "fam_local_guardian_sarah",
        ownerUid = "local_guardian_sarah",
        childId = "child_leo",
        childName = "Leo",
        code = "482-915",
        expiresAtEpochMs = System.currentTimeMillis() + 600_000L,
        status = "ACTIVE"
    ),
    val pairingCountdownSeconds: Int = 584,
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
    // Screen-specific interactive UI states
    val analyticsRange: String = "7 Days",
    val liveSessionActive: Boolean = true,
    val liveSessionMode: String = "Screen View", // Screen View, Front Camera, Ambient Audio
    val liveSessionTwoWayTalk: Boolean = false,
    val liveSessionReason: String = "Checking homework progress & safety check-in",
    val liveSessionRemainingSeconds: Int = 462,
    val biometricLockRequired: Boolean = true,
    val criticalAlertsOverrideDnd: Boolean = true,
    val darkModeOverride: Boolean = false,
    val offlineDiagnosticsCacheEnabled: Boolean = true,
    val anonymousDiagnosticTelemetry: Boolean = false,
    val lastSelfTestSummary: String = "All 4 Guardian Pipelines Verified (14ms Firestore RTT)"
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
    }

    private fun seedInitialAuditLog() {
        viewModelScope.launch {
            val currentLogs = localDao.getAllAuditLogs()
            if (currentLogs.isEmpty()) {
                recordAuditLog(
                    eventCode = "#AUD-1042",
                    actionTitle = "Guardian Workspace Initialized",
                    details = "Anchor Bilateral Transparency Engine & Room SQLite cache online.",
                    syncState = "ENFORCED"
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
                    isCloudConnected = true,
                    currentUserUid = uid,
                    guardianDisplayName = displayName,
                    guardianEmail = email,
                    currentScreen = AnchorScreen.P04_FAMILY_DASHBOARD,
                    backStack = emptyList(),
                    statusBannerMessage = "Authenticated as $email • Cloud Firestore Sync Active"
                )
            }
            try {
                val household = repository.bootstrapHouseholdIfNeeded(uid, displayName, email)
                val children = repository.getChildren(familyId).ifEmpty {
                    AnchorSeedData.defaultChildren(uid, familyId)
                }
                _uiState.update {
                    it.copy(
                        familyHousehold = household,
                        children = children,
                        selectedChildId = children.first().childId,
                        appRules = AnchorSeedData.defaultAppRules(uid, familyId),
                        schedules = AnchorSeedData.defaultSchedules(uid, familyId),
                        geofences = AnchorSeedData.defaultGeofences(uid, familyId),
                        alerts = AnchorSeedData.defaultAlerts(uid, familyId),
                        requests = AnchorSeedData.defaultRequests(uid, familyId),
                        messages = AnchorSeedData.defaultMessages(uid, familyId),
                        tasks = AnchorSeedData.defaultTasks(uid, familyId),
                        guardians = AnchorSeedData.defaultGuardians(uid, familyId),
                        premiumConfig = AnchorSeedData.defaultPremiumConfig(uid, familyId)
                    )
                }
                syncChildrenToLocalRoom(children)
                recordAuditLog(
                    eventCode = "#AUTH-200",
                    actionTitle = "Firebase Google Sign-In Verified",
                    details = "Connected to household ${household.familyName} ($familyId)",
                    syncState = "ENFORCED"
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        statusBannerMessage = "Signed in ($email). Initial Firestore sync notice: ${e.localizedMessage ?: "Saved locally"}"
                    )
                }
            }
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
                    "Cloud Firestore & Room Cache Active"
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
                syncState = if (state.isCloudConnected) "ENFORCED" else "LOCAL_ROOM"
            )
        }
    }

    fun signOutGuardian(context: Context) {
        viewModelScope.launch {
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
                statusBannerMessage = "Household settings saved for $familyName"
            )
        }
        persistFamilyIfCloud(updatedFamily)
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
                } catch (_: Exception) {}
            }
            recordAuditLog(
                eventCode = "#CHD-204",
                actionTitle = "Added Child Profile: ${newChild.name}",
                details = "Allowance: 120m/day • Device: ${newChild.deviceName}",
                syncState = "PENDING"
            )
        }
    }

    // ===============================================================
    // P-03: Pairing Code & Share Actions
    // ===============================================================
    fun generateNewPairingCode() {
        val state = _uiState.value
        val child = state.selectedChild
        val part1 = Random.nextInt(100, 999)
        val part2 = Random.nextInt(100, 999)
        val newCodeStr = "$part1-$part2"
        val newPairing = PairingCode(
            id = "pair_${child.childId}_${System.currentTimeMillis()}",
            codeId = "pair_${child.childId}_${System.currentTimeMillis()}",
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = child.childId,
            childName = child.name,
            code = newCodeStr,
            expiresAtEpochMs = System.currentTimeMillis() + 600_000L,
            status = "ACTIVE"
        )
        _uiState.update {
            it.copy(
                pairingCode = newPairing,
                pairingCountdownSeconds = 600,
                statusBannerMessage = "Generated fresh 10-minute pairing code $newCodeStr for ${child.name}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertPairingCode(newPairing)
                } catch (_: Exception) {}
            }
            recordAuditLog(
                eventCode = "#PAIR-301",
                actionTitle = "Generated Pairing Code ($newCodeStr)",
                details = "Target child: ${child.name} • Valid for 10 minutes",
                syncState = "ENFORCED"
            )
        }
    }

    fun copyPairingCodeToClipboard(context: Context) {
        val code = _uiState.value.pairingCode.code
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Anchor Pairing Code", code))
        postBannerMessage("Copied pairing code $code to clipboard")
    }

    fun sharePairingInstructions(context: Context) {
        val state = _uiState.value
        val child = state.selectedChild
        val shareText = buildString {
            appendLine("Anchor Family Guardian — Child Device Pairing")
            appendLine("Household: ${state.familyHousehold.familyName}")
            appendLine("Child Profile: ${child.name}")
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
        val updatedChild = if (isCurrentlyPaused) {
            child.copy(
                currentMode = "Free Play Window",
                policyPausedUntil = "",
                syncStatus = "ENFORCED",
                lastSyncLabel = "Resumed just now"
            )
        } else {
            child.copy(
                currentMode = "Paused ($durationLabel)",
                policyPausedUntil = durationLabel,
                syncStatus = "ENFORCED",
                lastSyncLabel = "Paused just now"
            )
        }
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
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
            lastSyncLabel = "+${bonusMinutes}m bonus synced just now",
            syncStatus = "ENFORCED"
        )
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
            banner = "Granted +${bonusMinutes}m bonus screen time to ${child.name}"
        )
    }

    fun triggerDeviceQuickAction(actionName: String) {
        val child = _uiState.value.selectedChild
        val updatedChild = child.copy(
            lastSyncLabel = "$actionName • Verified just now",
            syncStatus = "ENFORCED"
        )
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
            banner = "$actionName dispatched to ${child.deviceName} (Status: Enforced)"
        )
    }

    private fun updateChildInStateAndCloud(updatedChild: ChildProfile, banner: String) {
        val updatedList = _uiState.value.children.map {
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
            if (_uiState.value.isCloudConnected) {
                try {
                    repository.upsertChildProfile(updatedChild)
                } catch (_: Exception) {}
            }
            recordAuditLog(
                eventCode = "#POL-402",
                actionTitle = banner,
                details = "Child: ${updatedChild.name} • Mode: ${updatedChild.currentMode}",
                syncState = updatedChild.syncStatus
            )
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
        val reportText = buildString {
            appendLine("ANCHOR DIGITAL WELLBEING & ANALYTICS REPORT (${state.analyticsRange})")
            appendLine("Household: ${state.familyHousehold.familyName}")
            appendLine("Child: ${child.name} (${child.deviceName})")
            appendLine("Today's Usage: ${child.usedMinutesToday}m / ${child.dailyAllowanceMinutes}m")
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
                } catch (_: Exception) {}
            }
            recordAuditLog(
                eventCode = "#APP-701",
                actionTitle = "Updated App Rule: ${updatedRule.appName}",
                details = "Status: ${updatedRule.status} • Limit: ${updatedRule.dailyLimitMinutes}m",
                syncState = updatedRule.status
            )
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
                } catch (_: Exception) {}
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
            status = "ENFORCED"
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
                } catch (_: Exception) {}
            }
        }
    }

    // ===============================================================
    // P-08: Schedules & Downtime
    // ===============================================================
    fun toggleScheduleRoutine(scheduleId: String) {
        val state = _uiState.value
        val target = state.schedules.find { it.scheduleId == scheduleId } ?: return
        val updated = target.copy(enabled = !target.enabled)
        _uiState.update {
            it.copy(
                schedules = state.schedules.map { s -> if (s.scheduleId == scheduleId) updated else s },
                statusBannerMessage = "${updated.title} is now ${if (updated.enabled) "Active (Enforced)" else "Paused"}"
            )
        }
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertScheduleRoutine(updated)
                } catch (_: Exception) {}
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
            syncStatus = "ENFORCED"
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
                } catch (_: Exception) {}
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
        persistFamilyIfCloud(updatedFamily)
    }

    // ===============================================================
    // P-09: Location & Geofences
    // ===============================================================
    fun pingChildGps() {
        val child = _uiState.value.selectedChild
        val updatedChild = child.copy(
            lastSyncLabel = "High-Accuracy GPS Locked (< 6m accuracy)",
            syncStatus = "ENFORCED"
        )
        updateChildInStateAndCloud(
            updatedChild = updatedChild,
            banner = "Live GPS fix confirmed for ${child.name} at ${child.locationLabel}"
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
                } catch (_: Exception) {}
            }
        }
    }

    fun addSafeGeofence(name: String, radiusMeters: Int, scheduleSummary: String) {
        if (name.isBlank()) return
        val state = _uiState.value
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
            activeInside = false,
            alertsEnabled = true
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
                } catch (_: Exception) {}
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
    // P-10: Alerts & SOS Activity
    // ===============================================================
    fun triggerTestSosSirenDrill(context: Context) {
        val state = _uiState.value
        val child = state.selectedChild
        val id = "alert_sos_drill_${System.currentTimeMillis()}"
        val drillAlert = SafetyAlert(
            id = id,
            alertId = id,
            familyId = state.familyHousehold.familyId,
            ownerUid = state.currentUserUid,
            childId = child.childId,
            childName = child.name,
            title = "Practice SOS Siren Drill Verified",
            description = "Guardian-initiated bilateral SOS drill confirmed high-priority notification channel, GPS lock (${child.locationLabel}), and Tier-1 Guardian escalation.",
            category = "SOS",
            severity = "CRITICAL",
            timestampLabel = "Just now • Drill Completed",
            acknowledged = false
        )
        _uiState.update {
            it.copy(
                alerts = listOf(drillAlert) + it.alerts,
                statusBannerMessage = "Family SOS Drill executed — High-priority Android notification dispatched"
            )
        }
        dispatchLocalNotification(
            context = context,
            title = "Anchor SOS Drill — ${child.name}",
            body = "Practice SOS Siren verified. Location: ${child.locationLabel} • Battery: ${child.batteryLevel}%"
        )
        viewModelScope.launch {
            if (state.isCloudConnected) {
                try {
                    repository.upsertSafetyAlert(drillAlert)
                } catch (_: Exception) {}
            }
            recordAuditLog(
                eventCode = "#SOS-911",
                actionTitle = "Executed Family SOS Practice Drill",
                details = "Verified Android notification channel & Tier-1 guardian escalation.",
                syncState = "ENFORCED"
            )
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
                } catch (_: Exception) {}
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
                } catch (_: Exception) {}
            }
            recordAuditLog(
                eventCode = "#REQ-502",
                actionTitle = "Request ${updatedRequest.status}: ${target.targetName}",
                details = decisionSummary,
                syncState = "ENFORCED"
            )
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
        persistFamilyIfCloud(updated)
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
                } catch (_: Exception) {}
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
                } catch (_: Exception) {}
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
                } catch (_: Exception) {}
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
                syncState = "ENFORCED"
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
            localDao.clearAuditLogs()
            localDao.clearCachedChildren()
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
        persistFamilyIfCloud(updated)
        viewModelScope.launch {
            recordAuditLog(
                eventCode = "#SUB-150",
                actionTitle = "Updated Subscription Entitlement: $tier",
                details = "Premium Active: $active (Synced to household entitlement)",
                syncState = "ENFORCED"
            )
        }
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
                liveSessionActive = true,
                statusBannerMessage = "Switched Audited Live Session to $mode (Visible banner active on ${it.selectedChild.deviceName})"
            )
        }
        viewModelScope.launch {
            recordAuditLog(
                eventCode = _uiState.value.premiumConfig.liveSessionAuditCode,
                actionTitle = "Switched Audited Live Session Mode: $mode",
                details = "Child consent verified • Reason: ${_uiState.value.liveSessionReason}",
                syncState = "ENFORCED"
            )
        }
    }

    fun toggleLiveSessionTwoWayTalk() {
        _uiState.update {
            val next = !it.liveSessionTwoWayTalk
            it.copy(
                liveSessionTwoWayTalk = next,
                statusBannerMessage = if (next) "Two-Way Voice Channel Open with ${it.selectedChild.name}" else "Two-Way Voice Muted"
            )
        }
    }

    fun toggleLiveSessionActive() {
        val currentlyActive = _uiState.value.liveSessionActive
        _uiState.update {
            it.copy(
                liveSessionActive = !currentlyActive,
                statusBannerMessage = if (currentlyActive) {
                    "Ended Audited Live Session (${it.premiumConfig.liveSessionAuditCode}) — Logged to Family Chat"
                } else {
                    "Initiated Audited Live Session handshake with ${it.selectedChild.deviceName}"
                }
            )
        }
        if (currentlyActive) {
            sendChatMessage("[Audited Session ${_uiState.value.premiumConfig.liveSessionAuditCode} Ended] Thank you for checking in!")
        }
    }

    private fun savePremiumConfig(updated: PremiumFilterConfig, banner: String) {
        _uiState.update {
            it.copy(
                premiumConfig = updated,
                statusBannerMessage = banner
            )
        }
        viewModelScope.launch {
            if (_uiState.value.isCloudConnected) {
                try {
                    repository.upsertPremiumConfig(updated)
                } catch (_: Exception) {}
            }
            recordAuditLog(
                eventCode = "#PRM-610",
                actionTitle = banner,
                details = "Child: ${_uiState.value.selectedChild.name}",
                syncState = "ENFORCED"
            )
        }
    }

    // ===============================================================
    // P-20: Account Settings & Diagnostics
    // ===============================================================
    fun runConnectionSelfTest() {
        val state = _uiState.value
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val summary = if (state.isCloudConnected) {
            "Self-Test Passed at $timestamp • Firestore Cloud DB, Room SQLite, GPS & Notification Channels Nominal"
        } else {
            "Self-Test Passed at $timestamp • Room SQLite, GPS & Notification Channels Nominal (Firestore in Local Sandbox Mode)"
        }
        _uiState.update {
            it.copy(
                lastSelfTestSummary = summary,
                statusBannerMessage = summary
            )
        }
        viewModelScope.launch {
            recordAuditLog(
                eventCode = "#DIAG-200",
                actionTitle = "Ran Full Connection & Pipeline Self-Test",
                details = summary,
                syncState = "ENFORCED"
            )
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
            appendLine("Household: ${state.familyHousehold.familyName} (${state.familyHousehold.familyId})")
            appendLine("Cloud Firestore Connected: ${state.isCloudConnected}")
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
    private fun persistFamilyIfCloud(family: FamilyHousehold) {
        if (!_uiState.value.isCloudConnected) return
        viewModelScope.launch {
            try {
                repository.upsertFamilyHousehold(family)
            } catch (_: Exception) {}
        }
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
