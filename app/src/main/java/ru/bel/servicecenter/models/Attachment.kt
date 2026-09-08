package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Модель приложения к заказу (фотография).
 * Хранит ссылку на файл в Supabase Storage.
 */
@Serializable
data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val order_id: String,
    val filename: String,
    val url: String,
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)