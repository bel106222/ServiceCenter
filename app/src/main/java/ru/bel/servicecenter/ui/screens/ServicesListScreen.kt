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
import ru.bel.servicecenter.controllers.ServiceController
import ru.bel.servicecenter.models.Service

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesListScreen(
    serviceController: ServiceController = viewModel(),
    onEditService: (Service) -> Unit,
    onBack: () -> Unit
) {
    val services by serviceController.services.collectAsState()
    val message by serviceController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Услуги") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                serviceController.setEditingService(Service(service_name = "", service_description = "", category_id = "", is_fixprice = false))
                onEditService(serviceController.currentService.value)
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(services) { service ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(service.service_name, style = MaterialTheme.typography.titleMedium)
                        Text(service.service_description, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { onEditService(service) }) { Text("Изменить") }
                            TextButton(onClick = { serviceController.deleteService(service) }) { Text("Удалить") }
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