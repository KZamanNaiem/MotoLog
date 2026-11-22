package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.MotoDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Vehicle::class,
        FuelLog::class,
        ServiceLog::class,
        PartItem::class,
        ExpenseItem::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun motoDao(): MotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE vehicles ADD COLUMN purchaseDatePrecision TEXT NOT NULL DEFAULT 'FULL_DATE'"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "motolog_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDatabase(database.motoDao())
                    }
                }
            }

            private suspend fun seedDatabase(dao: MotoDao) {
                val now = System.currentTimeMillis()
                val dayMs = 86400000L

                // 1. Primary Motorcycle
                val moto1 = Vehicle(
                    id = 1,
                    name = "Yamaha MT-07 Cyber",
                    vehicleType = "Motorcycle",
                    make = "Yamaha",
                    model = "MT-07",
                    year = 2023,
                    vin = "JYARN78E9PA084120",
                    licensePlate = "MOTO-789",
                    currentOdometer = 14820.0,
                    fuelCapacityLiters = 14.0,
                    preferredDistanceUnit = "km",
                    preferredVolumeUnit = "L",
                    preferredCurrency = "$",
                    isActive = true,

                    // Tax Token (Expiring in 12 days!)
                    taxTokenNumber = "TX-2026-9812",
                    taxTokenExpiryTimestamp = now + dayMs * 12,
                    taxTokenNotes = "Annual Road Tax & Fitness Certificate",

                    // Registration Paper
                    registrationNumber = "REG-MOTO-2023-109",
                    registrationIssueTimestamp = now - dayMs * 365,
                    registrationExpiryTimestamp = now + dayMs * 180,
                    registrationNotes = "Registered under Main License",

                    // Driver License (Expiring in 5 days!)
                    driverLicenseNumber = "DL-908234-A",
                    driverLicenseCategory = "Class A Motorcycle & Light Motor",
                    driverLicenseExpiryTimestamp = now + dayMs * 5,
                    driverLicenseNotes = "Full Unrestricted License"
                )

                // 2. Secondary Car / SUV
                val car2 = Vehicle(
                    id = 2,
                    name = "Toyota RAV4 Hybrid",
                    vehicleType = "SUV",
                    make = "Toyota",
                    model = "RAV4 Hybrid AWD",
                    year = 2022,
                    vin = "4T3C1RFV5NU204918",
                    licensePlate = "CAR-5521",
                    currentOdometer = 38400.0,
                    fuelCapacityLiters = 55.0,
                    preferredDistanceUnit = "km",
                    preferredVolumeUnit = "L",
                    preferredCurrency = "$",
                    isActive = false,

                    // Tax Token (EXPIRED 3 days ago!)
                    taxTokenNumber = "TX-2025-4421",
                    taxTokenExpiryTimestamp = now - dayMs * 3,
                    taxTokenNotes = "Tax renewal due - urgent renewal needed",

                    // Registration Paper
                    registrationNumber = "REG-CAR-2022-883",
                    registrationIssueTimestamp = now - dayMs * 730,
                    registrationExpiryTimestamp = now + dayMs * 90,
                    registrationNotes = "Commercial Tax Clearance Passed",

                    // Driver License
                    driverLicenseNumber = "DL-908234-A",
                    driverLicenseCategory = "Class A & B Light Motor",
                    driverLicenseExpiryTimestamp = now + dayMs * 5,
                    driverLicenseNotes = "Full Driving License"
                )

                // 3. Scooter / Commuter
                val scooter3 = Vehicle(
                    id = 3,
                    name = "Honda PCX 160",
                    vehicleType = "Scooter",
                    make = "Honda",
                    model = "PCX 160",
                    year = 2024,
                    vin = "JH2KF4009RK102834",
                    licensePlate = "SCT-102",
                    currentOdometer = 3200.0,
                    fuelCapacityLiters = 8.1,
                    preferredDistanceUnit = "km",
                    preferredVolumeUnit = "L",
                    preferredCurrency = "$",
                    isActive = false,

                    taxTokenNumber = "TX-2026-PCX01",
                    taxTokenExpiryTimestamp = now + dayMs * 210,
                    taxTokenNotes = "City Commuter Permit",

                    registrationNumber = "REG-SCT-2024-001",
                    registrationIssueTimestamp = now - dayMs * 100,
                    registrationExpiryTimestamp = now + dayMs * 265,
                    registrationNotes = "First Owner Certificate",

                    driverLicenseNumber = "DL-908234-A",
                    driverLicenseCategory = "Class A & B Light Motor",
                    driverLicenseExpiryTimestamp = now + dayMs * 5,
                    driverLicenseNotes = "Full Driving License"
                )

                dao.insertVehicle(moto1)
                dao.insertVehicle(car2)
                dao.insertVehicle(scooter3)

                // Seed Fuel Logs for Vehicle 1
                val fuel1 = FuelLog(
                    vehicleId = 1,
                    dateTimestamp = now - dayMs * 2,
                    odometer = 14820.0,
                    litersFilled = 11.8,
                    pricePerLiter = 1.85,
                    totalCost = 21.83,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Shell V-Power",
                    notes = "Highway trip to coast"
                )
                val fuel2 = FuelLog(
                    vehicleId = 1,
                    dateTimestamp = now - dayMs * 9,
                    odometer = 14540.0,
                    litersFilled = 12.2,
                    pricePerLiter = 1.82,
                    totalCost = 22.20,
                    isFullTank = true,
                    fuelType = "95 Octane",
                    stationName = "Chevron",
                    notes = "Daily commute"
                )
                dao.insertFuelLog(fuel1)
                dao.insertFuelLog(fuel2)

                // Seed Fuel Log for Vehicle 2
                val fuelCar = FuelLog(
                    vehicleId = 2,
                    dateTimestamp = now - dayMs * 5,
                    odometer = 38400.0,
                    litersFilled = 42.0,
                    pricePerLiter = 1.75,
                    totalCost = 73.50,
                    isFullTank = true,
                    fuelType = "Unleaded 91",
                    stationName = "Mobil",
                    notes = "Family road trip top up"
                )
                dao.insertFuelLog(fuelCar)

                // Seed Service Logs for Vehicle 1
                val service1 = ServiceLog(
                    vehicleId = 1,
                    dateTimestamp = now - dayMs * 30,
                    odometer = 13800.0,
                    serviceType = "14,000 km Major Service",
                    totalCost = 185.0,
                    serviceProvider = "Yamaha Pro Workshop",
                    partsServiced = "Drive chain cleaned & tensioned, Throttle body sync, Calipers lubed",
                    partsChanged = "Engine Oil (Yamalube 10W-40 2.6L), OEM Oil Filter",
                    nextServiceOdometerDue = 18800.0,
                    nextServiceDateDue = now + dayMs * 150,
                    notes = "Chain slack set to 35mm. Front tire 3.5mm tread left."
                )
                dao.insertServiceLog(service1)

                // Seed Service Log for Vehicle 2
                val serviceCar = ServiceLog(
                    vehicleId = 2,
                    dateTimestamp = now - dayMs * 40,
                    odometer = 36000.0,
                    serviceType = "35,000 km Hybrid Maintenance",
                    totalCost = 260.0,
                    serviceProvider = "Toyota Authorized Service Center",
                    partsServiced = "Hybrid Battery Health Check, Tire Rotation, Brake Inspection",
                    partsChanged = "Toyota Genuine 0W-20 Synthetic Oil, Engine & Cabin Filters",
                    nextServiceOdometerDue = 45000.0,
                    nextServiceDateDue = now + dayMs * 180,
                    notes = "Hybrid system health 100%. Brake pads 70% life."
                )
                dao.insertServiceLog(serviceCar)

                // Seed Parts for Vehicle 1
                val part1 = PartItem(
                    vehicleId = 1,
                    name = "Engine Oil (Yamalube 10W-40)",
                    partNumber = "YAM-10W40-FS",
                    brand = "Yamalube",
                    installedDateTimestamp = now - dayMs * 30,
                    installedOdometer = 13800.0,
                    cost = 48.0,
                    recommendedIntervalKm = 5000.0,
                    isReplaced = false,
                    category = "Consumable",
                    notes = "Change every 5,000 km"
                )
                val part2 = PartItem(
                    vehicleId = 1,
                    name = "Front Tire (Michelin Road 6)",
                    partNumber = "120/70 ZR17",
                    brand = "Michelin",
                    installedDateTimestamp = now - dayMs * 200,
                    installedOdometer = 7500.0,
                    cost = 175.0,
                    recommendedIntervalKm = 12000.0,
                    isReplaced = false,
                    category = "Wear Item",
                    notes = "Dual compound wet grip tire"
                )
                dao.insertPartItem(part1)
                dao.insertPartItem(part2)

                // Seed Expense
                dao.insertExpense(
                    ExpenseItem(
                        vehicleId = 1,
                        dateTimestamp = now - dayMs * 45,
                        title = "Tax Token & Fitness Fee",
                        category = "Insurance/Tax",
                        cost = 140.0,
                        odometer = 13500.0,
                        notes = "Government tax token renewal fee"
                    )
                )
            }
        }
    }
}

