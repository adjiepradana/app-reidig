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
import io.ktor.client.request.parameter
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

    // --- Modul Harga & Grafik Emas Endpoints ---

    /** Mengambil harga emas 1 gram terkini dari live backend. */
    suspend fun getOneGramPrice(): PriceOut {
        val token = accessToken
        val response = client.get("/v1/prices/1g") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /** Mengambil riwayat harga emas 1 gram dari live backend. */
    suspend fun getOneGramPriceHistory(limit: Int = 500): List<PriceOut> {
        val token = accessToken
        val response = client.get("/v1/prices/1g/history") {
            parameter("limit", limit)
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Mengambil katalog SKU emas fisik lengkap beserta harga dan stok aktif (`GET /v1/catalog/skus`).
     */
    suspend fun getCatalogSkus(): CatalogOut {
        val token = accessToken
        val response = client.get("/v1/catalog/skus") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Mengambil daftar cabang resmi Raja Emas (`GET /v1/branches`).
     */
    suspend fun getBranches(): List<BranchOut> {
        val token = accessToken
        val response = client.get("/v1/branches") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /** Mengambil saldo kas dompet aktif pengguna (`GET /v1/wallet`). */
    suspend fun getWallet(): WalletOut {
        val token = accessToken
        val response = client.get("/v1/wallet") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /** Mengambil daftar mutasi / entri buku kas dompet (`GET /v1/wallet/entries`). */
    suspend fun getWalletEntries(): List<EntryOut> {
        val token = accessToken
        val response = client.get("/v1/wallet/entries") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /** Menambahkan saldo kas dompet (`POST /v1/wallet/topups`). */
    suspend fun topupWallet(amount: Long): WalletOut {
        val token = accessToken
        val response = client.post("/v1/wallet/topups") {
            contentType(ContentType.Application.Json)
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(TopupIn(amount = amount.toString()))
        }
        checkResponse(response)
        return response.body()
    }

    /** Mengajukan penarikan dana ke rekening bank (`POST /v1/wallet/withdrawals`). */
    suspend fun withdraw(
        amount: Long,
        bankAccountId: Int,
        pin: String,
        idempotencyKey: String? = null,
    ): WithdrawalOut {
        val token = accessToken
        val response = client.post("/v1/wallet/withdrawals") {
            contentType(ContentType.Application.Json)
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (idempotencyKey != null) {
                header("Idempotency-Key", idempotencyKey)
            }
            setBody(WithdrawIn(amount = amount.toString(), bankAccountId = bankAccountId, pin = pin))
        }
        checkResponse(response)
        return response.body()
    }

    /** Mengambil daftar riwayat penarikan dana (`GET /v1/wallet/withdrawals`). */
    suspend fun getWithdrawals(): List<WithdrawalOut> {
        val token = accessToken
        val response = client.get("/v1/wallet/withdrawals") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    // --- Modul 5: Trading Emas (Quotes, Orders & Tokens) Endpoints ---

    /**
     * Mengunci harga beli atau jual emas dalam bentuk kuotasi (`POST /v1/quotes`).
     */
    suspend fun createQuote(skuCode: String, quantity: Int = 1, side: String = "BUY"): QuoteOut {
        val token = accessToken
        val response = client.post("/v1/quotes") {
            contentType(ContentType.Application.Json)
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(QuoteIn(skuCode = skuCode, quantity = quantity, side = side))
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Mengeksekusi order pembelian emas berdasarkan ID kuotasi (`POST /v1/orders/buy`).
     */
    suspend fun buyOrder(
        quoteId: String,
        paymentMethod: String,
        idempotencyKey: String? = null,
    ): OrderOut {
        val token = accessToken
        val response = client.post("/v1/orders/buy") {
            contentType(ContentType.Application.Json)
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (idempotencyKey != null) {
                header("Idempotency-Key", idempotencyKey)
            }
            setBody(BuyIn(quoteId = quoteId, paymentMethod = paymentMethod))
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Mengeksekusi order penjualan (buyback) emas berdasarkan ID kuotasi dan token emas (`POST /v1/orders/sell`).
     */
    suspend fun sellOrder(
        quoteId: String,
        tokenCodes: List<String>,
        pin: String,
        idempotencyKey: String? = null,
    ): OrderOut {
        val token = accessToken
        val response = client.post("/v1/orders/sell") {
            contentType(ContentType.Application.Json)
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (idempotencyKey != null) {
                header("Idempotency-Key", idempotencyKey)
            }
            setBody(SellIn(quoteId = quoteId, tokenCodes = tokenCodes, pin = pin))
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Mengambil daftar seluruh riwayat order pembelian dan penjualan (`GET /v1/orders`).
     */
    suspend fun getOrders(): List<OrderOut> {
        val token = accessToken
        val response = client.get("/v1/orders") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Mengambil detail satu pesanan order (`GET /v1/orders/{order_id}`).
     */
    suspend fun getOrder(orderId: Int): OrderOut {
        val token = accessToken
        val response = client.get("/v1/orders/$orderId") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Membatalkan pesanan order yang berstatus menunggu pembayaran (`POST /v1/orders/{order_id}/cancel`).
     */
    suspend fun cancelOrder(orderId: Int): OrderOut {
        val token = accessToken
        val response = client.post("/v1/orders/$orderId/cancel") {
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        checkResponse(response)
        return response.body()
    }

    /**
     * Mengambil daftar kepemilikan kepingan emas digital nasabah (`GET /v1/tokens`).
     */
    suspend fun getTokens(includeClosed: Boolean = false): List<TokenOut> {
        val token = accessToken
        val response = client.get("/v1/tokens") {
            parameter("include_closed", includeClosed)
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
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
