package com.example.model

object AnchorSeedData {

    fun defaultFamily(ownerUid: String, familyId: String = "fam_$ownerUid"): FamilyHousehold {
        return FamilyHousehold(
            id = familyId,
            familyId = familyId,
            familyName = "The Jenkins Family",
            ownerUid = ownerUid,
            subscriptionTier = "FREE",
            subscriptionActive = false,
            autoApproveLearningApps = true,
            fairPlayNoticeMinutes = 5
        )
    }

    fun defaultChildren(ownerUid: String, familyId: String = "fam_$ownerUid"): List<ChildProfile> {
        return listOf(
            ChildProfile(
                id = "child_leo",
                childId = "child_leo",
                familyId = familyId,
                ownerUid = ownerUid,
                name = "Leo",
                age = 11,
                gradeLabel = "6th Grade • Oakridge Middle",
                avatarInitials = "LJ",
                deviceName = "Leo's Galaxy A54",
                deviceModel = "Samsung Galaxy A54 5G (SM-A546U)",
                osVersion = "Android 14 • One UI 6.1",
                batteryLevel = 84,
                chargingStatus = "Discharging (~9h remaining)",
                networkType = "Wi-Fi 6",
                networkName = "Jenkins_Home_5G",
                storageFreeGb = 42.8,
                syncStatus = "ENFORCED",
                lastSyncLabel = "Synced 12s ago",
                usedMinutesToday = 105,
                dailyAllowanceMinutes = 150,
                currentMode = "Free Play Window",
                locationLabel = "Oakridge Middle School",
                latitude = 37.7749,
                longitude = -122.4194,
                knoxVerified = true,
                accessibilityEnabled = true,
                deviceAdminEnabled = true,
                policyPausedUntil = ""
            ),
            ChildProfile(
                id = "child_maya",
                childId = "child_maya",
                familyId = familyId,
                ownerUid = ownerUid,
                name = "Maya",
                age = 8,
                gradeLabel = "3rd Grade • Oakridge Elementary",
                avatarInitials = "MJ",
                deviceName = "Maya's Pixel Tablet",
                deviceModel = "Google Pixel Tablet (Wi-Fi)",
                osVersion = "Android 15",
                batteryLevel = 92,
                chargingStatus = "Docked on Speaker Base",
                networkType = "Wi-Fi 6",
                networkName = "Jenkins_Home_5G",
                storageFreeGb = 68.4,
                syncStatus = "ENFORCED",
                lastSyncLabel = "Synced 28s ago",
                usedMinutesToday = 40,
                dailyAllowanceMinutes = 90,
                currentMode = "Homework & Reading Focus",
                locationLabel = "Home Sanctuary",
                latitude = 37.7812,
                longitude = -122.4110,
                knoxVerified = false,
                accessibilityEnabled = true,
                deviceAdminEnabled = true,
                policyPausedUntil = ""
            )
        )
    }

    fun defaultAppRules(ownerUid: String, familyId: String = "fam_$ownerUid", childId: String = "child_leo"): List<AppRule> {
        return listOf(
            AppRule(
                id = "rule_${childId}_roblox",
                ruleId = "rule_${childId}_roblox",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                packageName = "com.roblox.client",
                appName = "Roblox",
                category = "Games & Entertainment",
                dailyLimitMinutes = 45,
                usedMinutesToday = 45,
                status = "ENFORCED",
                blocked = false,
                whitelisted = false,
                scheduleOnly = false
            ),
            AppRule(
                id = "rule_${childId}_minecraft",
                ruleId = "rule_${childId}_minecraft",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                packageName = "com.mojang.minecraftpe",
                appName = "Minecraft",
                category = "Games & Entertainment",
                dailyLimitMinutes = 45,
                usedMinutesToday = 45,
                status = "ENFORCED",
                blocked = false,
                whitelisted = false,
                scheduleOnly = false
            ),
            AppRule(
                id = "rule_${childId}_ytkids",
                ruleId = "rule_${childId}_ytkids",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                packageName = "com.google.android.apps.youtube.kids",
                appName = "YouTube Kids",
                category = "Games & Entertainment",
                dailyLimitMinutes = 30,
                usedMinutesToday = 15,
                status = "SCHEDULE_ONLY",
                blocked = false,
                whitelisted = false,
                scheduleOnly = true
            ),
            AppRule(
                id = "rule_${childId}_discord",
                ruleId = "rule_${childId}_discord",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                packageName = "com.discord",
                appName = "Discord",
                category = "Social & Communication",
                dailyLimitMinutes = 30,
                usedMinutesToday = 18,
                status = "ENFORCED",
                blocked = false,
                whitelisted = false,
                scheduleOnly = false
            ),
            AppRule(
                id = "rule_${childId}_whatsapp",
                ruleId = "rule_${childId}_whatsapp",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                packageName = "com.whatsapp",
                appName = "WhatsApp Family",
                category = "Social & Communication",
                dailyLimitMinutes = 120,
                usedMinutesToday = 12,
                status = "WHITELISTED",
                blocked = false,
                whitelisted = true,
                scheduleOnly = false
            ),
            AppRule(
                id = "rule_${childId}_khan",
                ruleId = "rule_${childId}_khan",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                packageName = "org.khanacademy.android",
                appName = "Khan Academy",
                category = "School & Learning",
                dailyLimitMinutes = 240,
                usedMinutesToday = 28,
                status = "WHITELISTED",
                blocked = false,
                whitelisted = true,
                scheduleOnly = false
            ),
            AppRule(
                id = "rule_${childId}_chrome",
                ruleId = "rule_${childId}_chrome",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                packageName = "com.android.chrome",
                appName = "Google Chrome",
                category = "Web & Browsers",
                dailyLimitMinutes = 60,
                usedMinutesToday = 22,
                status = "ENFORCED",
                blocked = false,
                whitelisted = false,
                scheduleOnly = false
            )
        )
    }

    fun defaultSchedules(ownerUid: String, familyId: String = "fam_$ownerUid", childId: String = "child_leo"): List<ScheduleRoutine> {
        return listOf(
            ScheduleRoutine(
                id = "sched_bedtime",
                scheduleId = "sched_bedtime",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                title = "Bedtime & Night Sleep",
                windowDescription = "8:30 PM — 7:00 AM",
                daysSummary = "Sun, Mon, Tue, Wed, Thu",
                ruleSummary = "Phone calls, Emergency SOS & Alarm Clock only",
                iconType = "BEDTIME",
                enabled = true,
                syncStatus = "ENFORCED"
            ),
            ScheduleRoutine(
                id = "sched_school",
                scheduleId = "sched_school",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                title = "Oakridge School Hours",
                windowDescription = "8:00 AM — 3:15 PM",
                daysSummary = "Mon, Tue, Wed, Thu, Fri",
                ruleSummary = "Learning apps, Calculator, Google Classroom & Family Contacts allowed",
                iconType = "SCHOOL",
                enabled = true,
                syncStatus = "ENFORCED"
            ),
            ScheduleRoutine(
                id = "sched_dinner",
                scheduleId = "sched_dinner",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                title = "Dinner & Family Block",
                windowDescription = "6:00 PM — 7:00 PM",
                daysSummary = "Every Day",
                ruleSummary = "All entertainment & social apps paused at family table",
                iconType = "DINNER",
                enabled = true,
                syncStatus = "ENFORCED"
            )
        )
    }

    fun defaultGeofences(ownerUid: String, familyId: String = "fam_$ownerUid", childId: String = "child_leo"): List<SafeGeofence> {
        return listOf(
            SafeGeofence(
                id = "geo_school",
                geofenceId = "geo_school",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                name = "Oakridge Middle School",
                radiusMeters = 250,
                triggerSummary = "Notify on Arrival & Departure",
                scheduleSummary = "Mon–Fri • 7:45 AM – 3:30 PM",
                activeInside = true,
                alertsEnabled = true,
                latitude = 37.7749,
                longitude = -122.4194
            ),
            SafeGeofence(
                id = "geo_home",
                geofenceId = "geo_home",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                name = "Home Sanctuary",
                radiusMeters = 150,
                triggerSummary = "Notify on Unexpected Departure",
                scheduleSummary = "24/7 Active Guard",
                activeInside = false,
                alertsEnabled = true,
                latitude = 37.7812,
                longitude = -122.4110
            ),
            SafeGeofence(
                id = "geo_soccer",
                geofenceId = "geo_soccer",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = childId,
                name = "West Soccer Complex",
                radiusMeters = 300,
                triggerSummary = "Notify on Arrival & Departure",
                scheduleSummary = "Tue, Thu • 4:00 PM – 6:00 PM",
                activeInside = false,
                alertsEnabled = true,
                latitude = 37.7685,
                longitude = -122.4290
            )
        )
    }

    fun defaultAlerts(ownerUid: String, familyId: String = "fam_$ownerUid"): List<SafetyAlert> {
        return listOf(
            SafetyAlert(
                id = "alert_school_arrival",
                alertId = "alert_school_arrival",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                childName = "Leo",
                title = "Arrived safely at Oakridge Middle School",
                description = "Entered 250m safe boundary on time. School Focus Mode automatically engaged.",
                category = "GEOFENCE",
                severity = "INFO",
                timestampLabel = "Today • 7:54 AM",
                acknowledged = true
            ),
            SafetyAlert(
                id = "alert_permission_check",
                alertId = "alert_permission_check",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                childName = "Leo",
                title = "Knox & Accessibility Guard Verified",
                description = "Hardware attestation confirmed all policy shields are active and unmodified.",
                category = "DEVICE_HEALTH",
                severity = "INFO",
                timestampLabel = "Yesterday • 6:15 PM",
                acknowledged = true
            ),
            SafetyAlert(
                id = "alert_sos_drill",
                alertId = "alert_sos_drill",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                childName = "Leo",
                title = "Family SOS Practice Drill Completed",
                description = "Bilateral emergency drill tested GPS pin lock, 5s ambient audio beacon, and Tier-1 Guardian dispatch.",
                category = "SOS",
                severity = "WARNING",
                timestampLabel = "Earlier This Week • Sun 4:20 PM",
                acknowledged = true
            )
        )
    }

    fun defaultRequests(ownerUid: String, familyId: String = "fam_$ownerUid"): List<ChildRequest> {
        return listOf(
            ChildRequest(
                id = "req_minecraft_15m",
                requestId = "req_minecraft_15m",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                childName = "Leo",
                requestType = "EXTRA_TIME",
                targetName = "Minecraft",
                targetCategory = "Games & Entertainment",
                requestedBonusMinutes = 15,
                childNote = "Finished my math worksheet early! Can I have 15 more mins to finish building our bridge with Sam?",
                usedTodaySummary = "45m / 45m daily cap reached",
                status = "PENDING",
                decisionSummary = "",
                decidedBy = ""
            ),
            ChildRequest(
                id = "req_duolingo_install",
                requestId = "req_duolingo_install",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                childName = "Leo",
                requestType = "NEW_APP",
                targetName = "Duolingo: Language Lessons",
                targetCategory = "Education • Everyone (ESRB)",
                requestedBonusMinutes = 0,
                childNote = "Mme. Laurent recommended Duolingo for our French vocabulary practice.",
                usedTodaySummary = "New App Store Install Petition",
                status = "PENDING",
                decisionSummary = "",
                decidedBy = ""
            ),
            ChildRequest(
                id = "req_roblox_history",
                requestId = "req_roblox_history",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                childName = "Leo",
                requestType = "EXTRA_TIME",
                targetName = "Roblox",
                targetCategory = "Games & Entertainment",
                requestedBonusMinutes = 30,
                childNote = "Weekend tournament round",
                usedTodaySummary = "45m / 45m daily cap",
                status = "APPROVED",
                decisionSummary = "Approved +30m bonus",
                decidedBy = "Sarah Jenkins"
            )
        )
    }

    fun defaultMessages(ownerUid: String, familyId: String = "fam_$ownerUid"): List<FamilyMessage> {
        return listOf(
            FamilyMessage(
                id = "msg_1",
                messageId = "msg_1",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                senderName = "Leo",
                senderRole = "CHILD",
                text = "Hi Mom! Just got to school. Walked Daisy before leaving and refilled her water bowl.",
                isSystemEvent = false,
                timestampLabel = "7:56 AM"
            ),
            FamilyMessage(
                id = "msg_2",
                messageId = "msg_2",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                senderName = "Anchor Fair-Play Audit",
                senderRole = "SYSTEM",
                text = "Leo marked 'Math & French Homework' as Ready for Review (+15m Bonus Screen Time).",
                isSystemEvent = true,
                timestampLabel = "3:35 PM"
            ),
            FamilyMessage(
                id = "msg_3",
                messageId = "msg_3",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                senderName = "Sarah Jenkins",
                senderRole = "PARENT",
                text = "Proud of you Leo! I'll review your worksheet and unlock the +15m Minecraft bonus.",
                isSystemEvent = false,
                timestampLabel = "3:38 PM"
            )
        )
    }

    fun defaultTasks(ownerUid: String, familyId: String = "fam_$ownerUid"): List<FamilyTask> {
        return listOf(
            FamilyTask(
                id = "task_daisy",
                taskId = "task_daisy",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                title = "Walk Daisy & refill water bowl",
                rewardSummary = "+15m Weekend Bonus",
                dueSummary = "Completed Today • 7:30 AM",
                status = "COMPLETED",
                completedByChild = true,
                approvedByParent = true
            ),
            FamilyTask(
                id = "task_homework",
                taskId = "task_homework",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                title = "Math & French Homework",
                rewardSummary = "+15m Bonus Screen Time",
                dueSummary = "Submitted by Leo • Needs Guardian Sign-Off",
                status = "READY_FOR_REVIEW",
                completedByChild = true,
                approvedByParent = false
            ),
            FamilyTask(
                id = "task_dock",
                taskId = "task_dock",
                familyId = familyId,
                ownerUid = ownerUid,
                childId = "child_leo",
                title = "Dock phone in kitchen before dinner (6:00 PM)",
                rewardSummary = "+5 Family Trust Points",
                dueSummary = "Daily Habit Pact • 6:00 PM",
                status = "IN_PROGRESS",
                completedByChild = false,
                approvedByParent = false
            )
        )
    }

    fun defaultGuardians(ownerUid: String, familyId: String = "fam_$ownerUid"): List<GuardianContact> {
        return listOf(
            GuardianContact(
                id = "guard_sarah",
                guardianId = "guard_sarah",
                familyId = familyId,
                ownerUid = ownerUid,
                name = "Sarah Jenkins (You)",
                relationRole = "Mother • Primary Household Owner",
                emailOrPhone = "sarah.jenkins@family.org",
                isPrimaryGuardian = true,
                permissionsSummary = "Full Policy, Billing, Data Rights & SOS Authority",
                receiveGeofenceAlerts = true,
                receiveSosSms = true,
                status = "Owner • Active"
            ),
            GuardianContact(
                id = "guard_mark",
                guardianId = "guard_mark",
                familyId = familyId,
                ownerUid = ownerUid,
                name = "Mark Jenkins",
                relationRole = "Father • Co-Parent Guardian",
                emailOrPhone = "mark.jenkins@family.org",
                isPrimaryGuardian = true,
                permissionsSummary = "Approve Requests, Edit Schedules, SOS Dispatch",
                receiveGeofenceAlerts = true,
                receiveSosSms = true,
                status = "Verified Co-Guardian"
            ),
            GuardianContact(
                id = "guard_eleanor",
                guardianId = "guard_eleanor",
                familyId = familyId,
                ownerUid = ownerUid,
                name = "Grandma Eleanor",
                relationRole = "Grandmother • Trusted Emergency Contact",
                emailOrPhone = "+1 (415) 890-4421",
                isPrimaryGuardian = false,
                permissionsSummary = "Emergency SOS SMS & School Pickup Check-Ins only",
                receiveGeofenceAlerts = true,
                receiveSosSms = true,
                status = "SMS Verified"
            ),
            GuardianContact(
                id = "guard_david",
                guardianId = "guard_david",
                familyId = familyId,
                ownerUid = ownerUid,
                name = "Uncle David",
                relationRole = "Uncle • Backup Emergency Contact",
                emailOrPhone = "+1 (415) 612-8839",
                isPrimaryGuardian = false,
                permissionsSummary = "Tier-3 Emergency SOS Escalation Fallback",
                receiveGeofenceAlerts = false,
                receiveSosSms = true,
                status = "SMS Verified"
            )
        )
    }

    fun defaultPremiumConfig(ownerUid: String, familyId: String = "fam_$ownerUid", childId: String = "child_leo"): PremiumFilterConfig {
        return PremiumFilterConfig(
            id = "prem_$childId",
            configId = "prem_$childId",
            familyId = familyId,
            ownerUid = ownerUid,
            childId = childId
        )
    }
}
