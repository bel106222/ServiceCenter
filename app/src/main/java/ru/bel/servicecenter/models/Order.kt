package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Order(
    val id: String = UUID.randomUUID().toString(),
    val order_number: String,       // номер заказа
    val order_description: String,  // описание
    val user_id: String,            // владелец заказа (User)
    val order_sum: Float = 0f,      // общая сумма
    val is_completed: Boolean = false, // завершён ли заказ
    val is_time: Boolean = false,   // признак почасовой оплата
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)
