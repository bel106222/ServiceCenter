package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(orders) { order ->
                val cardColor = if (!order.is_completed) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Заказ № ${order.order_number}")
                        Text("Описание: ${order.order_description}")
                        Text("Сумма: ${order.order_sum}")
                        Text(if (order.is_completed) "Статус: Завершён" else "Статус: Требует выполнения")
                        Row {
                            TextButton(onClick = { onEditOrder(order) }) { Text("Изменить") }
                            TextButton(onClick = { onDeleteOrder(order) }) { Text("Удалить") }
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