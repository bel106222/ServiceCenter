-- Включаем расширение для UUID, если ещё не включено
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Таблица ролей
CREATE TABLE IF NOT EXISTS public.roles (
                                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_name TEXT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );

-- Таблица клиентов
CREATE TABLE IF NOT EXISTS public.clients (
                                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_title TEXT UNIQUE NOT NULL,
    client_address TEXT,
    client_details TEXT,
    is_legal BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS public.users (
                                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_name TEXT NOT NULL,
    user_email TEXT UNIQUE NOT NULL,
    user_phone TEXT,
    user_password TEXT NOT NULL,
    client_id UUID REFERENCES public.clients(id),
    role_id UUID NOT NULL REFERENCES public.roles(id),
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );

-- Таблица категорий
CREATE TABLE IF NOT EXISTS public.categories (
                                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_name TEXT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );

-- Таблица услуг
CREATE TABLE IF NOT EXISTS public.services (
                                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name TEXT UNIQUE NOT NULL,
    service_description TEXT,
    category_id UUID NOT NULL REFERENCES public.categories(id),
    is_fixprice BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );

-- Таблица цен
CREATE TABLE IF NOT EXISTS public.prices (
                                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_id UUID NOT NULL REFERENCES public.services(id),
    service_cost REAL NOT NULL,
    is_time BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );

-- Таблица заказов
CREATE TABLE IF NOT EXISTS public.orders (
                                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number TEXT UNIQUE NOT NULL,
    order_description TEXT,
    user_id UUID NOT NULL REFERENCES public.users(id),
    order_sum REAL DEFAULT 0,
    is_completed BOOLEAN DEFAULT false,
    is_time BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );

-- Таблица позиций заказа
CREATE TABLE IF NOT EXISTS public.order_items (
                                                  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES public.orders(id),
    service_id UUID NOT NULL REFERENCES public.services(id),
    user_id UUID NOT NULL REFERENCES public.users(id),
    orderitem_quantity INT DEFAULT 1,
    orderitem_cost REAL DEFAULT 0,
    is_online BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
    );