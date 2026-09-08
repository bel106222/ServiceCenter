package ru.bel.servicecenter.ui.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import ru.bel.servicecenter.models.DraftAttachment
import ru.bel.servicecenter.ui.components.ZoomableImageDialog
import ru.bel.servicecenter.viewmodels.OrderViewModel
import ru.bel.servicecenter.utils.ImageCompressor

/**
 * Экран работы с приложениями к заказу.
 * Позволяет выбирать изображения из галереи, просматривать их в полноэкранном режиме
 * и сохранять черновик. Фактическая загрузка в Storage происходит при сохранении заказа.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderAttachmentsScreen(
    orderViewModel: OrderViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val attachments by orderViewModel.draftAttachments.collectAsState()
    val context = LocalContext.current

    // Состояние для просмотра изображения на весь экран
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    // Лаунчер выбора изображения из галереи
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Сжимаем изображение перед добавлением в черновик
            val bytes = ImageCompressor.compress(context, uri)
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            orderViewModel.addDraftAttachment(
                uri = uri.toString(),
                fileName = fileName,
                fileBytes = bytes
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Приложения к заказу") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Отмена") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                imagePickerLauncher.launch("image/*")
            }) {
                Text("+")
            }
        },
        bottomBar = {
            // Кнопки Сохранить и Отмена внизу экрана
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(onClick = onSaved) {
                    Text("Сохранить")
                }
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Отмена")
                }
            }
        }
    ) { padding ->
        if (attachments.isEmpty()) {
            // Пустое состояние
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет приложений")
            }
        } else {
            // Сетка миниатюр
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(attachments) { attachment ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clickable {
                                // Открываем полноэкранный просмотр
                                selectedImageUrl = attachment.uri
                            }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(attachment.uri),
                            contentDescription = attachment.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Полноэкранный просмотр изображения
    if (selectedImageUrl != null) {
        ZoomableImageDialog(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }
}