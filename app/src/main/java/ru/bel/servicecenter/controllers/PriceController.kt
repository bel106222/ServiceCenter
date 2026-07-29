package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Price
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class PriceController : ViewModel() {

    private val _prices = MutableStateFlow<List<Price>>(emptyList())
    val prices: StateFlow<List<Price>> = _prices

    private val _currentPrice = MutableStateFlow(
        Price(service_id = "", service_cost = 0f, is_time = false)
    )
    val currentPrice: StateFlow<Price> = _currentPrice

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    init {
        loadPrices()
    }

    fun loadPrices() {
        viewModelScope.launch {
            try {
                _prices.value = RepositoryProvider.priceRepo.getAllPrices()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки цен: ${e.message}"
            }
        }
    }

    fun savePrice() {
        val price = _currentPrice.value
        val authUser = currentAuthUser

        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }

            val errs = mutableMapOf<String, String?>()
            errs["service_id"] = if (price.service_id.isBlank()) "Выберите услугу" else null
            errs["service_cost"] = if (price.service_cost <= 0) "Стоимость должна быть положительной" else null
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                val existing = RepositoryProvider.priceRepo.getPriceByServiceId(price.service_id)
                if (existing != null && existing.id != price.id) {
                    _errors.value = mapOf("service_id" to "Цена для этой услуги уже задана")
                    return@launch
                }
                if (price.id.isEmpty() || _prices.value.none { it.id == price.id }) {
                    RepositoryProvider.priceRepo.createPrice(price)
                    _message.value = "Цена создана"
                } else {
                    RepositoryProvider.priceRepo.updatePrice(price)
                    _message.value = "Цена обновлена"
                }
                loadPrices()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения цены: ${e.message}"
            }
        }
    }

    fun deletePrice(price: Price) {
        val authUser = currentAuthUser
        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }
            try {
                RepositoryProvider.priceRepo.deletePrice(price)
                loadPrices()
                _message.value = "Цена удалена"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun setEditingPrice(price: Price) {
        _currentPrice.value = price
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        val p = _currentPrice.value
        _currentPrice.value = when (field) {
            "service_id" -> p.copy(service_id = value)
            "cost" -> p.copy(service_cost = value.toFloatOrNull() ?: 0f)
            "is_time" -> p.copy(is_time = value.toBoolean())
            else -> p
        }
    }

    private suspend fun canModify(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == admin?.id || user.role_id == eng?.id
    }
}