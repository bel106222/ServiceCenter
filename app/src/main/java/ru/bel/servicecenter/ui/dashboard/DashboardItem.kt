package ru.bel.servicecenter.ui.dashboard

// Модель пункта меню для дашборда
data class DashboardItem(
    val title: String,
    val onClick: () -> Unit
)