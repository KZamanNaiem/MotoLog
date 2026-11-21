package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MotoViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsExpensesScreen(
    viewModel: MotoViewModel,
    onOpenExportData: (() -> Unit)? = null,
    onOpenAddFuel: (() -> Unit)? = null,
    onOpenAddService: (() -> Unit)? = null
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Recharts Monthly Breakdown, 1: Overview & List

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_expenses_screen")
    ) {
        // Top Sub-Navigation Tabs
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MotoOrange
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Monthly Recharts", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.BarChart, contentDescription = "Monthly Recharts Breakdown") },
                modifier = Modifier.testTag("tab_recharts_breakdown")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("All Expenses & Items", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "All Expenses & Backup") },
                modifier = Modifier.testTag("tab_expenses_overview")
            )
        }

        if (selectedSubTab == 0) {
            MonthlyRechartsScreen(
                viewModel = viewModel,
                onOpenAddFuel = onOpenAddFuel ?: {},
                onOpenAddService = onOpenAddService ?: {}
            )
        } else {
            ExpensesOverviewAndListView(
                viewModel = viewModel,
                onOpenExportData = onOpenExportData
            )
        }
    }
}

@Composable
fun ExpensesOverviewAndListView(
    viewModel: MotoViewModel,
    onOpenExportData: (() -> Unit)? = null
) {
    val expenseSummary by viewModel.expenseSummary.collectAsState()
    val expensesList by viewModel.expenses.collectAsState()
    val activeVehicle by viewModel.activeVehicle.collectAsState()
    val currency = activeVehicle.preferredCurrency

    val grandTotal = expenseSummary.totalOverallExpense
    val fuelPct = if (grandTotal > 0) (expenseSummary.fuelTotal / grandTotal).toFloat() else 0f
    val servicePct = if (grandTotal > 0) (expenseSummary.serviceTotal / grandTotal).toFloat() else 0f
    val otherPct = if (grandTotal > 0) (expenseSummary.otherExpensesTotal / grandTotal).toFloat() else 0f

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expenses_overview_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grand Total Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TOTAL VEHICLE EXPENSE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currency${String.format(Locale.US, "%.2f", grandTotal)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MotoOrange
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stacked expense bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (fuelPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(fuelPct)
                                    .fillMaxHeight()
                                    .background(FuelGreen)
                            )
                        }
                        if (servicePct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(servicePct)
                                    .fillMaxHeight()
                                    .background(ServiceBlue)
                            )
                        }
                        if (otherPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(otherPct)
                                    .fillMaxHeight()
                                    .background(WarningYellow)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Breakdown Rows
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpenseCategoryRow(
                            label = "Fuel Top-Ups",
                            amount = expenseSummary.fuelTotal,
                            percent = fuelPct * 100,
                            currency = currency,
                            color = FuelGreen
                        )
                        ExpenseCategoryRow(
                            label = "Maintenance & Service",
                            amount = expenseSummary.serviceTotal,
                            percent = servicePct * 100,
                            currency = currency,
                            color = ServiceBlue
                        )
                        ExpenseCategoryRow(
                            label = "Insurance, Mods & Accessories",
                            amount = expenseSummary.otherExpensesTotal,
                            percent = otherPct * 100,
                            currency = currency,
                            color = WarningYellow
                        )
                    }
                }
            }
        }

        // Additional Expense Items
        item {
            Text(
                text = "ADDITIONAL EXPENSES & MODS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        if (expensesList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No additional custom expenses logged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(expensesList) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = expense.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${expense.category} • ${dateFormat.format(Date(expense.dateTimestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currency${String.format(Locale.US, "%.2f", expense.cost)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MotoOrange
                            )
                            IconButton(onClick = { viewModel.deleteExpense(expense) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Expense",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Data Card
        if (onOpenExportData != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("expenses_export_backup_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MotoOrange.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = MotoOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Data Backup & Export",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Save all expenditure & logs to local JSON",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = onOpenExportData,
                                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("expenses_export_json_button")
                            ) {
                                Text("Export", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCategoryRow(
    label: String,
    amount: Double,
    percent: Float,
    currency: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "$currency${String.format(Locale.US, "%.2f", amount)} (${String.format(Locale.US, "%.0f", percent)}%)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
