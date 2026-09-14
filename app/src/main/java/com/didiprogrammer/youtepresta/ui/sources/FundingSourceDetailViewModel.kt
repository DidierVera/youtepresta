package com.didiprogrammer.youtepresta.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.SourceMovement
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.MovementType
import com.didiprogrammer.youtepresta.data.repository.NonManualSourceMovementException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FundingSourceDetailUiState {
    data object Loading : FundingSourceDetailUiState
    data class Error(val message: String) : FundingSourceDetailUiState
    data class Content(val source: FundingSource, val movements: List<SourceMovement>) : FundingSourceDetailUiState
}

class FundingSourceDetailViewModel : ViewModel() {

    private var sourceId: String? = null

    private val _uiState = MutableStateFlow<FundingSourceDetailUiState>(FundingSourceDetailUiState.Loading)
    val uiState: StateFlow<FundingSourceDetailUiState> = _uiState.asStateFlow()

    private val _isMovementSheetVisible = MutableStateFlow(false)
    val isMovementSheetVisible: StateFlow<Boolean> = _isMovementSheetVisible.asStateFlow()

    private val _editingMovement = MutableStateFlow<SourceMovement?>(null)
    val editingMovement: StateFlow<SourceMovement?> = _editingMovement.asStateFlow()

    private val _isSavingMovement = MutableStateFlow(false)
    val isSavingMovement: StateFlow<Boolean> = _isSavingMovement.asStateFlow()

    private val _movementError = MutableStateFlow<String?>(null)
    val movementError: StateFlow<String?> = _movementError.asStateFlow()

    private val _movementPendingDelete = MutableStateFlow<SourceMovement?>(null)
    val movementPendingDelete: StateFlow<SourceMovement?> = _movementPendingDelete.asStateFlow()

    private val _isDeletingMovement = MutableStateFlow(false)
    val isDeletingMovement: StateFlow<Boolean> = _isDeletingMovement.asStateFlow()

    private val _deleteMovementError = MutableStateFlow<String?>(null)
    val deleteMovementError: StateFlow<String?> = _deleteMovementError.asStateFlow()

    fun load(sourceId: String) {
        if (this.sourceId == sourceId) return
        this.sourceId = sourceId
        refresh()
    }

    fun refresh() {
        val id = sourceId ?: return
        viewModelScope.launch {
            _uiState.value = FundingSourceDetailUiState.Loading
            try {
                val source = FundingSourceRepository.getFundingSource(id)
                val movements = FundingSourceRepository.getSourceMovements(id)
                _uiState.value = FundingSourceDetailUiState.Content(source, movements)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FundingSourceDetailUiState.Error("No se pudo cargar el bolsillo.")
            }
        }
    }

    fun showAddMovementSheet() {
        _editingMovement.value = null
        _movementError.value = null
        _isMovementSheetVisible.value = true
    }

    fun showEditMovementSheet(movement: SourceMovement) {
        _editingMovement.value = movement
        _movementError.value = null
        _isMovementSheetVisible.value = true
    }

    fun dismissMovementSheet() {
        _isMovementSheetVisible.value = false
        _editingMovement.value = null
        _movementError.value = null
    }

    fun saveMovement(type: MovementType, amountInput: String, notes: String) {
        val id = sourceId ?: return
        val amount = amountInput.replace(",", ".").toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _movementError.value = "El monto debe ser mayor a 0."
            return
        }

        val editing = _editingMovement.value

        viewModelScope.launch {
            _isSavingMovement.value = true
            _movementError.value = null
            try {
                if (editing != null) {
                    FundingSourceRepository.updateMovement(
                        movementId = editing.id,
                        movementType = type,
                        amount = amount,
                        notes = notes.trim().ifBlank { null }
                    )
                } else {
                    FundingSourceRepository.addManualMovement(
                        sourceId = id,
                        movementType = type,
                        amount = amount,
                        notes = notes.trim().ifBlank { null }
                    )
                }
                _isSavingMovement.value = false
                _isMovementSheetVisible.value = false
                _editingMovement.value = null
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isSavingMovement.value = false
                _movementError.value = if (editing != null) {
                    "No se pudo actualizar el movimiento."
                } else {
                    "No se pudo guardar el movimiento."
                }
            }
        }
    }

    fun confirmDeleteMovement(movement: SourceMovement) {
        _deleteMovementError.value = null
        _movementPendingDelete.value = movement
    }

    fun dismissDeleteMovementConfirmation() {
        _movementPendingDelete.value = null
        _deleteMovementError.value = null
    }

    fun deleteMovement() {
        val movement = _movementPendingDelete.value ?: return

        viewModelScope.launch {
            _isDeletingMovement.value = true
            _deleteMovementError.value = null
            try {
                FundingSourceRepository.deleteMovement(movement.id)
                _isDeletingMovement.value = false
                _movementPendingDelete.value = null
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: NonManualSourceMovementException) {
                _isDeletingMovement.value = false
                _deleteMovementError.value = "Este movimiento viene de un préstamo o un pago y no se puede eliminar aquí."
            } catch (e: Exception) {
                _isDeletingMovement.value = false
                _deleteMovementError.value = "No se pudo eliminar el movimiento."
            }
        }
    }
}
