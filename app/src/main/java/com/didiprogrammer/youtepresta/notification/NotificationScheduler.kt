package com.didiprogrammer.youtepresta.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules [DailyLoanCheckWorker] to run about once every 24 hours. No exact time is requested
 * on purpose — the user just wants "some point during the day", so the system is left free to
 * pick a convenient moment for battery/Doze instead of an exact alarm.
 */
object NotificationScheduler {

    private const val WORK_NAME = "daily_loan_check"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DailyLoanCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
