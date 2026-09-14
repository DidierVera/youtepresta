package com.didiprogrammer.youtepresta.ui.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.didiprogrammer.youtepresta.ui.theme.Spacing
import com.didiprogrammer.youtepresta.ui.theme.statusColors

/**
 * The loan status pill shown in both [LoansScreen] and [LoanDetailScreen] — single source of
 * truth so the two never drift into different colors for the same status. Reuses the theme's own
 * error/errorContainer for "Atrasado" and onSurfaceVariant/surfaceVariant for "Pagado"; "Al día"
 * and "Vence hoy" use the custom success/warning roles from [MaterialTheme.statusColors].
 */
@Composable
fun StatusBadge(status: LoanVisualStatus, modifier: Modifier = Modifier) {
    val (contentColor, containerColor) = when (status) {
        LoanVisualStatus.PAID ->
            MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
        LoanVisualStatus.OVERDUE ->
            MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
        LoanVisualStatus.DUE_TODAY ->
            MaterialTheme.statusColors.onWarningContainer to MaterialTheme.statusColors.warningContainer
        LoanVisualStatus.ON_TRACK ->
            MaterialTheme.statusColors.onSuccessContainer to MaterialTheme.statusColors.successContainer
    }

    Box(
        modifier = modifier
            .background(color = containerColor, shape = RoundedCornerShape(Spacing.sm))
            .padding(horizontal = Spacing.sm + 2.dp, vertical = Spacing.xs + 2.dp)
    ) {
        Text(
            text = status.label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
