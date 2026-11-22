package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PurchaseDatePrecision
import com.example.data.model.PurchaseDateUtils
import com.example.ui.theme.MotoOrange
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelYearDropdownField(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    label: String = "Model Year",
    minYear: Int = 1950,
    maxYear: Int = Calendar.getInstance().get(Calendar.YEAR) + 1,
    modifier: Modifier = Modifier,
    testTag: String = "model_year_dropdown"
) {
    var expanded by remember { mutableStateOf(false) }
    val years = remember(minYear, maxYear) {
        (maxYear downTo minYear).toList()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedYear.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MotoOrange
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .testTag(testTag)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            years.forEach { yearOption ->
                val isSelected = yearOption == selectedYear
                DropdownMenuItem(
                    text = {
                        Text(
                            text = yearOption.toString(),
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) MotoOrange else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected year",
                                tint = MotoOrange
                            )
                        }
                    },
                    onClick = {
                        onYearSelected(yearOption)
                        expanded = false
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("${testTag}_option_$yearOption")
                )
            }
        }
    }
}

@Composable
fun PurchaseDatePickerField(
    selectedTimestamp: Long?,
    precision: PurchaseDatePrecision,
    onDateSelected: (timestamp: Long?, precision: PurchaseDatePrecision) -> Unit,
    label: String = "Purchased On (Optional)",
    allowClear: Boolean = true,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "purchase_date"
) {
    var showDialog by remember { mutableStateOf(false) }

    val formattedValue = remember(selectedTimestamp, precision) {
        PurchaseDateUtils.formatPurchaseDate(selectedTimestamp, precision)
    }
    val hasValue = selectedTimestamp != null && selectedTimestamp > 0L

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (hasValue) {
            MotoOrange.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (hasValue) MotoOrange.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { showDialog = true }
            .testTag("${testTagPrefix}_picker_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select Purchase Date",
                    tint = if (hasValue) MotoOrange else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formattedValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasValue) MotoOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (hasValue) precision.badgeLabel else "NOT SET",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasValue) MotoOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = { showDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = if (hasValue) "Change" else "Select",
                    fontWeight = FontWeight.Bold,
                    color = MotoOrange
                )
            }
        }
    }

    if (showDialog) {
        PurchaseDatePickerDialog(
            initialTimestamp = selectedTimestamp,
            initialPrecision = precision,
            allowClear = allowClear,
            onDismiss = { showDialog = false },
            onConfirm = { newTimestamp, newPrecision ->
                onDateSelected(newTimestamp, newPrecision)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDatePickerDialog(
    initialTimestamp: Long?,
    initialPrecision: PurchaseDatePrecision,
    allowClear: Boolean = true,
    minYear: Int = 1950,
    onDismiss: () -> Unit,
    onConfirm: (timestamp: Long?, precision: PurchaseDatePrecision) -> Unit
) {
    val todayCal = remember { Calendar.getInstance() }
    val currentYear = remember { todayCal.get(Calendar.YEAR) }
    val currentMonth = remember { todayCal.get(Calendar.MONTH) }

    val seedCal = remember(initialTimestamp) {
        Calendar.getInstance().apply {
            if (initialTimestamp != null && initialTimestamp > 0L) {
                timeInMillis = initialTimestamp
            }
        }
    }

    var draftPrecision by remember { mutableStateOf(initialPrecision) }
    var draftYear by remember {
        mutableIntStateOf(seedCal.get(Calendar.YEAR).coerceIn(minYear, currentYear))
    }
    var draftMonth by remember {
        mutableIntStateOf(seedCal.get(Calendar.MONTH).coerceIn(0, 11))
    }
    var draftDay by remember {
        mutableIntStateOf(seedCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, 31))
    }

    val yearsDescending = remember(minYear, currentYear) {
        (currentYear downTo minYear).toList()
    }
    val shortMonths = remember {
        DateFormatSymbols(Locale.US).shortMonths.take(12)
    }

    // Clamp month if user switches to currentYear and previously picked a later month
    LaunchedEffect(draftYear) {
        if (draftYear == currentYear && draftMonth > currentMonth) {
            draftMonth = currentMonth
        }
    }

    val draftPreviewTimestamp = remember(draftPrecision, draftYear, draftMonth, draftDay) {
        when (draftPrecision) {
            PurchaseDatePrecision.FULL_DATE -> PurchaseDateUtils.normalizeFullDateMillis(draftYear, draftMonth, draftDay)
            PurchaseDatePrecision.MONTH_YEAR -> PurchaseDateUtils.normalizeMonthYearMillis(draftYear, draftMonth)
            PurchaseDatePrecision.YEAR_ONLY -> PurchaseDateUtils.normalizeYearOnlyMillis(draftYear)
        }
    }

    val formattedPreview = remember(draftPreviewTimestamp, draftPrecision) {
        PurchaseDateUtils.formatPurchaseDate(draftPreviewTimestamp, draftPrecision)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("purchase_date_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MotoOrange
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Purchase Date",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Choose Full Date, Month & Year, or Year Only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("purchase_date_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Purchase Date Dialog"
                        )
                    }
                }

                // Precision Mode Tabs
                val selectedTabIdx = draftPrecision.ordinal
                PrimaryTabRow(selectedTabIndex = selectedTabIdx) {
                    PurchaseDatePrecision.entries.forEach { mode ->
                        Tab(
                            selected = draftPrecision == mode,
                            onClick = { draftPrecision = mode },
                            text = {
                                Text(
                                    text = mode.tabLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("purchase_tab_${mode.name.lowercase(Locale.US)}")
                        )
                    }
                }

                // Live Preview Pill
                Surface(
                    color = MotoOrange.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selection: $formattedPreview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MotoOrange.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = draftPrecision.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MotoOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Body per active tab
                when (draftPrecision) {
                    PurchaseDatePrecision.FULL_DATE -> {
                        val todayEndUtc = remember { localMillisToUtcPickerMillis(System.currentTimeMillis()) }
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = localMillisToUtcPickerMillis(draftPreviewTimestamp),
                            selectableDates = object : SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                    return utcTimeMillis <= todayEndUtc + 86400000L
                                }

                                override fun isSelectableYear(year: Int): Boolean {
                                    return year in minYear..currentYear
                                }
                            }
                        )

                        LaunchedEffect(datePickerState.selectedDateMillis) {
                            datePickerState.selectedDateMillis?.let { utcMillis ->
                                val localMillis = utcPickerMillisToLocalMillis(utcMillis)
                                val cal = Calendar.getInstance().apply { timeInMillis = localMillis }
                                draftYear = cal.get(Calendar.YEAR).coerceIn(minYear, currentYear)
                                draftMonth = cal.get(Calendar.MONTH).coerceIn(0, 11)
                                draftDay = cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, 31)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            DatePicker(
                                state = datePickerState,
                                title = null,
                                headline = null,
                                showModeToggle = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    PurchaseDatePrecision.MONTH_YEAR -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Year selector row with stepper + dropdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { if (draftYear > minYear) draftYear -= 1 },
                                    enabled = draftYear > minYear,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("purchase_month_year_prev_year")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous Year"
                                    )
                                }

                                ModelYearDropdownField(
                                    selectedYear = draftYear,
                                    onYearSelected = { draftYear = it },
                                    label = "Purchase Year",
                                    minYear = minYear,
                                    maxYear = currentYear,
                                    modifier = Modifier.weight(1f),
                                    testTag = "purchase_month_year_dropdown"
                                )

                                FilledTonalIconButton(
                                    onClick = { if (draftYear < currentYear) draftYear += 1 },
                                    enabled = draftYear < currentYear,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("purchase_month_year_next_year")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next Year"
                                    )
                                }
                            }

                            Text(
                                text = "SELECT MONTH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )

                            // 4x3 Month Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (rowIdx in 0 until 3) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (colIdx in 0 until 4) {
                                            val monthIdx = rowIdx * 4 + colIdx
                                            val isFutureMonth = draftYear == currentYear && monthIdx > currentMonth
                                            val isSelected = draftMonth == monthIdx && !isFutureMonth

                                            Surface(
                                                onClick = {
                                                    if (!isFutureMonth) {
                                                        draftMonth = monthIdx
                                                    }
                                                },
                                                enabled = !isFutureMonth,
                                                shape = RoundedCornerShape(12.dp),
                                                color = when {
                                                    isSelected -> MotoOrange
                                                    isFutureMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .testTag("purchase_month_cell_$monthIdx")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = shortMonths[monthIdx],
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isFutureMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        },
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PurchaseDatePrecision.YEAR_ONLY -> {
                        val initialIndex = remember(yearsDescending, draftYear) {
                            yearsDescending.indexOf(draftYear).coerceAtLeast(0)
                        }
                        val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SELECT PURCHASE YEAR ($minYear – $currentYear)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                state = gridState,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(272.dp)
                                    .testTag("purchase_year_only_grid")
                            ) {
                                items(yearsDescending) { yr ->
                                    val isSelected = yr == draftYear
                                    Surface(
                                        onClick = { draftYear = yr },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MotoOrange else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("purchase_year_cell_$yr")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = yr.toString(),
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Row
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (allowClear) {
                        TextButton(
                            onClick = { onConfirm(null, PurchaseDatePrecision.FULL_DATE) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("purchase_date_clear_button")
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onConfirm(draftPreviewTimestamp, draftPrecision)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("purchase_date_apply_button")
                        ) {
                            Text("Apply Date", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
