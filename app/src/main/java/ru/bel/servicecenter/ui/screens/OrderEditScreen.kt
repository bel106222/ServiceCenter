package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.OrderController
import ru.bel.servicecenter.rules.ValidationRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditScreen(
    orderController: OrderController,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentOrder by orderController.currentOrder.collectAsState()
    val errors by orderController.errors.collectAsState()
    val message by orderController.message.collectAsState()
    val authorName by orderController.authorName.collectAsState()
    val isAdminOrEngineer = orderController.isAdminOrEngineer

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Заказ создан" || message == "Заказ обновлён") {
                onSaved()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование заказа", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentOrder?.order_number ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Номер заказа") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentOrder?.order_description ?: "",
            onValueChange = { orderController.updateField("description", it) },
            label = { Text("Описание") },
            isError = errors["order_description"] != null,
            supportingText = { errors["order_description"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Поле "Автор" – нередактируемое, показывает имя автора
        OutlinedTextField(
            value = authorName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Автор") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = currentOrder?.is_completed ?: false,
                onCheckedChange = { orderController.updateField("is_completed", it.toString()) },
                enabled = !isSaving && isAdminOrEngineer
            )
            Text("Завершён")
        }

        if (isAdminOrEngineer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = currentOrder?.is_time ?: false,
                    onCheckedChange = { orderController.updateField("is_time", it.toString()) },
                    enabled = !isSaving
                )
                Text("Почасовая оплата")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            if (isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранение...")
                }
            } else {
                Button(onClick = {
                    isSaving = true
                    orderController.saveOrder()
                }) { Text("Сохранить") }
                Button(onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Отмена") }
            }
        }
    }
}