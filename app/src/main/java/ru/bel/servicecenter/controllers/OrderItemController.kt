package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import timber.log.Timber
import java.util.UUID

class OrderItemController : ViewModel() {

    private val _items = MutableStateFlow<List<OrderItem>>(emptyList())
    val items: StateFlow<List<OrderItem>> = _items

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    // Кэш актуальных цен: serviceId -> стоимость последней цены
    private val priceCache = mutableMapOf<String, Float>()

    private val _currentItem = MutableStateFlow<OrderItem?>(null)
    val currentItem: StateFlow<OrderItem?> = _currentItem

    private val _selectedService = MutableStateFlow<Service?>(null)
    val selectedService: StateFlow<Service?> = _selectedService

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null
    var currentOrderId: String = ""

    var orderController: OrderController? = null

    private var isSaving = false   // флаг, чтобы избежать повторного сохранения

    /**
     * Загружает услуги и актуальные цены в кэш.
     * Для каждой услуги выбирается цена с самой поздней датой создания.
     */
    fun loadServices() {
        viewModelScope.launch {
            try {
                _services.value = RepositoryProvider.serviceRepo.getAllServices()
                val allPrices = RepositoryProvider.priceRepo.getAllPrices()
                val latestPrices = allPrices
                    .groupBy { it.service_id }
                    .mapValues { (_, prices) ->
                        prices.maxByOrNull { it.created_at }?.service_cost ?: 0f
                    }
                priceCache.clear()
                priceCache.putAll(latestPrices)
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки услуг: ${e.message}"
            }
        }
    }

    fun loadItems(orderId: String) {
        viewModelScope.launch {
            try {
                _items.value = RepositoryProvider.orderItemRepo.getOrderItemsByOrderId(orderId)
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки позиций: ${e.message}"
            }
        }
    }

    fun startNewItem() {
        val user = currentAuthUser ?: return
        _currentItem.value = OrderItem(
            id = UUID.randomUUID().toString(),
            order_id = currentOrderId,
            service_id = "",
            user_id = user.id,
            orderitem_quantity = 1,
            orderitem_cost = 0f,
            is_online = false
        )
        _selectedService.value = null
        _message.value = null
    }

    fun startEditing(item: OrderItem) {
        _currentItem.value = item
        _selectedService.value = _services.value.find { it.id == item.service_id }
        _message.value = null
    }

    /**
     * Выбирает услугу, автоматически подставляя количество = 1 и стоимость из кэша.
     */
    fun selectService(service: Service) {
        _selectedService.value = service
        val item = _currentItem.value ?: return
        val cost = priceCache[service.id] ?: 0f
        _currentItem.value = item.copy(
            service_id = service.id,
            orderitem_quantity = 1,
            orderitem_cost = cost
        )
    }

    fun updateQuantity(quantity: Int) {
        val item = _currentItem.value ?: return
        _currentItem.value = item.copy(orderitem_quantity = quantity)
        recalculateCost()
    }

    fun updateCost(cost: Float) {
        val item = _currentItem.value ?: return
        _currentItem.value = item.copy(orderitem_cost = cost)
    }

    fun updateIsOnline(value: Boolean) {
        val item = _currentItem.value ?: return
        _currentItem.value = item.copy(is_online = value)
    }

    /**
     * Пересчитывает стоимость на основе цены услуги из кэша и количества.
     */
    fun recalculateCost() {
        val item = _currentItem.value ?: return
        val service = _selectedService.value ?: return
        val baseCost = priceCache[service.id] ?: 0f
        _currentItem.value = item.copy(orderitem_cost = baseCost * item.orderitem_quantity)
    }

    fun saveItem(onSaved: () -> Unit) {
        val item = _currentItem.value ?: return
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }

        viewModelScope.launch {
            if (!canModify(authUser)) {
                _message.value = "Недостаточно прав для изменения позиций заказа"
                return@launch
            }
            try {
                val isNew = item.id.isEmpty() || _items.value.none { it.id == item.id }
                if (isNew) {
                    RepositoryProvider.orderItemRepo.createOrderItem(item)
                    _message.value = "Позиция добавлена"
                } else {
                    RepositoryProvider.orderItemRepo.updateOrderItem(item)
                    _message.value = "Позиция обновлена"
                }

                // Загружаем актуальный список позиций для этого заказа
                val updatedItems = RepositoryProvider.orderItemRepo.getOrderItemsByOrderId(item.order_id)
                // Локально обновляем текущий заказ в OrderController
                orderController?.updateCurrentOrderLocally(item.order_id, updatedItems)

                onSaved()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения позиции: ${e.message}"
            }
        }
    }

    fun deleteItem(item: OrderItem, onDeleted: () -> Unit) {
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }
        viewModelScope.launch {
            if (!canModify(authUser)) {
                _message.value = "Недостаточно прав"
                return@launch
            }
            try {
                RepositoryProvider.orderItemRepo.deleteOrderItem(item)
                // Получаем актуальный список позиций и обновляем текущий заказ
                val updatedItems = RepositoryProvider.orderItemRepo.getOrderItemsByOrderId(item.order_id)
                orderController?.updateCurrentOrderLocally(item.order_id, updatedItems)
                _message.value = "Позиция удалена"
                onDeleted()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления позиции: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun canModify(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == admin?.id || user.role_id == eng?.id
    }
}