package ru.bel.servicecenter.ui.services

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.viewmodels.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceEditScreen(
    serviceViewModel: ServiceViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentService by serviceViewModel.currentService.collectAsState()
    val errors by serviceViewModel.errors.collectAsState()
    val message by serviceViewModel.message.collectAsState()
    val categories by serviceViewModel.categories.collectAsState()

    var isSaving by remember { mutableStateOf(false) }
    var selectedCategoryName by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentService, categories) {
        selectedCategoryName = categories.find { it.id == currentService?.category_id }?.category_name ?: "Не выбрана"
    }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Услуга создана" || message == "Услуга обновлена") {
                onSaved()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование услуги", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentService?.service_name ?: "",
            onValueChange = { serviceViewModel.updateField("name", it) },
            label = { Text("Название") },
            isError = errors["service_name"] != null,
            supportingText = { errors["service_name"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currentService?.service_description ?: "",
            onValueChange = { serviceViewModel.updateField("description", it) },
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = categoryDropdownExpanded,
            onExpandedChange = { categoryDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategoryName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Категория") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                isError = errors["category_id"] != null,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                enabled = !isSaving
            )
            ExposedDropdownMenu(
                expanded = categoryDropdownExpanded,
                onDismissRequest = { categoryDropdownExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.category_name) },
                        onClick = {
                            selectedCategoryName = category.category_name
                            serviceViewModel.updateField("category_id", category.id)
                            categoryDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = currentService?.is_fixprice ?: false,
                onCheckedChange = { serviceViewModel.updateField("is_fixprice", it.toString()) },
                enabled = !isSaving
            )
            Text("Фиксированная цена")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранение...")
                }
            } else {
                Button(onClick = {
                    isSaving = true
                    serviceViewModel.saveService()
                }) { Text("Сохранить") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Отмена") }
            }
        }
    }
}