package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.Order
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class OrderController : ViewModel() {

    // Все заказы с позициями (основной список)
    private val _ordersWithItems = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val ordersWithItems: StateFlow<List<OrderWithItems>> = _ordersWithItems

    // Список услуг (для отображения названий)
    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    // Список пользователей (для определения автора)
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    // Список клиентов (для генерации номера заказа)
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients

    // Текущий редактируемый заказ с позициями
    private val _currentOrderWithItems = MutableStateFlow<OrderWithItems?>(null)
    val currentOrderWithItems: StateFlow<OrderWithItems?> = _currentOrderWithItems

    // Имя автора заказа
    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName

    // Ошибки валидации
    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    // Сообщение пользователю
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    private var isNewOrder: Boolean = false

    var isAdminOrEngineer: Boolean = false
        private set

    /**
     * Загружает все заказы с позициями, услуги, пользователей и клиентов.
     * Вызывается при входе в раздел "Заказы".
     */
    suspend fun loadOrders() {
        val authUser = currentAuthUser ?: return
        val role = getRoleName(authUser.role_id)
        val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems()
        _ordersWithItems.value = when (role) {
            "admin", "engineer" -> allOrders
            "user" -> allOrders.filter { it.user_id == authUser.id }
            else -> emptyList()
        }
        _services.value = RepositoryProvider.serviceRepo.getAllServices()
        _users.value = RepositoryProvider.userRepo.getAllUsers()
        _clients.value = RepositoryProvider.clientRepo.getAllClients()
        isAdminOrEngineer = role == "admin" || role == "engineer"
    }

    /**
     * Обновляет список заказов после изменения позиции.
     * Используется из OrderItemController.
     */
    suspend fun refreshOrders() {
        loadOrders()
        // Если открыт заказ, обновляем его в currentOrderWithItems
        val currentId = _currentOrderWithItems.value?.id
        if (currentId != null) {
            _currentOrderWithItems.value = _ordersWithItems.value.find { it.id == currentId }
        }
    }

    /**
     * Подготавливает контроллер для создания нового заказа.
     */
    fun prepareForNewOrder() {
        val user = currentAuthUser ?: return
        val clientTitle = _clients.value.find { it.id == user.client_id }?.client_title ?: "XXX"
        val orderNumber = generateOrderNumber(clientTitle)
        _currentOrderWithItems.value = OrderWithItems(
            id = "",
            order_number = orderNumber,
            order_description = "",
            user_id = user.id,
            order_sum = 0f,
            is_completed = false,
            is_time = false,
            created_at = "",
            order_items = emptyList()
        )
        _authorName.value = user.user_name
        isNewOrder = true
        _errors.value = emptyMap()
        _message.value = null
    }

    /**
     * Подготавливает контроллер для редактирования существующего заказа.
     */
    fun prepareForEdit(orderWithItems: OrderWithItems) {
        _currentOrderWithItems.value = orderWithItems
        isNewOrder = false
        _authorName.value = _users.value.find { it.id == orderWithItems.user_id }?.user_name ?: "Неизвестный"
        _errors.value = emptyMap()
        _message.value = null
    }

    fun updateField(field: String, value: String) {
        val order = _currentOrderWithItems.value ?: return
        _currentOrderWithItems.value = when (field) {
            "description" -> order.copy(order_description = value)
            "is_completed" -> order.copy(is_completed = value.toBoolean())
            "is_time" -> order.copy(is_time = value.toBoolean())
            else -> order
        }
    }

    /**
     * Сохраняет текущий заказ (создание или обновление).
     */
    fun saveOrder() {
        val orderWithItems = _currentOrderWithItems.value ?: return
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }

        viewModelScope.launch {
            val errs = mutableMapOf<String, String?>()
            errs["order_description"] = ValidationRules.validateRequired(orderWithItems.order_description, "Описание")
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            // Преобразуем OrderWithItems в обычный Order для отправки
            val order = Order(
                id = orderWithItems.id,
                order_number = orderWithItems.order_number,
                order_description = orderWithItems.order_description,
                user_id = orderWithItems.user_id,
                order_sum = orderWithItems.order_sum,
                is_completed = orderWithItems.is_completed,
                is_time = orderWithItems.is_time,
                created_at = orderWithItems.created_at,
                deleted_at = orderWithItems.deleted_at
            )

            try {
                if (isNewOrder) {
                    RepositoryProvider.orderRepo.createOrder(order)
                    _message.value = "Заказ создан"
                } else {
                    RepositoryProvider.orderRepo.updateOrder(order)
                    _message.value = "Заказ обновлён"
                }
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
                Timber.e(e, "saveOrder error")
            }
        }
    }

    /**
     * Удаляет заказ, только если у него нет позиций.
     */
    fun deleteOrder(orderWithItems: OrderWithItems) {
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }
        viewModelScope.launch {
            if (!canEditOrder(authUser, orderWithItems)) {
                _message.value = "Недостаточно прав"
                return@launch
            }
            if (orderWithItems.order_items.isNotEmpty()) {
                _message.value = "Нельзя удалить заказ с позициями"
                return@launch
            }
            try {
                val order = Order(
                    id = orderWithItems.id,
                    order_number = orderWithItems.order_number,
                    order_description = orderWithItems.order_description,
                    user_id = orderWithItems.user_id,
                    order_sum = orderWithItems.order_sum,
                    is_completed = orderWithItems.is_completed,
                    is_time = orderWithItems.is_time,
                    created_at = orderWithItems.created_at,
                    deleted_at = orderWithItems.deleted_at
                )
                RepositoryProvider.orderRepo.deleteOrder(order)
                _message.value = "Заказ удалён"
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Удаляет позицию заказа и пересчитывает сумму.
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
                // Обновляем список заказов
                loadOrders()
                // Обновляем текущий заказ, если он открыт
                val currentId = _currentOrderWithItems.value?.id
                if (currentId == item.order_id) {
                    _currentOrderWithItems.value = _ordersWithItems.value.find { it.id == item.order_id }
                }
            } catch (e: Exception) {
                _message.value = "Ошибка удаления позиции: ${e.message}"
                Timber.e(e, "deleteOrderItem error")
            }
        }
    }

    /**
     * Пересчитывает сумму заказа на основе позиций и сохраняет её.
     * Используется после добавления/удаления позиции.
     */
    suspend fun updateOrderSum(orderId: String) {
        val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems()
        val orderWithItems = allOrders.find { it.id == orderId } ?: return
        val sum = orderWithItems.order_items.sumOf { it.orderitem_cost.toDouble() }.toFloat()
        val order = Order(
            id = orderWithItems.id,
            order_number = orderWithItems.order_number,
            order_description = orderWithItems.order_description,
            user_id = orderWithItems.user_id,
            order_sum = sum,
            is_completed = orderWithItems.is_completed,
            is_time = orderWithItems.is_time,
            created_at = orderWithItems.created_at,
            deleted_at = orderWithItems.deleted_at
        )
        RepositoryProvider.orderRepo.updateOrder(order)
        // Обновляем список заказов и текущий заказ
        _ordersWithItems.value = _ordersWithItems.value.map {
            if (it.id == orderId) it.copy(order_sum = sum) else it
        }
        _currentOrderWithItems.value = _currentOrderWithItems.value?.let {
            if (it.id == orderId) it.copy(order_sum = sum) else it
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

    private suspend fun canEditOrder(authUser: User, order: OrderWithItems): Boolean {
        val role = getRoleName(authUser.role_id)
        return when (role) {
            "admin", "engineer" -> true
            "user" -> order.user_id == authUser.id
            else -> false
        }
    }

    private fun generateOrderNumber(clientTitle: String): String {
        val prefix = generatePrefix(clientTitle)
        val existingNumbers = _ordersWithItems.value
            .filter { it.order_number.startsWith(prefix) }
            .mapNotNull { it.order_number.removePrefix(prefix).toIntOrNull() }
        val maxNum = existingNumbers.maxOrNull() ?: 0
        val next = maxNum + 1
        return prefix + next.toString().padStart(5, '0')
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