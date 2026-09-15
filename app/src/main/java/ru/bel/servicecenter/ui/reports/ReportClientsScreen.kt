package ru.bel.servicecenter.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.viewmodels.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportClientsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val rows by viewModel.clientRows.collectAsState()
    val total by viewModel.clientRowsTotal.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    LaunchedEffect(selectedPeriod) {
        viewModel.loadClientsReport()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказы по клиентам") },
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
                        "Итого: ${total.clients} клиент(ов), ${total.totalCount} заказ(ов)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${"%.2f".format(total.totalSum)} руб.",
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
            PeriodSelector(
                selected = selectedPeriod,
                onSelect = { viewModel.selectPeriod(it) }
            )

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                rows.isEmpty() -> Box(
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TableHeaderCell("Клиент", weight = 2f)
                        TableHeaderCell("Заказов", weight = 0.9f, alignEnd = true)
                        TableHeaderCell("Сумма", weight = 1.2f, alignEnd = true)
                    }
                    HorizontalDivider()

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(rows, key = { it.clientTitle }) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                TableBodyCell(row.clientTitle, weight = 2f)
                                TableBodyCell(row.count.toString(), weight = 0.9f, alignEnd = true)
                                TableBodyCell(
                                    "%.2f".format(row.sum),
                                    weight = 1.2f,
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