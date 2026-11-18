package com.reidig.shared

enum class TokenStatus { ACTIVE, LOCKED_INSTALLMENT, PRINT_PENDING, RETURNED, PRINTED, CANCELLED }

enum class PaymentMethod(val label: String) {
    WALLET("Saldo Kas"),
    QRIS("QRIS"),
    VIRTUAL_ACCOUNT("Virtual Account BCA"),
    VA_BCA("BCA Virtual Account"),
    VA_BRI("BRI Virtual Account"),
    VA_MANDIRI("Mandiri Virtual Account"),
    VA_BNI("BNI Virtual Account"),
    CICILAN("Cicil Emas (BSI Finance)"),
}

enum class PrintMethod(val label: String) {
    BRANCH("Ambil di cabang"),
    JNE("Dikirim ke alamat"),
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
)

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
    val status: String = "ok", // ok, pending, fail
    val method: String = "Saldo Kas",
    val grams: Double = 0.0,
    val pricePerGram: Long = 0,
    val fee: Long = 0,
    val tax: Long = 0,
    val bursaRef: String? = null,
    val kliringRef: String? = null,
    val payRef: String? = null,
)

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
}

enum class GoldBrand(val displayName: String) {
    ANTAM("Antam"),
    GALERI_24("Galeri 24"),
    LM_REI("LM REI"),
}

enum class PhysicalOrderStatus(val label: String) {
    WAITING_PAYMENT("Menunggu pembayaran"),
    CREATED("Pesanan dibuat"),
    PREPARING("Sedang disiapkan"),
    IN_TRANSIT("Dikirim"),
    READY_PICKUP("Siap diambil"),
    DONE("Selesai"),
}

data class PhysicalPrintOrder(
    val id: String,
    val brand: GoldBrand,
    val weightGrams: Double,
    val quantity: Int = 1,
    val method: PrintMethod,
    val branchId: String? = null,
    val branchName: String? = null,
    val destinationAddress: String? = null,
    val status: PhysicalOrderStatus,
    val ticketCode: String,
    val qrCodePayload: String,
    val printFee: Long = 50_000L,
    val shippingInsuranceFee: Long = 0L,
    val totalPayment: Long,
    val serialNumber: String? = null,
    val pickupScheduleDate: String? = null,
    val pickupScheduleTimeSlot: String? = null,
    val createdAtMs: Long,
)

data class InstallmentScheduleItem(
    val monthNumber: Int,
    val dueDateMs: Long,
    val amount: Long,
    val isPaid: Boolean,
    val paidAtMs: Long? = null,
)

data class GoldinInstallmentContract(
    val id: String,
    val partner: String = "BSI Finance",
    val goldGrams: Double,
    val goldPriceAtAgreement: Long,
    val downPayment: Long,
    val adminFee: Long = 25_000L,
    val tenorMonths: Int,
    val monthlyInstallment: Long,
    val autoDebitMethod: String = "kas",
    val agreedAtMs: Long,
    val isPaidOff: Boolean = false,
    val schedule: List<InstallmentScheduleItem> = emptyList(),
)

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
