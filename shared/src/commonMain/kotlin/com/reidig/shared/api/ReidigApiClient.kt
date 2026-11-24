package com.reidig.shared.api

import com.reidig.shared.ReidigException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Klien HTTP multiplatform berbasis Ktor 3.x untuk REST API REIDIG (`https://digital-v-101.lmrei.id`).
 */
class ReidigApiClient(
    val baseUrl: String = "https://digital-v-101.lmrei.id",
    private val clientFactory: () -> HttpClient = { defaultHttpClient() },
) {
    /** Token autentikasi JWT aktif pengguna. Disematkan otomatis pada header Authorization. */
    var accessToken: String? = null

    val client: HttpClient by lazy { clientFactory() }

    private suspend fun checkResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            val text = try { response.bodyAsText() } catch (e: Exception) { "" }
            var message: String? = null
            var code: String? = null

            try {
                val element = Json.parseToJsonElement(text)
                if (element is JsonObject) {
                    val errorObj = element["error"] as? JsonObject
                    code = errorObj?.get("code")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    message = errorObj?.get("message")?.let { (it as? JsonPrimitive)?.contentOrNull }

                    if (message == null) {
                        val detail = element["detail"]
                        message = when (detail) {
                            is JsonPrimitive -> detail.contentOrNull
                            is JsonArray -> detail.firstOrNull()?.let { (it as? JsonObject)?.get("msg")?.let { m -> (m as? JsonPrimitive)?.contentOrNull } }
                            else -> null
                        }
                    }
                }
            } catch (_: Exception) {}

            val finalMessage = message ?: if (text.isNotBlank()) text else "Permintaan gagal (${response.status.value})"
            val finalCode = code ?: response.status.value.toString()
            throw ReidigException(finalCode, finalMessage)
        }
    }

    // --- Autentikasi & Akun Endpoints ---

    /** Langkah 1: Minta kode 6 angka (OTP) ke email untuk REGISTER / RESET_PASSWORD. */
    suspend fun requestEmailCode(email: String, purpose: String = "REGISTER"): EmailCodeOut {
        val response = client.post("/v1/auth/email-code") {
            contentType(ContentType.Application.Json)
            setBody(EmailCodeIn(email = email, purpose = purpose))
        }
        checkResponse(response)
        return response.body()
    }

    /** Langkah 2: Verifikasi kode OTP dan dapatkan tiket sekali pakai (email_ticket). */
    suspend fun verifyEmailCode(email: String, code: String, purpose: String = "REGISTER"): EmailTicketOut {
        val response = client.post("/v1/auth/email-code/verify") {
            contentType(ContentType.Application.Json)
            setBody(EmailVerifyIn(email = email, purpose = purpose, code = code))
        }
        checkResponse(response)
        return response.body()
    }

    /** Langkah 3: Daftarkan akun baru dengan email_ticket dan langsung dapatkan access_token. */
    suspend fun register(data: RegisterIn): SessionOut {
        val response = client.post("/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(data)
        }
        checkResponse(response)
        val session: SessionOut = response.body()
        accessToken = session.accessToken
        return session
    }

    /** Masuk dengan email & kata sandi, mengembalikan access_token JWT. */
    suspend fun login(email: String, password: String): SessionOut {
        val response = client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginIn(email = email, password = password))
        }
        checkResponse(response)
        val session: SessionOut = response.body()
        accessToken = session.accessToken
        return session
    }

    /** Reset kata sandi menggunakan tiket verifikasi email. */
    suspend fun resetPassword(emailTicket: String, newPassword: String) {
        val response = client.post("/v1/auth/password-reset") {
            contentType(ContentType.Application.Json)
            setBody(PasswordResetIn(emailTicket = emailTicket, newPassword = newPassword))
        }
        checkResponse(response)
    }

    /** Ambil profil user yang sedang login saat ini. Membutuhkan access_token. */
    suspend fun getMe(): MeOut {
        val token = accessToken ?: throw ReidigException("UNAUTHENTICATED", "Sesi login tidak ditemukan.")
        val response = client.get("/v1/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        checkResponse(response)
        return response.body()
    }

    /** Pasang atau ubah 6 digit PIN keamanan. */
    suspend fun setPin(pin: String, oldPin: String? = null) {
        val token = accessToken ?: throw ReidigException("UNAUTHENTICATED", "Sesi login tidak ditemukan.")
        val response = client.put("/v1/me/pin") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(PinIn(pin = pin, oldPin = oldPin))
        }
        checkResponse(response)
    }

    /** Mengambil daftar rekening bank nasabah dari server. */
    suspend fun listBankAccounts(): List<BankAccountOut> {
        val token = accessToken ?: throw ReidigException("UNAUTHENTICATED", "Sesi login tidak ditemukan.")
        val response = client.get("/v1/me/bank-accounts") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        checkResponse(response)
        return response.body()
    }

    /** Mendaftarkan rekening bank baru ke akun pengguna di server. */
    suspend fun addBankAccount(bankCode: String, accountNumber: String, holderName: String): BankAccountOut {
        val token = accessToken ?: throw ReidigException("UNAUTHENTICATED", "Sesi login tidak ditemukan.")
        val response = client.post("/v1/me/bank-accounts") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(BankAccountIn(bankCode = bankCode, accountNumber = accountNumber, holderName = holderName))
        }
        checkResponse(response)
        return response.body()
    }

    /** Keluar dari sesi login. */
    suspend fun logout() {
        val token = accessToken
        if (token != null) {
            try {
                client.post("/v1/auth/logout") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            } catch (_: Exception) {}
        }
        accessToken = null
    }

    companion object {
        fun defaultHttpClient(baseUrl: String = "https://digital-v-101.lmrei.id"): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            coerceInputValues = true
                            encodeDefaults = true
                        }
                    )
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000L
                    connectTimeoutMillis = 15000L
                    socketTimeoutMillis = 15000L
                }
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.INFO
                }
                defaultRequest {
                    url(baseUrl.trimEnd('/') + "/")
                }
            }
        }
    }
}
