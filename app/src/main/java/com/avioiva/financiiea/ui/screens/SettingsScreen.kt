package com.avioiva.financiiea.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.avioiva.financiiea.AppContainer
import com.avioiva.financiiea.R
import com.avioiva.financiiea.data.BackupManager
import com.avioiva.financiiea.data.SettingsRepository
import com.avioiva.financiiea.ui.common.SectionCard
import com.avioiva.financiiea.ui.theme.BrandGold
import com.avioiva.financiiea.ui.theme.BrandTeal
import com.avioiva.financiiea.ui.theme.presetColors
import com.avioiva.financiiea.util.Formats
import com.avioiva.financiiea.util.ReminderScheduler
import com.avioiva.financiiea.util.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val languages = listOf(
    "en" to "English",
    "bn" to "বাংলা (Bengali)",
    "hi" to "हिन्दी (Hindi)",
    "ar" to "العربية (Arabic)",
    "zh" to "中文 (Chinese)",
    "fr" to "Français (French)",
    "ru" to "Русский (Russian)"
)

/** Settings screen (Prompt 6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = AppContainer.settings
    val state by settings.state.collectAsState(initial = null)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }
    var showRemovePin by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomColor by remember { mutableStateOf(false) }
    var pendingReminderOn by remember { mutableStateOf(false) }

    // notification permission (Android 13+)
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val s = state
            if (s != null) {
                ReminderScheduler.schedule(context, s.reminderHour, s.reminderMinute)
                scope.launch { settings.setReminder(true, s.reminderHour, s.reminderMinute) }
            }
        } else {
            pendingReminderOn = false
            Toast.makeText(context, context.getString(R.string.notifications_denied), Toast.LENGTH_LONG).show()
        }
    }

    // Drive folder picker
    val drivePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            scope.launch { settings.setDriveTreeUri(uri.toString()) }
            Toast.makeText(context, context.getString(R.string.drive_connected), Toast.LENGTH_SHORT).show()
        }
    }

    // Restore file picker
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: return@launch
                    val restored = BackupManager.parseBackup(text)
                    if (restored == null) {
                        Toast.makeText(context, context.getString(R.string.restore_failed), Toast.LENGTH_LONG).show()
                    } else {
                        val dao = AppContainer.db.transactionDao()
                        dao.deleteAll()
                        restored.forEach { dao.insert(it) }
                        Toast.makeText(context, context.getString(R.string.restore_done), Toast.LENGTH_LONG).show()
                    }
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.restore_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (state == null) return

    val s = state!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------- 1. Language ----------
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            val currentName = languages.firstOrNull { it.first == s.languageCode }?.second ?: "English"
            OutlinedButton(onClick = { showLanguageDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text(currentName)
            }
        }

        // ---------- 2. App Lock ----------
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_lock), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.app_lock_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = s.pinHash != null,
                    onCheckedChange = { wantOn ->
                        if (wantOn) showSetPin = true
                        else showRemovePin = true
                    }
                )
            }
            if (s.pinHash != null) {
                OutlinedButton(onClick = { showSetPin = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.change_pin))
                }
            }
        }

        // ---------- 3. Daily Reminder ----------
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.daily_reminder), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = s.reminderEnabled,
                    onCheckedChange = { wantOn ->
                        if (wantOn) {
                            if (ReminderScheduler.hasNotificationPermission(context)) {
                                ReminderScheduler.schedule(context, s.reminderHour, s.reminderMinute)
                                scope.launch { settings.setReminder(true, s.reminderHour, s.reminderMinute) }
                            } else {
                                pendingReminderOn = true
                                notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            ReminderScheduler.cancel(context)
                            scope.launch { settings.setReminder(false, s.reminderHour, s.reminderMinute) }
                        }
                    }
                )
            }
            if (s.reminderEnabled) {
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reminder_time) + ": " + Formats.hourMinute(s.reminderHour, s.reminderMinute))
                }
            }
        }

        // ---------- 4. Backup & Restore ----------
        SectionCard {
            Text(stringResource(R.string.backup_restore), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(
                    if (s.driveTreeUri != null) R.string.drive_connected else R.string.drive_not_connected
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (s.driveTreeUri != null) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(stringResource(R.string.backup_pick_folder), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { drivePicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Language, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.connect_drive), maxLines = 1)
            }
            OutlinedButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val txs = AppContainer.db.transactionDao().observeAll().first()
                        val json = BackupManager.createBackupJson(txs)
                        val tree = s.driveTreeUri
                        val ok = if (tree != null) {
                            ShareUtils.saveToTree(context, Uri.parse(tree), BackupManager.backupFileName(), "application/json", json)
                        } else false
                        if (!ok) {
                            val file = ShareUtils.writeCacheFile(context, BackupManager.backupFileName(), json)
                            ShareUtils.shareFile(context, file, "application/json", "FINANCIIEA Backup")
                        }
                    }.onSuccess {
                        Toast.makeText(context, context.getString(R.string.backup_done), Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, it.message ?: "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.CloudUpload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.backup_now), maxLines = 1)
            }
            OutlinedButton(onClick = { restorePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Restore, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.restore_backup), maxLines = 1)
            }
            OutlinedButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val txs = AppContainer.db.transactionDao().observeAll().first()
                        val json = BackupManager.createBackupJson(txs)
                        ShareUtils.saveToDownloads(context, BackupManager.backupFileName(), "application/json", json)
                    }.onSuccess {
                        Toast.makeText(context, context.getString(R.string.backup_done), Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, it.message ?: "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save_backup_phone), maxLines = 1)
            }
        }

        // ---------- 5. Theme colour ----------
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.theme_color), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            // preset swatches, 5 per row
            presetColors.chunked(5).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { c ->
                        val selected = s.accentColor == c.toArgbLong()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    scope.launch { settings.setAccentColor(c.toArgbLong()) }
                                }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { showCustomColor = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.custom_color))
            }
        }

        Text(
            stringResource(R.string.about_made),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }

    // ---------- dialogs ----------

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showLanguageDialog = false
                                    scope.launch { settings.setLanguage(code) }
                                    // Applies instantly to the whole app
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(code)
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (code == s.languageCode) FontWeight.Bold else FontWeight.Normal,
                                color = if (code == s.languageCode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onPinSet = { pin ->
                scope.launch {
                    settings.setPinHash(SettingsRepository.hashPin(pin))
                    Toast.makeText(context, context.getString(R.string.pin_set_done), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showRemovePin) {
        AlertDialog(
            onDismissRequest = { showRemovePin = false },
            title = { Text(stringResource(R.string.app_lock)) },
            text = { Text(stringResource(R.string.app_lock_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemovePin = false
                    scope.launch { settings.setPinHash(null) }
                }) { Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePin = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showTimePicker) {
        val tps = rememberTimePickerState(initialHour = s.reminderHour, initialMinute = s.reminderMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { TimePicker(state = tps) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    scope.launch { settings.setReminderTime(tps.hour, tps.minute) }
                    if (s.reminderEnabled) ReminderScheduler.schedule(context, tps.hour, tps.minute)
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showCustomColor) {
        CustomColorDialog(
            onDismiss = { showCustomColor = false },
            onApply = { argb ->
                showCustomColor = false
                scope.launch { settings.setAccentColor(argb) }
            }
        )
    }
}

private fun Color.toArgbLong(): Long = this.toArgb().toLong() and 0xFFFFFFFFL

/** HSL → Compose Color (self-contained helper, works on every Compose version). */
private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = (((hue % 360f) + 360f) % 360f) / 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun channel(t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    return Color(channel(h + 1f / 3f), channel(h), channel(h - 1f / 3f))
}

/** Simple HSL colour picker with a live preview. */
@Composable
private fun CustomColorDialog(onDismiss: () -> Unit, onApply: (Long) -> Unit) {
    var hue by remember { mutableStateOf(174f) }
    var sat by remember { mutableStateOf(0.72f) }
    var light by remember { mutableStateOf(0.45f) }
    val preview = hslColor(hue, sat, light)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(preview)
                )
                Text("Hue", style = MaterialTheme.typography.labelSmall)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text("Saturation", style = MaterialTheme.typography.labelSmall)
                Slider(value = sat, onValueChange = { sat = it }, valueRange = 0.15f..1f)
                Text("Lightness", style = MaterialTheme.typography.labelSmall)
                Slider(value = light, onValueChange = { light = it }, valueRange = 0.2f..0.75f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(preview.toArgbLong()) }) {
                Text(stringResource(R.string.apply), fontWeight = FontWeight.Bold, color = BrandGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
