package com.avioiva.financiiea.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "financiiea_settings")

/** Immutable snapshot of every setting. AppRoot treats a null SettingsState
 *  (from collectAsState's initial value) as "still loading". */
data class SettingsState(
    val accentColor: Long,
    val languageCode: String,
    val pinHash: String?,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val driveTreeUri: String?,
    val defaultCurrency: String
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ACCENT = longPreferencesKey("accent_color")
        val LANGUAGE = stringPreferencesKey("language")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val REMINDER_ON = booleanPreferencesKey("reminder_on")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MIN = intPreferencesKey("reminder_minute")
        val DRIVE_URI = stringPreferencesKey("drive_tree_uri")
        val CURRENCY = stringPreferencesKey("default_currency")
    }

    companion object {
        const val DEFAULT_ACCENT = 0xFF14B8A6L // teal — the brand default
        const val DEFAULT_CURRENCY = "BDT"

        fun hashPin(pin: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest((pin + ":financiiea").toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    val state: Flow<SettingsState> = context.dataStore.data.map { p ->
        SettingsState(
            accentColor = p[Keys.ACCENT] ?: DEFAULT_ACCENT,
            languageCode = p[Keys.LANGUAGE] ?: "en",
            pinHash = p[Keys.PIN_HASH],
            reminderEnabled = p[Keys.REMINDER_ON] ?: false,
            reminderHour = p[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = p[Keys.REMINDER_MIN] ?: 0,
            driveTreeUri = p[Keys.DRIVE_URI],
            defaultCurrency = p[Keys.CURRENCY] ?: DEFAULT_CURRENCY
        )
    }

    val accentColor: Flow<Long> = context.dataStore.data.map { it[Keys.ACCENT] ?: DEFAULT_ACCENT }
    val languageCode: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    suspend fun setAccentColor(argb: Long) = context.dataStore.edit { it[Keys.ACCENT] = argb }

    suspend fun setLanguage(code: String) = context.dataStore.edit { it[Keys.LANGUAGE] = code }

    suspend fun setPinHash(hash: String?) = context.dataStore.edit { prefs ->
        if (hash == null) prefs.remove(Keys.PIN_HASH) else prefs[Keys.PIN_HASH] = hash
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.REMINDER_ON] = enabled
        it[Keys.REMINDER_HOUR] = hour
        it[Keys.REMINDER_MIN] = minute
    }

    suspend fun setReminderTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.REMINDER_HOUR] = hour
        it[Keys.REMINDER_MIN] = minute
    }

    suspend fun setDriveTreeUri(uri: String?) = context.dataStore.edit { prefs ->
        if (uri == null) prefs.remove(Keys.DRIVE_URI) else prefs[Keys.DRIVE_URI] = uri
    }

    suspend fun setDefaultCurrency(code: String) = context.dataStore.edit { it[Keys.CURRENCY] = code }
}
