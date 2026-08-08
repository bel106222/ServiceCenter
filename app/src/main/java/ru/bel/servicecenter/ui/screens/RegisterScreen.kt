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
 * Экран регистрации нового пользователя.
 * После успешной регистрации AuthController автоматически выполняет вход,
 * дальнейшая навигация управляется MainActivity.
 */
@Composable
fun RegisterScreen(
    authController: AuthController = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val authError by authController.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Имя
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = ValidationRules.validateRequired(it, "Имя") },
            label = { Text("Имя") },
            isError = nameError != null,
            supportingText = { nameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = ValidationRules.validateEmail(it) },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Телефон
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = ValidationRules.validatePhone(it) },
            label = { Text("Телефон (+7XXXXXXXXXX)") },
            isError = phoneError != null,
            supportingText = { phoneError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Пароль
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = ValidationRules.validatePassword(it) },
            label = { Text("Пароль") },
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Ошибка регистрации
        authError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                nameError = ValidationRules.validateRequired(name, "Имя")
                emailError = ValidationRules.validateEmail(email)
                phoneError = ValidationRules.validatePhone(phone)
                passwordError = ValidationRules.validatePassword(password)
                if (listOf(nameError, emailError, phoneError, passwordError).all { it == null }) {
                    authController.register(name, email, phone, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зарегистрироваться")
        }
    }
}