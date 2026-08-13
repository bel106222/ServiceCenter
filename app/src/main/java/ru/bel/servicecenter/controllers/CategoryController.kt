package ru.bel.servicecenter.controllers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import timber.log.Timber

class CategoryController : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _currentCategory = MutableStateFlow(Category(category_name = ""))
    val currentCategory: StateFlow<Category> = _currentCategory

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    var currentAuthUser: User? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = RepositoryProvider.categoryRepo.getAllCategories()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки категорий: ${e.message}"
            }
        }
    }

    fun saveCategory() {
        val cat = _currentCategory.value
        val authUser = currentAuthUser

        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }

            val errs = mapOf("category_name" to ValidationRules.validateRequired(cat.category_name, "Название"))
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                val existing = RepositoryProvider.categoryRepo.getCategoryByName(cat.category_name)
                if (existing != null && existing.id != cat.id) {
                    _errors.value = mapOf("category_name" to "Такая категория уже существует"); return@launch
                }
                if (cat.id.isEmpty() || _categories.value.none { it.id == cat.id }) {
                    RepositoryProvider.categoryRepo.createCategory(cat)
                    _message.value = "Категория создана"
                } else {
                    RepositoryProvider.categoryRepo.updateCategory(cat)
                    _message.value = "Категория обновлена"
                }
                loadCategories()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }

    fun deleteCategory(cat: Category) {
        val authUser = currentAuthUser
        viewModelScope.launch {
            if (authUser == null || !canModify(authUser)) {
                _message.value = "Недостаточно прав"; return@launch
            }
            try {
                RepositoryProvider.categoryRepo.deleteCategory(cat)
                loadCategories()
                _message.value = "Категория удалена"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun setEditingCategory(cat: Category) {
        _currentCategory.value = cat
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        _currentCategory.value = _currentCategory.value.copy(category_name = value)
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun canModify(user: User): Boolean {
        val admin = RepositoryProvider.roleRepo.getRoleByName("admin")
        val eng = RepositoryProvider.roleRepo.getRoleByName("engineer")
        return user.role_id == admin?.id || user.role_id == eng?.id
    }
}