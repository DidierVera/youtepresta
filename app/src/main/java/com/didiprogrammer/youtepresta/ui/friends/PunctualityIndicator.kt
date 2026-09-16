package com.didiprogrammer.youtepresta.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.didiprogrammer.youtepresta.ui.theme.Spacing
import com.didiprogrammer.youtepresta.ui.theme.statusColors
import com.didiprogrammer.youtepresta.util.PunctualityStatus

/**
 * Single mapping from [PunctualityStatus] to color, reused by both [PunctualityDot] (compact,
 * used in the friends list) and [PunctualityBadge] (with its label, used in FriendDetailScreen) —
 * reuses the same semantic success/warning roles as [com.didiprogrammer.youtepresta.ui.loans.StatusBadge]
 * so the two traffic-light systems in the app stay visually consistent.
 */
@Composable
private fun punctualityColor(status: PunctualityStatus): Color = when (status) {
    PunctualityStatus.PUNCTUAL -> MaterialTheme.statusColors.success
    PunctualityStatus.OCCASIONALLY_LATE -> MaterialTheme.statusColors.warning
    PunctualityStatus.LATE -> MaterialTheme.colorScheme.error
    PunctualityStatus.NOT_ENOUGH_DATA -> MaterialTheme.colorScheme.outline
}

/** Compact colored dot with an accessible label — no room for text in a list row. */
@Composable
fun PunctualityDot(status: PunctualityStatus, modifier: Modifier = Modifier) {
    val label = stringResource(status.labelRes)
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(punctualityColor(status))
            .semantics { contentDescription = label }
    )
}

/** Dot + visible label, used where there's room for a full explanation (FriendDetailScreen). */
@Composable
fun PunctualityBadge(status: PunctualityStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(punctualityColor(status))
        )
        Text(
            text = stringResource(status.labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
