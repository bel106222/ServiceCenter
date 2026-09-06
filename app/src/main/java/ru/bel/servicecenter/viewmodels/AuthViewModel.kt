package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

/**
 * ViewModel для аутентификации.
 * Хранит текущего авторизованного пользователя и его роль.
 * Выполняет вход, регистрацию и выход.
 */
class AuthViewModel : ViewModel() {

    // Текущий авторизованный пользователь (null – не вошёл)
    private val _loggedUser = MutableStateFlow<User?>(null)
    val loggedUser: StateFlow<User?> = _loggedUser

    // Роль текущего пользователя (admin, engineer, user)
    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    // Сообщение об ошибке (для отображения на экране)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Попытка входа по email и паролю.
     * При успехе сохраняет пользователя и его роль (из глобального кэша).
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val user = RepositoryProvider.userRepo.getUserByEmail(email)
                if (user != null && BCrypt.checkpw(password, user.user_password)) {
                    // Роль берём из глобального кэша, который уже загружен при старте
                    val role = RoleCache.get(user.role_id)
                    _userRole.value = role
                    _loggedUser.value = user
                    _error.value = null
                    LoggerService.log("Успешный вход: ${user.user_email}, роль: $role")
                    Timber.i("Успешный вход: ${user.user_email}, роль: $role")
                } else {
                    _error.value = "Неверный email или пароль"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка входа: ${e.message}"
                Timber.e(e, "Ошибка входа")
            }
        }
    }

    /**
     * Регистрация нового пользователя.
     * Создаёт запись с ролью "user" и автоматически выполняет вход.
     */
    fun register(name: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            try {
                // Проверка уникальности email
                val existing = RepositoryProvider.userRepo.getUserByEmail(email)
                if (existing != null) {
                    _error.value = "Пользователь с таким email уже существует"
                    return@launch
                }

                // Получаем роль "user"
                val userRole = RepositoryProvider.roleRepo.getRoleByName("user")
                    ?: throw IllegalStateException("Роль 'user' не найдена в БД")

                // Хешируем пароль
                val hashed = BCrypt.hashpw(password, BCrypt.gensalt())

                // Создаём нового пользователя
                val newUser = User(
                    user_name = name,
                    user_email = email,
                    user_phone = phone,
                    user_password = hashed,
                    role_id = userRole.id,
                    client_id = null
                )
                RepositoryProvider.userRepo.createUser(newUser)

                // Автоматический вход
                _loggedUser.value = newUser
                _userRole.value = "user"
                _error.value = null
                LoggerService.log("Зарегистрирован и выполнен вход: ${newUser.user_email}")
                Timber.i("Зарегистрирован и выполнен вход: ${newUser.user_email}")
            } catch (e: Exception) {
                _error.value = "Ошибка регистрации: ${e.message}"
                Timber.e(e, "Ошибка регистрации")
            }
        }
    }

    /**
     * Выход из системы.
     */
    fun logout() {
        _loggedUser.value = null
        _userRole.value = null
        LoggerService.log("Пользователь вышел из системы")
        Timber.i("Пользователь вышел из системы")
    }

    /**
     * Обновляет данные текущего пользователя (например, после привязки клиента).
     */
    fun updateLoggedUser(user: User) {
        _loggedUser.value = user
    }
}