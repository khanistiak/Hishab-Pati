package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.Stack
import com.example.ui.util.AppHaptics
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.LocalHapticsEnabled
import com.example.data.model.AppLanguage

/**
 * Calculator expression evaluator supporting +, -, ×, ÷, %, parentheses, and decimals.
 * Percentage logic conforms to financial calculator standards:
 * - A × B% = A * (B / 100)
 * - A ÷ B% = A / (B / 100)
 * - A + B% = A + (A * B / 100)
 * - A - B% = A - (A * B / 100)
 * - Standalone B% = B / 100
 */
object CalculatorEvaluator {

    fun evaluate(expression: String): Double {
        if (expression.isBlank()) return 0.0

        val normalized = CurrencyFormatter.convertToEnglishDigits(expression)
        val cleanExpr = normalized
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .trim()

        var expr = cleanExpr
        var changed = true
        while (changed && expr.isNotEmpty()) {
            changed = false
            if (expr.last() in "+-*/(") {
                expr = expr.dropLast(1).trim()
                changed = true
            } else if (expr.last() == '%') {
                val beforePercent = expr.dropLast(1).trimEnd()
                if (beforePercent.isEmpty() || beforePercent.last() in "+-*/(") {
                    expr = beforePercent
                    changed = true
                }
            }
        }
        if (expr.isEmpty()) return 0.0

        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        if (openCount > closeCount) {
            expr += ")".repeat(openCount - closeCount)
        }

        return try {
            val tokens = tokenize(expr)
            if (tokens.isEmpty()) return 0.0
            val resolvedTokens = resolvePercentages(tokens)
            evalTokens(resolvedTokens)
        } catch (_: Exception) {
            0.0
        }
    }

    private fun tokenize(expr: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c in "+-*/()%" -> {
                    if (c == '-' && (list.isEmpty() || list.last() in "+-*/(")) {
                        var num = "-"
                        i++
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                            num += expr[i]
                            i++
                        }
                        if (num != "-") {
                            list.add(num)
                        }
                    } else {
                        list.add(c.toString())
                        i++
                    }
                }
                c.isDigit() || c == '.' -> {
                    var num = ""
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        num += expr[i]
                        i++
                    }
                    list.add(num)
                }
                else -> i++
            }
        }
        return list
    }

    /**
     * Resolves postfix percentage operators (%) into concrete values based on preceding arithmetic context.
     * Conforms to financial calculator standards:
     * - A + B% = A + (A * B / 100)
     * - A - B% = A - (A * B / 100)
     * - A * B% = A * (B / 100)
     * - A / B% = A / (B / 100)
     * - Standalone B% = B / 100
     */
    private fun resolvePercentages(tokens: List<String>): List<String> {
        val resolved = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val isNumber = token.toDoubleOrNull() != null || token == ")"
            if (isNumber && i + 1 < tokens.size && tokens[i + 1] == "%") {
                val numVal = token.toDoubleOrNull() ?: 0.0
                if (resolved.size >= 2 && resolved.last() in listOf("+", "-")) {
                    // Scope check for parentheses: if inside an unclosed '(', evaluate base only within that scope
                    val openCount = resolved.count { it == "(" }
                    val closeCount = resolved.count { it == ")" }
                    val baseTokens = if (openCount > closeCount) {
                        val lastOpen = resolved.lastIndexOf("(")
                        resolved.subList(lastOpen + 1, resolved.size)
                    } else {
                        resolved
                    }
                    val baseNum = evalTokens(baseTokens.dropLast(1))
                    val percentAmount = baseNum * (numVal / 100.0)
                    resolved.add(percentAmount.toString())
                } else if (resolved.size >= 1 && resolved.last() in listOf("*", "/")) {
                    val percentAmount = numVal / 100.0
                    resolved.add(percentAmount.toString())
                } else {
                    resolved.add((numVal / 100.0).toString())
                }
                i += 2 // Skip number and '%'
            } else if (token == "%") {
                // Stray '%' with no prior number
                i++
            } else {
                resolved.add(token)
                i++
            }
        }
        return resolved
    }

    private fun evalTokens(toks: List<String>): Double {
        if (toks.isEmpty()) return 0.0

        val values = Stack<Double>()
        val ops = Stack<Char>()

        fun applyOp(): Boolean {
            if (values.size < 2 || ops.isEmpty()) return false
            val op = ops.pop()
            if (op !in "+-*/") return true
            val b = values.pop()
            val a = values.pop()
            when (op) {
                '+' -> values.push(a + b)
                '-' -> values.push(a - b)
                '*' -> values.push(a * b)
                '/' -> values.push(if (b != 0.0) a / b else 0.0)
            }
            return true
        }

        fun precedence(op: Char): Int = if (op == '*' || op == '/') 2 else 1

        for (token in toks) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                values.push(num)
            } else if (token == "(") {
                ops.push('(')
            } else if (token == ")") {
                while (ops.isNotEmpty() && ops.peek() != '(') {
                    if (!applyOp()) {
                        ops.pop()
                    }
                }
                if (ops.isNotEmpty() && ops.peek() == '(') {
                    ops.pop()
                }
            } else if (token.length == 1 && token[0] in "+-*/") {
                val currentOp = token[0]
                while (ops.isNotEmpty() && ops.peek() != '(' && precedence(ops.peek()) >= precedence(currentOp)) {
                    if (!applyOp()) {
                        ops.pop()
                    }
                }
                ops.push(currentOp)
            }
        }

        while (ops.isNotEmpty()) {
            if (ops.peek() == '(') {
                ops.pop()
            } else {
                if (!applyOp()) {
                    ops.pop()
                }
            }
        }

        return if (values.isNotEmpty()) values.pop() else 0.0
    }
}

private class SyncExpressionHolder(var current: String) {
    val dispatchedQueue = ArrayDeque<String>()

    fun syncFromExternal(externalValue: String) {
        val matchIdx = dispatchedQueue.indexOfFirst { it == externalValue }
        if (matchIdx >= 0) {
            repeat(matchIdx + 1) { dispatchedQueue.removeFirst() }
        } else if (externalValue != current) {
            dispatchedQueue.clear()
            current = externalValue
        }
    }

    fun update(next: String, onEmit: (String) -> Unit) {
        current = next
        dispatchedQueue.addLast(next)
        if (dispatchedQueue.size > 32) {
            dispatchedQueue.removeFirst()
        }
        onEmit(next)
    }
}

internal fun formatEvaluatedNumber(value: Double): String {
    if (value <= 0.0) return ""
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

internal fun formatCalculatorDisplayExpression(
    rawExpression: String,
    language: AppLanguage
): String {
    if (rawExpression.isBlank()) {
        return if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits("0") else "0"
    }
    val hasOperator = rawExpression.contains("+") || rawExpression.contains("-") ||
        rawExpression.contains("×") || rawExpression.contains("÷") ||
        rawExpression.contains("%") || rawExpression.contains("(") || rawExpression.contains(")")
    if (!hasOperator) {
        val trimmed = rawExpression.trim()
        val hasTrailingDot = trimmed.endsWith(".")
        val parts = trimmed.split(".")
        val intDigits = parts[0].toLongOrNull()
        val formatted = if (intDigits != null) {
            val groupedInt = CurrencyFormatter.format(intDigits.toDouble(), "", language = AppLanguage.ENGLISH)
            when {
                hasTrailingDot -> "$groupedInt."
                parts.size > 1 -> "$groupedInt.${parts[1]}"
                else -> groupedInt
            }
        } else {
            trimmed
        }
        return if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(formatted) else formatted
    }
    return if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(rawExpression) else rawExpression
}

/**
 * Modern tactile calculator keypad with raised keys, live Amount Display Card,
 * synchronous zero-digit-drop expression state, full % percentage calculations,
 * and prominent Royal Indigo-to-Violet Done action.
 */
@Composable
fun CalculatorKeypad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onDone: (Double) -> Unit,
    onDismiss: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
    currencySymbol: String = "৳",
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current

    val currentOnExpressionChange by rememberUpdatedState(onExpressionChange)
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val exprHolder = remember { SyncExpressionHolder(expression) }
    SideEffect {
        exprHolder.syncFromExternal(expression)
    }

    val emitExpression: (String) -> Unit = remember {
        { next -> exprHolder.update(next) { currentOnExpressionChange(it) } }
    }

    val d = { digit: String ->
        if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(digit) else digit
    }

    // Palette calibrated to match Hishab Pati's Bento & Royal Indigo-Violet design language
    val sheetBg = if (isDark) Color(0xFF131A2A) else Color(0xFFEFF2F8)
    val numberBtnBg = if (isDark) Color(0xFF1E2638) else Color.White
    val numberTextColor = if (isDark) Color.White else Color(0xFF0F172A)

    val operatorBtnBg = if (isDark) Color(0xFF283050) else Color(0xFFE8EEFF)
    val operatorTextColor = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)

    val funcBtnBg = if (isDark) Color(0xFF22293C) else Color(0xFFF1F5F9)
    val funcTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val clearTextColor = Color(0xFFEF4444)

    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 620
    val buttonElevation = 2.dp
    val buttonShape = RoundedCornerShape(if (isCompactHeight) 12.dp else 18.dp)
    val buttonHeight = if (isCompactHeight) 32.dp else 52.dp
    val buttonSpacing = if (isCompactHeight) 4.dp else 9.dp

    val liveValue = remember(expression) { CalculatorEvaluator.evaluate(expression) }
    val hasCalcOp = expression.contains("+") || expression.contains("-") ||
        expression.contains("×") || expression.contains("÷") || expression.contains("%")
    val formattedLiveDisplay = remember(expression, language) {
        formatCalculatorDisplayExpression(expression, language)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calculator_keypad_sheet"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = sheetBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF28324E) else Color(0xFFDCE3F0)
        ),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp)
        ) {
            // Drag handle (36.dp × 4.dp) & Dismiss Chevron
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                        .align(Alignment.Center)
                )

                Surface(
                    onClick = { currentOnDismiss() },
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.CenterEnd)
                        .testTag("calculator_dismiss_button"),
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1E2638) else Color.White,
                    shadowElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Keypad",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dedicated Live Amount Display Card inside the top of the calculator sheet
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF1B2336) else Color.White,
                border = BorderStroke(
                    width = 1.25.dp,
                    color = if (isDark) Color(0xFF4F46E5).copy(alpha = 0.45f) else Color(0xFF4F46E5).copy(alpha = 0.24f)
                ),
                shadowElevation = if (isDark) 0.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompactHeight) 6.dp else 10.dp)
                    .testTag("calculator_live_amount_display")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                fontSize = 22.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedLiveDisplay,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (formattedLiveDisplay.length > 12) 20.sp else 26.sp,
                                color = if (expression.isBlank()) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                } else {
                                    numberTextColor
                                },
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hasCalcOp && liveValue > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4F46E5).copy(alpha = if (isDark) 0.24f else 0.10f)
                        ) {
                            Text(
                                text = "= ${CurrencyFormatter.format(liveValue, currencySymbol, language = language)}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Row 1: C, ( ), %, ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = "C",
                    textColor = clearTextColor,
                    backgroundColor = funcBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_clear",
                    onClick = { emitExpression("") }
                )

                CalcButton(
                    text = "( )",
                    textColor = funcTextColor,
                    backgroundColor = funcBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_paren",
                    onClick = {
                        val curr = exprHolder.current
                        val openCount = curr.count { it == '(' }
                        val closeCount = curr.count { it == ')' }
                        val lastChar = curr.trimEnd().lastOrNull()

                        val nextParen = if (openCount > closeCount && lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) {
                            ")"
                        } else {
                            if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) " × (" else "("
                        }
                        emitExpression(curr + nextParen)
                    }
                )

                // % Percentage Key
                CalcButton(
                    text = "%",
                    textColor = operatorTextColor,
                    backgroundColor = operatorBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_percent",
                    onClick = {
                        val trimmed = exprHolder.current.trimEnd()
                        if (trimmed.isNotEmpty() && (trimmed.last().isDigit() || trimmed.last() == ')')) {
                            emitExpression(trimmed + "%")
                        }
                    }
                )

                // ÷ (Divide)
                CalcButton(
                    text = "÷",
                    textColor = operatorTextColor,
                    backgroundColor = operatorBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_div",
                    onClick = { appendOperator(exprHolder.current, " ÷ ", emitExpression) }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 2: 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = d("7"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_7",
                    onClick = { appendDigit(exprHolder.current, "7", emitExpression) }
                )
                CalcButton(
                    text = d("8"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_8",
                    onClick = { appendDigit(exprHolder.current, "8", emitExpression) }
                )
                CalcButton(
                    text = d("9"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_9",
                    onClick = { appendDigit(exprHolder.current, "9", emitExpression) }
                )
                CalcButton(
                    text = "×",
                    textColor = operatorTextColor,
                    backgroundColor = operatorBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_mul",
                    onClick = { appendOperator(exprHolder.current, " × ", emitExpression) }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 3: 4, 5, 6, −
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = d("4"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_4",
                    onClick = { appendDigit(exprHolder.current, "4", emitExpression) }
                )
                CalcButton(
                    text = d("5"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_5",
                    onClick = { appendDigit(exprHolder.current, "5", emitExpression) }
                )
                CalcButton(
                    text = d("6"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_6",
                    onClick = { appendDigit(exprHolder.current, "6", emitExpression) }
                )
                CalcButton(
                    text = "−",
                    textColor = operatorTextColor,
                    backgroundColor = operatorBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_sub",
                    onClick = { appendOperator(exprHolder.current, " - ", emitExpression) }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 4: 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = d("1"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_1",
                    onClick = { appendDigit(exprHolder.current, "1", emitExpression) }
                )
                CalcButton(
                    text = d("2"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_2",
                    onClick = { appendDigit(exprHolder.current, "2", emitExpression) }
                )
                CalcButton(
                    text = d("3"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_3",
                    onClick = { appendDigit(exprHolder.current, "3", emitExpression) }
                )
                CalcButton(
                    text = "+",
                    textColor = operatorTextColor,
                    backgroundColor = operatorBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_add",
                    onClick = { appendOperator(exprHolder.current, " + ", emitExpression) }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 5: ., 0, ⌫, ✓ Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                // .
                CalcButton(
                    text = ".",
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_dot",
                    onClick = {
                        val curr = exprHolder.current
                        val trimmed = curr.trimEnd()
                        if (trimmed.isEmpty() || trimmed.last() in "+-×÷(") {
                            emitExpression(curr + "0.")
                        } else if (trimmed.endsWith("%") || trimmed.endsWith(")")) {
                            emitExpression("$trimmed × 0.")
                        } else {
                            val lastToken = trimmed.split(' ', '+', '-', '×', '÷', '(', ')', '%').lastOrNull() ?: ""
                            if (!lastToken.contains('.')) {
                                emitExpression(curr + ".")
                            }
                        }
                    }
                )

                // 0
                CalcButton(
                    text = d("0"),
                    textColor = numberTextColor,
                    backgroundColor = numberBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_0",
                    onClick = { appendDigit(exprHolder.current, "0", emitExpression) }
                )

                // ⌫ Backspace
                CalcIconButton(
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    tint = funcTextColor,
                    backgroundColor = funcBtnBg,
                    elevation = buttonElevation,
                    shape = buttonShape,
                    height = buttonHeight,
                    modifier = Modifier.weight(1f),
                    testTag = "calc_btn_backspace",
                    onClick = {
                        val curr = exprHolder.current
                        if (curr.isNotEmpty()) {
                            val trimmed = curr.trimEnd()
                            if (trimmed.length > 1 && trimmed.last() in "+-×÷") {
                                emitExpression(trimmed.dropLast(1).trimEnd())
                            } else {
                                emitExpression(curr.dropLast(1))
                            }
                        }
                    }
                )

                // ✓ Done (Prominent Royal Indigo-to-Violet Gradient Action — locks exact latest expression synchronously)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .shadow(
                            elevation = 4.dp,
                            shape = buttonShape,
                            spotColor = Color(0x554F46E5),
                            ambientColor = Color(0x224F46E5)
                        )
                        .clip(buttonShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF4F46E5),
                                    Color(0xFF7C3AED)
                                )
                            )
                        )
                        .clickable {
                            AppHaptics.performTap(view, hapticsEnabled)
                            val latestExpr = exprHolder.current
                            val result = CalculatorEvaluator.evaluate(latestExpr)
                            val committedStr = formatEvaluatedNumber(result)
                            emitExpression(committedStr)
                            currentOnDone(result)
                        }
                        .testTag("calc_btn_done"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

private fun appendDigit(curr: String, digit: String, onUpdate: (String) -> Unit) {
    val trimmed = curr.trimEnd()
    if (curr == "0" && digit != ".") {
        onUpdate(digit)
    } else if (trimmed.endsWith("%") || trimmed.endsWith(")")) {
        onUpdate("$trimmed × $digit")
    } else {
        onUpdate(curr + digit)
    }
}

private fun appendOperator(curr: String, op: String, onUpdate: (String) -> Unit) {
    if (curr.isBlank()) return
    val trimmed = curr.trimEnd()
    if (trimmed.endsWith("+") || trimmed.endsWith("-") || trimmed.endsWith("×") || trimmed.endsWith("÷")) {
        val base = trimmed.dropLast(1).trimEnd()
        onUpdate(base + op)
    } else {
        onUpdate(trimmed + op)
    }
}

@Composable
private fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color,
    backgroundColor: Color,
    elevation: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(18.dp),
    height: Dp = 54.dp,
    fontSize: TextUnit = 21.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    testTag: String = "",
    onClick: () -> Unit
) {
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Surface(
        onClick = {
            AppHaptics.performTap(view, hapticsEnabled)
            onClick()
        },
        modifier = modifier
            .height(height)
            .testTag(testTag),
        shape = shape,
        color = backgroundColor,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    color = textColor
                )
            )
        }
    }
}

@Composable
private fun CalcIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color,
    backgroundColor: Color,
    elevation: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(18.dp),
    height: Dp = 54.dp,
    testTag: String = "",
    onClick: () -> Unit
) {
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Surface(
        onClick = {
            AppHaptics.performTap(view, hapticsEnabled)
            onClick()
        },
        modifier = modifier
            .height(height)
            .testTag(testTag),
        shape = shape,
        color = backgroundColor,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
