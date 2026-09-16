package com.didiprogrammer.youtepresta.ui.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.model.LoanDueDateChange
import com.didiprogrammer.youtepresta.data.repository.LoanStatus
import com.didiprogrammer.youtepresta.ui.common.DialogButtonRow
import com.didiprogrammer.youtepresta.ui.payments.RegisterPaymentSheet
import com.didiprogrammer.youtepresta.ui.sources.formatCop
import com.didiprogrammer.youtepresta.ui.sources.formatPercent
import com.didiprogrammer.youtepresta.ui.theme.Spacing
import com.didiprogrammer.youtepresta.util.DueDateCalculator
import com.didiprogrammer.youtepresta.util.DueDayRule
import com.didiprogrammer.youtepresta.util.dueDayRuleEnum
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanDetailViewModel = viewModel()
) {
    LaunchedEffect(loanId) { viewModel.load(loanId) }

    val uiState by viewModel.uiState.collectAsState()
    val isPaymentSheetVisible by viewModel.isPaymentSheetVisible.collectAsState()
    val isExtendDialogVisible by viewModel.isExtendDialogVisible.collectAsState()
    val isExtendingDueDate by viewModel.isExtendingDueDate.collectAsState()
    val extendError by viewModel.extendError.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is LoanDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoanDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(state.messageRes), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Button(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is LoanDetailUiState.Content -> {
                LoanDetailContent(
                    loan = state.loan,
                    friend = state.friend,
                    source = state.source,
                    visualStatus = state.visualStatus,
                    payments = state.payments,
                    dueDateChanges = state.dueDateChanges,
                    onRegisterPayment = { viewModel.showPaymentSheet() },
                    onRegisterExtension = { viewModel.showExtendDialog() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )

                if (isPaymentSheetVisible) {
                    RegisterPaymentSheet(
                        loanId = state.loan.id,
                        outstandingPrincipal = state.loan.outstandingPrincipal,
                        monthlyInterestRate = state.loan.monthlyInterestRate,
                        defaultSourceId = state.loan.sourceId,
                        onDismiss = { viewModel.dismissPaymentSheet() },
                        onPaymentRegistered = {
                            viewModel.dismissPaymentSheet()
                            viewModel.refresh()
                        }
                    )
                }

                if (isExtendDialogVisible) {
                    ExtendDueDateDialog(
                        currentDueDate = state.loan.dueDate,
                        dueDayRule = state.loan.dueDayRuleEnum,
                        isSaving = isExtendingDueDate,
                        errorRes = extendError,
                        onDismiss = { viewModel.dismissExtendDialog() },
                        onConfirm = { newDueDate, notes -> viewModel.extendDueDate(newDueDate, notes) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanDetailContent(
    loan: Loan,
    friend: Friend?,
    source: FundingSource?,
    visualStatus: LoanVisualStatus,
    payments: List<PaymentListItem>,
    dueDateChanges: List<LoanDueDateChange>,
    onRegisterPayment: () -> Unit,
    onRegisterExtension: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = friend?.name ?: stringResource(R.string.common_unknown_friend),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (loan.hasBeenExtended) {
                        ExtendedChip()
                    }
                    StatusBadge(status = visualStatus)
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            val emptyValue = stringResource(R.string.common_empty_value)
            DetailRow(label = stringResource(R.string.loan_principal_amount_label), value = formatCop(loan.principalAmount))
            DetailRow(label = stringResource(R.string.loan_outstanding_balance_label), value = formatCop(loan.outstandingPrincipal))
            DetailRow(label = stringResource(R.string.loan_source_label), value = source?.name ?: emptyValue)
            DetailRow(label = stringResource(R.string.loan_date_label), value = formatLoanDate(loan.loanDate))
            DetailRow(label = stringResource(R.string.loan_due_date_field_label), value = loan.dueDate?.let { formatLoanDate(it) } ?: emptyValue)
            DetailRow(label = stringResource(R.string.loan_due_day_rule_label), value = stringResource(loan.dueDayRuleEnum.labelRes))
            DetailRow(label = stringResource(R.string.loan_monthly_interest_rate_label), value = formatPercent(loan.monthlyInterestRate))

            if (loan.status != LoanStatus.PAID) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Button(onClick = onRegisterPayment, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.payment_register_action))
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedButton(onClick = onRegisterExtension, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.loan_extend_action))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.md))

            Text(stringResource(R.string.loan_payments_history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (payments.isEmpty()) {
            Text(text = stringResource(R.string.loan_payments_empty_state))
        } else {
            payments.forEach { item ->
                PaymentRow(item = item)
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }

        if (dueDateChanges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.loan_due_date_changes_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            dueDateChanges.forEach { change ->
                DueDateChangeRow(change = change)
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
private fun DueDateChangeRow(change: LoanDueDateChange) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = formatLoanDate(change.createdAt.take(10)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(
                    R.string.loan_due_date_change_dates,
                    formatLoanDate(change.previousDueDate),
                    formatLoanDate(change.newDueDate)
                ),
                fontWeight = FontWeight.Bold
            )
            if (!change.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(text = change.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val monthOptionFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtendDueDateDialog(
    currentDueDate: String?,
    dueDayRule: DueDayRule,
    isSaving: Boolean,
    errorRes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (newDueDate: String, notes: String?) -> Unit
) {
    val currentDue = remember(currentDueDate) {
        currentDueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    }
    // Only future months relative to the loan's current due date — the exact day within that
    // month is never picked by hand, it's always DueDateCalculator's call per the loan's rule.
    val monthOptions = remember(currentDue, dueDayRule) {
        (1..12L).map { offset -> DueDateCalculator.dueDateForMonth(currentDue.plusMonths(offset), dueDayRule) }
    }
    var selectedDate by remember(monthOptions) { mutableStateOf(monthOptions.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.loan_extend_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDate?.format(monthOptionFormatter)?.replaceFirstChar { it.uppercase() }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSaving,
                        label = { Text(stringResource(R.string.loan_extend_new_due_date_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        monthOptions.forEach { date ->
                            DropdownMenuItem(
                                text = { Text(date.format(monthOptionFormatter).replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedDate = date
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    enabled = !isSaving,
                    label = { Text(stringResource(R.string.loan_extend_notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorRes != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(text = stringResource(errorRes), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            DialogButtonRow(
                onDismiss = onDismiss,
                onConfirm = {
                    val date = selectedDate ?: return@DialogButtonRow
                    onConfirm(date.toString(), notes)
                },
                confirmText = stringResource(R.string.loan_extend_save_button),
                enabled = !isSaving && selectedDate != null,
                isLoading = isSaving
            )
        }
    )
}

@Composable
private fun PaymentRow(item: PaymentListItem) {
    val payment = item.payment
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(text = formatLoanDate(payment.paymentDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (payment.principalPayment > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                PaymentAmountRow(
                    label = stringResource(R.string.loan_payment_capital_label),
                    destination = item.principalDestinationName,
                    amount = payment.principalPayment
                )
            }
            if (payment.interestPayment > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                PaymentAmountRow(
                    label = stringResource(R.string.loan_payment_interest_label),
                    destination = item.interestDestinationName ?: item.principalDestinationName,
                    amount = payment.interestPayment
                )
            }
        }
    }
}

@Composable
private fun PaymentAmountRow(label: String, destination: String?, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = destination ?: stringResource(R.string.common_empty_value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = formatCop(amount), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
}
