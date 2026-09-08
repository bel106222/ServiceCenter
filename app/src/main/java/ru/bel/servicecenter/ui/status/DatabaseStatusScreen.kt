package ru.bel.servicecenter.ui.status

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import ru.bel.servicecenter.ui.theme.ThemeManager
import ru.bel.servicecenter.R

@Composable
fun DatabaseStatusScreen(
    connectionOk: Boolean?,
    integrityOk: Boolean?,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_round),  // если файл называется ic_launcher_round.png
                contentDescription = "Логотип",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Сервисный центр", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))

            // Заголовок тем же размером, что и строки проверки
            Text("Статус базы данных:", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

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
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text("Выход")
                }
            }
        }

        Button(
            onClick = { ThemeManager.isDark = !ThemeManager.isDark },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (ThemeManager.isDark) "Светлая тема" else "Тёмная тема")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatusLine(label: String, status: Boolean?) {
    val text = when (status) {
        null -> "$label..."
        true -> "$label  ✓"
        false -> "$label  ✗"
    }
    val color = when (status) {
        true -> Color(0xFF4CAF50)
        false -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyLarge
    )
}