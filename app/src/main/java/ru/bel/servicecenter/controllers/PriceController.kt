package ru.bel.servicecenter.controllers
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
import timber.log.Timber

/**
 * Контроллер для работы с ценами.
 * Реализует логику выбора категории, загрузки услуг и актуальных цен.
 */
class PriceController : ViewModel() {

    // Список категорий
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    // Услуги с актуальной ценой для выбранной категории
    private val _serviceWithPrice = MutableStateFlow<List<ServiceWithPrice>>(emptyList())
    val serviceWithPrice: StateFlow<List<ServiceWithPrice>> = _serviceWithPrice

    // Текущая редактируемая цена
    private val _currentPrice = MutableStateFlow<Price?>(null)
    val currentPrice: StateFlow<Price?> = _currentPrice

    // Текущая услуга, для которой редактируется цена
    private val _currentService = MutableStateFlow<Service?>(null)
    val currentService: StateFlow<Service?> = _currentService

    // Сообщение пользователю
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Загружает все категории.
     */
    fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = RepositoryProvider.categoryRepo.getAllCategories()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки категорий: ${e.message}"
            }
        }
    }

    /**
     * Загружает услуги выбранной категории и их актуальные цены.
     * Актуальная цена – запись с самой поздней датой создания.
     */
    fun loadServicesWithPrices(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _serviceWithPrice.value = emptyList() // сразу очищаем предыдущий список
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Устанавливает услугу и текущую цену для редактирования.
     */
    fun startEditing(service: Service, price: Price?) {
        _currentService.value = service
        _currentPrice.value = price
    }

    /**
     * Создаёт новую цену для услуги.
     * После создания обновляет список актуальных цен.
     */
    fun createPrice(service: Service, cost: Float, isTime: Boolean) {
        viewModelScope.launch {
            try {
                val newPrice = Price(
                    service_id = service.id,
                    service_cost = cost,
                    is_time = isTime
                )
                RepositoryProvider.priceRepo.createPrice(newPrice)
                _message.value = "Цена обновлена"
                // Обновляем актуальные цены для выбранной категории
                if (!service.category_id.isNullOrBlank()) {
                    loadServicesWithPrices(service.category_id)
                }
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения цены: ${e.message}"
            }
        }
    }

    /**
     * Очищает сообщение.
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Вспомогательный класс для хранения услуги и её актуальной цены.
     */
    data class ServiceWithPrice(
        val service: Service,
        val price: Price?   // может быть null, если цена ещё не задана
    )

}