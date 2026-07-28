package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Price(
    val id: String = UUID.randomUUID().toString(),
    val service_id: String,         // внешний ключ к Service
    val service_cost: Float,        // стоимость услуги
    val is_time: Boolean,           // true – почасовая оплата
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)
