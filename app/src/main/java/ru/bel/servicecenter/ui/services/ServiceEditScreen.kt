package ru.bel.servicecenter.ui.services

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    val categories by serviceViewModel.categories.collectAsState()
    val operationCompleted by serviceViewModel.operationCompleted.collectAsState()

    var isSaving by remember { mutableStateOf(false) }
    var selectedCategoryName by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Загружаем категории, если их ещё нет
    LaunchedEffect(Unit) {
        if (categories.isEmpty()) {
            serviceViewModel.loadCategories()
        }
    }

    LaunchedEffect(currentService, categories) {
        selectedCategoryName = categories.find { it.id == currentService?.category_id }?.category_name ?: "Не выбрана"
    }

    LaunchedEffect(operationCompleted) {
        if (operationCompleted) {
            isSaving = false
            serviceViewModel.resetOperationCompleted()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Услуга") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = currentService?.service_name ?: "",
                onValueChange = { serviceViewModel.updateField("name", it) },
                label = { Text("Название") },
                leadingIcon = { Icon(Icons.Default.Build, null) },
                isError = errors["service_name"] != null,
                supportingText = { errors["service_name"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = currentService?.service_description ?: "",
                onValueChange = { serviceViewModel.updateField("description", it) },
                label = { Text("Описание") },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория") },
                    leadingIcon = { Icon(Icons.Default.Category, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    isError = errors["category_id"] != null,
                    supportingText = { errors["category_id"]?.let { Text(it) } },
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
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Фиксированная цена", modifier = Modifier.weight(1f))
                Switch(
                    checked = currentService?.is_fixprice ?: false,
                    onCheckedChange = { serviceViewModel.updateField("is_fixprice", it.toString()) },
                    enabled = !isSaving
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()) {
                if (isSaving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранение...")
                    }
                } else {
                    Button(onClick = {
                        isSaving = true
                        serviceViewModel.saveService()
                    }) {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить")
                    }
                    Spacer(Modifier.width(16.dp))
                    FilledTonalButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Отмена")
                    }
                }
            }
        }
    }
}