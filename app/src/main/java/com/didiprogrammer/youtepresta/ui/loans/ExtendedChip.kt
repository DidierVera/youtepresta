package com.didiprogrammer.youtepresta.ui.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.ui.theme.Spacing

/**
 * Outlined "Con prórroga" chip — deliberately styled with a border instead of [StatusBadge]'s
 * filled background so the two can sit side by side without competing for attention. Shown
 * whenever [com.didiprogrammer.youtepresta.data.model.Loan.hasBeenExtended] is true; never
 * replaces the color status badge, which keeps reflecting due_date/status independently.
 */
@Composable
fun ExtendedChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Spacing.sm))
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(Spacing.sm)
            )
            .padding(horizontal = Spacing.sm + 2.dp, vertical = Spacing.xs + 2.dp)
    ) {
        Text(
            text = stringResource(R.string.loan_extended_chip),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
