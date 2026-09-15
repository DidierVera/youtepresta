package com.didiprogrammer.youtepresta.ui.lock

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.ui.theme.Spacing

private const val REQUIRED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

/** Whether the device has any biometric or device-credential (PIN/pattern) lock configured. */
fun isDeviceLockAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(REQUIRED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Full-screen gate shown as an overlay on top of the app's content whenever [AppLockState] is
 * locked. Kept as an overlay rather than a NavHost destination so unlocking never disturbs the
 * underlying navigation back stack or any screen's scroll/form state.
 */
@Composable
fun LockScreen(onUnlocked: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var errorRes by remember { mutableStateOf<Int?>(null) }

    fun launchPrompt() {
        errorRes = null
        if (activity == null) return
        showBiometricPrompt(
            activity = activity,
            onSuccess = onUnlocked,
            onError = { code -> errorRes = lockErrorMessageFor(code) }
        )
    }

    LaunchedEffect(Unit) { launchPrompt() }

    BackHandler {
        // Never let "back" reveal app content from the lock screen — background the app instead,
        // same as most apps that lock themselves.
        (context as? Activity)?.moveTaskToBack(true)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.lg)
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.lock_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Button(onClick = { launchPrompt() }) {
                Text(stringResource(R.string.lock_unlock_button))
            }
            if (errorRes != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(text = stringResource(errorRes!!), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun lockErrorMessageFor(errorCode: Int): Int? = when (errorCode) {
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_CANCELED -> null
    BiometricPrompt.ERROR_LOCKOUT,
    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> R.string.lock_error_lockout
    else -> R.string.lock_error_generic
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (Int) -> Unit
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.lock_prompt_title))
        .setSubtitle(activity.getString(R.string.lock_prompt_subtitle))
        .setAllowedAuthenticators(REQUIRED_AUTHENTICATORS)
        .build()

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errorCode)
            }
        }
    )
    prompt.authenticate(promptInfo)
}
