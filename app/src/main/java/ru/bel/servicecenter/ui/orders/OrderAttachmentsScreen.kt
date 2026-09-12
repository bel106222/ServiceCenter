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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color

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
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bytes = ImageCompressor.compress(context, uri)
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            orderViewModel.addDraftAttachment(uri.toString(), fileName, bytes)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Приложения к заказу") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Отмена") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("+")
            }
        },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Button(onClick = onSaved) { Text("Сохранить") }
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) { Text("Отмена") }
            }
        }
    ) { padding ->
        if (attachments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет приложений")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
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
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedImageUrl != null) {
        ZoomableImageDialog(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }
}