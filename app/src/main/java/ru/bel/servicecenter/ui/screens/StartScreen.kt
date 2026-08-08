package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.ui.theme.ThemeManager

/**
 * Стартовый экран приложения.
 * Показывает логотип, статус подключения (упрощённо), кнопки входа/регистрации,
 * а также переключатель темы.
 */
@Composable
fun StartScreen(onNavigate: (String) -> Unit) {
    // onNavigate — колбэк для перехода на другие экраны (например, "login", "register")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сервисный центр",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Переключатель темы (светлая / тёмная)
        Button(onClick = { ThemeManager.isDark = !ThemeManager.isDark }) {
            Text(if (ThemeManager.isDark) "Светлая тема" else "Тёмная тема")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка входа
        Button(
            onClick = { onNavigate("login") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Вход")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка регистрации
        Button(
            onClick = { onNavigate("register") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Регистрация")
        }

    }
}