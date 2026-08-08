package ru.bel.servicecenter.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// Простейший менеджер смены темы (светлая/тёмная).
object ThemeManager {
    // Реактивное состояние, определяющее текущую тему
    var isDark by mutableStateOf(true)
}

// Определяем цветовые схемы
private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF388E3C),
    error = Color(0xFFD32F2F)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFFA5D6A7),
    error = Color(0xFFEF9A9A)
)

// Composable-обёртка, применяющая тему ко всему приложению
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = if (ThemeManager.isDark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}