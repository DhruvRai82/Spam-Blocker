package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BlockedCallEntity
import com.example.ui.viewmodel.CallBlockerViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(
    viewModel: CallBlockerViewModel,
    onNavigateToDialer: () -> Unit,
    onInspectCallerId: (String) -> Unit
) {
    val context = LocalContext.current
    val blockedCalls by viewModel.blockedCalls.collectAsState()

    var activeFilter by remember { mutableStateOf("All") } // All, Missed, Spam, Blocked, Unknown
    var searchText by remember { mutableStateOf("") }
    var showSearchRow by remember { mutableStateOf(false) }

    // Filtered logs
    val filteredLogs = remember(blockedCalls, activeFilter, searchText) {
        var baseList = blockedCalls.filter {
            searchText.isBlank() || it.phoneNumber.contains(searchText) || it.reason.contains(searchText, ignoreCase = true)
        }
        when (activeFilter) {
            "Spam" -> baseList.filter { it.wasBlocked || it.reason.contains("Spam", ignoreCase = true) }
            "Blocked" -> baseList.filter { it.wasBlocked }
            "Missed" -> baseList.filter { it.wasBlocked || it.reason.contains("Inactive", ignoreCase = true) } // Mock missed or blocked
            "Unknown" -> baseList.filter { !it.reason.contains("Contact", ignoreCase = true) }
            else -> baseList
        }
    }

    // Today / Yesterday sticky date categorization
    val categorized = remember(filteredLogs) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val yesterdayStr = dateFormat.format(Date(System.currentTimeMillis() - 24 * 3600 * 1000))

        filteredLogs.groupBy { log ->
            val logDateStr = dateFormat.format(Date(log.timestamp))
            when (logDateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> "This Week"
            }
        }.toSortedMap(compareBy {
            when (it) {
                "Today" -> 1
                "Yesterday" -> 2
                else -> 3
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // TOP CONTROLS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Screening Archives",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(onClick = { showSearchRow = !showSearchRow }) {
                    Icon(
                        imageVector = if (showSearchRow) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search search tool",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // EXPANSIBLE SEARCH PILL
            AnimatedVisibility(
                visible = showSearchRow,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search logs number...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                )
            }

            // FILTER SCROLL ROW
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterOptions = listOf("All", "Missed", "Spam", "Blocked", "Unknown")
                items(filterOptions) { filter ->
                    val isSelected = activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }

            // LIST LAYER
            if (categorized.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Screening Archives",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No call logs match this filter categories yet. Run some caller diagnostic simulations to log data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    categorized.forEach { (headerStr, itemsList) ->
                        // STICKY TIME HEADER
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = headerStr.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // INDIVIDUAL ITEMS
                        items(itemsList) { log ->
                            SwipeableCallLogItem(
                                log = log,
                                onInspect = { onInspectCallerId(log.phoneNumber) },
                                onDelete = { viewModel.deleteLog(log.id) },
                                onBlock = {
                                    viewModel.addPattern(log.phoneNumber)
                                    Toast.makeText(context, "${log.phoneNumber} added to template patterns blocker!", Toast.LENGTH_SHORT).show()
                                },
                                onSafe = {
                                    viewModel.addNumberToWhitelist(log.phoneNumber, "Whitelisted Subscriber")
                                    Toast.makeText(context, "${log.phoneNumber} designated as safe bypass!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // MORPHING DIALER FAB
        FloatingActionButton(
            onClick = onNavigateToDialer,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dialer_fab")
        ) {
            Icon(imageVector = Icons.Default.Dialpad, contentDescription = "Dialpad opener")
        }
    }
}

@Composable
fun SwipeableCallLogItem(
    log: BlockedCallEntity,
    onInspect: () -> Unit,
    onDelete: () -> Unit,
    onBlock: () -> Unit,
    onSafe: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "SwipeBackAnim")
    var contextMenuOpen by remember { mutableStateOf(false) }

    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeLabel = formatter.format(Date(log.timestamp))

    // Determine visual colors
    val isBlocked = log.wasBlocked
    val isSpam = log.reason.contains("Spam", ignoreCase = true) || log.reason.contains("Threat", ignoreCase = true)
    
    val avatarTint = when {
        isBlocked -> Color(0xFFDF1C24)          // Deep red for strict blocks
        isSpam -> Color(0xFFE67E22)             // Orange for suspected threats
        log.reason.contains("Whitelist") -> Color(0xFF1E824C) // Forest green for safe bypass
        else -> MaterialTheme.colorScheme.outline
    }

    val itemBgColor = when {
        isBlocked -> Color(0xFFDF1C24).copy(alpha = 0.04f)
        isSpam -> Color(0xFFE67E22).copy(alpha = 0.04f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-180f, 180f)
                    },
                    onDragEnd = {
                        if (offsetX < -120f) {
                            onBlock()
                        } else if (offsetX > 120f) {
                            onSafe()
                        }
                        offsetX = 0f
                    }
                )
            }
            .background(
                when {
                    offsetX > 55 -> Color(0xFF1E824C).copy(alpha = 0.15f) // Green on swipe-right (mark safe)
                    offsetX < -55 -> Color(0xFFDF1C24).copy(alpha = 0.15f) // Red on swipe-left (block)
                    else -> MaterialTheme.colorScheme.background
                }
            )
    ) {
        // SWIPE REVEAL BACKGROUND ACTIONS
        if (offsetX > 20) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF1E824C))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Whitelisted", color = Color(0xFF1E824C), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else if (offsetX < -20) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Strict Block", color = Color(0xFFDF1C24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = Color(0xFFDF1C24))
            }
        }

        // MAIN FOREGROUND CARD (SLIDABLE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .clickable { onInspect() },
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = itemBgColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AVATAR HALO
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(avatarTint.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(width = 1.5.dp, color = avatarTint, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBlocked) "!" else log.phoneNumber.takeLast(2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = avatarTint
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // MIDDLE DETAIL
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isBlocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = Color(0xFFDF1C24)) {
                                Text("BLOCKED", fontSize = 8.sp, color = Color.White)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = log.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBlocked) Color(0xFFDF1C24) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // RIGHT ACTION / TIMESTAMP
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    
                    Box {
                        IconButton(onClick = { contextMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Item menu options dropdown",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = contextMenuOpen,
                            onDismissRequest = { contextMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Lookup Caller ID") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    contextMenuOpen = false
                                    onInspect()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block Number Pattern") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    contextMenuOpen = false
                                    onBlock()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Designate As Safe") },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = {
                                    contextMenuOpen = false
                                    onSafe()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete This Log Entry") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    contextMenuOpen = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
