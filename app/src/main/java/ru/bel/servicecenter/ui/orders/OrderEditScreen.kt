package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.viewmodels.OrderItemViewModel
import ru.bel.servicecenter.viewmodels.OrderViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val authorName by orderViewModel.authorName.collectAsState()
    val isAdminOrEngineer = orderViewModel.isAdminOrEngineer
    val services by orderViewModel.services.collectAsState()
    val draftAttachments by orderViewModel.draftAttachments.collectAsState()
    val isLoadingAttachments by orderViewModel.isLoadingAttachments.collectAsState()
    val operationCompleted by orderViewModel.operationCompleted.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    val createdDateTime = remember(currentOrder) {
        val raw = currentOrder?.created_at
        if (raw.isNullOrBlank()) {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        } else {
            raw.take(19).replace("T", " ")
        }
    }

    LaunchedEffect(operationCompleted) {
        if (operationCompleted) {
            isSaving = false
            orderViewModel.resetOperationCompleted()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказ") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdminOrEngineer && currentOrder != null) {
                FloatingActionButton(onClick = onAddItem) {
                    Icon(Icons.Default.Build, contentDescription = "Добавить услугу")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = "${currentOrder?.order_number ?: ""} от $createdDateTime",
                onValueChange = {},
                readOnly = true,
                label = { Text("Номер и дата заказа") },
                leadingIcon = { Icon(Icons.Default.Receipt, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = currentOrder?.order_description ?: "",
                onValueChange = { orderViewModel.updateField("description", it) },
                label = { Text("Описание") },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                isError = errors["order_description"] != null,
                supportingText = { errors["order_description"]?.let { Text(it) } },
                minLines = 2,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = authorName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Автор") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = "${currentOrder?.order_sum ?: 0f} руб.",
                onValueChange = {},
                readOnly = true,
                label = { Text("Общая сумма") },
                leadingIcon = { Icon(Icons.Default.Payments, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAttachments,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isLoadingAttachments) "Вложения (загрузка...)"
                    else "Вложения (${draftAttachments.size})"
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Завершён", modifier = Modifier.weight(1f))
                Switch(
                    checked = currentOrder?.is_completed ?: false,
                    onCheckedChange = { orderViewModel.updateField("is_completed", it.toString()) },
                    enabled = !isSaving && isAdminOrEngineer
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Позиции заказа", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val orderItems = currentOrder?.order_items ?: emptyList()
            if (orderItems.isEmpty()) {
                Text("По заказу услуг не оказано.")
            } else {
                orderItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val serviceName = services.find { it.id == item.service_id }?.service_name
                                ?: "Неизвестная услуга"
                            Text("Услуга: $serviceName", style = MaterialTheme.typography.titleMedium)
                            Text("Количество: ${item.orderitem_quantity}")
                            Text("Сумма: ${item.orderitem_cost} руб.")
                            if (isAdminOrEngineer) {
                                Row {
                                    TextButton(onClick = { onEditItem(item) }) {
                                        Text("Изменить")
                                    }
                                    TextButton(onClick = { orderViewModel.deleteItemFromDraft(item.id) }) {
                                        Text("Удалить")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()) {
                if (isSaving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранение...")
                    }
                } else {
                    Button(onClick = {
                        isSaving = true
                        orderViewModel.saveOrder()
                    }) {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить")
                    }
                    Spacer(Modifier.width(16.dp))
                    FilledTonalButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Отмена")
                    }
                }
            }
        }
    }
}