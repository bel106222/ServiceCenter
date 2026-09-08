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

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var currentAuthUser: User? = null

    fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = RepositoryProvider.categoryRepo.getAllCategories()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки категорий: ${e.message}"
                Timber.e(e, "Ошибка загрузки категорий")
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
                _message.value = "Ошибка загрузки цен: ${e.message}"
                Timber.e(e, "Ошибка загрузки цен")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startEditing(service: Service, price: Price?) {
        _currentService.value = service
        _currentPrice.value = price
    }

    fun createPrice(service: Service, cost: Float, isTime: Boolean) {
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }
        viewModelScope.launch {
            if (!canModify(authUser)) {
                _message.value = "Недостаточно прав"
                return@launch
            }
            try {
                val newPrice = Price(
                    service_id = service.id,
                    service_cost = cost,
                    is_time = isTime
                )
                RepositoryProvider.priceRepo.createPrice(newPrice)
                _message.value = "Цена обновлена"
                LoggerService.log("Цена обновлена для услуги: ${service.service_name}")
                if (service.category_id.isNotBlank()) {
                    loadServicesWithPrices(service.category_id)
                }
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения цены: ${e.message}"
                Timber.e(e, "Ошибка сохранения цены")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
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