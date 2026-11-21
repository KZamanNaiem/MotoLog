package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FuelGreen
import com.example.ui.theme.MotoOrange
import com.example.ui.theme.ServiceBlue
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.MonthlyExpenseBreakdown
import com.example.ui.viewmodel.MotoViewModel
import java.util.Locale

class RechartsAndroidBridge(
    private val getMonthlyDataJson: () -> String,
    private val getVehicleInfoJson: () -> String,
    private val onAddFuel: () -> Unit,
    private val onAddService: () -> Unit,
    private val onSeedDemo: () -> Unit
) {
    @JavascriptInterface
    fun getMonthlyDataJson(): String = getMonthlyDataJson()

    @JavascriptInterface
    fun getVehicleInfoJson(): String = getVehicleInfoJson()

    @JavascriptInterface
    fun openAddFuel() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onAddFuel()
        }
    }

    @JavascriptInterface
    fun openAddService() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onAddService()
        }
    }

    @JavascriptInterface
    fun seedDemoMonths() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onSeedDemo()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyRechartsScreen(
    viewModel: MotoViewModel,
    onOpenAddFuel: () -> Unit,
    onOpenAddService: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val monthlyData by viewModel.monthlyExpenseBreakdown.collectAsState()
    val activeVehicle by viewModel.activeVehicle.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewLoaded by remember { mutableStateOf(false) }
    var showNativeCardsView by remember { mutableStateOf(false) }

    // Live update Webview whenever monthly data or active vehicle changes
    LaunchedEffect(monthlyData, activeVehicle, isWebViewLoaded) {
        if (isWebViewLoaded) {
            webViewRef?.let { webView ->
                val json = viewModel.getMonthlyBreakdownJson()
                val vehName = activeVehicle.name.replace("'", "\\'")
                val curr = activeVehicle.preferredCurrency
                val script = "if (window.updateMonthlyData) { window.updateMonthlyData('$json', '$vehName', '$curr'); }"
                webView.evaluateJavascript(script, null)
            }
        }
    }

    val bridge = remember(viewModel, onOpenAddFuel, onOpenAddService) {
        RechartsAndroidBridge(
            getMonthlyDataJson = { viewModel.getMonthlyBreakdownJson() },
            getVehicleInfoJson = { viewModel.getActiveVehicleInfoJson() },
            onAddFuel = onOpenAddFuel,
            onAddService = onOpenAddService,
            onSeedDemo = { viewModel.seedDemoMonthlyHistory() }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_recharts_screen"),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onOpenAddFuel,
                    containerColor = FuelGreen,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.testTag("recharts_fab_add_fuel")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = "Log Fuel Top-Up"
                    )
                }

                FloatingActionButton(
                    onClick = onOpenAddService,
                    containerColor = MotoOrange,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.testTag("recharts_fab_add_service")
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Log Service"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Action Controls
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrange.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MotoOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Monthly Recharts Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Expenses vs Fuel • ${activeVehicle.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Switch between Recharts and Native M3 list
                        IconButton(
                            onClick = { showNativeCardsView = !showNativeCardsView },
                            modifier = Modifier.testTag("toggle_recharts_native_view")
                        ) {
                            Icon(
                                imageVector = if (showNativeCardsView) Icons.Default.InsertChart else Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = if (showNativeCardsView) "Show Recharts" else "Show List",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Seed Demo History button
                        IconButton(
                            onClick = { viewModel.seedDemoMonthlyHistory() },
                            modifier = Modifier.testTag("recharts_seed_demo_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "Seed Sample History",
                                tint = WarningYellow
                            )
                        }

                        // Reload webview button
                        IconButton(
                            onClick = {
                                webViewRef?.reload()
                            },
                            modifier = Modifier.testTag("recharts_reload_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Recharts",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Main Content Area: Recharts WebView or Native Cards List
            if (showNativeCardsView) {
                NativeMonthlyBreakdownList(
                    monthlyList = monthlyData,
                    currency = activeVehicle.preferredCurrency
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recharts_webview_container")
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                setBackgroundColor(Color.parseColor("#12141a"))

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isWebViewLoaded = true
                                        // Push immediate data
                                        val json = viewModel.getMonthlyBreakdownJson()
                                        val vehName = activeVehicle.name.replace("'", "\\'")
                                        val curr = activeVehicle.preferredCurrency
                                        val script = "if (window.updateMonthlyData) { window.updateMonthlyData('$json', '$vehName', '$curr'); }"
                                        view?.evaluateJavascript(script, null)
                                    }
                                }

                                addJavascriptInterface(bridge, "AndroidBridge")
                                loadUrl("file:///android_asset/recharts/index.html")
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                            if (isWebViewLoaded) {
                                val json = viewModel.getMonthlyBreakdownJson()
                                val vehName = activeVehicle.name.replace("'", "\\'")
                                val curr = activeVehicle.preferredCurrency
                                val script = "if (window.updateMonthlyData) { window.updateMonthlyData('$json', '$vehName', '$curr'); }"
                                webView.evaluateJavascript(script, null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun NativeMonthlyBreakdownList(
    monthlyList: List<MonthlyExpenseBreakdown>,
    currency: String
) {
    if (monthlyList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No monthly expenses logged yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("native_monthly_breakdown_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(monthlyList.reversed(), key = { it.monthKey }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.fuelEntries} fuel fills • ${String.format(Locale.US, "%.1f", item.fuelLiters)} L filled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$currency${String.format(Locale.US, "%.2f", item.total)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MotoOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar
                    if (item.total > 0) {
                        val fuelP = (item.fuel / item.total).toFloat()
                        val servP = (item.service / item.total).toFloat()
                        val partP = (item.parts / item.total).toFloat()
                        val otherP = (item.other / item.total).toFloat()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (fuelP > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(fuelP)
                                        .fillMaxHeight()
                                        .background(FuelGreen)
                                )
                            }
                            if (servP > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(servP)
                                        .fillMaxHeight()
                                        .background(ServiceBlue)
                                )
                            }
                            if (partP > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(partP)
                                        .fillMaxHeight()
                                        .background(androidx.compose.ui.graphics.Color(0xFFA855F7))
                                )
                            }
                            if (otherP > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(otherP)
                                        .fillMaxHeight()
                                        .background(WarningYellow)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.fuel > 0) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Fuel: $currency${String.format(Locale.US, "%.2f", item.fuel)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FuelGreen
                                    )
                                }
                            )
                        }
                        if (item.service > 0) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Service: $currency${String.format(Locale.US, "%.2f", item.service)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ServiceBlue
                                    )
                                }
                            )
                        }
                        if (item.other > 0 || item.parts > 0) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Other: $currency${String.format(Locale.US, "%.2f", item.parts + item.other)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarningYellow
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
