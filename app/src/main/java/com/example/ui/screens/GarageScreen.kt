package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.MaintenanceAlertBanner
import com.example.ui.components.MaintenanceNotificationSettingsDialog
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.PartWearCard
import com.example.ui.components.QuickActionBanner
import com.example.ui.components.ServiceLogCard
import com.example.ui.theme.FuelGreen
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import com.example.ui.theme.WarningYellow
import com.example.data.model.PurchaseDatePrecision
import com.example.data.model.PurchaseDateUtils
import com.example.ui.components.ModelYearDropdownField
import com.example.ui.components.PurchaseDatePickerField
import com.example.ui.viewmodel.MotoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    viewModel: MotoViewModel,
    onNavigateToService: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToParts: () -> Unit,
    onOpenAddFuel: () -> Unit,
    onOpenAddService: () -> Unit,
    onOpenAddPart: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenExportData: () -> Unit = {},
    onLaunchOnboardingWizard: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onOpenBacklog: (com.example.ui.components.BacklogCategory) -> Unit = {}
) {
    val activeVehicle by viewModel.activeVehicle.collectAsState()
    val allVehicles by viewModel.allVehicles.collectAsState()
    val documentAlerts by viewModel.documentAlerts.collectAsState()
    val maintenanceAlerts by viewModel.maintenanceAlerts.collectAsState()
    val fuelStats by viewModel.fuelStats.collectAsState()
    val serviceStats by viewModel.serviceStats.collectAsState()
    val partsWearList by viewModel.partsWearStatusList.collectAsState()
    val recentServices by viewModel.serviceLogs.collectAsState()

    var partToReplace by remember { mutableStateOf<com.example.ui.viewmodel.PartWearStatus?>(null) }
    var showVehiclePickerMenu by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showOdometerScannerModal by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("garage_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // --- HERO VEHICLE BANNER WITH MULTI-VEHICLE SWITCHER ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(245.dp)
            ) {
                val bannerRes = R.drawable.img_motorcycle_hero_1785523175964

                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = "Vehicle Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )


                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                // Top Toolbar: Multi-Vehicle Dropdown Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Surface(
                            onClick = { showVehiclePickerMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            tonalElevation = 4.dp,
                            modifier = Modifier.testTag("vehicle_switcher_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeVehicle.vehicleType.contains("Car") || activeVehicle.vehicleType.contains("SUV")) Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = MotoOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = activeVehicle.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Vehicle",
                                    tint = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showVehiclePickerMenu,
                            onDismissRequest = { showVehiclePickerMenu = false }
                        ) {
                            Text(
                                text = "SELECT VEHICLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            allVehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (vehicle.vehicleType.contains("Car") || vehicle.vehicleType.contains("SUV")) Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                                contentDescription = null,
                                                tint = if (vehicle.id == activeVehicle.id) MotoOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = vehicle.name,
                                                    fontWeight = if (vehicle.id == activeVehicle.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "${vehicle.licensePlate} • ${vehicle.vehicleType}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchActiveVehicle(vehicle.id)
                                        showVehiclePickerMenu = false
                                    },
                                    trailingIcon = {
                                        if (vehicle.id == activeVehicle.id) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = MotoOrange
                                            )
                                        }
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = MotoOrange
                                        )
                                        Column {
                                            Text(
                                                text = "Add Vehicle (2-Step Wizard)",
                                                fontWeight = FontWeight.Bold,
                                                color = MotoOrange
                                            )
                                            Text(
                                                text = "Country, currency & vehicle type setup",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showVehiclePickerMenu = false
                                    onLaunchOnboardingWizard()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Quick Add Vehicle & Papers",
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    showVehiclePickerMenu = false
                                    showAddVehicleDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RateReview,
                                            contentDescription = null,
                                            tint = MotoOrange
                                        )
                                        Text(
                                            text = "Send App Feedback",
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    showVehiclePickerMenu = false
                                    onOpenFeedback()
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenEditProfile,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Vehicle & Papers",
                            tint = Color.White
                        )
                    }
                }

                // Bottom Overlay Vehicle Specs
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MotoOrange,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${activeVehicle.year} ${activeVehicle.make.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = activeVehicle.vehicleType,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (activeVehicle.purchaseDateTimestamp != null && activeVehicle.purchaseDateTimestamp!! > 0L) {
                            val purchasePrecision = PurchaseDatePrecision.fromString(activeVehicle.purchaseDatePrecision)
                            val purchaseLabel = PurchaseDateUtils.formatPurchaseDate(
                                activeVehicle.purchaseDateTimestamp,
                                purchasePrecision
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("hero_purchase_date_badge")
                            ) {
                                Text(
                                    text = "Bought $purchaseLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeVehicle.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { showOdometerScannerModal = true },
                            color = MotoOrange.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("hero_scan_meter_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan Meter Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Odo: ${activeVehicle.currentOdometer.toInt()} ${activeVehicle.preferredDistanceUnit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Plate: ${activeVehicle.licensePlate}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // --- EXPIRY ALERTS BANNER (TAX TOKEN / REGISTRATION / DRIVER LICENSE) ---
        if (documentAlerts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    documentAlerts.forEach { alert ->
                        val isUrgent = alert.isExpired || alert.daysRemaining <= 7
                        val cardBg = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                        val contentColor = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        val icon = if (isUrgent) Icons.Default.Warning else Icons.Default.NotificationImportant

                        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val expiryStr = df.format(Date(alert.expiryTimestamp))

                        val statusText = if (alert.isExpired) {
                            "EXPIRED ${Math.abs(alert.daysRemaining)} days ago ($expiryStr)"
                        } else {
                            "Expires in ${alert.daysRemaining} days ($expiryStr)"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenEditProfile() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = alert.docType,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${alert.docType} Alert: ${alert.vehicleName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                    Text(
                                        text = "${alert.docNumber} • $statusText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor.copy(alpha = 0.9f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Update Papers",
                                    tint = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- APPROACHING / OVERDUE MAINTENANCE ALERTS BANNER ---
        if (maintenanceAlerts.isNotEmpty()) {
            item {
                MaintenanceAlertBanner(
                    alerts = maintenanceAlerts,
                    onOpenNotificationSettings = { showNotificationSettingsDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // --- QUICK ACTION BANNER ---
        item {
            QuickActionBanner(
                onAddFuelClick = onOpenAddFuel,
                onAddServiceClick = onOpenAddService,
                onAddPartClick = onOpenAddPart,
                onScanOdoClick = { showOdometerScannerModal = true },
                onExportClick = onOpenExportData,
                onBacklogClick = { onOpenBacklog(com.example.ui.components.BacklogCategory.FUEL) },
                modifier = Modifier.padding(16.dp)
            )
        }

        // --- KEY METRIC CARDS GRID ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "LOGS & EFFICIENCY OVERVIEW",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricSummaryCard(
                        title = "Fuel Economy",
                        value = "${String.format(Locale.US, "%.1f", fuelStats.averageKmLiter)} km/L",
                        subtitle = "${String.format(Locale.US, "%.1f", fuelStats.averageMpg)} MPG avg",
                        icon = Icons.Default.LocalGasStation,
                        iconColor = FuelGreen,
                        modifier = Modifier.weight(1f)
                    )

                    val kmNext = serviceStats.kmUntilNextService
                    val nextSub = if (kmNext != null && kmNext > 0) "In ${kmNext.toInt()} km" else if (kmNext != null) "Overdue!" else "Due soon"

                    MetricSummaryCard(
                        title = "Service Due",
                        value = if (serviceStats.nextServiceOdometerDue != null) "${serviceStats.nextServiceOdometerDue?.toInt()} km" else "18,800 km",
                        subtitle = nextSub,
                        icon = Icons.Default.Build,
                        iconColor = ServiceBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricSummaryCard(
                        title = "Service Spend",
                        value = "$${String.format(Locale.US, "%.0f", serviceStats.totalServiceCost)}",
                        subtitle = "${serviceStats.totalServicesCount} maintenance logs",
                        icon = Icons.Default.Engineering,
                        iconColor = MotoOrange,
                        modifier = Modifier.weight(1f)
                    )

                    MetricSummaryCard(
                        title = "Parts Changed",
                        value = "${serviceStats.totalPartsReplacedCount} Parts",
                        subtitle = "Tracked in logs",
                        icon = Icons.Default.Autorenew,
                        iconColor = WarningYellow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- ACTIVE PART WEAR STATUS SECTION ---
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRITICAL PART WEAR & HEALTH",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = onNavigateToParts,
                    modifier = Modifier.testTag("view_all_parts_button")
                ) {
                    Text("View All (${partsWearList.size})")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(partsWearList.take(3)) { status ->
            PartWearCard(
                status = status,
                onReplaceClick = { partToReplace = status },
                onDeleteClick = { viewModel.deletePartItem(status.part) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // --- RECENT MAINTENANCE LOGS SECTION ---
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT SERVICE LOGS (${activeVehicle.name})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = onNavigateToService,
                    modifier = Modifier.testTag("view_all_service_button")
                ) {
                    Text("View All")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (recentServices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Service Logs for ${activeVehicle.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap '+ Service' above to add your first maintenance log for this vehicle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentServices.take(3)) { log ->
                ServiceLogCard(
                    serviceLog = log,
                    onDeleteClick = { viewModel.deleteServiceLog(log) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // --- BACKLOG & PAST HISTORY CARD ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("garage_backlog_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrange.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MotoOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Backlog & Past History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add saved previous fuel, service & replaced parts history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { onOpenBacklog(com.example.ui.components.BacklogCategory.FUEL) },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("garage_backlog_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backlog", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- LOCAL DATA BACKUP & EXPORT CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("garage_backup_export_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrange.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MotoOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Local Data Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Export all logs & vehicles to local JSON",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onOpenExportData,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("garage_export_json_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // App Feedback & Suggestions Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("garage_feedback_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrange.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RateReview,
                                    contentDescription = null,
                                    tint = MotoOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Feedback & Ideas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Help improve PitStop with suggestions or bugs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onOpenFeedback,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("garage_feedback_button")
                    ) {
                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Feedback", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Replace Dialog if triggered
    partToReplace?.let { status ->
        com.example.ui.components.ReplacePartDialog(
            partItem = status.part,
            currentOdometer = activeVehicle.currentOdometer,
            onDismiss = { partToReplace = null },
            onConfirm = { cost, odo ->
                viewModel.replacePart(status.part, cost, odo)
                partToReplace = null
            }
        )
    }

    // Maintenance Notification Settings Dialog
    if (showNotificationSettingsDialog) {
        MaintenanceNotificationSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationSettingsDialog = false }
        )
    }

    // Quick Odometer Scanner Dialog triggered from hero banner or quick actions
    if (showOdometerScannerModal) {
        com.example.ui.components.OdometerScannerDialog(
            initialValue = activeVehicle.currentOdometer,
            unit = activeVehicle.preferredDistanceUnit,
            onDismiss = { showOdometerScannerModal = false },
            onApplyReading = { scannedOdo ->
                viewModel.updateMotorcycleProfile(
                    name = activeVehicle.name,
                    make = activeVehicle.make,
                    model = activeVehicle.model,
                    year = activeVehicle.year,
                    vin = activeVehicle.vin,
                    licensePlate = activeVehicle.licensePlate,
                    currentOdometer = scannedOdo,
                    fuelCapacity = activeVehicle.fuelCapacityLiters
                )
                showOdometerScannerModal = false
            }
        )
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { name, type, make, model, year, purchaseDateTs, purchaseDatePrec, plate, odo, taxNo, taxDays, regNo, regDays, dlNo, dlDays ->
                viewModel.addVehicle(
                    name = name,
                    vehicleType = type,
                    make = make,
                    model = model,
                    year = year,
                    vin = "",
                    licensePlate = plate,
                    currentOdometer = odo,
                    fuelCapacity = 15.0,
                    purchaseDateTimestamp = purchaseDateTs,
                    purchaseDatePrecision = purchaseDatePrec,
                    taxTokenNumber = taxNo,
                    taxTokenExpiryDays = taxDays,
                    registrationNumber = regNo,
                    registrationExpiryDays = regDays,
                    driverLicenseNumber = dlNo,
                    driverLicenseExpiryDays = dlDays
                )
                showAddVehicleDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: String,
        make: String,
        model: String,
        year: Int,
        purchaseDateTimestamp: Long?,
        purchaseDatePrecision: String,
        plate: String,
        odo: Double,
        taxNo: String,
        taxDays: Int?,
        regNo: String,
        regDays: Int?,
        dlNo: String,
        dlDays: Int?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("Motorcycle") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var purchaseDateTimestamp by remember { mutableStateOf<Long?>(null) }
    var purchaseDatePrecision by remember { mutableStateOf(PurchaseDatePrecision.FULL_DATE) }
    var plate by remember { mutableStateOf("") }
    var odoStr by remember { mutableStateOf("0") }

    var taxNo by remember { mutableStateOf("") }
    var taxDaysStr by remember { mutableStateOf("365") }

    var regNo by remember { mutableStateOf("") }
    var regDaysStr by remember { mutableStateOf("365") }

    var dlNo by remember { mutableStateOf("") }
    var dlDaysStr by remember { mutableStateOf("365") }

    val vehicleTypes = listOf("Motorcycle", "Car / Sedan", "SUV", "Truck", "Scooter", "EV", "Van", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Vehicle & Papers") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Vehicle Nickname *") },
                        placeholder = { Text("e.g. Daily Commuter / Honda Civic") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Vehicle Type", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Motorcycle", "Car / Sedan", "SUV", "Scooter").forEach { type ->
                            FilterChip(
                                selected = vehicleType == type,
                                onClick = { vehicleType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = make,
                            onValueChange = { make = it },
                            label = { Text("Make") },
                            placeholder = { Text("Honda / Yamaha") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model") },
                            placeholder = { Text("Civic / MT-07") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModelYearDropdownField(
                            selectedYear = selectedYear,
                            onYearSelected = { selectedYear = it },
                            label = "Model Year",
                            modifier = Modifier.weight(1f),
                            testTag = "add_vehicle_year_dropdown"
                        )
                        OutlinedTextField(
                            value = plate,
                            onValueChange = { plate = it },
                            label = { Text("License Plate") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }

                item {
                    PurchaseDatePickerField(
                        selectedTimestamp = purchaseDateTimestamp,
                        precision = purchaseDatePrecision,
                        onDateSelected = { ts, prec ->
                            purchaseDateTimestamp = ts
                            purchaseDatePrecision = prec
                        },
                        label = "Purchased On (Optional)",
                        allowClear = true,
                        testTagPrefix = "add_vehicle_purchase_date"
                    )
                }

                item {
                    com.example.ui.components.OdometerInputFieldWithScanner(
                        value = odoStr,
                        onValueChange = { odoStr = it },
                        label = "Starting Odometer (km)",
                        unit = "km",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "DOCUMENT & EXPIRY TRACKING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MotoOrange
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taxNo,
                            onValueChange = { taxNo = it },
                            label = { Text("Tax Token #") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = taxDaysStr,
                            onValueChange = { taxDaysStr = it },
                            label = { Text("Tax Expires In (Days)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = regNo,
                            onValueChange = { regNo = it },
                            label = { Text("Reg Paper #") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = regDaysStr,
                            onValueChange = { regDaysStr = it },
                            label = { Text("Reg Expires In (Days)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dlNo,
                            onValueChange = { dlNo = it },
                            label = { Text("Driver License #") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = dlDaysStr,
                            onValueChange = { dlDaysStr = it },
                            label = { Text("License Expires In (Days)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            vehicleType,
                            make.ifBlank { "Vehicle" },
                            model.ifBlank { "Model" },
                            selectedYear,
                            purchaseDateTimestamp,
                            purchaseDatePrecision.name,
                            plate.ifBlank { "N/A" },
                            odoStr.toDoubleOrNull() ?: 0.0,
                            taxNo,
                            taxDaysStr.toIntOrNull(),
                            regNo,
                            regDaysStr.toIntOrNull(),
                            dlNo,
                            dlDaysStr.toIntOrNull()
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Vehicle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

