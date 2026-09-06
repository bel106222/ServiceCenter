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
import ru.bel.servicecenter.ui.prices.PriceCategoriesScreen
import ru.bel.servicecenter.ui.prices.PriceEditScreen
import ru.bel.servicecenter.ui.prices.PriceServicesScreen
import ru.bel.servicecenter.viewmodels.PriceViewModel

fun NavGraphBuilder.pricesGraph(
    navController: NavController,
    priceViewModel: PriceViewModel
) {
    composable("prices") {
        PriceCategoriesScreen(
            priceViewModel = priceViewModel,
            onSelectCategory = { category ->
                navController.navigate("price_services/${category.id}")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        "price_services/{categoryId}",
        arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
    ) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        val categories by priceViewModel.categories.collectAsState()
        val category = categories.find { it.id == categoryId }
        if (category != null) {
            PriceServicesScreen(
                priceViewModel = priceViewModel,
                category = category,
                onEditPrice = { service, price ->
                    priceViewModel.startEditing(service, price)
                    navController.navigate("price_edit/${service.id}")
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
        "price_edit/{serviceId}",
        arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
    ) { backStackEntry ->
        val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
        val servicesWithPrices by priceViewModel.serviceWithPrice.collectAsState()
        val serviceWithPrice = servicesWithPrices.find { it.service.id == serviceId }
        if (serviceWithPrice != null) {
            PriceEditScreen(
                priceViewModel = priceViewModel,
                service = serviceWithPrice.service,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}