package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.notification.AlertUrgency
import com.example.notification.MaintenanceNotificationItem
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.WarningYellow
import java.util.Locale

@Composable
fun MaintenanceAlertBanner(
    alerts: List<MaintenanceNotificationItem>,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val topAlert = alerts.first()
    val totalAlerts = alerts.size

    val bannerColor = if (alerts.any { it.isOverdue }) {
        OverdueRed
    } else if (alerts.any { it.urgency == AlertUrgency.IMMINENT }) {
        MotoOrange
    } else {
        WarningYellow
    }

    val containerBg = if (alerts.any { it.isOverdue }) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("maintenance_alert_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = bannerColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (alerts.any { it.isOverdue }) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (totalAlerts > 1) "Maintenance Due ($totalAlerts items)" else "Maintenance Approaching",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (alerts.any { it.isOverdue }) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (totalAlerts > 1) {
                                Surface(
                                    color = bannerColor,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$totalAlerts",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = topAlert.title.replace("🚨 ", "").replace("⚠️ ", "").replace("🔧 ", "").replace("⚙️ ", ""),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.testTag("banner_notification_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notification Settings",
                            tint = MotoOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (totalAlerts > 1) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.testTag("banner_expand_btn")
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Description of primary alert
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = topAlert.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expanded view if multiple alerts exist
            AnimatedVisibility(visible = isExpanded && totalAlerts > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    alerts.drop(1).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.isOverdue) OverdueRed.copy(alpha = 0.15f) else MotoOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (item.isOverdue) "OVERDUE" else "DUE SOON",
                                    color = if (item.isOverdue) OverdueRed else MotoOrange,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
