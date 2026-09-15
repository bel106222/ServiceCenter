package ru.bel.servicecenter.ui.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import ru.bel.servicecenter.models.DraftAttachment
import ru.bel.servicecenter.ui.components.ZoomableImageDialog
import ru.bel.servicecenter.utils.ImageCompressor
import ru.bel.servicecenter.viewmodels.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderAttachmentsScreen(
    orderViewModel: OrderViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val attachments by orderViewModel.draftAttachments.collectAsState()
    val isLoadingAttachments by orderViewModel.isLoadingAttachments.collectAsState()
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Приложения к заказу")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                imagePickerLauncher.launch("image/*")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить фото")
            }
        },
        bottomBar = {
            // Кнопки «Сохранить» и «Отмена» внизу экрана
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(onClick = onSaved) {
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
    ) { padding ->
        when {
            // Загрузка вложений из Яндекс.Диска
            isLoadingAttachments -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Загрузка вложений...")
                    }
                }
            }

            // Пустое состояние
            attachments.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Приложений пока нет",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Нажмите +, чтобы добавить фото",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Сетка миниатюр
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(attachments, key = { it.id }) { attachment ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { selectedImageUrl = attachment.uri }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(attachment.uri),
                                    contentDescription = attachment.fileName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Кнопка удаления в правом верхнем углу
                            IconButton(
                                onClick = { orderViewModel.removeDraftAttachment(attachment.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Удалить",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Полноэкранный просмотр выбранного изображения
    if (selectedImageUrl != null) {
        ZoomableImageDialog(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }
}