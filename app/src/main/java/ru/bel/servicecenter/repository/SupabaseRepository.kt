package ru.bel.servicecenter.repository
import ru.bel.servicecenter.BuildConfig
import ru.bel.servicecenter.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import timber.log.Timber

// Единый класс, реализующий все интерфейсы репозиториев через REST API Supabase.
// Использует Ktor для HTTP-запросов и kotlinx.serialization для парсинга JSON.
class SupabaseRepository : ClientRepository, RoleRepository, UserRepository,
    CategoryRepository, ServiceRepository, PriceRepository, OrderRepository,
    OrderItemRepository {

    // Создаём HTTP-клиент с поддержкой JSON
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true    // игнорировать незнакомые поля в JSON
                isLenient = true            // нестрогий режим (например, кавычки)
            })
        }
    }

    // Базовый URL Supabase REST API
    private val baseUrl = BuildConfig.SUPABASE_URL + "/rest/v1"
    // Секретный ключ (service_role) для авторизации
    private val apiKey = BuildConfig.SUPABASE_KEY

    // Вспомогательный метод GET – возвращает список объектов указанного типа.
    // queryParams – переменное количество пар "ключ=значение" для фильтрации.
    private suspend inline fun <reified T> get(
        table: String,
        vararg queryParams: Pair<String, String>
    ): List<T> {
        Timber.d("GET $table?${queryParams.joinToString("&") { "${it.first}=${it.second}" }}")
        return client.get("$baseUrl/$table") {
            header("apikey", apiKey)
            header("Authorization", "Bearer $apiKey")
            // Передаём параметры запроса (select, фильтры)
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }.body()
    }

    // Вспомогательный метод POST – создаёт запись и возвращает её (с заполненным id).
    private suspend inline fun <reified T> post(table: String, body: T): T {
        Timber.d("POST $table")
        return client.post("$baseUrl/$table") {
            header("apikey", apiKey)
            header("Authorization", "Bearer $apiKey")
            header("Prefer", "return=representation")  // вернуть созданную запись
            setBody(body)
        }.body()
    }

    // Вспомогательный метод PATCH – обновляет запись по id.
    private suspend inline fun <reified T> patch(table: String, id: String, body: T): T {
        Timber.d("PATCH $table/$id")
        return client.patch("$baseUrl/$table") {
            header("apikey", apiKey)
            header("Authorization", "Bearer $apiKey")
            header("Prefer", "return=representation")
            parameter("id", "eq.$id")      // фильтр: id = переданный UUID
            setBody(body)
        }.body()
    }

    // SoftDelete: устанавливает поле deleted_at в текущее время (запись остаётся в БД).
    private suspend fun softDelete(table: String, id: String) {
        Timber.d("SOFT DELETE $table/$id")
        val now = java.time.LocalDateTime.now().toString()
        client.patch("$baseUrl/$table") {
            header("apikey", apiKey)
            header("Authorization", "Bearer $apiKey")
            parameter("id", "eq.$id")
            setBody(mapOf("deleted_at" to now))
        }
    }

    // ------------------- Client -------------------
    override suspend fun createClient(client: Client) = post("clients", client)
    override suspend fun updateClient(client: Client) = patch("clients", client.id, client)
    override suspend fun deleteClient(client: Client) = softDelete("clients", client.id)
    override suspend fun getAllClients(): List<Client> =
        get("clients", "select" to "*", "deleted_at" to "is.null")

    // ------------------- Role -------------------
    override suspend fun createRole(role: Role) = post("roles", role)
    override suspend fun getRoleByName(name: String): Role? =
        get<Role>("roles", "select" to "*", "role_name" to "eq.$name").firstOrNull()

    // ------------------- User -------------------
    override suspend fun createUser(user: User) = post("users", user)
    override suspend fun updateUser(user: User) = patch("users", user.id, user)
    override suspend fun deleteUser(user: User) = softDelete("users", user.id)
    override suspend fun getUsersByClientId(clientId: String): List<User> =
        get("users", "select" to "*", "client_id" to "eq.$clientId", "deleted_at" to "is.null")
    override suspend fun getUserByEmail(email: String): User? =
        get<User>("users", "select" to "*", "user_email" to "eq.$email").firstOrNull()
    override suspend fun getAllUsers(): List<User> =
        get("users", "select" to "*", "deleted_at" to "is.null")

    // ------------------- Category -------------------
    override suspend fun createCategory(category: Category) = post("categories", category)
    override suspend fun updateCategory(category: Category) = patch("categories", category.id, category)
    override suspend fun deleteCategory(category: Category) = softDelete("categories", category.id)
    override suspend fun getAllCategories(): List<Category> =
        get("categories", "select" to "*", "deleted_at" to "is.null")
    override suspend fun getCategoryByName(name: String): Category? =
        get<Category>("categories", "select" to "*", "category_name" to "eq.$name").firstOrNull()

    // ------------------- Service -------------------
    override suspend fun createService(service: Service) = post("services", service)
    override suspend fun updateService(service: Service) = patch("services", service.id, service)
    override suspend fun deleteService(service: Service) = softDelete("services", service.id)
    override suspend fun getServicesByCategoryId(categoryId: String): List<Service> =
        get("services", "select" to "*", "category_id" to "eq.$categoryId", "deleted_at" to "is.null")
    override suspend fun getAllServices(): List<Service> =
        get("services", "select" to "*", "deleted_at" to "is.null")
    override suspend fun getServiceByName(name: String): Service? =
        get<Service>("services", "select" to "*", "service_name" to "eq.$name").firstOrNull()

    // ------------------- Price -------------------
    override suspend fun createPrice(price: Price) = post("prices", price)
    override suspend fun updatePrice(price: Price) = patch("prices", price.id, price)
    override suspend fun deletePrice(price: Price) = softDelete("prices", price.id)
    override suspend fun getPriceByServiceId(serviceId: String): Price? =
        get<Price>("prices", "select" to "*", "service_id" to "eq.$serviceId", "deleted_at" to "is.null").firstOrNull()
    override suspend fun getAllPrices(): List<Price> =
        get("prices", "select" to "*", "deleted_at" to "is.null")

    // ------------------- Order -------------------
    override suspend fun createOrder(order: Order) = post("orders", order)
    override suspend fun updateOrder(order: Order) = patch("orders", order.id, order)
    override suspend fun deleteOrder(order: Order) = softDelete("orders", order.id)
    override suspend fun getOrderByUserId(userId: String): List<Order> =
        get("orders", "select" to "*", "user_id" to "eq.$userId", "deleted_at" to "is.null")
    override suspend fun getOrderByOrderNumber(orderNumber: String): Order? =
        get<Order>("orders", "select" to "*", "order_number" to "eq.$orderNumber").firstOrNull()
    override suspend fun getAllOrders(): List<Order> =
        get("orders", "select" to "*", "deleted_at" to "is.null")

    // ------------------- OrderItem -------------------
    override suspend fun createOrderItem(item: OrderItem) = post("order_items", item)
    override suspend fun updateOrderItem(item: OrderItem) = patch("order_items", item.id, item)
    override suspend fun deleteOrderItem(item: OrderItem) = softDelete("order_items", item.id)
    override suspend fun getOrderItemsByOrderId(orderId: String): List<OrderItem> =
        get("order_items", "select" to "*", "order_id" to "eq.$orderId", "deleted_at" to "is.null")
}