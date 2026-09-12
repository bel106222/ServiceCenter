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
import kotlinx.coroutines.launch
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.ui.dashboard.DashboardItem
import ru.bel.servicecenter.ui.dashboard.DashboardScreen
import ru.bel.servicecenter.ui.reports.ReportsScreen
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.viewmodels.AuthViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun NavGraphBuilder.dashboardGraph(
    navController: NavController,
    authViewModel: AuthViewModel,
    onExit: () -> Unit
) {
    composable("dashboard") {
        val role by authViewModel.userRole.collectAsState()
        val coroutineScope = rememberCoroutineScope()

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

            val items = roleSpecificItems +
                    DashboardItem("Отчёты") { navController.navigate("reports") } +
                    DashboardItem("Выход") {
                        coroutineScope.launch {
                            // 1. Формируем имя файла: <userId>_<timestamp>.log
                            val userId = authViewModel.loggedUser.value?.id ?: "unknown"
                            val timestamp = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            val fileName = "${userId}_${timestamp}.log"

                            // 2. Собираем весь текст из статусной строки
                            val logsText = LoggerService.entries.value.joinToString("\n") {
                                "[${it.timestamp}] ${it.message}"
                            }

                            // 3. Выгружаем в Storage: bucket order-attachments, подпапка log
                            try {
                                RepositoryProvider.attachmentRepo.uploadFileToStorage(
                                    bucket = "order-attachments",
                                    fileName = "log/$fileName",
                                    fileBytes = logsText.toByteArray(Charsets.UTF_8),
                                    contentType = "text/plain"
                                )
                                LoggerService.log("Логи выгружены: $fileName")
                            } catch (e: Exception) {
                                // Тихо игнорируем — пользователь всё равно выходит
                                LoggerService.log("Ошибка выгрузки логов: ${e.message}")
                            }

                            // 4. Завершаем приложение
                            onExit()
                        }
                    }

            DashboardScreen(items = items)
        }
    }

    composable("reports") {
        val roleState by authViewModel.userRole.collectAsState()
        val roleString: String = roleState ?: "unknown"
        ReportsScreen(
            role = roleString,
            onBack = { navController.popBackStack() }
        )
    }
}