package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.feedback.FeedbackEntry
import com.example.data.feedback.FeedbackManager
import com.example.data.model.Vehicle
import com.example.ui.theme.FuelGreen
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import com.example.ui.theme.WarningYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackDialog(
    activeVehicle: Vehicle?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = New Feedback, 1 = History
    var historyList by remember { mutableStateOf(FeedbackManager.getFeedbackHistory(context)) }

    // Form States
    var rating by remember { mutableIntStateOf(5) }
    var selectedCategory by remember { mutableStateOf("Feature Request") }
    var message by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var showDiagnosticsPreview by remember { mutableStateOf(false) }
    var isSubmittedSuccess by remember { mutableStateOf(false) }

    val categories = listOf(
        "Feature Request" to Icons.Default.Lightbulb,
        "Bug Report" to Icons.Default.BugReport,
        "Fuel Tracking" to Icons.Default.LocalGasStation,
        "Service & Parts" to Icons.Default.Build,
        "UI & Design" to Icons.Default.Palette,
        "General Feedback" to Icons.Default.ChatBubble
    )

    val ratingLabels = mapOf(
        1 to "Needs Improvement 😕",
        2 to "Fair - Found Issues 😐",
        3 to "Good - Works Well 🙂",
        4 to "Very Good - Great Experience 😊",
        5 to "Excellent - Love PitStop! 🤩"
    )

    val diagnosticInfo = remember(activeVehicle) {
        FeedbackManager.getDeviceDiagnosticString(activeVehicle)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("feedback_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MotoOrange.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
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
                                    text = "PitStop Feedback",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Help us improve your ride & maintenance tracker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("feedback_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Feedback Dialog",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Sub-tabs: Compose / History
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MotoOrange
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            isSubmittedSuccess = false
                        },
                        text = { Text("Send Feedback", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        modifier = Modifier.testTag("tab_send_feedback")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            historyList = FeedbackManager.getFeedbackHistory(context)
                            selectedTab = 1
                        },
                        text = {
                            Text(
                                text = if (historyList.isNotEmpty()) "History (${historyList.size})" else "History",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        modifier = Modifier.testTag("tab_feedback_history")
                    )
                }

                // Main Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (selectedTab == 0) {
                        if (isSubmittedSuccess) {
                            // Submission Success Screen
                            FeedbackSuccessView(
                                onSendAnother = {
                                    message = ""
                                    isSubmittedSuccess = false
                                },
                                onDismiss = onDismiss
                            )
                        } else {
                            // Form Input Screen
                            FeedbackFormView(
                                rating = rating,
                                onRatingChange = { rating = it },
                                ratingLabels = ratingLabels,
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onCategorySelect = { selectedCategory = it },
                                message = message,
                                onMessageChange = { message = it },
                                userEmail = userEmail,
                                onEmailChange = { userEmail = it },
                                includeDiagnostics = includeDiagnostics,
                                onToggleDiagnostics = { includeDiagnostics = it },
                                diagnosticInfo = diagnosticInfo,
                                showDiagnosticsPreview = showDiagnosticsPreview,
                                onToggleDiagnosticsPreview = { showDiagnosticsPreview = !showDiagnosticsPreview },
                                onSubmit = {
                                    val entry = FeedbackEntry(
                                        rating = rating,
                                        category = selectedCategory,
                                        message = message.trim(),
                                        userEmail = userEmail.trim(),
                                        deviceDiagnostics = if (includeDiagnostics) diagnosticInfo else ""
                                    )
                                    FeedbackManager.saveFeedback(context, entry)
                                    historyList = FeedbackManager.getFeedbackHistory(context)
                                    isSubmittedSuccess = true
                                    Toast.makeText(context, "Thank you! Feedback saved locally.", Toast.LENGTH_SHORT).show()
                                },
                                onSendViaEmail = {
                                    val entry = FeedbackEntry(
                                        rating = rating,
                                        category = selectedCategory,
                                        message = message.trim().ifBlank { "(No detailed text provided)" },
                                        userEmail = userEmail.trim(),
                                        deviceDiagnostics = if (includeDiagnostics) diagnosticInfo else ""
                                    )
                                    FeedbackManager.saveFeedback(context, entry)
                                    historyList = FeedbackManager.getFeedbackHistory(context)
                                    val emailIntent = FeedbackManager.createEmailIntent(entry)
                                    try {
                                        context.startActivity(emailIntent)
                                        isSubmittedSuccess = true
                                    } catch (e: Exception) {
                                        // Fallback to share intent if no email client registered
                                        val shareIntent = FeedbackManager.createShareIntent(entry)
                                        context.startActivity(Intent.createChooser(shareIntent, "Send Feedback via"))
                                        isSubmittedSuccess = true
                                    }
                                }
                            )
                        }
                    } else {
                        // History View
                        FeedbackHistoryView(
                            historyList = historyList,
                            onClearHistory = {
                                FeedbackManager.clearFeedbackHistory(context)
                                historyList = emptyList()
                                Toast.makeText(context, "Feedback history cleared", Toast.LENGTH_SHORT).show()
                            },
                            onCopyEntry = { entry ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PitStop Feedback", "${entry.category} (${entry.rating}★): ${entry.message}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Feedback copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackFormView(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    ratingLabels: Map<Int, String>,
    categories: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    userEmail: String,
    onEmailChange: (String) -> Unit,
    includeDiagnostics: Boolean,
    onToggleDiagnostics: (Boolean) -> Unit,
    diagnosticInfo: String,
    showDiagnosticsPreview: Boolean,
    onToggleDiagnosticsPreview: () -> Unit,
    onSubmit: () -> Unit,
    onSendViaEmail: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Rating Header & Star Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RATE YOUR EXPERIENCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Interactive 5 Stars
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { onRatingChange(i) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("feedback_rating_star_$i")
                        ) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "$i Stars",
                                tint = if (i <= rating) MotoOrange else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Text(
                    text = ratingLabels[rating] ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MotoOrange
                )
            }
        }

        // 2. Category Selection Chips
        Column {
            Text(
                text = "FEEDBACK CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { (cat, icon) ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MotoOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoOrange.copy(alpha = 0.2f),
                            selectedLabelColor = MotoOrange
                        ),
                        modifier = Modifier.testTag("feedback_cat_${cat.replace(" ", "_")}")
                    )
                }
            }
        }

        // 3. Message Input
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WHAT'S ON YOUR MIND?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${message.length}/1000",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.length > 900) WarningYellow else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 1000) onMessageChange(it) },
                placeholder = {
                    Text("Describe what you like, what is missing, or any issues you encountered...")
                },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_input_message"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 4. Contact / Email Input
        Column {
            Text(
                text = "YOUR EMAIL (OPTIONAL)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = userEmail,
                onValueChange = onEmailChange,
                placeholder = { Text("e.g. rider@example.com for follow-up") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_input_email"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 5. Diagnostics Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = ServiceBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Attach Diagnostics",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Includes OS version & vehicle specs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = includeDiagnostics,
                        onCheckedChange = onToggleDiagnostics,
                        modifier = Modifier.testTag("feedback_toggle_diagnostics")
                    )
                }

                if (includeDiagnostics) {
                    TextButton(
                        onClick = onToggleDiagnosticsPreview,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (showDiagnosticsPreview) "Hide Diagnostics Preview ▲" else "View Diagnostics Preview ▼",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    AnimatedVisibility(visible = showDiagnosticsPreview) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = diagnosticInfo,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 6. Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Send via Email button
            OutlinedButton(
                onClick = onSendViaEmail,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("feedback_email_button")
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Email", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // In-App Submit button
            Button(
                onClick = onSubmit,
                enabled = message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("feedback_submit_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Submit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FeedbackSuccessView(
    onSendAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = FuelGreen.copy(alpha = 0.2f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FuelGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Thank You for Your Feedback!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your thoughts and bug reports directly help us improve PitStop for the riding and driving community.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSendAnother,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Write Another")
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("feedback_done_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FeedbackHistoryView(
    historyList: List<FeedbackEntry>,
    onClearHistory: () -> Unit,
    onCopyEntry: (FeedbackEntry) -> Unit
) {
    if (historyList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No Previous Feedback Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Feedback you send will be logged here for your reference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED SUBMISSIONS (${historyList.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onClearHistory) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear History", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyList, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(item.category, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                for (s in 1..5) {
                                    Icon(
                                        imageVector = if (s <= item.rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = if (s <= item.rating) MotoOrange else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            IconButton(
                                onClick = { onCopyEntry(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Feedback",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
