package com.didiprogrammer.youtepresta.ui.loans

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.LoanRepository
import com.didiprogrammer.youtepresta.data.repository.LoanSourceMovementException
import com.didiprogrammer.youtepresta.ui.common.SnackbarController
import com.didiprogrammer.youtepresta.util.DueDayRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FundingSourcesLoadState {
    data object Loading : FundingSourcesLoadState
    data class Error(@StringRes val messageRes: Int) : FundingSourcesLoadState
    data class Content(val sources: List<FundingSource>) : FundingSourcesLoadState
}

class NewLoanViewModel : ViewModel() {

    private val _sourcesState = MutableStateFlow<FundingSourcesLoadState>(FundingSourcesLoadState.Loading)
    val sourcesState: StateFlow<FundingSourcesLoadState> = _sourcesState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()

    private val _loanCreated = MutableStateFlow(false)
    val loanCreated: StateFlow<Boolean> = _loanCreated.asStateFlow()

    fun loadFundingSources() {
        viewModelScope.launch {
            _sourcesState.value = FundingSourcesLoadState.Loading
            try {
                _sourcesState.value = FundingSourcesLoadState.Content(FundingSourceRepository.getFundingSources())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _sourcesState.value = FundingSourcesLoadState.Error(R.string.sources_load_error)
            }
        }
    }

    fun createLoan(
        friend: Friend?,
        source: FundingSource?,
        amountInput: String,
        dueDayRule: DueDayRule,
        monthlyInterestRatePercentInput: String
    ) {
        if (friend == null) {
            _error.value = R.string.loan_friend_required_error
            return
        }
        if (source == null) {
            _error.value = R.string.loan_source_required_error
            return
        }
        val amount = amountInput.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _error.value = R.string.common_amount_invalid_error
            return
        }
        val ratePercent = monthlyInterestRatePercentInput.replace(",", ".").toDoubleOrNull()
        if (ratePercent == null || ratePercent < 0) {
            _error.value = R.string.loan_monthly_interest_rate_invalid_error
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                LoanRepository.createLoan(
                    friendId = friend.id,
                    sourceId = source.id,
                    principalAmount = amount,
                    dueDayRule = dueDayRule,
                    monthlyInterestRate = ratePercent / 100.0
                )
                _isSaving.value = false
                _loanCreated.value = true
                SnackbarController.show(R.string.snackbar_loan_created)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoanSourceMovementException) {
                // The loan row exists but its funding source was never debited — surface this
                // clearly instead of the generic message, and stay on screen (don't set
                // loanCreated) so the warning is actually seen instead of navigating away.
                _isSaving.value = false
                _error.value = R.string.loan_create_source_movement_error
            } catch (e: Exception) {
                _isSaving.value = false
                _error.value = R.string.loan_create_generic_error
            }
        }
    }
}
