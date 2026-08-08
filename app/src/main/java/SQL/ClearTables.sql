DO $$
DECLARE
r RECORD;
BEGIN
    -- Выбираем все пользовательские таблицы из схемы public
FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        -- Используем EXECUTE для динамического формирования команды
        EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE';
END LOOP;
END $$;