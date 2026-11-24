package com.reidig.shared.api

import com.reidig.shared.AppState
import com.reidig.shared.BankAccount
import com.reidig.shared.EmailCodeResult
import com.reidig.shared.MockRepository
import com.reidig.shared.PinVerifyResult
import com.reidig.shared.ReidigException
import com.reidig.shared.Rules
import com.reidig.shared.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Implementasi repositori yang terhubung langsung ke Live REST API REIDIG (`https://digital-v-101.lmrei.id`).
 * Modul Autentikasi & Akun dikonsumsi secara live ke server.
 * Modul lain yang belum diintegrasikan otomatis dialihkan ke fallback lokal.
 */
class ApiRepository(
    val api: ReidigApiClient,
    clock: () -> Long = System::currentTimeMillis,
) : MockRepository(clock = clock) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Menyimpan tiket email sekali pakai hasil verifikasi OTP */
    var latestEmailTicket: String? = null

    init {
        // Jika token sudah ada (misal dari saved storage), muat profil dari server
        if (api.accessToken != null) {
            scope.launch {
                try {
                    refreshMe()
                } catch (_: Exception) {}
            }
        }
    }

    override fun login(email: String, password: String) {
        if (!Rules.isValidEmail(email) || password.isEmpty()) {
            throw ReidigException("INVALID_CREDENTIALS", "Email atau kata sandi salah")
        }
        val trimmed = email.trim()

        runBlocking {
            try {
                val session = api.login(trimmed, password)
                api.accessToken = session.accessToken
                refreshMe()
            } catch (e: ReidigException) {
                throw e
            } catch (e: Exception) {
                throw ReidigException("NETWORK_ERROR", e.message ?: "Koneksi ke server gagal")
            }
        }
    }

    override fun register(
        name: String,
        email: String,
        password: String,
        username: String,
        birthDate: String?,
    ) {
        val ticket = latestEmailTicket
        if (ticket != null) {
            runBlocking {
                registerWithTicket(
                    name = name,
                    email = email,
                    password = password,
                    emailTicket = ticket,
                    phone = null,
                    birthDate = birthDate,
                )
            }
        } else {
            super.register(name, email, password, username, birthDate)
        }
    }

    override suspend fun requestEmailCode(email: String, purpose: String): EmailCodeResult {
        return try {
            val res = api.requestEmailCode(email.trim(), purpose)
            EmailCodeResult(
                expiresInSeconds = res.expiresInSeconds,
                resendInSeconds = res.resendInSeconds,
                devCode = res.devCode,
            )
        } catch (e: ReidigException) {
            throw e
        } catch (e: Exception) {
            throw ReidigException("NETWORK_ERROR", e.message ?: "Gagal meminta kode OTP")
        }
    }

    override suspend fun verifyEmailCode(email: String, code: String, purpose: String): String {
        return try {
            val res = api.verifyEmailCode(email.trim(), code.trim(), purpose)
            latestEmailTicket = res.emailTicket
            res.emailTicket
        } catch (e: ReidigException) {
            throw e
        } catch (e: Exception) {
            throw ReidigException("NETWORK_ERROR", e.message ?: "Kode verifikasi salah atau kedaluwarsa")
        }
    }

    override suspend fun registerWithTicket(
        name: String,
        email: String,
        password: String,
        emailTicket: String,
        phone: String?,
        birthDate: String?,
    ) {
        val isoDob = birthDate?.let { dob ->
            val parts = dob.split("/", "-")
            if (parts.size == 3 && parts[0].length <= 2) {
                val d = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                val y = parts[2]
                "$y-$m-$d"
            } else dob
        }
        val cleanPhone = phone?.filter { it.isDigit() }?.trimStart('0')?.ifBlank { null }
        try {
            val session = api.register(
                RegisterIn(
                    emailTicket = emailTicket,
                    name = name.trim(),
                    email = email.trim(),
                    password = password,
                    phone = cleanPhone,
                    dateOfBirth = isoDob,
                )
            )
            api.accessToken = session.accessToken
            refreshMe()
        } catch (e: ReidigException) {
            throw e
        } catch (e: Exception) {
            throw ReidigException("NETWORK_ERROR", e.message ?: "Pendaftaran gagal")
        }
    }

    override fun isRegisteredEmail(email: String): Boolean {
        // Pada mode Live API, izinkan semua email dengan format valid
        return Rules.isValidEmail(email)
    }

    override fun resetPassword(email: String, newPassword: String) {
        val ticket = latestEmailTicket
        if (ticket != null) {
            runBlocking {
                try {
                    api.resetPassword(ticket, newPassword)
                } catch (e: ReidigException) {
                    throw e
                } catch (e: Exception) {
                    throw ReidigException("NETWORK_ERROR", e.message ?: "Gagal mereset kata sandi")
                }
            }
        }
        super.resetPassword(email, newPassword)
    }

    override fun resetPin(newPin: String) {
        super.resetPin(newPin)
        if (api.accessToken != null) {
            scope.launch {
                try {
                    api.setPin(newPin)
                } catch (_: Exception) {}
            }
        }
    }

    override fun unlockAccountWithResetPin(newPin: String) {
        resetPin(newPin)
    }

    override fun verifyAndAddBankAccount(bank: String, number: String, holder: String): BankAccount {
        val addedLocal = super.verifyAndAddBankAccount(bank, number, holder)
        if (api.accessToken != null) {
            scope.launch {
                try {
                    api.addBankAccount(bank, number, holder)
                } catch (_: Exception) {}
            }
        }
        return addedLocal
    }

    override suspend fun setPinApi(pin: String, oldPin: String?) {
        try {
            api.setPin(pin, oldPin)
            _state.update {
                it.copy(
                    pin = pin,
                    pinFailedAttempts = 0,
                    pinLockLevel = 0,
                    isAccountLocked = false,
                )
            }
        } catch (e: ReidigException) {
            throw e
        } catch (e: Exception) {
            throw ReidigException("NETWORK_ERROR", e.message ?: "Gagal menyimpan PIN")
        }
    }

    override suspend fun refreshMe() {
        val me = api.getMe()
        val bankList = try {
            api.listBankAccounts()
        } catch (_: Exception) {
            emptyList()
        }

        _state.update { curr ->
            val updatedBanks = if (bankList.isNotEmpty()) {
                bankList.mapIndexed { idx, b ->
                    BankAccount(
                        id = b.id.toString(),
                        bank = b.bankCode,
                        number = b.accountNumberMasked,
                        holder = b.holderName,
                        isPrimary = idx == 0,
                        isAutoDebitActive = idx == 0,
                    )
                }
            } else {
                curr.bankAccounts
            }

            curr.copy(
                user = UserProfile(
                    name = me.name,
                    email = me.email,
                    phone = me.phone,
                    birthDate = me.dateOfBirth,
                    verified = me.verified,
                    isKycPending = me.kycStatus == "PENDING" || me.kycStatus == "SUBMITTED",
                    username = me.email.substringBefore('@').ifBlank { "reiuser" },
                ),
                bankAccounts = updatedBanks,
                isAccountLocked = me.pinLocked,
                isFirstTimeLogin = false,
            )
        }
    }

    override fun logout() {
        runBlocking {
            try {
                api.logout()
            } catch (_: Exception) {}
        }
        latestEmailTicket = null
        super.logout()
    }
}
