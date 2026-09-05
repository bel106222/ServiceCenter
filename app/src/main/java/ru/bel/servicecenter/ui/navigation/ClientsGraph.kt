package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.controllers.ClientController
import ru.bel.servicecenter.ui.clients.ClientsListScreen
import ru.bel.servicecenter.ui.clients.ClientEditScreen

fun NavGraphBuilder.clientsGraph(
    navController: NavController,
    clientController: ClientController
) {
    composable("clients") {
        ClientsListScreen(
            clientController = clientController,
            onEditClient = { client ->
                clientController.setEditingClient(client)
                navController.navigate("client_edit")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable("client_edit") {
        ClientEditScreen(
            clientController = clientController,
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }
}