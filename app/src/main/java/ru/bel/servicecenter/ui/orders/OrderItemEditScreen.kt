package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.viewmodels.OrderItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderItemEditScreen(
    orderItemViewModel: OrderItemViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentItem by orderItemViewModel.currentItem.collectAsState()
    val services by orderItemViewModel.services.collectAsState()
    val selectedService by orderItemViewModel.selectedService.collectAsState()

    var serviceDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        orderItemViewModel.loadServices()
    }

    LaunchedEffect(selectedService, currentItem?.orderitem_quantity) {
        orderItemViewModel.recalculateCost()
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
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = serviceDropdownExpanded,
                onDismissRequest = { serviceDropdownExpanded = false }
            ) {
                services.forEach { service ->
                    DropdownMenuItem(
                        text = { Text(service.service_name) },
                        onClick = {
                            orderItemViewModel.selectService(service)
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
                orderItemViewModel.updateQuantity(qty)
            },
            label = { Text("Количество") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentItem?.orderitem_cost?.toString() ?: "0",
            onValueChange = { value ->
                val cost = value.toFloatOrNull() ?: 0f
                orderItemViewModel.updateCost(cost)
            },
            label = { Text("Стоимость") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = currentItem?.is_online ?: false,
                onCheckedChange = { orderItemViewModel.updateIsOnline(it) }
            )
            Text("Удалённое выполнение")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                orderItemViewModel.saveItem(onSaved = onSaved)
            }) { Text("Сохранить") }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) { Text("Отмена") }
        }
    }
}