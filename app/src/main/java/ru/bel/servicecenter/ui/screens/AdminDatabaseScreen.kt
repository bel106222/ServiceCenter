package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.bel.servicecenter.controllers.AdminController
import ru.bel.servicecenter.utils.DataValidator
import ru.bel.servicecenter.utils.LoggerService

/**
 * Экран управления базой данных для администратора.
 * Позволяет очистить и пересоздать БД (требуется пароль),
 * а также проверить целостность начальных данных.
 */
@Composable
fun AdminDatabaseScreen(
    adminController: AdminController,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Пароль для операции очистки БД
    var password by remember { mutableStateOf("") }

    // Состояния для кнопок
    var isClearing by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    // Сообщение о результате
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Управление базой данных", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // Поле ввода пароля (для повторной инициализации)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль администратора (для очистки)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isClearing
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка "Очистить и пересоздать БД"
        Button(
            onClick = {
                isClearing = true
                message = null
                coroutineScope.launch {
                    try {
                        // Вызываем метод контроллера, который внутри запускает фабрику и устанавливает пароль
                        adminController.clearAndReseedDatabase(password)
                        message = "База данных успешно очищена и пересоздана"
                        LoggerService.log("БД очищена и пересоздана администратором")
                    } catch (e: Exception) {
                        message = "Ошибка: ${e.message}"
                        LoggerService.log("Ошибка очистки БД: ${e.message}")
                    } finally {
                        isClearing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = password.length >= 6 && !isClearing
        ) {
            if (isClearing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Очистить и пересоздать БД")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Кнопка "Проверить целостность"
        Button(
            onClick = {
                isChecking = true
                message = null
                coroutineScope.launch {
                    try {
                        val validator = DataValidator()
                        val result = validator.validate()
                        message = if (result) {
                            "Все проверки пройдены успешно"
                        } else {
                            "Обнаружены расхождения (смотрите статусную строку)"
                        }
                    } catch (e: Exception) {
                        message = "Ошибка проверки: ${e.message}"
                    } finally {
                        isChecking = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Проверить целостность данных")
            }
        }

        // Сообщение о результате
        message?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(msg, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Кнопка "Назад"
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Назад")
        }
    }
}