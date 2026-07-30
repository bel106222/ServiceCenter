package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.CategoryController

@Composable
fun CategoryEditScreen(
    categoryController: CategoryController = viewModel(),
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val current by categoryController.currentCategory.collectAsState()
    val errors by categoryController.errors.collectAsState()
    val message by categoryController.message.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Редактирование категории", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = current.category_name,
            onValueChange = { categoryController.updateField("name", it) },
            label = { Text("Название") },
            isError = errors["category_name"] != null,
            supportingText = { errors["category_name"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { categoryController.saveCategory() }) { Text("Сохранить") }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Отмена") }
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it)
        }

        LaunchedEffect(message) {
            if (message == "Категория создана" || message == "Категория обновлена") {
                onSaved()
            }
        }
    }
}