package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PartWearCard
import com.example.ui.components.ReplacePartDialog
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import com.example.ui.viewmodel.MotoViewModel
import com.example.ui.viewmodel.PartWearStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsManagerScreen(
    viewModel: MotoViewModel,
    onOpenAddPart: () -> Unit,
    onOpenBacklog: (() -> Unit)? = null
) {
    val partsWearList by viewModel.partsWearStatusList.collectAsState()
    val allParts by viewModel.partItems.collectAsState()
    val profile by viewModel.profile.collectAsState()

    val replacedParts = remember(allParts) {
        allParts.filter { it.isReplaced }
    }

    var selectedPartToReplace by remember { mutableStateOf<PartWearStatus?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddPart,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Part") },
                text = { Text("Track New Part", fontWeight = FontWeight.Bold) },
                containerColor = MotoOrange,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("fab_add_part")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("parts_manager_screen")
        ) {
            // Tab Header
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Parts (${partsWearList.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("parts_tab_active")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Replaced History (${replacedParts.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("parts_tab_replaced")
                )
            }

            if (onOpenBacklog != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Have past part installations or replacements?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onOpenBacklog,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("parts_open_backlog_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Add Past Parts Backlog",
                                tint = MotoOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Backlog",
                                fontWeight = FontWeight.Bold,
                                color = MotoOrange
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // ACTIVE PARTS & WEAR STATUS
                if (partsWearList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Parts Currently Tracked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ Track New Part' to start monitoring oil, tire, chain, or brake pad lifespans.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(partsWearList) { status ->
                            PartWearCard(
                                status = status,
                                onReplaceClick = { selectedPartToReplace = status },
                                onDeleteClick = { viewModel.deletePartItem(status.part) }
                            )
                        }
                    }
                }
            } else {
                // REPLACED / HISTORICAL PARTS
                if (replacedParts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Replaced Parts History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When you replace worn parts, past entries will be archived here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(replacedParts) { part ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
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
                                            text = part.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${part.brand} ${part.partNumber} • Replaced at ${part.installedOdometer.toInt()} km",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deletePartItem(part) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Part Entry",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

    selectedPartToReplace?.let { status ->
        ReplacePartDialog(
            partItem = status.part,
            currentOdometer = profile.currentOdometer,
            onDismiss = { selectedPartToReplace = null },
            onConfirm = { cost, odo ->
                viewModel.replacePart(status.part, cost, odo)
                selectedPartToReplace = null
            }
        )
    }
}
