package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun DatabaseStatusScreen(
    connectionOk: Boolean?,
    tablesExist: Boolean?,
    adminExists: Boolean?,
    isFactoryRunning: Boolean,
    factoryError: String?,
    onCheckAgain: () -> Unit,
    onRunFactory: (String) -> Unit,   // передаёт введённый пароль
    onExit: () -> Unit,
    onContinue: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Статус базы данных", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        StatusRow("Подключение к серверу", connectionOk)
        Spacer(modifier = Modifier.height(8.dp))
        StatusRow("Наличие таблиц", tablesExist)
        Spacer(modifier = Modifier.height(8.dp))
        StatusRow("Пользователь admin", adminExists)

        // Показываем поле пароля и кнопку запуска фабрики, если нет admin и всё остальное готово
        val canRunFactory = connectionOk == true && tablesExist == true && adminExists == false

        if (canRunFactory) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль администратора") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFactoryRunning
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onRunFactory(password) },
                enabled = password.length >= 6 && !isFactoryRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isFactoryRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Запустить фабрику")
                }
            }

            factoryError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопки внизу
        Button(onClick = onCheckAgain) {
            Text("Проверить снова")
        }

        Spacer(modifier = Modifier.height(12.dp))

        val allOk = connectionOk == true && tablesExist == true && adminExists == true
        Button(
            onClick = onContinue,
            enabled = allOk,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Продолжить")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выход")
        }
    }
}

@Composable
fun StatusRow(label: String, status: Boolean?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val text = when (status) {
            null -> "Проверка..."
            true -> "✓ $label"
            false -> "✗ $label"
        }
        val color = when (status) {
            true -> MaterialTheme.colorScheme.primary
            false -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.onSurface
        }
        Text(text = text, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}