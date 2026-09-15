package ru.bel.servicecenter.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Глобальная шина сообщений для Snackbar.
 * ViewModel отправляют сообщения — AppNavigation их показывает в Snackbar.
 */
object MessageBus {

    // Буфер на 10 сообщений, чтобы не терять их при быстрой отправке
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messages: SharedFlow<String> = _messages

    /**
     * Отправить сообщение для показа пользователю.
     * Не блокирует вызывающий код, работает даже без активных подписчиков.
     */
    fun show(message: String) {
        _messages.tryEmit(message)
    }
}