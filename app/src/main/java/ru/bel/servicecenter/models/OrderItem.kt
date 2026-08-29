package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderItem(
    val id: String = "",
    val order_id: String,
    val service_id: String,
    val user_id: String,
    val orderitem_quantity: Int = 1,
    val orderitem_cost: Float = 0f,
    val is_online: Boolean = false,
    val created_at: String = "",
    val deleted_at: String? = null,
    val services: ServiceName? = null   // ← новое поле
)