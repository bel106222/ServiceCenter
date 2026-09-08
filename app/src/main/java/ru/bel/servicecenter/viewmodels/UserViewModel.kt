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

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var currentAuthUser: User? = null

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _users.value = emptyList()
            try {
                _users.value = RepositoryProvider.userRepo.getAllUsers()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки пользователей: ${e.message}"
                Timber.e(e, "Ошибка загрузки пользователей")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveUser() {
        val user = _currentUser.value
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }

        viewModelScope.launch {
            if (!canModify(authUser, user.id.isEmpty())) {
                _message.value = "Недостаточно прав"
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
                    _errors.value = _errors.value.toMutableMap().apply { put("user_email", "Email уже используется") }
                    return@launch
                }

                var updatedUser = user
                if (user.user_password.isNotEmpty() && user.user_password != "***") {
                    val hashed = BCrypt.hashpw(user.user_password, BCrypt.gensalt())
                    updatedUser = user.copy(user_password = hashed)
                } else if (user.id.isNotEmpty()) {
                    val original = _users.value.find { it.id == user.id }
                    if (original != null) {
                        updatedUser = user.copy(user_password = original.user_password)
                    }
                }

                if (user.id.isEmpty() || _users.value.none { it.id == user.id }) {
                    RepositoryProvider.userRepo.createUser(updatedUser)
                    _message.value = "Пользователь создан"
                    LoggerService.log("Пользователь создан: ${updatedUser.user_email}")
                } else {
                    RepositoryProvider.userRepo.updateUser(updatedUser)
                    _message.value = "Профиль обновлён"
                    LoggerService.log("Пользователь обновлён: ${updatedUser.user_email}")
                }
                loadUsers()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
                Timber.e(e, "Ошибка сохранения пользователя")
            }
        }
    }

    fun deleteUser(user: User) {
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }
        viewModelScope.launch {
            if (!canModify(authUser, false)) {
                _message.value = "Недостаточно прав"
                return@launch
            }
            try {
                RepositoryProvider.userRepo.deleteUser(user)
                _message.value = "Пользователь удалён"
                LoggerService.log("Пользователь удалён: ${user.user_email}")
                loadUsers()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
                Timber.e(e, "Ошибка удаления пользователя")
            }
        }
    }

    fun setEditingUser(user: User) {
        _currentUser.value = user.copy(user_password = "***")
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

    private suspend fun canModify(user: User, isNew: Boolean): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return when (role) {
            "admin" -> true
            "engineer" -> isNew || user.client_id == null
            else -> false
        }
    }
}