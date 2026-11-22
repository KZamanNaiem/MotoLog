package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MotorcycleProfile
import com.example.data.model.PartItem
import com.example.data.model.PurchaseDatePrecision
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun isDateInPastDays(timestampMillis: Long): Boolean {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return timestampMillis < todayCal.timeInMillis
}

internal fun shiftDaysFromToday(daysAgo: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return cal.timeInMillis
}

internal fun shiftMonthsFromToday(monthsAgo: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -monthsAgo)
    return cal.timeInMillis
}

internal fun utcPickerMillisToLocalMillis(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

internal fun localMillisToUtcPickerMillis(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply {
        timeInMillis = localMillis
    }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, localCal.get(Calendar.YEAR))
        set(Calendar.MONTH, localCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
    }
    return utcCal.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackdatePickerField(
    selectedTimestamp: Long,
    onDateSelected: (Long) -> Unit,
    label: String = "Entry Date (Tap to Backdate)",
    modifier: Modifier = Modifier,
    testTagPrefix: String = "entry_date"
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val isBackdated = remember(selectedTimestamp) { isDateInPastDays(selectedTimestamp) }
    val formattedDate = remember(selectedTimestamp) { dateFormat.format(Date(selectedTimestamp)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isBackdated) MotoOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isBackdated) MotoOrange.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .testTag("${testTagPrefix}_picker_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isBackdated) Icons.Default.History else Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isBackdated) MotoOrange else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isBackdated) MotoOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isBackdated) "BACKDATED" else "TODAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isBackdated) MotoOrange else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { showDatePicker = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Pick Date", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Backdate Preset Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val presets = listOf(
                "Today" to { System.currentTimeMillis() },
                "-1d" to { shiftDaysFromToday(1) },
                "-1w" to { shiftDaysFromToday(7) },
                "-1m" to { shiftMonthsFromToday(1) },
                "-3m" to { shiftMonthsFromToday(3) }
            )
            presets.forEach { (chipLabel, timeProvider) ->
                AssistChip(
                    onClick = { onDateSelected(timeProvider()) },
                    label = { Text(chipLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("${testTagPrefix}_preset_$chipLabel")
                )
            }
        }

        if (isBackdated) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Historical entry: lower odometer values won't overwrite your vehicle's current reading.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }

    if (showDatePicker) {
        val todayEndUtc = remember { localMillisToUtcPickerMillis(System.currentTimeMillis()) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToUtcPickerMillis(selectedTimestamp),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayEndUtc + 86400000L
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onDateSelected(utcPickerMillisToLocalMillis(utcMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Apply Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceDialog(
    currentOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        dateTimestamp: Long,
        odometer: Double,
        serviceType: String,
        cost: Double,
        provider: String,
        partsServiced: String,
        partsChanged: String,
        nextOdometer: Double?,
        notes: String
    ) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var serviceType by remember { mutableStateOf("Scheduled Service") }
    var odometerStr by remember { mutableStateOf(currentOdometer.toInt().toString()) }
    var costStr by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var partsServiced by remember { mutableStateOf("") }
    var partsChanged by remember { mutableStateOf("") }
    var nextOdometerStr by remember { mutableStateOf((currentOdometer.toInt() + 5000).toString()) }
    var nextOdoEditedManually by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = ServiceBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Maintenance Service",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BackdatePickerField(
                    selectedTimestamp = selectedDateMillis,
                    onDateSelected = { newDate ->
                        selectedDateMillis = newDate
                        if (isDateInPastDays(newDate) && !nextOdoEditedManually) {
                            nextOdometerStr = ""
                        }
                    },
                    label = "Service Date (Today or Backdate)",
                    testTagPrefix = "service_date"
                )

                // Service Type Dropdown / Selection
                Text(
                    text = "Service Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = serviceType,
                    onValueChange = { serviceType = it },
                    label = { Text("Service Type / Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_type_input"),
                    singleLine = true
                )

                OdometerInputFieldWithScanner(
                    value = odometerStr,
                    onValueChange = { odometerStr = it },
                    label = "Service Odometer (km)",
                    unit = "km",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "service_odometer_input"
                )

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Total Service Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_cost_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Service Provider / Mechanic / DIY") },
                    placeholder = { Text("e.g., Authorized Dealer, Local Shop, Self") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_provider_input"),
                    singleLine = true
                )

                // --- PARTS SERVICED ---
                OutlinedTextField(
                    value = partsServiced,
                    onValueChange = { partsServiced = it },
                    label = { Text("Parts Serviced / Inspected / Adjusted") },
                    placeholder = { Text("e.g. Chain lubed & tensioned, Throttle freeplay, Brake fluid checked") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parts_serviced_input"),
                    minLines = 2
                )

                // --- PARTS CHANGED / REPLACED ---
                OutlinedTextField(
                    value = partsChanged,
                    onValueChange = { partsChanged = it },
                    label = { Text("Parts Changed / Replaced") },
                    placeholder = { Text("e.g. Engine Oil (Yamalube 10W-40), Oil Filter, Front Brake Pads") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parts_changed_input"),
                    minLines = 2
                )

                OutlinedTextField(
                    value = nextOdometerStr,
                    onValueChange = {
                        nextOdometerStr = it
                        nextOdoEditedManually = true
                    },
                    label = { Text("Next Recommended Service (km, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_next_odo_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Observations") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_notes_input"),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odometerStr.toDoubleOrNull() ?: currentOdometer
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    val nextOdo = nextOdometerStr.toDoubleOrNull()
                    onConfirm(
                        selectedDateMillis,
                        odo,
                        serviceType.ifBlank { "General Service" },
                        cost,
                        provider,
                        partsServiced,
                        partsChanged,
                        nextOdo,
                        notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ServiceBlue),
                modifier = Modifier.testTag("save_service_button")
            ) {
                Text("Save Service Log", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddFuelDialog(
    currentOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        dateTimestamp: Long,
        odometer: Double,
        liters: Double,
        pricePerLiter: Double,
        isFullTank: Boolean,
        fuelType: String,
        station: String,
        notes: String
    ) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var odometerStr by remember { mutableStateOf(currentOdometer.toInt().toString()) }
    var litersStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var isFullTank by remember { mutableStateOf(true) }
    var fuelType by remember { mutableStateOf("95 Octane") }
    var stationName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MotoOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Fuel Top-Up",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BackdatePickerField(
                    selectedTimestamp = selectedDateMillis,
                    onDateSelected = { selectedDateMillis = it },
                    label = "Fill-Up Date (Today or Backdate)",
                    testTagPrefix = "fuel_date"
                )

                OdometerInputFieldWithScanner(
                    value = odometerStr,
                    onValueChange = { odometerStr = it },
                    label = "Odometer at Fuel Fill (km)",
                    unit = "km",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "fuel_odometer_input"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = litersStr,
                        onValueChange = { litersStr = it },
                        label = { Text("Volume (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fuel_liters_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price per Liter ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fuel_price_input"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = { Text("Gas Station / Brand") },
                    placeholder = { Text("e.g. Shell, Chevron, BP") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fuel_station_input"),
                    singleLine = true
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
                        modifier = Modifier.testTag("fuel_full_tank_switch")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fuel_notes_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odometerStr.toDoubleOrNull() ?: currentOdometer
                    val lit = litersStr.toDoubleOrNull() ?: 0.0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    onConfirm(
                        selectedDateMillis,
                        odo,
                        lit,
                        price,
                        isFullTank,
                        fuelType,
                        stationName,
                        notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                modifier = Modifier.testTag("save_fuel_button")
            ) {
                Text("Save Fuel Log", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddPartDialog(
    currentOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        partNumber: String,
        brand: String,
        installedDateTimestamp: Long,
        installedOdometer: Double,
        cost: Double,
        recommendedIntervalKm: Double,
        category: String,
        notes: String,
        isReplaced: Boolean
    ) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var partNumber by remember { mutableStateOf("") }
    var odoStr by remember { mutableStateOf(currentOdometer.toInt().toString()) }
    var intervalStr by remember { mutableStateOf("5000") }
    var costStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Consumable") }
    var notes by remember { mutableStateOf("") }
    var isHistoricalReplaced by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Track Component / Part",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BackdatePickerField(
                    selectedTimestamp = selectedDateMillis,
                    onDateSelected = { selectedDateMillis = it },
                    label = "Installed Date (Today or Backdate)",
                    testTagPrefix = "part_date"
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Part Name") },
                    placeholder = { Text("e.g. Engine Oil, Front Tire, Drive Chain") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("part_name_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("part_brand_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = partNumber,
                        onValueChange = { partNumber = it },
                        label = { Text("Part #") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("part_number_input"),
                        singleLine = true
                    )
                }

                OdometerInputFieldWithScanner(
                    value = odoStr,
                    onValueChange = { odoStr = it },
                    label = "Installed Odometer (km)",
                    unit = "km",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "part_installed_odo_input"
                )

                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { intervalStr = it },
                    label = { Text("Lifespan Interval (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("part_interval_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Part Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("part_cost_input"),
                    singleLine = true
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
                            text = "Archive straight to Replaced History without wear alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isHistoricalReplaced,
                        onCheckedChange = { isHistoricalReplaced = it },
                        modifier = Modifier.testTag("part_already_replaced_switch")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Specifications") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("part_notes_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odoStr.toDoubleOrNull() ?: currentOdometer
                    val interval = intervalStr.toDoubleOrNull() ?: 5000.0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            partNumber,
                            brand,
                            selectedDateMillis,
                            odo,
                            cost,
                            interval,
                            category,
                            notes,
                            isHistoricalReplaced
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                modifier = Modifier.testTag("save_part_button")
            ) {
                Text("Save Part", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EditProfileDialog(
    profile: com.example.data.model.Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        make: String,
        model: String,
        year: Int,
        vin: String,
        licensePlate: String,
        currentOdometer: Double,
        fuelCapacity: Double,
        purchaseDateTimestamp: Long?,
        purchaseDatePrecision: String
    ) -> Unit,
    onUpdateDocuments: ((
        taxTokenNumber: String,
        taxTokenExpiryDays: Int?,
        registrationNumber: String,
        registrationExpiryDays: Int?,
        driverLicenseNumber: String,
        driverLicenseExpiryDays: Int?
    ) -> Unit)? = null,
    onOpenFeedback: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(profile.name) }
    var make by remember { mutableStateOf(profile.make) }
    var model by remember { mutableStateOf(profile.model) }
    var selectedYear by remember { mutableIntStateOf(profile.year.coerceAtLeast(1950)) }
    var purchaseDateTimestamp by remember { mutableStateOf(profile.purchaseDateTimestamp) }
    var purchaseDatePrecision by remember {
        mutableStateOf(PurchaseDatePrecision.fromString(profile.purchaseDatePrecision))
    }
    var vin by remember { mutableStateOf(profile.vin) }
    var licensePlate by remember { mutableStateOf(profile.licensePlate) }
    var odoStr by remember { mutableStateOf(profile.currentOdometer.toInt().toString()) }
    var fuelCapStr by remember { mutableStateOf(profile.fuelCapacityLiters.toString()) }

    val dayMs = 86400000L
    val now = System.currentTimeMillis()

    var taxNo by remember { mutableStateOf(profile.taxTokenNumber) }
    var taxDaysStr by remember {
        val days = profile.taxTokenExpiryTimestamp?.let { (it - now) / dayMs } ?: 365
        mutableStateOf(days.toString())
    }

    var regNo by remember { mutableStateOf(profile.registrationNumber) }
    var regDaysStr by remember {
        val days = profile.registrationExpiryTimestamp?.let { (it - now) / dayMs } ?: 365
        mutableStateOf(days.toString())
    }

    var dlNo by remember { mutableStateOf(profile.driverLicenseNumber) }
    var dlDaysStr by remember {
        val days = profile.driverLicenseExpiryTimestamp?.let { (it - now) / dayMs } ?: 365
        mutableStateOf(days.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Vehicle & License Papers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vehicle Nickname") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_nickname_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = make,
                        onValueChange = { make = it },
                        label = { Text("Make") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_make_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_model_input"),
                        singleLine = true
                    )
                }

                ModelYearDropdownField(
                    selectedYear = selectedYear,
                    onYearSelected = { selectedYear = it },
                    label = "Model Year",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_year_dropdown"
                )

                PurchaseDatePickerField(
                    selectedTimestamp = purchaseDateTimestamp,
                    precision = purchaseDatePrecision,
                    onDateSelected = { ts, prec ->
                        purchaseDateTimestamp = ts
                        purchaseDatePrecision = prec
                    },
                    label = "Purchased On (Optional)",
                    allowClear = true,
                    testTagPrefix = "profile_purchase_date"
                )

                OdometerInputFieldWithScanner(
                    value = odoStr,
                    onValueChange = { odoStr = it },
                    label = "Current Odometer (km)",
                    unit = "km",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_odometer_input"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it },
                        label = { Text("Number Plate") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_plate_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fuelCapStr,
                        onValueChange = { fuelCapStr = it },
                        label = { Text("Tank Capacity (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_fuel_cap_input"),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "EXPIRY & DOCUMENT REGISTRATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MotoOrange
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taxNo,
                        onValueChange = { taxNo = it },
                        label = { Text("Tax Token #") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = taxDaysStr,
                        onValueChange = { taxDaysStr = it },
                        label = { Text("Tax Exp (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = regNo,
                        onValueChange = { regNo = it },
                        label = { Text("Registration #") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = regDaysStr,
                        onValueChange = { regDaysStr = it },
                        label = { Text("Reg Exp (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dlNo,
                        onValueChange = { dlNo = it },
                        label = { Text("Driver License #") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dlDaysStr,
                        onValueChange = { dlDaysStr = it },
                        label = { Text("License Exp (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it },
                    label = { Text("VIN Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_vin_input"),
                    singleLine = true
                )

                onOpenFeedback?.let { openFeedback ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            openFeedback()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_feedback_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send App Feedback or Bug Report", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odoStr.toDoubleOrNull() ?: profile.currentOdometer
                    val cap = fuelCapStr.toDoubleOrNull() ?: profile.fuelCapacityLiters
                    onConfirm(
                        name,
                        make,
                        model,
                        selectedYear,
                        vin,
                        licensePlate,
                        odo,
                        cap,
                        purchaseDateTimestamp,
                        purchaseDatePrecision.name
                    )

                    onUpdateDocuments?.invoke(
                        taxNo,
                        taxDaysStr.toIntOrNull(),
                        regNo,
                        regDaysStr.toIntOrNull(),
                        dlNo,
                        dlDaysStr.toIntOrNull()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Vehicle & Papers", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}


@Composable
fun ReplacePartDialog(
    partItem: PartItem,
    currentOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (cost: Double, odometer: Double) -> Unit
) {
    var costStr by remember { mutableStateOf(partItem.cost.toString()) }
    var odoStr by remember { mutableStateOf(currentOdometer.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mark Part as Replaced",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Replacing: ${partItem.name} (${partItem.brand})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OdometerInputFieldWithScanner(
                    value = odoStr,
                    onValueChange = { odoStr = it },
                    label = "Odometer at Replacement (km)",
                    unit = "km",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("New Part Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odoStr.toDoubleOrNull() ?: currentOdometer
                    val cost = costStr.toDoubleOrNull() ?: partItem.cost
                    onConfirm(cost, odo)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange)
            ) {
                Text("Confirm Replacement", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
