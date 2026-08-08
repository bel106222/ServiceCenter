package ru.bel.servicecenter.repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.bel.servicecenter.BuildConfig
import ru.bel.servicecenter.models.*
import ru.bel.servicecenter.utils.LoggerService
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SupabaseRepository : ClientRepository, RoleRepository, UserRepository,
    CategoryRepository, ServiceRepository, PriceRepository, OrderRepository,
    OrderItemRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    private val baseUrl = BuildConfig.SUPABASE_URL + "/rest/v1"
    private val apiKey = BuildConfig.SUPABASE_KEY

    /**
     * Отправляет запрос и читает ответ (чтобы корректно закрыть соединение).
     * Таймауты увеличены до 30 секунд.
     */
    private suspend fun sendRequest(
        method: String,
        table: String,
        bodyString: String? = null,
        id: String? = null
    ): String = withContext(Dispatchers.IO) {
        var url = "$baseUrl/$table"
        if (id != null) url += "?id=eq.$id"

        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Connection", "close")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.doOutput = true

            if (bodyString != null) {
                LoggerService.log("$method $table ← $bodyString")
                OutputStreamWriter(connection.outputStream).use { it.write(bodyString) }
            } else {
                LoggerService.log("$method $table")
            }

            connection.connect()
            val responseCode = connection.responseCode
            // Читаем ответ (даже если он не нужен) – это освобождает соединение
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            LoggerService.log("$method $table → $responseCode: ${responseText.take(100)}")

            if (responseCode !in 200..299) {
                throw Exception("HTTP $responseCode: $responseText\n\nОтправленный JSON:\n$bodyString")
            }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    private suspend inline fun <reified T> post(table: String, body: T): T {
        val jsonBody = json.encodeToString(body)
        sendRequest("POST", table, jsonBody)
        return body
    }

    private suspend inline fun <reified T> patch(table: String, id: String, body: T): T {
        val jsonBody = json.encodeToString(body)
        sendRequest("PATCH", table, jsonBody, id)
        return body
    }

    private suspend fun softDelete(table: String, id: String) {
        val bodyMap = mapOf("deleted_at" to java.time.LocalDateTime.now().toString())
        val jsonBody = json.encodeToString(bodyMap)
        sendRequest("PATCH", table, jsonBody, id)
    }

    private suspend inline fun <reified T> get(
        table: String,
        vararg queryParams: Pair<String, String>
    ): List<T> = withContext(Dispatchers.IO) {
        val queryString = queryParams.joinToString("&") { "${it.first}=${it.second}" }
        val url = "$baseUrl/$table?$queryString"
        LoggerService.log("GET $url")

        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Connection", "close")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            connection.connect()
            val responseCode = connection.responseCode
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (responseCode != 200) {
                throw Exception("GET $url → $responseCode: $body")
            }
            json.decodeFromString(body)
        } finally {
            connection.disconnect()
        }
    }

    // ------------------- Реализация интерфейсов (без изменений) -------------------
    override suspend fun createClient(client: Client) = post("clients", client)
    override suspend fun updateClient(client: Client) = patch("clients", client.id, client)
    override suspend fun deleteClient(client: Client) = softDelete("clients", client.id)
    override suspend fun getAllClients(): List<Client> =
        get("clients", "select" to "*", "deleted_at" to "is.null")

    override suspend fun createRole(role: Role) = post("roles", role)
    override suspend fun getRoleByName(name: String): Role? =
        get<Role>("roles", "select" to "*", "role_name" to "eq.$name").firstOrNull()

    override suspend fun createUser(user: User) = post("users", user)
    override suspend fun updateUser(user: User) = patch("users", user.id, user)
    override suspend fun deleteUser(user: User) = softDelete("users", user.id)
    override suspend fun getUsersByClientId(clientId: String): List<User> =
        get("users", "select" to "*", "client_id" to "eq.$clientId", "deleted_at" to "is.null")
    override suspend fun getUserByEmail(email: String): User? =
        get<User>("users", "select" to "*", "user_email" to "eq.$email").firstOrNull()
    override suspend fun getAllUsers(): List<User> =
        get("users", "select" to "*", "deleted_at" to "is.null")

    override suspend fun createCategory(category: Category) = post("categories", category)
    override suspend fun updateCategory(category: Category) = patch("categories", category.id, category)
    override suspend fun deleteCategory(category: Category) = softDelete("categories", category.id)
    override suspend fun getAllCategories(): List<Category> =
        get("categories", "select" to "*", "deleted_at" to "is.null")
    override suspend fun getCategoryByName(name: String): Category? =
        get<Category>("categories", "select" to "*", "category_name" to "eq.$name").firstOrNull()

    override suspend fun createService(service: Service) = post("services", service)
    override suspend fun updateService(service: Service) = patch("services", service.id, service)
    override suspend fun deleteService(service: Service) = softDelete("services", service.id)
    override suspend fun getServicesByCategoryId(categoryId: String): List<Service> =
        get("services", "select" to "*", "category_id" to "eq.$categoryId", "deleted_at" to "is.null")
    override suspend fun getAllServices(): List<Service> =
        get("services", "select" to "*", "deleted_at" to "is.null")
    override suspend fun getServiceByName(name: String): Service? =
        get<Service>("services", "select" to "*", "service_name" to "eq.$name").firstOrNull()

    override suspend fun createPrice(price: Price) = post("prices", price)
    override suspend fun updatePrice(price: Price) = patch("prices", price.id, price)
    override suspend fun deletePrice(price: Price) = softDelete("prices", price.id)
    override suspend fun getPriceByServiceId(serviceId: String): Price? =
        get<Price>("prices", "select" to "*", "service_id" to "eq.$serviceId", "deleted_at" to "is.null").firstOrNull()
    override suspend fun getAllPrices(): List<Price> =
        get("prices", "select" to "*", "deleted_at" to "is.null")

    override suspend fun createOrder(order: Order) = post("orders", order)
    override suspend fun updateOrder(order: Order) = patch("orders", order.id, order)
    override suspend fun deleteOrder(order: Order) = softDelete("orders", order.id)
    override suspend fun getOrderByUserId(userId: String): List<Order> =
        get("orders", "select" to "*", "user_id" to "eq.$userId", "deleted_at" to "is.null")
    override suspend fun getOrderByOrderNumber(orderNumber: String): Order? =
        get<Order>("orders", "select" to "*", "order_number" to "eq.$orderNumber").firstOrNull()
    override suspend fun getAllOrders(): List<Order> =
        get("orders", "select" to "*", "deleted_at" to "is.null")

    override suspend fun createOrderItem(item: OrderItem) = post("order_items", item)
    override suspend fun updateOrderItem(item: OrderItem) = patch("order_items", item.id, item)
    override suspend fun deleteOrderItem(item: OrderItem) = softDelete("order_items", item.id)
    override suspend fun getOrderItemsByOrderId(orderId: String): List<OrderItem> =
        get("order_items", "select" to "*", "order_id" to "eq.$orderId", "deleted_at" to "is.null")

    // ---------- Проверки БД (IO) ----------
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        LoggerService.log("Проверка подключения к БД...")
        try {
            val url = "$baseUrl/"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            val code = connection.responseCode
            val success = code in 200..399
            LoggerService.log(if (success) "Подключение установлено (код $code)" else "Ошибка подключения: код $code")
            success
        } catch (e: Exception) {
            LoggerService.log("Ошибка подключения: ${e.message}")
            false
        }
    }

    suspend fun checkAdminExists(): Boolean = withContext(Dispatchers.IO) {
        try {
            LoggerService.log("Проверка наличия администратора...")
            // Запрашиваем user_name вместо id
            val url = "$baseUrl/users?select=user_name&user_name=eq.admin"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            val body = if (responseCode == 200) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            }
            connection.disconnect()

            LoggerService.log("Ответ сервера: $responseCode – $body")

            if (responseCode == 200) {
                // Теперь проверяем наличие "user_name":"admin" в ответе
                val exists = body.contains("\"user_name\":\"admin\"")
                LoggerService.log(if (exists) "Администратор найден" else "Администратор отсутствует")
                exists
            } else {
                LoggerService.log("Ошибка запроса: $responseCode")
                false
            }
        } catch (e: Exception) {
            LoggerService.log("Ошибка проверки администратора: ${e.message}")
            false
        }
    }
}