package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Order
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class OrderController : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _currentOrder = MutableStateFlow(
        Order(order_number = "", order_description = "", user_id = "")
    )
    val currentOrder: StateFlow<Order> = _currentOrder

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val authUser = currentAuthUser
                if (authUser == null) {
                    _orders.value = emptyList()
                    return@launch
                }
                val role = getRoleName(authUser.role_id)
                _orders.value = when (role) {
                    "admin", "engineer" -> RepositoryProvider.orderRepo.getAllOrders()
                    "user" -> RepositoryProvider.orderRepo.getOrderByUserId(authUser.id)
                    else -> emptyList()
                }
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки заказов: ${e.message}"
            }
        }
    }

    fun saveOrder() {
        val order = _currentOrder.value
        val authUser = currentAuthUser

        viewModelScope.launch {
            if (authUser == null) { _message.value = "Не выполнен вход"; return@launch }

            val isNew = order.id.isEmpty() || _orders.value.none { it.id == order.id }
            if (isNew) {
                val role = getRoleName(authUser.role_id)
                if (role == "user" && order.user_id != authUser.id) {
                    _message.value = "Вы можете создавать только свои заказы"; return@launch
                }
            } else {
                if (!canEditOrder(authUser, order)) {
                    _message.value = "Недостаточно прав"; return@launch
                }
            }

            val errs = mutableMapOf<String, String?>()
            errs["order_number"] = ValidationRules.validateRequired(order.order_number, "Номер")
            errs["order_description"] = ValidationRules.validateRequired(order.order_description, "Описание")
            errs["user_id"] = if (order.user_id.isBlank()) "Владелец заказа обязателен" else null
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                if (isNew) {
                    RepositoryProvider.orderRepo.createOrder(order)
                    _message.value = "Заказ создан"
                } else {
                    RepositoryProvider.orderRepo.updateOrder(order)
                    _message.value = "Заказ обновлён"
                }
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }

    fun deleteOrder(order: Order) {
        val authUser = currentAuthUser
        viewModelScope.launch {
            if (authUser == null) { _message.value = "Не выполнен вход"; return@launch }
            if (!canEditOrder(authUser, order)) { _message.value = "Недостаточно прав"; return@launch }
            try {
                RepositoryProvider.orderRepo.deleteOrder(order)
                loadOrders()
                _message.value = "Заказ удалён"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun setEditingOrder(order: Order) {
        _currentOrder.value = order
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        val o = _currentOrder.value
        _currentOrder.value = when (field) {
            "number" -> o.copy(order_number = value)
            "description" -> o.copy(order_description = value)
            "user_id" -> o.copy(user_id = value)
            "is_completed" -> o.copy(is_completed = value.toBoolean())
            "is_time" -> o.copy(is_time = value.toBoolean())
            "order_sum" -> o.copy(order_sum = value.toFloatOrNull() ?: 0f)
            else -> o
        }
    }

    private suspend fun canEditOrder(authUser: User, order: Order): Boolean {
        val role = getRoleName(authUser.role_id)
        return when (role) {
            "admin", "engineer" -> true
            "user" -> order.user_id == authUser.id
            else -> false
        }
    }

    private suspend fun getRoleName(roleId: String): String? {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        val user = RepositoryProvider.roleRepo.getRoleByName("user")
        return when (roleId) {
            admin?.id -> "admin"
            eng?.id -> "engineer"
            user?.id -> "user"
            else -> null
        }
    }
}