package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.Order
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class OrderController : ViewModel() {

    // Список заказов (для отображения в списке)
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    // Список пользователей (для определения автора заказа)
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    // Список клиентов (для генерации номера заказа)
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients

    // Текущий редактируемый заказ
    private val _currentOrder = MutableStateFlow<Order?>(null)
    val currentOrder: StateFlow<Order?> = _currentOrder

    // Имя автора заказа (для отображения)
    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName

    // Ошибки валидации
    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    // Сообщение пользователю
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Позиции текущего заказа
    private val _orderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val orderItems: StateFlow<List<OrderItem>> = _orderItems

    // Список услуг (для отображения названий в позициях заказа)
    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    // Текущий авторизованный пользователь
    var currentAuthUser: User? = null

    // Флаг: создаётся новый заказ или редактируется существующий
    private var isNewOrder: Boolean = false

    // Флаг: является ли пользователь администратором или инженером
    var isAdminOrEngineer: Boolean = false
        private set

    /**
     * Загружает список заказов, пользователей и клиентов.
     * Вызывается при входе в раздел "Заказы".
     */
    suspend fun loadOrders() {
        val authUser = currentAuthUser ?: return
        val role = getRoleName(authUser.role_id)
        val ordersResult = when (role) {
            "admin", "engineer" -> RepositoryProvider.orderRepo.getAllOrders()
            "user" -> RepositoryProvider.orderRepo.getOrderByUserId(authUser.id)
            else -> emptyList()
        }
        _orders.value = ordersResult
        _users.value = RepositoryProvider.userRepo.getAllUsers()
        _clients.value = RepositoryProvider.clientRepo.getAllClients()
        isAdminOrEngineer = role == "admin" || role == "engineer"
        _services.value = RepositoryProvider.serviceRepo.getAllServices()
    }

    /**
     * Подготавливает контроллер для создания нового заказа.
     * Использует уже загруженные списки, без сетевых запросов.
     */
    fun prepareForNewOrder() {
        val user = currentAuthUser ?: return
        val clientTitle = _clients.value.find { it.id == user.client_id }?.client_title ?: "XXX"
        val orderNumber = generateOrderNumber(clientTitle)
        _currentOrder.value = Order(
            order_number = orderNumber,
            order_description = "",
            user_id = user.id,
            is_completed = false,
            is_time = false,
            order_sum = 0f
        )
        _authorName.value = user.user_name
        isNewOrder = true
        _errors.value = emptyMap()
        _message.value = null
        _orderItems.value = emptyList()
    }

    /**
     * Подготавливает контроллер для редактирования существующего заказа.
     * Загружает позиции заказа.
     */
    fun prepareForEdit(order: Order) {
        _currentOrder.value = order
        isNewOrder = false
        _authorName.value = _users.value.find { it.id == order.user_id }?.user_name ?: "Неизвестный"
        _errors.value = emptyMap()
        _message.value = null
        // Загружаем позиции заказа
        loadOrderItems(order.id)
    }

    /**
     * Обновляет поле текущего заказа.
     */
    fun updateField(field: String, value: String) {
        val order = _currentOrder.value ?: return
        _currentOrder.value = when (field) {
            "number" -> order.copy(order_number = value)
            "description" -> order.copy(order_description = value)
            "user_id" -> order.copy(user_id = value)
            "is_completed" -> order.copy(is_completed = value.toBoolean())
            "is_time" -> order.copy(is_time = value.toBoolean())
            "order_sum" -> order.copy(order_sum = value.toFloatOrNull() ?: 0f)
            else -> order
        }
    }

    /**
     * Сохраняет заказ (создание или обновление).
     */
    fun saveOrder() {
        val order = _currentOrder.value ?: return
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }

        viewModelScope.launch {
            val errs = mutableMapOf<String, String?>()
            errs["order_number"] = ValidationRules.validateRequired(order.order_number, "Номер")
            errs["order_description"] = ValidationRules.validateRequired(order.order_description, "Описание")
            errs["user_id"] = if (order.user_id.isBlank()) "Владелец заказа обязателен" else null
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                if (isNewOrder) {
                    RepositoryProvider.orderRepo.createOrder(order)
                    _message.value = "Заказ создан"
                } else {
                    RepositoryProvider.orderRepo.updateOrder(order)
                    _message.value = "Заказ обновлён"
                }
                // Обновляем список заказов
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
                Timber.e(e, "saveOrder error")
            }
        }
    }

    /**
     * Удаляет заказ (только если у него нет позиций? По ТЗ удаление возможно только без позиций).
     */
    fun deleteOrder(order: Order) {
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }
        viewModelScope.launch {
            if (!canEditOrder(authUser, order)) {
                _message.value = "Недостаточно прав"; return@launch
            }
            // Проверяем, есть ли позиции
            val items = try {
                RepositoryProvider.orderItemRepo.getOrderItemsByOrderId(order.id)
            } catch (e: Exception) {
                emptyList()
            }
            if (items.isNotEmpty()) {
                _message.value = "Нельзя удалить заказ с позициями"
                return@launch
            }
            try {
                RepositoryProvider.orderRepo.deleteOrder(order)
                _message.value = "Заказ удалён"
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Загружает позиции заказа по id.
     */
    fun loadOrderItems(orderId: String) {
        viewModelScope.launch {
            try {
                _orderItems.value = RepositoryProvider.orderItemRepo.getOrderItemsByOrderId(orderId)
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки позиций: ${e.message}"
            }
        }
    }

    /**
     * Удаляет позицию заказа и пересчитывает сумму заказа.
     */
    fun deleteOrderItem(item: OrderItem) {
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }
        viewModelScope.launch {
            if (!isAdminOrEngineer) {
                _message.value = "Недостаточно прав"
                return@launch
            }
            try {
                RepositoryProvider.orderItemRepo.deleteOrderItem(item)
                _message.value = "Позиция удалена"
                // Пересчитываем сумму заказа
                updateOrderSum(item.order_id)
                // Обновляем список позиций
                loadOrderItems(item.order_id)
            } catch (e: Exception) {
                _message.value = "Ошибка удаления позиции: ${e.message}"
            }
        }
    }

    /**
     * Пересчитывает сумму заказа на основе позиций и сохраняет её.
     */
    fun updateOrderSum(orderId: String) {
        viewModelScope.launch {
            try {
                val items = RepositoryProvider.orderItemRepo.getOrderItemsByOrderId(orderId)
                val sum = items.sumOf { it.orderitem_cost.toDouble() }.toFloat()
                val order = RepositoryProvider.orderRepo.getOrderById(orderId) ?: return@launch
                val updatedOrder = order.copy(order_sum = sum)
                RepositoryProvider.orderRepo.updateOrder(updatedOrder)
                // Обновляем текущий заказ, если он открыт
                if (_currentOrder.value?.id == orderId) {
                    _currentOrder.value = updatedOrder
                }
                // Обновляем список заказов
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка обновления суммы: ${e.message}"
                Timber.e(e, "updateOrderSum error")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    // ------------------- Вспомогательные методы -------------------

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

    /**
     * Генерирует уникальный номер заказа на основе названия клиента.
     */
    private fun generateOrderNumber(clientTitle: String): String {
        val prefix = generatePrefix(clientTitle)
        val existingNumbers = _orders.value
            .filter { it.order_number.startsWith(prefix) }
            .mapNotNull { it.order_number.removePrefix(prefix).toIntOrNull() }
        val maxNum = existingNumbers.maxOrNull() ?: 0
        val next = maxNum + 1
        return prefix + next.toString().padStart(5, '0')
    }

    /**
     * Формирует префикс из первых трёх символов названия клиента.
     */
    private fun generatePrefix(title: String): String {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return "XXX"
        val first = cleaned.take(3)
        if (first.length == 3) return first.uppercase()
        val last = cleaned.last()
        return (first + last.toString().repeat(3 - first.length)).uppercase()
    }
}