package ru.bel.servicecenter.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.viewmodels.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDatabaseScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isClearing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление БД") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль администратора") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClearing
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    isClearing = true
                    adminViewModel.clearAndReseedDatabase(password)
                    isClearing = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = password.length >= 6 && !isClearing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Очистить и пересоздать БД")
                }
            }
        }
    }
}