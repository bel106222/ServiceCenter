package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.OrderController
import ru.bel.servicecenter.models.OrderWithItems

/**
 * Экран списка заказов.
 * Принимает готовый список OrderWithItems и отображает его.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orders: List<OrderWithItems>,
    onEditOrder: (OrderWithItems) -> Unit,
    onDeleteOrder: (OrderWithItems) -> Unit,
    onAddNewOrder: () -> Unit,
    onBack: () -> Unit
) {
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
                            TextButton(onClick = { onEditOrder(order) }) { Text("Изменить") }
                            TextButton(onClick = { onDeleteOrder(order) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}