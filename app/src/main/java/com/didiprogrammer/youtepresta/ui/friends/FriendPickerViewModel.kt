package com.didiprogrammer.youtepresta.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.data.repository.FriendRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendPickerViewModel : ViewModel() {

    private var hasLoaded = false

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()

    val filteredFriends: StateFlow<List<Friend>> = combine(_friends, _query) { friends, query ->
        if (query.isBlank()) {
            friends
        } else {
            friends.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadFriends() {
        if (hasLoaded) return
        hasLoaded = true

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _friends.value = FriendRepository.getFriends()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                hasLoaded = false
                _error.value = R.string.friends_load_error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun createFriend(name: String, onCreated: (Friend) -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _error.value = R.string.common_name_required_error
            return
        }

        viewModelScope.launch {
            _isCreating.value = true
            _error.value = null
            try {
                val created = FriendRepository.createFriend(name = trimmedName, phone = null, notes = null)
                _friends.value = _friends.value + created
                _query.value = created.name
                onCreated(created)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = R.string.friend_create_error
            } finally {
                _isCreating.value = false
            }
        }
    }
}
