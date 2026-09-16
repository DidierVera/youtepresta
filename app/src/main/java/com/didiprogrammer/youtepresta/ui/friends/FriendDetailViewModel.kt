package com.didiprogrammer.youtepresta.ui.friends

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.repository.FriendHasLoansException
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import com.didiprogrammer.youtepresta.data.repository.FriendSummary
import com.didiprogrammer.youtepresta.data.repository.FriendSummaryRepository
import com.didiprogrammer.youtepresta.ui.common.SnackbarController
import com.didiprogrammer.youtepresta.ui.loans.LoanListItem
import com.didiprogrammer.youtepresta.ui.loans.visualStatusFor
import com.didiprogrammer.youtepresta.util.PunctualityCalculator
import com.didiprogrammer.youtepresta.util.PunctualityStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val EMPTY_SUMMARY = FriendSummary(
    currentDebt = 0.0,
    activeLoanCount = 0,
    totalLoanedHistoric = 0.0,
    totalInterestCollected = 0.0,
    loans = emptyList(),
    payments = emptyList()
)

sealed interface FriendDetailUiState {
    data object Loading : FriendDetailUiState
    data class Error(@StringRes val messageRes: Int) : FriendDetailUiState
    data class Content(
        val friend: Friend,
        val summary: FriendSummary,
        val punctuality: PunctualityStatus,
        val loanItems: List<LoanListItem>
    ) : FriendDetailUiState
}

class FriendDetailViewModel : ViewModel() {

    private var friendId: String? = null

    private val _uiState = MutableStateFlow<FriendDetailUiState>(FriendDetailUiState.Loading)
    val uiState: StateFlow<FriendDetailUiState> = _uiState.asStateFlow()

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<Int?>(null)
    val saveError: StateFlow<Int?> = _saveError.asStateFlow()

    private val _isDeleteConfirmVisible = MutableStateFlow(false)
    val isDeleteConfirmVisible: StateFlow<Boolean> = _isDeleteConfirmVisible.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _deleteError = MutableStateFlow<Int?>(null)
    val deleteError: StateFlow<Int?> = _deleteError.asStateFlow()

    private val _friendDeleted = MutableStateFlow(false)
    val friendDeleted: StateFlow<Boolean> = _friendDeleted.asStateFlow()

    fun load(friendId: String) {
        if (this.friendId == friendId) return
        this.friendId = friendId
        refresh()
    }

    fun refresh() {
        val id = friendId ?: return
        viewModelScope.launch {
            _uiState.value = FriendDetailUiState.Loading
            try {
                val friend = FriendRepository.getFriends().find { it.id == id }
                    ?: throw NoSuchElementException("Friend $id not found")
                val summary = FriendSummaryRepository.getFriendSummaries()[id] ?: EMPTY_SUMMARY
                val punctuality = PunctualityCalculator.calculateForPayments(summary.payments)
                // Same order getLoans() already returns (due_date ascending) — no need to re-sort.
                val loanItems = summary.loans.map { loan ->
                    LoanListItem(loan = loan, friendName = friend.name, visualStatus = visualStatusFor(loan))
                }
                _uiState.value = FriendDetailUiState.Content(
                    friend = friend,
                    summary = summary,
                    punctuality = punctuality,
                    loanItems = loanItems
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FriendDetailUiState.Error(R.string.friend_detail_load_error)
            }
        }
    }

    fun showEditDialog() {
        _saveError.value = null
        _isDialogVisible.value = true
    }

    fun dismissDialog() {
        _isDialogVisible.value = false
        _saveError.value = null
    }

    fun saveFriend(name: String, phone: String, notes: String) {
        val id = friendId ?: return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _saveError.value = R.string.common_name_required_error
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                FriendRepository.updateFriend(
                    id = id,
                    name = trimmedName,
                    phone = phone.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null }
                )
                _isSaving.value = false
                _isDialogVisible.value = false
                SnackbarController.show(R.string.snackbar_friend_updated)
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isSaving.value = false
                _saveError.value = R.string.friend_update_error
            }
        }
    }

    fun confirmDelete() {
        _deleteError.value = null
        _isDeleteConfirmVisible.value = true
    }

    fun dismissDeleteConfirmation() {
        _isDeleteConfirmVisible.value = false
        _deleteError.value = null
    }

    fun deleteFriend() {
        val id = friendId ?: return
        viewModelScope.launch {
            _isDeleting.value = true
            _deleteError.value = null
            try {
                FriendRepository.deleteFriend(id)
                _isDeleting.value = false
                _isDeleteConfirmVisible.value = false
                _friendDeleted.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: FriendHasLoansException) {
                _isDeleting.value = false
                _deleteError.value = R.string.friend_has_loans_error
            } catch (e: Exception) {
                _isDeleting.value = false
                _deleteError.value = R.string.friend_delete_generic_error
            }
        }
    }
}
