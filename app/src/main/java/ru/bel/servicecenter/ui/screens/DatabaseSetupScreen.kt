package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Экран, который показывается, если в БД отсутствуют таблицы.
 * Содержит SQL-скрипт для ручного выполнения и кнопку «Проверить» для повторной проверки.
 */
@Composable
fun DatabaseSetupScreen(
    onCheckAgain: () -> Unit   // вызывается при нажатии кнопки «Проверить»
) {
    val scrollState = rememberScrollState()

    val sqlScript = """
ДЛЯ СОЗДАНИЯ БД НА САЙТЕ SUPABASE
НЕОБХОДИМО ЗАПУСТИТЬ SQL-СКРИПТ 
В WEB-ИНТЕРФЕЙСЕ СЕРВИСА.
""".trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "База данных не настроена",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Выполните следующий SQL-скрипт в SQL Editor вашего проекта Supabase, затем нажмите «Проверить».")
        Spacer(modifier = Modifier.height(12.dp))

        // Скрипт в прокручиваемом поле
        OutlinedTextField(
            value = sqlScript,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .height(200.dp),
            label = { Text("SQL-скрипт") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCheckAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Проверить")
        }
    }
}