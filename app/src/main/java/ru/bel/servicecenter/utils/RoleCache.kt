package ru.bel.servicecenter.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.bel.servicecenter.repository.RepositoryProvider

/**
 * Глобальный кэш ролей.
 * Загружается один раз после проверки БД и используется во всём приложении.
 */
object RoleCache {

    private val roles = mutableMapOf<String, String>() // id -> название

    /**
     * Загружает роли из БД и заполняет кэш.
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val engineer = RepositoryProvider.roleRepo.getRoleByName("engineer")
        val user = RepositoryProvider.roleRepo.getRoleByName("user")

        roles.clear()
        admin?.let { roles[it.id] = "admin" }
        engineer?.let { roles[it.id] = "engineer" }
        user?.let { roles[it.id] = "user" }

        LoggerService.log("Роли загружены в кэш: ${roles.size}")
    }

    /**
     * Возвращает название роли по её id или null, если роль не найдена.
     */
    fun get(roleId: String): String? = roles[roleId]
}