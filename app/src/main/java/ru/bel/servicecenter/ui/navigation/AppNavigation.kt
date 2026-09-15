package ru.bel.servicecenter.ui.navigation

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.ui.components.HistoryDialog
import ru.bel.servicecenter.ui.components.StatusBar
import ru.bel.servicecenter.ui.status.DatabaseStatusScreen
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.MessageBus
import ru.bel.servicecenter.utils.RoleCache
import ru.bel.servicecenter.utils.StatusTree
import ru.bel.servicecenter.viewmodels.*
import ru.bel.servicecenter.viewmodels.ReportsViewModel
import timber.log.Timber

@Composable
fun AppNavigation(onExit: () -> Unit) {
    val statusViewModel = remember { StatusViewModel() }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Настройка логирования
    LaunchedEffect(Unit) {
        Timber.uprootAll()
        Timber.plant(StatusTree())
        LoggerService.log("Приложение запущено")
    }

    // Подписка на глобальные сообщения для Snackbar
    LaunchedEffect(Unit) {
        MessageBus.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
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
    val reportsViewModel = remember { ReportsViewModel() }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            StatusBar(
                viewModel = statusViewModel,
                onShowHistory = { showHistoryDialog = true }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                val userRole by authViewModel.userRole.collectAsState()

                LaunchedEffect(loggedUser, userRole) {
                    val user = loggedUser ?: return@LaunchedEffect
                    val role = userRole ?: "user"

                    orderViewModel.currentAuthUser = user
                    orderViewModel.currentUserRole = role
                    clientViewModel.currentAuthUser = user
                    categoryViewModel.currentAuthUser = user
                    serviceViewModel.currentAuthUser = user
                    priceViewModel.currentAuthUser = user
                    userViewModel.currentAuthUser = user
                    orderItemViewModel.currentAuthUser = user
                    adminViewModel.currentAuthUser = user
                    reportsViewModel.currentAuthUser = user

                    LoggerService.log("Навигация: роль=$role, client_id=${user.client_id}")

                    when (role) {
                        "user" -> {
                            if (user.client_id.isNullOrBlank()) {
                                navController.navigate("not_a_client") {
                                    popUpTo("start") { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate("dashboard") {
                                    popUpTo("start") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        else -> {
                            navController.navigate("dashboard") {
                                popUpTo("start") { inclusive = true }
                                launchSingleTop = true
                            }
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
                        onExit = onExit
                    )
                    profileGraph(navController, authViewModel, userViewModel)
                    clientsGraph(navController, clientViewModel)
                    categoriesGraph(navController, categoryViewModel)
                    servicesGraph(navController, serviceViewModel)
                    pricesGraph(navController, priceViewModel)
                    ordersGraph(navController, orderViewModel, orderItemViewModel)
                    usersGraph(navController, userViewModel)
                    adminGraph(navController, adminViewModel)
                    reportsGraph(navController, authViewModel, reportsViewModel)
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