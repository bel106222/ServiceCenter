package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.ServiceController
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.Service

/**
 * Экран списка услуг выбранной категории.
 * Кнопка "Изменить" открывает редактор, кнопка "Удалить" удаляет услугу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesListScreen(
    serviceController: ServiceController,
    category: Category,
    onEditService: (Service) -> Unit,
    onBack: () -> Unit
) {
    val services by serviceController.services.collectAsState()
    val isLoading by serviceController.isLoading.collectAsState()
    val message by serviceController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(category.id) {
        serviceController.loadServices(category.id)
    }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Услуги: ${category.category_name}") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                serviceController.startEditing(null)
                serviceController.updateField("category_id", category.id)
                onEditService(serviceController.currentService.value!!)
            }) { Text("+") }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            services.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Нет услуг в этой категории")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(services) { service ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(service.service_name, style = MaterialTheme.typography.titleMedium)
                                Text(service.service_description)
                                Row {
                                    TextButton(onClick = { onEditService(service) }) { Text("Изменить") }
                                    TextButton(onClick = { serviceController.deleteService(service) }) { Text("Удалить") }
                                }
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
            confirmButton = { TextButton(onClick = {
                showMessage = false
                serviceController.clearMessage()
            }) { Text("OK") } }
        )
    }
}