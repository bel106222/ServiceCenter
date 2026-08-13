package ru.bel.servicecenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.bel.servicecenter.controllers.*
import ru.bel.servicecenter.factory.DataFactory
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.ui.screens.*
import ru.bel.servicecenter.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.bel.servicecenter.ui.components.StatusBar
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.StatusTree
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.style.TextOverflow
import ru.bel.servicecenter.controllers.StatusViewModel
import ru.bel.servicecenter.controllers.UserManagementViewModel
import ru.bel.servicecenter.ui.components.HistoryDialog
import ru.bel.servicecenter.utils.DataValidator
import ru.bel.servicecenter.ui.screens.FactoryState
import ru.bel.servicecenter.ui.screens.SelectClientScreen
import ru.bel.servicecenter.ui.screens.LoginScreen
import ru.bel.servicecenter.ui.screens.NotAClientScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {

    companion object {
        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        Timber.plant(Timber.DebugTree())

        setContent {
            AppTheme {
                val statusViewModel = remember { StatusViewModel() }

                LaunchedEffect(Unit) {
                    Timber.uprootAll()
                    Timber.plant(StatusTree())
                    LoggerService.log("Приложение запущено")
                }

                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()

                var connectionOk by remember { mutableStateOf<Boolean?>(null) }
                var adminExists by remember { mutableStateOf<Boolean?>(null) }
                var showApp by remember { mutableStateOf(false) }
                var factoryState by remember { mutableStateOf<FactoryState>(FactoryState.Idle) }
                var showHistoryDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val conn = RepositoryProvider.checkConnection()
                    connectionOk = conn
                    if (conn) {
                        adminExists = RepositoryProvider.checkAdminExists()
                    } else {
                        adminExists = false
                    }
                }

                LaunchedEffect(adminExists) {
                    if (adminExists == true) {
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
                                adminExists = adminExists,
                                factoryState = factoryState,
                                onRunFactory = { password ->
                                    factoryState = FactoryState.Running
                                    coroutineScope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val factory = DataFactory()
                                                val adminId = factory.createInitialStructure()
                                                factory.setAdminPassword(adminId, password)
                                                val validator = DataValidator()
                                                if (!validator.validate()) {
                                                    throw Exception("Проверка целостности не пройдена – смотрите статусную строку")
                                                }
                                            }
                                            adminExists = true
                                            factoryState = FactoryState.Success
                                            showApp = true
                                        } catch (e: Exception) {
                                            factoryState = FactoryState.Error(e.message ?: "Неизвестная ошибка")
                                            LoggerService.log("Ошибка фабрики: ${e.message}")
                                        }
                                    }
                                },
                                onExit = { finish() }
                            )
                        } else {
                            val authController = remember { AuthController() }
                            val adminController = remember { AdminController() }
                            val userController = remember { UserController() }
                            val userManagementViewModel = remember { UserManagementViewModel() }
                            val clientController = remember { ClientController() }
                            val categoryController = remember { CategoryController() }
                            val loggedUser by authController.loggedUser.collectAsState()

                            LaunchedEffect(loggedUser) {
                                loggedUser?.let { user ->
                                    val roleName = authController.getRoleName(user.role_id)
                                    when (roleName) {
                                        "user" -> {
                                            if (user.client_id.isNullOrBlank()) {
                                                navController.navigate("not_a_client") {
                                                    popUpTo("start") { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate("dashboard") {
                                                    popUpTo("start") { inclusive = true }
                                                }
                                            }
                                        }
                                        else -> {
                                            navController.navigate("dashboard") {
                                                popUpTo("start") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }

                            NavHost(navController, startDestination = "start") {
                                composable("start") {
                                    StartScreen(onNavigate = { navController.navigate(it) })
                                }

                                composable("login") {
                                    LoginScreen(authController = authController)
                                }

                                composable("register") {
                                    RegisterScreen(authController = authController)
                                }

                                composable("not_a_client") {
                                    NotAClientScreen(
                                        onContinue = {
                                            navController.navigate("select_client") {
                                                popUpTo("not_a_client") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("select_client") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    currentUser?.let { user ->
                                        SelectClientScreen(
                                            currentUser = user,
                                            onClientBound = {
                                                navController.navigate("dashboard") {
                                                    popUpTo("select_client") { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                }

                                composable("dashboard") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    currentUser?.let { user ->
                                        val roleName = remember { mutableStateOf<String?>(null) }
                                        LaunchedEffect(user) {
                                            roleName.value = authController.getRoleName(user.role_id)
                                        }
                                        roleName.value?.let { role ->
                                            val items = when (role) {
                                                "user" -> listOf(
                                                    DashboardItem("Профиль") { navController.navigate("user_profile") },
                                                    DashboardItem("Мои заказы") { navController.navigate("orders") }
                                                )
                                                "engineer" -> listOf(
                                                    DashboardItem("Профиль") { navController.navigate("engineer_profile") },
                                                    DashboardItem("Клиенты") { navController.navigate("clients") },
                                                    DashboardItem("Категории") { navController.navigate("categories") },
                                                    DashboardItem("Цены") { navController.navigate("prices") },
                                                    DashboardItem("Услуги") { navController.navigate("services") },
                                                    DashboardItem("Заказы") { navController.navigate("orders") }
                                                )
                                                "admin" -> listOf(
                                                    DashboardItem("Профиль") { navController.navigate("admin_profile") },
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
                                    }
                                }

                                composable("user_profile") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    currentUser?.let { user ->
                                        UserProfileScreen(
                                            currentUser = user,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }

                                composable("engineer_profile") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    currentUser?.let { user ->
                                        EngineerProfileScreen(
                                            currentUser = user,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }

                                composable("admin_profile") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    currentUser?.let { user ->
                                        AdminProfileScreen(
                                            currentUser = user,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }

                                composable("users") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    LaunchedEffect(currentUser) {
                                        userManagementViewModel.userController.currentAuthUser = currentUser
                                    }
                                    UsersListScreen(
                                        viewModel = userManagementViewModel,
                                        onEditUser = { user ->
                                            userManagementViewModel.userController.setEditingUser(user)
                                            navController.navigate("user_edit")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("user_edit") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    LaunchedEffect(currentUser) {
                                        userManagementViewModel.userController.currentAuthUser = currentUser
                                    }
                                    UserEditScreen(
                                        viewModel = userManagementViewModel,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable("clients") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    LaunchedEffect(currentUser) {
                                        clientController.currentAuthUser = currentUser
                                    }
                                    ClientsListScreen(
                                        clientController = clientController,
                                        onEditClient = { client ->
                                            clientController.setEditingClient(client)
                                            navController.navigate("client_edit")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("client_edit") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    LaunchedEffect(currentUser) {
                                        clientController.currentAuthUser = currentUser
                                    }
                                    ClientEditScreen(
                                        clientController = clientController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable("categories") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    LaunchedEffect(currentUser) {
                                        categoryController.currentAuthUser = currentUser
                                    }
                                    CategoriesListScreen(
                                        categoryController = categoryController,
                                        onEditCategory = { cat ->
                                            categoryController.setEditingCategory(cat)
                                            navController.navigate("category_edit")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("category_edit") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    LaunchedEffect(currentUser) {
                                        categoryController.currentAuthUser = currentUser
                                    }
                                    CategoryEditScreen(
                                        categoryController = categoryController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable("services") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val serviceController = remember { ServiceController().apply { currentAuthUser = currentUser } }
                                    ServicesListScreen(
                                        serviceController = serviceController,
                                        onEditService = { srv ->
                                            serviceController.setEditingService(srv)
                                            navController.navigate("service_edit")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("service_edit") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val serviceController = remember { ServiceController().apply { currentAuthUser = currentUser } }
                                    ServiceEditScreen(
                                        serviceController = serviceController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable("prices") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val priceController = remember { PriceController().apply { currentAuthUser = currentUser } }
                                    PriceListScreen(
                                        priceController = priceController,
                                        onEditPrice = { price ->
                                            priceController.setEditingPrice(price)
                                            navController.navigate("price_edit")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("price_edit") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val priceController = remember { PriceController().apply { currentAuthUser = currentUser } }
                                    PriceEditScreen(
                                        priceController = priceController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable("orders") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val orderController = remember { OrderController().apply { currentAuthUser = currentUser } }
                                    OrdersListScreen(
                                        orderController = orderController,
                                        onEditOrder = { order ->
                                            orderController.setEditingOrder(order)
                                            navController.navigate("order_edit")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("order_edit") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val orderController = remember { OrderController().apply { currentAuthUser = currentUser } }
                                    OrderEditScreen(
                                        orderController = orderController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    "order_items/{orderId}",
                                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                                    val orderItemController = remember {
                                        OrderItemController().apply {
                                            this.currentOrderId = orderId
                                            this.currentAuthUser = currentUser
                                        }
                                    }
                                    OrderItemListScreen(
                                        orderItemController = orderItemController,
                                        onEditItem = { item ->
                                            orderItemController.setEditingItem(item)
                                            navController.navigate("order_item_edit/$orderId")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    "order_item_edit/{orderId}",
                                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                                    val orderItemController = remember {
                                        OrderItemController().apply {
                                            this.currentOrderId = orderId
                                            this.currentAuthUser = currentUser
                                        }
                                    }
                                    OrderItemEditScreen(
                                        orderItemController = orderItemController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable("admin_database") {
                                    AdminDatabaseScreen(
                                        adminController = adminController,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            } // NavHost
                        }
                    }
                } // Scaffold

                if (showHistoryDialog) {
                    val logs by statusViewModel.logs.collectAsState()
                    HistoryDialog(
                        logs = logs,
                        onDismiss = { showHistoryDialog = false },
                        onClear = { statusViewModel.clearLogs() }
                    )
                }
            } // AppTheme
        } // setContent
    } // onCreate
} // MainActivity