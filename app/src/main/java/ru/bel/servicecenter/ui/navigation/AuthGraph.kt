package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.auth.LoginScreen
import ru.bel.servicecenter.ui.auth.RegisterScreen
import ru.bel.servicecenter.viewmodels.AuthViewModel

fun NavGraphBuilder.authGraph(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    composable("start") {
        LoginScreen(authViewModel = authViewModel)
    }
    composable("login") {
        LoginScreen(authViewModel = authViewModel)
    }
    composable("register") {
        RegisterScreen(authViewModel = authViewModel)
    }
}