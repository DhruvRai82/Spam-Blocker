package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CallBlockerViewModel

@Composable
fun BlockRulesScreen(
    viewModel: CallBlockerViewModel
) {
    val context = LocalContext.current
    var showAddWhitelistRow by remember { mutableStateOf(false) }

    val whitelist by viewModel.whitelistedNumbers.collectAsState()
    val patternSpecs = viewModel.patternsList

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        
        // 1. MASTER SHIELD PROTECTION SWITCH BLOCK
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isServiceEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = if (viewModel.isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Spam Shield",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (viewModel.isServiceEnabled) "Active background screening" else "Disabled background screening",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.isServiceEnabled,
                        onCheckedChange = { viewModel.toggleService() },
                        modifier = Modifier.testTag("master_spam_switch")
                    )
                }
            }
        }

        // 2. DETAILED THREAT SETTINGS TOGGLES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Smart Filter Categories",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Divider(modifier = Modifier.padding(bottom = 4.dp))

                    ToggleRuleRow(
                        title = "Block Robocalls",
                        subtitle = "Silences automated dialer voices using machine database",
                        checked = viewModel.isFilterRobocallsEnabled,
                        onCheckedChange = { viewModel.toggleFilterRobocalls() }
                    )

                    ToggleRuleRow(
                        title = "Block Telemarketing",
                        subtitle = "Bans commercial sales agency loops instantly",
                        checked = viewModel.isFilterTelemarketingEnabled,
                        onCheckedChange = { viewModel.toggleFilterTelemarketing() }
                    )

                    ToggleRuleRow(
                        title = "Block Scam Likely",
                        subtitle = "Auto-drops calls flagged with aggressive risk scores",
                        checked = viewModel.isFilterScamEnabled,
                        onCheckedChange = { viewModel.toggleFilterScam() }
                    )

                    ToggleRuleRow(
                        title = "Unknown Numbers Bypass",
                        subtitle = "Silently drops any number not present in directory",
                        checked = viewModel.isFilterUnknownEnabled,
                        onCheckedChange = { viewModel.toggleFilterUnknown() }
                    )

                    ToggleRuleRow(
                        title = "International Blacklist",
                        subtitle = "Blocks incoming overseas calls from unknown origins",
                        checked = viewModel.isFilterInternationalEnabled,
                        onCheckedChange = { viewModel.toggleFilterInternational() }
                    )

                    ToggleRuleRow(
                        title = "Silent Drop Hangup",
                        subtitle = "Terminates matches instantly instead of sending to VM",
                        checked = viewModel.isFilterSilentEnabled,
                        onCheckedChange = { viewModel.toggleFilterSilent() }
                    )
                }
            }
        }

        // 3. NUMBER PATTERNS CHIPS SECTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Wildcard Wildcard Prefixes",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Blocks groups of numbers (example: all from +91-140)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Field to add a Prefix Pattern
                    var patternInput by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = patternInput,
                            onValueChange = { patternInput = it },
                            placeholder = { Text("+91140 or +1800") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (patternInput.isNotBlank()) {
                                    viewModel.addPattern(patternInput)
                                    patternInput = ""
                                    Toast.makeText(context, "Added pattern matching wildcard!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Flow of chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        patternSpecs.forEach { prefix ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prefix, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.removePattern(prefix) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. DO NOT DISTURB ARC / RANGE SELECTOR CHIPS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Screening Quiet Hours",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Bypass block rules for select exceptions only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewModel.isDndEnabled,
                            onCheckedChange = { viewModel.toggleDndEnabled() }
                        )
                    }

                    if (viewModel.isDndEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual DND Hours range slider dials
                        var startHourState by remember { mutableStateOf(viewModel.dndStartHour.toFloat()) }
                        var endHourState by remember { mutableStateOf(viewModel.dndEndHour.toFloat()) }

                        // Start Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Quiet Block Start Hour", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${startHourState.toInt()}:00 PM", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            }
                            Slider(
                                value = startHourState,
                                onValueChange = {
                                    startHourState = it
                                    viewModel.updateDndRange(it.toInt(), 0, endHourState.toInt(), 0)
                                },
                                valueRange = 12f..23f
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // End Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Quiet Block End Hour", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${endHourState.toInt()}:00 AM", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            }
                            Slider(
                                value = endHourState,
                                onValueChange = {
                                    endHourState = it
                                    viewModel.updateDndRange(startHourState.toInt(), 0, it.toInt(), 0)
                                },
                                valueRange = 1f..11f
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bypass Starred Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Checkbox(checked = true, onCheckedChange = {})
                        }
                    }
                }
            }
        }

        // 5. MANUAL WHITELIST LISTING EXCEPTIONS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Emergency Whitelists Exception",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Numbers that will always bypass filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showAddWhitelistRow = !showAddWhitelistRow }) {
                            Icon(
                                imageVector = if (showAddWhitelistRow) Icons.Default.Close else Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Input Form inline
                    AnimatedVisibility(visible = showAddWhitelistRow) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var rawNo by remember { mutableStateOf("") }
                            var rawName by remember { mutableStateOf("") }

                            OutlinedTextField(
                                value = rawName,
                                onValueChange = { rawName = it },
                                placeholder = { Text("Display Label (example: Doctor)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = rawNo,
                                onValueChange = { rawNo = it },
                                placeholder = { Text("Numbers digits (example: +9198)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (rawNo.isNotBlank()) {
                                        viewModel.addNumberToWhitelist(rawNo, rawName)
                                        rawNo = ""
                                        rawName = ""
                                        showAddWhitelistRow = false
                                        Toast.makeText(context, "Bypass Whitelist registered!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Whitelist Bypass Entry")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (whitelist.isEmpty()) {
                        Text(
                            text = "No custom whitelist exceptions defined.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            whitelist.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(contact.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(contact.phoneNumber, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    IconButton(onClick = { viewModel.removeNumberFromWhitelist(contact.phoneNumber) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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

@Composable
fun ToggleRuleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f)
        )
    }
}
