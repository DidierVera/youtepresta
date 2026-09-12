package com.didiprogrammer.youtepresta.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FundingSourcesUiState {
    data object Loading : FundingSourcesUiState
    data class Error(val message: String) : FundingSourcesUiState
    data class Content(val sources: List<FundingSource>) : FundingSourcesUiState
}

class FundingSourcesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FundingSourcesUiState>(FundingSourcesUiState.Loading)
    val uiState: StateFlow<FundingSourcesUiState> = _uiState.asStateFlow()

    private val _isCreateSheetVisible = MutableStateFlow(false)
    val isCreateSheetVisible: StateFlow<Boolean> = _isCreateSheetVisible.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    fun loadFundingSources() {
        viewModelScope.launch {
            _uiState.value = FundingSourcesUiState.Loading
            try {
                val sources = FundingSourceRepository.getFundingSources()
                _uiState.value = FundingSourcesUiState.Content(sources)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FundingSourcesUiState.Error("No se pudieron cargar los bolsillos.")
            }
        }
    }

    fun showCreateSheet() {
        _createError.value = null
        _isCreateSheetVisible.value = true
    }

    fun dismissCreateSheet() {
        _isCreateSheetVisible.value = false
        _createError.value = null
    }

    fun createFundingSource(name: String, initialBalanceInput: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _createError.value = "El nombre es obligatorio."
            return
        }

        val initialBalance = if (initialBalanceInput.isBlank()) {
            0.0
        } else {
            initialBalanceInput.replace(",", ".").toDoubleOrNull()
        }

        if (initialBalance == null || initialBalance < 0) {
            _createError.value = "El saldo inicial debe ser un número válido."
            return
        }

        viewModelScope.launch {
            _isCreating.value = true
            _createError.value = null
            try {
                FundingSourceRepository.createFundingSource(trimmedName, initialBalance)
                _isCreating.value = false
                _isCreateSheetVisible.value = false
                loadFundingSources()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isCreating.value = false
                _createError.value = "No se pudo crear el bolsillo."
            }
        }
    }
}
