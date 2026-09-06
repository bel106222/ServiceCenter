package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.clients.ClientEditScreen
import ru.bel.servicecenter.ui.clients.ClientsListScreen
import ru.bel.servicecenter.viewmodels.ClientViewModel

fun NavGraphBuilder.clientsGraph(
    navController: NavController,
    clientViewModel: ClientViewModel
) {
    composable("clients") {
        ClientsListScreen(
            clientViewModel = clientViewModel,
            onEditClient = { client ->
                clientViewModel.setEditingClient(client)
                navController.navigate("client_edit")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable("client_edit") {
        ClientEditScreen(
            clientViewModel = clientViewModel,
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }
}