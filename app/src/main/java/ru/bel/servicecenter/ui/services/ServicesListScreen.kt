package ru.bel.servicecenter.ui.services

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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

    LaunchedEffect(category.id) {
        serviceViewModel.loadServices(category.id)
    }

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
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить услугу")
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            services.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Build, null, Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text("В этой категории услуг пока нет",
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Нажмите +, чтобы добавить первую услугу",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(services) { service ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, null,
                                    Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(service.service_name,
                                    style = MaterialTheme.typography.titleMedium)
                            }
                            if (service.service_description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(service.service_description,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            if (service.is_fixprice) {
                                Spacer(Modifier.height(4.dp))
                                Text("Фиксированная цена",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                TextButton(onClick = { onEditService(service) }) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Изменить")
                                }
                                TextButton(onClick = { serviceViewModel.deleteService(service) }) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Удалить")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}