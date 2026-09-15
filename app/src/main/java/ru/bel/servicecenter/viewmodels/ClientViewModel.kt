package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.MessageBus
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

class ClientViewModel : ViewModel() {

    // Список клиентов
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients

    // Текущий редактируемый клиент
    private val _currentClient = MutableStateFlow(
        Client(client_title = "", client_address = "", client_details = "", is_legal = false)
    )
    val currentClient: StateFlow<Client> = _currentClient

    // Ошибки валидации по полям
    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    // Флаг загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Флаг "операция успешно завершена" — для закрытия экрана
    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted: StateFlow<Boolean> = _operationCompleted

    var currentAuthUser: User? = null

    fun loadClients() {
        viewModelScope.launch {
            _isLoading.value = true
            _clients.value = emptyList()
            try {
                _clients.value = RepositoryProvider.clientRepo.getAllClients()
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки клиентов: ${e.message}")
                Timber.e(e, "Ошибка загрузки клиентов")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveClient() {
        val client = _currentClient.value
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }

        viewModelScope.launch {
            if (!canModifyClient(authUser, client.id.isEmpty())) {
                MessageBus.show("Недостаточно прав для сохранения клиента")
                return@launch
            }

            val errs = mutableMapOf<String, String?>()
            errs["client_title"] = ValidationRules.validateRequired(client.client_title, "Название")
            errs["client_address"] = ValidationRules.validateRequired(client.client_address, "Адрес")
            errs["client_details"] = ValidationRules.validateRequired(client.client_details, "Реквизиты")
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                if (client.id.isEmpty() || _clients.value.none { it.id == client.id }) {
                    RepositoryProvider.clientRepo.createClient(client)
                    MessageBus.show("Клиент создан")
                    LoggerService.log("Клиент создан: ${client.client_title}")
                } else {
                    RepositoryProvider.clientRepo.updateClient(client)
                    MessageBus.show("Клиент обновлён")
                    LoggerService.log("Клиент обновлён: ${client.client_title}")
                }
                _operationCompleted.value = true
                loadClients()
            } catch (e: Exception) {
                MessageBus.show("Ошибка сохранения: ${e.message}")
                Timber.e(e, "Ошибка сохранения клиента")
            }
        }
    }

    fun deleteClient(client: Client) {
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }
        viewModelScope.launch {
            if (!canDeleteClient(authUser)) {
                MessageBus.show("Недостаточно прав для удаления клиента")
                return@launch
            }
            try {
                RepositoryProvider.clientRepo.deleteClient(client)
                MessageBus.show("Клиент удалён")
                LoggerService.log("Клиент удалён: ${client.client_title}")
                loadClients()
            } catch (e: Exception) {
                MessageBus.show("Ошибка удаления: ${e.message}")
                Timber.e(e, "Ошибка удаления клиента")
            }
        }
    }

    fun setEditingClient(client: Client) {
        _currentClient.value = client
        _errors.value = emptyMap()
        _operationCompleted.value = false
    }

    fun initNewClient(defaultTitle: String = "") {
        _currentClient.value = Client(
            client_title = defaultTitle,
            client_address = "",
            client_details = "",
            is_legal = false
        )
        _errors.value = emptyMap()
        _operationCompleted.value = false
    }

    fun updateField(field: String, value: String) {
        val c = _currentClient.value
        _currentClient.value = when (field) {
            "title" -> c.copy(client_title = value)
            "address" -> c.copy(client_address = value)
            "details" -> c.copy(client_details = value)
            "is_legal" -> c.copy(is_legal = value.toBoolean())
            else -> c
        }
    }

    /**
     * Сброс флага завершения. Вызывается после закрытия экрана.
     */
    fun resetOperationCompleted() {
        _operationCompleted.value = false
    }

    private suspend fun canModifyClient(user: User, isNew: Boolean): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return when (role) {
            "admin", "engineer" -> true
            "user" -> isNew && user.client_id == null
            else -> false
        }
    }

    private suspend fun canDeleteClient(user: User): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return role == "admin" || role == "engineer"
    }
}