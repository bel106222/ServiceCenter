package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    var quantityError by remember { mutableStateOf<String?>(null) }
    var costError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        orderItemViewModel.loadServices()
    }

    LaunchedEffect(selectedService, currentItem?.orderitem_quantity) {
        orderItemViewModel.recalculateCost()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Позиция заказа") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            ExposedDropdownMenuBox(
                expanded = serviceDropdownExpanded,
                onExpandedChange = { serviceDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedService?.service_name ?: "Выберите услугу",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Услуга") },
                    leadingIcon = { Icon(Icons.Default.Build, null) },
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
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = currentItem?.orderitem_quantity?.toString() ?: "1",
                onValueChange = { value ->
                    val qty = value.toIntOrNull() ?: 1
                    orderItemViewModel.updateQuantity(qty)
                    quantityError = if (qty <= 0) "Количество должно быть > 0" else null
                },
                label = { Text("Количество") },
                leadingIcon = { Icon(Icons.Default.Numbers, null) },
                isError = quantityError != null,
                supportingText = { quantityError?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = currentItem?.orderitem_cost?.toString() ?: "0",
                onValueChange = { value ->
                    val cost = value.toFloatOrNull() ?: 0f
                    orderItemViewModel.updateCost(cost)
                    costError = if (cost <= 0) "Стоимость должна быть > 0" else null
                },
                label = { Text("Стоимость (руб.)") },
                leadingIcon = { Icon(Icons.Default.Payments, null) },
                isError = costError != null,
                supportingText = { costError?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Удалённое выполнение", modifier = Modifier.weight(1f))
                Switch(
                    checked = currentItem?.is_online ?: false,
                    onCheckedChange = { orderItemViewModel.updateIsOnline(it) }
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    orderItemViewModel.saveItem(onSaved = onSaved)
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