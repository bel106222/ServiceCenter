package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class OrderItemController : ViewModel() {

    private val _items = MutableStateFlow<List<OrderItem>>(emptyList())
    val items: StateFlow<List<OrderItem>> = _items

    private val _currentItem = MutableStateFlow(
        OrderItem(order_id = "", service_id = "", user_id = "")
    )
    val currentItem: StateFlow<OrderItem> = _currentItem

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null
    var currentOrderId: String = ""

    fun loadItems() {
        viewModelScope.launch {
            try {
                if (currentOrderId.isBlank()) {
                    _items.value = emptyList(); return@launch
                }
                _items.value = RepositoryProvider.orderItemRepo.getOrderItemsByOrderId(currentOrderId)
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки позиций: ${e.message}"
            }
        }
    }

    fun saveItem() {
        val item = _currentItem.value
        val authUser = currentAuthUser

        viewModelScope.launch {
            if (authUser == null) { _message.value = "Не выполнен вход"; return@launch }
            if (!canModify(authUser)) {
                _message.value = "Недостаточно прав для изменения позиций заказа"; return@launch
            }

            val errs = mutableMapOf<String, String?>()
            errs["service_id"] = if (item.service_id.isBlank()) "Выберите услугу" else null
            errs["user_id"] = if (item.user_id.isBlank()) "Инженер не выбран" else null
            errs["orderitem_cost"] = if (item.orderitem_cost <= 0) "Стоимость должна быть > 0" else null
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                val newItem = item.copy(order_id = currentOrderId)
                if (item.id.isEmpty() || _items.value.none { it.id == item.id }) {
                    RepositoryProvider.orderItemRepo.createOrderItem(newItem)
                    _message.value = "Позиция добавлена"
                } else {
                    RepositoryProvider.orderItemRepo.updateOrderItem(newItem)
                    _message.value = "Позиция обновлена"
                }
                loadItems()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения позиции: ${e.message}"
            }
        }
    }

    fun deleteItem(item: OrderItem) {
        val authUser = currentAuthUser
        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }
            try {
                RepositoryProvider.orderItemRepo.deleteOrderItem(item)
                loadItems()
                _message.value = "Позиция удалена"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления позиции: ${e.message}"
            }
        }
    }

    fun setEditingItem(item: OrderItem) {
        _currentItem.value = item
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        val i = _currentItem.value
        _currentItem.value = when (field) {
            "service_id" -> i.copy(service_id = value)
            "user_id" -> i.copy(user_id = value)
            "quantity" -> i.copy(orderitem_quantity = value.toIntOrNull() ?: 1)
            "cost" -> i.copy(orderitem_cost = value.toFloatOrNull() ?: 0f)
            "is_online" -> i.copy(is_online = value.toBoolean())
            else -> i
        }
    }

    private suspend fun canModify(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == admin?.id || user.role_id == eng?.id
    }
}