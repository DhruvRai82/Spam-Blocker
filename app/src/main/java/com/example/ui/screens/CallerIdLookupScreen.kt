package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun CallerIdLookupScreen(
    viewModel: CallBlockerViewModel,
    phoneNumber: String,
    onBack: () -> Unit,
    onNavigateToReport: (String) -> Unit
) {
    val context = LocalContext.current

    // Set properties relative to the phone digits
    val profile = remember(phoneNumber) {
        val clean = phoneNumber.trim()
        when {
            clean.startsWith("140") || clean.contains("140") -> {
                CallerProfileMock(
                    name = "Vanguard Sales Ltd",
                    number = clean,
                    riskLabel = "TELEMARKETER",
                    riskColor = Color(0xFFE67E22),
                    origination = "🇺🇸 Austin, TX",
                    totalReports = 245,
                    firstSeen = "May 02, 2026",
                    lastSeen = "2 hours ago",
                    breakdown = listOf("Auto-Telemarketing" to 0.65f, "Robocalls" to 0.25f, "Other Calls" to 0.10f),
                    reviews = listOf(
                        CommunityReview("Agresively asking to apply credit score cards.", "2 hours ago", "Robocall"),
                        CommunityReview("Calls repeatedly, hangs up immediately upon pick.", "Yesterday", "Silent Call"),
                        CommunityReview("Automated financial survey machine.", "2 days ago", "Telemarketer")
                    )
                )
            }
            clean.startsWith("1800") || clean.contains("800") -> {
                CallerProfileMock(
                    name = "Unified survey bots",
                    number = clean,
                    riskLabel = "ROBOCALL PREVENTED",
                    riskColor = Color(0xFFDF1C24),
                    origination = "🇺🇸 Chicago, IL",
                    totalReports = 1380,
                    firstSeen = "Jan 12, 2026",
                    lastSeen = "15 minutes ago",
                    breakdown = listOf("Robocalls" to 0.80f, "Silent Hangup" to 0.15f, "Fraud Alert" to 0.05f),
                    reviews = listOf(
                        CommunityReview("Taped auto voice offering medical sweeps.", "15 mins ago", "Robocall"),
                        CommunityReview("Annoying bot dialer sweep tracker.", "3 hours ago", "Robocall"),
                        CommunityReview("Spam likely recorded agent survey.", "3 days ago", "Scam Likely")
                    )
                )
            }
            clean.startsWith("+99") -> {
                CallerProfileMock(
                    name = "Foreign IRS Fake agent",
                    number = clean,
                    riskLabel = "HIGH FRAUD ALERT",
                    riskColor = Color(0xFFDF1C24),
                    origination = "🌐 Off-Shore Spoof",
                    totalReports = 780,
                    firstSeen = "Mar 11, 2026",
                    lastSeen = "5 hours ago",
                    breakdown = listOf("IRS Scam" to 0.75f, "Fraud Sweep" to 0.20f, "Other" to 0.05f),
                    reviews = listOf(
                        CommunityReview("Threatened lawsuit unless pay in crypto cards.", "5 hours ago", "Fraud"),
                        CommunityReview("Typical IRS offshore tax robocall scam.", "Yesterday", "Scam Likely")
                    )
                )
            }
            else -> {
                CallerProfileMock(
                    name = "Verified Business Contact",
                    number = clean,
                    riskLabel = "VERIFIED SAFE",
                    riskColor = Color(0xFF1E824C),
                    origination = "🇺🇸 San Francisco, CA",
                    totalReports = 0,
                    firstSeen = "N/A",
                    lastSeen = "1 day ago",
                    breakdown = listOf("Legitimate" to 0.98f, "Other" to 0.02f),
                    reviews = emptyList()
                )
            }
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Caller Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back back arrow")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            
            // 1. HERO HEADER AREA
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(profile.riskColor.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = if (profile.riskLabel.contains("SAFE")) Icons.Default.VerifiedUser else Icons.Default.Warning,
                            contentDescription = null,
                            tint = profile.riskColor,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = profile.number,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("caller_details_number")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = profile.origination,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 2. THREAT STATS SUMMARY CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Security Verdict", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            
                            Badge(containerColor = profile.riskColor) {
                                Text(profile.riskLabel, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(4.dp))
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("CROWD REPORTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${profile.totalReports} reports", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LAST ALERTED SEEN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(profile.lastSeen, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // 3. REPORT PERCENTAGE BREAKDOWN HORIZONTAL BAR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Threat Category Breakdown",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Distribution metrics based on regional crowd reports.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stacked Horizontal Percentage Slider Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            // Assign sequential bar colors
                            val barColors = listOf(Color(0xFFDF1C24), Color(0xFFE67E22), Color(0xFFF1C40F))
                            profile.breakdown.forEachIndexed { idx, (category, weight) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(weight.coerceAtLeast(0.01f))
                                        .background(barColors.getOrElse(idx) { MaterialTheme.colorScheme.outline })
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Legends column
                        profile.breakdown.forEachIndexed { idx, (category, weight) ->
                            val color = listOf(Color(0xFFDF1C24), Color(0xFFE67E22), Color(0xFFF1C40F)).getOrElse(idx) { MaterialTheme.colorScheme.outline }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("${(weight * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 4. HISTORIC USER FEEDBACK ITEMS SECTION
            item {
                Text(
                    text = "Recent Community Logs (${profile.reviews.size})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (profile.reviews.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "No user reviews posted yet for this number.",
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(profile.reviews) { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(profile.riskColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(review.category, color = profile.riskColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                                Text(review.timeLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"${review.comment}\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. INNER COMMAND TRIGGER ROW ACTIONS
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addPattern(phoneNumber)
                            Toast.makeText(context, "Number banned from screening!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("details_block_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Strict Block This Number")
                    }

                    OutlinedButton(
                        onClick = { onNavigateToReport(phoneNumber) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Spam Report Wizard")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.addNumberToWhitelist(phoneNumber, "Whitelisted Detail Pass")
                            Toast.makeText(context, "Added caller bypass exception!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Safe (Whitelist Bypass)")
                    }
                }
            }
        }
    }
}

data class CallerProfileMock(
    val name: String,
    val number: String,
    val riskLabel: String,
    val riskColor: Color,
    val origination: String,
    val totalReports: Int,
    val firstSeen: String,
    val lastSeen: String,
    val breakdown: List<Pair<String, Float>>,
    val reviews: List<CommunityReview>
)

data class CommunityReview(
    val comment: String,
    val timeLabel: String,
    val category: String
)
