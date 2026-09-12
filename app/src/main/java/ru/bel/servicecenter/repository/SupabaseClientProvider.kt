package ru.bel.servicecenter.repository

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import ru.bel.servicecenter.BuildConfig

object SupabaseClientProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Storage)
        }
    }
}