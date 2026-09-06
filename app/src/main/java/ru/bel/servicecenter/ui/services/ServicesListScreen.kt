package ru.bel.servicecenter.ui.services

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.viewmodels.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesListScreen(
    serviceViewModel: ServiceViewModel,
    category: Category,
    onEditService: (Service) -> Unit,
    onBack: () -> Unit
) {
    val services by serviceViewModel.services.collectAsState()
    val isLoading by serviceViewModel.isLoading.collectAsState()
    val message by serviceViewModel.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(category.id) {
        serviceViewModel.loadServices(category.id)
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
                serviceViewModel.startEditing(null)
                serviceViewModel.updateField("category_id", category.id)
                onEditService(serviceViewModel.currentService.value!!)
            }) { Text("+") }
        }
    ) { padding ->
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            services.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Нет услуг в этой категории")
            }
            else -> LazyColumn(modifier = Modifier.padding(padding)) {
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
                                TextButton(onClick = { serviceViewModel.deleteService(service) }) { Text("Удалить") }
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
                    serviceViewModel.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}