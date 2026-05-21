package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CallBlockerViewModel
import kotlin.math.roundToInt

@Composable
fun IncomingCallScreen(
    viewModel: CallBlockerViewModel,
    phoneNumber: String,
    onAcceptCall: () -> Unit,
    onDeclineCall: () -> Unit
) {
    val context = LocalContext.current

    // Evaluate spam classification depending on phone rules
    val classification = remember(phoneNumber) {
        when {
            phoneNumber.startsWith("140") || phoneNumber.contains("140") -> "TELEMARKETER"
            phoneNumber.startsWith("1800") || phoneNumber.contains("800") -> "ROBOCALL PREVENTED"
            phoneNumber.length == 7 -> "SPAM LIKELY"
            phoneNumber.startsWith("+99") -> "HIGH FRAUD ALERT"
            else -> "VERIFIED SAFE CALL"
        }
    }

    val riskColor = when (classification) {
        "TELEMARKETER" -> Color(0xFFE67E22)       // Orange
        "ROBOCALL PREVENTED" -> Color(0xFFDF1C24) // Crimson Red
        "SPAM LIKELY" -> Color(0xFFDF1C24)
        "HIGH FRAUD ALERT" -> Color(0xFFDF1C24)
        else -> Color(0xFF1E824C)                 // Forest green for verified
    }

    val sourceLabel = when (classification) {
        "TELEMARKETER" -> "Reported 2,420 times for credit telemarketing"
        "ROBOCALL PREVENTED" -> "Flagged as an automated voicemail auto-dialer"
        "SPAM LIKELY" -> "Detected in regional robocall community database"
        "HIGH FRAUD ALERT" -> "Suspicious international spoof address match"
        else -> "In your dynamic verified business whitelist"
    }

    val confidencePercent = when (classification) {
        "TELEMARKETER" -> 0.85f
        "ROBOCALL PREVENTED" -> 0.98f
        "SPAM LIKELY" -> 0.70f
        "HIGH FRAUD ALERT" -> 0.95f
        else -> 0.02f
    }

    // Breathing pulse halo animation
    val infiniteTransition = rememberInfiniteTransition(label = "PulseHalo")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Night deep slate
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP SIMULATED SYSTEM STATUS BAR AREA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = riskColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ShieldCall Active Screen", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
            }
        }

        // 2. RISK LEVEL SPAM PILL BADGE
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(riskColor.copy(alpha = 0.15f))
                .border(width = 1.5.dp, color = riskColor, shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = classification,
                color = riskColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        // 3. CALLER PROFILE WITH HALO RING
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Breathing Halo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scalePulse)
                        .clip(CircleShape)
                        .background(riskColor.copy(alpha = 0.08f))
                        .border(1.dp, riskColor.copy(alpha = 0.3f), CircleShape)
                )

                // Outer boundary ring
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .border(2.dp, riskColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (classification.contains("SAFE")) "✓" else "!",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = riskColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text info
            Text(
                text = if (classification.contains("SAFE")) "Verified Business" else "Unknown Suspicious",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = phoneNumber,
                fontSize = 18.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(vertical = 4.dp).testTag("incoming_caller_number")
            )

            Text(
                text = sourceLabel,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // 4. AI CROWD METRICS INSIGHTS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI intelligence indicator",
                        tint = riskColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ShieldCall AI Analytics",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (confidencePercent > 0.5f) {
                                "Crowdsourced logs indicate aggressive robocalling sweep activity in your area. Silence is highly recommended."
                            } else {
                                "This caller is recognized as verified commercial utility. ShieldCall suggests safe pass."
                            },
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confidence Slider Track
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Threat Confidence", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("${(confidencePercent * 100).roundToInt()}% Score", fontSize = 10.sp, color = riskColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = confidencePercent,
                        color = riskColor,
                        trackColor = Color(0xFF334155),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // 5. SLIDER ACTIONS PILLS ROW (DECLINE / SILENCE / ANSWER)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Silence outline shortcut
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Snoozing ringer and silencing alert...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.VolumeMute, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("MUTE ALERTS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DECLINE ACTION BUTTON
                Row(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDF1C24))
                        .clickable {
                            viewModel.simulateCall(phoneNumber, context) // Log into DB!
                            onDeclineCall()
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Decline decline call", tint = Color.White)
                }

                // SUB ACTIONS IN MIDDLE WHILST CALL INCOMING
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            viewModel.addPattern(phoneNumber)
                            viewModel.simulateCall(phoneNumber, context)
                            onDeclineCall()
                            Toast.makeText(context, "Call blocked and banned prefix!", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("Block & Decline", color = Color.White, fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B))
                    )
                }

                // ANSWER ACTION BUTTON
                Row(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E824C))
                        .clickable {
                            viewModel.simulateCall(phoneNumber, context) // Log into DB!
                            onAcceptCall()
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Answer accept call", tint = Color.White)
                }
            }
        }
    }
}
