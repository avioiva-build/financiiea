package com.avioiva.financiiea.ui.common

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.avioiva.financiiea.R
import com.avioiva.financiiea.ui.theme.BrandGold
import com.avioiva.financiiea.ui.theme.ExpenseRed
import com.avioiva.financiiea.ui.theme.IncomeGreen
import com.avioiva.financiiea.util.Formats
import java.util.Locale

val chartPalette = listOf(
    Color(0xFF14B8A6), Color(0xFFD4A017), Color(0xFF3B82F6), Color(0xFF22C55E),
    Color(0xFFF97316), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4),
    Color(0xFF94A3B8)
)

data class PieSlice(val label: String, val value: Double, val color: Color)

/** Donut chart with a legend. */
@Composable
fun PieChart(slices: List<PieSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(190.dp)) {
            if (total <= 0.0) {
                drawCircle(
                    color = Color(0xFF33456A),
                    radius = size.minDimension / 2 - 8f,
                    style = Stroke(width = 26f)
                )
                return@Canvas
            }
            var start = -90f
            val stroke = 34f
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            for (s in slices) {
                val sweep = (s.value / total * 360.0).toFloat()
                drawArc(
                    color = s.color,
                    startAngle = start,
                    sweepAngle = sweep - 1.2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke)
                )
                start += sweep
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (s in slices.sortedByDescending { it.value }) {
                val pct = if (total > 0) s.value / total * 100 else 0.0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(s.color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        String.format(Locale.US, "%.1f%%", pct) + "  •  " + Formats.moneyShort(s.value, "BDT"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

data class BarGroup(val label: String, val income: Double, val expense: Double)

/** Grouped bar chart: income vs expense for the last 6 months. */
@Composable
fun MonthlyBarChart(groups: List<BarGroup>, modifier: Modifier = Modifier) {
    val maxValue = groups.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(IncomeGreen)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.income), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(14.dp))
            LegendDot(ExpenseRed)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.expense), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(12.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (groups.isEmpty() || maxValue <= 0.0) return@Canvas
            val groupW = size.width / groups.size
            val barW = groupW * 0.22f
            val usableH = size.height - 8f
            groups.forEachIndexed { i, g ->
                val cx = groupW * i + groupW / 2f
                val inH = (g.income / maxValue * usableH).toFloat()
                val outH = (g.expense / maxValue * usableH).toFloat()
                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(cx - barW - 3.dp.toPx(), usableH - inH),
                    size = Size(barW, inH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(cx + 3.dp.toPx(), usableH - outH),
                    size = Size(barW, outH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                // baseline
                drawLine(
                    color = Color(0xFF33456A),
                    start = Offset(groupW * i + groupW * 0.1f, usableH + 2f),
                    end = Offset(groupW * (i + 1) - groupW * 0.1f, usableH + 2f),
                    strokeWidth = 2f
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            for (g in groups) {
                Text(
                    g.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
}
