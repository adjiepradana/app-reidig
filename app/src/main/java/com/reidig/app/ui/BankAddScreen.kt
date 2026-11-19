package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.SupportedBank
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Layar Tambah Rekening Bank (`25 Tambah rekening`, route `"bank-add"`).
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2422–2442, L4557–4566,
 * spesifikasi Design System v2 & Handoff §9 / §14.
 */
@Composable
fun BankAddScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onAccountAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val userKtpName = remember(state.user) {
        state.user?.name?.trim()?.uppercase()?.ifBlank { "BUDI SANTOSO" } ?: "BUDI SANTOSO"
    }

    var selectedBank by remember { mutableStateOf<SupportedBank?>(null) }
    var showBankSheet by remember { mutableStateOf(false) }
    var accountNumber by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val canSubmit = selectedBank != null && accountNumber.length >= 8 && !isChecking

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // Header 44dp
        TopTitleBar(
            title = "Tambah rekening",
            onBack = onBack,
        )

        // Konten formulir rekening
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Field 1: Nama Pemilik Rekening (Disabled / Sesuai e-KTP)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nama pemilik rekening",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.Neutral100)
                            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = userKtpName,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.Lock,
                            contentDescription = null,
                            tint = Palette.InkSoft,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Nama rekening harus sesuai dengan nama e-KTP untuk mencegah penipuan.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSoft,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            // Field 2: Bank (Dropdown Trigger Box)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Bank",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "*",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Palette.Critical,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
                            .clickable { showBankSheet = true }
                            .padding(
                                start = if (selectedBank != null) 4.dp else 16.dp,
                                end = 12.dp,
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                if (selectedBank != null) {
                                    GoldinBankLogoBadge(bankName = selectedBank!!.name)
                                    Text(
                                        text = selectedBank!!.name,
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = Palette.Ink,
                                    )
                                } else {
                                    Text(
                                        text = "Pilih bank",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 14.sp,
                                        color = Palette.InkSoft,
                                    )
                                }
                            }

                            Icon(
                                imageVector = PrototypeIcons.ChevronDown,
                                contentDescription = "Pilih bank",
                                tint = Palette.InkSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Field 3: Nomor Rekening (Input Numeric)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Nomor rekening",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "*",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Palette.Critical,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(
                                width = 1.dp,
                                color = if (errorMessage != null) Palette.Critical else Palette.Line,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (accountNumber.isEmpty()) {
                            Text(
                                text = "Masukkan nomor rekening",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.InkSoft,
                            )
                        }

                        BasicTextField(
                            value = accountNumber,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(16)
                                accountNumber = digits
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Palette.Ink,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(Palette.Primary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Tampilan pesan error duplikasi / mismatch / format
                    if (errorMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.AlertCircle,
                                contentDescription = null,
                                tint = Palette.Critical,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = Palette.Critical,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
            }

            // Info Card Keamanan & Validasi Sistem
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.Primary50)
                        .border(1.dp, Palette.Primary100, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.ShieldCheck,
                            contentDescription = null,
                            tint = Palette.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Rekening akan diverifikasi dengan sistem bank. Pastikan nomor rekening masih aktif untuk kenyamanan penarikan dana.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }

        // Sticky Footer CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 0.5.dp, color = Palette.Line)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            PrimaryButton(
                text = "Tambah rekening",
                onClick = {
                    if (canSubmit) {
                        val bank = selectedBank ?: return@PrimaryButton
                        // Cek duplikasi rekening lokal
                        val isDuplicate = state.bankAccounts.any {
                            it.bank.equals(bank.name, ignoreCase = true) &&
                                it.number.filter { ch -> ch.isDigit() } == accountNumber
                        }

                        if (isDuplicate) {
                            errorMessage = "Rekening ini sudah terdaftar di akunmu."
                            return@PrimaryButton
                        }

                        // Simulasi pengecekan rekening 1.2 detik (Main.dc.html L3856-3863)
                        isChecking = true
                        errorMessage = null

                        coroutineScope.launch {
                            delay(1200L)
                            isChecking = false
                            // Pengecekan sukses -> Buka konfirmasi PIN
                            showPinDialog = true
                        }
                    }
                },
                enabled = canSubmit,
                loading = isChecking,
                loadingText = "Mengecek rekening…",
                modifier = Modifier.fillMaxWidth(),
                height = 44.dp,
            )
        }
    }

    // Sheet Pemilihan Bank
    if (showBankSheet) {
        BankSelectSheet(
            selectedBankKey = selectedBank?.key,
            onSelectBank = { bank ->
                selectedBank = bank
                showBankSheet = false
                errorMessage = null
            },
            onDismiss = { showBankSheet = false },
        )
    }

    // Dialog Konfirmasi PIN Transaksi untuk Menyimpan Rekening (Main.dc.html L4581, L3868)
    if (showPinDialog) {
        val bank = selectedBank
        if (bank != null) {
            PinDialog(
                env = env,
                title = "Masukkan PIN 6 digit untuk menyimpan rekening ini.",
                onDismiss = { showPinDialog = false },
                onVerified = {
                    showPinDialog = false
                    try {
                        env.repo.verifyAndAddBankAccount(
                            bank = bank.name,
                            number = accountNumber,
                            holder = userKtpName,
                        )
                        env.notify("Rekening ${bank.name} ••••${accountNumber.takeLast(4)} tersimpan")
                        onAccountAdded()
                    } catch (e: Exception) {
                        val msg = e.message.orEmpty()
                        errorMessage = when {
                            msg.contains("DUPLICATE_ACCOUNT", ignoreCase = true) || msg.contains("sudah terdaftar", ignoreCase = true) ->
                                "Rekening ini sudah terdaftar di akunmu."
                            msg.contains("MISMATCH_NAME", ignoreCase = true) || msg.contains("tidak sesuai", ignoreCase = true) ->
                                "Nama pemilik rekening tidak sesuai dengan e-KTP. Gunakan rekening atas namamu sendiri."
                            msg.contains("INVALID_ACCOUNT", ignoreCase = true) || msg.contains("tidak valid", ignoreCase = true) ->
                                "Rekening tidak ditemukan. Periksa kembali bank dan nomor rekening."
                            else -> msg.ifBlank { "Gagal menambahkan rekening" }
                        }
                    }
                },
            )
        }
    }
}

@ScreenPreview
@Composable
private fun BankAddScreenPreview() = ReidigTheme {
    BankAddScreen(
        env = rememberPreviewEnv(),
        onBack = {},
        onAccountAdded = {},
    )
}
