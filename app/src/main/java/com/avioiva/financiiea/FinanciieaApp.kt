package com.avioiva.financiiea

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.avioiva.financiiea.data.AppDatabase
import com.avioiva.financiiea.data.SettingsRepository

/** Simple service locator — one database and one settings store for the whole app. */
object AppContainer {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        db = AppDatabase.build(appContext)
        settings = SettingsRepository(appContext)
    }
}

class FinanciieaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "daily_reminder"
    }
}
