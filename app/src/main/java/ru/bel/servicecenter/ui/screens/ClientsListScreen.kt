package ru.bel.servicecenter.ui.screens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.ClientController
import ru.bel.servicecenter.models.Client

/**
 * Экран списка организаций (клиентов).
 * Доступен только admin и engineer, проверка прав внутри контроллера.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsListScreen(
    clientController: ClientController = viewModel(),
    onEditClient: (Client) -> Unit,
    onBack: () -> Unit
) {
    val clients by clientController.clients.collectAsState()
    val message by clientController.message.collectAsState()

    // Показываем диалог при наличии сообщения
    var showMessage by remember { mutableStateOf(false) }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Клиенты") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                clientController.setEditingClient(Client(client_title = "", client_address = "", client_details = "", is_legal = false))
                onEditClient(clientController.currentClient.value) // переход на редактирование
            }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(clients) { client ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(client.client_title, style = MaterialTheme.typography.titleMedium)
                        Text(client.client_address)
                        Row {
                            TextButton(onClick = { onEditClient(client) }) { Text("Изменить") }
                            TextButton(onClick = { clientController.deleteClient(client) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }

    if (showMessage) {
        AlertDialog(
            onDismissRequest = { showMessage = false },
            title = { Text("Сообщение") },
            text = { Text(message ?: "") },
            confirmButton = { TextButton(onClick = { showMessage = false }) { Text("OK") } }
        )
    }
}