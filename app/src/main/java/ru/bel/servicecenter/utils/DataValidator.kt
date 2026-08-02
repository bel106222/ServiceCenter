package ru.bel.servicecenter.utils
import ru.bel.servicecenter.repository.RepositoryProvider

/**
 * Класс для проверки целостности начальных данных,
 * созданных фабрикой [DataFactory].
 * Не изменяет БД, только выполняет GET-запросы и логирует результаты.
 */
class DataValidator {

    /**
     * Проверяет наличие всех обязательных записей:
     * роли (admin, engineer, user), пользователь admin с паролем,
     * клиент "Сервисный центр", 6 категорий, 14 услуг, 14 цен.
     * @return true, если все проверки пройдены, иначе false.
     */
    suspend fun validate(): Boolean {
        LoggerService.log("=== Проверка целостности начальных данных ===")
        var allOk = true

        // 1. Роли
        val roles = listOf("admin", "engineer", "user")
        for (role in roles) {
            val found = RepositoryProvider.roleRepo.getRoleByName(role)
            if (found == null) {
                LoggerService.log("ОШИБКА: роль '$role' не найдена")
                allOk = false
            } else {
                LoggerService.log("✓ Роль '$role' присутствует")
            }
        }

        // 2. Администратор
        val users = RepositoryProvider.userRepo.getAllUsers()
        val admin = users.find { it.user_name == "admin" }
        when {
            admin == null -> {
                LoggerService.log("ОШИБКА: пользователь admin не найден")
                allOk = false
            }
            admin.user_password.isBlank() -> {
                LoggerService.log("ОШИБКА: у admin не установлен пароль")
                allOk = false
            }
            else -> LoggerService.log("✓ Пользователь admin с паролем присутствует")
        }

        // 3. Клиент
        val clients = RepositoryProvider.clientRepo.getAllClients()
        if (clients.none { it.client_title == "Сервисный центр" }) {
            LoggerService.log("ОШИБКА: клиент 'Сервисный центр' не найден")
            allOk = false
        } else {
            LoggerService.log("✓ Клиент 'Сервисный центр' присутствует")
        }

        // 4. Категории
        val categories = RepositoryProvider.categoryRepo.getAllCategories()
        val expectedCategories = listOf(
            "Организация локальной сети",
            "Ремонт и обслуживание оргтехники",
            "Ремонт компьютеров, ноутбуков, моноблоков",
            "ИТ-услуги по оргтехнике",
            "ИТ-услуги по компьютерам, ноутбукам, моноблокам",
            "ИТ-услуги по серверам и сетевому оборудованию"
        )
        for (name in expectedCategories) {
            if (categories.none { it.category_name == name }) {
                LoggerService.log("ОШИБКА: категория '$name' не найдена")
                allOk = false
            } else {
                LoggerService.log("✓ Категория '$name' присутствует")
            }
        }

        // 5. Услуги
        val services = RepositoryProvider.serviceRepo.getAllServices()
        val expectedServices = listOf(
            "Монтаж кабельной сети (ЛВС)",
            "Забор компьютерной техники в сервис",
            "Доставка компьютерной техники из сервиса",
            "Чистка, оптимизация операционной системы",
            "Восстановление работоспособности ОС",
            "Выезд к заказчику",
            "Ремонт оргтехники 1 категории сложности",
            "Ремонт оргтехники 2 категории сложности",
            "Ремонт оргтехники 3 категории сложности",
            "Подключение принтеров, МФУ, сканеров и т.д.",
            "Заправка картриджей",
            "Работы посредством удаленного подключения (при технической возможности)",
            "Консультация и подбор оборудования под определенные задачи",
            "Работы с выездом к заказчику (минимальная стоимость, включая 1 час работ)"
        )
        for (name in expectedServices) {
            if (services.none { it.service_name == name }) {
                LoggerService.log("ОШИБКА: услуга '$name' не найдена")
                allOk = false
            } else {
                LoggerService.log("✓ Услуга '$name' присутствует")
            }
        }

        // 6. Цены
        val prices = RepositoryProvider.priceRepo.getAllPrices()
        if (prices.size != 14) {
            LoggerService.log("ОШИБКА: количество цен ${prices.size}, ожидалось 14")
            allOk = false
        } else {
            LoggerService.log("✓ Количество цен совпадает (14)")
        }

        if (allOk) {
            LoggerService.log("=== Все проверки пройдены успешно ===")
        } else {
            LoggerService.log("=== Обнаружены расхождения, требуется повторная инициализация ===")
        }
        return allOk
    }
}