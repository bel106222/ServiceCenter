package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.OrderItem
interface OrderItemRepository {
    suspend fun createOrderItem(item: OrderItem): OrderItem
    suspend fun updateOrderItem(item: OrderItem): OrderItem
    suspend fun deleteOrderItem(item: OrderItem)
    suspend fun getOrderItemsByOrderId(orderId: String): List<OrderItem>
}