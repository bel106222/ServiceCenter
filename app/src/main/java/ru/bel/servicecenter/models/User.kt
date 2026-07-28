package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val user_name: String,          // имя пользователя
    val user_email: String,         // email (используется для входа)
    val user_phone: String,         // телефон
    val user_password: String,      // хешированный пароль (bcrypt)
    val client_id: String? = null,  // внешний ключ к Client (может быть пустым)
    val role_id: String,            // внешний ключ к Role
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)

