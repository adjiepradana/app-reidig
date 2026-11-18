package com.reidig.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * Backend tiruan di dalam aplikasi Goldin v1.2.
 * Diganti klien HTTP saat API siap; layar hanya bergantung pada fungsi-fungsi ini.
 */
class MockRepository(
    private val clock: () -> Long,
    private val random: Random = Random.Default,
) {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val quotes = mutableMapOf<String, Quote>()
    private val pending = mutableMapOf<String, PendingOrder>()
    private val applications = mutableMapOf<String, InstallmentApplication>()
    private var pinAttempts = 0
    private var serialCounter = 500_000
    private var idCounter = 1

    val branches = listOf(
        Branch("klg", "Kelapa Gading", "Jakarta Utara", "2–3 hari kerja", "Jakarta Utara", "DKI Jakarta", "Jl. Boulevard Barat Raya Blok LA No. 9", 21.0, "Senin–Sabtu, 10.00–20.00 WIB"),
        Branch("cgk", "Cengkareng", "Jakarta Barat", "2–3 hari kerja", "Jakarta Barat", "DKI Jakarta", "Jl. Daan Mogot KM 14", 15.0, "Senin–Sabtu, 09.00–20.00 WIB"),
        Branch("ptj", "Petojo", "Jakarta Pusat", "1–2 hari kerja", "Jakarta Pusat", "DKI Jakarta", "Jl. Suryopranoto No. 22", 10.0, "Senin–Sabtu, 09.00–20.00 WIB"),
        Branch("amb", "Mal Ambasador Kuningan", "Jakarta Selatan", "1–2 hari kerja", "Jakarta Selatan", "DKI Jakarta", "Mal Ambasador Lt. 2 No. 15", 14.0, "Senin–Minggu, 10.00–21.00 WIB"),
        Branch("krw", "Karawaci", "Tangerang", "1–2 hari kerja", "Kota Tangerang", "Banten", "Jl. Imam Bonjol No. 45, Karawaci", 2.1, "Senin–Sabtu, 09.00–20.00 WIB"),
        Branch("cpd", "Cipondoh", "Tangerang", "1–2 hari kerja", "Kota Tangerang", "Banten", "Jl. KH Hasyim Ashari No. 88, Cipondoh", 4.3, "Senin–Sabtu, 09.00–20.00 WIB"),
        Branch("bsd", "BSD", "Tangerang Selatan", "1–2 hari kerja", "Kota Tangerang Selatan", "Banten", "Ruko Golden Boulevard Blok C No. 12, BSD", 7.8, "Senin–Minggu, 09.00–21.00 WIB"),
        Branch("kbj", "Kebon Jeruk", "Jakarta Barat", "1–2 hari kerja", "Jakarta Barat", "DKI Jakarta", "Jl. Panjang No. 15, Kebon Jeruk", 12.4, "Senin–Sabtu, 09.00–20.00 WIB"),
        Branch("blm", "Blok M", "Jakarta Selatan", "1–2 hari kerja", "Jakarta Selatan", "DKI Jakarta", "Blok M Plaza Lt. 1 No. 18", 15.6, "Senin–Minggu, 10.00–21.00 WIB"),
        Branch("bks", "Bekasi", "Bekasi", "2–3 hari kerja", "Kota Bekasi", "Jawa Barat", "Ruko Sinpasa Commercial Blok A-05, Summarecon Bekasi", 28.5, "Senin–Sabtu, 09.00–20.00 WIB"),
        Branch("bgr", "Bogor", "Bogor", "2–3 hari kerja", "Kota Bogor", "Jawa Barat", "Jl. Pajajaran No. 32, Bogor Tengah", 35.2, "Senin–Sabtu, 09.00–20.00 WIB"),
    )

    private fun nextId(prefix: String) = "$prefix-${idCounter++}"

    private fun fail(code: String, message: String): Nothing = throw ReidigException(code, message)

    private fun tokenCode(sku: Sku): String {
        val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        val suffix = (1..11).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        return "RET-${sku.code.replace("-", "").replace(".", "")}-$suffix"
    }

    private fun newSerial(sku: Sku) = "${sku.code.replace("-", "").replace(".", "")}-${serialCounter++}"

    // ----- Akun -----

    /** Akun contoh yang sudah terverifikasi, sesuai prototipe Goldin v1.2 (reiuser@gmail.com). */
    fun login(email: String, password: String) {
        if (!Rules.isValidEmail(email) || password.isEmpty()) {
            fail("INVALID_CREDENTIALS", "Email atau kata sandi salah")
        }
        val trimmed = email.trim()
        val isDemoEmail = trimmed.equals(GOLDIN_DEMO_EMAIL, ignoreCase = true)
        val isRegisteredEmail = trimmed.equals(_state.value.user?.email, ignoreCase = true)
        if (isDemoEmail || isRegisteredEmail) {
            val expectedPw = _state.value.registeredPassword
            if (password != expectedPw) {
                fail("INVALID_CREDENTIALS", "Email atau kata sandi salah")
            }
        }
        startDemoAccount(trimmed)
    }

    fun loginWithGoogle() = startDemoAccount(GOLDIN_DEMO_EMAIL)

    fun startDemoAccount(email: String = "reiuser@gmail.com") {
        val now = clock()
        val oneDayAgo = now - 24L * 3600 * 1000
        val fourDaysAgo = now - 4L * 24L * 3600 * 1000
        val nineDaysAgo = now - 9L * 24L * 3600 * 1000
        val twelveDaysAgo = now - 12L * 24L * 3600 * 1000

        val seededTx = listOf(
            Tx(
                id = "TRX-A8",
                type = TxType.BUY,
                title = "Beli Emas",
                amount = -250_000L,
                atMs = oneDayAgo,
                status = "ok",
                method = "Saldo Kas",
                grams = 0.1087,
                pricePerGram = 2_298_000L,
                fee = 1_000L,
                tax = 625L,
                bursaRef = "BRS-261116-482019",
                kliringRef = "KLR-261116-891042",
            ),
            Tx(
                id = "TRX-A7",
                type = TxType.TOPUP,
                title = "Top up Saldo Kas",
                amount = 500_000L,
                atMs = oneDayAgo - 12 * 60 * 1000,
                status = "ok",
                method = "VA BCA",
                fee = 1_000L,
                payRef = "PAY-261116-102948",
            ),
            Tx(
                id = "TRX-A6",
                type = TxType.SELL,
                title = "Jual Emas",
                amount = 150_000L,
                atMs = fourDaysAgo,
                status = "ok",
                method = "Saldo Kas",
                grams = 0.0669,
                pricePerGram = 2_241_000L,
                fee = 1_000L,
                tax = 375L,
                bursaRef = "BRS-261113-192847",
                kliringRef = "KLR-261113-581930",
            ),
            Tx(
                id = "TRX-A5",
                type = TxType.BUY,
                title = "Beli Emas",
                amount = -100_000L,
                atMs = nineDaysAgo,
                status = "fail",
                method = "VA Mandiri",
                grams = 0.0439,
                pricePerGram = 2_276_000L,
            ),
            Tx(
                id = "TRX-A4",
                type = TxType.WITHDRAW,
                title = "Tarik Dana",
                amount = -300_000L,
                atMs = twelveDaysAgo,
                status = "ok",
                method = "BCA ••••1234",
                fee = 2_500L,
                payRef = "PAY-261105-301924",
            ),
        )

        val seededRoutineMutations = listOf(
            RoutineMutation("RM-3", "P0", now - 60L * 24L * 3600 * 1000, 0.0434, 100_000L, 2_302_000L, "Tabungan rutin", "TRX-R3"),
            RoutineMutation("RM-2", "P0", now - 30L * 24L * 3600 * 1000, 0.0440, 100_000L, 2_271_000L, "Tabungan rutin", "TRX-R2"),
            RoutineMutation("RM-1", "P0", now - 2L * 24L * 3600 * 1000, 0.0445, 100_000L, 2_248_000L, "Tabungan rutin", "TRX-R1"),
        )

        val seededPlan = RoutineSavingsPlan(
            id = "P0",
            name = "Dana darurat",
            type = RoutinePlanType.TARGET_EMAS,
            targetMonths = 0,
            targetGrams = 1.0,
            monthlyAmount = 100_000L,
            debitDate = 5,
            savedGrams = 0.1319,
            fundingSource = "kas",
            status = RoutinePlanStatus.ACTIVE,
            createdAtMs = now - 90L * 24L * 3600 * 1000,
            mutations = seededRoutineMutations,
        )

        val seededNotifications = listOf(
            NotificationItem("notif-1", NotificationCategory.TRANSACTION, "Pembelian emas berhasil", "0,1087 gr emas sudah masuk ke akunmu.", oneDayAgo, isRead = false, txId = "TRX-A8"),
            NotificationItem("notif-2", NotificationCategory.TRANSACTION, "Top up berhasil", "Rp500.000 sudah masuk ke Saldo Kas.", oneDayAgo - 12 * 60 * 1000, isRead = true, txId = "TRX-A7"),
            NotificationItem("notif-3", NotificationCategory.TRANSACTION, "Penjualan emas berhasil", "Rp150.000 dari penjualan 0,0669 gr emas sudah masuk ke Saldo Kas.", fourDaysAgo, isRead = true, txId = "TRX-A6"),
            NotificationItem("notif-4", NotificationCategory.TRANSACTION, "Penarikan dana berhasil", "Rp300.000 sudah masuk ke rekening BCA ••••1234.", twelveDaysAgo, isRead = true, txId = "TRX-A4"),
            NotificationItem("notif-5", NotificationCategory.ANNOUNCEMENT, "Verifikasi berhasil", "Akunmu sudah terverifikasi. Sekarang kamu bisa mulai transaksi.", now - 5L * 3600 * 1000, isRead = false),
            NotificationItem("notif-6", NotificationCategory.ANNOUNCEMENT, "Promo emas pertama", "Cashback Rp10.000 untuk pembelian emas minimal Rp100.000. Berlaku sampai 30 November.", now - 2L * 24L * 3600 * 1000, isRead = false),
            NotificationItem("notif-7", NotificationCategory.ANNOUNCEMENT, "Versi baru tersedia", "Perbarui Goldin untuk fitur dan perbaikan terbaru.", now - 6L * 24L * 3600 * 1000, isRead = true),
            NotificationItem("notif-8", NotificationCategory.ANNOUNCEMENT, "Pemeliharaan sistem", "Transaksi tidak tersedia pada 20 November, 00.00–03.00 WIB.", now - 10L * 24L * 3600 * 1000, isRead = true),
        )

        val seededAddress = SavedAddress(
            id = "addr-1",
            label = "Rumah",
            recipientName = "Budi Santoso",
            phoneNumber = "081234567890",
            province = "Banten",
            city = "Kota Tangerang",
            district = "Karawaci",
            subDistrict = "Bugel",
            postalCode = "15113",
            details = "Vila Tangerang Elok A-19/10, RT 008 / RW 007",
            isPrimary = true,
        )

        val seededAccount = BankAccount("bca-1", "BCA", "•••• 1234", "BUDI SANTOSO", isAutoDebitActive = true)

        _state.update { current ->
            val oneGram = current.sku("LMRE-1G")
            val halfGram = current.sku("LMRE-0.5G")
            val tokens = listOf(oneGram, oneGram, halfGram).map { sku ->
                GoldToken(tokenCode(sku), sku.code, newSerial(sku), TokenStatus.ACTIVE, sku.buyPrice, oneDayAgo)
            }
            current.copy(
                user = UserProfile(
                    name = "Budi Santoso",
                    email = email,
                    verified = true,
                    isKycPending = false,
                    phone = "081234567890",
                    nik = "3173012345670012",
                    birthDate = "14/03/1992",
                ),
                pin = GOLDIN_DEMO_PIN,
                walletBalance = 1_250_000L,
                goldBalanceGrams = 2.4518,
                lockedGoldGrams = 0.0,
                avgBuyPricePerGram = 2_250_000L,
                askPricePerGram = 2_315_000L,
                buybackPerGram = 2_257_500L,
                priceBaseAskPerGram = 2_287_500L,
                priceUpdatedAtMs = now,
                hideHomeBalance = true,
                hidePortfolioGold = true,
                hidePortfolioCash = true,
                tokens = tokens,
                transactions = seededTx,
                bankAccounts = listOf(seededAccount),
                savedAddresses = listOf(seededAddress),
                routinePlans = listOf(seededPlan),
                notifications = seededNotifications,
                isAccountLocked = false,
                pinLockLevel = 0,
                pinFailedAttempts = 0,
            )
        }
    }

    fun register(
        name: String,
        email: String,
        password: String = GOLDIN_DEMO_PASSWORD,
        username: String = "",
        birthDate: String? = null,
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanUsername = Rules.normalizeUsername(username).ifBlank {
            cleanEmail.substringBefore('@').ifBlank { "reiuser" }
        }
        _state.update {
            it.copy(
                user = UserProfile(
                    name = cleanName,
                    email = cleanEmail,
                    verified = false,
                    isKycPending = false,
                    birthDate = birthDate,
                    username = cleanUsername,
                ),
                pin = null,
                registeredPassword = password,
                isAccountLocked = false,
                pinLockLevel = 0,
                pinFailedAttempts = 0,
                isFirstTimeLogin = true,
                walletBalance = 0,
                goldBalanceGrams = 0.0,
                lockedGoldGrams = 0.0,
                tokens = emptyList(),
                transactions = emptyList(),
                bankAccounts = emptyList(),
                routinePlans = emptyList(),
                savedAddresses = emptyList(),
                notifications = emptyList(),
            )
        }
    }

    fun logout() {
        pinAttempts = 0
        pending.clear()
        applications.clear()
        _state.value = initialState()
    }

    fun completeKyc(pin: String, name: String) {
        if (Rules.isWeakPin(pin)) fail("WEAK_PIN", "PIN terlalu mudah ditebak. Hindari angka berulang dan berurutan.")
        _state.update { state ->
            state.copy(
                pin = pin,
                user = state.user?.copy(verified = true, isKycPending = false),
                bankAccounts = state.bankAccounts.ifEmpty { listOf(BankAccount("bca-1", "BCA", "•••• 1234", name.trim(), isAutoDebitActive = true)) },
            )
        }
    }

    fun submitKycPending(
        pin: String? = null,
        fullName: String? = null,
        phone: String? = null,
        birthDate: String? = null,
        nik: String? = null,
        biometricEnabled: Boolean? = null,
    ) {
        if (pin != null) {
            Rules.pinValidationError(pin, birthDate)?.let { fail("WEAK_PIN", it) }
        }
        _state.update { s ->
            val current = s.user
            val updatedUser = current?.copy(
                name = fullName?.trim()?.takeIf { it.isNotEmpty() } ?: current.name,
                phone = phone?.takeIf { it.isNotEmpty() } ?: current.phone,
                birthDate = birthDate?.takeIf { it.isNotEmpty() } ?: current.birthDate,
                nik = nik?.replace(" ", "")?.takeIf { it.isNotEmpty() } ?: current.nik,
                verified = false,
                isKycPending = true,
            )
            s.copy(
                user = updatedUser,
                pin = pin ?: s.pin,
                isBiometricEnabled = biometricEnabled ?: s.isBiometricEnabled,
            )
        }
    }

    fun approveKyc() {
        _state.update { s ->
            val notif = NotificationItem(
                id = nextId("notif"),
                category = NotificationCategory.ANNOUNCEMENT,
                title = "Verifikasi berhasil",
                message = "Akunmu sudah terverifikasi. Sekarang kamu bisa mulai transaksi.",
                atMs = clock(),
                isRead = false,
            )
            s.copy(
                user = s.user?.copy(verified = true, isKycPending = false),
                notifications = listOf(notif) + s.notifications,
            )
        }
    }

    /**
     * Verifikasi PIN terpadu untuk login dan transaksi dengan eskalasi lockout bertingkat.
     * Aturan v1.2:
     * - 1x/2x salah: remaining attempts (sisa 2 / 1 kesempatan)
     * - 3x salah: PIN terkunci sementara (level 1 = 10 mnt, level 2 = 30 mnt, level 3 = 1 jam)
     * - 4x set salah: Akun terkunci (harus atur ulang PIN lewat Lupa PIN)
     * - Benar: reset percobaan dan level lockout
     */
    fun checkLoginPin(pin: String): PinVerifyResult {
        if (_state.value.isAccountLocked) {
            return PinVerifyResult.AccountLocked
        }
        val expected = _state.value.pin ?: GOLDIN_DEMO_PIN
        if (pin == expected || pin == GOLDIN_DEMO_PIN) {
            _state.update { it.copy(pinFailedAttempts = 0, pinLockLevel = 0) }
            return PinVerifyResult.Success
        }

        val nextAttempts = _state.value.pinFailedAttempts + 1
        if (nextAttempts < 3) {
            _state.update { it.copy(pinFailedAttempts = nextAttempts) }
            return PinVerifyResult.Incorrect(remainingAttempts = 3 - nextAttempts, attempts = nextAttempts)
        }

        val nextLevel = _state.value.pinLockLevel + 1
        if (nextLevel <= 3) {
            val lockDuration = Rules.PIN_LOCK_DURATIONS_MINUTES.getOrElse(nextLevel - 1) { 60 }
            _state.update { it.copy(pinFailedAttempts = 0, pinLockLevel = nextLevel) }
            return PinVerifyResult.TemporarilyLocked(level = nextLevel, lockMinutes = lockDuration)
        } else {
            _state.update { it.copy(pinFailedAttempts = 0, pinLockLevel = 4, isAccountLocked = true) }
            return PinVerifyResult.AccountLocked
        }
    }

    fun verifyPin(pin: String): Boolean {
        return when (val res = checkLoginPin(pin)) {
            is PinVerifyResult.Success -> true
            is PinVerifyResult.AccountLocked -> fail("PIN_LOCKED", "Akun terkunci. Atur ulang PIN untuk membuka akunmu.")
            is PinVerifyResult.TemporarilyLocked -> fail("PIN_LOCKED", "PIN terkunci sementara selama ${res.lockMinutes} menit.")
            is PinVerifyResult.Incorrect -> false
        }
    }

    fun resetPin(newPin: String) {
        if (Rules.isWeakPin(newPin, _state.value.user?.birthDate)) {
            fail("WEAK_PIN", "PIN terlalu mudah ditebak. Hindari angka berulang, berurutan, dan tanggal lahir.")
        }
        _state.update {
            it.copy(
                pin = newPin,
                pinFailedAttempts = 0,
                pinLockLevel = 0,
                isAccountLocked = false,
            )
        }
    }

    fun unlockAccountWithResetPin(newPin: String) {
        resetPin(newPin)
    }

    fun isRegisteredEmail(email: String): Boolean {
        val trimmed = email.trim()
        val currentEmail = _state.value.user?.email ?: GOLDIN_DEMO_EMAIL
        return trimmed.equals(currentEmail, ignoreCase = true) ||
            trimmed.equals(GOLDIN_DEMO_EMAIL, ignoreCase = true)
    }

    fun resetPassword(email: String, newPassword: String) {
        if (!Rules.isValidEmail(email)) fail("INVALID_EMAIL", "Format email belum benar.")
        if (!isRegisteredEmail(email)) {
            fail("EMAIL_NOT_FOUND", "Email belum terdaftar")
        }
        if (!Rules.isValidGoldinPassword(newPassword)) {
            fail("INVALID_PASSWORD", "Kata sandi minimal 8 karakter dan mengandung huruf kapital.")
        }
        _state.update { it.copy(registeredPassword = newPassword) }
    }

    fun verifyAccountForResetPin(email: String, password: String): Boolean {
        if (!Rules.isValidEmail(email)) fail("INVALID_CREDENTIALS", "Email atau kata sandi salah")
        val trimmed = email.trim()
        val currentEmail = _state.value.user?.email ?: GOLDIN_DEMO_EMAIL
        if (!trimmed.equals(currentEmail, ignoreCase = true) && !trimmed.equals(GOLDIN_DEMO_EMAIL, ignoreCase = true)) {
            fail("INVALID_CREDENTIALS", "Email atau kata sandi salah")
        }
        if (password != _state.value.registeredPassword) {
            fail("INVALID_CREDENTIALS", "Email atau kata sandi salah")
        }
        return true
    }

    fun setFirstTimeLogin(isFirstTime: Boolean) {
        _state.update { it.copy(isFirstTimeLogin = isFirstTime) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _state.update { it.copy(isBiometricEnabled = enabled) }
    }

    // ----- Beli -----

    fun quoteBuy(skuCode: String, quantity: Int): Quote {
        val sku = _state.value.sku(skuCode)
        if (quantity < 1) fail("INVALID", "Jumlah minimal 1 keping.")
        if (sku.stock < quantity) fail("INSUFFICIENT_STOCK", "Stok ${sku.code} tidak mencukupi.")
        val quote = Quote(
            id = nextId("QT"),
            skuCode = skuCode,
            quantity = quantity,
            unitPrice = sku.buyPrice,
            total = sku.buyPrice * quantity,
            buybackValueNow = sku.buybackPrice * quantity,
            expiresAtMs = clock() + Rules.QUOTE_TTL_MS,
        )
        quotes[quote.id] = quote
        return quote
    }

    fun buy(quoteId: String, method: PaymentMethod): List<GoldToken> =
        confirmPayment(createOrder(quoteId, method).id)

    fun createOrder(quoteId: String, method: PaymentMethod): PendingOrder {
        val quote = validQuote(quoteId)
        val state = _state.value
        val user = state.user ?: fail("UNAUTHENTICATED", "Silakan masuk terlebih dahulu.")
        if (!user.verified) fail("KYC_REQUIRED", "Verifikasi identitas diperlukan sebelum membeli.")
        val sku = state.sku(quote.skuCode)
        if (sku.stock < quote.quantity) fail("INSUFFICIENT_STOCK", "Stok ${sku.code} tidak mencukupi.")
        if (method == PaymentMethod.WALLET && state.walletBalance < quote.total) {
            fail("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi.")
        }
        val code = when (method) {
            PaymentMethod.WALLET -> ""
            PaymentMethod.QRIS -> "QR-" + random.nextInt(100000, 999999)
            PaymentMethod.VIRTUAL_ACCOUNT, PaymentMethod.VA_BCA -> "39358" + random.nextLong(100000000L, 999999999L)
            PaymentMethod.VA_BRI -> "88810" + random.nextLong(100000000L, 999999999L)
            PaymentMethod.VA_MANDIRI -> "89508" + random.nextLong(100000000L, 999999999L)
            PaymentMethod.VA_BNI -> "98816" + random.nextLong(100000000L, 999999999L)
            PaymentMethod.CICILAN -> ""
        }
        val order = PendingOrder(nextId("ORD"), quote, method, clock() + Rules.PAYMENT_WINDOW_MS, code)
        adjustStock(sku.code, -quote.quantity)
        pending[order.id] = order
        quotes.remove(quoteId)
        return order
    }

    fun confirmPayment(orderId: String): List<GoldToken> {
        val order = pending[orderId] ?: fail("ORDER_NOT_FOUND", "Pesanan tidak ditemukan.")
        if (clock() > order.payBeforeMs) {
            cancelOrder(orderId)
            fail("ORDER_EXPIRED", "Batas waktu pembayaran habis. Keping dilepas kembali.")
        }
        val state = _state.value
        val quote = order.quote
        val sku = state.sku(quote.skuCode)
        if (order.method == PaymentMethod.WALLET && state.walletBalance < quote.total) {
            cancelOrder(orderId)
            fail("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi.")
        }
        val now = clock()
        val minted = List(quote.quantity) {
            GoldToken(tokenCode(sku), sku.code, newSerial(sku), TokenStatus.ACTIVE, quote.unitPrice, now)
        }
        val gramsAdded = (sku.weightMg * quote.quantity) / 1000.0
        val tx = Tx(
            id = nextId("TX"),
            type = TxType.BUY,
            title = "Beli ${quote.quantity} × ${sku.product} ${gram(sku.weightMg)}",
            amount = -quote.total,
            atMs = now,
            status = "ok",
            method = order.method.label,
            grams = gramsAdded,
            pricePerGram = sku.buyPrice * 1000L / sku.weightMg,
            fee = 1_000L,
            tax = (quote.total * 25) / 10000,
            bursaRef = "BRS-" + (now % 1000000),
            kliringRef = "KLR-" + (now % 1000000),
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Pembelian emas berhasil",
            message = "${formatGrams4(gramsAdded)} sudah masuk ke akunmu.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = if (order.method == PaymentMethod.WALLET) s.walletBalance - quote.total else s.walletBalance,
                goldBalanceGrams = s.goldBalanceGrams + gramsAdded,
                tokens = minted + s.tokens,
                transactions = listOf(tx) + s.transactions,
                notifications = listOf(notif) + s.notifications,
            )
        }
        pending.remove(orderId)
        return minted
    }

    fun cancelOrder(orderId: String) {
        val order = pending.remove(orderId) ?: return
        adjustStock(order.quote.skuCode, order.quote.quantity)
    }

    private fun adjustStock(skuCode: String, delta: Int) {
        _state.update { s -> s.copy(skus = s.skus.map { if (it.code == skuCode) it.copy(stock = it.stock + delta) else it }) }
    }

    private fun validQuote(id: String): Quote {
        val quote = quotes[id] ?: fail("QUOTE_EXPIRED", "Harga sudah berubah. Silakan ulangi.")
        if (clock() > quote.expiresAtMs) {
            quotes.remove(id)
            fail("QUOTE_EXPIRED", "Batas waktu harga habis. Silakan ulangi.")
        }
        return quote
    }

    // ----- Beli / Jual Langsung Gramasi (Goldin v1.2) -----

    fun buyGoldDirect(nominal: Long, grams: Double, method: PaymentMethod): Tx {
        val state = _state.value
        val user = state.user ?: fail("UNAUTHENTICATED", "Silakan masuk terlebih dahulu.")
        if (!user.verified) fail("KYC_REQUIRED", "Verifikasi identitas diperlukan sebelum membeli.")
        if (nominal < 10_000L) fail("INVALID", "Minimal transaksi Rp10.000.")
        if (method == PaymentMethod.WALLET && state.walletBalance < nominal) {
            fail("INSUFFICIENT_BALANCE", "Saldo Kas tidak mencukupi.")
        }
        val now = clock()
        val fee = 1_000L
        val tax = (nominal * 25) / 10000
        val total = nominal + fee + tax
        val tx = Tx(
            id = nextId("TRX-B"),
            type = TxType.BUY,
            title = "Beli Emas",
            amount = -total,
            atMs = now,
            status = "ok",
            method = method.label,
            grams = grams,
            pricePerGram = state.askPricePerGram,
            fee = fee,
            tax = tax,
            bursaRef = "BRS-" + (now % 1000000),
            kliringRef = "KLR-" + (now % 1000000),
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Pembelian emas berhasil",
            message = "${formatGrams4(grams)} sudah masuk ke akunmu.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = if (method == PaymentMethod.WALLET) s.walletBalance - total else s.walletBalance,
                goldBalanceGrams = s.goldBalanceGrams + grams,
                transactions = listOf(tx) + s.transactions,
                notifications = listOf(notif) + s.notifications,
            )
        }
        return tx
    }

    fun sellGoldDirect(grams: Double): Tx {
        val state = _state.value
        val user = state.user ?: fail("UNAUTHENTICATED", "Silakan masuk terlebih dahulu.")
        if (!user.verified) fail("KYC_REQUIRED", "Verifikasi identitas diperlukan sebelum menjual.")
        if (grams <= 0.0) fail("INVALID", "Masukkan berat emas yang valid.")
        if (grams > state.freeGoldGrams) fail("INSUFFICIENT_GOLD", "Saldo emas bebas tidak mencukupi.")
        val now = clock()
        val subtotal = (grams * state.buybackPerGram).toLong()
        val fee = 1_000L
        val tax = (subtotal * 25) / 10000
        val total = subtotal - fee - tax
        val tx = Tx(
            id = nextId("TRX-S"),
            type = TxType.SELL,
            title = "Jual Emas",
            amount = total,
            atMs = now,
            status = "ok",
            method = "Saldo Kas",
            grams = grams,
            pricePerGram = state.buybackPerGram,
            fee = fee,
            tax = tax,
            bursaRef = "BRS-" + (now % 1000000),
            kliringRef = "KLR-" + (now % 1000000),
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Penjualan emas berhasil",
            message = "${rupiah(total)} dari penjualan ${formatGrams4(grams)} sudah masuk ke Saldo Kas.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = s.walletBalance + total,
                goldBalanceGrams = (s.goldBalanceGrams - grams).coerceAtLeast(0.0),
                transactions = listOf(tx) + s.transactions,
                notifications = listOf(notif) + s.notifications,
            )
        }
        return tx
    }

    // ----- Jual Keping Tradisional -----

    fun sell(tokenCode: String): Long {
        val state = _state.value
        val token = token(tokenCode)
        if (token.status != TokenStatus.ACTIVE) fail("TOKEN_LOCKED", "Token ini sedang terkunci dan tidak bisa dijual.")
        if (!Rules.canSellAt(token, clock())) fail("HOLDING_PERIOD_ACTIVE", "Token baru bisa dijual 24 jam setelah pembelian.")
        val sku = state.sku(token.skuCode)
        _state.update { s ->
            s.copy(
                walletBalance = s.walletBalance + sku.buybackPrice,
                goldBalanceGrams = (s.goldBalanceGrams - sku.weightMg / 1000.0).coerceAtLeast(0.0),
                skus = s.skus.map { if (it.code == sku.code) it.copy(stock = it.stock + 1) else it },
                tokens = s.tokens.map { if (it.code == tokenCode) it.copy(status = TokenStatus.RETURNED) else it },
                transactions = listOf(Tx(nextId("TX"), TxType.SELL, "Jual ${sku.product} ${gram(sku.weightMg)}", sku.buybackPrice, clock())) + s.transactions,
            )
        }
        return sku.buybackPrice
    }

    private fun token(code: String): GoldToken =
        _state.value.tokens.firstOrNull { it.code == code } ?: fail("TOKEN_NOT_OWNED", "Token tidak ditemukan.")

    // ----- Saldo Kas & Rekening -----

    fun topup(amount: Long, method: PaymentMethod): Tx {
        if (amount < 10_000L) fail("INVALID", "Minimal top up Rp10.000.")
        val now = clock()
        val fee = 1_000L
        val tx = Tx(
            id = nextId("TRX-T"),
            type = TxType.TOPUP,
            title = "Top up Saldo Kas",
            amount = amount,
            atMs = now,
            status = "ok",
            method = method.label,
            fee = fee,
            payRef = "PAY-" + (now % 1000000),
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Top up berhasil",
            message = "${rupiah(amount)} sudah masuk ke Saldo Kas.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = s.walletBalance + amount,
                transactions = listOf(tx) + s.transactions,
                notifications = listOf(notif) + s.notifications,
            )
        }
        return tx
    }

    fun withdraw(amount: Long, bankAccountId: String) {
        val state = _state.value
        val account = state.bankAccounts.firstOrNull {
            it.id == bankAccountId || it.id.startsWith(bankAccountId) || it.bank.equals(bankAccountId, ignoreCase = true)
        } ?: fail("INVALID", "Pilih rekening tujuan.")
        if (amount <= 0) fail("INVALID", "Masukkan nominal penarikan.")
        if (amount > state.walletBalance) fail("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi.")
        val now = clock()
        val fee = 2_500L
        val tx = Tx(
            id = nextId("TRX-W"),
            type = TxType.WITHDRAW,
            title = "Tarik Dana",
            amount = -amount,
            atMs = now,
            status = "ok",
            method = "${account.bank} ${account.number}",
            fee = fee,
            payRef = "PAY-" + (now % 1000000),
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Penarikan dana berhasil",
            message = "${rupiah(amount)} sudah masuk ke rekening ${account.bank} ${account.number}.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = s.walletBalance - amount,
                transactions = listOf(tx) + s.transactions,
                notifications = listOf(notif) + s.notifications,
            )
        }
    }

    fun addBankAccount(bank: String, number: String, holder: String) {
        val state = _state.value
        if (state.bankAccounts.any { it.number == number }) {
            fail("DUPLICATE_ACCOUNT", "Rekening ini sudah terdaftar di akunmu.")
        }
        val userKtpName = state.user?.name ?: "Budi Santoso"
        if (!holder.equals(userKtpName, ignoreCase = true)) {
            fail("MISMATCH_NAME", "Nama pemilik rekening tidak sesuai dengan e-KTP. Gunakan rekening atas namamu sendiri.")
        }
        val newAccount = BankAccount(nextId("bank"), bank, number, holder, isAutoDebitActive = true)
        _state.update { it.copy(bankAccounts = it.bankAccounts + newAccount) }
    }

    fun deleteBankAccount(id: String) {
        _state.update { s -> s.copy(bankAccounts = s.bankAccounts.filterNot { it.id == id }) }
    }

    // ----- Tabungan Rutin -----

    fun createRoutinePlan(
        name: String,
        type: RoutinePlanType,
        targetMonths: Int,
        targetGrams: Double,
        monthlyAmount: Long,
        debitDate: Int,
        source: String,
    ): RoutineSavingsPlan {
        if (monthlyAmount < 10_000L) fail("INVALID", "Minimal nominal per bulan Rp10.000.")
        if (debitDate !in 1..28) fail("INVALID", "Tanggal debit harus antara 1 sampai 28.")
        val now = clock()
        val plan = RoutineSavingsPlan(
            id = nextId("PLN"),
            name = name.trim().ifBlank { "Tabungan Rutin" },
            type = type,
            targetMonths = targetMonths,
            targetGrams = targetGrams,
            monthlyAmount = monthlyAmount,
            debitDate = debitDate,
            savedGrams = 0.0,
            fundingSource = source,
            status = RoutinePlanStatus.ACTIVE,
            createdAtMs = now,
        )
        _state.update { it.copy(routinePlans = it.routinePlans + plan) }
        return plan
    }

    fun pauseRoutinePlan(planId: String) {
        _state.update { s ->
            s.copy(routinePlans = s.routinePlans.map { if (it.id == planId) it.copy(status = RoutinePlanStatus.PAUSED) else it })
        }
    }

    fun resumeRoutinePlan(planId: String) {
        _state.update { s ->
            s.copy(routinePlans = s.routinePlans.map { if (it.id == planId) it.copy(status = RoutinePlanStatus.ACTIVE) else it })
        }
    }

    fun stopRoutinePlan(planId: String) {
        _state.update { s -> s.copy(routinePlans = s.routinePlans.filterNot { it.id == planId }) }
    }

    // ----- Beranda: Visibilitas Saldo & Refresh Harga Emas -----

    /** Toggle ikon mata di Beranda sekaligus menyinkronkan kedua kartu di Portofolio (Handoff §6). */
    fun toggleHomeBalanceVisibility() {
        _state.update { s ->
            val next = !s.hideHomeBalance
            s.copy(
                hideHomeBalance = next,
                hidePortfolioGold = next,
                hidePortfolioCash = next,
            )
        }
    }

    /** Toggle mata kartu Tabungan Emas secara independen di Portofolio (Handoff §9). */
    fun togglePortfolioGoldVisibility() {
        _state.update { it.copy(hidePortfolioGold = !it.hidePortfolioGold) }
    }

    /** Toggle mata kartu Saldo Kas secara independen di Portofolio (Handoff §9). */
    fun togglePortfolioCashVisibility() {
        _state.update { it.copy(hidePortfolioCash = !it.hidePortfolioCash) }
    }

    /**
     * Menyimulasikan pembaruan harga emas manual (`Main.dc.html` L4034–4042).
     * Mengembalikan arah flash (`"up"`, `"down"`, atau `null`).
     */
    fun refreshGoldPrice(formattedHm: String? = null, deltaOverride: Long? = null): String? {
        val current = _state.value.askPricePerGram
        val rawDelta = deltaOverride ?: run {
            val steps = listOf(-1_500L, -1_000L, -500L, 500L, 1_000L, 1_500L, 2_000L)
            val idx = ((clock() / 100L + current / 500L) % steps.size).toInt().let { if (it < 0) it + steps.size else it }
            steps[idx]
        }
        val nextAsk = roundTo500((current + rawDelta).coerceAtLeast(1_500_000L))
        val nextBid = roundTo500((nextAsk * 0.9752).toLong())
        val flash = when {
            nextAsk > current -> "up"
            nextAsk < current -> "down"
            else -> null
        }
        val label = formattedHm?.let { "$it WIB" } ?: _state.value.priceUpdatedAtLabel
        _state.update { s ->
            s.copy(
                askPricePerGram = nextAsk,
                buybackPerGram = nextBid,
                priceUpdatedAtMs = clock(),
                priceUpdatedAtLabel = label,
                priceFlash = flash,
            )
        }
        return flash
    }

    fun clearPriceFlash() {
        _state.update { it.copy(priceFlash = null) }
    }

    private fun roundTo500(value: Long): Long = ((value + 250L) / 500L) * 500L

    // ----- Notifikasi -----

    fun markNotificationRead(id: String) {
        _state.update { s -> s.copy(notifications = s.notifications.map { if (it.id == id) it.copy(isRead = true) else it }) }
    }

    fun markAllNotificationsRead() {
        _state.update { s -> s.copy(notifications = s.notifications.map { it.copy(isRead = true) }) }
    }

    fun deleteNotification(id: String) {
        _state.update { s -> s.copy(notifications = s.notifications.filterNot { it.id == id }) }
    }

    // ----- Cetak Emas Tradisional & Fisik Baru -----

    fun createPrintOrder(tokenCodes: List<String>, method: PrintMethod, address: String?, branchId: String?): PrintOrder {
        val state = _state.value
        if (tokenCodes.isEmpty()) fail("INVALID", "Pilih minimal satu token.")
        val tokens = tokenCodes.map { token(it) }
        if (tokens.any { it.status != TokenStatus.ACTIVE }) fail("TOKEN_LOCKED", "Ada token yang tidak bisa dicetak saat ini.")
        val total = tokens.sumOf { state.sku(it.skuCode).weightMg }
        val branch = branches.firstOrNull { it.id == branchId }
        val destination = when (method) {
            PrintMethod.BRANCH -> branch?.let { "Cabang ${it.name}" } ?: fail("INVALID", "Pilih cabang pengambilan.")
            PrintMethod.JNE -> address?.trim()?.takeIf { it.isNotEmpty() } ?: fail("INVALID", "Isi alamat pengiriman.")
        }
        if (method == PrintMethod.BRANCH && !Rules.isPickupEligible(total)) {
            fail("PICKUP_MIN_WEIGHT_NOT_MET", "Minimal berat untuk ambil di cabang adalah 2 gram.")
        }
        val order = PrintOrder(nextId("PRT"), tokenCodes, method, destination, PrintStatus.WAITING, branch?.estimate ?: "2–4 hari kerja", clock())
        _state.update { s ->
            s.copy(
                tokens = s.tokens.map { if (tokenCodes.contains(it.code)) it.copy(status = TokenStatus.PRINT_PENDING) else it },
                printOrders = listOf(order) + s.printOrders,
            )
        }
        return order
    }

    // ----- Cicil Emas -----

    fun submitInstallment(skuCode: String, downPayment: Long, tenorMonths: Int, currentPrice: Long, agreementVersion: String): InstallmentApplication {
        val state = _state.value
        val sku = state.sku(skuCode)
        if (agreementVersion.isBlank()) fail("AGREEMENT_REQUIRED", "Setujui perjanjian pembiayaan sebelum mengajukan.")
        if (sku.buyPrice != currentPrice) fail("PRICE_CHANGED", "Harga sudah berubah. Tinjau kembali simulasi Anda.")
        if (sku.stock < 1) fail("INSUFFICIENT_STOCK", "Stok keping ini sedang habis.")
        val application = InstallmentApplication(nextId("APP"), skuCode, sku.buyPrice, downPayment, tenorMonths, agreementVersion, clock(), clock() + Rules.PAYMENT_WINDOW_MS)
        adjustStock(skuCode, -1)
        applications[application.id] = application
        return application
    }

    fun payDownPayment(applicationId: String, method: PaymentMethod): InstallmentContract {
        val application = applications[applicationId] ?: fail("ORDER_NOT_FOUND", "Pengajuan tidak ditemukan.")
        if (clock() > application.payBeforeMs) {
            cancelInstallment(applicationId)
            fail("ORDER_EXPIRED", "Batas waktu pembayaran habis. Pengajuan dibatalkan.")
        }
        val state = _state.value
        if (method == PaymentMethod.WALLET && state.walletBalance < application.downPayment) {
            fail("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi.")
        }
        val sku = state.sku(application.skuCode)
        val token = GoldToken(tokenCode(sku), sku.code, newSerial(sku), TokenStatus.LOCKED_INSTALLMENT, application.price, clock())
        val contract = InstallmentContract(
            nextId("MTR"),
            "BSI Finance",
            token.code,
            application.price,
            application.downPayment,
            application.tenorMonths,
            0,
            application.agreementVersion,
            application.agreedAtMs,
        )
        val grams = sku.weightMg / 1000.0
        _state.update { s ->
            s.copy(
                walletBalance = if (method == PaymentMethod.WALLET) s.walletBalance - application.downPayment else s.walletBalance,
                lockedGoldGrams = s.lockedGoldGrams + grams,
                tokens = listOf(token) + s.tokens,
                contracts = listOf(contract) + s.contracts,
                transactions = listOf(Tx(nextId("TX"), TxType.INSTALLMENT, "Uang muka cicil ${sku.product} ${gram(sku.weightMg)}", -application.downPayment, clock())) + s.transactions,
            )
        }
        applications.remove(applicationId)
        return contract
    }

    fun cancelInstallment(applicationId: String) {
        val application = applications.remove(applicationId) ?: return
        adjustStock(application.skuCode, 1)
    }

    fun markInstallmentPaidOff(contractId: String) {
        _state.update { s ->
            val contract = s.contracts.first { it.id == contractId }
            s.copy(
                tokens = s.tokens.map { if (it.code == contract.tokenCode) it.copy(status = TokenStatus.ACTIVE) else it },
                contracts = s.contracts.map { if (it.id == contractId) it.copy(paidMonths = it.tenorMonths) else it },
                lockedGoldGrams = 0.0,
            )
        }
    }

    fun downPaymentCode(method: PaymentMethod): String = when (method) {
        PaymentMethod.QRIS -> "QR-" + random.nextInt(100000, 999999)
        else -> "70070" + random.nextLong(100000000L, 999999999L)
    }

    companion object {
        const val GOLDIN_DEMO_EMAIL = "reiuser@gmail.com"
        const val GOLDIN_DEMO_PASSWORD = "Reitesting123"
        const val GOLDIN_DEMO_PIN = "135790"
        const val DEMO_PIN = "147258"
        const val DEMO_EMAIL_OTP = "654321"
        const val DEMO_PHONE_OTP = "123456"
        private const val SKU_BUYBACK_PER_GRAM = 2_310_000L
        private const val ASK_PER_GRAM = 2_315_000L
        private const val BID_PER_GRAM = 2_257_500L
        private const val BASE_ASK_PER_GRAM = 2_287_500L

        private fun sku(code: String, product: String, weightMg: Long, buyPrice: Long, stock: Int) =
            Sku(code, product, weightMg, buyPrice, SKU_BUYBACK_PER_GRAM * weightMg / 1000, stock)

        private fun initialState() = AppState(
            skus = listOf(
                sku("LMRE-0.1G", "LM Raja Emas", 100, 305_000, 40),
                sku("LMRE-0.5G", "LM Raja Emas", 500, 1_308_500, 30),
                sku("LMRE-1G", "LM Raja Emas", 1_000, 2_513_500, 40),
                sku("LMRE-2G", "LM Raja Emas", 2_000, 5_007_000, 20),
                sku("LMRE-5G", "LM Raja Emas", 5_000, 12_442_500, 16),
                sku("LMRE-10G", "LM Raja Emas", 10_000, 25_135_000, 12),
                sku("LMRE-25G", "LM Raja Emas", 25_000, 61_737_500, 8),
                sku("LMRE-50G", "LM Raja Emas", 50_000, 123_375_000, 6),
                sku("LMRE-100G", "LM Raja Emas", 100_000, 246_550_000, 4),
                sku("LMRE-250G", "LM Raja Emas", 250_000, 614_265_000, 2),
                sku("LMRE-500G", "LM Raja Emas", 500_000, 1_228_320_000, 2),
                sku("LMRE-1000G", "LM Raja Emas", 1_000_000, 2_456_600_000, 1),
            ),
            buybackPerGram = BID_PER_GRAM,
            askPricePerGram = ASK_PER_GRAM,
            priceBaseAskPerGram = BASE_ASK_PER_GRAM,
            priceUpdatedAtLabel = "12.30 WIB",
        )
    }
}
