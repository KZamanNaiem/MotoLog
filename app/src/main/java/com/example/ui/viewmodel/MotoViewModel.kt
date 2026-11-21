package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.MotoRepository
import com.example.notification.AlertUrgency
import com.example.notification.MaintenanceNotificationItem
import com.example.notification.MaintenanceNotificationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class DocumentExpiryAlert(
    val vehicleId: Long,
    val vehicleName: String,
    val docType: String, // "Tax Token", "Registration Paper", "Driver's License"
    val docNumber: String,
    val expiryTimestamp: Long,
    val daysRemaining: Long,
    val isExpired: Boolean,
    val isWarning: Boolean // <= 30 days remaining
)

data class FuelStats(
    val totalFuelCost: Double = 0.0,
    val totalLitersFilled: Double = 0.0,
    val averageKmLiter: Double = 0.0, // km / L
    val averageLitersPer100Km: Double = 0.0,
    val averageMpg: Double = 0.0,
    val averagePricePerLiter: Double = 0.0,
    val costPerKm: Double = 0.0,
    val totalFuelDistanceKm: Double = 0.0
)

data class ServiceStats(
    val totalServiceCost: Double = 0.0,
    val totalServicesCount: Int = 0,
    val lastServiceDate: Long? = null,
    val lastServiceOdometer: Double? = null,
    val nextServiceOdometerDue: Double? = null,
    val kmUntilNextService: Double? = null,
    val totalPartsReplacedCount: Int = 0
)

data class PartWearStatus(
    val part: PartItem,
    val currentKmUsed: Double,
    val remainingKm: Double,
    val healthPercentage: Float, // 0.0 to 1.0 (1.0 = 100% healthy, 0.0 = overdue)
    val isOverdue: Boolean
)

data class ExpenseSummary(
    val totalOverallExpense: Double = 0.0,
    val fuelTotal: Double = 0.0,
    val serviceTotal: Double = 0.0,
    val partsTotal: Double = 0.0,
    val otherExpensesTotal: Double = 0.0
)

data class MonthlyExpenseBreakdown(
    val monthKey: String, // "2026-09"
    val month: String, // "Sep '26"
    val fullName: String, // "September 2026"
    val fuel: Double = 0.0,
    val service: Double = 0.0,
    val parts: Double = 0.0,
    val other: Double = 0.0,
    val total: Double = 0.0,
    val fuelLiters: Double = 0.0,
    val fuelEntries: Int = 0,
    val serviceEntries: Int = 0,
    val otherEntries: Int = 0
)

class MotoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val dao = database.motoDao()
    private val repository = MotoRepository(dao)
    private val dataExporter = com.example.data.export.DataExporter(dao)

    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehiclesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVehicle: StateFlow<Vehicle> = repository.activeVehicleFlow
        .map { it ?: allVehicles.value.firstOrNull() ?: Vehicle() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Vehicle())

    // Legacy profile mapping
    val profile: StateFlow<Vehicle> = activeVehicle

    // --- Document & Driver License Expiry Alerts ---
    val documentAlerts: StateFlow<List<DocumentExpiryAlert>> = combine(allVehicles, activeVehicle) { vehicles, active ->
        val list = mutableListOf<DocumentExpiryAlert>()
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        vehicles.forEach { veh ->
            // 1. Tax Token Check
            veh.taxTokenExpiryTimestamp?.let { exp ->
                val days = (exp - now) / dayMs
                val isExpired = days < 0
                val isWarning = days <= 30
                if (isExpired || isWarning) {
                    list.add(
                        DocumentExpiryAlert(
                            vehicleId = veh.id,
                            vehicleName = veh.name,
                            docType = "Tax Token",
                            docNumber = veh.taxTokenNumber.ifBlank { "N/A" },
                            expiryTimestamp = exp,
                            daysRemaining = days,
                            isExpired = isExpired,
                            isWarning = isWarning
                        )
                    )
                }
            }

            // 2. Registration Paper Check
            veh.registrationExpiryTimestamp?.let { exp ->
                val days = (exp - now) / dayMs
                val isExpired = days < 0
                val isWarning = days <= 30
                if (isExpired || isWarning) {
                    list.add(
                        DocumentExpiryAlert(
                            vehicleId = veh.id,
                            vehicleName = veh.name,
                            docType = "Registration Paper",
                            docNumber = veh.registrationNumber.ifBlank { "N/A" },
                            expiryTimestamp = exp,
                            daysRemaining = days,
                            isExpired = isExpired,
                            isWarning = isWarning
                        )
                    )
                }
            }

            // 3. Driver's License Check
            veh.driverLicenseExpiryTimestamp?.let { exp ->
                val days = (exp - now) / dayMs
                val isExpired = days < 0
                val isWarning = days <= 30
                if (isExpired || isWarning) {
                    list.add(
                        DocumentExpiryAlert(
                            vehicleId = veh.id,
                            vehicleName = veh.name,
                            docType = "Driver's License",
                            docNumber = veh.driverLicenseNumber.ifBlank { "N/A" },
                            expiryTimestamp = exp,
                            daysRemaining = days,
                            isExpired = isExpired,
                            isWarning = isWarning
                        )
                    )
                }
            }
        }
        list.sortedBy { it.daysRemaining }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered logs by active vehicle ID
    val fuelLogs: StateFlow<List<FuelLog>> = combine(activeVehicle, repository.fuelLogsFlow) { active, allLogs ->
        allLogs.filter { it.vehicleId == active.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceLogs: StateFlow<List<ServiceLog>> = combine(activeVehicle, repository.serviceLogsFlow) { active, allLogs ->
        allLogs.filter { it.vehicleId == active.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partItems: StateFlow<List<PartItem>> = combine(activeVehicle, repository.partItemsFlow) { active, allParts ->
        allParts.filter { it.vehicleId == active.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseItem>> = combine(activeVehicle, repository.expensesFlow) { active, allExp ->
        allExp.filter { it.vehicleId == active.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Calculated Fuel Stats ---
    val fuelStats: StateFlow<FuelStats> = combine(fuelLogs, activeVehicle) { logs, veh ->
        if (logs.isEmpty()) return@combine FuelStats()

        val sortedLogs = logs.sortedBy { it.odometer }
        val totalCost = logs.sumOf { it.totalCost }
        val totalLiters = logs.sumOf { it.litersFilled }
        val avgPrice = if (totalLiters > 0) totalCost / totalLiters else 0.0

        var totalDistanceKm = 0.0
        var totalFuelUsedForDist = 0.0

        for (i in 1 until sortedLogs.size) {
            val prev = sortedLogs[i - 1]
            val curr = sortedLogs[i]
            val dist = curr.odometer - prev.odometer
            if (dist > 0 && curr.isFullTank) {
                totalDistanceKm += dist
                totalFuelUsedForDist += curr.litersFilled
            }
        }

        val avgKmL = if (totalFuelUsedForDist > 0) totalDistanceKm / totalFuelUsedForDist else 23.5
        val avgL100 = if (avgKmL > 0) 100.0 / avgKmL else 0.0
        val avgMpg = avgKmL * 2.35215
        val costPerKm = if (totalDistanceKm > 0) (sortedLogs.drop(1).sumOf { it.totalCost }) / totalDistanceKm else 0.0

        FuelStats(
            totalFuelCost = totalCost,
            totalLitersFilled = totalLiters,
            averageKmLiter = avgKmL,
            averageLitersPer100Km = avgL100,
            averageMpg = avgMpg,
            averagePricePerLiter = avgPrice,
            costPerKm = costPerKm,
            totalFuelDistanceKm = totalDistanceKm
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelStats())

    // --- Calculated Service Stats ---
    val serviceStats: StateFlow<ServiceStats> = combine(serviceLogs, activeVehicle) { logs, veh ->
        if (logs.isEmpty()) return@combine ServiceStats()

        val sortedByDate = logs.sortedByDescending { it.dateTimestamp }
        val totalCost = logs.sumOf { it.totalCost }
        val lastLog = sortedByDate.firstOrNull()

        var partsReplacedCount = 0
        logs.forEach { log ->
            if (log.partsChanged.isNotBlank()) {
                val parts = log.partsChanged.split(",", "\n").filter { it.isNotBlank() }
                partsReplacedCount += parts.size
            }
        }

        val nextDueOdo = sortedByDate.mapNotNull { it.nextServiceOdometerDue }.firstOrNull()
        val kmUntilNext = if (nextDueOdo != null) nextDueOdo - veh.currentOdometer else null

        ServiceStats(
            totalServiceCost = totalCost,
            totalServicesCount = logs.size,
            lastServiceDate = lastLog?.dateTimestamp,
            lastServiceOdometer = lastLog?.odometer,
            nextServiceOdometerDue = nextDueOdo,
            kmUntilNextService = kmUntilNext,
            totalPartsReplacedCount = partsReplacedCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceStats())

    // --- Active Parts Health / Wear Status ---
    val partsWearStatusList: StateFlow<List<PartWearStatus>> = combine(partItems, activeVehicle) { parts, veh ->
        parts.filter { !it.isReplaced }.map { part ->
            val usedKm = max(0.0, veh.currentOdometer - part.installedOdometer)
            val interval = if (part.recommendedIntervalKm > 0) part.recommendedIntervalKm else 5000.0
            val remaining = interval - usedKm
            val healthRatio = (remaining / interval).toFloat()
            val clampedHealth = max(0f, min(1f, healthRatio))
            val isOverdue = remaining <= 0

            PartWearStatus(
                part = part,
                currentKmUsed = usedKm,
                remainingKm = remaining,
                healthPercentage = clampedHealth,
                isOverdue = isOverdue
            )
        }.sortedBy { it.healthPercentage }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Overall Expense Summary ---
    val expenseSummary: StateFlow<ExpenseSummary> = combine(fuelStats, serviceStats, partItems, expenses) { fuel, service, parts, extraExp ->
        val partsDirectCost = parts.sumOf { it.cost }
        val extraTotal = extraExp.sumOf { it.cost }
        val grandTotal = fuel.totalFuelCost + service.totalServiceCost + extraTotal

        ExpenseSummary(
            totalOverallExpense = grandTotal,
            fuelTotal = fuel.totalFuelCost,
            serviceTotal = service.totalServiceCost,
            partsTotal = partsDirectCost,
            otherExpensesTotal = extraTotal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseSummary())

    // --- Monthly Expense & Fuel Breakdown for Recharts ---
    val monthlyExpenseBreakdown: StateFlow<List<MonthlyExpenseBreakdown>> = combine(
        fuelLogs,
        serviceLogs,
        partItems,
        expenses
    ) { fuels, services, parts, extraExp ->
        val cal = Calendar.getInstance()
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthShortFormat = SimpleDateFormat("MMM ''yy", Locale.US)
        val monthFullFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

        val allMonthKeys = mutableSetOf<String>()

        // Generate past 6 months up to current month as baseline
        val baselineCal = Calendar.getInstance()
        baselineCal.add(Calendar.MONTH, -5)
        for (i in 0..5) {
            allMonthKeys.add(monthKeyFormat.format(baselineCal.time))
            baselineCal.add(Calendar.MONTH, 1)
        }

        fuels.forEach { allMonthKeys.add(monthKeyFormat.format(Date(it.dateTimestamp))) }
        services.forEach { allMonthKeys.add(monthKeyFormat.format(Date(it.dateTimestamp))) }
        parts.forEach { allMonthKeys.add(monthKeyFormat.format(Date(it.installedDateTimestamp))) }
        extraExp.forEach { allMonthKeys.add(monthKeyFormat.format(Date(it.dateTimestamp))) }

        allMonthKeys.sorted().map { key ->
            val partsOfKey = key.split("-")
            val year = partsOfKey.getOrNull(0)?.toIntOrNull() ?: 2026
            val monthIdx = (partsOfKey.getOrNull(1)?.toIntOrNull() ?: 1) - 1

            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthIdx)
            cal.set(Calendar.DAY_OF_MONTH, 1)

            val monthLabel = monthShortFormat.format(cal.time)
            val fullLabel = monthFullFormat.format(cal.time)

            val matchingFuels = fuels.filter { monthKeyFormat.format(Date(it.dateTimestamp)) == key }
            val matchingServices = services.filter { monthKeyFormat.format(Date(it.dateTimestamp)) == key }
            val matchingParts = parts.filter { monthKeyFormat.format(Date(it.installedDateTimestamp)) == key }
            val matchingOthers = extraExp.filter { monthKeyFormat.format(Date(it.dateTimestamp)) == key }

            val fuelCost = matchingFuels.sumOf { it.totalCost }
            val fuelLiters = matchingFuels.sumOf { it.litersFilled }
            val serviceCost = matchingServices.sumOf { it.totalCost }
            val partsCost = matchingParts.sumOf { it.cost }
            val otherCost = matchingOthers.sumOf { it.cost }
            val totalCost = fuelCost + serviceCost + partsCost + otherCost

            MonthlyExpenseBreakdown(
                monthKey = key,
                month = monthLabel,
                fullName = fullLabel,
                fuel = fuelCost,
                service = serviceCost,
                parts = partsCost,
                other = otherCost,
                total = totalCost,
                fuelLiters = fuelLiters,
                fuelEntries = matchingFuels.size,
                serviceEntries = matchingServices.size,
                otherEntries = matchingOthers.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Maintenance Interval Notification State & Engine ---
    val notificationsEnabledState = MutableStateFlow(
        MaintenanceNotificationManager.isNotificationEnabled(application)
    )
    val alertThresholdKmState = MutableStateFlow(
        MaintenanceNotificationManager.getAlertThresholdKm(application)
    )

    val maintenanceAlerts: StateFlow<List<MaintenanceNotificationItem>> = combine(
        activeVehicle,
        serviceLogs,
        partItems,
        alertThresholdKmState
    ) { vehicle, services, parts, _ ->
        MaintenanceNotificationManager.evaluateMaintenanceAlerts(getApplication(), vehicle, services, parts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        MaintenanceNotificationManager.initNotificationChannel(application)

        // Reactive background check: when mileage, active vehicle, or logs change, evaluate & post notifications
        viewModelScope.launch {
            combine(activeVehicle, serviceLogs, partItems, notificationsEnabledState) { vehicle, services, parts, enabled ->
                if (enabled && vehicle.id > 0 && vehicle.currentOdometer > 0) {
                    MaintenanceNotificationManager.checkAndPostMaintenanceNotifications(
                        application,
                        vehicle,
                        services,
                        parts
                    )
                }
            }.collect()
        }
    }

    // --- Onboarding & Setup Actions ---

    fun completeOnboarding(
        ownerName: String,
        country: String,
        currencySymbol: String,
        distanceUnit: String,
        volumeUnit: String,
        vehicleType: String,
        model: String,
        year: Int?,
        color: String,
        boughtOnTimestamp: Long?,
        numberPlate: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val calculatedYear = year ?: 2023
            val vehicleName = if (model.isNotBlank()) model else "$vehicleType $calculatedYear"

            val initialVehicle = Vehicle(
                name = vehicleName,
                vehicleType = vehicleType,
                make = model.split(" ").firstOrNull() ?: vehicleType,
                model = model.ifBlank { vehicleType },
                year = calculatedYear,
                color = color.ifBlank { "Matte Black" },
                purchaseDateTimestamp = boughtOnTimestamp ?: now,
                ownerName = ownerName.ifBlank { "Driver" },
                country = country,
                vin = "VIN-${System.currentTimeMillis() % 1000000}",
                licensePlate = numberPlate.ifBlank { "N/A" },
                currentOdometer = 0.0,
                fuelCapacityLiters = if (vehicleType.contains("Boat") || vehicleType.contains("Bus") || vehicleType.contains("Truck")) 65.0 else if (vehicleType.contains("Car") || vehicleType.contains("SUV")) 50.0 else 14.0,
                preferredDistanceUnit = distanceUnit,
                preferredVolumeUnit = volumeUnit,
                preferredCurrency = currencySymbol,
                isActive = true,
                taxTokenNumber = "",
                taxTokenExpiryTimestamp = now + dayMs * 365,
                taxTokenNotes = "Tax token registration",
                registrationNumber = numberPlate.ifBlank { "" },
                registrationExpiryTimestamp = now + dayMs * 365,
                driverLicenseNumber = "",
                driverLicenseExpiryTimestamp = now + dayMs * 365
            )

            val newId = repository.insertVehicle(initialVehicle)
            repository.setActiveVehicle(newId)
        }
    }

    // --- Actions ---

    fun switchActiveVehicle(vehicleId: Long) {
        viewModelScope.launch {
            repository.setActiveVehicle(vehicleId)
        }
    }

    fun addVehicle(
        name: String,
        vehicleType: String,
        make: String,
        model: String,
        year: Int,
        vin: String,
        licensePlate: String,
        currentOdometer: Double,
        fuelCapacity: Double,
        taxTokenNumber: String = "",
        taxTokenExpiryDays: Int? = 365,
        registrationNumber: String = "",
        registrationExpiryDays: Int? = 365,
        driverLicenseNumber: String = "",
        driverLicenseExpiryDays: Int? = 365
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val vehicle = Vehicle(
                name = name,
                vehicleType = vehicleType,
                make = make,
                model = model,
                year = year,
                vin = vin,
                licensePlate = licensePlate,
                currentOdometer = currentOdometer,
                fuelCapacityLiters = fuelCapacity,
                isActive = true,
                taxTokenNumber = taxTokenNumber,
                taxTokenExpiryTimestamp = taxTokenExpiryDays?.let { now + it * dayMs },
                registrationNumber = registrationNumber,
                registrationExpiryTimestamp = registrationExpiryDays?.let { now + it * dayMs },
                driverLicenseNumber = driverLicenseNumber,
                driverLicenseExpiryTimestamp = driverLicenseExpiryDays?.let { now + it * dayMs }
            )
            val newId = repository.insertVehicle(vehicle)
            repository.setActiveVehicle(newId)
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle)
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }

    fun updateVehicleDocuments(
        vehicleId: Long,
        taxTokenNumber: String,
        taxTokenExpiryTimestamp: Long?,
        registrationNumber: String,
        registrationExpiryTimestamp: Long?,
        driverLicenseNumber: String,
        driverLicenseExpiryTimestamp: Long?
    ) {
        viewModelScope.launch {
            val veh = allVehicles.value.firstOrNull { it.id == vehicleId } ?: activeVehicle.value
            val updated = veh.copy(
                taxTokenNumber = taxTokenNumber,
                taxTokenExpiryTimestamp = taxTokenExpiryTimestamp,
                registrationNumber = registrationNumber,
                registrationExpiryTimestamp = registrationExpiryTimestamp,
                driverLicenseNumber = driverLicenseNumber,
                driverLicenseExpiryTimestamp = driverLicenseExpiryTimestamp
            )
            repository.updateVehicle(updated)
        }
    }

    fun updateMotorcycleProfile(
        name: String,
        make: String,
        model: String,
        year: Int,
        vin: String,
        licensePlate: String,
        currentOdometer: Double,
        fuelCapacity: Double
    ) {
        viewModelScope.launch {
            val current = activeVehicle.value
            repository.updateVehicle(
                current.copy(
                    name = name,
                    make = make,
                    model = model,
                    year = year,
                    vin = vin,
                    licensePlate = licensePlate,
                    currentOdometer = currentOdometer,
                    fuelCapacityLiters = fuelCapacity
                )
            )
        }
    }

    fun addFuelLog(
        dateTimestamp: Long,
        odometer: Double,
        litersFilled: Double,
        pricePerLiter: Double,
        isFullTank: Boolean,
        fuelType: String,
        stationName: String,
        notes: String
    ) {
        viewModelScope.launch {
            val totalCost = litersFilled * pricePerLiter
            val log = FuelLog(
                vehicleId = activeVehicle.value.id,
                dateTimestamp = dateTimestamp,
                odometer = odometer,
                litersFilled = litersFilled,
                pricePerLiter = pricePerLiter,
                totalCost = totalCost,
                isFullTank = isFullTank,
                fuelType = fuelType,
                stationName = stationName,
                notes = notes
            )
            repository.addFuelLog(log, updateOdometerIfHigher = true)
        }
    }

    fun deleteFuelLog(fuelLog: FuelLog) {
        viewModelScope.launch {
            repository.deleteFuelLog(fuelLog)
        }
    }

    fun addServiceLog(
        dateTimestamp: Long,
        odometer: Double,
        serviceType: String,
        totalCost: Double,
        serviceProvider: String,
        partsServiced: String,
        partsChanged: String,
        nextServiceOdometerDue: Double?,
        notes: String
    ) {
        viewModelScope.launch {
            val log = ServiceLog(
                vehicleId = activeVehicle.value.id,
                dateTimestamp = dateTimestamp,
                odometer = odometer,
                serviceType = serviceType,
                totalCost = totalCost,
                serviceProvider = serviceProvider,
                partsServiced = partsServiced,
                partsChanged = partsChanged,
                nextServiceOdometerDue = nextServiceOdometerDue,
                notes = notes
            )
            repository.addServiceLog(log, updateOdometerIfHigher = true)
        }
    }

    fun deleteServiceLog(serviceLog: ServiceLog) {
        viewModelScope.launch {
            repository.deleteServiceLog(serviceLog)
        }
    }

    fun addPartItem(
        name: String,
        partNumber: String,
        brand: String,
        installedDateTimestamp: Long,
        installedOdometer: Double,
        cost: Double,
        recommendedIntervalKm: Double,
        category: String,
        notes: String
    ) {
        viewModelScope.launch {
            val item = PartItem(
                vehicleId = activeVehicle.value.id,
                name = name,
                partNumber = partNumber,
                brand = brand,
                installedDateTimestamp = installedDateTimestamp,
                installedOdometer = installedOdometer,
                cost = cost,
                recommendedIntervalKm = recommendedIntervalKm,
                category = category,
                notes = notes
            )
            repository.addPartItem(item)
        }
    }

    fun replacePart(partItem: PartItem, newCost: Double, currentOdometer: Double) {
        viewModelScope.launch {
            repository.markPartReplaced(partItem, newCost, currentOdometer)
        }
    }

    fun deletePartItem(partItem: PartItem) {
        viewModelScope.launch {
            repository.deletePartItem(partItem)
        }
    }

    fun addExpense(
        title: String,
        category: String,
        amount: Double,
        dateTimestamp: Long,
        odometer: Double?,
        notes: String
    ) {
        viewModelScope.launch {
            val expense = ExpenseItem(
                vehicleId = activeVehicle.value.id,
                dateTimestamp = dateTimestamp,
                title = title,
                category = category,
                cost = amount,
                odometer = odometer,
                notes = notes
            )
            repository.addExpense(expense)
        }
    }

    fun deleteExpense(expenseItem: ExpenseItem) {
        viewModelScope.launch {
            repository.deleteExpense(expenseItem)
        }
    }

    // --- DATA EXPORT METHODS ---
    suspend fun getExportJsonPayload(): Pair<String, com.example.data.export.ExportPayloadSummary> {
        return dataExporter.generateExportJson()
    }

    suspend fun exportDataToLocalFile(): com.example.data.export.ExportResult {
        return dataExporter.exportToFile(getApplication())
    }

    suspend fun saveExportToUri(uri: android.net.Uri, jsonContent: String): Boolean {
        return dataExporter.writeJsonToUri(getApplication(), uri, jsonContent)
    }

    // --- MAINTENANCE NOTIFICATION CONTROLS ---
    fun setNotificationsEnabled(enabled: Boolean) {
        MaintenanceNotificationManager.setNotificationEnabled(getApplication(), enabled)
        notificationsEnabledState.value = enabled
        if (enabled) {
            triggerMaintenanceNotificationCheck()
        }
    }

    fun setAlertThresholdKm(thresholdKm: Double) {
        MaintenanceNotificationManager.setAlertThresholdKm(getApplication(), thresholdKm)
        alertThresholdKmState.value = thresholdKm
        triggerMaintenanceNotificationCheck()
    }

    fun sendTestMaintenanceNotification(): Boolean {
        return MaintenanceNotificationManager.sendTestNotification(
            getApplication(),
            activeVehicle.value
        )
    }

    fun triggerMaintenanceNotificationCheck(): Int {
        return MaintenanceNotificationManager.checkAndPostMaintenanceNotifications(
            getApplication(),
            activeVehicle.value,
            serviceLogs.value,
            partItems.value
        )
    }

    fun isNotificationPermissionGranted(): Boolean {
        return MaintenanceNotificationManager.hasNotificationPermission(getApplication())
    }

    // --- Recharts Helper Methods ---

    fun getMonthlyBreakdownJson(): String {
        val items = monthlyExpenseBreakdown.value
        val sb = StringBuilder("[")
        items.forEachIndexed { index, item ->
            if (index > 0) sb.append(",")
            sb.append(
                """{"monthKey":"${item.monthKey}","month":"${item.month}","fullName":"${item.fullName}","fuel":${String.format(Locale.US, "%.2f", item.fuel)},"service":${String.format(Locale.US, "%.2f", item.service)},"parts":${String.format(Locale.US, "%.2f", item.parts)},"other":${String.format(Locale.US, "%.2f", item.other)},"total":${String.format(Locale.US, "%.2f", item.total)},"fuelLiters":${String.format(Locale.US, "%.2f", item.fuelLiters)},"fuelEntries":${item.fuelEntries},"serviceEntries":${item.serviceEntries},"otherEntries":${item.otherEntries}}"""
            )
        }
        sb.append("]")
        return sb.toString()
    }

    fun getActiveVehicleInfoJson(): String {
        val veh = activeVehicle.value
        val nameEscaped = veh.name.replace("\"", "\\\"")
        val currEscaped = veh.preferredCurrency.replace("\"", "\\\"")
        return """{"name":"$nameEscaped","currency":"$currEscaped"}"""
    }

    fun seedDemoMonthlyHistory() {
        viewModelScope.launch {
            val vehId = activeVehicle.value.id
            if (vehId <= 0) return@launch
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            // 5 months ago: 2 fuel fills, chain service
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 150,
                    odometer = 11200.0,
                    litersFilled = 12.0,
                    pricePerLiter = 1.85,
                    totalCost = 22.20,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Shell",
                    notes = "Highway ride"
                )
            )
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 140,
                    odometer = 11480.0,
                    litersFilled = 13.1,
                    pricePerLiter = 1.86,
                    totalCost = 24.36,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "BP",
                    notes = "Weekend canyon trip"
                )
            )
            dao.insertServiceLog(
                ServiceLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 142,
                    odometer = 11450.0,
                    serviceType = "Chain Lube & Tension",
                    totalCost = 25.0,
                    serviceProvider = "DIY Garage",
                    partsServiced = "Drive chain cleaned with Motul degreaser",
                    partsChanged = "",
                    notes = "Slack adjusted to 30mm"
                )
            )

            // 4 months ago: 2 fuel fills, oil change & filter
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 115,
                    odometer = 11900.0,
                    litersFilled = 12.5,
                    pricePerLiter = 1.84,
                    totalCost = 23.00,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Mobil",
                    notes = "Commute"
                )
            )
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 100,
                    odometer = 12220.0,
                    litersFilled = 12.8,
                    pricePerLiter = 1.85,
                    totalCost = 23.68,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Shell",
                    notes = "City riding"
                )
            )
            dao.insertServiceLog(
                ServiceLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 105,
                    odometer = 12100.0,
                    serviceType = "Engine Oil & Filter Service",
                    totalCost = 75.0,
                    serviceProvider = "Yamaha Workshop",
                    partsServiced = "Oil drainage, crush washer replaced",
                    partsChanged = "Yamalube 10W-40, OEM Filter",
                    notes = "Smooth shift performance after oil change"
                )
            )

            // 3 months ago: 2 fuel fills, custom mod / phone mount expense
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 85,
                    odometer = 12600.0,
                    litersFilled = 12.2,
                    pricePerLiter = 1.88,
                    totalCost = 22.93,
                    isFullTank = true,
                    fuelType = "98 Octane",
                    stationName = "Chevron",
                    notes = "Premium fuel test"
                )
            )
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 72,
                    odometer = 12950.0,
                    litersFilled = 13.0,
                    pricePerLiter = 1.87,
                    totalCost = 24.31,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Shell",
                    notes = "Mountain tour"
                )
            )
            dao.insertExpense(
                ExpenseItem(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 80,
                    title = "Quad Lock Vibration Dampener Mount",
                    category = "Accessories",
                    cost = 69.95,
                    odometer = 12700.0,
                    notes = "Handlebar phone mount with anti-vibe module"
                )
            )

            // 2 months ago: 2 fuel fills, brake service
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 55,
                    odometer = 13350.0,
                    litersFilled = 12.4,
                    pricePerLiter = 1.89,
                    totalCost = 23.43,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "BP",
                    notes = "Night ride"
                )
            )
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 42,
                    odometer = 13700.0,
                    litersFilled = 12.9,
                    pricePerLiter = 1.87,
                    totalCost = 24.12,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Chevron",
                    notes = "Commuting"
                )
            )
            dao.insertServiceLog(
                ServiceLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 48,
                    odometer = 13500.0,
                    serviceType = "Brake Fluid Flush & Front Pads",
                    totalCost = 95.0,
                    serviceProvider = "Moto Brake Specialists",
                    partsServiced = "DOT 4 brake fluid bleed, caliper pistons cleaned",
                    partsChanged = "EBC Double-H Sintered Front Pads",
                    notes = "Sharp initial brake bite restored"
                )
            )

            // 1 month ago: 2 fuel fills
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 25,
                    odometer = 14100.0,
                    litersFilled = 12.0,
                    pricePerLiter = 1.85,
                    totalCost = 22.20,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Shell",
                    notes = "Weekly refill"
                )
            )
            dao.insertFuelLog(
                FuelLog(
                    vehicleId = vehId,
                    dateTimestamp = now - dayMs * 14,
                    odometer = 14420.0,
                    litersFilled = 12.6,
                    pricePerLiter = 1.86,
                    totalCost = 23.43,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "BP",
                    notes = "Coast run"
                )
            )
        }
    }
}

