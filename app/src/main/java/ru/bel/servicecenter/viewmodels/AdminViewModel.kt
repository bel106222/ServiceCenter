package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.factory.DataFactory
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

/**
 * ViewModel для административных функций.
 * Очистка БД и проверка целостности.
 */
class AdminViewModel : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    /**
     * Полная очистка и повторная инициализация БД.
     * Доступно только администратору.
     */
    fun clearAndReseedDatabase(adminPassword: String) {
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }
        if (RoleCache.get(authUser.role_id) != "admin") {
            _message.value = "Требуется роль администратора"
            return
        }

        viewModelScope.launch {
            try {
                val factory = DataFactory()
                val adminId = factory.createInitialStructure()
                factory.setAdminPassword(adminId, adminPassword)

                _message.value = "База данных очищена и пересоздана"
                LoggerService.log("БД очищена и пересоздана администратором")
            } catch (e: Exception) {
                _message.value = "Ошибка очистки БД: ${e.message}"
                Timber.e(e, "Ошибка очистки БД")
            }
        }
    }

    /**
     * Очищает сообщение.
     */
    fun clearMessage() {
        _message.value = null
    }
}