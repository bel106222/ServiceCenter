package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.Client
interface ClientRepository {
    suspend fun createClient(client: Client): Client
    suspend fun updateClient(client: Client): Client
    suspend fun deleteClient(client: Client)  // SoftDelete (установит deleted_at)
    suspend fun getAllClients(): List<Client>
}