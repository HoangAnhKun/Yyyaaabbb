package com.hoang.calculator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Season(
    val id: String,
    var name: String = "",
    var panelName: String = "",
    var description: String = "",
    var settings: CraftSettings = CraftSettings(),
    var inputs: MutableList<InputMaterial> = mutableListOf(),
    var outputs: MutableList<OutputProduct> = mutableListOf(),
    var lastEdited: Long = System.currentTimeMillis(),
    var pinned: Boolean = false
) {
    fun snapshot(): Season = copy(
        settings = settings.copy(),
        inputs = inputs.map { it.copy() }.toMutableList(),
        outputs = outputs.map { it.copy() }.toMutableList()
    )
}

data class AppData(
    var seasons: MutableList<Season> = mutableListOf(),
    var openSeasonIds: MutableList<String> = mutableListOf(),
    var selectedSeasonId: String? = null,
    var splashEnabled: Boolean = true,
    var builtinKeypad: Boolean = true,
    var autoSave: Boolean = true,
    var forceKeepAlive: Boolean = false,
    var quickSettingsIds: MutableList<String> = mutableListOf()
)

fun parseNumber(text: String): Double? {
    val normalized = text.trim().lowercase().replace(",", "").replace("_", "")
    if (normalized.isEmpty()) return null
    val suffix = normalized.takeLast(1)
    val numberText = if (suffix == "k" || suffix == "m" || suffix == "b") normalized.dropLast(1) else normalized
    val value = numberText.toDoubleOrNull() ?: return null
    return when (suffix) {
        "k" -> value * 1_000
        "m" -> value * 1_000_000
        "b" -> value * 1_000_000_000
        else -> value
    }
}

fun formatCompact(value: Double?): String {
    if (value == null || !value.isFinite()) return "—"
    val absolute = kotlin.math.abs(value)
    val text = when {
        absolute >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000)
        absolute >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        absolute >= 1_000 -> "%.1fK".format(value / 1_000)
        absolute >= 100 -> "%.0f".format(value)
        else -> "%.2f".format(value).trimEnd('0').trimEnd('.')
    }
    return text.replace('-', '-')
}

object AppStorage {
    private const val fileName = "hoang_calculator.json"

    fun dataFile(context: Context): File = File(context.filesDir, fileName)

    fun save(context: Context, data: AppData) {
        context.filesDir.mkdirs()
        val root = JSONObject()
        root.put("version", "0.0.10")
        val seasons = JSONArray()
        data.seasons.forEach { season ->
            val item = JSONObject()
            item.put("id", season.id)
            item.put("name", season.name)
            item.put("panelName", season.panelName)
            item.put("description", season.description)
            item.put("lastEdited", season.lastEdited)
            item.put("pinned", season.pinned)
            val s = season.settings
            item.put("settings", JSONObject()
                .putOpt("costFeePer100", s.costFeePer100)
                .putOpt("finalOutputValue", s.finalOutputValue)
                .put("returnRate", s.returnRate)
                .putOpt("totalWillMade", s.totalWillMade)
                .put("outputPerCraft", s.outputPerCraft)
                .put("premiumActive", s.premiumActive)
                .put("sellLeftover", s.sellLeftover)
                .put("craftFee", s.craftFee))
            val inputs = JSONArray()
            season.inputs.forEach { value ->
                inputs.put(JSONObject().put("id", value.id).put("name", value.name)
                    .put("imageUri", value.imageUri).put("buyLocation", value.buyLocation)
                    .put("buyMethod", value.buyMethod).putOpt("price", value.price)
                    .putOpt("amountPerCraft", value.amountPerCraft).put("useReturnRate", value.useReturnRate))
            }

            item.put("inputs", inputs)
            val outputs = JSONArray()
            season.outputs.forEach { value ->
                outputs.put(JSONObject().put("id", value.id).put("name", value.name)
                    .put("imageUri", value.imageUri).put("sellLocation", value.sellLocation)
                    .put("sellMethod", value.sellMethod).putOpt("price", value.price))
            }
            item.put("outputs", outputs)
            seasons.put(item)
        }
        root.put("seasons", seasons)
        root.put("openSeasonIds", JSONArray(data.openSeasonIds))
        root.putOpt("selectedSeasonId", data.selectedSeasonId)
        root.put("splashEnabled", data.splashEnabled)
        root.put("builtinKeypad", data.builtinKeypad)
        root.put("autoSave", data.autoSave)
        root.put("forceKeepAlive", data.forceKeepAlive)
        root.put("quickSettingsIds", JSONArray(data.quickSettingsIds))
        dataFile(context).writeText(root.toString(2))
        val download = File("/sdcard/Download/1 Hoàng Calculator Folder")
        if (download.mkdirs() || download.exists()) {
            runCatching { File(download, fileName).writeText(root.toString(2)) }
        }
    }

    fun import(context: Context, uri: android.net.Uri): AppData? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val text = input.bufferedReader().use { it.readText() }
            dataFile(context).writeText(text)
            load(context, false)
        }
    }

    fun load(context: Context, preferExternal: Boolean = true): AppData {
        val preferred = File("/sdcard/Download/1 Hoàng Calculator Folder/$fileName")
        val file = if (preferExternal && preferred.exists()) preferred else dataFile(context)
        if (!file.exists()) return AppData()
        return runCatching {
            val root = JSONObject(file.readText())
            val data = AppData()
            val seasons = root.optJSONArray("seasons") ?: JSONArray()
            for (i in 0 until seasons.length()) {
                val item = seasons.getJSONObject(i)
                val s = item.optJSONObject("settings") ?: JSONObject()
                val settings = CraftSettings(
                    optDouble(s, "costFeePer100"),
                    optDouble(s, "finalOutputValue"),
                    s.optDouble("returnRate", 0.0),
                    optDouble(s, "totalWillMade"),
                    s.optDouble("outputPerCraft", 1.0),
                    s.optBoolean("premiumActive", false),
                    s.optBoolean("sellLeftover", true),
                    s.optDouble("craftFee", 0.0)
                )
                val inputs = mutableListOf<InputMaterial>()
                val inputsJson = item.optJSONArray("inputs") ?: JSONArray()
                for (j in 0 until inputsJson.length()) {
                    val v = inputsJson.getJSONObject(j)
                    inputs.add(InputMaterial(v.optString("id"), v.optString("name"), v.optString("imageUri"),
                        v.optString("buyLocation"), v.optString("buyMethod", "Buy Now"),
                        optDouble(v, "price"), optDouble(v, "amountPerCraft"), v.optBoolean("useReturnRate", true)))
                }
                val outputs = mutableListOf<OutputProduct>()
                val outputsJson = item.optJSONArray("outputs") ?: JSONArray()
                for (j in 0 until outputsJson.length()) {
                    val v = outputsJson.getJSONObject(j)
                    outputs.add(OutputProduct(v.optString("id"), v.optString("name"), v.optString("imageUri"),
                        v.optString("sellLocation"), v.optString("sellMethod", "Sell Now"), optDouble(v, "price")))
                }
                data.seasons.add(Season(item.optString("id"), item.optString("name"),
                    item.optString("panelName"), item.optString("description"), settings, inputs, outputs,
                    item.optLong("lastEdited", System.currentTimeMillis()), item.optBoolean("pinned", false)))
            }
            val ids = root.optJSONArray("openSeasonIds") ?: JSONArray()
            for (i in 0 until ids.length()) data.openSeasonIds.add(ids.optString(i))
            data.selectedSeasonId = if (root.has("selectedSeasonId")) root.optString("selectedSeasonId") else null
            data.splashEnabled = root.optBoolean("splashEnabled", true)
            data.builtinKeypad = root.optBoolean("builtinKeypad", true)
            data.autoSave = root.optBoolean("autoSave", true)
            data.forceKeepAlive = root.optBoolean("forceKeepAlive", false)
            val quickIds = root.optJSONArray("quickSettingsIds") ?: JSONArray()
            for (i in 0 until quickIds.length()) data.quickSettingsIds.add(quickIds.optString(i))
            data
        }.getOrDefault(AppData())
    }

    private fun optDouble(json: JSONObject, key: String): Double? =
        if (json.has(key) && !json.isNull(key)) json.optDouble(key, Double.NaN).takeIf { it.isFinite() } else null
}
