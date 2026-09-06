package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.admin.AdminDatabaseScreen
import ru.bel.servicecenter.viewmodels.AdminViewModel

fun NavGraphBuilder.adminGraph(
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    composable("admin_database") {
        AdminDatabaseScreen(
            adminViewModel = adminViewModel,
            onBack = { navController.popBackStack() }
        )
    }
}