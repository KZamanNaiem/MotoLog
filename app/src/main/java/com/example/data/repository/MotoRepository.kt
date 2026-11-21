package com.example.data.repository

import com.example.data.dao.MotoDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class MotoRepository(private val dao: MotoDao) {

    val allVehiclesFlow: Flow<List<Vehicle>> = dao.getAllVehicles()
    val activeVehicleFlow: Flow<Vehicle?> = dao.getActiveVehicle()

    // Legacy profile flow
    val profileFlow: Flow<Vehicle?> = dao.getProfileFlow()

    fun getFuelLogsForVehicle(vehicleId: Long): Flow<List<FuelLog>> = dao.getFuelLogsForVehicle(vehicleId)
    fun getServiceLogsForVehicle(vehicleId: Long): Flow<List<ServiceLog>> = dao.getServiceLogsForVehicle(vehicleId)
    fun getPartItemsForVehicle(vehicleId: Long): Flow<List<PartItem>> = dao.getPartItemsForVehicle(vehicleId)
    fun getExpensesForVehicle(vehicleId: Long): Flow<List<ExpenseItem>> = dao.getExpensesForVehicle(vehicleId)

    val fuelLogsFlow: Flow<List<FuelLog>> = dao.getAllFuelLogs()
    val serviceLogsFlow: Flow<List<ServiceLog>> = dao.getAllServiceLogs()
    val partItemsFlow: Flow<List<PartItem>> = dao.getAllPartItems()
    val expensesFlow: Flow<List<ExpenseItem>> = dao.getAllExpenses()

    suspend fun setActiveVehicle(vehicleId: Long) {
        dao.setActiveVehicle(vehicleId)
    }

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        return dao.insertVehicle(vehicle)
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        dao.updateVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.deleteVehicle(vehicle)
    }

    suspend fun updateProfile(profile: Vehicle) {
        dao.updateVehicle(profile)
    }

    suspend fun addFuelLog(fuelLog: FuelLog, updateOdometerIfHigher: Boolean = true) {
        dao.insertFuelLog(fuelLog)
        if (updateOdometerIfHigher) {
            val vehicle = dao.getVehicleByIdSync(fuelLog.vehicleId) ?: dao.getActiveVehicleSync()
            if (vehicle != null && fuelLog.odometer > vehicle.currentOdometer) {
                dao.updateVehicle(vehicle.copy(currentOdometer = fuelLog.odometer))
            }
        }
    }

    suspend fun deleteFuelLog(fuelLog: FuelLog) {
        dao.deleteFuelLog(fuelLog)
    }

    suspend fun addServiceLog(serviceLog: ServiceLog, updateOdometerIfHigher: Boolean = true) {
        dao.insertServiceLog(serviceLog)
        if (updateOdometerIfHigher) {
            val vehicle = dao.getVehicleByIdSync(serviceLog.vehicleId) ?: dao.getActiveVehicleSync()
            if (vehicle != null && serviceLog.odometer > vehicle.currentOdometer) {
                dao.updateVehicle(vehicle.copy(currentOdometer = serviceLog.odometer))
            }
        }
    }

    suspend fun deleteServiceLog(serviceLog: ServiceLog) {
        dao.deleteServiceLog(serviceLog)
    }

    suspend fun addPartItem(partItem: PartItem) {
        dao.insertPartItem(partItem)
    }

    suspend fun updatePartItem(partItem: PartItem) {
        dao.updatePartItem(partItem)
    }

    suspend fun deletePartItem(partItem: PartItem) {
        dao.deletePartItem(partItem)
    }

    suspend fun addExpense(expenseItem: ExpenseItem) {
        dao.insertExpense(expenseItem)
    }

    suspend fun deleteExpense(expenseItem: ExpenseItem) {
        dao.deleteExpense(expenseItem)
    }

    suspend fun markPartReplaced(partItem: PartItem, replacementCost: Double, replacementOdometer: Double) {
        val now = System.currentTimeMillis()
        // Mark old part as replaced
        dao.updatePartItem(partItem.copy(isReplaced = true))
        
        // Log service entry for part replacement
        val serviceEntry = ServiceLog(
            vehicleId = partItem.vehicleId,
            dateTimestamp = now,
            odometer = replacementOdometer,
            serviceType = "Part Replacement - ${partItem.name}",
            totalCost = replacementCost,
            serviceProvider = "Garage",
            partsServiced = "Replaced worn part ${partItem.name}",
            partsChanged = "${partItem.name} (${partItem.brand} ${partItem.partNumber})",
            notes = "Replaced at $replacementOdometer km"
        )
        dao.insertServiceLog(serviceEntry)

        // Insert new active part tracking item
        val newPart = partItem.copy(
            id = 0,
            installedDateTimestamp = now,
            installedOdometer = replacementOdometer,
            cost = replacementCost,
            isReplaced = false
        )
        dao.insertPartItem(newPart)

        // Update profile odometer if higher
        val vehicle = dao.getVehicleByIdSync(partItem.vehicleId) ?: dao.getActiveVehicleSync()
        if (vehicle != null && replacementOdometer > vehicle.currentOdometer) {
            dao.updateVehicle(vehicle.copy(currentOdometer = replacementOdometer))
        }
    }
}

