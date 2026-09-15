package com.didiprogrammer.youtepresta.ui.loans

import androidx.annotation.StringRes
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.repository.LoanStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Purely visual classification for the loan list/detail badges. Never written back to the
 * database — `status` in Postgres only ever moves between active/partial/paid (set when
 * payments are recorded in Phase 5); "overdue" here is computed from due_date each time the
 * screen is drawn.
 */
enum class LoanVisualStatus(@StringRes val labelRes: Int) {
    PAID(R.string.loan_status_paid),
    OVERDUE(R.string.loan_status_overdue),
    DUE_TODAY(R.string.loan_status_due_today),
    ON_TRACK(R.string.loan_status_on_track)
}

fun visualStatusFor(loan: Loan, today: LocalDate = LocalDate.now()): LoanVisualStatus {
    if (loan.status == LoanStatus.PAID) return LoanVisualStatus.PAID

    val dueDate = loan.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return LoanVisualStatus.ON_TRACK

    return when {
        dueDate.isBefore(today) -> LoanVisualStatus.OVERDUE
        dueDate.isEqual(today) -> LoanVisualStatus.DUE_TODAY
        else -> LoanVisualStatus.ON_TRACK
    }
}

private val loanDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))

fun formatLoanDate(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate).format(loanDateFormatter) }.getOrDefault(isoDate)

fun formatLoanDate(date: LocalDate): String = date.format(loanDateFormatter)
