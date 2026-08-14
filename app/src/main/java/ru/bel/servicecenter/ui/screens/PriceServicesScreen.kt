package ru.bel.servicecenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.PriceController
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.Price
import ru.bel.servicecenter.models.Service

/**
 * Экран списка услуг выбранной категории с актуальными ценами.
 * При нажатии на услугу открывается редактирование цены.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceServicesScreen(
    priceController: PriceController,
    category: Category,
    onEditPrice: (Service, Price?) -> Unit,
    onBack: () -> Unit
) {
    val servicesWithPrices by priceController.serviceWithPrice.collectAsState()
    val isLoading by priceController.isLoading.collectAsState()
    val message by priceController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(category.id) {
        priceController.loadServicesWithPrices(category.id)
    }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Услуги: ${category.category_name}") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            servicesWithPrices.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Нет услуг в этой категории")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(servicesWithPrices) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = item.service.service_name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (item.price != null) {
                                        if (item.price.is_time) {
                                            "${item.price.service_cost} руб/час"
                                        } else {
                                            "${item.price.service_cost} руб"
                                        }
                                    } else {
                                        "Цена не задана"
                                    }
                                )
                                if (item.price != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (item.price.is_time) "Почасовая оплата" else "Фиксированная цена",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    TextButton(onClick = { onEditPrice(item.service, item.price) }) { Text("Изменить") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMessage) {
        AlertDialog(
            onDismissRequest = { showMessage = false },
            title = { Text("Сообщение") },
            text = { Text(message ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    showMessage = false
                    priceController.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}