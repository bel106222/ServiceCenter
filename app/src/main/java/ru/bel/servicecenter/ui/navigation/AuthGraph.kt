package ru.bel.servicecenter.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.auth.LoginScreen
import ru.bel.servicecenter.ui.auth.NotAClientScreen
import ru.bel.servicecenter.ui.auth.RegisterScreen
import ru.bel.servicecenter.ui.auth.SelectClientScreen
import ru.bel.servicecenter.viewmodels.AuthViewModel

fun NavGraphBuilder.authGraph(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // Стартовый экран — вход
    composable("start") {
        LoginScreen(
            authViewModel = authViewModel,
            onNavigateToRegister = { navController.navigate("register") }
        )
    }

    // Регистрация
    composable("register") {
        RegisterScreen(authViewModel = authViewModel)
    }

    // Предупреждение: пользователь ещё не клиент
    composable("not_a_client") {
        NotAClientScreen(
            onContinue = {
                navController.navigate("select_client") {
                    popUpTo("not_a_client") { inclusive = true }
                }
            }
        )
    }

    // Выбор или создание клиента
    composable("select_client") {
        val currentUser by authViewModel.loggedUser.collectAsState()
        currentUser?.let { user ->
            SelectClientScreen(
                currentUser = user,
                authViewModel = authViewModel,
                onClientBound = {
                    navController.navigate("dashboard") {
                        popUpTo("select_client") { inclusive = true }
                    }
                }
            )
        }
    }
}