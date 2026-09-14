package com.didiprogrammer.youtepresta.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FriendsUiState {
    data object Loading : FriendsUiState
    data class Error(val message: String) : FriendsUiState
    data class Content(val friends: List<Friend>) : FriendsUiState
}

class FriendsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _isCreateDialogVisible = MutableStateFlow(false)
    val isCreateDialogVisible: StateFlow<Boolean> = _isCreateDialogVisible.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = FriendsUiState.Loading
            try {
                val friends = FriendRepository.getFriends()
                _uiState.value = FriendsUiState.Content(friends)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FriendsUiState.Error("No se pudieron cargar los amigos.")
            }
        }
    }

    fun showCreateDialog() {
        _createError.value = null
        _isCreateDialogVisible.value = true
    }

    fun dismissCreateDialog() {
        _isCreateDialogVisible.value = false
        _createError.value = null
    }

    fun createFriend(name: String, phone: String, notes: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _createError.value = "El nombre es obligatorio."
            return
        }

        viewModelScope.launch {
            _isCreating.value = true
            _createError.value = null
            try {
                FriendRepository.createFriend(
                    name = trimmedName,
                    phone = phone.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null }
                )
                _isCreating.value = false
                _isCreateDialogVisible.value = false
                loadFriends()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isCreating.value = false
                _createError.value = "No se pudo crear el amigo."
            }
        }
    }
}
