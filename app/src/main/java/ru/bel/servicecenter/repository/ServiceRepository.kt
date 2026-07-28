package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.Service
interface ServiceRepository {
    suspend fun createService(service: Service): Service
    suspend fun updateService(service: Service): Service
    suspend fun deleteService(service: Service)
    suspend fun getServicesByCategoryId(categoryId: String): List<Service>
    suspend fun getAllServices(): List<Service>
    suspend fun getServiceByName(name: String): Service?
}