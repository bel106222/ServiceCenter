package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.bel.servicecenter.controllers.UserController
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.Role
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    userController: UserController,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentUser by userController.currentUser.collectAsState()
    val errors by userController.errors.collectAsState()
    val message by userController.message.collectAsState()

    var roles by remember { mutableStateOf<List<Role>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var selectedRoleName by remember { mutableStateOf("") }
    var selectedClientName by remember { mutableStateOf("") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    // Загружаем роли и клиентов для выпадающих списков
    LaunchedEffect(Unit) {
        try {
            val roleNames = listOf("admin", "engineer", "user")
            roles = roleNames.mapNotNull { RepositoryProvider.roleRepo.getRoleByName(it) }
            clients = RepositoryProvider.clientRepo.getAllClients()
        } catch (e: Exception) {
            LoggerService.log("Ошибка загрузки справочников: ${e.message}")
        }
    }

    // Устанавливаем названия при изменении currentUser или справочников
    LaunchedEffect(currentUser, roles, clients) {
        selectedRoleName = roles.find { it.id == currentUser.role_id }?.role_name ?: "Не выбрана"
        selectedClientName = clients.find { it.id == currentUser.client_id }?.client_title ?: "Не выбран"
    }

    LaunchedEffect(message) {
        if (message != null && (message == "Пользователь создан" || message == "Профиль обновлён")) {
            onSaved()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование пользователя", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentUser.user_name,
            onValueChange = { userController.updateField("name", it) },
            label = { Text("Имя") },
            isError = errors["user_name"] != null,
            supportingText = { errors["user_name"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentUser.user_email,
            onValueChange = { userController.updateField("email", it) },
            label = { Text("Email") },
            isError = errors["user_email"] != null,
            supportingText = { errors["user_email"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentUser.user_phone,
            onValueChange = { userController.updateField("phone", it) },
            label = { Text("Телефон") },
            isError = errors["user_phone"] != null,
            supportingText = { errors["user_phone"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentUser.user_password,
            onValueChange = { userController.updateField("password", it) },
            label = { Text("Новый пароль (оставьте пустым, если не меняется)") },
            isError = errors["user_password"] != null,
            supportingText = { errors["user_password"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Выпадающий список ролей
        ExposedDropdownMenuBox(
            expanded = roleDropdownExpanded,
            onExpandedChange = { roleDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedRoleName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Роль") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                enabled = true
            )
            ExposedDropdownMenu(
                expanded = roleDropdownExpanded,
                onDismissRequest = { roleDropdownExpanded = false }
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.role_name) },
                        onClick = {
                            selectedRoleName = role.role_name
                            userController.updateField("role_id", role.id)
                            roleDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Выпадающий список клиентов
        ExposedDropdownMenuBox(
            expanded = clientDropdownExpanded,
            onExpandedChange = { clientDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedClientName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Клиент") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                enabled = true
            )
            ExposedDropdownMenu(
                expanded = clientDropdownExpanded,
                onDismissRequest = { clientDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Без клиента") },
                    onClick = {
                        selectedClientName = "Без клиента"
                        userController.updateField("client_id", "")
                        clientDropdownExpanded = false
                    }
                )
                clients.forEach { client ->
                    DropdownMenuItem(
                        text = { Text(client.client_title) },
                        onClick = {
                            selectedClientName = client.client_title
                            userController.updateField("client_id", client.id)
                            clientDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { userController.saveUser() }) { Text("Сохранить") }
            Button(onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) { Text("Отмена") }
        }
    }
}