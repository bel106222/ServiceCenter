package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.viewmodels.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orderViewModel: OrderViewModel,
    onEditOrder: (OrderWithItems) -> Unit,
    onDeleteOrder: (OrderWithItems) -> Unit,
    onAddNewOrder: () -> Unit,
    onBack: () -> Unit
) {
    val orders by orderViewModel.ordersWithItems.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val message by orderViewModel.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        orderViewModel.loadOrders()
    }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказы") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewOrder) { Text("+") }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(orders) { order ->
                    // Цвет обводки: яркий для незавершённых, нейтральный для завершённых
                    val borderColor = if (!order.is_completed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(2.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Заказ № ${order.order_number} от ${order.created_at.take(16).replace("T", " ")}")
                            Text("Описание: ${order.order_description}")
                            Text("Сумма: ${order.order_sum}")
                            Text("Оказано услуг: ${order.order_items.size}")
                            Row {
                                TextButton(onClick = { onEditOrder(order) }) { Text("Изменить") }
                                TextButton(onClick = { onDeleteOrder(order) }) { Text("Удалить") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMessage) {
        AlertDialog(
            onDismissRequest = { showMessage = false },
            title = { Text("Сообщение") },
            text = { Text(message ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    showMessage = false
                    orderViewModel.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}