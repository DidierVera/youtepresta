package com.didiprogrammer.youtepresta.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.model.Payment
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.LoanRepository
import com.didiprogrammer.youtepresta.data.repository.PaymentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentListItem(
    val payment: Payment,
    val principalDestinationName: String?,
    val interestDestinationName: String?
)

sealed interface LoanDetailUiState {
    data object Loading : LoanDetailUiState
    data class Error(val message: String) : LoanDetailUiState
    data class Content(
        val loan: Loan,
        val friend: Friend?,
        val source: FundingSource?,
        val visualStatus: LoanVisualStatus,
        val payments: List<PaymentListItem>
    ) : LoanDetailUiState
}

class LoanDetailViewModel : ViewModel() {

    private var loanId: String? = null

    private val _uiState = MutableStateFlow<LoanDetailUiState>(LoanDetailUiState.Loading)
    val uiState: StateFlow<LoanDetailUiState> = _uiState.asStateFlow()

    private val _isPaymentSheetVisible = MutableStateFlow(false)
    val isPaymentSheetVisible: StateFlow<Boolean> = _isPaymentSheetVisible.asStateFlow()

    fun load(loanId: String) {
        if (this.loanId == loanId) return
        this.loanId = loanId
        refresh()
    }

    fun refresh() {
        val id = loanId ?: return
        viewModelScope.launch {
            _uiState.value = LoanDetailUiState.Loading
            try {
                val loan = LoanRepository.getLoan(id)
                val friend = FriendRepository.getFriends().find { it.id == loan.friendId }
                val source = FundingSourceRepository.getFundingSource(loan.sourceId)
                val payments = PaymentRepository.getPayments(id)
                val sourcesById = FundingSourceRepository.getFundingSources().associateBy { it.id }
                val paymentItems = payments.map { payment ->
                    PaymentListItem(
                        payment = payment,
                        principalDestinationName = sourcesById[payment.principalDestinationSourceId]?.name,
                        interestDestinationName = payment.interestDestinationSourceId
                            ?.let { sourcesById[it]?.name }
                    )
                }
                _uiState.value = LoanDetailUiState.Content(
                    loan = loan,
                    friend = friend,
                    source = source,
                    visualStatus = visualStatusFor(loan),
                    payments = paymentItems
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = LoanDetailUiState.Error("No se pudo cargar el préstamo.")
            }
        }
    }

    fun showPaymentSheet() {
        _isPaymentSheetVisible.value = true
    }

    fun dismissPaymentSheet() {
        _isPaymentSheetVisible.value = false
    }
}
