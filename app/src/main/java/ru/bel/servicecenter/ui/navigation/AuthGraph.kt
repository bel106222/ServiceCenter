package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.controllers.AuthController
import ru.bel.servicecenter.ui.auth.LoginScreen
import ru.bel.servicecenter.ui.auth.RegisterScreen

fun NavGraphBuilder.authGraph(
    navController: NavController,
    authController: AuthController
) {
    composable("start") {
        LoginScreen(authController = authController)
    }
    composable("login") {
        LoginScreen(authController = authController)
    }
    composable("register") {
        RegisterScreen(authController = authController)
    }
    // Здесь позже добавим not_a_client и select_client
}