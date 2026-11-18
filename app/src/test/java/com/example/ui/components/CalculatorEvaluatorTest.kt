package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEvaluatorTest {

    @Test
    fun testBasicArithmetic() {
        assertEquals(190.0, CalculatorEvaluator.evaluate("120 + 35 × 2"), 0.001)
        assertEquals(225.0, CalculatorEvaluator.evaluate("(500 - 50) ÷ 2"), 0.001)
        assertEquals(1500.5, CalculatorEvaluator.evaluate("1000.5 + 500"), 0.001)
        assertEquals(250.0, CalculatorEvaluator.evaluate("250 + "), 0.001)
    }

    @Test
    fun testPercentageCalculations() {
        // Requirements: 200 + 10% = 220, 200 × 10% = 20
        assertEquals(220.0, CalculatorEvaluator.evaluate("200 + 10%"), 0.001)
        assertEquals(20.0, CalculatorEvaluator.evaluate("200 × 10%"), 0.001)
        assertEquals(20.0, CalculatorEvaluator.evaluate("200 * 10%"), 0.001)

        // Subtraction and division percentage behavior
        assertEquals(180.0, CalculatorEvaluator.evaluate("200 − 10%"), 0.001)
        assertEquals(180.0, CalculatorEvaluator.evaluate("200 - 10%"), 0.001)
        assertEquals(2000.0, CalculatorEvaluator.evaluate("200 ÷ 10%"), 0.001)
        assertEquals(2000.0, CalculatorEvaluator.evaluate("200 / 10%"), 0.001)

        // Standalone and chained percentages
        assertEquals(0.1, CalculatorEvaluator.evaluate("10%"), 0.001)
        assertEquals(231.0, CalculatorEvaluator.evaluate("200 + 10% + 5%"), 0.001)
        assertEquals(170.0, CalculatorEvaluator.evaluate("200 - 15%"), 0.001)
        assertEquals(5.0, CalculatorEvaluator.evaluate("10% * 50"), 0.001)

        // Parentheses with percentages
        assertEquals(220.0, CalculatorEvaluator.evaluate("(100 + 100) + 10%"), 0.001)
        assertEquals(78.0, CalculatorEvaluator.evaluate("100 - (20 + 10%)"), 0.001)
        assertEquals(440.0, CalculatorEvaluator.evaluate("(200 + 10%) * 2"), 0.001)

        // Safe division by zero
        assertEquals(0.0, CalculatorEvaluator.evaluate("200 ÷ 0%"), 0.001)

        // Safe handling of repeated operators and invalid expressions without crashing
        assertEquals(200.0, CalculatorEvaluator.evaluate("200 + %"), 0.001)
        assertEquals(0.0, CalculatorEvaluator.evaluate("%%%"), 0.001)
        assertEquals(0.1, CalculatorEvaluator.evaluate("10%%"), 0.001)
        assertEquals(220.0, CalculatorEvaluator.evaluate("200 + 10% + "), 0.001)
        assertEquals(0.0, CalculatorEvaluator.evaluate(""), 0.001)
    }
}
