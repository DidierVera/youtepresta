package com.didiprogrammer.youtepresta.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingSourcesScreen(
    onBack: () -> Unit,
    onSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FundingSourcesViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadFundingSources() }

    val uiState by viewModel.uiState.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val isCreateSheetVisible by viewModel.isCreateSheetVisible.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sources_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateSheet() }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.sources_create_fab))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TextButton(
                onClick = { viewModel.toggleShowArchived() },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = Spacing.md)
            ) {
                Text(stringResource(if (showArchived) R.string.sources_view_active else R.string.sources_view_archived))
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val state = uiState) {
                    is FundingSourcesUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is FundingSourcesUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(state.messageRes), color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Button(onClick = { viewModel.loadFundingSources() }) {
                                    Text(stringResource(R.string.common_retry))
                                }
                            }
                        }
                    }
                    is FundingSourcesUiState.Content -> {
                        if (state.sources.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(
                                        if (showArchived) R.string.sources_archived_empty_state else R.string.sources_empty_state
                                    )
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(Spacing.md)
                            ) {
                                items(state.sources, key = { it.id }) { source ->
                                    FundingSourceRow(
                                        source = source,
                                        onClick = { onSourceClick(source.id) }
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreateSheetVisible) {
        CreateFundingSourceSheet(viewModel = viewModel)
    }
}

@Composable
private fun FundingSourceRow(source: FundingSource, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = source.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (source.isArchived) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.source_archived_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(Spacing.sm)
                            )
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    )
                }
            }
            Text(text = formatCop(source.currentBalance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFundingSourceSheet(viewModel: FundingSourcesViewModel) {
    val sheetState = rememberModalBottomSheetState()
    val isCreating by viewModel.isCreating.collectAsState()
    val createError by viewModel.createError.collectAsState()

    var name by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }

    val parsedBalance = if (initialBalance.isBlank()) 0.0 else initialBalance.replace(",", ".").toDoubleOrNull()
    val isBalanceValid = parsedBalance != null && parsedBalance >= 0
    val isFormValid = name.isNotBlank() && isBalanceValid

    ModalBottomSheet(
        onDismissRequest = { if (!isCreating) viewModel.dismissCreateSheet() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg)
                .imePadding()
        ) {
            Text(stringResource(R.string.source_create_sheet_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.common_name_label)) },
                singleLine = true,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = initialBalance,
                onValueChange = { initialBalance = it },
                label = { Text(stringResource(R.string.source_initial_balance_label)) },
                singleLine = true,
                enabled = !isCreating,
                isError = !isBalanceValid,
                supportingText = {
                    if (!isBalanceValid) {
                        Text(stringResource(R.string.source_initial_balance_invalid_error))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            Button(
                onClick = { viewModel.createFundingSource(name, initialBalance) },
                enabled = !isCreating && isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.common_create))
                }
            }

            if (createError != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(text = stringResource(createError!!), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
