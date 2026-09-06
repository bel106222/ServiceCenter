package ru.bel.servicecenter.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.Role
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.viewmodels.UserViewModel
import ru.bel.servicecenter.utils.LoggerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    userViewModel: UserViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val errors by userViewModel.errors.collectAsState()
    val message by userViewModel.message.collectAsState()

    var roles by remember { mutableStateOf<List<Role>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var selectedRoleName by remember { mutableStateOf("") }
    var selectedClientName by remember { mutableStateOf("") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val roleNames = listOf("admin", "engineer", "user")
            roles = roleNames.mapNotNull { RepositoryProvider.roleRepo.getRoleByName(it) }
            clients = RepositoryProvider.clientRepo.getAllClients()
        } catch (e: Exception) {
            LoggerService.log("Ошибка загрузки справочников: ${e.message}")
        }
    }

    LaunchedEffect(currentUser, roles, clients) {
        selectedRoleName = roles.find { it.id == currentUser.role_id }?.role_name ?: "Не выбрана"
        selectedClientName = clients.find { it.id == currentUser.client_id }?.client_title ?: "Не выбран"
    }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Пользователь создан" || message == "Профиль обновлён") {
                onSaved()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование пользователя", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentUser.user_name,
            onValueChange = { userViewModel.updateField("name", it) },
            label = { Text("Имя") },
            isError = errors["user_name"] != null,
            supportingText = { errors["user_name"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentUser.user_email,
            onValueChange = { userViewModel.updateField("email", it) },
            label = { Text("Email") },
            isError = errors["user_email"] != null,
            supportingText = { errors["user_email"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentUser.user_phone,
            onValueChange = { userViewModel.updateField("phone", it) },
            label = { Text("Телефон") },
            isError = errors["user_phone"] != null,
            supportingText = { errors["user_phone"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentUser.user_password,
            onValueChange = { userViewModel.updateField("password", it) },
            label = { Text("Новый пароль (оставьте пустым, если не меняется)") },
            isError = errors["user_password"] != null,
            supportingText = { errors["user_password"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(16.dp))

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
                enabled = !isSaving
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
                            userViewModel.updateField("role_id", role.id)
                            roleDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

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
                enabled = !isSaving
            )
            ExposedDropdownMenu(
                expanded = clientDropdownExpanded,
                onDismissRequest = { clientDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Без клиента") },
                    onClick = {
                        selectedClientName = "Без клиента"
                        userViewModel.updateField("client_id", "")
                        clientDropdownExpanded = false
                    }
                )
                clients.forEach { client ->
                    DropdownMenuItem(
                        text = { Text(client.client_title) },
                        onClick = {
                            selectedClientName = client.client_title
                            userViewModel.updateField("client_id", client.id)
                            clientDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранение...")
                }
            } else {
                Button(onClick = {
                    isSaving = true
                    userViewModel.saveUser()
                }) { Text("Сохранить") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Отмена") }
            }
        }
    }
}