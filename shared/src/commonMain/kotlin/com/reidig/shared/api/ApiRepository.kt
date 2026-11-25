package com.reidig.shared.api

import com.reidig.shared.AppState
import com.reidig.shared.BankAccount
import com.reidig.shared.Branch
import com.reidig.shared.EmailCodeResult
import com.reidig.shared.GoldBranch
import com.reidig.shared.MockRepository
import com.reidig.shared.PaymentMethod
import com.reidig.shared.PinVerifyResult
import com.reidig.shared.ReidigException
import com.reidig.shared.Rules
import com.reidig.shared.Sku
import com.reidig.shared.Tx
import com.reidig.shared.TxType
import com.reidig.shared.UserProfile
import com.reidig.shared.WITHDRAW_FEE
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

    /** Menyimpan PIN yang baru saja diverifikasi pengguna untuk otorisasi penarikan dana live */
    var lastVerifiedPin: String? = null

    init {
        // Muat katalog SKU dan cabang resmi dari server secara asynchronous (public endpoints)
        scope.launch {
            try {
                fetchCatalogSkus()
                fetchBranches()
            } catch (_: Exception) {}
        }
        // Jika token sudah ada (misal dari saved storage), muat profil, harga emas, dan saldo kas dari server
        if (api.accessToken != null) {
            scope.launch {
                try {
                    refreshMe()
                    fetchGoldPrice()
                    fetchWallet()
                    fetchWalletEntries()
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
                fetchGoldPrice()
                fetchCatalogSkus()
                fetchBranches()
                fetchWallet()
                fetchWalletEntries()
            } catch (e: ReidigException) {
                throw e
            } catch (e: Exception) {
                throw ReidigException("NETWORK_ERROR", e.message ?: "Koneksi ke server gagal")
            }
        }
    }

    override fun verifyPin(pin: String): Boolean {
        val ok = super.verifyPin(pin)
        if (ok) lastVerifiedPin = pin
        return ok
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
        lastVerifiedPin = newPin
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
                    isKycPending = (me.kycStatus == "PENDING" || me.kycStatus == "SUBMITTED"),
                    username = me.email.substringBefore('@').ifBlank { "pengguna" },
                ),
                bankAccounts = if (updatedBanks.isNotEmpty()) updatedBanks else curr.bankAccounts,
                isAccountLocked = me.pinLocked,
                isFirstTimeLogin = false,
            )
        }

        try {
            fetchGoldPrice()
            fetchWallet()
            fetchWalletEntries()
        } catch (_: Exception) {}
    }

    // --- Modul Harga & Grafik Emas ---

    private fun parseIsoTimeToWib(isoString: String): String {
        try {
            val tIndex = isoString.indexOf('T')
            if (tIndex != -1 && isoString.length >= tIndex + 6) {
                val hourStr = isoString.substring(tIndex + 1, tIndex + 3)
                val minuteStr = isoString.substring(tIndex + 4, tIndex + 6)
                val utcHour = hourStr.toIntOrNull() ?: return "12.30 WIB"
                val wibHour = (utcHour + 7) % 24
                val hh = wibHour.toString().padStart(2, '0')
                return "$hh.$minuteStr WIB"
            }
        } catch (_: Exception) {}
        return "12.30 WIB"
    }

    override suspend fun fetchGoldPrice(): PriceOut? {
        val token = api.accessToken ?: return null
        return try {
            val priceOut = api.getOneGramPrice()
            val buyPriceLong = priceOut.buyPrice.toLongOrNull() ?: _state.value.askPricePerGram
            val buybackPriceLong = priceOut.buybackPrice.toLongOrNull() ?: _state.value.buybackPerGram
            val timeLabel = parseIsoTimeToWib(priceOut.fetchedAt)

            _state.update { curr ->
                val prevAsk = curr.askPricePerGram
                val flash = when {
                    buyPriceLong > prevAsk -> "up"
                    buyPriceLong < prevAsk -> "down"
                    else -> curr.priceFlash
                }
                val updatedSkus = curr.skus.map { sku ->
                    if (sku.code == priceOut.skuCode || sku.code == "LMRE-1G" || curr.skus.size == 1) {
                        sku.copy(buyPrice = buyPriceLong, buybackPrice = buybackPriceLong)
                    } else sku
                }
                curr.copy(
                    askPricePerGram = buyPriceLong,
                    buybackPerGram = buybackPriceLong,
                    priceUpdatedAtLabel = timeLabel,
                    priceFlash = flash,
                    skus = updatedSkus,
                )
            }
            priceOut
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchGoldPriceHistory(limit: Int): List<PriceOut> {
        val token = api.accessToken ?: return emptyList()
        return try {
            api.getOneGramPriceHistory(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun refreshGoldPrice(formattedHm: String?, deltaOverride: Long?): String? {
        if (api.accessToken != null) {
            return runBlocking {
                try {
                    val priceOut = fetchGoldPrice()
                    if (priceOut != null) {
                        _state.value.priceFlash
                    } else {
                        super.refreshGoldPrice(formattedHm, deltaOverride)
                    }
                } catch (_: Exception) {
                    super.refreshGoldPrice(formattedHm, deltaOverride)
                }
            }
        }
        return super.refreshGoldPrice(formattedHm, deltaOverride)
    }

    override suspend fun fetchCatalogSkus(): CatalogOut? {
        return try {
            val catalog = api.getCatalogSkus()
            if (catalog.skus.isNotEmpty()) {
                val mappedSkus = catalog.skus.map { item ->
                    Sku(
                        code = item.code,
                        product = item.product,
                        weightMg = item.weightMg,
                        buyPrice = item.buyPrice?.toLongOrNull() ?: 0L,
                        buybackPrice = item.buybackPrice?.toLongOrNull() ?: 0L,
                        stock = item.stock,
                        brand = item.brand,
                    )
                }
                _state.update { curr ->
                    val oneGram = mappedSkus.firstOrNull { it.code == "LMRE-1G" || it.weightMg == 1000L }
                    val newAsk = if (oneGram != null && oneGram.buyPrice > 0L) oneGram.buyPrice else curr.askPricePerGram
                    val newBid = if (oneGram != null && oneGram.buybackPrice > 0L) oneGram.buybackPrice else curr.buybackPerGram
                    curr.copy(
                        skus = mappedSkus,
                        askPricePerGram = newAsk,
                        buybackPerGram = newBid,
                    )
                }
            }
            catalog
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchBranches(): List<BranchOut> {
        return try {
            val remoteBranches = api.getBranches()
            if (remoteBranches.isNotEmpty()) {
                val mappedGoldBranches = remoteBranches.map { remote ->
                    val codeLower = remote.code.lowercase()
                    val existing = officialGoldBranches.firstOrNull {
                        it.id.equals(codeLower, ignoreCase = true) ||
                        it.name.contains(remote.name, ignoreCase = true)
                    }
                    val fullName = if (remote.name.startsWith("Raja Emas", ignoreCase = true)) {
                        remote.name
                    } else {
                        "Raja Emas ${remote.name}"
                    }
                    if (existing != null) {
                        existing.copy(
                            id = codeLower,
                            name = fullName,
                            city = remote.area ?: existing.city,
                        )
                    } else {
                        GoldBranch(
                            id = codeLower,
                            name = fullName,
                            address = "Jl. Cabang ${remote.name}${if (!remote.area.isNullOrBlank()) ", " + remote.area else ""}",
                            city = remote.area ?: "Indonesia",
                            province = remote.area ?: "Indonesia",
                            distanceKm = 3.5,
                            openingHours = "Senin–Sabtu, 09.00–17.00 WIB",
                            isSevenDays = false,
                            phone = "021-55667788",
                        )
                    }
                }
                officialGoldBranches = mappedGoldBranches

                val mappedBranches = remoteBranches.map { remote ->
                    val codeLower = remote.code.lowercase()
                    val existing = branches.firstOrNull {
                        it.id.equals(codeLower, ignoreCase = true) ||
                        it.name.contains(remote.name, ignoreCase = true)
                    }
                    if (existing != null) {
                        existing.copy(
                            id = codeLower,
                            name = remote.name,
                            area = remote.area ?: existing.area,
                            estimate = remote.pickupEstimate ?: existing.estimate,
                        )
                    } else {
                        Branch(
                            id = codeLower,
                            name = remote.name,
                            area = remote.area ?: "",
                            estimate = remote.pickupEstimate ?: "1–2 hari kerja",
                            city = remote.area ?: "",
                        )
                    }
                }
                branches = mappedBranches
            }
            remoteBranches
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Modul Saldo Kas & Dompet (Wallet) ---

    override suspend fun fetchWallet(): WalletOut? {
        val token = api.accessToken ?: return null
        return try {
            val wallet = api.getWallet()
            val availableLong = wallet.available.toLongOrNull() ?: _state.value.walletBalance
            val heldLong = wallet.held.toLongOrNull() ?: 0L
            _state.update { curr ->
                curr.copy(
                    walletBalance = availableLong,
                    walletHeldBalance = heldLong,
                )
            }
            wallet
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchWalletEntries(): List<EntryOut> {
        val token = api.accessToken ?: return emptyList()
        return try {
            val entries = api.getWalletEntries()
            if (entries.isNotEmpty()) {
                val mappedTxs = entries.map { entry ->
                    val amt = entry.amount.toLongOrNull() ?: 0L
                    val isPositive = amt >= 0L
                    val absAmt = if (isPositive) amt else -amt
                    val type = when (entry.type.uppercase()) {
                        "TOPUP", "DEPOSIT" -> TxType.TOPUP
                        "WITHDRAWAL", "WITHDRAW" -> TxType.WITHDRAW
                        "BUY" -> TxType.BUY
                        "SELL" -> TxType.SELL
                        else -> if (isPositive) TxType.TOPUP else TxType.WITHDRAW
                    }
                    val title = when (type) {
                        TxType.TOPUP -> "Top up Saldo Kas"
                        TxType.WITHDRAW -> "Tarik Dana"
                        TxType.BUY -> "Beli Emas"
                        TxType.SELL -> "Jual Emas"
                        else -> entry.type
                    }
                    Tx(
                        id = "ENTRY-${entry.id}",
                        type = type,
                        title = title,
                        amount = amt,
                        subtotal = absAmt,
                        fee = 0L,
                        total = absAmt,
                        atMs = clock(),
                        status = "ok",
                        payRef = entry.referenceId ?: "REF-${entry.id}",
                    )
                }
                _state.update { curr ->
                    val existingIds = curr.transactions.map { it.id }.toSet()
                    val newEntries = mappedTxs.filter { it.id !in existingIds }
                    curr.copy(transactions = newEntries + curr.transactions)
                }
            }
            entries
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchWithdrawals(): List<WithdrawalOut> {
        val token = api.accessToken ?: return emptyList()
        return try {
            api.getWithdrawals()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun requestWithdrawal(amount: Long, bankAccountId: String, pin: String): Tx {
        if (api.accessToken != null) {
            val effectivePin = pin.ifBlank { lastVerifiedPin ?: MockRepository.GOLDIN_DEMO_PIN }
            val accountIdInt = bankAccountId.toIntOrNull()
                ?: _state.value.bankAccounts.firstOrNull { it.id == bankAccountId }?.id?.toIntOrNull()
                ?: 1

            return runBlocking {
                try {
                    val res = api.withdraw(
                        amount = amount,
                        bankAccountId = accountIdInt,
                        pin = effectivePin,
                        idempotencyKey = "wd-${clock()}-${amount}",
                    )
                    fetchWallet()
                    val now = clock()
                    val tx = Tx(
                        id = "WDR-${res.id}",
                        type = TxType.WITHDRAW,
                        title = "Tarik Dana",
                        amount = -amount,
                        subtotal = amount,
                        fee = WITHDRAW_FEE,
                        total = amount + WITHDRAW_FEE,
                        atMs = now,
                        status = res.status.lowercase(),
                        method = "${res.bankCode} ${res.accountNumberMasked}",
                        bankAccount = "${res.bankCode} ${res.accountNumberMasked}",
                        payRef = "WDR-${res.id}",
                    )
                    _state.update { s ->
                        s.copy(transactions = listOf(tx) + s.transactions)
                    }
                    tx
                } catch (e: ReidigException) {
                    throw e
                } catch (e: Exception) {
                    super.requestWithdrawal(amount, bankAccountId, pin)
                }
            }
        }
        return super.requestWithdrawal(amount, bankAccountId, pin)
    }

    override fun topup(amount: Long, method: PaymentMethod): Tx {
        if (api.accessToken != null) {
            return runBlocking {
                try {
                    val walletOut = api.topupWallet(amount)
                    val availableLong = walletOut.available.toLongOrNull()
                    val tx = super.topup(amount, method)
                    if (availableLong != null) {
                        _state.update { it.copy(walletBalance = availableLong) }
                    }
                    tx
                } catch (_: Exception) {
                    super.topup(amount, method)
                }
            }
        }
        return super.topup(amount, method)
    }

    override fun logout() {
        runBlocking {
            try {
                api.logout()
            } catch (_: Exception) {}
        }
        latestEmailTicket = null
        lastVerifiedPin = null
        super.logout()
    }
}
