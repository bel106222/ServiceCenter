package ru.bel.servicecenter.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectClientScreen(
    currentUser: User,
    authViewModel: AuthViewModel,
    onClientBound: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Список существующих клиентов
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    // Состояние формы создания нового клиента
    var showCreateForm by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf(currentUser.user_name) }
    var newAddress by remember { mutableStateOf("") }
    var newDetails by remember { mutableStateOf("") }
    var newIsLegal by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Загружаем список клиентов при открытии
    LaunchedEffect(Unit) {
        try {
            clients = RepositoryProvider.clientRepo.getAllClients()
        } catch (e: Exception) {
            message = "Ошибка загрузки клиентов: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выбор клиента") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (showCreateForm) {
                // ---------- Форма создания нового клиента ----------
                Text("Новый клиент", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Название организации / ФИО") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newAddress,
                    onValueChange = { newAddress = it },
                    label = { Text("Адрес") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newDetails,
                    onValueChange = { newDetails = it },
                    label = { Text("Реквизиты") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Юридическое лицо", modifier = Modifier.weight(1f))
                    Switch(
                        checked = newIsLegal,
                        onCheckedChange = { newIsLegal = it },
                        enabled = !isSaving
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Button(onClick = {
                            if (newTitle.isBlank()) {
                                message = "Введите название"
                                return@Button
                            }
                            isSaving = true
                            message = null
                            coroutineScope.launch {
                                try {
                                    // 1. Создаём клиента
                                    val newClient = RepositoryProvider.clientRepo.createClient(
                                        Client(
                                            client_title = newTitle,
                                            client_address = newAddress,
                                            client_details = newDetails,
                                            is_legal = newIsLegal
                                        )
                                    )
                                    // 2. Привязываем его к текущему пользователю
                                    val updatedUser = currentUser.copy(client_id = newClient.id)
                                    RepositoryProvider.userRepo.updateUser(updatedUser)
                                    // 3. Обновляем loggedUser в AuthViewModel
                                    authViewModel.updateLoggedUser(updatedUser)
                                    LoggerService.log("Привязан клиент: ${newClient.client_title}")
                                    // 4. Переходим на дашборд
                                    onClientBound()
                                } catch (e: Exception) {
                                    message = "Ошибка: ${e.message}"
                                    isSaving = false
                                }
                            }
                        }) {
                            Text("Создать")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(onClick = { showCreateForm = false }) {
                            Text("Назад")
                        }
                    }
                }
            } else {
                // ---------- Список существующих клиентов ----------
                Text("Выберите существующего клиента", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (clients.isEmpty()) {
                    Text("Клиентов пока нет")
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(clients) { client ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            try {
                                                val updatedUser = currentUser.copy(client_id = client.id)
                                                RepositoryProvider.userRepo.updateUser(updatedUser)
                                                authViewModel.updateLoggedUser(updatedUser)
                                                LoggerService.log("Привязан клиент: ${client.client_title}")
                                                onClientBound()
                                            } catch (e: Exception) {
                                                message = "Ошибка привязки: ${e.message}"
                                            }
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(client.client_title, style = MaterialTheme.typography.titleMedium)
                                    if (client.client_address.isNotBlank()) {
                                        Text(client.client_address)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        newTitle = currentUser.user_name
                        showCreateForm = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Создать нового клиента")
                }

                message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}