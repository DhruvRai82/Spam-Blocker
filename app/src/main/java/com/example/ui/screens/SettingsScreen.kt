package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CallBlockerViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: CallBlockerViewModel
) {
    val context = LocalContext.current
    val reports by viewModel.spamReports.collectAsState()

    val activeThemeSegment = viewModel.activeTheme
    val aiSensitivityVal = viewModel.aiSensitivityVal
    val selectedIconVariant = viewModel.selectedIconVariant
    val isTelemetryShareEnabled = viewModel.isTelemetryShareEnabled

    var tempSensitivity by remember(aiSensitivityVal) { mutableStateOf(aiSensitivityVal) }
    var isSyncing by remember { mutableStateOf(false) }
    var dbUpdateDate by remember { mutableStateOf("Last updated: 2 hours ago") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        
        // 1. PROFILE PROFILE HEADER WITH DYNAMIC STATS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.size(96.dp)
                    ) {
                        // Profile Avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = "Edit photo profile button",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Little edit icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Commander Guardian",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )

                    Text(
                        text = "+1-555-SHIELD-01",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contribution Badge linked to DB elements!
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF39C12).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFF39C12), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFF39C12), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardian Shield: ${reports.size} Reports Filed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF39C12)
                            )
                        }
                    }
                }
            }
        }

        // 2. DETAILED PROTECTION CONFIGS
        item {
            SettingsGroupHeader(label = "SHIELD CORE SETTINGS")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Core Role Manager Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mock Core Screen clearance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Force operational role grant if emulator denies native popup dialogs.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewModel.isMockRoleGranted,
                            onCheckedChange = { viewModel.toggleMockRoleGranted() },
                            modifier = Modifier.testTag("mock_role_toggle")
                        )
                    }

                    Divider()

                    // DB Updates option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Threat Blacklist Cache", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(dbUpdateDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        val scope = rememberCoroutineScope()
                        OutlinedButton(
                            enabled = !isSyncing,
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    dbUpdateDate = "Syncing threat directories..."
                                    kotlinx.coroutines.delay(1200)
                                    isSyncing = false
                                    dbUpdateDate = "Last updated: Just now (✓ OK)"
                                    Toast.makeText(context, "Anti-robocalls vectors synchronized smoothly!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Force Sync", fontSize = 11.sp)
                            }
                        }
                    }

                    Divider()

                    // Low High Sensitivity Dials
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AI Screening Confidence Level", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = when {
                                    tempSensitivity < 0.4f -> "Mild Sensitivity"
                                    tempSensitivity < 0.8f -> "Standard Shield"
                                    else -> "Maximum Defend"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = tempSensitivity,
                            onValueChange = { tempSensitivity = it },
                            onValueChangeFinished = {
                                viewModel.updateSensitivity(tempSensitivity)
                                Toast.makeText(context, "AI sensitivity saved: ${String.format("%.2f", tempSensitivity)}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // 3. APPEARANCE WITH SEGMENTED COLOR SELECTS
        item {
            SettingsGroupHeader(label = "INTERFACE & APPEARANCE")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Theme segmented select row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Active Interface Palette", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            listOf("System", "Light", "Dark").forEach { theme ->
                                val active = activeThemeSegment == theme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { 
                                            viewModel.selectTheme(theme)
                                            Toast.makeText(context, "$theme theme applied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = theme,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Divider()

                    // Select launchers visual variants
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Defending Launcher Variant App Icon", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Defending Cobalt", "Crimson Threat", "Slate Dark").forEach { variant ->
                                val active = selectedIconVariant == variant
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { 
                                            viewModel.selectIconVariant(variant)
                                            Toast.makeText(context, "App icon updated to $variant configuration!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = variant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. STORAGE PRIVACY TRASH CLEARING
        item {
            SettingsGroupHeader(label = "SECURITY DATA CONTROL")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Database Telemetry Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Stream anonymous robocalls prefix details to crowdsourced clouds.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isTelemetryShareEnabled,
                            onCheckedChange = { 
                                viewModel.toggleTelemetryShare()
                                val msg = if (!isTelemetryShareEnabled) "Crowdsourced logs telemetry sharing active" else "Private offline-only mode active"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wipe local directories caches", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        OutlinedButton(
                            onClick = {
                                viewModel.clearLogs()
                                viewModel.clearSpamReports()
                                Toast.makeText(context, "Archive databases fully cleared!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Clear all Logs", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // 5. TRASH TERMINATION LOGOUT BUTTON
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "ShieldCall system sessions signing off...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("logout_btn")
                ) {
                    Text("De-register ShieldCall Account Device", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SettingsGroupHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
}
