package com.didiprogrammer.youtepresta.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.FundingSource
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.PaymentRepository
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
                _error.value = "No se pudieron cargar los bolsillos."
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
        val principal = principalPaymentInput.replace(",", ".").toDoubleOrNull()
        val interest = interestPaymentInput.replace(",", ".").toDoubleOrNull()

        if (principal == null || interest == null || principal < 0 || interest < 0) {
            _error.value = "Los montos deben ser números válidos y no negativos."
            return
        }
        if (principal == 0.0 && interest == 0.0) {
            _error.value = "Registra al menos un abono a capital o de interés mayor a 0."
            return
        }
        if (principalDestination == null) {
            _error.value = "Elige el bolsillo destino del capital."
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isSaving.value = false
                _error.value = "No se pudo registrar el pago."
            }
        }
    }
}
