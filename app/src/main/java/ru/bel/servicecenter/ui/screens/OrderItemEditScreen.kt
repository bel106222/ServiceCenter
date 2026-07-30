package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.OrderItemController

@Composable
fun OrderItemEditScreen(
    orderItemController: OrderItemController = viewModel(),
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val current by orderItemController.currentItem.collectAsState()
    val errors by orderItemController.errors.collectAsState()
    val message by orderItemController.message.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Позиция заказа", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = current.service_id,
            onValueChange = { orderItemController.updateField("service_id", it) },
            label = { Text("ID услуги") },
            isError = errors["service_id"] != null,
            supportingText = { errors["service_id"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = current.user_id,
            onValueChange = { orderItemController.updateField("user_id", it) },
            label = { Text("ID инженера") },
            isError = errors["user_id"] != null,
            supportingText = { errors["user_id"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = current.orderitem_quantity.toString(),
            onValueChange = { orderItemController.updateField("quantity", it) },
            label = { Text("Количество") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = current.orderitem_cost.toString(),
            onValueChange = { orderItemController.updateField("cost", it) },
            label = { Text("Стоимость") },
            isError = errors["orderitem_cost"] != null,
            supportingText = { errors["orderitem_cost"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = current.is_online,
                onCheckedChange = { orderItemController.updateField("is_online", it.toString()) }
            )
            Text("Удалённое выполнение")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { orderItemController.saveItem() }) { Text("Сохранить") }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Отмена") }
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }

        LaunchedEffect(message) {
            if (message == "Позиция добавлена" || message == "Позиция обновлена") {
                onSaved()
            }
        }
    }
}