package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class AnchorScreen(val code: String, val title: String, val isPremium: Boolean = false) {
    P01_WELCOME_SIGN_IN("P-01", "Welcome & Sign In"),
    P02_FAMILY_ONBOARDING("P-02", "Family Onboarding"),
    P03_ADD_CHILD_PAIR_CODE("P-03", "Add Child & Pair Device"),
    P04_FAMILY_DASHBOARD("P-04", "Family Dashboard"),
    P05_CHILD_PROFILE_DEVICE("P-05", "Child Profile & Device Status"),
    P06_SCREEN_TIME_ANALYTICS("P-06", "Screen-Time Analytics"),
    P07_APP_USAGE_LIMITS("P-07", "App Usage & Limits"),
    P08_SCHEDULES_DOWNTIME("P-08", "Schedules & Downtime"),
    P09_LOCATION_GEOFENCES("P-09", "Location & Geofences"),
    P10_ALERTS_SOS_ACTIVITY("P-10", "Alerts & SOS Activity"),
    P11_EXTRA_TIME_REQUESTS("P-11", "Extra Time & App Requests"),
    P12_FAMILY_CHAT_TASKS("P-12", "Family Chat & Shared Tasks"),
    P13_GUARDIAN_ROLES_CONTACTS("P-13", "Guardian Roles & Trusted Contacts"),
    P14_PRIVACY_EXPORT_DELETION("P-14", "Privacy, Data Rights & Deletion"),
    P15_PREMIUM_FEATURES("P-15", "Premium Features & Subscription"),
    P16_WEB_FILTER_RULES("P-16", "Web & Content Filters", isPremium = true),
    P17_SOCIAL_CONTENT_FILTERS("P-17", "Social App & Content Filters", isPremium = true),
    P18_NOTIFICATION_FILTER("P-18", "Notification & Distraction Filter", isPremium = true),
    P19_LIVE_SESSION_VIEWER("P-19", "Active Live Session Viewer", isPremium = true),
    P20_ACCOUNT_SETTINGS_DIAGNOSTICS("P-20", "Account Settings & Diagnostics")
}

data class UserProfile(
    @DocumentId var id: String = "",
    var uid: String = "",
    var displayName: String = "",
    var email: String = "",
    var role: String = "PRIMARY_GUARDIAN",
    var familyId: String = "",
    var notificationCadence: String = "ALL_EVENTS",
    @ServerTimestamp var createdAt: Timestamp? = null,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class FamilyHousehold(
    @DocumentId var id: String = "",
    var familyId: String = "",
    var familyName: String = "My Family",
    var ownerUid: String = "",
    var subscriptionTier: String = "FREE", // FREE, PREMIUM_ANNUAL, PREMIUM_MONTHLY
    var subscriptionActive: Boolean = false,
    var autoApproveLearningApps: Boolean = true,
    var fairPlayNoticeMinutes: Int = 5,
    @ServerTimestamp var createdAt: Timestamp? = null,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class ChildProfile(
    @DocumentId var id: String = "",
    var childId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var pairedChildUid: String = "",
    var name: String = "",
    var age: Int = 0,
    var gradeLabel: String = "",
    var avatarInitials: String = "",
    var deviceName: String = "",
    var deviceModel: String = "",
    var osVersion: String = "",
    var batteryLevel: Int = 0,
    var chargingStatus: String = "Unknown",
    var networkType: String = "Unknown",
    var networkName: String = "",
    var storageFreeGb: Double = 0.0,
    var syncStatus: String = "PENDING", // ENFORCED, PENDING, FAILED, UNSUPPORTED, QUEUED_OFFLINE, LOCAL_ONLY
    var lastSyncLabel: String = "Not synced",
    var usedMinutesToday: Int = 0,
    var dailyAllowanceMinutes: Int = 0,
    var currentMode: String = "Not configured",
    var locationLabel: String = "Location unavailable",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var knoxVerified: Boolean = true,
    var accessibilityEnabled: Boolean = true,
    var deviceAdminEnabled: Boolean = true,
    var policyPausedUntil: String = "",
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class PairingCode(
    @DocumentId var id: String = "",
    var codeId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var childName: String = "",
    var code: String = "",
    var expiresAtEpochMs: Long = 0L,
    var status: String = "NONE", // ACTIVE, CONSUMED, EXPIRED, REVOKED
    var revoked: Boolean = false,
    var attemptCount: Int = 0,
    var maxAttempts: Int = 5,
    var consumedByChildUid: String = "",
    var consumedAtEpochMs: Long = 0L,
    var revokedAtEpochMs: Long = 0L,
    @ServerTimestamp var createdAt: Timestamp? = null
)

data class PairedDevice(
    @DocumentId var id: String = "",
    var deviceId: String = "",
    var childUid: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var childName: String = "",
    var codeId: String = "",
    var redeemedCode: String = "",
    var deviceName: String = "",
    var deviceModel: String = "",
    var osVersion: String = "",
    var status: String = "PAIRED", // PAIRED, REVOKED
    var pairedAtEpochMs: Long = 0L,
    var lastHeartbeatEpochMs: Long = 0L,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class AppRule(
    @DocumentId var id: String = "",
    var ruleId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var packageName: String = "",
    var appName: String = "",
    var category: String = "Games & Entertainment",
    var dailyLimitMinutes: Int = 45,
    var usedMinutesToday: Int = 30,
    var status: String = "ENFORCED", // ENFORCED, PENDING, SCHEDULE_ONLY, WHITELISTED, BLOCKED
    var blocked: Boolean = false,
    var whitelisted: Boolean = false,
    var scheduleOnly: Boolean = false,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class ScheduleRoutine(
    @DocumentId var id: String = "",
    var scheduleId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var title: String = "",
    var windowDescription: String = "",
    var daysSummary: String = "Sun, Mon, Tue, Wed, Thu",
    var ruleSummary: String = "Phone calls, Emergency SOS & Alarm Clock only",
    var iconType: String = "BEDTIME", // BEDTIME, SCHOOL, DINNER, CUSTOM
    var enabled: Boolean = true,
    var syncStatus: String = "ENFORCED",
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class SafeGeofence(
    @DocumentId var id: String = "",
    var geofenceId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var name: String = "",
    var radiusMeters: Int = 250,
    var triggerSummary: String = "Notify on Arrival & Departure",
    var scheduleSummary: String = "Mon–Fri • 7:45 AM – 3:30 PM",
    var activeInside: Boolean = false,
    var alertsEnabled: Boolean = true,
    var latitude: Double = 37.7749,
    var longitude: Double = -122.4194,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class SafetyAlert(
    @DocumentId var id: String = "",
    var alertId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var childName: String = "Leo",
    var title: String = "",
    var description: String = "",
    var category: String = "GEOFENCE", // SOS, GEOFENCE, DEVICE_HEALTH, SYSTEM
    var severity: String = "INFO", // INFO, WARNING, CRITICAL
    var timestampLabel: String = "Today • 8:02 AM",
    var acknowledged: Boolean = true,
    @ServerTimestamp var createdAt: Timestamp? = null
)

data class ChildRequest(
    @DocumentId var id: String = "",
    var requestId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var childName: String = "Leo",
    var requestType: String = "EXTRA_TIME", // EXTRA_TIME, NEW_APP
    var targetName: String = "Minecraft",
    var targetCategory: String = "Games & Entertainment",
    var requestedBonusMinutes: Int = 15,
    var childNote: String = "",
    var usedTodaySummary: String = "45m / 45m daily cap reached",
    var status: String = "PENDING", // PENDING, APPROVED, DECLINED
    var decisionSummary: String = "",
    var decidedBy: String = "",
    @ServerTimestamp var createdAt: Timestamp? = null,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class FamilyMessage(
    @DocumentId var id: String = "",
    var messageId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var senderName: String = "",
    var senderRole: String = "PARENT", // PARENT, CHILD, SYSTEM
    var text: String = "",
    @get:PropertyName("isSystemEvent") @set:PropertyName("isSystemEvent")
    var isSystemEvent: Boolean = false,
    var timestampLabel: String = "3:42 PM",
    @ServerTimestamp var createdAt: Timestamp? = null
)

data class FamilyTask(
    @DocumentId var id: String = "",
    var taskId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var title: String = "",
    var rewardSummary: String = "+15m Bonus Screen Time",
    var dueSummary: String = "Due Today by 6:00 PM",
    var status: String = "IN_PROGRESS", // IN_PROGRESS, READY_FOR_REVIEW, COMPLETED
    var completedByChild: Boolean = false,
    var approvedByParent: Boolean = false,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class GuardianContact(
    @DocumentId var id: String = "",
    var guardianId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var name: String = "",
    var relationRole: String = "",
    var emailOrPhone: String = "",
    @get:PropertyName("isPrimaryGuardian") @set:PropertyName("isPrimaryGuardian")
    var isPrimaryGuardian: Boolean = true,
    var permissionsSummary: String = "Full Policy & SOS Authority",
    var receiveGeofenceAlerts: Boolean = true,
    var receiveSosSms: Boolean = true,
    var status: String = "Verified Active",
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class PremiumFilterConfig(
    @DocumentId var id: String = "",
    var configId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var webFilterProfile: String = "BALANCED_GUARDIAN", // STRICT_SAFEGUARD, BALANCED_GUARDIAN, CUSTOM_GRANULAR
    var blockedCategories: List<String> = listOf(
        "Adult & Explicit Content",
        "Gambling, Betting & Loot Boxes",
        "Violence, Hate & Extremism",
        "Social Media Web Portals"
    ),
    var whitelistedDomains: List<String> = listOf(
        "khanacademy.org",
        "duolingo.com",
        "pbskids.org",
        "nasa.gov",
        "oakridgeschools.edu"
    ),
    var blacklistedDomains: List<String> = listOf(
        "omegle.com",
        "4chan.org",
        "discord.com/login",
        "unblockedgames76.io"
    ),
    var socialShieldProfile: String = "SUPERVISED", // SUPERVISED, STRICT_BLOCK, GRANULAR
    var discordDmWhitelistOnly: Boolean = true,
    var robloxChatFiltered: Boolean = true,
    var whatsappUnknownBlocked: Boolean = true,
    var youtubeRestrictedMode: Boolean = true,
    var notificationFilterEnabled: Boolean = true,
    var notificationProtocol: String = "BATCHING", // BATCHING, TOTAL_SILENCE, EMERGENCY_ONLY
    var batchMuteSocialDuringStudy: Boolean = true,
    var blockGamingStreakPrompts: Boolean = true,
    var allowLearningNotifications: Boolean = true,
    var liveSessionAuditCode: String = "#LIV-84920",
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class LiveSessionRequest(
    @DocumentId var id: String = "",
    var sessionId: String = "",
    var familyId: String = "",
    var ownerUid: String = "",
    var childId: String = "",
    var auditCode: String = "#LIV-84920",
    var mode: String = "Screen View",
    var reason: String = "Checking homework progress & safety check-in",
    var status: String = "AWAITING_CHILD_CONSENT", // AWAITING_CHILD_CONSENT, CONSENT_GRANTED, DECLINED_BY_CHILD, ENDED
    var consentGranted: Boolean = false,
    var requestedAtEpochMs: Long = 0L,
    @ServerTimestamp var updatedAt: Timestamp? = null
)
