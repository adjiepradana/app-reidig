package com.reidig.shared

enum class TokenStatus { ACTIVE, LOCKED_INSTALLMENT, PRINT_PENDING, RETURNED, PRINTED, CANCELLED }

enum class PaymentMethod(
    val label: String,
    val key: String = "",
    val vaPrefix: String? = null,
) {
    WALLET("Saldo Kas", "kas"),
    QRIS("QRIS", "qris"),
    VIRTUAL_ACCOUNT("Virtual Account BCA", "va-bca", "39358"),
    VA_BCA("BCA Virtual Account", "va-bca", "39358"),
    VA_BRI("BRI Virtual Account", "va-bri", "88810"),
    VA_MANDIRI("Mandiri Virtual Account", "va-mandiri", "89508"),
    VA_BNI("BNI Virtual Account", "va-bni", "98816"),
    CICILAN("Cicil Emas (BSI Finance)", "cicil"),
    ROUTINE("Tabungan rutin", "routine");

    val isVa: Boolean get() = vaPrefix != null
    val isWallet: Boolean get() = this == WALLET
    val isQris: Boolean get() = this == QRIS
    val isCicil: Boolean get() = this == CICILAN

    companion object {
        fun fromKey(key: String): PaymentMethod = values().firstOrNull { it.key == key } ?: WALLET
    }
}

enum class PrintMethod(val label: String) {
    BRANCH("Ambil di cabang"),
    JNE("Dikirim ke alamat"),
    DELIVERY("Dikirim ke alamat");

    val isBranch: Boolean get() = this == BRANCH
    val isDelivery: Boolean get() = this == JNE || this == DELIVERY
}

enum class PrintStatus(val label: String) {
    WAITING("Disiapkan gudang"),
    SHIPPED("Dikirim JNE"),
    IN_TRANSIT("Dalam perjalanan ke cabang"),
    READY_PICKUP("Siap diambil"),
    DONE("Selesai"),
}

enum class TxType(val label: String) {
    BUY("Beli emas"),
    SELL("Jual emas"),
    WITHDRAW("Tarik dana"),
    PRINT("Cetak emas"),
    INSTALLMENT("Cicil emas"),
    TOPUP("Top up"),
    ROUTINE_DEBIT("Tabungan rutin"),
}

data class Sku(
    val code: String,
    val product: String,
    val weightMg: Long,
    val buyPrice: Long,
    val buybackPrice: Long,
    val stock: Int,
) {
    val spreadPercent: Double get() = (buyPrice - buybackPrice) * 100.0 / buyPrice
}

data class GoldToken(
    val code: String,
    val skuCode: String,
    val serial: String,
    val status: TokenStatus,
    val acquiredPrice: Long,
    val acquiredAtMs: Long,
)

data class Quote(
    val id: String,
    val skuCode: String,
    val quantity: Int,
    val unitPrice: Long,
    val total: Long,
    val buybackValueNow: Long,
    val expiresAtMs: Long,
)

data class PendingOrder(
    val id: String,
    val quote: Quote,
    val method: PaymentMethod,
    val payBeforeMs: Long,
    val paymentCode: String,
)

data class BankAccount(
    val id: String,
    val bank: String,
    val number: String,
    val holder: String,
    val isAutoDebitActive: Boolean = false,
    val addedAtMs: Long = 0L,
    val isPrimary: Boolean = false,
) {
    val maskedNumber: String
        get() {
            val clean = number.filter { it.isDigit() }
            return if (clean.length >= 4) {
                "•••• " + clean.takeLast(4)
            } else {
                number
            }
        }
    val displayLabel: String get() = "$bank $maskedNumber"
}

// ===== Konstanta & Bank Portofolio (Handoff §9 & §14, Prototipe L3386) =====

const val MIN_TOPUP_RP = 10_000L
const val TOPUP_FEE = 1_000L
const val MIN_WITHDRAW_RP = 50_000L
const val WITHDRAW_FEE = 2_500L
const val CUST_VA_NUMBER = "100248173561"

data class SupportedBank(
    val key: String,
    val name: String,
    val vaPrefix: String,
)

val SUPPORTED_BANKS = listOf(
    SupportedBank(key = "bca", name = "BCA", vaPrefix = "3901"),
    SupportedBank(key = "bri", name = "BRI", vaPrefix = "8881"),
    SupportedBank(key = "mandiri", name = "Mandiri", vaPrefix = "8950"),
    SupportedBank(key = "bni", name = "BNI", vaPrefix = "9881"),
)

fun findSupportedBank(keyOrName: String): SupportedBank =
    SUPPORTED_BANKS.firstOrNull {
        it.key.equals(keyOrName, ignoreCase = true) || it.name.equals(keyOrName, ignoreCase = true)
    } ?: SUPPORTED_BANKS[0]

data class Branch(
    val id: String,
    val name: String,
    val area: String,
    val estimate: String = "1–2 hari kerja",
    val city: String = "Tangerang",
    val province: String = "Banten",
    val address: String = "",
    val distanceKm: Double = 2.5,
    val openingHours: String = "Senin–Sabtu, 09.00–20.00 WIB",
)

data class PrintOrder(
    val id: String,
    val tokenCodes: List<String>,
    val method: PrintMethod,
    val destination: String,
    val status: PrintStatus,
    val estimate: String,
    val createdAtMs: Long,
)

data class InstallmentContract(
    val id: String,
    val partner: String,
    val tokenCode: String,
    val price: Long,
    val downPayment: Long,
    val tenorMonths: Int,
    val paidMonths: Int,
    val agreementVersion: String,
    val agreedAtMs: Long,
) {
    val monthlyBeforeFees: Long get() = (price - downPayment) / tenorMonths
}

data class InstallmentApplication(
    val id: String,
    val skuCode: String,
    val price: Long,
    val downPayment: Long,
    val tenorMonths: Int,
    val agreementVersion: String,
    val agreedAtMs: Long,
    val payBeforeMs: Long,
) {
    val monthlyBeforeFees: Long get() = (price - downPayment) / tenorMonths
}

data class Tx(
    val id: String,
    val type: TxType,
    val title: String,
    val amount: Long,
    val atMs: Long,
    val status: String = "ok", // "ok", "pending", "fail", "process"
    val method: String = "Saldo Kas",
    val paymentMethod: PaymentMethod = PaymentMethod.WALLET,
    val grams: Double = 0.0,
    val pricePerGram: Long = 0,
    val subtotal: Long = 0,
    val fee: Long = 0,
    val tax: Long = 0,
    val total: Long = 0,
    val expiresAtMs: Long? = null,
    val bursaRef: String? = null,
    val kliringRef: String? = null,
    val payRef: String? = null,
    val virtualAccountNumber: String? = null,
    val qrisPayload: String? = null,
    val failureReason: String? = null,
    val planId: String? = null,
    val brand: String? = null,
    val destination: String? = null,
    val bankAccount: String? = null,
    val executedAtMs: Long? = null,
    val settledAtMs: Long? = null,
) {
    val effectiveTotal: Long get() = if (total != 0L) total else kotlin.math.abs(amount)
    val effectiveSubtotal: Long
        get() = when {
            subtotal != 0L -> subtotal
            type == TxType.BUY -> (effectiveTotal - fee - tax).coerceAtLeast(0L)
            type == TxType.SELL -> (effectiveTotal + fee + tax)
            else -> effectiveTotal
        }
    val isTrade: Boolean get() = type == TxType.BUY || type == TxType.SELL
    fun isAct(nowMs: Long): Boolean = (status == "pending" && (expiresAtMs == null || nowMs < expiresAtMs)) || status == "process"
}

// ===== Konstanta & Model Perhitungan Transaksi (Handoff §7 & §14) =====

const val MIN_TRADE_RP = 10_000L
const val TRADE_ADMIN_FEE = 1_000L
const val TRADE_TAX_RATE = 0.0025 // PPh 22: 0,25%
const val PRICE_LOCK_SECONDS = 30
const val PAYMENT_WINDOW_MINUTES = 15

/** Pembulatan ke bawah dengan presisi 4 desimal (0,0000 gr) per prototipe Goldin v1.2. */
fun floorGrams4(grams: Double): Double = kotlin.math.floor(grams * 10_000.0 + 1e-6) / 10_000.0

data class TradeCalculation(
    val isBuy: Boolean,
    val pricePerGram: Long,
    val grams: Double,
    val subtotal: Long,
    val adminFee: Long,
    val tax: Long,
    val total: Long,
)

data class TradeQuote(
    val id: String,
    val isBuy: Boolean,
    val lockedPricePerGram: Long,
    val grams: Double,
    val subtotal: Long,
    val adminFee: Long,
    val tax: Long,
    val total: Long,
    val lockedAtMs: Long,
    val expiresAtMs: Long,
) {
    fun isExpired(nowMs: Long): Boolean = nowMs >= expiresAtMs
    fun secondsRemaining(nowMs: Long): Int = ((expiresAtMs - nowMs).coerceAtLeast(0L) / 1000L).toInt()
}

/** Menghitung nominal, berat, admin, pajak, dan total berdasarkan aturan Handoff §7 & §14. */
fun calculateTradeAmounts(
    isBuy: Boolean,
    pricePerGram: Long,
    inputGrams: Double? = null,
    inputSubtotal: Long? = null,
): TradeCalculation {
    val subtotal: Long
    val grams: Double
    if (inputSubtotal != null) {
        subtotal = inputSubtotal
        grams = if (pricePerGram > 0) floorGrams4(subtotal.toDouble() / pricePerGram.toDouble()) else 0.0
    } else {
        grams = floorGrams4(inputGrams ?: 0.0)
        subtotal = kotlin.math.round(grams * pricePerGram.toDouble()).toLong()
    }
    val adminFee = TRADE_ADMIN_FEE
    val tax = kotlin.math.round(subtotal.toDouble() * TRADE_TAX_RATE).toLong()
    val total = if (isBuy) subtotal + adminFee + tax else (subtotal - adminFee - tax).coerceAtLeast(0L)
    return TradeCalculation(
        isBuy = isBuy,
        pricePerGram = pricePerGram,
        grams = grams,
        subtotal = subtotal,
        adminFee = adminFee,
        tax = tax,
        total = total,
    )
}

/** Format tanggal 6 digit YYMMDD (KMP safe). */
fun formatYmd(epochMs: Long): String {
    val totalSeconds = epochMs / 1000L
    val days = (totalSeconds / 86400L).toInt()
    var l = days + 68569 + 2440588
    val n = (4 * l) / 146097
    l -= (146097 * n + 3) / 4
    val i = (4000 * (l + 1)) / 1461001
    l = l - (1461 * i) / 4 + 31
    val j = (80 * l) / 2447
    val day = l - (2447 * j) / 80
    l = j / 11
    val month = j + 2 - (12 * l)
    val year = 100 * (n - 49) + i + l
    val yy = (year % 100).toString().padStart(2, '0')
    val mm = month.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "$yy$mm$dd"
}

/** Pembuatan nomor Virtual Account format bank (e.g. 39358 + 11 digit unik). */
fun generateVaNumber(prefix: String, orderId: String): String {
    val hash = kotlin.math.abs((orderId + "va").hashCode().toLong())
    val customerSuffix = (hash % 100000000000L).toString().padStart(11, '0').take(11)
    return prefix + customerSuffix
}

/** Format spasi per 4 digit Virtual Account (e.g. 3935 8100 2481 7356). */
fun formatVaWithSpaces(va: String): String {
    return va.replace(Regex("(\\d{4})(?=\\d)"), "$1 ")
}

data class UserProfile(
    val name: String,
    val email: String,
    val verified: Boolean,
    val isKycPending: Boolean = false,
    val phone: String? = null,
    val nik: String? = null,
    val birthDate: String? = null,
    val username: String = "reiuser",
)

/** Hasil verifikasi PIN login dan transaksi */
sealed class PinVerifyResult {
    object Success : PinVerifyResult()
    data class Incorrect(val remainingAttempts: Int, val attempts: Int) : PinVerifyResult()
    data class TemporarilyLocked(val level: Int, val lockMinutes: Int) : PinVerifyResult()
    object AccountLocked : PinVerifyResult()
}

// ===== Domain Model Goldin v1.2 =====

data class DateComponents(val year: Int, val month: Int, val day: Int)

fun parseDateComponents(epochMs: Long): DateComponents {
    val totalSeconds = epochMs / 1000L
    val days = (totalSeconds / 86400L).toInt()
    var l = days + 68569 + 2440588
    val n = (4 * l) / 146097
    l -= (146097 * n + 3) / 4
    val i = (4000 * (l + 1)) / 1461001
    l = l - (1461 * i) / 4 + 31
    val j = (80 * l) / 2447
    val day = l - (2447 * j) / 80
    l = j / 11
    val month = j + 2 - (12 * l)
    val year = 100 * (n - 49) + i + l
    return DateComponents(year, month, day)
}

fun epochMsOf(year: Int, month: Int, day: Int, hour: Int = 8, minute: Int = 0): Long {
    val y = if (month <= 2) year - 1 else year
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val m = if (month <= 2) month + 12 else month
    val doy = (153 * (m - 3) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era.toLong() * 146097L + doe.toLong() - 719468L
    return (days * 86400L + hour * 3600L + minute * 60L) * 1000L
}

fun calculateNextDebitDate(debitDay: Int, fromMs: Long): Long {
    val date = parseDateComponents(fromMs)
    val safeDay = debitDay.coerceIn(1, 28)
    return if (date.day < safeDay) {
        epochMsOf(date.year, date.month, safeDay, 8, 0)
    } else {
        val nextMonth = if (date.month == 12) 1 else date.month + 1
        val nextYear = if (date.month == 12) date.year + 1 else date.year
        epochMsOf(nextYear, nextMonth, safeDay, 8, 0)
    }
}

fun calculateInstallmentDueDate(startMs: Long, monthIndex: Int, dueDay: Int): Long {
    val date = parseDateComponents(startMs)
    val totalMonths = date.month + monthIndex
    val targetYear = date.year + (totalMonths - 1) / 12
    val targetMonth = ((totalMonths - 1) % 12) + 1
    val safeDay = dueDay.coerceIn(1, 28)
    return epochMsOf(targetYear, targetMonth, safeDay, 8, 0)
}

enum class RoutinePlanType(val label: String) {
    PERIODE("Periode"),
    TARGET_EMAS("Target emas"),
}

enum class RoutinePlanStatus {
    ACTIVE,
    PAUSED,
}

data class RoutineMutation(
    val id: String,
    val planId: String,
    val atMs: Long,
    val grams: Double,
    val amount: Long,
    val pricePerGram: Long,
    val method: String = "Tabungan rutin",
    val txId: String? = null,
)

data class RoutineSavingsPlan(
    val id: String,
    val name: String,
    val type: RoutinePlanType,
    val targetMonths: Int = 0,
    val targetGrams: Double = 0.0,
    val monthlyAmount: Long,
    val debitDate: Int, // 1..28
    val savedGrams: Double = 0.0,
    val fundingSource: String = "kas", // "kas" atau id rekening bank
    val status: RoutinePlanStatus = RoutinePlanStatus.ACTIVE,
    val createdAtMs: Long,
    val mutations: List<RoutineMutation> = emptyList(),
) {
    val progressPercent: Double
        get() = when (type) {
            RoutinePlanType.PERIODE -> if (targetMonths > 0) (mutations.size.toDouble() / targetMonths) * 100.0 else 0.0
            RoutinePlanType.TARGET_EMAS -> if (targetGrams > 0.0) (savedGrams / targetGrams) * 100.0 else 0.0
        }

    val hasTarget: Boolean
        get() = when (type) {
            RoutinePlanType.PERIODE -> targetMonths > 0
            RoutinePlanType.TARGET_EMAS -> targetGrams > 0.0
        }

    val isPaused: Boolean get() = status == RoutinePlanStatus.PAUSED
}

// ===== Konstanta & Perhitungan Cicil Emas (Handoff §11 & §14, Prototipe L3413-3416) =====

const val CC_MIN_VAL = 500_000L
const val CC_ADMIN_FEE = 25_000L
const val CC_MARGIN_RATE = 0.009 // 0,9%/bulan flat
const val CC_PARTNER_NAME = "BSI Finance"
const val CC_LOCK_WINDOW_MIN = 15

val CC_TENORS = listOf(3, 6, 12, 24)
val CC_DPS = listOf(10, 20, 30)

data class InstallmentCalculation(
    val grams: Double,
    val pricePerGram: Long,
    val goldValue: Long,
    val downPaymentPercent: Int,
    val downPaymentAmount: Long,
    val principal: Long,
    val margin: Long,
    val tenorMonths: Int,
    val monthlyInstallment: Long,
    val adminFee: Long = CC_ADMIN_FEE,
) {
    val totalDownPayment: Long get() = downPaymentAmount + adminFee
    val totalFinancing: Long get() = principal + margin
}

fun calculateInstallment(
    grams: Double,
    pricePerGram: Long,
    tenorMonths: Int,
    downPaymentPercent: Int,
): InstallmentCalculation {
    val goldValue = kotlin.math.round(grams * pricePerGram).toLong()
    val dpAmt = kotlin.math.round(goldValue * downPaymentPercent / 100.0).toLong()
    val pokok = goldValue - dpAmt
    val margin = kotlin.math.round(pokok * CC_MARGIN_RATE * tenorMonths).toLong()
    val inst = kotlin.math.ceil((pokok + margin).toDouble() / tenorMonths).toLong()
    return InstallmentCalculation(
        grams = grams,
        pricePerGram = pricePerGram,
        goldValue = goldValue,
        downPaymentPercent = downPaymentPercent,
        downPaymentAmount = dpAmt,
        principal = pokok,
        margin = margin,
        tenorMonths = tenorMonths,
        monthlyInstallment = inst,
        adminFee = CC_ADMIN_FEE,
    )
}

data class InstallmentQuote(
    val id: String,
    val grams: Double,
    val pricePerGram: Long,
    val calculation: InstallmentCalculation,
    val paymentMode: String = "auto",
    val fundingSource: String = "kas",
    val lockedAtMs: Long,
    val expiresAtMs: Long,
) {
    fun isExpired(nowMs: Long): Boolean = nowMs >= expiresAtMs
    fun secondsRemaining(nowMs: Long): Int = ((expiresAtMs - nowMs).coerceAtLeast(0L) / 1000L).toInt()
}

enum class GoldBrand(val key: String, val displayName: String, val certType: String, val serialPrefix: String) {
    LM_REI("lmrei", "LM REI", "Sertifikat Raja Emas", "LMR"),
    ANTAM("antam", "Antam", "CertiCard", "ANT"),
    GALERI_24("g24", "Galeri 24", "Sertifikat Galeri 24", "G24");

    companion object {
        fun fromKey(k: String): GoldBrand = entries.firstOrNull { it.key.equals(k, ignoreCase = true) } ?: LM_REI
    }
}

typealias PrintBrand = GoldBrand

enum class PhysicalOrderStatus(val label: String) {
    WAITING_PAYMENT("Menunggu pembayaran"),
    CREATED("Pesanan dibuat"),
    PREPARING("Sedang disiapkan"),
    SHIPPED_TO_BRANCH("Dikirim ke cabang"),
    IN_TRANSIT("Dikirim"),
    READY_PICKUP("Siap diambil"),
    DONE("Selesai"),
    DELIVERED("Diterima");

    val isDone: Boolean get() = this == DONE || this == DELIVERED
    val isReadyPickup: Boolean get() = this == READY_PICKUP
}

data class GoldBranch(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val distanceKm: Double,
    val openingHours: String,
    val isSevenDays: Boolean,
    val phone: String = "021-55667788",
)

data class GoldBarStock(
    val brand: GoldBrand,
    val weightGrams: Double,
    val inStock: Boolean,
)

data class PhysicalPrintOrder(
    val id: String,
    val orderNumber: String = id,
    val brand: GoldBrand,
    val weightGrams: Double,
    val quantity: Int = 1,
    val method: PrintMethod,
    val branchId: String? = null,
    val branchName: String? = null,
    val branchAddress: String? = null,
    val destinationAddress: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val status: PhysicalOrderStatus,
    val ticketCode: String,
    val qrCodePayload: String,
    val printFee: Long = 50_000L,
    val shippingInsuranceFee: Long = 0L,
    val totalPayment: Long,
    val serialNumber: String? = null,
    val trackingNumber: String? = null,
    val pickupScheduleDate: String? = null,
    val pickupScheduleTimeSlot: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.WALLET,
    val createdAtMs: Long,
    val txId: String? = null,
) {
    val isPickup: Boolean get() = method == PrintMethod.BRANCH
    val isDelivery: Boolean get() = method != PrintMethod.BRANCH
}

typealias GoldinPrintOrder = PhysicalPrintOrder

data class InstallmentScheduleItem(
    val monthNumber: Int,
    val dueDateMs: Long,
    val amount: Long,
    val isPaid: Boolean,
    val paidAtMs: Long? = null,
    val txId: String? = null,
)

data class GoldinInstallmentContract(
    val id: String,
    val partner: String = CC_PARTNER_NAME,
    val goldGrams: Double,
    val goldPriceAtAgreement: Long,
    val goldValue: Long = 0L,
    val downPayment: Long,
    val adminFee: Long = CC_ADMIN_FEE,
    val principal: Long = 0L,
    val margin: Long = 0L,
    val tenorMonths: Int,
    val downPaymentPercent: Int = 20,
    val monthlyInstallment: Long,
    val paidMonths: Int = 0,
    val paymentMode: String = "auto", // "auto" atau "manual"
    val autoDebitSource: String = "kas", // "kas" atau id rekening bank
    val dueDay: Int = 5, // 1..28
    val agreedAtMs: Long,
    val status: String = "active", // "active", "done"
    val isPaidOff: Boolean = false,
    val schedule: List<InstallmentScheduleItem> = emptyList(),
) {
    val isDone: Boolean get() = isPaidOff || status == "done" || paidMonths >= tenorMonths
    val remainingPayment: Long get() = monthlyInstallment * (tenorMonths - paidMonths).coerceAtLeast(0)
    val progressPercent: Double get() = if (tenorMonths > 0) (paidMonths.toDouble() / tenorMonths) * 100.0 else 0.0
    val nextDueItem: InstallmentScheduleItem? get() = schedule.firstOrNull { !it.isPaid }
}

data class SavedAddress(
    val id: String,
    val label: String,
    val recipientName: String,
    val phoneNumber: String,
    val province: String,
    val city: String,
    val district: String,
    val subDistrict: String,
    val postalCode: String,
    val details: String,
    val isPrimary: Boolean = false,
)

enum class NotificationCategory(val label: String) {
    TRANSACTION("Transaksi"),
    ANNOUNCEMENT("Pengumuman"),
}

data class NotificationItem(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val message: String,
    val atMs: Long,
    val isRead: Boolean = false,
    val txId: String? = null,
)

data class AppState(
    val user: UserProfile? = null,
    val pin: String? = null,
    val registeredPassword: String = "Reitesting123",
    val isAccountLocked: Boolean = false,
    val pinLockLevel: Int = 0,
    val pinFailedAttempts: Int = 0,
    val isFirstTimeLogin: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val hideHomeBalance: Boolean = true,
    val hidePortfolioGold: Boolean = true,
    val hidePortfolioCash: Boolean = true,
    val skus: List<Sku> = emptyList(),
    val buybackPerGram: Long = 2_257_500L,
    val askPricePerGram: Long = 2_315_000L,
    val priceBaseAskPerGram: Long = 2_287_500L,
    val priceUpdatedAtMs: Long = 0L,
    val priceUpdatedAtLabel: String = "12.30 WIB",
    val priceFlash: String? = null,
    val walletBalance: Long = 0,
    val goldBalanceGrams: Double = 0.0,
    val lockedGoldGrams: Double = 0.0,
    val avgBuyPricePerGram: Long = 2_250_000L,
    val tokens: List<GoldToken> = emptyList(),
    val transactions: List<Tx> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val printOrders: List<PrintOrder> = emptyList(),
    val contracts: List<InstallmentContract> = emptyList(),
    // Koleksi baru Goldin v1.2
    val routinePlans: List<RoutineSavingsPlan> = emptyList(),
    val installmentContracts: List<GoldinInstallmentContract> = emptyList(),
    val physicalOrders: List<PhysicalPrintOrder> = emptyList(),
    val savedAddresses: List<SavedAddress> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
) {
    val openTokens: List<GoldToken>
        get() = tokens.filter { it.status != TokenStatus.RETURNED && it.status != TokenStatus.PRINTED && it.status != TokenStatus.CANCELLED }

    fun sku(code: String): Sku = skus.first { it.code == code }

    val ownedTokens: List<GoldToken> get() = openTokens.filter { it.status != TokenStatus.LOCKED_INSTALLMENT }

    val totalWeightMg: Long get() = (goldBalanceGrams * 1000).toLong()

    /** Total nilai estimasi tabungan emas berdasarkan harga beli kembali (bid). */
    val portfolioValue: Long get() = (goldBalanceGrams * buybackPerGram).toLong()

    /** Saldo emas yang dapat dicairkan atau dicetak (di luar yang terkunci cicilan). */
    val freeGoldGrams: Double get() = (goldBalanceGrams - lockedGoldGrams).coerceAtLeast(0.0)

    val unreadNotificationCount: Int get() = notifications.count { !it.isRead }

    /** Persentase perubahan harga beli hari ini terhadap harga acuan dasar (`BASE = Rp2.287.500`). */
    val dayPriceChangePct: Double
        get() = if (priceBaseAskPerGram > 0) {
            (askPricePerGram - priceBaseAskPerGram).toDouble() / priceBaseAskPerGram.toDouble() * 100.0
        } else {
            0.0
        }
}

class ReidigException(val code: String, message: String) : Exception(message)
