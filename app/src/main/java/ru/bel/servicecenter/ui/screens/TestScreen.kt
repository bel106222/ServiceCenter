package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.controllers.OrderController
/**
 * Тестовый экран для инженеров.
 * По нажатию "Старт" загружает все заказы из таблицы orders и выводит их в текстовое поле.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    orderController: OrderController,
    //orders: List<OrderWithItems>,
//    onEditOrder: (OrderWithItems) -> Unit,
//    onDeleteOrder: (OrderWithItems) -> Unit,
//    onAddNewOrder: () -> Unit,
    onBack: () -> Unit
) {
    // Состояние для текста тестовых сообщений
    var messageText by remember { mutableStateOf("") }
    // Флаг выполнения запроса
    var isLoading by remember { mutableStateOf(false) }
    // Скоуп для запуска корутин
    val coroutineScope = rememberCoroutineScope()

//    LaunchedEffect(Unit) {
//        orderController.loadOrders()
//    }
    val ordersWithItems by orderController.ordersWithItems.collectAsState()
    val orders = ordersWithItems

    Scaffold(
        topBar = {
            TopAppBar(
                //title = { Text("Заказы") },
                title = { Text(ordersWithItems.size.toString()) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(orders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Заказ № ${order.order_number}")
                        Text("Описание: ${order.order_description}")
                        Text("Сумма: ${order.order_sum}")
                        Text("Позиций: ${order.order_items.size}")
                        Row {
                            //TextButton(onClick = { onEditOrder(order) }) { Text("Изменить") }
                            //TextButton(onClick = { onDeleteOrder(order) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }

//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Заголовок сверху
//        Text(
//            "Тестовый экран",
//            style = MaterialTheme.typography.headlineMedium,
//            modifier = Modifier.align(Alignment.CenterHorizontally)
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Кнопка "Старт" с индикацией загрузки
//        Button(
//            onClick = {
//                isLoading = true
//                messageText = "Загрузка заказов...\n"
//                coroutineScope.launch {
//                    try {
//                        val ordersWithItems = RepositoryProvider.orderRepo.getOrdersWithItems()
//                        val text = buildString {
//                            ordersWithItems.forEach { order ->
//                                append("=== Заказ ${order.order_number} ===\n")
//                                append("ID: ${order.id}\n")
//                                append("Описание: ${order.order_description}\n")
//                                append("Сумма: ${order.order_sum}\n")
//                                append("Завершён: ${order.is_completed}\n")
//                                append("Повременная оплата: ${order.is_time}\n")
//                                append("Дата создания: ${order.created_at}\n")
//                                append("Позиции (${order.order_items.size}):\n")
//                                if (order.order_items.isEmpty()) {
//                                    append("   (нет позиций)\n")
//                                } else {
//                                    order.order_items.forEachIndexed { index, item ->
//                                        append("   --- Позиция ${index + 1} ---\n")
//                                        append("   ID позиции: ${item.id}\n")
//                                        append("   Услуга: ${item.services?.service_name ?: "Не указано"}\n")
//                                        append("   Количество: ${item.orderitem_quantity}\n")
//                                        append("   Стоимость: ${item.orderitem_cost}\n")
//                                        append("   Удалённое выполнение: ${item.is_online}\n")
//                                        append("   Дата создания: ${item.created_at}\n")
//                                    }
//                                }
//                                append("\n")
//                            }
//                            if (ordersWithItems.isEmpty()) {
//                                append("Заказов нет.")
//                            }
//                        }
//                        messageText = text
//                        LoggerService.log("Тестовый запрос выполнен успешно, заказов: ${ordersWithItems.size}")
//                    } catch (e: Exception) {
//                        messageText = "Ошибка: ${e.message}"
//                        LoggerService.log("Ошибка тестового запроса: ${e.message}")
//                    } finally {
//                        isLoading = false
//                    }
//                }
//            },
//            enabled = !isLoading,
//            modifier = Modifier.align(Alignment.CenterHorizontally)
//        ) {
//            if (isLoading) {
//                CircularProgressIndicator(modifier = Modifier.size(20.dp))
//            } else {
//                Text("Старт")
//            }
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Текстовая область для вывода сообщений
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(1f)
//        ) {
//            Text(
//                text = messageText,
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(rememberScrollState())
//            )
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Кнопка "Назад" внизу
//        Button(
//            onClick = onBack,
//            modifier = Modifier
//                .fillMaxWidth()
//                .align(Alignment.CenterHorizontally)
//        ) {
//            Text("Назад")
//        }
//    }
}