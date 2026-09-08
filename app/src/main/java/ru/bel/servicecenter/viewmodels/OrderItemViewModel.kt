package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

class OrderItemViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<OrderItem>>(emptyList())
    val items: StateFlow<List<OrderItem>> = _items

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    private val _currentItem = MutableStateFlow<OrderItem?>(null)
    val currentItem: StateFlow<OrderItem?> = _currentItem

    private val _selectedService = MutableStateFlow<Service?>(null)
    val selectedService: StateFlow<Service?> = _selectedService

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null
    var currentOrderId: String = ""
    var orderViewModel: OrderViewModel? = null

    private val priceCache = mutableMapOf<String, Float>()

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
                Timber.e(e, "Ошибка загрузки услуг")
            }
        }
    }

    fun startNewItem() {
        val user = currentAuthUser ?: return
        _currentItem.value = OrderItem(
            id = java.util.UUID.randomUUID().toString(),
            order_id = currentOrderId,
            service_id = "",
            user_id = user.id,
            orderitem_quantity = 1,
            orderitem_cost = 0f,
            is_online = false,
            created_at = java.time.LocalDateTime.now().toString(),
            deleted_at = null
        )
        _selectedService.value = null
        _message.value = null
    }

    fun startEditing(item: OrderItem) {
        _currentItem.value = item
        _selectedService.value = _services.value.find { it.id == item.service_id }
        _message.value = null
    }

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

    fun recalculateCost() {
        val item = _currentItem.value ?: return
        val service = _selectedService.value ?: return
        val baseCost = priceCache[service.id] ?: 0f
        _currentItem.value = item.copy(orderitem_cost = baseCost * item.orderitem_quantity)
    }

    fun saveItem(onSaved: () -> Unit) {
        val item = _currentItem.value ?: return
        if (item.id.isEmpty() || _items.value.none { it.id == item.id }) {
            orderViewModel?.addItemToDraft(item)
        } else {
            orderViewModel?.updateItemInDraft(item)
        }
        onSaved()
    }

    fun deleteDraftItem(item: OrderItem, onDeleted: () -> Unit) {
        orderViewModel?.deleteItemFromDraft(item.id)
        onDeleted()
    }

    fun clearMessage() {
        _message.value = null
    }
}