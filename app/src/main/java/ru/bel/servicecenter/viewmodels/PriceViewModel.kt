package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.Price
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.MessageBus
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

class PriceViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _serviceWithPrice = MutableStateFlow<List<ServiceWithPrice>>(emptyList())
    val serviceWithPrice: StateFlow<List<ServiceWithPrice>> = _serviceWithPrice

    private val _currentService = MutableStateFlow<Service?>(null)
    val currentService: StateFlow<Service?> = _currentService

    private val _currentPrice = MutableStateFlow<Price?>(null)
    val currentPrice: StateFlow<Price?> = _currentPrice

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingCategories = MutableStateFlow(false)
    val isLoadingCategories: StateFlow<Boolean> = _isLoadingCategories

    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted: StateFlow<Boolean> = _operationCompleted

    var currentAuthUser: User? = null

    fun loadCategories() {
        viewModelScope.launch {
            _isLoadingCategories.value = true
            try {
                _categories.value = RepositoryProvider.categoryRepo.getAllCategories()
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки категорий: ${e.message}")
                Timber.e(e, "Ошибка загрузки категорий")
            } finally {
                _isLoadingCategories.value = false
            }
        }
    }

    fun loadServicesWithPrices(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _serviceWithPrice.value = emptyList()
            try {
                val services = RepositoryProvider.serviceRepo.getServicesByCategoryId(categoryId)
                val allPrices = RepositoryProvider.priceRepo.getAllPrices()
                val result = services.map { service ->
                    val pricesForService = allPrices.filter { it.service_id == service.id }
                    val actualPrice = pricesForService.maxByOrNull { it.created_at }
                    ServiceWithPrice(service, actualPrice)
                }
                _serviceWithPrice.value = result
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки цен: ${e.message}")
                Timber.e(e, "Ошибка загрузки цен")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startEditing(service: Service, price: Price?) {
        _currentService.value = service
        _currentPrice.value = price
        _operationCompleted.value = false
    }

    fun createPrice(service: Service, cost: Float, isTime: Boolean) {
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }
        viewModelScope.launch {
            if (!canModify(authUser)) {
                MessageBus.show("Недостаточно прав")
                return@launch
            }
            try {
                val newPrice = Price(
                    service_id = service.id,
                    service_cost = cost,
                    is_time = isTime
                )
                RepositoryProvider.priceRepo.createPrice(newPrice)
                MessageBus.show("Цена обновлена")
                LoggerService.log("Цена обновлена для услуги: ${service.service_name}")
                _operationCompleted.value = true
                if (service.category_id.isNotBlank()) {
                    loadServicesWithPrices(service.category_id)
                }
            } catch (e: Exception) {
                MessageBus.show("Ошибка сохранения цены: ${e.message}")
                Timber.e(e, "Ошибка сохранения цены")
            }
        }
    }

    fun resetOperationCompleted() {
        _operationCompleted.value = false
    }

    data class ServiceWithPrice(
        val service: Service,
        val price: Price?
    )

    private suspend fun canModify(user: User): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return role == "admin" || role == "engineer"
    }
}