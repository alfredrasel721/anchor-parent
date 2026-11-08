package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnchorScreen
import com.example.ui.components.BilateralTransparencyCard
import com.example.ui.components.ChildSelectorBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AnchorPrimary
import com.example.ui.theme.AnchorPrimaryContainer
import com.example.ui.theme.AnchorSecondary
import com.example.ui.theme.AnchorSecondaryContainer
import com.example.ui.theme.StatusEnforced
import com.example.ui.theme.StatusEnforcedBg
import com.example.viewmodel.AnchorUiState

// ============================================================================
// P-01: Welcome & Sign In (p_01_welcome_sign_in.html)
// ============================================================================
@Composable
fun P01WelcomeSignInScreen(
    state: AnchorUiState,
    onGoogleSignIn: () -> Unit,
    onEnterGuardianWorkspace: (AnchorScreen) -> Unit,
    onStartOnboarding: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card matching Stitch P-01
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AnchorPrimary, AnchorPrimaryContainer)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = AnchorSecondaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Anchor Emblem",
                            tint = AnchorSecondary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ANCHOR FAMILY SYSTEM • v4.2",
                            style = MaterialTheme.typography.labelSmall,
                            color = AnchorSecondaryContainer
                        )
                        Text(
                            text = "Anchor Parent",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Calm, Transparent Digital Guardianship.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Built on mutual family trust rather than covert surveillance. Manage screen allowances, bedtime schedules, safe school zones, and SOS protection in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        // The Anchor Family Pledge Card
        BilateralTransparencyCard(
            title = "The Anchor Family Pledge",
            description = "Every rule, schedule, and safety check is visible to both parent and child. No hidden spyware, no secret keylogging, and explicit child consent for any live safety check-in."
        )

        // Primary Authentication Card (Google Sign-In via Jetpack Credential Manager)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    Text(
                        text = "Guardian Authentication",
                        style = MaterialTheme.typography.titleLarge
                    )
                    StatusBadge(if (state.isCloudConnected) "CLOUD READY" else "CREDENTIAL MANAGER")
                }

                Text(
                    text = "Sign in with your Google Guardian account to synchronize your household across Firebase Firestore Enterprise and local Room storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onGoogleSignIn,
                    enabled = !state.authLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AnchorPrimary)
                ) {
                    if (state.authLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Connecting to Google Identity...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google Guardian Account",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Biometric / Instant Guardian Workspace Entry (supports emulators without Play account signed in)
                OutlinedButton(
                    onClick = { onEnterGuardianWorkspace(AnchorScreen.P04_FAMILY_DASHBOARD) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("enter_guardian_workspace_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = AnchorSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock Sarah Jenkins Household (P-04 Dashboard)",
                        color = AnchorPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartOnboarding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_family_onboarding_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("New Family Setup (P-02)")
                    }
                    OutlinedButton(
                        onClick = { onEnterGuardianWorkspace(AnchorScreen.P03_ADD_CHILD_PAIR_CODE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_pair_child_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Pair Child Code (P-03)")
                    }
                }
            }
        }

        // Security & Architecture Notice
        Surface(
            color = StatusEnforcedBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = StatusEnforced
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "End-to-End Household Isolation",
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusEnforced
                    )
                    Text(
                        text = "Protected by zero-trust Firestore Security Rules, Jetpack Credential Manager OAuth2, and Android Keystore hardware attestation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// P-02: Family Onboarding (p_02_family_onboarding.html)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun P02FamilyOnboardingScreen(
    state: AnchorUiState,
    onSaveHousehold: (String, String, String, String) -> Unit,
    onAddChild: (String, Int, String, String) -> Unit,
    onProceedToPairing: () -> Unit
) {
    var familyName by remember(state.familyHousehold.familyName) {
        mutableStateOf(state.familyHousehold.familyName)
    }
    var guardianName by remember(state.guardianDisplayName) {
        mutableStateOf(state.guardianDisplayName)
    }
    var selectedRole by remember(state.guardianRoleChip) {
        mutableStateOf(state.guardianRoleChip)
    }
    var selectedCadence by remember(state.notificationCadence) {
        mutableStateOf(state.notificationCadence)
    }

    var newChildName by remember { mutableStateOf("") }
    var newChildAge by remember { mutableStateOf("10") }
    var newChildGrade by remember { mutableStateOf("5th Grade") }
    var newChildDevice by remember { mutableStateOf("Galaxy Tab A9") }

    var pledgeTransparent by remember { mutableStateOf(true) }
    var pledgeEmergencyAlwaysOn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Indicator
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "STEP 1 OF 3 • HOUSEHOLD CHARTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = AnchorPrimary
                    )
                    Text(
                        text = "Configure Family & Guardian Identity",
                        style = MaterialTheme.typography.titleMedium,
                        color = AnchorPrimary
                    )
                }
                StatusBadge("ACTIVE")
            }
        }

        // Section 1: Family & Guardian Profile
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Household & Primary Guardian",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    label = { Text("Family Household Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_family_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("Primary Guardian Display Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_guardian_name_input"),
                    singleLine = true
                )

                Text(
                    text = "Guardian Family Role",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mother", "Father", "Legal Guardian", "Co-Parent").forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role) }
                        )
                    }
                }

                Text(
                    text = "Notification Cadence",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All Safety & Requests", "Critical & Requests Only", "Daily Digest + SOS").forEach { cad ->
                        FilterChip(
                            selected = selectedCadence == cad,
                            onClick = { selectedCadence = cad },
                            label = { Text(cad) }
                        )
                    }
                }

                Button(
                    onClick = { onSaveHousehold(familyName, guardianName, selectedRole, selectedCadence) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_household_profile_btn")
                ) {
                    Text("Save Household Profile")
                }
            }
        }

        // Section 2: Children Roster & Add Child Form
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. Children in Household (${state.children.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                state.children.forEach { child ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${child.name} (Age ${child.age})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${child.gradeLabel} • ${child.deviceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(child.syncStatus)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add Another Child Profile",
                    style = MaterialTheme.typography.labelLarge,
                    color = AnchorPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newChildName,
                        onValueChange = { newChildName = it },
                        label = { Text("Child Name") },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("new_child_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newChildAge,
                        onValueChange = { newChildAge = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.6f)
                            .testTag("new_child_age_input"),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newChildGrade,
                        onValueChange = { newChildGrade = it },
                        label = { Text("Grade / School") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newChildDevice,
                        onValueChange = { newChildDevice = it },
                        label = { Text("Target Device") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedButton(
                    onClick = {
                        onAddChild(
                            newChildName,
                            newChildAge.toIntOrNull() ?: 10,
                            newChildGrade,
                            newChildDevice
                        )
                        newChildName = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_add_child_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Child to Household Roster")
                }
            }
        }

        // Section 3: Family Trust Agreement
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3. Bilateral Family Trust Charter",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = pledgeTransparent,
                        onCheckedChange = { pledgeTransparent = it }
                    )
                    Text(
                        text = "I agree to discuss screen boundaries openly with my child; all active rules will be shown clearly on their device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = pledgeEmergencyAlwaysOn,
                        onCheckedChange = { pledgeEmergencyAlwaysOn = it }
                    )
                    Text(
                        text = "Emergency SOS, 911 dialer, and Primary Guardian calls will never be blocked during downtime.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        onSaveHousehold(familyName, guardianName, selectedRole, selectedCadence)
                        onProceedToPairing()
                    },
                    enabled = pledgeTransparent && pledgeEmergencyAlwaysOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_to_pair_code_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue to Step 2: Pair Child Device (P-03)")
                }
            }
        }
    }
}

// ============================================================================
// P-03: Add Child & Pair Device (p_03_add_child_pair_code.html)
// ============================================================================
@Composable
fun P03AddChildPairCodeScreen(
    state: AnchorUiState,
    onSelectChild: (String) -> Unit,
    onGenerateNewCode: () -> Unit,
    onCopyCode: () -> Unit,
    onShareInstructions: () -> Unit,
    onFinishToDashboard: () -> Unit
) {
    var showQrMatrix by remember { mutableStateOf(false) }
    val selectedChild = state.selectedChild

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Child selector bar
        ChildSelectorBar(
            children = state.children,
            selectedChildId = state.selectedChildId,
            onSelectChild = onSelectChild,
            onAddChildClick = { /* Already on P-03 */ }
        )

        // Pairing Code Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "PAIRING FOR ${selectedChild.name.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AnchorPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    StatusBadge("VALID 9m 44s")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Switcher (6-Digit Code vs QR Matrix)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(4.dp)
                ) {
                    Surface(
                        color = if (!showQrMatrix) AnchorPrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showQrMatrix = false }
                            .testTag("pair_mode_passcode_tab")
                    ) {
                        Text(
                            text = "6-Digit Passcode",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!showQrMatrix) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    Surface(
                        color = if (showQrMatrix) AnchorPrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showQrMatrix = true }
                            .testTag("pair_mode_qr_tab")
                    ) {
                        Text(
                            text = "QR Handshake Matrix",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (showQrMatrix) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!showQrMatrix) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, AnchorPrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.pairingCode.code,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 42.sp,
                                    letterSpacing = 6.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = AnchorPrimary,
                                modifier = Modifier.testTag("pairing_code_display")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "One-Time Cryptographic Token • Target: ${selectedChild.deviceName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Deterministic visual QR matrix rendered on Canvas
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .size(210.dp)
                            .border(2.dp, AnchorPrimary, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        val codeSeed = state.pairingCode.code.hashCode()
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val grid = 11
                            val cellW = size.width / grid
                            val cellH = size.height / grid
                            for (r in 0 until grid) {
                                for (c in 0 until grid) {
                                    val isFinder = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                                    val bit = ((codeSeed shr ((r * grid + c) % 24)) and 1) == 1
                                    if (isFinder || bit) {
                                        drawRect(
                                            color = AnchorPrimary,
                                            topLeft = Offset(c * cellW, r * cellH),
                                            size = Size(cellW * 0.9f, cellH * 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan from Anchor Child App on ${selectedChild.deviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyCode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_pairing_code_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code")
                    }
                    OutlinedButton(
                        onClick = onGenerateNewCode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("refresh_pairing_code_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Code")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onShareInstructions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_pairing_link_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AnchorSecondary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Pairing Invite via Android Share Sheet")
                }
            }
        }

        // 3-Step Pairing Guide Card
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
                    Icon(Icons.Default.PhonelinkSetup, contentDescription = null, tint = AnchorPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How Child Device Pairing Works",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "1. Install Anchor Child Companion on ${selectedChild.name}'s Android device.\n" +
                        "2. Enter passcode ${state.pairingCode.code} or scan the QR matrix above.\n" +
                        "3. Review the transparent Family Charter together with ${selectedChild.name} and grant Usage Stats, Accessibility & Location permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onFinishToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("go_to_dashboard_from_pairing_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Family Dashboard (P-04)")
                }
            }
        }
    }
}
