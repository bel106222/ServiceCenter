package ru.bel.servicecenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.bel.servicecenter.models.Category
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.repository.RepositoryProvider
import ru.bel.servicecenter.rules.ValidationRules
import ru.bel.servicecenter.utils.LoggerService
import ru.bel.servicecenter.utils.RoleCache
import timber.log.Timber

class CategoryViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _currentCategory = MutableStateFlow(Category(category_name = ""))
    val currentCategory: StateFlow<Category> = _currentCategory

    private val _errors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val errors: StateFlow<Map<String, String?>> = _errors

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var currentAuthUser: User? = null

    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _categories.value = emptyList()
            try {
                _categories.value = RepositoryProvider.categoryRepo.getAllCategories()
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки категорий: ${e.message}"
                Timber.e(e, "Ошибка загрузки категорий")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveCategory() {
        val category = _currentCategory.value
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }

        viewModelScope.launch {
            if (!canModify(authUser)) {
                _message.value = "Недостаточно прав"
                return@launch
            }

            val errs = mapOf("category_name" to ValidationRules.validateRequired(category.category_name, "Название"))
            _errors.value = errs
            if (errs.any { it.value != null }) return@launch

            try {
                val existing = RepositoryProvider.categoryRepo.getCategoryByName(category.category_name)
                if (existing != null && existing.id != category.id) {
                    _errors.value = mapOf("category_name" to "Такая категория уже существует")
                    return@launch
                }

                if (category.id.isEmpty() || _categories.value.none { it.id == category.id }) {
                    RepositoryProvider.categoryRepo.createCategory(category)
                    _message.value = "Категория создана"
                    LoggerService.log("Категория создана: ${category.category_name}")
                } else {
                    RepositoryProvider.categoryRepo.updateCategory(category)
                    _message.value = "Категория обновлена"
                    LoggerService.log("Категория обновлена: ${category.category_name}")
                }
                loadCategories()
            } catch (e: Exception) {
                _message.value = "Ошибка сохранения: ${e.message}"
                Timber.e(e, "Ошибка сохранения категории")
            }
        }
    }

    fun deleteCategory(category: Category) {
        val authUser = currentAuthUser ?: run {
            _message.value = "Не выполнен вход"
            return
        }
        viewModelScope.launch {
            if (!canModify(authUser)) {
                _message.value = "Недостаточно прав"
                return@launch
            }
            try {
                RepositoryProvider.categoryRepo.deleteCategory(category)
                _message.value = "Категория удалена"
                LoggerService.log("Категория удалена: ${category.category_name}")
                loadCategories()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
                Timber.e(e, "Ошибка удаления категории")
            }
        }
    }

    fun setEditingCategory(category: Category) {
        _currentCategory.value = category
        _errors.value = emptyMap()
    }

    fun updateField(field: String, value: String) {
        _currentCategory.value = _currentCategory.value.copy(category_name = value)
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun canModify(user: User): Boolean {
        val role = RoleCache.get(user.role_id) ?: return false
        return role == "admin" || role == "engineer"
    }
}