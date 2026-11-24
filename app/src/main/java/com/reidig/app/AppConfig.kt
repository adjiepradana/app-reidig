package com.reidig.app

import com.reidig.shared.MockRepository
import com.reidig.shared.api.ApiRepository
import com.reidig.shared.api.ReidigApiClient

/**
 * Konfigurasi lingkungan aplikasi Goldin.
 * Mengontrol saklar antara Live REST API Backend (`https://digital-v-101.lmrei.id`) dan Offline Mock.
 */
object AppConfig {
    /** Saklar utama: true = Live Backend API, false = Offline Mock */
    var useLiveApi: Boolean = true

    /** Base URL REST API Backend REIDIG */
    var backendUrl: String = "https://digital-v-101.lmrei.id"

    fun createRepository(live: Boolean = useLiveApi): MockRepository {
        return if (live) {
            ApiRepository(
                api = ReidigApiClient(baseUrl = backendUrl),
                clock = System::currentTimeMillis,
            )
        } else {
            MockRepository(clock = System::currentTimeMillis)
        }
    }
}
