package ru.bel.servicecenter.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.viewmodels.CategoryViewModel

@Composable
fun CategoryEditScreen(
    categoryViewModel: CategoryViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentCategory by categoryViewModel.currentCategory.collectAsState()
    val errors by categoryViewModel.errors.collectAsState()
    val message by categoryViewModel.message.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            isSaving = false
            if (message == "Категория создана" || message == "Категория обновлена") {
                onSaved()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование категории", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = currentCategory.category_name,
            onValueChange = { categoryViewModel.updateField("name", it) },
            label = { Text("Название категории") },
            isError = errors["category_name"] != null,
            supportingText = { errors["category_name"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )

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
                    categoryViewModel.saveCategory()
                }) { Text("Сохранить") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Отмена") }
            }
        }
    }
}