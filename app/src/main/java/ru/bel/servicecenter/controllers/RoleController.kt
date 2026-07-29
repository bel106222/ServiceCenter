package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Role
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules

class RoleController : ViewModel() {

    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles

    private val _currentRole = MutableStateFlow(Role(role_name = ""))
    val currentRole: StateFlow<Role> = _currentRole

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    init {
        loadRoles()
    }

    fun loadRoles() {
        viewModelScope.launch {
            try {
                val names = listOf("admin", "engineer", "user")
                _roles.value = names.mapNotNull { RepositoryProvider.roleRepo.getRoleByName(it) }
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки ролей: ${e.message}"
            }
        }
    }

    fun saveRole() {
        val role = _currentRole.value
        val authUser = currentAuthUser

        viewModelScope.launch {
            if (authUser == null || !isAdmin(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }

            val errs = mapOf("role_name" to ValidationRules.validateRequired(role.role_name, "Название"))
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                val existing = RepositoryProvider.roleRepo.getRoleByName(role.role_name)
                if (existing != null && existing.id != role.id) {
                    _errors.value = mapOf("role_name" to "Роль уже существует"); return@launch
                }
                if (role.id.isEmpty()) {
                    RepositoryProvider.roleRepo.createRole(role)
                    _message.value = "Роль создана"
                } else {
                    _message.value = "Редактирование ролей не предусмотрено"
                }
                loadRoles()
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun deleteRole(role: Role) {
        viewModelScope.launch {
            _message.value = "Удаление ролей отключено"
        }
    }

    fun setEditingRole(role: Role) {
        _currentRole.value = role
        _errors.value = emptyMap()
    }

    fun updateRoleName(name: String) {
        _currentRole.value = _currentRole.value.copy(role_name = name)
    }

    private suspend fun isAdmin(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin") ?: return false
        return user.role_id == admin.id
    }
}