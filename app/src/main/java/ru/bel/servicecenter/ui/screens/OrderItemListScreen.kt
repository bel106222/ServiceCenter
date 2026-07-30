package ru.bel.servicecenter.ui.screens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.OrderItemController
import ru.bel.servicecenter.models.OrderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderItemListScreen(
    orderItemController: OrderItemController = viewModel(),
    onEditItem: (OrderItem) -> Unit,
    onBack: () -> Unit
) {
    val items by orderItemController.items.collectAsState()
    val message by orderItemController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Позиции заказа") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                orderItemController.setEditingItem(OrderItem(order_id = orderItemController.currentOrderId, service_id = "", user_id = ""))
                onEditItem(orderItemController.currentItem.value)
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Услуга ID: ${item.service_id}")
                        Text("Кол-во: ${item.orderitem_quantity}, Сумма: ${item.orderitem_cost}")
                        Text("Инженер: ${item.user_id}")
                        Row {
                            TextButton(onClick = { onEditItem(item) }) { Text("Изменить") }
                            TextButton(onClick = { orderItemController.deleteItem(item) }) { Text("Удалить") }
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
            confirmButton = { TextButton(onClick = { showMessage = false }) { Text("OK") } }
        )
    }
}