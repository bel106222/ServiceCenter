package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MenuAnchorType
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.OrderItemController
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.rules.ValidationRules
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderItemEditScreen(
    orderItemController: OrderItemController,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentItem by orderItemController.currentItem.collectAsState()
    val services by orderItemController.services.collectAsState()
    val selectedService by orderItemController.selectedService.collectAsState()
    val message by orderItemController.message.collectAsState()

    var isSaving by remember { mutableStateOf(false) }
    var serviceDropdownExpanded by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var costError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedService, currentItem?.orderitem_quantity) {
        orderItemController.recalculateCost()
    }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Позиция добавлена" || message == "Позиция обновлена") {
                onSaved()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Позиция заказа", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = serviceDropdownExpanded,
            onExpandedChange = { serviceDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedService?.service_name ?: "Выберите услугу",
                onValueChange = {},
                readOnly = true,
                label = { Text("Услуга") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                enabled = !isSaving
            )
            ExposedDropdownMenu(
                expanded = serviceDropdownExpanded,
                onDismissRequest = { serviceDropdownExpanded = false }
            ) {
                services.forEach { service ->
                    DropdownMenuItem(
                        text = { Text(service.service_name) },
                        onClick = {
                            orderItemController.selectService(service)
                            serviceDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentItem?.orderitem_quantity?.toString() ?: "1",
            onValueChange = { value ->
                val qty = value.toIntOrNull() ?: 1
                orderItemController.updateQuantity(qty)
                quantityError = if (qty <= 0) "Количество должно быть > 0" else null
            },
            label = { Text("Количество") },
            isError = quantityError != null,
            supportingText = { quantityError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentItem?.orderitem_cost?.toString() ?: "0",
            onValueChange = { value ->
                val cost = value.toFloatOrNull() ?: 0f
                orderItemController.updateCost(cost)
                costError = if (cost <= 0) "Стоимость должна быть > 0" else null
            },
            label = { Text("Стоимость") },
            isError = costError != null,
            supportingText = { costError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = currentItem?.is_online ?: false,
                onCheckedChange = { orderItemController.updateIsOnline(it) },
                enabled = !isSaving
            )
            Text("Удалённое выполнение")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            if (isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранение...")
                }
            } else {
                Button(onClick = {
                    isSaving = true
                    orderItemController.saveItem(onSaved = {})
                }) { Text("Сохранить") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Отмена") }
            }
        }
    }
}