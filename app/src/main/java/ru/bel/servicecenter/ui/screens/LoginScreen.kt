package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.AuthController
import ru.bel.servicecenter.rules.ValidationRules

/**
 * Экран входа в систему.
 * Использует AuthController для проверки учётных данных.
 */
@Composable
fun LoginScreen(
    authController: AuthController = viewModel(),
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Ошибка от контроллера (например, неверный пароль)
    val authError by authController.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Поле email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = ValidationRules.validateEmail(it)
            },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Поле пароль
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = ValidationRules.validatePassword(it)
            },
            label = { Text("Пароль") },
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Общая ошибка аутентификации
        authError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Кнопка "Войти"
        Button(
            onClick = {
                // Финальная проверка перед отправкой
                emailError = ValidationRules.validateEmail(email)
                passwordError = ValidationRules.validatePassword(password)
                if (emailError == null && passwordError == null) {
                    authController.login(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти")
        }

        // Если вход успешен, перенаправляем дальше
        LaunchedEffect(authController.loggedUser.value) {
            if (authController.loggedUser.value != null) {
                onLoginSuccess()
            }
        }
    }
}