package com.didiprogrammer.youtepresta.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import com.didiprogrammer.youtepresta.data.repository.LoanRepository
import kotlinx.coroutines.CancellationException

/**
 * Runs roughly once a day (see [NotificationScheduler]) and notifies the user if any loan is due
 * today or still overdue. Friend names are resolved the same way [com.didiprogrammer.youtepresta.ui.loans.LoansViewModel]
 * does: a best-effort separate fetch, falling back to a placeholder name if it fails, so a
 * friends-lookup hiccup never suppresses the reminder itself.
 */
class DailyLoanCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val loans = LoanRepository.getLoansDueTodayOrOverdue()
            if (loans.isNotEmpty()) {
                val friendsById = try {
                    FriendRepository.getFriends().associateBy { it.id }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyMap()
                }

                val items = loans.map { loan ->
                    LoanNotificationItem(
                        loanId = loan.id,
                        friendName = friendsById[loan.friendId]?.name
                            ?: applicationContext.getString(R.string.common_unknown_friend),
                        outstandingAmount = loan.outstandingPrincipal
                    )
                }

                NotificationHelper.showDueLoansNotification(applicationContext, items)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
