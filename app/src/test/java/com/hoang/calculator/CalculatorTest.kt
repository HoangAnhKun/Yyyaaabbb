package com.hoang.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {
    @Test
    fun returnRateSolverExample() {
        val result = Calculator.calculateReturnRate(183.0, 367)
        assertEquals(119.0, result.first, 0.0001)
        assertEquals(0.0, result.second, 0.0001)
    }

    @Test
    fun nutritionFeeFormula() {
        val result = Calculator.calculate(
            CraftSettings(costFeePer100 = 100.0, finalOutputValue = 10.0,
                totalWillMade = 50.0, outputPerCraft = 1.0),
            emptyList(),
            emptyList()
        )
        assertEquals(56.25, result.nutritionFee ?: 0.0, 0.0001)
    }

    @Test
    fun noReturnRateUsesFullDemand() {
        val result = Calculator.calculate(
            CraftSettings(totalWillMade = 10.0, outputPerCraft = 1.0, returnRate = 50.0),
            listOf(InputMaterial(amountPerCraft = 5.0, price = 2.0, useReturnRate = false)),
            emptyList()
        )
        assertEquals(50.0, result.inputs.single().purchase, 0.0001)
        assertEquals(0.0, result.inputs.single().leftover, 0.0001)
    }

    @Test
    fun floorAtThirtySixPointSeven() {
        assertEquals(183.0, Calculator.floorAmount(183.5), 0.0001)
    }

    @Test
    fun tenTimesFiftySolver() {
        val result = Calculator.calculateReturnRate(500.0, 367)
        assertEquals(320.0, result.first, 0.0001)
    }

    @Test
    fun leftoverTaxAndSellToggle() {
        val sold = Calculator.calculate(
            CraftSettings(totalWillMade = 3.0, outputPerCraft = 1.0, returnRate = 36.7, sellLeftover = true),
            listOf(InputMaterial(amountPerCraft = 1.0, price = 100.0, useReturnRate = true)),
            emptyList()
        )
        val leftover = sold.leftovers.single()
        assertEquals(leftover.grossIncome!! * 0.08, leftover.salesTax!!, 0.0001)

        val off = Calculator.calculate(
            CraftSettings(totalWillMade = 3.0, outputPerCraft = 1.0, returnRate = 36.7, sellLeftover = false),
            listOf(InputMaterial(amountPerCraft = 1.0, price = 100.0, useReturnRate = true)),
            emptyList()
        )
        assertNull(off.leftovers.single().grossIncome)
    }

    @Test
    fun premiumUsesFourPercentTax() {
        val result = Calculator.calculate(
            CraftSettings(totalWillMade = 10.0, premiumActive = true),
            emptyList(),
            listOf(OutputProduct(price = 100.0, sellMethod = "Sell Now"))
        )
        assertEquals(1000.0 * 0.04, result.outputs.single().salesTax!!, 0.0001)
        val standard = Calculator.calculate(
            CraftSettings(totalWillMade = 10.0, premiumActive = false),
            emptyList(),
            listOf(OutputProduct(price = 100.0, sellMethod = "Sell Now"))
        )
        assertEquals(1000.0 * 0.08, standard.outputs.single().salesTax!!, 0.0001)
    }

    @Test
    fun setupFeeOnlyForOrders() {
        val input = Calculator.calculate(
            CraftSettings(totalWillMade = 10.0),
            listOf(InputMaterial(amountPerCraft = 1.0, price = 100.0, buyMethod = "Buy Order", useReturnRate = false)),
            listOf(OutputProduct(price = 100.0, sellMethod = "Sell Order"))
        )
        assertEquals(25.0, input.inputs.single().setupFee!!, 0.0001)
        assertEquals(25.0, input.outputs.single().setupFee!!, 0.0001)

        val now = Calculator.calculate(
            CraftSettings(totalWillMade = 10.0),
            listOf(InputMaterial(amountPerCraft = 1.0, price = 100.0, buyMethod = "Buy Now", useReturnRate = false)),
            listOf(OutputProduct(price = 100.0, sellMethod = "Sell Now"))
        )
        assertEquals(0.0, now.inputs.single().setupFee!!, 0.0001)
        assertEquals(0.0, now.outputs.single().setupFee!!, 0.0001)
    }

    @Test
    fun missingValuesRemainDash() {
        assertEquals("—", formatCompact(null))
        assertEquals("—", formatCompact(Double.NaN))
    }
}
