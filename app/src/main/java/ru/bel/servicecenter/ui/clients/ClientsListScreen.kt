package ru.bel.servicecenter.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val isLoading by clientViewModel.isLoading.collectAsState()

    // Клиент, которого пользователь хочет удалить (для диалога подтверждения).
    var clientToDelete by remember { mutableStateOf<Client?>(null) }

    // Строка поиска
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        clientViewModel.loadClients()
    }

    // Отфильтрованный список: ищем по названию, без учёта регистра.
    // Пересчитывается только когда меняется список клиентов или строка поиска.
    val filteredClients = remember(clients, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            clients
        } else {
            clients.filter { it.client_title.contains(query, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Двухстрочный заголовок: название и подсказка про свайп.
                    Column {
                        Text("Клиенты")
                        Text(
                            "Смахните карточку вправо, чтобы удалить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                clientViewModel.initNewClient()
                onEditClient(clientViewModel.currentClient.value)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить клиента")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ============================================================
            // Поиск по названию
            // ============================================================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск") },
                placeholder = { Text("Название клиента") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ============================================================
            // Основной контент
            // ============================================================
            Box(modifier = Modifier.weight(1f)) {
                when {
                    // Идёт загрузка
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    // Клиентов вообще нет
                    clients.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Business, null,
                            Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Клиентов пока нет", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Нажмите +, чтобы добавить первого клиента",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Клиенты есть, но поиск ничего не нашёл
                    filteredClients.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search, null,
                            Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Попробуйте изменить запрос",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Список
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredClients, key = { it.id }) { client ->
                            SwipeableClientCard(
                                client = client,
                                onClick = {
                                    clientViewModel.setEditingClient(client)
                                    onEditClient(client)
                                },
                                onSwipeToDelete = { clientToDelete = client }
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог подтверждения удаления
    clientToDelete?.let { client ->
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Удалить клиента?") },
            text = {
                Text("«${client.client_title}» будет удалён. Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(onClick = {
                    clientViewModel.deleteClient(client)
                    clientToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Карточка клиента с возможностью смахнуть вправо для удаления.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableClientCard(
    client: Client,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeToDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .size(28.dp)
                )
            }
        }
    ) {
        ClientCard(client = client, onClick = onClick)
    }
}

/**
 * Одна карточка клиента.
 */
@Composable
private fun ClientCard(
    client: Client,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Строка 1: иконка + название
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Business, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    client.client_title,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Строка 2: адрес (если есть)
            if (client.client_address.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place, null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        client.client_address,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Строка 3: тип лица (юр/физ)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (client.is_legal) "Юридическое лицо" else "Физическое лицо",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}