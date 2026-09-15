package com.didiprogrammer.youtepresta.ui.common

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lets any ViewModel fire a one-off success confirmation ("Préstamo creado", "Pago registrado",
 * ...) without needing a Context or a reference to the single SnackbarHostState that lives at the
 * top of the Compose tree (in MainActivity, above the NavHost) — same lightweight-singleton
 * pattern already used for the repositories.
 */
object SnackbarController {
    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    fun show(@StringRes messageRes: Int) {
        _messages.tryEmit(messageRes)
    }
}
