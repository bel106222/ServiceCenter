package ru.bel.servicecenter.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.users.UserEditScreen
import ru.bel.servicecenter.ui.users.UsersListScreen
import ru.bel.servicecenter.viewmodels.UserViewModel

fun NavGraphBuilder.usersGraph(
    navController: NavController,
    userViewModel: UserViewModel
) {
    composable("users") {
        UsersListScreen(
            userViewModel = userViewModel,
            onEditUser = { user ->
                userViewModel.setEditingUser(user)
                navController.navigate("user_edit")
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable("user_edit") {
        UserEditScreen(
            userViewModel = userViewModel,
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }
}