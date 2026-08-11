package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.ClientManagementViewModel
import ru.bel.servicecenter.models.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsListScreen(
    viewModel: ClientManagementViewModel,
    onEditClient: (Client) -> Unit,
    onBack: () -> Unit
) {
    val clients by viewModel.clientController.clients.collectAsState()
    val message by viewModel.clientController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Клиенты") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.clientController.clearMessage()
                viewModel.clientController.setEditingClient(
                    Client(client_title = "", client_address = "", client_details = "", is_legal = false)
                )
                onEditClient(viewModel.clientController.currentClient.value)
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(clients) { client ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(client.client_title, style = MaterialTheme.typography.titleMedium)
                        Text(client.client_address)
                        Row {
                            TextButton(onClick = {
                                viewModel.clientController.clearMessage()
                                viewModel.clientController.setEditingClient(client)
                                onEditClient(client)
                            }) { Text("Изменить") }
                            TextButton(onClick = { viewModel.clientController.deleteClient(client) }) {
                                Text("Удалить")
                            }
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
            confirmButton = {
                TextButton(onClick = {
                    showMessage = false
                    viewModel.clientController.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}