package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import timber.log.Timber

/**
 * Контроллер аутентификации.
 * Хранит текущего авторизованного пользователя и его роль.
 * Выполняет вход, регистрацию и выход.
 */
class AuthController : ViewModel() {

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
     * При успехе сохраняет пользователя и его роль.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val user = RepositoryProvider.userRepo.getUserByEmail(email)
                if (user != null && BCrypt.checkpw(password, user.user_password)) {
                    // Сначала определяем роль
                    val role = getRoleName(user.role_id)
                    // Затем обновляем состояние
                    _userRole.value = role
                    _loggedUser.value = user
                    _error.value = null
                    Timber.i("Успешный вход: ${user.user_email}, роль: $role")
                } else {
                    _error.value = "Неверный email или пароль"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка входа: ${e.message}"
            }
        }
    }

    /**
     * Регистрация нового пользователя.
     * Создаёт запись с ролью "user", автоматически выполняет вход.
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
        Timber.i("Пользователь вышел из системы")
    }

    fun updateLoggedUser(user: User) {
        _loggedUser.value = user
    }

    /**
     * Вспомогательный метод для определения названия роли по её id.
     * Выполняет несколько запросов к БД, но вызывается только один раз при входе.
     */
    private suspend fun getRoleName(roleId: String): String? {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        val user = RepositoryProvider.roleRepo.getRoleByName("user")
        return when (roleId) {
            admin?.id -> "admin"
            eng?.id -> "engineer"
            user?.id -> "user"
            else -> null
        }
    }
}