package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

/**
 * Контроллер для управления клиентами.
 */
class ClientController : ViewModel() {

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients

    private val _currentClient = MutableStateFlow(
        Client(client_title = "", client_address = "", client_details = "", is_legal = false)
    )
    val currentClient: StateFlow<Client> = _currentClient

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    fun loadClients() {
        viewModelScope.launch {
            try {
                _clients.value = RepositoryProvider.clientRepo.getAllClients()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки клиентов: ${e.message}"
            }
        }
    }
    private suspend fun canModifyClient(user: User, client: Client, isNew: Boolean): Boolean {
        val adminRole = RepositoryProvider.roleRepo.getRoleByName("admin")
        val engRole = RepositoryProvider.roleRepo.getRoleByName("engineer")
        val userRole = RepositoryProvider.roleRepo.getRoleByName("user")
        return when (user.role_id) {
            adminRole?.id -> true
            engRole?.id -> true
            userRole?.id -> isNew && user.client_id == null   // разрешаем создать первого клиента
            else -> false
        }
    }

    private suspend fun canDeleteClient(user: User, client: Client): Boolean {
        val adminRole = RepositoryProvider.roleRepo.getRoleByName("admin")
        val engRole = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == adminRole?.id || user.role_id == engRole?.id
    }

    fun saveClient() {
        val client = _currentClient.value
        val authUser = currentAuthUser

        viewModelScope.launch {
            // Проверка прав: admin и engineer могут сохранять
            if (authUser == null) {
                _message.value = "Не выполнен вход"; return@launch
            }
            if (!isAdminOrEngineer(authUser)) {
                _message.value = "Недостаточно прав для сохранения клиента"; return@launch
            }

            // Валидация
            val errs = mutableMapOf<String, String?>()
            errs["client_title"] = ValidationRules.validateRequired(client.client_title, "Название")
            errs["client_address"] = ValidationRules.validateRequired(client.client_address, "Адрес")
            errs["client_details"] = ValidationRules.validateRequired(client.client_details, "Реквизиты")
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                val isNew = client.id.isEmpty() || _clients.value.none { it.id == client.id }
                if (isNew) {
                    RepositoryProvider.clientRepo.createClient(client)
                    _message.value = "Клиент создан"
                } else {
                    RepositoryProvider.clientRepo.updateClient(client)
                    _message.value = "Клиент обновлён"
                }
                loadClients()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }

    fun deleteClient(client: Client) {
        val authUser = currentAuthUser
        viewModelScope.launch {
            if (authUser == null || !isAdminOrEngineer(authUser)) {
                _message.value = "Недостаточно прав для удаления"; return@launch
            }
            try {
                RepositoryProvider.clientRepo.deleteClient(client)
                loadClients()
                _message.value = "Клиент удалён"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun setEditingClient(client: Client) {
        _currentClient.value = client
        _errors.value = emptyMap()
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

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun isAdminOrEngineer(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == admin?.id || user.role_id == eng?.id
    }

    fun initNewClient(defaultTitle: String = "") {
        _currentClient.value = Client(
            client_title = defaultTitle,
            client_address = "",
            client_details = "",
            is_legal = false
        )
        _errors.value = emptyMap()
    }
}