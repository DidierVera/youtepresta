package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.model.Payment

data class FriendSummary(
    val currentDebt: Double,
    val activeLoanCount: Int,
    val totalLoanedHistoric: Double,
    val totalInterestCollected: Double,
    val loans: List<Loan>,
    val payments: List<Payment>
)

/**
 * Per-friend loan/payment totals for FriendsScreen and FriendDetailScreen. Loads every loan and
 * payment for the signed-in user in two queries total — never one query per friend — and
 * aggregates them in memory, keyed by friendId. Both screens call this same function so their
 * numbers can never drift apart. Safe at this app's single-user data volume (see CLAUDE.md); a
 * friend with no loans at all simply has no entry in the returned map.
 */
object FriendSummaryRepository {

    suspend fun getFriendSummaries(): Map<String, FriendSummary> {
        val loans = LoanRepository.getLoans()
        val payments = PaymentRepository.getAllPayments()

        val friendIdByLoanId = loans.associate { it.id to it.friendId }
        val loansByFriendId = loans.groupBy { it.friendId }
        val paymentsByFriendId = payments
            .mapNotNull { payment -> friendIdByLoanId[payment.loanId]?.let { friendId -> friendId to payment } }
            .groupBy({ it.first }, { it.second })

        return loansByFriendId.mapValues { (friendId, friendLoans) ->
            val activeOrPartial = friendLoans.filter {
                it.status == LoanStatus.ACTIVE || it.status == LoanStatus.PARTIAL
            }
            val friendPayments = paymentsByFriendId[friendId].orEmpty()
            FriendSummary(
                currentDebt = activeOrPartial.sumOf { it.outstandingPrincipal },
                activeLoanCount = activeOrPartial.size,
                totalLoanedHistoric = friendLoans.sumOf { it.principalAmount },
                totalInterestCollected = friendPayments.sumOf { it.interestPayment },
                loans = friendLoans,
                payments = friendPayments
            )
        }
    }
}
