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
import ru.bel.servicecenter.controllers.CategoryController
import ru.bel.servicecenter.models.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesListScreen(
    categoryController: CategoryController = viewModel(),
    onEditCategory: (Category) -> Unit,
    onBack: () -> Unit
) {
    val categories by categoryController.categories.collectAsState()
    val message by categoryController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Категории") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                categoryController.setEditingCategory(Category(category_name = ""))
                onEditCategory(categoryController.currentCategory.value)
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(categories) { cat ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat.category_name)
                        Row {
                            TextButton(onClick = { onEditCategory(cat) }) { Text("Изменить") }
                            TextButton(onClick = { categoryController.deleteCategory(cat) }) { Text("Удалить") }
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