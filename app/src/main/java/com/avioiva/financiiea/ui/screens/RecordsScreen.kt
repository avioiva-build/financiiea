package com.avioiva.financiiea.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avioiva.financiiea.AppContainer
import com.avioiva.financiiea.R
import com.avioiva.financiiea.data.BackupManager
import com.avioiva.financiiea.data.Transaction
import com.avioiva.financiiea.data.TxType
import com.avioiva.financiiea.ui.common.BarGroup
import com.avioiva.financiiea.ui.common.Categories
import com.avioiva.financiiea.ui.common.ConfirmDialog
import com.avioiva.financiiea.ui.common.EmptyView
import com.avioiva.financiiea.ui.common.MonthlyBarChart
import com.avioiva.financiiea.ui.common.PieChart
import com.avioiva.financiiea.ui.common.PieSlice
import com.avioiva.financiiea.ui.common.SectionCard
import com.avioiva.financiiea.ui.common.TransactionRow
import com.avioiva.financiiea.ui.common.chartPalette
import com.avioiva.financiiea.ui.theme.ExpenseRed
import com.avioiva.financiiea.ui.theme.IncomeGreen
import com.avioiva.financiiea.util.Formats
import com.avioiva.financiiea.util.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.YearMonth

private enum class HistoryFilter { DAY, WEEK, MONTH, CUSTOM }
private enum class ReportPeriod { WEEK, MONTH, YEAR, ALL }

/** "Transaction History & Reports" — two tabs (Prompt 5). */
@Composable
fun RecordsScreen(onEditTransaction: (Long) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val titles = listOf(
        stringResource(R.string.title_history),
        stringResource(R.string.title_reports)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            titles.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(
                            title,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        when (tab) {
            0 -> HistoryTab(onEditTransaction)
            else -> ReportsTab()
        }
    }
}

// ---------------------------------------------------------------------
// History tab
// ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(onEditTransaction: (Long) -> Unit) {
    val all by AppContainer.db.transactionDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val dao = AppContainer.db.transactionDao()

    var filter by remember { mutableStateOf(HistoryFilter.MONTH) }
    var customStart by remember {
        mutableStateOf(Formats.startOfDay() - 6L * 24 * 3600 * 1000)
    }
    var customEnd by remember { mutableStateOf(Formats.endOfDay()) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var actionTx by remember { mutableStateOf<Transaction?>(null) }
    var deleteTx by remember { mutableStateOf<Transaction?>(null) }

    val range = when (filter) {
        HistoryFilter.DAY -> Formats.startOfDay() to Formats.endOfDay()
        HistoryFilter.WEEK -> Formats.last7Days()
        HistoryFilter.MONTH -> Formats.currentMonth()
        HistoryFilter.CUSTOM -> customStart to customEnd
    }
    val filtered = remember(all, filter, customStart, customEnd) {
        all.filter { it.timestamp >= range.first && it.timestamp <= range.second }
    }
    val totalIn = filtered.filter { it.txType != TxType.EXPENSE }.sumOf { it.amount }
    val totalOut = filtered.filter { it.txType == TxType.EXPENSE }.sumOf { it.amount }
    val mainCurrency = filtered.firstOrNull()?.currency ?: "BDT"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter chips: Day / Week / Month / Custom
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = {
                        Text(
                            stringResource(
                                when (f) {
                                    HistoryFilter.DAY -> R.string.filter_day
                                    HistoryFilter.WEEK -> R.string.filter_week
                                    HistoryFilter.MONTH -> R.string.filter_month
                                    HistoryFilter.CUSTOM -> R.string.filter_custom
                                }
                            )
                        )
                    }
                )
            }
        }
        if (filter == HistoryFilter.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.custom_from) + ": " + Formats.day(customStart), maxLines = 1)
                }
                OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.custom_to) + ": " + Formats.day(customEnd), maxLines = 1)
                }
            }
        }

        // Totals strip for the selected period
        SectionCard {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.in_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "+" + Formats.money(totalIn, mainCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.out_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "-" + Formats.money(totalOut, mainCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.current_balance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        Formats.money(totalIn - totalOut, mainCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyView(stringResource(R.string.empty_range))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { tx ->
                    TransactionRow(tx) { actionTx = tx }
                }
            }
        }
    }

    // Tap a transaction → Edit or Delete
    actionTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { actionTx = null },
            title = { Text(stringResource(R.string.edit_or_delete)) },
            text = {
                Text(
                    (tx.note.ifEmpty { stringResource(Categories.byKey(tx.category).labelRes) }) +
                        "  •  " + Formats.money(tx.amount, tx.currency),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    actionTx = null
                    onEditTransaction(tx.id)
                }) {
                    Text(
                        stringResource(R.string.edit),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteTx = tx
                    actionTx = null
                }) {
                    Text(stringResource(R.string.delete), color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    deleteTx?.let { tx ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_msg),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                scope.launch { dao.delete(tx) }
                deleteTx = null
            },
            onDismiss = { deleteTx = null }
        )
    }

    if (pickingStart) {
        val st = rememberDatePickerState(initialSelectedDateMillis = customStart)
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        customStart = Formats.startOfDay(
                            java.time.Instant.ofEpochMilli(it).atZone(Formats.zone()).toLocalDate()
                        )
                    }
                    pickingStart = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = st) }
    }
    if (pickingEnd) {
        val st = rememberDatePickerState(initialSelectedDateMillis = customEnd)
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        customEnd = Formats.endOfDay(
                            java.time.Instant.ofEpochMilli(it).atZone(Formats.zone()).toLocalDate()
                        )
                    }
                    pickingEnd = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingEnd = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = st) }
    }
}

// ---------------------------------------------------------------------
// Reports tab
// ---------------------------------------------------------------------

@Composable
private fun ReportsTab() {
    val all by AppContainer.db.transactionDao().observeAll().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var period by remember { mutableStateOf(ReportPeriod.MONTH) }
    val range = when (period) {
        ReportPeriod.WEEK -> Formats.last7Days()
        ReportPeriod.MONTH -> Formats.currentMonth()
        ReportPeriod.YEAR -> Formats.currentYear()
        ReportPeriod.ALL -> 0L to Long.MAX_VALUE
    }
    val periodLabel = stringResource(
        when (period) {
            ReportPeriod.WEEK -> R.string.this_week
            ReportPeriod.MONTH -> R.string.this_month
            ReportPeriod.YEAR -> R.string.this_year
            ReportPeriod.ALL -> R.string.all_time
        }
    )

    val periodTx = remember(all, period) {
        all.filter { it.timestamp >= range.first && it.timestamp <= range.second }
    }

    // Pie: expense share per category
    val pieSlices = remember(periodTx) {
        periodTx.filter { it.txType == TxType.EXPENSE }
            .groupBy { it.category }
            .map { (key, list) -> key to list.sumOf { it.amount } }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .mapIndexed { i, (key, value) ->
                PieSlice(key, value, chartPalette[i % chartPalette.size])
            }
    }

    // Bars: last 6 months income vs expense
    val barGroups = remember(all) {
        (0..5).map { YearMonth.now().minusMonths((5 - it).toLong()) }.map { m ->
            val inM = all.filter {
                Formats.monthOf(it.timestamp) == m && it.txType != TxType.EXPENSE
            }.sumOf { it.amount }
            val outM = all.filter {
                Formats.monthOf(it.timestamp) == m && it.txType == TxType.EXPENSE
            }.sumOf { it.amount }
            BarGroup(Formats.monthShort(m), inM, outM)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p },
                        label = {
                            Text(
                                stringResource(
                                    when (p) {
                                        ReportPeriod.WEEK -> R.string.filter_week
                                        ReportPeriod.MONTH -> R.string.filter_month
                                        ReportPeriod.YEAR -> R.string.this_year
                                        ReportPeriod.ALL -> R.string.all_time
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }
        item {
            SectionCard {
                Text(
                    stringResource(R.string.spending_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pieSlices.isEmpty()) {
                    EmptyView(stringResource(R.string.report_empty))
                } else {
                    PieChart(
                        slices = pieSlices.map { s ->
                            s.copy(label = stringResource(Categories.byKey(s.label).labelRes))
                        }
                    )
                }
            }
        }
        item {
            SectionCard {
                Text(
                    stringResource(R.string.monthly_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                MonthlyBarChart(groups = barGroups)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                val file = BackupManager.createPdf(context, periodTx, periodLabel)
                                ShareUtils.shareFile(context, file, "application/pdf", periodLabel)
                            }.onFailure {
                                Toast.makeText(
                                    context, it.message ?: "Error", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.export_pdf), maxLines = 1, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                val csv = BackupManager.createCsv(periodTx)
                                val file = ShareUtils.writeCacheFile(
                                    context, BackupManager.csvFileName(), csv
                                )
                                ShareUtils.shareFile(context, file, "text/csv", periodLabel)
                            }.onFailure {
                                Toast.makeText(
                                    context, it.message ?: "Error", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.export_csv), maxLines = 1, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
