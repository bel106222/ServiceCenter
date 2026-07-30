package ru.bel.servicecenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.PriceController

@Composable
fun PriceEditScreen(
    priceController: PriceController = viewModel(),
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val current by priceController.currentPrice.collectAsState()
    val errors by priceController.errors.collectAsState()
    val message by priceController.message.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование цены", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = current.service_id,
            onValueChange = { priceController.updateField("service_id", it) },
            label = { Text("ID услуги") },
            isError = errors["service_id"] != null,
            supportingText = { errors["service_id"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = current.service_cost.toString(),
            onValueChange = { priceController.updateField("cost", it) },
            label = { Text("Стоимость") },
            isError = errors["service_cost"] != null,
            supportingText = { errors["service_cost"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = current.is_time,
                onCheckedChange = { priceController.updateField("is_time", it.toString()) }
            )
            Text("Почасовая оплата")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { priceController.savePrice() }) { Text("Сохранить") }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Отмена") }
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }

        LaunchedEffect(message) {
            if (message == "Цена создана" || message == "Цена обновлена") {
                onSaved()
            }
        }
    }
}