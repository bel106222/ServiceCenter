package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderItem(
    val id: String = UUID.randomUUID().toString(),
    val order_id: String,           // внешний ключ к Order
    val service_id: String,         // id оказанной услуги (Service)
    val services: ServiceName? = null, // наименование оказанной услуги
    val user_id: String,            // инженер, выполнивший услугу
    val orderitem_quantity: Int = 1,// количество
    val orderitem_cost: Float = 0f, // стоимость позиции
    val is_online: Boolean = false, // true – удалённое выполнение
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)

