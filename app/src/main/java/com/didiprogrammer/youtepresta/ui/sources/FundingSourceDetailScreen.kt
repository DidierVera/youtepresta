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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import com.didiprogrammer.youtepresta.ui.common.DialogButtonRow
import com.didiprogrammer.youtepresta.ui.theme.Spacing
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
    val isMovementSheetVisible by viewModel.isMovementSheetVisible.collectAsState()
    val movementPendingDelete by viewModel.movementPendingDelete.collectAsState()

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
                        Spacer(modifier = Modifier.height(Spacing.sm))
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
                    onEditMovement = { viewModel.showEditMovementSheet(it) },
                    onDeleteMovement = { viewModel.confirmDeleteMovement(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (isMovementSheetVisible) {
        MovementSheet(viewModel = viewModel)
    }

    if (movementPendingDelete != null) {
        DeleteMovementDialog(viewModel = viewModel, movement = movementPendingDelete!!)
    }
}

@Composable
private fun FundingSourceDetailContent(
    source: FundingSource,
    movements: List<SourceMovement>,
    onEditMovement: (SourceMovement) -> Unit,
    onDeleteMovement: (SourceMovement) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(text = source.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(text = formatCop(source.currentBalance), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider()

        if (movements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Este bolsillo todavía no tiene movimientos.")
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(Spacing.md)) {
                items(movements, key = { it.id }) { movement ->
                    MovementRow(
                        movement = movement,
                        onEdit = { onEditMovement(movement) },
                        onDelete = { onDeleteMovement(movement) }
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun MovementRow(movement: SourceMovement, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isIncome = movement.movementType == MovementType.INCOME.dbValue
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val isManual = movement.referenceLoanId == null && movement.referencePaymentId == null
    val sourceLabel = when {
        movement.referenceLoanId != null -> "Préstamo"
        movement.referencePaymentId != null -> "Pago"
        else -> null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = formatMovementDate(movement.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!movement.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(text = movement.notes, style = MaterialTheme.typography.bodyMedium)
                }
                if (sourceLabel != null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = sourceLabel,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${formatCop(abs(movement.amount))}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                if (isManual) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar movimiento")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar movimiento")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovementSheet(viewModel: FundingSourceDetailViewModel) {
    val sheetState = rememberModalBottomSheetState()
    val isSaving by viewModel.isSavingMovement.collectAsState()
    val movementError by viewModel.movementError.collectAsState()
    val editingMovement by viewModel.editingMovement.collectAsState()

    val initialType = editingMovement?.let {
        if (it.movementType == MovementType.INCOME.dbValue) MovementType.INCOME else MovementType.OUTFLOW
    } ?: MovementType.INCOME

    var selectedType by remember(editingMovement) { mutableStateOf(initialType) }
    var amount by remember(editingMovement) {
        mutableStateOf(editingMovement?.amount?.let { formatPlainAmount(it) }.orEmpty())
    }
    var notes by remember(editingMovement) { mutableStateOf(editingMovement?.notes.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = { viewModel.dismissMovementSheet() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg)
                .imePadding()
        ) {
            Text(
                if (editingMovement != null) "Editar movimiento" else "Agregar dinero",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(Spacing.md))

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
                Spacer(modifier = Modifier.width(Spacing.sm))
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
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Nota (opcional)") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            Button(
                onClick = { viewModel.saveMovement(selectedType, amount, notes) },
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
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(text = movementError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DeleteMovementDialog(viewModel: FundingSourceDetailViewModel, movement: SourceMovement) {
    val isDeleting by viewModel.isDeletingMovement.collectAsState()
    val deleteError by viewModel.deleteMovementError.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.dismissDeleteMovementConfirmation() },
        title = { Text("Eliminar movimiento") },
        text = {
            Column {
                Text("¿Seguro que quieres eliminar este movimiento de ${formatCop(movement.amount)}? Esta acción no se puede deshacer.")
                if (deleteError != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(text = deleteError.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            DialogButtonRow(
                onDismiss = { viewModel.dismissDeleteMovementConfirmation() },
                onConfirm = { viewModel.deleteMovement() },
                confirmText = "Eliminar",
                enabled = !isDeleting,
                isLoading = isDeleting,
                confirmColor = MaterialTheme.colorScheme.error
            )
        }
    )
}
