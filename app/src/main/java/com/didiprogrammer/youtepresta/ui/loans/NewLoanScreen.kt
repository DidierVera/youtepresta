package com.didiprogrammer.youtepresta.ui.loans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.ui.friends.FriendPicker
import com.didiprogrammer.youtepresta.ui.sources.formatCop
import com.didiprogrammer.youtepresta.ui.theme.Spacing
import com.didiprogrammer.youtepresta.util.DueDateCalculator
import com.didiprogrammer.youtepresta.util.DueDayRule
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLoanScreen(
    onBack: () -> Unit,
    onLoanSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewLoanViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadFundingSources() }

    val sourcesState by viewModel.sourcesState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val loanCreated by viewModel.loanCreated.collectAsState()

    LaunchedEffect(loanCreated) {
        if (loanCreated) onLoanSaved()
    }

    var selectedFriend by remember { mutableStateOf<Friend?>(null) }
    var selectedSource by remember { mutableStateOf<FundingSource?>(null) }
    var amount by remember { mutableStateOf("") }
    var dueDayRule by remember { mutableStateOf(DueDayRule.DAY_15) }
    var monthlyInterestRatePercent by remember { mutableStateOf("") }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    val amountValue = amount.replace(",", ".").toDoubleOrNull()
    val isAmountValid = amountValue != null && amountValue > 0
    val isAmountError = amount.isNotBlank() && !isAmountValid

    val ratePercentValue = monthlyInterestRatePercent.replace(",", ".").toDoubleOrNull()
    val isRateValid = ratePercentValue != null && ratePercentValue >= 0
    val isRateError = monthlyInterestRatePercent.isNotBlank() && !isRateValid

    val firstDueDate = remember(dueDayRule) { DueDateCalculator.firstDueDate(LocalDate.now(), dueDayRule) }

    val isFormValid = selectedFriend != null &&
        selectedSource != null &&
        isAmountValid &&
        isRateValid

    // Absorb the system back gesture while a save is in flight so leaving the screen can't
    // cancel the network call partway through (see LoanSourceMovementException for what a
    // half-finished loan+movement write looks like).
    BackHandler(enabled = isSaving) {}

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_new_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (!isSaving) onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg)
        ) {
            FriendPicker(onFriendSelected = { selectedFriend = it })
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.common_amount_label)) },
                singleLine = true,
                enabled = !isSaving,
                isError = isAmountError,
                supportingText = {
                    if (isAmountError) {
                        Text(stringResource(R.string.common_amount_invalid_error))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            when (val state = sourcesState) {
                is FundingSourcesLoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                is FundingSourcesLoadState.Error -> {
                    Text(stringResource(state.messageRes), color = MaterialTheme.colorScheme.error)
                }
                is FundingSourcesLoadState.Content -> {
                    ExposedDropdownMenuBox(
                        expanded = sourceDropdownExpanded,
                        onExpandedChange = { sourceDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSource?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isSaving,
                            label = { Text(stringResource(R.string.loan_source_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = sourceDropdownExpanded,
                            onDismissRequest = { sourceDropdownExpanded = false }
                        ) {
                            state.sources.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_source_with_balance, source.name, formatCop(source.currentBalance))) },
                                    onClick = {
                                        selectedSource = source
                                        sourceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val source = selectedSource
                    if (source != null && amountValue != null && amountValue > source.currentBalance) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = stringResource(R.string.loan_amount_exceeds_balance_error, formatCop(source.currentBalance)),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            Text(stringResource(R.string.loan_due_day_rule_label), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { dueDayRule = DueDayRule.DAY_15 },
                    enabled = !isSaving,
                    colors = if (dueDayRule == DueDayRule.DAY_15) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(DueDayRule.DAY_15.labelRes))
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Button(
                    onClick = { dueDayRule = DueDayRule.LAST_BUSINESS_DAY },
                    enabled = !isSaving,
                    colors = if (dueDayRule == DueDayRule.LAST_BUSINESS_DAY) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(DueDayRule.LAST_BUSINESS_DAY.labelRes))
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = monthlyInterestRatePercent,
                onValueChange = { monthlyInterestRatePercent = it },
                label = { Text(stringResource(R.string.loan_monthly_interest_rate_label)) },
                singleLine = true,
                enabled = !isSaving,
                isError = isRateError,
                supportingText = {
                    if (isRateError) {
                        Text(stringResource(R.string.loan_monthly_interest_rate_invalid_error))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.loan_first_due_date_preview, formatLoanDate(firstDueDate)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = {
                    viewModel.createLoan(
                        friend = selectedFriend,
                        source = selectedSource,
                        amountInput = amount,
                        dueDayRule = dueDayRule,
                        monthlyInterestRatePercentInput = monthlyInterestRatePercent
                    )
                },
                enabled = !isSaving && isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.loan_save_button))
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(text = stringResource(error!!), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
