package ru.bel.servicecenter.ui.prices

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.viewmodels.PriceViewModel

@Composable
fun PriceEditScreen(
    priceViewModel: PriceViewModel,
    service: Service,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentPrice by priceViewModel.currentPrice.collectAsState()
    val message by priceViewModel.message.collectAsState()

    var cost by remember { mutableStateOf(currentPrice?.service_cost?.toString() ?: "") }
    var isTime by remember { mutableStateOf(currentPrice?.is_time ?: false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Цена обновлена") {
                onSaved()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование цены", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = service.service_name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Услуга") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cost,
            onValueChange = { cost = it },
            label = { Text("Стоимость") },
            isError = cost.toFloatOrNull() == null || (cost.toFloatOrNull() ?: 0f) <= 0,
            supportingText = {
                if (cost.toFloatOrNull() == null || (cost.toFloatOrNull() ?: 0f) <= 0) {
                    Text("Введите положительное число")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isTime,
                onCheckedChange = { isTime = it },
                enabled = !isSaving
            )
            Text("Почасовая оплата")
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
                    val costValue = cost.toFloatOrNull()
                    if (costValue != null && costValue > 0) {
                        isSaving = true
                        priceViewModel.createPrice(service, costValue, isTime)
                    }
                }) { Text("Сохранить") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Отмена") }
            }
        }
    }
}