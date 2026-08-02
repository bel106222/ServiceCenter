package ru.bel.servicecenter.utils
import timber.log.Timber

/**
 * Специальное дерево Timber, которое перенаправляет все логи в LoggerService.
 * Формат: "Уровень/Тег: сообщение"
 */
class StatusTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            2 -> "V"   // Timber.VERBOSE
            3 -> "D"   // Timber.DEBUG
            4 -> "I"   // Timber.INFO
            5 -> "W"   // Timber.WARN
            6 -> "E"   // Timber.ERROR
            7 -> "A"   // Timber.ASSERT
            else -> "?"
        }
        val fullMessage = "$level/${tag ?: "?"}: $message"
        LoggerService.log(fullMessage)
    }
}