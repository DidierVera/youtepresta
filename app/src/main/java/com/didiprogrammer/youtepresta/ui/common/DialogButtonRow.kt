package com.didiprogrammer.youtepresta.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.didiprogrammer.youtepresta.ui.theme.Spacing

/**
 * The two-button row used at the bottom of every confirmation dialog (Cancelar + a primary
 * action). Both buttons always take equal width via `weight(1f)` — pass it as the dialog's
 * `confirmButton` slot and leave `dismissButton` unset, since AlertDialog's own
 * confirmButton/dismissButton pair doesn't size them equally on its own.
 */
@Composable
fun DialogButtonRow(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancelar",
    enabled: Boolean = true,
    isLoading: Boolean = false,
    confirmColor: Color = Color.Unspecified
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        TextButton(onClick = onDismiss, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(dismissText)
        }
        TextButton(onClick = onConfirm, enabled = enabled, modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(confirmText, color = confirmColor)
            }
        }
    }
}
