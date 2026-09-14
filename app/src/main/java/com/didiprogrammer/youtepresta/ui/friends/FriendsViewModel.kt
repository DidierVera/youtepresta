package com.didiprogrammer.youtepresta.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.repository.FriendHasLoansException
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

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    private val _editingFriend = MutableStateFlow<Friend?>(null)
    val editingFriend: StateFlow<Friend?> = _editingFriend.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _friendPendingDelete = MutableStateFlow<Friend?>(null)
    val friendPendingDelete: StateFlow<Friend?> = _friendPendingDelete.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

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
        _editingFriend.value = null
        _saveError.value = null
        _isDialogVisible.value = true
    }

    fun showEditDialog(friend: Friend) {
        _editingFriend.value = friend
        _saveError.value = null
        _isDialogVisible.value = true
    }

    fun dismissDialog() {
        _isDialogVisible.value = false
        _editingFriend.value = null
        _saveError.value = null
    }

    fun saveFriend(name: String, phone: String, notes: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _saveError.value = "El nombre es obligatorio."
            return
        }

        val editing = _editingFriend.value

        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                val phoneValue = phone.trim().ifBlank { null }
                val notesValue = notes.trim().ifBlank { null }
                if (editing != null) {
                    FriendRepository.updateFriend(
                        id = editing.id,
                        name = trimmedName,
                        phone = phoneValue,
                        notes = notesValue
                    )
                } else {
                    FriendRepository.createFriend(
                        name = trimmedName,
                        phone = phoneValue,
                        notes = notesValue
                    )
                }
                _isSaving.value = false
                _isDialogVisible.value = false
                _editingFriend.value = null
                loadFriends()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isSaving.value = false
                _saveError.value = if (editing != null) {
                    "No se pudo actualizar el amigo."
                } else {
                    "No se pudo crear el amigo."
                }
            }
        }
    }

    fun confirmDelete(friend: Friend) {
        _deleteError.value = null
        _friendPendingDelete.value = friend
    }

    fun dismissDeleteConfirmation() {
        _friendPendingDelete.value = null
        _deleteError.value = null
    }

    fun deleteFriend() {
        val friend = _friendPendingDelete.value ?: return

        viewModelScope.launch {
            _isDeleting.value = true
            _deleteError.value = null
            try {
                FriendRepository.deleteFriend(friend.id)
                _isDeleting.value = false
                _friendPendingDelete.value = null
                loadFriends()
            } catch (e: CancellationException) {
                throw e
            } catch (e: FriendHasLoansException) {
                _isDeleting.value = false
                _deleteError.value = "No puedes eliminar un amigo con préstamos registrados."
            } catch (e: Exception) {
                _isDeleting.value = false
                _deleteError.value = "No se pudo eliminar el amigo."
            }
        }
    }
}
