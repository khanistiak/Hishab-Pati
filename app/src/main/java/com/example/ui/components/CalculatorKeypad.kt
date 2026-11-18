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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Modern tactile calculator keypad with raised keys, soft accent operators,
 * full % percentage calculations, and prominent gradient Done action.
 */
@Composable
fun CalculatorKeypad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onDone: (Double) -> Unit,
    onDismiss: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current

    val d = { digit: String ->
        if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(digit) else digit
    }

    // Palette calibrated to match the visual reference screenshot with high contrast and raised buttons
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
    val buttonHeight = if (isCompactHeight) 32.dp else 54.dp
    val buttonSpacing = if (isCompactHeight) 4.dp else 10.dp

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
        shadowElevation = 6.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp)
        ) {
            // Drag handle & Dismiss Chevron
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 10.dp)
            ) {
                // Center pill handle
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                        .align(Alignment.Center)
                )

                // Down arrow to dismiss
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
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
                    onClick = { onExpressionChange("") }
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
                        val openCount = expression.count { it == '(' }
                        val closeCount = expression.count { it == ')' }
                        val lastChar = expression.trimEnd().lastOrNull()

                        val nextParen = if (openCount > closeCount && lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) {
                            ")"
                        } else {
                            if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) " × (" else "("
                        }
                        onExpressionChange(expression + nextParen)
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
                        val trimmed = expression.trimEnd()
                        if (trimmed.isNotEmpty() && (trimmed.last().isDigit() || trimmed.last() == ')')) {
                            onExpressionChange(trimmed + "%")
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
                    onClick = { appendOperator(expression, " ÷ ", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "7", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "8", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "9", onExpressionChange) }
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
                    onClick = { appendOperator(expression, " × ", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "4", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "5", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "6", onExpressionChange) }
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
                    onClick = { appendOperator(expression, " - ", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "1", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "2", onExpressionChange) }
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
                    onClick = { appendDigit(expression, "3", onExpressionChange) }
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
                    onClick = { appendOperator(expression, " + ", onExpressionChange) }
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
                        val trimmed = expression.trimEnd()
                        if (trimmed.isEmpty() || trimmed.last() in "+-×÷(") {
                            onExpressionChange(expression + "0.")
                        } else if (trimmed.endsWith("%") || trimmed.endsWith(")")) {
                            onExpressionChange("$trimmed × 0.")
                        } else {
                            val lastToken = trimmed.split(' ', '+', '-', '×', '÷', '(', ')', '%').lastOrNull() ?: ""
                            if (!lastToken.contains('.')) {
                                onExpressionChange(expression + ".")
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
                    onClick = { appendDigit(expression, "0", onExpressionChange) }
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
                        if (expression.isNotEmpty()) {
                            val trimmed = expression.trimEnd()
                            if (trimmed.length > 1 && trimmed.last() in "+-×÷") {
                                onExpressionChange(trimmed.dropLast(1).trimEnd())
                            } else {
                                onExpressionChange(expression.dropLast(1))
                            }
                        }
                    }
                )

                // ✓ Done (Prominent Gradient Action)
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
                            val result = CalculatorEvaluator.evaluate(expression)
                            onDone(result)
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
