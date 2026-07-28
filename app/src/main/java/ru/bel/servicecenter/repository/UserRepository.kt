package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.User
interface UserRepository {
    suspend fun createUser(user: User): User
    suspend fun updateUser(user: User): User
    suspend fun deleteUser(user: User)
    suspend fun getUsersByClientId(clientId: String): List<User>
    suspend fun getUserByEmail(email: String): User?
    suspend fun getAllUsers(): List<User>
}