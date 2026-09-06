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
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

/**
 * ViewModel для управления клиентами.
 * Обеспечивает загрузку, создание, редактирование и удаление клиентов
 * с учётом прав доступа на основе роли пользователя (из глобального кэша).
 */
class ClientViewModel : ViewModel() {

    // Список всех клиентов
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

    // Сообщение пользователю (успех/ошибка)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Текущий авторизованный пользователь
    var currentAuthUser: User? = null

    /**
     * Загружает список клиентов из репозитория.
     */
    fun loadClients() {
        viewModelScope.launch {
            try {
                _clients.value = RepositoryProvider.clientRepo.getAllClients()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки клиентов: ${e.message}"
                Timber.e(e, "Ошибка загрузки клиентов")
            }
        }
    }

    /**
     * Сохраняет клиента (создание или обновление) после валидации и проверки прав.
     */
    fun saveClient() {
        val client = _currentClient.value
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }

        viewModelScope.launch {
            // Проверка прав на основе роли и типа операции (создание/редактирование)
            if (!canModifyClient(authUser, client.id.isEmpty())) {
                _message.value = "Недостаточно прав для сохранения клиента"
                return@launch
            }

            // Валидация обязательных полей
            val errs = mutableMapOf<String, String?>()
            errs["client_title"] = ValidationRules.validateRequired(client.client_title, "Название")
            errs["client_address"] = ValidationRules.validateRequired(client.client_address, "Адрес")
            errs["client_details"] = ValidationRules.validateRequired(client.client_details, "Реквизиты")
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                if (client.id.isEmpty() || _clients.value.none { it.id == client.id }) {
                    RepositoryProvider.clientRepo.createClient(client)
                    _message.value = "Клиент создан"
                    LoggerService.log("Клиент создан: ${client.client_title}")
                } else {
                    RepositoryProvider.clientRepo.updateClient(client)
                    _message.value = "Клиент обновлён"
                    LoggerService.log("Клиент обновлён: ${client.client_title}")
                }
                // Обновляем список после изменения
                loadClients()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
                Timber.e(e, "Ошибка сохранения клиента")
            }
        }
    }

    /**
     * Удаляет клиента (SoftDelete) с проверкой прав.
     */
    fun deleteClient(client: Client) {
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }
        viewModelScope.launch {
            // Удаление разрешено только администратору и инженеру
            if (!canDeleteClient(authUser)) {
                _message.value = "Недостаточно прав для удаления клиента"
                return@launch
            }
            try {
                RepositoryProvider.clientRepo.deleteClient(client)
                _message.value = "Клиент удалён"
                LoggerService.log("Клиент удалён: ${client.client_title}")
                loadClients()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
                Timber.e(e, "Ошибка удаления клиента")
            }
        }
    }

    /**
     * Устанавливает клиента для редактирования.
     */
    fun setEditingClient(client: Client) {
        _currentClient.value = client
        _errors.value = emptyMap()
    }

    /**
     * Инициализирует нового клиента (например, при нажатии кнопки "+").
     */
    fun initNewClient(defaultTitle: String = "") {
        _currentClient.value = Client(
            client_title = defaultTitle,
            client_address = "",
            client_details = "",
            is_legal = false
        )
        _errors.value = emptyMap()
    }

    /**
     * Обновляет отдельное поле текущего клиента.
     */
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
     * Очищает сообщение (например, после закрытия диалога).
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Проверяет, может ли пользователь создавать/редактировать клиента.
     */
    private suspend fun canModifyClient(user: User, isNew: Boolean): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return when (role) {
            "admin", "engineer" -> true
            // Пользователь с пустым client_id может один раз создать клиента (привязка)
            "user" -> isNew && user.client_id == null
            else -> false
        }
    }

    /**
     * Проверяет, может ли пользователь удалять клиента.
     */
    private suspend fun canDeleteClient(user: User): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return role == "admin" || role == "engineer"
    }
}