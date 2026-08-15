package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Order

/**
 * Экран списка заказов.
 * Получает уже загруженный список и отображает его без дополнительных запросов.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orders: List<Order>,
    onEditOrder: (Order) -> Unit,
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
                        Row {
                            TextButton(onClick = { onEditOrder(order) }) { Text("Изменить") }
                            // Кнопка удаления может быть добавлена позже, если требуется.
                            // Для неё потребуется доступ к OrderController в этом экране,
                            // но текущая задача не подразумевает удаление.
                        }
                    }
                }
            }
        }
    }
}