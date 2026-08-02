package ru.bel.servicecenter.utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
/**
 * Сервис для хранения и отображения пользовательских сообщений и логов.
 * Каждое сообщение автоматически получает временную метку.
 */
object LoggerService {

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries

    fun log(message: String) {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
        // Атомарное добавление элемента
        _entries.update { currentList ->
            currentList + LogEntry(timestamp, message)
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    data class LogEntry(
        val timestamp: String,
        val message: String
    )
}