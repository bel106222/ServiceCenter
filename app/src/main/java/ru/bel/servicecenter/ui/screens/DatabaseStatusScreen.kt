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
    tablesExist: Boolean?,
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
        StatusRow("Проверка БД", tablesExist)
        Spacer(modifier = Modifier.height(8.dp))
        StatusRow("Пользователь admin", adminExists)

        Spacer(modifier = Modifier.height(24.dp))

        // Случай: нет подключения
        if (connectionOk == false) {
            Text("Не удалось подключиться к БД", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Выход") }
            return@Column
        }

        // Случай: таблицы не существуют
        if (tablesExist == false) {
            Text(
                "Таблицы не найдены. Обратитесь к администратору БД.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Выход") }
            return@Column
        }

        // Таблицы есть, admin отсутствует – нужна инициализация
        if (tablesExist == true && adminExists == false) {
            when (factoryState) {
                FactoryState.Idle -> {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль администратора") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onRunFactory(password) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = password.length >= 6
                    ) {
                        Text("Ок")
                    }
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

                FactoryState.Success -> {
                    // Сюда не попадаем, так как после успеха сразу переходим к стартовому экрану
                }
            }
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