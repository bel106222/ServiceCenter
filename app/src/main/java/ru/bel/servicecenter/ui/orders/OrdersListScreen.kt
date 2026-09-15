package ru.bel.servicecenter.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.viewmodels.OrderViewModel

/**
 * Фильтр по статусу заказа.
 */
private enum class StatusFilter(val label: String) {
    ALL("Все"),
    ACTIVE("В работе"),
    COMPLETED("Завершённые")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orderViewModel: OrderViewModel,
    onEditOrder: (OrderWithItems) -> Unit,
    onDeleteOrder: (OrderWithItems) -> Unit,
    onAddNewOrder: () -> Unit,
    onBack: () -> Unit
) {
    val orders by orderViewModel.ordersWithItems.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()

    // Заказ, который пользователь хочет удалить (для диалога подтверждения).
    var orderToDelete by remember { mutableStateOf<OrderWithItems?>(null) }

    // Строка поиска
    var searchQuery by remember { mutableStateOf("") }

    // Активный фильтр по статусу
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }

    LaunchedEffect(Unit) {
        orderViewModel.loadOrders()
    }

    // Отфильтрованный и найденный список.
    // Пересчитывается только когда меняется один из ключей.
    val filteredOrders = remember(orders, searchQuery, statusFilter) {
        orders.filter { order ->
            // 1. Фильтр по статусу
            val statusMatch = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> !order.is_completed
                StatusFilter.COMPLETED -> order.is_completed
            }

            // 2. Поиск по номеру или описанию
            val query = searchQuery.trim().lowercase()
            val searchMatch = query.isEmpty() ||
                    order.order_number.lowercase().contains(query) ||
                    order.order_description.lowercase().contains(query)

            statusMatch && searchMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewOrder) {
                Icon(Icons.Default.Add, contentDescription = "Добавить заказ")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ============================================================
            // Поиск и фильтры
            // ============================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск") },
                    placeholder = { Text("Номер или описание") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Три чипа-фильтра
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilter.values().forEach { filter ->
                        FilterChip(
                            selected = statusFilter == filter,
                            onClick = { statusFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }

                // Подсказка про свайп — одна на весь экран, под фильтрами.
                Spacer(Modifier.height(4.dp))
                Text(
                    "Смахните карточку вправо, чтобы удалить",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ============================================================
            // Основной контент
            // ============================================================
            Box(modifier = Modifier.weight(1f)) {
                when {
                    // Идёт загрузка — показываем крутилку
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    // Заказов вообще нет
                    orders.isEmpty() -> EmptyOrdersState()

                    // Есть заказы, но они не прошли фильтр или поиск
                    filteredOrders.isEmpty() -> NoResultsState()

                    // Список
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            SwipeableOrderCard(
                                order = order,
                                onClick = { onEditOrder(order) },
                                onSwipeToDelete = { orderToDelete = order }
                            )
                        }
                    }
                }
            }
        }
    }

    // ================================================================
    // Диалог подтверждения удаления
    // ================================================================
    orderToDelete?.let { order ->
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Удалить заказ?") },
            text = {
                Text("Заказ № ${order.order_number} будет удалён. Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteOrder(order)
                    orderToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Состояние «заказов вообще нет».
 */
@Composable
private fun EmptyOrdersState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Receipt, null, Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("Заказов пока нет", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Нажмите +, чтобы создать первый заказ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Состояние «есть заказы, но фильтр/поиск ничего не нашли».
 */
@Composable
private fun NoResultsState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search, null, Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Попробуйте изменить запрос или фильтр",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Карточка заказа с возможностью смахнуть вправо для удаления.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableOrderCard(
    order: OrderWithItems,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeToDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .size(28.dp)
                )
            }
        }
    ) {
        OrderCard(order = order, onClick = onClick)
    }
}

/**
 * Одна карточка заказа.
 */
@Composable
private fun OrderCard(
    order: OrderWithItems,
    onClick: () -> Unit
) {
    val formattedSum = remember(order.order_sum) {
        "%.2f".format(order.order_sum)
    }

    val formattedDate = remember(order.created_at) {
        if (order.created_at.length >= 16) {
            order.created_at.take(16).replace("T", " ")
        } else {
            order.created_at
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Строка 1: иконка, номер заказа, чип статуса
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Receipt, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Заказ № ${order.order_number} от $formattedDate",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(isCompleted = order.is_completed)
            }

            // Строка 2: описание
            if (order.order_description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Description, null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        order.order_description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Строка 3: сумма
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Payments, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Сумма: $formattedSum руб.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Строка 4: количество услуг
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Оказано услуг: ${order.order_items.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Маленький чип, показывающий статус заказа.
 */
@Composable
private fun StatusChip(isCompleted: Boolean) {
    val containerColor = if (isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isCompleted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val label = if (isCompleted) "Завершён" else "В работе"

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}