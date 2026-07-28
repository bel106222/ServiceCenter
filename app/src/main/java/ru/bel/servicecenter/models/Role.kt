package ru.bel.servicecenter.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Role(
    val id: String = UUID.randomUUID().toString(),
    val role_name: String,          // название роли (admin, engineer, user)
    val created_at: String = java.time.LocalDateTime.now().toString(),
    val deleted_at: String? = null
)

