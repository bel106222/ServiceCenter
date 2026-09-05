package ru.bel.servicecenter.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.bel.servicecenter.controllers.*
import ru.bel.servicecenter.factory.DataFactory
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.controllers.StatusViewModel
import ru.bel.servicecenter.controllers.AuthController
import ru.bel.servicecenter.ui.auth.LoginScreen
import ru.bel.servicecenter.ui.components.HistoryDialog
import ru.bel.servicecenter.ui.components.StatusBar
import ru.bel.servicecenter.ui.test.TestScreen
import ru.bel.servicecenter.ui.status.DatabaseStatusScreen
import ru.bel.servicecenter.utils.DataValidator
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.StatusTree
import timber.log.Timber

/**
 * Главный компонент навигации.
 * Содержит состояние приложения, контроллеры и NavHost.
 */
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
    val authController = remember { AuthController() }

    var connectionOk by remember { mutableStateOf<Boolean?>(null) }
    var integrityOk by remember { mutableStateOf<Boolean?>(null) }
    var showApp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        connectionOk = RepositoryProvider.checkConnection()
        if (connectionOk == true) {
            integrityOk = RepositoryProvider.checkAdminExists()
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
                val loggedUser by authController.loggedUser.collectAsState()

                LaunchedEffect(loggedUser, authController.userRole.value) {
                    if (loggedUser != null && authController.userRole.value != null) {
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
                    authGraph(navController, authController)
                    dashboardGraph(navController, authController)
                    // другие графы по мере добавления
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