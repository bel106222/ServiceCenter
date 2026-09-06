package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.categories.CategoriesListScreen
import ru.bel.servicecenter.ui.categories.CategoryEditScreen
import ru.bel.servicecenter.viewmodels.CategoryViewModel

fun NavGraphBuilder.categoriesGraph(
    navController: NavController,
    categoryViewModel: CategoryViewModel
) {
    composable("categories") {
        CategoriesListScreen(
            categoryViewModel = categoryViewModel,
            onEditCategory = { category ->
                categoryViewModel.setEditingCategory(category)
                navController.navigate("category_edit")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable("category_edit") {
        CategoryEditScreen(
            categoryViewModel = categoryViewModel,
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }
}