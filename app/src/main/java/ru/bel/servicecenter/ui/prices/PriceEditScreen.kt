package ru.bel.servicecenter.ui.prices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.viewmodels.PriceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceEditScreen(
    priceViewModel: PriceViewModel,
    service: Service,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentPrice by priceViewModel.currentPrice.collectAsState()
    val operationCompleted by priceViewModel.operationCompleted.collectAsState()

    var cost by remember { mutableStateOf(currentPrice?.service_cost?.toString() ?: "") }
    var isTime by remember { mutableStateOf(currentPrice?.is_time ?: false) }
    var isSaving by remember { mutableStateOf(false) }
    var costError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(operationCompleted) {
        if (operationCompleted) {
            isSaving = false
            priceViewModel.resetOperationCompleted()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Цена услуги") },
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
            OutlinedTextField(
                value = service.service_name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Услуга") },
                leadingIcon = { Icon(Icons.Default.Build, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = cost,
                onValueChange = {
                    cost = it
                    costError = if (it.isNotEmpty() && (it.toFloatOrNull() == null || (it.toFloatOrNull() ?: 0f) <= 0))
                        "Введите положительное число" else null
                },
                label = { Text("Стоимость (руб.)") },
                leadingIcon = { Icon(Icons.Default.Payments, null) },
                isError = costError != null,
                supportingText = { costError?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Почасовая оплата", modifier = Modifier.weight(1f))
                Switch(
                    checked = isTime,
                    onCheckedChange = { isTime = it },
                    enabled = !isSaving
                )
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
                        val costValue = cost.toFloatOrNull()
                        if (costValue != null && costValue > 0) {
                            isSaving = true
                            priceViewModel.createPrice(service, costValue, isTime)
                        } else {
                            costError = "Введите положительное число"
                        }
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