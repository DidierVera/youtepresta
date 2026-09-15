package com.didiprogrammer.youtepresta.ui.sources

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FundingSourcesUiState {
    data object Loading : FundingSourcesUiState
    data class Error(@StringRes val messageRes: Int) : FundingSourcesUiState
    data class Content(val sources: List<FundingSource>) : FundingSourcesUiState
}

class FundingSourcesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FundingSourcesUiState>(FundingSourcesUiState.Loading)
    val uiState: StateFlow<FundingSourcesUiState> = _uiState.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _isCreateSheetVisible = MutableStateFlow(false)
    val isCreateSheetVisible: StateFlow<Boolean> = _isCreateSheetVisible.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createError = MutableStateFlow<Int?>(null)
    val createError: StateFlow<Int?> = _createError.asStateFlow()

    fun loadFundingSources() {
        viewModelScope.launch {
            _uiState.value = FundingSourcesUiState.Loading
            try {
                val sources = if (_showArchived.value) {
                    FundingSourceRepository.getFundingSources(includeArchived = true).filter { it.isArchived }
                } else {
                    FundingSourceRepository.getFundingSources()
                }
                _uiState.value = FundingSourcesUiState.Content(sources)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FundingSourcesUiState.Error(R.string.sources_load_error)
            }
        }
    }

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
        loadFundingSources()
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
            _createError.value = R.string.common_name_required_error
            return
        }

        val initialBalance = if (initialBalanceInput.isBlank()) {
            0.0
        } else {
            initialBalanceInput.replace(",", ".").toDoubleOrNull()
        }

        if (initialBalance == null || initialBalance < 0) {
            _createError.value = R.string.source_initial_balance_invalid_error
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
                _createError.value = R.string.source_create_error
            }
        }
    }
}
