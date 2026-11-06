package com.hoang.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.floor
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class InputResult(val material: InputMaterial, val demand: Double, val purchase: Double, val leftover: Double, val grossCost: Double?, val setupFee: Double?, val afterFees: Double?)
data class LeftoverResult(val material: InputMaterial, val amount: Double, val grossIncome: Double?, val setupFee: Double?, val salesTax: Double?, val afterFees: Double?)
data class OutputResult(val product: OutputProduct, val amount: Double, val grossIncome: Double?, val setupFee: Double?, val salesTax: Double?, val afterFees: Double?)
data class CalculationResult(val crafts: Double?, val nutritionFee: Double?, val inputs: List<InputResult>, val leftovers: List<LeftoverResult>, val outputs: List<OutputResult>, val totalExpenses: Double, val totalIncome: Double, val profit: Double, val roi: Double?, val profitPerItem: Double?, val breakEvenPrice: Double?, val adjustmentTotal: Double)

object Calculator {
    fun floorConsumedRate(consumed: Long, scaledRate12: BigDecimal): Long {
        val consumedExact = BigDecimal.valueOf(consumed)
        val rateExact = scaledRate12.setScale(12, RoundingMode.DOWN)
        return consumedExact.multiply(rateExact).setScale(0, RoundingMode.FLOOR).longValueExact()
    }

    fun calculateReturnRate(demand: Double, scaledRate12: BigDecimal): Pair<Double, Double> {
        if (!demand.isFinite() || demand <= 0) return 0.0 to 0.0
        val d = floor(demand).toLong()
        if (d <= 0) return 0.0 to 0.0
        var low = 0L
        var high = d
        while (low < high) {
            val mid = low + (high - low) / 2
            if (simulate(mid, d, scaledRate12).success) high = mid else low = mid + 1
        }
        val result = simulate(low, d, scaledRate12)
        return low.toDouble() to result.stock.toDouble()
    }

    private fun simulate(start: Long, demand: Long, scaledRate12: BigDecimal): SimulatedStock {
        var stock = start
        var used = 0L
        while (used < demand && stock > 0) {
            val u = minOf(stock, demand - used)
            used += u
            stock -= u
            val gained = floorConsumedRate(u, scaledRate12)
            stock += gained
        }
        return SimulatedStock(used >= demand, stock)
    }

    private data class SimulatedStock(val success: Boolean, val stock: Long)

    fun calculate(season: Season): CalculationResult {
        val total = season.totalWillMade
        val perCraft = season.outputPerCraft
        val crafts = if (total != null && total.isFinite() && perCraft.isFinite() && perCraft > 0) ceil(total / perCraft) else null
        val nutrition = if (season.finalOutputValue != null && total != null && season.costFeePer100 != null && season.finalOutputValue.isFinite() && total.isFinite() && season.costFeePer100.isFinite()) season.finalOutputValue * total * season.costFeePer100 * 0.001125 else null
        val scaledRate12 = runCatching {
            BigDecimal(season.returnRate.ifBlank { "0" }).divide(BigDecimal(100), 12, RoundingMode.DOWN)
        }.getOrDefault(BigDecimal.ZERO)
        val inputs = season.inputs.map { material ->
            val demand = if (crafts != null && material.amountPerCraft != null && material.amountPerCraft.isFinite()) crafts * material.amountPerCraft else Double.NaN
            val solved = if (material.useReturnRate && demand.isFinite()) calculateReturnRate(demand, scaledRate12) else (if (demand.isFinite()) demand else 0.0) to 0.0
            val purchase = if (demand.isFinite()) solved.first else Double.NaN
            val gross = if (purchase.isFinite() && material.price != null) purchase * material.price else null
            val setup = if (gross != null && material.buyMethod == "Buy Order") gross * 0.025 else gross?.let { 0.0 }
            val after = if (gross != null && setup != null) gross + setup else null
            InputResult(material, if (demand.isFinite()) demand else Double.NaN, purchase, solved.second, gross, setup, after)
        }
        val taxRate = if (season.premiumActive) 0.04 else 0.08
        val leftovers = inputs.map { result ->
            val gross = if (season.sellLeftover && result.leftover > 0 && result.material.price != null) result.leftover * result.material.price else null
            val setup = gross?.let { 0.0 }
            val tax = gross?.let { it * taxRate }
            val after = if (gross != null && tax != null) gross - tax else null
            LeftoverResult(result.material, result.leftover, gross, setup, tax, after)
        }
        val amount = if (total != null && total.isFinite()) total else Double.NaN
        val outputs = season.outputs.map { product ->
            val gross = if (amount.isFinite() && product.price != null) amount * product.price else null
            val setup = if (gross != null && product.sellMethod == "Sell Order") gross * 0.025 else gross?.let { 0.0 }
            val tax = gross?.let { it * taxRate }
            val after = if (gross != null && setup != null && tax != null) gross - setup - tax else null
            OutputResult(product, if (amount.isFinite()) amount else Double.NaN, gross, setup, tax, after)
        }
        val adjustmentTotal = (nutrition ?: 0.0) + season.craftFee + season.adjustments.mapNotNull { it.amount }.sum()
        val totalExpenses = inputs.mapNotNull { it.afterFees }.sum() + adjustmentTotal
        val totalIncome = outputs.mapNotNull { it.afterFees }.sum() + leftovers.mapNotNull { it.afterFees }.sum()
        val profit = totalIncome - totalExpenses
        val roi = if (totalExpenses != 0.0) profit / totalExpenses * 100.0 else null
        val itemCount = if (total != null && total > 0) total else null
        return CalculationResult(crafts, nutrition, inputs, leftovers, outputs, totalExpenses, totalIncome, profit, roi, itemCount?.let { profit / it }, itemCount?.let { totalExpenses / it }, adjustmentTotal)
    }

    fun formatCompact(value: Double?, rounding: String = "Always", precision: Int = 2): String {
        if (value == null || !value.isFinite()) return "—"
        val sign = if (value < 0) "-" else ""
        val absolute = kotlin.math.abs(value)
        val safeRounding = when (rounding) { "Always", "Never", "Click to switch" -> rounding; else -> "Always" }
        if (safeRounding == "Never") {
            val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ','; decimalSeparator = '.' }
            val pattern = "#,##0" + if (precision > 0) "." + "#".repeat(precision) else ""
            val formatter = DecimalFormat(pattern, symbols).apply { isGroupingUsed = true }
            return sign + formatter.format(BigDecimal.valueOf(absolute).setScale(precision.coerceIn(0, 6), RoundingMode.HALF_UP))
        }
        val text = when {
            safeRounding != "Never" && absolute >= 1_000_000 -> {
                val scaled = BigDecimal.valueOf(absolute / 1_000_000).setScale(precision.coerceIn(0, 6), RoundingMode.HALF_UP)
                "${scaled.stripTrailingZeros().toPlainString()}M"
            }
            safeRounding != "Never" && absolute >= 1_000 -> {
                val scaled = BigDecimal.valueOf(absolute / 1_000).setScale(precision.coerceIn(0, 6), RoundingMode.HALF_UP)
                if (scaled >= BigDecimal(1000)) {
                    val million = BigDecimal.valueOf(absolute / 1_000_000).setScale(precision.coerceIn(0, 6), RoundingMode.HALF_UP)
                    "${million.stripTrailingZeros().toPlainString()}M"
                } else {
                    "${scaled.stripTrailingZeros().toPlainString()}k"
                }
            }
            absolute >= 100 -> BigDecimal.valueOf(absolute).setScale(precision.coerceIn(0, 6), RoundingMode.HALF_UP).let {
                if (precision == 0) it.toPlainString() else it.stripTrailingZeros().toPlainString()
            }
            else -> String.format(Locale.US, "%.${precision.coerceIn(0, 6)}f", absolute).trimEnd('0').trimEnd('.')
        }
        return sign + text
    }

    fun parseReturnRateDigits(digits: String): Double {
        val normalized = digits.filter(Char::isDigit)
        if (normalized.isEmpty()) return 10.0
        val clamped = normalized.take(10)
        val raw = when {
            clamped.length <= 2 -> clamped.padEnd(2, '0')
            else -> clamped
        }
        val integer = raw.take(2)
        val integerValue = integer.toIntOrNull()?.coerceIn(10, 99) ?: 10
        val decimal = raw.drop(2).takeWhile { it.isDigit() }
        val parsed = BigDecimal(integerValue.toString()).add(
            if (decimal.isEmpty()) BigDecimal.ZERO else BigDecimal("0.$decimal")
        ).setScale(decimal.length.coerceAtLeast(1), RoundingMode.DOWN).coerceAtLeast(BigDecimal("10.0")).coerceAtMost(BigDecimal("99.99999999"))
        return parsed.toDouble()
    }

    fun formatReturnRateDigits(digits: String): String {
        val normalized = digits.filter(Char::isDigit)
        if (normalized.isEmpty()) return ""
        val clamped = normalized.take(10)
        val padded = clamped.padEnd(2, '0')
        val integer = padded.take(2).toIntOrNull()?.coerceIn(10, 99) ?: 10
        val integerDigits = integer.toString().padStart(2, '0')
        val decimal = padded.drop(2)
        return if (decimal.isEmpty()) integerDigits else "$integerDigits.$decimal"
    }

    fun parseNumber(text: String): Double? {
        val cleaned = text.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return if (value.isFinite()) value else null
    }
}
