package ru.bel.servicecenter.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.bel.servicecenter.ui.profile.*
import ru.bel.servicecenter.viewmodels.AuthViewModel
import ru.bel.servicecenter.viewmodels.UserViewModel

fun NavGraphBuilder.profileGraph(
    navController: NavController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel
) {
    composable("profile_user") {
        val currentUser by authViewModel.loggedUser.collectAsState()
        currentUser?.let { user ->
            userViewModel.currentAuthUser = user
            UserProfileScreen(
                currentUser = user,
                userViewModel = userViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }

    composable("profile_engineer") {
        val currentUser by authViewModel.loggedUser.collectAsState()
        currentUser?.let { user ->
            userViewModel.currentAuthUser = user
            EngineerProfileScreen(
                currentUser = user,
                userViewModel = userViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }

    composable("profile_admin") {
        val currentUser by authViewModel.loggedUser.collectAsState()
        currentUser?.let { user ->
            userViewModel.currentAuthUser = user
            AdminProfileScreen(
                currentUser = user,
                userViewModel = userViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}