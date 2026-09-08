package ru.bel.servicecenter.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.ui.components.HistoryDialog
import ru.bel.servicecenter.ui.components.StatusBar
import ru.bel.servicecenter.ui.status.DatabaseStatusScreen
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.RoleCache
import ru.bel.servicecenter.utils.StatusTree
import ru.bel.servicecenter.viewmodels.*
import timber.log.Timber

@Composable
fun AppNavigation(onExit: () -> Unit) {
    val statusViewModel = remember { StatusViewModel() }
    var showHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Timber.uprootAll()
        Timber.plant(StatusTree())
        LoggerService.log("Приложение запущено")
    }

    val navController = rememberNavController()

    val authViewModel = remember { AuthViewModel() }
    val adminViewModel = remember { AdminViewModel() }
    val userViewModel = remember { UserViewModel() }
    val clientViewModel = remember { ClientViewModel() }
    val categoryViewModel = remember { CategoryViewModel() }
    val priceViewModel = remember { PriceViewModel() }
    val serviceViewModel = remember { ServiceViewModel() }
    val orderViewModel = remember { OrderViewModel() }
    val orderItemViewModel = remember { OrderItemViewModel() }
    orderItemViewModel.orderViewModel = orderViewModel

    var connectionOk by remember { mutableStateOf<Boolean?>(null) }
    var integrityOk by remember { mutableStateOf<Boolean?>(null) }
    var showApp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        connectionOk = RepositoryProvider.checkConnection()
        if (connectionOk == true) {
            integrityOk = RepositoryProvider.checkAdminExists()
            if (integrityOk == true) {
                RoleCache.load()
            }
        } else {
            integrityOk = false
        }
    }

    LaunchedEffect(connectionOk, integrityOk) {
        if (connectionOk == true && integrityOk == true) {
            showApp = true
        }
    }

    Scaffold(
        bottomBar = {
            StatusBar(
                viewModel = statusViewModel,
                onShowHistory = { showHistoryDialog = true }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (!showApp) {
                DatabaseStatusScreen(
                    connectionOk = connectionOk,
                    integrityOk = integrityOk,
                    onExit = onExit
                )
            } else {
                val loggedUser by authViewModel.loggedUser.collectAsState()

                LaunchedEffect(loggedUser) {
                    loggedUser?.let { user ->
                        val role = authViewModel.userRole.value ?: "user"

                        // Устанавливаем роль и пользователя в ViewModel заказов
                        orderViewModel.currentAuthUser = user
                        orderViewModel.currentUserRole = role

                        // Для остальных ViewModel тоже можно сразу установить пользователя
                        clientViewModel.currentAuthUser = user
                        categoryViewModel.currentAuthUser = user
                        serviceViewModel.currentAuthUser = user
                        priceViewModel.currentAuthUser = user
                        userViewModel.currentAuthUser = user
                        orderItemViewModel.currentAuthUser = user

                        LoggerService.log("Переход на дашборд")
                        navController.navigate("dashboard") {
                            popUpTo("start") { inclusive = true }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "start"
                ) {
                    authGraph(navController, authViewModel)
                    dashboardGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        onExit = onExit   // <-- передаём колбэк
                    )
                    profileGraph(navController, authViewModel, userViewModel)
                    clientsGraph(navController, clientViewModel)
                    categoriesGraph(navController, categoryViewModel)
                    servicesGraph(navController, serviceViewModel)
                    pricesGraph(navController, priceViewModel)
                    ordersGraph(navController, orderViewModel, orderItemViewModel)
                    usersGraph(navController, userViewModel)
                    adminGraph(navController, adminViewModel)
                }
            }
        }
    }

    if (showHistoryDialog) {
        val logs by statusViewModel.logs.collectAsState()
        HistoryDialog(
            logs = logs,
            onDismiss = { showHistoryDialog = false },
            onClear = { statusViewModel.clearLogs() }
        )
    }
}