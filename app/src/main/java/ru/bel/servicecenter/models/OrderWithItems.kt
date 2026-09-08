package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable

@Serializable
data class UserName(
    val user_name: String
)
@Serializable
data class OrderWithItems(
    val id: String,
    val order_number: String,
    val order_description: String,
    val user_id: String,
    val order_sum: Float = 0f,
    val is_completed: Boolean = false,
    val is_time: Boolean = false,
    val created_at: String = "",
    val deleted_at: String? = null,
    val order_items: List<OrderItem> = emptyList(),
    val users: UserName? = null
)