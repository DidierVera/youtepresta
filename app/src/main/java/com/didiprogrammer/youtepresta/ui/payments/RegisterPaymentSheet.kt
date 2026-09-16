package com.didiprogrammer.youtepresta.ui.payments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.ui.sources.formatCop
import com.didiprogrammer.youtepresta.ui.theme.Spacing
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPaymentSheet(
    loanId: String,
    outstandingPrincipal: Double,
    monthlyInterestRate: Double,
    defaultSourceId: String?,
    onDismiss: () -> Unit,
    onPaymentRegistered: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterPaymentViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadFundingSources() }

    val fundingSources by viewModel.fundingSources.collectAsState()
    val isLoadingSources by viewModel.isLoadingSources.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val paymentRegistered by viewModel.paymentRegistered.collectAsState()

    LaunchedEffect(paymentRegistered) {
        if (paymentRegistered) onPaymentRegistered()
    }

    var principalPayment by remember { mutableStateOf("") }
    var principalDestination by remember { mutableStateOf<FundingSource?>(null) }
    var interestDestination by remember { mutableStateOf<FundingSource?>(null) }
    var principalDropdownExpanded by remember { mutableStateOf(false) }
    var interestDropdownExpanded by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    // Never user-entered (see CLAUDE.md): the monthly rate applies to the outstanding principal
    // as it stands right before this payment, not the loan's original amount.
    val interestPayment = round(outstandingPrincipal * monthlyInterestRate)

    val principalValue = principalPayment.replace(",", ".").toDoubleOrNull()
    val isPrincipalValid = principalValue != null && principalValue > 0
    val isPrincipalError = principalPayment.isNotBlank() && !isPrincipalValid
    val isFormValid = isPrincipalValid && principalDestination != null

    LaunchedEffect(fundingSources) {
        if (principalDestination == null && fundingSources.isNotEmpty()) {
            principalDestination = fundingSources.find { it.id == defaultSourceId } ?: fundingSources.first()
        }
    }

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg)
                .imePadding()
        ) {
            Text(stringResource(R.string.payment_register_action), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.payment_outstanding_balance_label, formatCop(outstandingPrincipal)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = principalPayment,
                onValueChange = { principalPayment = it },
                label = { Text(stringResource(R.string.payment_principal_label)) },
                singleLine = true,
                enabled = !isSaving,
                isError = isPrincipalError,
                supportingText = {
                    if (isPrincipalError) {
                        Text(stringResource(R.string.common_amount_invalid_error))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = formatCop(interestPayment),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.payment_interest_calculated_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            val total = (principalValue ?: 0.0) + interestPayment
            Text(text = stringResource(R.string.payment_total_label, formatCop(total)), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Spacing.md))

            if (isLoadingSources) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                ExposedDropdownMenuBox(
                    expanded = principalDropdownExpanded,
                    onExpandedChange = { principalDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = principalDestination?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSaving,
                        label = { Text(stringResource(R.string.payment_principal_destination_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = principalDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = principalDropdownExpanded,
                        onDismissRequest = { principalDropdownExpanded = false }
                    ) {
                        fundingSources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_source_with_balance, source.name, formatCop(source.currentBalance))) },
                                onClick = {
                                    principalDestination = source
                                    principalDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))

                TextButton(onClick = { showMore = !showMore }) {
                    Text(stringResource(if (showMore) R.string.common_show_less else R.string.common_show_more))
                }

                if (showMore) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = stringResource(R.string.payment_interest_destination_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    ExposedDropdownMenuBox(
                        expanded = interestDropdownExpanded,
                        onExpandedChange = { interestDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = interestDestination?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isSaving,
                            label = { Text(stringResource(R.string.payment_interest_destination_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = interestDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = interestDropdownExpanded,
                            onDismissRequest = { interestDropdownExpanded = false }
                        ) {
                            fundingSources.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_source_with_balance, source.name, formatCop(source.currentBalance))) },
                                    onClick = {
                                        interestDestination = source
                                        interestDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            Button(
                onClick = {
                    viewModel.registerPayment(
                        loanId = loanId,
                        principalPaymentInput = principalPayment,
                        interestPayment = interestPayment,
                        principalDestination = principalDestination,
                        interestDestination = interestDestination
                    )
                },
                enabled = !isSaving && isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.payment_register_action))
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(text = stringResource(error!!), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
