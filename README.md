# Service Center — мобильное приложение для сервисного центра

Android-приложение для автоматизации работы сервисного центра:
учёт клиентов, услуг, заказов, цен и сотрудников, формирование отчётов.

---

## Содержание

- [Возможности](#возможности)
- [Технологии](#технологии)
- [Уведомления](#уведомления)
- [Архитектура](#архитектура)
- [Структура проекта](#структура-проекта)
- [Быстрый старт](#быстрый-старт)
- [Конфигурация](#конфигурация)
- [Схема базы данных](#схема-базы-данных)
- [Роли пользователей](#роли-пользователей)
- [Отчёты](#отчёты)
- [Автор](#автор)

---

## Возможности

### Учёт данных

- **Клиенты** — юридические и физические лица, с адресом и реквизитами.
- **Категории услуг** — группировка услуг по направлениям.
- **Услуги** — с описанием и признаком фиксированной цены.
- **Цены** — история цен по каждой услуге, поддержка почасовой оплаты.
- **Пользователи** — роли, привязка к клиенту.
- **Заказы** — с позициями, вложениями (фото), суммой и статусом.
- **Позиции заказов** — исполнитель, количество, стоимость, признак удалённого выполнения.

### Функциональность

- Аутентификация по email и паролю (BCrypt-хеширование).
- Регистрация новых пользователей с ролью `user`.
- Привязка пользователя к клиенту при первом входе.
- Поиск по всем спискам (клиенты, категории, услуги, пользователи, заказы, цены).
- Свайп-вправо для удаления элементов (с подтверждением).
- Фильтрация заказов по статусу и поиск по номеру/описанию.
- Загрузка фотографий к заказам (сжатие на клиенте).
- Просмотр фотографий с зумом.
- Отчёты по ролям.
- Глобальные уведомления через Snackbar.
- Светлая и тёмная темы.
- Email-уведомления о заказах через Supabase Edge Functions и вебхуки.

### Технические особенности

- Архитектура MVVM.
- Реактивный UI на Jetpack Compose + Material 3.
- Асинхронная работа через корутины и StateFlow.
- Soft delete (`deleted_at`) вместо физического удаления.
- Логирование через Timber + отображение в UI.
- Кастомный `MessageBus` для централизованных уведомлений.
- Кэширование ролей (`RoleCache`) и цен (`priceCache`).

---

## Технологии

| Категория | Технология |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Архитектура | MVVM |
| Асинхронность | Kotlin Coroutines, StateFlow |
| Сеть | HttpURLConnection (REST), Supabase SDK (S3 Storage) |
| Бэкенд (БД) | Supabase (PostgreSQL) |
| Хранилище файлов | Яндекс.Диск |
| Сериализация | kotlinx.serialization |
| Хеширование | BCrypt |
| Логирование | Timber |
| Загрузка изображений | Coil |
| HTTP-клиент | OkHttp (для Яндекс.Диска) |
| Email | SMTP Mail.ru через Supabase Edge Functions |

---

## Уведомления

Email-уведомления работают на стороне Supabase через **Edge Functions** (Deno/TypeScript), которые вызываются вебхуками (Database Webhooks) при изменениях в таблице `orders`.

### События

| Событие | Триггер | Получатели | Что в письме |
|---|---|---|---|
| **Новый заказ** | `INSERT` в `orders` | Все инженеры (роль `engineer`) | Номер, автор, описание, сумма |
| **Заказ завершён** | `UPDATE` в `orders`, когда `is_completed` меняется на `true` | Автор заказа | Номер, описание, итоговая сумма |

### Конфигурация

Параметры SMTP и другие секреты хранятся в **Environment Variables** проекта Supabase:

```
SMTP_HOST=smtp.mail.ru
SMTP_PORT=465
SMTP_USER=your-email@mail.ru
SMTP_PASSWORD=your-app-password
```

Вебхуки настраиваются в **Supabase → Database → Webhooks**:

- **webhook_new_order**: таблица `orders`, событие `INSERT`, тип `HTTP Request`, URL — адрес Edge Function.
- **webhook_order_completed**: таблица `orders`, событие `UPDATE`, тип `HTTP Request`, URL — адрес Edge Function.

> ⚠️ Секреты SMTP **не хранятся в репозитории**. Они прописываются только в панели Supabase (Edge Functions → Secrets).

---

## Архитектура

```
┌─────────────────────────────────────┐
│            UI (Compose)             │
│  Screens · Dialogs · Components     │
└──────────────┬──────────────────────┘
               │ collectAsState / call
┌──────────────▼──────────────────────┐
│            ViewModel                │
│  StateFlow · viewModelScope         │
└──────────────┬──────────────────────┘
               │ suspend fun
┌──────────────▼──────────────────────┐
│          Repository                 │
│  RepositoryProvider (singleton)     │
│  SupabaseRepository                 │
│  YandexDiskRepository               │
└──────────────┬──────────────────────┘
               │ HTTP
┌──────────────▼──────────────────────┐
│   Supabase (PostgreSQL) · Яндекс    │
└─────────────────────────────────────┘
```

**Ключевые принципы:**

- **Один источник правды.** Каждая ViewModel владеет своим `StateFlow`.
- **Разделение сообщений и навигации.** Сообщения — через `MessageBus`. Сигнал о завершении операции — через `operationCompleted: StateFlow<Boolean>`.
- **Repository pattern.** Один `SupabaseRepository` реализует все интерфейсы репозиториев.
- **Soft delete.** Все удаления — через установку `deleted_at`, физическое удаление не используется.

---

## Структура проекта

```
app/src/main/java/ru/bel/servicecenter/
├── factory/              # DataFactory — начальные данные
├── models/               # Data-классы (Client, User, Order и т.д.)
├── repository/           # RepositoryProvider, SupabaseRepository, YandexDiskRepository
├── rules/                # ValidationRules — валидация полей
├── ui/
│   ├── admin/            # Управление БД (админ)
│   ├── auth/             # Login, Register, SelectClient, NotAClient
│   ├── categories/       # Список и редактор категорий
│   ├── clients/          # Список и редактор клиентов
│   ├── components/       # StatusBar, HistoryDialog, ZoomableImage
│   ├── dashboard/        # Главное меню
│   ├── navigation/       # NavGraph и графы по разделам
│   ├── orders/           # Список, редактор, вложения, позиции
│   ├── prices/           # Категории цен, услуги с ценами, редактор цены
│   ├── profile/          # Профили admin / engineer / user
│   ├── reports/          # Меню отчётов и экраны отчётов
│   ├── services/         # Категории услуг, список услуг, редактор
│   ├── status/           # DatabaseStatusScreen
│   ├── theme/            # ThemeManager, AppTheme
│   └── users/            # Список и редактор пользователей
├── utils/                # MessageBus, RoleCache, LoggerService и др.
├── viewmodels/           # Все ViewModel
├── MainActivity.kt
└── BuildConfig (SUPABASE_URL, SUPABASE_KEY, YANDEX_TOKEN)
```

---

## Быстрый старт

### Требования

- Android Studio Hedgehog (2023.1.1) или новее.
- JDK 17+.
- Android SDK 34.
- Эмулятор или устройство на Android 8.0 (API 26) и выше.
- Аккаунт Supabase с созданной базой данных.
- OAuth-токен Яндекс.Диска.

### Сборка

```bash
# Клонировать репозиторий
git clone https://github.com/your-username/ServiceCenter.git
cd ServiceCenter

# Открыть в Android Studio и синхронизировать Gradle
# Или собрать через терминал:
./gradlew assembleDebug
```

### Запуск

```bash
./gradlew installDebug
```

Или через Android Studio: **Run → Run 'app'**.

---

## Конфигурация

Секреты не хранятся в репозитории. Они прописываются в `local.properties` (не коммитится) или через переменные окружения.

### `local.properties`

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-key
YANDEX_TOKEN=your-yandex-oauth-token
```

### `app/build.gradle.kts`

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"${localProperties["SUPABASE_URL"]}\"")
buildConfigField("String", "SUPABASE_KEY", "\"${localProperties["SUPABASE_KEY"]}\"")
buildConfigField("String", "YANDEX_TOKEN", "\"${localProperties["YANDEX_TOKEN"]}\"")
```

## Схема базы данных

```sql
roles        (id, role_name, created_at, deleted_at)
clients      (id, client_title, client_address, client_details,
              is_legal, created_at, deleted_at)
users        (id, user_name, user_email, user_phone, user_password,
              client_id → clients, role_id → roles,
              created_at, deleted_at)
categories   (id, category_name, created_at, deleted_at)
services     (id, service_name, service_description,
              category_id → categories, is_fixprice,
              created_at, deleted_at)
prices       (id, service_id → services, service_cost, is_time,
              created_at, deleted_at)
orders       (id, order_number, order_description,
              user_id → users, order_sum, is_completed, is_time,
              created_at, deleted_at)
order_items  (id, order_id → orders, service_id → services,
              user_id → users, orderitem_quantity, orderitem_cost,
              is_online, created_at, deleted_at)
attachments  (id, order_id → orders, filename, url,
              created_at, deleted_at)
```

**Общие правила:**

- Первичные ключи — `UUID` (`uuid_generate_v4()`).
- `created_at` — `TIMESTAMP DEFAULT now()`.
- Soft delete — через `deleted_at`.
- Уникальность: `role_name`, `client_title`, `category_name`, `service_name`, `user_email`, `order_number`.

---

## Роли пользователей

| Роль | Возможности |
|---|---|
| **admin** | Всё: пользователи, клиенты, категории, услуги, цены, заказы, отчёты, управление БД. |
| **engineer** | Клиенты, категории, услуги, цены, заказы, отчёты (без управления пользователями и БД). |
| **user** | Свои заказы, свой профиль, отчёты только по своим заказам. |

---

## Отчёты

### Для роли `user`

- Мои заказы за период.
- Мои расходы по услугам.
- Мои расходы по месяцам.

### Для ролей `engineer` и `admin`

- Все заказы за период.
- Выручка по услугам.
- Заказы по клиентам.
- Услуги по сотрудникам.
- Выручка по категориям.

**Периоды:** неделя, месяц, 3 месяца, всё время.

---

## Автор

**Беляев Дмитрий**
- GitHub: [@bel106222](https://github.com/bel106222)
- Email: _bel@mail.ru
