package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PartItem
import com.example.data.model.ServiceLog
import com.example.data.model.Vehicle
import java.util.Locale
import kotlin.math.max

enum class AlertUrgency {
    APPROACHING,
    IMMINENT,
    OVERDUE
}

data class MaintenanceNotificationItem(
    val id: Int,
    val title: String,
    val description: String,
    val vehicleName: String,
    val distanceRemaining: Double,
    val distanceUnit: String,
    val urgency: AlertUrgency,
    val isOverdue: Boolean
)

object MaintenanceNotificationManager {

    const val CHANNEL_ID = "pitstop_maintenance_alerts"
    private const val CHANNEL_NAME = "Maintenance & Mileage Alerts"
    private const val CHANNEL_DESCRIPTION = "Alerts when scheduled motorcycle service or part wear intervals are approaching based on current mileage"
    private const val PREFS_NAME = "pitstop_notification_prefs"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_ALERT_THRESHOLD_KM = "alert_threshold_km"
    private const val KEY_LAST_NOTIFIED_HASH = "last_notified_hash"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.parseColor("#FF5722") // MotoOrange
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getAlertThresholdKm(context: Context): Double {
        return getPrefs(context).getFloat(KEY_ALERT_THRESHOLD_KM, 500f).toDouble()
    }

    fun setAlertThresholdKm(context: Context, thresholdKm: Double) {
        getPrefs(context).edit().putFloat(KEY_ALERT_THRESHOLD_KM, thresholdKm.toFloat()).apply()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Evaluates current mileage against scheduled services and active parts wear,
     * returning a list of actionable maintenance alerts.
     */
    fun evaluateMaintenanceAlerts(
        context: Context,
        vehicle: Vehicle,
        serviceLogs: List<ServiceLog>,
        partItems: List<PartItem>
    ): List<MaintenanceNotificationItem> {
        val alerts = mutableListOf<MaintenanceNotificationItem>()
        val thresholdKm = getAlertThresholdKm(context)
        val currentOdo = vehicle.currentOdometer
        val unit = vehicle.preferredDistanceUnit.ifBlank { "km" }

        // 1. Check Next Scheduled Service from latest Service Log
        val scheduledService = serviceLogs
            .filter { it.nextServiceOdometerDue != null && it.nextServiceOdometerDue > 0 }
            .maxByOrNull { it.dateTimestamp }

        scheduledService?.nextServiceOdometerDue?.let { nextDueOdo ->
            val remainingKm = nextDueOdo - currentOdo
            if (remainingKm <= thresholdKm) {
                val isOverdue = remainingKm <= 0
                val urgency = when {
                    isOverdue -> AlertUrgency.OVERDUE
                    remainingKm <= 150.0 -> AlertUrgency.IMMINENT
                    else -> AlertUrgency.APPROACHING
                }

                val title = if (isOverdue) {
                    "🚨 Service Overdue: ${scheduledService.serviceType.ifBlank { "Scheduled Maintenance" }}"
                } else {
                    "🔧 Maintenance Approaching: ${scheduledService.serviceType.ifBlank { "Scheduled Maintenance" }}"
                }

                val desc = if (isOverdue) {
                    val overdueBy = String.format(Locale.US, "%.0f %s", -remainingKm, unit)
                    "${vehicle.name} has passed scheduled service by $overdueBy (Target: ${String.format(Locale.US, "%.0f", nextDueOdo)} $unit)."
                } else {
                    val dueIn = String.format(Locale.US, "%.0f %s", remainingKm, unit)
                    "Due in $dueIn for ${vehicle.name} (Target: ${String.format(Locale.US, "%.0f", nextDueOdo)} $unit)."
                }

                alerts.add(
                    MaintenanceNotificationItem(
                        id = (vehicle.id * 1000 + 1).toInt(),
                        title = title,
                        description = desc,
                        vehicleName = vehicle.name,
                        distanceRemaining = remainingKm,
                        distanceUnit = unit,
                        urgency = urgency,
                        isOverdue = isOverdue
                    )
                )
            }
        }

        // 2. Check Active Tracked Parts Wear and Replacement Intervals
        val activeParts = partItems.filter { !it.isReplaced }
        for (part in activeParts) {
            val interval = if (part.recommendedIntervalKm > 0) part.recommendedIntervalKm else 5000.0
            val usedKm = max(0.0, currentOdo - part.installedOdometer)
            val remainingKm = interval - usedKm

            if (remainingKm <= thresholdKm) {
                val isOverdue = remainingKm <= 0
                val urgency = when {
                    isOverdue -> AlertUrgency.OVERDUE
                    remainingKm <= 150.0 -> AlertUrgency.IMMINENT
                    else -> AlertUrgency.APPROACHING
                }

                val title = if (isOverdue) {
                    "⚠️ Part Replacement Overdue: ${part.name}"
                } else {
                    "⚙️ Part Service Due: ${part.name}"
                }

                val desc = if (isOverdue) {
                    val overdueKm = String.format(Locale.US, "%.0f %s", -remainingKm, unit)
                    "${part.name} on ${vehicle.name} has exceeded its recommended interval by $overdueKm. Inspect or replace now."
                } else {
                    val inKm = String.format(Locale.US, "%.0f %s", remainingKm, unit)
                    "Recommended service/replacement in $inKm on ${vehicle.name} (${String.format(Locale.US, "%.0f/%.0f %s", usedKm, interval, unit)} used)."
                }

                alerts.add(
                    MaintenanceNotificationItem(
                        id = (vehicle.id * 1000 + (part.id % 900) + 10).toInt(),
                        title = title,
                        description = desc,
                        vehicleName = vehicle.name,
                        distanceRemaining = remainingKm,
                        distanceUnit = unit,
                        urgency = urgency,
                        isOverdue = isOverdue
                    )
                )
            }
        }

        return alerts
    }

    /**
     * Checks maintenance alerts against current mileage and posts system notifications
     * to the Android notification shade if enabled and permitted.
     */
    fun checkAndPostMaintenanceNotifications(
        context: Context,
        vehicle: Vehicle,
        serviceLogs: List<ServiceLog>,
        partItems: List<PartItem>
    ): Int {
        if (!isNotificationEnabled(context)) return 0
        if (!hasNotificationPermission(context)) return 0

        initNotificationChannel(context)

        val alerts = evaluateMaintenanceAlerts(context, vehicle, serviceLogs, partItems)
        if (alerts.isEmpty()) return 0

        var postedCount = 0
        val notificationManager = NotificationManagerCompat.from(context)

        alerts.forEach { alert ->
            val notification = buildMaintenanceNotification(context, alert)
            try {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                ) {
                    notificationManager.notify(alert.id, notification)
                    postedCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return postedCount
    }

    /**
     * Builds a rich Material Design Notification for an approaching maintenance interval.
     */
    fun buildMaintenanceNotification(
        context: Context,
        alert: MaintenanceNotificationItem
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", "maintenance")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = when (alert.urgency) {
            AlertUrgency.OVERDUE -> NotificationCompat.PRIORITY_MAX
            AlertUrgency.IMMINENT -> NotificationCompat.PRIORITY_HIGH
            AlertUrgency.APPROACHING -> NotificationCompat.PRIORITY_DEFAULT
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(alert.description)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(alert.description)
                    .setBigContentTitle(alert.title)
                    .setSummaryText(alert.vehicleName)
            )
            .setColor(if (alert.isOverdue) Color.parseColor("#E53935") else Color.parseColor("#FF5722"))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "Open Maintenance",
                pendingIntent
            )
            .build()
    }

    /**
     * Dispatches an instant test notification so the user can verify
     * that notifications and vibration alerts are functional.
     */
    fun sendTestNotification(context: Context, vehicle: Vehicle?): Boolean {
        if (!hasNotificationPermission(context)) return false

        initNotificationChannel(context)

        val vehicleName = vehicle?.name ?: "Motorcycle"
        val testAlert = MaintenanceNotificationItem(
            id = 9999,
            title = "🔧 Scheduled Maintenance Interval Alert",
            description = "Test reminder: Engine oil and filter inspection interval approaching for $vehicleName in 250 ${vehicle?.preferredDistanceUnit ?: "km"}.",
            vehicleName = vehicleName,
            distanceRemaining = 250.0,
            distanceUnit = vehicle?.preferredDistanceUnit ?: "km",
            urgency = AlertUrgency.APPROACHING,
            isOverdue = false
        )

        val notification = buildMaintenanceNotification(context, testAlert)
        val notificationManager = NotificationManagerCompat.from(context)

        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                notificationManager.notify(testAlert.id, notification)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
