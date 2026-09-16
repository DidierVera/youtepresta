package com.didiprogrammer.youtepresta.ui.sources

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.SourceMovement
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.LoanRepository
import com.didiprogrammer.youtepresta.data.repository.MovementType
import com.didiprogrammer.youtepresta.data.repository.NonManualSourceMovementException
import com.didiprogrammer.youtepresta.data.repository.PaymentRepository
import com.didiprogrammer.youtepresta.data.repository.SourceHasActiveLoansException
import com.didiprogrammer.youtepresta.data.repository.SourceInUseException
import com.didiprogrammer.youtepresta.ui.common.SnackbarController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovementListItem(val movement: SourceMovement, val friendName: String?)

sealed interface FundingSourceDetailUiState {
    data object Loading : FundingSourceDetailUiState
    data class Error(@StringRes val messageRes: Int) : FundingSourceDetailUiState
    data class Content(
        val source: FundingSource,
        val movements: List<MovementListItem>,
        val hasAnyLoan: Boolean,
        val canArchive: Boolean
    ) : FundingSourceDetailUiState
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

    private val _movementError = MutableStateFlow<Int?>(null)
    val movementError: StateFlow<Int?> = _movementError.asStateFlow()

    private val _movementPendingDelete = MutableStateFlow<SourceMovement?>(null)
    val movementPendingDelete: StateFlow<SourceMovement?> = _movementPendingDelete.asStateFlow()

    private val _isDeletingMovement = MutableStateFlow(false)
    val isDeletingMovement: StateFlow<Boolean> = _isDeletingMovement.asStateFlow()

    private val _deleteMovementError = MutableStateFlow<Int?>(null)
    val deleteMovementError: StateFlow<Int?> = _deleteMovementError.asStateFlow()

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
                val archiveStatus = FundingSourceRepository.getArchiveStatus(id)
                _uiState.value = FundingSourceDetailUiState.Content(
                    source = source,
                    movements = withFriendNames(movements),
                    hasAnyLoan = archiveStatus.hasAnyLoan,
                    canArchive = archiveStatus.canArchive
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FundingSourceDetailUiState.Error(R.string.source_detail_load_error)
            }
        }
    }

    /**
     * Resolves which friend a movement belongs to: a loan movement points at the friend it was
     * lent to directly (`reference_loan_id`), a payment movement first has to look up which loan
     * that payment was for (`reference_payment_id` -> payment -> loan -> friend). Fetches every
     * loan/payment/friend once instead of a query per movement — cheap at this app's data volume
     * (see CLAUDE.md) and the same "fetch broad, join in memory" pattern used elsewhere
     * (e.g. [com.didiprogrammer.youtepresta.data.repository.FriendSummaryRepository]).
     */
    private suspend fun withFriendNames(movements: List<SourceMovement>): List<MovementListItem> {
        if (movements.none { it.referenceLoanId != null || it.referencePaymentId != null }) {
            return movements.map { MovementListItem(it, friendName = null) }
        }

        val friendNamesById = try {
            FriendRepository.getFriends().associate { it.id to it.name }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }
        val loanFriendIdsById = try {
            LoanRepository.getLoans().associate { it.id to it.friendId }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }
        val loanIdsByPaymentId = try {
            PaymentRepository.getAllPayments().associate { it.id to it.loanId }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }

        return movements.map { movement ->
            val loanId = movement.referenceLoanId ?: loanIdsByPaymentId[movement.referencePaymentId]
            val friendId = loanId?.let { loanFriendIdsById[it] }
            MovementListItem(movement, friendName = friendId?.let { friendNamesById[it] })
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
            _movementError.value = R.string.common_amount_invalid_error
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
                if (editing != null) {
                    SnackbarController.show(R.string.snackbar_movement_updated)
                }
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isSavingMovement.value = false
                _movementError.value = if (editing != null) {
                    R.string.movement_update_error
                } else {
                    R.string.movement_create_error
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
                _deleteMovementError.value = R.string.movement_delete_non_manual_error
            } catch (e: Exception) {
                _isDeletingMovement.value = false
                _deleteMovementError.value = R.string.movement_delete_generic_error
            }
        }
    }

    private val _isProcessingArchiveAction = MutableStateFlow(false)
    val isProcessingArchiveAction: StateFlow<Boolean> = _isProcessingArchiveAction.asStateFlow()

    private val _archiveActionError = MutableStateFlow<Int?>(null)
    val archiveActionError: StateFlow<Int?> = _archiveActionError.asStateFlow()

    fun archiveSource() {
        val id = sourceId ?: return
        viewModelScope.launch {
            _isProcessingArchiveAction.value = true
            _archiveActionError.value = null
            try {
                FundingSourceRepository.archiveSource(id)
                _isProcessingArchiveAction.value = false
                SnackbarController.show(R.string.snackbar_source_archived)
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SourceHasActiveLoansException) {
                _isProcessingArchiveAction.value = false
                _archiveActionError.value = R.string.source_archive_active_loans_error
            } catch (e: Exception) {
                _isProcessingArchiveAction.value = false
                _archiveActionError.value = R.string.source_archive_generic_error
            }
        }
    }

    fun unarchiveSource() {
        val id = sourceId ?: return
        viewModelScope.launch {
            _isProcessingArchiveAction.value = true
            _archiveActionError.value = null
            try {
                FundingSourceRepository.unarchiveSource(id)
                _isProcessingArchiveAction.value = false
                SnackbarController.show(R.string.snackbar_source_unarchived)
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isProcessingArchiveAction.value = false
                _archiveActionError.value = R.string.source_unarchive_error
            }
        }
    }

    private val _isDeleteSourceConfirmVisible = MutableStateFlow(false)
    val isDeleteSourceConfirmVisible: StateFlow<Boolean> = _isDeleteSourceConfirmVisible.asStateFlow()

    private val _isDeletingSource = MutableStateFlow(false)
    val isDeletingSource: StateFlow<Boolean> = _isDeletingSource.asStateFlow()

    private val _deleteSourceError = MutableStateFlow<Int?>(null)
    val deleteSourceError: StateFlow<Int?> = _deleteSourceError.asStateFlow()

    private val _sourceDeleted = MutableStateFlow(false)
    val sourceDeleted: StateFlow<Boolean> = _sourceDeleted.asStateFlow()

    fun confirmDeleteSource() {
        _deleteSourceError.value = null
        _isDeleteSourceConfirmVisible.value = true
    }

    fun dismissDeleteSourceConfirmation() {
        _isDeleteSourceConfirmVisible.value = false
        _deleteSourceError.value = null
    }

    fun deleteSource() {
        val id = sourceId ?: return
        viewModelScope.launch {
            _isDeletingSource.value = true
            _deleteSourceError.value = null
            try {
                FundingSourceRepository.deleteSourceIfUnused(id)
                _isDeletingSource.value = false
                _isDeleteSourceConfirmVisible.value = false
                _sourceDeleted.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: SourceInUseException) {
                _isDeletingSource.value = false
                _deleteSourceError.value = R.string.source_delete_in_use_error
            } catch (e: Exception) {
                _isDeletingSource.value = false
                _deleteSourceError.value = R.string.source_delete_generic_error
            }
        }
    }
}
