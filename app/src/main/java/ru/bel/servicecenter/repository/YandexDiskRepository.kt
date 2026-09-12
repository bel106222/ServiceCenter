package ru.bel.servicecenter.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.bel.servicecenter.BuildConfig
import ru.bel.servicecenter.utils.LoggerService
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Репозиторий для работы с Яндекс.Диском.
 * Файлы складываются в папку ServiceCenter (создаётся вручную через веб-интерфейс).
 * Имя файла формируется как <order_id>_<имя>, чтобы избежать коллизий.
 */
class YandexDiskRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://cloud-api.yandex.net/v1/disk"
    private val token = BuildConfig.YANDEX_TOKEN

    // Фиксированная папка, которую пользователь создаёт вручную
    private val rootFolder = "ServiceCenter"

    /**
     * Загружает файл на Яндекс.Диск в папку ServiceCenter.
     * @param uniqueName — уникальное имя файла (включая order_id)
     * @param fileBytes — содержимое файла
     * @return полный путь на диске: "disk:/ServiceCenter/uniqueName"
     */
    suspend fun uploadFile(
        uniqueName: String,
        fileBytes: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val diskPath = "disk:/$rootFolder/$uniqueName"
        val encodedPath = URLEncoder.encode(diskPath, "UTF-8")

        // 1. Получаем URL для загрузки
        val uploadUrlRequest = Request.Builder()
            .url("$baseUrl/resources/upload?path=$encodedPath&overwrite=true")
            .header("Authorization", "OAuth $token")
            .header("Accept", "application/json")
            .get()
            .build()

        val uploadUrlJson = client.newCall(uploadUrlRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: ""
                throw IOException("Яндекс: ошибка получения URL ${response.code}: $error")
            }
            response.body?.string() ?: throw IOException("Яндекс: пустой ответ")
        }

        val href = extractHref(uploadUrlJson)
            ?: throw IOException("Яндекс: не найден href: $uploadUrlJson")

        // 2. Загружаем файл
        val putRequest = Request.Builder()
            .url(href)
            .put(fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
            .build()

        client.newCall(putRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Яндекс: ошибка загрузки файла ${response.code}")
            }
        }
        LoggerService.log("Яндекс: файл загружен ($diskPath, ${fileBytes.size} байт)")

        diskPath
    }

    /**
     * Скачивает файл с Яндекс.Диска по полному пути.
     */
    suspend fun downloadFile(diskPath: String): ByteArray = withContext(Dispatchers.IO) {
        val encodedPath = URLEncoder.encode(diskPath, "UTF-8")

        val downloadUrlRequest = Request.Builder()
            .url("$baseUrl/resources/download?path=$encodedPath")
            .header("Authorization", "OAuth $token")
            .header("Accept", "application/json")
            .get()
            .build()

        val downloadUrlJson = client.newCall(downloadUrlRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: ""
                throw IOException("Яндекс: ошибка получения URL для скачивания ${response.code}: $error")
            }
            response.body?.string() ?: throw IOException("Яндекс: пустой ответ")
        }

        val href = extractHref(downloadUrlJson)
            ?: throw IOException("Яндекс: не найден href для скачивания")

        val getRequest = Request.Builder().url(href).get().build()
        return@withContext client.newCall(getRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Яндекс: ошибка скачивания ${response.code}")
            }
            response.body?.bytes() ?: throw IOException("Яндекс: пустое тело")
        }
    }

    /**
     * Удаляет файл с Яндекс.Диска.
     */
    suspend fun deleteFile(diskPath: String) = withContext(Dispatchers.IO) {
        val encodedPath = URLEncoder.encode(diskPath, "UTF-8")

        val request = Request.Builder()
            .url("$baseUrl/resources?path=$encodedPath&permanently=true")
            .header("Authorization", "OAuth $token")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw IOException("Яндекс: ошибка удаления ${response.code}")
            }
        }
        LoggerService.log("Яндекс: файл удалён ($diskPath)")
    }

    private fun extractHref(json: String): String? {
        val regex = "\"href\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    /**
     * Возвращает временную HTTP-ссылку для скачивания файла.
     * Ссылка живёт несколько часов, но при каждом открытии заказа запрашивается заново.
     */
    suspend fun getDownloadUrl(diskPath: String): String = withContext(Dispatchers.IO) {
        val encodedPath = URLEncoder.encode(diskPath, "UTF-8")

        val request = Request.Builder()
            .url("$baseUrl/resources/download?path=$encodedPath")
            .header("Authorization", "OAuth $token")
            .header("Accept", "application/json")
            .get()
            .build()

        val json = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: ""
                throw IOException("Яндекс: ошибка получения ссылки ${response.code}: $error")
            }
            response.body?.string() ?: throw IOException("Яндекс: пустой ответ")
        }

        extractHref(json) ?: throw IOException("Яндекс: не найден href: $json")
    }
}