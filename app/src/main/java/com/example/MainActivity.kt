package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.data.local.AnchorLocalDatabase
import com.example.data.remote.AnchorRepository
import com.example.model.AnchorScreen
import com.example.ui.components.ScreenDirectoryDialog
import com.example.ui.components.StitchBottomNavBar
import com.example.ui.components.StitchTopAppBar
import com.example.ui.screens.P01WelcomeSignInScreen
import com.example.ui.screens.P02FamilyOnboardingScreen
import com.example.ui.screens.P03AddChildPairCodeScreen
import com.example.ui.screens.P04FamilyDashboardScreen
import com.example.ui.screens.P05ChildProfileDeviceStatusScreen
import com.example.ui.screens.P06ScreenTimeAnalyticsScreen
import com.example.ui.screens.P07AppUsageLimitsScreen
import com.example.ui.screens.P08SchedulesDowntimeScreen
import com.example.ui.screens.P09LocationGeofencesScreen
import com.example.ui.screens.P10AlertsSosActivityScreen
import com.example.ui.screens.P11ExtraTimeAppRequestsScreen
import com.example.ui.screens.P12FamilyChatTasksScreen
import com.example.ui.screens.P13GuardianRolesTrustedContactsScreen
import com.example.ui.screens.P14PrivacyExportDeletionScreen
import com.example.ui.screens.P15PremiumFeaturesScreen
import com.example.ui.screens.P16WebFilterRulesScreen
import com.example.ui.screens.P17SocialAppContentFiltersScreen
import com.example.ui.screens.P18NotificationFilterScreen
import com.example.ui.screens.P19LiveSessionViewerScreen
import com.example.ui.screens.P20AccountSettingsDiagnosticsScreen
import com.example.ui.theme.AnchorParentTheme
import com.example.ui.theme.AnchorPrimary
import com.example.viewmodel.AnchorParentViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebaseSafely()
        enableEdgeToEdge()

        val repository = AnchorRepository.create(applicationContext)
        val localDao = AnchorLocalDatabase.getInstance(applicationContext).anchorLocalDao()
        val authManager = AuthManager()

        setContent {
            val viewModel: AnchorParentViewModel = viewModel(
                factory = AnchorParentViewModel.Factory(repository, localDao, authManager)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AnchorParentTheme(darkTheme = uiState.darkModeOverride) {
                AnchorParentRoot(viewModel = viewModel)
            }
        }
    }

    private fun initializeFirebaseSafely() {
        try {
            FirebaseApp.initializeApp(this)
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } catch (_: Exception) {
            // Safe fallback in unit test or uninitialized environment
        }
    }
}

@Composable
fun AnchorParentRoot(viewModel: AnchorParentViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showScreenDirectory by remember { mutableStateOf(false) }

    // Request runtime notification/location permissions gracefully when signed in
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handled gracefully */ }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            try {
                permissionLauncher.launch(perms.toTypedArray())
            } catch (_: Exception) {
                // Ignore if ActivityResultRegistry not active in test harness
            }
        }
    }

    // Mandatory BackHandler for state-switched navigation
    BackHandler(enabled = state.backStack.isNotEmpty() || state.currentScreen != AnchorScreen.P04_FAMILY_DASHBOARD) {
        if (!viewModel.navigateBack()) {
            if (state.currentScreen != AnchorScreen.P01_WELCOME_SIGN_IN &&
                state.currentScreen != AnchorScreen.P04_FAMILY_DASHBOARD
            ) {
                viewModel.navigateTo(AnchorScreen.P04_FAMILY_DASHBOARD)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            StitchTopAppBar(
                currentScreen = state.currentScreen,
                familyName = state.familyHousehold.familyName,
                isCloudConnected = state.isCloudConnected,
                isPremiumActive = state.isPremiumActive,
                canNavigateBack = state.backStack.isNotEmpty(),
                onBack = { viewModel.navigateBack() },
                onOpenDirectory = { showScreenDirectory = true },
                onOpenPremium = { viewModel.navigateTo(AnchorScreen.P15_PREMIUM_FEATURES) }
            )
        },
        bottomBar = {
            if (state.currentScreen != AnchorScreen.P01_WELCOME_SIGN_IN) {
                StitchBottomNavBar(
                    currentScreen = state.currentScreen,
                    pendingRequestsCount = state.pendingRequestsCount,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Feedback Banner
            state.statusBannerMessage?.let { msg ->
                Surface(
                    color = AnchorPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("status_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearBannerMessage() },
                            modifier = Modifier.clickable { viewModel.clearBannerMessage() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Notice",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (state.currentScreen) {
                    AnchorScreen.P01_WELCOME_SIGN_IN -> P01WelcomeSignInScreen(
                        state = state,
                        onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                        onEnterGuardianWorkspace = { viewModel.enterGuardianWorkspace(it) },
                        onStartOnboarding = { viewModel.navigateTo(AnchorScreen.P02_FAMILY_ONBOARDING) }
                    )
                    AnchorScreen.P02_FAMILY_ONBOARDING -> P02FamilyOnboardingScreen(
                        state = state,
                        onSaveHousehold = { fam, guard, role, cad ->
                            viewModel.updateOnboardingHousehold(fam, guard, role, cad)
                        },
                        onAddChild = { name, age, grade, dev ->
                            viewModel.addChildToHousehold(name, age, grade, dev)
                        },
                        onProceedToPairing = { viewModel.navigateTo(AnchorScreen.P03_ADD_CHILD_PAIR_CODE) }
                    )
                    AnchorScreen.P03_ADD_CHILD_PAIR_CODE -> P03AddChildPairCodeScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onGenerateNewCode = { viewModel.generateNewPairingCode() },
                        onCopyCode = { viewModel.copyPairingCodeToClipboard(context) },
                        onShareInstructions = { viewModel.sharePairingInstructions(context) },
                        onFinishToDashboard = { viewModel.enterGuardianWorkspace(AnchorScreen.P04_FAMILY_DASHBOARD) }
                    )
                    AnchorScreen.P04_FAMILY_DASHBOARD -> P04FamilyDashboardScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onNavigate = { viewModel.navigateTo(it) },
                        onTogglePauseDevice = { viewModel.togglePauseChildDevice(it) },
                        onGrantBonusMinutes = { viewModel.grantBonusMinutes(it) },
                        onResolveRequest = { id, ok, mins, summary ->
                            viewModel.resolveChildRequest(id, ok, mins, summary)
                        }
                    )
                    AnchorScreen.P05_CHILD_PROFILE_DEVICE -> P05ChildProfileDeviceStatusScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onQuickAction = { viewModel.triggerDeviceQuickAction(it) },
                        onTogglePauseDevice = { viewModel.togglePauseChildDevice(it) },
                        onPairReplacementDevice = { viewModel.navigateTo(AnchorScreen.P03_ADD_CHILD_PAIR_CODE) }
                    )
                    AnchorScreen.P06_SCREEN_TIME_ANALYTICS -> P06ScreenTimeAnalyticsScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onSelectRange = { viewModel.setAnalyticsRange(it) },
                        onExportReport = { viewModel.exportAnalyticsReport(context) },
                        onNavigateToAppLimits = { viewModel.navigateTo(AnchorScreen.P07_APP_USAGE_LIMITS) }
                    )
                    AnchorScreen.P07_APP_USAGE_LIMITS -> P07AppUsageLimitsScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onAdjustDailyAllowance = { viewModel.adjustDailyAllowance(it) },
                        onToggleAppBlock = { viewModel.toggleAppBlock(it) },
                        onAdjustAppLimit = { id, delta -> viewModel.adjustAppRuleLimit(id, delta) },
                        onAddCustomAppRule = { name, cat, lim -> viewModel.addCustomAppRule(name, cat, lim) },
                        onNavigateToSchedules = { viewModel.navigateTo(AnchorScreen.P08_SCHEDULES_DOWNTIME) }
                    )
                    AnchorScreen.P08_SCHEDULES_DOWNTIME -> P08SchedulesDowntimeScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onToggleSchedule = { viewModel.toggleScheduleRoutine(it) },
                        onAddSchedule = { t, w, d, r -> viewModel.addCustomSchedule(t, w, d, r) },
                        onInstantFocusPause = { viewModel.togglePauseChildDevice(it) },
                        onUpdateFairPlayNotice = { viewModel.updateFairPlayNoticeMinutes(it) }
                    )
                    AnchorScreen.P09_LOCATION_GEOFENCES -> P09LocationGeofencesScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onPingGps = { viewModel.pingChildGps() },
                        onOpenDirections = { viewModel.openDirectionsToChild(context) },
                        onToggleGeofenceAlerts = { viewModel.toggleGeofenceAlerts(it) },
                        onAddGeofence = { n, r, s -> viewModel.addSafeGeofence(n, r, s) },
                        onDeleteGeofence = { viewModel.deleteSafeGeofence(it) }
                    )
                    AnchorScreen.P10_ALERTS_SOS_ACTIVITY -> P10AlertsSosActivityScreen(
                        state = state,
                        onTriggerSosDrill = { viewModel.triggerTestSosSirenDrill(context) },
                        onAcknowledgeAlert = { viewModel.acknowledgeAlert(it) }
                    )
                    AnchorScreen.P11_EXTRA_TIME_REQUESTS -> P11ExtraTimeAppRequestsScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onResolveRequest = { id, ok, mins, summary ->
                            viewModel.resolveChildRequest(id, ok, mins, summary)
                        },
                        onToggleAutoApproveLearning = { viewModel.toggleAutoApproveLearningApps() }
                    )
                    AnchorScreen.P12_FAMILY_CHAT_TASKS -> P12FamilyChatTasksScreen(
                        state = state,
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        onAssignTask = { t, r, d -> viewModel.assignFamilyTask(t, r, d) },
                        onApproveTask = { viewModel.approveFamilyTask(it) }
                    )
                    AnchorScreen.P13_GUARDIAN_ROLES_CONTACTS -> P13GuardianRolesTrustedContactsScreen(
                        state = state,
                        onToggleGeofenceAlerts = { viewModel.toggleGuardianGeofenceAlerts(it) },
                        onInviteContact = { n, r, e, p -> viewModel.inviteGuardianOrContact(n, r, e, p) }
                    )
                    AnchorScreen.P14_PRIVACY_EXPORT_DELETION -> P14PrivacyExportDeletionScreen(
                        state = state,
                        onExportJsonArchive = { viewModel.exportHouseholdDataJson(context) },
                        onPurgeHistoricalLogs = { viewModel.purgeHistoricalLogs() },
                        onToggleAnonymousTelemetry = { viewModel.toggleAnonymousTelemetry() },
                        onDeleteHousehold = { viewModel.initiateFamilyDeletion() }
                    )
                    AnchorScreen.P15_PREMIUM_FEATURES -> P15PremiumFeaturesScreen(
                        state = state,
                        onSetSubscriptionTier = { tier, active -> viewModel.setSubscriptionTier(tier, active) },
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    AnchorScreen.P16_WEB_FILTER_RULES -> P16WebFilterRulesScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onSelectProfile = { viewModel.updateWebFilterProfile(it) },
                        onToggleCategory = { viewModel.toggleWebBlockedCategory(it) },
                        onAddWhitelist = { viewModel.addWhitelistedDomain(it) },
                        onRemoveWhitelist = { viewModel.removeWhitelistedDomain(it) },
                        onAddBlacklist = { viewModel.addBlacklistedDomain(it) },
                        onRemoveBlacklist = { viewModel.removeBlacklistedDomain(it) },
                        onNavigateToPremium = { viewModel.navigateTo(AnchorScreen.P15_PREMIUM_FEATURES) },
                        onInstantUnlockTrial = { viewModel.setSubscriptionTier("PREMIUM_ANNUAL", true) }
                    )
                    AnchorScreen.P17_SOCIAL_CONTENT_FILTERS -> P17SocialAppContentFiltersScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onSelectShieldProfile = { viewModel.updateSocialShieldProfile(it) },
                        onTogglePlatformRule = { viewModel.toggleSocialPlatformRule(it) },
                        onNavigateToPremium = { viewModel.navigateTo(AnchorScreen.P15_PREMIUM_FEATURES) },
                        onInstantUnlockTrial = { viewModel.setSubscriptionTier("PREMIUM_ANNUAL", true) }
                    )
                    AnchorScreen.P18_NOTIFICATION_FILTER -> P18NotificationFilterScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onUpdateConfig = { en, proto, batch, streak ->
                            viewModel.updateNotificationFilterConfig(en, proto, batch, streak)
                        },
                        onNavigateToPremium = { viewModel.navigateTo(AnchorScreen.P15_PREMIUM_FEATURES) },
                        onInstantUnlockTrial = { viewModel.setSubscriptionTier("PREMIUM_ANNUAL", true) }
                    )
                    AnchorScreen.P19_LIVE_SESSION_VIEWER -> P19LiveSessionViewerScreen(
                        state = state,
                        onSelectChild = { viewModel.selectChild(it) },
                        onSelectMode = { viewModel.setLiveSessionMode(it) },
                        onToggleTwoWayTalk = { viewModel.toggleLiveSessionTwoWayTalk() },
                        onToggleSessionActive = { viewModel.toggleLiveSessionActive() },
                        onNavigateToPremium = { viewModel.navigateTo(AnchorScreen.P15_PREMIUM_FEATURES) },
                        onInstantUnlockTrial = { viewModel.setSubscriptionTier("PREMIUM_ANNUAL", true) }
                    )
                    AnchorScreen.P20_ACCOUNT_SETTINGS_DIAGNOSTICS -> P20AccountSettingsDiagnosticsScreen(
                        state = state,
                        onRunSelfTest = { viewModel.runConnectionSelfTest() },
                        onTogglePreference = { viewModel.toggleSettingPreference(it) },
                        onExportSystemLog = { viewModel.exportSystemLogBundle(context) },
                        onNavigate = { viewModel.navigateTo(it) },
                        onSignOut = { viewModel.signOutGuardian(context) }
                    )
                }
            }
        }
    }

    if (showScreenDirectory) {
        ScreenDirectoryDialog(
            currentScreen = state.currentScreen,
            isPremiumActive = state.isPremiumActive,
            onSelectScreen = { viewModel.navigateTo(it) },
            onDismiss = { showScreenDirectory = false }
        )
    }
}
