package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.PurchaseDatePrecision
import com.example.ui.components.ModelYearDropdownField
import com.example.ui.components.PurchaseDatePickerField
import com.example.ui.theme.MotoOrange
import java.util.*

data class CountryConfig(
    val name: String,
    val flagEmoji: String,
    val currencySymbol: String,
    val currencyCode: String,
    val distanceUnit: String, // "km" or "mi"
    val volumeUnit: String    // "L" or "gal"
)

data class VehicleTypeItem(
    val id: String,
    val label: String,
    val emoji: String,
    val icon: ImageVector,
    val description: String
)

val PopularCountries = listOf(
    CountryConfig("United States", "🇺🇸", "$", "USD", "mi", "gal"),
    CountryConfig("United Kingdom", "🇬🇧", "£", "GBP", "mi", "L"),
    CountryConfig("Canada", "🇨🇦", "CA$", "CAD", "km", "L"),
    CountryConfig("Germany (EU)", "🇩🇪", "€", "EUR", "km", "L"),
    CountryConfig("France", "🇫🇷", "€", "EUR", "km", "L"),
    CountryConfig("Australia", "🇦🇺", "A$", "AUD", "km", "L"),
    CountryConfig("Japan", "🇯🇵", "¥", "JPY", "km", "L"),
    CountryConfig("India", "🇮🇳", "₹", "INR", "km", "L"),
    CountryConfig("Bangladesh", "🇧🇩", "৳", "BDT", "km", "L"),
    CountryConfig("Pakistan", "🇵🇰", "₨", "PKR", "km", "L"),
    CountryConfig("Brazil", "🇧🇷", "R$", "BRL", "km", "L"),
    CountryConfig("Mexico", "🇲🇽", "Mex$", "MXN", "km", "L"),
    CountryConfig("Saudi Arabia", "🇸🇦", "SAR", "SAR", "km", "L"),
    CountryConfig("United Arab Emirates", "🇦🇪", "AED", "AED", "km", "L"),
    CountryConfig("South Africa", "🇿🇦", "R", "ZAR", "km", "L"),
    CountryConfig("Singapore", "🇸🇬", "S$", "SGD", "km", "L"),
    CountryConfig("Malaysia", "🇲🇾", "RM", "MYR", "km", "L"),
    CountryConfig("Philippines", "🇵🇭", "₱", "PHP", "km", "L"),
    CountryConfig("Indonesia", "🇮🇩", "Rp", "IDR", "km", "L"),
    CountryConfig("Global / Other", "🌐", "$", "USD", "km", "L")
)

val SupportedVehicleTypes = listOf(
    VehicleTypeItem("Motorcycle", "Motorcycle / Bike", "🏍️", Icons.Default.TwoWheeler, "Cruiser, Sportbike, Dirtbike"),
    VehicleTypeItem("Car", "Car / Sedan", "🚗", Icons.Default.DirectionsCar, "Sedan, Hatchback, Coupe"),
    VehicleTypeItem("SUV", "SUV / Crossover", "🚙", Icons.Default.DirectionsCar, "Compact, Mid-size, Full SUV"),
    VehicleTypeItem("Bus", "Bus", "🚌", Icons.Default.DirectionsBus, "Coach, Minibus, Transit"),
    VehicleTypeItem("Boat", "Boat / Watercraft", "🚤", Icons.Default.DirectionsBoat, "Speedboat, Yacht, Jet Ski"),
    VehicleTypeItem("Truck", "Truck / Pickup", "🛻", Icons.Default.LocalShipping, "Pickup, Semi, Heavy Duty"),
    VehicleTypeItem("Scooter", "Scooter / Moped", "🛵", Icons.Default.TwoWheeler, "Urban Commuter, 125cc-300cc"),
    VehicleTypeItem("EV", "EV / Electric", "⚡", Icons.Default.ElectricCar, "Electric Car, e-Motorcycle"),
    VehicleTypeItem("Van", "Van / Minivan", "🚐", Icons.Default.AirportShuttle, "Passenger Van, Cargo, Camper"),
    VehicleTypeItem("Other", "Other Vehicle", "🚜", Icons.Default.Build, "ATV, Tractor, Custom Rig")
)

val ColorPresets = listOf(
    Pair("Matte Black", Color(0xFF1E1E1E)),
    Pair("Pearl White", Color(0xFFF0F0F0)),
    Pair("Racing Red", Color(0xFFD32F2F)),
    Pair("Yamaha Blue", Color(0xFF1976D2)),
    Pair("Silver Grey", Color(0xFF9E9E9E)),
    Pair("Forest Green", Color(0xFF2E7D32)),
    Pair("Sunset Orange", Color(0xFFFF6F00)),
    Pair("Solar Yellow", Color(0xFFFBC02D)),
    Pair("Midnight Purple", Color(0xFF4A148C))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (
        name: String,
        country: String,
        currencySymbol: String,
        distanceUnit: String,
        volumeUnit: String,
        vehicleType: String,
        model: String,
        year: Int?,
        color: String,
        boughtOnTimestamp: Long?,
        purchaseDatePrecision: String,
        numberPlate: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // Step state: 1 or 2
    var currentStep by remember { mutableStateOf(1) }

    // Step 1 states
    var ownerName by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(PopularCountries[0]) }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var selectedVehicleType by remember { mutableStateOf(SupportedVehicleTypes[0]) }
    var showVehicleTypeDropdown by remember { mutableStateOf(false) }

    // Step 2 states
    var vehicleModel by remember { mutableStateOf("") }
    var selectedModelYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedColor by remember { mutableStateOf(ColorPresets[0].first) }
    var customColorText by remember { mutableStateOf("") }
    var purchaseDateTimestamp by remember { mutableStateOf<Long?>(null) }
    var purchaseDatePrecision by remember { mutableStateOf(PurchaseDatePrecision.FULL_DATE) }
    var numberPlate by remember { mutableStateOf("") }

    val effectiveColor = if (customColorText.isNotBlank()) customColorText else selectedColor

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo_road_gear_1787706553297),
                            contentDescription = "PitStop Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PitStop Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentStep == 1) "Step 1 of 2: Owner & Region" else "Step 2 of 2: Vehicle Specs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Progress indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MotoOrange)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (currentStep == 2) MotoOrange else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentStep,
            label = "onboarding_step_anim",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { step ->
            if (step == 1) {
                // --- STEP 1: Name, Country, Vehicle Type ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
                ) {
                    item {
                        // Header Banner
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo_road_gear_1787706553297),
                                    contentDescription = "PitStop Logo",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Column {
                                    Text(
                                        text = "Welcome to PitStop!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Let's set up your profile and primary vehicle to customize currency and maintenance tracking.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 1. Name Input
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "1. YOUR NAME / NICKNAME *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("Owner / Driver Name") },
                                placeholder = { Text("e.g. Alex Hunter, John") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_name_input")
                            )
                        }
                    }

                    // 2. Country Selector (Sets Currency and Distance Units)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "2. COUNTRY (SETS CURRENCY & UNITS) *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { showCountryDropdown = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_country_selector")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = selectedCountry.flagEmoji,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            Column {
                                                Text(
                                                    text = selectedCountry.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Currency: ${selectedCountry.currencySymbol} (${selectedCountry.currencyCode}) • Distance: ${selectedCountry.distanceUnit}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Country"
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showCountryDropdown,
                                    onDismissRequest = { showCountryDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    Text(
                                        text = "CHOOSE YOUR COUNTRY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    PopularCountries.forEach { country ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(country.flagEmoji, style = MaterialTheme.typography.titleMedium)
                                                    Column {
                                                        Text(country.name, fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            "${country.currencySymbol} (${country.currencyCode}) • ${country.distanceUnit}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedCountry = country
                                                showCountryDropdown = false
                                            },
                                            trailingIcon = {
                                                if (country.name == selectedCountry.name) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MotoOrange)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Vehicle Type Selector (Dropdown & Quick Select Chips)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "3. VEHICLE TYPE *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Quick selection cards
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SupportedVehicleTypes.take(5).forEach { vType ->
                                    val isSelected = selectedVehicleType.id == vType.id
                                    Surface(
                                        onClick = { selectedVehicleType = vType },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MotoOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MotoOrange) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(text = vType.emoji, style = MaterialTheme.typography.titleMedium)
                                                Column {
                                                    Text(
                                                        text = vType.label,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) MotoOrange else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = vType.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MotoOrange,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // More vehicle types dropdown trigger
                            Box(modifier = Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = { showVehicleTypeDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("More Vehicle Types (Truck, Scooter, EV, Van, Boat, etc.)")
                                }

                                DropdownMenu(
                                    expanded = showVehicleTypeDropdown,
                                    onDismissRequest = { showVehicleTypeDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    SupportedVehicleTypes.forEach { vType ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(vType.emoji)
                                                    Text(vType.label, fontWeight = FontWeight.Medium)
                                                }
                                            },
                                            onClick = {
                                                selectedVehicleType = vType
                                                showVehicleTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Next Button
                    item {
                        Button(
                            onClick = {
                                if (ownerName.isBlank()) {
                                    ownerName = "Driver"
                                }
                                currentStep = 2
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = "Next: Vehicle Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                }
            } else {
                // --- STEP 2: Vehicle Model, Model Year, Colour, Bought On, Number Plate ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
                ) {
                    item {
                        // Summary of Step 1
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(selectedVehicleType.emoji, style = MaterialTheme.typography.titleMedium)
                                    Column {
                                        Text(
                                            text = "$ownerName's ${selectedVehicleType.label}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${selectedCountry.flagEmoji} ${selectedCountry.name} (${selectedCountry.currencySymbol} • ${selectedCountry.distanceUnit})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { currentStep = 1 }
                                ) {
                                    Text("Change")
                                }
                            }
                        }
                    }

                    // 1. Vehicle Model Input
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "VEHICLE MODEL *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = vehicleModel,
                                onValueChange = { vehicleModel = it },
                                label = { Text("Model / Trim Name") },
                                placeholder = {
                                    Text(
                                        when (selectedVehicleType.id) {
                                            "Motorcycle" -> "e.g. Yamaha MT-07 / Honda CB500X"
                                            "Car" -> "e.g. Honda Civic / Toyota Corolla"
                                            "SUV" -> "e.g. Toyota RAV4 / Ford Explorer"
                                            "Boat" -> "e.g. Yamaha 242X / Sea-Doo GTX"
                                            "Bus" -> "e.g. Mercedes Tourismo / Volvo 9700"
                                            else -> "e.g. Model / Spec Name"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = selectedVehicleType.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_model_input")
                            )
                        }
                    }

                    // 2. Model Year Dropdown
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "MODEL YEAR (1950 – NEXT YEAR)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ModelYearDropdownField(
                                selectedYear = selectedModelYear,
                                onYearSelected = { selectedModelYear = it },
                                label = "Model Year",
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "onboarding_year_dropdown"
                            )
                        }
                    }

                    // 3. Colour
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "VEHICLE COLOUR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Visual Colour Swatches
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(ColorPresets) { (name, color) ->
                                    val isSelected = selectedColor == name && customColorText.isBlank()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                selectedColor = name
                                                customColorText = ""
                                            }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) MotoOrange else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (color == Color(0xFFF0F0F0) || color == Color(0xFFFBC02D)) Color.Black else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = name.split(" ").last(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MotoOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Custom color input field
                            OutlinedTextField(
                                value = customColorText,
                                onValueChange = { customColorText = it },
                                label = { Text("Or Custom Colour Name") },
                                placeholder = { Text("e.g. Sonic Silver, Matte Olive") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_color_input")
                            )
                        }
                    }

                    // 4. Purchased On Calendar Popup (Full Date / Month+Year / Year Only)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PURCHASED ON / BOUGHT DATE (OPTIONAL)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            PurchaseDatePickerField(
                                selectedTimestamp = purchaseDateTimestamp,
                                precision = purchaseDatePrecision,
                                onDateSelected = { ts, prec ->
                                    purchaseDateTimestamp = ts
                                    purchaseDatePrecision = prec
                                },
                                label = "Purchased On (Exact Date, Month & Year, or Year)",
                                allowClear = true,
                                testTagPrefix = "onboarding_purchase_date"
                            )
                        }
                    }

                    // 5. Number (Optional: License Plate / Registration / Hull #)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "NUMBER PLATE / REGISTRATION (OPTIONAL)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = numberPlate,
                                onValueChange = { numberPlate = it },
                                label = { Text(if (selectedVehicleType.id == "Boat") "Vessel / Hull #" else "License / Number Plate") },
                                placeholder = { Text("e.g. ABC-1234 / DL-04-AB") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_plate_input")
                            )
                        }
                    }

                    // Action Buttons (Back and Finish Setup)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    val yearInt = selectedModelYear
                                    val finalModelName = if (vehicleModel.isNotBlank()) vehicleModel else "${selectedVehicleType.label} $yearInt"

                                    onComplete(
                                        ownerName.ifBlank { "Driver" },
                                        selectedCountry.name,
                                        selectedCountry.currencySymbol,
                                        selectedCountry.distanceUnit,
                                        selectedCountry.volumeUnit,
                                        selectedVehicleType.label,
                                        finalModelName,
                                        yearInt,
                                        effectiveColor,
                                        purchaseDateTimestamp,
                                        purchaseDatePrecision.name,
                                        numberPlate
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MotoOrange),
                                modifier = Modifier
                                    .weight(1.8f)
                                    .height(52.dp)
                                    .testTag("onboarding_finish_button")
                            ) {
                                Text(
                                    text = "Start Using PitStop",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
