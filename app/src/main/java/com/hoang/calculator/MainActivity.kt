package com.hoang.calculator

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: HoangViewModel
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) contentResolver.openOutputStream(uri)?.use { viewModel.export(it) }
    }
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) contentResolver.openInputStream(uri)?.use { viewModel.import(it) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[HoangViewModel::class.java]
        setContent { App(viewModel, exportLauncher::launch, importLauncher::launch) }
    }
}

@Composable
fun App(viewModel: HoangViewModel, exportJson: (String) -> Unit, importJson: (Array<String>) -> Unit) {
    val state by viewModel.state.collectAsState()
    if (!state.loaded || state.splashVisible) {
        Splash(enabled = state.settings.splashEnabled, onFinished = viewModel::finishSplash)
    } else {
        val settings = state.settings
        val palette = when (settings.themePreset) {
            "Light" -> lightColorScheme(primary = parseColor(settings.accentHex, Color(0xFFFF5252)), background = parseColor(settings.backgroundHex, Color(0xFFF4F4F4)), onBackground = parseColor(settings.textColorHex, Color(0xFF202124)), surface = parseColor(settings.panelHex, Color(0xFFFFFFFF)).copy(alpha = settings.cardOpacity), onSurface = parseColor(settings.textColorHex, Color(0xFF202124)), surfaceVariant = parseColor(settings.categoryHex, Color(0xFFEAEAEA)).copy(alpha = settings.cardOpacity))
            "AMOLED black" -> darkColorScheme(primary = parseColor(settings.accentHex, Color(0xFFFF5252)), background = Color.Black, onBackground = parseColor(settings.textColorHex, Color.White), surface = Color.Black.copy(alpha = settings.cardOpacity), onSurface = parseColor(settings.textColorHex, Color.White), surfaceVariant = Color(0xFF111111).copy(alpha = settings.cardOpacity))
            else -> darkColorScheme(background = parseColor(settings.backgroundHex, Color(0xFF121212)), onBackground = parseColor(settings.textColorHex, Color.White), surface = parseColor(settings.panelHex, Color(0xFF191919)).copy(alpha = settings.cardOpacity), onSurface = parseColor(settings.textColorHex, Color.White), surfaceVariant = parseColor(settings.categoryHex, Color(0xFF242424)).copy(alpha = settings.cardOpacity), primary = parseColor(settings.accentHex, Color(0xFFFF5252)), secondary = Color(0xFF4CAF50))
        }
        val contextForWindow = LocalContext.current
        DisposableEffect(settings.keepScreenOn) {
            val activity = contextForWindow as? android.app.Activity
            val window = activity?.window
            if (settings.keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
        val appTypography = androidx.compose.material3.Typography(
            bodySmall = androidx.compose.ui.text.TextStyle(fontSize = settings.fontSize.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = "tnum"),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = settings.fontSize.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = "tnum"),
            bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = (settings.fontSize + 2f).sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = "tnum"),
            titleSmall = androidx.compose.ui.text.TextStyle(fontSize = (settings.fontSize + 2f).sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = "tnum"),
            titleMedium = androidx.compose.ui.text.TextStyle(fontSize = (settings.fontSize + 4f).sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = "tnum"),
            titleLarge = androidx.compose.ui.text.TextStyle(fontSize = (settings.fontSize + 8f).sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontFeatureSettings = "tnum")
        )
        val appShapes = androidx.compose.material3.Shapes(
            extraSmall = RoundedCornerShape(settings.cornerRadius.dp / 2),
            small = RoundedCornerShape(settings.cornerRadius.dp),
            medium = RoundedCornerShape(settings.cornerRadius.dp),
            large = RoundedCornerShape(settings.cornerRadius.dp * 1.5f),
            extraLarge = RoundedCornerShape(settings.cornerRadius.dp * 2f)
        )
        MaterialTheme(colorScheme = palette, typography = appTypography, shapes = appShapes) {
            val densityPadding = when (settings.uiDensity) { "Compact" -> 4.dp; "Spacious" -> 16.dp; else -> 10.dp }
            Box(Modifier.fillMaxSize().background(palette.background)) {
                if (settings.backgroundImage.isNotBlank()) {
                    AsyncImage(
                        model = java.io.File(settings.backgroundImage),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = settings.cardOpacity
                    )
                }
                if (settings.backgroundVideo.isNotBlank()) {
                    VideoBackground(
                        settings.backgroundVideo,
                        settings.videoSound,
                        Modifier.fillMaxSize()
                    )
                }
                Box(Modifier.fillMaxSize().padding(densityPadding)) {
                    AppScaffold(state, viewModel, exportJson, importJson)
                }
            }
        }
    }
}

fun parseColor(hex: String, fallback: Color): Color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { fallback }

@Composable
fun VideoBackground(path: String, volume: Float, modifier: Modifier = Modifier) {
    if (path.isBlank() || !java.io.File(path).isFile) return
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            val player = androidx.media3.exoplayer.ExoPlayer.Builder(ctx).build().apply {
                setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                this.volume = volume.coerceIn(0f, 1f)
                playWhenReady = true
                prepare()
            }
            androidx.media3.ui.PlayerView(ctx).apply {
                useController = false
                this.player = player
            }.also { view ->
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> player.playWhenReady = true
                        Lifecycle.Event.ON_STOP -> player.playWhenReady = false
                        Lifecycle.Event.ON_DESTROY -> player.release()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: android.view.View) {}
                    override fun onViewDetachedFromWindow(view: android.view.View) {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        player.release()
                    }
                })
            }
        },
        update = { view -> view.player?.volume = volume.coerceIn(0f, 1f) },
        modifier = modifier
    )
}

@Composable
fun DebouncedTextField(value: String, onValue: (String) -> Unit, modifier: Modifier, label: String) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(text) {
        if (text != value) {
            delay(150)
            onValue(text)
        }
    }
    OutlinedTextField(text, { text = it }, modifier, label = { Text(label) }, singleLine = label != "Description")
}

@Composable
fun TextFieldRow(value: String, onValue: (String) -> Unit, label: String, imagePath: String = "", onImagePicked: (String) -> Unit = {}, onImageRemove: () -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FlowRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ImagePickerBox(imagePath, onImagePicked, onImageRemove, 48)
        var text = value
        OutlinedTextField(value = value, onValueChange = onValue, label = { Text(label) }, singleLine = true, modifier = Modifier.widthIn(min = 120.dp, max = 320.dp))
        IconButton(onClick = {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ContentCopy, "Copy") }
        IconButton(onClick = {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (pasted != null) onValue(pasted)
        }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ContentPaste, "Paste") }
        IconButton(onClick = { onValue("") }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Clear") }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FlowRow(modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(modifier = modifier, horizontalArrangement = horizontalArrangement, verticalArrangement = verticalArrangement) { content() }
}

@Composable
fun NumberText(text: String, colorize: Boolean, rounding: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall) {
    var expanded by remember { mutableStateOf(false) }
    val value = if (text.contains("—")) null else Calculator.parseNumber(text.filter { it.isDigit() || it == '.' || it == '-' })
    val shown = when {
        rounding == "Never" && value != null -> Calculator.formatCompact(value, "Never")
        rounding == "Click to switch" && expanded && value != null -> Calculator.formatCompact(value, "Never")
        else -> text
    }
    val color = if (!colorize || text.contains("—") || shown.contains("—")) MaterialTheme.colorScheme.onSurface else if (shown.trimStart().startsWith("-")) Color(0xFFFF5252) else Color(0xFF69F0AE)
    Text(shown, modifier = modifier.clickable(enabled = rounding == "Click to switch") { expanded = !expanded }, color = color, style = style)
}

fun copyPickedMedia(context: android.content.Context, uri: Uri, prefix: String): String? = try {
    val folder = java.io.File(context.filesDir, "media")
    if (!folder.exists()) folder.mkdirs()
    val extension = context.contentResolver.getType(uri)?.substringAfterLast('/', prefix) ?: prefix
    val file = java.io.File(folder, "${prefix}_${java.util.UUID.randomUUID()}.$extension")
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    file.absolutePath
} catch (_: Exception) { null }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImagePickerBox(path: String, onPicked: (String) -> Unit, onRemove: () -> Unit, sizeDp: Int = 72) {
    val context = LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) copyPickedMedia(context, uri, "image")?.let(onPicked)
    }
    Box(
        Modifier.size(sizeDp.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF303030)).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).combinedClickable(onClick = {
            picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }, onLongClick = onRemove),
        contentAlignment = Alignment.Center
    ) {
        if (path.isBlank()) Text("IMG", fontWeight = FontWeight.Bold)
        else AsyncImage(model = java.io.File(path), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

@Composable
fun Splash(enabled: Boolean, onFinished: () -> Unit) {
    val scope = rememberCoroutineScope()
    if (enabled) {
        androidx.compose.runtime.LaunchedEffect(Unit) { scope.launch { delay(3600); onFinished() } }
    } else {
        androidx.compose.runtime.LaunchedEffect(Unit) { onFinished() }
    }
    val transition = rememberInfiniteTransition(label = "")
    val phase by transition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "")
    val view = LocalView.current
    Box(Modifier.fillMaxSize().background(Color(0xFF111111)).clickable {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onFinished()
    }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(260.dp, 180.dp)) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.grass),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomStart).size(120.dp).graphicsLayer { rotationZ = kotlin.math.sin(phase * 12f) * 4f }
                )
                if (phase > 0.35f) androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.cloud),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopStart).size(110.dp).offset(x = (phase * 36).dp)
                )
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.chicken),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(96.dp).offset(x = if (phase > 0.68f) ((phase - 0.68f) * 160f).dp else 0.dp).graphicsLayer {
                        rotationZ = if (phase < 0.68f) kotlin.math.sin(phase * 24f) * 8f else 0f
                    }
                )
                if (phase > 0.35f && phase < 0.68f) repeat(12) { index ->
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.droplet),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.TopStart).size(18.dp).offset(x = (index * 19).dp, y = ((phase * 170f + index * 13f) % 150f).dp)
                    )
                }
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.kfc),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).size(96.dp).graphicsLayer { alpha = if (phase > 0.68f) 1f else 0.85f }
                )
            }
            Text("Hoàng Calculator", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(if (phase < 0.35f) "Chicken finds grass" else if (phase < 0.68f) "Rain starts" else "Running into KFC", color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
fun AppScaffold(state: UiState, viewModel: HoangViewModel, exportJson: (String) -> Unit, importJson: (Array<String>) -> Unit) {
    val context = LocalContext.current
    val season = state.seasons.firstOrNull { it.id == state.selectedSeasonId }
    val result = state.calculation
    val homeListState = rememberLazyListState()
    val homeScrolled = state.screen == "home" && (homeListState.firstVisibleItemIndex > 0 || homeListState.firstVisibleItemScrollOffset > 0)
    val scope = rememberCoroutineScope()
    var bottomExpanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AnimatedVisibility(visible = state.screen != "home" || !homeScrolled) { TopBar(state, viewModel) } },
        bottomBar = {
            val season = state.seasons.firstOrNull { it.id == state.selectedSeasonId }
            if (state.screen == "home" && result != null && !homeScrolled) Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .clickable { bottomExpanded = !bottomExpanded }
                    .pointerInput(Unit) {
                        detectDragGestures { _, drag ->
                            if (drag.y < -24f) bottomExpanded = true
                            if (drag.y > 24f) bottomExpanded = false
                        }
                    }
            ) {
                if (bottomExpanded) Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryChip("Mat Buy Cost before setup fee", result.inputs.mapNotNull { it.grossCost }.sum(), false, rounding = state.settings.numberRounding)
                        SummaryChip("Mat Buy Setup Fee", result.inputs.mapNotNull { it.setupFee }.sum(), false, rounding = state.settings.numberRounding)
                        SummaryChip("Leftover Sell Earn before tax", result.leftovers.mapNotNull { it.grossIncome }.sum(), true, rounding = state.settings.numberRounding)
                        SummaryChip("Leftover Sell Tax", result.leftovers.mapNotNull { it.salesTax }.sum(), false, rounding = state.settings.numberRounding)
                        SummaryChip("Product Sell Earn before tax, setup fee", result.outputs.mapNotNull { it.grossIncome }.sum(), true, rounding = state.settings.numberRounding)
                        SummaryChip("Product Sell Tax", result.outputs.mapNotNull { it.salesTax }.sum(), false, rounding = state.settings.numberRounding)
                        SummaryChip("Product Sell Setup Fee", result.outputs.mapNotNull { it.setupFee }.sum(), false, rounding = state.settings.numberRounding)
                        SummaryChip("Crafting Fee", season?.craftFee ?: 0.0, false, rounding = state.settings.numberRounding)
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryChip("Total Cost", result.totalExpenses, false, rounding = state.settings.numberRounding)
                        SummaryChip("Total Earn", result.totalIncome, true, rounding = state.settings.numberRounding)
                        SummaryChip("Total Profit", result.profit, result.profit >= 0, rounding = state.settings.numberRounding)
                        SummaryChip("Total Profit %", result.roi ?: Double.NaN, (result.roi ?: 0.0) >= 0, true, state.settings.numberRounding)
                    }
                } else {
                    FlowRow(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryChip("Expenses", result.totalExpenses, false, rounding = state.settings.numberRounding)
                        SummaryChip("Income", result.totalIncome, true, rounding = state.settings.numberRounding)
                        SummaryChip("Profit", result.profit, result.profit >= 0, rounding = state.settings.numberRounding)
                        SummaryChip("ROI", result.roi ?: Double.NaN, (result.roi ?: 0.0) >= 0, true, state.settings.numberRounding)
                    }
                }
            }
        },
        floatingActionButton = {
            if (state.screen == "home" && homeScrolled) {
                FloatingActionButton(onClick = { scope.launch { homeListState.animateScrollToItem(0) } }) {
                    Icon(Icons.Default.KeyboardArrowUp, "Scroll to top")
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.screen) {
                "seasons" -> SeasonListScreen(state, viewModel)
                "settings" -> SettingsScreen(state, viewModel, exportJson, importJson)
                else -> HomeScreen(state, viewModel, season, result, homeListState)
            }
        }
    }
}

@Composable
fun SummaryChip(label: String, value: Double, positive: Boolean, percent: Boolean = false, rounding: String = "Always", colorize: Boolean = true) {
    val text = if (!value.isFinite()) "—" else if (percent) "${Calculator.formatCompact(value, rounding)}%" else Calculator.formatCompact(value, rounding)
    Column { Text(label, style = MaterialTheme.typography.labelSmall); NumberText(text, colorize, rounding, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)) }
}

@Composable
fun TopBar(state: UiState, viewModel: HoangViewModel) {
    Surface(tonalElevation = 4.dp) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setScreen("seasons") }) { Icon(Icons.AutoMirrored.Filled.List, "Seasons") }
                IconButton(onClick = {
                    if (state.selectedSeasonId == null) viewModel.createSeason() else viewModel.selectSeason(state.selectedSeasonId)
                }) { Icon(Icons.Default.Home, "Home") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = viewModel::undo, enabled = state.undo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                IconButton(onClick = viewModel::redo, enabled = state.redo.isNotEmpty()) { Icon(Icons.Default.Redo, contentDescription = "Redo") }
                IconButton(onClick = { viewModel.setScreen("settings") }) { Icon(Icons.Default.Settings, "Settings") }
            }
            if (state.openSeasonIds.isNotEmpty()) LazyRow(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                items(state.openSeasonIds, key = { it }) { id ->
                    val season = state.seasons.firstOrNull { it.id == id }
                    if (season != null) FilterChip(
                        selected = state.selectedSeasonId == id,
                        onClick = { viewModel.selectSeason(id) },
                        label = { Text(season.name.ifBlank { season.panelName.ifBlank { "Temporary" } }) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, "Close", Modifier.size(16.dp).clickable { viewModel.closeSeason(id) })
                        },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SeasonListScreen(state: UiState, viewModel: HoangViewModel) {
    val sortOptions = listOf("Name A-Z", "Name Z-A", "Last edited newest", "Last edited oldest", "Created newest", "Created oldest", "Profit high-low", "Profit low-high", "Profit % high-low", "Profit % low-high", "Cost high-low", "Cost low-high", "Earn high-low", "Earn low-high", "Pinned first", "Manual order")
    val seasons = state.seasons.filter { it.temporary.not() || it.name.isNotBlank() || it.panelName.isNotBlank() }.filter { state.search.isBlank() || it.name.contains(state.search, true) || it.panelName.contains(state.search, true) || it.description.contains(state.search, true) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(state.search, { viewModel.setSearch(it) }, Modifier.weight(1f), label = { Text("Search") }, singleLine = true)
            Spacer(Modifier.width(8.dp))
            Dropdown("Sort", state.settings.seasonSort, sortOptions) { viewModel.sortSeasons(it) }
        }
        Spacer(Modifier.height(8.dp))
        ToggleRow("Edit Mode", state.settings.seasonEditMode) { viewModel.updateSetting { it.copy(seasonEditMode = !it.seasonEditMode) } }
        IconButton(onClick = viewModel::createSeason, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, "Create season") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(seasons, key = { it.id }) { season ->
                Card(Modifier.fillMaxWidth().clickable { viewModel.selectSeason(season.id) }) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.widthIn(min = 72.dp, max = 130.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            ImagePickerBox(season.imageUri, { viewModel.updateSeasonImage(season.id, it) }, { viewModel.updateSeasonImage(season.id, "") }, 48)
                            Text(season.name.ifBlank { season.panelName.ifBlank { "Temporary" } }, fontWeight = FontWeight.Bold, maxLines = 2)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row { Text("Cost: "); NumberText(season.totalExpenses?.let { "-${Calculator.formatCompact(it, state.settings.numberRounding).trimStart('-')}" } ?: "—", true, state.settings.numberRounding) }
                            Row { Text("Earn: "); NumberText(season.totalIncome?.let { Calculator.formatCompact(it, state.settings.numberRounding) } ?: "—", true, state.settings.numberRounding) }
                            Row { Text("Profit: "); NumberText(season.profit?.let { Calculator.formatCompact(it, state.settings.numberRounding) } ?: "—", true, state.settings.numberRounding) }
                            Row { Text("Profit %: "); NumberText(season.roiPercent?.let { Calculator.formatCompact(it, state.settings.numberRounding) + "%" } ?: "—", true, state.settings.numberRounding) }
                        }
                    }
                    if (state.settings.seasonEditMode) FlowRow(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        IconButton(onClick = { viewModel.renameSeason(season.id, if (season.name.isBlank()) "Renamed Season" else season.name) }) { Icon(Icons.Default.Edit, "Rename") }
                        IconButton(onClick = { viewModel.duplicateSeason(season.id) }) { Icon(Icons.Default.ContentCopy, "Duplicate") }
                        IconButton(onClick = { viewModel.toggleSeasonPin(season.id) }) { Icon(Icons.Default.PushPin, if (season.pinned) "Unpin" else "Pin") }
                        IconButton(onClick = { viewModel.deleteSeason(season.id) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(state: UiState, viewModel: HoangViewModel, season: Season?, result: CalculationResult?, listState: LazyListState) {
    if (season == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            IconButton(onClick = viewModel::createSeason, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, "Create season") }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp, 12.dp, 12.dp, 92.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "actions:${season.id}") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = viewModel::undo, enabled = state.undo.isNotEmpty(), modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                IconButton(onClick = viewModel::redo, enabled = state.redo.isNotEmpty(), modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Redo, "Redo") }
                IconButton(onClick = viewModel::saveSelected, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Save, "Save") }
                IconButton(onClick = viewModel::reloadSeason, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Refresh, "Reload") }
                IconButton(onClick = { viewModel.minimizeSeason(season.id) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.KeyboardArrowDown, "Minimize") }
                IconButton(onClick = { viewModel.closeSeason(season.id) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Close") }
            }
        }
        item(key = "panel:${season.id}") { PanelCard(state, viewModel, season) }
        item(key = "recipe:${season.id}") { RecipeCard(state, viewModel, season) }
        item(key = "inputs:${season.id}") { InputsCard(state, viewModel, season, result) }
        item(key = "outputs:${season.id}") { OutputsCard(state, viewModel, season, result) }
        item(key = "breakdown:${season.id}") { BreakdownCard(state, viewModel, season, result) }
    }
}

@Composable
fun ZoneCard(key: String, state: UiState, title: String, viewModel: HoangViewModel, content: @Composable () -> Unit) {
    val collapsed = state.collapsed[key] ?: false
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = parseColor(state.settings.categoryHex, Color(0xFF242424))),
        shape = RoundedCornerShape(state.settings.cornerRadius.dp)
    ) {
        Row(Modifier.fillMaxWidth().clickable { viewModel.toggleCollapsed(key) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, null)
        }
        AnimatedVisibility(visible = !collapsed) { Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) { content() } }
    }
}

@Composable
fun PanelCard(state: UiState, viewModel: HoangViewModel, season: Season) {
    ZoneCard("panel", state, "Panel Info", viewModel) {
        Row(verticalAlignment = Alignment.Top) {
            ImagePickerBox(season.imageUri, { viewModel.updatePanel("imageUri", it) }, { viewModel.updatePanel("imageUri", "") })
            Spacer(Modifier.width(10.dp))
            DebouncedTextField(season.panelName, { viewModel.updatePanel("panelName", it) }, Modifier.weight(1f), "Name Panel")
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Spacer(Modifier.width(82.dp))
            Column(Modifier.weight(1f)) {
                DebouncedTextField(season.name, { viewModel.updatePanel("name", it) }, Modifier.fillMaxWidth(), "Season Name")
                Spacer(Modifier.height(8.dp))
                DebouncedTextField(season.description, { viewModel.updatePanel("description", it) }, Modifier.fillMaxWidth(), "Description")
            }
        }
        ToggleRow("Edit Mode", state.settings.homeEditMode) { viewModel.updateSetting { it.copy(homeEditMode = !it.homeEditMode) } }
        QuickSettings(state, viewModel)
    }
}

@Composable
fun RecipeCard(state: UiState, viewModel: HoangViewModel, season: Season) {
    ZoneCard("recipe", state, "Recipe Overall Settings", viewModel) {
        val nutritionSheet = remember { mutableStateOf(false) }
        val itemValueSheet = remember { mutableStateOf(false) }
        val returnRateSheet = remember { mutableStateOf(false) }
        val totalSheet = remember { mutableStateOf(false) }
        val outputPerCraftSheet = remember { mutableStateOf(false) }
        val craftFeeSheet = remember { mutableStateOf(false) }
        NumberField("Nutrition fee /100", season.costFeePer100, { viewModel.updateRecipe("costFee", it) }, state, sheetVisibleState = nutritionSheet, onNext = { itemValueSheet.value = true })
        NumberField("Item value (nutrition)", season.finalOutputValue, { viewModel.updateRecipe("finalValue", it) }, state, sheetVisibleState = itemValueSheet, onNext = { returnRateSheet.value = true })
        NumberField("Return Rate", season.returnRate.takeIf { it != "0" }?.let { Calculator.parseNumber(it) }, { viewModel.updateRecipe("returnRate", it) }, state, sheetVisibleState = returnRateSheet, onNext = { totalSheet.value = true })
        NumberField("Total Will Made", season.totalWillMade, { viewModel.updateRecipe("total", it) }, state, sheetVisibleState = totalSheet, onNext = { outputPerCraftSheet.value = true })
        NumberField("Output Amount Per Craft", season.outputPerCraft, { viewModel.updateRecipe("outputPerCraft", it ?: 1.0) }, state, sheetVisibleState = outputPerCraftSheet, onNext = { craftFeeSheet.value = true })
        ToggleRow("Premium Active", season.premiumActive) { viewModel.toggleRecipe("premium") }
        ToggleRow("Sell Leftover", season.sellLeftover) { viewModel.toggleRecipe("leftover") }
        NumberField("Craft Fee", season.craftFee.takeIf { it != 0.0 }, { viewModel.updateRecipe("craftFee", it) }, state, allowNegative = true, sheetVisibleState = craftFeeSheet)
    }
}

@Composable
fun InputsCard(state: UiState, viewModel: HoangViewModel, season: Season, result: CalculationResult?) {
    val context = LocalContext.current
    ZoneCard("inputs", state, "Input Materials (${season.inputs.size})", viewModel) {
        Row {
            IconButton(onClick = viewModel::createInput, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, "Add input") }
            IconButton(onClick = viewModel::deleteSelectedInputs, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Delete selected") }
            IconButton(onClick = viewModel::sortInputs, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Sort, "Sort inputs") }
        }
        season.inputs.forEachIndexed { index, input ->
            val inputResult = result?.inputs?.firstOrNull { it.material.id == input.id }
            val unitPriceSheet = remember(input.id) { mutableStateOf(false) }
            val qtyPerCraftSheet = remember("${input.id}-qty") { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = parseColor(state.settings.textBoxHex, Color(0xFF303030)))) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(input.selected, { viewModel.toggleInputSelected(input.id) })
                        TextFieldRow(input.name, { viewModel.updateInput(input.id) { item -> item.copy(name = it) } }, "Material name", input.imageUri, { viewModel.updateInput(input.id) { item -> item.copy(imageUri = it) } }, { viewModel.updateInput(input.id) { item -> item.copy(imageUri = "") } }, Modifier.weight(1f))
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dropdown("Buy Location", input.buyLocation, LOCATION_OPTIONS) {
                            viewModel.updateInput(input.id) { item -> item.copy(buyLocation = it) }
                            viewModel.updateSetting { settings -> settings.copy(lastBuyLocation = it) }
                        }
                        Dropdown("Buy Method", input.buyMethod, METHOD_OPTIONS) {
                            viewModel.updateInput(input.id) { item -> item.copy(buyMethod = it) }
                            viewModel.updateSetting { settings -> settings.copy(lastBuyMethod = it) }
                        }
                    }
                    NumberField("Unit price", input.price, { viewModel.updateInput(input.id) { item -> item.copy(price = it) } }, state, sheetVisibleState = unitPriceSheet, onNext = { qtyPerCraftSheet.value = true })
                    NumberField("Qty per craft", input.amountPerCraft, { viewModel.updateInput(input.id) { item -> item.copy(amountPerCraft = it) } }, state, sheetVisibleState = qtyPerCraftSheet)
                    ToggleRow("Use return rate", input.useReturnRate) { viewModel.updateInput(input.id) { it.copy(useReturnRate = !it.useReturnRate) } }
                    Text("Real qty needed: ${Calculator.formatCompact(inputResult?.purchase)}")
                    Text("Real cost: ${Calculator.formatCompact(inputResult?.afterFees)}")
                    if (state.settings.homeEditMode) Row {
                        IconButton(onClick = { viewModel.moveInput(input.id, -1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                        IconButton(onClick = { viewModel.moveInput(input.id, 1) }, enabled = index < season.inputs.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                        IconButton(onClick = { viewModel.duplicateInput(input.id) }) { Icon(Icons.Default.ContentCopy, "Duplicate") }
                        IconButton(onClick = { viewModel.deleteInput(input.id) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
fun OutputsCard(state: UiState, viewModel: HoangViewModel, season: Season, result: CalculationResult?) {
    ZoneCard("outputs", state, "Output Products (${season.outputs.size})", viewModel) {
        Row {
            IconButton(onClick = viewModel::createOutput, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, "Add output") }
            IconButton(onClick = viewModel::deleteSelectedOutputs, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Delete selected") }
            IconButton(onClick = viewModel::sortOutputs, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Sort, "Sort outputs") }
        }
        season.outputs.forEachIndexed { index, output ->
            val outputResult = result?.outputs?.firstOrNull { it.product.id == output.id }
            val sellPriceSheet = remember(output.id) { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = parseColor(state.settings.textBoxHex, Color(0xFF303030)))) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(output.selected, { viewModel.toggleOutputSelected(output.id) })
                        TextFieldRow(output.name, { viewModel.updateOutput(output.id) { item -> item.copy(name = it) } }, "Product name", output.imageUri, { viewModel.updateOutput(output.id) { item -> item.copy(imageUri = it) } }, { viewModel.updateOutput(output.id) { item -> item.copy(imageUri = "") } }, Modifier.weight(1f))
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dropdown("Sell Location", output.sellLocation, LOCATION_OPTIONS) {
                            viewModel.updateOutput(output.id) { item -> item.copy(sellLocation = it) }
                            viewModel.updateSetting { settings -> settings.copy(lastSellLocation = it) }
                        }
                        Dropdown("Sell Method", output.sellMethod, SELL_METHOD_OPTIONS) {
                            viewModel.updateOutput(output.id) { item -> item.copy(sellMethod = it) }
                            viewModel.updateSetting { settings -> settings.copy(lastSellMethod = it) }
                        }
                    }
                    NumberField("Sell price", output.price, { viewModel.updateOutput(output.id) { item -> item.copy(price = it) } }, state, sheetVisibleState = sellPriceSheet)
                    Text("Net income: ${Calculator.formatCompact(outputResult?.afterFees, state.settings.numberRounding)}")
                    if (state.settings.homeEditMode) Row {
                        IconButton(onClick = { viewModel.moveOutput(output.id, -1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                        IconButton(onClick = { viewModel.moveOutput(output.id, 1) }, enabled = index < season.outputs.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                        IconButton(onClick = { viewModel.duplicateOutput(output.id) }) { Icon(Icons.Default.ContentCopy, "Duplicate") }
                        IconButton(onClick = { viewModel.deleteOutput(output.id) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownCard(state: UiState, viewModel: HoangViewModel, season: Season, result: CalculationResult?) {
                        ZoneCard("breakdown", state, "Breakdown", viewModel) {
                            if (result == null) return@ZoneCard
                            Text("Input Materials", fontWeight = FontWeight.Bold)
                            result.inputs.forEach { item ->
                                TableRow(listOf(
                                    "Amount ${Calculator.formatCompact(item.purchase, state.settings.numberRounding)}",
                                    "Buy ${Calculator.formatCompact(item.material.price, state.settings.numberRounding)}",
                                    item.material.buyLocation,
                                    item.material.buyMethod,
                                    "Gross ${Calculator.formatCompact(item.grossCost, state.settings.numberRounding)}",
                                    "Fee ${Calculator.formatCompact(item.setupFee, state.settings.numberRounding)}",
                                    "After ${Calculator.formatCompact(item.afterFees, state.settings.numberRounding)}"
                                ), state)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Leftover Returns", fontWeight = FontWeight.Bold)
                            result.leftovers.forEach { item ->
                                val visible = season.sellLeftover && item.grossIncome != null
                                TableRow(listOf(
                                    "Amount ${Calculator.formatCompact(item.amount, state.settings.numberRounding)}",
                                    if (visible) "Sell ${Calculator.formatCompact(item.material.price, state.settings.numberRounding)}" else "Sell —",
                                    item.material.buyLocation,
                                    "Sell Now",
                                    "Gross ${if (visible) Calculator.formatCompact(item.grossIncome, state.settings.numberRounding) else "—"}",
                                    "Fee ${if (visible) Calculator.formatCompact(item.setupFee, state.settings.numberRounding) else "—"}",
                                    "Tax ${if (visible) Calculator.formatCompact(item.salesTax, state.settings.numberRounding) else "—"}",
                                    "After ${if (visible) Calculator.formatCompact(item.afterFees, state.settings.numberRounding) else "—"}"
                                ), state)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Outputs", fontWeight = FontWeight.Bold)
                            result.outputs.forEach { item ->
                                TableRow(listOf(
                                    "Amount ${Calculator.formatCompact(item.amount, state.settings.numberRounding)}",
                                    "Sell ${Calculator.formatCompact(item.product.price, state.settings.numberRounding)}",
                                    item.product.sellLocation,
                                    item.product.sellMethod,
                                    "Gross ${Calculator.formatCompact(item.grossIncome, state.settings.numberRounding)}",
                                    "Fee ${Calculator.formatCompact(item.setupFee, state.settings.numberRounding)}",
                                    "Tax ${Calculator.formatCompact(item.salesTax, state.settings.numberRounding)}",
                                    "After ${Calculator.formatCompact(item.afterFees, state.settings.numberRounding)}"
                                ), state)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Other Adjustments", fontWeight = FontWeight.Bold)
                            val otherAdjustmentValues = buildList {
                                add(if (result.nutritionFee == null) "Nutrition —" else "Nutrition ${Calculator.formatCompact(-kotlin.math.abs(result.nutritionFee), state.settings.numberRounding)}")
                                if (season.craftFee != 0.0) add("Craft Fee ${Calculator.formatCompact(season.craftFee, state.settings.numberRounding)}")
                                season.adjustments.forEach { adjustment ->
                                    add("${adjustment.name.ifBlank { "Adjustment" }} ${adjustment.amount?.let { Calculator.formatCompact(it, state.settings.numberRounding) } ?: "—"}")
                                }
                            }
                            TableRow(otherAdjustmentValues, state)
                            Spacer(Modifier.height(10.dp))
                            Text("Summary", fontWeight = FontWeight.Bold)
                            TableRow(listOf("Total Expenses", Calculator.formatCompact(-result.totalExpenses, state.settings.numberRounding)), state)
                            TableRow(listOf("Total Income", Calculator.formatCompact(result.totalIncome, state.settings.numberRounding)), state)
                            TableRow(listOf("Profit", Calculator.formatCompact(result.profit, state.settings.numberRounding)), state)
                            TableRow(listOf("ROI", if (result.roi == null) "—" else "${Calculator.formatCompact(result.roi, state.settings.numberRounding)}%"), state)
                            TableRow(listOf("Profit per item", Calculator.formatCompact(result.profitPerItem, state.settings.numberRounding)), state)
                            TableRow(listOf("Break-even sell price", Calculator.formatCompact(result.breakEvenPrice, state.settings.numberRounding)), state)
                        }
                    }
                    @Composable
                    fun TableRow(values: List<String>, state: UiState) {
                        FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            values.forEach { value ->
                                val numeric = value.substringAfter(' ', "")
                                val prefix = if (value.contains(" ") && numeric.startsWith("-", false).not() && numeric.none { it.isDigit() }.not()) value.substringBeforeLast(' ') + " " else ""
                                if (prefix.isNotEmpty()) {
                                    Text(prefix.trimEnd(), style = MaterialTheme.typography.bodySmall)
                                    NumberText(numeric, true, state.settings.numberRounding, style = MaterialTheme.typography.bodySmall)
                                } else if (value.startsWith("-") || value.any(Char::isDigit) && value.contains(Regex("[,kM.%]"))) {
                                    NumberText(value, true, state.settings.numberRounding, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Text(value, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
                    @Composable
                    fun NumberField(label: String, value: Double?, onValue: (Double?) -> Unit, state: UiState, allowNegative: Boolean = false, onNext: (() -> Unit)? = null, sheetVisibleState: androidx.compose.runtime.MutableState<Boolean>? = null) {
                        val display = if (value == null) "" else java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
                        var field by remember(display) { mutableStateOf(TextFieldValue(display, TextRange(display.length))) }
                        var localSheetVisible by remember { mutableStateOf(false) }
                        val sheetVisible = sheetVisibleState?.value ?: localSheetVisible
                        val view = LocalView.current
                        LaunchedEffect(field.text) { delay(150); onValue(if (label == "Return Rate") Calculator.parseReturnRateDigits(field.text) else Calculator.parseNumber(field.text)) }
                        Column {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = field, onValueChange = { next ->
                                    val filtered = buildString { next.text.forEachIndexed { index, char -> if (char.isDigit() || (char == '.' && !contains('.')) || (char == '-' && index == 0 && allowNegative)) append(char) } }
                                    val formatted = if (label == "Return Rate") Calculator.formatReturnRateDigits(filtered) else if (filtered.isNotEmpty() && filtered.any(Char::isDigit)) Calculator.formatCompact(Calculator.parseNumber(filtered), "Never", state.settings.decimalPrecision) else filtered
                                    val digitsBeforeCursor = next.text.take(next.selection.start.coerceIn(0, next.text.length)).count { it.isDigit() }
                                    val rawPrefix = next.text.take(next.selection.start.coerceIn(0, next.text.length))
                                    val cursorOffset = if (label != "Return Rate" && rawPrefix.contains('.') && formatted.contains('.')) {
                                        val decimalIndex = formatted.indexOf('.')
                                        val decimalsAfter = rawPrefix.substringAfter('.', "").length
                                        (decimalIndex + 1 + decimalsAfter).coerceIn(0, formatted.length)
                                    } else {
                                        var digitsSeen = 0
                                        if (digitsBeforeCursor == 0) 0 else formatted.indexOfLast { ch ->
                                            if (ch.isDigit()) { digitsSeen++; digitsSeen <= digitsBeforeCursor } else false
                                        }.let { if (it >= 0) it + 1 else formatted.length }
                                    }
                                    field = TextFieldValue(formatted, TextRange(cursorOffset.coerceIn(0, formatted.length)))
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }, modifier = Modifier.widthIn(min = 120.dp).weight(1f, fill = false), label = { Text(label) }, singleLine = true,
                                    keyboardOptions = if (state.settings.keypadEnabled) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                if (state.settings.keypadEnabled) IconButton(onClick = { if (sheetVisibleState == null) localSheetVisible = true else sheetVisibleState.value = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Dialpad, "Open keypad") }
                                IconButton(onClick = {
                                    val clipboard = view.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(label, field.text))
                                }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ContentCopy, "Copy") }
                                IconButton(onClick = {
                                    val clipboard = view.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.filter { it.isDigit() || it == '.' || (it == '-' && allowNegative) } ?: return@IconButton
                                    val formatted = if (label == "Return Rate") Calculator.formatReturnRateDigits(pasted.filter(Char::isDigit)) else if (pasted.isNotEmpty() && pasted.any(Char::isDigit)) Calculator.formatCompact(Calculator.parseNumber(pasted), "Never", state.settings.decimalPrecision) else pasted
                                    field = TextFieldValue(formatted, TextRange(formatted.length))
                                }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ContentPaste, "Paste") }
                                IconButton(onClick = { field = TextFieldValue("", TextRange.Zero); onValue(null) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Clear") }
                            }
                            if (sheetVisible) KeypadSheet(
                                label = label,
                                initialValue = if (label == "Return Rate") field.text.filter(Char::isDigit) else java.math.BigDecimal.valueOf(Calculator.parseNumber(field.text) ?: 0.0).stripTrailingZeros().toPlainString(),
                                state = state,
                                allowNegative = allowNegative,
                                onDismiss = { if (sheetVisibleState == null) localSheetVisible = false else sheetVisibleState.value = false },
                                onSave = { sheetValue ->
                                    field = TextFieldValue(sheetValue, TextRange(sheetValue.length))
                                    onValue(if (label == "Return Rate") Calculator.parseReturnRateDigits(sheetValue) else Calculator.parseNumber(sheetValue))
                                },
                                onNext = onNext?.let { next -> {
                                    if (sheetVisibleState == null) localSheetVisible = false else sheetVisibleState.value = false
                                    next()
                                } }
                            )
                        }
                    }

                    @Composable
                    fun ToggleRow(label: String, checked: Boolean, onChecked: () -> Unit) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Switch(checked = checked, onCheckedChange = { onChecked() })
                        }
                    }

                    @OptIn(ExperimentalMaterial3Api::class)
                    @Composable
                    fun Dropdown(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                selected,
                                {},
                                readOnly = true,
                                label = { Text(label) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).width(190.dp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                options.forEach { option ->
                                    androidx.compose.material3.DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false })
                                }
                            }
                        }
                    }

                    @Composable
                    fun SettingsScreen(state: UiState, viewModel: HoangViewModel, exportJson: (String) -> Unit, importJson: (Array<String>) -> Unit) {
                        val settings = state.settings
                        var resetDefaultsConfirm by remember { mutableStateOf(false) }
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            ZoneCard("settings_colors", state, "Colors", viewModel) {
                                listOf(
                                    "Background" to settings.backgroundHex,
                                    "Season" to settings.seasonHex,
                                    "Panel" to settings.panelHex,
                                    "Category" to settings.categoryHex,
                                    "Text Box" to settings.textBoxHex,
                                    "Number Box" to settings.numberBoxHex,
                                    "Button" to settings.buttonHex,
                                    "Text Color" to settings.textColorHex,
                                    "Accent" to settings.accentHex,
                                    "Other" to settings.otherHex
                                ).forEach { (label, hex) ->
                                    var pickerVisible by remember(hex) { mutableStateOf(false) }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(48.dp).padding(5.dp).clip(RoundedCornerShape(10.dp)).background(parseColor(hex, Color.Black)).border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp)).clickable { pickerVisible = true })
                                        Spacer(Modifier.width(10.dp))
                                        OutlinedTextField(hex, { value ->
                                            if (value.startsWith("#") && (value.length == 7 || value.length == 9)) {
                                                val valid = value.drop(1).all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }
                                                if (valid) viewModel.updateSetting { current ->
                                                val updated = when (label) {
                                                    "Background" -> current.copy(backgroundHex = value)
                                                    "Season" -> current.copy(seasonHex = value)
                                                    "Panel" -> current.copy(panelHex = value)
                                                    "Category" -> current.copy(categoryHex = value)
                                                    "Text Box" -> current.copy(textBoxHex = value)
                                                    "Number Box" -> current.copy(numberBoxHex = value)
                                                    "Button" -> current.copy(buttonHex = value)
                                                    "Text Color" -> current.copy(textColorHex = value)
                                                    "Accent" -> current.copy(accentHex = value)
                                                    else -> current.copy(otherHex = value)
                                                }
                                                updated.copy(themePreset = "Custom")
                                                }
                                            }
                                        }, Modifier.weight(1f), label = { Text(label) })
                                    }
                                    if (pickerVisible) {
                                        val initialColor = parseColor(hex, Color.Black)
                                        val initialHsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(initialColor.toArgb(), initialHsv)
                                        var hue by remember(hex) { mutableStateOf(initialHsv[0]) }
                                        var saturation by remember(hex) { mutableStateOf(initialHsv[1]) }
                                        var value by remember(hex) { mutableStateOf(initialHsv[2]) }
                                        var alpha by remember(hex) {
                                            val parsed = parseColor(hex, Color.Black)
                                            mutableStateOf(parsed.alpha)
                                        }
                                        var hexDraft by remember(hex) { mutableStateOf(hex) }
                                        val hsv = floatArrayOf(hue, saturation, value)
                                        val picked = Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), hsv))
                                        androidx.compose.material3.AlertDialog(
                                            onDismissRequest = { pickerVisible = false },
                                            title = { Text(label) },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    var squareSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
                                                    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                                    Box(
                                                        Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp))
                                                            .onSizeChanged { squareSize = it }
                                                            .background(hueColor)
                                                            .background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                                                            .pointerInput(Unit) {
                                                                detectTapGestures { position ->
                                                                    if (squareSize.width > 0 && squareSize.height > 0) {
                                                                        saturation = (position.x / squareSize.width).coerceIn(0f, 1f)
                                                                        value = 1f - (position.y / squareSize.height).coerceIn(0f, 1f)
                                                                    }
                                                                }
                                                            }
                                                            .pointerInput(Unit) {
                                                                detectDragGestures { change, _ ->
                                                                    if (squareSize.width > 0 && squareSize.height > 0) {
                                                                        saturation = (change.position.x / squareSize.width).coerceIn(0f, 1f)
                                                                        value = 1f - (change.position.y / squareSize.height).coerceIn(0f, 1f)
                                                                    }
                                                                    change.consume()
                                                                }
                                                            }
                                                    )
                                                    Slider(hue, { hue = it }, valueRange = 0f..360f)
                                                    Slider(saturation, { saturation = it }, valueRange = 0f..1f)
                                                    Slider(value, { value = it }, valueRange = 0f..1f)
                                                    Slider(alpha, { alpha = it }, valueRange = 0f..1f)
                                                    OutlinedTextField(
                                                        value = hexDraft,
                                                        onValueChange = { draft ->
                                                            hexDraft = draft
                                                            if (draft.startsWith("#") && (draft.length == 7 || draft.length == 9) &&
                                                                draft.drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                                                                val parsed = parseColor(draft, picked)
                                                                val parsedHsv = FloatArray(3)
                                                                android.graphics.Color.colorToHSV(parsed.toArgb(), parsedHsv)
                                                                hue = parsedHsv[0]
                                                                saturation = parsedHsv[1]
                                                                value = parsedHsv[2]
                                                                alpha = parsed.alpha
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        label = { Text("Hex color") },
                                                        singleLine = true
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(Modifier.size(56.dp).background(parseColor(hex, Color.Black), RoundedCornerShape(8.dp)))
                                                        Box(Modifier.size(56.dp).background(picked, RoundedCornerShape(8.dp)))
                                                        Text("#" + java.lang.Integer.toHexString(picked.toArgb()).uppercase().padStart(8, '0'))
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                IconButton(onClick = {
                                                    val newHex = "#" + java.lang.Integer.toHexString(picked.toArgb()).uppercase().padStart(8, '0')
                                                    viewModel.updateSetting { current ->
                                                        val updated = when (label) {
                                                            "Background" -> current.copy(backgroundHex = newHex)
                                                            "Season" -> current.copy(seasonHex = newHex)
                                                            "Panel" -> current.copy(panelHex = newHex)
                                                            "Category" -> current.copy(categoryHex = newHex)
                                                            "Text Box" -> current.copy(textBoxHex = newHex)
                                                            "Number Box" -> current.copy(numberBoxHex = newHex)
                                                            "Button" -> current.copy(buttonHex = newHex)
                                                            "Text Color" -> current.copy(textColorHex = newHex)
                                                            "Accent" -> current.copy(accentHex = newHex)
                                                            else -> current.copy(otherHex = newHex)
                                                        }
                                                        updated.copy(themePreset = "Custom")
                                                    }
                                                    pickerVisible = false
                                                }) { Icon(Icons.Default.Save, "Apply color") }
                                            },
                                            dismissButton = { IconButton(onClick = { pickerVisible = false }) { Icon(Icons.Default.Close, "Cancel color") } }
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                            ZoneCard("settings_theme", state, "Theme & Appearance", viewModel) {
                                Dropdown("Theme Preset", settings.themePreset, listOf("Dark", "Light", "AMOLED black", "Custom")) { preset ->
                                    viewModel.updateSetting { current ->
                                        when (preset) {
                                            "Light" -> current.copy(themePreset = preset, backgroundHex="#F4F4F4", seasonHex="#FFFFFF", panelHex="#FFFFFF", categoryHex="#EAEAEA", textBoxHex="#F0F0F0", numberBoxHex="#FFFFFF", buttonHex="#E0E0E0", textColorHex="#202124", accentHex="#FFFF5252", otherHex="#DADADA")
                                            "AMOLED black" -> current.copy(themePreset = preset, backgroundHex="#000000", seasonHex="#000000", panelHex="#000000", categoryHex="#111111", textBoxHex="#111111", numberBoxHex="#111111", buttonHex="#222222", textColorHex="#FFFFFF", accentHex="#FFFF5252", otherHex="#111111")
                                            "Dark" -> current.copy(themePreset = preset, backgroundHex="#121212", seasonHex="#202124", panelHex="#191919", categoryHex="#242424", textBoxHex="#303030", numberBoxHex="#303030", buttonHex="#3A3A3A", textColorHex="#FFFFFF", accentHex="#FFFF5252", otherHex="#2A2A2A")
                                            else -> current.copy(themePreset = "Custom")
                                        }
                                    }
                                }
                                Dropdown("UI Density", settings.uiDensity, listOf("Comfortable", "Compact", "Spacious")) { viewModel.updateSetting { current -> current.copy(uiDensity = it) } }
                                Dropdown("Number Format", settings.numberFormat, listOf("1,234", "1.234", "Short")) { viewModel.updateSetting { current -> current.copy(numberFormat = it) } }
                                Dropdown("Number Rounding", settings.numberRounding, listOf("Always", "Never", "Click to switch")) { viewModel.updateSetting { current -> current.copy(numberRounding = it) } }
                                SliderSetting("Corner Radius", settings.cornerRadius, 0f, 32f) { viewModel.updateSetting { current -> current.copy(cornerRadius = it) } }
                                SliderSetting("Font Size", settings.fontSize, 12f, 24f) { viewModel.updateSetting { current -> current.copy(fontSize = it) } }
                                SliderSetting("Card Opacity", settings.cardOpacity, 0.3f, 1f) { viewModel.updateSetting { current -> current.copy(cardOpacity = it) } }
                                SliderSetting("Animation Speed", settings.animationSpeed, 0.5f, 2f) { viewModel.updateSetting { current -> current.copy(animationSpeed = it) } }
                                SliderSetting("Decimal Precision", settings.decimalPrecision.toFloat(), 0f, 6f) { viewModel.updateSetting { current -> current.copy(decimalPrecision = it.toInt()) } }
                            }
                            ZoneCard("settings_audio", state, "Audio", viewModel) {
                                AudioSlider("Master", settings.masterAudio) { value -> viewModel.updateSetting { it.copy(masterAudio = value) } }
                                AudioSlider("Effects", settings.effectsAudio) { viewModel.updateSetting { current -> current.copy(effectsAudio = it) } }
                                AudioSlider("Background", settings.backgroundAudio) { viewModel.updateSetting { current -> current.copy(backgroundAudio = it) } }
                                AudioSlider("Video Sound", settings.videoSound) { viewModel.updateSetting { current -> current.copy(videoSound = it) } }
                            }
                            ZoneCard("settings_behavior", state, "Behavior & Layout", viewModel) {
                                ToggleRow("Splash animation", settings.splashEnabled) { viewModel.updateSetting { it.copy(splashEnabled = !it.splashEnabled) } }
                                ToggleRow("Built-in number keypad", settings.keypadEnabled) { viewModel.updateSetting { it.copy(keypadEnabled = !it.keypadEnabled) } }
                                ToggleRow("Keypad symbol chips", settings.keypadSymbolChips) { viewModel.updateSetting { it.copy(keypadSymbolChips = !it.keypadSymbolChips) } }
                                ToggleRow("Keypad quick adjust chips", settings.keypadQuickAdjustChips) { viewModel.updateSetting { it.copy(keypadQuickAdjustChips = !it.keypadQuickAdjustChips) } }
                                ToggleRow("Animations", settings.animationsEnabled) { viewModel.updateSetting { it.copy(animationsEnabled = !it.animationsEnabled) } }
                                ToggleRow("Horizontal HUD", settings.horizontalHud) { viewModel.updateSetting { it.copy(horizontalHud = !it.horizontalHud) } }
                                ToggleRow("Vertical HUD", settings.verticalHud) { viewModel.updateSetting { it.copy(verticalHud = !it.verticalHud) } }
                                ToggleRow("Auto save", settings.autoSave) { viewModel.updateSetting { it.copy(autoSave = !it.autoSave) } }
                                ToggleRow("Force keep-alive", settings.forceKeepAlive) { viewModel.updateSetting { it.copy(forceKeepAlive = !it.forceKeepAlive) } }
                                ToggleRow("Remember open seasons", settings.rememberOpenSeasons) { viewModel.updateSetting { it.copy(rememberOpenSeasons = !it.rememberOpenSeasons) } }
                                ToggleRow("Haptic feedback", settings.hapticsEnabled) { viewModel.updateSetting { it.copy(hapticsEnabled = !it.hapticsEnabled) } }
                                ToggleRow("Keep screen on", settings.keepScreenOn) { viewModel.updateSetting { it.copy(keepScreenOn = !it.keepScreenOn) } }
                                ToggleRow("Confirm before delete", settings.confirmBeforeDelete) { viewModel.updateSetting { it.copy(confirmBeforeDelete = !it.confirmBeforeDelete) } }
                            }
                            ZoneCard("quick_settings", state, "Quick Settings", viewModel) {
                                val options = listOf("Splash", "Keypad", "Keypad symbol chips", "Keypad quick adjust chips", "Animations", "Horizontal HUD", "Vertical HUD", "Auto save", "Remember open seasons", "Keep-alive", "Haptics", "Keep screen on", "Confirm before delete", "Number Rounding", "Theme Preset")
                                options.forEach { option ->
                                    val selected = option in settings.quickSettings
                                    Row(Modifier.fillMaxWidth().clickable {
                                        viewModel.updateSetting { current -> current.copy(quickSettings = if (selected) current.quickSettings - option else current.quickSettings + option) }
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(selected, null)
                                        Text(option)
                                    }
                                }
                            }
                            ZoneCard("settings_media", state, "Background & Surface Media", viewModel) {
                                MediaPickerSlot("Background image/GIF/video", settings.backgroundImage.ifBlank { settings.backgroundVideo }, settings.videoSound) { path -> viewModel.updateSetting { current -> current.copy(backgroundImage = if (path.endsWith(".gif") || path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".webp")) path else "", backgroundVideo = if (path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mkv") || path.endsWith(".mov")) path else "") } }

                                listOf("Season", "Panel", "Category", "Text Box", "Number Box", "Button", "Text Color").forEach { label ->
                                    val path = when (label) { "Season" -> settings.seasonMedia; "Panel" -> settings.panelMedia; "Category" -> settings.categoryMedia; "Text Box" -> settings.textBoxMedia; "Number Box" -> settings.numberBoxMedia; "Button" -> settings.buttonMedia; else -> settings.textColorMedia }
                                    MediaPickerSlot(label, path, settings.videoSound) { picked -> viewModel.updateSetting { current -> when (label) { "Season" -> current.copy(seasonMedia = picked); "Panel" -> current.copy(panelMedia = picked); "Category" -> current.copy(categoryMedia = picked); "Text Box" -> current.copy(textBoxMedia = picked); "Number Box" -> current.copy(numberBoxMedia = picked); "Button" -> current.copy(buttonMedia = picked); else -> current.copy(textColorMedia = picked) } } }
                                }
                            }
                            ZoneCard("default_seasons", state, "Default Season Settings", viewModel) {
                                ToggleRow("Use default season settings", settings.useDefaultSeasonSettings) { viewModel.updateSetting { it.copy(useDefaultSeasonSettings = !it.useDefaultSeasonSettings) } }
                                OutlinedTextField(
                                    value = settings.defaultSeason.panelName,
                                    onValueChange = { value -> viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(panelName = value)) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Panel info") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = settings.defaultSeason.description,
                                    onValueChange = { value -> viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(description = value)) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Recipe") },
                                    singleLine = true
                                )
                                ToggleRow("Premium Active", settings.defaultSeason.premiumActive) { viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(premiumActive = !it.defaultSeason.premiumActive)) } }
                                ToggleRow("Sell Leftover", settings.defaultSeason.sellLeftover) { viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(sellLeftover = !it.defaultSeason.sellLeftover)) } }
                                DebouncedTextField(
                                    java.math.BigDecimal(settings.defaultSeason.returnRate).stripTrailingZeros().toPlainString(),
                                    { value -> viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(returnRate = java.math.BigDecimal.valueOf(Calculator.parseReturnRateDigits(value.filter(Char::isDigit))).stripTrailingZeros().toPlainString())) } },
                                    Modifier.fillMaxWidth(),
                                    "Return Rate"
                                )
                                DebouncedTextField(
                                    settings.defaultSeason.costFeePer100?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty(),
                                    { value -> viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(costFeePer100 = Calculator.parseNumber(value))) } },
                                    Modifier.fillMaxWidth(),
                                    "Nutrition fee /100"
                                )
                                DebouncedTextField(
                                    settings.defaultSeason.finalOutputValue?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty(),
                                    { value -> viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(finalOutputValue = Calculator.parseNumber(value))) } },
                                    Modifier.fillMaxWidth(),
                                    "Item value (nutrition)"
                                )
                                Dropdown("Buy Location", settings.defaultSeason.inputs.firstOrNull()?.buyLocation ?: "Caerleon", LOCATION_OPTIONS) { location -> viewModel.updateSetting { it.copy(lastBuyLocation = location, defaultSeason = it.defaultSeason.copy(inputs = it.defaultSeason.inputs.map { input -> input.copy(buyLocation = location) })) } }
                                Dropdown("Sell Location", settings.defaultSeason.outputs.firstOrNull()?.sellLocation ?: "Caerleon", LOCATION_OPTIONS) { location -> viewModel.updateSetting { it.copy(lastSellLocation = location, defaultSeason = it.defaultSeason.copy(outputs = it.defaultSeason.outputs.map { output -> output.copy(sellLocation = location) })) } }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Input templates (${settings.defaultSeason.inputs.size})", modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(inputs = it.defaultSeason.inputs + InputMaterial())) } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, "Add default input") }
                                    IconButton(onClick = { viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(inputs = it.defaultSeason.inputs.dropLast(1))) } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Remove default input") }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Output templates (${settings.defaultSeason.outputs.size})", modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(outputs = it.defaultSeason.outputs + OutputProduct())) } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Add, "Add default output") }
                                    IconButton(onClick = { viewModel.updateSetting { it.copy(defaultSeason = it.defaultSeason.copy(outputs = it.defaultSeason.outputs.dropLast(1))) } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Remove default output") }
                                }
                                Row {
                                    IconButton(onClick = viewModel::saveCurrentSeasonAsDefault, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Save, "Save current season as default") }
                                    IconButton(onClick = { resetDefaultsConfirm = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.RestartAlt, "Reset defaults to factory") }
                                }
                                if (resetDefaultsConfirm) androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { resetDefaultsConfirm = false },
                                    title = { Text("Reset default settings") },
                                    text = { Text("Restore factory defaults for new seasons?") },
                                    confirmButton = { IconButton(onClick = { viewModel.resetDefaultSeasonSettings(); resetDefaultsConfirm = false }) { Icon(Icons.Default.Check, "Confirm reset") } },
                                    dismissButton = { IconButton(onClick = { resetDefaultsConfirm = false }) { Icon(Icons.Default.Close, "Cancel reset") } }
                                )
                            }
                            ZoneCard("backup", state, "Backup & Storage", viewModel) {
                                IconButton(onClick = { exportJson("hoang-calculator-backup.json") }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.FileDownload, "Export JSON") }
                                IconButton(onClick = { importJson(arrayOf("application/json")) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.FileUpload, "Import JSON") }
                                Text("Preferred folder: /sdcard/Download/1 Hoàng Calculator Folder")
                                Text("Fallback: app private storage")
                                IconButton(onClick = { viewModel.updateSetting { AppSettings() } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.RestartAlt, "Reset settings") }
                            }
                            ZoneCard("about", state, "About", viewModel) {
                                Text("Hoàng Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Version ${BuildConfig.VERSION_NAME}")
                            }
                        }
                    }

                    @Composable
                    fun SliderSetting(label: String, value: Float, min: Float, max: Float, onValue: (Float) -> Unit) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, modifier = Modifier.weight(1f))
                                Text(if (label == "Animation Speed" || label == "Card Opacity") String.format(Locale.US, "%.2f", value) else value.toInt().toString())
                            }
                            Slider(value = value, onValueChange = onValue, valueRange = min..max)
                        }
                    }

                    @Composable
                    fun MediaPickerSlot(label: String, path: String, videoSound: Float, onPicked: (String) -> Unit) {
                        val context = LocalContext.current
                        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                        val picker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                            if (uri != null) copyPickedMedia(context, uri, "media")?.let(onPicked)
                        }
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(label, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                                    if (path.isBlank()) Text("Preview", style = MaterialTheme.typography.labelSmall)
                                    else if (path.lowercase().matches(Regex(".*\\.(mp4|webm|mkv|mov)$"))) {
                                        androidx.compose.ui.viewinterop.AndroidView(factory = { ctx ->
                                            androidx.media3.ui.PlayerView(ctx).apply {
                                                useController = false
                                                val player = androidx.media3.exoplayer.ExoPlayer.Builder(ctx).build().apply {
                                                    setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
                                                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                                                    volume = videoSound.coerceIn(0f, 1f)
                                                    playWhenReady = true
                                                    prepare()
                                                }
                                                this.player = player
                                                val observer = LifecycleEventObserver { _, event ->
                                                    when (event) {
                                                        Lifecycle.Event.ON_START -> player.playWhenReady = true
                                                        Lifecycle.Event.ON_STOP -> player.playWhenReady = false
                                                        Lifecycle.Event.ON_DESTROY -> player.release()
                                                        else -> Unit
                                                    }
                                                }
                                                lifecycleOwner.lifecycle.addObserver(observer)
                                                addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                                                    override fun onViewAttachedToWindow(view: android.view.View) {}
                                                    override fun onViewDetachedFromWindow(view: android.view.View) {
                                                        lifecycleOwner.lifecycle.removeObserver(observer)
                                                        player.release()
                                                    }
                                                })
                                            }
                                        }, update = { view -> view.player?.volume = videoSound.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxSize())
                                    } else AsyncImage(model = java.io.File(path), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.AddPhotoAlternate, "Pick media") }
                                IconButton(onClick = {
                                    if (path.isNotBlank()) java.io.File(path).delete()
                                    onPicked("")
                                }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "Remove media") }
                            }
                        }
                    }

                    @Composable
                    fun AudioSlider(label: String, value: Float, onValue: (Float) -> Unit) {
                        Column {
                            Text("$label: ${(value * 100).toInt()}%")
                            Slider(value = value, onValueChange = onValue, valueRange = 0f..1f)
                        }
                    }

                    @Composable
                    fun QuickSettings(state: UiState, viewModel: HoangViewModel) {
                        val selected = state.settings.quickSettings
                        if (selected.isEmpty()) return
                        Spacer(Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("Quick Settings", fontWeight = FontWeight.Bold)
                                selected.forEach { item ->
                                    val checked = when (item) {
                                        "Splash" -> state.settings.splashEnabled
                                        "Keypad" -> state.settings.keypadEnabled
                                        "Keypad symbol chips" -> state.settings.keypadSymbolChips
                                        "Keypad quick adjust chips" -> state.settings.keypadQuickAdjustChips
                                        "Animations" -> state.settings.animationsEnabled
                                        "Horizontal HUD" -> state.settings.horizontalHud
                                        "Vertical HUD" -> state.settings.verticalHud
                                        "Auto save" -> state.settings.autoSave
                                        "Remember open seasons" -> state.settings.rememberOpenSeasons
                                        "Keep-alive" -> state.settings.forceKeepAlive
                                        "Haptics" -> state.settings.hapticsEnabled
                                        "Keep screen on" -> state.settings.keepScreenOn
                                        "Confirm before delete" -> state.settings.confirmBeforeDelete
                                        "Premium" -> state.settings.premiumDefault
                                        "Sell Leftover" -> state.settings.sellLeftoverDefault
                                        else -> false
                                    }
                                    ToggleRow(item, checked) {
                                        viewModel.updateSetting { settings ->
                                            when (item) {
                                                "Splash" -> settings.copy(splashEnabled = !settings.splashEnabled)
                                                "Keypad" -> settings.copy(keypadEnabled = !settings.keypadEnabled)
                                                "Keypad symbol chips" -> settings.copy(keypadSymbolChips = !settings.keypadSymbolChips)
                                                "Keypad quick adjust chips" -> settings.copy(keypadQuickAdjustChips = !settings.keypadQuickAdjustChips)
                                                "Animations" -> settings.copy(animationsEnabled = !settings.animationsEnabled)
                                                "Horizontal HUD" -> settings.copy(horizontalHud = !settings.horizontalHud)
                                                "Vertical HUD" -> settings.copy(verticalHud = !settings.verticalHud)
                                                "Auto save" -> settings.copy(autoSave = !settings.autoSave)
                                                "Remember open seasons" -> settings.copy(rememberOpenSeasons = !settings.rememberOpenSeasons)
                                                "Keep-alive" -> settings.copy(forceKeepAlive = !settings.forceKeepAlive)
                                                "Haptics" -> settings.copy(hapticsEnabled = !settings.hapticsEnabled)
                                                "Keep screen on" -> settings.copy(keepScreenOn = !settings.keepScreenOn)
                                                "Confirm before delete" -> settings.copy(confirmBeforeDelete = !settings.confirmBeforeDelete)
                                                "Premium" -> settings.copy(premiumDefault = !settings.premiumDefault)
                                                "Sell Leftover" -> settings.copy(sellLeftoverDefault = !settings.sellLeftoverDefault)
                                                else -> settings
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
