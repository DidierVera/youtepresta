package com.didiprogrammer.youtepresta

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.didiprogrammer.youtepresta.data.repository.SessionState
import com.didiprogrammer.youtepresta.notification.NotificationHelper
import com.didiprogrammer.youtepresta.notification.NotificationScheduler
import com.didiprogrammer.youtepresta.ui.auth.AuthViewModel
import com.didiprogrammer.youtepresta.ui.auth.LoginScreen
import com.didiprogrammer.youtepresta.ui.common.SnackbarController
import com.didiprogrammer.youtepresta.ui.friends.FriendsScreen
import com.didiprogrammer.youtepresta.ui.home.HomeScreen
import com.didiprogrammer.youtepresta.ui.loans.LoanDetailScreen
import com.didiprogrammer.youtepresta.ui.loans.LoansScreen
import com.didiprogrammer.youtepresta.ui.loans.NewLoanScreen
import com.didiprogrammer.youtepresta.ui.lock.AppLockState
import com.didiprogrammer.youtepresta.ui.lock.LockScreen
import com.didiprogrammer.youtepresta.ui.lock.isDeviceLockAvailable
import com.didiprogrammer.youtepresta.ui.sources.FundingSourceDetailScreen
import com.didiprogrammer.youtepresta.ui.sources.FundingSourcesScreen
import com.didiprogrammer.youtepresta.ui.theme.YouTePrestaTheme
import kotlinx.coroutines.launch

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"
private const val ROUTE_SOURCES = "sources"
private const val ARG_SOURCE_ID = "sourceId"
private const val ROUTE_SOURCE_DETAIL = "sources/{$ARG_SOURCE_ID}"
private const val ROUTE_FRIENDS = "friends"
private const val ROUTE_LOANS = "loans"
private const val ROUTE_NEW_LOAN = "loans/new"
private const val ARG_LOAN_ID = "loanId"
// Deliberately "loan/{loanId}" (singular), not "loans/{loanId}", so it can never be ambiguous
// with the static "loans/new" route.
private const val ROUTE_LOAN_DETAIL = "loan/{$ARG_LOAN_ID}"

/** What to navigate to once the NavHost is ready, read from the Intent that opened/resumed the activity. */
private sealed interface PendingNavigation {
    data class LoanDetail(val loanId: String) : PendingNavigation
    data object LoansList : PendingNavigation
}

private fun Intent.toPendingNavigation(): PendingNavigation? {
    val loanId = getStringExtra(NotificationHelper.EXTRA_LOAN_ID)
    return when {
        loanId != null -> PendingNavigation.LoanDetail(loanId)
        getBooleanExtra(NotificationHelper.EXTRA_SHOW_LOANS_LIST, false) -> PendingNavigation.LoansList
        else -> null
    }
}

class MainActivity : FragmentActivity() {
    private var pendingNavigation by mutableStateOf<PendingNavigation?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNavigation = intent.toPendingNavigation()
        setContent {
            YouTePrestaTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    SnackbarController.messages.collect { messageRes ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(messageRes))
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    AppRoot(
                        modifier = Modifier.padding(innerPadding),
                        pendingNavigation = pendingNavigation,
                        onPendingNavigationConsumed = { pendingNavigation = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavigation = intent.toPendingNavigation()
    }
}

@Composable
private fun AppRoot(
    modifier: Modifier = Modifier,
    pendingNavigation: PendingNavigation? = null,
    onPendingNavigationConsumed: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = viewModel()
    val sessionState by authViewModel.sessionState.collectAsState()
    val context = LocalContext.current
    val lockRequired = remember(context) { isDeviceLockAvailable(context) }
    val isUnlocked by AppLockState.isUnlocked.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Re-lock every time the app leaves the foreground, same as most apps that gate
            // themselves behind biometrics — a fresh login explicitly unlocks it again. An
            // ON_STOP caused by a configuration change (e.g. rotation) is not really leaving the
            // app, even though the screen is locked to portrait as a second line of defense —
            // ignore it so it doesn't force a spurious re-lock.
            if (event == Lifecycle.Event.ON_STOP && (context as? Activity)?.isChangingConfigurations != true) {
                AppLockState.markLocked()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(sessionState) {
        if (sessionState == SessionState.AUTHENTICATED) {
            NotificationScheduler.schedule(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    when (sessionState) {
        SessionState.LOADING -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        SessionState.AUTHENTICATED, SessionState.UNAUTHENTICATED -> {
            val navController = rememberNavController()
            val startDestination = if (sessionState == SessionState.AUTHENTICATED) ROUTE_HOME else ROUTE_LOGIN

            LaunchedEffect(sessionState, pendingNavigation) {
                if (sessionState == SessionState.AUTHENTICATED && pendingNavigation != null) {
                    when (pendingNavigation) {
                        is PendingNavigation.LoanDetail -> navController.navigate("loan/${pendingNavigation.loanId}")
                        PendingNavigation.LoansList -> navController.navigate(ROUTE_LOANS)
                    }
                    onPendingNavigationConsumed()
                }
            }

            Box(modifier = modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(ROUTE_LOGIN) {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                // A password was just typed, so the identity check the lock
                                // screen would otherwise demand is already satisfied for this session.
                                AppLockState.markUnlocked()
                                navController.navigateClearingBackStack(ROUTE_HOME)
                            }
                        )
                    }
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            onSignOut = {
                                authViewModel.signOut()
                                AppLockState.markLocked()
                                navController.navigateClearingBackStack(ROUTE_LOGIN)
                            },
                            onViewFundingSources = { navController.navigate(ROUTE_SOURCES) },
                            onViewFriends = { navController.navigate(ROUTE_FRIENDS) },
                            onViewLoans = { navController.navigate(ROUTE_LOANS) }
                        )
                    }
                    composable(ROUTE_SOURCES) {
                        FundingSourcesScreen(
                            onBack = { navController.popBackStack() },
                            onSourceClick = { sourceId -> navController.navigate("sources/$sourceId") }
                        )
                    }
                    composable(
                        route = ROUTE_SOURCE_DETAIL,
                        arguments = listOf(navArgument(ARG_SOURCE_ID) { type = NavType.StringType })
                    ) { backStackEntry ->
                        val sourceId = backStackEntry.arguments?.getString(ARG_SOURCE_ID).orEmpty()
                        FundingSourceDetailScreen(
                            sourceId = sourceId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ROUTE_FRIENDS) {
                        FriendsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ROUTE_LOANS) {
                        LoansScreen(
                            onBack = { navController.popBackStack() },
                            onNewLoan = { navController.navigate(ROUTE_NEW_LOAN) },
                            onLoanClick = { loanId -> navController.navigate("loan/$loanId") }
                        )
                    }
                    composable(ROUTE_NEW_LOAN) {
                        NewLoanScreen(
                            onBack = { navController.popBackStack() },
                            onLoanSaved = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = ROUTE_LOAN_DETAIL,
                        arguments = listOf(navArgument(ARG_LOAN_ID) { type = NavType.StringType })
                    ) { backStackEntry ->
                        val loanId = backStackEntry.arguments?.getString(ARG_LOAN_ID).orEmpty()
                        LoanDetailScreen(
                            loanId = loanId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (sessionState == SessionState.AUTHENTICATED && lockRequired && !isUnlocked) {
                    LockScreen(
                        onUnlocked = { AppLockState.markUnlocked() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
