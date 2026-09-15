package ru.bel.servicecenter.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.bel.servicecenter.viewmodels.ClientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(
    clientViewModel: ClientViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val currentClient by clientViewModel.currentClient.collectAsState()
    val errors by clientViewModel.errors.collectAsState()
    val operationCompleted by clientViewModel.operationCompleted.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    // Реагируем на успешное сохранение
    LaunchedEffect(operationCompleted) {
        if (operationCompleted) {
            isSaving = false
            clientViewModel.resetOperationCompleted()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Клиент") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = currentClient.client_title,
                onValueChange = { clientViewModel.updateField("title", it) },
                label = { Text("Название организации / ФИО") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                isError = errors["client_title"] != null,
                supportingText = { errors["client_title"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = currentClient.client_address,
                onValueChange = { clientViewModel.updateField("address", it) },
                label = { Text("Адрес") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                isError = errors["client_address"] != null,
                supportingText = { errors["client_address"]?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = currentClient.client_details,
                onValueChange = { clientViewModel.updateField("details", it) },
                label = { Text("Реквизиты") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                isError = errors["client_details"] != null,
                supportingText = { errors["client_details"]?.let { Text(it) } },
                minLines = 2,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Юридическое лицо", modifier = Modifier.weight(1f))
                Switch(
                    checked = currentClient.is_legal,
                    onCheckedChange = { clientViewModel.updateField("is_legal", it.toString()) },
                    enabled = !isSaving
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранение...")
                    }
                } else {
                    Button(onClick = {
                        isSaving = true
                        clientViewModel.saveClient()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledTonalButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Отмена")
                    }
                }
            }
        }
    }
}