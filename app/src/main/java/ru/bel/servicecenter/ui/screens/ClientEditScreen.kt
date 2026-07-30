package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.ClientController
import ru.bel.servicecenter.models.Client

/**
 * Экран создания/редактирования клиента.
 * Поля формы с мгновенной валидацией и отображением ошибок.
 */
@Composable
fun ClientEditScreen(
    clientController: ClientController = viewModel(),
    onSaved: () -> Unit,          // вызывается после успешного сохранения
    onCancel: () -> Unit
) {
    val currentClient by clientController.currentClient.collectAsState()
    val errors by clientController.errors.collectAsState()
    val message by clientController.message.collectAsState()

    // Локальные состояния для полей (чтобы менять через updateField)
    // Используем copy при вводе
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Редактирование клиента", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Название
        OutlinedTextField(
            value = currentClient.client_title,
            onValueChange = { clientController.updateField("title", it) },
            label = { Text("Название организации") },
            isError = errors["client_title"] != null,
            supportingText = { errors["client_title"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Адрес
        OutlinedTextField(
            value = currentClient.client_address,
            onValueChange = { clientController.updateField("address", it) },
            label = { Text("Адрес") },
            isError = errors["client_address"] != null,
            supportingText = { errors["client_address"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Реквизиты
        OutlinedTextField(
            value = currentClient.client_details,
            onValueChange = { clientController.updateField("details", it) },
            label = { Text("Реквизиты") },
            isError = errors["client_details"] != null,
            supportingText = { errors["client_details"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Признак юр. лица
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = currentClient.is_legal,
                onCheckedChange = { clientController.updateField("is_legal", it.toString()) }
            )
            Text("Юридическое лицо")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопки
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { clientController.saveClient() }) { Text("Сохранить") }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Отмена") }
        }

        // Сообщение об успехе
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        // Перенаправление после успешного сохранения
        LaunchedEffect(message) {
            if (message == "Клиент создан" || message == "Клиент обновлён") {
                onSaved()
            }
        }
    }
}