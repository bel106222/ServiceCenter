package ru.bel.servicecenter.repository

// Синглтон, предоставляющий единый экземпляр всех репозиториев.
object RepositoryProvider {
    // Один объект SupabaseRepository реализует все интерфейсы
    private val repo = SupabaseRepository()

    // Публичные ссылки на репозитории
    val clientRepo: ClientRepository = repo
    val roleRepo: RoleRepository = repo
    val userRepo: UserRepository = repo
    val categoryRepo: CategoryRepository = repo
    val serviceRepo: ServiceRepository = repo
    val priceRepo: PriceRepository = repo
    val orderRepo: OrderRepository = repo
    val orderItemRepo: OrderItemRepository = repo

    suspend fun checkConnection() = repo.checkConnection()
    suspend fun checkAdminExists() = repo.checkAdminExists()
}