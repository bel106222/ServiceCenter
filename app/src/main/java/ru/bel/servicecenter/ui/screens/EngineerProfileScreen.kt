package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.UserController
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService

/**
 * Профиль инженера.
 * Отображает и позволяет редактировать личные данные.
 * Привязанный клиент загружается из БД по client_id текущего пользователя.
 */
@Composable
fun EngineerProfileScreen(
    currentUser: User,
    onBack: () -> Unit
) {
    val userController = remember { UserController().apply { currentAuthUser = currentUser } }

    var name by remember { mutableStateOf(currentUser.user_name) }
    var email by remember { mutableStateOf(currentUser.user_email) }
    var phone by remember { mutableStateOf(currentUser.user_phone) }
    var password by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var message by remember { mutableStateOf<String?>(null) }

    var clientTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser.client_id) {
        if (!currentUser.client_id.isNullOrBlank()) {
            try {
                val clients = RepositoryProvider.clientRepo.getAllClients()
                val client = clients.find { it.id == currentUser.client_id }
                clientTitle = client?.client_title ?: "Клиент не найден"
            } catch (e: Exception) {
                LoggerService.log("Ошибка загрузки клиента: ${e.message}")
                clientTitle = "Ошибка загрузки"
            }
        } else {
            clientTitle = "Не привязан"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Профиль инженера", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (clientTitle != null) {
            Text("Привязанный клиент: $clientTitle", style = MaterialTheme.typography.bodyMedium)
        } else {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = ValidationRules.validateRequired(it, "Имя") },
            label = { Text("Имя") },
            isError = nameError != null,
            supportingText = { nameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = ValidationRules.validateEmail(it) },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = ValidationRules.validatePhone(it) },
            label = { Text("Телефон") },
            isError = phoneError != null,
            supportingText = { phoneError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = if (it.isNotEmpty()) ValidationRules.validatePassword(it) else null },
            label = { Text("Новый пароль (оставьте пустым, чтобы не менять)") },
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                nameError = ValidationRules.validateRequired(name, "Имя")
                emailError = ValidationRules.validateEmail(email)
                phoneError = ValidationRules.validatePhone(phone)
                if (password.isNotEmpty()) {
                    passwordError = ValidationRules.validatePassword(password)
                }
                if (listOf(nameError, emailError, phoneError, passwordError).all { it == null }) {
                    userController.updateField("name", name)
                    userController.updateField("email", email)
                    userController.updateField("phone", phone)
                    if (password.isNotEmpty()) {
                        userController.updateField("password", password)
                    }
                    userController.saveUser()
                    message = "Профиль обновлён"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить") }

        message?.let { Spacer(modifier = Modifier.height(8.dp)); Text(it) }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}