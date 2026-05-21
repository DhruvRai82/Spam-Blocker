package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun ReportSpamScreen(
    viewModel: CallBlockerViewModel,
    defaultNumber: String, // Pre-filled from caller details if navigate with arg
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(1) } // Steps: 1, 2, 3, 4 (Success state is 4)

    // Form states
    var repNumber by remember { mutableStateOf(defaultNumber) }
    var repCategory by remember { mutableStateOf("") }
    var repDescription by remember { mutableStateOf("") }
    var receivedCallToggle by remember { mutableStateOf(true) }

    val recentDialedLogs by viewModel.blockedCalls.collectAsState()
    val prefillSuggestions = remember(recentDialedLogs) {
        recentDialedLogs.take(3).map { it.phoneNumber }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Report Threat", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close reporting process")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // A. INDICATOR FOR GUIDED MULTI-STEPS
            if (currentStep < 4) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STEP $currentStep OF 3",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = when (currentStep) {
                                1 -> "Identify Number"
                                2 -> "Classify Threat"
                                else -> "Describe Event"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Step Progress Line indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..3) {
                            val active = i <= currentStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                            )
                        }
                    }
                }
            }

            // B. CENTRAL COMPARTMENT LAYOUT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "StepAnim"
                ) { targetStep ->
                    when (targetStep) {
                        1 -> {
                            // STEP 1 — NUMBER ENTRY
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Whose number is suspicious?",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Enter the caller digits you intend to file against our community firewall.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = repNumber,
                                    onValueChange = { repNumber = it },
                                    label = { Text("Phone Number") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("report_field_phone")
                                )

                                // Prefill buttons helper
                                if (prefillSuggestions.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("REPLY TO RECENT CALLS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            prefillSuggestions.forEach { text ->
                                                AssistChip(
                                                    onClick = { repNumber = text },
                                                    label = { Text(text) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // STEP 2 — SELECTION GRID CARDS
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Select Category Threat",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                val categories = listOf(
                                    ReportCategoryItem("Robocall", Icons.Default.VolumeMute),
                                    ReportCategoryItem("Telemarketer", Icons.Default.Call),
                                    ReportCategoryItem("Scam likely", Icons.Default.Warning),
                                    ReportCategoryItem("Fraud Alert", Icons.Default.Security),
                                    ReportCategoryItem("Harassment", Icons.Default.PriorityHigh),
                                    ReportCategoryItem("Silent Call", Icons.Default.StarBorder)
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(260.dp)
                                ) {
                                    items(categories) { item ->
                                        val isSelected = repCategory == item.name
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(76.dp)
                                                .clickable { repCategory = item.name }
                                                .testTag("category_card_${item.name}"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                }
                                            ),
                                            border = if (isSelected) {
                                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                            } else null
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // STEP 3 DETAILS & CONFIRM
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Add Additional Context",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                OutlinedTextField(
                                    value = repDescription,
                                    onValueChange = { repDescription = it },
                                    placeholder = { Text("What happened during this call? What did they request? (Optional)") },
                                    minLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Check options toggler
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("I actually answered this call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = receivedCallToggle,
                                            onCheckedChange = { receivedCallToggle = it }
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // STEP 4 SUCCESS FINAL SCREEN ANIM
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E824C).copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success Animation Placeholder",
                                        tint = Color(0xFF1E824C),
                                        modifier = Modifier.size(60.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Threat Report Filed!",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Thank you for guarding the community! Your report against $repNumber is cataloged inside ShieldCall's real-time local algorithms, helping other local users instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // C. BOTTOM COMMANDS WIZARD TRIGGER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep < 4) {
                    OutlinedButton(
                        onClick = {
                            if (currentStep > 1) currentStep--
                            else onDone()
                        }
                    ) {
                        Text(if (currentStep == 1) "Cancel" else "Back")
                    }

                    Button(
                        onClick = {
                            if (currentStep == 1) {
                                if (repNumber.isNotBlank()) currentStep = 2
                                else Toast.makeText(context, "Please enter telephone digits first!", Toast.LENGTH_SHORT).show()
                            } else if (currentStep == 2) {
                                if (repCategory.isNotBlank()) currentStep = 3
                                else Toast.makeText(context, "Select category threat first!", Toast.LENGTH_SHORT).show()
                            } else if (currentStep == 3) {
                                // Save inside Room Database!
                                viewModel.addSpamReport(
                                    phoneNumber = repNumber,
                                    category = repCategory,
                                    description = repDescription,
                                    receivedCall = receivedCallToggle
                                )
                                // Autonullify and increment to Success Step
                                currentStep = 4
                            }
                        },
                        modifier = Modifier.testTag("spam_wizard_next_btn")
                    ) {
                        Text(if (currentStep == 3) "Submit Report" else "Continue")
                    }
                } else {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("spam_wizard_done_btn")
                    ) {
                        Text("Return to ShieldHub")
                    }
                }
            }
        }
    }
}

data class ReportCategoryItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
