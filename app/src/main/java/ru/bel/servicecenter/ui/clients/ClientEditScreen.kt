package ru.bel.servicecenter.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.viewmodels.ClientViewModel

@Composable
fun ClientEditScreen(
    clientViewModel: ClientViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentClient by clientViewModel.currentClient.collectAsState()
    val errors by clientViewModel.errors.collectAsState()
    val message by clientViewModel.message.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Клиент создан" || message == "Клиент обновлён") {
                onSaved()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование клиента", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentClient.client_title,
            onValueChange = { clientViewModel.updateField("title", it) },
            label = { Text("Название организации") },
            isError = errors["client_title"] != null,
            supportingText = { errors["client_title"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentClient.client_address,
            onValueChange = { clientViewModel.updateField("address", it) },
            label = { Text("Адрес") },
            isError = errors["client_address"] != null,
            supportingText = { errors["client_address"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentClient.client_details,
            onValueChange = { clientViewModel.updateField("details", it) },
            label = { Text("Реквизиты") },
            isError = errors["client_details"] != null,
            supportingText = { errors["client_details"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = currentClient.is_legal,
                onCheckedChange = { clientViewModel.updateField("is_legal", it.toString()) },
                enabled = !isSaving
            )
            Text("Юридическое лицо")
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
                    isSaving = true
                    clientViewModel.saveClient()
                }) { Text("Сохранить") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Отмена") }
            }
        }
    }
}