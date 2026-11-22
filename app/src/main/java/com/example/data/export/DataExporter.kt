package com.example.data.export

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.dao.MotoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ExportPayloadSummary(
    val totalVehicles: Int,
    val totalFuelLogs: Int,
    val totalServiceLogs: Int,
    val totalParts: Int,
    val totalExpenses: Int,
    val totalExpenditure: Double,
    val generatedAtIso: String
)

data class ExportResult(
    val success: Boolean,
    val fileName: String,
    val filePath: String?,
    val jsonContent: String,
    val summary: ExportPayloadSummary,
    val errorMessage: String? = null
)

class DataExporter(private val dao: MotoDao) {

    suspend fun generateExportJson(): Pair<String, ExportPayloadSummary> = withContext(Dispatchers.IO) {
        val vehicles = dao.getAllVehiclesList()
        val fuelLogs = dao.getAllFuelLogsList()
        val serviceLogs = dao.getAllServiceLogsList()
        val partItems = dao.getAllPartItemsList()
        val expenseItems = dao.getAllExpensesList()

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateHumanFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val now = System.currentTimeMillis()
        val nowIso = isoFormat.format(Date(now))

        val root = JSONObject()

        // Metadata Header
        val metadata = JSONObject().apply {
            put("application", "PitStop Motorcycle & Vehicle Manager")
            put("version", "1.0")
            put("schemaVersion", 1)
            put("exportTimestamp", now)
            put("exportDateIso", nowIso)
            put("exportDateHuman", dateHumanFormat.format(Date(now)))
        }
        root.put("metadata", metadata)

        // Summary Calculations
        val totalFuelCost = fuelLogs.sumOf { it.totalCost }
        val totalServiceCost = serviceLogs.sumOf { it.totalCost }
        val totalPartsCost = partItems.sumOf { it.cost }
        val totalCustomExpenseCost = expenseItems.sumOf { it.cost }
        val grandTotalExpenditure = totalFuelCost + totalServiceCost + totalCustomExpenseCost

        val summaryObj = JSONObject().apply {
            put("totalVehicles", vehicles.size)
            put("totalFuelLogs", fuelLogs.size)
            put("totalServiceLogs", serviceLogs.size)
            put("totalPartItems", partItems.size)
            put("totalExpenseItems", expenseItems.size)
            put("totalFuelExpenditure", totalFuelCost)
            put("totalServiceExpenditure", totalServiceCost)
            put("totalPartsExpenditure", totalPartsCost)
            put("totalCustomExpenditure", totalCustomExpenseCost)
            put("grandTotalExpenditure", grandTotalExpenditure)
        }
        root.put("summary", summaryObj)

        // Vehicles Array
        val vehiclesArray = JSONArray()
        for (v in vehicles) {
            val vObj = JSONObject().apply {
                put("id", v.id)
                put("name", v.name)
                put("vehicleType", v.vehicleType)
                put("make", v.make)
                put("model", v.model)
                put("year", v.year)
                put("color", v.color)
                put("purchaseDateTimestamp", v.purchaseDateTimestamp ?: JSONObject.NULL)
                put("purchaseDatePrecision", v.purchaseDatePrecision)
                put("ownerName", v.ownerName)
                put("country", v.country)
                put("vin", v.vin)
                put("licensePlate", v.licensePlate)
                put("currentOdometer", v.currentOdometer)
                put("fuelCapacityLiters", v.fuelCapacityLiters)
                put("preferredDistanceUnit", v.preferredDistanceUnit)
                put("preferredVolumeUnit", v.preferredVolumeUnit)
                put("preferredCurrency", v.preferredCurrency)
                put("isActive", v.isActive)
                // Documents
                put("taxTokenNumber", v.taxTokenNumber)
                put("taxTokenExpiryTimestamp", v.taxTokenExpiryTimestamp ?: JSONObject.NULL)
                put("taxTokenNotes", v.taxTokenNotes)
                put("registrationNumber", v.registrationNumber)
                put("registrationIssueTimestamp", v.registrationIssueTimestamp ?: JSONObject.NULL)
                put("registrationExpiryTimestamp", v.registrationExpiryTimestamp ?: JSONObject.NULL)
                put("registrationNotes", v.registrationNotes)
                put("driverLicenseNumber", v.driverLicenseNumber)
                put("driverLicenseCategory", v.driverLicenseCategory)
                put("driverLicenseExpiryTimestamp", v.driverLicenseExpiryTimestamp ?: JSONObject.NULL)
                put("driverLicenseNotes", v.driverLicenseNotes)
            }
            vehiclesArray.put(vObj)
        }
        root.put("vehicles", vehiclesArray)

        // Fuel Logs Array
        val fuelArray = JSONArray()
        for (f in fuelLogs) {
            val fObj = JSONObject().apply {
                put("id", f.id)
                put("vehicleId", f.vehicleId)
                put("dateTimestamp", f.dateTimestamp)
                put("dateFormatted", dateHumanFormat.format(Date(f.dateTimestamp)))
                put("odometer", f.odometer)
                put("litersFilled", f.litersFilled)
                put("pricePerLiter", f.pricePerLiter)
                put("totalCost", f.totalCost)
                put("isFullTank", f.isFullTank)
                put("fuelType", f.fuelType)
                put("stationName", f.stationName)
                put("notes", f.notes)
            }
            fuelArray.put(fObj)
        }
        root.put("fuelLogs", fuelArray)

        // Service Logs Array
        val serviceArray = JSONArray()
        for (s in serviceLogs) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("vehicleId", s.vehicleId)
                put("dateTimestamp", s.dateTimestamp)
                put("dateFormatted", dateHumanFormat.format(Date(s.dateTimestamp)))
                put("odometer", s.odometer)
                put("serviceType", s.serviceType)
                put("totalCost", s.totalCost)
                put("serviceProvider", s.serviceProvider)
                put("partsServiced", s.partsServiced)
                put("partsChanged", s.partsChanged)
                put("nextServiceOdometerDue", s.nextServiceOdometerDue ?: JSONObject.NULL)
                put("nextServiceDateDue", s.nextServiceDateDue ?: JSONObject.NULL)
                put("notes", s.notes)
            }
            serviceArray.put(sObj)
        }
        root.put("serviceLogs", serviceArray)

        // Parts Wear Array
        val partsArray = JSONArray()
        for (p in partItems) {
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("vehicleId", p.vehicleId)
                put("name", p.name)
                put("partNumber", p.partNumber)
                put("brand", p.brand)
                put("installedDateTimestamp", p.installedDateTimestamp)
                put("installedDateFormatted", dateHumanFormat.format(Date(p.installedDateTimestamp)))
                put("installedOdometer", p.installedOdometer)
                put("cost", p.cost)
                put("recommendedIntervalKm", p.recommendedIntervalKm)
                put("isReplaced", p.isReplaced)
                put("category", p.category)
                put("notes", p.notes)
            }
            partsArray.put(pObj)
        }
        root.put("partItems", partsArray)

        // Expense Items Array
        val expenseArray = JSONArray()
        for (e in expenseItems) {
            val eObj = JSONObject().apply {
                put("id", e.id)
                put("vehicleId", e.vehicleId)
                put("dateTimestamp", e.dateTimestamp)
                put("dateFormatted", dateHumanFormat.format(Date(e.dateTimestamp)))
                put("title", e.title)
                put("category", e.category)
                put("cost", e.cost)
                put("odometer", e.odometer ?: JSONObject.NULL)
                put("notes", e.notes)
            }
            expenseArray.put(eObj)
        }
        root.put("expenseItems", expenseArray)

        val jsonString = root.toString(2)
        val summary = ExportPayloadSummary(
            totalVehicles = vehicles.size,
            totalFuelLogs = fuelLogs.size,
            totalServiceLogs = serviceLogs.size,
            totalParts = partItems.size,
            totalExpenses = expenseItems.size,
            totalExpenditure = grandTotalExpenditure,
            generatedAtIso = nowIso
        )

        Pair(jsonString, summary)
    }

    suspend fun exportToFile(context: Context): ExportResult = withContext(Dispatchers.IO) {
        try {
            val (jsonContent, summary) = generateExportJson()
            val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "pitstop_backup_$timeStampStr.json"

            // Save to app documents directory
            val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val exportFile = File(exportDir, fileName)
            exportFile.writeText(jsonContent, Charsets.UTF_8)

            ExportResult(
                success = true,
                fileName = fileName,
                filePath = exportFile.absolutePath,
                jsonContent = jsonContent,
                summary = summary
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                fileName = "",
                filePath = null,
                jsonContent = "",
                summary = ExportPayloadSummary(0, 0, 0, 0, 0, 0.0, ""),
                errorMessage = e.localizedMessage ?: "Unknown error while saving export file"
            )
        }
    }

    suspend fun writeJsonToUri(context: Context, uri: Uri, jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
