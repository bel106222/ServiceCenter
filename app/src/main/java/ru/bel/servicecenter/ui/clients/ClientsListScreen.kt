package ru.bel.servicecenter.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.viewmodels.ClientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsListScreen(
    clientViewModel: ClientViewModel,
    onEditClient: (Client) -> Unit,
    onBack: () -> Unit
) {
    val clients by clientViewModel.clients.collectAsState()
    val message by clientViewModel.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        clientViewModel.loadClients()
    }
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
                clientViewModel.clearMessage()
                clientViewModel.initNewClient()
                onEditClient(clientViewModel.currentClient.value)
            }) { Text("+") }
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
                            TextButton(onClick = {
                                clientViewModel.clearMessage()
                                clientViewModel.setEditingClient(client)
                                onEditClient(client)
                            }) { Text("Изменить") }
                            TextButton(onClick = { clientViewModel.deleteClient(client) }) { Text("Удалить") }
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
                    clientViewModel.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}