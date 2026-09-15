package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.OrderWithItems
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.ui.reports.ReportPeriod
import ru.bel.servicecenter.utils.MessageBus
import timber.log.Timber
import java.time.YearMonth
import ru.bel.servicecenter.utils.RoleCache

/**
 * ViewModel для всех отчётов.
 * Пока реализован отчёт для роли "user":
 *  - Мои заказы за период
 * Дальше добавим отчёты для engineer/admin.
 */
class ReportsViewModel : ViewModel() {

    // Кто сейчас работает с отчётами
    var currentAuthUser: User? = null

    // ---------- Общие поля ----------

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    // ---------- Отчёт «Мои заказы за период» ----------

    private val _myOrders = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val myOrders: StateFlow<List<OrderWithItems>> = _myOrders

    private val _myOrdersTotal = MutableStateFlow(OrdersTotal(0, 0f))
    val myOrdersTotal: StateFlow<OrdersTotal> = _myOrdersTotal

    fun loadMyOrdersReport() {
        val user = currentAuthUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()
                val all = RepositoryProvider.orderRepo.getOrdersWithItems()
                val filtered = all
                    .filter { it.user_id == user.id }
                    .filter { it.created_at >= from }
                    .sortedByDescending { it.created_at }

                _myOrders.value = filtered
                _myOrdersTotal.value = OrdersTotal(
                    count = filtered.size,
                    sum = filtered.sumOf { it.order_sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по заказам")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Итоги по списку заказов. */
    data class OrdersTotal(
        val count: Int,
        val sum: Float
    )

    // ---------- Отчёт «Мои расходы по услугам» ----------

    private val _myServices = MutableStateFlow<List<ServiceRow>>(emptyList())
    val myServices: StateFlow<List<ServiceRow>> = _myServices

    private val _myServicesTotal = MutableStateFlow(ServicesTotal(0, 0, 0f))
    val myServicesTotal: StateFlow<ServicesTotal> = _myServicesTotal

    fun loadMyServicesReport() {
        val user = currentAuthUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()
                val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems()
                val allServices = RepositoryProvider.serviceRepo.getAllServices()
                val serviceNames = allServices.associate { it.id to it.service_name }

                // Собираем все позиции заказов пользователя за период
                val rows = allOrders
                    .filter { it.user_id == user.id }
                    .filter { it.created_at >= from }
                    .flatMap { it.order_items }
                    .groupBy { it.service_id }
                    .map { (serviceId, items) ->
                        ServiceRow(
                            serviceName = serviceNames[serviceId] ?: "Неизвестная услуга",
                            count = items.sumOf { it.orderitem_quantity },
                            sum = items.sumOf { it.orderitem_cost.toDouble() }.toFloat()
                        )
                    }
                    .sortedByDescending { it.sum }

                _myServices.value = rows
                _myServicesTotal.value = ServicesTotal(
                    uniqueServices = rows.size,
                    totalCount = rows.sumOf { it.count },
                    totalSum = rows.sumOf { it.sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по услугам")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Одна строка отчёта по услугам. */
    data class ServiceRow(
        val serviceName: String,
        val count: Int,
        val sum: Float
    )

    /** Итоги по отчёту услуг. */
    data class ServicesTotal(
        val uniqueServices: Int,
        val totalCount: Int,
        val totalSum: Float
    )

    // ---------- Отчёт «Мои расходы по месяцам» ----------

    private val _myMonths = MutableStateFlow<List<MonthRow>>(emptyList())
    val myMonths: StateFlow<List<MonthRow>> = _myMonths

    private val _myMonthsTotal = MutableStateFlow(MonthsTotal(0, 0, 0f))
    val myMonthsTotal: StateFlow<MonthsTotal> = _myMonthsTotal

    fun loadMyMonthlyReport() {
        val user = currentAuthUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()
                val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems()

                // Берём заказы пользователя за период, группируем по месяцу.
                // monthKey — строка "yyyy-MM", например "2024-01".
                val rows = allOrders
                    .filter { it.user_id == user.id }
                    .filter { it.created_at >= from }
                    .filter { it.created_at.length >= 7 }
                    .groupBy { it.created_at.take(7) }
                    .map { (monthKey, ordersInMonth) ->
                        val ym = try {
                            YearMonth.parse(monthKey)
                        } catch (e: Exception) {
                            null
                        }
                        MonthRow(
                            monthKey = monthKey,
                            displayName = ym?.let {
                                ruMonthLabel(it)
                            } ?: monthKey,
                            count = ordersInMonth.size,
                            sum = ordersInMonth.sumOf { it.order_sum.toDouble() }.toFloat()
                        )
                    }
                    .sortedByDescending { it.monthKey }  // новые месяцы сверху

                _myMonths.value = rows
                _myMonthsTotal.value = MonthsTotal(
                    months = rows.size,
                    totalCount = rows.sumOf { it.count },
                    totalSum = rows.sumOf { it.sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по месяцам")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Одна строка отчёта по месяцам. */
    data class MonthRow(
        val monthKey: String,     // "2024-01" — для сортировки
        val displayName: String,  // "Январь 2024" — для отображения
        val count: Int,
        val sum: Float
    )

    /** Итоги по отчёту месяцев. */
    data class MonthsTotal(
        val months: Int,
        val totalCount: Int,
        val totalSum: Float
    )

    /**
     * Возвращает название месяца на русском, например "Январь 2024".
     * Без внешних библиотек — просто массив из 12 названий.
     */
    private fun ruMonthLabel(ym: YearMonth): String {
        val months = listOf(
            "Январь", "Февраль", "Март", "Апрель",
            "Май", "Июнь", "Июль", "Август",
            "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        val name = months.getOrElse(ym.monthValue - 1) { ym.monthValue.toString() }
        return "$name ${ym.year}"
    }

    // ---------- Отчёт «Все заказы за период» (engineer/admin) ----------

    private val _allOrders = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val allOrders: StateFlow<List<OrderWithItems>> = _allOrders

    private val _allOrdersTotal = MutableStateFlow(OrdersTotal(0, 0f))
    val allOrdersTotal: StateFlow<OrdersTotal> = _allOrdersTotal

    fun loadAllOrdersReport() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()
                val all = RepositoryProvider.orderRepo.getOrdersWithItems()
                val filtered = all
                    .filter { it.created_at >= from }
                    .sortedByDescending { it.created_at }

                _allOrders.value = filtered
                _allOrdersTotal.value = OrdersTotal(
                    count = filtered.size,
                    sum = filtered.sumOf { it.order_sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по всем заказам")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ---------- Отчёт «Выручка по услугам» (engineer/admin) ----------

    private val _allServices = MutableStateFlow<List<ServiceRow>>(emptyList())
    val allServices: StateFlow<List<ServiceRow>> = _allServices

    private val _allServicesTotal = MutableStateFlow(ServicesTotal(0, 0, 0f))
    val allServicesTotal: StateFlow<ServicesTotal> = _allServicesTotal

    fun loadAllServicesReport() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()
                val allOrders = RepositoryProvider.orderRepo.getOrdersWithItems()
                val allServices = RepositoryProvider.serviceRepo.getAllServices()
                val serviceNames = allServices.associate { it.id to it.service_name }

                val rows = allOrders
                    .filter { it.created_at >= from }
                    .flatMap { it.order_items }
                    .groupBy { it.service_id }
                    .map { (serviceId, items) ->
                        ServiceRow(
                            serviceName = serviceNames[serviceId] ?: "Неизвестная услуга",
                            count = items.sumOf { it.orderitem_quantity },
                            sum = items.sumOf { it.orderitem_cost.toDouble() }.toFloat()
                        )
                    }
                    .sortedByDescending { it.sum }

                _allServices.value = rows
                _allServicesTotal.value = ServicesTotal(
                    uniqueServices = rows.size,
                    totalCount = rows.sumOf { it.count },
                    totalSum = rows.sumOf { it.sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по всем услугам")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ---------- Отчёт «Заказы по клиентам» (engineer/admin) ----------

    private val _clientRows = MutableStateFlow<List<ClientRow>>(emptyList())
    val clientRows: StateFlow<List<ClientRow>> = _clientRows

    private val _clientRowsTotal = MutableStateFlow(ClientTotal(0, 0, 0f))
    val clientRowsTotal: StateFlow<ClientTotal> = _clientRowsTotal

    fun loadClientsReport() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()

                // Загружаем всё, что нужно для связей
                val orders = RepositoryProvider.orderRepo.getOrdersWithItems()
                    .filter { it.created_at >= from }
                val users = RepositoryProvider.userRepo.getAllUsers()
                val clients = RepositoryProvider.clientRepo.getAllClients()

                // user_id → client_id
                val userToClient = users.associate { it.id to it.client_id }
                // client_id → client_title
                val clientTitles = clients.associate { it.id to it.client_title }

                // Для каждого заказа находим название клиента
                val rows = orders
                    .groupBy { order ->
                        val clientId = userToClient[order.user_id]
                        val title = clientId?.let { clientTitles[it] }
                        title ?: "Без клиента"
                    }
                    .map { (clientTitle, clientOrders) ->
                        ClientRow(
                            clientTitle = clientTitle,
                            count = clientOrders.size,
                            sum = clientOrders.sumOf { it.order_sum.toDouble() }.toFloat()
                        )
                    }
                    .sortedByDescending { it.sum }

                _clientRows.value = rows
                _clientRowsTotal.value = ClientTotal(
                    clients = rows.size,
                    totalCount = rows.sumOf { it.count },
                    totalSum = rows.sumOf { it.sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по клиентам")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Одна строка отчёта по клиентам. */
    data class ClientRow(
        val clientTitle: String,
        val count: Int,
        val sum: Float
    )

    /** Итоги по отчёту клиентов. */
    data class ClientTotal(
        val clients: Int,
        val totalCount: Int,
        val totalSum: Float
    )

    // ---------- Отчёт «Заказы по инженерам» (engineer/admin) ----------

    private val _engineerRows = MutableStateFlow<List<EngineerRow>>(emptyList())
    val engineerRows: StateFlow<List<EngineerRow>> = _engineerRows

    private val _engineerRowsTotal = MutableStateFlow(EngineerTotal(0, 0, 0f))
    val engineerRowsTotal: StateFlow<EngineerTotal> = _engineerRowsTotal

    fun loadEngineersReport() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()

                val orders = RepositoryProvider.orderRepo.getOrdersWithItems()
                    .filter { it.created_at >= from }
                val users = RepositoryProvider.userRepo.getAllUsers()

                // Оставляем только сотрудников сервисного центра:
                // роль "admin" или "engineer". Обычные пользователи (role "user")
                // в этот отчёт не попадают.
                val staffUsers = users.filter { user ->
                    val role = RoleCache.get(user.role_id)
                    role == "admin" || role == "engineer"
                }
                val staffIds = staffUsers.map { it.id }.toSet()
                val userNames = staffUsers.associate { it.id to it.user_name }

                // Собираем все позиции всех заказов за период,
                // у которых исполнитель — сотрудник сервисного центра.
                // order_items.user_id = кто оказал услугу (инженер).
                // orders.user_id      = кто создал заказ (автор).
                // Нам нужен именно исполнитель.
                val staffItems = orders
                    .flatMap { it.order_items }
                    .filter { it.user_id in staffIds }

                // Группируем по исполнителю
                val rows = staffItems
                    .groupBy { it.user_id }
                    .map { (userId, items) ->
                        EngineerRow(
                            userName = userNames[userId] ?: "Неизвестный",
                            count = items.sumOf { it.orderitem_quantity },
                            sum = items.sumOf { it.orderitem_cost.toDouble() }.toFloat()
                        )
                    }
                    .sortedByDescending { it.sum }

                _engineerRows.value = rows
                _engineerRowsTotal.value = EngineerTotal(
                    engineers = rows.size,
                    totalCount = rows.sumOf { it.count },
                    totalSum = rows.sumOf { it.sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по инженерам")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Одна строка отчёта по инженерам. */
    data class EngineerRow(
        val userName: String,
        val count: Int,
        val sum: Float
    )

    /** Итоги по отчёту инженеров. */
    data class EngineerTotal(
        val engineers: Int,
        val totalCount: Int,
        val totalSum: Float
    )

    // ---------- Отчёт «Выручка по категориям» (engineer/admin) ----------

    private val _categoryRows = MutableStateFlow<List<CategoryRow>>(emptyList())
    val categoryRows: StateFlow<List<CategoryRow>> = _categoryRows

    private val _categoryRowsTotal = MutableStateFlow(CategoryTotal(0, 0, 0f))
    val categoryRowsTotal: StateFlow<CategoryTotal> = _categoryRowsTotal

    fun loadCategoriesReport() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val from = _selectedPeriod.value.startDate()

                // Загружаем всё, что нужно для связей
                val orders = RepositoryProvider.orderRepo.getOrdersWithItems()
                    .filter { it.created_at >= from }
                val services = RepositoryProvider.serviceRepo.getAllServices()
                val categories = RepositoryProvider.categoryRepo.getAllCategories()

                // service_id → category_id
                val serviceToCategory = services.associate { it.id to it.category_id }
                // category_id → category_name
                val categoryNames = categories.associate { it.id to it.category_name }

                // Собираем все позиции всех заказов за период
                val rows = orders
                    .flatMap { it.order_items }
                    .groupBy { item ->
                        // Определяем название категории для позиции
                        val categoryId = serviceToCategory[item.service_id]
                        categoryId?.let { categoryNames[it] } ?: "Без категории"
                    }
                    .map { (categoryName, items) ->
                        CategoryRow(
                            categoryName = categoryName,
                            count = items.sumOf { it.orderitem_quantity },
                            sum = items.sumOf { it.orderitem_cost.toDouble() }.toFloat()
                        )
                    }
                    .sortedByDescending { it.sum }

                _categoryRows.value = rows
                _categoryRowsTotal.value = CategoryTotal(
                    categories = rows.size,
                    totalCount = rows.sumOf { it.count },
                    totalSum = rows.sumOf { it.sum.toDouble() }.toFloat()
                )
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки отчёта: ${e.message}")
                Timber.e(e, "Ошибка загрузки отчёта по категориям")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Одна строка отчёта по категориям. */
    data class CategoryRow(
        val categoryName: String,
        val count: Int,
        val sum: Float
    )

    /** Итоги по отчёту категорий. */
    data class CategoryTotal(
        val categories: Int,
        val totalCount: Int,
        val totalSum: Float
    )
}