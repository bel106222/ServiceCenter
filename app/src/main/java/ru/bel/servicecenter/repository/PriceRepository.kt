package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.Price
interface PriceRepository {
    suspend fun createPrice(price: Price): Price
    suspend fun updatePrice(price: Price): Price
    suspend fun deletePrice(price: Price)
    suspend fun getPriceByServiceId(serviceId: String): Price?
    suspend fun getAllPrices(): List<Price>
}