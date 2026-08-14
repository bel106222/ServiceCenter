package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class ServiceController : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    private val _currentService = MutableStateFlow<Service?>(null)
    val currentService: StateFlow<Service?> = _currentService

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

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
            }
        }
    }

    fun loadServices(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _services.value = emptyList()
            try {
                _services.value = RepositoryProvider.serviceRepo.getServicesByCategoryId(categoryId)
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки услуг: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startEditing(service: Service?) {
        _currentService.value = service ?: Service(
            service_name = "",
            service_description = "",
            category_id = "",
            is_fixprice = false
        )
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        val service = _currentService.value ?: return
        _currentService.value = when (field) {
            "name" -> service.copy(service_name = value)
            "description" -> service.copy(service_description = value)
            "category_id" -> service.copy(category_id = value)
            "is_fixprice" -> service.copy(is_fixprice = value.toBoolean())
            else -> service
        }
    }

    fun saveService() {
        val service = _currentService.value ?: return
        val authUser = currentAuthUser

        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }

            val errs = mutableMapOf<String, String?>()
            errs["service_name"] = ValidationRules.validateRequired(service.service_name, "Название")
            errs["category_id"] = if (service.category_id.isBlank()) "Выберите категорию" else null
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                if (service.id.isEmpty() || _services.value.none { it.id == service.id }) {
                    RepositoryProvider.serviceRepo.createService(service)
                    _message.value = "Услуга создана"
                } else {
                    RepositoryProvider.serviceRepo.updateService(service)
                    _message.value = "Услуга обновлена"
                }
                // Обновляем список услуг
                loadServices(service.category_id)
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }

    fun deleteService(service: Service) {
        val authUser = currentAuthUser
        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }
            try {
                RepositoryProvider.serviceRepo.deleteService(service)
                _message.value = "Услуга удалена"
                loadServices(service.category_id)
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun canModify(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == admin?.id || user.role_id == eng?.id
    }
}