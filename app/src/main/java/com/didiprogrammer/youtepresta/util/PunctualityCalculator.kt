package com.didiprogrammer.youtepresta.util

import androidx.annotation.StringRes
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Payment
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PunctualityStatus(@StringRes val labelRes: Int) {
    PUNCTUAL(R.string.punctuality_punctual),
    OCCASIONALLY_LATE(R.string.punctuality_occasionally_late),
    LATE(R.string.punctuality_late),
    NOT_ENOUGH_DATA(R.string.punctuality_not_enough_data)
}

/**
 * Single source of truth for the punctuality traffic light shown on FriendsScreen/
 * FriendDetailScreen — if the thresholds ever change, they only need to change here.
 */
object PunctualityCalculator {
    const val MINIMUM_PAYMENTS_REQUIRED = 2
    private const val OCCASIONALLY_LATE_MAX_AVERAGE_DAYS = 5.0

    /** Positive = paid after the due date; zero/negative = on time or early. */
    fun daysLate(paymentDate: LocalDate, dueDateAtPayment: LocalDate): Long =
        ChronoUnit.DAYS.between(dueDateAtPayment, paymentDate)

    /**
     * [daysLateValues] is the friend's full history — every payment across every one of their
     * loans, paid off or not, per the business rule (never averaged per-loan first).
     */
    fun calculate(daysLateValues: List<Long>): PunctualityStatus {
        if (daysLateValues.size < MINIMUM_PAYMENTS_REQUIRED) return PunctualityStatus.NOT_ENOUGH_DATA
        val average = daysLateValues.average()
        return when {
            average <= 0.0 -> PunctualityStatus.PUNCTUAL
            average <= OCCASIONALLY_LATE_MAX_AVERAGE_DAYS -> PunctualityStatus.OCCASIONALLY_LATE
            else -> PunctualityStatus.LATE
        }
    }

    fun calculateForPayments(payments: List<Payment>): PunctualityStatus =
        calculate(
            payments.mapNotNull { payment ->
                runCatching {
                    daysLate(LocalDate.parse(payment.paymentDate), LocalDate.parse(payment.dueDateAtPayment))
                }.getOrNull()
            }
        )
}
