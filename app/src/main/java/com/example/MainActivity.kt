package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MotorcycleProfile
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MotoLogTheme
import com.example.ui.viewmodel.MotoViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    GARAGE("Garage", Icons.Filled.TwoWheeler, Icons.Outlined.TwoWheeler, "tab_garage"),
    SERVICE("Service Log", Icons.Filled.Build, Icons.Outlined.Build, "tab_service"),
    FUEL("Fuel", Icons.Filled.LocalGasStation, Icons.Outlined.LocalGasStation, "tab_fuel"),
    PARTS("Parts & Wear", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_parts"),
    EXPENSES("Expenses", Icons.Filled.PieChart, Icons.Outlined.PieChart, "tab_expenses")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MotoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MotoLogTheme {
                MotoLogMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoLogMainApp(viewModel: MotoViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("pitstop_prefs", android.content.Context.MODE_PRIVATE) }
    var hasCompletedOnboarding by remember {
        mutableStateOf(prefs.getBoolean("has_completed_onboarding", false))
    }
    var showOnboardingModal by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf(NavigationTab.GARAGE) }

    val profile by viewModel.profile.collectAsState()

    // Dialog States
    var showAddFuelDialog by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showExportDataDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    val maintenanceAlerts by viewModel.maintenanceAlerts.collectAsState()

    if (!hasCompletedOnboarding || showOnboardingModal) {
        OnboardingScreen(
            onComplete = { name, country, currencySymbol, distanceUnit, volumeUnit, vehicleType, model, year, color, boughtOnTimestamp, numberPlate ->
                viewModel.completeOnboarding(
                    ownerName = name,
                    country = country,
                    currencySymbol = currencySymbol,
                    distanceUnit = distanceUnit,
                    volumeUnit = volumeUnit,
                    vehicleType = vehicleType,
                    model = model,
                    year = year,
                    color = color,
                    boughtOnTimestamp = boughtOnTimestamp,
                    numberPlate = numberPlate
                )
                prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                hasCompletedOnboarding = true
                showOnboardingModal = false
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_road_gear_1787706553297),
                            contentDescription = "PitStop Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (currentTab) {
                                NavigationTab.GARAGE -> "PitStop Garage"
                                NavigationTab.SERVICE -> "Maintenance & Service"
                                NavigationTab.FUEL -> "Fuel Top-Up Tracker"
                                NavigationTab.PARTS -> "Parts & Component Wear"
                                NavigationTab.EXPENSES -> "Analytics & Expenses"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFeedbackDialog = true },
                        modifier = Modifier.testTag("appbar_feedback")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = "Send App Feedback",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showNotificationSettingsDialog = true },
                        modifier = Modifier.testTag("appbar_notifications")
                    ) {
                        Badge(
                            containerColor = if (maintenanceAlerts.isNotEmpty()) com.example.ui.theme.MotoOrange else androidx.compose.ui.graphics.Color.Transparent
                        ) {
                            Icon(
                                imageVector = if (maintenanceAlerts.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Maintenance Notification Settings",
                                tint = if (maintenanceAlerts.isNotEmpty()) com.example.ui.theme.MotoOrange else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showExportDataDialog = true },
                        modifier = Modifier.testTag("appbar_export_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Data Backup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("appbar_edit_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Garage Vehicle Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    NavigationTab.GARAGE -> GarageScreen(
                        viewModel = viewModel,
                        onNavigateToService = { currentTab = NavigationTab.SERVICE },
                        onNavigateToFuel = { currentTab = NavigationTab.FUEL },
                        onNavigateToParts = { currentTab = NavigationTab.PARTS },
                        onOpenAddFuel = { showAddFuelDialog = true },
                        onOpenAddService = { showAddServiceDialog = true },
                        onOpenAddPart = { showAddPartDialog = true },
                        onOpenEditProfile = { showEditProfileDialog = true },
                        onOpenExportData = { showExportDataDialog = true },
                        onLaunchOnboardingWizard = { showOnboardingModal = true },
                        onOpenFeedback = { showFeedbackDialog = true }
                    )

                    NavigationTab.SERVICE -> MaintenanceLogScreen(
                        viewModel = viewModel,
                        onOpenAddService = { showAddServiceDialog = true }
                    )

                    NavigationTab.FUEL -> FuelTrackerScreen(
                        viewModel = viewModel,
                        onOpenAddFuel = { showAddFuelDialog = true },
                        onNavigateToExpenses = { currentTab = NavigationTab.EXPENSES }
                    )

                    NavigationTab.PARTS -> PartsManagerScreen(
                        viewModel = viewModel,
                        onOpenAddPart = { showAddPartDialog = true }
                    )

                    NavigationTab.EXPENSES -> AnalyticsExpensesScreen(
                        viewModel = viewModel,
                        onOpenExportData = { showExportDataDialog = true },
                        onOpenAddFuel = { showAddFuelDialog = true },
                        onOpenAddService = { showAddServiceDialog = true }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showExportDataDialog) {
        ExportDataDialog(
            viewModel = viewModel,
            onDismiss = { showExportDataDialog = false }
        )
    }

    if (showAddServiceDialog) {
        AddServiceDialog(
            currentOdometer = profile.currentOdometer,
            onDismiss = { showAddServiceDialog = false },
            onConfirm = { date, odo, type, cost, provider, partsServ, partsChanged, nextOdo, notes ->
                viewModel.addServiceLog(date, odo, type, cost, provider, partsServ, partsChanged, nextOdo, notes)
                showAddServiceDialog = false
            }
        )
    }

    if (showAddFuelDialog) {
        AddFuelDialog(
            currentOdometer = profile.currentOdometer,
            onDismiss = { showAddFuelDialog = false },
            onConfirm = { date, odo, liters, price, isFull, fuelType, station, notes ->
                viewModel.addFuelLog(date, odo, liters, price, isFull, fuelType, station, notes)
                showAddFuelDialog = false
            }
        )
    }

    if (showAddPartDialog) {
        AddPartDialog(
            currentOdometer = profile.currentOdometer,
            onDismiss = { showAddPartDialog = false },
            onConfirm = { name, partNum, brand, odo, cost, interval, category, notes ->
                viewModel.addPartItem(name, partNum, brand, System.currentTimeMillis(), odo, cost, interval, category, notes)
                showAddPartDialog = false
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, make, model, year, vin, plate, odo, fuelCap ->
                viewModel.updateMotorcycleProfile(name, make, model, year, vin, plate, odo, fuelCap)
                showEditProfileDialog = false
            },
            onUpdateDocuments = { taxNo, taxDays, regNo, regDays, dlNo, dlDays ->
                val now = System.currentTimeMillis()
                val dayMs = 86400000L
                val taxExp = taxDays?.let { now + it * dayMs }
                val regExp = regDays?.let { now + it * dayMs }
                val dlExp = dlDays?.let { now + it * dayMs }
                viewModel.updateVehicleDocuments(
                    vehicleId = profile.id,
                    taxTokenNumber = taxNo,
                    taxTokenExpiryTimestamp = taxExp,
                    registrationNumber = regNo,
                    registrationExpiryTimestamp = regExp,
                    driverLicenseNumber = dlNo,
                    driverLicenseExpiryTimestamp = dlExp
                )
            },
            onOpenFeedback = { showFeedbackDialog = true }
        )
    }

    if (showNotificationSettingsDialog) {
        MaintenanceNotificationSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationSettingsDialog = false }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            activeVehicle = profile,
            onDismiss = { showFeedbackDialog = false }
        )
    }

}
