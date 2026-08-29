package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.models.Role
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class UserController : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _currentUser = MutableStateFlow(
        User(user_name = "", user_email = "", user_phone = "", user_password = "", role_id = "")
    )
    val currentUser: StateFlow<User> = _currentUser

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    init {
        loadUsers()
    }

    // Списки для выпадающих полей
    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients

//    init {
//        loadUsers()
//        loadRoles()
//        loadClients()
//    }

    fun loadRoles() {
        viewModelScope.launch {
            try {
                val adminRole = RepositoryProvider.roleRepo.getRoleByName("admin")
                val engRole = RepositoryProvider.roleRepo.getRoleByName("engineer")
                val userRole = RepositoryProvider.roleRepo.getRoleByName("user")
                _roles.value = listOfNotNull(adminRole, engRole, userRole)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки ролей")
            }
        }
    }

    fun loadClients() {
        viewModelScope.launch {
            try {
                _clients.value = RepositoryProvider.clientRepo.getAllClients()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки клиентов")
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _users.value = RepositoryProvider.userRepo.getAllUsers()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки пользователей: ${e.message}"
            }
        }
    }

    fun saveUser() {
        val user = _currentUser.value
        val authUser = currentAuthUser
        val isNew = user.id.isEmpty() || _users.value.none { it.id == user.id }

        viewModelScope.launch {
            // Проверка прав
            if (!isNew) {
                if (authUser == null) {
                    _message.value = "Не выполнен вход"; return@launch
                }
                if (!canEditUser(authUser, user)) {
                    _message.value = "Недостаточно прав для редактирования"; return@launch
                }
            } else {
                if (!canCreateUser(authUser)) {
                    _message.value = "Недостаточно прав для создания"; return@launch
                }
            }

            // Валидация
            val errs = mutableMapOf<String, String?>()
            errs["user_name"] = ValidationRules.validateRequired(user.user_name, "Имя")
            errs["user_email"] = ValidationRules.validateEmail(user.user_email)
            errs["user_phone"] = ValidationRules.validatePhone(user.user_phone)
            if (isNew) {
                errs["user_password"] = ValidationRules.validatePassword(user.user_password)
            } else {
                // Если пароль не менялся (оставлен "***" или пустой), не ругаемся
                if (user.user_password != "***" && user.user_password.isNotEmpty()) {
                    errs["user_password"] = ValidationRules.validatePassword(user.user_password)
                }
            }
            if (user.role_id.isBlank()) errs["role_id"] = "Роль не выбрана"
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            viewModelScope.launch {
                try {
                    // Проверка уникальности email
                    val existing = RepositoryProvider.userRepo.getUserByEmail(user.user_email)
                    if (existing != null && existing.id != user.id) {
                        _errors.value =
                            _errors.value.toMutableMap().apply { put("user_email", "Email уже используется") }
                        return@launch
                    }

                    var updatedUser = user
                    // Хешируем пароль только если он реально изменился (не "***" и не пустой)
                    if (user.user_password != "***" && user.user_password.isNotEmpty()) {
                        val hashed = BCrypt.hashpw(user.user_password, BCrypt.gensalt())
                        updatedUser = user.copy(user_password = hashed)
                    } else if (!isNew) {
                        // Оставляем старый пароль – копируем из исходного пользователя
                        val original = _users.value.find { it.id == user.id }
                        if (original != null) {
                            updatedUser = user.copy(user_password = original.user_password)
                        }
                    }

                    if (isNew) {
                        RepositoryProvider.userRepo.createUser(updatedUser)
                        _message.value = "Пользователь создан"
                    } else {
                        RepositoryProvider.userRepo.updateUser(updatedUser)
                        _message.value = "Профиль обновлён"
                    }
                    loadUsers()
                } catch (e: Exception) {
                    _message.value = "Ошибка сохранения: ${e.message}"
                }
            }
        }
    }

    fun deleteUser(user: User) {
        val authUser = currentAuthUser
        viewModelScope.launch {
            if (authUser == null) { _message.value = "Не выполнен вход"; return@launch }
            if (!canDeleteUser(authUser, user)) { _message.value = "Недостаточно прав"; return@launch }
            try {
                RepositoryProvider.userRepo.deleteUser(user)
                loadUsers()
                _message.value = "Пользователь удалён"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun setEditingUser(user: User) {
        _currentUser.value = user.copy(user_password = "***")   // заглушка, чтобы не показывать хеш
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        val c = _currentUser.value
        _currentUser.value = when (field) {
            "name" -> c.copy(user_name = value)
            "email" -> c.copy(user_email = value)
            "phone" -> c.copy(user_phone = value)
            "password" -> c.copy(user_password = value)
            "role_id" -> c.copy(role_id = value)
            "client_id" -> c.copy(client_id = value)
            else -> c
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun canEditUser(auth: User, target: User): Boolean {
        val roles = mapOf(
            "admin" to RepositoryProvider.roleRepo.getRoleByName("admin")?.id,
            "engineer" to RepositoryProvider.roleRepo.getRoleByName("engineer")?.id,
            "user" to RepositoryProvider.roleRepo.getRoleByName("user")?.id
        )
        return when (auth.role_id) {
            roles["admin"] -> true
            roles["engineer"] -> auth.client_id == target.client_id
            roles["user"] -> auth.id == target.id
            else -> false
        }
    }

    private suspend fun canCreateUser(auth: User?): Boolean {
        if (auth == null) return false
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return auth.role_id == admin?.id || auth.role_id == eng?.id
    }

    private suspend fun canDeleteUser(auth: User, target: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        return auth.role_id == admin?.id || auth.id == target.id
    }
}