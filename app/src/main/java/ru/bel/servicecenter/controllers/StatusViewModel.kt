package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.bel.servicecenter.utils.LoggerService

class StatusViewModel : ViewModel() {
    val logs: StateFlow<List<LoggerService.LogEntry>> = LoggerService.entries

    fun clearLogs() = LoggerService.clear()
}