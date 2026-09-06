package ru.bel.servicecenter.ui.prices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.viewmodels.PriceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceCategoriesScreen(
    priceViewModel: PriceViewModel,
    onSelectCategory: (Category) -> Unit,
    onBack: () -> Unit
) {
    val categories by priceViewModel.categories.collectAsState()
    val message by priceViewModel.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        priceViewModel.loadCategories()
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
        if (categories.isEmpty()) {
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

    if (showMessage) {
        AlertDialog(
            onDismissRequest = { showMessage = false },
            title = { Text("Сообщение") },
            text = { Text(message ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    showMessage = false
                    priceViewModel.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}