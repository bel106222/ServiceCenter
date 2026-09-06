package ru.bel.servicecenter.viewmodels

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
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber
import java.util.UUID
import java.time.LocalDateTime

/**
 * ViewModel для работы с заказами и их позициями.
 * Использует единый запрос для получения заказов с позициями и услугами,
 * что минимизирует сетевые обращения и ускоряет работу интерфейса.
 */
class OrderViewModel : ViewModel() {

    // Все заказы с позициями (основной список)
    private val _ordersWithItems = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val ordersWithItems: StateFlow<List<OrderWithItems>> = _ordersWithItems

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
    var currentUserRole: String? = null

    private var isNewOrder: Boolean = false

    var isAdminOrEngineer: Boolean = false
        private set

    /**
     * Загружает заказы с позициями одним запросом.
     * Роль берётся из currentUserRole (уже в памяти).
     */
    suspend fun loadOrders() {
        val authUser = currentAuthUser ?: return
        val role = currentUserRole ?: "user"
        val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems()
        _ordersWithItems.value = when (role) {
            "admin", "engineer" -> allOrders
            "user" -> allOrders.filter { it.user_id == authUser.id }
            else -> emptyList()
        }
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

    /**
     * Обновляет отдельное поле текущего заказа.
     */
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

            try {
                if (isNewOrder) {
                    RepositoryProvider.orderRepo.createOrder(order)
                    _message.value = "Заказ создан"
                    LoggerService.log("Заказ создан: ${order.order_number}")
                } else {
                    RepositoryProvider.orderRepo.updateOrder(order)
                    _message.value = "Заказ обновлён"
                    LoggerService.log("Заказ обновлён: ${order.order_number}")
                }
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
                Timber.e(e, "Ошибка сохранения заказа")
            }
        }
    }

    /**
     * Удаляет заказ, если у него нет позиций.
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
                LoggerService.log("Заказ удалён: ${order.order_number}")
                loadOrders()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
                Timber.e(e, "Ошибка удаления заказа")
            }
        }
    }

    /**
     * Удаляет позицию заказа и пересчитывает сумму.
     * Работает с локальным состоянием для мгновенного обновления UI.
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

            // Оптимистичное удаление из текущего состояния
            val newItems = current.order_items.filterNot { it.id == item.id }
            val newSum = newItems.sumOf { it.orderitem_cost.toDouble() }.toFloat()
            _currentOrderWithItems.value = current.copy(order_items = newItems, order_sum = newSum)
            _ordersWithItems.value = _ordersWithItems.value.map {
                if (it.id == current.id) it.copy(order_items = newItems, order_sum = newSum) else it
            }

            try {
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
                LoggerService.log("Позиция удалена из заказа: ${current.order_number}")
            } catch (e: Exception) {
                // Откатываем изменения
                _currentOrderWithItems.value = current
                _ordersWithItems.value = _ordersWithItems.value.map {
                    if (it.id == current.id) it.copy(order_items = current.order_items, order_sum = current.order_sum) else it
                }
                _message.value = "Ошибка удаления позиции: ${e.message}"
                Timber.e(e, "Ошибка удаления позиции")
            }
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
        _ordersWithItems.value = _ordersWithItems.value.map {
            if (it.id == orderId) it.copy(order_sum = sum) else it
        }
        _currentOrderWithItems.value = _currentOrderWithItems.value?.let {
            if (it.id == orderId) it.copy(order_sum = sum) else it
        }
    }

    /**
     * Очищает сообщение.
     */
    fun clearMessage() {
        _message.value = null
    }

    private suspend fun canEditOrder(authUser: User, order: OrderWithItems): Boolean {
        val role = RoleCache.get(authUser.role_id) ?: return false
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
        val existingNumbers = _ordersWithItems.value
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