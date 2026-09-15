package ru.bel.servicecenter.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.viewmodels.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    categoryViewModel: CategoryViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentCategory by categoryViewModel.currentCategory.collectAsState()
    val errors by categoryViewModel.errors.collectAsState()
    val operationCompleted by categoryViewModel.operationCompleted.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(operationCompleted) {
        if (operationCompleted) {
            isSaving = false
            categoryViewModel.resetOperationCompleted()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Категория") },
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
                value = currentCategory.category_name,
                onValueChange = { categoryViewModel.updateField("name", it) },
                label = { Text("Название категории") },
                leadingIcon = { Icon(Icons.Default.Category, null) },
                isError = errors["category_name"] != null,
                supportingText = { errors["category_name"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
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
                        categoryViewModel.saveCategory()
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