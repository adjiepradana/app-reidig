package com.reidig.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
        assertEquals("•••• 1234", state.bankAccounts.first().number)
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
    fun logoutClearsAccountData() {
        demo()
        repo.logout()
        assertEquals(null, state.user)
        assertTrue(state.tokens.isEmpty())
        assertEquals(0, state.walletBalance)
    }
}
