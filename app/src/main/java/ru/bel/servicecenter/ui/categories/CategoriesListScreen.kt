package ru.bel.servicecenter.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.viewmodels.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesListScreen(
    categoryViewModel: CategoryViewModel,
    onEditCategory: (Category) -> Unit,
    onBack: () -> Unit
) {
    val categories by categoryViewModel.categories.collectAsState()
    val isLoading by categoryViewModel.isLoading.collectAsState()
    val message by categoryViewModel.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        categoryViewModel.loadCategories()
    }
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
                categoryViewModel.clearMessage()
                categoryViewModel.setEditingCategory(Category(category_name = ""))
                onEditCategory(categoryViewModel.currentCategory.value)
            }) { Text("+") }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(category.category_name, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                TextButton(onClick = {
                                    categoryViewModel.clearMessage()
                                    categoryViewModel.setEditingCategory(category)
                                    onEditCategory(category)
                                }) { Text("Изменить") }
                                TextButton(onClick = { categoryViewModel.deleteCategory(category) }) { Text("Удалить") }
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
                    categoryViewModel.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}