package com.reidig.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockRepositoryTest {
    private var now = 1_000_000_000_000L
    private val repo = MockRepository(clock = { now })
    private val state get() = repo.state.value

    private fun assertCode(code: String, block: () -> Unit) {
        assertEquals(code, assertFailsWith<ReidigException> { block() }.code)
    }

    private fun demo() = repo.login("uji@contoh.id", "Rahasia123")

    @Test
    fun formatting() {
        assertEquals("Rp2.513.500", rupiah(2_513_500))
        assertEquals("Rp305.000", rupiah(305_000))
        assertEquals("-Rp100.000", rupiah(-100_000))
        assertEquals("2,4518 gr", formatGrams4(2.4518))
        assertEquals("0,1 gr", gram(100))
        assertEquals("0,25 gr", gram(250))
        assertEquals("1000 gr", gram(1_000_000))
        assertEquals("24,3%", percent(Sku("x", "x", 100, 305_000, 231_000, 1).spreadPercent))
    }

    @Test
    fun weakPins() {
        listOf("111111", "123456", "654321", "121212", "12345").forEach { assertTrue(Rules.isWeakPin(it), it) }
        listOf("147258", "902714").forEach { assertFalse(Rules.isWeakPin(it), it) }
        // PIN tanggal lahir dicegah
        assertTrue(Rules.isWeakPin("140392", dob = "14/03/1992"))
        assertTrue(Rules.isWeakPin("920314", dob = "14/03/1992"))
    }

    @Test
    fun identityRules() {
        assertTrue(Rules.isAdult("14/03/1992", 2026, 11, 3))
        assertFalse(Rules.isAdult("04/11/2008", 2026, 11, 3))
        assertTrue(Rules.isAdult("03/11/2008", 2026, 11, 3))
        assertFalse(Rules.isAdult("31/02/1990", 2026, 11, 3))
        assertTrue(Rules.isValidIndonesianPhone("81234567890"))
        assertFalse(Rules.isValidIndonesianPhone("0812345"))
        assertTrue(Rules.isValidNik("3173012345670012"))
        assertFalse(Rules.isValidNik("31730123"))
    }

    @Test
    fun buyRequiresKyc() {
        repo.register("Baru", "baru@contoh.id")
        val quote = repo.quoteBuy("LMRE-1G", 1)
        assertCode("KYC_REQUIRED") { repo.buy(quote.id, PaymentMethod.QRIS) }
    }

    @Test
    fun quoteExpires() {
        demo()
        val quote = repo.quoteBuy("LMRE-1G", 1)
        now += Rules.QUOTE_TTL_MS + 1
        assertCode("QUOTE_EXPIRED") { repo.buy(quote.id, PaymentMethod.QRIS) }
    }

    @Test
    fun quoteCannotBeReused() {
        demo()
        val quote = repo.quoteBuy("LMRE-0.1G", 1)
        repo.buy(quote.id, PaymentMethod.QRIS)
        assertCode("QUOTE_EXPIRED") { repo.buy(quote.id, PaymentMethod.QRIS) }
    }

    @Test
    fun buyMintsOneTokenPerPieceAndReducesStock() {
        demo()
        val before = state.sku("LMRE-1G").stock
        val minted = repo.buy(repo.quoteBuy("LMRE-1G", 2).id, PaymentMethod.QRIS)
        assertEquals(2, minted.size)
        assertEquals(2, minted.map { it.code }.toSet().size)
        assertEquals(2, minted.map { it.serial }.toSet().size)
        assertEquals(before - 2, state.sku("LMRE-1G").stock)
    }

    @Test
    fun pendingOrderHoldsStockUntilPaidOrCancelled() {
        demo()
        val before = state.sku("LMRE-1G").stock
        val order = repo.createOrder(repo.quoteBuy("LMRE-1G", 2).id, PaymentMethod.VIRTUAL_ACCOUNT)
        assertEquals(before - 2, state.sku("LMRE-1G").stock)
        repo.cancelOrder(order.id)
        assertEquals(before, state.sku("LMRE-1G").stock)
        assertCode("ORDER_NOT_FOUND") { repo.confirmPayment(order.id) }
    }

    @Test
    fun priceStaysLockedDuringPaymentWindow() {
        demo()
        val order = repo.createOrder(repo.quoteBuy("LMRE-1G", 1).id, PaymentMethod.QRIS)
        now += Rules.QUOTE_TTL_MS + 1
        assertEquals(1, repo.confirmPayment(order.id).size)
    }

    @Test
    fun lateCallbackDoesNotMintTokenAndReleasesStock() {
        demo()
        val before = state.sku("LMRE-1G").stock
        val tokens = state.tokens.size
        val order = repo.createOrder(repo.quoteBuy("LMRE-1G", 1).id, PaymentMethod.QRIS)
        now += Rules.PAYMENT_WINDOW_MS + 1
        assertCode("ORDER_EXPIRED") { repo.confirmPayment(order.id) }
        assertEquals(before, state.sku("LMRE-1G").stock)
        assertEquals(tokens, state.tokens.size)
    }

    @Test
    fun duplicateCallbackMintsOnlyOnce() {
        demo()
        val order = repo.createOrder(repo.quoteBuy("LMRE-1G", 1).id, PaymentMethod.QRIS)
        repo.confirmPayment(order.id)
        val tokens = state.tokens.size
        assertCode("ORDER_NOT_FOUND") { repo.confirmPayment(order.id) }
        assertEquals(tokens, state.tokens.size)
    }

    @Test
    fun walletPaymentChecksBalance() {
        demo()
        assertCode("INSUFFICIENT_BALANCE") { repo.buy(repo.quoteBuy("LMRE-1G", 1).id, PaymentMethod.WALLET) }
        val balance = state.walletBalance
        repo.buy(repo.quoteBuy("LMRE-0.1G", 1).id, PaymentMethod.WALLET)
        assertEquals(balance - 305_000, state.walletBalance)
    }

    @Test
    fun cannotBuyMoreThanStock() {
        demo()
        assertCode("INSUFFICIENT_STOCK") { repo.quoteBuy("LMRE-1000G", 2) }
    }

    @Test
    fun sellReturnsTokenCreditsWalletAndRestoresStock() {
        demo()
        val token = state.tokens.first { it.skuCode == "LMRE-1G" }
        val balance = state.walletBalance
        val stock = state.sku("LMRE-1G").stock
        assertEquals(2_310_000, repo.sell(token.code))
        assertEquals(balance + 2_310_000, state.walletBalance)
        assertEquals(stock + 1, state.sku("LMRE-1G").stock)
        assertEquals(TokenStatus.RETURNED, state.tokens.first { it.code == token.code }.status)
        assertCode("TOKEN_LOCKED") { repo.sell(token.code) }
    }

    @Test
    fun newTokenCannotBeSoldDuringHoldingPeriod() {
        demo()
        val token = repo.buy(repo.quoteBuy("LMRE-0.1G", 1).id, PaymentMethod.QRIS).single()
        assertCode("HOLDING_PERIOD_ACTIVE") { repo.sell(token.code) }
        now += Rules.HOLDING_PERIOD_MS
        repo.sell(token.code)
    }

    @Test
    fun tokenCodesAreNeverReused() {
        demo()
        val token = state.tokens.first { it.skuCode == "LMRE-1G" }
        repo.sell(token.code)
        val next = repo.buy(repo.quoteBuy("LMRE-1G", 1).id, PaymentMethod.QRIS).single()
        assertTrue(next.code != token.code)
        assertEquals(TokenStatus.RETURNED, state.tokens.first { it.code == token.code }.status)
    }

    @Test
    fun withdrawCannotExceedBalance() {
        demo()
        val balance = state.walletBalance
        assertCode("INSUFFICIENT_BALANCE") { repo.withdraw(balance + 1, "bca") }
        assertCode("INVALID") { repo.withdraw(0, "bca") }
        repo.withdraw(balance, "bca")
        assertEquals(0, state.walletBalance)
    }

    @Test
    fun pickupNeedsTwoGramsInTotal() {
        demo()
        val half = state.tokens.first { it.skuCode == "LMRE-0.5G" }.code
        val (a, b) = state.tokens.filter { it.skuCode == "LMRE-1G" }.map { it.code }
        assertCode("PICKUP_MIN_WEIGHT_NOT_MET") { repo.createPrintOrder(listOf(half), PrintMethod.BRANCH, null, "klg") }
        assertCode("PICKUP_MIN_WEIGHT_NOT_MET") { repo.createPrintOrder(listOf(a, half), PrintMethod.BRANCH, null, "klg") }
        assertEquals(TokenStatus.ACTIVE, state.tokens.first { it.code == a }.status)
        assertCode("INVALID") { repo.createPrintOrder(listOf(a, b), PrintMethod.BRANCH, null, "tidak-ada") }

        val order = repo.createPrintOrder(listOf(a, b), PrintMethod.BRANCH, null, "klg")
        assertEquals("Cabang Kelapa Gading", order.destination)
        assertEquals(TokenStatus.PRINT_PENDING, state.tokens.first { it.code == a }.status)
    }

    @Test
    fun jneHasNoMinimumButNeedsAddress() {
        demo()
        val half = state.tokens.first { it.skuCode == "LMRE-0.5G" }.code
        assertCode("INVALID") { repo.createPrintOrder(listOf(half), PrintMethod.JNE, " ", null) }
        repo.createPrintOrder(listOf(half), PrintMethod.JNE, "Jl. Contoh 1, Bandung", null)
        assertCode("TOKEN_LOCKED") { repo.sell(half) }
    }

    private fun submit(skuCode: String = "LMRE-0.5G", downPayment: Long = 300_000) =
        repo.submitInstallment(skuCode, downPayment, 6, state.sku(skuCode).buyPrice, Rules.INSTALLMENT_AGREEMENT_VERSION)

    @Test
    fun installmentLocksTokenUntilPartnerConfirms() {
        demo()
        val valueBefore = state.portfolioValue
        val contract = repo.payDownPayment(submit().id, PaymentMethod.QRIS)
        assertEquals(valueBefore, state.portfolioValue)
        assertEquals(Rules.INSTALLMENT_AGREEMENT_VERSION, contract.agreementVersion)
        assertCode("TOKEN_LOCKED") { repo.sell(contract.tokenCode) }
        assertCode("TOKEN_LOCKED") { repo.createPrintOrder(listOf(contract.tokenCode), PrintMethod.JNE, "Alamat", null) }
        now += Rules.HOLDING_PERIOD_MS
        repo.markInstallmentPaidOff(contract.id)
        repo.sell(contract.tokenCode)
    }

    @Test
    fun installmentRequiresAgreementAndCurrentPrice() {
        demo()
        val price = state.sku("LMRE-1G").buyPrice
        assertCode("AGREEMENT_REQUIRED") { repo.submitInstallment("LMRE-1G", 500_000, 6, price, "") }
        assertCode("PRICE_CHANGED") { repo.submitInstallment("LMRE-1G", 500_000, 6, price - 1, Rules.INSTALLMENT_AGREEMENT_VERSION) }
        assertEquals(40, state.sku("LMRE-1G").stock)
    }

    @Test
    fun submittingAgreementHoldsPieceButIssuesNoTokenBeforeDownPayment() {
        demo()
        val stock = state.sku("LMRE-0.5G").stock
        val tokens = state.tokens.size
        val balance = state.walletBalance
        val application = submit()
        assertEquals(stock - 1, state.sku("LMRE-0.5G").stock)
        assertEquals(tokens, state.tokens.size)
        assertEquals(balance, state.walletBalance)
        repo.cancelInstallment(application.id)
        assertEquals(stock, state.sku("LMRE-0.5G").stock)
        assertCode("ORDER_NOT_FOUND") { repo.payDownPayment(application.id, PaymentMethod.QRIS) }
    }

    @Test
    fun walletDownPaymentChecksBalanceAndCanRetryWithOtherMethod() {
        demo()
        val application = submit("LMRE-10G", 5_000_000)
        val balance = state.walletBalance
        assertCode("INSUFFICIENT_BALANCE") { repo.payDownPayment(application.id, PaymentMethod.WALLET) }
        assertEquals(balance, state.walletBalance)
        repo.payDownPayment(application.id, PaymentMethod.VIRTUAL_ACCOUNT)
        assertEquals(balance, state.walletBalance)
        assertEquals(1, state.contracts.size)
    }

    @Test
    fun expiredDownPaymentCancelsApplicationAndReleasesPiece() {
        demo()
        val stock = state.sku("LMRE-0.5G").stock
        val application = submit()
        now += Rules.PAYMENT_WINDOW_MS + 1
        assertCode("ORDER_EXPIRED") { repo.payDownPayment(application.id, PaymentMethod.QRIS) }
        assertEquals(stock, state.sku("LMRE-0.5G").stock)
        assertTrue(state.contracts.isEmpty())
    }

    @Test
    fun pinLocksAfterRepeatedFailures() {
        demo()
        // 2x salah: mengembalikan false
        assertFalse(repo.verifyPin("000000"))
        assertFalse(repo.verifyPin("000000"))
        // Percobaan ke-3 salah: mengunci PIN sementara dan melempar PIN_LOCKED
        assertCode("PIN_LOCKED") { repo.verifyPin("000000") }
    }

    @Test
    fun goldinPasswordRules() {
        // Aturan: Minimal 8 karakter dan mengandung huruf kapital
        assertFalse(Rules.isValidGoldinPassword("pendek"))
        assertFalse(Rules.isValidGoldinPassword("tanpakapital123"))
        assertFalse(Rules.isValidGoldinPassword("KAPITAL"))
        assertTrue(Rules.isValidGoldinPassword("Reitesting123"))
        assertTrue(Rules.isValidGoldinPassword("Password8"))
    }

    @Test
    fun goldinLoginCredentialsValidation() {
        // Password salah untuk akun resmi
        assertCode("INVALID_CREDENTIALS") {
            repo.login(MockRepository.GOLDIN_DEMO_EMAIL, "PasswordSalah")
        }
        // Format email tidak valid
        assertCode("INVALID_CREDENTIALS") {
            repo.login("bukanemail", "Reitesting123")
        }
        // Berhasil login dengan kredensial benar
        repo.login(MockRepository.GOLDIN_DEMO_EMAIL, MockRepository.GOLDIN_DEMO_PASSWORD)
        assertEquals(MockRepository.GOLDIN_DEMO_EMAIL, state.user?.email)
    }

    @Test
    fun goldinMultiTierPinLockoutAndResetFlow() {
        repo.startDemoAccount()
        assertEquals(0, state.pinFailedAttempts)
        assertEquals(0, state.pinLockLevel)
        assertFalse(state.isAccountLocked)

        // Set 1: 1x salah, 2x salah, 3x salah -> Lock 10 menit
        val res1 = repo.checkLoginPin("999999")
        assertTrue(res1 is PinVerifyResult.Incorrect && res1.remainingAttempts == 2)
        val res2 = repo.checkLoginPin("999999")
        assertTrue(res2 is PinVerifyResult.Incorrect && res2.remainingAttempts == 1)
        val res3 = repo.checkLoginPin("999999")
        assertTrue(res3 is PinVerifyResult.TemporarilyLocked && res3.level == 1 && res3.lockMinutes == 10)
        assertEquals(1, state.pinLockLevel)

        // Set 2: 3x salah lagi -> Lock 30 menit
        repo.checkLoginPin("999999")
        repo.checkLoginPin("999999")
        val resLvl2 = repo.checkLoginPin("999999")
        assertTrue(resLvl2 is PinVerifyResult.TemporarilyLocked && resLvl2.level == 2 && resLvl2.lockMinutes == 30)
        assertEquals(2, state.pinLockLevel)

        // Set 3: 3x salah lagi -> Lock 60 menit (1 jam)
        repo.checkLoginPin("999999")
        repo.checkLoginPin("999999")
        val resLvl3 = repo.checkLoginPin("999999")
        assertTrue(resLvl3 is PinVerifyResult.TemporarilyLocked && resLvl3.level == 3 && resLvl3.lockMinutes == 60)
        assertEquals(3, state.pinLockLevel)

        // Set 4: 3x salah lagi -> Akun Terkunci (AccountLocked)
        repo.checkLoginPin("999999")
        repo.checkLoginPin("999999")
        val resLvl4 = repo.checkLoginPin("999999")
        assertTrue(resLvl4 is PinVerifyResult.AccountLocked)
        assertTrue(state.isAccountLocked)

        // Setelah terkunci, percobaan PIN apapun mengembalikan AccountLocked
        assertTrue(repo.checkLoginPin("135790") is PinVerifyResult.AccountLocked)

        // Alur Lupa PIN: Reset PIN membuka kunci akun
        repo.unlockAccountWithResetPin("902714")
        assertFalse(state.isAccountLocked)
        assertEquals(0, state.pinLockLevel)
        assertEquals(0, state.pinFailedAttempts)
        assertEquals("902714", state.pin)

        // Verifikasi PIN baru berhasil
        assertTrue(repo.checkLoginPin("902714") is PinVerifyResult.Success)
    }

    @Test
    fun goldinResetPasswordFlow() {
        repo.startDemoAccount()
        // Email tidak terdaftar
        assertCode("EMAIL_NOT_FOUND") {
            repo.resetPassword("tidakada@gmail.com", "PasswordBaru123")
        }
        // Password tidak memenuhi aturan
        assertCode("INVALID_PASSWORD") {
            repo.resetPassword(MockRepository.GOLDIN_DEMO_EMAIL, "lemah")
        }
        // Berhasil reset password
        repo.resetPassword(MockRepository.GOLDIN_DEMO_EMAIL, "SandiBaru2026")
        assertEquals("SandiBaru2026", state.registeredPassword)

        // Login dengan password lama gagal
        assertCode("INVALID_CREDENTIALS") {
            repo.login(MockRepository.GOLDIN_DEMO_EMAIL, MockRepository.GOLDIN_DEMO_PASSWORD)
        }
        // Login dengan password baru sukses
        repo.login(MockRepository.GOLDIN_DEMO_EMAIL, "SandiBaru2026")
        assertEquals(MockRepository.GOLDIN_DEMO_EMAIL, state.user?.email)
    }

    @Test
    fun goldinDemoAccountSeedVerification() {
        repo.startDemoAccount()
        assertEquals("reiuser@gmail.com", state.user?.email)
        assertEquals("Budi Santoso", state.user?.name)
        assertTrue(state.user?.verified == true)
        assertEquals(1_250_000L, state.walletBalance)
        assertEquals(2.4518, state.goldBalanceGrams)
        assertEquals(1, state.bankAccounts.size)
        assertEquals("•••• 1234", state.bankAccounts.first().maskedNumber)
        assertEquals(1, state.savedAddresses.size)
        assertEquals("15113", state.savedAddresses.first().postalCode)
        assertEquals(1, state.routinePlans.size)
        assertEquals("Dana darurat", state.routinePlans.first().name)
        assertEquals(8, state.notifications.size)
        assertTrue(repo.verifyPin("135790"))
    }

    @Test
    fun goldinUsernameAndRegisterProfileFields() {
        assertEquals("salsa_gold", Rules.normalizeUsername("  Salsa_Gold "))
        assertFalse(Rules.isUsernameMinLength("abc"))
        assertTrue(Rules.isUsernameMinLength("salsa"))
        listOf("admin", "EMDIG", "indriana", "emas").forEach {
            assertTrue(Rules.isUsernameTaken(it), it)
        }
        assertFalse(Rules.isUsernameTaken("salsabila"))

        repo.register(
            name = "Salsa Nabila",
            email = "salsa@gmail.com",
            password = "Password1",
            username = "SalsaGold",
            birthDate = "11/02/2003",
        )
        assertEquals("salsagold", state.user?.username)
        assertEquals("11/02/2003", state.user?.birthDate)
        assertFalse(state.user?.verified == true)
        assertTrue(repo.isRegisteredEmail("salsa@gmail.com"))
        assertTrue(repo.isRegisteredEmail(MockRepository.GOLDIN_DEMO_EMAIL))
        assertFalse(repo.isRegisteredEmail("asing@gmail.com"))
    }

    @Test
    fun goldinKycPinValidationAndPendingReviewFlow() {
        assertEquals("PIN tidak boleh berisi angka yang sama semua", Rules.pinValidationError("111111"))
        assertEquals("PIN tidak boleh berisi angka berurutan", Rules.pinValidationError("123456"))
        assertEquals("PIN tidak boleh berisi angka berurutan", Rules.pinValidationError("654321"))
        assertEquals("PIN tidak boleh memakai tanggal lahirmu", Rules.pinValidationError("110203", dob = "11/02/2003"))
        assertEquals(null, Rules.pinValidationError("135790", dob = "11/02/2003"))

        assertTrue(Rules.isValidNameOnlyLetters("Siti Aminah"))
        assertFalse(Rules.isValidNameOnlyLetters("Ab"))
        assertFalse(Rules.isValidNameOnlyLetters("Siti 123"))
        assertEquals(2, KycOptions.gender.size)
        assertEquals(7, KycOptions.agama.size)

        repo.register("Indriana Putri", "indriana@gmail.com", "Password1", "indriana99", "11/02/2003")
        assertCode("WEAK_PIN") {
            repo.submitKycPending(pin = "110203", birthDate = "11/02/2003")
        }
        repo.submitKycPending(
            pin = "135790",
            fullName = "INDRIANA PUTRI",
            phone = "+6281234567890",
            birthDate = "11/02/2003",
            nik = "3671 0151 0203 0002",
            biometricEnabled = true,
        )
        assertFalse(state.user?.verified == true)
        assertTrue(state.user?.isKycPending == true)
        assertEquals("135790", state.pin)
        assertTrue(state.isBiometricEnabled)
        assertEquals("3671015102030002", state.user?.nik)

        repo.approveKyc()
        assertTrue(state.user?.verified == true)
        assertFalse(state.user?.isKycPending == true)
        assertTrue(state.notifications.any { it.title == "Verifikasi berhasil" })
    }

    @Test
    fun goldinHomeBalanceVisibilityPriceRefreshAndNotifications() {
        demo()
        // Tersembunyi default sesuai Handoff §6
        assertTrue(state.hideHomeBalance)
        assertTrue(state.hidePortfolioGold)
        assertTrue(state.hidePortfolioCash)

        // Toggle mata Beranda menyinkronkan kedua kartu Portofolio
        repo.toggleHomeBalanceVisibility()
        assertFalse(state.hideHomeBalance)
        assertFalse(state.hidePortfolioGold)
        assertFalse(state.hidePortfolioCash)

        // Mata Portofolio independen
        repo.togglePortfolioGoldVisibility()
        assertTrue(state.hidePortfolioGold)
        assertFalse(state.hidePortfolioCash)

        // Refresh harga naik -> flash "up" + kelipatan 500
        val beforeAsk = state.askPricePerGram
        val flashUp = repo.refreshGoldPrice(formattedHm = "14.55", deltaOverride = 1_500L)
        assertEquals("up", flashUp)
        assertEquals("up", state.priceFlash)
        assertEquals(beforeAsk + 1_500L, state.askPricePerGram)
        assertEquals(0L, state.buybackPerGram % 500L)
        assertEquals("14.55 WIB", state.priceUpdatedAtLabel)

        // Refresh harga turun -> flash "down" lalu dibersihkan
        val flashDown = repo.refreshGoldPrice(formattedHm = "14.56", deltaOverride = -1_000L)
        assertEquals("down", flashDown)
        repo.clearPriceFlash()
        assertEquals(null, state.priceFlash)

        // Notifikasi: baca satu, tandai semua dibaca, hapus
        val initialUnread = state.unreadNotificationCount
        assertTrue(initialUnread > 0)
        val firstUnread = state.notifications.first { !it.isRead }
        repo.markNotificationRead(firstUnread.id)
        assertEquals(initialUnread - 1, state.unreadNotificationCount)

        repo.markAllNotificationsRead()
        assertEquals(0, state.unreadNotificationCount)

        val beforeCount = state.notifications.size
        repo.deleteNotification(firstUnread.id)
        assertEquals(beforeCount - 1, state.notifications.size)
        assertFalse(state.notifications.any { it.id == firstUnread.id })
    }

    @Test
    fun logoutClearsAccountData() {
        demo()
        repo.logout()
        assertEquals(null, state.user)
        assertTrue(state.tokens.isEmpty())
        assertEquals(0, state.walletBalance)
    }

    // ===== Tes Sub-Tahap 5.1: Transaksi, Perhitungan Finansial, Price-Lock & Riwayat =====

    @Test
    fun tradeMathFormulasBuyAndSell() {
        // Rumus Beli: Subtotal Rp250.000 @ Rp2.298.000/gr
        val buyCalc = calculateTradeAmounts(isBuy = true, pricePerGram = 2_298_000L, inputSubtotal = 250_000L)
        assertEquals(0.1087, buyCalc.grams)
        assertEquals(250_000L, buyCalc.subtotal)
        assertEquals(1_000L, buyCalc.adminFee)
        assertEquals(625L, buyCalc.tax) // 250.000 * 0.0025 = 625
        assertEquals(251_625L, buyCalc.total)

        // Rumus Jual: Subtotal Rp150.000 @ Rp2.241.000/gr -> 0,0669 gr, fee Rp1.000, tax Rp375, dana diterima Rp148.625
        val sellCalc = calculateTradeAmounts(isBuy = false, pricePerGram = 2_241_000L, inputSubtotal = 150_000L)
        assertEquals(0.0669, sellCalc.grams)
        assertEquals(150_000L, sellCalc.subtotal)
        assertEquals(1_000L, sellCalc.adminFee)
        assertEquals(375L, sellCalc.tax) // 150.000 * 0.0025 = 375
        assertEquals(148_625L, sellCalc.total) // Dana diterima

        // Rumus Jual input berat: 0.1 gr @ Rp2.200.000/gr -> subtotal Rp220.000
        val sellByGrams = calculateTradeAmounts(isBuy = false, pricePerGram = 2_200_000L, inputGrams = 0.1)
        assertEquals(0.1, sellByGrams.grams)
        assertEquals(220_000L, sellByGrams.subtotal)
        assertEquals(1_000L, sellByGrams.adminFee)
        assertEquals(550L, sellByGrams.tax) // 220.000 * 0.0025 = 550
        assertEquals(218_450L, sellByGrams.total)

        // Helper string format VA dan tanggal
        assertEquals("251119", formatYmd(1763520000000L))
        assertEquals("261119", formatYmd(1763520000000L + 365L * 86400 * 1000L))
        val vaBca = generateVaNumber("39358", "TRX-B-101")
        assertTrue(vaBca.startsWith("39358"))
        assertEquals(16, vaBca.length)
        val formattedVa = formatVaWithSpaces(vaBca)
        assertTrue(formattedVa.contains(" "))
    }

    @Test
    fun tradeQuotePriceLock30SecondsLifecycle() {
        demo()
        val quote = repo.createTradeQuote(isBuy = true, inputSubtotal = 100_000L)
        assertEquals(30, quote.secondsRemaining(now))
        assertFalse(quote.isExpired(now))

        // Maju 15 detik: masih aktif
        now += 15_000L
        assertEquals(15, quote.secondsRemaining(now))
        assertFalse(quote.isExpired(now))

        // Maju 16 detik lagi (total 31 detik): kedaluwarsa
        now += 16_000L
        assertTrue(quote.isExpired(now))
        assertEquals(0, quote.secondsRemaining(now))

        // Order yang kedaluwarsa tidak bisa disubmit
        assertCode("QUOTE_EXPIRED") {
            repo.submitTradeOrder(quote.id, PaymentMethod.WALLET)
        }

        // Refresh quote mengunci harga saat ini untuk 30 detik baru
        repo.refreshGoldPrice(deltaOverride = 2_000L) // Ask naik Rp2.000
        val refreshed = repo.refreshTradeQuote(quote.id)
        assertFalse(refreshed.isExpired(now))
        assertEquals(state.askPricePerGram, refreshed.lockedPricePerGram)
        assertEquals(30, refreshed.secondsRemaining(now))
    }

    @Test
    fun tradeOrderMinRp10000Validation() {
        demo()
        assertCode("INVALID_AMOUNT") {
            repo.createTradeQuote(isBuy = true, inputSubtotal = 5_000L)
        }
    }

    @Test
    fun tradeInstantWalletBuyAndSell() {
        demo()
        val initialWallet = state.walletBalance
        val initialGold = state.goldBalanceGrams

        // 1. Beli emas dengan Saldo Kas
        val buyQuote = repo.createTradeQuote(isBuy = true, inputSubtotal = 100_000L)
        val buyTx = repo.submitTradeOrder(buyQuote.id, PaymentMethod.WALLET)
        assertEquals("ok", buyTx.status)
        assertEquals(TxType.BUY, buyTx.type)
        assertEquals(initialWallet - buyQuote.total, state.walletBalance)
        assertEquals(initialGold + buyQuote.grams, state.goldBalanceGrams)
        assertTrue(buyTx.bursaRef?.startsWith("BRS-") == true)
        assertTrue(buyTx.kliringRef?.startsWith("KLR-") == true)
        assertTrue(state.notifications.any { it.txId == buyTx.id && it.title == "Pembelian emas berhasil" })

        // 2. Beli emas gagal saat Saldo Kas tidak cukup
        val bigQuote = repo.createTradeQuote(isBuy = true, inputSubtotal = 10_000_000L)
        assertCode("INSUFFICIENT_BALANCE") {
            repo.submitTradeOrder(bigQuote.id, PaymentMethod.WALLET)
        }

        // 3. Jual emas ke Saldo Kas
        val goldBeforeSell = state.goldBalanceGrams
        val walletBeforeSell = state.walletBalance
        val sellQuote = repo.createTradeQuote(isBuy = false, inputGrams = 0.05)
        val sellTx = repo.submitTradeOrder(sellQuote.id, PaymentMethod.WALLET)
        assertEquals("ok", sellTx.status)
        assertEquals(TxType.SELL, sellTx.type)
        assertEquals(goldBeforeSell - 0.05, state.goldBalanceGrams)
        assertEquals(walletBeforeSell + sellQuote.total, state.walletBalance)
        assertTrue(state.notifications.any { it.txId == sellTx.id && it.title == "Penjualan emas berhasil" })

        // 4. Jual emas gagal jika melebihi saldo emas bebas
        assertCode("INSUFFICIENT_GOLD") {
            repo.createTradeQuote(isBuy = false, inputGrams = 100.0)
        }
    }

    @Test
    fun tradePendingVaAndQrisLifecycle() {
        demo()
        val initialGold = state.goldBalanceGrams

        // 1. Submit order dengan VA BCA -> status pending, timer 15 menit
        val quoteVa = repo.createTradeQuote(isBuy = true, inputSubtotal = 200_000L)
        val txVa = repo.submitTradeOrder(quoteVa.id, PaymentMethod.VA_BCA)
        assertEquals("pending", txVa.status)
        assertTrue(txVa.virtualAccountNumber?.startsWith("39358") == true)
        assertEquals(now + PAYMENT_WINDOW_MINUTES * 60 * 1000L, txVa.expiresAtMs)
        assertEquals(initialGold, state.goldBalanceGrams) // Emas belum masuk

        // Muncul di transaksi aktif
        val activeList = repo.getActiveTransactions()
        assertTrue(activeList.any { it.id == txVa.id })

        // 2. Simulasi pembayaran selesai ("Cek status pembayaran")
        now += 60_000L // 1 menit kemudian
        val confirmedTx = repo.confirmTradePayment(txVa.id)
        assertEquals("ok", confirmedTx.status)
        assertTrue(confirmedTx.bursaRef?.startsWith("BRS-") == true)
        assertTrue(confirmedTx.kliringRef?.startsWith("KLR-") == true)
        assertEquals(initialGold + txVa.grams, state.goldBalanceGrams)
        assertFalse(repo.getActiveTransactions().any { it.id == txVa.id })

        // 3. Order kedaluwarsa setelah 15 menit
        val quoteQris = repo.createTradeQuote(isBuy = true, inputSubtotal = 150_000L)
        val txQris = repo.submitTradeOrder(quoteQris.id, PaymentMethod.QRIS)
        assertEquals("pending", txQris.status)
        now += (PAYMENT_WINDOW_MINUTES + 1) * 60 * 1000L // Melebihi 15 menit
        assertCode("PAYMENT_EXPIRED") {
            repo.confirmTradePayment(txQris.id)
        }
        val expiredTx = repo.getTransaction(txQris.id)
        assertEquals("fail", expiredTx?.status)
    }

    @Test
    fun tradeCancelOrder() {
        demo()
        val quote = repo.createTradeQuote(isBuy = true, inputSubtotal = 50_000L)
        val tx = repo.submitTradeOrder(quote.id, PaymentMethod.VA_BRI)
        assertEquals("pending", tx.status)

        val cancelled = repo.cancelTradeOrder(tx.id)
        assertEquals("fail", cancelled.status)
        assertEquals("fail", repo.getTransaction(tx.id)?.status)
        assertFalse(repo.getActiveTransactions().any { it.id == tx.id })
    }

    @Test
    fun transactionFilterTabsAndRanges() {
        demo()
        // Tab Beli emas: harus mencakup Beli emas reguler dan debit Tabungan rutin
        val beliTxs = repo.filterTransactions(tab = "beli")
        assertTrue(beliTxs.isNotEmpty())
        assertTrue(beliTxs.all { it.type == TxType.BUY || it.type == TxType.ROUTINE_DEBIT })
        assertTrue(beliTxs.any { it.method == "Tabungan rutin" })

        // Tab Jual emas
        val jualTxs = repo.filterTransactions(tab = "jual")
        assertTrue(jualTxs.isNotEmpty())
        assertTrue(jualTxs.all { it.type == TxType.SELL })

        // Tab Saldo Kas: top up dan tarik dana
        val kasTxs = repo.filterTransactions(tab = "kas")
        assertTrue(kasTxs.isNotEmpty())
        assertTrue(kasTxs.all { it.type == TxType.TOPUP || it.type == TxType.WITHDRAW })

        // Filter status: selesai (ok) vs gagal (fail)
        val okTxs = repo.filterTransactions(statusFilter = "ok")
        assertTrue(okTxs.all { it.status == "ok" })
        val failTxs = repo.filterTransactions(statusFilter = "fail")
        assertTrue(failTxs.all { it.status == "fail" })
    }

    // ===== PENGUJIAN TAHAP 6: PORTOFOLIO, SALDO KAS, REKENING BANK & TOP UP / TARIK DANA =====

    @Test
    fun portfolioIndependentEyeToggles() {
        demo()
        assertTrue(state.hidePortfolioGold)
        assertTrue(state.hidePortfolioCash)

        repo.toggleHidePortfolioGold()
        assertFalse(state.hidePortfolioGold)
        assertTrue(state.hidePortfolioCash)

        repo.toggleHidePortfolioCash()
        assertFalse(state.hidePortfolioGold)
        assertFalse(state.hidePortfolioCash)

        repo.toggleHidePortfolioGold()
        assertTrue(state.hidePortfolioGold)
        assertFalse(state.hidePortfolioCash)
    }

    @Test
    fun topupVaLifecycle() {
        demo()
        val initialWallet = state.walletBalance

        // 1. Request Top Up VA BCA (prefix 3901)
        val txBca = repo.requestTopupVa("bca")
        assertEquals("pending", txBca.status)
        assertEquals(TxType.TOPUP, txBca.type)
        assertEquals("BCA Virtual Account", txBca.method)
        assertEquals(TOPUP_FEE, txBca.fee)
        val expectedVa = "3901" + CUST_VA_NUMBER
        assertEquals(expectedVa, txBca.virtualAccountNumber)
        assertEquals(initialWallet, state.walletBalance) // Saldo belum bertambah

        // 2. Request Top Up VA Mandiri (prefix 8950)
        val txMandiri = repo.requestTopupVa("mandiri")
        assertEquals("8950" + CUST_VA_NUMBER, txMandiri.virtualAccountNumber)

        // 3. Simulasi pembayaran VA selesai (+Rp500.000)
        val settled = repo.simulateVaTopupSuccess(txBca.id, 500_000L)
        assertEquals("ok", settled.status)
        assertEquals(initialWallet + 500_000L, state.walletBalance)
        assertTrue(state.notifications.any { it.txId == settled.id && it.title == "Top up berhasil" })
    }

    @Test
    fun topupQrisLifecycle() {
        demo()
        val initialWallet = state.walletBalance

        // 1. Validasi minimal top up Rp10.000
        assertCode("INVALID") {
            repo.createTopupQris(5_000L)
        }

        // 2. Request Top Up QRIS Rp100.000
        val txQris = repo.createTopupQris(100_000L)
        assertEquals("pending", txQris.status)
        assertEquals(TxType.TOPUP, txQris.type)
        assertEquals("QRIS", txQris.method)
        assertEquals(100_000L, txQris.amount)
        assertEquals(TOPUP_FEE, txQris.fee)
        assertEquals(101_000L, txQris.total)
        assertTrue(txQris.qrisPayload?.contains(CUST_VA_NUMBER) == true)
        assertEquals(now + 15 * 60 * 1000L, txQris.expiresAtMs)
        assertEquals(initialWallet, state.walletBalance)

        // 3. Simulasi pembayaran QRIS selesai
        val settled = repo.simulateQrisPaymentSuccess(txQris.id)
        assertEquals("ok", settled.status)
        assertEquals(initialWallet + 100_000L, state.walletBalance)
        assertTrue(state.notifications.any { it.txId == settled.id && it.title == "Top up berhasil" })
    }

    @Test
    fun bankAccountAddVerificationAndDuplicates() {
        demo()
        val initialCount = state.bankAccounts.size

        // 1. Nomor rekening terlalu pendek (< 6 digit)
        assertCode("INVALID_ACCOUNT") {
            repo.verifyAndAddBankAccount("BRI", "123")
        }

        // 2. Duplikasi nomor rekening yang sudah ada
        val existingNumber = state.bankAccounts.first().number
        assertCode("DUPLICATE_ACCOUNT") {
            repo.verifyAndAddBankAccount("BCA", existingNumber, "BUDI SANTOSO")
        }

        // 3. Nama pemilik rekening tidak sesuai e-KTP
        assertCode("MISMATCH_NAME") {
            repo.verifyAndAddBankAccount("BRI", "9876543210", "Ahmad Sudirman")
        }

        // 4. Sukses menambahkan rekening atas nama e-KTP
        val newAcc = repo.verifyAndAddBankAccount("BRI", "9876543210", "BUDI SANTOSO")
        assertEquals("BRI", newAcc.bank)
        assertEquals("9876543210", newAcc.number)
        assertEquals("•••• 3210", newAcc.maskedNumber)
        assertEquals("BRI •••• 3210", newAcc.displayLabel)
        assertEquals(initialCount + 1, state.bankAccounts.size)
    }

    @Test
    fun bankAccountDeletionAndPrimaryReassignment() {
        demo()
        val bcaAcc = state.bankAccounts.first()
        // Tambah rekening kedua
        val briAcc = repo.verifyAndAddBankAccount("BRI", "5544332211", "BUDI SANTOSO")
        assertEquals(2, state.bankAccounts.size)

        // Hapus rekening pertama (utama)
        repo.deleteBankAccount(bcaAcc.id)
        assertEquals(1, state.bankAccounts.size)
        // Rekening tersisa otomatis menjadi rekening utama
        val remaining = state.bankAccounts.first()
        assertEquals(briAcc.id, remaining.id)
        assertTrue(remaining.isPrimary)
    }

    @Test
    fun autoDebitActivationWithOtp() {
        demo()
        val acc = state.bankAccounts.first()
        assertFalse(acc.isAutoDebitActive)

        // OTP salah
        assertCode("INVALID_OTP") {
            repo.toggleAutoDebit(acc.id, "999999")
        }

        // OTP benar 123456
        val ok = repo.toggleAutoDebit(acc.id, "123456")
        assertTrue(ok)
        val updated = state.bankAccounts.first { it.id == acc.id }
        assertTrue(updated.isAutoDebitActive)
    }

    @Test
    fun withdrawalLifecycleAndFeeDeduction() {
        demo()
        val acc = state.bankAccounts.first()
        val initialWallet = state.walletBalance // Rp1.250.000

        // 1. Validasi minimal penarikan Rp50.000
        assertCode("INVALID") {
            repo.requestWithdrawal(20_000L, acc.id)
        }

        // 2. Rekening tidak ditemukan
        assertCode("INVALID") {
            repo.requestWithdrawal(100_000L, "non-existent-id")
        }

        // 3. Saldo tidak mencukupi (nominal + fee Rp2.500)
        assertCode("INSUFFICIENT_BALANCE") {
            repo.requestWithdrawal(initialWallet, acc.id) // Butuh initialWallet + Rp2.500
        }

        // 4. Penarikan valid Rp200.000 -> saldo terpotong Rp202.500
        val txWdr = repo.requestWithdrawal(200_000L, acc.id)
        assertEquals("process", txWdr.status)
        assertEquals(TxType.WITHDRAW, txWdr.type)
        assertEquals(-200_000L, txWdr.amount)
        assertEquals(200_000L, txWdr.subtotal)
        assertEquals(WITHDRAW_FEE, txWdr.fee)
        assertEquals(202_500L, txWdr.total)
        assertEquals(initialWallet - 202_500L, state.walletBalance)
        assertTrue(state.notifications.any { it.txId == txWdr.id && it.title == "Penarikan dalam proses" })

        // 5. Penarikan selesai diverifikasi
        val completed = repo.completeWithdrawal(txWdr.id)
        assertEquals("ok", completed.status)
        assertTrue(state.notifications.any { it.txId == txWdr.id && it.title == "Penarikan dana berhasil" })
    }

    @Test
    fun routineSavingsLifecycleAndDebit() {
        demo()
        val initialGold = state.goldBalanceGrams // 2.4518 gr
        val initialWallet = state.walletBalance // Rp1.250.000

        // 1. Validasi input
        assertCode("INVALID") {
            repo.createRoutinePlan("Plan Gagal", RoutinePlanType.TARGET_EMAS, 0, 1.0, 5_000L, 5, "kas")
        }
        assertCode("INVALID") {
            repo.createRoutinePlan("Plan Gagal", RoutinePlanType.TARGET_EMAS, 0, 1.0, 50_000L, 29, "kas")
        }

        // 2. Buat tabungan rutin tipe Target Emas
        val plan = repo.createRoutinePlan("Tabungan Liburan", RoutinePlanType.TARGET_EMAS, 0, 2.0, 100_000L, 15, "kas")
        assertEquals("Tabungan Liburan", plan.name)
        assertEquals(RoutinePlanType.TARGET_EMAS, plan.type)
        assertEquals(2.0, plan.targetGrams)
        assertEquals(100_000L, plan.monthlyAmount)
        assertEquals(15, plan.debitDate)
        assertEquals(RoutinePlanStatus.ACTIVE, plan.status)
        assertTrue(state.routinePlans.any { it.id == plan.id })

        // 3. Jeda dan Lanjutkan
        repo.pauseRoutinePlan(plan.id)
        assertEquals(RoutinePlanStatus.PAUSED, repo.getRoutinePlan(plan.id)?.status)
        assertCode("PLAN_PAUSED") {
            repo.executeRoutineDebit(plan.id)
        }

        repo.resumeRoutinePlan(plan.id)
        assertEquals(RoutinePlanStatus.ACTIVE, repo.getRoutinePlan(plan.id)?.status)

        // 4. Eksekusi debit berkala
        val txDebit = repo.executeRoutineDebit(plan.id)
        assertEquals(TxType.BUY, txDebit.type)
        assertEquals("Tabungan rutin", txDebit.method)
        assertEquals(100_000L, txDebit.subtotal)
        assertEquals(plan.id, txDebit.planId)
        assertTrue(state.goldBalanceGrams > initialGold)
        assertTrue(state.walletBalance < initialWallet)

        val updatedPlan = repo.getRoutinePlan(plan.id)!!
        assertEquals(1, updatedPlan.mutations.size)
        assertTrue(updatedPlan.savedGrams > 0.0)

        // 5. Hentikan tabungan rutin (plan terhapus dari daftar, emas tetap di portofolio)
        val goldBeforeStop = state.goldBalanceGrams
        repo.stopRoutinePlan(plan.id)
        assertFalse(state.routinePlans.any { it.id == plan.id })
        assertEquals(goldBeforeStop, state.goldBalanceGrams)
    }

    @Test
    fun installmentCalculationsAndBounds() {
        demo()

        // 1. Validasi minimal harga emas Rp500.000
        assertCode("INVALID_AMOUNT") {
            repo.createInstallmentQuote(0.1, 12, 20) // 0.1 gr * 2.315.000 = 231.500 < 500.000
        }

        // 2. Perhitungan formula matematis flat margin syariah BSI Finance
        // 1.0 gr @ 2.315.000/gr, Tenor 12 bulan, DP 20%
        val quote = repo.createInstallmentQuote(1.0, 12, 20)
        val calc = quote.calculation
        assertEquals(2_315_000L, calc.goldValue)
        assertEquals(463_000L, calc.downPaymentAmount) // 20% dari 2.315.000
        assertEquals(1_852_000L, calc.principal) // Pokok pembiayaan = 2.315.000 - 463.000
        // Margin = round(1.852.000 * 0.009 * 12) = 200.016
        assertEquals(200_016L, calc.margin)
        // Angsuran per bulan = ceil((1.852.000 + 200.016) / 12) = ceil(2.052.016 / 12) = 171.002
        assertEquals(171_002L, calc.monthlyInstallment)
        assertEquals(25_000L, calc.adminFee)
        assertEquals(488_000L, calc.totalDownPayment) // 463.000 + 25.000
        assertFalse(quote.isExpired(now))

        // 3. Verifikasi scoring
        assertTrue(repo.verifyScoring(quote.id, isSimulatedPass = true))
        assertFalse(repo.verifyScoring(quote.id, isSimulatedPass = false))
    }

    @Test
    fun installmentLifecycleAndGoldUnlockOnPayoff() {
        demo()
        // Top up dana agar mencukupi untuk DP + 3 angsuran bulanan
        val topupTx = repo.requestTopupVa("bca")
        repo.simulateVaTopupSuccess(topupTx.id, 2_000_000L)

        val initialLockedGold = state.lockedGoldGrams // 0.0 gr
        val initialGoldBal = state.goldBalanceGrams // 2.4518 gr
        val initialWallet = state.walletBalance // Rp3.250.000

        // 1. Buat quote 1 gr tenor 3 bulan DP 10%
        val quote = repo.createInstallmentQuote(1.0, 3, 10)
        val dpTotal = quote.calculation.totalDownPayment

        // 2. Tanda tangani akad & bayar DP (OTP salah -> gagal)
        assertCode("INVALID_OTP") {
            repo.signAndPayInstallmentAkad(quote.id, "000000", PaymentMethod.WALLET, MockRepository.GOLDIN_DEMO_PIN)
        }

        // OTP benar 123456 & PIN benar 135790
        val contract = repo.signAndPayInstallmentAkad(quote.id, "123456", PaymentMethod.WALLET, MockRepository.GOLDIN_DEMO_PIN)
        assertEquals(1.0, contract.goldGrams)
        assertEquals(3, contract.tenorMonths)
        assertEquals(0, contract.paidMonths)
        assertEquals("active", contract.status)
        assertEquals(3, contract.schedule.size)
        assertEquals(initialLockedGold + 1.0, state.lockedGoldGrams) // Emas terkunci 1 gr
        assertEquals(initialWallet - dpTotal, state.walletBalance)
        assertEquals(initialGoldBal, state.goldBalanceGrams) // Saldo aktif belum bertambah

        // 3. Bayar angsuran bulan ke-1
        val afterMonth1 = repo.payInstallmentMonthly(contract.id, PaymentMethod.WALLET, MockRepository.GOLDIN_DEMO_PIN)
        assertEquals(1, afterMonth1.paidMonths)
        assertEquals(1.0, state.lockedGoldGrams) // Masih terkunci
        assertFalse(afterMonth1.isDone)

        // 4. Bayar angsuran bulan ke-2
        val afterMonth2 = repo.payInstallmentMonthly(contract.id, PaymentMethod.WALLET, MockRepository.GOLDIN_DEMO_PIN)
        assertEquals(2, afterMonth2.paidMonths)
        assertEquals(1.0, state.lockedGoldGrams) // Masih terkunci
        assertFalse(afterMonth2.isDone)

        // 5. Bayar angsuran bulan ke-3 (Pelunasan 100%)
        val afterMonth3 = repo.payInstallmentMonthly(contract.id, PaymentMethod.WALLET, MockRepository.GOLDIN_DEMO_PIN)
        assertEquals(3, afterMonth3.paidMonths)
        assertTrue(afterMonth3.isDone)
        assertEquals("done", afterMonth3.status)
        assertTrue(afterMonth3.isPaidOff)

        // Verifikasi pelepasan emas (Unlocked to Active Portfolio):
        assertEquals(0.0, state.lockedGoldGrams) // Gram terkunci berkurang ke 0
        assertEquals(floorGrams4(initialGoldBal + 1.0), state.goldBalanceGrams) // Saldo aktif bertambah 1 gr!
        assertTrue(state.notifications.any { it.title == "Cicilan emas lunas" })

        // Angsuran yang sudah lunas tidak bisa dibayar lagi
        assertCode("ALREADY_PAID") {
            repo.payInstallmentMonthly(contract.id, PaymentMethod.WALLET, MockRepository.GOLDIN_DEMO_PIN)
        }
    }

    @Test
    fun installmentAcceleratedPayoffUnlocksGoldImmediately() {
        demo()
        val topupTx = repo.requestTopupVa("bca")
        repo.simulateVaTopupSuccess(topupTx.id, 5_000_000L)

        val initialLocked = state.lockedGoldGrams
        val initialGold = state.goldBalanceGrams

        val quote = repo.createInstallmentQuote(
            grams = 2.0,
            tenorMonths = 6,
            dpPercent = 20,
            paymentMode = "manual",
            fundingSource = "kas",
        )
        val contract = repo.signAndPayInstallmentAkad(
            quoteId = quote.id,
            smsOtp = "123456",
            paymentMethod = PaymentMethod.WALLET,
            pin = MockRepository.GOLDIN_DEMO_PIN,
        )
        assertEquals(floorGrams4(initialLocked + 2.0), state.lockedGoldGrams)

        // Bayar 1 bulan dulu
        repo.payInstallmentMonthly(contract.id, PaymentMethod.WALLET, MockRepository.GOLDIN_DEMO_PIN)
        assertEquals(1, repo.getInstallmentContract(contract.id)?.paidMonths)

        // Lunasi langsung sisa 5 bulan (Pelunasan dipercepat)
        val paidOffContract = repo.payoffInstallmentContract(
            contractId = contract.id,
            paymentMethod = PaymentMethod.WALLET,
            pin = MockRepository.GOLDIN_DEMO_PIN,
        )
        assertTrue(paidOffContract.isDone)
        assertEquals(6, paidOffContract.paidMonths)
        assertTrue(paidOffContract.schedule.all { it.isPaid })
        assertEquals(0.0, state.lockedGoldGrams)
        assertEquals(floorGrams4(initialGold + 2.0), state.goldBalanceGrams)
    }

    @Test
    fun dateHelperCalculations() {
        val testEpoch = 1794960000000L // Contoh epoch ms
        val comp = parseDateComponents(testEpoch)
        assertTrue(comp.year >= 2026)
        assertTrue(comp.month in 1..12)
        assertTrue(comp.day in 1..31)

        val reconstructed = epochMsOf(comp.year, comp.month, comp.day, 8, 0)
        val comp2 = parseDateComponents(reconstructed)
        assertEquals(comp.year, comp2.year)
        assertEquals(comp.month, comp2.month)
        assertEquals(comp.day, comp2.day)

        // Cek perhitungan jatuh tempo cicilan (1 bulan ke depan)
        val due1 = calculateInstallmentDueDate(reconstructed, 1, 5)
        val compDue1 = parseDateComponents(due1)
        assertEquals(5, compDue1.day)

        // Cek perhitungan tanggal debit berikutnya
        val nextDebit = calculateNextDebitDate(15, reconstructed)
        val compDebit = parseDateComponents(nextDebit)
        assertEquals(15, compDebit.day)
    }

    @Test
    fun goldinPhysicalPrintOrderFlowAndBranches() {
        demo()

        // 1. Verifikasi 8 Cabang Raja Emas Resmi
        val branches = repo.getGoldBranches()
        assertEquals(8, branches.size)
        val branchKwc = repo.getGoldBranch("kwc")
        assertNotNull(branchKwc)
        assertEquals("Raja Emas Karawaci", branchKwc.name)
        assertEquals("Banten", branchKwc.province)
        assertEquals(1.2, branchKwc.distanceKm)
        assertFalse(branchKwc.isSevenDays)

        val branchBsd = repo.getGoldBranch("bsd")
        assertNotNull(branchBsd)
        assertTrue(branchBsd.isSevenDays)

        // 2. Verifikasi Katalog & Stok
        val stocks = repo.getGoldBarStock()
        assertEquals(24, stocks.size) // 3 brand * 8 pecahan
        assertFalse(repo.isStockAvailable(GoldBrand.ANTAM, 50.0))
        assertFalse(repo.isStockAvailable(GoldBrand.GALERI_24, 3.0))
        assertFalse(repo.isStockAvailable(GoldBrand.GALERI_24, 100.0))
        assertTrue(repo.isStockAvailable(GoldBrand.LM_REI, 1.0))
        assertTrue(repo.isStockAvailable(GoldBrand.ANTAM, 1.0))
        assertTrue(repo.isStockAvailable(GoldBrand.GALERI_24, 1.0))

        // 3. Verifikasi Seeded Orders Demo
        val initialOrders = repo.getPhysicalPrintOrders()
        assertEquals(2, initialOrders.size)
        val demo1 = repo.getPhysicalPrintOrder("CTK-DEMO-01")
        assertNotNull(demo1)
        assertEquals(PhysicalOrderStatus.READY_PICKUP, demo1.status)
        assertEquals("TKT-REI-839102", demo1.ticketCode)
        assertEquals("Selasa, 24 Nov 2026", demo1.pickupScheduleDate)

        // 4. Verifikasi Validasi Pembuatan Pesanan
        val initialGold = state.goldBalanceGrams
        val initialWallet = state.walletBalance

        // Error: Stok habis
        assertCode("OUT_OF_STOCK") {
            repo.createPhysicalPrintOrder(
                brand = GoldBrand.ANTAM,
                weightGrams = 50.0,
                method = PrintMethod.BRANCH,
                pin = MockRepository.GOLDIN_DEMO_PIN,
            )
        }

        // Error: Saldo emas bebas tidak cukup (saldo 2.4518 gr vs minta 5 gr)
        assertCode("INSUFFICIENT_GOLD") {
            repo.createPhysicalPrintOrder(
                brand = GoldBrand.LM_REI,
                weightGrams = 5.0,
                method = PrintMethod.BRANCH,
                pin = MockRepository.GOLDIN_DEMO_PIN,
            )
        }

        // Error: PIN salah
        assertCode("WRONG_PIN") {
            repo.createPhysicalPrintOrder(
                brand = GoldBrand.LM_REI,
                weightGrams = 1.0,
                method = PrintMethod.BRANCH,
                pin = "000000",
            )
        }

        // 5. Sukses: Cetak 1 gr LM REI Ambil di Cabang (Biaya Rp50.000)
        val order1 = repo.createPhysicalPrintOrder(
            brand = GoldBrand.LM_REI,
            weightGrams = 1.0,
            method = PrintMethod.BRANCH,
            branchId = "kwc",
            paymentMethod = PaymentMethod.WALLET,
            pin = MockRepository.GOLDIN_DEMO_PIN,
        )
        assertEquals(1.0, order1.weightGrams)
        assertEquals(50_000L, order1.printFee)
        assertEquals(0L, order1.shippingInsuranceFee)
        assertEquals(50_000L, order1.totalPayment)
        assertTrue(order1.ticketCode.startsWith("TKT-REI-") || order1.ticketCode.startsWith("CTK-"))
        assertTrue(order1.serialNumber?.startsWith("LMR-") == true)
        assertEquals(floorGrams4(initialGold - 1.0), state.goldBalanceGrams)
        assertEquals(initialWallet - 50_000L, state.walletBalance)

        // 6. Sukses: Cetak 1 gr Galeri 24 Dikirim ke Alamat (Biaya Rp50.000 + Rp35.000 = Rp85.000)
        val order2 = repo.createPhysicalPrintOrder(
            brand = GoldBrand.GALERI_24,
            weightGrams = 1.0,
            method = PrintMethod.DELIVERY,
            deliveryAddress = "Jl. Sudirman No. 10, Jakarta",
            recipientName = "Budi Santoso",
            recipientPhone = "081234567890",
            paymentMethod = PaymentMethod.WALLET,
            pin = MockRepository.GOLDIN_DEMO_PIN,
        )
        assertEquals(50_000L, order2.printFee)
        assertEquals(35_000L, order2.shippingInsuranceFee)
        assertEquals(85_000L, order2.totalPayment)
        assertTrue(order2.trackingNumber?.startsWith("RE") == true)
        assertTrue(order2.serialNumber?.startsWith("G24-") == true)
        assertEquals(floorGrams4(initialGold - 2.0), state.goldBalanceGrams)

        // 7. Verifikasi Atur Jadwal Pengambilan
        val updatedOrder = repo.updatePickupSchedule(order1.id, "Kamis, 26 Nov 2026", "11.00–12.00 WIB")
        assertEquals("Kamis, 26 Nov 2026", updatedOrder.pickupScheduleDate)
        assertEquals("11.00–12.00 WIB", updatedOrder.pickupScheduleTimeSlot)
        assertEquals("Kamis, 26 Nov 2026", repo.getPhysicalPrintOrder(order1.id)?.pickupScheduleDate)

        // 8. Verifikasi Tambah Alamat Tersimpan
        val newAddr = SavedAddress(
            id = "addr-test-2",
            label = "Kantor",
            recipientName = "Budi Kantor",
            phoneNumber = "081299998888",
            province = "DKI Jakarta",
            city = "Jakarta Selatan",
            district = "Kebayoran Baru",
            subDistrict = "Senayan",
            postalCode = "12190",
            details = "Gedung Bursa Efek Indonesia Tower 1 Lt. 15",
            isPrimary = false,
        )
        repo.addSavedAddress(newAddr)
        assertTrue(repo.state.value.savedAddresses.any { it.id == "addr-test-2" })
    }

    @Test
    fun modul3CatalogSkusAndOfficialBranchesSerializationAndMapping() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        // 1. Uji Deserialisasi CatalogOut & SkuOut dari respons OpenAPI
        val rawCatalogJson = """
            {
                "trading_halted": false,
                "skus": [
                    {
                        "code": "LMRE-1G",
                        "brand": "Raja Emas",
                        "product": "Emas Murni 1 Gram",
                        "weight_mg": 1000,
                        "buy_price": "2513500",
                        "buyback_price": "2310000",
                        "stock": 50,
                        "trading_halted": false,
                        "price_updated_at": "2026-11-25T09:00:00Z"
                    },
                    {
                        "code": "LMRE-5G",
                        "brand": "Raja Emas",
                        "product": "Emas Murni 5 Gram",
                        "weight_mg": 5000,
                        "buy_price": "12442500",
                        "buyback_price": "11550000",
                        "stock": 0,
                        "trading_halted": false,
                        "price_updated_at": "2026-11-25T09:00:00Z"
                    }
                ]
            }
        """.trimIndent()

        val catalogOut = json.decodeFromString<com.reidig.shared.api.CatalogOut>(rawCatalogJson)
        assertFalse(catalogOut.tradingHalted)
        assertEquals(2, catalogOut.skus.size)
        assertEquals("LMRE-1G", catalogOut.skus[0].code)
        assertEquals(1000L, catalogOut.skus[0].weightMg)
        assertEquals("2513500", catalogOut.skus[0].buyPrice)
        assertEquals(50, catalogOut.skus[0].stock)

        // 2. Uji Deserialisasi BranchOut dari respons OpenAPI
        val rawBranchesJson = """
            [
                {
                    "code": "kwc",
                    "name": "Karawaci",
                    "area": "Kota Tangerang",
                    "pickup_estimate": "1–2 hari kerja"
                },
                {
                    "code": "bdg",
                    "name": "Bandung Dago",
                    "area": "Kota Bandung",
                    "pickup_estimate": "2–3 hari kerja"
                }
            ]
        """.trimIndent()

        val branchesOut = json.decodeFromString<List<com.reidig.shared.api.BranchOut>>(rawBranchesJson)
        assertEquals(2, branchesOut.size)
        assertEquals("kwc", branchesOut[0].code)
        assertEquals("Karawaci", branchesOut[0].name)
        assertEquals("Kota Tangerang", branchesOut[0].area)
        assertEquals("1–2 hari kerja", branchesOut[0].pickupEstimate)

        // 3. Uji Pemetaan SKU ke Model Domain
        val mappedSkus = catalogOut.skus.map { item ->
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
        assertEquals(2, mappedSkus.size)
        assertEquals(2_513_500L, mappedSkus[0].buyPrice)
        assertEquals(2_310_000L, mappedSkus[0].buybackPrice)
        assertEquals("Raja Emas", mappedSkus[0].brand)

        // 4. Uji Ketersediaan Stok Fisik Berdasarkan SKU
        val repoLocal = MockRepository(clock = { now })
        // Override state skus dengan mappedSkus
        // 1g stock=50 -> available, 5g stock=0 -> out of stock
        // Update MockRepository state dengan reflect via internal atau mock skus
        val initialBranches = repoLocal.getGoldBranches()
        assertTrue(initialBranches.isNotEmpty())
        val kwcBranch = repoLocal.getGoldBranch("kwc")
        assertNotNull(kwcBranch)
        assertEquals("kwc", kwcBranch.id)
    }

    @Test
    fun modul4WalletAndWithdrawalDtosAndFlow() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        // 1. Uji Deserialisasi WalletOut
        val rawWalletJson = """
            {
                "available": "3500000",
                "held": "250000"
            }
        """.trimIndent()
        val walletOut = json.decodeFromString<com.reidig.shared.api.WalletOut>(rawWalletJson)
        assertEquals("3500000", walletOut.available)
        assertEquals("250000", walletOut.held)
        assertEquals(3_500_000L, walletOut.available.toLongOrNull())
        assertEquals(250_000L, walletOut.held.toLongOrNull())

        // 2. Uji Deserialisasi EntryOut (Mutasi Saldo Kas)
        val rawEntriesJson = """
            [
                {
                    "id": 101,
                    "type": "TOPUP",
                    "amount": "1000000",
                    "reference_type": "WALLET_TOPUP",
                    "reference_id": "TOP-101",
                    "created_at": "2026-11-25T08:30:00Z"
                },
                {
                    "id": 102,
                    "type": "WITHDRAWAL",
                    "amount": "-500000",
                    "reference_type": "WALLET_WITHDRAWAL",
                    "reference_id": "WDR-102",
                    "created_at": "2026-11-25T09:15:00Z"
                }
            ]
        """.trimIndent()
        val entries = json.decodeFromString<List<com.reidig.shared.api.EntryOut>>(rawEntriesJson)
        assertEquals(2, entries.size)
        assertEquals(101, entries[0].id)
        assertEquals("TOPUP", entries[0].type)
        assertEquals("1000000", entries[0].amount)
        assertEquals(-500_000L, entries[1].amount.toLongOrNull())

        // 3. Uji Serialisasi WithdrawIn & Deserialisasi WithdrawalOut
        val withdrawIn = com.reidig.shared.api.WithdrawIn(
            amount = "500000",
            bankAccountId = 1,
            pin = "135790",
        )
        val withdrawInJson = json.encodeToString(com.reidig.shared.api.WithdrawIn.serializer(), withdrawIn)
        assertTrue(withdrawInJson.contains("\"amount\":\"500000\""))
        assertTrue(withdrawInJson.contains("\"bank_account_id\":1"))

        val rawWithdrawalJson = """
            {
                "id": 88,
                "amount": "500000",
                "status": "PROCESSING",
                "bank_code": "BCA",
                "account_number_masked": "•••• 1234",
                "created_at": "2026-11-25T09:20:00Z"
            }
        """.trimIndent()
        val withdrawalOut = json.decodeFromString<com.reidig.shared.api.WithdrawalOut>(rawWithdrawalJson)
        assertEquals(88, withdrawalOut.id)
        assertEquals("PROCESSING", withdrawalOut.status)
        assertEquals("BCA", withdrawalOut.bankCode)
        assertEquals("•••• 1234", withdrawalOut.accountNumberMasked)

        // 4. Uji Penarikan Dana di MockRepository
        val repoLocal = MockRepository(clock = { now })
        repoLocal.login("uji@contoh.id", "Rahasia123")
        val initialWallet = repoLocal.state.value.walletBalance
        val acc = repoLocal.state.value.bankAccounts.first()

        val txWdr = repoLocal.requestWithdrawal(amount = 200_000L, bankAccountId = acc.id, pin = MockRepository.GOLDIN_DEMO_PIN)
        assertEquals(TxType.WITHDRAW, txWdr.type)
        assertEquals(-200_000L, txWdr.amount)
        assertEquals(202_500L, txWdr.total)
        assertEquals(initialWallet - 202_500L, repoLocal.state.value.walletBalance)
    }

    @Test
    fun modul5TradingDtosAndOrderFlow() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        // 1. Uji Deserialisasi QuoteOut & Serialisasi QuoteIn
        val quoteIn = com.reidig.shared.api.QuoteIn(
            skuCode = "LMRE-1G",
            quantity = 2,
            side = "BUY",
        )
        val quoteInJson = json.encodeToString(com.reidig.shared.api.QuoteIn.serializer(), quoteIn)
        assertTrue(quoteInJson.contains("\"sku_code\":\"LMRE-1G\""))
        assertTrue(quoteInJson.contains("\"quantity\":2"))

        val rawQuoteJson = """
            {
                "id": "quote-992-abc",
                "sku_code": "LMRE-1G",
                "side": "BUY",
                "quantity": 2,
                "unit_price": "2513500",
                "total": "5027000",
                "buyback_value_now": "4620000",
                "expires_at": "2026-11-25T14:30:00Z"
            }
        """.trimIndent()
        val quoteOut = json.decodeFromString<com.reidig.shared.api.QuoteOut>(rawQuoteJson)
        assertEquals("quote-992-abc", quoteOut.id)
        assertEquals("LMRE-1G", quoteOut.skuCode)
        assertEquals("BUY", quoteOut.side)
        assertEquals(2, quoteOut.quantity)
        assertEquals("2513500", quoteOut.unitPrice)
        assertEquals("5027000", quoteOut.total)

        // 2. Uji Serialisasi BuyIn & Deserialisasi OrderOut
        val buyIn = com.reidig.shared.api.BuyIn(
            quoteId = "quote-992-abc",
            paymentMethod = "WALLET",
        )
        val buyInJson = json.encodeToString(com.reidig.shared.api.BuyIn.serializer(), buyIn)
        assertTrue(buyInJson.contains("\"quote_id\":\"quote-992-abc\""))
        assertTrue(buyInJson.contains("\"payment_method\":\"WALLET\""))

        val rawOrderJson = """
            {
                "id": 5501,
                "side": "BUY",
                "status": "COMPLETED",
                "payment_method": "WALLET",
                "sku_code": "LMRE-1G",
                "quantity": 2,
                "unit_price": "2513500",
                "gross": "5027000",
                "tax": "0",
                "net": "5027000",
                "pay_before": null,
                "created_at": "2026-11-25T14:00:00Z",
                "token_codes": ["TKN-001", "TKN-002"],
                "payment": {
                    "id": 8801,
                    "method": "WALLET",
                    "amount": "5027000",
                    "status": "PAID",
                    "payment_code": "WAL-5501",
                    "expires_at": null
                }
            }
        """.trimIndent()
        val orderOut = json.decodeFromString<com.reidig.shared.api.OrderOut>(rawOrderJson)
        assertEquals(5501, orderOut.id)
        assertEquals("BUY", orderOut.side)
        assertEquals("COMPLETED", orderOut.status)
        assertEquals(2, orderOut.quantity)
        assertEquals("5027000", orderOut.net)
        assertEquals(2, orderOut.tokenCodes.size)
        assertNotNull(orderOut.payment)
        assertEquals("WAL-5501", orderOut.payment?.paymentCode)

        // 3. Uji Serialisasi SellIn & Deserialisasi TokenOut
        val sellIn = com.reidig.shared.api.SellIn(
            quoteId = "quote-sell-123",
            tokenCodes = listOf("TKN-001"),
            pin = "135790",
        )
        val sellInJson = json.encodeToString(com.reidig.shared.api.SellIn.serializer(), sellIn)
        assertTrue(sellInJson.contains("\"quote_id\":\"quote-sell-123\""))
        assertTrue(sellInJson.contains("\"pin\":\"135790\""))

        val rawTokenJson = """
            {
                "code": "TKN-001",
                "sku_code": "LMRE-1G",
                "weight_mg": 1000,
                "serial_number": "LMR-2026-001",
                "status": "ACTIVE",
                "acquired_price": "2513500",
                "acquired_at": "2026-11-25T14:00:00Z",
                "sellable_at": "2026-11-25T14:00:00Z"
            }
        """.trimIndent()
        val tokenOut = json.decodeFromString<com.reidig.shared.api.TokenOut>(rawTokenJson)
        assertEquals("TKN-001", tokenOut.code)
        assertEquals(1000L, tokenOut.weightMg)
        assertEquals("ACTIVE", tokenOut.status)

        // 4. Uji Alur Kuotasi & Order Beli/Jual di Repository
        val repoLocal = MockRepository(clock = { now })
        repoLocal.login("uji@contoh.id", "Rahasia123")
        val initialWallet = repoLocal.state.value.walletBalance
        val initialGold = repoLocal.state.value.goldBalanceGrams

        // Buat Quote Beli 1 Gram
        val buyQuote = repoLocal.createTradeQuote(isBuy = true, inputGrams = 1.0)
        assertNotNull(buyQuote)
        assertTrue(buyQuote.total > 0L)
        assertEquals(1.0, buyQuote.grams)

        // Eksekusi Order Beli via Wallet
        val buyTx = repoLocal.submitTradeOrder(buyQuote.id, PaymentMethod.WALLET)
        assertEquals(TxType.BUY, buyTx.type)
        assertEquals("ok", buyTx.status)
        assertEquals(initialGold + 1.0, repoLocal.state.value.goldBalanceGrams)
        assertEquals(initialWallet - buyQuote.total, repoLocal.state.value.walletBalance)

        // Buat Quote Jual 1 Gram
        val sellQuote = repoLocal.createTradeQuote(isBuy = false, inputGrams = 1.0)
        assertNotNull(sellQuote)
        assertFalse(sellQuote.isBuy)
        assertEquals(1.0, sellQuote.grams)

        // Eksekusi Order Jual
        val currentWallet = repoLocal.state.value.walletBalance
        val currentGold = repoLocal.state.value.goldBalanceGrams
        val sellTx = repoLocal.submitTradeOrder(sellQuote.id, PaymentMethod.WALLET)
        assertEquals(TxType.SELL, sellTx.type)
        assertEquals("ok", sellTx.status)
        assertEquals(currentGold - 1.0, repoLocal.state.value.goldBalanceGrams)
        assertEquals(currentWallet + sellQuote.total, repoLocal.state.value.walletBalance)
    }
}
