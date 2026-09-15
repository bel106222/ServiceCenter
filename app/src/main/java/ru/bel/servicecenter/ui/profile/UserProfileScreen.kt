package ru.bel.servicecenter.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.viewmodels.UserViewModel
import ru.bel.servicecenter.utils.LoggerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    currentUser: User,
    userViewModel: UserViewModel,
    onBack: () -> Unit
) {
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

    // Готовим ViewModel к редактированию именно этого пользователя
    LaunchedEffect(currentUser) {
        userViewModel.setEditingUser(currentUser)
    }

    LaunchedEffect(currentUser.client_id) {
        if (!currentUser.client_id.isNullOrBlank()) {
            try {
                val clients = RepositoryProvider.clientRepo.getAllClients()
                clientTitle = clients.find { it.id == currentUser.client_id }?.client_title ?: "Клиент не найден"
            } catch (e: Exception) {
                LoggerService.log("Ошибка загрузки клиента: ${e.message}")
                clientTitle = "Ошибка загрузки"
            }
        } else {
            clientTitle = "Не привязан"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мой профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (clientTitle != null) {
                Text(
                    "Привязанный клиент: $clientTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                onValueChange = {
                    password = it
                    passwordError = if (it.isNotEmpty()) ValidationRules.validatePassword(it) else null
                },
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
                    if (password.isNotEmpty()) passwordError = ValidationRules.validatePassword(password)
                    if (listOf(nameError, emailError, phoneError, passwordError).all { it == null }) {
                        userViewModel.updateField("name", name)
                        userViewModel.updateField("email", email)
                        userViewModel.updateField("phone", phone)
                        if (password.isNotEmpty()) userViewModel.updateField("password", password)
                        userViewModel.saveUser()
                        message = "Профиль обновлён"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить") }

            message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it)
            }
        }
    }
}