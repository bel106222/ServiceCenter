package ru.bel.servicecenter.ui.screens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.PriceController
import ru.bel.servicecenter.models.Price

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(
    priceController: PriceController = viewModel(),
    onEditPrice: (Price) -> Unit,
    onBack: () -> Unit
) {
    val prices by priceController.prices.collectAsState()
    val message by priceController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Прайс-лист") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                priceController.setEditingPrice(Price(service_id = "", service_cost = 0f, is_time = false))
                onEditPrice(priceController.currentPrice.value)
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(prices) { price ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Услуга ID: ${price.service_id}")
                        Text("Стоимость: ${price.service_cost}")
                        Text(if (price.is_time) "Почасовая" else "Фиксированная")
                        Row {
                            TextButton(onClick = { onEditPrice(price) }) { Text("Изменить") }
                            TextButton(onClick = { priceController.deletePrice(price) }) { Text("Удалить") }
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
            confirmButton = { TextButton(onClick = { showMessage = false }) { Text("OK") } }
        )
    }
}