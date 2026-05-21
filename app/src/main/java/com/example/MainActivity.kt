package com.example

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.CallBlockerRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CallBlockerViewModel
import com.example.ui.viewmodel.CallBlockerViewModelFactory
import com.example.ui.screens.*

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

                ShieldCallApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldCallApp(viewModel: CallBlockerViewModel) {
    val context = LocalContext.current

    // ROUTING STATES
    var activeTab by remember { mutableStateOf(0) } // 0: Home, 1: Recents, 2: Contacts, 3: Me/Settings
    
    // OVERLAY VIEWS STATES
    var activeIncomingCallNumber by remember { mutableStateOf<String?>(null) }
    var inspectedNumberDetails by remember { mutableStateOf<String?>(null) }
    var reportSpamNumber by remember { mutableStateOf<String?>(null) }
    
    var isDialerVisible by remember { mutableStateOf(false) }
    var isBlockRulesVisible by remember { mutableStateOf(false) }
    var isInsightsVisible by remember { mutableStateOf(false) }

    // Simulation triggers
    var showSimulatorToolbar by remember { mutableStateOf(false) }

    // Standard Android Role State
    var isScreeningRoleHeld by remember {
        mutableStateOf(isCallScreeningRoleHeld(context))
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isScreeningRoleHeld = isCallScreeningRoleHeld(context)
        Toast.makeText(context, "System Role check updated!", Toast.LENGTH_SHORT).show()
    }

    // Back Gesture Interceptor for predictive dismissal
    BackHandler(
        enabled = activeIncomingCallNumber != null || inspectedNumberDetails != null || reportSpamNumber != null || isDialerVisible || isBlockRulesVisible || isInsightsVisible
    ) {
        when {
            activeIncomingCallNumber != null -> activeIncomingCallNumber = null
            inspectedNumberDetails != null -> inspectedNumberDetails = null
            reportSpamNumber != null -> reportSpamNumber = null
            isDialerVisible -> isDialerVisible = false
            isBlockRulesVisible -> isBlockRulesVisible = false
            isInsightsVisible -> isInsightsVisible = false
        }
    }

    // MAIN SCAFFOLD COMPONENT
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
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "ShieldCall OS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Material You Screening Edition",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Quick stats toggle
                    IconButton(onClick = { isInsightsVisible = true }) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = "Query analytics dashboard")
                    }

                    // Simulated trigger panel shortcut
                    IconButton(onClick = { showSimulatorToolbar = !showSimulatorToolbar }) {
                        Icon(
                            imageVector = if (showSimulatorToolbar) Icons.Default.BugReport else Icons.Default.PhoneCallback,
                            contentDescription = "Simulate mock caller",
                            tint = if (showSimulatorToolbar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0 && !isDialerVisible && !isBlockRulesVisible && !isInsightsVisible,
                    onClick = {
                        activeTab = 0
                        isDialerVisible = false
                        isBlockRulesVisible = false
                        isInsightsVisible = false
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home tab button") },
                    label = { Text("Home", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = activeTab == 1 && !isDialerVisible,
                    onClick = {
                        activeTab = 1
                        isDialerVisible = false
                        isBlockRulesVisible = false
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "Recents archives") },
                    label = { Text("Recents", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_recents")
                )

                NavigationBarItem(
                    selected = activeTab == 2 && !isDialerVisible,
                    onClick = {
                        activeTab = 2
                        isDialerVisible = false
                        isBlockRulesVisible = false
                    },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts directories") },
                    label = { Text("Contacts", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_contacts")
                )

                NavigationBarItem(
                    selected = activeTab == 3 && !isDialerVisible,
                    onClick = {
                        activeTab = 3
                        isDialerVisible = false
                        isBlockRulesVisible = false
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Settings tabs") },
                    label = { Text("Me", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            
            // 1. COLLAPSIBLE SIMULATION TOOLBAR (For Test validation purposes)
            AnimatedVisibility(
                visible = showSimulatorToolbar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DIAGNOSTIC CALL SIMULATOR PANEL",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Trigger real incoming calls matching spam patterns on emulators to verify active drop rules and overlays.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeIncomingCallNumber = "1400987112" // Spam Telemarketer prefix
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Mock Telemarketer", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                activeIncomingCallNumber = "1800555019" // Robocall prefix
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Mock Robocall", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                activeIncomingCallNumber = "+1 (415) 345-0922" // Clean safe bypass
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E824C))
                        ) {
                            Text("Mock Safe Pass", fontSize = 10.sp)
                        }
                    }
                }
            }

            // 2. MAIN ACTIVE TAB PORT
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (showSimulatorToolbar) 120.dp else 0.dp)
            ) {
                when (activeTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { activeTab = it },
                        onQuickAction = { action ->
                            when (action) {
                                "block" -> isDialerVisible = true
                                "report" -> reportSpamNumber = ""
                                "lookup" -> isDialerVisible = true
                                "filter" -> isBlockRulesVisible = true
                            }
                        }
                    )
                    1 -> RecentsScreen(
                        viewModel = viewModel,
                        onNavigateToDialer = { isDialerVisible = true },
                        onInspectCallerId = { number -> inspectedNumberDetails = number }
                    )
                    2 -> ContactsScreen(
                        viewModel = viewModel
                    )
                    3 -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }

            // 3. FULL SCREEN DIALER LAYER (SLIDES UP)
            AnimatedVisibility(
                visible = isDialerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                DialerScreen(
                    viewModel = viewModel,
                    onBack = { isDialerVisible = false },
                    onInitiateSimulatedCall = { number ->
                        activeIncomingCallNumber = number
                        isDialerVisible = false
                    }
                )
            }

            // 4. FULL SCREEN BLOCK RULES LAYER (SLIDES UP)
            AnimatedVisibility(
                visible = isBlockRulesVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Blocking Rules", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { isBlockRulesVisible = false }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close block rules modal")
                                }
                            }
                        )
                    }
                ) { innerScaffold ->
                    Box(modifier = Modifier.padding(innerScaffold)) {
                        BlockRulesScreen(viewModel = viewModel)
                    }
                }
            }

            // 5. FULL SCREEN ANLYTICS INSIGHTS LAYER (SLIDES UP)
            AnimatedVisibility(
                visible = isInsightsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Protection Analytics insights", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { isInsightsVisible = false }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close insights panel")
                                }
                            }
                        )
                    }
                ) { innerScaffold ->
                    Box(modifier = Modifier.padding(innerScaffold)) {
                        InsightsScreen(viewModel = viewModel)
                    }
                }
            }

            // 6. INCOMING CALL ACTIVE INTERACTIVE OVERLAY SCREEN
            if (activeIncomingCallNumber != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IncomingCallScreen(
                        viewModel = viewModel,
                        phoneNumber = activeIncomingCallNumber!!,
                        onAcceptCall = {
                            activeIncomingCallNumber = null
                            Toast.makeText(context, "Call Answered successfully!", Toast.LENGTH_SHORT).show()
                        },
                        onDeclineCall = {
                            activeIncomingCallNumber = null
                            Toast.makeText(context, "Simulated spam call dropped and logged!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 7. INSPECTED CALLERID DETAILS OVERLAY
            if (inspectedNumberDetails != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CallerIdLookupScreen(
                        viewModel = viewModel,
                        phoneNumber = inspectedNumberDetails!!,
                        onBack = { inspectedNumberDetails = null },
                        onNavigateToReport = { number ->
                            reportSpamNumber = number
                            inspectedNumberDetails = null
                        }
                    )
                }
            }

            // 8. REPORT SPAM STEP-WIZARD OVERLAY
            if (reportSpamNumber != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ReportSpamScreen(
                        viewModel = viewModel,
                        defaultNumber = reportSpamNumber!!,
                        onDone = { reportSpamNumber = null }
                    )
                }
            }
        }
    }
}

fun isCallScreeningRoleHeld(context: Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        return false
    }
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
    return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
}
