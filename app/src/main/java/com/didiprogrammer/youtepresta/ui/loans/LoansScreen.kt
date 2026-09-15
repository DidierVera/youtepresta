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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.ui.sources.formatCop
import com.didiprogrammer.youtepresta.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    onBack: () -> Unit,
    onNewLoan: () -> Unit,
    onLoanClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoansViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadLoans() }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loans_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewLoan) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.loans_new_fab))
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is LoansUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoansUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(state.messageRes), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Button(onClick = { viewModel.loadLoans() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is LoansUiState.Content -> {
                if (state.loans.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.loans_empty_state))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(Spacing.md)
                    ) {
                        items(state.loans, key = { it.loan.id }) { item ->
                            LoanRow(item = item, onClick = { onLoanClick(item.loan.id) })
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanRow(item: LoanListItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.friendName ?: stringResource(R.string.common_unknown_friend),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                item.loan.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.loan_due_date_label, formatLoanDate(dueDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCop(item.loan.principalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.loan.hasBeenExtended) {
                        ExtendedChip()
                    }
                    StatusBadge(status = item.visualStatus)
                }
            }
        }
    }
}
