package com.didiprogrammer.youtepresta.ui.sources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.data.model.FundingSource

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
    val isCreateSheetVisible by viewModel.isCreateSheetVisible.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Bolsillos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateSheet() }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear bolsillo")
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is FundingSourcesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FundingSourcesUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadFundingSources() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is FundingSourcesUiState.Content -> {
                if (state.sources.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aún no tienes bolsillos. Crea el primero con el botón +.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.sources, key = { it.id }) { source ->
                            FundingSourceRow(
                                source = source,
                                onClick = { onSourceClick(source.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = source.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = formatCop(source.currentBalance), style = MaterialTheme.typography.bodyLarge)
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

    ModalBottomSheet(
        onDismissRequest = { viewModel.dismissCreateSheet() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .imePadding()
        ) {
            Text("Nuevo bolsillo", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = initialBalance,
                onValueChange = { initialBalance = it },
                label = { Text("Saldo inicial (opcional)") },
                singleLine = true,
                enabled = !isCreating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.createFundingSource(name, initialBalance) },
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Crear")
                }
            }

            if (createError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = createError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
