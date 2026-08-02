package ru.bel.servicecenter
//
//import android.os.Bundle
//import android.widget.TextView
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//
//class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//        val textView = findViewById<TextView>(R.id.startText)
//        textView.text = "Подключение к базе данных..."
//    }
//}
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
import ru.bel.servicecenter.ui.components.HistoryDialog
import ru.bel.servicecenter.ui.screens.FactoryState
import ru.bel.servicecenter.utils.DataValidator
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
                var tablesExist by remember { mutableStateOf<Boolean?>(null) }
                var adminExists by remember { mutableStateOf<Boolean?>(null) }
                var showApp by remember { mutableStateOf(false) }
                var factoryState by remember { mutableStateOf<FactoryState>(FactoryState.Idle) }
                var factoryError by remember { mutableStateOf<String?>(null) }
                var showHistoryDialog by remember { mutableStateOf(false) }

                fun runChecks() {
                    coroutineScope.launch {
                        connectionOk = null; tablesExist = null; adminExists = null
                        val conn = RepositoryProvider.checkConnection()
                        connectionOk = conn
                        if (conn) {
                            LoggerService.log("Подключение установлено")
                            LoggerService.log("Проверка наличия таблиц в БД")
                            val tables = RepositoryProvider.checkTablesExist()
                            tablesExist = tables
                            if (tables) {
                                LoggerService.log("Таблицы найдены, проверка пользователя admin")
                                adminExists = RepositoryProvider.checkAdminExists()
                                LoggerService.log(if (adminExists == true) "admin существует" else "admin отсутствует")
                            } else {
                                adminExists = false
                                LoggerService.log("Таблицы не найдены")
                            }
                        } else {
                            tablesExist = false; adminExists = false
                            LoggerService.log("Не удалось подключиться к БД")
                        }
                    }
                }

                LaunchedEffect(Unit) { runChecks() }

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
                                tablesExist = tablesExist,
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

                                                // Проверка целостности
                                                val validator = DataValidator()
                                                if (!validator.validate()) {
                                                    throw Exception("Проверка целостности не пройдена – смотрите статусную строку")
                                                }
                                            }
                                            adminExists = true
                                            factoryState = FactoryState.Success
                                            showApp = true   // автоматический переход на стартовый экран
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

                            NavHost(navController, startDestination = "start") {
                                composable("start") {
                                    StartScreen(onNavigate = { navController.navigate(it) })
                                }

                                composable("login") {
                                    LoginScreen(
                                        authController = authController,
                                        onLoginSuccess = {
                                            navController.navigate("clients") {
                                                popUpTo("start") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("register") {
                                    RegisterScreen(
                                        authController = authController,
                                        onRegisterSuccess = {
                                            navController.navigate("login") {
                                                popUpTo("start") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("admin") {
                                    AdminScreen(
                                        adminController = adminController,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                // ------------------- Клиенты -------------------
                                composable("clients") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val clientController = remember { ClientController() }
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
                                    val clientController = remember { ClientController() }
                                    LaunchedEffect(currentUser) {
                                        clientController.currentAuthUser = currentUser
                                    }
                                    ClientEditScreen(
                                        clientController = clientController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                // ------------------- Категории -------------------
                                composable("categories") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val catController = remember { CategoryController() }
                                    LaunchedEffect(currentUser) {
                                        catController.currentAuthUser = currentUser
                                    }
                                    CategoriesListScreen(
                                        categoryController = catController,
                                        onEditCategory = { cat ->
                                            catController.setEditingCategory(cat)
                                            navController.navigate("category_edit")
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("category_edit") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val catController = remember { CategoryController() }
                                    LaunchedEffect(currentUser) {
                                        catController.currentAuthUser = currentUser
                                    }
                                    CategoryEditScreen(
                                        categoryController = catController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                // ------------------- Услуги -------------------
                                composable("services") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val serviceController = remember { ServiceController() }
                                    LaunchedEffect(currentUser) {
                                        serviceController.currentAuthUser = currentUser
                                    }
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
                                    val serviceController = remember { ServiceController() }
                                    LaunchedEffect(currentUser) {
                                        serviceController.currentAuthUser = currentUser
                                    }
                                    ServiceEditScreen(
                                        serviceController = serviceController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                // ------------------- Цены -------------------
                                composable("prices") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val priceController = remember { PriceController() }
                                    LaunchedEffect(currentUser) {
                                        priceController.currentAuthUser = currentUser
                                    }
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
                                    val priceController = remember { PriceController() }
                                    LaunchedEffect(currentUser) {
                                        priceController.currentAuthUser = currentUser
                                    }
                                    PriceEditScreen(
                                        priceController = priceController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                // ------------------- Заказы -------------------
                                composable("orders") {
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val orderController = remember { OrderController() }
                                    LaunchedEffect(currentUser) {
                                        orderController.currentAuthUser = currentUser
                                    }
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
                                    val orderController = remember { OrderController() }
                                    LaunchedEffect(currentUser) {
                                        orderController.currentAuthUser = currentUser
                                    }
                                    OrderEditScreen(
                                        orderController = orderController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                // ------------------- Позиции заказа -------------------
                                composable(
                                    "order_items/{orderId}",
                                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val currentUser by authController.loggedUser.collectAsState()
                                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                                    val orderItemController = remember { OrderItemController() }
                                    orderItemController.currentOrderId = orderId
                                    LaunchedEffect(currentUser) {
                                        orderItemController.currentAuthUser = currentUser
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
                                    val orderItemController = remember { OrderItemController() }
                                    orderItemController.currentOrderId = orderId
                                    LaunchedEffect(currentUser) {
                                        orderItemController.currentAuthUser = currentUser
                                    }
                                    OrderItemEditScreen(
                                        orderItemController = orderItemController,
                                        onSaved = { navController.popBackStack() },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }
                            } // NavHost
                        } // else
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