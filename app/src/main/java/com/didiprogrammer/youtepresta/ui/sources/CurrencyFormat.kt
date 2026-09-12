package com.didiprogrammer.youtepresta.ui.sources

import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val copFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

private val movementDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-CO"))

fun formatCop(amount: Double): String = copFormatter.format(amount)

fun formatMovementDate(isoTimestamp: String): String = try {
    OffsetDateTime.parse(isoTimestamp)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(movementDateFormatter)
} catch (e: DateTimeParseException) {
    isoTimestamp
}
