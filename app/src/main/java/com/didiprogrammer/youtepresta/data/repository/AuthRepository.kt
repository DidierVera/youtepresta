package com.didiprogrammer.youtepresta.data.repository

import com.didiprogrammer.youtepresta.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Whether there is an active Supabase session, derived from [io.github.jan.supabase.auth.Auth.sessionStatus].
 * [LOADING] covers the initial read from local storage right after process start.
 */
enum class SessionState {
    LOADING,
    AUTHENTICATED,
    UNAUTHENTICATED
}

object AuthRepository {

    private val auth = SupabaseClientProvider.client.auth
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sessionState: StateFlow<SessionState> = auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> SessionState.AUTHENTICATED
                is SessionStatus.NotAuthenticated -> SessionState.UNAUTHENTICATED
                is SessionStatus.RefreshFailure -> SessionState.UNAUTHENTICATED
                SessionStatus.Initializing -> SessionState.LOADING
            }
        }
        .stateIn(repositoryScope, SharingStarted.Eagerly, SessionState.LOADING)

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
