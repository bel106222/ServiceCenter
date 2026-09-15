package ru.bel.servicecenter.ui.reports

import java.time.LocalDateTime

/**
 * Доступные периоды для отчётов.
 * startDate() возвращает строку-начало периода в формате, совместимом
 * с полем created_at (ISO-8601: "2024-01-15T15:30:45.123456").
 * Сравнение строк работает корректно, потому что формат фиксированный.
 */
enum class ReportPeriod(val label: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    QUARTER("3 месяца"),
    ALL("Всё время");

    fun startDate(): String {
        val now = LocalDateTime.now()
        val from = when (this) {
            WEEK -> now.minusDays(7)
            MONTH -> now.minusDays(30)
            QUARTER -> now.minusDays(90)
            ALL -> LocalDateTime.of(2000, 1, 1, 0, 0)
        }
        return from.toString()
    }
}