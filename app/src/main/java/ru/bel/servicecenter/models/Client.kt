package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

// Data-класс "Клиент". Использует @Serializable для автоматического преобразования в/из JSON.
@Serializable
data class Client(
    val id: String = UUID.randomUUID().toString(), // первичный ключ (UUID)
    val client_title: String,                      // название организации / клиента
    val client_address: String,                    // адрес
    val client_details: String,                    // реквизиты
    val is_legal: Boolean,                         // true – юр. лицо
    val created_at: String = java.time.LocalDateTime.now().toString(), // дата создания
    val deleted_at: String? = null                 // null – запись активна (SoftDelete)
)
