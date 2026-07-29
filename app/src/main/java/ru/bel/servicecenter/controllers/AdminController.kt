package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.factory.DataFactory
import timber.log.Timber

class AdminController : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    fun clearAndReseedDatabase(adminPassword: String) {
        viewModelScope.launch {
            val authUser = currentAuthUser
            if (authUser == null) {
                _message.value = "Не выполнен вход"; return@launch
            }
            if (!isAdmin(authUser)) {
                _message.value = "Требуется роль администратора"; return@launch
            }
            try {
                DataFactory().seedData(adminPassword)
                _message.value = "База данных очищена и пересоздана"
                Timber.w("БД очищена и пересоздана администратором")
            } catch (e: Exception) {
                _message.value = "Ошибка очистки БД: ${e.message}"
            }
        }
    }

    private suspend fun isAdmin(user: User): Boolean {
        val adminRole = RepositoryProvider.roleRepo.getRoleByName("admin")
        return user.role_id == adminRole?.id
    }
}