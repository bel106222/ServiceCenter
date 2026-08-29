package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.OrderController
import ru.bel.servicecenter.models.OrderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditScreen(
    orderController: OrderController,
    onAddItem: () -> Unit,
    onEditItem: (OrderItem) -> Unit,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentOrder by orderController.currentOrderWithItems.collectAsState()
    val errors by orderController.errors.collectAsState()
    val message by orderController.message.collectAsState()
    val authorName by orderController.authorName.collectAsState()
    val isAdminOrEngineer = orderController.isAdminOrEngineer

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Заказ создан" || message == "Заказ обновлён") {
                onSaved()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isAdminOrEngineer && currentOrder != null) {
                FloatingActionButton(onClick = onAddItem) { Text("+") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Редактирование заказа", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // Номер заказа (не редактируется)
            OutlinedTextField(
                value = currentOrder?.order_number ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Номер заказа") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Описание заказа
            OutlinedTextField(
                value = currentOrder?.order_description ?: "",
                onValueChange = { orderController.updateField("description", it) },
                label = { Text("Описание") },
                isError = errors["order_description"] != null,
                supportingText = { errors["order_description"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Автор (используем authorName из контроллера)
            OutlinedTextField(
                value = authorName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Автор") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Сумма заказа
            Text(
                text = "Общая сумма: ${currentOrder?.order_sum ?: 0f}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Заголовок позиций
            Text("Позиции заказа", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Список позиций или сообщение об отсутствии
            val orderItems = currentOrder?.order_items ?: emptyList()
            if (orderItems.isEmpty()) {
                Text("По заказу услуг не оказано.")
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(orderItems) { item ->   // ← переменная item определена здесь
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val serviceName = item.services?.service_name ?: "Неизвестная услуга"
                                Text("Услуга: $serviceName")
                                Text("Количество: ${item.orderitem_quantity}")
                                Text("Сумма: ${item.orderitem_cost}")
                                if (isAdminOrEngineer) {
                                    Row {
                                        TextButton(onClick = { onEditItem(item) }) { Text("Изменить") }
                                        TextButton(onClick = { orderController.deleteOrderItem(item) }) { Text("Удалить") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранение...")
                    }
                } else {
                    Button(onClick = {
                        isSaving = true
                        orderController.saveOrder()
                    }) { Text("Сохранить") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("Отмена") }
                }
            }
        }
    }
}