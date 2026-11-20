package com.example.ui.screens.accounts

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class AccountFlowStats(
    val inThisMonth: Double,
    val outThisMonth: Double,
    val last90DaysNet: Double,
    val dailyBalances90d: List<Double>
)

fun computeAccountFlowStats(
    account: AccountEntity,
    accountTxs: List<TransactionEntity>,
    nowMillis: Long = System.currentTimeMillis()
): AccountFlowStats {
    val monthCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val monthStartMillis = monthCal.timeInMillis

    var inThisMonth = 0.0
    var outThisMonth = 0.0
    var last90Net = 0.0

    val todayEndCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val todayEndMillis = todayEndCal.timeInMillis
    val dayMillis = TimeUnit.DAYS.toMillis(1)
    val start90Millis = todayEndMillis - (89L * dayMillis)

    // Group net delta per day index [0..89], where 89 = today, 0 = 89 days ago
    val dailyNet = DoubleArray(90)

    accountTxs.forEach { tx ->
        val incoming = tx.type == TransactionType.INCOME ||
            (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id) ||
            (tx.type == TransactionType.CC_PAYMENT && tx.toAccountId == account.id)
        val outgoing = tx.type == TransactionType.EXPENSE ||
            (tx.type == TransactionType.TRANSFER && tx.accountId == account.id) ||
            (tx.type == TransactionType.CC_PAYMENT && tx.accountId == account.id)

        val signedAmount = when {
            incoming && !outgoing -> tx.amount
            outgoing && !incoming -> -tx.amount
            else -> 0.0
        }

        if (tx.timestamp >= monthStartMillis) {
            if (signedAmount > 0) inThisMonth += signedAmount
            else if (signedAmount < 0) outThisMonth += abs(signedAmount)
        }

        if (tx.timestamp in start90Millis..todayEndMillis) {
            last90Net += signedAmount
            val daysAgo = ((todayEndMillis - tx.timestamp) / dayMillis).toInt().coerceIn(0, 89)
            val dayIndex = 89 - daysAgo
            dailyNet[dayIndex] += signedAmount
        }
    }

    // Reconstruct historical end-of-day balance backwards from current account.balance
    val balances = DoubleArray(90)
    var running = account.balance
    for (dayIndex in 89 downTo 0) {
        balances[dayIndex] = running
        running -= dailyNet[dayIndex]
    }

    return AccountFlowStats(
        inThisMonth = inThisMonth,
        outThisMonth = outThisMonth,
        last90DaysNet = last90Net,
        dailyBalances90d = balances.toList()
    )
}

@Composable
fun AccountQuickActionBar(
    isCreditCard: Boolean,
    language: AppLanguage,
    onAddClick: () -> Unit,
    onTransferClick: () -> Unit,
    onPayBillClick: () -> Unit,
    onStatementClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AccountQuickActionPill(
            icon = Icons.Rounded.Add,
            label = AppStrings.t("quick_add", language),
            onClick = onAddClick,
            modifier = Modifier
                .weight(1f)
                .testTag("account_quick_add_pill"),
            accentColor = MaterialTheme.colorScheme.primary
        )

        if (isCreditCard) {
            AccountQuickActionPill(
                icon = Icons.Rounded.CreditCard,
                label = AppStrings.t("pay_bill", language),
                onClick = onPayBillClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("account_pay_bill_pill"),
                accentColor = MaterialTheme.colorScheme.primary
            )
            AccountQuickActionPill(
                icon = Icons.Rounded.ReceiptLong,
                label = AppStrings.t("statement_label", language),
                onClick = onStatementClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("account_statement_pill"),
                accentColor = MaterialTheme.colorScheme.onSurface
            )
        } else {
            AccountQuickActionPill(
                icon = Icons.Rounded.SwapHoriz,
                label = AppStrings.t("transfer_label", language),
                onClick = onTransferClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("account_quick_transfer_pill"),
                accentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AccountQuickActionPill(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pillBg = if (isDark) Color(0xFF192234) else Color.White

    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = pillBg,
        shadowElevation = if (isDark) 0.dp else 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AccountMonthlyFlowSparklineCard(
    stats: AccountFlowStats,
    currencySymbol: String,
    language: AppLanguage,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF192234) else Color.White
    val emerald = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val coral = if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626)
    val mutedText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)

    val netPrefix = if (stats.last90DaysNet >= 0.0) "+" else "-"
    val netColor = if (stats.last90DaysNet > 0.0) emerald else if (stats.last90DaysNet < 0.0) coral else MaterialTheme.colorScheme.onSurface
    val lineColor = if (stats.last90DaysNet >= 0.0) emerald else accentColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("account_monthly_flow_sparkline_card"),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        shadowElevation = if (isDark) 0.dp else 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 3-Column Summary Row: IN THIS MONTH | OUT THIS MONTH | LAST 90 DAYS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.t("in_this_month", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = mutedText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${CurrencyFormatter.format(stats.inThisMonth, currencySymbol, language = language)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = emerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppStrings.t("out_this_month", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = mutedText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${CurrencyFormatter.format(stats.outThisMonth, currencySymbol, language = language)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = coral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = AppStrings.t("last_90_days", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = mutedText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$netPrefix${CurrencyFormatter.format(abs(stats.last90DaysNet), currencySymbol, language = language)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = netColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smooth Canvas Sparkline Chart (90-day balance progression)
            val points = remember(stats.dailyBalances90d) {
                // Downsample 90 days into 18 smooth checkpoints (every 5 days + final today point)
                val raw = stats.dailyBalances90d
                if (raw.size <= 2) raw
                else {
                    val sampled = mutableListOf<Double>()
                    var i = 0
                    while (i < raw.size - 1) {
                        sampled.add(raw[i])
                        i += 5
                    }
                    sampled.add(raw.last())
                    sampled
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f || points.isEmpty()) return@Canvas

                    val padTop = 8.dp.toPx()
                    val padBottom = 8.dp.toPx()
                    val padEnd = 6.dp.toPx()
                    val chartH = (h - padTop - padBottom).coerceAtLeast(1f)
                    val chartW = (w - padEnd).coerceAtLeast(1f)

                    val minVal = points.minOrNull() ?: 0.0
                    val maxVal = points.maxOrNull() ?: 0.0
                    val span = (maxVal - minVal)

                    val coords = points.mapIndexed { idx, v ->
                        val x = if (points.size == 1) chartW else (idx.toFloat() / (points.size - 1)) * chartW
                        val y = if (span < 0.001) {
                            padTop + chartH * 0.5f
                        } else {
                            val norm = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
                            padTop + chartH * (1f - norm)
                        }
                        Offset(x, y)
                    }

                    val strokePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 0 until coords.size - 1) {
                            val p0 = coords[i]
                            val p1 = coords[i + 1]
                            val midX = (p0.x + p1.x) * 0.5f
                            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(strokePath)
                        lineTo(coords.last().x, h)
                        lineTo(coords.first().x, h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = if (isDark) 0.24f else 0.18f),
                                lineColor.copy(alpha = 0.01f)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    val lastPt = coords.last()
                    drawCircle(
                        color = lineColor.copy(alpha = 0.22f),
                        radius = 7.dp.toPx(),
                        center = lastPt
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 3.5.dp.toPx(),
                        center = lastPt
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = AppStrings.t("ninety_days_ago", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = mutedText,
                        fontSize = 10.5.sp
                    )
                )
                Text(
                    text = AppStrings.t("today_label", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = mutedText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    )
                )
            }
        }
    }
}

@Composable
fun AccountEntriesSummaryRow(
    filteredTxs: List<TransactionEntity>,
    accountId: String,
    currencySymbol: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val emerald = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val coral = if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626)

    val (periodInflow, periodOutflow) = remember(filteredTxs, accountId) {
        var inflow = 0.0
        var outflow = 0.0
        filteredTxs.forEach { tx ->
            val isIncoming = tx.type == TransactionType.INCOME ||
                (tx.type == TransactionType.TRANSFER && tx.toAccountId == accountId) ||
                (tx.type == TransactionType.CC_PAYMENT && tx.toAccountId == accountId)
            if (isIncoming) inflow += tx.amount else outflow += tx.amount
        }
        inflow to outflow
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag("account_entries_summary_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = AppStrings.t("date_details_header", language),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                fontSize = 10.5.sp
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (periodInflow > 0.0 || periodOutflow == 0.0) {
                Text(
                    text = "+${CurrencyFormatter.format(periodInflow, currencySymbol, language = language)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = emerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
            if (periodOutflow > 0.0 || periodInflow == 0.0) {
                Text(
                    text = "-${CurrencyFormatter.format(periodOutflow, currencySymbol, language = language)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = coral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
        }
    }
}

fun shareCreditCardStatement(
    context: Context,
    account: AccountEntity,
    cycle: BillingCycleOption,
    accountTxs: List<TransactionEntity>,
    currencySymbol: String,
    language: AppLanguage
) {
    val isBn = language == AppLanguage.BANGLA
    val recentLines = accountTxs.take(10).joinToString("\n") { tx ->
        val sign = if (tx.type == TransactionType.INCOME ||
            (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id) ||
            (tx.type == TransactionType.CC_PAYMENT && tx.toAccountId == account.id)
        ) "+" else "-"
        val dateStr = DateFormatter.formatDateGroup(tx.timestamp, language)
        "• $dateStr | ${tx.title.ifBlank { tx.category }}: $sign${CurrencyFormatter.format(tx.amount, currencySymbol, language = language)}"
    }

    val statusText = when {
        cycle.isPaid -> if (isBn) "পরিশোধিত" else "Paid"
        cycle.isOverdue -> if (isBn) "বকেয়া (মেয়াদ উত্তীর্ণ)" else "Overdue"
        else -> if (isBn) "বকেয়া" else "Due"
    }

    val body = buildString {
        appendLine("${AppStrings.t("app_name", language)} — ${account.name}")
        appendLine("${cycle.label} ${if (isBn) "বিলিং স্টেটমেন্ট" else "Billing Statement"}")
        appendLine("────────────────────────")
        appendLine("${if (isBn) "স্টেটমেন্ট বিল" else "Statement Bill"}: ${CurrencyFormatter.format(cycle.billAmount, currencySymbol, language = language)}")
        appendLine("${if (isBn) "পরিশোধের তারিখ" else "Due Date"}: ${cycle.dueDate}")
        appendLine("${if (isBn) "ন্যূনতম বকেয়া" else "Minimum Due"}: ${CurrencyFormatter.format(cycle.minPayment, currencySymbol, language = language)}")
        appendLine("${if (isBn) "সাইকেল খরচ" else "Cycle Spend"}: ${CurrencyFormatter.format(cycle.cycleSpend, currencySymbol, language = language)}")
        appendLine("${if (isBn) "অবস্থা" else "Status"}: $statusText")
        if (recentLines.isNotBlank()) {
            appendLine()
            appendLine(if (isBn) "সাম্প্রতিক লেনদেন:" else "Recent Entries:")
            appendLine(recentLines)
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${account.name} — ${cycle.label}")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, AppStrings.t("share_statement", language)))
}
