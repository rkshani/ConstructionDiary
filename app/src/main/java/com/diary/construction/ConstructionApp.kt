package com.diary.construction

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.diary.construction.data.reminder.ReminderNotifier
import com.diary.construction.data.reminder.ReminderScheduler
import com.diary.construction.data.settings.LocaleStore
import com.diary.construction.di.ApplicationScope
import com.diary.construction.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ConstructionApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var reminderNotifier: ReminderNotifier

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun attachBaseContext(base: Context) {
        // Background work (notifications, PDFs) uses the app language too.
        super.attachBaseContext(LocaleStore.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        reminderNotifier.createChannel()
        // Keep the daily reminder job in step with the reminder switches.
        appScope.launch {
            settings.settings
                .map { Triple(it.labourReminder, it.supplierReminder, it.stageReminder) }
                .distinctUntilChanged()
                .collect { runCatching { reminderScheduler.sync(settings.current()) } }
        }
    }
}
