package com.didiprogrammer.youtepresta.ui.sources

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.didiprogrammer.youtepresta.data.model.SourceMovement
import com.didiprogrammer.youtepresta.data.repository.MovementType
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingSourceDetailScreen(
    sourceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FundingSourceDetailViewModel = viewModel()
) {
    LaunchedEffect(sourceId) { viewModel.load(sourceId) }

    val uiState by viewModel.uiState.collectAsState()
    val isAddMovementSheetVisible by viewModel.isAddMovementSheetVisible.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle del bolsillo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is FundingSourceDetailUiState.Content) {
                FloatingActionButton(onClick = { viewModel.showAddMovementSheet() }) {
                    Text("+")
                }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is FundingSourceDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FundingSourceDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is FundingSourceDetailUiState.Content -> {
                FundingSourceDetailContent(
                    source = state.source,
                    movements = state.movements,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (isAddMovementSheetVisible) {
        AddMovementSheet(viewModel = viewModel)
    }
}

@Composable
private fun FundingSourceDetailContent(
    source: FundingSource,
    movements: List<SourceMovement>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = source.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = formatCop(source.currentBalance), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider()

        if (movements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Este bolsillo todavía no tiene movimientos.")
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(movements, key = { it.id }) { movement ->
                    MovementRow(movement)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MovementRow(movement: SourceMovement) {
    val isIncome = movement.movementType == MovementType.INCOME.dbValue
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = formatMovementDate(movement.createdAt), style = MaterialTheme.typography.bodySmall)
                if (!movement.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = movement.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                text = "${if (isIncome) "+" else "-"}${formatCop(abs(movement.amount))}",
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMovementSheet(viewModel: FundingSourceDetailViewModel) {
    val sheetState = rememberModalBottomSheetState()
    val isSaving by viewModel.isSavingMovement.collectAsState()
    val movementError by viewModel.movementError.collectAsState()

    var selectedType by remember { mutableStateOf(MovementType.INCOME) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { viewModel.dismissAddMovementSheet() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .imePadding()
        ) {
            Text("Agregar dinero", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { selectedType = MovementType.INCOME },
                    enabled = !isSaving,
                    colors = if (selectedType == MovementType.INCOME) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Agregar")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { selectedType = MovementType.OUTFLOW },
                    enabled = !isSaving,
                    colors = if (selectedType == MovementType.OUTFLOW) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retirar")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Nota (opcional)") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.addMovement(selectedType, amount, notes) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }

            if (movementError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = movementError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
