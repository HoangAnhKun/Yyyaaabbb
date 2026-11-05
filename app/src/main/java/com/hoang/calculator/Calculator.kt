package com.hoang.calculator

import kotlin.math.ceil
import kotlin.math.floor

data class InputMaterial(
    val id: String = "",
    val name: String = "",
    val imageUri: String = "",
    val buyLocation: String = "",
    val buyMethod: String = "Buy Now",
    val price: Double? = null,
    val amountPerCraft: Double? = null,
    val useReturnRate: Boolean = true
)

data class OutputProduct(
    val id: String = "",
    val name: String = "",
    val imageUri: String = "",
    val sellLocation: String = "",
    val sellMethod: String = "Sell Now",
    val price: Double? = null
)

data class CraftSettings(
    val costFeePer100: Double? = null,
    val finalOutputValue: Double? = null,
    val returnRate: Double = 0.0,
    val totalWillMade: Double? = null,
    val outputPerCraft: Double = 1.0,
    val premiumActive: Boolean = false,
    val sellLeftover: Boolean = true,
    val craftFee: Double = 0.0
)

data class InputResult(
    val material: InputMaterial,
    val demand: Double,
    val purchase: Double,
    val leftover: Double,
    val grossCost: Double?,
    val setupFee: Double?,
    val afterFees: Double?
)

data class LeftoverResult(
    val material: InputMaterial,
    val amount: Double,
    val grossIncome: Double?,
    val setupFee: Double?,
    val salesTax: Double?,
    val afterFees: Double?
)

data class OutputResult(
    val product: OutputProduct,
    val amount: Double,
    val grossIncome: Double?,
    val setupFee: Double?,
    val salesTax: Double?,
    val afterFees: Double?
)

data class CalculationResult(
    val crafts: Double?,
    val nutritionFee: Double?,
    val inputs: List<InputResult>,
    val leftovers: List<LeftoverResult>,
    val outputs: List<OutputResult>,
    val totalExpenses: Double,
    val totalIncome: Double,
    val profit: Double,
    val roi: Double?,
    val profitPerItem: Double?,
    val breakEvenPrice: Double?
)

object Calculator {
    fun floorAmount(amount: Double): Double =
        if (amount.isFinite()) floor(amount).toDouble() else Double.NaN

    fun calculateReturnRate(
        demand: Double,
        returnRatePercentTenths: Int
    ): Pair<Double, Double> {
        if (!demand.isFinite() || demand <= 0.0) return 0.0 to 0.0
        val demandInt = floor(demand).toLong()
        if (demandInt <= 0L) return 0.0 to 0.0
        var low = 0L
        var high = demandInt
        while (low < high) {
            val mid = low + (high - low) / 2
            val stock = simulate(mid, demandInt, returnRatePercentTenths)
            if (stock.success) high = mid else low = mid + 1
        }
        val result = simulate(low, demandInt, returnRatePercentTenths)
        return low.toDouble() to result.stock.toDouble()
    }

    private fun simulate(start: Long, demand: Long, rateTenths: Int): SimulatedStock {
        var stock = start
        var used = 0L
        while (used < demand && stock > 0) {
            val u = minOf(stock, demand - used)
            used += u
            stock -= u
            stock += floor(u.toDouble() * rateTenths / 1000.0).toLong()
        }
        return SimulatedStock(used >= demand, stock)
    }

    private data class SimulatedStock(val success: Boolean, val stock: Long)

    fun calculate(
        settings: CraftSettings,
        inputs: List<InputMaterial>,
        outputs: List<OutputProduct>
    ): CalculationResult {
        val total = settings.totalWillMade
        val perCraft = settings.outputPerCraft
        val crafts = if (total != null && total.isFinite() && perCraft.isFinite() && perCraft > 0) {
            ceil(total / perCraft)
        } else null

        val nutrition = if (
            settings.finalOutputValue != null &&
            total != null &&
            settings.costFeePer100 != null &&
            settings.finalOutputValue.isFinite() &&
            total.isFinite() &&
            settings.costFeePer100.isFinite()
        ) settings.finalOutputValue * total * settings.costFeePer100 * 0.001125
        else null

        val rateTenths = floor(settings.returnRate * 10.0 + 0.0000001).toInt().coerceAtLeast(0)
        val inputResults = inputs.map { material ->
            val demand = if (crafts != null && material.amountPerCraft != null &&
                crafts.isFinite() && material.amountPerCraft.isFinite()
            ) crafts * material.amountPerCraft else Double.NaN
            val solved = if (material.useReturnRate && demand.isFinite()) {
                calculateReturnRate(demand, rateTenths)
            } else demand to 0.0
            val purchase = if (demand.isNaN()) Double.NaN else solved.first
            val leftover = if (material.useReturnRate && demand.isFinite()) solved.second else 0.0
            val gross = if (purchase.isFinite() && material.price != null) purchase * material.price else null
            val setup = if (gross != null) if (material.buyMethod == "Buy Order") gross * 0.025 else 0.0 else null
            val after = if (gross != null && setup != null) gross + setup else null
            InputResult(material, demand, purchase, leftover, gross, setup, after)
        }

        val taxRate = if (settings.premiumActive) 0.04 else 0.08
        val leftoverResults = inputResults.map { result ->
            val price = if (settings.sellLeftover && result.material.price != null && result.leftover > 0) result.material.price else null
            val gross = if (price != null) result.leftover * price else null
            val setup = if (gross != null) 0.0 else null
            val tax = if (gross != null) gross * taxRate else null
            val after = if (gross != null && tax != null) gross - tax else null
            LeftoverResult(result.material, result.leftover, gross, setup, tax, after)
        }

        val amount = if (total != null && total.isFinite()) total else Double.NaN
        val outputResults = outputs.map { product ->
            val gross = if (amount.isFinite() && product.price != null) amount * product.price else null
            val setup = if (gross != null) if (product.sellMethod == "Sell Order") gross * 0.025 else 0.0 else null
            val tax = if (gross != null) gross * taxRate else null
            val after = if (gross != null && setup != null && tax != null) gross - setup - tax else null
            OutputResult(product, amount, gross, setup, tax, after)
        }

        val inputCost = inputResults.mapNotNull { it.afterFees }.sum()
        val leftoverIncome = leftoverResults.mapNotNull { it.afterFees }.sum()
        val outputIncome = outputResults.mapNotNull { it.afterFees }.sum()
        val adjustments = settings.craftFee + (nutrition ?: 0.0)
        val totalExpenses = inputCost + adjustments
        val totalIncome = outputIncome + leftoverIncome
        val profit = totalIncome - totalExpenses
        val roi = if (totalExpenses != 0.0) profit / totalExpenses * 100.0 else null
        val items = if (total != null && total > 0) total else null
        val profitPerItem = if (items != null) profit / items else null
        val breakEven = if (items != null && items > 0) totalExpenses / items else null
        return CalculationResult(crafts, nutrition, inputResults, leftoverResults, outputResults,
            totalExpenses, totalIncome, profit, roi, profitPerItem, breakEven)
    }
}
