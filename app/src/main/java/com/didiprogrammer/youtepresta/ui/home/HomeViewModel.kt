package com.didiprogrammer.youtepresta.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import com.didiprogrammer.youtepresta.data.repository.FundingSourceRepository
import com.didiprogrammer.youtepresta.data.repository.LoanRepository
import com.didiprogrammer.youtepresta.data.repository.LoanStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Quick counts shown as each dashboard card's subtitle. Purely cosmetic — a failed fetch just
 * leaves that one subtitle blank instead of blocking the dashboard or showing an error.
 */
class HomeViewModel : ViewModel() {

    private val _fundingSourcesCount = MutableStateFlow<Int?>(null)
    val fundingSourcesCount: StateFlow<Int?> = _fundingSourcesCount.asStateFlow()

    private val _friendsCount = MutableStateFlow<Int?>(null)
    val friendsCount: StateFlow<Int?> = _friendsCount.asStateFlow()

    private val _activeLoansCount = MutableStateFlow<Int?>(null)
    val activeLoansCount: StateFlow<Int?> = _activeLoansCount.asStateFlow()

    fun loadCounts() {
        viewModelScope.launch {
            try {
                _fundingSourcesCount.value = FundingSourceRepository.getFundingSources().size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _fundingSourcesCount.value = null
            }
        }
        viewModelScope.launch {
            try {
                _friendsCount.value = FriendRepository.getFriends().size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _friendsCount.value = null
            }
        }
        viewModelScope.launch {
            try {
                _activeLoansCount.value = LoanRepository.getLoans().count { it.status != LoanStatus.PAID }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _activeLoansCount.value = null
            }
        }
    }
}
