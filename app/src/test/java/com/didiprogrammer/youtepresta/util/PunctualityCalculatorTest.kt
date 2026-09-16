package com.didiprogrammer.youtepresta.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PunctualityCalculatorTest {

    @Test
    fun `fewer than 2 payments is not enough data`() {
        assertEquals(PunctualityStatus.NOT_ENOUGH_DATA, PunctualityCalculator.calculate(emptyList()))
        assertEquals(PunctualityStatus.NOT_ENOUGH_DATA, PunctualityCalculator.calculate(listOf(10L)))
    }

    @Test
    fun `average at or below zero is punctual`() {
        assertEquals(PunctualityStatus.PUNCTUAL, PunctualityCalculator.calculate(listOf(0L, 0L)))
        assertEquals(PunctualityStatus.PUNCTUAL, PunctualityCalculator.calculate(listOf(-3L, 1L)))
    }

    @Test
    fun `average between 1 and 5 days is occasionally late`() {
        assertEquals(PunctualityStatus.OCCASIONALLY_LATE, PunctualityCalculator.calculate(listOf(1L, 1L)))
        assertEquals(PunctualityStatus.OCCASIONALLY_LATE, PunctualityCalculator.calculate(listOf(5L, 5L)))
        assertEquals(PunctualityStatus.OCCASIONALLY_LATE, PunctualityCalculator.calculate(listOf(3L, 4L)))
    }

    @Test
    fun `average above 5 days is late`() {
        assertEquals(PunctualityStatus.LATE, PunctualityCalculator.calculate(listOf(6L, 6L)))
        assertEquals(PunctualityStatus.LATE, PunctualityCalculator.calculate(listOf(0L, 100L)))
    }

    @Test
    fun `daysLate is positive when paid after the due date and negative when paid early`() {
        assertEquals(
            10L,
            PunctualityCalculator.daysLate(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 15))
        )
        assertEquals(
            -5L,
            PunctualityCalculator.daysLate(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15))
        )
        assertEquals(
            0L,
            PunctualityCalculator.daysLate(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15))
        )
    }

    @Test
    fun `Juan example - one on time and one 10 days late payment averages to occasionally late boundary`() {
        // On-time payment (0 days late) and one 10 days late average to 5.0 exactly — the
        // inclusive upper edge of "occasionally late".
        val daysLate = listOf(
            PunctualityCalculator.daysLate(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)),
            PunctualityCalculator.daysLate(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 15))
        )
        assertEquals(PunctualityStatus.OCCASIONALLY_LATE, PunctualityCalculator.calculate(daysLate))
    }
}
