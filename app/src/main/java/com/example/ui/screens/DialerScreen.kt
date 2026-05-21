package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CallBlockerViewModel

@Composable
fun DialerScreen(
    viewModel: CallBlockerViewModel,
    onBack: () -> Unit,
    onInitiateSimulatedCall: (String) -> Unit // Navigates to Screen 5 Incoming Call active layout
) {
    var typedDigits by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Real-time lookup logic depending on typed digits
    val lookupState = remember(typedDigits) {
        when {
            typedDigits.isBlank() -> "Ready to dial"
            typedDigits.length < 4 -> "Type more for Live Lookup..."
            typedDigits.startsWith("140") || typedDigits.contains("140") -> "🚨 Live Match: SPAM Telemarker (+91 140)"
            typedDigits.startsWith("1800") || typedDigits.contains("800") -> "🚨 Live Match: Robocaller Alert (+1 800)"
            typedDigits.length == 7 -> "🚨 Live Match: FRAUD (Scam likely)"
            typedDigits.startsWith("+99") -> "🚨 Match: High Risk Foreign Suspect"
            else -> "✓ Passed: Whitelist Safe Number"
        }
    }

    // Suggestions shown when digits are empty
    val suggestions = listOf("1400987112", "1800555019", "+991002344", "Mom (987654)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // 1. TOP HEADER WITH NUMBER DISPLAY
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back back key")
                }
                
                Text(
                    text = "DASH DIALER",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = {
                        typedDigits = ""
                    },
                    enabled = typedDigits.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clean input")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Typographical Big Input Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = typedDigits.ifEmpty { "Enter Number" },
                    fontSize = if (typedDigits.length > 10) 28.sp else 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (typedDigits.isEmpty()) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialer_typed_text")
                )

                if (typedDigits.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            typedDigits = typedDigits.dropLast(1)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace back deletion",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. LIVE LOOKUP PREVIEW CHIP
            AssistChip(
                onClick = {},
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (lookupState.contains("Live") || lookupState.contains("Risk")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                label = { Text(lookupState, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = if (lookupState.contains("🚨")) Icons.Default.Warning else Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.testTag("dialer_lookup_chip")
            )
        }

        // Suggestions helper chips
        if (typedDigits.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "QUICK DIAL RECENT SUGGESTS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    modifier = Modifier.padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(suggestions) { cell ->
                        AssistChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                typedDigits = cell.substringAfter("(").substringBefore(")")
                            },
                            label = { Text(cell) }
                        )
                    }
                }
            }
        }

        // 3. DIALPAD 3x4 GRID
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keys = listOf(
                listOf("1" to " ", "2" to "ABC", "3" to "DEF"),
                listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                listOf("*" to "", "0" to "+", "#" to "")
            )

            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    rowKeys.forEach { (num, alpha) ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    typedDigits += num
                                }
                                .testTag("keypad_btn_$num")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = num,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (alpha.isNotEmpty()) {
                                    Text(
                                        text = alpha,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. ACTION CONTROLS BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Outlined secondary video route
            OutlinedIconButton(
                onClick = {
                    Toast.makeText(context, "Initiating Secure HD Voice protocol...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(54.dp)
            ) {
                Icon(imageVector = Icons.Default.VideoCall, contentDescription = "Video screen call link", tint = MaterialTheme.colorScheme.primary)
            }

            // Central Massive Caller action FAB
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E824C)) // green dial
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (typedDigits.isNotBlank()) {
                            onInitiateSimulatedCall(typedDigits)
                        } else {
                            Toast.makeText(context, "Please enter telephone digits first!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .testTag("dialer_call_action_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Simulate active caller screening",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Shortcuts shortcut links
            IconButton(
                onClick = {
                    Toast.makeText(context, "Live call log sync activated!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(54.dp)
            ) {
                Icon(imageVector = Icons.Default.PersonSearch, contentDescription = "Simulate index check", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
