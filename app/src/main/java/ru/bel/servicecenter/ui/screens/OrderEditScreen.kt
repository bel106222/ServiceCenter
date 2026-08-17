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
    val currentOrder by orderController.currentOrder.collectAsState()
    val orderItems by orderController.orderItems.collectAsState()
    val errors by orderController.errors.collectAsState()
    val message by orderController.message.collectAsState()
    val authorName by orderController.authorName.collectAsState()
    val isAdminOrEngineer = orderController.isAdminOrEngineer
    val services by orderController.services.collectAsState()
    var isSaving by remember { mutableStateOf(false) }

    // Загружаем позиции при открытии, если заказ уже существует
    LaunchedEffect(currentOrder?.id) {
        currentOrder?.id?.let { orderId ->
            if (orderId.isNotEmpty()) {
                orderController.loadOrderItems(orderId)
            }
        }
    }

    // Обработка сообщений
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
            // Кнопка добавления позиции доступна только инженеру/админу
            if (isAdminOrEngineer && currentOrder != null) {
                FloatingActionButton(onClick = onAddItem) {
                    Text("+")
                }
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

            // Описание
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

            // Автор (не редактируется)
            OutlinedTextField(
                value = authorName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Автор") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Признак завершённости (для user заблокирован)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = currentOrder?.is_completed ?: false,
                    onCheckedChange = { orderController.updateField("is_completed", it.toString()) },
                    enabled = !isSaving && isAdminOrEngineer
                )
                Text("Завершён")
            }

            // Признак почасовой оплаты (виден только инженеру/админу)
            if (isAdminOrEngineer) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = currentOrder?.is_time ?: false,
                        onCheckedChange = { orderController.updateField("is_time", it.toString()) },
                        enabled = !isSaving
                    )
                    Text("Почасовая оплата")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Позиции заказа", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Отображение позиций или сообщения об их отсутствии
            if (orderItems.isEmpty()) {
                Text("По заказу услуг не оказано.")
            } else {
                LazyColumn {
                    items(orderItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val serviceName = services.find { it.id == item.service_id }?.service_name ?: "Неизвестная услуга"
                                Text("Услуга: $serviceName")
                                Text("Количество: ${item.orderitem_quantity}")
                                Text("Сумма: ${item.orderitem_cost}")
                                // Кнопки изменения/удаления только для инженера/админа
                                if (isAdminOrEngineer) {
                                    Row {
                                        TextButton(onClick = { onEditItem(item) }) { Text("Изменить") }
                                        TextButton(onClick = {
                                            orderController.deleteOrderItem(item)
                                        }) { Text("Удалить") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
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
                    Button(onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("Отмена") }
                }
            }
        }
    }
}