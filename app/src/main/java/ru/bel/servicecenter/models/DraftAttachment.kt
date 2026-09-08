package ru.bel.servicecenter.models

/**
 * Локальное представление вложения (фотографии) на время редактирования заказа.
 * Содержит либо ссылку на существующий файл (attachment), либо байты нового изображения для загрузки.
 */
data class DraftAttachment(
    val id: String,                    // уникальный id
    val uri: String,                   // content:// URI или http(s) URL для отображения
    val fileName: String,
    val isNew: Boolean,                // true, если изображение ещё не загружено в Storage
    val fileBytes: ByteArray? = null   // сжатые байты для новых файлов, null для уже загруженных
)