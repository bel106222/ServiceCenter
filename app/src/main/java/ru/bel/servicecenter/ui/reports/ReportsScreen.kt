package ru.bel.servicecenter.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Описание одного отчёта в меню.
 */
data class ReportMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    role: String,
    onOpenMyOrders: () -> Unit,
    onOpenMyServices: () -> Unit,
    onOpenMyMonthly: () -> Unit,
    onOpenAllOrders: () -> Unit,
    onOpenAllServices: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenEngineers: () -> Unit,
    onOpenCategories: () -> Unit,
    onBack: () -> Unit
) {

    val items: List<ReportMenuItem> = when (role) {
        "user" -> listOf(
            ReportMenuItem(
                title = "Мои заказы за период",
                subtitle = "Список заказов с суммой и итогом",
                icon = Icons.Default.Receipt,
                onClick = onOpenMyOrders
            ),
            ReportMenuItem(
                title = "Мои расходы по услугам",
                subtitle = "Сколько раз и на какую сумму заказывали каждую услугу",
                icon = Icons.Default.Build,
                onClick = onOpenMyServices
            ),
            ReportMenuItem(
                title = "Мои расходы по месяцам",
                subtitle = "Сумма заказов по месяцам за выбранный период",
                icon = Icons.Default.CalendarMonth,
                onClick = onOpenMyMonthly
            )
        )
        "engineer", "admin" -> listOf(
            ReportMenuItem(
                title = "Все заказы за период",
                subtitle = "Список всех заказов с суммой и итогом",
                icon = Icons.Default.List,
                onClick = onOpenAllOrders
            ),
            ReportMenuItem(
                title = "Выручка по услугам",
                subtitle = "Сколько раз и на какую сумму оказана каждая услуга",
                icon = Icons.Default.Build,
                onClick = onOpenAllServices
            ),
            ReportMenuItem(
                title = "Заказы по клиентам",
                subtitle = "Количество заказов и сумма по каждому клиенту",
                icon = Icons.Default.Business,
                onClick = onOpenClients
            ),
            ReportMenuItem(
                title = "Услуги по сотрудникам",
                subtitle = "Количество оказанных услуг и сумма по каждому сотруднику",
                icon = Icons.Default.Person,
                onClick = onOpenEngineers
            ),
            ReportMenuItem(
                title = "Выручка по категориям",
                subtitle = "Сумма и количество услуг по категориям",
                icon = Icons.Default.Category,
                onClick = onOpenCategories
            )
        )
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчёты") },
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
        if (items.isEmpty()) {
            // Заглушка для ролей, для которых отчёты ещё не сделаны
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Assessment, null,
                    Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text("Отчёты в разработке", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Для вашей роли отчёты появятся позже",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(items) { item ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { item.onClick() },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                item.icon, null,
                                Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}