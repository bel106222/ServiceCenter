package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.bel.servicecenter.controllers.UserController
import ru.bel.servicecenter.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(
    userController: UserController = viewModel(),
    onEditUser: (User) -> Unit,
    onBack: () -> Unit
) {
    val users by userController.users.collectAsState()
    val message by userController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) showMessage = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пользователи") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Переход к созданию нового пользователя
                userController.setEditingUser(
                    User(
                        user_name = "",
                        user_email = "",
                        user_phone = "",
                        user_password = "",
                        role_id = ""
                    )
                )
                onEditUser(userController.currentUser.value)
            }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(users) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(user.user_name, style = MaterialTheme.typography.titleMedium)
                        Text(user.user_email)
                        Row {
                            TextButton(onClick = { onEditUser(user) }) { Text("Изменить") }
                            TextButton(onClick = { userController.deleteUser(user) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }

    if (showMessage) {
        AlertDialog(
            onDismissRequest = { showMessage = false },
            title = { Text("Сообщение") },
            text = { Text(message ?: "") },
            confirmButton = { TextButton(onClick = { showMessage = false }) { Text("OK") } }
        )
    }
}