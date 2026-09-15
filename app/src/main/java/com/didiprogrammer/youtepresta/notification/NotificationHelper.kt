package com.didiprogrammer.youtepresta.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.didiprogrammer.youtepresta.MainActivity
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.ui.sources.formatCop

/** One loan that needs a collection reminder, with its friend name already resolved. */
data class LoanNotificationItem(
    val loanId: String,
    val friendName: String,
    val outstandingAmount: Double
)

object NotificationHelper {

    private const val CHANNEL_ID = "loan_due_reminders"
    private const val NOTIFICATION_ID = 1001

    const val EXTRA_LOAN_ID = "loan_id"
    const val EXTRA_SHOW_LOANS_LIST = "show_loans_list"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a single grouped notification for every loan due today or overdue: one loan gets a
     * direct link to its detail screen, several get a summary that opens the loans list instead.
     */
    fun showDueLoansNotification(context: Context, loans: List<LoanNotificationItem>) {
        if (loans.isEmpty()) return
        ensureChannel(context)

        val title: String
        val body: String
        val targetIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (loans.size == 1) {
            val loan = loans.first()
            title = context.getString(R.string.notification_single_title, loan.friendName)
            body = context.getString(R.string.notification_single_body, formatCop(loan.outstandingAmount))
            targetIntent.putExtra(EXTRA_LOAN_ID, loan.loanId)
        } else {
            title = context.getString(R.string.notification_multiple_title, loans.size)
            body = loans.joinToString(", ") { it.friendName }
            targetIntent.putExtra(EXTRA_SHOW_LOANS_LIST, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
