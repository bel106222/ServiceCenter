package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.viewmodels.OrderItemViewModel
import ru.bel.servicecenter.viewmodels.OrderViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditScreen(
    orderViewModel: OrderViewModel,
    orderItemViewModel: OrderItemViewModel,
    onAddItem: () -> Unit,
    onEditItem: (OrderItem) -> Unit,
    onAttachments: () -> Unit,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentOrder by orderViewModel.currentOrderWithItems.collectAsState()
    val errors by orderViewModel.errors.collectAsState()
    val message by orderViewModel.message.collectAsState()
    val authorName by orderViewModel.authorName.collectAsState()
    val isAdminOrEngineer = orderViewModel.isAdminOrEngineer
    val services by orderViewModel.services.collectAsState()
    val draftAttachments by orderViewModel.draftAttachments.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    val createdDateTime = remember(currentOrder) {
        val raw = currentOrder?.created_at
        if (raw.isNullOrBlank()) {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        } else {
            raw.take(19).replace("T", " ")
        }
    }

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

            OutlinedTextField(
                value = "${currentOrder?.order_number ?: ""} от $createdDateTime",
                onValueChange = {},
                readOnly = true,
                label = { Text("Номер и дата заказа") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = currentOrder?.order_description ?: "",
                onValueChange = { orderViewModel.updateField("description", it) },
                label = { Text("Описание") },
                isError = errors["order_description"] != null,
                supportingText = { errors["order_description"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = authorName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Автор") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Общая сумма: ${currentOrder?.order_sum ?: 0f}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onAttachments) {
                Text("Вложения (${draftAttachments.size})")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = currentOrder?.is_completed ?: false,
                    onCheckedChange = { orderViewModel.updateField("is_completed", it.toString()) },
                    enabled = !isSaving && isAdminOrEngineer
                )
                Text("Завершён")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Позиции заказа", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

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
                            val serviceName = services.find { it.id == item.service_id }?.service_name ?: "Неизвестная услуга"
                            Text("Услуга: $serviceName")
                            Text("Количество: ${item.orderitem_quantity}")
                            Text("Сумма: ${item.orderitem_cost}")
                            if (isAdminOrEngineer) {
                                Row {
                                    TextButton(onClick = { onEditItem(item) }) { Text("Изменить") }
                                    TextButton(onClick = { orderItemViewModel.deleteDraftItem(item) {} }) { Text("Удалить") }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        orderViewModel.saveOrder()
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