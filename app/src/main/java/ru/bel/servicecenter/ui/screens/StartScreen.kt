package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.R
import ru.bel.servicecenter.ui.theme.ThemeManager

/**
 * Стартовый экран: логотип, заголовок, кнопки входа/регистрации.
 * Кнопка смены темы внизу, над статусной строкой.
 */
@Composable
fun StartScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Центральная часть
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Логотип
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round_),
                contentDescription = "Логотип",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Сервисный центр", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onNavigate("login") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Вход") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onNavigate("register") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Регистрация") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка темы внизу
        Button(onClick = { ThemeManager.isDark = !ThemeManager.isDark }) {
            Text(if (ThemeManager.isDark) "Светлая тема" else "Тёмная тема")
        }
    }
}