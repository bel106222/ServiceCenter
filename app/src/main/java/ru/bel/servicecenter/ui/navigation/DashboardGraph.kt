package ru.bel.servicecenter.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.dashboard.DashboardItem
import ru.bel.servicecenter.ui.dashboard.DashboardScreen
import ru.bel.servicecenter.ui.reports.ReportsScreen
import ru.bel.servicecenter.viewmodels.AuthViewModel

fun NavGraphBuilder.dashboardGraph(
    navController: NavController,
    authViewModel: AuthViewModel,
    onExit: () -> Unit
) {
    composable("dashboard") {
        val role by authViewModel.userRole.collectAsState()
        if (role == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val roleSpecificItems = when (role) {
                "user" -> listOf(
                    DashboardItem("Профиль") { navController.navigate("profile_user") },
                    DashboardItem("Заказы") { navController.navigate("orders") }
                )
                "engineer" -> listOf(
                    DashboardItem("Профиль") { navController.navigate("profile_engineer") },
                    DashboardItem("Клиенты") { navController.navigate("clients") },
                    DashboardItem("Категории") { navController.navigate("categories") },
                    DashboardItem("Цены") { navController.navigate("prices") },
                    DashboardItem("Услуги") { navController.navigate("services") },
                    DashboardItem("Заказы") { navController.navigate("orders") }
                )
                "admin" -> listOf(
                    DashboardItem("Профиль") { navController.navigate("profile_admin") },
                    DashboardItem("Пользователи") { navController.navigate("users") },
                    DashboardItem("Клиенты") { navController.navigate("clients") },
                    DashboardItem("Категории") { navController.navigate("categories") },
                    DashboardItem("Цены") { navController.navigate("prices") },
                    DashboardItem("Услуги") { navController.navigate("services") },
                    DashboardItem("Заказы") { navController.navigate("orders") },
                    DashboardItem("База данных") { navController.navigate("admin_database") }
                )
                else -> emptyList()
            }

            val items = roleSpecificItems + DashboardItem("Отчёты") {
                navController.navigate("reports")
            } + DashboardItem("Выход") {
                onExit()
            }

            DashboardScreen(items = items)
        }
    }

    composable("reports") {
        val role by authViewModel.userRole.collectAsState()
        ReportsScreen(
            role = role ?: "unknown",
            onBack = { navController.popBackStack() }
        )
    }
}