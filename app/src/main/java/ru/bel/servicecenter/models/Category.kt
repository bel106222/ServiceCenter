package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val category_name: String,      // название категории услуг
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)
