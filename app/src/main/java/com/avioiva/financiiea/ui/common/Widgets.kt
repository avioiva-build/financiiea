package com.avioiva.financiiea.ui.common

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.avioiva.financiiea.R
import com.avioiva.financiiea.data.Transaction
import com.avioiva.financiiea.data.TxType
import com.avioiva.financiiea.ui.theme.BrandGold
import com.avioiva.financiiea.ui.theme.ExpenseRed
import com.avioiva.financiiea.ui.theme.IncomeGreen
import com.avioiva.financiiea.util.Formats

data class Category(val key: String, val icon: ImageVector, @StringRes val labelRes: Int, val tint: Color)

object Categories {
    val all = listOf(
        Category("food", Icons.Rounded.Restaurant, R.string.cat_food, Color(0xFFF97316)),
        Category("transport", Icons.Rounded.DirectionsBus, R.string.cat_transport, Color(0xFF3B82F6)),
        Category("shopping", Icons.Rounded.ShoppingCart, R.string.cat_shopping, Color(0xFFEC4899)),
        Category("bills", Icons.AutoMirrored.Rounded.ReceiptLong, R.string.cat_bills, Color(0xFFD4A017)),
        Category("health", Icons.Rounded.LocalHospital, R.string.cat_health, Color(0xFFEF4444)),
        Category("education", Icons.Rounded.School, R.string.cat_education, Color(0xFF8B5CF6)),
        Category("entertainment", Icons.Rounded.Movie, R.string.cat_entertainment, Color(0xFF06B6D4)),
        Category("other", Icons.Rounded.Category, R.string.cat_other, Color(0xFF94A3B8))
    )

    fun byKey(key: String): Category = all.firstOrNull { it.key == key } ?: all.last()
}

/** Amount colour: money in = green, money out = red. */
fun txColor(tx: Transaction): Color =
    if (tx.txType == TxType.EXPENSE) ExpenseRed else IncomeGreen

@Composable
fun CategoryBadge(categoryKey: String, modifier: Modifier = Modifier, size: Int = 40) {
    val cat = Categories.byKey(categoryKey)
    Box(
        modifier = modifier
            .size(size.dp)
            .background(cat.tint.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(cat.icon, contentDescription = cat.key, tint = cat.tint, modifier = Modifier.size((size * 0.55).dp))
    }
}

@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBadge(tx.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val title = when (tx.txType) {
                    TxType.CREDIT -> tx.fromWhom.ifEmpty { stringResource(R.string.type_credit) }
                    else -> tx.note.ifEmpty { stringResource(Categories.byKey(tx.category).labelRes) }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(Categories.byKey(tx.category).labelRes) +
                        "  •  " + Formats.dayTime(tx.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val sign = if (tx.txType == TxType.EXPENSE) "-" else "+"
                Text(
                    sign + Formats.money(tx.amount, tx.currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = txColor(tx),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (tx.txAccount.name == "BANK") stringResource(R.string.bank)
                    else stringResource(R.string.cash),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun EmptyView(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmText, color = ExpenseRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** Small brand word-mark used in drawer header & splash. */
@Composable
fun BrandName(style: androidx.compose.ui.text.TextStyle, taglineStyle: androidx.compose.ui.text.TextStyle? = null, columnAlignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = columnAlignment) {
        Text("FINANCIIEA", style = style, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        if (taglineStyle != null) {
            Text(
                stringResource(R.string.tagline),
                style = taglineStyle,
                fontStyle = FontStyle.Italic,
                color = BrandGold
            )
        }
    }
}
