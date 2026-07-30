package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.ServiceController

@Composable
fun ServiceEditScreen(
    serviceController: ServiceController = viewModel(),
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val current by serviceController.currentService.collectAsState()
    val errors by serviceController.errors.collectAsState()
    val message by serviceController.message.collectAsState()

    // Получаем список категорий для выбора (можно через отдельный контроллер или загружать здесь)
    // Упростим: в ServiceController можно добавить список категорий, но для краткости используем TextField.
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование услуги", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = current.service_name,
            onValueChange = { serviceController.updateField("name", it) },
            label = { Text("Название") },
            isError = errors["service_name"] != null,
            supportingText = { errors["service_name"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = current.service_description,
            onValueChange = { serviceController.updateField("description", it) },
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Категория – упрощённо: ввод UUID, но можно выпадающий список.
        OutlinedTextField(
            value = current.category_id,
            onValueChange = { serviceController.updateField("category_id", it) },
            label = { Text("ID категории") },
            isError = errors["category_id"] != null,
            supportingText = { errors["category_id"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = current.is_fixprice,
                onCheckedChange = { serviceController.updateField("is_fixprice", it.toString()) }
            )
            Text("Фиксированная цена")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { serviceController.saveService() }) { Text("Сохранить") }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Отмена") }
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }

        LaunchedEffect(message) {
            if (message == "Услуга создана" || message == "Услуга обновлена") {
                onSaved()
            }
        }
    }
}