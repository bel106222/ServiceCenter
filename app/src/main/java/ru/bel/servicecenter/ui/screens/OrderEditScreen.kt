package ru.bel.servicecenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.OrderController
import ru.bel.servicecenter.models.OrderItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    // Формируем дату для отображения
    val createdDateTime = remember(currentOrder) {
        val raw = currentOrder?.created_at
        if (raw.isNullOrBlank()) {
            // Если дата не задана (создание нового заказа), используем текущее время
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        } else {
            // Для редактирования берём первые 19 символов (до секунд) и заменяем T на пробел
            raw.take(19).replace("T", " ")
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
                .verticalScroll(rememberScrollState())
        ) {
            Text("Редактирование заказа", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // Поле "Номер и дата заказа"
            OutlinedTextField(
                value = "${currentOrder?.order_number ?: ""} от $createdDateTime",
                onValueChange = {},
                readOnly = true,
                label = { Text("Номер и дата заказа") },
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

            // Сумма заказа
            Text(
                text = "Общая сумма: ${currentOrder?.order_sum ?: 0f}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Чекбокс "Завершён"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = currentOrder?.is_completed ?: false,
                    onCheckedChange = { orderController.updateField("is_completed", it.toString()) },
                    enabled = !isSaving && isAdminOrEngineer
                )
                Text("Завершён")
            }

            // Чекбокс "Почасовая оплата" (виден только инженеру/админу)
//            if (isAdminOrEngineer) {
//                Spacer(modifier = Modifier.height(8.dp))
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Checkbox(
//                        checked = currentOrder?.is_time ?: false,
//                        onCheckedChange = { orderController.updateField("is_time", it.toString()) },
//                        enabled = !isSaving
//                    )
//                    Text("Почасовая оплата")
//                }
//            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Позиции заказа", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Позиции заказа
            val orderItems = currentOrder?.order_items ?: emptyList()
            if (orderItems.isEmpty()) {
                Text("По заказу услуг не оказано.")
            } else {
                orderItems.forEach { item ->
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

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопки сохранения
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