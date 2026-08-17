package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.Order
interface OrderRepository {
    suspend fun createOrder(order: Order): Order
    suspend fun updateOrder(order: Order): Order
    suspend fun deleteOrder(order: Order)
    suspend fun getOrderByUserId(userId: String): List<Order>
    suspend fun getOrderByOrderNumber(orderNumber: String): Order?
    suspend fun getAllOrders(): List<Order>

    suspend fun getOrderById(orderId: String): Order?
}