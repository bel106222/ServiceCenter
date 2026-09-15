package ru.bel.servicecenter.ui.prices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.Price
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.viewmodels.PriceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceServicesScreen(
    priceViewModel: PriceViewModel,
    category: Category,
    onEditPrice: (Service, Price?) -> Unit,
    onBack: () -> Unit
) {
    val servicesWithPrices by priceViewModel.serviceWithPrice.collectAsState()
    val isLoading by priceViewModel.isLoading.collectAsState()

    // Строка поиска
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(category.id) {
        priceViewModel.loadServicesWithPrices(category.id)
    }

    // Отфильтрованный список: ищем по названию услуги, без учёта регистра
    val filteredServices = remember(servicesWithPrices, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            servicesWithPrices
        } else {
            servicesWithPrices.filter {
                it.service.service_name.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Цены: ${category.category_name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ============================================================
            // Поиск по названию услуги
            // ============================================================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск") },
                placeholder = { Text("Название услуги") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ============================================================
            // Основной контент
            // ============================================================
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    // В категории нет услуг
                    servicesWithPrices.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Payments, null,
                            Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "В этой категории услуг пока нет",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Сначала добавьте услуги в разделе «Услуги»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Поиск ничего не нашёл
                    filteredServices.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search, null,
                            Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Попробуйте изменить запрос",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Список услуг с ценами
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredServices, key = { it.service.id }) { item ->
                            PriceServiceCard(
                                item = item,
                                onClick = { onEditPrice(item.service, item.price) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка услуги с ценой. Кликабельна → открывается экран редактирования цены.
 */
@Composable
private fun PriceServiceCard(
    item: PriceViewModel.ServiceWithPrice,
    onClick: () -> Unit
) {
    // Форматируем стоимость (2 знака после запятой)
    val formattedCost = item.price?.let { "%.2f".format(it.service_cost) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Строка 1: иконка + название услуги + иконка «изменить»
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.service.service_name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Edit, null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Строка 2: цена
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Payments, null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (item.price != null && formattedCost != null) {
                        if (item.price.is_time) "$formattedCost руб/час"
                        else "$formattedCost руб."
                    } else {
                        "Цена не задана"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Строка 3: пометка про почасовую оплату
            if (item.price != null && item.price.is_time) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule, null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Почасовая оплата",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}