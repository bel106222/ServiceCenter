package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.DraftAttachment
import ru.bel.servicecenter.models.Order
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.models.UserName
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber
import java.util.UUID
import java.time.LocalDateTime

class OrderViewModel : ViewModel() {

    private val _ordersWithItems = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val ordersWithItems: StateFlow<List<OrderWithItems>> = _ordersWithItems

    private val _currentOrderWithItems = MutableStateFlow<OrderWithItems?>(null)
    val currentOrderWithItems: StateFlow<OrderWithItems?> = _currentOrderWithItems

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName

    // Черновик вложений (фотографий)
    private val _draftAttachments = MutableStateFlow<List<DraftAttachment>>(emptyList())
    val draftAttachments: StateFlow<List<DraftAttachment>> = _draftAttachments

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var currentAuthUser: User? = null
    var currentUserRole: String? = null

    private var isNewOrder: Boolean = false

    var isAdminOrEngineer: Boolean = false
        private set

    // Храним исходные позиции при редактировании, чтобы определить изменения
    private var originalItems: List<OrderItem> = emptyList()

    suspend fun loadOrders() {
        val authUser = currentAuthUser ?: return
        val role = currentUserRole ?: "user"
        _isLoading.value = true
        _ordersWithItems.value = emptyList()
        try {
            val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems().map { order ->
                order.copy(order_items = order.order_items.filter { it.deleted_at == null })
            }
            val filtered = when (role) {
                "admin", "engineer" -> allOrders
                "user" -> allOrders.filter { it.user_id == authUser.id }
                else -> emptyList()
            }
            // Сортировка: сначала незавершённые (is_completed = false), затем завершённые.
            // Внутри каждой группы – по убыванию created_at (свежие сверху).
            val sorted = filtered.sortedWith(
                compareBy<OrderWithItems> { it.is_completed }          // false раньше true
                    .thenByDescending { it.created_at }                // свежие сверху
            )
            _ordersWithItems.value = sorted
            _services.value = RepositoryProvider.serviceRepo.getAllServices()
            isAdminOrEngineer = role == "admin" || role == "engineer"
        } catch (e: Exception) {
            _message.value = "Ошибка загрузки заказов: ${e.message}"
            Timber.e(e, "Ошибка загрузки заказов")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun refreshOrders() {
        loadOrders()
        val currentId = _currentOrderWithItems.value?.id
        if (currentId != null) {
            _currentOrderWithItems.value = _ordersWithItems.value.find { it.id == currentId }
            originalItems = _currentOrderWithItems.value?.order_items ?: emptyList()
        }
    }

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
            originalItems = emptyList()
            _draftAttachments.value = emptyList()
            _errors.value = emptyMap()
            _message.value = null
        }
    }

    fun prepareForEdit(orderWithItems: OrderWithItems) {
        _currentOrderWithItems.value = orderWithItems
        originalItems = orderWithItems.order_items
        isNewOrder = false
        _authorName.value = orderWithItems.users?.user_name ?: "Неизвестный"
        _errors.value = emptyMap()
        _message.value = null

        // Загружаем приложения из БД и кладём в черновик
        viewModelScope.launch {
            val attachments = RepositoryProvider.attachmentRepo.getAttachmentsByOrderId(orderWithItems.id)
            _draftAttachments.value = attachments.map { attachment ->
                DraftAttachment(
                    id = attachment.id,
                    uri = attachment.url,
                    fileName = attachment.filename,
                    isNew = false,
                    fileBytes = null
                )
            }
        }
    }

    fun updateField(field: String, value: String) {
        val order = _currentOrderWithItems.value ?: return
        _currentOrderWithItems.value = when (field) {
            "description" -> order.copy(order_description = value)
            "is_completed" -> order.copy(is_completed = value.toBoolean())
            else -> order
        }
    }

    // ===== Методы работы с черновиком =====

    fun addItemToDraft(item: OrderItem) {
        val current = _currentOrderWithItems.value ?: return
        val newItems = current.order_items + item
        val newSum = newItems.sumOf { it.orderitem_cost.toDouble() }.toFloat()
        _currentOrderWithItems.value = current.copy(order_items = newItems, order_sum = newSum)
    }

    fun updateItemInDraft(item: OrderItem) {
        val current = _currentOrderWithItems.value ?: return
        val newItems = current.order_items.map { if (it.id == item.id) item else it }
        val newSum = newItems.sumOf { it.orderitem_cost.toDouble() }.toFloat()
        _currentOrderWithItems.value = current.copy(order_items = newItems, order_sum = newSum)
    }

    fun deleteItemFromDraft(itemId: String) {
        val current = _currentOrderWithItems.value ?: return
        val newItems = current.order_items.filterNot { it.id == itemId }
        val newSum = newItems.sumOf { it.orderitem_cost.toDouble() }.toFloat()
        _currentOrderWithItems.value = current.copy(order_items = newItems, order_sum = newSum)
    }

    // Добавить новое вложение в черновик
    fun addDraftAttachment(uri: String, fileName: String, fileBytes: ByteArray?) {
        val newId = UUID.randomUUID().toString()
        val draft = DraftAttachment(
            id = newId,
            uri = uri,
            fileName = fileName,
            isNew = true,
            fileBytes = fileBytes
        )
        _draftAttachments.value = _draftAttachments.value + draft
    }

    // Удалить вложение из черновика
    fun removeDraftAttachment(attachmentId: String) {
        _draftAttachments.value = _draftAttachments.value.filterNot { it.id == attachmentId }
    }

    // Очистить черновик (при отмене или после сохранения)
    fun clearDraftAttachments() {
        _draftAttachments.value = emptyList()
    }

    // ===== Сохранение заказа =====

    fun saveOrder() {
        val orderWithItems = _currentOrderWithItems.value ?: return
        val authUser = currentAuthUser ?: run { _message.value = "Не выполнен вход"; return }

        viewModelScope.launch {
            val errs = mutableMapOf<String, String?>()
            errs["order_description"] = ValidationRules.validateRequired(orderWithItems.order_description, "Описание")
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            val currentItems = orderWithItems.order_items
            val originalIds = originalItems.map { it.id }.toSet()
            val currentIds = currentItems.map { it.id }.toSet()

            try {
                // Удаляем позиции, которые отсутствуют в текущем списке
                originalItems.forEach { original ->
                    if (original.id !in currentIds) {
                        RepositoryProvider.orderItemRepo.deleteOrderItem(original)
                    }
                }

                // Создаём новые или обновляем существующие
                currentItems.forEach { item ->
                    if (item.id !in originalIds) {
                        RepositoryProvider.orderItemRepo.createOrderItem(item)
                    } else {
                        RepositoryProvider.orderItemRepo.updateOrderItem(item)
                    }
                }

                // Сохраняем сам заказ
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

                if (isNewOrder) {
                    RepositoryProvider.orderRepo.createOrder(order)
                    _message.value = "Заказ создан"
                    LoggerService.log("Заказ создан: ${order.order_number}")
                } else {
                    RepositoryProvider.orderRepo.updateOrder(order)
                    _message.value = "Заказ обновлён"
                    LoggerService.log("Заказ обновлён: ${order.order_number}")
                }

                // Обновляем список и текущий заказ из БД
                loadOrders()
                val updated = _ordersWithItems.value.find { it.id == order.id }
                if (updated != null) {
                    _currentOrderWithItems.value = updated
                    originalItems = updated.order_items
                }
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
                Timber.e(e, "Ошибка сохранения заказа")
            }
        }
    }

    // ===== Удаление всего заказа =====

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

    // Вспомогательные методы (canEditOrder, generateOrderNumber, updateOrderSum) оставьте как ранее, но updateOrderSum больше не нужен, так как сумма считается на клиенте и сохраняется через saveOrder.

    private suspend fun canEditOrder(authUser: User, order: OrderWithItems): Boolean {
        val role = RoleCache.get(authUser.role_id) ?: return false
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

    fun clearMessage() {
        _message.value = null
    }
}