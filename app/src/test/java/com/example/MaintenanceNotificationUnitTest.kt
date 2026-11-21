package com.example

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PartItem
import com.example.data.model.ServiceLog
import com.example.data.model.Vehicle
import com.example.notification.AlertUrgency
import com.example.notification.MaintenanceNotificationManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MaintenanceNotificationUnitTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val context = application

    @Test
    fun testChannelInitialization() {
        MaintenanceNotificationManager.initNotificationChannel(context)
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = notificationManager.getNotificationChannel(MaintenanceNotificationManager.CHANNEL_ID)
        assertNotNull("Maintenance notification channel should be created", channel)
        assertEquals("Maintenance & Mileage Alerts", channel.name)
    }

    @Test
    fun testEvaluateServiceLogApproachingMaintenance() {
        val vehicle = Vehicle(
            id = 1,
            name = "Yamaha MT-07",
            currentOdometer = 9850.0,
            preferredDistanceUnit = "km"
        )

        // Next service scheduled at 10,000 km -> 150 km remaining (within default 500 km threshold)
        val serviceLog = ServiceLog(
            id = 10,
            vehicleId = 1,
            dateTimestamp = 1700000000000L,
            odometer = 5000.0,
            serviceType = "Engine Oil & Filter",
            totalCost = 75.0,
            serviceProvider = "Yamaha Official",
            partsServiced = "Oil filter",
            partsChanged = "Motul 7100 10W40",
            nextServiceOdometerDue = 10000.0
        )

        val alerts = MaintenanceNotificationManager.evaluateMaintenanceAlerts(
            context = context,
            vehicle = vehicle,
            serviceLogs = listOf(serviceLog),
            partItems = emptyList()
        )

        assertEquals("Should have 1 approaching service alert", 1, alerts.size)
        val alert = alerts.first()
        assertEquals(AlertUrgency.IMMINENT, alert.urgency)
        assertTrue("Title should mention Engine Oil & Filter", alert.title.contains("Engine Oil & Filter"))
        assertEquals(150.0, alert.distanceRemaining, 0.1)
        assertFalse("Should not be overdue", alert.isOverdue)
    }

    @Test
    fun testEvaluateOverdueServiceMaintenance() {
        val vehicle = Vehicle(
            id = 1,
            name = "Yamaha MT-07",
            currentOdometer = 10120.0,
            preferredDistanceUnit = "km"
        )

        // Next service was due at 10,000 km -> overdue by 120 km
        val serviceLog = ServiceLog(
            id = 10,
            vehicleId = 1,
            dateTimestamp = 1700000000000L,
            odometer = 5000.0,
            serviceType = "Major 10k Service",
            totalCost = 150.0,
            serviceProvider = "Workshop",
            partsServiced = "Spark plugs",
            partsChanged = "Air filter",
            nextServiceOdometerDue = 10000.0
        )

        val alerts = MaintenanceNotificationManager.evaluateMaintenanceAlerts(
            context = context,
            vehicle = vehicle,
            serviceLogs = listOf(serviceLog),
            partItems = emptyList()
        )

        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertEquals(AlertUrgency.OVERDUE, alert.urgency)
        assertTrue("Should be marked as overdue", alert.isOverdue)
        assertTrue(alert.title.contains("Overdue"))
    }

    @Test
    fun testEvaluatePartWearApproachingInterval() {
        val vehicle = Vehicle(
            id = 1,
            name = "Honda Rebel 500",
            currentOdometer = 14800.0,
            preferredDistanceUnit = "km"
        )

        // Installed at 10,000 km with 5,000 km interval -> due at 15,000 km -> 200 km remaining
        val partItem = PartItem(
            id = 20,
            vehicleId = 1,
            name = "Drive Chain & Sprockets",
            installedDateTimestamp = 1700000000000L,
            installedOdometer = 10000.0,
            recommendedIntervalKm = 5000.0,
            cost = 120.0
        )

        val alerts = MaintenanceNotificationManager.evaluateMaintenanceAlerts(
            context = context,
            vehicle = vehicle,
            serviceLogs = emptyList(),
            partItems = listOf(partItem)
        )

        assertEquals("Should have 1 approaching part replacement alert", 1, alerts.size)
        val alert = alerts.first()
        assertEquals(AlertUrgency.APPROACHING, alert.urgency)
        assertTrue(alert.title.contains("Drive Chain & Sprockets"))
        assertEquals(200.0, alert.distanceRemaining, 0.1)
    }

    @Test
    fun testPostMaintenanceNotificationsWhenEnabled() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        MaintenanceNotificationManager.setNotificationEnabled(context, true)
        MaintenanceNotificationManager.setAlertThresholdKm(context, 500.0)

        val vehicle = Vehicle(
            id = 1,
            name = "Kawasaki Ninja 400",
            currentOdometer = 4900.0
        )
        val serviceLog = ServiceLog(
            id = 1,
            vehicleId = 1,
            dateTimestamp = 1700000000000L,
            odometer = 1000.0,
            serviceType = "Oil Change",
            totalCost = 50.0,
            serviceProvider = "DIY",
            partsServiced = "Oil",
            partsChanged = "Motul",
            nextServiceOdometerDue = 5000.0
        )

        val postedCount = MaintenanceNotificationManager.checkAndPostMaintenanceNotifications(
            context = context,
            vehicle = vehicle,
            serviceLogs = listOf(serviceLog),
            partItems = emptyList()
        )

        assertEquals("Should post 1 notification for approaching oil change", 1, postedCount)
    }

    @Test
    fun testMotoViewModelInitialization() {
        val viewModel = com.example.ui.viewmodel.MotoViewModel(application)
        assertNotNull("MotoViewModel should initialize cleanly", viewModel)
        assertNotNull("Active vehicle flow should not be null", viewModel.activeVehicle)
        assertNotNull("Service logs flow should not be null", viewModel.serviceLogs)
        assertNotNull("Part items flow should not be null", viewModel.partItems)
        assertNotNull("Maintenance alerts flow should not be null", viewModel.maintenanceAlerts)
    }
}
