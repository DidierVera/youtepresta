package com.didiprogrammer.youtepresta.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.PaymentRepository
import com.didiprogrammer.youtepresta.ui.common.SnackbarController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterPaymentViewModel : ViewModel() {

    private val _fundingSources = MutableStateFlow<List<FundingSource>>(emptyList())
    val fundingSources: StateFlow<List<FundingSource>> = _fundingSources.asStateFlow()

    private val _isLoadingSources = MutableStateFlow(false)
    val isLoadingSources: StateFlow<Boolean> = _isLoadingSources.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()

    private val _paymentRegistered = MutableStateFlow(false)
    val paymentRegistered: StateFlow<Boolean> = _paymentRegistered.asStateFlow()

    fun loadFundingSources() {
        viewModelScope.launch {
            _isLoadingSources.value = true
            try {
                _fundingSources.value = FundingSourceRepository.getFundingSources()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = R.string.sources_load_error
            } finally {
                _isLoadingSources.value = false
            }
        }
    }

    fun registerPayment(
        loanId: String,
        principalPaymentInput: String,
        interestPaymentInput: String,
        principalDestination: FundingSource?,
        interestDestination: FundingSource?
    ) {
        // A field left blank means "no payment on this side" (0), not "invalid" — matches the
        // live total preview in RegisterPaymentSheet, which already treats blank as 0.
        val principal = principalPaymentInput.replace(",", ".").let { if (it.isBlank()) 0.0 else it.toDoubleOrNull() }
        val interest = interestPaymentInput.replace(",", ".").let { if (it.isBlank()) 0.0 else it.toDoubleOrNull() }

        if (principal == null || interest == null || principal < 0 || interest < 0) {
            _error.value = R.string.payment_amounts_invalid_error
            return
        }
        if (principal == 0.0 && interest == 0.0) {
            _error.value = R.string.payment_amount_zero_error
            return
        }
        if (principalDestination == null) {
            _error.value = R.string.payment_principal_destination_required_error
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                PaymentRepository.createPayment(
                    loanId = loanId,
                    principalPayment = principal,
                    interestPayment = interest,
                    principalDestinationSourceId = principalDestination.id,
                    interestDestinationSourceId = interestDestination?.id
                )
                _isSaving.value = false
                _paymentRegistered.value = true
                SnackbarController.show(R.string.snackbar_payment_registered)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isSaving.value = false
                _error.value = R.string.payment_register_generic_error
            }
        }
    }
}
