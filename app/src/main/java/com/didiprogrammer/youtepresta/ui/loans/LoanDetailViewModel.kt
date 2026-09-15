package com.didiprogrammer.youtepresta.ui.loans

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.model.Loan
import com.didiprogrammer.youtepresta.data.model.LoanDueDateChange
import com.didiprogrammer.youtepresta.data.model.Payment
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.LoanRepository
import com.didiprogrammer.youtepresta.data.repository.PaymentRepository
import com.didiprogrammer.youtepresta.ui.common.SnackbarController
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
    data class Error(@StringRes val messageRes: Int) : LoanDetailUiState
    data class Content(
        val loan: Loan,
        val friend: Friend?,
        val source: FundingSource?,
        val visualStatus: LoanVisualStatus,
        val payments: List<PaymentListItem>,
        val dueDateChanges: List<LoanDueDateChange>
    ) : LoanDetailUiState
}

class LoanDetailViewModel : ViewModel() {

    private var loanId: String? = null

    private val _uiState = MutableStateFlow<LoanDetailUiState>(LoanDetailUiState.Loading)
    val uiState: StateFlow<LoanDetailUiState> = _uiState.asStateFlow()

    private val _isPaymentSheetVisible = MutableStateFlow(false)
    val isPaymentSheetVisible: StateFlow<Boolean> = _isPaymentSheetVisible.asStateFlow()

    private val _isExtendDialogVisible = MutableStateFlow(false)
    val isExtendDialogVisible: StateFlow<Boolean> = _isExtendDialogVisible.asStateFlow()

    private val _isExtendingDueDate = MutableStateFlow(false)
    val isExtendingDueDate: StateFlow<Boolean> = _isExtendingDueDate.asStateFlow()

    private val _extendError = MutableStateFlow<Int?>(null)
    val extendError: StateFlow<Int?> = _extendError.asStateFlow()

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
                val dueDateChanges = LoanRepository.getDueDateChanges(id)
                // includeArchived: true — a payment's destination source may since have been
                // archived, but its name must still resolve for this historical list.
                val sourcesById = FundingSourceRepository.getFundingSources(includeArchived = true).associateBy { it.id }
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
                    payments = paymentItems,
                    dueDateChanges = dueDateChanges
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = LoanDetailUiState.Error(R.string.loan_detail_load_error)
            }
        }
    }

    fun showPaymentSheet() {
        _isPaymentSheetVisible.value = true
    }

    fun dismissPaymentSheet() {
        _isPaymentSheetVisible.value = false
    }

    fun showExtendDialog() {
        _extendError.value = null
        _isExtendDialogVisible.value = true
    }

    fun dismissExtendDialog() {
        _isExtendDialogVisible.value = false
    }

    fun extendDueDate(newDueDate: String, notes: String?) {
        val id = loanId ?: return
        viewModelScope.launch {
            _isExtendingDueDate.value = true
            _extendError.value = null
            try {
                LoanRepository.extendDueDate(id, newDueDate, notes?.takeIf { it.isNotBlank() })
                _isExtendDialogVisible.value = false
                SnackbarController.show(R.string.snackbar_due_date_extended)
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _extendError.value = R.string.loan_extend_generic_error
            } finally {
                _isExtendingDueDate.value = false
            }
        }
    }
}
