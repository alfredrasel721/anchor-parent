package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AnchorScreen
import com.example.ui.components.BilateralTransparencyCard
import com.example.ui.components.ChildSelectorBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AnchorPrimary
import com.example.ui.theme.AnchorSecondary
import com.example.ui.theme.AnchorTertiary
import com.example.ui.theme.StatusEnforced
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.viewmodel.AnchorUiState

// ============================================================================
// P-04: Family Dashboard (p_04_family_dashboard.html)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun P04FamilyDashboardScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onNavigate: (AnchorScreen) -> Unit,
    onTogglePauseDevice: (String) -> Unit,
    onGrantBonusMinutes: (Int) -> Unit,
    onResolveRequest: (String, Boolean, Int, String) -> Unit
) {
    val child = state.selectedChild
    val progress = (child.usedMinutesToday.toFloat() / child.dailyAllowanceMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    val remainingMins = (child.dailyAllowanceMinutes - child.usedMinutesToday).coerceAtLeast(0)
    val isPaused = child.currentMode.startsWith("Paused")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Child Selector Strip
        ChildSelectorBar(
            children = state.children,
            selectedChildId = state.selectedChildId,
            onSelectChild = onSelectChild,
            onAddChildClick = { onNavigate(AnchorScreen.P03_ADD_CHILD_PAIR_CODE) }
        )

        // Device Sync & Telemetry Strip Card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(AnchorScreen.P05_CHILD_PROFILE_DEVICE) }
                .testTag("dashboard_device_status_strip")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = AnchorPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${child.deviceName} • ${child.locationLabel}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${child.lastSyncLabel} • Battery ${child.batteryLevel}% • ${child.networkType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(child.syncStatus)
            }
        }

        // Today's Screen Allowance Radial Gauge Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TODAY'S DIGITAL RHYTHM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${child.name}'s Screen Allowance",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Surface(
                        color = if (isPaused) Color(0xFFFEF3F2) else Color(0xFFE6F4F1),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = child.currentMode,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPaused) StatusFailed else StatusEnforced,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Radial Ring Gauge
                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 14.dp.toPx()
                            drawArc(
                                color = Color(0xFFE0E9F9),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (progress > 0.9f) StatusPending else AnchorPrimary,
                                startAngle = 135f,
                                sweepAngle = 270f * progress,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${child.usedMinutesToday / 60}h ${child.usedMinutesToday % 60}m",
                                style = MaterialTheme.typography.headlineMedium,
                                color = AnchorPrimary
                            )
                            Text(
                                text = "of ${child.dailyAllowanceMinutes / 60}h ${child.dailyAllowanceMinutes % 60}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = "${remainingMins}m remaining today",
                            style = MaterialTheme.typography.titleMedium,
                            color = AnchorSecondary
                        )
                        Text(
                            text = "• Learning & School: 42m\n• Games & Fun: 45m\n• Social & Chat: 18m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "View Full Analytics (P-06) →",
                            style = MaterialTheme.typography.labelLarge,
                            color = AnchorPrimary,
                            modifier = Modifier
                                .clickable { onNavigate(AnchorScreen.P06_SCREEN_TIME_ANALYTICS) }
                                .testTag("open_analytics_from_dashboard")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions Row: Pause Device, +30m Bonus, Downtime
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onTogglePauseDevice("1 Hour") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) AnchorSecondary else AnchorPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_pause_device_btn")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaused) "Resume" else "Pause")
                    }
                    OutlinedButton(
                        onClick = { onGrantBonusMinutes(30) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_bonus_30m_btn")
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+30m")
                    }
                    OutlinedButton(
                        onClick = { onNavigate(AnchorScreen.P08_SCHEDULES_DOWNTIME) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_downtime_btn")
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedules")
                    }
                }
            }
        }

        // Priority Pending Requests Card (P-11)
        val pendingRequests = state.requests.filter { it.status == "PENDING" }
        if (pendingRequests.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pending Child Petitions (${pendingRequests.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Inbox (P-11) →",
                            style = MaterialTheme.typography.labelLarge,
                            color = AnchorPrimary,
                            modifier = Modifier.clickable { onNavigate(AnchorScreen.P11_EXTRA_TIME_REQUESTS) }
                        )
                    }

                    pendingRequests.forEach { req ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${req.childName} • ${req.targetName}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    StatusBadge(req.requestType)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${req.childNote}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onResolveRequest(
                                                req.requestId,
                                                true,
                                                if (req.requestedBonusMinutes > 0) req.requestedBonusMinutes else 15,
                                                "Approved by Guardian from Dashboard"
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("approve_req_${req.requestId}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = AnchorSecondary)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (req.requestedBonusMinutes > 0) "Approve +${req.requestedBonusMinutes}m" else "Approve App")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onResolveRequest(
                                                req.requestId,
                                                false,
                                                0,
                                                "Let's stick to today's schedule first"
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("decline_req_${req.requestId}")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Decline")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Module Grid for all Free Core & Premium Safeguard Hubs
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Guardian Command Center (All 20 Modules)",
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickLinks = listOf(
                        AnchorScreen.P05_CHILD_PROFILE_DEVICE to "P-05 Device Health",
                        AnchorScreen.P06_SCREEN_TIME_ANALYTICS to "P-06 Analytics",
                        AnchorScreen.P07_APP_USAGE_LIMITS to "P-07 App Limits",
                        AnchorScreen.P08_SCHEDULES_DOWNTIME to "P-08 Schedules",
                        AnchorScreen.P09_LOCATION_GEOFENCES to "P-09 Geofences",
                        AnchorScreen.P10_ALERTS_SOS_ACTIVITY to "P-10 SOS & Alerts",
                        AnchorScreen.P11_EXTRA_TIME_REQUESTS to "P-11 Requests",
                        AnchorScreen.P12_FAMILY_CHAT_TASKS to "P-12 Chat & Tasks",
                        AnchorScreen.P13_GUARDIAN_ROLES_CONTACTS to "P-13 Guardians",
                        AnchorScreen.P14_PRIVACY_EXPORT_DELETION to "P-14 Data Rights",
                        AnchorScreen.P15_PREMIUM_FEATURES to "P-15 Premium Tier",
                        AnchorScreen.P16_WEB_FILTER_RULES to "P-16 Web Filter ★",
                        AnchorScreen.P17_SOCIAL_CONTENT_FILTERS to "P-17 Social Shield ★",
                        AnchorScreen.P18_NOTIFICATION_FILTER to "P-18 Notif Filter ★",
                        AnchorScreen.P19_LIVE_SESSION_VIEWER to "P-19 Live Check ★",
                        AnchorScreen.P20_ACCOUNT_SETTINGS_DIAGNOSTICS to "P-20 Diagnostics"
                    )
                    quickLinks.forEach { (screen, label) ->
                        FilterChip(
                            selected = false,
                            onClick = { onNavigate(screen) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("quick_link_${screen.code}")
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// P-05: Child Profile & Device Status (p_05_child_profile_device_status.html)
// ============================================================================
@Composable
fun P05ChildProfileDeviceStatusScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    onTogglePauseDevice: (String) -> Unit,
    onPairReplacementDevice: () -> Unit
) {
    val child = state.selectedChild
    var selectedPauseDuration by remember { mutableStateOf("1 Hour") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ChildSelectorBar(
            children = state.children,
            selectedChildId = state.selectedChildId,
            onSelectChild = onSelectChild,
            onAddChildClick = onPairReplacementDevice
        )

        // Child Identity & Sync Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(AnchorPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = child.avatarInitials,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${child.name} (Age ${child.age})",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${child.gradeLabel} • ${child.deviceModel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    StatusBadge(child.syncStatus)
                }

                LinearProgressIndicator(
                    progress = {
                        (child.usedMinutesToday.toFloat() / child.dailyAllowanceMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
                Text(
                    text = "Used ${child.usedMinutesToday}m of ${child.dailyAllowanceMinutes}m allowance • ${child.lastSyncLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Guardian Quick Actions: Ping Device, Force Sync, Restart App Guard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onQuickAction("Audible Device Ping") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("p05_ping_device_btn")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping")
                    }
                    OutlinedButton(
                        onClick = { onQuickAction("Force Cloud Policy Sync") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("p05_force_sync_btn")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Force Sync")
                    }
                    OutlinedButton(
                        onClick = { onQuickAction("Service Heartbeat Restart") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("p05_restart_guard_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Heartbeat")
                    }
                }
            }
        }

        // Hardware Telemetry 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = AnchorSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Battery Telemetry", style = MaterialTheme.typography.labelSmall)
                    Text("${child.batteryLevel}%", style = MaterialTheme.typography.headlineSmall)
                    Text(child.chargingStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = AnchorPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Network Link", style = MaterialTheme.typography.labelSmall)
                    Text(child.networkType, style = MaterialTheme.typography.headlineSmall)
                    Text(child.networkName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = AnchorPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Internal Storage", style = MaterialTheme.typography.labelSmall)
                    Text("${child.storageFreeGb} GB Free", style = MaterialTheme.typography.headlineSmall)
                    Text("128 GB Total UFS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = AnchorSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Safe Zone Anchor", style = MaterialTheme.typography.labelSmall)
                    Text(child.locationLabel, style = MaterialTheme.typography.titleMedium)
                    Text(child.osVersion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Guardian Shield & OS Integrity Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AnchorPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardian Shield & OS Integrity", style = MaterialTheme.typography.titleMedium)
                }
                IntegrityRow("Samsung Knox Hardware Attestation", if (child.knoxVerified) "VERIFIED" else "UNSUPPORTED")
                IntegrityRow("Accessibility App Guard Service", if (child.accessibilityEnabled) "ENFORCED" else "PENDING")
                IntegrityRow("Device Admin Uninstall Protection", if (child.deviceAdminEnabled) "ENFORCED" else "PENDING")
                IntegrityRow("UsageStatsManager Telemetry", "ENFORCED")
            }
        }

        // Pause Policy Enforcements Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Temporary Policy Override / Device Replacement", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1 Hour", "Rest of Day", "Until Resumed").forEach { dur ->
                        FilterChip(
                            selected = selectedPauseDuration == dur,
                            onClick = { selectedPauseDuration = dur },
                            label = { Text(dur) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onTogglePauseDevice(selectedPauseDuration) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("p05_pause_policy_btn")
                    ) {
                        Text(if (child.currentMode.startsWith("Paused")) "Resume Enforcements" else "Pause Enforcements ($selectedPauseDuration)")
                    }
                    OutlinedButton(
                        onClick = onPairReplacementDevice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pair Replacement (P-03)")
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrityRow(label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        StatusBadge(status)
    }
}

// ============================================================================
// P-06: Screen-Time Analytics (p_06_screen_time_analytics.html)
// ============================================================================
@Composable
fun P06ScreenTimeAnalyticsScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onSelectRange: (String) -> Unit,
    onExportReport: () -> Unit,
    onNavigateToAppLimits: () -> Unit
) {
    val child = state.selectedChild

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ChildSelectorBar(
            children = state.children,
            selectedChildId = state.selectedChildId,
            onSelectChild = onSelectChild,
            onAddChildClick = {}
        )

        // Range Selector: Today, 7 Days, 30 Days, Custom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Today", "7 Days", "30 Days", "Custom").forEach { range ->
                FilterChip(
                    selected = state.analyticsRange == range,
                    onClick = { onSelectRange(range) },
                    label = { Text(range) },
                    modifier = Modifier.testTag("analytics_range_$range")
                )
            }
        }

        // Weekly Balance Scorecard
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DIGITAL WELLBEING SCORECARD (${state.analyticsRange})", style = MaterialTheme.typography.labelSmall)
                        Text("${child.name}'s Screen vs Offline Balance", style = MaterialTheme.typography.titleLarge)
                    }
                    StatusBadge("92% ON TARGET")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Avg Daily Screen Time", style = MaterialTheme.typography.labelSmall)
                            Text("1h 54m", style = MaterialTheme.typography.headlineSmall, color = AnchorPrimary)
                            Text("18% below 2h 30m cap", style = MaterialTheme.typography.bodySmall, color = StatusEnforced)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Sleep Wind-Down", style = MaterialTheme.typography.labelSmall)
                            Text("7 / 7 Nights", style = MaterialTheme.typography.headlineSmall, color = AnchorSecondary)
                            Text("Docked before 8:30 PM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Stacked Bar Chart Card (Mon–Sun)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Daily Screen Rhythm (Learning vs Fun vs Social)", style = MaterialTheme.typography.titleMedium)

                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val learningFracs = listOf(0.32f, 0.40f, 0.35f, 0.45f, 0.28f, 0.20f, 0.25f)
                val gamingFracs = listOf(0.30f, 0.25f, 0.28f, 0.25f, 0.42f, 0.48f, 0.35f)
                val socialFracs = listOf(0.15f, 0.12f, 0.14f, 0.10f, 0.18f, 0.22f, 0.16f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(top = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = days.size
                        val slotW = size.width / barCount
                        val barW = slotW * 0.48f
                        val maxH = size.height - 24.dp.toPx()

                        // Dashed allowance threshold line
                        drawLine(
                            color = StatusPending,
                            start = Offset(0f, maxH * 0.15f),
                            end = Offset(size.width, maxH * 0.15f),
                            strokeWidth = 2.dp.toPx()
                        )

                        for (i in 0 until barCount) {
                            val x = i * slotW + (slotW - barW) / 2f
                            val hLearn = maxH * learningFracs[i]
                            val hGame = maxH * gamingFracs[i]
                            val hSoc = maxH * socialFracs[i]

                            var currentBottom = maxH
                            // Learning segment (Secondary Teal)
                            drawRect(
                                color = AnchorSecondary,
                                topLeft = Offset(x, currentBottom - hLearn),
                                size = Size(barW, hLearn)
                            )
                            currentBottom -= hLearn
                            // Gaming segment (Primary Navy)
                            drawRect(
                                color = AnchorPrimary,
                                topLeft = Offset(x, currentBottom - hGame),
                                size = Size(barW, hGame)
                            )
                            currentBottom -= hGame
                            // Social segment (Amber)
                            drawRect(
                                color = AnchorTertiary,
                                topLeft = Offset(x, currentBottom - hSoc),
                                size = Size(barW, hSoc)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    days.forEach { d ->
                        Text(text = d, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendDot(AnchorSecondary, "Learning (42m)")
                    LegendDot(AnchorPrimary, "Games (45m)")
                    LegendDot(AnchorTertiary, "Social (18m)")
                }
            }
        }

        // Top Apps Breakdown Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top Used Apps & Category Caps", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Edit Caps (P-07) →",
                        style = MaterialTheme.typography.labelLarge,
                        color = AnchorPrimary,
                        modifier = Modifier.clickable { onNavigateToAppLimits() }
                    )
                }

                state.appRules.forEach { rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.appName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${rule.category} • ${rule.usedMinutesToday}m used of ${rule.dailyLimitMinutes}m cap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(rule.status)
                    }
                }

                Button(
                    onClick = onExportReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_analytics_report_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Screen-Time Summary Report")
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }
}
