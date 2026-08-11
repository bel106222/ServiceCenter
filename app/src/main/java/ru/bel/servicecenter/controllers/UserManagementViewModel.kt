package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Client
import ru.bel.servicecenter.models.Role
import ru.bel.servicecenter.repository.RepositoryProvider
import timber.log.Timber

class UserManagementViewModel : ViewModel() {
    // Контроллер пользователей
    val userController = UserController()

    // Справочники
    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients

    init {
        viewModelScope.launch {
            try {
                val roleNames = listOf("admin", "engineer", "user")
                _roles.value = roleNames.mapNotNull { RepositoryProvider.roleRepo.getRoleByName(it) }
                _clients.value = RepositoryProvider.clientRepo.getAllClients()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки справочников")
            }
        }
    }
}