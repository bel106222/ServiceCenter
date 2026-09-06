package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.bel.servicecenter.utils.LoggerService

/**
 * ViewModel для статусной строки и истории сообщений.
 */
class StatusViewModel : ViewModel() {
    val logs: StateFlow<List<LoggerService.LogEntry>> = LoggerService.entries

    fun clearLogs() = LoggerService.clear()
}