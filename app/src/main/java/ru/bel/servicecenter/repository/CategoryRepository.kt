package ru.bel.servicecenter.repository
import ru.bel.servicecenter.models.Category
interface CategoryRepository {
    suspend fun createCategory(category: Category): Category
    suspend fun updateCategory(category: Category): Category
    suspend fun deleteCategory(category: Category)
    suspend fun getAllCategories(): List<Category>
    suspend fun getCategoryByName(name: String): Category?
}