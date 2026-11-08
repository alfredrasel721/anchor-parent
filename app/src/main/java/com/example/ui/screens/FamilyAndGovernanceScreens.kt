package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.BilateralTransparencyCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AnchorError
import com.example.ui.theme.AnchorPrimary
import com.example.ui.theme.AnchorSecondary
import com.example.viewmodel.AnchorUiState

// ============================================================================
// P-12: Family Chat & Shared Tasks (p_12_family_chat_tasks.html)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun P12FamilyChatTasksScreen(
    state: AnchorUiState,
    onSendMessage: (String) -> Unit,
    onAssignTask: (String, String, String) -> Unit,
    onApproveTask: (String) -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskReward by remember { mutableStateOf("+15m Bonus Screen Time") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shared Rhythm & Habit Tracking Card
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
                    Text("Shared Tasks & Habit Pacts (${state.tasks.size})", style = MaterialTheme.typography.titleMedium)
                    StatusBadge("FREE CORE")
                }

                state.tasks.forEach { task ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${task.rewardSummary} • ${task.dueSummary}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(task.status)
                            }
                            if (task.status != "COMPLETED") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onApproveTask(task.taskId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AnchorSecondary),
                                    modifier = Modifier.testTag("approve_task_${task.taskId}")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify & Unlock ${task.rewardSummary}")
                                }
                            }
                        }
                    }
                }

                // Assign New Task Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("New Habit Pact / Task") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_task_title_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onAssignTask(newTaskTitle, newTaskReward, "Due Today")
                            newTaskTitle = ""
                        },
                        modifier = Modifier.testTag("assign_task_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Assign")
                    }
                }
            }
        }

        // Family Chat Thread Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Bilateral Family Chat & Audit Stream", style = MaterialTheme.typography.titleMedium)

                state.messages.forEach { msg ->
                    val isParent = msg.senderRole == "PARENT"
                    val isSystem = msg.isSystemEvent || msg.senderRole == "SYSTEM"

                    Surface(
                        color = when {
                            isSystem -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            isParent -> AnchorPrimary
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isParent && !isSystem) Color.White.copy(alpha = 0.85f)
                                    else AnchorPrimary
                                )
                                Text(
                                    text = msg.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isParent && !isSystem) Color.White.copy(alpha = 0.75f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isParent && !isSystem) Color.White
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Quick Prompts
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Dinner is ready in 10 mins!",
                        "Awarded +15m Trust Bonus!",
                        "10m Bedtime Wind-Down reminder",
                        "Great job on homework!"
                    ).forEach { prompt ->
                        FilterChip(
                            selected = false,
                            onClick = { onSendMessage(prompt) },
                            label = { Text(prompt) }
                        )
                    }
                }

                // Composer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Message ${state.selectedChild.name}...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onSendMessage(messageInput)
                            messageInput = ""
                        },
                        modifier = Modifier.testTag("send_chat_message_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

// ============================================================================
// P-13: Guardian Roles & Trusted Contacts (p_13_guardian_roles_trusted_contacts.html)
// ============================================================================
@Composable
fun P13GuardianRolesTrustedContactsScreen(
    state: AnchorUiState,
    onToggleGeofenceAlerts: (String) -> Unit,
    onInviteContact: (String, String, String, Boolean) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("Trusted Emergency Contact") }
    var newContactInfo by remember { mutableStateOf("") }
    var isPrimaryGuardian by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BilateralTransparencyCard(
            title = "Anchor Separation of Powers",
            description = "Primary Guardians manage schedules and app limits. Trusted Emergency Contacts only receive SOS dispatches and optional school arrival check-ins—never browsing or app usage history."
        )

        val primaryGuardians = state.guardians.filter { it.isPrimaryGuardian }
        val trustedContacts = state.guardians.filter { !it.isPrimaryGuardian }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Primary Household Guardians (${primaryGuardians.size})", style = MaterialTheme.typography.titleMedium)
                primaryGuardians.forEach { g ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(g.name, style = MaterialTheme.typography.titleSmall)
                                Text(g.relationRole, style = MaterialTheme.typography.labelSmall, color = AnchorPrimary)
                                Text(g.permissionsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(g.status)
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Trusted Emergency SOS Contacts (${trustedContacts.size})", style = MaterialTheme.typography.titleMedium)
                trustedContacts.forEach { c ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                Text("${c.relationRole} • ${c.emailOrPhone}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Receive Geofence Check-ins: ${if (c.receiveGeofenceAlerts) "Enabled" else "SOS Only"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AnchorSecondary
                                )
                            }
                            Switch(
                                checked = c.receiveGeofenceAlerts,
                                onCheckedChange = { onToggleGeofenceAlerts(c.guardianId) },
                                modifier = Modifier.testTag("toggle_guardian_geo_${c.guardianId}")
                            )
                        }
                    }
                }
            }
        }

        // Invite Guardian or Emergency Contact Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Invite Co-Parent or Trusted Emergency Contact", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Full Name (e.g. Aunt Clara)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_guardian_name_input"),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newRole,
                        onValueChange = { newRole = it },
                        label = { Text("Relation / Role") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newContactInfo,
                        onValueChange = { newContactInfo = it },
                        label = { Text("Email or SMS Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPrimaryGuardian,
                        onCheckedChange = { isPrimaryGuardian = it }
                    )
                    Text("Grant Co-Parent Guardian policy management authority", style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        onInviteContact(newName, newRole, newContactInfo, isPrimaryGuardian)
                        newName = ""
                        newContactInfo = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_guardian_submit_btn")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Verified Family Invitation")
                }
            }
        }
    }
}

// ============================================================================
// P-14: Privacy, Data Rights & Deletion (p_14_privacy_export_deletion.html)
// ============================================================================
@Composable
fun P14PrivacyExportDeletionScreen(
    state: AnchorUiState,
    onExportJsonArchive: () -> Unit,
    onPurgeHistoricalLogs: () -> Unit,
    onToggleAnonymousTelemetry: () -> Unit,
    onDeleteHousehold: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BilateralTransparencyCard(
            title = "Zero Covert Telemetry & Statutory Data Rights",
            description = "Anchor complies with COPPA and GDPR-K principles. Household data is never sold, never used for advertising profiling, and can be exported or purged at any time."
        )

        // Household Data Inventory Card
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
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = AnchorPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Household Data Inventory & Retention", style = MaterialTheme.typography.titleMedium)
                }
                Text("• Location Trails (${state.geofences.size} zones): Auto-purged every 30 days", style = MaterialTheme.typography.bodySmall)
                Text("• App Usage & Category Totals (${state.appRules.size} rules): Aggregated daily", style = MaterialTheme.typography.bodySmall)
                Text("• Emergency SOS Drills (${state.alerts.count { it.category == "SOS" }} events): Encrypted audit log", style = MaterialTheme.typography.bodySmall)
                Text("• Bilateral Audit Trail (${state.auditLogs.size} local Room entries): Tamper-evident", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Statutory Privacy Actions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Data Portability & Minimization Controls", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = onExportJsonArchive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_household_json_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Portable JSON Data Archive")
                }

                OutlinedButton(
                    onClick = onPurgeHistoricalLogs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purge_historical_logs_btn")
                ) {
                    Text("Purge Historical Location & Audit Logs (Keep Rules)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anonymous Diagnostic Crash Telemetry", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Strictly opt-in. Zero child identity or browsing data included.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.anonymousDiagnosticTelemetry,
                        onCheckedChange = { onToggleAnonymousTelemetry() }
                    )
                }
            }
        }

        // Danger Zone Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3F2)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Danger Zone • Right to Erasure", style = MaterialTheme.typography.titleMedium, color = AnchorError)
                Text(
                    "Permanently delete ${state.familyHousehold.familyName}, unpair all child devices, and erase all cloud Firestore and local Room records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AnchorError
                )
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AnchorError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("initiate_family_deletion_btn")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Initiate Family Deletion Request")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirm Permanent Household Deletion") },
            text = {
                Text("This will immediately erase all household records from local Room cache and Firebase Firestore and return to P-01 Welcome. Proceed?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteHousehold()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnchorError),
                    modifier = Modifier.testTag("confirm_delete_household_btn")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
