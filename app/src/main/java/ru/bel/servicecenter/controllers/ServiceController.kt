package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Service
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class ServiceController : ViewModel() {

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services

    private val _currentService = MutableStateFlow(
        Service(service_name = "", service_description = "", category_id = "", is_fixprice = false)
    )
    val currentService: StateFlow<Service> = _currentService

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    init {
        loadServices()
    }

    fun loadServices() {
        viewModelScope.launch {
            try {
                _services.value = RepositoryProvider.serviceRepo.getAllServices()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки услуг: ${e.message}"
            }
        }
    }

    fun saveService() {
        val srv = _currentService.value
        val authUser = currentAuthUser

        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }

            val errs = mutableMapOf<String, String?>()
            errs["service_name"] = ValidationRules.validateRequired(srv.service_name, "Название")
            errs["category_id"] = if (srv.category_id.isBlank()) "Выберите категорию" else null
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                if (srv.id.isEmpty() || _services.value.none { it.id == srv.id }) {
                    RepositoryProvider.serviceRepo.createService(srv)
                    _message.value = "Услуга создана"
                } else {
                    RepositoryProvider.serviceRepo.updateService(srv)
                    _message.value = "Услуга обновлена"
                }
                loadServices()
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
                loadServices()
                _message.value = "Услуга удалена"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun setEditingService(service: Service) {
        _currentService.value = service
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        val s = _currentService.value
        _currentService.value = when (field) {
            "name" -> s.copy(service_name = value)
            "description" -> s.copy(service_description = value)
            "category_id" -> s.copy(category_id = value)
            "is_fixprice" -> s.copy(is_fixprice = value.toBoolean())
            else -> s
        }
    }

    private suspend fun canModify(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == admin?.id || user.role_id == eng?.id
    }
}