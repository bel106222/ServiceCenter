package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.MessageBus
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

class UserViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _currentUser = MutableStateFlow(
        User(user_name = "", user_email = "", user_phone = "", user_password = "", role_id = "")
    )
    val currentUser: StateFlow<User> = _currentUser

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted: StateFlow<Boolean> = _operationCompleted

    var currentAuthUser: User? = null

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _users.value = emptyList()
            try {
                _users.value = RepositoryProvider.userRepo.getAllUsers()
            } catch (e: Exception) {
                MessageBus.show("Ошибка загрузки пользователей: ${e.message}")
                Timber.e(e, "Ошибка загрузки пользователей")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveUser() {
        val user = _currentUser.value
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }

        viewModelScope.launch {
            if (!canModify(authUser, false, user.id)) {
                MessageBus.show("Недостаточно прав")
                return@launch
            }

            val errs = mutableMapOf<String, String?>()
            errs["user_name"] = ValidationRules.validateRequired(user.user_name, "Имя")
            errs["user_email"] = ValidationRules.validateEmail(user.user_email)
            errs["user_phone"] = ValidationRules.validatePhone(user.user_phone)

            if (user.id.isEmpty() || _users.value.none { it.id == user.id }) {
                errs["user_password"] = ValidationRules.validatePassword(user.user_password)
            } else if (user.user_password.isNotEmpty() && user.user_password != "***") {
                errs["user_password"] = ValidationRules.validatePassword(user.user_password)
            }
            if (user.role_id.isBlank()) errs["role_id"] = "Роль не выбрана"

            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                val existing = RepositoryProvider.userRepo.getUserByEmail(user.user_email)
                if (existing != null && existing.id != user.id) {
                    _errors.value = _errors.value.toMutableMap()
                        .apply { put("user_email", "Email уже используется") }
                    return@launch
                }

                var updatedUser = user
                if (user.user_password.isNotEmpty() && user.user_password != "***") {
                    // Пользователь ввёл новый пароль — хешируем и сохраняем.
                    val hashed = BCrypt.hashpw(user.user_password, BCrypt.gensalt())
                    updatedUser = user.copy(user_password = hashed)
                } else if (user.id.isNotEmpty()) {
                    // Пароль не меняется. Нужно найти оригинал, чтобы не отправить в БД "***".
                    val original = _users.value.find { it.id == user.id }
                        ?: RepositoryProvider.userRepo.getAllUsers().find { it.id == user.id }

                    if (original != null) {
                        updatedUser = user.copy(user_password = original.user_password)
                    } else {
                        // Не нашли пользователя — не рискуем затирать пароль.
                        MessageBus.show("Не удалось получить исходные данные профиля")
                        return@launch
                    }
                }

                if (user.id.isEmpty() || _users.value.none { it.id == user.id }) {
                    RepositoryProvider.userRepo.createUser(updatedUser)
                    MessageBus.show("Пользователь создан")
                    LoggerService.log("Пользователь создан: ${updatedUser.user_email}")
                } else {
                    RepositoryProvider.userRepo.updateUser(updatedUser)
                    MessageBus.show("Профиль обновлён")
                    LoggerService.log("Пользователь обновлён: ${updatedUser.user_email}")
                }
                _operationCompleted.value = true
                loadUsers()
            } catch (e: Exception) {
                MessageBus.show("Ошибка сохранения: ${e.message}")
                Timber.e(e, "Ошибка сохранения пользователя")
            }
        }
    }

    fun deleteUser(user: User) {
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }
        viewModelScope.launch {
            if (!canModify(authUser, user.id.isEmpty(), user.id)) {
                MessageBus.show("Недостаточно прав")
                return@launch
            }
            try {
                RepositoryProvider.userRepo.deleteUser(user)
                MessageBus.show("Пользователь удалён")
                LoggerService.log("Пользователь удалён: ${user.user_email}")
                loadUsers()
            } catch (e: Exception) {
                MessageBus.show("Ошибка удаления: ${e.message}")
                Timber.e(e, "Ошибка удаления пользователя")
            }
        }
    }

    fun setEditingUser(user: User) {
        _currentUser.value = user.copy(user_password = "***")
        _errors.value = emptyMap()
        _operationCompleted.value = false
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

    fun resetOperationCompleted() {
        _operationCompleted.value = false
    }

    /**
     * Проверяет, имеет ли право [editor] редактировать пользователя [targetUserId].
     * [isNew] — true, если создаём нового пользователя.
     * [targetUserId] — id редактируемого пользователя (null при создании).
     */
    private suspend fun canModify(
        editor: User,
        isNew: Boolean,
        targetUserId: String? = null
    ): Boolean {
        // Редактируем свой собственный профиль — разрешено всем ролям.
        if (!isNew && targetUserId != null && editor.id == targetUserId) return true

        val role = RoleCache.get(editor.role_id) ?: return false
        return when (role) {
            "admin" -> true
            "engineer" -> isNew || editor.client_id == null
            else -> false
        }
    }
}