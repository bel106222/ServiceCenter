package ru.bel.servicecenter.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.reports.ReportAllOrdersScreen
import ru.bel.servicecenter.ui.reports.ReportAllServicesScreen
import ru.bel.servicecenter.ui.reports.ReportCategoriesScreen
import ru.bel.servicecenter.ui.reports.ReportClientsScreen
import ru.bel.servicecenter.ui.reports.ReportEngineersScreen
import ru.bel.servicecenter.ui.reports.ReportMonthlyScreen
import ru.bel.servicecenter.ui.reports.ReportOrdersScreen
import ru.bel.servicecenter.ui.reports.ReportServicesScreen
import ru.bel.servicecenter.ui.reports.ReportsScreen
import ru.bel.servicecenter.viewmodels.AuthViewModel
import ru.bel.servicecenter.viewmodels.ReportsViewModel

fun NavGraphBuilder.reportsGraph(
    navController: NavController,
    authViewModel: AuthViewModel,
    reportsViewModel: ReportsViewModel
) {
    composable("reports") {
        val role by authViewModel.userRole.collectAsState()
        ReportsScreen(
            role = role ?: "unknown",
            onOpenMyOrders = { navController.navigate("report_my_orders") },
            onOpenMyServices = { navController.navigate("report_my_services") },
            onOpenMyMonthly = { navController.navigate("report_my_monthly") },
            onOpenAllOrders = { navController.navigate("report_all_orders") },
            onOpenAllServices = { navController.navigate("report_all_services") },
            onOpenClients = { navController.navigate("report_clients") },
            onOpenEngineers = { navController.navigate("report_engineers") },
            onOpenCategories = { navController.navigate("report_categories") },
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_my_orders") {
        ReportOrdersScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_my_services") {
        ReportServicesScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_my_monthly") {
        ReportMonthlyScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_all_orders") {
        ReportAllOrdersScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_all_services") {
        ReportAllServicesScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_clients") {
        ReportClientsScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_engineers") {
        ReportEngineersScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable("report_categories") {
        ReportCategoriesScreen(
            viewModel = reportsViewModel,
            onBack = { navController.popBackStack() }
        )
    }
}