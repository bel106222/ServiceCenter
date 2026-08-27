package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Service(
    val id: String = UUID.randomUUID().toString(),
    val service_name: String,       // название услуги
    val service_description: String,// описание
    val category_id: String,        // внешний ключ к Category
    val is_fixprice: Boolean,       // true – фиксированная цена
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)
@Serializable
data class ServiceName(
    val service_name: String
)