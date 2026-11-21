package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "Yamaha MT-07",
    val vehicleType: String = "Motorcycle", // "Motorcycle", "Car / Sedan", "SUV", "Bus", "Boat", "Truck", "Scooter", "EV", "Van", "Other"
    val make: String = "Yamaha",
    val model: String = "MT-07",
    val year: Int = 2023,
    val color: String = "Matte Black",
    val purchaseDateTimestamp: Long? = null,
    val ownerName: String = "Rider",
    val country: String = "United States",
    val vin: String = "JYARN78E1PA001234",
    val licensePlate: String = "MOTO-789", // Vehicle Number Plate
    val currentOdometer: Double = 12450.0,
    val fuelCapacityLiters: Double = 14.0,
    val preferredDistanceUnit: String = "km", // "km" or "mi"
    val preferredVolumeUnit: String = "L",   // "L" or "gal"
    val preferredCurrency: String = "$",
    val isActive: Boolean = false,

    // Tax Token Details
    val taxTokenNumber: String = "TX-2025-9812",
    val taxTokenExpiryTimestamp: Long? = System.currentTimeMillis() + 86400000L * 18, // 18 days from now
    val taxTokenNotes: String = "Annual Road Tax & Fitness Token",

    // Registration Paper Details
    val registrationNumber: String = "REG-MH-2023-774",
    val registrationIssueTimestamp: Long? = System.currentTimeMillis() - 86400000L * 365,
    val registrationExpiryTimestamp: Long? = System.currentTimeMillis() + 86400000L * 120, // 120 days from now
    val registrationNotes: String = "Official Vehicle Registration Certificate",

    // Driver's License Details
    val driverLicenseNumber: String = "DL-908234-A",
    val driverLicenseCategory: String = "Class A & B (Motorcycle & Light Motor)",
    val driverLicenseExpiryTimestamp: Long? = System.currentTimeMillis() + 86400000L * 25, // 25 days from now
    val driverLicenseNotes: String = "International Driving Permit Attached"
)

// Legacy alias for smooth transition
typealias MotorcycleProfile = Vehicle

@Entity(tableName = "fuel_logs")
data class FuelLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long = 1L,
    val dateTimestamp: Long,
    val odometer: Double,
    val litersFilled: Double,
    val pricePerLiter: Double,
    val totalCost: Double,
    val isFullTank: Boolean = true,
    val fuelType: String = "95 Octane",
    val stationName: String = "",
    val notes: String = ""
)

@Entity(tableName = "service_logs")
data class ServiceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long = 1L,
    val dateTimestamp: Long,
    val odometer: Double,
    val serviceType: String, // e.g., "Full Service", "Oil & Filter", "Tire Change", "Chain Maintenance", "Repair"
    val totalCost: Double,
    val serviceProvider: String, // e.g. "Yamaha Authorized Dealer", "DIY", "Local Workshop"
    val partsServiced: String, // e.g. "Chain cleaned & tensioned, Throttle freeplay, Brake fluid inspection"
    val partsChanged: String,  // e.g. "Engine Oil 10W-40 (2.6L), Yamaha Oil Filter, Front Brake Pads"
    val nextServiceOdometerDue: Double? = null,
    val nextServiceDateDue: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "part_items")
data class PartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long = 1L,
    val name: String, // e.g. "Engine Oil", "Front Tire", "Drive Chain", "Spark Plugs"
    val partNumber: String = "",
    val brand: String = "",
    val installedDateTimestamp: Long,
    val installedOdometer: Double,
    val cost: Double = 0.0,
    val recommendedIntervalKm: Double = 5000.0, // e.g., 5000 km replacement interval
    val isReplaced: Boolean = false,
    val category: String = "Consumable", // "Consumable", "Wear Item", "Upgrade", "Spare"
    val notes: String = ""
)

@Entity(tableName = "expense_items")
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long = 1L,
    val dateTimestamp: Long,
    val title: String,
    val category: String, // "Fuel", "Service", "Parts & Mods", "Gear", "Insurance/Tax", "Other"
    val cost: Double,
    val odometer: Double? = null,
    val notes: String = ""
)

