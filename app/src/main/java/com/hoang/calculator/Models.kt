package com.hoang.calculator

import androidx.compose.runtime.Immutable
import java.math.BigDecimal
import java.util.UUID

@Immutable
data class InputMaterial(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val imageUri: String = "",
    val buyLocation: String = "Caerleon",
    val buyMethod: String = "Buy Now",
    val price: Double? = null,
    val amountPerCraft: Double? = null,
    val useReturnRate: Boolean = true,
    val selected: Boolean = false
)

@Immutable
data class OutputProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val imageUri: String = "",
    val sellLocation: String = "Caerleon",
    val sellMethod: String = "Sell Now",
    val price: Double? = null,
    val selected: Boolean = false
)

@Immutable
data class Adjustment(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amount: Double? = null
)

@Immutable
data class Season(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val panelName: String = "",
    val description: String = "",
    val imageUri: String = "",
    val costFeePer100: Double? = null,
    val finalOutputValue: Double? = null,
    val returnRate: String = "0",
    val totalWillMade: Double? = null,
    val outputPerCraft: Double = 1.0,
    val premiumActive: Boolean = false,
    val sellLeftover: Boolean = true,
    val craftFee: Double = 0.0,
    val inputs: List<InputMaterial> = emptyList(),
    val outputs: List<OutputProduct> = emptyList(),
    val adjustments: List<Adjustment> = emptyList(),
    val editedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val manualOrder: Int = 0,
    val totalExpenses: Double? = null,
    val totalIncome: Double? = null,
    val profit: Double? = null,
    val roiPercent: Double? = null,
    val temporary: Boolean = true
)

@Immutable
data class AppSettings(
    val backgroundHex: String = "#121212",
    val seasonHex: String = "#202124",
    val panelHex: String = "#191919",
    val categoryHex: String = "#242424",
    val textBoxHex: String = "#303030",
    val numberBoxHex: String = "#303030",
    val otherHex: String = "#2A2A2A",
    val buttonHex: String = "#3A3A3A",
    val textColorHex: String = "#FFFFFF",
    val accentHex: String = "#FFFF5252",
    val backgroundImage: String = "",
    val backgroundVideo: String = "",
    val seasonMedia: String = "",
    val panelMedia: String = "",
    val categoryMedia: String = "",
    val textBoxMedia: String = "",
    val numberBoxMedia: String = "",
    val buttonMedia: String = "",
    val textColorMedia: String = "",
    val themePreset: String = "Dark",
    val cornerRadius: Float = 18f,
    val fontSize: Float = 14f,
    val uiDensity: String = "Comfortable",
    val cardOpacity: Float = 1f,
    val animationSpeed: Float = 1f,
    val splashEnabled: Boolean = true,
    val masterAudio: Float = 1f,
    val effectsAudio: Float = 1f,
    val backgroundAudio: Float = 1f,
    val videoSound: Float = 1f,
    val keypadEnabled: Boolean = true,
    val keypadSymbolChips: Boolean = true,
    val keypadQuickAdjustChips: Boolean = true,
    val animationsEnabled: Boolean = true,
    val horizontalHud: Boolean = true,
    val autoSave: Boolean = true,
    val rememberOpenSeasons: Boolean = true,
    val forceKeepAlive: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val confirmBeforeDelete: Boolean = true,
    val premiumDefault: Boolean = false,
    val sellLeftoverDefault: Boolean = true,
    val numberFormat: String = "1,234",
    val numberRounding: String = "Always",
    val decimalPrecision: Int = 2,
    val seasonSort: String = "Last edited newest",
    val seasonEditMode: Boolean = false,
    val homeEditMode: Boolean = false,
    val verticalHud: Boolean = true,
    val quickSettings: List<String> = emptyList(),
    val useDefaultSeasonSettings: Boolean = false,
    val defaultSeason: Season = Season(),
    val lastBuyLocation: String = "Caerleon",
    val lastBuyMethod: String = "Buy Now",
    val lastSellLocation: String = "Caerleon",
    val lastSellMethod: String = "Sell Now",
    val enabledAnimations: List<String> = listOf("Splash", "Cards", "Tabs")
)

val LOCATION_OPTIONS = listOf(
    "Breclin", "Black Market", "Lymhurst", "Fort Sterling", "The Ford",
    "Mart Lock", "Bridge Watch", "Caerleon", "Arthur's Rest",
    "Merlyn's Rest", "Morgana's Rest"
)
val METHOD_OPTIONS = listOf("Buy Now", "Buy Order")
val SELL_METHOD_OPTIONS = listOf("Sell Now", "Sell Order")

fun normalizeLocation(value: String): String = if (value in LOCATION_OPTIONS) value else "Caerleon"
fun normalizeBuyMethod(value: String): String = if (value in METHOD_OPTIONS) value else "Buy Now"
fun normalizeSellMethod(value: String): String = if (value in SELL_METHOD_OPTIONS) value else "Sell Now"

fun AppSettings.normalized(): AppSettings = copy(
    themePreset = when (themePreset) { "Dark" -> "Dark"; "Light" -> "Light"; "AMOLED black", "AMOLED" -> "AMOLED black"; "Custom" -> "Custom"; else -> "Dark" },
    uiDensity = when (uiDensity) { "Comfortable", "Compact", "Spacious" -> uiDensity; else -> "Comfortable" },
    numberFormat = when (numberFormat) { "1,234", "1.234", "Short" -> numberFormat; else -> "1,234" },
    numberRounding = when (numberRounding) { "Always", "Never", "Click to switch" -> numberRounding; else -> "Always" },
    seasonSort = when (seasonSort) { "Name A-Z", "Name Z-A", "Last edited newest", "Last edited oldest", "Created newest", "Created oldest", "Profit high-low", "Profit low-high", "Profit % high-low", "Profit % low-high", "Cost high-low", "Cost low-high", "Earn high-low", "Earn low-high", "Pinned first", "Manual order" -> seasonSort; else -> "Last edited newest" },
    cornerRadius = cornerRadius.coerceIn(0f, 32f),
    fontSize = fontSize.coerceIn(12f, 24f),
    cardOpacity = cardOpacity.coerceIn(0.3f, 1f),
    animationSpeed = animationSpeed.coerceIn(0.5f, 2f),
    masterAudio = masterAudio.coerceIn(0f, 1f),
    effectsAudio = effectsAudio.coerceIn(0f, 1f),
    backgroundAudio = backgroundAudio.coerceIn(0f, 1f),
    videoSound = videoSound.coerceIn(0f, 1f),
    decimalPrecision = decimalPrecision.coerceIn(0, 6),
    lastBuyLocation = normalizeLocation(lastBuyLocation),
    lastSellLocation = normalizeLocation(lastSellLocation),
    lastBuyMethod = normalizeBuyMethod(lastBuyMethod),
    lastSellMethod = normalizeSellMethod(lastSellMethod),
    defaultSeason = defaultSeason.copy(
        returnRate = runCatching {
            val rate = BigDecimal(defaultSeason.returnRate.ifBlank { "0" }).coerceAtLeast(BigDecimal.ZERO).coerceAtMost(BigDecimal("99.99999999"))
            rate.stripTrailingZeros().toPlainString()
        }.getOrDefault("0"),
        inputs = defaultSeason.inputs.map { it.copy(buyLocation = normalizeLocation(it.buyLocation), buyMethod = normalizeBuyMethod(it.buyMethod)) },
        outputs = defaultSeason.outputs.map { it.copy(sellLocation = normalizeLocation(it.sellLocation), sellMethod = normalizeSellMethod(it.sellMethod)) }
    )
)

@Immutable
data class UiState(
    val loaded: Boolean = false,
    val splashVisible: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val seasons: List<Season> = emptyList(),
    val openSeasonIds: List<String> = emptyList(),
    val selectedSeasonId: String? = null,
    val screen: String = "home",
    val search: String = "",
    val undo: List<Season> = emptyList(),
    val redo: List<Season> = emptyList(),
    val collapsed: Map<String, Boolean> = emptyMap(),
    val numberPadVisible: Boolean = false,
    val calculation: CalculationResult? = null
)
