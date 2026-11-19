package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.app.R
import com.reidig.shared.BankAccount
import com.reidig.shared.SUPPORTED_BANKS
import com.reidig.shared.SupportedBank
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Badge Logo Bank resmi 72x40dp sesuai prototipe `Main.dc.html` L2002.
 */
@Composable
fun GoldinBankLogoBadge(
    bankName: String,
    modifier: Modifier = Modifier,
) {
    val logoRes = when {
        bankName.contains("BCA", ignoreCase = true) -> R.drawable.logo_bca
        bankName.contains("BRI", ignoreCase = true) -> R.drawable.logo_bri
        bankName.contains("Mandiri", ignoreCase = true) -> R.drawable.logo_mandiri
        bankName.contains("BNI", ignoreCase = true) -> R.drawable.logo_bni
        else -> null
    }

    Box(
        modifier = modifier
            .width(72.dp)
            .height(40.dp)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = bankName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.Primary50)
                    .border(1.dp, Palette.Line, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = bankName.take(4).uppercase(),
                    color = Palette.Primary,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/**
 * Bottom Sheet Pemilihan Bank (`Sheet · Pilih bank`, `Main.dc.html` L4198).
 * Menampilkan daftar 4 bank resmi yang didukung (BCA, BRI, Mandiri, BNI)
 * dengan logo resmi dan radio ring indicator.
 */
@Composable
fun BankSelectSheet(
    selectedBankKey: String?,
    onSelectBank: (SupportedBank) -> Unit,
    onDismiss: () -> Unit,
) {
    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = "Pilih bank",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            SUPPORTED_BANKS.forEachIndexed { index, bank ->
                val isSelected = selectedBankKey?.equals(bank.key, ignoreCase = true) == true ||
                    selectedBankKey?.equals(bank.name, ignoreCase = true) == true

                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val isPressed by interactionSource.collectIsPressedAsState()
                val rowBg by animateColorAsState(
                    targetValue = when {
                        isPressed -> Palette.Neutral200
                        isHovered -> Palette.Neutral100
                        isSelected -> Palette.Primary50
                        else -> Color.Transparent
                    },
                    animationSpec = tween(GoldinMotion.DurationColor),
                    label = "bankSelectRowBg_$index",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(rowBg)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelectBank(bank) },
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GoldinBankLogoBadge(bankName = bank.name)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (bank.key.lowercase()) {
                                "bca" -> "Bank Central Asia (BCA)"
                                "bri" -> "Bank Rakyat Indonesia (BRI)"
                                "mandiri" -> "Bank Mandiri"
                                "bni" -> "Bank Negara Indonesia (BNI)"
                                else -> bank.name
                            },
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                    }

                    // Radio indicator ring (20dp, dot 10dp)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Palette.Primary else Palette.Line,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Palette.Primary),
                            )
                        }
                    }
                }

                if (index < SUPPORTED_BANKS.lastIndex) {
                    HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)
                }
            }
        }
    }
}

/**
 * Bottom Sheet Drawer Detail Rekening Bank (`Sheet · Detail rekening` `Main.dc.html` L3001-3023).
 * Menampilkan logo bank resmi, nomor rekening dengan tombol salin, nama pemilik,
 * tanggal ditambahkan, status auto-debit, dan tombol outline merah "Hapus rekening".
 */
@Composable
fun BankDetailDrawer(
    bank: BankAccount,
    env: AppEnv? = null,
    onDismiss: () -> Unit,
    onCopyNumber: () -> Unit,
    onDelete: () -> Unit,
    onAutoDebitToggled: (() -> Unit)? = null,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAutoDebitConfirm by remember { mutableStateOf(false) }
    var showAutoDebitOtp by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID")) }

    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = "Detail rekening",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Box Bank Logo & Nama Bank (Main.dc.html L3010-3013)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
                    .padding(start = 4.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GoldinBankLogoBadge(bankName = bank.bank)
                    Text(
                        text = bank.bank,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Palette.Ink,
                    )
                }
            }

            // Rincian Rekening (Main.dc.html L3014-3019)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Baris Nomor Rekening + Tombol Salin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Nomor rekening",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                        )
                        Text(
                            text = bank.number.chunked(4).joinToString(" "),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Palette.Ink,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCopyNumber),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.Copy,
                            contentDescription = "Salin nomor rekening",
                            tint = Palette.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)

                // Baris Nama Pemilik
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "Nama pemilik",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                    )
                    Text(
                        text = bank.holder,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                        textAlign = TextAlign.End,
                    )
                }

                // Baris Ditambahkan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "Ditambahkan",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                    )
                    Text(
                        text = if (bank.addedAtMs > 0L) dateFormat.format(Date(bank.addedAtMs)) else "–",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                        textAlign = TextAlign.End,
                    )
                }

                HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)

                // Baris Status Auto-Debit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Auto-debit",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            color = Palette.InkSecondary,
                        )
                        Text(
                            text = if (bank.isAutoDebitActive) "Aktif untuk tabungan & cicilan" else "Belum aktif",
                            fontFamily = BodyFontFamily,
                            fontSize = 11.sp,
                            color = if (bank.isAutoDebitActive) Palette.Good700 else Palette.InkSoft,
                        )
                    }

                    if (bank.isAutoDebitActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Palette.GoodSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Aktif",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Palette.Good700,
                            )
                        }
                    } else {
                        SecondaryButton(
                            text = "Aktifkan",
                            onClick = { showAutoDebitConfirm = true },
                            height = 32.dp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Tombol Destructive Outline Hapus Rekening (Main.dc.html L3020)
            DestructiveButton(
                text = "Hapus rekening",
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                height = 44.dp,
                filled = false,
                leadingIcon = PrototypeIcons.Trash,
            )
        }
    }

    // Modal Konfirmasi Hapus Rekening (Main.dc.html L4128)
    if (showDeleteConfirm) {
        GoldinModal(
            title = "Hapus rekening ini?",
            message = "${bank.displayLabel} a.n. ${bank.holder} tidak bisa dipakai untuk tarik dana lagi. Kamu bisa menambahkannya kembali kapan saja.",
            confirmLabel = "Hapus",
            cancelLabel = "Batal",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onCancel = { showDeleteConfirm = false },
        )
    }

    // Modal Konfirmasi Aktifkan Auto-Debit (Main.dc.html L4130)
    if (showAutoDebitConfirm) {
        AutoDebitModal(
            bank = bank,
            onConfirm = {
                showAutoDebitConfirm = false
                showAutoDebitOtp = true
            },
            onDismiss = { showAutoDebitConfirm = false },
        )
    }

    // Dialog Verifikasi SMS OTP Auto-Debit (Main.dc.html L3796, L4887)
    if (showAutoDebitOtp) {
        AutoDebitOtpDialog(
            bank = bank,
            onDismiss = { showAutoDebitOtp = false },
            onVerified = { otpCode ->
                env?.repo?.toggleAutoDebit(bank.id, otpCode)
                env?.notify("Auto-debit ${bank.displayLabel} aktif")
                showAutoDebitOtp = false
                onAutoDebitToggled?.invoke()
            },
        )
    }
}

/**
 * Modal Konfirmasi Aktivasi Auto-Debit (`Modal · Aktifkan auto-debit`, `Main.dc.html` L4130).
 */
@Composable
fun AutoDebitModal(
    bank: BankAccount,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GoldinModal(
        title = "Aktifkan auto-debit?",
        message = "${bank.displayLabel} akan didebit otomatis sesuai jadwal tabunganmu.",
        confirmLabel = "Lanjut",
        cancelLabel = "Batal",
        onConfirm = onConfirm,
        onCancel = onDismiss,
    )
}

/**
 * Dialog Verifikasi SMS OTP Bank untuk Auto-Debit (`Main.dc.html` L3796, L4887).
 * Menampilkan 6 digit kotak OTP, hitung mundur 30 detik, simulasi verifikasi,
 * dan kode tes default "123456".
 */
@Composable
fun AutoDebitOtpDialog(
    bank: BankAccount,
    onDismiss: () -> Unit,
    onVerified: (String) -> Unit,
) {
    var otpCode by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableIntStateOf(30) }
    var isVerifying by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Countdown Timer 30 detik
    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000L)
            timerSeconds -= 1
        }
    }

    // Auto-focus input
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto submit saat 6 digit terisi
    LaunchedEffect(otpCode) {
        if (otpCode.length == 6 && !isVerifying) {
            isVerifying = true
            hasError = false
            errorMessage = null
            focusManager.clearFocus()

            delay(800L) // Simulasi verifikasi OTP 800ms
            if (otpCode == "123456") {
                onVerified(otpCode)
            } else {
                isVerifying = false
                hasError = true
                errorMessage = "Kode OTP salah. Coba lagi (kode tes: 123456)."
                // Trigger shake animation
                coroutineScope.launch {
                    shakeOffset.animateTo(
                        targetValue = 10f,
                        animationSpec = tween(durationMillis = 60),
                    )
                    shakeOffset.animateTo(
                        targetValue = -10f,
                        animationSpec = tween(durationMillis = 60),
                    )
                    shakeOffset.animateTo(
                        targetValue = 6f,
                        animationSpec = tween(durationMillis = 60),
                    )
                    shakeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 60),
                    )
                }
            }
        }
    }

    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = "Verifikasi SMS OTP",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Icon SMS / Bank Shield
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Palette.Primary50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PrototypeIcons.ShieldCheck,
                    contentDescription = null,
                    tint = Palette.Primary,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Title & Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "SMS dari ${bank.bank}",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Palette.Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Kode OTP dikirim melalui SMS dari ${bank.bank} ke 0812••••7890 (kode tes: 123456).",
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    color = Palette.InkSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }

            // 6-Digit OTP Boxes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Hidden TextField to capture numeric input
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(6)
                        otpCode = digits
                        if (hasError) {
                            hasError = false
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )

                // Visual 6 digit boxes (48x48dp per Handoff §3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(6) { index ->
                        val char = otpCode.getOrNull(index)
                        val isCurrent = index == otpCode.length
                        val boxBorderColor = when {
                            hasError -> Palette.Critical
                            isCurrent -> Palette.Primary
                            char != null -> Palette.Primary
                            else -> Palette.Line
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(
                                    width = if (isCurrent || hasError) 2.dp else 1.dp,
                                    color = boxBorderColor,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { focusRequester.requestFocus() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (char != null) {
                                Text(
                                    text = char.toString(),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Palette.Ink,
                                )
                            } else if (isCurrent && !isVerifying) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(20.dp)
                                        .background(Palette.Primary),
                                )
                            }
                        }
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = PrototypeIcons.AlertCircle,
                        contentDescription = null,
                        tint = Palette.Critical,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = errorMessage.orEmpty(),
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        color = Palette.Critical,
                    )
                }
            }

            // Spinner saat verifying
            AnimatedVisibility(
                visible = isVerifying,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Palette.Primary,
                    )
                    Text(
                        text = "Memverifikasi kode OTP…",
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = Palette.Primary,
                    )
                }
            }

            // Timer & Resend Button
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (timerSeconds > 0) {
                    Text(
                        text = "Kirim ulang kode dalam 0:${timerSeconds.toString().padStart(2, '0')}",
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = Palette.InkSecondary,
                    )
                } else {
                    Text(
                        text = "Tidak menerima SMS? ",
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = Palette.InkSecondary,
                    )
                    Text(
                        text = "Kirim ulang",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Palette.Primary,
                        modifier = Modifier.clickable {
                            timerSeconds = 30
                            otpCode = ""
                            hasError = false
                            errorMessage = null
                        },
                    )
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun BankDetailDrawerPreview() = ReidigTheme {
    BankDetailDrawer(
        bank = BankAccount(
            id = "bank-1",
            bank = "BCA",
            number = "1234567890",
            holder = "BUDI SANTOSO",
            addedAtMs = System.currentTimeMillis(),
            isPrimary = true,
            isAutoDebitActive = false,
        ),
        onDismiss = {},
        onCopyNumber = {},
        onDelete = {},
    )
}
