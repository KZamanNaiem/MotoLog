package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.export.ExportPayloadSummary
import com.example.ui.theme.FuelGreen
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import com.example.ui.viewmodel.MotoViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataDialog(
    viewModel: MotoViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isGenerating by remember { mutableStateOf(true) }
    var jsonPayload by remember { mutableStateOf("") }
    var exportSummary by remember { mutableStateOf<ExportPayloadSummary?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showJsonPreview by remember { mutableStateOf(false) }
    var savedSuccessPath by remember { mutableStateOf<String?>(null) }
    var isSavingToDoc by remember { mutableStateOf(false) }

    val defaultFileName = remember {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        "pitstop_logs_backup_$dateStr.json"
    }

    // Storage Access Framework launcher to save directly to user-selected folder
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && jsonPayload.isNotBlank()) {
            coroutineScope.launch {
                isSavingToDoc = true
                val success = viewModel.saveExportToUri(uri, jsonPayload)
                isSavingToDoc = false
                if (success) {
                    savedSuccessPath = "Saved to selected destination successfully"
                    Toast.makeText(context, "✅ JSON backup saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    errorMessage = "Failed to write JSON to selected location."
                    Toast.makeText(context, "❌ Export failed to save file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Load export payload on entry
    LaunchedEffect(Unit) {
        try {
            val (json, summary) = viewModel.getExportJsonPayload()
            jsonPayload = json
            exportSummary = summary
            isGenerating = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to generate backup JSON"
            isGenerating = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("export_data_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export Data",
                                    tint = MotoOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Export Logs & Data",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Save to local JSON file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MotoOrange)
                            Text(
                                text = "Compiling vehicle logs & telemetry...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = errorMessage ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // Content Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Success / Status Banner
                        if (savedSuccessPath != null) {
                            Surface(
                                color = FuelGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FuelGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = FuelGreen)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Backup Successfully Saved!",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FuelGreen
                                        )
                                        Text(
                                            text = savedSuccessPath.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Summary Statistics Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DATA INCLUDED IN BACKUP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "${(jsonPayload.toByteArray(Charsets.UTF_8).size / 1024.0).let { String.format(Locale.US, "%.1f KB", it) }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MotoOrange
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                exportSummary?.let { summary ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        ExportStatChip(
                                            label = "Vehicles",
                                            count = summary.totalVehicles.toString(),
                                            icon = Icons.Default.TwoWheeler,
                                            color = MotoOrange
                                        )
                                        ExportStatChip(
                                            label = "Fuel Logs",
                                            count = summary.totalFuelLogs.toString(),
                                            icon = Icons.Default.LocalGasStation,
                                            color = FuelGreen
                                        )
                                        ExportStatChip(
                                            label = "Service",
                                            count = summary.totalServiceLogs.toString(),
                                            icon = Icons.Default.Build,
                                            color = ServiceBlue
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        ExportStatChip(
                                            label = "Parts Wear",
                                            count = summary.totalParts.toString(),
                                            icon = Icons.Default.Settings,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        ExportStatChip(
                                            label = "Expenses",
                                            count = summary.totalExpenses.toString(),
                                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        ExportStatChip(
                                            label = "Total Cost",
                                            count = "$${String.format(Locale.US, "%.0f", summary.totalExpenditure)}",
                                            icon = Icons.Default.AttachMoney,
                                            color = MotoOrange
                                        )
                                    }
                                }
                            }
                        }

                        // Target filename info
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = defaultFileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Primary Action: Save to Local Storage File
                        Button(
                            onClick = {
                                saveFileLauncher.launch(defaultFileName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_save_json_file"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrange)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save to Local JSON File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Secondary Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Quick save to app documents
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.exportDataToLocalFile()
                                        if (res.success && res.filePath != null) {
                                            savedSuccessPath = "Saved at: ${res.filePath}"
                                            Toast.makeText(context, "Saved to Documents: ${res.fileName}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed: ${res.errorMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_quick_save_docs"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auto Save", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            // Share via standard intent
                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, jsonPayload)
                                        putExtra(Intent.EXTRA_TITLE, "PitStop Vehicle Logs Backup")
                                        type = "application/json"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Logs JSON to...")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_share_json"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            // Copy JSON to Clipboard
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("PitStop JSON Export", jsonPayload)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "📋 JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_copy_json"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Inspect Raw JSON Toggle
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showJsonPreview = !showJsonPreview },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (showJsonPreview) "Hide Raw JSON Preview" else "Preview Raw JSON Payload",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = if (showJsonPreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Collapsible Raw JSON Viewer
                        AnimatedVisibility(
                            visible = showJsonPreview,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1E24))
                                    .border(1.dp, Color(0xFF33333E), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = jsonPayload,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF80CBC4),
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportStatChip(
    label: String,
    count: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(96.dp)
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
