package com.didiprogrammer.youtepresta.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.repository.AuthRepository
import com.didiprogrammer.youtepresta.data.repository.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(@StringRes val messageRes: Int) : LoginUiState
}

class AuthViewModel : ViewModel() {

    val sessionState: StateFlow<SessionState> = AuthRepository.sessionState

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun login() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _uiState.value = LoginUiState.Error(R.string.login_error_empty_fields)
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                AuthRepository.signIn(currentEmail, currentPassword)
                _uiState.value = LoginUiState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(R.string.login_error_invalid_credentials)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            AuthRepository.signOut()
            // Clear the form so the next person to use this device (a different account, since
            // logins are shared devices among trusted people, not just this one's own re-login)
            // never sees a stale email left over from whoever was signed in before.
            _email.value = ""
            _password.value = ""
            _uiState.value = LoginUiState.Idle
        }
    }
}
