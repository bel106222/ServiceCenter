package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.Role
interface RoleRepository {
    suspend fun createRole(role: Role): Role
    suspend fun getRoleByName(name: String): Role?
}