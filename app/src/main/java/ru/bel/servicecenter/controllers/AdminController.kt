package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.factory.DataFactory
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import timber.log.Timber

/**
 * Контроллер административных функций.
 * Теперь для полной очистки и пересоздания БД используется
 * createInitialStructure() + setAdminPassword() вместо seedData().
 */
class AdminController : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    /**
     * Полная очистка (hard delete) и повторное создание структуры с установкой пароля.
     * Доступно только администратору.
     */
    fun clearAndReseedDatabase(adminPassword: String) {
        viewModelScope.launch {
            val authUser = currentAuthUser
            if (authUser == null) {
                _message.value = "Не выполнен вход"
                return@launch
            }
            if (!isAdmin(authUser)) {
                _message.value = "Требуется роль администратора"
                return@launch
            }
            try {
                // 1. Создаём структуру (роли, админ с пустым паролем, клиент, услуги, цены)
                val factory = DataFactory()
                val adminId = factory.createInitialStructure()

                // 2. Устанавливаем пароль администратора
                factory.setAdminPassword(adminId, adminPassword)

                _message.value = "База данных пересоздана"
                Timber.w("БД очищена и пересоздана администратором")
            } catch (e: Exception) {
                _message.value = "Ошибка очистки БД: ${e.message}"
                Timber.e(e, "Ошибка очистки БД")
            }
        }
    }

    // Вспомогательная проверка прав
    private suspend fun isAdmin(user: User): Boolean {
        val adminRole = RepositoryProvider.roleRepo.getRoleByName("admin")
        return user.role_id == adminRole?.id
    }
}