package ru.bel.servicecenter.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.bel.servicecenter.ui.services.ServiceCategoriesScreen
import ru.bel.servicecenter.ui.services.ServiceEditScreen
import ru.bel.servicecenter.ui.services.ServicesListScreen
import ru.bel.servicecenter.viewmodels.ServiceViewModel

fun NavGraphBuilder.servicesGraph(
    navController: NavController,
    serviceViewModel: ServiceViewModel
) {
    composable("services") {
        ServiceCategoriesScreen(
            serviceViewModel = serviceViewModel,
            onSelectCategory = { category ->
                navController.navigate("services_by_category/${category.id}")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        "services_by_category/{categoryId}",
        arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
    ) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        val categories by serviceViewModel.categories.collectAsState()
        val category = categories.find { it.id == categoryId }
        if (category != null) {
            ServicesListScreen(
                serviceViewModel = serviceViewModel,
                category = category,
                onEditService = { service ->
                    serviceViewModel.startEditing(service)
                    navController.navigate("service_edit/${service.id}")
                },
                onBack = { navController.popBackStack() }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    composable(
        "service_edit/{serviceId}",
        arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
    ) { backStackEntry ->
        val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
        val services by serviceViewModel.services.collectAsState()
        val service = services.find { it.id == serviceId }
        if (service != null) {
            serviceViewModel.startEditing(service)
            ServiceEditScreen(
                serviceViewModel = serviceViewModel,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        } else {
            serviceViewModel.startEditing(null)
            ServiceEditScreen(
                serviceViewModel = serviceViewModel,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}