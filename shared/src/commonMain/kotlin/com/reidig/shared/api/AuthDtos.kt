package com.reidig.shared.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailCodeIn(
    val email: String,
    val purpose: String, // "REGISTER" | "RESET_PASSWORD"
)

@Serializable
data class EmailCodeOut(
    @SerialName("expires_in_seconds") val expiresInSeconds: Int,
    @SerialName("resend_in_seconds") val resendInSeconds: Int,
    @SerialName("dev_code") val devCode: String? = null,
)

@Serializable
data class EmailVerifyIn(
    val email: String,
    val purpose: String,
    val code: String,
)

@Serializable
data class EmailTicketOut(
    @SerialName("email_ticket") val emailTicket: String,
)

@Serializable
data class RegisterIn(
    @SerialName("email_ticket") val emailTicket: String,
    val name: String,
    val email: String,
    val password: String,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val phone: String? = null,
)

@Serializable
data class LoginIn(
    val email: String,
    val password: String,
)

@Serializable
data class SessionOut(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
)

@Serializable
data class PasswordResetIn(
    @SerialName("email_ticket") val emailTicket: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class MeOut(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("kyc_status") val kycStatus: String = "NONE",
    @SerialName("kyc_tier") val kycTier: Int = 0,
    val verified: Boolean = false,
    @SerialName("has_pin") val hasPin: Boolean = false,
    @SerialName("pin_locked") val pinLocked: Boolean = false,
)

@Serializable
data class PinIn(
    val pin: String,
    @SerialName("old_pin") val oldPin: String? = null,
)

@Serializable
data class BankAccountIn(
    @SerialName("bank_code") val bankCode: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("holder_name") val holderName: String,
)

@Serializable
data class BankAccountOut(
    val id: Int,
    @SerialName("bank_code") val bankCode: String,
    @SerialName("account_number_masked") val accountNumberMasked: String,
    @SerialName("holder_name") val holderName: String,
    val verified: Boolean = false,
)

@Serializable
data class PriceOut(
    @SerialName("sku_code") val skuCode: String,
    @SerialName("buy_price") val buyPrice: String,
    @SerialName("buyback_price") val buybackPrice: String,
    val source: String,
    @SerialName("fetched_at") val fetchedAt: String,
)

@Serializable
data class SkuOut(
    val code: String,
    val brand: String = "Raja Emas",
    val product: String,
    @SerialName("weight_mg") val weightMg: Long,
    @SerialName("buy_price") val buyPrice: String? = null,
    @SerialName("buyback_price") val buybackPrice: String? = null,
    val stock: Int = 0,
    @SerialName("trading_halted") val tradingHalted: Boolean = false,
    @SerialName("price_updated_at") val priceUpdatedAt: String? = null,
)

@Serializable
data class CatalogOut(
    @SerialName("trading_halted") val tradingHalted: Boolean = false,
    val skus: List<SkuOut> = emptyList(),
)

@Serializable
data class BranchOut(
    val code: String,
    val name: String,
    val area: String? = null,
    @SerialName("pickup_estimate") val pickupEstimate: String? = null,
)

@Serializable
data class WalletOut(
    val available: String,
    val held: String = "0",
)

@Serializable
data class EntryOut(
    val id: Int,
    val type: String,
    val amount: String,
    @SerialName("reference_type") val referenceType: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class TopupIn(
    val amount: String,
)

@Serializable
data class WithdrawIn(
    val amount: String,
    @SerialName("bank_account_id") val bankAccountId: Int,
    val pin: String,
)

@Serializable
data class WithdrawalOut(
    val id: Int,
    val amount: String,
    val status: String,
    @SerialName("bank_code") val bankCode: String,
    @SerialName("account_number_masked") val accountNumberMasked: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ApiErrorDetail(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail? = null,
    val detail: String? = null,
)
