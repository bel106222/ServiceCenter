package ru.bel.servicecenter.repository

import ru.bel.servicecenter.models.Attachment

interface AttachmentRepository {
    suspend fun getAttachmentsByOrderId(orderId: String): List<Attachment>
    suspend fun createAttachment(attachment: Attachment)
    suspend fun deleteAttachment(attachment: Attachment)
    suspend fun uploadFileToStorage(
        bucket: String,
        fileName: String,
        fileBytes: ByteArray,
        contentType: String
    ): String  // возвращает публичный URL файла
}