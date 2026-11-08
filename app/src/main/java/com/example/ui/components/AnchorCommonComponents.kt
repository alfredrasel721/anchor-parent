package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AnchorScreen
import com.example.model.ChildProfile
import com.example.ui.theme.AnchorPrimary
import com.example.ui.theme.AnchorPrimaryContainer
import com.example.ui.theme.AnchorSecondary
import com.example.ui.theme.AnchorTertiary
import com.example.ui.theme.AnchorTertiaryFixed
import com.example.ui.theme.StatusEnforced
import com.example.ui.theme.StatusEnforcedBg
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusFailedBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusUnsupported
import com.example.ui.theme.StatusUnsupportedBg

@Composable
fun StitchTopAppBar(
    currentScreen: AnchorScreen,
    familyName: String,
    isCloudConnected: Boolean,
    isPremiumActive: Boolean,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenPremium: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (canNavigateBack && currentScreen != AnchorScreen.P04_FAMILY_DASHBOARD) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("top_bar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AnchorPrimary, AnchorPrimaryContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Anchor Brand Shield",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = currentScreen.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = familyName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = currentScreen.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isPremiumActive) AnchorTertiaryFixed else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onOpenPremium() }
                            .testTag("top_bar_tier_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Subscription Tier",
                                tint = if (isPremiumActive) AnchorTertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPremiumActive) "PREMIUM" else "FREE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPremiumActive) AnchorTertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenDirectory,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .testTag("open_screen_directory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "All 20 Parent Screens Directory",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Connection & Sync strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (isCloudConnected) StatusEnforced else AnchorPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCloudConnected) {
                            "Firebase Firestore Enterprise + Room Cache Synced"
                        } else {
                            "Room SQLite Local Persistence Active • Bilateral Audit On"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Tap Grid for P-01..P-20",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenDirectory() }
                )
            }
        }
    }
}

@Composable
fun StitchBottomNavBar(
    currentScreen: AnchorScreen,
    pendingRequestsCount: Int,
    onNavigate: (AnchorScreen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple(AnchorScreen.P04_FAMILY_DASHBOARD, "Home", Icons.Default.Home),
            Triple(AnchorScreen.P07_APP_USAGE_LIMITS, "Controls", Icons.Default.Tune),
            Triple(AnchorScreen.P09_LOCATION_GEOFENCES, "Location", Icons.Default.LocationOn),
            Triple(AnchorScreen.P12_FAMILY_CHAT_TASKS, "Family", Icons.Default.Forum),
            Triple(AnchorScreen.P20_ACCOUNT_SETTINGS_DIAGNOSTICS, "Settings", Icons.Default.Settings)
        )

        items.forEach { (screen, label, icon) ->
            val selected = when (screen) {
                AnchorScreen.P04_FAMILY_DASHBOARD -> currentScreen in listOf(
                    AnchorScreen.P04_FAMILY_DASHBOARD,
                    AnchorScreen.P05_CHILD_PROFILE_DEVICE,
                    AnchorScreen.P06_SCREEN_TIME_ANALYTICS
                )
                AnchorScreen.P07_APP_USAGE_LIMITS -> currentScreen in listOf(
                    AnchorScreen.P07_APP_USAGE_LIMITS,
                    AnchorScreen.P08_SCHEDULES_DOWNTIME,
                    AnchorScreen.P16_WEB_FILTER_RULES,
                    AnchorScreen.P17_SOCIAL_CONTENT_FILTERS,
                    AnchorScreen.P18_NOTIFICATION_FILTER
                )
                AnchorScreen.P09_LOCATION_GEOFENCES -> currentScreen in listOf(
                    AnchorScreen.P09_LOCATION_GEOFENCES,
                    AnchorScreen.P10_ALERTS_SOS_ACTIVITY,
                    AnchorScreen.P19_LIVE_SESSION_VIEWER
                )
                AnchorScreen.P12_FAMILY_CHAT_TASKS -> currentScreen in listOf(
                    AnchorScreen.P11_EXTRA_TIME_REQUESTS,
                    AnchorScreen.P12_FAMILY_CHAT_TASKS,
                    AnchorScreen.P13_GUARDIAN_ROLES_CONTACTS
                )
                AnchorScreen.P20_ACCOUNT_SETTINGS_DIAGNOSTICS -> currentScreen in listOf(
                    AnchorScreen.P14_PRIVACY_EXPORT_DELETION,
                    AnchorScreen.P15_PREMIUM_FEATURES,
                    AnchorScreen.P20_ACCOUNT_SETTINGS_DIAGNOSTICS
                )
                else -> currentScreen == screen
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen) },
                modifier = Modifier.testTag("bottom_nav_${label.lowercase()}"),
                icon = {
                    if (label == "Family" && pendingRequestsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(pendingRequestsCount.toString()) }
                            }
                        ) {
                            Icon(imageVector = icon, contentDescription = label)
                        }
                    } else {
                        Icon(imageVector = icon, contentDescription = label)
                    }
                },
                label = { Text(text = label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
fun ChildSelectorBar(
    children: List<ChildProfile>,
    selectedChildId: String,
    onSelectChild: (String) -> Unit,
    onAddChildClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        children.forEach { child ->
            val isSelected = child.childId == selectedChildId
            Surface(
                color = if (isSelected) AnchorPrimary else MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onSelectChild(child.childId) }
                    .testTag("select_child_${child.childId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = child.avatarInitials,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else AnchorPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${child.name} (${child.age})",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${child.deviceName} • ${child.batteryLevel}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .clickable { onAddChildClick() }
                .testTag("add_child_pill_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Child",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add / Pair Child",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status.uppercase()) {
        "ENFORCED", "ACTIVE", "APPROVED", "COMPLETED", "VERIFIED" ->
            Triple(StatusEnforcedBg, StatusEnforced, status.uppercase())
        "PENDING", "SCHEDULE_ONLY", "READY_FOR_REVIEW", "WARNING" ->
            Triple(StatusPendingBg, StatusPending, status.replace("_", " ").uppercase())
        "BLOCKED", "FAILED", "CRITICAL", "DECLINED" ->
            Triple(StatusFailedBg, StatusFailed, status.uppercase())
        "WHITELISTED", "INFO" ->
            Triple( Color(0xFFDAE2FF), AnchorPrimary, status.uppercase())
        else ->
            Triple(StatusUnsupportedBg, StatusUnsupported, status.uppercase())
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PremiumGateBanner(
    featureTitle: String,
    isPremiumActive: Boolean,
    onUpgradeClick: () -> Unit,
    onInstantUnlockTrial: () -> Unit
) {
    if (isPremiumActive) {
        Surface(
            color = StatusEnforcedBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusEnforced,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Anchor Family Premium Active — $featureTitle unlocked & enforced",
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusEnforced
                    )
                }
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = AnchorTertiaryFixed),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("premium_paywall_banner")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium Feature",
                        tint = AnchorTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ANCHOR PREMIUM SAFEGUARD GROUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = AnchorTertiary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$featureTitle requires Anchor Family Premium",
                    style = MaterialTheme.typography.titleMedium,
                    color = AnchorTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Core screen-time limits, schedules, geofences, SOS alerts, family chat, and shared tasks are 100% FREE. Upgrade or start your 14-day trial to enforce $featureTitle on child devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnchorTertiary.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onInstantUnlockTrial,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AnchorTertiary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("instant_unlock_premium_trial_btn")
                    ) {
                        Text("Start 14-Day Free Trial Now")
                    }
                    OutlinedButton(
                        onClick = onUpgradeClick,
                        modifier = Modifier.testTag("view_premium_plans_btn")
                    ) {
                        Text("View P-15 Plans", color = AnchorTertiary)
                    }
                }
            }
        }
    }
}

@Composable
fun BilateralTransparencyCard(
    title: String,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = AnchorSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AnchorSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScreenDirectoryDialog(
    currentScreen: AnchorScreen,
    isPremiumActive: Boolean,
    onSelectScreen: (AnchorScreen) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Anchor Parent — All 20 Screens",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Google Stitch Reference Navigation (P-01 to P-20)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Directory")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(AnchorScreen.entries) { screen ->
                        val isCurrent = screen == currentScreen
                        Surface(
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectScreen(screen)
                                    onDismiss()
                                }
                                .testTag("dir_nav_${screen.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = AnchorPrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = screen.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (screen.isPremium) {
                                    Surface(
                                        color = if (isPremiumActive) StatusEnforcedBg else AnchorTertiaryFixed,
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = if (isPremiumActive) "PREMIUM ON" else "PREMIUM",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPremiumActive) StatusEnforced else AnchorTertiary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = StatusEnforcedBg,
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = "FREE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusEnforced,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
