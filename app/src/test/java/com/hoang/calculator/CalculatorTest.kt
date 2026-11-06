package com.hoang.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorTest {
    @Test
    fun solverExampleIsSmallestPurchase() {
        val rate = BigDecimal("0.367000000000")
        val (purchase, leftover) = Calculator.calculateReturnRate(183.0, rate)
        assertEquals(119.0, purchase, 0.0001)
        assertEquals(0.0, leftover, 0.0001)
        for (b in 0 until purchase.toInt()) {
            var stock = b.toLong()
            var used = 0L
            while (used < 183 && stock > 0) {
                val u = minOf(stock, 183 - used)
                used += u
                stock -= u
                stock += Math.floor(u.toDouble() * 367 / 1000).toLong()
            }
            assertTrue(used < 183)
        }
    }

    @Test
    fun tenPerCraftTimesFifty() {
        val result = Calculator.calculateReturnRate(500.0, BigDecimal("0.367000000000"))
        assertEquals(320.0, result.first, 0.0001)
    }

    @Test
    fun premiumChangesTax() {
        val standard = Calculator.calculate(Season(totalWillMade = 10.0, returnRate = "0", premiumActive = false, outputs = listOf(OutputProduct(price = 100.0))))
        val premium = Calculator.calculate(Season(totalWillMade = 10.0, returnRate = "0", premiumActive = true, outputs = listOf(OutputProduct(price = 100.0))))
        assertEquals(80.0, standard.outputs.single().salesTax!!, 0.0001)
        assertEquals(40.0, premium.outputs.single().salesTax!!, 0.0001)
    }

    @Test
    fun setupFeeOnlyOnOrders() {
        val now = Calculator.calculate(Season(totalWillMade = 10.0, inputs = listOf(InputMaterial(price = 100.0, amountPerCraft = 1.0, useReturnRate = false, buyMethod = "Buy Now")), outputs = listOf(OutputProduct(price = 100.0, sellMethod = "Sell Now"))))
        val order = Calculator.calculate(Season(totalWillMade = 10.0, inputs = listOf(InputMaterial(price = 100.0, amountPerCraft = 1.0, useReturnRate = false, buyMethod = "Buy Order")), outputs = listOf(OutputProduct(price = 100.0, sellMethod = "Sell Order"))))
        assertEquals(0.0, now.inputs.single().setupFee!!, 0.0001)
        assertEquals(0.0, now.outputs.single().setupFee!!, 0.0001)
        assertEquals(25.0, order.inputs.single().setupFee!!, 0.0001)
        assertEquals(25.0, order.outputs.single().setupFee!!, 0.0001)
    }

    @Test
    fun sellLeftoverToggleControlsIncomeAndDash() {
        val on = Calculator.calculate(Season(totalWillMade = 3.0, returnRate = "36.7", sellLeftover = true, inputs = listOf(InputMaterial(price = 100.0, amountPerCraft = 1.0, useReturnRate = true))))
        val off = Calculator.calculate(Season(totalWillMade = 3.0, returnRate = "36.7", sellLeftover = false, inputs = listOf(InputMaterial(price = 100.0, amountPerCraft = 1.0, useReturnRate = true))))
        assertTrue(on.leftovers.single().afterFees != null)
        assertEquals("—", Calculator.formatCompact(off.leftovers.single().grossIncome))
        assertNull(off.leftovers.single().grossIncome)
    }

    @Test
    fun nutritionFeeFormula() {
        val result = Calculator.calculate(Season(costFeePer100 = 100.0, finalOutputValue = 10.0, totalWillMade = 50.0, outputPerCraft = 1.0))
        assertEquals(56.25, result.nutritionFee!!, 0.0001)
    }

    @Test
    fun numberFormatterModesNeverAndPresets() {
        assertEquals("—", Calculator.formatCompact(null, "Never"))
        assertEquals("0", Calculator.formatCompact(0.0, "Never", 2))
        assertEquals("999", Calculator.formatCompact(999.0, "Never", 2))
        assertEquals("15,000", Calculator.formatCompact(15000.0, "Never", 0))
        assertEquals("1,234,567", Calculator.formatCompact(1234567.0, "Never", 0))
        assertEquals("100,000,000", Calculator.formatCompact(100000000.0, "Never", 0))
        assertEquals("-1,234", Calculator.formatCompact(-1234.0, "Never", 0))
        assertEquals("15k", Calculator.formatCompact(15000.0, "Always", 2))
        assertEquals("15k", Calculator.formatCompact(15000.0, "Click to switch", 0))
        assertEquals("-1k", Calculator.formatCompact(-1000.0, "Always", 2))
        assertEquals("999", Calculator.formatCompact(999.0, "Always", 2))
        assertEquals("100M", Calculator.formatCompact(100000000.0, "Always", 0))
        assertEquals("999", Calculator.formatCompact(999.0, "Always", 0))
        assertEquals("999.95k", Calculator.formatCompact(999950.0, "Always", 2))
    }

    @Test
    fun exactConsumedRateFloor() {
        assertEquals(183L, Calculator.floorConsumedRate(500L, BigDecimal("0.367000000000")))
    }

    @Test
    fun returnRateDigitsUseIntegerAndDecimals() {
        assertEquals(10.372, Calculator.parseReturnRateDigits("10372"), 0.0001)
        assertEquals(83.7473883, Calculator.parseReturnRateDigits("837473883"), 0.0000001)
    }

    @Test
    fun missingValuesRemainDash() {
        assertEquals("—", Calculator.formatCompact(null))
        assertEquals("—", Calculator.formatCompact(Double.NaN))
    }

    @Test
    fun noReturnRateUsesDemand() {
        val result = Calculator.calculate(Season(totalWillMade = 10.0, outputs = emptyList(), inputs = listOf(InputMaterial(price = 2.0, amountPerCraft = 5.0, useReturnRate = false))))
        assertEquals(50.0, result.inputs.single().purchase, 0.0001)
        assertEquals(0.0, result.inputs.single().leftover, 0.0001)
    }

    @Test
    fun returnRateFormatterAndBounds() {
        assertEquals("", Calculator.formatReturnRateDigits(""))
        assertEquals("10.372", Calculator.formatReturnRateDigits("10372"))
        assertEquals(99.99999999, Calculator.parseReturnRateDigits("9999999999"), 0.00000001)
        assertEquals(10.0, Calculator.parseReturnRateDigits(""), 0.0001)
    }

    @Test
    fun settingsNormalizationKeepsValidValuesAndRejectsInvalidEnums() {
        val settings = AppSettings(
            themePreset = "unknown",
            uiDensity = "unknown",
            numberRounding = "unknown",
            cornerRadius = 99f,
            fontSize = 5f,
            cardOpacity = 2f,
            animationSpeed = 0f,
            decimalPrecision = 99,
            videoSound = 2f,
            lastBuyLocation = "unknown",
            lastBuyMethod = "unknown"
        ).normalized()
        assertEquals("Dark", settings.themePreset)
        assertEquals("Comfortable", settings.uiDensity)
        assertEquals("Always", settings.numberRounding)
        assertEquals(32f, settings.cornerRadius, 0.001f)
        assertEquals(12f, settings.fontSize, 0.001f)
        assertEquals(1f, settings.cardOpacity, 0.001f)
        assertEquals(0.5f, settings.animationSpeed, 0.001f)
        assertEquals(6, settings.decimalPrecision)
        assertEquals(1f, settings.videoSound, 0.001f)
        assertEquals("Caerleon", settings.lastBuyLocation)
        assertEquals("Buy Now", settings.lastBuyMethod)
    }
}
