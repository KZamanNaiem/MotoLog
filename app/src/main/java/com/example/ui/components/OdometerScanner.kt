package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiOdometerResult
import com.example.ai.OdometerAiExtractor
import com.example.ui.theme.FuelGreen
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

data class OdoCandidate(
    val value: Double,
    val label: String,
    val confidence: String,
    val isPrimary: Boolean = false
)

data class SampleMeter(
    val name: String,
    val odoValue: Double,
    val type: String,
    val displayColor: androidx.compose.ui.graphics.Color
)

val SampleMeterPresets = listOf(
    SampleMeter("Digital LCD", 24850.0, "Yamaha MT-07 Cluster", androidx.compose.ui.graphics.Color(0xFF00E5FF)),
    SampleMeter("Modern TFT", 14320.0, "BMW GS 1250 Display", androidx.compose.ui.graphics.Color(0xFFFFB300)),
    SampleMeter("Analog Roll", 38920.0, "Classic Honda CB Analog", androidx.compose.ui.graphics.Color(0xFF76FF03)),
    SampleMeter("Scooter ODO", 8450.0, "Vespa 300 Dashboard", androidx.compose.ui.graphics.Color(0xFFFF5252)),
    SampleMeter("Trip & ODO", 52100.0, "Kawasaki Ninja Meter", androidx.compose.ui.graphics.Color(0xFF00E676))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdometerScannerDialog(
    initialValue: Double = 0.0,
    unit: String = "km",
    onDismiss: () -> Unit,
    onApplyReading: (Double) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var detectedOdometer by remember { mutableStateOf(if (initialValue > 0) initialValue else 24850.0) }
    var detectedCandidates by remember { mutableStateOf<List<OdoCandidate>>(emptyList()) }
    var manualEditValue by remember { mutableStateOf(if (initialValue > 0) initialValue.toInt().toString() else "24850") }
    var selectedPresetName by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("Take a photo of your meter or pick an image") }
    var aiBadgeText by remember { mutableStateOf<String?>("Gemini AI Vision Ready") }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedPresetName = null
            isProcessing = true
            statusMessage = "Analyzing cluster photo with AI..."
            coroutineScope.launch {
                val aiResult = OdometerAiExtractor.extractMileage(bitmap, unit)
                detectedOdometer = aiResult.odometer
                detectedCandidates = aiResult.candidates
                manualEditValue = aiResult.odometer.toInt().toString()
                statusMessage = if (aiResult.isAiPowered) {
                    "Gemini AI extracted ${aiResult.odometer.toInt()} $unit (${aiResult.meterType})"
                } else {
                    "Smart OCR recognized ${aiResult.odometer.toInt()} $unit (${aiResult.meterType})"
                }
                aiBadgeText = if (aiResult.isAiPowered) "✨ Powered by Gemini AI" else "⚡ Smart On-Device Vision"
                isProcessing = false
            }
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            capturedBitmap = bitmap
                            selectedPresetName = null
                            isProcessing = true
                            statusMessage = "Analyzing gallery photo with AI..."
                        }
                    }
                    if (bitmap != null) {
                        val aiResult = OdometerAiExtractor.extractMileage(bitmap, unit)
                        withContext(Dispatchers.Main) {
                            detectedOdometer = aiResult.odometer
                            detectedCandidates = aiResult.candidates
                            manualEditValue = aiResult.odometer.toInt().toString()
                            statusMessage = if (aiResult.isAiPowered) {
                                "Gemini AI extracted ${aiResult.odometer.toInt()} $unit (${aiResult.meterType})"
                            } else {
                                "Smart OCR recognized ${aiResult.odometer.toInt()} $unit"
                            }
                            aiBadgeText = if (aiResult.isAiPowered) "✨ Powered by Gemini AI" else "⚡ Smart On-Device Vision"
                            isProcessing = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "Could not load image: ${e.message}"
                        isProcessing = false
                    }
                }
            }
        }
    }

    // Scanning laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MotoOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AI Odometer Scanner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MotoOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MotoOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Camera photo capture with AI mileage recognition",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Action Buttons to Snap Photo or Choose
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            cameraLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("scan_take_photo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("scan_pick_gallery_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                // Sample Preset Selector (Great for instant testing & all meter types)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "OR CHOOSE SAMPLE METER PRESET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SampleMeterPresets) { preset ->
                            val isSelected = selectedPresetName == preset.name
                            Surface(
                                onClick = {
                                    selectedPresetName = preset.name
                                    detectedOdometer = preset.odoValue
                                    manualEditValue = preset.odoValue.toInt().toString()
                                    detectedCandidates = listOf(
                                        OdoCandidate(preset.odoValue, "TOTAL ODOMETER", "High Confidence (99%)", true),
                                        OdoCandidate(142.5, "TRIP A", "92% Confidence")
                                    )
                                    statusMessage = "Loaded ${preset.name} (${preset.type})"
                                    aiBadgeText = "Simulated ${preset.type}"
                                    // Generate sample mock bitmap visualization
                                    capturedBitmap = createMockMeterBitmap(preset.odoValue, preset.name)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MotoOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.5.dp, MotoOrange) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = preset.displayColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${preset.name} (${preset.odoValue.toInt()} $unit)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Camera / Meter Photo Viewfinder & Scanner HUD
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                    border = BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color(0xFF333333)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Meter Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // Default Camera Placeholder HUD
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MotoOrange.copy(alpha = 0.8f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Point camera at vehicle Odometer / Cluster",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Supports Digital LCD, TFT screens, and Analog dials",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        if (isProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = MotoOrange)
                                    Text(
                                        text = "AI analyzing meter digits...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Scanning Laser Line Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.04f)
                                .align(Alignment.TopCenter)
                                .offset(y = (180 * laserYOffset).dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            MotoOrange.copy(alpha = 0.2f),
                                            MotoOrange,
                                            MotoOrange.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )

                        // Targeting Reticle & ODO Bounding Box
                        Box(
                            modifier = Modifier
                                .size(width = 220.dp, height = 70.dp)
                                .border(
                                    width = 2.dp,
                                    color = FuelGreen,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ODO TARGET AREA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FuelGreen,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${manualEditValue.ifBlank { "0" }} $unit",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Recognition Status Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FuelGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, FuelGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FuelGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        aiBadgeText?.let { badge ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Extracted Value Input with direct adjustment
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DETECTED ODOMETER VALUE ($unit)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = manualEditValue,
                        onValueChange = {
                            manualEditValue = it
                            it.toDoubleOrNull()?.let { num -> detectedOdometer = num }
                        },
                        label = { Text("Odometer Reading ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MotoOrange
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = unit,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_odometer_result_input")
                    )
                }

                // Multiple Candidates (e.g. Trip vs Total Odo selector)
                if (detectedCandidates.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "DETECTED METERS & TRIPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        detectedCandidates.forEach { candidate ->
                            Surface(
                                onClick = {
                                    detectedOdometer = candidate.value
                                    manualEditValue = candidate.value.toInt().toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (manualEditValue == candidate.value.toInt().toString()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (candidate.isPrimary) Icons.Default.Verified else Icons.Default.Timeline,
                                            contentDescription = null,
                                            tint = if (candidate.isPrimary) FuelGreen else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${candidate.value.toInt()} $unit (${candidate.label})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = candidate.confidence,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalVal = manualEditValue.toDoubleOrNull() ?: detectedOdometer
                    onApplyReading(finalVal)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FuelGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("apply_scanned_odometer_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Apply ${manualEditValue.ifBlank { detectedOdometer.toInt().toString() }} $unit",
                    fontWeight = FontWeight.Bold
                )
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

/**
 * Reusable Odometer Input Field with an embedded Camera/Scanner Trigger Button
 * that launches a photo capture intent to read and extract mileage values using AI.
 */
@Composable
fun OdometerInputFieldWithScanner(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Odometer (km)",
    unit: String = "km",
    modifier: Modifier = Modifier,
    testTag: String = "odometer_input",
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var showScannerDialog by remember { mutableStateOf(false) }
    var isProcessingDirectPhoto by remember { mutableStateOf(false) }
    var directStatusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Direct Camera intent launcher from the camera icon button
    val directCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isProcessingDirectPhoto = true
            directStatusMessage = "AI reading meter photo..."
            coroutineScope.launch {
                val aiResult = OdometerAiExtractor.extractMileage(bitmap, unit)
                onValueChange(aiResult.odometer.toInt().toString())
                directStatusMessage = "✨ AI Extracted ${aiResult.odometer.toInt()} $unit"
                isProcessingDirectPhoto = false
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = leadingIcon ?: {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MotoOrange
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    if (isProcessingDirectPhoto) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(2.dp),
                            strokeWidth = 2.dp,
                            color = MotoOrange
                        )
                    } else {
                        IconButton(
                            onClick = {
                                // Trigger camera capture intent directly or open dialog
                                directCameraLauncher.launch(null)
                            },
                            modifier = Modifier.testTag("${testTag}_camera_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Capture photo to read mileage using AI",
                                tint = MotoOrange
                            )
                        }
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        // Helper Action Row with Quick Direct Capture & Full HUD Scanner Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clickable { directCameraLauncher.launch(null) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MotoOrange,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = directStatusMessage ?: "Tap camera to capture & extract ODO with AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (directStatusMessage != null) FuelGreen else MotoOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = { showScannerDialog = true },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(
                    text = "AI HUD View",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showScannerDialog) {
        val currentVal = value.toDoubleOrNull() ?: 0.0
        OdometerScannerDialog(
            initialValue = currentVal,
            unit = unit,
            onDismiss = { showScannerDialog = false },
            onApplyReading = { scannedOdo ->
                onValueChange(scannedOdo.toInt().toString())
                directStatusMessage = "✨ AI Extracted ${scannedOdo.toInt()} $unit"
                showScannerDialog = false
            }
        )
    }
}

/**
 * Creates a mock meter bitmap display for presets
 */
private fun createMockMeterBitmap(odo: Double, title: String): Bitmap {
    val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    // Background cluster dark
    paint.color = AndroidColor.rgb(20, 24, 30)
    canvas.drawRect(0f, 0f, 320f, 180f, paint)

    // Gauge circle outline
    paint.color = AndroidColor.rgb(255, 111, 0)
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawCircle(160f, 90f, 75f, paint)

    // Digital ODO text
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = AndroidColor.WHITE
    paint.textSize = 28f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("${odo.toInt()} km", 160f, 95f, paint)

    paint.color = AndroidColor.rgb(0, 229, 255)
    paint.textSize = 14f
    canvas.drawText("TOTAL ODO", 160f, 125f, paint)

    return bitmap
}
