package ru.bel.servicecenter.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.viewmodels.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportOrdersScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val orders by viewModel.myOrders.collectAsState()
    val total by viewModel.myOrdersTotal.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    // Перезагружаем отчёт при смене периода или при входе на экран.
    LaunchedEffect(selectedPeriod) {
        viewModel.loadMyOrdersReport()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заказы") },
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
        bottomBar = {
            // Итоговая строка снизу
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Итого: ${total.count} заказ(ов)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${"%.2f".format(total.sum)} руб.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Выбор периода
            PeriodSelector(
                selected = selectedPeriod,
                onSelect = { viewModel.selectPeriod(it) }
            )

            // Таблица
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                orders.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "За выбранный период заказов нет",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    // Заголовок таблицы
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TableHeaderCell("Дата", weight = 1.4f)
                        TableHeaderCell("Номер", weight = 1.2f)
                        TableHeaderCell("Сумма", weight = 1f, alignEnd = true)
                    }
                    HorizontalDivider()

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(orders, key = { it.id }) { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                TableBodyCell(
                                    formatDate(order.created_at),
                                    weight = 1.4f
                                )
                                TableBodyCell(
                                    order.order_number,
                                    weight = 1.2f
                                )
                                TableBodyCell(
                                    "%.2f".format(order.order_sum),
                                    weight = 1f,
                                    alignEnd = true
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Ряд кнопок-периодов. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(
    selected: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPeriod.values().forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(period.label) }
            )
        }
    }
}

@Composable
fun RowScope.TableHeaderCell(
    text: String,
    weight: Float,
    alignEnd: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(weight),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun RowScope.TableBodyCell(
    text: String,
    weight: Float,
    alignEnd: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(weight),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

/** Дата: "2024-01-15T15:30:45.123456" → "2024-01-15 15:30". */
fun formatDate(raw: String): String {
    return if (raw.length >= 16) raw.take(16).replace("T", " ")
    else raw
}