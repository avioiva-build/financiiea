package com.avioiva.financiiea.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.foundation.text.KeyboardOptions
import com.avioiva.financiiea.AppContainer
import com.avioiva.financiiea.R
import com.avioiva.financiiea.data.Account
import com.avioiva.financiiea.data.Transaction
import com.avioiva.financiiea.data.TxType
import com.avioiva.financiiea.ui.common.Categories
import com.avioiva.financiiea.ui.common.CategoryBadge
import com.avioiva.financiiea.ui.common.SectionCard
import com.avioiva.financiiea.ui.theme.ExpenseRed
import com.avioiva.financiiea.util.Currencies
import com.avioiva.financiiea.util.Formats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** Add / Edit Transaction screen (Prompt 4). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(txId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = AppContainer.db.transactionDao()
    val settings = AppContainer.settings

    var isEdit by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(txId == -1L) }

    // form state
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currencies.DEFAULT) }
    var account by remember { mutableStateOf(Account.CASH) }
    var category by remember { mutableStateOf("food") }
    var fromWhom by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var receiptPath by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var currencyMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReceipt by remember { mutableStateOf(false) }

    LaunchedEffect(txId) {
        if (txId != -1L) {
            dao.getById(txId)?.let { tx ->
                isEdit = true
                type = tx.txType
                amountText = if (tx.amount % 1.0 == 0.0)
                    tx.amount.toInt().toString() else tx.amount.toString()
                currency = tx.currency
                account = tx.txAccount
                category = tx.category
                fromWhom = tx.fromWhom
                note = tx.note
                timestamp = tx.timestamp
                receiptPath = tx.receiptPath
            }
        } else {
            currency = settings.state.first().defaultCurrency
        }
        loaded = true
    }

    // ---- receipt capture ----
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            val src = cameraUri ?: return@rememberLauncherForActivityResult
            val dest = File(File(context.filesDir, "receipts").apply { mkdirs() }, "r_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(src)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            receiptPath = dest.absolutePath
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val dest = File(File(context.filesDir, "receipts").apply { mkdirs() }, "r_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            receiptPath = dest.absolutePath
        }
    }

    if (!loaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- 1. Expense / Payment / Credit toggle ----
        SectionCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val types = listOf(
                    Triple(TxType.EXPENSE, R.string.type_expense, ExpenseRed),
                    Triple(TxType.PAYMENT, R.string.type_payment, MaterialTheme.colorScheme.primary),
                    Triple(TxType.CREDIT, R.string.type_credit, com.avioiva.financiiea.ui.theme.BrandGold)
                )
                types.forEach { (t, labelRes, tint) ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(stringResource(labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tint.copy(alpha = 0.25f),
                            selectedLabelColor = tint
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (type == TxType.CREDIT) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = fromWhom,
                    onValueChange = { fromWhom = it },
                    label = { Text(stringResource(R.string.credit_from)) },
                    placeholder = { Text(stringResource(R.string.credit_from_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ---- 2. Amount + currency + account ----
        SectionCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v ->
                        if (v.length <= 14 && v.all { it.isDigit() || it == '.' }) amountText = v
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                // currency dropdown
                Box {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.currency)) },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                    // transparent overlay so taps open the menu
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { currencyMenu = true }
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = currencyMenu,
                        onDismissRequest = { currencyMenu = false }
                    ) {
                        Currencies.codes.forEach { code ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("$code (${Currencies.symbol(code)})") },
                                onClick = {
                                    currency = code
                                    currencyMenu = false
                                }
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Account.entries.forEach { acc ->
                    FilterChip(
                        selected = account == acc,
                        onClick = { account = acc },
                        label = {
                            Text(
                                stringResource(
                                    if (acc == Account.CASH) R.string.cash else R.string.bank
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- 3. Category icon grid ----
        SectionCard {
            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
            Categories.all.chunked(4).forEach { rowCats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCats.forEach { cat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { category = cat.key }
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (category == cat.key) cat.tint.copy(alpha = 0.22f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryBadge(cat.key, size = 38)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(cat.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = if (category == cat.key) cat.tint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // pad the last row to keep 4 columns
                    repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ---- 4. Date & time ----
        SectionCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Formats.day(timestamp), maxLines = 1)
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    val dt = Formats.toLocalDateTime(timestamp)
                    Text(String.format("%02d:%02d", dt.hour, dt.minute), maxLines = 1)
                }
            }
        }

        // ---- 5. Notes ----
        SectionCard {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.notes)) },
                placeholder = { Text(stringResource(R.string.notes_hint)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---- 6. Receipt (camera / gallery) ----
        SectionCard {
            Text(stringResource(R.string.receipt), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    val dir = File(context.filesDir, "camera").apply { mkdirs() }
                    val file = File(dir, "c_${UUID.randomUUID()}.jpg")
                    cameraUri = FileProvider.getUriForFile(
                        context, "com.avioiva.financiiea.fileprovider", file
                    )
                    cameraLauncher.launch(cameraUri!!)
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.camera), maxLines = 1)
                }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.gallery), maxLines = 1)
                }
            }
            if (receiptPath.isNotEmpty() && File(receiptPath).exists()) {
                Spacer(Modifier.height(4.dp))
                Box {
                    val bmp = remember(receiptPath) {
                        BitmapFactory.decodeFile(receiptPath)?.let {
                            val ratio = 240f / it.height.coerceAtLeast(1)
                            android.graphics.Bitmap.createScaledBitmap(
                                it, (it.width * ratio).toInt().coerceAtLeast(1), 240, true
                            )
                        }
                    }
                    bmp?.let {
                        Image(
                            bitmap = it.asImageBitmapCompat(),
                            contentDescription = stringResource(R.string.receipt),
                            modifier = Modifier
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showReceipt = true },
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(
                        onClick = {
                            File(receiptPath).delete()
                            receiptPath = ""
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .size(28.dp)
                    ) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.remove_receipt), tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        // ---- 7. Save ----
        Button(
            onClick = {
                val amount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                if (amount <= 0.0) {
                    error = context.getString(R.string.error_amount)
                    return@Button
                }
                val tx = Transaction(
                    id = if (isEdit) txId else 0L,
                    type = type.name,
                    amount = amount,
                    currency = currency,
                    account = account.name,
                    category = category,
                    fromWhom = if (type == TxType.CREDIT) fromWhom.trim() else "",
                    note = note.trim(),
                    timestamp = timestamp,
                    receiptPath = receiptPath
                )
                scope.launch {
                    if (isEdit) {
                        dao.update(tx)
                    } else {
                        dao.insert(tx)
                    }
                    settings.setDefaultCurrency(currency)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(if (isEdit) R.string.updated else R.string.saved),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    onDone()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                stringResource(R.string.save),
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
        }

        if (isEdit) {
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
            ) {
                Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.delete))
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // ---- dialogs ----

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { sel ->
                        val old = Formats.toLocalDateTime(timestamp)
                        val newDate = Formats.millisFrom(
                            java.time.Instant.ofEpochMilli(sel).atZone(Formats.zone()).toLocalDate(),
                            old.hour, old.minute
                        )
                        timestamp = newDate
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val now = Formats.toLocalDateTime(timestamp)
        val state = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.time)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val date = Formats.toLocalDateTime(timestamp).toLocalDate()
                    timestamp = Formats.millisFrom(date, state.hour, state.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteConfirm) {
        com.avioiva.financiiea.ui.common.ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_msg),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                scope.launch {
                    dao.getById(txId)?.let { dao.delete(it) }
                    onDone()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showReceipt && receiptPath.isNotEmpty()) {
        val bmp = remember(receiptPath) { BitmapFactory.decodeFile(receiptPath) }
        bmp?.let {
            AlertDialog(
                onDismissRequest = { showReceipt = false },
                confirmButton = {
                    TextButton(onClick = { showReceipt = false }) { Text(stringResource(R.string.ok)) }
                },
                text = {
                    Image(
                        bitmap = it.asImageBitmapCompat(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

/** Small helper so BitmapFactory results can be shown with Compose Image(). */
private fun android.graphics.Bitmap.asImageBitmapCompat(): androidx.compose.ui.graphics.ImageBitmap =
    this.asImageBitmap()
