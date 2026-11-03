package com.avioiva.financiiea.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avioiva.financiiea.AppContainer
import com.avioiva.financiiea.R
import com.avioiva.financiiea.data.Account
import com.avioiva.financiiea.data.Transaction
import com.avioiva.financiiea.data.TxType
import com.avioiva.financiiea.ui.common.EmptyView
import com.avioiva.financiiea.ui.common.SectionCard
import com.avioiva.financiiea.ui.common.TransactionRow
import com.avioiva.financiiea.ui.theme.BrandGold
import com.avioiva.financiiea.ui.theme.ExpenseRed
import com.avioiva.financiiea.ui.theme.IncomeGreen
import com.avioiva.financiiea.ui.theme.NavyDeep
import com.avioiva.financiiea.ui.theme.NavySurface
import com.avioiva.financiiea.util.Formats
import java.time.LocalDate

/** Home Dashboard — opens here directly, no login required. */
@Composable
fun HomeScreen(onOpenTransaction: (Long) -> Unit) {
    val transactions by AppContainer.db.transactionDao().observeAll()
        .collectAsState(initial = emptyList())

    val balance = computeBalance(transactions)
    val todayIncome = remember(transactions) {
        transactions.filter {
            it.txType != TxType.EXPENSE && it.timestamp >= Formats.startOfDay()
        }.sumOf { it.amount }
    }
    val todayExpense = remember(transactions) {
        transactions.filter {
            it.txType == TxType.EXPENSE && it.timestamp >= Formats.startOfDay()
        }.sumOf { it.amount }
    }
    val recent = remember(transactions) { transactions.take(10) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BalanceCard(
                total = balance.first,
                cash = balance.second,
                bank = balance.third,
                currency = transactions.firstOrNull()?.currency ?: "BDT"
            )
        }
        item {
            SummaryCard(
                todayIncome = todayIncome,
                todayExpense = todayExpense,
                currency = transactions.firstOrNull()?.currency ?: "BDT"
            )
        }
        item {
            Text(
                stringResource(R.string.recent_transactions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (recent.isEmpty()) {
            item { EmptyView(stringResource(R.string.empty_transactions)) }
        } else {
            items(recent, key = { it.id }) { tx ->
                TransactionRow(tx) { onOpenTransaction(tx.id) }
            }
        }
    }
}

@Composable
private fun BalanceCard(total: Double, cash: Double, bank: Double, currency: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(NavySurface, NavyDeep, NavySurface)),
                RoundedCornerShape(22.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                stringResource(R.string.current_balance).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFA8B8CF),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                Formats.money(total, currency),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniAccountStat(
                    icon = { tint -> Icon(Icons.Rounded.Payments, null, tint = tint, modifier = Modifier.size(18.dp)) },
                    label = stringResource(R.string.cash),
                    amount = cash,
                    currency = currency,
                    modifier = Modifier.weight(1f)
                )
                MiniAccountStat(
                    icon = { tint -> Icon(Icons.Rounded.AccountBalance, null, tint = tint, modifier = Modifier.size(18.dp)) },
                    label = stringResource(R.string.bank),
                    amount = bank,
                    currency = currency,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniAccountStat(
    icon: @Composable (Color) -> Unit,
    label: String,
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon(BrandGold)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA8B8CF))
            Text(
                Formats.moneyShort(amount, currency),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SummaryCard(todayIncome: Double, todayExpense: Double, currency: String) {
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryHalf(
                icon = Icons.Rounded.ArrowUpward,
                iconTint = IncomeGreen,
                label = stringResource(R.string.todays_income),
                amount = todayIncome,
                currency = currency,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
            SummaryHalf(
                icon = Icons.Rounded.ArrowDownward,
                iconTint = ExpenseRed,
                label = stringResource(R.string.todays_expenses),
                amount = todayExpense,
                currency = currency,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryHalf(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                Formats.money(amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** (total, cash, bank) balances across all transactions. */
fun computeBalance(transactions: List<Transaction>): Triple<Double, Double, Double> {
    var cash = 0.0
    var bank = 0.0
    for (tx in transactions) {
        val signed = tx.signedAmount
        when (tx.txAccount) {
            Account.CASH -> cash += signed
            Account.BANK -> bank += signed
        }
    }
    return Triple(cash + bank, cash, bank)
}
