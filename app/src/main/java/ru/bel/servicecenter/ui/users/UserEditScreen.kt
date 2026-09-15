package ru.bel.servicecenter.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.Role
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    userViewModel: UserViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val errors by userViewModel.errors.collectAsState()
    val operationCompleted by userViewModel.operationCompleted.collectAsState()

    var roles by remember { mutableStateOf<List<Role>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var selectedRoleName by remember { mutableStateOf("") }
    var selectedClientName by remember { mutableStateOf("") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

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

    LaunchedEffect(operationCompleted) {
        if (operationCompleted) {
            isSaving = false
            userViewModel.resetOperationCompleted()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пользователь") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = currentUser.user_name,
                onValueChange = { userViewModel.updateField("name", it) },
                label = { Text("Имя") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                isError = errors["user_name"] != null,
                supportingText = { errors["user_name"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = currentUser.user_email,
                onValueChange = { userViewModel.updateField("email", it) },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                isError = errors["user_email"] != null,
                supportingText = { errors["user_email"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = currentUser.user_phone,
                onValueChange = { userViewModel.updateField("phone", it) },
                label = { Text("Телефон") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                isError = errors["user_phone"] != null,
                supportingText = { errors["user_phone"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = currentUser.user_password,
                onValueChange = { userViewModel.updateField("password", it) },
                label = { Text("Новый пароль (оставьте пустым, если не меняется)") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Скрыть" else "Показать"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = errors["user_password"] != null,
                supportingText = { errors["user_password"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = roleDropdownExpanded,
                onExpandedChange = { roleDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedRoleName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Роль") },
                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) },
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
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = clientDropdownExpanded,
                onExpandedChange = { clientDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedClientName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Клиент") },
                    leadingIcon = { Icon(Icons.Default.Business, null) },
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

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()) {
                if (isSaving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранение...")
                    }
                } else {
                    Button(onClick = {
                        isSaving = true
                        userViewModel.saveUser()
                    }) {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить")
                    }
                    Spacer(Modifier.width(16.dp))
                    FilledTonalButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Отмена")
                    }
                }
            }
        }
    }
}