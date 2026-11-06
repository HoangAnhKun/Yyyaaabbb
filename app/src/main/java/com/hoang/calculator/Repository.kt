package com.hoang.calculator

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal

data class PersistedState(val settings: AppSettings = AppSettings(), val seasons: List<Season> = emptyList(), val openSeasonIds: List<String> = emptyList(), val selectedSeasonId: String? = null)

class Repository(private val context: Context) {
    private val gson = Gson()
    private val file get() = File(context.filesDir, "hoang_calculator.json")
    private val externalFolder get() = File("/sdcard/Download/1 Hoàng Calculator Folder")

    fun load(): PersistedState = try {
        if (file.exists()) {
            val loaded = gson.fromJson(file.readText(), PersistedState::class.java) ?: PersistedState()
            loaded.copy(
                settings = loaded.settings.normalized(),
                seasons = loaded.seasons.map { it.copy(returnRate = normalizeReturnRate(it.returnRate)) }
            )
        } else PersistedState()
    } catch (_: Exception) {
        PersistedState()
    }

    fun save(state: PersistedState) {
        val json = gson.toJson(state)
        try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) parent.mkdirs()
            file.writeText(json)
        } catch (_: Exception) {}
        try {
            if (externalFolder.exists() || externalFolder.mkdirs()) File(externalFolder, "hoang_calculator.json").writeText(json)
        } catch (_: Exception) {}
    }

    fun export(output: OutputStream) {
        val state = load()
        output.use { it.write(gson.toJson(state).toByteArray()) }
    }

    fun import(input: InputStream): PersistedState = input.use {
        val loaded = gson.fromJson(it.bufferedReader().readText(), PersistedState::class.java) ?: PersistedState()
        loaded.copy(
            settings = loaded.settings.normalized(),
            seasons = loaded.seasons.map { it.copy(returnRate = normalizeReturnRate(it.returnRate)) }
        )
    }

    private fun normalizeReturnRate(value: String): String = runCatching {
        val rate = BigDecimal(value.ifBlank { "0" }).coerceAtLeast(BigDecimal.ZERO).coerceAtMost(BigDecimal("99.99999999"))
        rate.stripTrailingZeros().toPlainString()
    }.getOrDefault("0")
}
