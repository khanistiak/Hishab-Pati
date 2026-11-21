package com.example.ui.screens.plan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class SunburstSubSlice(
    val label: String,
    val amount: Double,
    val sweepAngle: Float,
    val color: Color,
    val isRemainingHeadroom: Boolean = false,
    val isOverrun: Boolean = false
)

private data class SunburstCategorySlice(
    val key: String,
    val label: String,
    val spent: Double,
    val limit: Double,
    val hasLimit: Boolean,
    val percentage: Float,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float,
    val outerSlices: List<SunburstSubSlice>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSunburstCard(
    activeBudgetModels: List<CategoryBudgetUiModel>,
    unbudgetedWithSpend: List<CategoryBudgetUiModel>,
    monthExpenses: List<TransactionEntity>,
    totalMonthSpent: Double,
    totalBudgetLimit: Double,
    currencySymbol: String,
    language: AppLanguage,
    cardColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    budgetedSpent: Double = totalMonthSpent
) {
    val isBn = language == AppLanguage.BANGLA

    val slices = remember(activeBudgetModels, unbudgetedWithSpend, monthExpenses) {
        buildSunburstSlices(
            activeBudgetModels = activeBudgetModels,
            unbudgetedWithSpend = unbudgetedWithSpend,
            monthExpenses = monthExpenses,
            isBn = isBn
        )
    }

    var selectedKey by remember(slices) { mutableStateOf<String?>(null) }
    var selectedSubLabel by remember(slices) { mutableStateOf<String?>(null) }
    var selectedSubAmount by remember(slices) { mutableStateOf<Double?>(null) }

    val selectedSlice = remember(slices, selectedKey) {
        slices.firstOrNull { it.key == selectedKey }
    }

    val sweepProgress by animateFloatAsState(
        targetValue = if (slices.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "sunburst_sweep"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        shadowElevation = 1.5.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_sunburst_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.t("spending_distribution", language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.t("sunburst_subtitle", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            fontSize = 11.5.sp
                        )
                    )
                }

                if (selectedSlice != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4F46E5).copy(alpha = 0.14f),
                        onClick = {
                            selectedKey = null
                            selectedSubLabel = null
                            selectedSubAmount = null
                        }
                    ) {
                        Text(
                            text = AppStrings.t("all", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.t("sunburst_empty", language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Interactive 2-Ring Sunburst Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(236.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val dividerColor = cardColor

                    Canvas(
                        modifier = Modifier
                            .size(224.dp)
                            .testTag("budget_sunburst_canvas")
                            .pointerInput(slices) {
                                detectTapGestures { offset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val dx = offset.x - cx
                                    val dy = offset.y - cy
                                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                    val maxR = minOf(cx, cy)

                                    val innerStroke = maxR * 0.24f
                                    val outerStroke = maxR * 0.22f
                                    val innerRadius = maxR * 0.55f
                                    val outerRadius = maxR * 0.84f

                                    // Center hub tap resets selection
                                    if (dist < innerRadius - innerStroke / 2f) {
                                        selectedKey = null
                                        selectedSubLabel = null
                                        selectedSubAmount = null
                                        return@detectTapGestures
                                    }

                                    // Normalize angle to match -90f startAngle clockwise in [0, 360)
                                    var deg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
                                    deg = (deg + 90f + 360f) % 360f
                                    val targetAngle = -90f + deg

                                    val hitCategory = slices.firstOrNull { s ->
                                        targetAngle >= s.startAngle && targetAngle <= (s.startAngle + s.sweepAngle)
                                    }
                                    if (hitCategory != null) {
                                        val tappedOuter = dist >= (outerRadius - outerStroke / 2f)
                                        if (tappedOuter && hitCategory.outerSlices.isNotEmpty()) {
                                            var cur = hitCategory.startAngle
                                            val hitSub = hitCategory.outerSlices.firstOrNull { sub ->
                                                val end = cur + sub.sweepAngle
                                                val inside = targetAngle in cur..end
                                                cur = end
                                                inside
                                            }
                                            selectedKey = hitCategory.key
                                            if (hitSub != null && !hitSub.isRemainingHeadroom) {
                                                selectedSubLabel = hitSub.label
                                                selectedSubAmount = hitSub.amount
                                            } else {
                                                selectedSubLabel = null
                                                selectedSubAmount = null
                                            }
                                        } else {
                                            if (selectedKey == hitCategory.key && selectedSubLabel == null) {
                                                selectedKey = null
                                            } else {
                                                selectedKey = hitCategory.key
                                            }
                                            selectedSubLabel = null
                                            selectedSubAmount = null
                                        }
                                    }
                                }
                            }
                    ) {
                        val minDim = minOf(size.width, size.height)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxR = minDim / 2f

                        val innerStroke = maxR * 0.24f
                        val outerStroke = maxR * 0.22f
                        val innerRadius = maxR * 0.55f
                        val outerRadius = maxR * 0.84f

                        val innerTopLeft = Offset(cx - innerRadius, cy - innerRadius)
                        val innerSize = Size(innerRadius * 2f, innerRadius * 2f)
                        val outerTopLeft = Offset(cx - outerRadius, cy - outerRadius)
                        val outerSize = Size(outerRadius * 2f, outerRadius * 2f)

                        // Subtle background rings
                        drawArc(
                            color = trackBg,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = innerTopLeft,
                            size = innerSize,
                            style = Stroke(width = innerStroke)
                        )
                        drawArc(
                            color = trackBg.copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = outerTopLeft,
                            size = outerSize,
                            style = Stroke(width = outerStroke)
                        )

                        val hasSelection = selectedSlice != null
                        val gapDeg = if (slices.size > 1) 1.8f else 0f

                        slices.forEach { slice ->
                            val isSelected = selectedSlice?.key == slice.key
                            val dimAlpha = if (hasSelection && !isSelected) 0.30f else 1f
                            val effectiveSweep = ((slice.sweepAngle - gapDeg) * sweepProgress).coerceAtLeast(0.8f)

                            // Inner ring: Category budget allocation / total weight
                            drawArc(
                                color = slice.color.copy(alpha = if (isSelected) 1f else 0.88f * dimAlpha),
                                startAngle = slice.startAngle,
                                sweepAngle = effectiveSweep,
                                useCenter = false,
                                topLeft = innerTopLeft,
                                size = innerSize,
                                style = Stroke(
                                    width = if (isSelected) innerStroke * 1.10f else innerStroke,
                                    cap = StrokeCap.Butt
                                )
                            )

                            // Outer ring: Spending distribution relative to limit (spent/subcategories vs headroom vs overrun)
                            var curStart = slice.startAngle
                            val scaleFactor = if (slice.sweepAngle > 0f) effectiveSweep / slice.sweepAngle else 1f
                            slice.outerSlices.forEach { sub ->
                                val subSweep = (sub.sweepAngle * scaleFactor).coerceAtLeast(0.4f)
                                val subAlpha = when {
                                    sub.isRemainingHeadroom -> if (hasSelection && !isSelected) 0.08f else 0.18f
                                    else -> dimAlpha
                                }
                                drawArc(
                                    color = sub.color.copy(alpha = subAlpha),
                                    startAngle = curStart,
                                    sweepAngle = (subSweep - 0.5f).coerceAtLeast(0.4f),
                                    useCenter = false,
                                    topLeft = outerTopLeft,
                                    size = outerSize,
                                    style = Stroke(
                                        width = if (isSelected && !sub.isRemainingHeadroom) outerStroke * 1.12f else outerStroke,
                                        cap = StrokeCap.Butt
                                    )
                                )
                                curStart += subSweep
                            }

                            // Crisp separator tick between categories on both rings
                            if (slices.size > 1) {
                                drawArc(
                                    color = dividerColor,
                                    startAngle = slice.startAngle + effectiveSweep,
                                    sweepAngle = gapDeg.coerceAtLeast(0.8f),
                                    useCenter = false,
                                    topLeft = outerTopLeft,
                                    size = outerSize,
                                    style = Stroke(width = outerStroke + innerStroke)
                                )
                            }
                        }
                    }

                    // Center Readout Hub
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .size(106.dp)
                            .clip(CircleShape)
                            .clickable {
                                selectedKey = null
                                selectedSubLabel = null
                                selectedSubAmount = null
                            }
                            .padding(horizontal = 6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedSlice != null) {
                            Text(
                                text = selectedSubLabel ?: selectedSlice.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.format(
                                    selectedSubAmount ?: selectedSlice.spent,
                                    currencySymbol,
                                    language = language
                                ),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedSlice.hasLimit && selectedSlice.percentage >= 100f) {
                                    ExpenseRed
                                } else {
                                    selectedSlice.color
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subCaption = if (selectedSlice.hasLimit) {
                                val pctStr = if (isBn) {
                                    CurrencyFormatter.convertToBanglaDigits(selectedSlice.percentage.roundToInt().toString())
                                } else {
                                    selectedSlice.percentage.roundToInt().toString()
                                }
                                "$pctStr% ${AppStrings.t("of", language)} ${CurrencyFormatter.format(selectedSlice.limit, currencySymbol, language = language)}"
                            } else {
                                AppStrings.t("no_limit", language)
                            }
                            Text(
                                text = subCaption,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val displaySpent = if (totalBudgetLimit > 0.0) budgetedSpent else totalMonthSpent
                            Text(
                                text = if (totalBudgetLimit > 0.0) {
                                    AppStrings.t("budgeted_spent", language)
                                } else {
                                    AppStrings.t("total_spent", language)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.format(displaySpent, currencySymbol, language = language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (totalBudgetLimit > 0.0) {
                                val overallPct = ((budgetedSpent / totalBudgetLimit) * 100.0).roundToInt()
                                val pctStr = if (isBn) {
                                    CurrencyFormatter.convertToBanglaDigits(overallPct.toString())
                                } else {
                                    overallPct.toString()
                                }
                                Text(
                                    text = "$pctStr% ${AppStrings.t("used_suffix", language)}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        overallPct > 100 -> ExpenseRed
                                        overallPct >= 80 -> Color(0xFFF59E0B)
                                        else -> IncomeGreen
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Category Breakdown Pills (Legend)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.forEach { slice ->
                        val isSelected = selectedKey == slice.key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                slice.color.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) slice.color.copy(alpha = 0.5f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (selectedKey == slice.key) {
                                        selectedKey = null
                                    } else {
                                        selectedKey = slice.key
                                    }
                                    selectedSubLabel = null
                                    selectedSubAmount = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = slice.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                val badgeText = if (slice.hasLimit) {
                                    val p = if (isBn) {
                                        CurrencyFormatter.convertToBanglaDigits(slice.percentage.roundToInt().toString())
                                    } else {
                                        slice.percentage.roundToInt().toString()
                                    }
                                    "$p%"
                                } else {
                                    CurrencyFormatter.format(slice.spent, currencySymbol, language = language)
                                }
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !slice.hasLimit -> MaterialTheme.colorScheme.onSurfaceVariant
                                        slice.percentage >= 100f -> ExpenseRed
                                        slice.percentage >= 80f -> Color(0xFFF59E0B)
                                        else -> slice.color
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildSunburstSlices(
    activeBudgetModels: List<CategoryBudgetUiModel>,
    unbudgetedWithSpend: List<CategoryBudgetUiModel>,
    monthExpenses: List<TransactionEntity>,
    isBn: Boolean
): List<SunburstCategorySlice> {
    data class RawEntry(
        val key: String,
        val label: String,
        val categoryName: String,
        val subcategoryName: String,
        val spent: Double,
        val limit: Double,
        val hasLimit: Boolean,
        val percentage: Float,
        val color: Color,
        val weight: Double
    )

    val entries = mutableListOf<RawEntry>()

    activeBudgetModels.forEach { model ->
        val baseColor = try {
            Color(android.graphics.Color.parseColor(model.categoryColorHex))
        } catch (_: Exception) {
            Color(0xFF0284C7)
        }
        val weight = maxOf(model.limit, model.spent).coerceAtLeast(1.0)
        entries.add(
            RawEntry(
                key = model.uniqueKey,
                label = model.displayName,
                categoryName = model.categoryName,
                subcategoryName = model.subcategoryName,
                spent = model.spent,
                limit = model.limit,
                hasLimit = true,
                percentage = model.percentage,
                color = baseColor,
                weight = weight
            )
        )
    }

    val unbudgetedTotal = unbudgetedWithSpend.sumOf { it.spent }
    if (unbudgetedTotal > 0.001) {
        entries.add(
            RawEntry(
                key = "__unbudgeted__",
                label = if (isBn) "বাজেটবিহীন" else "Unbudgeted",
                categoryName = "",
                subcategoryName = "",
                spent = unbudgetedTotal,
                limit = 0.0,
                hasLimit = false,
                percentage = 0f,
                color = Color(0xFF64748B),
                weight = unbudgetedTotal
            )
        )
    }

    val totalWeight = entries.sumOf { it.weight }
    if (totalWeight <= 0.0 || entries.isEmpty()) return emptyList()

    var currentStartAngle = -90f
    return entries.map { entry ->
        val sweep = ((entry.weight / totalWeight) * 360.0).toFloat().coerceAtLeast(2f)
        val outerSlices = mutableListOf<SunburstSubSlice>()

        if (!entry.hasLimit) {
            // Unbudgeted: show each top unbudgeted category as an outer ring segment
            var remainingSweep = sweep
            val topUnbudgeted = unbudgetedWithSpend.take(4)
            val otherAmt = unbudgetedWithSpend.drop(4).sumOf { it.spent }
            topUnbudgeted.forEachIndexed { idx, uModel ->
                val subSweep = ((uModel.spent / unbudgetedTotal) * sweep).toFloat().coerceAtLeast(0.5f)
                val shade = lerp(entry.color, Color(0xFF94A3B8), (idx * 0.18f).coerceAtMost(0.6f))
                outerSlices.add(
                    SunburstSubSlice(
                        label = uModel.displayName,
                        amount = uModel.spent,
                        sweepAngle = subSweep.coerceAtMost(remainingSweep),
                        color = shade
                    )
                )
                remainingSweep = (remainingSweep - subSweep).coerceAtLeast(0f)
            }
            if (otherAmt > 0.001 && remainingSweep > 0.4f) {
                outerSlices.add(
                    SunburstSubSlice(
                        label = if (isBn) "অন্যান্য" else "Others",
                        amount = otherAmt,
                        sweepAngle = remainingSweep,
                        color = Color(0xFF94A3B8)
                    )
                )
            }
        } else {
            val pool = maxOf(entry.limit, entry.spent).coerceAtLeast(1.0)
            val spentWithinLimit = minOf(entry.spent, entry.limit)
            val overrun = (entry.spent - entry.limit).coerceAtLeast(0.0)
            val headroom = (entry.limit - entry.spent).coerceAtLeast(0.0)

            // Check if this budgeted parent category has subcategory transactions to show in outer ring
            val matchingTxs = monthExpenses.filter { tx ->
                tx.category.equals(entry.categoryName, ignoreCase = true) &&
                    (entry.subcategoryName.isBlank() || tx.subcategory.equals(entry.subcategoryName, ignoreCase = true))
            }

            if (spentWithinLimit > 0.001) {
                val spentSweep = ((spentWithinLimit / pool) * sweep).toFloat()
                val subGroups = matchingTxs
                    .groupBy { it.subcategory.ifBlank { entry.label } }
                    .mapValues { it.value.sumOf { tx -> tx.amount } }
                    .toList()
                    .sortedByDescending { it.second }

                if (subGroups.size > 1 && entry.subcategoryName.isBlank()) {
                    val totalSubSpent = subGroups.sumOf { it.second }.coerceAtLeast(1.0)
                    subGroups.take(4).forEachIndexed { idx, (subName, subAmt) ->
                        val frac = (subAmt / totalSubSpent).toFloat()
                        val subSweep = (spentSweep * frac).coerceAtLeast(0.5f)
                        val tintColor = when {
                            entry.percentage >= 100f -> lerp(ExpenseRed, Color(0xFFFCA5A5), (idx * 0.16f).coerceAtMost(0.5f))
                            entry.percentage >= 80f -> lerp(Color(0xFFF59E0B), entry.color, (idx * 0.2f).coerceAtMost(0.5f))
                            else -> lerp(entry.color, Color.White, (idx * 0.16f).coerceAtMost(0.45f))
                        }
                        outerSlices.add(
                            SunburstSubSlice(
                                label = subName,
                                amount = subAmt,
                                sweepAngle = subSweep,
                                color = tintColor
                            )
                        )
                    }
                    val remainingSubAmt = subGroups.drop(4).sumOf { it.second }
                    if (remainingSubAmt > 0.001) {
                        val remFrac = (remainingSubAmt / totalSubSpent).toFloat()
                        outerSlices.add(
                            SunburstSubSlice(
                                label = if (isBn) "অন্যান্য" else "Other",
                                amount = remainingSubAmt,
                                sweepAngle = (spentSweep * remFrac).coerceAtLeast(0.5f),
                                color = entry.color.copy(alpha = 0.72f)
                            )
                        )
                    }
                } else {
                    val spendColor = when {
                        entry.percentage >= 100f -> ExpenseRed
                        entry.percentage >= 80f -> Color(0xFFF59E0B)
                        else -> entry.color
                    }
                    outerSlices.add(
                        SunburstSubSlice(
                            label = entry.label,
                            amount = spentWithinLimit,
                            sweepAngle = spentSweep,
                            color = spendColor
                        )
                    )
                }
            }

            if (overrun > 0.001) {
                val overSweep = ((overrun / pool) * sweep).toFloat().coerceAtLeast(1.0f)
                outerSlices.add(
                    SunburstSubSlice(
                        label = "${entry.label} (${if (isBn) "বেশি" else "Over"})",
                        amount = overrun,
                        sweepAngle = overSweep,
                        color = Color(0xFFB91C1C),
                        isOverrun = true
                    )
                )
            }

            if (headroom > 0.001) {
                val headroomSweep = ((headroom / pool) * sweep).toFloat().coerceAtLeast(0.5f)
                outerSlices.add(
                    SunburstSubSlice(
                        label = if (isBn) "অবশিষ্ট সীমা" else "Remaining",
                        amount = headroom,
                        sweepAngle = headroomSweep,
                        color = entry.color,
                        isRemainingHeadroom = true
                    )
                )
            }
        }

        val slice = SunburstCategorySlice(
            key = entry.key,
            label = entry.label,
            spent = entry.spent,
            limit = entry.limit,
            hasLimit = entry.hasLimit,
            percentage = entry.percentage,
            color = entry.color,
            startAngle = currentStartAngle,
            sweepAngle = sweep,
            outerSlices = outerSlices
        )
        currentStartAngle += sweep
        slice
    }
}
