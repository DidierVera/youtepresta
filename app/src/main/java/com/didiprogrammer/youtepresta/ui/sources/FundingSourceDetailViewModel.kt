package com.didiprogrammer.youtepresta.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.SourceMovement
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.MovementType
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

    private val _isAddMovementSheetVisible = MutableStateFlow(false)
    val isAddMovementSheetVisible: StateFlow<Boolean> = _isAddMovementSheetVisible.asStateFlow()

    private val _isSavingMovement = MutableStateFlow(false)
    val isSavingMovement: StateFlow<Boolean> = _isSavingMovement.asStateFlow()

    private val _movementError = MutableStateFlow<String?>(null)
    val movementError: StateFlow<String?> = _movementError.asStateFlow()

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
        _movementError.value = null
        _isAddMovementSheetVisible.value = true
    }

    fun dismissAddMovementSheet() {
        _isAddMovementSheetVisible.value = false
        _movementError.value = null
    }

    fun addMovement(type: MovementType, amountInput: String, notes: String) {
        val id = sourceId ?: return
        val amount = amountInput.replace(",", ".").toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _movementError.value = "El monto debe ser mayor a 0."
            return
        }

        viewModelScope.launch {
            _isSavingMovement.value = true
            _movementError.value = null
            try {
                FundingSourceRepository.addManualMovement(
                    sourceId = id,
                    movementType = type,
                    amount = amount,
                    notes = notes.trim().ifBlank { null }
                )
                _isSavingMovement.value = false
                _isAddMovementSheetVisible.value = false
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isSavingMovement.value = false
                _movementError.value = "No se pudo guardar el movimiento."
            }
        }
    }
}
