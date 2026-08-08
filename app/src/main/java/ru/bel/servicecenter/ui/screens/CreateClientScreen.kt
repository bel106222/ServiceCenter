package ru.bel.servicecenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService

@Composable
fun CreateClientScreen(
    defaultTitle: String,
    onClientCreated: (Client) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(defaultTitle) }
    var address by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isLegal by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var detailsError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Новый клиент", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it; titleError = ValidationRules.validateRequired(it, "Название") },
            label = { Text("Название организации / клиента") },
            isError = titleError != null,
            supportingText = { titleError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it; addressError = ValidationRules.validateRequired(it, "Адрес") },
            label = { Text("Адрес") },
            isError = addressError != null,
            supportingText = { addressError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = details,
            onValueChange = { details = it; detailsError = ValidationRules.validateRequired(it, "Реквизиты") },
            label = { Text("Реквизиты") },
            isError = detailsError != null,
            supportingText = { detailsError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = isLegal, onCheckedChange = { isLegal = it })
            Text("Юридическое лицо")
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                titleError = ValidationRules.validateRequired(title, "Название")
                addressError = ValidationRules.validateRequired(address, "Адрес")
                detailsError = ValidationRules.validateRequired(details, "Реквизиты")
                if (titleError == null && addressError == null && detailsError == null) {
                    coroutineScope.launch {
                        try {
                            val newClient = RepositoryProvider.clientRepo.createClient(
                                Client(
                                    client_title = title,
                                    client_address = address,
                                    client_details = details,
                                    is_legal = isLegal
                                )
                            )
                            LoggerService.log("Клиент создан: ${newClient.client_title}")
                            onClientCreated(newClient)
                        } catch (e: Exception) {
                            errorMessage = "Ошибка: ${e.message}"
                        }
                    }
                }
            }) { Text("Сохранить") }

            Button(onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) { Text("Отмена") }
        }
    }
}