package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.OrderController

@Composable
fun OrderEditScreen(
    orderController: OrderController = viewModel(),
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val current by orderController.currentOrder.collectAsState()
    val errors by orderController.errors.collectAsState()
    val message by orderController.message.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование заказа", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = current.order_number,
            onValueChange = { orderController.updateField("number", it) },
            label = { Text("Номер заказа") },
            isError = errors["order_number"] != null,
            supportingText = { errors["order_number"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = current.order_description,
            onValueChange = { orderController.updateField("description", it) },
            label = { Text("Описание") },
            isError = errors["order_description"] != null,
            supportingText = { errors["order_description"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Поле user_id – для admin/engineer можно ввести ID пользователя; для user будет заполнено автоматически
        OutlinedTextField(
            value = current.user_id,
            onValueChange = { orderController.updateField("user_id", it) },
            label = { Text("ID владельца") },
            isError = errors["user_id"] != null,
            supportingText = { errors["user_id"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Признак завершённости
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = current.is_completed,
                onCheckedChange = { orderController.updateField("is_completed", it.toString()) }
            )
            Text("Завершён")
        }

        // Почасовая оплата
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = current.is_time,
                onCheckedChange = { orderController.updateField("is_time", it.toString()) }
            )
            Text("Почасовая оплата")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { orderController.saveOrder() }) { Text("Сохранить") }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Отмена") }
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }

        LaunchedEffect(message) {
            if (message == "Заказ создан" || message == "Заказ обновлён") {
                onSaved()
            }
        }
    }
}