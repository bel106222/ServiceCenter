package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.bel.servicecenter.controllers.AuthController
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService

@Composable
fun SelectClientScreen(
    currentUser: User,
    authController: AuthController,
    onClientBound: () -> Unit
) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var showCreateClient by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            clients = RepositoryProvider.clientRepo.getAllClients()
        } catch (e: Exception) {
            LoggerService.log("Ошибка загрузки клиентов: ${e.message}")
        }
    }

    if (showCreateClient) {
        CreateClientScreen(
            defaultTitle = currentUser.user_name,
            onClientCreated = { newClient ->
                coroutineScope.launch {
                    try {
                        val updatedUser = currentUser.copy(client_id = newClient.id)
                        RepositoryProvider.userRepo.updateUser(updatedUser)
                        authController.updateLoggedUser(updatedUser)   // обновляем текущего пользователя
                        LoggerService.log("Пользователь привязан к клиенту: ${newClient.client_title}")
                        onClientBound()
                    } catch (e: Exception) {
                        LoggerService.log("Ошибка привязки клиента: ${e.message}")
                    }
                }
            },
            onCancel = { showCreateClient = false }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Выберите клиента", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(clients) { client ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                coroutineScope.launch {
                                    // Привязываем выбранного клиента напрямую
                                    val updatedUser = currentUser.copy(client_id = client.id)
                                    RepositoryProvider.userRepo.updateUser(updatedUser)
                                    LoggerService.log("Пользователь привязан к клиенту: ${client.client_title}")
                                    onClientBound()
                                }
                            }
                    ) {
                        Text(
                            text = client.client_title,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showCreateClient = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Создать нового клиента")
            }
        }
    }
}