package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Order
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.models.UserName
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService
import java.util.UUID
import timber.log.Timber
import java.time.LocalDateTime

class OrderController : ViewModel() {

    private val _ordersWithItems = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val ordersWithItems: StateFlow<List<OrderWithItems>> = _ordersWithItems

    private val _currentOrderWithItems = MutableStateFlow<OrderWithItems?>(null)
    val currentOrderWithItems: StateFlow<OrderWithItems?> = _currentOrderWithItems

    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null
    var currentUserRole: String? = null   // роль берётся из AuthController

    private var isNewOrder: Boolean = false

    var isAdminOrEngineer: Boolean = false
        private set

    /**
     * Загружает список заказов с позициями одним запросом.
     * Роль берётся из currentUserRole (уже в памяти).
     */
    suspend fun loadOrders() {
        val authUser = currentAuthUser ?: return
        val role = currentUserRole ?: "user"
        val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems()
        val filtered = when (role) {
            "admin", "engineer" -> allOrders
            "user" -> allOrders.filter { it.user_id == authUser.id }
            else -> emptyList()
        }

        // Сортировка:
        // 1. Незавершённые (is_completed=false) — по возрастанию created_at (старые сверху)
        // 2. Завершённые (is_completed=true) — по убыванию created_at (свежие сверху)
        val sorted = filtered.sortedWith(
            compareByDescending<OrderWithItems> { it.is_completed }          // незавершённые первыми
                .thenBy { if (!it.is_completed) it.created_at else "" }     // для незавершённых — по возрастанию
                .thenByDescending { if (it.is_completed) it.created_at else "" } // для завершённых — по убыванию
        )
        _ordersWithItems.value = sorted
        isAdminOrEngineer = role == "admin" || role == "engineer"
    }

    /**
     * Перезагружает список заказов (например, после сохранения позиции).
     */
    suspend fun refreshOrders() {
        loadOrders()
        val currentId = _currentOrderWithItems.value?.id
        if (currentId != null) {
            _currentOrderWithItems.value = _ordersWithItems.value.find { it.id == currentId }
        }
    }

    /**
     * Подготавливает создание нового заказа.
     * Для генерации номера загружает клиентов (единственный дополнительный запрос).
     */
    fun prepareForNewOrder() {
        val user = currentAuthUser ?: return
        viewModelScope.launch {
            val clients = RepositoryProvider.clientRepo.getAllClients()
            val clientTitle = clients.find { it.id == user.client_id }?.client_title ?: "XXX"
            val orderNumber = generateOrderNumber(clientTitle)

            _currentOrderWithItems.value = OrderWithItems(
                id = UUID.randomUUID().toString(),
                order_number = orderNumber,
                order_description = "",
                user_id = user.id,
                order_sum = 0f,
                is_completed = false,
                is_time = false,
                created_at = LocalDateTime.now().toString(),
                order_items = emptyList(),
                users = UserName(user.user_name)
            )
            _authorName.value = user.user_name
            isNewOrder = true
            _errors.value = emptyMap()
            _message.value = null
        }
    }

    /**
     * Подготавливает редактирование существующего заказа.
     * Все данные уже есть в объекте, дополнительных запросов нет.
     */
    fun prepareForEdit(orderWithItems: OrderWithItems) {
        _currentOrderWithItems.value = orderWithItems
        isNewOrder = false
        _authorName.value = orderWithItems.users?.user_name ?: "Неизвестный"
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

            LoggerService.log("Сохраняем заказ: id=${order.id}, номер=${order.order_number}, описание=${order.order_description}")
            try {
                if (isNewOrder) {
                    RepositoryProvider.orderRepo.createOrder(order)
                    LoggerService.log("Заказ создан успешно")
                    _message.value = "Заказ создан"
                    loadOrders()
                } else {
                    RepositoryProvider.orderRepo.updateOrder(order)
                    LoggerService.log("Заказ обновлён успешно")
                    _message.value = "Заказ обновлён"
                    _ordersWithItems.value = _ordersWithItems.value.map {
                        if (it.id == order.id) {
                            it.copy(
                                order_description = order.order_description,
                                order_sum = order.order_sum,
                                is_completed = order.is_completed,
                                is_time = order.is_time
                            )
                        } else it
                    }
                }
            } catch (e: Exception) {
                LoggerService.log("Ошибка сохранения заказа: ${e.message}")
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

            val current = _currentOrderWithItems.value ?: return@launch
            if (current.id != item.order_id) return@launch

            // Вычисляем новые позиции и сумму после удаления
            val newItems = current.order_items.filterNot { it.id == item.id }
            val newSum = newItems.sumOf { it.orderitem_cost.toDouble() }.toFloat()

            // Оптимистично обновляем UI
            _currentOrderWithItems.value = current.copy(order_items = newItems, order_sum = newSum)
            _ordersWithItems.value = _ordersWithItems.value.map {
                if (it.id == current.id) it.copy(order_items = newItems, order_sum = newSum) else it
            }

            try {
                // Удаляем позицию из БД
                RepositoryProvider.orderItemRepo.deleteOrderItem(item)

                // Обновляем заказ в БД с новой суммой
                val updatedOrder = Order(
                    id = current.id,
                    order_number = current.order_number,
                    order_description = current.order_description,
                    user_id = current.user_id,
                    order_sum = newSum,
                    is_completed = current.is_completed,
                    is_time = current.is_time,
                    created_at = current.created_at,
                    deleted_at = current.deleted_at
                )
                RepositoryProvider.orderRepo.updateOrder(updatedOrder)

                _message.value = "Позиция удалена"
            } catch (e: Exception) {
                // Откатываем изменения в случае ошибки
                _currentOrderWithItems.value = current
                _ordersWithItems.value = _ordersWithItems.value.map {
                    if (it.id == current.id) it.copy(order_items = current.order_items, order_sum = current.order_sum) else it
                }
                _message.value = "Ошибка удаления позиции: ${e.message}"
                Timber.e(e, "deleteOrderItem error")
            }
        }
    }

    /**
     * Локально обновляет позиции текущего заказа и сумму.
     * Используется после сохранения или удаления позиции, чтобы экран обновился мгновенно.
     */
    fun updateCurrentOrderLocally(orderId: String, newItems: List<OrderItem>) {
        val current = _currentOrderWithItems.value ?: return
        if (current.id == orderId) {
            val newSum = newItems.sumOf { it.orderitem_cost.toDouble() }.toFloat()
            _currentOrderWithItems.value = current.copy(order_items = newItems, order_sum = newSum)
        }
        _ordersWithItems.value = _ordersWithItems.value.map {
            if (it.id == orderId) it.copy(order_items = newItems, order_sum = newItems.sumOf { item -> item.orderitem_cost.toDouble() }.toFloat()) else it
        }
    }

    /**
     * Пересчитывает сумму заказа на основе позиций и сохраняет её.
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
        // Обновляем локальные данные
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

    private suspend fun canEditOrder(authUser: User, order: OrderWithItems): Boolean {
        return when (currentUserRole) {
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