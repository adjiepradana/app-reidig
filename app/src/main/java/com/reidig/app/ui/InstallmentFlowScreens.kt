package com.reidig.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.BankAccount
import com.reidig.shared.CC_ADMIN_FEE
import com.reidig.shared.CC_DPS
import com.reidig.shared.CC_LOCK_WINDOW_MIN
import com.reidig.shared.CC_MIN_VAL
import com.reidig.shared.CC_PARTNER_NAME
import com.reidig.shared.CC_TENORS
import com.reidig.shared.GoldinInstallmentContract
import com.reidig.shared.InstallmentCalculation
import com.reidig.shared.InstallmentQuote
import com.reidig.shared.PaymentMethod
import com.reidig.shared.calculateInstallment
import com.reidig.shared.formatGrams4
import com.reidig.shared.formatNumberOnly
import com.reidig.shared.parseDateComponents
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

// =========================================================================================
// SCREEN 40: SIMULASI CICIL EMAS (`40 Cicil emas · Simulasi` / `cicil/sim`)
// =========================================================================================

@Composable
fun InstallmentSimScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onNext: (Double, Int, Int) -> Unit,
    initialGrams: Double = 1.0,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    var gramsRaw by rememberSaveable { mutableStateOf(if (initialGrams > 0.0) formatGrams4(initialGrams).replace(" gr", "").trim() else "1") }
    var selectedTenor by rememberSaveable { mutableIntStateOf(12) }
    var selectedDp by rememberSaveable { mutableIntStateOf(20) }
    var showExitModal by remember { mutableStateOf(false) }

    val gramsVal = gramsRaw.replace(',', '.').toDoubleOrNull() ?: 0.0
    val calc = remember(gramsVal, selectedTenor, selectedDp, state.askPricePerGram) {
        if (gramsVal > 0.0) {
            calculateInstallment(gramsVal, state.askPricePerGram, selectedTenor, selectedDp)
        } else null
    }

    val isGoldValueValid = calc != null && calc.goldValue >= CC_MIN_VAL
    val isGoldValueError = gramsRaw.isNotEmpty() && gramsVal > 0.0 && !isGoldValueValid

    BackHandler { showExitModal = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // Top App Bar
        TopTitleBar(
            title = "Cicil emas",
            onBack = { showExitModal = true },
        )

        // Stepper: Langkah 1 dari 3
        LabeledStepBar(
            current = 1,
            labels = listOf("Simulasi", "Pembayaran", "Konfirmasi"),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = GoldinSpace.Xl, end = GoldinSpace.Xl, top = GoldinSpace.Lg, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xl),
        ) {
            // 1. Kartu Harga Emas Hari Ini
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                        .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Harga emas hari ini",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                    )
                    Text(
                        text = "${rupiah(state.askPricePerGram)}/gr",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                }
            }

            // 2. Input Berat Emas*
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Berat emas",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "*",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Critical,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(GoldinControl.SizeMd)
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Color.White)
                            .border(1.dp, if (isGoldValueError) Palette.Critical else Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                            .padding(horizontal = GoldinSpace.Lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (gramsRaw.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.InkSoft,
                                )
                            }
                            BasicTextField(
                                value = gramsRaw,
                                onValueChange = { input ->
                                    val clean = input.replace('.', ',').filter { it.isDigit() || it == ',' }
                                    val parts = clean.split(',')
                                    gramsRaw = if (parts.size > 1) {
                                        "${parts[0].take(4)},${parts.drop(1).joinToString("").take(4)}"
                                    } else {
                                        clean.take(4)
                                    }
                                },
                                textStyle = TextStyle(
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Palette.Primary700),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            text = "gr",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            color = Palette.InkSecondary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    if (isGoldValueError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.AlertCircle,
                                contentDescription = null,
                                tint = Palette.Critical,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Minimal harga emas Rp500.000",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = Palette.Critical,
                            )
                        }
                    } else {
                        Text(
                            text = "Minimal harga emas Rp500.000",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // 3. Choice Chips Tenor*
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tenor",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "*",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Critical,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CC_TENORS.forEach { tenor ->
                            val isSelected = selectedTenor == tenor
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) Palette.Primary50 else Color.White,
                                animationSpec = tween(GoldinMotion.DurationHover),
                                label = "tenorBg",
                            )
                            val borderColor = if (isSelected) Palette.Primary700 else Palette.Line
                            val textColor = if (isSelected) Palette.Primary700 else Palette.Ink

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Lg))
                                    .background(bg)
                                    .border(1.dp, borderColor, RoundedCornerShape(GoldinRadius.Lg))
                                    .clickable { selectedTenor = tenor },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$tenor bln",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = textColor,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Choice Chips Uang Muka (DP)*
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Uang muka (DP)",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "*",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Critical,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CC_DPS.forEach { dp ->
                            val isSelected = selectedDp == dp
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) Palette.Primary50 else Color.White,
                                animationSpec = tween(GoldinMotion.DurationHover),
                                label = "dpBg",
                            )
                            val borderColor = if (isSelected) Palette.Primary700 else Palette.Line
                            val textColor = if (isSelected) Palette.Primary700 else Palette.Ink

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Lg))
                                    .background(bg)
                                    .border(1.dp, borderColor, RoundedCornerShape(GoldinRadius.Lg))
                                    .clickable { selectedDp = dp },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$dp%",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = textColor,
                                )
                            }
                        }
                    }
                }
            }

            // 5. Kartu Rincian Simulasi Real-Time (Muncul jika emas >= Rp500.000)
            if (isGoldValueValid) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(GoldinSpace.Lg),
                        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                    ) {
                        Text(
                            text = "Rincian simulasi",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Palette.Ink,
                        )

                        SimCalculationRow("Harga emas (estimasi)", formatNumberOnly(calc.goldValue), isMono = true)
                        SimCalculationRow("Uang muka (DP $selectedDp%)", formatNumberOnly(calc.downPaymentAmount), isMono = true)
                        SimCalculationRow("Pokok pembiayaan", formatNumberOnly(calc.principal), isMono = true)
                        SimCalculationRow("Margin (0,9%/bulan)", formatNumberOnly(calc.margin), isMono = true)
                        SimCalculationRow("Tenor", "$selectedTenor bulan", isMono = false)

                        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Angsuran per bulan",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Ink,
                            )
                            Text(
                                text = "${rupiah(calc.monthlyInstallment)}/bln",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Palette.Primary700,
                            )
                        }
                    }
                }
            }
        }

        // Fixed Bottom Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = GoldinBorder.Hairline, color = Palette.Line, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Lg)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                text = "Selanjutnya",
                enabled = isGoldValueValid,
                onClick = { onNext(gramsVal, selectedTenor, selectedDp) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Modal Konfirmasi Keluar dari Cicil Emas
    if (showExitModal) {
        GoldinModal(
            title = "Keluar dari cicil emas?",
            message = "Simulasi yang sudah kamu atur akan dibatalkan.",
            confirmLabel = "Keluar",
            isDestructive = true,
            onConfirm = {
                showExitModal = false
                onBack()
            },
            cancelLabel = "Lanjut cicil",
            onCancel = { showExitModal = false },
        )
    }
}

// =========================================================================================
// SCREEN 41: PEMBAYARAN ANGSURAN (`41 Cicil emas · Pembayaran angsuran` / `cicil/method`)
// =========================================================================================

@Composable
fun InstallmentPaymentMethodScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onNext: (String, String) -> Unit,
    onAddBank: () -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedMode by rememberSaveable { mutableStateOf("auto") } // "auto" vs "manual"
    var selectedSource by rememberSaveable { mutableStateOf("kas") } // "kas" vs bankId
    var showExitModal by remember { mutableStateOf(false) }

    var pendingAutoDebitBank by remember { mutableStateOf<BankAccount?>(null) }
    var pendingOtpBank by remember { mutableStateOf<BankAccount?>(null) }

    val isStepValid = selectedMode == "manual" || (selectedMode == "auto" && selectedSource.isNotBlank())

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        TopTitleBar(
            title = "Cicil emas",
            onBack = onBack,
        )

        // Stepper: Langkah 2 dari 3
        LabeledStepBar(
            current = 2,
            labels = listOf("Simulasi", "Pembayaran", "Konfirmasi"),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = GoldinSpace.Xl, end = GoldinSpace.Xl, top = GoldinSpace.Lg, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xl),
        ) {
            // Mitra BSI Finance Info Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Palette.Primary50)
                        .border(1.dp, Palette.Primary100, RoundedCornerShape(GoldinRadius.Lg))
                        .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = PrototypeIcons.ShieldCheck,
                        contentDescription = null,
                        tint = Palette.Primary700,
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            text = "Mitra Pembiayaan Resmi",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Palette.Primary700,
                        )
                        Text(
                            text = "Pembiayaan syariah difasilitasi oleh $CC_PARTNER_NAME.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // Pilihan Metode Angsuran (Auto-debit vs Manual)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pembayaran angsuran",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "*",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Critical,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                    ) {
                        MethodRadioRow(
                            title = "Auto-debit",
                            subtitle = "Dipotong otomatis setiap jatuh tempo",
                            isSelected = selectedMode == "auto",
                            onClick = { selectedMode = "auto" },
                        )
                        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                        MethodRadioRow(
                            title = "Bayar manual",
                            subtitle = "Bayar sendiri sebelum jatuh tempo",
                            isSelected = selectedMode == "manual",
                            onClick = { selectedMode = "manual" },
                        )
                    }
                }
            }

            // Sumber Dana Auto-Debit (Jika memilih auto-debit)
            if (selectedMode == "auto") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sumber dana auto-debit",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Ink,
                            )
                            Text(
                                text = "*",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Critical,
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GoldinRadius.Lg))
                                .background(Color.White)
                                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                        ) {
                            // Saldo Kas
                            MethodRadioRow(
                                title = "Saldo Kas",
                                subtitle = "Saldo ${rupiah(state.walletBalance)}",
                                isSelected = selectedSource == "kas",
                                onClick = { selectedSource = "kas" },
                            )

                            // Rekening Bank
                            state.bankAccounts.forEach { bank ->
                                HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                                BankAutoDebitRadioRow(
                                    bank = bank,
                                    isSelected = selectedSource == bank.id,
                                    onClick = {
                                        if (bank.isAutoDebitActive) {
                                            selectedSource = bank.id
                                        } else {
                                            pendingAutoDebitBank = bank
                                        }
                                    },
                                )
                            }
                        }

                        // "+ Tambah rekening"
                        Row(
                            modifier = Modifier
                                .height(GoldinControl.TouchMin)
                                .clip(RoundedCornerShape(GoldinRadius.Full))
                                .clickable(onClick = onAddBank)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.Plus,
                                contentDescription = null,
                                tint = Palette.Primary700,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Tambah rekening",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Primary700,
                            )
                        }
                    }
                }
            }
        }

        // Fixed Bottom Bar: [Sebelumnya] · [Selanjutnya]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = GoldinBorder.Hairline, color = Palette.Line, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Lg)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                SecondaryButton(
                    text = "Sebelumnya",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Selanjutnya",
                    enabled = isStepValid,
                    onClick = { onNext(selectedMode, selectedSource) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Modal Konfirmasi Auto-Debit
    pendingAutoDebitBank?.let { bank ->
        AutoDebitModal(
            bank = bank,
            onConfirm = {
                val b = bank
                pendingAutoDebitBank = null
                pendingOtpBank = b
            },
            onDismiss = { pendingAutoDebitBank = null },
        )
    }

    // Dialog OTP Auto-Debit
    pendingOtpBank?.let { bank ->
        AutoDebitOtpDialog(
            bank = bank,
            onDismiss = { pendingOtpBank = null },
            onVerified = { otpCode ->
                val ok = env.repo.toggleAutoDebit(bank.id, otpCode)
                if (ok) {
                    selectedSource = bank.id
                    pendingOtpBank = null
                    Toast.makeText(context, "Auto-debit ${bank.bank} aktif", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Kode OTP salah", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

// =========================================================================================
// SCREEN 42: KONFIRMASI PENGAJUAN (`42 Cicil emas · Konfirmasi` / `cicil/confirm`)
// =========================================================================================

@Composable
fun InstallmentConfirmScreen(
    env: AppEnv,
    grams: Double,
    tenorMonths: Int,
    dpPercent: Int,
    paymentMode: String,
    fundingSource: String,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val calc = remember(grams, tenorMonths, dpPercent, state.askPricePerGram) {
        calculateInstallment(grams, state.askPricePerGram, tenorMonths, dpPercent)
    }

    val payModeText = if (paymentMode == "auto") {
        val src = if (fundingSource == "kas") {
            "Saldo Kas"
        } else {
            val b = state.bankAccounts.firstOrNull { it.id == fundingSource }
            if (b != null) "${b.bank} ${b.maskedNumber}" else "Saldo Kas"
        }
        "Auto-debit · $src"
    } else {
        "Bayar manual"
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        TopTitleBar(title = "Cicil emas", onBack = onBack)

        // Stepper: Langkah 3 dari 3
        LabeledStepBar(
            current = 3,
            labels = listOf("Simulasi", "Pembayaran", "Konfirmasi"),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = GoldinSpace.Xl, end = GoldinSpace.Xl, top = GoldinSpace.Lg, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xl),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Lg),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                ) {
                    Text(
                        text = "Ringkasan pengajuan",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = Palette.Ink,
                    )

                    SimCalculationRow("Berat emas", formatGrams4(grams), isMono = true)
                    SimCalculationRow("Harga emas (estimasi)", rupiah(calc.goldValue), isMono = true)
                    SimCalculationRow("Uang muka (DP $dpPercent%)", rupiah(calc.downPaymentAmount), isMono = true)
                    SimCalculationRow("Pokok pembiayaan", rupiah(calc.principal), isMono = true)
                    SimCalculationRow("Margin (0,9%/bulan)", rupiah(calc.margin), isMono = true)
                    SimCalculationRow("Tenor", "$tenorMonths bulan", isMono = false)
                    SimCalculationRow("Mitra pembiayaan", CC_PARTNER_NAME, isMono = false)
                    SimCalculationRow("Pembayaran angsuran", payModeText, isMono = false)

                    HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Angsuran per bulan",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "${rupiah(calc.monthlyInstallment)}/bln",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Palette.Primary700,
                        )
                    }
                }
            }
        }

        // Fixed Bottom Bar: [Sebelumnya] · [Ajukan cicilan]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = GoldinBorder.Hairline, color = Palette.Line, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Lg)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                SecondaryButton(
                    text = "Sebelumnya",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Ajukan cicilan",
                    onClick = {
                        try {
                            val quote = env.repo.createInstallmentQuote(
                                grams = grams,
                                tenorMonths = tenorMonths,
                                dpPercent = dpPercent,
                                paymentMode = paymentMode,
                                fundingSource = fundingSource,
                            )
                            onSubmit(quote.id)
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Gagal membuat pengajuan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// =========================================================================================
// SCREEN 43 & 43B: PENGECEKAN SCORING & REJECTED (`ccScore` / `ccRejected`)
// =========================================================================================

@Composable
fun InstallmentScoringScreen(
    env: AppEnv,
    quoteId: String,
    onApproved: (String) -> Unit,
    onUbahSimulasi: () -> Unit,
    onExit: () -> Unit,
    isSimulatedPass: Boolean = true,
) {
    var isChecking by remember { mutableStateOf(true) }
    var isRejected by remember { mutableStateOf(false) }

    LaunchedEffect(quoteId) {
        delay(2200L) // Simulasi loading 2.2 detik sesuai prototipe
        val passed = try {
            env.repo.verifyScoring(quoteId, isSimulatedPass)
        } catch (e: Exception) {
            false
        }
        if (passed) {
            onApproved(quoteId)
        } else {
            isChecking = false
            isRejected = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        if (isChecking) {
            // Screen 43: Loading Scoring Check
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = GoldinSpace.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "spin")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(GoldinMotion.DurationSpin, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "spinnerAngle",
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Palette.Primary50),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PrototypeIcons.Loader2,
                        contentDescription = null,
                        tint = Palette.Primary700,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { rotationZ = angle },
                    )
                }

                Spacer(Modifier.height(GoldinSpace.Xxl))

                Text(
                    text = "Memeriksa kelayakan pengajuan...",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    color = Palette.Ink,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(GoldinSpace.Sm))

                Text(
                    text = "Mitra pembiayaan sedang memproses datamu.",
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (isRejected) {
            // Screen 43b: Pengajuan Ditolak
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = GoldinSpace.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Palette.Critical100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PrototypeIcons.AlertCircle,
                        contentDescription = null,
                        tint = Palette.Critical,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(Modifier.height(GoldinSpace.Xxl))

                Text(
                    text = "Pengajuan belum disetujui",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    color = Palette.Ink,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(GoldinSpace.Sm))

                Text(
                    text = "Maaf, pengajuan cicil emasmu saat ini belum memenuhi kriteria mitra pembiayaan $CC_PARTNER_NAME. Kamu dapat mencoba dengan nominal atau tenor lain.",
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            // Bottom Actions: [Kembali] · [Ubah simulasi]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Xl),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                ) {
                    SecondaryButton(
                        text = "Kembali",
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = "Ubah simulasi",
                        onClick = onUbahSimulasi,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// =========================================================================================
// SCREEN 44: AKAD PEMBIAYAAN & BAYAR DP (`44 Cicil emas · Akad & DP` / `cicil/akad`)
// =========================================================================================

@Composable
fun InstallmentAkadScreen(
    env: AppEnv,
    quoteId: String,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val quote = env.repo.getInstallmentQuote(quoteId)
    val context = LocalContext.current

    var selectedPm by rememberSaveable { mutableStateOf<PaymentMethod>(PaymentMethod.WALLET) }
    var isAgreed by rememberSaveable { mutableStateOf(false) }

    var showEsignOtp by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showExitModal by remember { mutableStateOf(false) }

    // 15-Minute Countdown Timer
    var secondsLeft by rememberSaveable { mutableIntStateOf(quote?.secondsRemaining(System.currentTimeMillis()) ?: (CC_LOCK_WINDOW_MIN * 60)) }
    LaunchedEffect(quoteId) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    val timerMinutes = secondsLeft / 60
    val timerSeconds = secondsLeft % 60
    val timerFormatted = String.format(Locale.getDefault(), "%02d:%02d", timerMinutes, timerSeconds)

    BackHandler { showExitModal = true }

    if (quote == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Paper)
                .statusBarsPadding(),
        ) {
            TopTitleBar(title = "Akad pembiayaan", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pengajuan tidak ditemukan atau sudah kedaluwarsa.", color = Palette.InkSecondary)
            }
        }
        return
    }

    val calc = quote.calculation
    val dueDay = parseDateComponents(System.currentTimeMillis()).day.coerceIn(1, 28)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        TopTitleBar(title = "Akad pembiayaan", onBack = { showExitModal = true })

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = GoldinSpace.Xl, end = GoldinSpace.Xl, top = GoldinSpace.Lg, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xl),
        ) {
            // Price Lock 15-Minute Info Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Palette.Info50)
                        .border(1.dp, Palette.Info100, RoundedCornerShape(GoldinRadius.Lg))
                        .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = PrototypeIcons.Clock,
                        contentDescription = null,
                        tint = Palette.Info700,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Harga emas dikunci. Selesaikan akad dalam $timerFormatted.",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Palette.Info700,
                    )
                }
            }

            // Kartu Detail Akad Pembiayaan
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Lg),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                ) {
                    Text(
                        text = "Detail akad pembiayaan",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Palette.Ink,
                    )

                    SimCalculationRow("Mitra pembiayaan", CC_PARTNER_NAME, isMono = false)
                    SimCalculationRow("Berat emas", formatGrams4(quote.grams), isMono = true)
                    SimCalculationRow("Harga emas", "${rupiah(quote.pricePerGram)}/gr", isMono = true)
                    SimCalculationRow("Total pembiayaan", rupiah(calc.totalFinancing), isMono = true)
                    SimCalculationRow("Angsuran per bulan", "${rupiah(calc.monthlyInstallment)} (${calc.tenorMonths}x)", isMono = true)
                    SimCalculationRow("Jatuh tempo", "Setiap tanggal $dueDay", isMono = false)
                }
            }

            // Kartu Pembayaran Uang Muka (Bayar Sekarang)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Lg),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                ) {
                    Text(
                        text = "Bayar sekarang",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Palette.Ink,
                    )

                    SimCalculationRow("Uang muka (DP ${calc.downPaymentPercent}%)", formatNumberOnly(calc.downPaymentAmount), isMono = true)
                    SimCalculationRow("Biaya administrasi", formatNumberOnly(calc.adminFee), isMono = true)

                    HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Total bayar DP",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = rupiah(calc.totalDownPayment),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Palette.Primary700,
                        )
                    }
                }
            }

            // Pilihan Metode Pembayaran DP
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Metode pembayaran DP",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "*",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Critical,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                    ) {
                        // Saldo Kas
                        DpPaymentRadioRow(
                            title = "Saldo Kas",
                            subtitle = "Saldo ${rupiah(state.walletBalance)}",
                            isSelected = selectedPm == PaymentMethod.WALLET,
                            onClick = { selectedPm = PaymentMethod.WALLET },
                        )
                        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                        // Virtual Account BCA
                        DpPaymentRadioRow(
                            title = "BCA Virtual Account",
                            subtitle = "Konfirmasi otomatis",
                            isSelected = selectedPm == PaymentMethod.VA_BCA,
                            onClick = { selectedPm = PaymentMethod.VA_BCA },
                        )
                        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                        // Virtual Account Mandiri
                        DpPaymentRadioRow(
                            title = "Mandiri Virtual Account",
                            subtitle = "Konfirmasi otomatis",
                            isSelected = selectedPm == PaymentMethod.VA_MANDIRI,
                            onClick = { selectedPm = PaymentMethod.VA_MANDIRI },
                        )
                        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                        // QRIS
                        DpPaymentRadioRow(
                            title = "QRIS",
                            subtitle = "Gopay, OVO, Dana, ShopeePay",
                            isSelected = selectedPm == PaymentMethod.QRIS,
                            onClick = { selectedPm = PaymentMethod.QRIS },
                        )
                    }
                }
            }

            // Checkbox Persetujuan Akad
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAgreed = !isAgreed }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isAgreed,
                        onCheckedChange = { isAgreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Palette.Primary700,
                            checkmarkColor = Color.White,
                            uncheckedColor = Palette.Neutral300,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Saya menyetujui seluruh klausul akad pembiayaan syariah ini.",
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Palette.Ink,
                    )
                }
            }
        }

        // Fixed Bottom Bar
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = GoldinBorder.Hairline, color = Palette.Line, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Lg)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                text = "Tanda tangani & bayar",
                enabled = isAgreed && secondsLeft > 0,
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    showEsignOtp = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Modal Konfirmasi Keluar Akad
    if (showExitModal) {
        GoldinModal(
            title = "Keluar dari cicil emas?",
            message = "Pengikatan harga emas akan dibatalkan.",
            confirmLabel = "Keluar",
            isDestructive = true,
            onConfirm = {
                showExitModal = false
                onBack()
            },
            cancelLabel = "Lanjut akad",
            onCancel = { showExitModal = false },
        )
    }

    // Dialog e-Sign SMS OTP (6 digit, demo code "123456")
    if (showEsignOtp) {
        InstallmentEsignOtpDialog(
            phone = state.user?.phone ?: "081234567890",
            onDismiss = { showEsignOtp = false },
            onVerified = {
                showEsignOtp = false
                showPinDialog = true
            },
        )
    }

    // Dialog PIN Transaksi 6 Digit
    if (showPinDialog) {
        PinDialog(
            env = env,
            title = "Konfirmasi PIN",
            onDismiss = { showPinDialog = false },
            onVerified = {
                showPinDialog = false
                try {
                    val contract = env.repo.signAndPayInstallmentAkad(
                        quoteId = quoteId,
                        smsOtp = "123456",
                        paymentMethod = selectedPm,
                        pin = state.pin ?: "135790",
                    )
                    Toast.makeText(context, "Cicilan emas aktif", Toast.LENGTH_SHORT).show()
                    onSuccess(contract.id)
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "Gagal memproses pembayaran DP", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

@Composable
private fun InstallmentEsignOtpDialog(
    phone: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    var otpCode by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableIntStateOf(30) }
    var hasError by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000L)
            timerSeconds -= 1
        }
    }

    fun handleDigit(d: Char) {
        if (otpCode.length < 6) {
            hasError = false
            val newCode = otpCode + d
            otpCode = newCode
            if (newCode.length == 6) {
                if (newCode == "123456") {
                    onVerified()
                } else {
                    hasError = true
                }
            }
        }
    }

    fun handleDelete() {
        if (otpCode.isNotEmpty()) {
            hasError = false
            otpCode = otpCode.dropLast(1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x660F052C))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                    .background(Color.White)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .navigationBarsPadding()
                    .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Md, bottom = GoldinSpace.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Handle bar (36x4dp neutral-300 sesuai prototipe & Design System v2)
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(Palette.Neutral300),
                )

                // Icon SMS
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Palette.Primary50),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = GoldinIcons.SmsMessage,
                        contentDescription = null,
                        tint = Palette.Primary700,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Text(
                    text = "Tanda Tangan Elektronik",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Palette.Ink,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Masukkan kode OTP 6 digit yang dikirim ke nomor $phone untuk e-sign akad.",
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Palette.InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                // 6 Kotak Digit OTP menggunakan CodeBoxes standar aplikasi (persis seperti PinDialog namun unmasked untuk OTP)
                Box(
                    modifier = Modifier.padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CodeBoxes(
                        code = otpCode,
                        total = 6,
                        masked = false,
                        error = hasError,
                    )
                }

                if (hasError) {
                    Text(
                        text = "Kode OTP salah. Masukkan '123456' untuk demo.",
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        color = Palette.Critical,
                        textAlign = TextAlign.Center,
                    )
                }

                // Kirim ulang timer
                Text(
                    text = if (timerSeconds > 0) "Kirim ulang dalam ${timerSeconds}d" else "Kirim ulang kode",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (timerSeconds > 0) Palette.InkSoft else Palette.Primary700,
                    modifier = Modifier
                        .clickable(enabled = timerSeconds == 0) { timerSeconds = 30 }
                        .padding(vertical = 2.dp),
                )

                // In-App Keypad Numerik 3x4 (1-9, 0, Backspace) - Persis sama seperti PinDialog
                Box(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    NumberPad(
                        onDigit = { digit -> handleDigit(digit) },
                        onDelete = { handleDelete() },
                    )
                }

                SecondaryButton(
                    text = "Batal",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                )
            }
        }
    }
}

// =========================================================================================
// SCREEN 45: SUKSES CICILAN AKTIF (`45 Cicil emas · Aktif` / `cicil/success`)
// =========================================================================================

@Composable
fun InstallmentSuccessScreen(
    env: AppEnv,
    contractId: String,
    onSeeContract: (String) -> Unit,
) {
    val contract = env.repo.getInstallmentContract(contractId)
    val gramsText = if (contract != null) formatGrams4(contract.goldGrams) else "1,0000 gr"

    // Pulse animation
    val pulseTransition = rememberInfiniteTransition(label = "pulseRing")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(GoldinMotion.DurationPulseRing, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringScale",
    )
    val ringAlpha by pulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(GoldinMotion.DurationPulseRing, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = GoldinSpace.Xl, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Hero 88dp Circle with Pulsing Gold Rings
            Box(
                modifier = Modifier.size(88.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Outer Pulsing Ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                            alpha = ringAlpha
                        }
                        .clip(CircleShape)
                        .border(2.dp, Palette.Gold, CircleShape),
                )
                // Inner 76dp Circle
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Palette.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PrototypeIcons.Check,
                        contentDescription = null,
                        tint = Palette.Gold,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Spacer(Modifier.height(GoldinSpace.Xxl))

            // Title
            Text(
                text = "Cicilan emas aktif",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                color = Palette.Ink,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(GoldinSpace.Sm))

            // Subtitle
            Text(
                text = "$gramsText emas terkunci di Tabungan Emas sampai cicilan lunas.",
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.InkSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // Bottom CTA Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Xl),
        ) {
            PrimaryButton(
                text = "Lihat cicilan",
                onClick = { onSeeContract(contractId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// =========================================================================================
// HELPER COMPOSABLES
// =========================================================================================

@Composable
private fun SimCalculationRow(
    label: String,
    value: String,
    isMono: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 14.sp,
            color = Palette.InkSecondary,
        )
        Text(
            text = value,
            fontFamily = if (isMono) MonoFontFamily else BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Palette.Ink,
        )
    }
}

@Composable
private fun MethodRadioRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) Palette.Primary50 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = GoldinSpace.Sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Palette.Ink,
            )
            Text(
                text = subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = Palette.InkSecondary,
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 6.dp else 1.5.dp,
                    color = if (isSelected) Palette.Primary700 else Palette.Neutral300,
                    shape = CircleShape,
                )
                .background(Color.White),
        )
    }
}

@Composable
private fun BankAutoDebitRadioRow(
    bank: BankAccount,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) Palette.Primary50 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoldinBankLogoBadge(bankName = bank.bank)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = GoldinSpace.Sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "${bank.bank} ${bank.maskedNumber}",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Palette.Ink,
            )
            Text(
                text = if (bank.isAutoDebitActive) "Auto-debit aktif" else "Aktifkan auto-debit",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (bank.isAutoDebitActive) Palette.Good700 else Palette.Primary700,
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 6.dp else 1.5.dp,
                    color = if (isSelected) Palette.Primary700 else Palette.Neutral300,
                    shape = CircleShape,
                )
                .background(Color.White),
        )
    }
}

@Composable
private fun DpPaymentRadioRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) Palette.Primary50 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = GoldinSpace.Sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Palette.Ink,
            )
            Text(
                text = subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = Palette.InkSecondary,
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 6.dp else 1.5.dp,
                    color = if (isSelected) Palette.Primary700 else Palette.Neutral300,
                    shape = CircleShape,
                )
                .background(Color.White),
        )
    }
}
