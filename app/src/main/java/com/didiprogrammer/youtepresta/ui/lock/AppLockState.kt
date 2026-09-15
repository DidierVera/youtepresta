package com.didiprogrammer.youtepresta.ui.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app content is currently gated behind [LockScreen]. Locked by default so a restored
 * session (auto sign-in on cold start, no password just typed) still requires proof of identity
 * before showing anything; AppRoot unlocks it right after a fresh login and re-locks it whenever
 * the app goes to background.
 */
object AppLockState {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun markUnlocked() {
        _isUnlocked.value = true
    }

    fun markLocked() {
        _isUnlocked.value = false
    }
}
