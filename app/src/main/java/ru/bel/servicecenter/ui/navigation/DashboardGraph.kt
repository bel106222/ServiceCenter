package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.controllers.AuthController
import ru.bel.servicecenter.ui.dashboard.DashboardScreen
import ru.bel.servicecenter.ui.dashboard.DashboardItem
import ru.bel.servicecenter.ui.common.PlaceholderScreen

fun NavGraphBuilder.dashboardGraph(
    navController: NavController,
    authController: AuthController
) {
    composable("dashboard") {
        val role = authController.userRole.value ?: "user"
        val items = when (role) {
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
        DashboardScreen(items = items)
    }

    // Заглушки для будущих разделов
    composable("profile_user") { PlaceholderScreen("Профиль пользователя") }
    composable("profile_engineer") { PlaceholderScreen("Профиль инженера") }
    composable("profile_admin") { PlaceholderScreen("Профиль администратора") }
    composable("clients") { PlaceholderScreen("Клиенты") }
    composable("categories") { PlaceholderScreen("Категории") }
    composable("prices") { PlaceholderScreen("Цены") }
    composable("services") { PlaceholderScreen("Услуги") }
    composable("orders") { PlaceholderScreen("Заказы") }
    composable("users") { PlaceholderScreen("Пользователи") }
    composable("admin_database") { PlaceholderScreen("База данных") }
}