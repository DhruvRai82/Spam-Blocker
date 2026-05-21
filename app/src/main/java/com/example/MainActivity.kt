package com.example

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.database.BlockedCallEntity
import com.example.data.database.WhitelistedNumberEntity
import com.example.data.repository.CallBlockerRepository
import com.example.ui.theme.ColorAllowed
import com.example.ui.theme.ColorBlocked
import com.example.ui.theme.ColorNeutral
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CallBlockerViewModel
import com.example.ui.viewmodel.CallBlockerViewModelFactory
import com.example.ui.viewmodel.DeviceContact
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CallBlockerRepository(applicationContext, database.blockedCallDao())
        val factory = CallBlockerViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: CallBlockerViewModel = viewModel(factory = factory)
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.updateContactCount(context)
                }

                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: CallBlockerViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Shield, 1: Contacts, 2: Dialer, 3: Settings
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddWhitelistDialog by remember { mutableStateOf(false) }

    // Observers
    val blockedCalls by viewModel.blockedCalls.collectAsState()
    val whitelistedNumbers by viewModel.whitelistedNumbers.collectAsState()

    // Permissions check
    var hasReadContactsPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasWriteContactsPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCallScreeningRoleHeld by remember {
        mutableStateOf(isCallScreeningRoleHeld(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permMap ->
        hasReadContactsPermission = permMap[android.Manifest.permission.READ_CONTACTS] == true
        hasWriteContactsPermission = permMap[android.Manifest.permission.WRITE_CONTACTS] == true
        viewModel.updateContactCount(context)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isCallScreeningRoleHeld = isCallScreeningRoleHeld(context)
    }

    LaunchedEffect(hasReadContactsPermission) {
        if (hasReadContactsPermission) {
            viewModel.updateContactCount(context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Screen Shield",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Smart Call Guard Edition",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.updateContactCount(context)
                        isCallScreeningRoleHeld = isCallScreeningRoleHeld(context)
                        Toast.makeText(context, "Shield verified", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = "Refresh shield metrics"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield active center"
                        )
                    },
                    label = { Text("Shield Armor") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "Contact directory"
                        )
                    },
                    label = { Text("Contacts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Sleek keyboard"
                        )
                    },
                    label = { Text("Dialer") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Block tuning"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 1 && hasReadContactsPermission) {
                FloatingActionButton(
                    onClick = { showAddContactDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_contact_fab")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Contact")
                }
            } else if (activeTab == 3) {
                FloatingActionButton(
                    onClick = { showAddWhitelistDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("add_whitelist_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Whitelist Bypass")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "NavigationAnimation"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> ShieldHubScreen(
                        viewModel = viewModel,
                        isRoleHeld = isCallScreeningRoleHeld,
                        hasContactsPerm = hasReadContactsPermission,
                        onRequestRole = {
                            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                roleLauncher.launch(intent)
                            } else {
                                Toast.makeText(context, "System screening role not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRequestPerms = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CONTACTS,
                                    android.Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        }
                    )
                    1 -> ContactsDirectoryScreen(
                        viewModel = viewModel,
                        hasPerm = hasReadContactsPermission,
                        onRequestPerms = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CONTACTS,
                                    android.Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        }
                    )
                    2 -> KeypadDialerScreen()
                    3 -> SettingsScreen(
                        viewModel = viewModel,
                        whitelistedNumbers = whitelistedNumbers,
                        blockedLogs = blockedCalls,
                        onDeleteWhitelist = { viewModel.removeNumberFromWhitelist(it) },
                        onClearLogs = { viewModel.clearLogs() },
                        onDeleteLog = { viewModel.deleteLog(it) }
                    )
                }
            }
        }
    }

    if (showAddContactDialog) {
        CreateContactDialog(
            onDismiss = { showAddContactDialog = false },
            onSave = { name, phone ->
                val succeed = viewModel.addContactToDeviceBook(context, name, phone)
                if (succeed) {
                    Toast.makeText(context, "Contact created successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Permission or database failure", Toast.LENGTH_LONG).show()
                }
                showAddContactDialog = false
            }
        )
    }

    if (showAddWhitelistDialog) {
        AddWhitelistDialog(
            onDismiss = { showAddWhitelistDialog = false },
            onAdd = { phone, name ->
                viewModel.addNumberToWhitelist(phone, name)
                Toast.makeText(context, "Bypass added for $phone", Toast.LENGTH_SHORT).show()
                showAddWhitelistDialog = false
            }
        )
    }
}

@Composable
fun ShieldHubScreen(
    viewModel: CallBlockerViewModel,
    isRoleHeld: Boolean,
    hasContactsPerm: Boolean,
    onRequestRole: () -> Unit,
    onRequestPerms: () -> Unit
) {
    val context = LocalContext.current
    val protectedCount = viewModel.deviceContactCount
    val isEngineOn = viewModel.isServiceEnabled
    val isBlockActive = viewModel.isBlockNonContactsEnabled

    val isGuarded = isEngineOn && isBlockActive && isRoleHeld && hasContactsPerm

    // Diagnostics simulator section states
    var isDiagnosticOpen by remember { mutableStateOf(false) }
    var testNumber by remember { mutableStateOf("") }

    // Pulsing animations for Radar Circle
    val infiniteTransition = rememberInfiniteTransition(label = "RadarBreath")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadiusScaling"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepSpin"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RADAR ACTIVE ZONE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SHIELD SYSTEM HEALTH",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = if (isGuarded) ColorAllowed else MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated Animated Google Radar Core
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        // Ambient Breathing Wave
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val activeColor = if (isGuarded) ColorAllowed else ColorBlocked
                            drawCircle(
                                color = activeColor.copy(alpha = 0.1f),
                                radius = size.minDimension / 2f * pulseScale
                            )
                            drawCircle(
                                color = activeColor.copy(alpha = 0.15f),
                                radius = size.minDimension / 2.6f
                            )
                            // Outer spinning sweep line
                            drawArc(
                                color = activeColor.copy(alpha = 0.3f),
                                startAngle = sweepAngle,
                                sweepAngle = 45f,
                                useCenter = true,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Circular Solid Shield Orb
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (isGuarded) {
                                            listOf(ColorAllowed, ColorAllowed.copy(alpha = 0.7f))
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            )
                                        }
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = if (isGuarded) Icons.Default.VerifiedUser else Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isGuarded) "SYSTEM FULLY SECURED" else "ACTION REQUIRED",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isGuarded) ColorAllowed else ColorBlocked
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isGuarded) {
                            "Call Screening is continuously validating incoming callers and automatically rejecting spam threats."
                        } else {
                            "Please complete the configuration settings below so the core Spam Blocker can screen unknown numbers."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // TELEMETRY & CONTROLS GAUGE
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Protected Contacts
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$protectedCount Contacts",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Shield Whitelist",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Overall Blocker Status
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = if (isEngineOn) ColorAllowed else ColorNeutral,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEngineOn) "Shield On" else "Shield Off",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isBlockActive) "Auto-Block Unknown" else "Allow Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // INTEGRITY & PERMISSION ACTIONS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Core Setup checklist",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 1. Android Assistant Default app Role
                    ListItem(
                        headlineContent = { Text("Default screening Application", fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                if (isRoleHeld) "Standard assistant permissions configured"
                                else "Mandatory setup option to validate call data"
                            )
                        },
                        leadingContent = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isRoleHeld) ColorAllowed.copy(alpha = 0.15f) else ColorBlocked.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = if (isRoleHeld) Icons.Default.Check else Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = if (isRoleHeld) ColorAllowed else ColorBlocked,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingContent = {
                            if (!isRoleHeld) {
                                Button(
                                    onClick = onRequestRole,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Grant", fontSize = 12.sp)
                                }
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // 2. Contacts Read & Write Permissions
                    ListItem(
                        headlineContent = { Text("Interactive Contacts Permissions", fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                if (hasContactsPerm) "Complete read/write setup active"
                                else "Permit app directory searching list"
                            )
                        },
                        leadingContent = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (hasContactsPerm) ColorAllowed.copy(alpha = 0.15f) else ColorBlocked.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = if (hasContactsPerm) Icons.Default.Check else Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = if (hasContactsPerm) ColorAllowed else ColorBlocked,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingContent = {
                            if (!hasContactsPerm) {
                                Button(
                                    onClick = onRequestPerms,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Grant", fontSize = 12.sp)
                                }
                            }
                        }
                    )
                }
            }
        }

        // DIAGNOSTIC TESTING EXPANSION CONTROLS (TEST SIMULATION PANEL)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDiagnosticOpen = !isDiagnosticOpen },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Handyman,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Developer Diagnostics Lab",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Simulate incoming call screening profiles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { isDiagnosticOpen = !isDiagnosticOpen }) {
                            Icon(
                                imageVector = if (isDiagnosticOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Section"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isDiagnosticOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Screening Engine Call Simulator",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Simulate how the background service operates under active incoming call system requests. Results appear in Settings -> Log archives.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = testNumber,
                                onValueChange = { testNumber = it },
                                label = { Text("Incoming call Phone Number") },
                                placeholder = { Text("+1 (555) 762-1100") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("simulate_phone_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (testNumber.isNotBlank()) {
                                        viewModel.simulateCall(testNumber, context)
                                        Toast.makeText(context, "Call routing simulated for $testNumber", Toast.LENGTH_SHORT).show()
                                        testNumber = ""
                                    } else {
                                        Toast.makeText(context, "Input code missing", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("simulate_call_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PhonelinkRing, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Screen Evaluation")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsDirectoryScreen(
    viewModel: CallBlockerViewModel,
    hasPerm: Boolean,
    onRequestPerms: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedContactForDetail by remember { mutableStateOf<DeviceContact?>(null) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    if (!hasPerm) {
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
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Unlock Contact Protection",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Permit access to find, displays and screen real phone contacts on this app dashboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPerms,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant system Contacts rights")
                }
            }
        }
    } else {
        val filteredContacts = remember(searchQuery, viewModel.deviceContacts) {
            if (searchQuery.isBlank()) {
                viewModel.deviceContacts
            } else {
                viewModel.deviceContacts.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.phoneNumber.contains(searchQuery)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Google Search Pill Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search your phone directory...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contact_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        )
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }
            }

            if (filteredContacts.isEmpty()) {
                EmptyCollectionPlaceholder(
                    icon = Icons.Default.ManageAccounts,
                    title = if (searchQuery.isEmpty()) "Directory Is Purely Empty" else "No Directory Matches",
                    subtitle = if (searchQuery.isEmpty()) "Add your first real caller using the Plus float toggle below." else "Try adjusting search digits."
                )
            } else {
                // Alphabetically Grouped Contacts List
                val grouped = remember(filteredContacts) {
                    filteredContacts.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '?' }
                        .toSortedMap()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (charKey, contactSublist) ->
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = charKey.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(contactSublist) { contact ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedContactForDetail = contact },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Contact Avatar
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                    )
                                                )
                                            )
                                    ) {
                                        Text(
                                            text = contact.name.take(1).uppercase(Locale.getDefault()),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = contact.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = {
                                        viewModel.addNumberToWhitelist(contact.phoneNumber, contact.name)
                                        Toast.makeText(context, "Bypassed Whitelisted: ${contact.name}", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Quick whitelist addition",
                                            tint = ColorAllowed.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Google Profile card Detail Dialog
    selectedContactForDetail?.let { contact ->
        ContactProfileDialog(
            contact = contact,
            onDismiss = { selectedContactForDetail = null },
            onCall = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                context.startActivity(intent)
                selectedContactForDetail = null
            },
            onDelete = {
                val succeed = viewModel.deleteContactFromDeviceBook(context, contact.phoneNumber)
                if (succeed) {
                    Toast.makeText(context, "Deleted Contact: ${contact.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Delete contact failed", Toast.LENGTH_SHORT).show()
                }
                selectedContactForDetail = null
            }
        )
    }
}

@Composable
fun EmptyCollectionPlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun ContactProfileDialog(
    contact: DeviceContact,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mega Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // DIAL Action inside dialog
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ColorAllowed.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Initiate Voice Call",
                            tint = ColorAllowed
                        )
                    }

                    // Delete Contact completely
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ColorBlocked.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = "Delete contact permanently",
                            tint = ColorBlocked
                        )
                    }

                    // Cancel / Close profile details
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadDialerScreen() {
    val context = LocalContext.current
    var inputDigits by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Dialer Display Number View
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = inputDigits,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "DigitsAnimation"
            ) { digits ->
                Text(
                    text = digits.ifEmpty { "Enter Number" },
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (digits.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (inputDigits.isNotBlank()) {
                Text(
                    text = "Tap green floating dial to call via system handler",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
            }
        }

        // T9 Dial Pad Keyboard Grid
        val buttonsList = listOf(
            Pair("1", ""), Pair("2", "A B C"), Pair("3", "D E F"),
            Pair("4", "G H I"), Pair("5", "J K L"), Pair("6", "M N O"),
            Pair("7", "P Q R S"), Pair("8", "T U V"), Pair("9", "W X Y Z"),
            Pair("*", ""), Pair("0", "+"), Pair("#", "")
        )

        Column(
            modifier = Modifier.wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (i in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (j in 0 until 3) {
                        val index = i * 3 + j
                        val digitAndLetters = buttonsList[index]
                        DialerButton(
                            digit = digitAndLetters.first,
                            letters = digitAndLetters.second,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                inputDigits += digitAndLetters.first
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action floating row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacing or speed dial
                IconButton(onClick = { /* Space reserved */ }, modifier = Modifier.size(56.dp)) {
                    Icon(imageVector = Icons.Default.StarBorder, contentDescription = "Speed dial star", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }

                // Call Action Green key
                IconButton(
                    onClick = {
                        if (inputDigits.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$inputDigits"))
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Number display empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ColorAllowed)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Dial Phone call", tint = Color.White)
                }

                // Delete Backspace action
                IconButton(
                    onClick = {
                        if (inputDigits.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            inputDigits = inputDigits.dropLast(1)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Icon(imageVector = Icons.Default.Backspace, contentDescription = "Backspace typing delete", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun DialerButton(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: CallBlockerViewModel,
    whitelistedNumbers: List<WhitelistedNumberEntity>,
    blockedLogs: List<BlockedCallEntity>,
    onDeleteWhitelist: (String) -> Unit,
    onClearLogs: () -> Unit,
    onDeleteLog: (Int) -> Unit
) {
    var isAIFilterEnabled by remember { mutableStateOf(true) }
    var assistantVoiceIndex by remember { mutableStateOf(0) }
    val assistantVoices = listOf("Voice Male Standard", "Voice Female Organic", "Synthetic Guard")

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MASTER TOGGLES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Protection Master Switches",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Switch 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Shield Protection",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Intercept and evaluate call data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewModel.isServiceEnabled,
                            onCheckedChange = { viewModel.toggleService() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = ColorAllowed
                            ),
                            modifier = Modifier.scale(0.85f).testTag("service_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block Unknown Non-Contacts",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Block anyone who is not in contact book",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewModel.isBlockNonContactsEnabled,
                            onCheckedChange = { viewModel.toggleBlockNonContacts() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = ColorBlocked
                            ),
                            modifier = Modifier.scale(0.85f).testTag("block_unknown_toggle_switch")
                        )
                    }
                }
            }
        }

        // GOOGLE SCREENING ASSISTANT
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Voice Assistant Screening",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Community Spam Prefix Auto Filter",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Perform real-time matching against known spam logs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAIFilterEnabled,
                            onCheckedChange = { isAIFilterEnabled = it },
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Assistant Voice Pronunciation",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose audio tone used to announce incoming block screenings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        assistantVoices.forEachIndexed { index, voice ->
                            ElevatedCard(
                                onClick = { assistantVoiceIndex = index },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (assistantVoiceIndex == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = voice,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // WHITELIST CONFIGS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Exempt numbers whitelist (${whitelistedNumbers.size})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Numbers listed below bypass protection shields even if they aren't in your main phone contacts sync directories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (whitelistedNumbers.isEmpty()) {
                        Text(
                            text = "No custom whitelist bypass exceptions defined yet.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            whitelistedNumbers.forEach { whitelisted ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(whitelisted.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(whitelisted.phoneNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = { onDeleteWhitelist(whitelisted.phoneNumber) },
                                        modifier = Modifier.testTag("delete_whitelist_${whitelisted.phoneNumber}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete bypass", tint = ColorBlocked)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // HISTORY LISTINGS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historical Activity (${blockedLogs.size})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (blockedLogs.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onClearLogs,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear all", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (blockedLogs.isEmpty()) {
                        Text(
                            text = "Shield historical log vault is empty. No blocks recorded.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            blockedLogs.forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colored vertical indicator
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (log.wasBlocked) ColorBlocked else ColorAllowed)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(log.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = if (log.wasBlocked) "BLOCKED" else "ALLOWED",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (log.wasBlocked) ColorBlocked else ColorAllowed
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(log.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = dateFormat.format(Date(log.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onDeleteLog(log.id) },
                                        modifier = Modifier.size(32.dp).testTag("delete_log_${log.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContactDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNo by remember { mutableStateOf("") }

    val isValid = phoneNo.isNotBlank() && firstName.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Device Contact",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Creates a real phonebook entry inside the device system storage repository.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNo,
                    onValueChange = { phoneNo = it },
                    label = { Text("Phone Number *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val fullname = if (lastName.isBlank()) firstName else "$firstName $lastName"
                            onSave(fullname, phoneNo)
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("add_contact_save_button")
                    ) {
                        Text("Save Contact")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWhitelistDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Shield Overrule Bypass",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Bypass block filters for specific business or courier numbers without creating contacts entries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Exception Name / Description") },
                    placeholder = { Text("e.g. Courier Prime") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 (555) 545-0900") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (phoneInput.isNotBlank()) {
                                onAdd(phoneInput, nameInput)
                            }
                        },
                        modifier = Modifier.testTag("dialog_save_button"),
                        enabled = phoneInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Authorize")
                    }
                }
            }
        }
    }
}

fun isCallScreeningRoleHeld(context: Context): Boolean {
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
    return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
}
