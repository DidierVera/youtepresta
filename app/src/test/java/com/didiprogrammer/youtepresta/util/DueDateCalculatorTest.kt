package com.didiprogrammer.youtepresta.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DueDateCalculatorTest {

    @Test
    fun `firstDueDate is day 15 of the month after the loan date`() {
        // The acceptance example from Fase 8: a loan on Sep 15, 2026 first falls due Oct 15, 2026.
        val loanDate = LocalDate.of(2026, 9, 15)
        assertEquals(
            LocalDate.of(2026, 10, 15),
            DueDateCalculator.firstDueDate(loanDate, DueDayRule.DAY_15)
        )
    }

    @Test
    fun `nextDueDate advances day 15 by exactly one month from the previous due date`() {
        assertEquals(
            LocalDate.of(2026, 11, 15),
            DueDateCalculator.nextDueDate(LocalDate.of(2026, 10, 15), DueDayRule.DAY_15)
        )
        assertEquals(
            LocalDate.of(2026, 12, 15),
            DueDateCalculator.nextDueDate(LocalDate.of(2026, 11, 15), DueDayRule.DAY_15)
        )
    }

    @Test
    fun `dueDateForMonth with day_15 ignores the anchor's own day`() {
        assertEquals(
            LocalDate.of(2026, 9, 15),
            DueDateCalculator.dueDateForMonth(LocalDate.of(2026, 9, 1), DueDayRule.DAY_15)
        )
        assertEquals(
            LocalDate.of(2026, 9, 15),
            DueDateCalculator.dueDateForMonth(LocalDate.of(2026, 9, 30), DueDayRule.DAY_15)
        )
    }

    @Test
    fun `last business day retreats from a Saturday month-end to Friday`() {
        // Oct 31, 2026 is a Saturday.
        assertEquals(
            LocalDate.of(2026, 10, 30),
            DueDateCalculator.dueDateForMonth(LocalDate.of(2026, 10, 1), DueDayRule.LAST_BUSINESS_DAY)
        )
    }

    @Test
    fun `last business day retreats two days from a Sunday month-end to Friday`() {
        // Jan 31, 2027 is a Sunday.
        assertEquals(
            LocalDate.of(2027, 1, 29),
            DueDateCalculator.dueDateForMonth(LocalDate.of(2027, 1, 1), DueDayRule.LAST_BUSINESS_DAY)
        )
    }

    @Test
    fun `last business day is the month-end itself when it's already a weekday`() {
        // Nov 30, 2026 is a Monday.
        assertEquals(
            LocalDate.of(2026, 11, 30),
            DueDateCalculator.dueDateForMonth(LocalDate.of(2026, 11, 1), DueDayRule.LAST_BUSINESS_DAY)
        )
    }

    @Test
    fun `DueDayRule maps to and from its Supabase db value`() {
        assertEquals(DueDayRule.DAY_15, DueDayRule.fromDbValue("day_15"))
        assertEquals(DueDayRule.LAST_BUSINESS_DAY, DueDayRule.fromDbValue("last_business_day"))
        assertEquals("day_15", DueDayRule.DAY_15.dbValue)
        assertEquals("last_business_day", DueDayRule.LAST_BUSINESS_DAY.dbValue)
    }
}
