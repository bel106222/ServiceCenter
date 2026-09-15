package ru.bel.servicecenter.viewmodels

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
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.MessageBus
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

class ServiceViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    private val _currentService = MutableStateFlow<Service?>(null)
    val currentService: StateFlow<Service?> = _currentService

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

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

    fun loadServices(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _services.value = emptyList()
            try {
                _services.value = RepositoryProvider.serviceRepo.getServicesByCategoryId(categoryId)
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки услуг: ${e.message}")
                Timber.e(e, "Ошибка загрузки услуг")
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
        _operationCompleted.value = false
    }

    fun saveService() {
        val service = _currentService.value ?: return
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }

        viewModelScope.launch {
            if (!canModify(authUser)) {
                MessageBus.show("Недостаточно прав")
                return@launch
            }

            val errs = mutableMapOf<String, String?>()
            errs["service_name"] = ValidationRules.validateRequired(service.service_name, "Название")
            errs["category_id"] = if (service.category_id.isBlank()) "Выберите категорию" else null
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                if (service.id.isEmpty() || _services.value.none { it.id == service.id }) {
                    RepositoryProvider.serviceRepo.createService(service)
                    MessageBus.show("Услуга создана")
                    LoggerService.log("Услуга создана: ${service.service_name}")
                } else {
                    RepositoryProvider.serviceRepo.updateService(service)
                    MessageBus.show("Услуга обновлена")
                    LoggerService.log("Услуга обновлена: ${service.service_name}")
                }
                _operationCompleted.value = true
                loadServices(service.category_id)
            } catch (e: Exception) {
                MessageBus.show("Ошибка сохранения: ${e.message}")
                Timber.e(e, "Ошибка сохранения услуги")
            }
        }
    }

    fun deleteService(service: Service) {
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
                RepositoryProvider.serviceRepo.deleteService(service)
                MessageBus.show("Услуга удалена")
                LoggerService.log("Услуга удалена: ${service.service_name}")
                loadServices(service.category_id)
            } catch (e: Exception) {
                MessageBus.show("Ошибка удаления: ${e.message}")
                Timber.e(e, "Ошибка удаления услуги")
            }
        }
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

    fun resetOperationCompleted() {
        _operationCompleted.value = false
    }

    private suspend fun canModify(user: User): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return role == "admin" || role == "engineer"
    }
}