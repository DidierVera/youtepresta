package com.didiprogrammer.youtepresta.ui.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.repository.InterestType
import com.didiprogrammer.youtepresta.data.repository.LoanStatus
import com.didiprogrammer.youtepresta.ui.payments.RegisterPaymentSheet
import com.didiprogrammer.youtepresta.ui.sources.formatCop
import com.didiprogrammer.youtepresta.ui.theme.Spacing

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle del préstamo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Reintentar")
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
                    onRegisterPayment = { viewModel.showPaymentSheet() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )

                if (isPaymentSheetVisible) {
                    RegisterPaymentSheet(
                        loanId = state.loan.id,
                        outstandingPrincipal = state.loan.outstandingPrincipal,
                        defaultSourceId = state.loan.sourceId,
                        onDismiss = { viewModel.dismissPaymentSheet() },
                        onPaymentRegistered = {
                            viewModel.dismissPaymentSheet()
                            viewModel.refresh()
                        }
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
    onRegisterPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = friend?.name ?: "Amigo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = visualStatus)
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            DetailRow(label = "Monto prestado", value = formatCop(loan.principalAmount))
            DetailRow(label = "Saldo pendiente", value = formatCop(loan.outstandingPrincipal))
            DetailRow(label = "Bolsillo de origen", value = source?.name ?: "—")
            DetailRow(label = "Fecha del préstamo", value = formatLoanDate(loan.loanDate))
            DetailRow(label = "Fecha tentativa de pago", value = loan.dueDate?.let { formatLoanDate(it) } ?: "—")

            if (loan.interestType == InterestType.FIXED.dbValue && loan.interestValue != null) {
                DetailRow(label = "Interés acordado (informativo)", value = "${loan.interestValue}")
            }

            if (loan.status != LoanStatus.PAID) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Button(onClick = onRegisterPayment, modifier = Modifier.fillMaxWidth()) {
                    Text("Registrar pago")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.md))

            Text("Historial de pagos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (payments.isEmpty()) {
            Text(
                text = "Aún no se ha registrado ningún pago.",
                modifier = Modifier.padding(horizontal = Spacing.lg)
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                items(payments, key = { it.payment.id }) { item ->
                    PaymentRow(item = item)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(item: PaymentListItem) {
    val payment = item.payment
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(text = formatLoanDate(payment.paymentDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (payment.principalPayment > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                PaymentAmountRow(label = "Capital", destination = item.principalDestinationName, amount = payment.principalPayment)
            }
            if (payment.interestPayment > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                PaymentAmountRow(
                    label = "Interés",
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
            Text(text = destination ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
