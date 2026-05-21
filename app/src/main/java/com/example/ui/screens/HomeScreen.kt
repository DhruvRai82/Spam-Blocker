package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CallBlockerViewModel

@Composable
fun HomeScreen(
    viewModel: CallBlockerViewModel,
    onNavigateToTab: (Int) -> Unit, // 0: Home, 1: Recents, 2: Contacts, 3: Me
    onQuickAction: (String) -> Unit // "block", "report", "lookup", "filter"
) {
    val blockedCalls by viewModel.blockedCalls.collectAsState()
    val isEngineOn = viewModel.isServiceEnabled
    val hasContactsPerm = true // visual fallback or permission checks
    val isRoleHeld = viewModel.isMockRoleGranted // leverages mock grant

    // Simple reactive stats derived directly from actual DB entries
    val blockedToday = remember(blockedCalls) {
        blockedCalls.filter { it.wasBlocked }.size
    }
    val spamIdentified = remember(blockedCalls) {
        blockedCalls.filter { it.reason.contains("Spam", ignoreCase = true) || it.reason.contains("Block", ignoreCase = true) }.size
    }
    val safePassed = remember(blockedCalls) {
        blockedCalls.filter { !it.wasBlocked }.size
    }

    var tipDismissed by remember { mutableStateOf(false) }

    // Floating rotate angle for the gradient ring
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldSpin")
    val shieldRotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShieldSpinAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // CONTENT LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. PROTECTION STATUS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("protection_status_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEngineOn) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    }
                )
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
                        Column {
                            Text(
                                text = "SHIELD FORCE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isEngineOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isEngineOn) "Shield Call Active" else "Shield Defeated",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isEngineOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        // Elegant customized M3 Switch
                        Switch(
                            checked = isEngineOn,
                            onCheckedChange = { viewModel.toggleService() },
                            modifier = Modifier.testTag("shield_status_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated Radial Breath Ring Core
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(130.dp)
                    ) {
                        if (isEngineOn) {
                            // Spinning Slow Gradient Arc
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(shieldRotateAngle)
                            ) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFF1A6CF6),
                                            Color(0xFF00C6FF),
                                            Color(0xFF0072FF),
                                            Color(0xFF1A6CF6)
                                        )
                                    ),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx())
                                )
                            }
                        }

                        // Inner solid ball
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isEngineOn) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                        ) {
                            Icon(
                                imageVector = if (isEngineOn) Icons.Default.VerifiedUser else Icons.Default.Shield,
                                contentDescription = "Active status shield",
                                tint = if (isEngineOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEngineOn) {
                            "Defending you from unknown calls & spam robocalls"
                        } else {
                            "Your caller screening is currently turned OFF. Tap switcher to reactivate."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // 2. QUICK STATS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Blocked Today",
                    value = blockedToday.toString(),
                    icon = Icons.Default.Block,
                    tint = MaterialTheme.colorScheme.error
                )
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Spam Detected",
                    value = spamIdentified.toString(),
                    icon = Icons.Default.Warning,
                    tint = Color(0xFFF39C12)
                )
                StatMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Safe Passed",
                    value = safePassed.toString(),
                    icon = Icons.Default.CheckCircle,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 3. RECENT THREAT CATEGORIES ROW
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Recent Threats Handled",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val threats = listOf("Robocall", "Telemarketer", "Scam Likely", "Fraud Alert")
                    items(threats) { category ->
                        AssistChip(
                            onClick = { onQuickAction("filter") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(category) },
                            modifier = Modifier.testTag("threat_chip_$category")
                        )
                    }
                }
            }

            // 4. ROTATING SAFETY TIP CARD
            if (!tipDismissed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Did you know tip icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Blocker Hint",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Block non-contacts by default under settings to enjoy a quiet, spam-free experience.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { tipDismissed = true }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss tip code")
                        }
                    }
                }
            }

            // 5. QUICK ACTIONS GRID 2x2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Command Center",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickButton(
                        modifier = Modifier.weight(1f),
                        label = "Block Number",
                        icon = Icons.Default.Block,
                        onClick = { onQuickAction("block") }
                    )
                    QuickButton(
                        modifier = Modifier.weight(1f),
                        label = "Report Spam",
                        icon = Icons.Default.Warning,
                        onClick = { onQuickAction("report") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickButton(
                        modifier = Modifier.weight(1f),
                        label = "Search Caller",
                        icon = Icons.Default.Search,
                        onClick = { onQuickAction("lookup") }
                    )
                    QuickButton(
                        modifier = Modifier.weight(1f),
                        label = "Block Rules",
                        icon = Icons.Default.Tune,
                        onClick = { onQuickAction("filter") }
                    )
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QuickButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = label, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
