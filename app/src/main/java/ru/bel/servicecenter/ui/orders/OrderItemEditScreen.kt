package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

    // Управляет показом нижней панели со списком услуг
    var showServiceSheet by remember { mutableStateOf(false) }
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
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
            // ============================================================
            // Поле выбора услуги.
            // Раньше это был ExposedDropdownMenuBox — теперь карточка,
            // которая открывает нижнюю панель со списком.
            // ============================================================
            OutlinedCard(
                onClick = { showServiceSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // Мелкая подпись «Услуга»
                        Text(
                            text = "Услуга",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        // Крупное название выбранной услуги.
                        // Если ничего не выбрано — приглашение.
                        Text(
                            text = selectedService?.service_name ?: "Нажмите, чтобы выбрать",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedService != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ----- Количество -----
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

            // ----- Стоимость -----
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

            // ----- Удалённое выполнение -----
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

            // ----- Кнопки -----
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

    // ============================================================
    // Нижняя панель со списком услуг.
    // Показывается поверх экрана, когда showServiceSheet = true.
    // ============================================================
    if (showServiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showServiceSheet = false }
        ) {
            // Заголовок панели
            Text(
                text = "Выберите услугу",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()

            if (services.isEmpty()) {
                // Если список пуст — показываем заглушку
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Список услуг пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(services, key = { it.id }) { service ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    service.service_name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                if (service.service_description.isNotBlank()) {
                                    Text(
                                        service.service_description,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                // Выбрали услугу → закрываем панель
                                orderItemViewModel.selectService(service)
                                showServiceSheet = false
                            }
                        )
                        // Разделитель между услугами
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}