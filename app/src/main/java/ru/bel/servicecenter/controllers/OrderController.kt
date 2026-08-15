package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.Order
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class OrderController : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _currentOrder = MutableStateFlow<Order?>(null)
    val currentOrder: StateFlow<Order?> = _currentOrder

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    // Добавьте новое состояние для имени автора
    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName

    // Флаг: является ли текущий пользователь admin/engineer (для прав на is_completed/is_time)
    var isAdminOrEngineer: Boolean = false
        private set

    var currentAuthUser: User? = null

    /**
     * Возвращает список заказов для текущего пользователя в зависимости от его роли.
     * Для admin и engineer — все заказы, для user — только его заказы.
     */
    suspend fun fetchOrders(): List<Order> {
        val authUser = currentAuthUser
        if (authUser == null) {
            Timber.e("fetchOrders: currentAuthUser is null")
            return emptyList()
        }
        val role = getRoleName(authUser.role_id)
        Timber.d("fetchOrders: role=$role, user=${authUser.user_name}")
        val result = try {
            when (role) {
                "admin", "engineer" -> RepositoryProvider.orderRepo.getAllOrders()
                "user" -> RepositoryProvider.orderRepo.getOrderByUserId(authUser.id)
                else -> emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "fetchOrders: error loading orders")
            emptyList()
        }
        Timber.d("fetchOrders: got ${result.size} orders")
        return result
    }

    // Инициализация для создания нового заказа
    suspend fun initForCreating() {
        _isInitializing.value = true
        try {
            val user = currentAuthUser ?: return
            isAdminOrEngineer = checkIfAdminOrEngineer(user)
            val client = getClientForUser(user)
            val orderNumber = generateOrderNumber(client?.client_title ?: "XXX")
            _currentOrder.value = Order(
                order_number = orderNumber,
                order_description = "",
                user_id = user.id,
                is_completed = false,
                is_time = false
            )
            _authorName.value = user.user_name   // автор – текущий пользователь
            _errors.value = emptyMap()
        } finally {
            _isInitializing.value = false
        }
    }

    // Инициализация для редактирования существующего заказа
    suspend fun initForEditing(order: Order) {
        _isInitializing.value = true
        try {
            val user = currentAuthUser ?: return
            isAdminOrEngineer = checkIfAdminOrEngineer(user)
            _currentOrder.value = order
            // Определяем имя автора по user_id
            val allUsers = RepositoryProvider.userRepo.getAllUsers()
            val author = allUsers.find { it.id == order.user_id }
            _authorName.value = author?.user_name ?: "Не установлен"
            _errors.value = emptyMap()
        } finally {
            _isInitializing.value = false
        }
    }

    // Загрузка списка заказов (оставляем как было, но добавим сброс сообщения при необходимости)
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
        val order = _currentOrder.value ?: return
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }

        viewModelScope.launch {
            val isNew = order.id.isEmpty() || _orders.value.none { it.id == order.id }

            // Права на создание/редактирование
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

            // Валидация
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
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }
        viewModelScope.launch {
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

    fun clearMessage() {
        _message.value = null
    }

    // ------------------- Вспомогательные методы -------------------

    private suspend fun checkIfAdminOrEngineer(user: User): Boolean {
        val adminRole = RepositoryProvider.roleRepo.getRoleByName("admin")
        val engRole = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == adminRole?.id || user.role_id == engRole?.id
    }

    private suspend fun getClientForUser(user: User): Client? {
        val clientId = user.client_id ?: return null
        return RepositoryProvider.clientRepo.getAllClients().find { it.id == clientId }
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

    private suspend fun canEditOrder(authUser: User, order: Order): Boolean {
        val role = getRoleName(authUser.role_id)
        return when (role) {
            "admin", "engineer" -> true
            "user" -> order.user_id == authUser.id
            else -> false
        }
    }

    fun updateField(field: String, value: String) {
        val order = _currentOrder.value ?: return
        _currentOrder.value = when (field) {
            "number" -> order.copy(order_number = value)
            "description" -> order.copy(order_description = value)
            "user_id" -> order.copy(user_id = value)
            "is_completed" -> order.copy(is_completed = value.toBoolean())
            "is_time" -> order.copy(is_time = value.toBoolean())
            else -> order
        }
    }

    // Генерация уникального номера заказа
    private suspend fun generateOrderNumber(clientTitle: String): String {
        val prefix = generatePrefix(clientTitle)
        val allOrders = RepositoryProvider.orderRepo.getAllOrders()
        var maxNum = allOrders.filter { it.order_number.startsWith(prefix) }
            .mapNotNull { it.order_number.removePrefix(prefix).toIntOrNull() }
            .maxOrNull() ?: 0
        var next = maxNum + 1
        var candidate = prefix + next.toString().padStart(5, '0')
        while (RepositoryProvider.orderRepo.getOrderByOrderNumber(candidate) != null) {
            next++
            candidate = prefix + next.toString().padStart(5, '0')
        }
        return candidate
    }

    private fun generatePrefix(title: String): String {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return "XXX"
        val first = cleaned.take(3)
        if (first.length == 3) return first.uppercase()
        val last = cleaned.last()
        return (first + last.toString().repeat(3 - first.length)).uppercase()
    }
}