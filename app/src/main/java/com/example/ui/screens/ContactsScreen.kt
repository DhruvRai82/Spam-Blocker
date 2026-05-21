package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CallBlockerViewModel
import com.example.ui.viewmodel.DeviceContact

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    viewModel: CallBlockerViewModel
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: All, 1: Favorites, 2: Blocked

    var searchQuery by remember { mutableStateOf("") }
    var showAddContactSheet by remember { mutableStateOf(false) }

    // State bindings
    val deviceContacts = viewModel.deviceContacts
    val whitelist by viewModel.whitelistedNumbers.collectAsState()
    val blockedPatterns = viewModel.patternsList

    // 1. Filtered and sorted Contacts
    val filteredContacts = remember(deviceContacts, searchQuery) {
        val base = deviceContacts.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
        }
        base.sortedBy { it.name.uppercase() }
    }

    // Alphabetic indices grouping for A-Z layout
    val groupedContacts = remember(filteredContacts) {
        filteredContacts.groupBy { it.name.take(1).uppercase() }
    }

    // A-Z letters
    val alphabet = ('A'..'Z').map { it.toString() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // SORT OVERVIEW SEARCH INPUT HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your contact directories...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contacts_search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.updateContactCount(context)
                        Toast.makeText(context, "Directories reindexed safely!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Contacts Directory",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // NAVIGATION CARDS TAB ROW
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("All", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("all_tab")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("VIP / Favorites", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("favorites_tab")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Pattern Blocks", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("blocked_tab")
                )
            }

            // CONTENT TAB SELECTIVE VIEWPORT
            when (activeTab) {
                0 -> {
                    // ALL DIRECTORY WITH SIDE INDEX BAR
                    if (groupedContacts.isEmpty()) {
                        ContactsPlaceholder(
                            label = "No Contacts Found",
                            info = "Verify you granted read permission. Try clicking the Refresh button above.",
                            onAction = { showAddContactSheet = true }
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Main scrolled LazyColumn
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                groupedContacts.forEach { (alphabetHeader, itemsGroup) ->
                                    stickyHeader {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = alphabetHeader,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    items(itemsGroup) { contact ->
                                        ContactRowItem(
                                            contact = contact,
                                            onToggleFav = {
                                                Toast.makeText(context, "VIP Status modified!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }

                            // Vertically stacked Alphabetic Scrub Index Scroll Bar
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceAround
                            ) {
                                alphabet.forEach { letter ->
                                    Text(
                                        text = letter,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (groupedContacts.containsKey(letter)) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        },
                                        modifier = Modifier.clickable {
                                            Toast.makeText(context, "Indexed to $letter", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // VIP CAROUSEL + ALL REMAINING
                    val favoritesList = remember(deviceContacts) {
                        deviceContacts.filter { it.isFavorite }
                    }

                    if (favoritesList.isEmpty()) {
                        ContactsPlaceholder(
                            label = "No VIP/Starred Contacts",
                            info = "Contacts marked as Starred appear as VIP priority, passing shielding bypasses.",
                            onAction = {}
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Horizontal big horizontal carousel top items
                            Text(
                                text = "VIP BYPASS DIRECTORY",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(favoritesList) { fav ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(76.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = fav.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Rest list items
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(favoritesList) { contact ->
                                    ContactRowItem(contact = contact, onToggleFav = {})
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // BLOCKED PATTERNS LIST
                    if (blockedPatterns.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No Active Patterns", fontWeight = FontWeight.Bold)
                                Text(
                                    "Your pattern blocker list is currently empty. Define wildcards (like +1-800 or +91-140) to silence groups of prefix calls instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(blockedPatterns) { prefix ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Prefix Group Match",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Blocks any call starting with $prefix",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }

                                        AssistChip(
                                            onClick = {
                                                viewModel.removePattern(prefix)
                                                Toast.makeText(context, "$prefix pattern deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text("Delete") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ADD CONTACT FLOATING ACTION
        FloatingActionButton(
            onClick = { showAddContactSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_contact_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Contact")
        }

        // NATIVE SIMULATED BOTTOM SHEET FOR SAVING CONTACT DIRECTORY
        if (showAddContactSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showAddContactSheet = false }
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Register Contact Bypass",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Add contact directly to device directory to allow bypassing calls past active shields.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        var newName by remember { mutableStateOf("") }
                        var newPhone by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_contact_name_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Phone Number digits") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_contact_phone_field")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddContactSheet = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    if (newName.isNotBlank() && newPhone.isNotBlank()) {
                                        val ok = viewModel.addContactToDeviceBook(context, newName, newPhone)
                                        if (ok) {
                                            Toast.makeText(context, "$newName saved to Contacts system!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Mock fallback adding to whitelist directly
                                            viewModel.addNumberToWhitelist(newPhone, newName)
                                            Toast.makeText(context, "$newName Whitelisted bypass!", Toast.LENGTH_SHORT).show()
                                        }
                                        viewModel.updateContactCount(context)
                                        showAddContactSheet = false
                                    } else {
                                        Toast.makeText(context, "Fill in all fields!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_contact_btn")
                            ) {
                                Text("Save Contact")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactRowItem(
    contact: DeviceContact,
    onToggleFav: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // INITIAL ROUND COMPRESSED AVATAR
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // NAME DETAILS
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (contact.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Shield Verified Caller Verified",
                            tint = Color(0xFF1A6CF6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Star VIP Status Indicator
            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Mark Star / VIP status",
                    tint = if (contact.isFavorite) Color(0xFFF39C12) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ContactsPlaceholder(
    label: String,
    info: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                info,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text("Bypass Add Direct")
            }
        }
    }
}
