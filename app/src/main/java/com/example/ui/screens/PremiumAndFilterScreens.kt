package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.AnchorPrimaryContainer
import com.example.ui.theme.AnchorSecondary
import com.example.ui.theme.AnchorTertiary
import com.example.ui.theme.AnchorTertiaryFixed
import com.example.ui.theme.StatusEnforced
import com.example.viewmodel.AnchorUiState

// ============================================================================
// P-15: Premium Features & Subscription (p_15_premium_features.html)
// ============================================================================
@Composable
fun P15PremiumFeaturesScreen(
    state: AnchorUiState,
    onSetSubscriptionTier: (String, Boolean) -> Unit,
    onNavigate: (AnchorScreen) -> Unit
) {
    var selectedPlan by remember(state.familyHousehold.subscriptionTier) {
        mutableStateOf(
            if (state.familyHousehold.subscriptionTier == "PREMIUM_MONTHLY") "PREMIUM_MONTHLY"
            else "PREMIUM_ANNUAL"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(AnchorPrimary, AnchorPrimaryContainer)))
                .padding(22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = AnchorTertiaryFixed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ANCHOR FAMILY PREMIUM",
                            style = MaterialTheme.typography.labelLarge,
                            color = AnchorTertiaryFixed
                        )
                    }
                    StatusBadge(if (state.isPremiumActive) "PREMIUM ACTIVE" else "FREE CORE TIER")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Everything Essential is Free. Upgrade Only for Advanced Live & Content Shields.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All core screen-time limits, schedules, safe geofences, SOS alerts, requests, family chat, shared tasks, and privacy exports are 100% FREE forever.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        // The Exact 6 Mandatory Premium Feature Groups Breakdown
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
                    text = "The 6 Exclusive Premium Safeguard Groups",
                    style = MaterialTheme.typography.titleMedium
                )
                val sixGroups = listOf(
                    Triple("1. Screen Mirroring & Live Screen Viewing", "Audited, consent-gated live screen check-in (P-19)", AnchorScreen.P19_LIVE_SESSION_VIEWER),
                    Triple("2. Live Camera Access", "Emergency / safety consent-gated front camera check (P-19)", AnchorScreen.P19_LIVE_SESSION_VIEWER),
                    Triple("3. Live Microphone Audio", "Audited ambient audio & two-way voice safety check (P-19)", AnchorScreen.P19_LIVE_SESSION_VIEWER),
                    Triple("4. Notification Filtering", "Study & Bedtime distraction batching & streak interception (P-18)", AnchorScreen.P18_NOTIFICATION_FILTER),
                    Triple("5. Website & Web-Content Filtering", "SafeSearch, category blocks & custom domain lists (P-16)", AnchorScreen.P16_WEB_FILTER_RULES),
                    Triple("6. Social-Media Content Filtering", "Discord, Roblox, WhatsApp & YouTube safety shields (P-17)", AnchorScreen.P17_SOCIAL_CONTENT_FILTERS)
                )
                sixGroups.forEach { (title, subtitle, targetScreen) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(targetScreen) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(if (state.isPremiumActive) "UNLOCKED" else "PREMIUM")
                        }
                    }
                }
            }
        }

        // Plan Selector Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Family Subscription Plan", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = if (selectedPlan == "PREMIUM_ANNUAL") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (selectedPlan == "PREMIUM_ANNUAL") 2.dp else 1.dp,
                                color = if (selectedPlan == "PREMIUM_ANNUAL") AnchorPrimary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPlan = "PREMIUM_ANNUAL" }
                            .testTag("select_annual_plan_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("BEST VALUE • SAVE 37%", style = MaterialTheme.typography.labelSmall, color = AnchorSecondary)
                            Text("Annual Family", style = MaterialTheme.typography.titleMedium)
                            Text("$4.99 / mo", style = MaterialTheme.typography.headlineSmall, color = AnchorPrimary)
                            Text("Billed $59.99/yr • Up to 5 children", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Surface(
                        color = if (selectedPlan == "PREMIUM_MONTHLY") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (selectedPlan == "PREMIUM_MONTHLY") 2.dp else 1.dp,
                                color = if (selectedPlan == "PREMIUM_MONTHLY") AnchorPrimary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPlan = "PREMIUM_MONTHLY" }
                            .testTag("select_monthly_plan_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("FLEXIBLE", style = MaterialTheme.typography.labelSmall)
                            Text("Monthly Plan", style = MaterialTheme.typography.titleMedium)
                            Text("$7.99 / mo", style = MaterialTheme.typography.headlineSmall, color = AnchorPrimary)
                            Text("Cancel anytime • Up to 5 children", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Button(
                    onClick = { onSetSubscriptionTier(selectedPlan, true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("activate_premium_subscription_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AnchorPrimary)
                ) {
                    Text(
                        if (state.isPremiumActive) "Update Active Plan ($selectedPlan)"
                        else "Start 14-Day Free Trial & Unlock All 6 Premium Groups"
                    )
                }

                if (state.isPremiumActive) {
                    OutlinedButton(
                        onClick = { onSetSubscriptionTier("FREE", false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("revert_to_free_tier_btn")
                    ) {
                        Text("Switch Back to Free Core Tier (Test Paywall Gating)")
                    }
                }

                Text(
                    text = "Environment Note: Subscription state is persisted in Firebase Firestore (`families/{familyId}`) and local Room storage. Live Google Play Billing merchant settlement requires a Play Console release build.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// P-16: Web & Content Filters [PREMIUM] (p_16_web_filter_rules.html)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun P16WebFilterRulesScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onSelectProfile: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    onAddWhitelist: (String) -> Unit,
    onRemoveWhitelist: (String) -> Unit,
    onAddBlacklist: (String) -> Unit,
    onRemoveBlacklist: (String) -> Unit,
    onNavigateToPremium: () -> Unit,
    onInstantUnlockTrial: () -> Unit
) {
    var newAllowedDomain by remember { mutableStateOf("") }
    var newBlockedDomain by remember { mutableStateOf("") }
    var showBlockScreenPreview by remember { mutableStateOf(false) }
    val config = state.premiumConfig

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PremiumGateBanner(
            featureTitle = "Website & Web-Content Filtering",
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

        // Protection Profiles Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Web Protection Profile", style = MaterialTheme.typography.titleMedium)
                listOf(
                    "STRICT_SAFEGUARD" to "Strict SafeGuard (Whitelisted & Kid-Safe Sites Only)",
                    "BALANCED_GUARDIAN" to "Balanced Guardian (Blocks Adult, Gambling, Violence & Unmoderated Chat)",
                    "CUSTOM_GRANULAR" to "Custom Granular Protection (Per-Category & Domain Control)"
                ).forEach { (key, desc) ->
                    Surface(
                        color = if (config.webFilterProfile == key) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = state.isPremiumActive) { onSelectProfile(key) }
                            .testTag("web_profile_$key")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (config.webFilterProfile == key) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AnchorPrimary)
                            }
                        }
                    }
                }
            }
        }

        // System Category Rules
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("System Category Interception Rules", style = MaterialTheme.typography.titleMedium)
                val allCategories = listOf(
                    "Adult & Explicit Content (Mandatory SafeSearch Lock)",
                    "Gambling, Betting & Loot Boxes",
                    "Violence, Hate & Extremism",
                    "Social Media Web Portals",
                    "Online Gaming Portals",
                    "Unmoderated AI Chatbots"
                )
                allCategories.forEach { cat ->
                    val isAdult = cat.startsWith("Adult")
                    val isBlocked = isAdult || config.blockedCategories.contains(cat)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(
                            checked = isBlocked,
                            enabled = state.isPremiumActive && !isAdult,
                            onCheckedChange = { onToggleCategory(cat) }
                        )
                    }
                }
            }
        }

        // Whitelist & Blacklist Domain Management
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Always Allowed Whitelist (${config.whitelistedDomains.size})", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    config.whitelistedDomains.forEach { domain ->
                        FilterChip(
                            selected = true,
                            onClick = { if (state.isPremiumActive) onRemoveWhitelist(domain) },
                            label = { Text("$domain ✕") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newAllowedDomain,
                        onValueChange = { newAllowedDomain = it },
                        enabled = state.isPremiumActive,
                        label = { Text("Add Allowed Site (e.g. scratch.mit.edu)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("whitelist_domain_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onAddWhitelist(newAllowedDomain)
                            newAllowedDomain = ""
                        },
                        enabled = state.isPremiumActive,
                        modifier = Modifier.testTag("add_whitelist_domain_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Always Blocked Blacklist (${config.blacklistedDomains.size})", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    config.blacklistedDomains.forEach { domain ->
                        FilterChip(
                            selected = true,
                            onClick = { if (state.isPremiumActive) onRemoveBlacklist(domain) },
                            label = { Text("$domain ✕") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newBlockedDomain,
                        onValueChange = { newBlockedDomain = it },
                        enabled = state.isPremiumActive,
                        label = { Text("Block Domain (e.g. example.com)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("blacklist_domain_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onAddBlacklist(newBlockedDomain)
                            newBlockedDomain = ""
                        },
                        enabled = state.isPremiumActive,
                        colors = ButtonDefaults.buttonColors(containerColor = AnchorError),
                        modifier = Modifier.testTag("add_blacklist_domain_btn")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null)
                    }
                }

                OutlinedButton(
                    onClick = { showBlockScreenPreview = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_child_block_screen_btn")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Child Block Screen Experience")
                }
            }
        }
    }

    if (showBlockScreenPreview) {
        AlertDialog(
            onDismissRequest = { showBlockScreenPreview = false },
            title = { Text("Child Block Screen Preview") },
            text = {
                Text(
                    "\"Heads up, ${state.selectedChild.name}! This website is paused by your family's ${config.webFilterProfile} agreement. If you need this page for schoolwork, tap 'Ask Guardian for Access' below.\""
                )
            },
            confirmButton = {
                Button(onClick = { showBlockScreenPreview = false }) {
                    Text("Close Preview")
                }
            }
        )
    }
}

// ============================================================================
// P-17: Social App & Content Filters [PREMIUM] (p_17_social_app_content_filters.html)
// ============================================================================
@Composable
fun P17SocialAppContentFiltersScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onSelectShieldProfile: (String) -> Unit,
    onTogglePlatformRule: (String) -> Unit,
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
            featureTitle = "Social-Media Content Filtering",
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

        // Global Shield Profile Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Global Social Shield Mode", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SUPERVISED", "STRICT_BLOCK", "GRANULAR").forEach { mode ->
                        FilterChip(
                            selected = config.socialShieldProfile == mode,
                            enabled = state.isPremiumActive,
                            onClick = { onSelectShieldProfile(mode) },
                            label = { Text(mode.replace("_", " ")) },
                            modifier = Modifier.testTag("social_shield_$mode")
                        )
                    }
                }
            }
        }

        // Platform Policies Cards: Discord, Roblox, WhatsApp, YouTube
        val platforms = listOf(
            Triple("DISCORD", "Discord — Direct Message & Server Guard", config.discordDmWhitelistOnly to "Allow DMs from Whitelisted Family/Friends Only"),
            Triple("ROBLOX", "Roblox — Experience & In-Game Chat Shield", config.robloxChatFiltered to "Enforce Account Restrictions & Filter Public Chat"),
            Triple("WHATSAPP", "WhatsApp — Unknown Sender & Group Guard", config.whatsappUnknownBlocked to "Block Messages & Group Invites from Unknown Numbers"),
            Triple("YOUTUBE", "YouTube & Shorts — Restricted Feed Guard", config.youtubeRestrictedMode to "Enforce Strict Restricted Mode & Hide Comment Feeds")
        )

        platforms.forEach { (key, title, statePair) ->
            val (checked, desc) = statePair
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = checked,
                        enabled = state.isPremiumActive,
                        onCheckedChange = { onTogglePlatformRule(key) },
                        modifier = Modifier.testTag("toggle_social_$key")
                    )
                }
            }
        }

        BilateralTransparencyCard(
            title = "No Covert Private Message Reading",
            description = "Anchor filters unknown senders, explicit links, and unmoderated public servers structurally—without secretly harvesting or uploading your child's private friend conversations."
        )
    }
}
