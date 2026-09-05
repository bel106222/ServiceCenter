package ru.bel.servicecenter.ui.status

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DatabaseStatusScreen(
    connectionOk: Boolean?,
    integrityOk: Boolean?,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Статус базы данных", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        StatusLine("Подключение к серверу", connectionOk)
        Spacer(modifier = Modifier.height(8.dp))
        StatusLine("Целостность БД", integrityOk)

        if (connectionOk == false || integrityOk == false) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Ошибка: " + when {
                    connectionOk == false -> "нет подключения к серверу"
                    integrityOk == false -> "база данных не готова или повреждена"
                    else -> ""
                },
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("Выход")
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, status: Boolean?) {
    val (text, color) = when (status) {
        null -> "$label..." to MaterialTheme.colorScheme.onSurface
        true -> "$label ✓" to Color(0xFF4CAF50)
        false -> "$label ✗" to MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyLarge
    )
}