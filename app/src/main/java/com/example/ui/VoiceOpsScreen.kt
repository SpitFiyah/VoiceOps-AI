package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.data.api.ParsedIntentResponse
import com.example.data.db.InventoryEntity
import com.example.data.db.MandiPriceEntity
import com.example.data.db.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Custom vector graphics for ArrowUpward and ArrowDownward to bypass missing material elements
val MyArrowUpward: ImageVector = ImageVector.Builder(
    name = "MyArrowUpward",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).path {
    moveTo(12f, 4f)
    lineTo(5f, 11f)
    lineTo(7f, 13f)
    lineTo(11f, 9f)
    lineTo(11f, 20f)
    lineTo(13f, 20f)
    lineTo(13f, 9f)
    lineTo(17f, 13f)
    lineTo(19f, 11f)
    close()
}.build()

val MyArrowDownward: ImageVector = ImageVector.Builder(
    name = "MyArrowDownward",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).path {
    moveTo(12f, 20f)
    lineTo(19f, 13f)
    lineTo(17f, 11f)
    lineTo(13f, 15f)
    lineTo(13f, 4f)
    lineTo(11f, 4f)
    lineTo(11f, 15f)
    lineTo(7f, 11f)
    lineTo(5f, 13f)
    close()
}.build()

private fun TransactionEntity.isSale(): Boolean = category.equals("Sale", ignoreCase = true)

private fun TransactionEntity.isOutgoing(): Boolean {
    return category.equals("Expense", ignoreCase = true) ||
        category.equals("Purchase", ignoreCase = true) ||
        category.equals("Stock In", ignoreCase = true) ||
        category.equals("STOCK IN", ignoreCase = true)
}

private fun startOfDayMillis(daysAgo: Int = 0): Long {
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfMonthMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun businessHealthScore(transactions: List<TransactionEntity>, inventory: List<InventoryEntity>): Int {
    if (transactions.isEmpty() && inventory.isEmpty()) return 50

    val today = startOfDayMillis()
    val yesterday = startOfDayMillis(1)
    val todayRevenue = transactions.filter { it.isSale() && it.timestamp >= today }.sumOf { it.totalAmount }
    val yesterdayRevenue = transactions.filter { it.isSale() && it.timestamp in yesterday until today }.sumOf { it.totalAmount }
    val expenseTotal = transactions.filter { it.isOutgoing() }.sumOf { it.totalAmount }
    val revenueTotal = transactions.filter { it.isSale() }.sumOf { it.totalAmount }
    val lowStockCount = inventory.count { it.stockQuantity < 20.0 }
    val recentConsistency = transactions.count { it.timestamp >= startOfDayMillis(6) }.coerceAtMost(14)

    var score = 60
    score += when {
        todayRevenue > yesterdayRevenue && todayRevenue > 0.0 -> 12
        todayRevenue > 0.0 -> 7
        else -> 0
    }
    score += ((revenueTotal - expenseTotal) / 500.0).toInt().coerceIn(-12, 12)
    score += recentConsistency
    score -= lowStockCount * 7
    return score.coerceIn(0, 100)
}

private fun topSellingItem(transactions: List<TransactionEntity>): String {
    return transactions
        .filter { it.isSale() }
        .groupBy { it.item }
        .maxByOrNull { entry -> entry.value.sumOf { it.quantity } }
        ?.key
        ?: "No sales yet"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceOpsScreen(
    viewModel: VoiceOpsViewModel,
    onRecordClick: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val mandiPrices by viewModel.mandiPrices.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val latestResponse by viewModel.latestResponse.collectAsStateWithLifecycle()

    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val wasChangedManually by viewModel.wasChangedManually.collectAsStateWithLifecycle()

    val detectedCountryCode by viewModel.detectedCountryCode.collectAsStateWithLifecycle()
    val detectedCountryName by viewModel.detectedCountryName.collectAsStateWithLifecycle()
    val detectedCityName by viewModel.detectedCityName.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val isLocationDetected by viewModel.isLocationDetected.collectAsStateWithLifecycle()

    fun translate(key: String): String {
        return com.example.ui.Localization.translate(currentLang, key)
    }

    var activeTab by remember { mutableStateOf(0) } // 0 = Ledger, 1 = Mandi, 2 = Credit

    // Aggregate values
    val todayStart = remember { startOfDayMillis() }
    val weekStart = remember { startOfDayMillis(6) }
    val monthStart = remember { startOfMonthMillis() }
    val salesTotal = transactions.filter { it.isSale() }.sumOf { it.totalAmount }
    val udhaarTotal = transactions.filter { it.category == "Udhaar" }.sumOf { it.totalAmount }
    val expensesTotal = transactions.filter { it.isOutgoing() }.sumOf { it.totalAmount }
    val dailyRevenue = transactions.filter { it.isSale() && it.timestamp >= todayStart }.sumOf { it.totalAmount }
    val weeklyRevenue = transactions.filter { it.isSale() && it.timestamp >= weekStart }.sumOf { it.totalAmount }
    val monthlyRevenue = transactions.filter { it.isSale() && it.timestamp >= monthStart }.sumOf { it.totalAmount }
    val healthScore = businessHealthScore(transactions, inventory)
    val inventoryHealth = if (inventory.isEmpty()) 0 else {
        (((inventory.size - inventory.count { it.stockQuantity < 20.0 }).toDouble() / inventory.size) * 100).toInt()
    }
    val topSeller = topSellingItem(transactions)
    val lowStockItems = inventory.filter { it.stockQuantity < 20.0 }

    // Dynamic voice credit score logic
    val creditScore = remember(transactions) {
        if (transactions.isEmpty()) 450 else {
            val volumeCoeff = (transactions.sumOf { it.totalAmount } / 50.0).coerceAtMost(300.0)
            val countCoeff = (transactions.size * 15).coerceAtMost(100)
            (500 + volumeCoeff + countCoeff).toInt().coerceAtMost(850)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090A10),
                        Color(0xFF111422),
                        Color(0xFF05060A)
                    )
                )
            )
    ) {
        // Glowing Neon Background mesh
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.25f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF3D00).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.5f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8E24AA).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.45f, size.height * 0.85f),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Header Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "VoiceOPS AI Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(14.dp))
                        )
                        Column {
                            Text(
                                text = translate("APP_NAME"),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp,
                                    fontSize = 24.sp
                                )
                            )
                            Text(
                                text = translate("SUBTITLE"),
                                color = Color(0xB3FFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x221DE9B6))
                            .border(BorderStroke(1.dp, Color(0x331DE9B6)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DE9B6))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                translate("AI_ACTIVE"),
                                color = Color(0xFF1DE9B6),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Beautiful interactive Language Selector Scrollbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (wasChangedManually) "🌐" else "📍 Auto:",
                        fontSize = 12.sp,
                        color = Color(0x80FFFFFF),
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(com.example.ui.Localization.supportedLanguages) { lang ->
                            val isSelected = lang == currentLang
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF2196F3).copy(alpha = 0.25f) else Color(0x10FFFFFF))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF2196F3) else Color(0x1FFFFFFF)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectLanguage(lang) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lang,
                                    color = if (isSelected) Color(0xFF3FBFFA) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Dynamic Location detected banner
                Spacer(modifier = Modifier.height(4.dp))
                if (isLocationDetected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0F00E676))
                            .border(BorderStroke(1.dp, Color(0x2F00E676)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🟢 Geolocated: ${if (detectedCityName.isNotEmpty()) "$detectedCityName, " else ""}$detectedCountryName ($currencySymbol)",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0F00E5FF))
                            .border(BorderStroke(1.dp, Color(0x2F00E5FF)), RoundedCornerShape(8.dp))
                            .clickable { onRequestLocationPermission() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${translate("REGION_DEFAULT_BANNER")} ${if (detectedCountryName.isNotEmpty()) "$detectedCountryName" else "United States"} (Assumed). Tap here to use live Browser Geolocation 🛰️",
                            color = Color(0xC000E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dashboard Cards Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardCard(
                        title = translate("TOTAL_SALES"),
                        value = "${currencySymbol}${"%,.0f".format(salesTotal)}",
                        icon = MyArrowUpward,
                        iconColor = Color(0xFF1DE9B6),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        title = translate("UDHAAR_DUE"),
                        value = "${currencySymbol}${"%,.0f".format(udhaarTotal)}",
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        title = translate("EXPENSES"),
                        value = "${currencySymbol}${"%,.0f".format(expensesTotal)}",
                        icon = MyArrowDownward,
                        iconColor = Color(0xFFFFD700),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardCard(
                        title = "TODAY",
                        value = "${currencySymbol}${"%,.0f".format(dailyRevenue)}",
                        icon = Icons.Default.DateRange,
                        iconColor = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        title = "7 DAYS",
                        value = "${currencySymbol}${"%,.0f".format(weeklyRevenue)}",
                        icon = MyArrowUpward,
                        iconColor = Color(0xFF1DE9B6),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        title = "MONTH",
                        value = "${currencySymbol}${"%,.0f".format(monthlyRevenue)}",
                        icon = Icons.Default.DateRange,
                        iconColor = Color(0xFFFFD700),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color(0x121DE9B6)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Business Health",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Top seller: $topSeller",
                                    color = Color(0x99FFFFFF),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "$healthScore/100",
                                color = when {
                                    healthScore >= 75 -> Color(0xFF1DE9B6)
                                    healthScore >= 55 -> Color(0xFFFFD700)
                                    else -> Color(0xFFFF5252)
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        }

                        LinearProgressIndicator(
                            progress = { healthScore / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (healthScore >= 70) Color(0xFF1DE9B6) else Color(0xFFFFD700),
                            trackColor = Color(0x1AFFFFFF)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Inventory health $inventoryHealth%",
                                color = Color(0xB3FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (lowStockItems.isEmpty()) "Stock healthy" else "Restock: ${lowStockItems.take(2).joinToString { it.itemName }}",
                                color = if (lowStockItems.isEmpty()) Color(0xFF1DE9B6) else Color(0xFFFFA726),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Speech core module panel
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderGradient = Brush.sweepGradient(listOf(Color(0x40FFFFFF), Color(0xFF00E5FF), Color(0x40FFFFFF)))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = translate("MIC_TAP_PROMPT"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x99FFFFFF),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        MicOrbButton(
                            isProcessing = isProcessing,
                            onClick = onRecordClick,
                            modifier = Modifier.testTag("voice_ops_mic_button")
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (isProcessing) translate("TRANSLATING") else translate("TAP_TO_SPEAK_LABEL"),
                            color = if (isProcessing) Color(0xFF00E5FF) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Manual text input fallback to type transaction commands
                        var manualTextInput by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = manualTextInput,
                            onValueChange = { manualTextInput = it },
                            placeholder = { Text("Or type command...", color = Color(0x60FFFFFF), fontSize = 13.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (manualTextInput.isNotEmpty()) {
                                    IconButton(onClick = {
                                        viewModel.processVoiceCommand(manualTextInput)
                                        manualTextInput = ""
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = Color(0xFF00E5FF)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = Color(0x0FFFFFFF),
                                unfocusedContainerColor = Color(0x05FFFFFF)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = translate("DEMO_CHIPS_HEADER"),
                            fontSize = 9.sp,
                            color = Color(0x80FFFFFF),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val chips = com.example.ui.Localization.demoChipsByLanguage[currentLang]
                            ?: com.example.ui.Localization.demoChipsByLanguage["English"]!!

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chips.forEach { chip ->
                                DemoPhraseChip(
                                    label = chip.label,
                                    phrase = chip.phrase,
                                    onClick = { viewModel.processVoiceCommand(it) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Tab Slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    val tabs = listOf(
                        translate("LEDGER_STREAM_TAB"),
                        translate("MANDI_PRICES_TAB"),
                        translate("VOICE_CREDIT_TAB")
                    )
                    tabs.forEachIndexed { index, title ->
                        val selected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0x2BFFFFFF) else Color.Transparent)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (selected) Color(0x3DFFFFFF) else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { activeTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (selected) Color.White else Color(0x99FFFFFF),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector tabs
                Box(modifier = Modifier.fillMaxWidth()) {
                    when (activeTab) {
                        0 -> LedgerTab(
                            transactions = transactions,
                            inventory = inventory,
                            isSyncing = isSyncing,
                            currentLang = currentLang,
                            currencySymbol = currencySymbol,
                            onDelete = { viewModel.deleteTransaction(it) },
                            onClearAll = { viewModel.clearAllData() },
                            onSeed = { viewModel.seedSampleTransactions() },
                            onSync = { viewModel.syncLocalOfflineData() }
                        )
                        1 -> MandiTab(mandiPrices = mandiPrices, currentLang = currentLang, currencySymbol = currencySymbol)
                        2 -> CreditTab(creditScore = creditScore, volume = salesTotal + udhaarTotal, currentLang = currentLang, currencySymbol = currencySymbol)
                    }
                }
            }

            // Real-Time parsed AI feedback Overlay
            AnimatedVisibility(
                visible = latestResponse != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + shrinkVertically()
            ) {
                latestResponse?.let { response ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xB3000000))
                            .clickable { viewModel.dismissLatestResponse() }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) {},
                            containerColor = Color(0xFF121424).copy(alpha = 0.98f),
                            borderGradient = Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF3D00)))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (response.type) {
                                                "transaction" -> Icons.Default.CheckCircle
                                                "query" -> Icons.Default.Search
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = translate("VOICE_TRANSLATION_TITLE"),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                    IconButton(onClick = { viewModel.dismissLatestResponse() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x0F00E5FF))
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = translate("EXPLAINED_OUT_LOUD"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E5FF),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = response.explanation,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (response.type == "transaction") {
                                    GridFeatures(response, currentLang, currencySymbol)
                                } else if (response.type == "query" && response.queryAnswer != null) {
                                    Text(
                                        text = response.queryAnswer,
                                        color = Color(0xFFE0E0E0),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { viewModel.dismissLatestResponse() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(translate("DONE_BUTTON"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridFeatures(response: ParsedIntentResponse, currentLang: String, currencySymbol: String) {
    fun translate(key: String): String {
        return com.example.ui.Localization.translate(currentLang, key)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .border(BorderStroke(1.dp, Color(0x0DFFFFFF)), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureItem(label = translate("ITEM"), value = response.item ?: "General", modifier = Modifier.weight(1f))
            FeatureItem(label = translate("CAT"), value = response.category ?: "Sale", valueColor = if (response.category == "Udhaar") Color(0xFFFF5252) else Color(0xFF1DE9B6), modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureItem(label = translate("QTY"), value = "${response.quantity} ${response.unit}", modifier = Modifier.weight(1f))
            FeatureItem(label = translate("RATE"), value = "$currencySymbol${response.pricePerUnit}", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureItem(label = translate("TOTAL"), value = "$currencySymbol${response.totalAmount}", valueColor = Color(0xFF00E5FF), isBold = true, modifier = Modifier.weight(1f))
            if (response.partyName != null) {
                FeatureItem(label = translate("DEBT"), value = response.partyName, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FeatureItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    isBold: Boolean = false
) {
    Column(modifier = modifier.padding(vertical = 2.dp)) {
        Text(label.uppercase(), fontSize = 9.sp, color = Color(0x80FFFFFF), fontWeight = FontWeight.Bold)
        Text(
            value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerTab(
    transactions: List<TransactionEntity>,
    inventory: List<InventoryEntity>,
    isSyncing: Boolean,
    currentLang: String,
    currencySymbol: String,
    onDelete: (TransactionEntity) -> Unit,
    onClearAll: () -> Unit,
    onSeed: () -> Unit,
    onSync: () -> Unit
) {
    fun translate(key: String): String {
        return com.example.ui.Localization.translate(currentLang, key)
    }

    // Dynamic state handles for interactive search, category chips, and chronological sorting
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var sortByOption by remember { mutableStateOf("Newest") }

    // Multi-language translation labels for the timeline analytics components
    val labelSearch = when (currentLang) {
        "Hindi" -> "उत्पाद, विवरण या नाम खोजें..."
        "Hinglish" -> "Search item, details ya name..."
        "Tamil" -> "பொருள் அல்லது விவரங்களைத் தேடு..."
        else -> "Search item, text or name..."
    }
    val labelCategoryAll = when (currentLang) {
        "Hindi" -> "सभी"
        "Hinglish" -> "All"
        "Tamil" -> "அனைத்தும்"
        else -> "All"
    }
    val labelCategorySales = when (currentLang) {
        "Hindi" -> "बिक्री"
        "Hinglish" -> "Sales"
        "Tamil" -> "விற்பனை"
        else -> "Sales"
    }
    val labelCategoryExpenses = when (currentLang) {
        "Hindi" -> "खर्च / खरीद"
        "Hinglish" -> "Expenses / Purchases"
        "Tamil" -> "செலவுகள்"
        else -> "Purchase & Expense"
    }
    val labelCategoryUdhaar = when (currentLang) {
        "Hindi" -> "उधार"
        "Hinglish" -> "Udhaar"
        "Tamil" -> "உதார்"
        else -> "Udhaar"
    }
    val labelSortBy = when (currentLang) {
        "Hindi" -> "क्रमबद्ध करें:"
        "Hinglish" -> "Sort by:"
        "Tamil" -> "வரிசைப்படுத்து:"
        else -> "Sort by:"
    }
    val labelSortNewest = when (currentLang) {
        "Hindi" -> "नवीनतम"
        "Hinglish" -> "Newest"
        "Tamil" -> "புதியது"
        else -> "Newest"
    }
    val labelSortOldest = when (currentLang) {
        "Hindi" -> "पुराना"
        "Hinglish" -> "Oldest"
        "Tamil" -> "பழையது"
        else -> "Oldest"
    }
    val labelSortHighest = when (currentLang) {
        "Hindi" -> "उच्च मूल्य"
        "Hinglish" -> "Highest Val"
        "Tamil" -> "அதிகப்படியான"
        else -> "Highest Val"
    }
    val labelTotalRevenue = when (currentLang) {
        "Hindi" -> "कुल राजस्व (बिक्री)"
        "Hinglish" -> "Total Sales Revenue"
        "Tamil" -> "மொத்த விற்பனை"
        else -> "Total Sales Revenue"
    }
    val labelTotalPurchases = when (currentLang) {
        "Hindi" -> "कुल जावक (खर्च / खरीद)"
        "Hinglish" -> "Total Outgoings"
        "Tamil" -> "மொத்த செலவுகள்"
        else -> "Total Outgoings"
    }
    val labelTimeline = when (currentLang) {
        "Hindi" -> "🕒 बहीखाता घटनाक्रम"
        "Hinglish" -> "🕒 Chronological Ledger"
        "Tamil" -> "🕒 காலவரிசை பேரேடு"
        else -> "🕒 Chronological Ledger"
    }
    val labelNoResults = when (currentLang) {
        "Hindi" -> "कोई मेल खाता रिकॉर्ड नहीं मिला"
        "Hinglish" -> "No matching records found"
        "Tamil" -> "பொருத்தமான பதிவுகள் இல்லை"
        else -> "No matching records found"
    }

    // Compute metrics
    val sumSales = remember(transactions) {
        transactions.filter { it.category.equals("Sale", ignoreCase = true) }.sumOf { it.totalAmount }
    }
    val countSales = remember(transactions) {
        transactions.count { it.category.equals("Sale", ignoreCase = true) }
    }
    val sumExpenses = remember(transactions) {
        transactions.filter { 
            it.category.equals("Expense", ignoreCase = true) || 
            it.category.equals("Purchase", ignoreCase = true) || 
            it.category.equals("STOCK IN", ignoreCase = true) 
        }.sumOf { it.totalAmount }
    }
    val countExpenses = remember(transactions) {
        transactions.count { 
            it.category.equals("Expense", ignoreCase = true) || 
            it.category.equals("Purchase", ignoreCase = true) || 
            it.category.equals("STOCK IN", ignoreCase = true) 
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val unsyncedCount = transactions.count { !it.isSynced }

        // 1. Sync & Connection Panel
        GlassPanel(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            containerColor = Color(0x0DFFFFFF)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (unsyncedCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = "Sync",
                            tint = if (unsyncedCount > 0) Color(0xFFFFA726) else Color(0xFF1DE9B6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentLang == "Hindi") "क्लाउड बैकअप स्थिति" else "Cloud Backup & Sync",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (unsyncedCount > 0) {
                                    if (currentLang == "Hindi") "$unsyncedCount डेटा सिंक नहीं हुआ है (स्थानीय रूप से सुरक्षित है)" 
                                    else "$unsyncedCount logs saved locally offline"
                                } else {
                                    if (currentLang == "Hindi") "सभी डेटा सुरक्षित है और सिंक किया गया है" 
                                    else "All data fully backed up to cloud safe storage"
                                },
                                color = if (unsyncedCount > 0) Color(0xFFFFA726) else Color(0xB3FFFFFF),
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    Button(
                        onClick = onSync,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (unsyncedCount > 0) Color(0xFF1DE9B6) else Color(0x1FFFFFFF),
                            contentColor = if (unsyncedCount > 0) Color(0xFF0F172A) else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = if (unsyncedCount > 0) Color(0xFF0F172A) else Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Now",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLang == "Hindi") "सिंक करें" else "Sync",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Persistent Business Metrics Overlay (Chorological Activity tracking)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sales revenue tracker
            GlassPanel(
                modifier = Modifier.weight(1f),
                containerColor = Color(0x141DE9B6)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = labelTotalRevenue,
                        color = Color(0xB31DE9B6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%,.0f".format(sumSales)}",
                        color = Color(0xFF1DE9B6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$countSales ${if (countSales == 1) "sale record" else "sale records"}",
                        color = Color(0x66FFFFFF),
                        fontSize = 9.sp
                    )
                }
            }

            // Expense & Purchase outgoing tracker
            GlassPanel(
                modifier = Modifier.weight(1f),
                containerColor = Color(0x14FFA726)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = labelTotalPurchases,
                        color = Color(0xB3FFA726),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%,.0f".format(sumExpenses)}",
                        color = Color(0xFFFFA726),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$countExpenses ${if (countExpenses == 1) "expense record" else "expense records"}",
                        color = Color(0x66FFFFFF),
                        fontSize = 9.sp
                    )
                }
            }
        }

        // 3. Inventory Stocks Panel
        if (inventory.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLang == "Hindi") "📦 उपलब्ध स्टॉक इनवेंटरी" else "📦 Available Stock Inventory",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${inventory.size} items",
                        color = Color(0x80FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(inventory) { item ->
                        GlassPanel(
                            modifier = Modifier.width(110.dp),
                            containerColor = Color(0x19FFFFFF)
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (item.stockQuantity < 20.0) Color(0x33FF5252) else Color(0x1F1DE9B6)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (item.stockQuantity < 20.0) "LOW" else "STOCK",
                                        color = if (item.stockQuantity < 20.0) Color(0xFFFF5252) else Color(0xFF1DE9B6),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = item.itemName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${"%,.1f".format(item.stockQuantity)} ${item.unit}",
                                    color = if (item.stockQuantity < 20.0) Color(0xFFFF5252) else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (item.isSynced) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (item.isSynced) Color(0x991DE9B6) else Color(0x99FFA726),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (item.isSynced) "Synced" else "Offline",
                                        color = if (item.isSynced) Color(0x66FFFFFF) else Color(0x66FFA726),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0x33FFFFFF),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = translate("LEDGER_EMPTY_TITLE"),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = translate("LEDGER_EMPTY_DESC"),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color(0xB3FFFFFF)
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSeed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x15FFFFFF),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(translate("LOAD_SAMPLE_BUTTON"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 4. Interactive Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(labelSearch, color = Color(0x4DFFFFFF), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0x80FFFFFF), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0FFFFFFF),
                        unfocusedContainerColor = Color(0x05FFFFFF),
                        focusedBorderColor = Color(0x33FFFFFF),
                        unfocusedBorderColor = Color(0x10FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ledger_search_field")
                )

                // 5. Category filter chips
                val categoriesList = listOf(
                    Triple("All", labelCategoryAll, transactions.size),
                    Triple("Sale", labelCategorySales, transactions.count { it.category.equals("Sale", ignoreCase = true) }),
                    Triple("Expense", labelCategoryExpenses, transactions.count { 
                        it.category.equals("Expense", ignoreCase = true) || 
                        it.category.equals("Purchase", ignoreCase = true) || 
                        it.category.equals("STOCK IN", ignoreCase = true) 
                    }),
                    Triple("Udhaar", labelCategoryUdhaar, transactions.count { it.category.equals("Udhaar", ignoreCase = true) })
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriesList) { (key, label, count) ->
                        val isSelected = selectedCategoryFilter == key
                        val outlineColor = if (isSelected) Color(0xFF1DE9B6) else Color(0x15FFFFFF)
                        val bgColor = if (isSelected) Color(0x1F1DE9B6) else Color(0x0AFFFFFF)
                        val textColor = if (isSelected) Color.White else Color(0x80FFFFFF)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .border(BorderStroke(1.dp, outlineColor), RoundedCornerShape(16.dp))
                                .clickable { selectedCategoryFilter = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0x2BFFFFFF) else Color(0x0FFFFFFF))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        color = if (isSelected) Color.White else Color(0x4DFFFFFF),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Sorting selector and timeline title
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labelTimeline,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = labelSortBy,
                            fontSize = 9.sp,
                            color = Color(0x55FFFFFF),
                            fontWeight = FontWeight.Medium
                        )
                        
                        listOf(
                            "Newest" to labelSortNewest,
                            "Oldest" to labelSortOldest,
                            "Highest" to labelSortHighest
                        ).forEachIndexed { index, (key, text) ->
                            val isSelected = sortByOption == key
                            Text(
                                text = text,
                                color = if (isSelected) Color(0xFF1DE9B6) else Color(0x40FFFFFF),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { sortByOption = key }
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            )
                            if (index < 2) {
                                Text("|", color = Color(0x10FFFFFF), fontSize = 8.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Perform client-side filter and sorting based on dynamic query parameters
                val filteredAndSortedList = remember(transactions, searchQuery, selectedCategoryFilter, sortByOption) {
                    var list = transactions
                    
                    // Filter by category
                    if (selectedCategoryFilter != "All") {
                        list = if (selectedCategoryFilter == "Expense") {
                            list.filter { 
                                it.category.equals("Expense", ignoreCase = true) || 
                                it.category.equals("Purchase", ignoreCase = true) || 
                                it.category.equals("STOCK IN", ignoreCase = true) 
                            }
                        } else {
                            list.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                        }
                    }
                    
                    // Filter by search query text (supporting item name, raw speech, partys etc.)
                    if (searchQuery.isNotEmpty()) {
                        val queryLower = searchQuery.lowercase(Locale.getDefault())
                        list = list.filter { 
                            it.item.lowercase(Locale.getDefault()).contains(queryLower) || 
                            it.rawVoiceText.lowercase(Locale.getDefault()).contains(queryLower) ||
                            (it.partyName?.lowercase(Locale.getDefault())?.contains(queryLower) ?: false)
                        }
                    }
                    
                    // Sort (Newest, Oldest or Highest totalAmount)
                    when (sortByOption) {
                        "Newest" -> list.sortedByDescending { it.timestamp }
                        "Oldest" -> list.sortedBy { it.timestamp }
                        "Highest" -> list.sortedByDescending { it.totalAmount }
                        else -> list.sortedByDescending { it.timestamp }
                    }
                }

                if (filteredAndSortedList.isEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0x22FFFFFF),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = labelNoResults,
                            color = Color(0x4DFFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredAndSortedList.size} " + translate("ACTIVE_RECORDS"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x40FFFFFF),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = translate("CLEAR_ALL"),
                            color = Color(0x99FF5252),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onClearAll() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredAndSortedList.forEach { tx ->
                            LedgerRowItem(
                                tx = tx,
                                currencySymbol = currencySymbol,
                                currentLang = currentLang,
                                onDelete = { onDelete(tx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerRowItem(
    tx: TransactionEntity,
    currencySymbol: String,
    currentLang: String,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(tx.timestamp) { formatter.format(Date(tx.timestamp)) }

    // Dynamic relative timestamp calculations for a clear timeline look
    val relativeTime = remember(tx.timestamp) {
        val now = System.currentTimeMillis()
        val diff = now - tx.timestamp
        val oneDay = 86400000L
        when {
            diff < 60000L -> if (currentLang == "Hindi") "अभी-अभी" else "Just now"
            diff < 3600000L -> {
                val mins = diff / 60000L
                if (currentLang == "Hindi") "$mins मिनट पहले" else "$mins mins ago"
            }
            diff < oneDay && SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now)) == SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(tx.timestamp)) -> {
                if (currentLang == "Hindi") "आज" else "Today"
            }
            diff < 2 * oneDay -> if (currentLang == "Hindi") "कल" else "Yesterday"
            else -> {
                val days = (diff / oneDay).toInt()
                if (days < 7) {
                    if (currentLang == "Hindi") "$days दिन पहले" else "$days days ago"
                } else null
            }
        }
    }

    val finalTimeStr = if (relativeTime != null) "$relativeTime • $dateStr" else dateStr

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color(0x19FFFFFF)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (tx.category) {
                            "Sale" -> Color(0x1A1DE9B6)
                            "Udhaar" -> Color(0x1AFF5252)
                            else -> Color(0x1AFFD700)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tx.category) {
                        "Sale" -> MyArrowUpward
                        "Udhaar" -> Icons.Default.Info
                        else -> MyArrowDownward
                    },
                    contentDescription = null,
                    tint = when (tx.category) {
                        "Sale" -> Color(0xFF1DE9B6)
                        "Udhaar" -> Color(0xFFFF5252)
                        else -> Color(0xFFFFD700)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.item,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x12FFFFFF))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = tx.category.uppercase(),
                            color = Color(0x99FFFFFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\"${tx.rawVoiceText}\"",
                    color = Color(0xB3FFFFFF),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$finalTimeStr • ${tx.quantity} ${tx.unit} @ $currencySymbol${tx.pricePerUnit}",
                    color = Color(0x66FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${"%,.0f".format(tx.totalAmount)}",
                    color = when (tx.category) {
                        "Sale" -> Color(0xFF1DE9B6)
                        "Udhaar" -> Color(0xFFFF5252)
                        else -> Color(0xFFFFD700)
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tx.isSynced) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Synced to Cloud",
                            tint = Color(0x991DE9B6),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Cloud",
                            color = Color(0x991DE9B6),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Saved offline",
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Local",
                            color = Color(0xFFFFA726),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0x4DFFFFFF),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun MandiTab(mandiPrices: List<MandiPriceEntity>, currentLang: String, currencySymbol: String) {
    fun translate(key: String): String {
        return com.example.ui.Localization.translate(currentLang, key)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = translate("LIVE_COMMODITY_INDICATOR"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x80FFFFFF),
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            mandiPrices.forEach { crop ->
                MandiRowItem(crop = crop, currencySymbol = currencySymbol)
            }
        }
    }
}

@Composable
fun MandiRowItem(crop: MandiPriceEntity, currencySymbol: String) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateStr = remember(crop.lastUpdated) { formatter.format(Date(crop.lastUpdated)) }

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color(0x19FFFFFF)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = crop.cropName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x12FFFFFF))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = crop.state.uppercase(),
                            color = Color(0x80FFFFFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mandi: ${crop.marketName} • Updated $dateStr",
                    color = Color(0x66FFFFFF),
                    fontSize = 10.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${"%.2f".format(crop.price)} / ${crop.unit}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (crop.priceChangeTrend) {
                                        "UP" -> Color(0xFF1DE9B6)
                                        "DOWN" -> Color(0xFFFF5252)
                                        else -> Color(0xFFB0BEC5)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (crop.priceChangeTrend) {
                                "UP" -> "+3.2% UP"
                                "DOWN" -> "-1.5% DOWN"
                                else -> "STABLE"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (crop.priceChangeTrend) {
                                "UP" -> Color(0xFF1DE9B6)
                                "DOWN" -> Color(0xFFFF5252)
                                else -> Color(0xFFB0BEC5)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreditTab(creditScore: Int, volume: Double, currentLang: String, currencySymbol: String) {
    val progress = remember(creditScore) { (creditScore - 300) / 550f }
    val progressAnim = remember { Animatable(0f) }

    fun translate(key: String): String {
        return com.example.ui.Localization.translate(currentLang, key)
    }
    
    LaunchedEffect(creditScore) {
        progressAnim.animateTo(
            targetValue = progress,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = translate("CREDIT_ELIGIBILITY_TITLE"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x80FFFFFF),
            letterSpacing = 0.5.sp
        )

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = translate("CREDIT_SCORE_TITLE"),
                    fontSize = 11.sp,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(18.dp))

                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(140.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color(0x15FFFFFF),
                                startAngle = -210f,
                                sweepAngle = 240f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx())
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFF8E24AA), Color(0xFF00E5FF), Color(0xFF1DE9B6))
                                ),
                                startAngle = -210f,
                                sweepAngle = 240f * progressAnim.value,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx())
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = creditScore.toString(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when {
                                creditScore >= 750 -> translate("EXCELLENT")
                                creditScore >= 650 -> translate("GOOD_TIER")
                                else -> translate("NEEDS_MORE_DATA")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                creditScore >= 750 -> Color(0xFF1DE9B6)
                                creditScore >= 650 -> Color(0xFFFFD700)
                                else -> Color(0xFFFF5252)
                            },
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${translate("CRYPTO_VOLUME")} $currencySymbol${"%,.0f".format(volume)}",
                    fontSize = 11.sp,
                    color = Color(0xB3FFFFFF)
                )
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth(), containerColor = Color(0x19FFFFFF)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = translate("PARTNER_INTEGRATION_TITLE"),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Divider(color = Color(0x1AFFFFFF))

                PartnerItem(
                    partnerName = "Reliance JioMart Supply Credit",
                    desc = "Your daily voice transcripts structure real-time supply limits to purchase wholesale stocks instantly."
                )
                PartnerItem(
                    partnerName = "HDFC / SBI Rural FinTech Access",
                    desc = "Passive ledger logs authenticate consistent cashflows, bypassing physical paperwork check criteria."
                )
            }
        }

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x3300E5FF),
                contentColor = Color(0xFF00E5FF)
            ),
            border = BorderStroke(1.dp, Color(0x6600E5FF)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF00E5FF))
            Spacer(modifier = Modifier.width(8.dp))
            Text(translate("APPLY_LOAN_BUTTON"), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun PartnerItem(partnerName: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF1DE9B6),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(partnerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, color = Color(0x99FFFFFF), fontSize = 11.sp)
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x13FFFFFF))
            .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 8.sp,
                    color = Color(0x80FFFFFF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0x13FFFFFF),
    borderGradient: Brush = Brush.linearGradient(listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF))),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .border(BorderStroke(1.dp, borderGradient), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun MicOrbButton(
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isProcessing) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(110.dp)
            .clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            val scaleVal = if (isProcessing) 1.25f else scale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = if (isProcessing) 0.45f else 0.35f),
                        Color.Transparent
                    )
                ),
                radius = (size.width / 2f) * scaleVal
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x3DFFFFFF))
                .border(
                    BorderStroke(
                        2.dp,
                        if (isProcessing) Color(0xFF00E5FF) else Color(0x66FFFFFF)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = if (isProcessing) Color(0xFF00E5FF) else Color.White,
                    topLeft = Offset(w * 0.35f, h * 0.15f),
                    size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.43f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f, w * 0.15f)
                )
                drawArc(
                    color = if (isProcessing) Color(0xFF00E5FF) else Color.White,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.23f),
                    size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.43f),
                    style = Stroke(width = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                drawLine(
                    color = if (isProcessing) Color(0xFF00E5FF) else Color.White,
                    start = Offset(w * 0.5f, h * 0.65f),
                    end = Offset(w * 0.5f, h * 0.88f),
                    strokeWidth = w * 0.08f
                )
            }
        }

        if (isProcessing) {
            Canvas(modifier = Modifier.size(86.dp)) {
                drawArc(
                    color = Color(0xFF00E5FF),
                    startAngle = angle,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun DemoPhraseChip(
    label: String,
    phrase: String,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0AFFFFFF))
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(10.dp))
            .clickable { onClick(phrase) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xCC00E5FF),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
