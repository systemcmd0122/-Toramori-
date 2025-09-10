package com.example.e_zuka.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.e_zuka.data.model.AuthState
import com.example.e_zuka.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }


    Box(modifier = modifier.fillMaxSize()) {
        when (authState) {
            is AuthState.Loading -> {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is AuthState.Unauthenticated -> {
                // Show authentication screens when unauthenticated
                AuthNavigation(viewModel = viewModel)
            }

            is AuthState.Authenticated -> {
                ProfileScreen(
                    user = (authState as AuthState.Authenticated).user,
                    viewModel = viewModel
                )
            }

            is AuthState.UserNameVerificationRequired -> {
                UserNameVerificationScreen(
                    user = (authState as AuthState.UserNameVerificationRequired).user,
                    viewModel = viewModel
                )
            }

            is AuthState.RegionVerificationRequired -> {
                RegionVerificationScreen(
                    user = (authState as AuthState.RegionVerificationRequired).user,
                    viewModel = viewModel
                )
            }

            is AuthState.Error -> {
                // 汎用エラー画面 - 再試行とログアウトを提案
                val message = (authState as AuthState.Error).message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.padding(24.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            Text(text = "エラーが発生しました", style = MaterialTheme.typography.headlineSmall)
                            Text(text = message, style = MaterialTheme.typography.bodyMedium)
                            androidx.compose.material3.Button(onClick = { viewModel.signOut() }) {
                                Text(text = "ログアウトしてやり直す")
                            }
                        }
                    }
                }
            }
        }

        // Snackbar for messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { snackbarData ->
            Snackbar(
                action = {
                    TextButton(
                        onClick = {
                            snackbarData.dismiss()
                        }
                    ) {
                        Text("閉じる")
                    }
                }
            ) {
                Text(snackbarData.visuals.message)
            }
        }
    }
}

@Composable
fun AuthNavigation(
    viewModel: AuthViewModel
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "signin"
    ) {
        composable("signin") {
            SignInScreen(
                viewModel = viewModel,
                onNavigateToSignUp = {
                    navController.navigate("signup")
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                viewModel = viewModel,
                onNavigateToSignIn = {
                    navController.popBackStack()
                }
            )
        }
    }
}