package com.didiprogrammer.youtepresta.util

import androidx.annotation.StringRes
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Loan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The recurring monthly due-day rule a loan is paid on. Stored in Supabase as the plain string in
 * [dbValue] (`loans.due_day_rule`, `text` with a `check` constraint) rather than a native Postgres
 * enum — matches how `Loan` maps every other lookup-style column, and needs no custom
 * `KSerializer` since `Loan.dueDayRule` just carries the raw string; call [fromDbValue]/[dbValue]
 * at the boundary wherever the enum is actually needed (UI, [DueDateCalculator]).
 */
enum class DueDayRule(val dbValue: String, @StringRes val labelRes: Int) {
    DAY_15("day_15", R.string.loan_due_day_rule_day_15),
    LAST_BUSINESS_DAY("last_business_day", R.string.loan_due_day_rule_last_business_day);

    companion object {
        fun fromDbValue(value: String): DueDayRule = entries.find { it.dbValue == value } ?: DAY_15
    }
}

/** [Loan.dueDayRule] resolved to its enum — the raw string only exists for the Supabase mapping. */
val Loan.dueDayRuleEnum: DueDayRule
    get() = DueDayRule.fromDbValue(dueDayRule)

/**
 * Single source of truth for every payment due date in the app: a brand-new loan's first due
 * date, the next due date after a non-final payment, and the month a due-date extension can land
 * on all go through this object — never hand-pick or reimplement the calculation elsewhere.
 */
object DueDateCalculator {

    /** The first due date of a new loan: [rule]'s occurrence in the month right after [loanDate]. */
    fun firstDueDate(loanDate: LocalDate, rule: DueDayRule): LocalDate =
        dueDateForMonth(loanDate.plusMonths(1), rule)

    /** The next due date after [previousDueDate]: [rule]'s occurrence one month later. */
    fun nextDueDate(previousDueDate: LocalDate, rule: DueDayRule): LocalDate =
        dueDateForMonth(previousDueDate.plusMonths(1), rule)

    /**
     * [rule]'s occurrence in the same month as [monthAnchor] (only the year/month of [monthAnchor]
     * matters, not its day-of-month). `DAY_15` is always the 15th; `LAST_BUSINESS_DAY` is the
     * month's last calendar day, walked back to the closest Friday if that lands on a weekend —
     * no holiday calendar, weekends only, by design.
     */
    fun dueDateForMonth(monthAnchor: LocalDate, rule: DueDayRule): LocalDate {
        val yearMonth = YearMonth.from(monthAnchor)
        return when (rule) {
            DueDayRule.DAY_15 -> yearMonth.atDay(15)
            DueDayRule.LAST_BUSINESS_DAY -> {
                var day = yearMonth.atEndOfMonth()
                while (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) {
                    day = day.minusDays(1)
                }
                day
            }
        }
    }
}
