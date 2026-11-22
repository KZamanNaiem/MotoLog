package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.backlog.BacklogImportMode
import com.example.data.backlog.BacklogImportParser
import com.example.data.backlog.BacklogParseResult
import com.example.data.model.FuelLog
import com.example.data.model.PartItem
import com.example.data.model.ServiceLog
import com.example.data.model.Vehicle
import com.example.ui.theme.FuelGreen
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import com.example.ui.viewmodel.MotoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BacklogCategory(val label: String) {
    FUEL("Fuel"),
    SERVICE("Service"),
    PART("Part")
}

private sealed class QueuedBacklogEntry(val timestamp: Long, val keyId: Int) {
    class Fuel(val item: FuelLog, keyId: Int) : QueuedBacklogEntry(item.dateTimestamp, keyId)
    class Service(val item: ServiceLog, keyId: Int) : QueuedBacklogEntry(item.dateTimestamp, keyId)
    class Part(val item: PartItem, keyId: Int) : QueuedBacklogEntry(item.installedDateTimestamp, keyId)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BacklogHistoryDialog(
    viewModel: MotoViewModel,
    activeVehicle: Vehicle,
    initialCategory: BacklogCategory = BacklogCategory.FUEL,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // --- Tab 0 State: Quick Backlog Queue Builder ---
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedDateMillis by remember { mutableLongStateOf(shiftDaysFromToday(7)) }
    var odometerStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    // Fuel fields
    var litersStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var stationStr by remember { mutableStateOf("") }
    var isFullTank by remember { mutableStateOf(true) }

    // Service fields
    var serviceTypeStr by remember { mutableStateOf("Oil & Filter Change") }
    var serviceCostStr by remember { mutableStateOf("") }
    var serviceProviderStr by remember { mutableStateOf("") }
    var partsChangedStr by remember { mutableStateOf("") }

    // Part fields
    var partNameStr by remember { mutableStateOf("") }
    var partBrandStr by remember { mutableStateOf("") }
    var partCostStr by remember { mutableStateOf("") }
    var partIntervalStr by remember { mutableStateOf("5000") }
    var partIsReplaced by remember { mutableStateOf(false) }

    var nextKeyId by remember { mutableIntStateOf(1) }
    val queuedEntries = remember { mutableStateListOf<QueuedBacklogEntry>() }

    // --- Tab 1 State: CSV / JSON Import ---
    var importMode by remember {
        mutableStateOf(
            when (initialCategory) {
                BacklogCategory.FUEL -> BacklogImportMode.FUEL_CSV
                BacklogCategory.SERVICE -> BacklogImportMode.SERVICE_CSV
                BacklogCategory.PART -> BacklogImportMode.PARTS_CSV
            }
        )
    }
    var importText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                if (content.isNotBlank()) {
                    importText = content
                    if (content.trimStart().startsWith("{") || content.trimStart().startsWith("[")) {
                        importMode = BacklogImportMode.AUTO_MIXED
                    }
                    Toast.makeText(context, "Loaded file into preview", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to read file: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val parseResult: BacklogParseResult = remember(importText, importMode, activeVehicle.id) {
        if (importText.isBlank()) {
            BacklogParseResult()
        } else {
            BacklogImportParser.parseInput(importText, activeVehicle.id, importMode)
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("backlog_history_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MotoOrange.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MotoOrange
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Backlog & Past History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${activeVehicle.name} • Backdate past fuel, service & parts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("backlog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Backlog Dialog"
                        )
                    }
                }

                // Mode Tabs
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = if (queuedEntries.isEmpty()) "Quick Builder" else "Quick Builder (${queuedEntries.size})",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("backlog_tab_quick")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Paste / Import File",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("backlog_tab_import")
                    )
                }

                // Body Content
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category Selector Pills
                        Text(
                            text = "1. SELECT LOG TYPE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BacklogCategory.entries.forEach { category ->
                                val isSelected = selectedCategory == category
                                val accentColor = when (category) {
                                    BacklogCategory.FUEL -> FuelGreen
                                    BacklogCategory.SERVICE -> ServiceBlue
                                    BacklogCategory.PART -> MotoOrange
                                }
                                val icon = when (category) {
                                    BacklogCategory.FUEL -> Icons.Default.LocalGasStation
                                    BacklogCategory.SERVICE -> Icons.Default.Build
                                    BacklogCategory.PART -> Icons.Default.Settings
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = category
                                        formError = null
                                    },
                                    label = { Text(category.label, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("backlog_category_${category.name.lowercase(Locale.US)}")
                                )
                            }
                        }

                        // Backdate Picker
                        BackdatePickerField(
                            selectedTimestamp = selectedDateMillis,
                            onDateSelected = { selectedDateMillis = it },
                            label = "2. Historical Entry Date",
                            testTagPrefix = "backlog_date"
                        )

                        // Odometer Input
                        OdometerInputFieldWithScanner(
                            value = odometerStr,
                            onValueChange = {
                                odometerStr = it
                                formError = null
                            },
                            label = "Historical Odometer (${activeVehicle.preferredDistanceUnit}) *",
                            unit = activeVehicle.preferredDistanceUnit,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "backlog_odometer_input"
                        )

                        // Category-specific form
                        when (selectedCategory) {
                            BacklogCategory.FUEL -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = litersStr,
                                        onValueChange = {
                                            litersStr = it
                                            formError = null
                                        },
                                        label = { Text("Volume (${activeVehicle.preferredVolumeUnit}) *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("backlog_fuel_volume_input")
                                    )
                                    OutlinedTextField(
                                        value = priceStr,
                                        onValueChange = { priceStr = it },
                                        label = { Text("Price / ${activeVehicle.preferredVolumeUnit} (${activeVehicle.preferredCurrency})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("backlog_fuel_price_input")
                                    )
                                }
                                OutlinedTextField(
                                    value = stationStr,
                                    onValueChange = { stationStr = it },
                                    label = { Text("Fuel Station (optional)") },
                                    placeholder = { Text("e.g. Shell, BP, Chevron") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backlog_fuel_station_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Full Tank Fill",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Switch(
                                        checked = isFullTank,
                                        onCheckedChange = { isFullTank = it },
                                        modifier = Modifier.testTag("backlog_fuel_full_switch")
                                    )
                                }
                            }

                            BacklogCategory.SERVICE -> {
                                val servicePresets = listOf(
                                    "Oil & Filter Change",
                                    "Full Service",
                                    "Tire Change",
                                    "Brake Service",
                                    "Chain & Sprocket"
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(servicePresets) { preset ->
                                        AssistChip(
                                            onClick = { serviceTypeStr = preset },
                                            label = { Text(preset, fontSize = 11.sp) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = serviceTypeStr,
                                    onValueChange = {
                                        serviceTypeStr = it
                                        formError = null
                                    },
                                    label = { Text("Service Type *") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backlog_service_type_input")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = serviceCostStr,
                                        onValueChange = { serviceCostStr = it },
                                        label = { Text("Cost (${activeVehicle.preferredCurrency})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("backlog_service_cost_input")
                                    )
                                    OutlinedTextField(
                                        value = serviceProviderStr,
                                        onValueChange = { serviceProviderStr = it },
                                        label = { Text("Workshop / DIY") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("backlog_service_provider_input")
                                    )
                                }
                                OutlinedTextField(
                                    value = partsChangedStr,
                                    onValueChange = { partsChangedStr = it },
                                    label = { Text("Parts Changed (optional)") },
                                    placeholder = { Text("e.g. 10W-40 Oil, Oil Filter, Spark Plug") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backlog_service_parts_input")
                                )
                            }

                            BacklogCategory.PART -> {
                                OutlinedTextField(
                                    value = partNameStr,
                                    onValueChange = {
                                        partNameStr = it
                                        formError = null
                                    },
                                    label = { Text("Part Name *") },
                                    placeholder = { Text("e.g. Drive Chain, Rear Tire, Air Filter") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backlog_part_name_input")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = partBrandStr,
                                        onValueChange = { partBrandStr = it },
                                        label = { Text("Brand (optional)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("backlog_part_brand_input")
                                    )
                                    OutlinedTextField(
                                        value = partCostStr,
                                        onValueChange = { partCostStr = it },
                                        label = { Text("Cost (${activeVehicle.preferredCurrency})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("backlog_part_cost_input")
                                    )
                                }
                                OutlinedTextField(
                                    value = partIntervalStr,
                                    onValueChange = { partIntervalStr = it },
                                    label = { Text("Lifespan Interval (${activeVehicle.preferredDistanceUnit})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backlog_part_interval_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Already Replaced (Past History)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Archive to Replaced History instead of active wear tracking",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = partIsReplaced,
                                        onCheckedChange = { partIsReplaced = it },
                                        modifier = Modifier.testTag("backlog_part_replaced_switch")
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notesStr,
                            onValueChange = { notesStr = it },
                            label = { Text("Notes (optional)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backlog_notes_input")
                        )

                        formError?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val odo = odometerStr.toDoubleOrNull()
                                if (odo == null || odo < 0.0) {
                                    formError = "Please enter a valid historical odometer reading."
                                    return@OutlinedButton
                                }

                                when (selectedCategory) {
                                    BacklogCategory.FUEL -> {
                                        val vol = litersStr.toDoubleOrNull()
                                        if (vol == null || vol <= 0.0) {
                                            formError = "Please enter a valid fuel volume."
                                            return@OutlinedButton
                                        }
                                        val price = priceStr.toDoubleOrNull() ?: 0.0
                                        val log = FuelLog(
                                            vehicleId = activeVehicle.id,
                                            dateTimestamp = selectedDateMillis,
                                            odometer = odo,
                                            litersFilled = vol,
                                            pricePerLiter = price,
                                            totalCost = vol * price,
                                            isFullTank = isFullTank,
                                            fuelType = "95 Octane",
                                            stationName = stationStr.trim(),
                                            notes = notesStr.trim()
                                        )
                                        queuedEntries.add(QueuedBacklogEntry.Fuel(log, nextKeyId++))
                                        odometerStr = ""
                                        litersStr = ""
                                        priceStr = ""
                                        stationStr = ""
                                        notesStr = ""
                                        formError = null
                                    }

                                    BacklogCategory.SERVICE -> {
                                        if (serviceTypeStr.isBlank()) {
                                            formError = "Please enter a service type."
                                            return@OutlinedButton
                                        }
                                        val cost = serviceCostStr.toDoubleOrNull() ?: 0.0
                                        val log = ServiceLog(
                                            vehicleId = activeVehicle.id,
                                            dateTimestamp = selectedDateMillis,
                                            odometer = odo,
                                            serviceType = serviceTypeStr.trim(),
                                            totalCost = cost,
                                            serviceProvider = serviceProviderStr.trim(),
                                            partsServiced = "",
                                            partsChanged = partsChangedStr.trim(),
                                            nextServiceOdometerDue = null,
                                            notes = notesStr.trim()
                                        )
                                        queuedEntries.add(QueuedBacklogEntry.Service(log, nextKeyId++))
                                        odometerStr = ""
                                        serviceCostStr = ""
                                        serviceProviderStr = ""
                                        partsChangedStr = ""
                                        notesStr = ""
                                        formError = null
                                    }

                                    BacklogCategory.PART -> {
                                        if (partNameStr.isBlank()) {
                                            formError = "Please enter the part name."
                                            return@OutlinedButton
                                        }
                                        val cost = partCostStr.toDoubleOrNull() ?: 0.0
                                        val interval = partIntervalStr.toDoubleOrNull()?.takeIf { it > 0 } ?: 5000.0
                                        val part = PartItem(
                                            vehicleId = activeVehicle.id,
                                            name = partNameStr.trim(),
                                            partNumber = "",
                                            brand = partBrandStr.trim(),
                                            installedDateTimestamp = selectedDateMillis,
                                            installedOdometer = odo,
                                            cost = cost,
                                            recommendedIntervalKm = interval,
                                            isReplaced = partIsReplaced,
                                            category = "Consumable",
                                            notes = notesStr.trim()
                                        )
                                        queuedEntries.add(QueuedBacklogEntry.Part(part, nextKeyId++))
                                        odometerStr = ""
                                        partNameStr = ""
                                        partBrandStr = ""
                                        partCostStr = ""
                                        notesStr = ""
                                        formError = null
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("backlog_add_to_queue_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MotoOrange)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = MotoOrange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add ${selectedCategory.label} Entry to Queue",
                                fontWeight = FontWeight.Bold,
                                color = MotoOrange
                            )
                        }

                        // Queued Items Preview
                        if (queuedEntries.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "QUEUED HISTORICAL RECORDS (${queuedEntries.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )

                            val sortedQueue = queuedEntries.sortedByDescending { it.timestamp }
                            sortedQueue.forEach { entry ->
                                val badgeColor = when (entry) {
                                    is QueuedBacklogEntry.Fuel -> FuelGreen
                                    is QueuedBacklogEntry.Service -> ServiceBlue
                                    is QueuedBacklogEntry.Part -> MotoOrange
                                }
                                val badgeText = when (entry) {
                                    is QueuedBacklogEntry.Fuel -> "FUEL"
                                    is QueuedBacklogEntry.Service -> "SERVICE"
                                    is QueuedBacklogEntry.Part -> "PART"
                                }
                                val titleText = when (entry) {
                                    is QueuedBacklogEntry.Fuel -> "${String.format(Locale.US, "%.1f", entry.item.litersFilled)} ${activeVehicle.preferredVolumeUnit} • ${activeVehicle.preferredCurrency}${String.format(Locale.US, "%.2f", entry.item.totalCost)}"
                                    is QueuedBacklogEntry.Service -> "${entry.item.serviceType} • ${activeVehicle.preferredCurrency}${String.format(Locale.US, "%.2f", entry.item.totalCost)}"
                                    is QueuedBacklogEntry.Part -> "${entry.item.name}${if (entry.item.isReplaced) " (Replaced)" else " (Active)"}"
                                }
                                val odoText = when (entry) {
                                    is QueuedBacklogEntry.Fuel -> "${entry.item.odometer.toInt()} ${activeVehicle.preferredDistanceUnit}"
                                    is QueuedBacklogEntry.Service -> "${entry.item.odometer.toInt()} ${activeVehicle.preferredDistanceUnit}"
                                    is QueuedBacklogEntry.Part -> "${entry.item.installedOdometer.toInt()} ${activeVehicle.preferredDistanceUnit}"
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = badgeColor.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = badgeColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${dateFormat.format(Date(entry.timestamp))} • $odoText",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = titleText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { queuedEntries.remove(entry) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove queued entry",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Bar for Tab 0
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${queuedEntries.size} queued",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sorted chronologically upon save",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                val fuels = queuedEntries.filterIsInstance<QueuedBacklogEntry.Fuel>().map { it.item }
                                val services = queuedEntries.filterIsInstance<QueuedBacklogEntry.Service>().map { it.item }
                                val parts = queuedEntries.filterIsInstance<QueuedBacklogEntry.Part>().map { it.item }
                                val total = fuels.size + services.size + parts.size
                                if (total > 0) {
                                    viewModel.saveBacklogBatch(
                                        fuelLogsToAdd = fuels,
                                        serviceLogsToAdd = services,
                                        partItemsToAdd = parts,
                                        updateOdometerIfHigher = true
                                    )
                                    Toast.makeText(
                                        context,
                                        "Saved $total historical record(s) to ${activeVehicle.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            },
                            enabled = queuedEntries.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("backlog_save_queue_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save Backlog (${queuedEntries.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Tab 1: Paste / Import File (CSV or JSON)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "IMPORT FORMAT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(BacklogImportMode.entries) { mode ->
                                FilterChip(
                                    selected = importMode == mode,
                                    onClick = { importMode = mode },
                                    label = { Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("backlog_import_mode_${mode.name.lowercase(Locale.US)}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("text/*", "application/json", "*/*")
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("backlog_pick_file_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick CSV / JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    importText = BacklogImportParser.getSampleTemplate(importMode)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("backlog_load_sample_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Template", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            label = { Text("Paste saved CSV lines or PitStop JSON backup") },
                            placeholder = { Text("Tap 'Load Template' to see format or pick a .csv / .json file") },
                            minLines = 6,
                            maxLines = 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backlog_import_text_input")
                        )

                        // Parse Preview Summary
                        if (importText.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (parseResult.totalCount > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (parseResult.totalCount > 0) FuelGreen else MotoOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "${parseResult.fuelLogs.size} Fuel • ${parseResult.serviceLogs.size} Service • ${parseResult.partItems.size} Parts ready",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (parseResult.warnings.isNotEmpty()) {
                                        parseResult.warnings.take(4).forEach { warning ->
                                            Text(
                                                text = "• $warning",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    parseResult.fuelLogs.take(2).forEach { fuel ->
                                        Text(
                                            text = "Fuel (${dateFormat.format(Date(fuel.dateTimestamp))}): ${fuel.odometer.toInt()} ${activeVehicle.preferredDistanceUnit}, ${fuel.litersFilled} ${activeVehicle.preferredVolumeUnit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    parseResult.serviceLogs.take(2).forEach { svc ->
                                        Text(
                                            text = "Service (${dateFormat.format(Date(svc.dateTimestamp))}): ${svc.serviceType} @ ${svc.odometer.toInt()} ${activeVehicle.preferredDistanceUnit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    parseResult.partItems.take(2).forEach { part ->
                                        Text(
                                            text = "Part (${dateFormat.format(Date(part.installedDateTimestamp))}): ${part.name} @ ${part.installedOdometer.toInt()} ${activeVehicle.preferredDistanceUnit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Bar for Tab 1
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${parseResult.totalCount} parsed records",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Safe merge into ${activeVehicle.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                val total = parseResult.totalCount
                                if (total > 0) {
                                    viewModel.saveBacklogBatch(
                                        fuelLogsToAdd = parseResult.fuelLogs,
                                        serviceLogsToAdd = parseResult.serviceLogs,
                                        partItemsToAdd = parseResult.partItems,
                                        updateOdometerIfHigher = true
                                    )
                                    Toast.makeText(
                                        context,
                                        "Imported $total historical record(s) to ${activeVehicle.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            },
                            enabled = parseResult.totalCount > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("backlog_confirm_import_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import (${parseResult.totalCount})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
