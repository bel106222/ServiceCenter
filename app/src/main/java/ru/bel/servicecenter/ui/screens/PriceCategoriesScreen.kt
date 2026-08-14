package ru.bel.servicecenter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.PriceController
import ru.bel.servicecenter.models.Category

/**
 * Экран списка категорий для раздела "Цены".
 * При выборе категории открывается список услуг с ценами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceCategoriesScreen(
    priceController: PriceController,
    onSelectCategory: (Category) -> Unit,
    onBack: () -> Unit
) {
    val categories by priceController.categories.collectAsState()
    val message by priceController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        priceController.loadCategories()
        isLoading = false
    }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Цены") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    items(categories) { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable { onSelectCategory(category) }
                        ) {
                            Text(
                                text = category.category_name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
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
            confirmButton = { TextButton(onClick = {
                showMessage = false
                priceController.clearMessage()
            }) { Text("OK") } }
        )
    }
}