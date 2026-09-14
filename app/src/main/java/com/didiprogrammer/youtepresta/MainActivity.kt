package com.didiprogrammer.youtepresta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.didiprogrammer.youtepresta.data.repository.SessionState
import com.didiprogrammer.youtepresta.ui.auth.AuthViewModel
import com.didiprogrammer.youtepresta.ui.auth.LoginScreen
import com.didiprogrammer.youtepresta.ui.friends.FriendsScreen
import com.didiprogrammer.youtepresta.ui.home.HomeScreen
import com.didiprogrammer.youtepresta.ui.loans.LoanDetailScreen
import com.didiprogrammer.youtepresta.ui.loans.LoansScreen
import com.didiprogrammer.youtepresta.ui.loans.NewLoanScreen
import com.didiprogrammer.youtepresta.ui.sources.FundingSourceDetailScreen
import com.didiprogrammer.youtepresta.ui.sources.FundingSourcesScreen
import com.didiprogrammer.youtepresta.ui.theme.YouTePrestaTheme

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YouTePrestaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun AppRoot(modifier: Modifier = Modifier) {
    val authViewModel: AuthViewModel = viewModel()
    val sessionState by authViewModel.sessionState.collectAsState()

    when (sessionState) {
        SessionState.LOADING -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        SessionState.AUTHENTICATED, SessionState.UNAUTHENTICATED -> {
            val navController = rememberNavController()
            val startDestination = if (sessionState == SessionState.AUTHENTICATED) ROUTE_HOME else ROUTE_LOGIN

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier
            ) {
                composable(ROUTE_LOGIN) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = { navController.navigateClearingBackStack(ROUTE_HOME) }
                    )
                }
                composable(ROUTE_HOME) {
                    HomeScreen(
                        onSignOut = {
                            authViewModel.signOut()
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
