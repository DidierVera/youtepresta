package com.didiprogrammer.youtepresta.ui.loans

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.repository.InterestType
import com.didiprogrammer.youtepresta.ui.friends.FriendPicker
import com.didiprogrammer.youtepresta.ui.sources.formatCop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var interestType by remember { mutableStateOf(InterestType.NONE) }
    var interestValue by remember { mutableStateOf("") }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo préstamo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .padding(24.dp)
        ) {
            FriendPicker(onFriendSelected = { selectedFriend = it })
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
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = sourcesState) {
                is FundingSourcesLoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                is FundingSourcesLoadState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
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
                            label = { Text("Bolsillo de origen") },
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
                                    text = { Text("${source.name} (${formatCop(source.currentBalance)})") },
                                    onClick = {
                                        selectedSource = source
                                        sourceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val amountValue = amount.replace(",", ".").toDoubleOrNull()
                    val source = selectedSource
                    if (source != null && amountValue != null && amountValue > source.currentBalance) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El monto supera el saldo actual del bolsillo (${formatCop(source.currentBalance)}).",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = dueDate?.let { formatLoanDate(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                enabled = !isSaving,
                label = { Text("Fecha tentativa de pago") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Elegir fecha")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { showMore = !showMore }) {
                Text(if (showMore) "Mostrar menos" else "Mostrar más")
            }

            if (showMore) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Interés (solo informativo, no se calcula automáticamente)", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { interestType = InterestType.NONE },
                        enabled = !isSaving,
                        colors = if (interestType == InterestType.NONE) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ninguno")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { interestType = InterestType.FIXED },
                        enabled = !isSaving,
                        colors = if (interestType == InterestType.FIXED) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fijo")
                    }
                }

                if (interestType == InterestType.FIXED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = interestValue,
                        onValueChange = { interestValue = it },
                        label = { Text("Valor acordado (%, solo para recordar)") },
                        singleLine = true,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.createLoan(
                        friend = selectedFriend,
                        source = selectedSource,
                        amountInput = amount,
                        dueDate = dueDate,
                        interestType = interestType,
                        interestValueInput = interestValue
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar préstamo")
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
