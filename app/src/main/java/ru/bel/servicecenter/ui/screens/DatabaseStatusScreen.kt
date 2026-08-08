package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Состояния фабрики начальных данных
 */
sealed class FactoryState {
    object Idle : FactoryState()
    object Running : FactoryState()
    object Success : FactoryState()
    data class Error(val message: String) : FactoryState()
}
@Composable
fun DatabaseStatusScreen(
    connectionOk: Boolean?,
    adminExists: Boolean?,
    factoryState: FactoryState,
    onRunFactory: (String) -> Unit,
    onExit: () -> Unit
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
        StatusRow("Проверка БД", adminExists)
        Spacer(modifier = Modifier.height(24.dp))

        // Если проверка ещё не завершена
        if (connectionOk == null) {
            CircularProgressIndicator()
            return@Column
        }

        // Если нет подключения
        if (connectionOk == false) {
            Text("Не удалось подключиться к БД", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Выход") }
            return@Column
        }

        // Подключение есть, но admin ещё не проверен
        if (adminExists == null) {
            CircularProgressIndicator()
            return@Column
        }

        // Подключение есть, admin существует – не показываем ничего (MainActivity переключит)
        if (adminExists == true) return@Column

        // admin отсутствует – инициализация
        when (factoryState) {
            FactoryState.Idle -> {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Задайте пароль администратора") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onRunFactory(password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.length >= 6
                ) { Text("Ок") }
            }

            FactoryState.Running -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Подготовка БД...")
            }

            is FactoryState.Error -> {
                Text(
                    "Ошибка инициализации: ${factoryState.message}. Обратитесь к администратору БД.",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Выход") }
            }

            FactoryState.Success -> { /* не достигается */ }
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