package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.factory.DataFactory
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.MessageBus
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

class AdminViewModel : ViewModel() {

    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted: StateFlow<Boolean> = _operationCompleted

    var currentAuthUser: User? = null

    fun clearAndReseedDatabase(adminPassword: String) {
        val authUser = currentAuthUser ?: run {
            MessageBus.show("Не выполнен вход")
            return
        }
        if (RoleCache.get(authUser.role_id) != "admin") {
            MessageBus.show("Требуется роль администратора")
            return
        }

        viewModelScope.launch {
            try {
                val factory = DataFactory()
                val adminId = factory.createInitialStructure()
                factory.setAdminPassword(adminId, adminPassword)
                MessageBus.show("База данных очищена и пересоздана")
                LoggerService.log("БД очищена и пересоздана администратором")
                _operationCompleted.value = true
            } catch (e: Exception) {
                MessageBus.show("Ошибка очистки БД: ${e.message}")
                Timber.e(e, "Ошибка очистки БД")
            }
        }
    }

    fun resetOperationCompleted() {
        _operationCompleted.value = false
    }
}