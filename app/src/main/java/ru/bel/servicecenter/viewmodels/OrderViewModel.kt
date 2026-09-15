package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Attachment
import ru.bel.servicecenter.models.DraftAttachment
import ru.bel.servicecenter.models.Order
import ru.bel.servicecenter.models.OrderItem
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.models.UserName
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.MessageBus
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class OrderViewModel : ViewModel() {

    // ---------- Заказы ----------
    private val _ordersWithItems = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val ordersWithItems: StateFlow<List<OrderWithItems>> = _ordersWithItems

    private val _currentOrderWithItems = MutableStateFlow<OrderWithItems?>(null)
    val currentOrderWithItems: StateFlow<OrderWithItems?> = _currentOrderWithItems

    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    // ---------- Вложения (черновик) ----------
    private val _draftAttachments = MutableStateFlow<List<DraftAttachment>>(emptyList())
    val draftAttachments: StateFlow<List<DraftAttachment>> = _draftAttachments

    private val _isLoadingAttachments = MutableStateFlow(false)
    val isLoadingAttachments: StateFlow<Boolean> = _isLoadingAttachments

    // Флаг для закрытия экрана редактирования
    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted: StateFlow<Boolean> = _operationCompleted

    // ---------- Служебные поля ----------
    var currentAuthUser: User? = null
    var currentUserRole: String? = null

    private var isNewOrder: Boolean = false
    private var originalItems: List<OrderItem> = emptyList()

    var isAdminOrEngineer: Boolean = false
        private set

    // ============================================================
    //                     ЗАГРУЗКА ЗАКАЗОВ
    // ============================================================

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
            // Сначала незавершённые (по убыванию даты), затем завершённые
            val sorted = filtered.sortedWith(
                compareBy<OrderWithItems> { it.is_completed }
                    .thenByDescending { it.created_at }
            )
            _ordersWithItems.value = sorted
            _services.value = RepositoryProvider.serviceRepo.getAllServices()
            isAdminOrEngineer = role == "admin" || role == "engineer"
        } catch (e: Exception) {
            MessageBus.show("Ошибка загрузки заказов: ${e.message}")
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

    // ============================================================
    //                 ПОДГОТОВКА НОВОГО ЗАКАЗА
    // ============================================================

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
            _isLoadingAttachments.value = false
            _errors.value = emptyMap()
            _operationCompleted.value = false
        }
    }

    // ============================================================
    //                ПОДГОТОВКА РЕДАКТИРОВАНИЯ
    // ============================================================

    fun prepareForEdit(orderWithItems: OrderWithItems) {
        _currentOrderWithItems.value = orderWithItems
        originalItems = orderWithItems.order_items
        isNewOrder = false
        _authorName.value = orderWithItems.users?.user_name ?: "Неизвестный"
        _errors.value = emptyMap()
        _operationCompleted.value = false

        _draftAttachments.value = emptyList()
        _isLoadingAttachments.value = true
        viewModelScope.launch {
            try {
                val attachments = RepositoryProvider.attachmentRepo.getAttachmentsByOrderId(orderWithItems.id)
                val drafts = attachments.mapNotNull { attachment ->
                    try {
                        val url = RepositoryProvider.yandexDiskRepo.getDownloadUrl(attachment.url)
                        DraftAttachment(
                            id = attachment.id,
                            uri = url,
                            fileName = attachment.filename,
                            isNew = false,
                            fileBytes = null
                        )
                    } catch (e: Exception) {
                        LoggerService.log("ОШИБКА ссылки ${attachment.filename}: ${e.message}")
                        null
                    }
                }
                _draftAttachments.value = drafts
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки вложений: ${e.message}")
                Timber.e(e, "Ошибка загрузки вложений")
            } finally {
                _isLoadingAttachments.value = false
            }
        }
    }

    // ============================================================
    //              РАБОТА С ЧЕРНОВИКОМ ВЛОЖЕНИЙ
    // ============================================================

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

    fun removeDraftAttachment(attachmentId: String) {
        _draftAttachments.value = _draftAttachments.value.filterNot { it.id == attachmentId }
    }

    // ============================================================
    //          РАБОТА С ЧЕРНОВИКОМ ПОЗИЦИЙ ЗАКАЗА
    // ============================================================

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

    // ============================================================
    //                     ПОЛЕ ЗАКАЗА
    // ============================================================

    fun updateField(field: String, value: String) {
        val order = _currentOrderWithItems.value ?: return
        _currentOrderWithItems.value = when (field) {
            "description" -> order.copy(order_description = value)
            "is_completed" -> order.copy(is_completed = value.toBoolean())
            "is_time" -> order.copy(is_time = value.toBoolean())
            else -> order
        }
    }

    // ============================================================
    //                    СОХРАНЕНИЕ ЗАКАЗА
    // ============================================================
    fun saveOrder() {
        val orderWithItems = _currentOrderWithItems.value ?: return
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }

        viewModelScope.launch {
            val errs = mutableMapOf<String, String?>()
            errs["order_description"] = ValidationRules.validateRequired(orderWithItems.order_description, "Описание")
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            val currentItems = orderWithItems.order_items
            val originalIds = originalItems.map { it.id }.toSet()
            val currentIds = currentItems.map { it.id }.toSet()

            try {
                // ========================================================
                // ШАГ 1. Сохраняем заказ в БД
                // ========================================================
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
                } else {
                    RepositoryProvider.orderRepo.updateOrder(order)
                }

                // ========================================================
                // ШАГ 2. Удаляем позиции, которых больше нет в черновике
                // ========================================================
                originalItems.forEach { original ->
                    if (original.id !in currentIds) {
                        RepositoryProvider.orderItemRepo.deleteOrderItem(original)
                    }
                }

                // ========================================================
                // ШАГ 3. Создаём/обновляем позиции
                // ========================================================
                currentItems.forEach { item ->
                    if (item.id !in originalIds) {
                        RepositoryProvider.orderItemRepo.createOrderItem(item)
                    } else {
                        RepositoryProvider.orderItemRepo.updateOrderItem(item)
                    }
                }

                // ========================================================
                // ШАГ 4. Вложения: удаляем те, что убрал пользователь
                // ========================================================
                val currentAttachments = _draftAttachments.value
                val originalAttachments = RepositoryProvider.attachmentRepo.getAttachmentsByOrderId(order.id)
                val currentAttachmentIds = currentAttachments.map { it.id }.toSet()

                originalAttachments.forEach { original ->
                    if (original.id !in currentAttachmentIds) {
                        try {
                            RepositoryProvider.yandexDiskRepo.deleteFile(original.url)
                        } catch (e: Exception) {
                            LoggerService.log("Ошибка удаления с Яндекс.Диска: ${e.message}")
                        }
                        RepositoryProvider.attachmentRepo.deleteAttachment(original)
                    }
                }

                // ========================================================
                // ШАГ 5. Вложения: загружаем новые
                // ========================================================
                for (draft in currentAttachments) {
                    if (draft.isNew) {
                        val fileBytes = draft.fileBytes
                            ?: throw Exception("Отсутствуют данные изображения для ${draft.fileName}")
                        val uniqueName = "${order.id}_${draft.fileName}"
                        val diskPath = RepositoryProvider.yandexDiskRepo.uploadFile(
                            uniqueName = uniqueName,
                            fileBytes = fileBytes
                        )
                        val attachment = Attachment(
                            order_id = order.id,
                            filename = draft.fileName,
                            url = diskPath
                        )
                        RepositoryProvider.attachmentRepo.createAttachment(attachment)
                    }
                }

                // ========================================================
                // ШАГ 6. Всё записано в БД. Сигналим успех.
                //         Сначала показываем сообщение, затем помечаем операцию
                //         завершённой — экран начнёт закрываться сразу.
                // ========================================================
                MessageBus.show(if (isNewOrder) "Заказ создан" else "Заказ обновлён")
                LoggerService.log(
                    if (isNewOrder) "Заказ создан: ${order.order_number}"
                    else "Заказ обновлён: ${order.order_number}"
                )
                _operationCompleted.value = true

                // ========================================================
                // ШАГ 7. Обновляем кэш в фоне.
                //         Этот блок выполняется ПОСЛЕ сигнала об успехе.
                //         Если он упадёт — сохранение всё равно считается успешным,
                //         потому что заказ уже в БД. Поэтому свой try/catch.
                // ========================================================
                try {
                    loadOrders()
                    val updated = _ordersWithItems.value.find { it.id == order.id }
                    if (updated != null) {
                        _currentOrderWithItems.value = updated
                        originalItems = updated.order_items
                    }

                    val freshAttachments = RepositoryProvider.attachmentRepo.getAttachmentsByOrderId(order.id)
                    _draftAttachments.value = freshAttachments.mapNotNull { attachment ->
                        try {
                            val url = RepositoryProvider.yandexDiskRepo.getDownloadUrl(attachment.url)
                            DraftAttachment(
                                id = attachment.id,
                                uri = url,
                                fileName = attachment.filename,
                                isNew = false,
                                fileBytes = null
                            )
                        } catch (e: Exception) {
                            LoggerService.log("Ошибка перезагрузки вложения: ${e.message}")
                            null
                        }
                    }
                } catch (refreshEx: Exception) {
                    // Кэш не обновился — не критично, заказ уже сохранён.
                    Timber.w(refreshEx, "Ошибка обновления кэша после сохранения заказа")
                }

            } catch (e: Exception) {
                MessageBus.show("Ошибка сохранения: ${e.message}")
                Timber.e(e, "Ошибка сохранения заказа")
            }
        }
    }

    // ============================================================
    //                  УДАЛЕНИЕ ЗАКАЗА
    // ============================================================

    fun deleteOrder(orderWithItems: OrderWithItems) {
        val authUser = currentAuthUser ?: run { MessageBus.show("Не выполнен вход"); return }
        viewModelScope.launch {
            if (!canEditOrder(authUser, orderWithItems)) {
                MessageBus.show("Недостаточно прав")
                return@launch
            }
            if (orderWithItems.order_items.isNotEmpty()) {
                MessageBus.show("Нельзя удалить заказ с позициями")
                return@launch
            }
            try {
                val attachments = RepositoryProvider.attachmentRepo.getAttachmentsByOrderId(orderWithItems.id)
                attachments.forEach { att ->
                    try {
                        RepositoryProvider.yandexDiskRepo.deleteFile(att.url)
                    } catch (e: Exception) {
                        LoggerService.log("Ошибка удаления файла: ${e.message}")
                    }
                    RepositoryProvider.attachmentRepo.deleteAttachment(att)
                }

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
                MessageBus.show("Заказ удалён")
                LoggerService.log("Заказ удалён: ${order.order_number}")
                loadOrders()
            } catch (e: Exception) {
                MessageBus.show("Ошибка удаления: ${e.message}")
                Timber.e(e, "Ошибка удаления заказа")
            }
        }
    }

    // ============================================================
    //                  ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    fun resetOperationCompleted() {
        _operationCompleted.value = false
    }

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
}