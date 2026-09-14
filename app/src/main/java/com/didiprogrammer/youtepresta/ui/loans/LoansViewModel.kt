package com.didiprogrammer.youtepresta.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import com.didiprogrammer.youtepresta.data.repository.LoanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoanListItem(
    val loan: Loan,
    val friendName: String,
    val visualStatus: LoanVisualStatus
)

sealed interface LoansUiState {
    data object Loading : LoansUiState
    data class Error(val message: String) : LoansUiState
    data class Content(val loans: List<LoanListItem>) : LoansUiState
}

class LoansViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoansUiState>(LoansUiState.Loading)
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.value = LoansUiState.Loading
            try {
                // getLoans() already skips any individual row it can't decode. Friend names are
                // a separate, independent lookup: if it fails, show the loans anyway with a
                // placeholder name instead of losing the whole list over an unrelated fetch.
                val loans = LoanRepository.getLoans()
                val friendsById = try {
                    FriendRepository.getFriends().associateBy { it.id }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyMap()
                }
                val items = loans.mapNotNull { loan ->
                    runCatching {
                        LoanListItem(
                            loan = loan,
                            friendName = friendsById[loan.friendId]?.name ?: "Amigo",
                            visualStatus = visualStatusFor(loan)
                        )
                    }.getOrNull()
                }
                _uiState.value = LoansUiState.Content(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = LoansUiState.Error("No se pudieron cargar los préstamos.")
            }
        }
    }
}
