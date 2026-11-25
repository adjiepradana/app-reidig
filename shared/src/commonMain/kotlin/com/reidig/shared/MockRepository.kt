package com.reidig.shared

import com.reidig.shared.api.PriceOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * Backend tiruan di dalam aplikasi Goldin v1.2.
 * Diganti klien HTTP saat API siap; layar hanya bergantung pada fungsi-fungsi ini.
 */
open class MockRepository(
    protected val clock: () -> Long,
    protected val random: Random = Random.Default,
) {
    protected val _state = MutableStateFlow(initialState())
    open val state: StateFlow<AppState> = _state.asStateFlow()

    private val quotes = mutableMapOf<String, Quote>()
    private val pending = mutableMapOf<String, PendingOrder>()
    private val applications = mutableMapOf<String, InstallmentApplication>()
    val tradeQuotes = mutableMapOf<String, TradeQuote>()
    private val installmentQuotes = mutableMapOf<String, InstallmentQuote>()
    private var pinAttempts = 0
    private var serialCounter = 500_000
    private var idCounter = 1

    open var branches: List<Branch> = listOf(
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

    open var officialGoldBranches: List<GoldBranch> = listOf(
        GoldBranch("kwc", "Raja Emas Karawaci", "Jl. Imam Bonjol No. 39, Karawaci", "Kota Tangerang", "Banten", 1.2, "Senin–Sabtu, 09.00–17.00 WIB", false),
        GoldBranch("cpd", "Raja Emas Cipondoh", "Jl. KH Hasyim Ashari No. 21, Cipondoh", "Kota Tangerang", "Banten", 3.8, "Senin–Sabtu, 09.00–17.00 WIB", false),
        GoldBranch("bsd", "Raja Emas BSD", "Jl. Pahlawan Seribu Kav. 3, Serpong", "Kota Tangerang Selatan", "Banten", 9.5, "Setiap hari, 10.00–20.00 WIB", true),
        GoldBranch("kbj", "Raja Emas Kebon Jeruk", "Jl. Panjang No. 18, Kebon Jeruk", "Jakarta Barat", "DKI Jakarta", 11.2, "Senin–Sabtu, 09.00–17.00 WIB", false),
        GoldBranch("blm", "Raja Emas Blok M", "Jl. Melawai Raya No. 8, Kebayoran Baru", "Jakarta Selatan", "DKI Jakarta", 19.6, "Setiap hari, 10.00–18.00 WIB", true),
        GoldBranch("kgd", "Raja Emas Kelapa Gading", "Jl. Boulevard Raya Blok M No. 12, Kelapa Gading", "Jakarta Utara", "DKI Jakarta", 27.4, "Senin–Sabtu, 09.00–17.00 WIB", false),
        GoldBranch("bks", "Raja Emas Bekasi", "Jl. Ahmad Yani No. 7, Bekasi Selatan", "Kota Bekasi", "Jawa Barat", 34.1, "Senin–Sabtu, 09.00–17.00 WIB", false),
        GoldBranch("bgr", "Raja Emas Bogor", "Jl. Pajajaran No. 45, Bogor Tengah", "Kota Bogor", "Jawa Barat", 48.3, "Senin–Sabtu, 09.00–17.00 WIB", false),
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
    open fun login(email: String, password: String = GOLDIN_DEMO_PASSWORD) {
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

    fun startDemoAccount(email: String = GOLDIN_DEMO_EMAIL) {
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
                amount = -251_625L,
                atMs = oneDayAgo,
                status = "ok",
                method = "Saldo Kas",
                paymentMethod = PaymentMethod.WALLET,
                grams = 0.1087,
                pricePerGram = 2_298_000L,
                subtotal = 250_000L,
                fee = 1_000L,
                tax = 625L,
                total = 251_625L,
                bursaRef = "BRS-261116-482019",
                kliringRef = "KLR-261116-891042",
                executedAtMs = oneDayAgo + 4000L,
                settledAtMs = oneDayAgo + 120_000L,
            ),
            Tx(
                id = "TRX-A7",
                type = TxType.TOPUP,
                title = "Top up Saldo Kas",
                amount = 500_000L,
                atMs = oneDayAgo - 12 * 60 * 1000,
                status = "ok",
                method = "VA BCA",
                paymentMethod = PaymentMethod.VA_BCA,
                subtotal = 500_000L,
                fee = 1_000L,
                total = 501_000L,
                payRef = "PAY-261116-102948",
            ),
            Tx(
                id = "TRX-A6",
                type = TxType.SELL,
                title = "Jual Emas",
                amount = 148_625L,
                atMs = fourDaysAgo,
                status = "ok",
                method = "Saldo Kas",
                paymentMethod = PaymentMethod.WALLET,
                grams = 0.0669,
                pricePerGram = 2_241_000L,
                subtotal = 150_000L,
                fee = 1_000L,
                tax = 375L,
                total = 148_625L,
                bursaRef = "BRS-261113-192847",
                kliringRef = "KLR-261113-581930",
                executedAtMs = fourDaysAgo + 4000L,
                settledAtMs = fourDaysAgo + 120_000L,
            ),
            Tx(
                id = "TRX-A5",
                type = TxType.BUY,
                title = "Beli Emas",
                amount = -101_250L,
                atMs = nineDaysAgo,
                status = "fail",
                method = "VA Mandiri",
                paymentMethod = PaymentMethod.VA_MANDIRI,
                grams = 0.0439,
                pricePerGram = 2_276_000L,
                subtotal = 100_000L,
                fee = 1_000L,
                tax = 250L,
                total = 101_250L,
                failureReason = "Pembayaran tidak diterima sampai batas waktu",
            ),
            Tx(
                id = "TRX-A4",
                type = TxType.WITHDRAW,
                title = "Tarik Dana",
                amount = -300_000L,
                atMs = twelveDaysAgo,
                status = "ok",
                method = "BCA ••••1234",
                paymentMethod = PaymentMethod.WALLET,
                subtotal = 300_000L,
                fee = 2_500L,
                total = 302_500L,
                payRef = "PAY-261105-301924",
            ),
        )

        val seededRoutineMutations = listOf(
            RoutineMutation("RM-3", "P0", now - 60L * 24L * 3600 * 1000, 0.0434, 100_000L, 2_302_000L, "Tabungan rutin", "TRX-R3"),
            RoutineMutation("RM-2", "P0", now - 30L * 24L * 3600 * 1000, 0.0440, 100_000L, 2_271_000L, "Tabungan rutin", "TRX-R2"),
            RoutineMutation("RM-1", "P0", now - 2L * 24L * 3600 * 1000, 0.0445, 100_000L, 2_248_000L, "Tabungan rutin", "TRX-R1"),
        )

        val seededRoutineTxs = seededRoutineMutations.map { rm ->
            val sub = rm.amount
            val tax = kotlin.math.round(sub.toDouble() * TRADE_TAX_RATE).toLong()
            val total = sub + TRADE_ADMIN_FEE + tax
            Tx(
                id = rm.txId ?: nextId("TRX-R"),
                type = TxType.BUY,
                title = "Beli Emas",
                amount = -total,
                atMs = rm.atMs,
                status = "ok",
                method = "Tabungan rutin",
                paymentMethod = PaymentMethod.ROUTINE,
                grams = rm.grams,
                pricePerGram = rm.pricePerGram,
                subtotal = sub,
                fee = TRADE_ADMIN_FEE,
                tax = tax,
                total = total,
                planId = rm.planId,
                bursaRef = "BRS-26" + (rm.atMs % 1000000),
                kliringRef = "KLR-26" + (rm.atMs % 1000000),
                executedAtMs = rm.atMs + 4000L,
                settledAtMs = rm.atMs + 120_000L,
            )
        }

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

        val seededAccount = BankAccount(
            id = "bca-1",
            bank = "BCA",
            number = "0281931234",
            holder = "BUDI SANTOSO",
            isAutoDebitActive = false,
            isPrimary = true,
            addedAtMs = now - 30 * 86400000L,
        )

        val seededReadyPickupOrder = PhysicalPrintOrder(
            id = "CTK-DEMO-01",
            orderNumber = "CTK-20261120-001",
            brand = GoldBrand.LM_REI,
            weightGrams = 1.0,
            quantity = 1,
            method = PrintMethod.BRANCH,
            branchId = "kwc",
            branchName = "Raja Emas Karawaci",
            branchAddress = "Jl. Imam Bonjol No. 39, Karawaci, Kota Tangerang",
            status = PhysicalOrderStatus.READY_PICKUP,
            ticketCode = "TKT-REI-839102",
            qrCodePayload = "GOLDIN:PRINT:CTK-DEMO-01:TKT-REI-839102",
            printFee = 50_000L,
            shippingInsuranceFee = 0L,
            totalPayment = 50_000L,
            serialNumber = "LMR-9081234",
            pickupScheduleDate = "Selasa, 24 Nov 2026",
            pickupScheduleTimeSlot = "10.00–11.00 WIB",
            paymentMethod = PaymentMethod.WALLET,
            createdAtMs = now - 2 * 86400000L,
        )

        val seededCompletedOrder = PhysicalPrintOrder(
            id = "CTK-DEMO-02",
            orderNumber = "CTK-20261110-002",
            brand = GoldBrand.ANTAM,
            weightGrams = 2.0,
            quantity = 1,
            method = PrintMethod.BRANCH,
            branchId = "bsd",
            branchName = "Raja Emas BSD",
            branchAddress = "Jl. Pahlawan Seribu Kav. 3, Serpong",
            status = PhysicalOrderStatus.DONE,
            ticketCode = "TKT-REI-102948",
            qrCodePayload = "GOLDIN:PRINT:CTK-DEMO-02:TKT-REI-102948",
            printFee = 50_000L,
            shippingInsuranceFee = 0L,
            totalPayment = 50_000L,
            serialNumber = "ANT-8391024",
            pickupScheduleDate = "Rabu, 11 Nov 2026",
            pickupScheduleTimeSlot = "14.00–15.00 WIB",
            paymentMethod = PaymentMethod.WALLET,
            createdAtMs = now - 12 * 86400000L,
        )

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
                transactions = (seededTx + seededRoutineTxs).sortedByDescending { it.atMs },
                bankAccounts = listOf(seededAccount),
                savedAddresses = listOf(seededAddress),
                routinePlans = listOf(seededPlan),
                notifications = seededNotifications,
                physicalOrders = listOf(seededReadyPickupOrder, seededCompletedOrder),
                isAccountLocked = false,
                pinLockLevel = 0,
                pinFailedAttempts = 0,
            )
        }
    }

    open fun register(name: String, email: String, password: String = GOLDIN_DEMO_PASSWORD, username: String = "", birthDate: String? = null) {
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

    open fun logout() {
        pinAttempts = 0
        quotes.clear()
        pending.clear()
        applications.clear()
        tradeQuotes.clear()
        _state.value = initialState()
    }

    fun completeKyc(pin: String, name: String) {
        if (Rules.isWeakPin(pin)) fail("WEAK_PIN", "PIN terlalu mudah ditebak. Hindari angka berulang dan berurutan.")
        _state.update { state ->
            state.copy(
                pin = pin,
                user = state.user?.copy(verified = true, isKycPending = false),
                bankAccounts = state.bankAccounts.ifEmpty {
                    listOf(
                        BankAccount(
                            id = "bca-1",
                            bank = "BCA",
                            number = "0281931234",
                            holder = name.trim(),
                            isAutoDebitActive = true,
                            isPrimary = true,
                            addedAtMs = clock(),
                        )
                    )
                },
            )
        }
    }

    fun submitKycPending(pin: String? = null, fullName: String? = null, phone: String? = null, birthDate: String? = null, nik: String? = null, biometricEnabled: Boolean? = null) {
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
    open fun checkLoginPin(pin: String): PinVerifyResult {
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

    open fun verifyPin(pin: String): Boolean {
        return when (val res = checkLoginPin(pin)) {
            is PinVerifyResult.Success -> true
            is PinVerifyResult.AccountLocked -> fail("PIN_LOCKED", "Akun terkunci. Atur ulang PIN untuk membuka akunmu.")
            is PinVerifyResult.TemporarilyLocked -> fail("PIN_LOCKED", "PIN terkunci sementara selama ${res.lockMinutes} menit.")
            is PinVerifyResult.Incorrect -> false
        }
    }

    open fun resetPin(newPin: String) {
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

    open fun unlockAccountWithResetPin(newPin: String) {
        resetPin(newPin)
    }

    open fun isRegisteredEmail(email: String): Boolean {
        val trimmed = email.trim()
        val currentEmail = _state.value.user?.email ?: GOLDIN_DEMO_EMAIL
        return trimmed.equals(currentEmail, ignoreCase = true) ||
            trimmed.equals(GOLDIN_DEMO_EMAIL, ignoreCase = true)
    }

    open fun resetPassword(email: String, newPassword: String) {
        if (!Rules.isValidEmail(email)) fail("INVALID_EMAIL", "Format email belum benar.")
        if (!isRegisteredEmail(email)) {
            fail("EMAIL_NOT_FOUND", "Email belum terdaftar")
        }
        if (!Rules.isValidGoldinPassword(newPassword)) {
            fail("INVALID_PASSWORD", "Kata sandi minimal 8 karakter dan mengandung huruf kapital.")
        }
        _state.update { it.copy(registeredPassword = newPassword) }
    }

    open suspend fun requestEmailCode(email: String, purpose: String = "REGISTER"): EmailCodeResult {
        return EmailCodeResult(expiresInSeconds = 600, resendInSeconds = 30, devCode = "123456")
    }

    open suspend fun verifyEmailCode(email: String, code: String, purpose: String = "REGISTER"): String {
        return "mock-ticket-$code"
    }

    open suspend fun registerWithTicket(name: String, email: String, password: String, emailTicket: String, phone: String? = null, birthDate: String? = null) {
        register(name = name, email = email, password = password, birthDate = birthDate)
    }

    open suspend fun setPinApi(pin: String, oldPin: String? = null) {
        resetPin(pin)
    }

    open suspend fun refreshMe() {}

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
            PaymentMethod.CICILAN, PaymentMethod.ROUTINE -> ""
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

    // ----- Beli / Jual Langsung Gramasi & Siklus Price-Lock (Goldin v1.2 — Handoff §7 & §14) -----

    fun calculateTrade(isBuy: Boolean, inputGrams: Double? = null, inputSubtotal: Long? = null, customPrice: Long? = null): TradeCalculation {
        val price = customPrice ?: if (isBuy) _state.value.askPricePerGram else _state.value.buybackPerGram
        return calculateTradeAmounts(isBuy, price, inputGrams, inputSubtotal)
    }

    open fun createTradeQuote(isBuy: Boolean, inputGrams: Double? = null, inputSubtotal: Long? = null): TradeQuote {
        val state = _state.value
        val user = state.user ?: fail("UNAUTHENTICATED", "Silakan masuk terlebih dahulu.")
        if (!user.verified && !user.isKycPending) fail("KYC_REQUIRED", "Verifikasi identitas diperlukan sebelum transaksi.")

        val calc = calculateTrade(isBuy, inputGrams, inputSubtotal, null)
        if (calc.subtotal < MIN_TRADE_RP) fail("INVALID_AMOUNT", "Minimal transaksi ${rupiah(MIN_TRADE_RP)}.")
        if (!isBuy && calc.grams > state.freeGoldGrams) {
            fail("INSUFFICIENT_GOLD", "Saldo emas bebas tidak mencukupi.")
        }

        val now = clock()
        val quote = TradeQuote(
            id = nextId("TQ"),
            isBuy = isBuy,
            lockedPricePerGram = calc.pricePerGram,
            grams = calc.grams,
            subtotal = calc.subtotal,
            adminFee = calc.adminFee,
            tax = calc.tax,
            total = calc.total,
            lockedAtMs = now,
            expiresAtMs = now + PRICE_LOCK_SECONDS * 1000L,
        )
        tradeQuotes[quote.id] = quote
        return quote
    }

    open fun refreshTradeQuote(quoteId: String): TradeQuote {
        val prev = tradeQuotes[quoteId] ?: fail("QUOTE_NOT_FOUND", "Kutipan harga tidak ditemukan.")
        val now = clock()
        val currentPrice = if (prev.isBuy) _state.value.askPricePerGram else _state.value.buybackPerGram
        val calc = calculateTradeAmounts(prev.isBuy, currentPrice, inputGrams = prev.grams)
        val refreshed = TradeQuote(
            id = nextId("TQ"),
            isBuy = prev.isBuy,
            lockedPricePerGram = currentPrice,
            grams = calc.grams,
            subtotal = calc.subtotal,
            adminFee = calc.adminFee,
            tax = calc.tax,
            total = calc.total,
            lockedAtMs = now,
            expiresAtMs = now + PRICE_LOCK_SECONDS * 1000L,
        )
        tradeQuotes.remove(quoteId)
        tradeQuotes[refreshed.id] = refreshed
        return refreshed
    }

    open fun getTradeQuote(quoteId: String): TradeQuote? = tradeQuotes[quoteId]

    open fun submitTradeOrder(quoteId: String, method: PaymentMethod): Tx {
        val state = _state.value
        val user = state.user ?: fail("UNAUTHENTICATED", "Silakan masuk terlebih dahulu.")
        if (!user.verified) fail("KYC_REQUIRED", "Verifikasi identitas diperlukan sebelum transaksi.")

        val quote = tradeQuotes[quoteId] ?: fail("QUOTE_NOT_FOUND", "Kutipan harga tidak ditemukan.")
        val now = clock()
        if (quote.isExpired(now)) {
            fail("QUOTE_EXPIRED", "Harga sudah kedaluwarsa. Silakan refresh harga.")
        }

        val txId = nextId(if (quote.isBuy) "TRX-B" else "TRX-S")
        val ymdDate = formatYmd(now)
        val randomSuffix = (random.nextInt(100_000, 999_999)).toString()

        if (quote.isBuy) {
            if (method == PaymentMethod.WALLET) {
                if (state.walletBalance < quote.total) {
                    fail("INSUFFICIENT_BALANCE", "Saldo Kas tidak mencukupi.")
                }
                val tx = Tx(
                    id = txId,
                    type = TxType.BUY,
                    title = "Beli Emas",
                    amount = -quote.total,
                    atMs = now,
                    status = "ok",
                    method = method.label,
                    paymentMethod = method,
                    grams = quote.grams,
                    pricePerGram = quote.lockedPricePerGram,
                    subtotal = quote.subtotal,
                    fee = quote.adminFee,
                    tax = quote.tax,
                    total = quote.total,
                    bursaRef = "BRS-$ymdDate-$randomSuffix",
                    kliringRef = "KLR-$ymdDate-$randomSuffix",
                    executedAtMs = now + 4000L,
                    settledAtMs = now + 120_000L,
                )
                val notif = NotificationItem(
                    id = nextId("notif"),
                    category = NotificationCategory.TRANSACTION,
                    title = "Pembelian emas berhasil",
                    message = "${formatGrams4(quote.grams)} sudah masuk ke akunmu.",
                    atMs = now,
                    isRead = false,
                    txId = tx.id,
                )
                _state.update { s ->
                    s.copy(
                        walletBalance = s.walletBalance - quote.total,
                        goldBalanceGrams = s.goldBalanceGrams + quote.grams,
                        transactions = listOf(tx) + s.transactions,
                        notifications = listOf(notif) + s.notifications,
                    )
                }
                tradeQuotes.remove(quoteId)
                return tx
            } else {
                // Virtual Account atau QRIS -> status "pending" (15 menit)
                val isVa = method.isVa
                val prefix = method.vaPrefix ?: "39358"
                val vaNo = if (isVa) generateVaNumber(prefix, txId) else null
                val qris = if (method == PaymentMethod.QRIS) "00020101021226670016ID.CO.REIDIG.WWW01189360091800000000000215$txId" else null
                val expiresAt = now + PAYMENT_WINDOW_MINUTES * 60 * 1000L

                val tx = Tx(
                    id = txId,
                    type = TxType.BUY,
                    title = "Beli Emas",
                    amount = -quote.total,
                    atMs = now,
                    status = "pending",
                    method = method.label,
                    paymentMethod = method,
                    grams = quote.grams,
                    pricePerGram = quote.lockedPricePerGram,
                    subtotal = quote.subtotal,
                    fee = quote.adminFee,
                    tax = quote.tax,
                    total = quote.total,
                    expiresAtMs = expiresAt,
                    virtualAccountNumber = vaNo,
                    qrisPayload = qris,
                )
                _state.update { s ->
                    s.copy(transactions = listOf(tx) + s.transactions)
                }
                tradeQuotes.remove(quoteId)
                return tx
            }
        } else {
            // Jual Emas -> Instant Saldo Kas
            if (state.freeGoldGrams < quote.grams) {
                fail("INSUFFICIENT_GOLD", "Saldo emas bebas tidak mencukupi.")
            }
            val tx = Tx(
                id = txId,
                type = TxType.SELL,
                title = "Jual Emas",
                amount = quote.total,
                atMs = now,
                status = "ok",
                method = "Saldo Kas",
                paymentMethod = PaymentMethod.WALLET,
                grams = quote.grams,
                pricePerGram = quote.lockedPricePerGram,
                subtotal = quote.subtotal,
                fee = quote.adminFee,
                tax = quote.tax,
                total = quote.total,
                bursaRef = "BRS-$ymdDate-$randomSuffix",
                kliringRef = "KLR-$ymdDate-$randomSuffix",
                executedAtMs = now + 4000L,
                settledAtMs = now + 120_000L,
            )
            val notif = NotificationItem(
                id = nextId("notif"),
                category = NotificationCategory.TRANSACTION,
                title = "Penjualan emas berhasil",
                message = "${rupiah(quote.total)} dari penjualan ${formatGrams4(quote.grams)} sudah masuk ke Saldo Kas.",
                atMs = now,
                isRead = false,
                txId = tx.id,
            )
            _state.update { s ->
                s.copy(
                    walletBalance = s.walletBalance + quote.total,
                    goldBalanceGrams = (s.goldBalanceGrams - quote.grams).coerceAtLeast(0.0),
                    transactions = listOf(tx) + s.transactions,
                    notifications = listOf(notif) + s.notifications,
                )
            }
            tradeQuotes.remove(quoteId)
            return tx
        }
    }

    fun confirmTradePayment(txId: String): Tx {
        val state = _state.value
        val tx = state.transactions.firstOrNull { it.id == txId } ?: fail("TX_NOT_FOUND", "Transaksi tidak ditemukan.")
        if (tx.status != "pending") fail("INVALID_TX_STATUS", "Status transaksi bukan menunggu pembayaran.")
        val now = clock()
        if (tx.expiresAtMs != null && now >= tx.expiresAtMs) {
            val failedTx = tx.copy(status = "fail", failureReason = "Batas waktu pembayaran habis")
            _state.update { s ->
                s.copy(transactions = s.transactions.map { if (it.id == txId) failedTx else it })
            }
            fail("PAYMENT_EXPIRED", "Batas waktu pembayaran sudah habis.")
        }

        val ymdDate = formatYmd(now)
        val randomSuffix = (random.nextInt(100_000, 999_999)).toString()
        val completedTx = tx.copy(
            status = "ok",
            bursaRef = "BRS-$ymdDate-$randomSuffix",
            kliringRef = "KLR-$ymdDate-$randomSuffix",
            executedAtMs = now + 4000L,
            settledAtMs = now + 120_000L,
        )

        // Khusus transaksi Pesanan Cetak Fisik
        val matchingCetak = state.physicalOrders.firstOrNull { it.txId == tx.id }
        if (matchingCetak != null) {
            val updatedOrder = matchingCetak.copy(status = PhysicalOrderStatus.READY_PICKUP)
            val notif = NotificationItem(
                id = nextId("notif"),
                category = NotificationCategory.TRANSACTION,
                title = "Pesanan cetak emas dibayar",
                message = "Pembayaran cetak emas ${matchingCetak.brand.displayName} ${formatGrams4(matchingCetak.weightGrams)} gr berhasil diterima.",
                atMs = now,
                isRead = false,
                txId = tx.id,
            )
            _state.update { s ->
                s.copy(
                    goldBalanceGrams = floorGrams4((s.goldBalanceGrams - matchingCetak.weightGrams).coerceAtLeast(0.0)),
                    physicalOrders = s.physicalOrders.map { if (it.id == matchingCetak.id) updatedOrder else it },
                    transactions = s.transactions.map { if (it.id == txId) completedTx else it },
                    notifications = listOf(notif) + s.notifications,
                )
            }
            return completedTx
        }

        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Pembelian emas berhasil",
            message = "${formatGrams4(tx.grams)} sudah masuk ke akunmu.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                goldBalanceGrams = s.goldBalanceGrams + tx.grams,
                transactions = s.transactions.map { if (it.id == txId) completedTx else it },
                notifications = listOf(notif) + s.notifications,
            )
        }
        return completedTx
    }

    fun cancelTradeOrder(txId: String, reason: String = ""): Tx {
        val state = _state.value
        val tx = state.transactions.firstOrNull { it.id == txId } ?: fail("TX_NOT_FOUND", "Transaksi tidak ditemukan.")
        val cancelledTx = tx.copy(status = "fail", failureReason = reason)
        _state.update { s ->
            s.copy(transactions = s.transactions.map { if (it.id == txId) cancelledTx else it })
        }
        return cancelledTx
    }

    fun getActiveTransactions(types: List<TxType> = listOf(TxType.BUY, TxType.SELL)): List<Tx> {
        val now = clock()
        return _state.value.transactions.filter {
            types.contains(it.type) && it.isAct(now)
        }
    }

    fun filterTransactions(
        tab: String = "all",
        statusFilter: String = "all",
        rangeDays: Int = 0,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): List<Tx> {
        val now = clock()
        return _state.value.transactions.filter { t ->
            val typeOk = when (tab) {
                "all" -> true
                "beli" -> t.type == TxType.BUY || t.type == TxType.ROUTINE_DEBIT
                "jual" -> t.type == TxType.SELL
                "cetak" -> t.type == TxType.PRINT
                "kas" -> t.type == TxType.TOPUP || t.type == TxType.WITHDRAW
                else -> true
            }
            val statusOk = when (statusFilter) {
                "all" -> true
                "process" -> t.status == "process" || (t.status == "pending" && t.isAct(now))
                "ok" -> t.status == "ok"
                "fail" -> t.status == "fail"
                else -> true
            }
            val rangeOk = when {
                fromMs != null && toMs != null -> t.atMs >= fromMs && t.atMs <= (toMs + 24L * 3600 * 1000L - 1)
                rangeDays > 0 -> t.atMs >= now - rangeDays * 24L * 3600 * 1000L
                else -> true
            }
            typeOk && statusOk && rangeOk
        }
    }

    fun getTransaction(txId: String): Tx? = _state.value.transactions.firstOrNull { it.id == txId }

    fun buyGoldDirect(nominal: Long, grams: Double, method: PaymentMethod): Tx {
        val state = _state.value
        val user = state.user ?: fail("UNAUTHENTICATED", "Silakan masuk terlebih dahulu.")
        if (!user.verified) fail("KYC_REQUIRED", "Verifikasi identitas diperlukan sebelum membeli.")
        if (nominal < MIN_TRADE_RP) fail("INVALID", "Minimal transaksi ${rupiah(MIN_TRADE_RP)}.")
        val calc = calculateTrade(isBuy = true, inputGrams = null, inputSubtotal = nominal, customPrice = null)
        if (method == PaymentMethod.WALLET && state.walletBalance < calc.total) {
            fail("INSUFFICIENT_BALANCE", "Saldo Kas tidak mencukupi.")
        }
        val now = clock()
        val ymdDate = formatYmd(now)
        val randomSuffix = (random.nextInt(100_000, 999_999)).toString()
        val tx = Tx(
            id = nextId("TRX-B"),
            type = TxType.BUY,
            title = "Beli Emas",
            amount = -calc.total,
            atMs = now,
            status = "ok",
            method = method.label,
            paymentMethod = method,
            grams = if (grams > 0.0) grams else calc.grams,
            pricePerGram = state.askPricePerGram,
            subtotal = calc.subtotal,
            fee = calc.adminFee,
            tax = calc.tax,
            total = calc.total,
            bursaRef = "BRS-$ymdDate-$randomSuffix",
            kliringRef = "KLR-$ymdDate-$randomSuffix",
            executedAtMs = now + 4000L,
            settledAtMs = now + 120_000L,
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Pembelian emas berhasil",
            message = "${formatGrams4(tx.grams)} sudah masuk ke akunmu.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = if (method == PaymentMethod.WALLET) s.walletBalance - calc.total else s.walletBalance,
                goldBalanceGrams = s.goldBalanceGrams + tx.grams,
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
        val calc = calculateTrade(isBuy = false, inputGrams = grams, inputSubtotal = null, customPrice = null)
        val now = clock()
        val ymdDate = formatYmd(now)
        val randomSuffix = (random.nextInt(100_000, 999_999)).toString()
        val tx = Tx(
            id = nextId("TRX-S"),
            type = TxType.SELL,
            title = "Jual Emas",
            amount = calc.total,
            atMs = now,
            status = "ok",
            method = "Saldo Kas",
            paymentMethod = PaymentMethod.WALLET,
            grams = calc.grams,
            pricePerGram = state.buybackPerGram,
            subtotal = calc.subtotal,
            fee = calc.adminFee,
            tax = calc.tax,
            total = calc.total,
            bursaRef = "BRS-$ymdDate-$randomSuffix",
            kliringRef = "KLR-$ymdDate-$randomSuffix",
            executedAtMs = now + 4000L,
            settledAtMs = now + 120_000L,
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Penjualan emas berhasil",
            message = "${rupiah(calc.total)} dari penjualan ${formatGrams4(calc.grams)} sudah masuk ke Saldo Kas.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = s.walletBalance + calc.total,
                goldBalanceGrams = (s.goldBalanceGrams - calc.grams).coerceAtLeast(0.0),
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

    // ----- Saldo Kas, Rekening & Portofolio (Handoff §9 & §14) -----

    fun toggleHidePortfolioGold() {
        _state.update { it.copy(hidePortfolioGold = !it.hidePortfolioGold) }
    }

    fun toggleHidePortfolioCash() {
        _state.update { it.copy(hidePortfolioCash = !it.hidePortfolioCash) }
    }

    /** Meminta order Top Up Virtual Account dengan nomor VA statis (Handoff §9 L100 & §14 L135) */
    fun requestTopupVa(bankKey: String): Tx {
        val bank = findSupportedBank(bankKey)
        val now = clock()
        val rawVa = bank.vaPrefix + CUST_VA_NUMBER
        val defaultAmount = 500_000L
        val pm = when (bank.key.lowercase()) {
            "bri" -> PaymentMethod.VA_BRI
            "mandiri" -> PaymentMethod.VA_MANDIRI
            "bni" -> PaymentMethod.VA_BNI
            else -> PaymentMethod.VA_BCA
        }
        val tx = Tx(
            id = nextId("TRX-TU-VA"),
            type = TxType.TOPUP,
            title = "Top up ${bank.name} VA",
            amount = defaultAmount,
            subtotal = defaultAmount,
            fee = TOPUP_FEE,
            total = defaultAmount + TOPUP_FEE,
            atMs = now,
            status = "pending",
            method = "${bank.name} Virtual Account",
            paymentMethod = pm,
            virtualAccountNumber = rawVa,
            expiresAtMs = now + 24 * 3600 * 1000L,
            payRef = "VA-" + (now % 1000000),
        )
        _state.update { s -> s.copy(transactions = listOf(tx) + s.transactions) }
        return tx
    }

    /** Mensimulasikan pembayaran Top Up Virtual Account berhasil (+Rp500.000 saldo kas per prototipe L2114) */
    fun simulateVaTopupSuccess(txId: String, amount: Long): Tx {
        val now = clock()
        val existing = _state.value.transactions.firstOrNull { it.id == txId }
        val effectiveAmount = amount
        val updatedTx = existing?.copy(
            amount = effectiveAmount,
            subtotal = effectiveAmount,
            total = effectiveAmount + TOPUP_FEE,
            status = "ok",
            settledAtMs = now,
        ) ?: Tx(
            id = txId,
            type = TxType.TOPUP,
            title = "Top up Virtual Account",
            amount = effectiveAmount,
            subtotal = effectiveAmount,
            fee = TOPUP_FEE,
            total = effectiveAmount + TOPUP_FEE,
            atMs = now,
            status = "ok",
            settledAtMs = now,
            method = "Virtual Account",
            paymentMethod = PaymentMethod.VIRTUAL_ACCOUNT,
            payRef = "PAY-" + (now % 1000000),
        )

        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Top up berhasil",
            message = "${rupiah(effectiveAmount)} sudah masuk ke Saldo Kas.",
            atMs = now,
            isRead = false,
            txId = updatedTx.id,
        )

        _state.update { s ->
            val otherTxs = s.transactions.filterNot { it.id == txId }
            s.copy(
                walletBalance = s.walletBalance + effectiveAmount,
                transactions = listOf(updatedTx) + otherTxs,
                notifications = listOf(notif) + s.notifications,
            )
        }
        return updatedTx
    }

    /** Membuat tagihan QRIS dinamis 15 menit dengan validasi nominal min Rp10.000 (Handoff §9 L100 & §14 L135) */
    fun createTopupQris(amount: Long): Tx {
        if (amount < MIN_TOPUP_RP) fail("INVALID", "Minimal top up ${rupiah(MIN_TOPUP_RP)}.")
        val now = clock()
        val fee = TOPUP_FEE
        val total = amount + fee
        val qrisPayload = "00020101021226670016ID.CO.QRIS.WWW01189360000000000000010215$CUST_VA_NUMBER"
        val tx = Tx(
            id = nextId("TRX-TU-QR"),
            type = TxType.TOPUP,
            title = "Top up QRIS",
            amount = amount,
            subtotal = amount,
            fee = fee,
            total = total,
            atMs = now,
            status = "pending",
            method = "QRIS",
            paymentMethod = PaymentMethod.QRIS,
            qrisPayload = qrisPayload,
            expiresAtMs = now + 15 * 60 * 1000L,
            payRef = "QR-" + (now % 1000000),
        )
        _state.update { s -> s.copy(transactions = listOf(tx) + s.transactions) }
        return tx
    }

    /** Mensimulasikan pembayaran QRIS berhasil diverifikasi */
    fun simulateQrisPaymentSuccess(txId: String): Tx {
        val now = clock()
        val tx = _state.value.transactions.firstOrNull { it.id == txId }
            ?: fail("NOT_FOUND", "Transaksi QRIS tidak ditemukan.")
        val updatedTx = tx.copy(
            status = "ok",
            settledAtMs = now,
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Top up berhasil",
            message = "${rupiah(tx.amount)} sudah masuk ke Saldo Kas.",
            atMs = now,
            isRead = false,
            txId = updatedTx.id,
        )
        _state.update { s ->
            val otherTxs = s.transactions.filterNot { it.id == txId }
            s.copy(
                walletBalance = s.walletBalance + tx.amount,
                transactions = listOf(updatedTx) + otherTxs,
                notifications = listOf(notif) + s.notifications,
            )
        }
        return updatedTx
    }

    open fun topup(amount: Long, method: PaymentMethod): Tx {
        if (amount < MIN_TOPUP_RP) fail("INVALID", "Minimal top up ${rupiah(MIN_TOPUP_RP)}.")
        val now = clock()
        val fee = TOPUP_FEE
        val tx = Tx(
            id = nextId("TRX-T"),
            type = TxType.TOPUP,
            title = "Top up Saldo Kas",
            amount = amount,
            subtotal = amount,
            fee = fee,
            total = amount + fee,
            atMs = now,
            status = "ok",
            settledAtMs = now,
            method = method.label,
            paymentMethod = method,
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

    /** Memverifikasi dan menambahkan rekening bank nasabah (Handoff §9 L101) */
    open fun verifyAndAddBankAccount(bank: String, number: String, holder: String = "Pengguna"): BankAccount {
        val state = _state.value
        val cleanNumber = number.filter { it.isDigit() }
        if (cleanNumber.length < 6) {
            fail("INVALID_ACCOUNT", "Nomor rekening tidak valid.")
        }
        if (state.bankAccounts.any { it.number.filter { ch -> ch.isDigit() } == cleanNumber }) {
            fail("DUPLICATE_ACCOUNT", "Rekening ini sudah terdaftar di akunmu.")
        }
        val userKtpName = state.user?.name ?: "Budi Santoso"
        val cleanHolder = holder.trim().ifBlank { userKtpName }
        if (!cleanHolder.equals(userKtpName, ignoreCase = true)) {
            fail("MISMATCH_NAME", "Nama pemilik rekening tidak sesuai dengan e-KTP. Gunakan rekening atas namamu sendiri.")
        }
        val now = clock()
        val isFirst = state.bankAccounts.isEmpty()
        val newAccount = BankAccount(
            id = nextId("bank"),
            bank = bank.trim().ifBlank { "BCA" },
            number = cleanNumber,
            holder = cleanHolder,
            addedAtMs = now,
            isPrimary = isFirst,
            isAutoDebitActive = isFirst,
        )
        _state.update { it.copy(bankAccounts = it.bankAccounts + newAccount) }
        return newAccount
    }

    fun addBankAccount(bank: String, number: String, holder: String = "Pengguna") {
        verifyAndAddBankAccount(bank, number, holder)
    }

    fun deleteBankAccount(id: String) {
        _state.update { s ->
            val remaining = s.bankAccounts.filterNot { it.id == id }
            // Jika rekening utama terhapus, jadikan yang pertama sebagai utama
            val adjusted = if (remaining.isNotEmpty() && remaining.none { it.isPrimary }) {
                listOf(remaining.first().copy(isPrimary = true)) + remaining.drop(1)
            } else remaining
            s.copy(bankAccounts = adjusted)
        }
    }

    /** Mengaktifkan fitur Auto-Debit pada rekening bank tertentu melalui verifikasi SMS OTP (Handoff §9 L103) */
    fun toggleAutoDebit(bankAccountId: String, otpCode: String): Boolean {
        if (otpCode.trim() != "123456") {
            fail("INVALID_OTP", "Kode OTP salah atau telah kadaluarsa.")
        }
        var found = false
        _state.update { s ->
            val updated = s.bankAccounts.map { b ->
                if (b.id == bankAccountId) {
                    found = true
                    b.copy(isAutoDebitActive = true)
                } else b
            }
            s.copy(bankAccounts = updated)
        }
        return found
    }

    /** Menarik dana kas ke rekening bank tujuan dengan biaya Rp2.500 di atas nominal (Handoff §9 L102 & §14 L135) */
    open fun requestWithdrawal(amount: Long, bankAccountId: String, pin: String = ""): Tx {
        val state = _state.value
        val account = state.bankAccounts.firstOrNull {
            it.id == bankAccountId || it.id.startsWith(bankAccountId) || it.bank.equals(bankAccountId, ignoreCase = true)
        } ?: fail("INVALID", "Pilih rekening tujuan.")
        if (amount < MIN_WITHDRAW_RP) {
            fail("INVALID", "Minimal penarikan ${rupiah(MIN_WITHDRAW_RP)}.")
        }
        val totalDeduction = amount + WITHDRAW_FEE
        if (totalDeduction > state.walletBalance) {
            fail("INSUFFICIENT_BALANCE", "Saldo Kas tidak mencukupi untuk penarikan dan biaya admin.")
        }
        val now = clock()
        val tx = Tx(
            id = nextId("TRX-W"),
            type = TxType.WITHDRAW,
            title = "Tarik Dana",
            amount = -amount,
            subtotal = amount,
            fee = WITHDRAW_FEE,
            total = totalDeduction,
            atMs = now,
            status = "process", // Handoff §9: Penarikan diproses maks. 1x24 jam
            method = "${account.bank} ${account.maskedNumber}",
            bankAccount = "${account.bank} ${account.maskedNumber}",
            payRef = "WDR-" + (now % 1000000),
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Penarikan dalam proses",
            message = "${rupiah(amount)} dalam proses pengiriman ke ${account.bank} ${account.maskedNumber}, maks 1x24 jam.",
            atMs = now,
            isRead = false,
            txId = tx.id,
        )
        _state.update { s ->
            s.copy(
                walletBalance = s.walletBalance - totalDeduction,
                transactions = listOf(tx) + s.transactions,
                notifications = listOf(notif) + s.notifications,
            )
        }
        return tx
    }

    fun completeWithdrawal(txId: String): Tx {
        val now = clock()
        val tx = _state.value.transactions.firstOrNull { it.id == txId }
            ?: fail("NOT_FOUND", "Transaksi penarikan tidak ditemukan.")
        val updatedTx = tx.copy(
            status = "ok",
            settledAtMs = now,
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Penarikan dana berhasil",
            message = "${rupiah(tx.effectiveSubtotal)} sudah masuk ke rekening ${tx.method}.",
            atMs = now,
            isRead = false,
            txId = updatedTx.id,
        )
        _state.update { s ->
            val others = s.transactions.filterNot { it.id == txId }
            s.copy(
                transactions = listOf(updatedTx) + others,
                notifications = listOf(notif) + s.notifications,
            )
        }
        return updatedTx
    }

    open fun withdraw(amount: Long, bankAccountId: String) {
        val state = _state.value
        val account = state.bankAccounts.firstOrNull {
            it.id == bankAccountId || it.id.startsWith(bankAccountId) || it.bank.equals(bankAccountId, ignoreCase = true)
        } ?: fail("INVALID", "Pilih rekening tujuan.")
        if (amount <= 0) fail("INVALID", "Masukkan nominal penarikan.")
        if (amount > state.walletBalance) fail("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi.")
        val now = clock()
        val tx = Tx(
            id = nextId("TRX-W"),
            type = TxType.WITHDRAW,
            title = "Tarik Dana",
            amount = -amount,
            subtotal = amount,
            fee = WITHDRAW_FEE,
            total = amount,
            atMs = now,
            status = "ok",
            method = "${account.bank} ${account.maskedNumber}",
            bankAccount = "${account.bank} ${account.maskedNumber}",
            payRef = "PAY-" + (now % 1000000),
        )
        val notif = NotificationItem(
            id = nextId("notif"),
            category = NotificationCategory.TRANSACTION,
            title = "Penarikan dana berhasil",
            message = "${rupiah(amount)} sudah masuk ke rekening ${account.bank} ${account.maskedNumber}.",
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

    // ----- Tabungan Rutin -----

    fun createRoutinePlan(name: String, type: RoutinePlanType, targetMonths: Int, targetGrams: Double, monthlyAmount: Long, debitDate: Int, source: String): RoutineSavingsPlan {
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

    fun getRoutinePlan(planId: String): RoutineSavingsPlan? =
        _state.value.routinePlans.firstOrNull { it.id == planId }

    fun executeRoutineDebit(planId: String): Tx {
        val s = _state.value
        val plan = s.routinePlans.firstOrNull { it.id == planId } ?: fail("PLAN_NOT_FOUND", "Plan tidak ditemukan.")
        if (plan.status == RoutinePlanStatus.PAUSED) fail("PLAN_PAUSED", "Plan tabungan sedang dijeda.")
        if (s.walletBalance < plan.monthlyAmount) fail("INSUFFICIENT_BALANCE", "Saldo kas tidak mencukupi untuk auto-debit.")

        val now = clock()
        val grams = floorGrams4(plan.monthlyAmount.toDouble() / s.askPricePerGram)
        val mutation = RoutineMutation(
            id = nextId("MUT"),
            planId = plan.id,
            atMs = now,
            grams = grams,
            amount = plan.monthlyAmount,
            pricePerGram = s.askPricePerGram,
            method = "Tabungan rutin",
        )
        val tx = Tx(
            id = nextId("TX"),
            type = TxType.BUY,
            title = "Tabungan rutin: ${plan.name}",
            amount = -plan.monthlyAmount,
            atMs = now,
            subtotal = plan.monthlyAmount,
            grams = grams,
            pricePerGram = s.askPricePerGram,
            method = "Tabungan rutin",
            planId = plan.id,
        )
        val notif = NotificationItem(
            id = nextId("NOTIF"),
            category = NotificationCategory.TRANSACTION,
            title = "Debit tabungan rutin berhasil",
            message = "Berhasil menabung ${formatGrams4(grams)} gr untuk ${plan.name}",
            atMs = now,
            txId = tx.id,
        )

        _state.update { curr ->
            val updatedPlans = curr.routinePlans.map {
                if (it.id == planId) it.copy(
                    savedGrams = floorGrams4(it.savedGrams + grams),
                    mutations = listOf(mutation) + it.mutations,
                ) else it
            }
            curr.copy(
                walletBalance = curr.walletBalance - plan.monthlyAmount,
                goldBalanceGrams = floorGrams4(curr.goldBalanceGrams + grams),
                routinePlans = updatedPlans,
                transactions = listOf(tx) + curr.transactions,
                notifications = listOf(notif) + curr.notifications,
            )
        }
        return tx
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
    open fun refreshGoldPrice(formattedHm: String? = null, deltaOverride: Long? = null): String? {
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

    open suspend fun fetchGoldPrice(): PriceOut? = null

    open suspend fun fetchGoldPriceHistory(limit: Int = 500): List<PriceOut> = emptyList()

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
            PrintMethod.JNE, PrintMethod.DELIVERY -> address?.trim()?.takeIf { it.isNotEmpty() } ?: fail("INVALID", "Isi alamat pengiriman.")
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

    fun createInstallmentQuote(
        grams: Double,
        tenorMonths: Int,
        dpPercent: Int,
        paymentMode: String = "auto",
        fundingSource: String = "kas",
    ): InstallmentQuote {
        val s = _state.value
        val goldValue = kotlin.math.round(grams * s.askPricePerGram).toLong()
        if (goldValue < CC_MIN_VAL) fail("INVALID_AMOUNT", "Minimal harga emas Rp500.000 untuk pengajuan cicil.")
        val calc = calculateInstallment(grams, s.askPricePerGram, tenorMonths, dpPercent)
        val now = clock()
        val quote = InstallmentQuote(
            id = nextId("CCQ"),
            grams = grams,
            pricePerGram = s.askPricePerGram,
            calculation = calc,
            paymentMode = paymentMode,
            fundingSource = fundingSource,
            lockedAtMs = now,
            expiresAtMs = now + CC_LOCK_WINDOW_MIN * 60 * 1000L,
        )
        installmentQuotes[quote.id] = quote
        return quote
    }

    fun getInstallmentQuote(quoteId: String): InstallmentQuote? = installmentQuotes[quoteId]

    fun verifyScoring(quoteId: String, isSimulatedPass: Boolean): Boolean {
        if (!installmentQuotes.containsKey(quoteId)) fail("ORDER_NOT_FOUND", "Pengajuan tidak ditemukan.")
        return isSimulatedPass
    }

    fun signAndPayInstallmentAkad(
        quoteId: String,
        smsOtp: String,
        paymentMethod: PaymentMethod,
        pin: String,
    ): GoldinInstallmentContract {
        val quote = installmentQuotes[quoteId] ?: fail("ORDER_NOT_FOUND", "Pengajuan tidak ditemukan.")
        if (smsOtp != "123456") fail("INVALID_OTP", "Kode OTP SMS salah.")
        if (!verifyPin(pin)) fail("WRONG_PIN", "PIN salah. Coba lagi.")

        val s = _state.value
        val totalDp = quote.calculation.totalDownPayment
        if (paymentMethod == PaymentMethod.WALLET && s.walletBalance < totalDp) {
            fail("INSUFFICIENT_BALANCE", "Saldo kas tidak mencukupi untuk pembayaran uang muka.")
        }

        val now = clock()
        val schedule = (1..quote.calculation.tenorMonths).map { monthIdx ->
            InstallmentScheduleItem(
                monthNumber = monthIdx,
                dueDateMs = calculateInstallmentDueDate(now, monthIdx, 5),
                amount = quote.calculation.monthlyInstallment,
                isPaid = false,
            )
        }

        val contract = GoldinInstallmentContract(
            id = nextId("CTR"),
            partner = CC_PARTNER_NAME,
            goldGrams = quote.grams,
            goldPriceAtAgreement = quote.pricePerGram,
            goldValue = quote.calculation.goldValue,
            downPayment = quote.calculation.downPaymentAmount,
            adminFee = quote.calculation.adminFee,
            principal = quote.calculation.principal,
            margin = quote.calculation.margin,
            tenorMonths = quote.calculation.tenorMonths,
            downPaymentPercent = quote.calculation.downPaymentPercent,
            monthlyInstallment = quote.calculation.monthlyInstallment,
            paidMonths = 0,
            paymentMode = quote.paymentMode,
            autoDebitSource = quote.fundingSource,
            dueDay = 5,
            agreedAtMs = now,
            status = "active",
            isPaidOff = false,
            schedule = schedule,
        )

        val tx = Tx(
            id = nextId("TX"),
            type = TxType.INSTALLMENT,
            title = "Uang muka cicil emas ${formatGrams4(quote.grams)} gr",
            amount = if (paymentMethod == PaymentMethod.WALLET) -totalDp else 0L,
            atMs = now,
            subtotal = totalDp,
            grams = quote.grams,
        )

        val notif = NotificationItem(
            id = nextId("NOTIF"),
            category = NotificationCategory.TRANSACTION,
            title = "Pengajuan cicil emas disetujui",
            message = "Kontrak cicil emas ${formatGrams4(quote.grams)} gr telah aktif.",
            atMs = now,
            txId = tx.id,
        )

        _state.update { curr ->
            curr.copy(
                walletBalance = if (paymentMethod == PaymentMethod.WALLET) curr.walletBalance - totalDp else curr.walletBalance,
                lockedGoldGrams = floorGrams4(curr.lockedGoldGrams + quote.grams),
                installmentContracts = listOf(contract) + curr.installmentContracts,
                transactions = listOf(tx) + curr.transactions,
                notifications = listOf(notif) + curr.notifications,
            )
        }

        installmentQuotes.remove(quoteId)
        return contract
    }

    fun getInstallmentContract(contractId: String): GoldinInstallmentContract? =
        _state.value.installmentContracts.firstOrNull { it.id == contractId }

    fun payInstallmentMonthly(
        contractId: String,
        paymentMethod: PaymentMethod,
        pin: String,
    ): GoldinInstallmentContract {
        val s = _state.value
        val contract = s.installmentContracts.firstOrNull { it.id == contractId }
            ?: fail("ORDER_NOT_FOUND", "Kontrak cicil tidak ditemukan.")
        if (contract.isDone || contract.paidMonths >= contract.tenorMonths) {
            fail("ALREADY_PAID", "Cicilan sudah lunas.")
        }
        if (!verifyPin(pin)) fail("WRONG_PIN", "PIN salah. Coba lagi.")

        val amount = contract.monthlyInstallment
        if (paymentMethod == PaymentMethod.WALLET && s.walletBalance < amount) {
            fail("INSUFFICIENT_BALANCE", "Saldo Kas tidak cukup untuk membayar angsuran.")
        }

        val now = clock()
        val nextMonth = contract.paidMonths + 1
        val isDone = nextMonth >= contract.tenorMonths

        val tx = Tx(
            id = nextId("TX"),
            type = TxType.INSTALLMENT,
            title = "Angsuran ke-$nextMonth cicil emas ${formatGrams4(contract.goldGrams)} gr",
            amount = if (paymentMethod == PaymentMethod.WALLET) -amount else 0L,
            atMs = now,
            subtotal = amount,
        )

        var updated: GoldinInstallmentContract? = null

        _state.update { curr ->
            val list = curr.installmentContracts.map {
                if (it.id == contractId) {
                    val newSchedule = it.schedule.map { item ->
                        if (item.monthNumber == nextMonth) item.copy(isPaid = true, paidAtMs = now, txId = tx.id)
                        else item
                    }
                    val u = it.copy(
                        paidMonths = nextMonth,
                        status = if (isDone) "done" else "active",
                        isPaidOff = isDone,
                        schedule = newSchedule,
                    )
                    updated = u
                    u
                } else it
            }

            val notifs = if (isDone) {
                listOf(
                    NotificationItem(
                        id = nextId("NOTIF"),
                        category = NotificationCategory.TRANSACTION,
                        title = "Cicilan emas lunas",
                        message = "Selamat! Cicilan emas ${formatGrams4(contract.goldGrams)} gr telah lunas dan kini masuk ke portofolio aktif.",
                        atMs = now,
                    )
                ) + curr.notifications
            } else curr.notifications

            curr.copy(
                walletBalance = if (paymentMethod == PaymentMethod.WALLET) curr.walletBalance - amount else curr.walletBalance,
                lockedGoldGrams = if (isDone) floorGrams4((curr.lockedGoldGrams - contract.goldGrams).coerceAtLeast(0.0)) else curr.lockedGoldGrams,
                goldBalanceGrams = if (isDone) floorGrams4(curr.goldBalanceGrams + contract.goldGrams) else curr.goldBalanceGrams,
                installmentContracts = list,
                transactions = listOf(tx) + curr.transactions,
                notifications = notifs,
            )
        }

        return updated ?: contract
    }

    fun payoffInstallmentContract(
        contractId: String,
        paymentMethod: PaymentMethod,
        pin: String,
    ): GoldinInstallmentContract {
        val s = _state.value
        val contract = s.installmentContracts.firstOrNull { it.id == contractId }
            ?: fail("ORDER_NOT_FOUND", "Kontrak cicil tidak ditemukan.")
        if (contract.isDone || contract.paidMonths >= contract.tenorMonths) {
            fail("ALREADY_PAID", "Cicilan sudah lunas.")
        }
        if (!verifyPin(pin)) fail("WRONG_PIN", "PIN salah. Coba lagi.")

        val remainingMonths = contract.tenorMonths - contract.paidMonths
        val remainingTotal = contract.monthlyInstallment * remainingMonths
        if (paymentMethod == PaymentMethod.WALLET && s.walletBalance < remainingTotal) {
            fail("INSUFFICIENT_BALANCE", "Saldo Kas tidak cukup untuk pelunasan dipercepat.")
        }

        val now = clock()
        val tx = Tx(
            id = nextId("TX"),
            type = TxType.INSTALLMENT,
            title = "Pelunasan dipercepat cicil emas ${formatGrams4(contract.goldGrams)} gr",
            amount = if (paymentMethod == PaymentMethod.WALLET) -remainingTotal else 0L,
            atMs = now,
            subtotal = remainingTotal,
        )

        var updated: GoldinInstallmentContract? = null

        _state.update { curr ->
            val list = curr.installmentContracts.map {
                if (it.id == contractId) {
                    val newSchedule = it.schedule.map { item ->
                        if (!item.isPaid) item.copy(isPaid = true, paidAtMs = now, txId = tx.id)
                        else item
                    }
                    val u = it.copy(
                        paidMonths = it.tenorMonths,
                        status = "done",
                        isPaidOff = true,
                        schedule = newSchedule,
                    )
                    updated = u
                    u
                } else it
            }

            val notif = NotificationItem(
                id = nextId("NOTIF"),
                category = NotificationCategory.TRANSACTION,
                title = "Cicilan emas lunas",
                message = "Selamat! Cicilan emas ${formatGrams4(contract.goldGrams)} gr telah lunas dan kini masuk ke portofolio aktif.",
                atMs = now,
                txId = tx.id,
            )

            curr.copy(
                walletBalance = if (paymentMethod == PaymentMethod.WALLET) curr.walletBalance - remainingTotal else curr.walletBalance,
                lockedGoldGrams = floorGrams4((curr.lockedGoldGrams - contract.goldGrams).coerceAtLeast(0.0)),
                goldBalanceGrams = floorGrams4(curr.goldBalanceGrams + contract.goldGrams),
                installmentContracts = list,
                transactions = listOf(tx) + curr.transactions,
                notifications = listOf(notif) + curr.notifications,
            )
        }

        return updated ?: contract
    }

    // =========================================================================================
    // CETAK EMAS FISIK & RESERVASI CABANG RAJA EMAS (TAHAP 9)
    // =========================================================================================

    open fun getGoldBranches(): List<GoldBranch> = officialGoldBranches

    open fun getGoldBranch(id: String): GoldBranch? = officialGoldBranches.firstOrNull { it.id == id }

    open suspend fun fetchCatalogSkus(): com.reidig.shared.api.CatalogOut? = null

    open suspend fun fetchBranches(): List<com.reidig.shared.api.BranchOut> = emptyList()

    open suspend fun fetchWallet(): com.reidig.shared.api.WalletOut? = null

    open suspend fun fetchWalletEntries(): List<com.reidig.shared.api.EntryOut> = emptyList()

    open suspend fun fetchWithdrawals(): List<com.reidig.shared.api.WithdrawalOut> = emptyList()

    open suspend fun fetchOrders(): List<com.reidig.shared.api.OrderOut> = emptyList()

    open suspend fun fetchTokens(includeClosed: Boolean = false): List<com.reidig.shared.api.TokenOut> = emptyList()

    fun getGoldBarStock(): List<GoldBarStock> {
        val sizes = listOf(1.0, 2.0, 3.0, 5.0, 10.0, 25.0, 50.0, 100.0)
        return GoldBrand.entries.flatMap { brand ->
            sizes.map { size ->
                val inStock = isStockAvailable(brand, size)
                GoldBarStock(brand, size, inStock)
            }
        }
    }

    open fun isStockAvailable(brand: GoldBrand, weightGrams: Double): Boolean {
        if (brand == GoldBrand.LM_REI) {
            val mg = (weightGrams * 1000).toLong()
            val match = _state.value.skus.firstOrNull { it.weightMg == mg }
            if (match != null) return match.stock > 0
        }
        val weight = weightGrams.toInt()
        if (brand == GoldBrand.ANTAM && weight == 50) return false
        if (brand == GoldBrand.GALERI_24 && (weight == 3 || weight == 100)) return false
        return true
    }

    fun getPhysicalPrintOrders(): List<PhysicalPrintOrder> = _state.value.physicalOrders

    fun getPhysicalPrintOrder(id: String): PhysicalPrintOrder? = _state.value.physicalOrders.firstOrNull { it.id == id }

    fun createPhysicalPrintOrder(brand: GoldBrand, weightGrams: Double, method: PrintMethod, branchId: String? = null, deliveryAddress: String? = null, recipientName: String? = null, recipientPhone: String? = null, paymentMethod: PaymentMethod = PaymentMethod.WALLET, pin: String): PhysicalPrintOrder {
        val s = _state.value
        if (weightGrams <= 0.0) fail("INVALID", "Berat keping emas tidak valid.")
        if (!isStockAvailable(brand, weightGrams)) fail("OUT_OF_STOCK", "Stok keping ini sedang habis.")
        if (s.freeGoldGrams < weightGrams) fail("INSUFFICIENT_GOLD", "Saldo emas bebas tidak mencukupi untuk cetak.")
        if (!verifyPin(pin)) fail("WRONG_PIN", "PIN salah. Coba lagi.")

        val isKas = paymentMethod == PaymentMethod.WALLET
        val isBranch = method == PrintMethod.BRANCH
        val branch = if (isBranch) getGoldBranch(branchId ?: "kwc") ?: officialGoldBranches.first() else null
        val printFee = 50_000L
        val shippingFee = if (isBranch) 0L else 35_000L
        val totalFee = printFee + shippingFee

        if (isKas && s.walletBalance < totalFee) {
            fail("INSUFFICIENT_BALANCE", "Saldo Kas tidak cukup untuk membayar biaya cetak.")
        }

        val now = clock()
        val ymd = formatYmd(now).replace("-", "")
        val hexSuffix = ((now / 1000L % 9000L) + 1000L).toString()
        val orderCode = "CTK-$ymd-$hexSuffix"
        val orderId = orderCode
        val serialNumber = brand.serialPrefix + "-" + ((serialCounter++) % 9000000 + 1000000)
        val ticketCode = orderCode
        val qrPayload = "GOLDIN:PRINT:$orderId:$ticketCode"
        val trackingNumber = if (!isBranch) "RE" + ((serialCounter * 7) % 9000000000L + 1000000000L) else null

        val vaNumber = when (paymentMethod) {
            PaymentMethod.VA_BCA -> "3935 8019 2498 2261"
            PaymentMethod.VA_BRI -> "8801 9249 8226 1234"
            PaymentMethod.VA_MANDIRI -> "8902 2498 2261 4567"
            PaymentMethod.VA_BNI -> "9882 4982 2614 7890"
            else -> "3935 8019 2498 2261"
        }

        val tx = Tx(
            id = nextId("TX"),
            type = TxType.WITHDRAW,
            title = "Cetak emas ${brand.displayName} ${formatGrams4(weightGrams)} gr",
            amount = if (isKas) -totalFee else 0L,
            atMs = now,
            status = if (isKas) "ok" else "pending",
            method = paymentMethod.label,
            paymentMethod = paymentMethod,
            grams = weightGrams,
            subtotal = printFee,
            fee = shippingFee,
            total = totalFee,
            expiresAtMs = if (!isKas) now + 15 * 60 * 1000L else null,
            virtualAccountNumber = if (paymentMethod.isVa) vaNumber else null,
            brand = brand.displayName,
            destination = if (isBranch) (branch?.name ?: "Raja Emas BSD") else deliveryAddress,
        )

        val notif = NotificationItem(
            id = nextId("NOTIF"),
            category = NotificationCategory.TRANSACTION,
            title = if (isKas) "Pesanan cetak emas dibuat" else "Menunggu pembayaran cetak emas",
            message = if (isKas) "Pesanan cetak ${brand.displayName} ${formatGrams4(weightGrams)} gr berhasil dibuat." else "Selesaikan pembayaran biaya cetak ${brand.displayName} sebelum batas waktu.",
            atMs = now,
            txId = tx.id,
        )

        val order = PhysicalPrintOrder(
            id = orderId,
            orderNumber = orderId,
            brand = brand,
            weightGrams = weightGrams,
            quantity = 1,
            method = method,
            branchId = branch?.id,
            branchName = branch?.name ?: if (isBranch) "Raja Emas BSD" else null,
            branchAddress = branch?.address,
            destinationAddress = deliveryAddress,
            recipientName = recipientName ?: s.user?.name,
            recipientPhone = recipientPhone ?: s.user?.phone,
            status = if (isKas) PhysicalOrderStatus.READY_PICKUP else PhysicalOrderStatus.CREATED,
            ticketCode = ticketCode,
            qrCodePayload = qrPayload,
            printFee = printFee,
            shippingInsuranceFee = shippingFee,
            totalPayment = totalFee,
            serialNumber = serialNumber,
            trackingNumber = trackingNumber,
            pickupScheduleDate = null,
            pickupScheduleTimeSlot = null,
            paymentMethod = paymentMethod,
            createdAtMs = now,
            txId = tx.id,
        )

        _state.update { curr ->
            curr.copy(
                goldBalanceGrams = if (isKas) floorGrams4((curr.goldBalanceGrams - weightGrams).coerceAtLeast(0.0)) else curr.goldBalanceGrams,
                walletBalance = if (isKas) curr.walletBalance - totalFee else curr.walletBalance,
                physicalOrders = listOf(order) + curr.physicalOrders,
                transactions = listOf(tx) + curr.transactions,
                notifications = listOf(notif) + curr.notifications,
            )
        }

        return order
    }

    fun updatePickupSchedule(orderId: String, date: String, slot: String): PhysicalPrintOrder {
        var updated: PhysicalPrintOrder? = null
        _state.update { curr ->
            val list = curr.physicalOrders.map {
                if (it.id == orderId) {
                    val u = it.copy(pickupScheduleDate = date, pickupScheduleTimeSlot = slot)
                    updated = u
                    u
                } else it
            }
            curr.copy(physicalOrders = list)
        }
        return updated ?: fail("ORDER_NOT_FOUND", "Pesanan cetak tidak ditemukan.")
    }

    fun addSavedAddress(address: SavedAddress) {
        _state.update { curr ->
            val updatedList = if (address.isPrimary) {
                curr.savedAddresses.map { it.copy(isPrimary = false) } + address
            } else {
                curr.savedAddresses + address
            }
            curr.copy(savedAddresses = updatedList)
        }
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
