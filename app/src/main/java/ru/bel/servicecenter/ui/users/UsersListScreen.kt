package ru.bel.servicecenter.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.models.User
import ru.bel.servicecenter.utils.RoleCache
import ru.bel.servicecenter.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(
    userViewModel: UserViewModel,
    onEditUser: (User) -> Unit,
    onBack: () -> Unit
) {
    val users by userViewModel.users.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    // Пользователь, которого пользователь хочет удалить (для диалога подтверждения).
    var userToDelete by remember { mutableStateOf<User?>(null) }

    // Строка поиска
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        userViewModel.loadUsers()
    }

    // Отфильтрованный список: ищем по имени и email, без учёта регистра
    val filteredUsers = remember(users, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            users
        } else {
            users.filter {
                it.user_name.contains(query, ignoreCase = true) ||
                        it.user_email.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Пользователи")
                        Text(
                            "Смахните карточку вправо, чтобы удалить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                userViewModel.setEditingUser(
                    User(user_name = "", user_email = "", user_phone = "",
                        user_password = "", role_id = "")
                )
                onEditUser(userViewModel.currentUser.value)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить пользователя")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ============================================================
            // Поиск по имени или email
            // ============================================================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск") },
                placeholder = { Text("Имя или email") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ============================================================
            // Основной контент
            // ============================================================
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    // Пользователей вообще нет
                    users.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Person, null,
                            Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Пользователей пока нет", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Нажмите +, чтобы добавить первого пользователя",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Пользователи есть, но поиск ничего не нашёл
                    filteredUsers.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search, null,
                            Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Попробуйте изменить запрос",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Список пользователей
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredUsers, key = { it.id }) { user ->
                            SwipeableUserCard(
                                user = user,
                                onClick = {
                                    userViewModel.setEditingUser(user)
                                    onEditUser(user)
                                },
                                onSwipeToDelete = { userToDelete = user }
                            )
                        }
                    }
                }
            }
        }
    }

    // ================================================================
    // Диалог подтверждения удаления
    // ================================================================
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Удалить пользователя?") },
            text = {
                Text("«${user.user_name}» (${user.user_email}) будет удалён. Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.deleteUser(user)
                    userToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Карточка пользователя с возможностью смахнуть вправо для удаления.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableUserCard(
    user: User,
    onClick: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeToDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .size(28.dp)
                )
            }
        }
    ) {
        UserCard(user = user, onClick = onClick)
    }
}

/**
 * Одна карточка пользователя.
 */
@Composable
private fun UserCard(
    user: User,
    onClick: () -> Unit
) {
    // Название роли берём из RoleCache по role_id.
    // Если роль не загружена — показываем прочерк.
    val roleName = remember(user.role_id) {
        RoleCache.get(user.role_id) ?: "—"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Строка 1: имя + чип роли
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    user.user_name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                RoleChip(roleName = roleName)
            }

            // Строка 2: email
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Email, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    user.user_email,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Маленький чип с названием роли.
 */
@Composable
private fun RoleChip(roleName: String) {
    // Для разных ролей — разный цвет, чтобы визуально различались.
    val containerColor = when (roleName) {
        "admin" -> MaterialTheme.colorScheme.errorContainer
        "engineer" -> MaterialTheme.colorScheme.primaryContainer
        "user" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (roleName) {
        "admin" -> MaterialTheme.colorScheme.onErrorContainer
        "engineer" -> MaterialTheme.colorScheme.onPrimaryContainer
        "user" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = roleName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}