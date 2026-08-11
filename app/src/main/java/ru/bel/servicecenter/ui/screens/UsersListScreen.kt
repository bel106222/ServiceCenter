package ru.bel.servicecenter.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.controllers.UserManagementViewModel
import ru.bel.servicecenter.models.User

/**
 * Экран со списком всех пользователей.
 * Доступен только администратору.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(
    viewModel: UserManagementViewModel,
    onEditUser: (User) -> Unit,
    onBack: () -> Unit
) {
    val users by viewModel.userController.users.collectAsState()
    val message by viewModel.userController.message.collectAsState()
    var showMessage by remember { mutableStateOf(false) }

    // Показываем диалог при появлении сообщения
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
                // Очищаем сообщение и создаём пустого пользователя для добавления
                viewModel.userController.clearMessage()
                viewModel.userController.setEditingUser(
                    User(
                        user_name = "",
                        user_email = "",
                        user_phone = "",
                        user_password = "",
                        role_id = ""
                    )
                )
                onEditUser(viewModel.userController.currentUser.value)
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
                            TextButton(onClick = {
                                // Очищаем сообщение и готовим выбранного пользователя к редактированию
                                viewModel.userController.clearMessage()
                                viewModel.userController.setEditingUser(user)
                                onEditUser(user)
                            }) { Text("Изменить") }
                            TextButton(onClick = { viewModel.userController.deleteUser(user) }) {
                                Text("Удалить")
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог с сообщением (успех/ошибка)
    if (showMessage) {
        AlertDialog(
            onDismissRequest = { showMessage = false },
            title = { Text("Сообщение") },
            text = { Text(message ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    showMessage = false
                    viewModel.userController.clearMessage() // очищаем сообщение
                }) { Text("OK") }
            }
        )
    }
}