package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AnchorScreen
import com.example.ui.components.BilateralTransparencyCard
import com.example.ui.components.ChildSelectorBar
import com.example.ui.components.PremiumGateBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AnchorError
import com.example.ui.theme.AnchorPrimary
import com.example.ui.theme.AnchorSecondary
import com.example.ui.theme.StatusEnforced
import com.example.ui.theme.StatusEnforcedBg
import com.example.viewmodel.AnchorUiState

// ============================================================================
// P-18: Notification & Distraction Filter [PREMIUM] (p_18_notification_filter.html)
// ============================================================================
@Composable
fun P18NotificationFilterScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onUpdateConfig: (Boolean, String, Boolean, Boolean) -> Unit,
    onNavigateToPremium: () -> Unit,
    onInstantUnlockTrial: () -> Unit
) {
    val config = state.premiumConfig

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PremiumGateBanner(
            featureTitle = "Notification & Distraction Filtering",
            isPremiumActive = state.isPremiumActive,
            onUpgradeClick = onNavigateToPremium,
            onInstantUnlockTrial = onInstantUnlockTrial
        )

        ChildSelectorBar(
            children = state.children,
            selectedChildId = state.selectedChildId,
            onSelectChild = onSelectChild,
            onAddChildClick = {}
        )

        // Master Switch & Interception Protocol Card
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Study & Bedtime Notification Shield", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Intercept non-essential pings during Oakridge School Hours & Night Sleep",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.notificationFilterEnabled,
                        enabled = state.isPremiumActive,
                        onCheckedChange = {
                            onUpdateConfig(
                                it,
                                config.notificationProtocol,
                                config.batchMuteSocialDuringStudy,
                                config.blockGamingStreakPrompts
                            )
                        },
                        modifier = Modifier.testTag("toggle_notif_filter_master")
                    )
                }

                Text("Interception Protocol", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BATCHING", "TOTAL_SILENCE", "EMERGENCY_ONLY").forEach { proto ->
                        FilterChip(
                            selected = config.notificationProtocol == proto,
                            enabled = state.isPremiumActive,
                            onClick = {
                                onUpdateConfig(
                                    config.notificationFilterEnabled,
                                    proto,
                                    config.batchMuteSocialDuringStudy,
                                    config.blockGamingStreakPrompts
                                )
                            },
                            label = { Text(proto.replace("_", " ")) },
                            modifier = Modifier.testTag("notif_proto_$proto")
                        )
                    }
                }
            }
        }

        // Category Rules Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Category Interception Policies", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Batch & Mute Social Group Pings", style = MaterialTheme.typography.titleSmall)
                        Text("Delivers held messages in a calm 3:30 PM digest after school", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = config.batchMuteSocialDuringStudy,
                        enabled = state.isPremiumActive,
                        onCheckedChange = {
                            onUpdateConfig(
                                config.notificationFilterEnabled,
                                config.notificationProtocol,
                                it,
                                config.blockGamingStreakPrompts
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Suppress Manipulative Game 'Streak' Alerts", style = MaterialTheme.typography.titleSmall)
                        Text("Blocks FOMO & loot-box timer notifications during homework", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = config.blockGamingStreakPrompts,
                        enabled = state.isPremiumActive,
                        onCheckedChange = {
                            onUpdateConfig(
                                config.notificationFilterEnabled,
                                config.notificationProtocol,
                                config.batchMuteSocialDuringStudy,
                                it
                            )
                        }
                    )
                }
            }
        }

        BilateralTransparencyCard(
            title = "Zero-Break Safety Failsafe",
            description = "Calls and messages from Primary Guardians, Trusted Emergency Contacts, school nurses, and calendar alarms always bypass notification filtering."
        )
    }
}

// ============================================================================
// P-19: Active Live Session Viewer [PREMIUM] (p_19_live_session_viewer.html)
// Covers Premium Groups 1 (Screen Mirroring), 2 (Live Camera), 3 (Live Audio)
// ============================================================================
@Composable
fun P19LiveSessionViewerScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onSelectMode: (String) -> Unit,
    onToggleTwoWayTalk: () -> Unit,
    onToggleSessionActive: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onInstantUnlockTrial: () -> Unit
) {
    val child = state.selectedChild

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PremiumGateBanner(
            featureTitle = "Audited Live Screen, Camera & Microphone Check-In",
            isPremiumActive = state.isPremiumActive,
            onUpgradeClick = onNavigateToPremium,
            onInstantUnlockTrial = onInstantUnlockTrial
        )

        ChildSelectorBar(
            children = state.children,
            selectedChildId = state.selectedChildId,
            onSelectChild = onSelectChild,
            onAddChildClick = {}
        )

        // Audited Session Compliance Banner
        Surface(
            color = StatusEnforcedBg,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusEnforced)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AUDITED SESSION ${state.premiumConfig.liveSessionAuditCode} • EXPLICIT CONSENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusEnforced
                        )
                        Text(
                            text = "${child.name} approved this session on ${child.deviceName}. Persistent Android foreground indicator is visible to child.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                StatusBadge(if (state.liveSessionActive) "LIVE 07:42" else "ENDED")
            }
        }

        // Mode Switcher: Screen View (Group 1) | Front Camera (Group 2) | Ambient Audio (Group 3)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                Triple("Screen View", Icons.Default.ScreenShare, "live_mode_screen"),
                Triple("Front Camera", Icons.Default.CameraFront, "live_mode_camera"),
                Triple("Ambient Audio", Icons.Default.GraphicEq, "live_mode_audio")
            )
            modes.forEach { (mode, icon, tag) ->
                val selected = state.liveSessionMode == mode
                Button(
                    onClick = { onSelectMode(mode) },
                    enabled = state.isPremiumActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) AnchorPrimary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tag)
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(mode.substringBefore(" "), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Live Session Viewport Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D141F)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.liveSessionMode} • ${child.deviceName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    StatusBadge(if (state.liveSessionActive) "CONSENT ACTIVE" else "STANDBY")
                }

                // Viewport Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF162235)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val bars = 24
                        val barW = size.width / (bars * 1.8f)
                        for (i in 0 until bars) {
                            val ratio = if (state.liveSessionActive) {
                                0.2f + 0.6f * kotlin.math.abs(kotlin.math.sin((i + 1) * 0.55f))
                            } else {
                                0.08f
                            }
                            val h = size.height * ratio * 0.5f
                            drawRect(
                                color = Color(0xFF94F1D6).copy(alpha = 0.45f),
                                topLeft = Offset(i * (barW * 1.8f) + 16f, (size.height - h) / 2f),
                                size = Size(barW, h)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (state.liveSessionActive) {
                                "Active Mode: ${state.liveSessionMode}"
                            } else {
                                "Live Session Ended"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Stated Reason: \"${state.liveSessionReason}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94F1D6)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hardware Note: Real-time WebRTC MediaProjection / CameraX / AudioRecord streaming requires a paired physical Child Companion device and signaling relay.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleTwoWayTalk,
                        enabled = state.isPremiumActive && state.liveSessionActive,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_two_way_talk_btn")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.liveSessionTwoWayTalk) "Mute Two-Way Talk" else "Two-Way Talk",
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = onToggleSessionActive,
                        enabled = state.isPremiumActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.liveSessionActive) AnchorError else AnchorSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_live_session_btn")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (state.liveSessionActive) "End Session Now" else "Start Audited Check")
                    }
                }
            }
        }
    }
}

// ============================================================================
// P-20: Account Settings & Diagnostics (p_20_account_settings_diagnostics.html)
// ============================================================================
@Composable
fun P20AccountSettingsDiagnosticsScreen(
    state: AnchorUiState,
    onRunSelfTest: () -> Unit,
    onTogglePreference: (String) -> Unit,
    onExportSystemLog: () -> Unit,
    onNavigate: (AnchorScreen) -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Guardian Account & License Overview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(state.guardianDisplayName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${state.guardianEmail} • ${state.familyHousehold.familyName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(state.familyHousehold.subscriptionTier)
                }
                Text(
                    text = "Licensed Child Devices: ${state.children.size} / 5 Household Slots Active",
                    style = MaterialTheme.typography.labelLarge,
                    color = AnchorPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onNavigate(AnchorScreen.P15_PREMIUM_FEATURES) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Subscription (P-15)")
                    }
                    OutlinedButton(
                        onClick = { onNavigate(AnchorScreen.P14_PRIVACY_EXPORT_DELETION) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Privacy & Data (P-14)")
                    }
                }
            }
        }

        // Real-Time Pipeline Health & Self-Test Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AnchorPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Real-Time Pipeline Health & Diagnostics", style = MaterialTheme.typography.titleMedium)
                }

                PipelineRow("Firebase Firestore Enterprise DB", if (state.isCloudConnected) "ENFORCED" else "PENDING")
                PipelineRow("Room SQLite Offline Policy Cache", "ENFORCED")
                PipelineRow("Emergency SOS High-Priority Channel", "ENFORCED")
                PipelineRow("Samsung Knox & Accessibility Attestation", "VERIFIED")

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.lastSelfTestSummary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = AnchorSecondary
                    )
                }

                Button(
                    onClick = onRunSelfTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_connection_self_test_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Full Connection Self-Test")
                }
            }
        }

        // Parent App Controls & Preferences
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Parent App Controls & Preferences", style = MaterialTheme.typography.titleMedium)

                PrefSwitchRow(
                    title = "Require Biometrics / Screen Lock on Launch",
                    checked = state.biometricLockRequired,
                    onToggle = { onTogglePreference("BIOMETRIC") }
                )
                PrefSwitchRow(
                    title = "Critical SOS Alerts Override Android DND",
                    checked = state.criticalAlertsOverrideDnd,
                    onToggle = { onTogglePreference("DND_OVERRIDE") }
                )
                PrefSwitchRow(
                    title = "Dark Theme Display Mode",
                    checked = state.darkModeOverride,
                    onToggle = { onTogglePreference("DARK_MODE") }
                )
                PrefSwitchRow(
                    title = "Offline Room SQLite Diagnostics Cache",
                    checked = state.offlineDiagnosticsCacheEnabled,
                    onToggle = { onTogglePreference("OFFLINE_CACHE") }
                )
            }
        }

        // Audit Trail & Cryptographic Log Export
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Recent Cryptographic Audit Log (${state.auditLogs.size})", style = MaterialTheme.typography.titleMedium)
                state.auditLogs.take(4).forEach { log ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("${log.eventCode} • ${log.actionTitle}", style = MaterialTheme.typography.labelLarge)
                            Text(log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onExportSystemLog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_system_log_bundle_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Cryptographic System Log Bundle")
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_guardian_btn")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = AnchorError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Guardian Session", color = AnchorError)
                }
            }
        }
    }
}

@Composable
private fun PipelineRow(name: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        StatusBadge(status)
    }
}

@Composable
private fun PrefSwitchRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
