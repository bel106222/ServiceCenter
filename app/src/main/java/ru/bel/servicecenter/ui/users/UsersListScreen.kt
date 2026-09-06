package ru.bel.servicecenter.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(
    userViewModel: UserViewModel,
    onEditUser: (User) -> Unit,
    onBack: () -> Unit
) {
    val users by userViewModel.users.collectAsState()
    val message by userViewModel.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userViewModel.loadUsers()
    }
    LaunchedEffect(message) { if (message != null) showMessage = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пользователи") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                userViewModel.clearMessage()
                userViewModel.setEditingUser(
                    User(user_name = "", user_email = "", user_phone = "", user_password = "", role_id = "")
                )
                onEditUser(userViewModel.currentUser.value)
            }) { Text("+") }
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
                            TextButton(onClick = {
                                userViewModel.clearMessage()
                                userViewModel.setEditingUser(user)
                                onEditUser(user)
                            }) { Text("Изменить") }
                            TextButton(onClick = { userViewModel.deleteUser(user) }) { Text("Удалить") }
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
            confirmButton = {
                TextButton(onClick = {
                    showMessage = false
                    userViewModel.clearMessage()
                }) { Text("OK") }
            }
        )
    }
}