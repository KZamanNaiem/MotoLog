package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FuelLog
import com.example.data.model.ServiceLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.PartWearStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceLogCard(
    serviceLog: ServiceLog,
    onDeleteClick: () -> Unit,
    currencySymbol: String = "$",
    distanceUnit: String = "km",
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(serviceLog.dateTimestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_card_${serviceLog.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header: Service Type & Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ServiceBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Service",
                            tint = ServiceBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = serviceLog.serviceType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$formattedDate • ${serviceLog.odometer.toInt()} $distanceUnit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%.2f", serviceLog.totalCost)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MotoOrange
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_service_${serviceLog.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Service Log",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (serviceLog.serviceProvider.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = serviceLog.serviceProvider,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // --- PARTS SERVICED SECTION ---
            if (serviceLog.partsServiced.isNotBlank()) {
                Text(
                    text = "PARTS / ITEMS SERVICED:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ServiceBlue,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val servicedList = serviceLog.partsServiced.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                    servicedList.forEach { item ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ServiceBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = ServiceBlue.copy(alpha = 0.1f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // --- PARTS CHANGED / REPLACED SECTION ---
            if (serviceLog.partsChanged.isNotBlank()) {
                Text(
                    text = "PARTS REPLACED / CHANGED:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MotoOrange,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val changedList = serviceLog.partsChanged.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                    changedList.forEach { item ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = MotoOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MotoOrange.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            if (serviceLog.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = serviceLog.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FuelLogCard(
    fuelLog: FuelLog,
    onDeleteClick: () -> Unit,
    currencySymbol: String = "$",
    distanceUnit: String = "km",
    volumeUnit: String = "L",
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(fuelLog.dateTimestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fuel_card_${fuelLog.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FuelGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = "Fuel Top Up",
                        tint = FuelGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${fuelLog.litersFilled} $volumeUnit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (fuelLog.isFullTank) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = FuelGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Full Tank",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FuelGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "$formattedDate • ${fuelLog.odometer.toInt()} $distanceUnit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fuelLog.stationName.isNotBlank() || fuelLog.fuelType.isNotBlank()) {
                        Text(
                            text = listOf(fuelLog.fuelType, fuelLog.stationName).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.2f", fuelLog.totalCost)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = FuelGreen
                )
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.2f", fuelLog.pricePerLiter)}/$volumeUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_fuel_${fuelLog.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Fuel Entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PartWearCard(
    status: PartWearStatus,
    onReplaceClick: () -> Unit,
    onDeleteClick: () -> Unit,
    distanceUnit: String = "km",
    modifier: Modifier = Modifier
) {
    val part = status.part
    val healthPercentInt = (status.healthPercentage * 100).toInt()

    val progressColor = when {
        status.isOverdue -> AlertRed
        status.healthPercentage < 0.25f -> WarningYellow
        else -> FuelGreen
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("part_wear_card_${part.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${part.brand} ${if (part.partNumber.isNotBlank()) "(${part.partNumber})" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = progressColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(progressColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (status.isOverdue) "Overdue" else "$healthPercentInt% Life",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { status.healthPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Used: ${status.currentKmUsed.toInt()} / ${part.recommendedIntervalKm.toInt()} $distanceUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (status.isOverdue) "Overdue by ${(-status.remainingKm).toInt()} $distanceUnit" else "${status.remainingKm.toInt()} $distanceUnit remaining",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (status.isOverdue) AlertRed else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_part_${part.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Part",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onReplaceClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("replace_part_button_${part.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mark Replaced",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
