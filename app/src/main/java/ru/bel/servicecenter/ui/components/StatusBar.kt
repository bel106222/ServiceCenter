package ru.bel.servicecenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.StatusViewModel

/**
 * Только строка состояния. При нажатии вызывает onShowHistory.
 */
@Composable
fun StatusBar(
    viewModel: StatusViewModel,
    onShowHistory: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    val lastLog = logs.lastOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onShowHistory() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = lastLog?.let { "[${it.timestamp}] ${it.message}" } ?: "Готов",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}