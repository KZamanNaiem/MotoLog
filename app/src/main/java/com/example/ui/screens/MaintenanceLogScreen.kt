package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.MaintenanceAlertBanner
import com.example.ui.components.MaintenanceNotificationSettingsDialog
import com.example.ui.components.ServiceLogCard
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import com.example.ui.viewmodel.MotoViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogScreen(
    viewModel: MotoViewModel,
    onOpenAddService: () -> Unit
) {
    val serviceLogs by viewModel.serviceLogs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val maintenanceAlerts by viewModel.maintenanceAlerts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Major", "Oil", "Tires & Brakes", "DIY", "Repairs")

    val filteredLogs = remember(serviceLogs, searchQuery, selectedCategoryFilter) {
        serviceLogs.filter { log ->
            val matchesSearch = searchQuery.isBlank() ||
                    log.serviceType.contains(searchQuery, ignoreCase = true) ||
                    log.partsServiced.contains(searchQuery, ignoreCase = true) ||
                    log.partsChanged.contains(searchQuery, ignoreCase = true) ||
                    log.serviceProvider.contains(searchQuery, ignoreCase = true) ||
                    log.notes.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedCategoryFilter) {
                "Major" -> log.serviceType.contains("Major", ignoreCase = true) || log.serviceType.contains("Service", ignoreCase = true)
                "Oil" -> log.serviceType.contains("Oil", ignoreCase = true) || log.partsChanged.contains("Oil", ignoreCase = true)
                "Tires & Brakes" -> log.serviceType.contains("Tire", ignoreCase = true) || log.serviceType.contains("Brake", ignoreCase = true) || log.partsServiced.contains("Brake", ignoreCase = true)
                "DIY" -> log.serviceProvider.contains("DIY", ignoreCase = true) || log.serviceProvider.contains("Self", ignoreCase = true)
                "Repairs" -> log.serviceType.contains("Repair", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val totalFilteredCost = filteredLogs.sumOf { it.totalCost }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddService,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Service") },
                text = { Text("Log Service", fontWeight = FontWeight.Bold) },
                containerColor = ServiceBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("fab_add_service_log")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("maintenance_log_screen")
        ) {
            // Header Stats Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
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
                            text = "Maintenance & Service History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredLogs.size} logs recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { showNotificationSettingsDialog = true },
                            modifier = Modifier.testTag("btn_open_maintenance_notifications")
                        ) {
                            Badge(
                                containerColor = if (maintenanceAlerts.isNotEmpty()) MotoOrange else Color.Transparent
                            ) {
                                Icon(
                                    imageVector = if (maintenanceAlerts.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = "Maintenance Notification Settings",
                                    tint = if (maintenanceAlerts.isNotEmpty()) MotoOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalFilteredCost)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MotoOrange
                            )
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Approaching / Overdue Maintenance Alert Banner
            if (maintenanceAlerts.isNotEmpty()) {
                MaintenanceAlertBanner(
                    alerts = maintenanceAlerts,
                    onOpenNotificationSettings = { showNotificationSettingsDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Search Bar & Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search parts serviced, replaced, or provider...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_search_bar")
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = (selectedCategoryFilter == category),
                            onClick = { selectedCategoryFilter = category },
                            label = { Text(category, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ServiceBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_chip_$category")
                        )
                    }
                }
            }

            // Logs List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Service Logs Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing your search query or add a new maintenance record.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs) { log ->
                        ServiceLogCard(
                            serviceLog = log,
                            onDeleteClick = { viewModel.deleteServiceLog(log) }
                        )
                    }
                }
            }
        }
    }

    if (showNotificationSettingsDialog) {
        MaintenanceNotificationSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationSettingsDialog = false }
        )
    }
}
