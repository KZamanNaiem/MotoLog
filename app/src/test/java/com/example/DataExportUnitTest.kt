package com.example

import com.example.data.dao.MotoDao
import com.example.data.export.DataExporter
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataExportUnitTest {

    private val fakeDao = object : MotoDao {
        override fun getAllVehicles(): Flow<List<Vehicle>> = flowOf(listOf(Vehicle(id = 1, name = "Honda CB500X")))
        override suspend fun getAllVehiclesList(): List<Vehicle> = listOf(
            Vehicle(id = 1, name = "Honda CB500X", make = "Honda", model = "CB500X", year = 2022)
        )
        override fun getVehicleById(id: Long): Flow<Vehicle?> = flowOf(null)
        override suspend fun getVehicleByIdSync(id: Long): Vehicle? = null
        override fun getActiveVehicle(): Flow<Vehicle?> = flowOf(null)
        override suspend fun getActiveVehicleSync(): Vehicle? = null
        override suspend fun insertVehicle(vehicle: Vehicle): Long = 1L
        override suspend fun updateVehicle(vehicle: Vehicle) {}
        override suspend fun deleteVehicle(vehicle: Vehicle) {}
        override suspend fun clearAllActiveVehicles() {}
        override fun getProfileFlow(): Flow<Vehicle?> = flowOf(null)
        override suspend fun getProfileSync(): Vehicle? = null
        override fun getFuelLogsForVehicle(vehicleId: Long): Flow<List<FuelLog>> = flowOf(emptyList())
        override fun getAllFuelLogs(): Flow<List<FuelLog>> = flowOf(emptyList())
        override suspend fun getAllFuelLogsList(): List<FuelLog> = listOf(
            FuelLog(id = 1, vehicleId = 1, dateTimestamp = 1700000000000L, odometer = 5000.0, litersFilled = 12.0, pricePerLiter = 1.5, totalCost = 18.0)
        )
        override suspend fun insertFuelLog(fuelLog: FuelLog): Long = 1L
        override suspend fun updateFuelLog(fuelLog: FuelLog) {}
        override suspend fun deleteFuelLog(fuelLog: FuelLog) {}
        override fun getServiceLogsForVehicle(vehicleId: Long): Flow<List<ServiceLog>> = flowOf(emptyList())
        override fun getAllServiceLogs(): Flow<List<ServiceLog>> = flowOf(emptyList())
        override suspend fun getAllServiceLogsList(): List<ServiceLog> = listOf(
            ServiceLog(id = 1, vehicleId = 1, dateTimestamp = 1700000000000L, odometer = 5000.0, serviceType = "Oil Change", totalCost = 65.0, serviceProvider = "DIY", partsServiced = "Filter", partsChanged = "10W-40")
        )
        override suspend fun insertServiceLog(serviceLog: ServiceLog): Long = 1L
        override suspend fun updateServiceLog(serviceLog: ServiceLog) {}
        override suspend fun deleteServiceLog(serviceLog: ServiceLog) {}
        override fun getPartItemsForVehicle(vehicleId: Long): Flow<List<PartItem>> = flowOf(emptyList())
        override fun getAllPartItems(): Flow<List<PartItem>> = flowOf(emptyList())
        override suspend fun getAllPartItemsList(): List<PartItem> = listOf(
            PartItem(id = 1, vehicleId = 1, name = "Brake Pads", installedDateTimestamp = 1700000000000L, installedOdometer = 4000.0, cost = 45.0)
        )
        override suspend fun insertPartItem(partItem: PartItem): Long = 1L
        override suspend fun updatePartItem(partItem: PartItem) {}
        override suspend fun deletePartItem(partItem: PartItem) {}
        override fun getExpensesForVehicle(vehicleId: Long): Flow<List<ExpenseItem>> = flowOf(emptyList())
        override fun getAllExpenses(): Flow<List<ExpenseItem>> = flowOf(emptyList())
        override suspend fun getAllExpensesList(): List<ExpenseItem> = listOf(
            ExpenseItem(id = 1, vehicleId = 1, dateTimestamp = 1700000000000L, title = "Helmet Shield", category = "Gear", cost = 30.0)
        )
        override suspend fun insertExpense(expenseItem: ExpenseItem): Long = 1L
        override suspend fun deleteExpense(expenseItem: ExpenseItem) {}
    }

    @Test
    fun testJsonExportStructure() = runBlocking {
        val exporter = DataExporter(fakeDao)
        val (jsonString, summary) = exporter.generateExportJson()

        assertNotNull(jsonString)
        assertTrue(jsonString.isNotBlank())

        val json = JSONObject(jsonString)
        assertTrue(json.has("metadata"))
        assertTrue(json.has("summary"))
        assertTrue(json.has("vehicles"))
        assertTrue(json.has("fuelLogs"))
        assertTrue(json.has("serviceLogs"))
        assertTrue(json.has("partItems"))
        assertTrue(json.has("expenseItems"))

        assertEquals(1, json.getJSONArray("vehicles").length())
        assertEquals(1, json.getJSONArray("fuelLogs").length())
        assertEquals(1, json.getJSONArray("serviceLogs").length())
        assertEquals(1, json.getJSONArray("partItems").length())
        assertEquals(1, json.getJSONArray("expenseItems").length())

        assertEquals(1, summary.totalVehicles)
        assertEquals(1, summary.totalFuelLogs)
        assertEquals(1, summary.totalServiceLogs)
        assertEquals(1, summary.totalParts)
        assertEquals(1, summary.totalExpenses)
        assertEquals(18.0 + 65.0 + 30.0, summary.totalExpenditure, 0.01)
    }
}
