package com.hoang.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("hoang_calculator", MODE_PRIVATE)
        if (!prefs.getBoolean("storage_permission_asked", false) && Build.VERSION.SDK_INT <= 29) {
            prefs.edit().putBoolean("storage_permission_asked", true).apply()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
            }
        }
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val dataState = remember { mutableStateOf(AppData()) }
    val loaded = remember { mutableStateOf(false) }
    val splash = remember { mutableStateOf(true) }
    val screen = remember { mutableStateOf("home") }
    val revision = remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val data = withContext(Dispatchers.IO) { AppStorage.load(context) }
        dataState.value = data
        if (data.forceKeepAlive) KeepAliveService.start(context)
        if (data.openSeasonIds.isEmpty() && data.seasons.isNotEmpty()) {
            data.openSeasonIds.add(data.seasons.first().id)
            data.selectedSeasonId = data.seasons.first().id
        }
        loaded.value = true
        if (!data.splashEnabled) splash.value = false
        else delay(3000)
    }

    fun persist() {
        if (dataState.value.autoSave) {
            scope.launch(Dispatchers.IO) { AppStorage.save(context, dataState.value) }
        }
    }

    fun newTemporarySeason(): Season {
        val season = Season(UUID.randomUUID().toString(), "", "Temporary Panel", "Temporary season")
        val data = dataState.value
        data.seasons.add(season)
        data.openSeasonIds.add(season.id)
        data.selectedSeasonId = season.id
        revision.intValue++
        persist()
        return season
    }

    fun currentSeason(): Season? {
        val id = dataState.value.selectedSeasonId
        return dataState.value.seasons.firstOrNull { it.id == id }
    }

    if (!loaded.value || splash.value) {
        SplashScreen(enabled = dataState.value.splashEnabled) {
            splash.value = false
        }
    } else {
        Scaffold(
            topBar = {
                Surface(tonalElevation = 3.dp) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { screen.value = "seasons" }) {
                                Icon(Icons.Default.List, contentDescription = "Season List")
                            }
                            IconButton(onClick = {
                                screen.value = "home"
                                if (currentSeason() == null) newTemporarySeason()
                            }) {
                                Icon(Icons.Default.Home, contentDescription = "Home")
                            }
                            IconButton(onClick = { screen.value = "settings" }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            Spacer(Modifier.weight(1f))
                            Text("v0.0.10", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        if (dataState.value.openSeasonIds.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                dataState.value.openSeasonIds.forEach { id ->
                                    val season = dataState.value.seasons.firstOrNull { it.id == id } ?: return@forEach
                                    FilterChip(
                                        selected = dataState.value.selectedSeasonId == id,
                                        onClick = { dataState.value.selectedSeasonId = id; screen.value = "home"; revision.intValue++ },
                                        label = { Text(season.name.ifBlank { season.panelName }) },
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Close season",
                                                modifier = Modifier.size(16.dp).clickable {
                                                    dataState.value.openSeasonIds.remove(id)
                                                    if (dataState.value.selectedSeasonId == id) dataState.value.selectedSeasonId =
                                                        dataState.value.openSeasonIds.lastOrNull()
                                                    revision.intValue++
                                                    persist()
                                                })
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                revision.intValue
                when (screen.value) {
                    "seasons" -> SeasonListScreen(dataState.value,
                        onOpen = { id ->
                            if (!dataState.value.openSeasonIds.contains(id)) dataState.value.openSeasonIds.add(id)
                            dataState.value.selectedSeasonId = id
                            screen.value = "home"
                        },
                        onChange = { revision.intValue++; persist() },
                        onCreate = {
                            val season = newTemporarySeason()
                            screen.value = "home"
                        })
                    "settings" -> SettingsScreen(dataState.value, onChange = { revision.intValue++; persist() })
                    else -> {
                        val season = currentSeason() ?: newTemporarySeason()
                        SeasonPanel(season, dataState.value, dataState.value.builtinKeypad, onChange = {
                            season.lastEdited = System.currentTimeMillis()
                            revision.intValue++
                            persist()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(enabled: Boolean, onSkip: () -> Unit) {
    val progress = rememberInfiniteTransition(label = "splash").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    Column(
        Modifier.fillMaxSize().background(Color.White).clickable { onSkip() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (progress.value < 0.4f) "\uD83D\uDC13" else if (progress.value < 0.65f) "\uD83C\uDF27️\uD83D\uDC13" else "\uD83C\uDF57", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.width(180.dp))
        Spacer(Modifier.height(12.dp))
        Text("Hoàng Calculator")
    }
}

@Composable
private fun SeasonListScreen(
    data: AppData,
    onOpen: (String) -> Unit,
    onChange: () -> Unit,
    onCreate: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Last edited") }
    val shown = remember(query, sort, data.seasons.size, data.seasons.map { it.lastEdited }.hashCode()) {
        data.seasons.filter { it.name.contains(query, true) || it.panelName.contains(query, true) }
            .sortedWith(if (sort == "Name") compareBy { it.name.lowercase() }
            else compareByDescending { it.lastEdited })
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it }, Modifier.weight(1f), label = { Text("Search") })
            IconButton(onClick = onCreate) { Icon(Icons.Default.Add, contentDescription = "New season") }
        }
        Row {
            listOf("Last edited", "Name", "Profit").forEach {
                FilterChip(selected = sort == it, onClick = { sort = it }, label = { Text(it) }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        LazyColumn {
            itemsIndexed(shown) { _, season ->
                ElevatedCard(Modifier.fillWidthPadding().padding(vertical = 4.dp).clickable { onOpen(season.id) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (season.pinned) Icons.Default.Star else Icons.Default.Description, null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(season.name.ifBlank { season.panelName.ifBlank { "Temporary season" } }, fontWeight = FontWeight.Bold)
                            Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(season.lastEdited)), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { season.pinned = !season.pinned; season.lastEdited = System.currentTimeMillis(); onChange() }) {
                            Icon(if (season.pinned) Icons.Default.Star else Icons.Default.StarOutline, contentDescription = "Pin")
                        }
                        IconButton(onClick = {
                            val copy = season.copy(id = UUID.randomUUID().toString(), name = season.name + " Copy",
                                inputs = season.inputs.map { it.copy(id = UUID.randomUUID().toString()) }.toMutableList(),
                                outputs = season.outputs.map { it.copy(id = UUID.randomUUID().toString()) }.toMutableList())
                            data.seasons.add(copy); onChange()
                        }) { Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate") }
                        IconButton(onClick = { data.seasons.remove(season); data.openSeasonIds.remove(season.id); onChange() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.fillWidthPadding(): Modifier = this.fillMaxWidth()

@Composable
private fun SeasonPanel(season: Season, appData: AppData, builtinKeypad: Boolean, onChange: () -> Unit) {
    var quickSettings by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }
    var reloadDialog by remember { mutableStateOf(false) }
    val undoHistory = remember(season.id) { mutableStateListOf<Season>() }
    val redoHistory = remember(season.id) { mutableStateListOf<Season>() }
    var lastSnapshot by remember(season.id) { mutableStateOf(season.snapshot()) }
    val recordChange: () -> Unit = {
        undoHistory.add(lastSnapshot)
        lastSnapshot = season.snapshot()
        redoHistory.clear()
        onChange()
    }
    val onChange = recordChange
    val result = remember(season.settings, season.inputs.size, season.outputs.size,
        season.inputs.map { Triple(it.price, it.amountPerCraft, it.useReturnRate) },
        season.outputs.map { it.price }) {
        Calculator.calculate(season.settings, season.inputs, season.outputs)
    }

    if (saveDialog) {
        var name by remember { mutableStateOf(season.name) }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text("Save season") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Season name") }) },
            confirmButton = { TextButton(onClick = {
                season.name = name; season.lastEdited = System.currentTimeMillis(); onChange(); saveDialog = false
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { saveDialog = false }) { Text("Cancel") } }
        )
    }
    if (reloadDialog) {
        AlertDialog(
            onDismissRequest = { reloadDialog = false },
            title = { Text("Reload panel") },
            text = { Text("Reset all panel values to defaults?") },
            confirmButton = { TextButton(onClick = {
                season.settings = CraftSettings(outputPerCraft = 1.0)
                season.inputs.clear(); season.outputs.clear(); onChange(); reloadDialog = false
            }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { reloadDialog = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(season.panelName.ifBlank { "Name Panel" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(enabled = undoHistory.isNotEmpty(), onClick = {
                        val previous = undoHistory.removeLastOrNull() ?: return@IconButton
                        redoHistory.add(season.snapshot())
                        season.name = previous.name
                        season.panelName = previous.panelName
                        season.description = previous.description
                        season.settings = previous.settings
                        season.inputs = previous.inputs
                        season.outputs = previous.outputs
                        lastSnapshot = season.snapshot()
                        onChange()
                    }) { Icon(Icons.Default.Undo, contentDescription = "Undo") }
                    IconButton(enabled = redoHistory.isNotEmpty(), onClick = {
                        val next = redoHistory.removeLastOrNull() ?: return@IconButton
                        undoHistory.add(season.snapshot())
                        season.name = next.name
                        season.panelName = next.panelName
                        season.description = next.description
                        season.settings = next.settings
                        season.inputs = next.inputs
                        season.outputs = next.outputs
                        lastSnapshot = season.snapshot()
                        onChange()
                    }) { Icon(Icons.Default.Redo, contentDescription = "Redo") }
                    IconButton(onClick = { if (season.name.isBlank()) saveDialog = true else onChange() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { reloadDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }
                OutlinedTextField(season.panelName, {
                    season.panelName = it; onChange()
                }, Modifier.fillMaxWidth(), label = { Text("Name Panel") })
                OutlinedTextField(season.description, {
                    season.description = it; onChange()
                }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 2)
                TextButton(onClick = { quickSettings = !quickSettings }) {
                    Text(if (quickSettings) "Close Quick Settings" else "Open Quick Settings")
                }
                if (quickSettings) QuickSettingsInline(season.settings, appData, onChange)
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Recipe Overall Settings", style = MaterialTheme.typography.titleMedium)
                NumberField("Cost Fee Per 100 Nutrition", season.settings.costFeePer100, {
                    season.settings = season.settings.copy(costFeePer100 = it); onChange()
                }, builtinKeypad)
                NumberField("Final Single Output Item Value for Nutrition Fee", season.settings.finalOutputValue, {
                    season.settings = season.settings.copy(finalOutputValue = it); onChange()
                }, builtinKeypad)
                NumberField("Return Rate %", season.settings.returnRate, {
                    season.settings = season.settings.copy(returnRate = it ?: 0.0); onChange()
                }, builtinKeypad)
                NumberField("Total Will Made", season.settings.totalWillMade, {
                    season.settings = season.settings.copy(totalWillMade = it); onChange()
                }, builtinKeypad)
                NumberField("Output Amount Per Craft", season.settings.outputPerCraft, {
                    season.settings = season.settings.copy(outputPerCraft = it ?: 1.0); onChange()
                }, builtinKeypad)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Premium Active")
                    Switch(season.settings.premiumActive, {
                        season.settings = season.settings.copy(premiumActive = it); onChange()
                    })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sell Leftover")
                    Switch(season.settings.sellLeftover, {
                        season.settings = season.settings.copy(sellLeftover = it); onChange()
                    })
                }
                if (season.settings.returnRate > 100) Warning("Return rate is over 100%.")
                if (season.settings.totalWillMade != null && season.settings.totalWillMade == 0.0) Warning("Total Will Made is 0.")
            }
        }
        Spacer(Modifier.height(8.dp))
        MaterialInputs(season, builtinKeypad, onChange)
        Spacer(Modifier.height(8.dp))
        MaterialOutputs(season, builtinKeypad, onChange)
        Spacer(Modifier.height(8.dp))
        Breakdown(season.settings, result)
    }
}

@Composable
private fun QuickSettingsInline(settings: CraftSettings, appData: AppData, onChange: () -> Unit) {
    Text("Quick Settings", fontWeight = FontWeight.Bold)
    val options = listOf("Premium Active", "Sell Leftover", "Return Rate", "Total Will Made", "Cost Fee")
    options.forEach { option ->
        val selected = appData.quickSettingsIds.contains(option)
        FilterChip(selected = selected, onClick = {
            if (selected) appData.quickSettingsIds.remove(option) else appData.quickSettingsIds.add(option)
            onChange()
        }, label = { Text(option) }, modifier = Modifier.padding(end = 4.dp))
    }
}

@Composable
private fun MaterialInputs(season: Season, builtinKeypad: Boolean, onChange: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Input Materials", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val number = nextTemporaryNumber(season.inputs.map { it.name })
                    season.inputs.add(InputMaterial(UUID.randomUUID().toString(), "Temporary $number",
                        buyMethod = "Buy Now", buyLocation = season.inputs.lastOrNull()?.buyLocation ?: "",
                        useReturnRate = true))
                    onChange()
                }) { Text("Create input") }
            }
            season.inputs.forEachIndexed { index, input ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(input.name, {
                                season.inputs[index] = input.copy(name = it); onChange()
                            }, Modifier.weight(1f), label = { Text("Name") })
                            IconButton(onClick = {
                                season.inputs[index] = input.copy(name = ""); onChange()
                            }) { Icon(Icons.Default.Close, contentDescription = "Clear name") }
                            IconButton(onClick = {
                                season.inputs[index] = input.copy(name = input.name); onChange()
                            }) { Icon(Icons.Default.ContentPaste, contentDescription = "Paste") }
                            IconButton(onClick = {
                                season.inputs.removeAt(index); onChange()
                            }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        }
                        Dropdown("Buy Location", listOf("All Cities", "Rest", "Black Market", "Brecilien", "Caerleon"), input.buyLocation) {
                            season.inputs[index] = input.copy(buyLocation = it); onChange()
                        }
                        Dropdown("Buy Method", listOf("Buy Now", "Buy Order"), input.buyMethod) {
                            season.inputs[index] = input.copy(buyMethod = it); onChange()
                        }
                        NumberField("Silver cost for single amount", input.price, {
                            season.inputs[index] = input.copy(price = it); onChange()
                        }, builtinKeypad)
                        NumberField("Total amount need for one made", input.amountPerCraft, {
                            season.inputs[index] = input.copy(amountPerCraft = it); onChange()
                        }, builtinKeypad)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Use return rate"); Switch(input.useReturnRate, {
                                season.inputs[index] = input.copy(useReturnRate = it); onChange()
                            })
                        }
                        val solved = SolverText(season.settings, input)
                        Text("Total real amount need after calculating: ${formatCompact(solved.first)}", style = MaterialTheme.typography.bodySmall)
                        Text("Total real silver cost need: ${formatCompact(solved.second)}", style = MaterialTheme.typography.bodySmall)
                        if (input.price == null) Warning("Missing price.")
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialOutputs(season: Season, builtinKeypad: Boolean, onChange: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Output Products", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val number = nextTemporaryNumber(season.outputs.map { it.name })
                    season.outputs.add(OutputProduct(UUID.randomUUID().toString(), "Temporary $number",
                        sellMethod = "Sell Now", sellLocation = season.outputs.lastOrNull()?.sellLocation ?: ""))
                    onChange()
                }) { Text("Create output") }
            }
            season.outputs.forEachIndexed { index, output ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(output.name, {
                                season.outputs[index] = output.copy(name = it); onChange()
                            }, Modifier.weight(1f), label = { Text("Name") })
                            IconButton(onClick = { season.outputs[index] = output.copy(name = ""); onChange() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear name")
                            }
                            IconButton(onClick = { season.outputs.removeAt(index); onChange() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        Dropdown("Sell Location", listOf("All Cities", "Rest", "Black Market", "Brecilien", "Caerleon"), output.sellLocation) {
                            season.outputs[index] = output.copy(sellLocation = it); onChange()
                        }
                        Dropdown("Sell Method", listOf("Sell Now", "Sell Order"), output.sellMethod) {
                            season.outputs[index] = output.copy(sellMethod = it); onChange()
                        }
                        NumberField("Silver got from selling single amount", output.price, {
                            season.outputs[index] = output.copy(price = it); onChange()
                        }, builtinKeypad)
                        val amount = season.settings.totalWillMade ?: Double.NaN
                        val gross = if (amount.isFinite() && output.price != null) amount * output.price else null
                        val setup = if (gross != null && output.sellMethod == "Sell Order") gross * 0.025 else 0.0
                        val tax = if (gross != null) gross * if (season.settings.premiumActive) 0.04 else 0.08 else null
                        val net = if (gross != null && tax != null) gross - setup - tax else null
                        Text("Total Silver Income after selling: ${formatCompact(net)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SolverText(settings: CraftSettings, input: InputMaterial): Pair<Double?, Double?> {
    val total = settings.totalWillMade ?: return null to null
    val perCraft = settings.outputPerCraft
    val amountPer = input.amountPerCraft ?: return null to null
    val crafts = if (perCraft > 0 && total.isFinite()) kotlin.math.ceil(total / perCraft) else return null to null
    val demand = crafts * amountPer
    val rateTenths = kotlin.math.floor(settings.returnRate * 10.0 + 0.0000001).toInt().coerceAtLeast(0)
    val solved = if (input.useReturnRate) Calculator.calculateReturnRate(demand, rateTenths) else demand to 0.0
    val setup = if (input.buyMethod == "Buy Order" && input.price != null && solved.first.isFinite())
        solved.first * input.price!! * 0.025 else 0.0
    val cost = if (input.price != null && solved.first.isFinite()) solved.first * input.price!! + setup else null
    return solved.first to cost
}

@Composable
private fun Breakdown(settings: CraftSettings, result: CalculationResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Breakdown", style = MaterialTheme.typography.titleMedium)
            Text("Inputs", fontWeight = FontWeight.Bold)
            result.inputs.forEach {
                val gross = formatCompact(it.grossCost)
                val setup = formatCompact(it.setupFee)
                val after = formatCompact(it.afterFees)
                Text("${it.material.name.ifBlank { "Unnamed" }} | Amount ${formatCompact(it.purchase)} | Price ${formatCompact(it.material.price)} | Gross $gross | Setup $setup | After $after")
            }
            Text("Leftover Returns", fontWeight = FontWeight.Bold)
            result.leftovers.forEach {
                val tax = formatCompact(it.salesTax)
                val income = formatCompact(it.afterFees)
                Text("${it.material.name.ifBlank { "Unnamed" }} | Amount ${formatCompact(it.amount)} | ${if (settings.sellLeftover) "Income $income | Tax $tax" else "—"}")
            }
            Text("Outputs", fontWeight = FontWeight.Bold)
            result.outputs.forEach {
                Text("${it.product.name.ifBlank { "Unnamed" }} | Amount ${formatCompact(it.amount)} | Price ${formatCompact(it.product.price)} | Gross ${formatCompact(it.grossIncome)} | After ${formatCompact(it.afterFees)}")
            }
            Text("Other Adjustments", fontWeight = FontWeight.Bold)
            Text("Craft Fee ${formatCompact(settings.craftFee)} | Nutrition Fee ${formatCompact(result.nutritionFee)}")
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text("Total Expenses: ${formatCompact(result.totalExpenses)}")
            Text("Total Income: ${formatCompact(result.totalIncome)}")
            Text("Profit: ${formatCompact(result.profit)}", color = if (result.profit >= 0) Color(0xFF168B4A) else Color.Red)
            Text("ROI: ${formatCompact(result.roi)}%")
            Text("Profit per item: ${formatCompact(result.profitPerItem)}")
            Text("Break-even sell price per item: ${formatCompact(result.breakEvenPrice)}")
        }
    }
}

@Composable
private fun NumberField(label: String, value: Double?, onValue: (Double?) -> Unit, builtin: Boolean = false) {
    var text by remember(value) { mutableStateOf(if (value == null) "" else value.toString()) }
    var keypadOpen by remember { mutableStateOf(false) }
    if (keypadOpen) {
        AlertDialog(
            onDismissRequest = { keypadOpen = false },
            title = { Text(label) },
            text = {
                Column {
                    OutlinedTextField(text, { text = it; onValue(parseNumber(it)) }, Modifier.fillMaxWidth(), readOnly = true)
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "k", "m", "b", "⌫")
                    keys.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { key ->
                                TextButton(onClick = {
                                    text = if (key == "⌫") text.dropLast(1) else text + key
                                    onValue(parseNumber(text))
                                }, Modifier.weight(1f)) { Text(key) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { keypadOpen = false }) { Text("Done") } }
        )
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValue(parseNumber(it))
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(enabled = builtin) { keypadOpen = true },
        readOnly = builtin,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

@Composable
private fun Dropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        OutlinedButton(onClick = { expanded = true }, Modifier.fillMaxWidth()) {
            Text("$label: ${selected.ifBlank { "—" }}", modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
private fun Warning(text: String) {
    Text(text, color = Color(0xFFB3261E), style = MaterialTheme.typography.bodySmall)
}

private fun nextTemporaryNumber(names: List<String>): Int {
    val numbers = names.mapNotNull { Regex("""Temporary\s+(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
    val highest = numbers.maxOrNull() ?: 0
    return highest + 1
}

@Composable
private fun SettingsScreen(data: AppData, onChange: () -> Unit) {
    var quickOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            AppStorage.save(context, data)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.writer().use { writer -> writer.write(AppStorage.dataFile(context).readText()) }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val imported = AppStorage.import(context, uri)
            if (imported != null) {
                data.seasons = imported.seasons
                data.openSeasonIds = imported.openSeasonIds
                data.selectedSeasonId = imported.selectedSeasonId
                data.splashEnabled = imported.splashEnabled
                data.builtinKeypad = imported.builtinKeypad
                data.autoSave = imported.autoSave
                data.forceKeepAlive = imported.forceKeepAlive
                data.quickSettingsIds = imported.quickSettingsIds
                onChange()
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                SettingSwitch("Splash animation", data.splashEnabled) { data.splashEnabled = it; onChange() }
                SettingSwitch("Built-in number keypad", data.builtinKeypad) { data.builtinKeypad = it; onChange() }
                SettingSwitch("Auto save", data.autoSave) { data.autoSave = it; onChange() }
                SettingSwitch("Force keep-alive", data.forceKeepAlive) {
                    data.forceKeepAlive = it
                    if (it) KeepAliveService.start(context) else KeepAliveService.stop(context)
                    onChange()
                }
                TextButton(onClick = { quickOpen = !quickOpen }) { Text("Quick Settings") }
                if (quickOpen) {
                    listOf("Splash animation", "Built-in keypad", "Auto save", "Force keep-alive").forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Colors", style = MaterialTheme.typography.titleMedium)
                listOf("Background", "Season", "Panel", "Category", "Text box", "Number box", "Others").forEach {
                    Text("$it: Red", color = Color.Red)
                }
                TextButton(onClick = {}) { Text("Choose local image/video/GIF background") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Audio", style = MaterialTheme.typography.titleMedium)
                listOf("Master", "Effects", "Background", "Video sound").forEach { name ->
                    var value by remember { mutableFloatStateOf(1f) }
                    Text(name)
                    Slider(value, { value = it }, valueRange = 0f..1f)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Export / Import", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = {
                        exportLauncher.launch("hoang_calculator_backup.json")
                    }) { Text("Export JSON") }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import JSON") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text("Hoàng Calculator")
                Text("Version 0.0.10")
                Text("Offline Android calculator for Albion Online")
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, value: Boolean, onValue: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(value, onValue)
    }
}
