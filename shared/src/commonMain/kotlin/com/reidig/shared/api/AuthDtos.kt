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
data class ApiErrorDetail(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail? = null,
    val detail: String? = null,
)
