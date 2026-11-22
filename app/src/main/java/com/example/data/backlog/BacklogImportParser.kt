package com.example.data.backlog

import com.example.data.model.FuelLog
import com.example.data.model.PartItem
import com.example.data.model.ServiceLog
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

enum class BacklogImportMode(val label: String) {
    AUTO_MIXED("Auto / Mixed CSV or JSON"),
    FUEL_CSV("Fuel History CSV"),
    SERVICE_CSV("Service History CSV"),
    PARTS_CSV("Parts History CSV")
}

data class BacklogParseResult(
    val fuelLogs: List<FuelLog> = emptyList(),
    val serviceLogs: List<ServiceLog> = emptyList(),
    val partItems: List<PartItem> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val totalCount: Int
        get() = fuelLogs.size + serviceLogs.size + partItems.size
}

object BacklogImportParser {

    private val dateFormats = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "dd-MM-yyyy",
        "MMM dd, yyyy",
        "dd MMM yyyy",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    fun parseDateToMillis(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // Check numeric epoch millis
        trimmed.toLongOrNull()?.let { num ->
            if (num > 1_000_000_000_000L) return num
            if (num > 1_000_000_000L) return num * 1000L
        }

        for (pattern in dateFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }
                val date = sdf.parse(trimmed)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun getSampleTemplate(mode: BacklogImportMode): String = when (mode) {
        BacklogImportMode.FUEL_CSV -> """
            # date, odometer, liters_or_gal, price_per_unit, station, full_tank(true/false), notes
            2026-06-12, 11850, 11.4, 1.84, Shell, true, Weekend ride
            2026-07-04, 12190, 12.1, 1.87, Chevron, true, Highway commute
            2026-08-18, 12540, 10.8, 1.89, BP, false, Quick top-up
        """.trimIndent()

        BacklogImportMode.SERVICE_CSV -> """
            # date, odometer, service_type, cost, provider, parts_serviced, parts_changed, notes
            2026-05-10, 11000, Oil & Filter Change, 68.50, Yamaha Pro Shop, Chain tensioned, Yamalube 10W-40 & OEM Filter, Smooth engine
            2026-07-22, 12300, Brake Inspection & Flush, 95.00, Local Moto Garage, Calipers cleaned, DOT 4 Fluid, Front brake firm
        """.trimIndent()

        BacklogImportMode.PARTS_CSV -> """
            # date, installed_odometer, part_name, brand, cost, interval_km, replaced(true/false), notes
            2026-04-15, 10500, Drive Chain & Sprockets, DID, 145.00, 15000, false, 520 VX3 Gold X-Ring
            2026-03-01, 8200, Rear Tire, Michelin, 195.00, 10000, true, Replaced previously
        """.trimIndent()

        BacklogImportMode.AUTO_MIXED -> """
            # Mixed Format: type(fuel/service/part), date, odometer, field1, field2, field3, ...
            fuel, 2026-06-12, 11850, 11.4, 1.84, Shell, true, Weekend ride
            service, 2026-07-22, 12300, Oil & Filter Change, 68.50, Yamaha Shop, Chain lubed, 10W-40 Oil
            part, 2026-04-15, 10500, Drive Chain, DID, 145.00, 15000, false, Installed before trip
        """.trimIndent()
    }

    fun parseInput(
        rawText: String,
        vehicleId: Long,
        mode: BacklogImportMode
    ): BacklogParseResult {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return BacklogParseResult(warnings = listOf("Paste CSV rows, JSON backup, or load a file first."))
        }

        // Detect JSON payload
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseJsonPayload(trimmed, vehicleId)
        }

        return parseCsvLines(trimmed, vehicleId, mode)
    }

    private fun parseJsonPayload(jsonText: String, vehicleId: Long): BacklogParseResult {
        val fuels = mutableListOf<FuelLog>()
        val services = mutableListOf<ServiceLog>()
        val parts = mutableListOf<PartItem>()
        val warnings = mutableListOf<String>()

        try {
            if (jsonText.startsWith("{")) {
                val root = JSONObject(jsonText)
                root.optJSONArray("fuelLogs")?.let { arr ->
                    parseFuelJsonArray(arr, vehicleId, fuels)
                }
                root.optJSONArray("serviceLogs")?.let { arr ->
                    parseServiceJsonArray(arr, vehicleId, services)
                }
                root.optJSONArray("partItems")?.let { arr ->
                    parsePartJsonArray(arr, vehicleId, parts)
                }
                if (fuels.isEmpty() && services.isEmpty() && parts.isEmpty()) {
                    warnings.add("JSON object did not contain 'fuelLogs', 'serviceLogs', or 'partItems'.")
                }
            } else {
                val arr = JSONArray(jsonText)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val type = obj.optString("type", "").lowercase(Locale.US)
                    when {
                        type == "fuel" || obj.has("litersFilled") -> {
                            parseFuelJsonArray(JSONArray().put(obj), vehicleId, fuels)
                        }
                        type == "service" || obj.has("serviceType") -> {
                            parseServiceJsonArray(JSONArray().put(obj), vehicleId, services)
                        }
                        type == "part" || obj.has("recommendedIntervalKm") -> {
                            parsePartJsonArray(JSONArray().put(obj), vehicleId, parts)
                        }
                    }
                }
                if (fuels.isEmpty() && services.isEmpty() && parts.isEmpty()) {
                    warnings.add("JSON array could not be mapped to fuel, service, or part items.")
                }
            }
        } catch (e: Exception) {
            warnings.add("Invalid JSON: ${e.localizedMessage ?: "Parse error"}")
        }

        return BacklogParseResult(fuels, services, parts, warnings)
    }

    private fun parseFuelJsonArray(arr: JSONArray, vehicleId: Long, out: MutableList<FuelLog>) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val ts = obj.optLong("dateTimestamp", 0L).takeIf { it > 0L }
                ?: parseDateToMillis(obj.optString("date", obj.optString("dateFormatted", "")))
                ?: System.currentTimeMillis()
            val odo = obj.optDouble("odometer", 0.0)
            val liters = obj.optDouble("litersFilled", 0.0)
            val price = obj.optDouble("pricePerLiter", 0.0)
            val total = obj.optDouble("totalCost", liters * price).let {
                if (it.isNaN() || it <= 0.0) liters * price else it
            }
            if (liters > 0 || total > 0) {
                out.add(
                    FuelLog(
                        vehicleId = vehicleId,
                        dateTimestamp = ts,
                        odometer = odo,
                        litersFilled = liters,
                        pricePerLiter = if (price > 0) price else if (liters > 0) total / liters else 0.0,
                        totalCost = total,
                        isFullTank = obj.optBoolean("isFullTank", true),
                        fuelType = obj.optString("fuelType", "95 Octane").ifBlank { "95 Octane" },
                        stationName = obj.optString("stationName", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }
    }

    private fun parseServiceJsonArray(arr: JSONArray, vehicleId: Long, out: MutableList<ServiceLog>) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val ts = obj.optLong("dateTimestamp", 0L).takeIf { it > 0L }
                ?: parseDateToMillis(obj.optString("date", obj.optString("dateFormatted", "")))
                ?: System.currentTimeMillis()
            val type = obj.optString("serviceType", "Maintenance Service").ifBlank { "Maintenance Service" }
            val nextOdo = if (obj.isNull("nextServiceOdometerDue")) null else obj.optDouble("nextServiceOdometerDue").takeIf { !it.isNaN() && it > 0 }
            out.add(
                ServiceLog(
                    vehicleId = vehicleId,
                    dateTimestamp = ts,
                    odometer = obj.optDouble("odometer", 0.0),
                    serviceType = type,
                    totalCost = obj.optDouble("totalCost", 0.0),
                    serviceProvider = obj.optString("serviceProvider", ""),
                    partsServiced = obj.optString("partsServiced", ""),
                    partsChanged = obj.optString("partsChanged", ""),
                    nextServiceOdometerDue = nextOdo,
                    notes = obj.optString("notes", "")
                )
            )
        }
    }

    private fun parsePartJsonArray(arr: JSONArray, vehicleId: Long, out: MutableList<PartItem>) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue
            val ts = obj.optLong("installedDateTimestamp", 0L).takeIf { it > 0L }
                ?: parseDateToMillis(obj.optString("date", obj.optString("installedDateFormatted", "")))
                ?: System.currentTimeMillis()
            out.add(
                PartItem(
                    vehicleId = vehicleId,
                    name = name,
                    partNumber = obj.optString("partNumber", ""),
                    brand = obj.optString("brand", ""),
                    installedDateTimestamp = ts,
                    installedOdometer = obj.optDouble("installedOdometer", 0.0),
                    cost = obj.optDouble("cost", 0.0),
                    recommendedIntervalKm = obj.optDouble("recommendedIntervalKm", 5000.0).takeIf { !it.isNaN() && it > 0 } ?: 5000.0,
                    isReplaced = obj.optBoolean("isReplaced", false),
                    category = obj.optString("category", "Consumable").ifBlank { "Consumable" },
                    notes = obj.optString("notes", "")
                )
            )
        }
    }

    private fun parseCsvLines(
        text: String,
        vehicleId: Long,
        mode: BacklogImportMode
    ): BacklogParseResult {
        val fuels = mutableListOf<FuelLog>()
        val services = mutableListOf<ServiceLog>()
        val parts = mutableListOf<PartItem>()
        val warnings = mutableListOf<String>()

        val lines = text.lines()
        lines.forEachIndexed { idx, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEachIndexed

            val cols = line.split(",").map { it.trim() }
            if (cols.size < 3) {
                warnings.add("Line ${idx + 1}: Needs at least 3 comma-separated values.")
                return@forEachIndexed
            }

            // Skip header rows
            val firstLower = cols[0].lowercase(Locale.US)
            if (firstLower == "date" || firstLower == "type" || firstLower == "category") {
                return@forEachIndexed
            }

            when (mode) {
                BacklogImportMode.AUTO_MIXED -> {
                    when (firstLower) {
                        "fuel", "gas", "petrol" -> parseFuelCols(cols.drop(1), vehicleId, idx + 1, fuels, warnings)
                        "service", "maintenance", "repair" -> parseServiceCols(cols.drop(1), vehicleId, idx + 1, services, warnings)
                        "part", "parts", "component" -> parsePartCols(cols.drop(1), vehicleId, idx + 1, parts, warnings)
                        else -> {
                            // Try inferring if first col is a date
                            if (parseDateToMillis(cols[0]) != null) {
                                // Heuristic: if col 2 and col 3 are numeric -> Fuel (date, odo, liters, price)
                                val col2Num = cols.getOrNull(2)?.toDoubleOrNull()
                                val col3Num = cols.getOrNull(3)?.toDoubleOrNull()
                                if (col2Num != null && col3Num != null) {
                                    parseFuelCols(cols, vehicleId, idx + 1, fuels, warnings)
                                } else {
                                    parseServiceCols(cols, vehicleId, idx + 1, services, warnings)
                                }
                            } else {
                                warnings.add("Line ${idx + 1}: Unknown prefix '${cols[0]}'. Start with fuel/service/part or a date.")
                            }
                        }
                    }
                }
                BacklogImportMode.FUEL_CSV -> parseFuelCols(cols, vehicleId, idx + 1, fuels, warnings)
                BacklogImportMode.SERVICE_CSV -> parseServiceCols(cols, vehicleId, idx + 1, services, warnings)
                BacklogImportMode.PARTS_CSV -> parsePartCols(cols, vehicleId, idx + 1, parts, warnings)
            }
        }

        return BacklogParseResult(fuels, services, parts, warnings)
    }

    private fun parseFuelCols(
        cols: List<String>,
        vehicleId: Long,
        lineNum: Int,
        out: MutableList<FuelLog>,
        warnings: MutableList<String>
    ) {
        val ts = parseDateToMillis(cols.getOrNull(0) ?: "")
        val odo = cols.getOrNull(1)?.toDoubleOrNull()
        val liters = cols.getOrNull(2)?.toDoubleOrNull()
        val price = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0

        if (ts == null || odo == null || liters == null) {
            warnings.add("Line $lineNum (Fuel): Expected 'date, odometer, volume, price, ...'")
            return
        }

        val station = cols.getOrNull(4).orEmpty()
        val isFull = cols.getOrNull(5)?.lowercase(Locale.US)?.let {
            it != "false" && it != "0" && it != "no" && it != "partial"
        } ?: true
        val notes = cols.drop(6).joinToString(", ")

        out.add(
            FuelLog(
                vehicleId = vehicleId,
                dateTimestamp = ts,
                odometer = odo,
                litersFilled = liters,
                pricePerLiter = price,
                totalCost = liters * price,
                isFullTank = isFull,
                fuelType = "95 Octane",
                stationName = station,
                notes = notes
            )
        )
    }

    private fun parseServiceCols(
        cols: List<String>,
        vehicleId: Long,
        lineNum: Int,
        out: MutableList<ServiceLog>,
        warnings: MutableList<String>
    ) {
        val ts = parseDateToMillis(cols.getOrNull(0) ?: "")
        val odo = cols.getOrNull(1)?.toDoubleOrNull()
        val serviceType = cols.getOrNull(2)?.takeIf { it.isNotBlank() }

        if (ts == null || odo == null || serviceType == null) {
            warnings.add("Line $lineNum (Service): Expected 'date, odometer, service_type, cost, ...'")
            return
        }

        val cost = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val provider = cols.getOrNull(4).orEmpty()
        val partsServiced = cols.getOrNull(5).orEmpty()
        val partsChanged = cols.getOrNull(6).orEmpty()
        val notes = cols.drop(7).joinToString(", ")

        out.add(
            ServiceLog(
                vehicleId = vehicleId,
                dateTimestamp = ts,
                odometer = odo,
                serviceType = serviceType,
                totalCost = cost,
                serviceProvider = provider,
                partsServiced = partsServiced,
                partsChanged = partsChanged,
                nextServiceOdometerDue = null,
                notes = notes
            )
        )
    }

    private fun parsePartCols(
        cols: List<String>,
        vehicleId: Long,
        lineNum: Int,
        out: MutableList<PartItem>,
        warnings: MutableList<String>
    ) {
        val ts = parseDateToMillis(cols.getOrNull(0) ?: "")
        val odo = cols.getOrNull(1)?.toDoubleOrNull()
        val name = cols.getOrNull(2)?.takeIf { it.isNotBlank() }

        if (ts == null || odo == null || name == null) {
            warnings.add("Line $lineNum (Part): Expected 'date, installed_odometer, part_name, brand, cost, ...'")
            return
        }

        val brand = cols.getOrNull(3).orEmpty()
        val cost = cols.getOrNull(4)?.toDoubleOrNull() ?: 0.0
        val interval = cols.getOrNull(5)?.toDoubleOrNull()?.takeIf { it > 0 } ?: 5000.0
        val isReplaced = cols.getOrNull(6)?.lowercase(Locale.US)?.let {
            it == "true" || it == "1" || it == "yes" || it == "replaced"
        } ?: false
        val notes = cols.drop(7).joinToString(", ")

        out.add(
            PartItem(
                vehicleId = vehicleId,
                name = name,
                partNumber = "",
                brand = brand,
                installedDateTimestamp = ts,
                installedOdometer = odo,
                cost = cost,
                recommendedIntervalKm = interval,
                isReplaced = isReplaced,
                category = "Consumable",
                notes = notes
            )
        )
    }
}
