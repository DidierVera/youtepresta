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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.didiprogrammer.youtepresta.data.repository.SessionState
import com.didiprogrammer.youtepresta.ui.auth.AuthViewModel
import com.didiprogrammer.youtepresta.ui.auth.LoginScreen
import com.didiprogrammer.youtepresta.ui.home.HomeScreen
import com.didiprogrammer.youtepresta.ui.theme.YouTePrestaTheme

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"

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
                        }
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
