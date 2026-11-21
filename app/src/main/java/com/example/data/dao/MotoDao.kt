package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoDao {

    // --- Vehicles ---
    @Query("SELECT * FROM vehicles ORDER BY isActive DESC, id ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles ORDER BY isActive DESC, id ASC")
    suspend fun getAllVehiclesList(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun getVehicleById(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleByIdSync(id: Long): Vehicle?

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    fun getActiveVehicle(): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveVehicleSync(): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isActive = 0")
    suspend fun clearAllActiveVehicles()

    @Transaction
    suspend fun setActiveVehicle(vehicleId: Long) {
        clearAllActiveVehicles()
        val vehicle = getVehicleByIdSync(vehicleId)
        if (vehicle != null) {
            updateVehicle(vehicle.copy(isActive = true))
        }
    }

    // --- Legacy profile methods for compatibility ---
    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    fun getProfileFlow(): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    suspend fun getProfileSync(): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: Vehicle) {
        insertVehicle(profile)
    }

    // --- Fuel Logs ---
    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY odometer DESC, dateTimestamp DESC")
    fun getFuelLogsForVehicle(vehicleId: Long): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs ORDER BY odometer DESC, dateTimestamp DESC")
    fun getAllFuelLogs(): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs ORDER BY odometer DESC, dateTimestamp DESC")
    suspend fun getAllFuelLogsList(): List<FuelLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(fuelLog: FuelLog): Long

    @Update
    suspend fun updateFuelLog(fuelLog: FuelLog)

    @Delete
    suspend fun deleteFuelLog(fuelLog: FuelLog)

    // --- Service Logs ---
    @Query("SELECT * FROM service_logs WHERE vehicleId = :vehicleId ORDER BY odometer DESC, dateTimestamp DESC")
    fun getServiceLogsForVehicle(vehicleId: Long): Flow<List<ServiceLog>>

    @Query("SELECT * FROM service_logs ORDER BY odometer DESC, dateTimestamp DESC")
    fun getAllServiceLogs(): Flow<List<ServiceLog>>

    @Query("SELECT * FROM service_logs ORDER BY odometer DESC, dateTimestamp DESC")
    suspend fun getAllServiceLogsList(): List<ServiceLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceLog(serviceLog: ServiceLog): Long

    @Update
    suspend fun updateServiceLog(serviceLog: ServiceLog)

    @Delete
    suspend fun deleteServiceLog(serviceLog: ServiceLog)

    // --- Part Items ---
    @Query("SELECT * FROM part_items WHERE vehicleId = :vehicleId ORDER BY installedOdometer DESC")
    fun getPartItemsForVehicle(vehicleId: Long): Flow<List<PartItem>>

    @Query("SELECT * FROM part_items ORDER BY installedOdometer DESC")
    fun getAllPartItems(): Flow<List<PartItem>>

    @Query("SELECT * FROM part_items ORDER BY installedOdometer DESC")
    suspend fun getAllPartItemsList(): List<PartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartItem(partItem: PartItem): Long

    @Update
    suspend fun updatePartItem(partItem: PartItem)

    @Delete
    suspend fun deletePartItem(partItem: PartItem)

    // --- Expense Items ---
    @Query("SELECT * FROM expense_items WHERE vehicleId = :vehicleId ORDER BY dateTimestamp DESC")
    fun getExpensesForVehicle(vehicleId: Long): Flow<List<ExpenseItem>>

    @Query("SELECT * FROM expense_items ORDER BY dateTimestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseItem>>

    @Query("SELECT * FROM expense_items ORDER BY dateTimestamp DESC")
    suspend fun getAllExpensesList(): List<ExpenseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expenseItem: ExpenseItem): Long

    @Delete
    suspend fun deleteExpense(expenseItem: ExpenseItem)
}

