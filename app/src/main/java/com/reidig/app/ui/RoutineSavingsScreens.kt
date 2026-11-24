package com.reidig.app.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.BankAccount
import com.reidig.shared.NotificationCategory
import com.reidig.shared.PaymentMethod
import com.reidig.shared.RoutineMutation
import com.reidig.shared.RoutinePlanStatus
import com.reidig.shared.RoutinePlanType
import com.reidig.shared.RoutineSavingsPlan
import com.reidig.shared.calculateNextDebitDate
import com.reidig.shared.formatGrams4
import com.reidig.shared.formatNumberOnly
import com.reidig.shared.rupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// =========================================================================================
// SCREEN 27: DAFTAR TABUNGAN RUTIN (`27 Tabungan rutin` / `plans`)
// =========================================================================================

@Composable
fun RoutinePlanListScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onOpenPlanDetail: (String) -> Unit,
    onCreatePlan: () -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val plans = state.routinePlans

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // Top App Bar
        TopTitleBar(title = "Tabungan rutin", onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (plans.isEmpty()) {
                // Empty state card (Handoff §10 & Prototipe L2200)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Xxl),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(horizontal = GoldinSpace.Lg, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Palette.Primary50),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.Repeat,
                                    contentDescription = null,
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                text = "Belum ada tabungan rutin.",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Palette.InkSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(4.dp))
                            // Outline Button
                            val buttonInteraction = remember { MutableInteractionSource() }
                            val isHovered by buttonInteraction.collectIsHoveredAsState()
                            val isPressed by buttonInteraction.collectIsPressedAsState()
                            val buttonBg by animateColorAsState(
                                targetValue = when {
                                    isPressed -> Palette.Primary100
                                    isHovered -> Palette.Primary50
                                    else -> Color.Transparent
                                },
                                animationSpec = tween(GoldinMotion.DurationHover),
                                label = "btnBg",
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(GoldinControl.SizeSm)
                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                    .background(buttonBg)
                                    .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                                    .clickable(
                                        interactionSource = buttonInteraction,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = onCreatePlan,
                                    ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.Plus,
                                    contentDescription = null,
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Buat tabungan rutin",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Palette.Primary700,
                                )
                            }
                        }
                    }
                }
            } else {
                // List of plan cards
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = GoldinSpace.Xl,
                        end = GoldinSpace.Xl,
                        top = GoldinSpace.Lg,
                        bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                ) {
                    items(plans, key = { it.id }) { plan ->
                        RoutinePlanItemCard(
                            plan = plan,
                            onClick = { onOpenPlanDetail(plan.id) },
                        )
                    }
                }

                // Bottom CTA Button bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(
                            width = GoldinBorder.Hairline,
                            color = Palette.Line,
                            shape = RoundedCornerShape(0.dp),
                        )
                        .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Lg)
                        .navigationBarsPadding(),
                ) {
                    PrimaryButton(
                        text = "Buat tabungan rutin",
                        onClick = onCreatePlan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutinePlanItemCard(
    plan: RoutineSavingsPlan,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Neutral200
            isHovered -> Palette.Neutral100
            else -> Color.White
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "cardBg",
    )

    val isPaused = plan.status == RoutinePlanStatus.PAUSED
    val hasTarget = plan.hasTarget
    val pctFraction = (plan.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val pctLabel = "${plan.progressPercent.coerceIn(0.0, 100.0).roundToInt()}%"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Lg))
            .background(bg)
            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = GoldinSpace.Sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Title + status pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
            ) {
                Text(
                    text = plan.name,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .clip(RoundedCornerShape(GoldinRadius.Full))
                            .background(Palette.Neutral200)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Dijeda",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            lineHeight = 20.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // Subtitle: Rp... · Setiap tanggal X
            Text(
                text = "${rupiah(plan.monthlyAmount)} · Setiap tanggal ${plan.debitDate}",
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                color = Palette.InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Progress bar if target exists
            if (hasTarget) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(GoldinRadius.Full))
                            .background(Palette.Neutral200),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pctFraction)
                                .height(4.dp)
                                .clip(RoundedCornerShape(GoldinRadius.Full))
                                .background(Palette.Gold500),
                        )
                    }
                    Text(
                        text = pctLabel,
                        fontFamily = MonoFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Palette.InkSecondary,
                    )
                }
            }
        }

        Icon(
            imageVector = PrototypeIcons.ChevronRight,
            contentDescription = null,
            tint = Palette.InkSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

// =========================================================================================
// SCREEN 27B: BUAT TABUNGAN RUTIN (`27b Buat tabungan rutin` / `plan-create`)
// =========================================================================================

@Composable
fun RoutinePlanFormScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onPlanCreated: (String) -> Unit,
    onAddBank: () -> Unit,
    prefillDraft: RoutinePlanPrefillDraft? = null,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Consuming prefill from Beranda promo card if available
    var hasConsumedPrefill by rememberSaveable { mutableStateOf(false) }
    var initialPrefillType: RoutinePlanType? = null
    var initialPrefillAmt = ""
    var initialPrefillTarget = ""

    if (!hasConsumedPrefill && prefillDraft != null && prefillDraft.hasPending) {
        val consumed = prefillDraft.consume()
        if (consumed != null) {
            val (amt, target) = consumed
            if (amt > 0L) initialPrefillAmt = amt.toString()
            if (target != null && target > 0.0) {
                initialPrefillTarget = formatGrams4(target).replace(" gr", "").trim()
                initialPrefillType = RoutinePlanType.TARGET_EMAS
            }
            hasConsumedPrefill = true
        }
    }

    var planName by rememberSaveable { mutableStateOf("") }
    var planType by rememberSaveable { mutableStateOf<RoutinePlanType?>(initialPrefillType ?: RoutinePlanType.PERIODE) }
    var targetMonths by rememberSaveable { mutableStateOf("12") }
    var targetGrams by rememberSaveable { mutableStateOf(initialPrefillTarget) }
    var monthlyAmountRaw by rememberSaveable { mutableStateOf(initialPrefillAmt) }
    var debitDate by rememberSaveable { mutableStateOf<Int?>(5) }
    var selectedSource by rememberSaveable { mutableStateOf("kas") }

    var showDateSheet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingAutoDebitBank by remember { mutableStateOf<BankAccount?>(null) }
    var pendingOtpBank by remember { mutableStateOf<BankAccount?>(null) }

    // Validation
    val amountNum = monthlyAmountRaw.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val isAmountValid = amountNum >= 10_000L
    val isAmountError = monthlyAmountRaw.isNotEmpty() && !isAmountValid
    val isMonthsValid = planType == RoutinePlanType.PERIODE && (targetMonths.toIntOrNull() ?: 0) in 1..36
    val isTargetValid = planType == RoutinePlanType.TARGET_EMAS && (targetGrams.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
    val isFormValid = planName.isNotBlank() &&
        planType != null &&
        (isMonthsValid || isTargetValid) &&
        isAmountValid &&
        debitDate != null &&
        selectedSource.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        TopTitleBar(title = "Tabungan rutin", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = GoldinSpace.Xl, end = GoldinSpace.Xl, top = GoldinSpace.Lg, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xl),
        ) {
            // 1. Nama tabungan*
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    FieldLabel(label = "Nama tabungan", isRequired = true)
                    GoldinTextInput(
                        value = planName,
                        onValueChange = { planName = it.take(30) },
                        placeholder = "e.g. Dana darurat",
                    )
                }
            }

            // 2. Jenis tabungan*
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    FieldLabel(label = "Jenis tabungan", isRequired = true)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                    ) {
                        RoutineTypeRadioRow(
                            title = "Periode",
                            subtitle = "Nabung selama jangka waktu tertentu",
                            isSelected = planType == RoutinePlanType.PERIODE,
                            onClick = { planType = RoutinePlanType.PERIODE },
                        )
                        HorizontalDivider(
                            thickness = GoldinBorder.Hairline,
                            color = Palette.Line,
                        )
                        RoutineTypeRadioRow(
                            title = "Target emas",
                            subtitle = "Nabung sampai target gram tercapai",
                            isSelected = planType == RoutinePlanType.TARGET_EMAS,
                            onClick = { planType = RoutinePlanType.TARGET_EMAS },
                        )
                    }
                }
            }

            // 3. Jangka waktu* ATAU Target emas* (Conditional)
            item {
                if (planType == RoutinePlanType.PERIODE) {
                    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                        FieldLabel(label = "Jangka waktu", isRequired = true)
                        GoldinTextInput(
                            value = targetMonths,
                            onValueChange = { targetMonths = it.filter { ch -> ch.isDigit() }.take(3) },
                            placeholder = "12",
                            suffix = "bulan",
                            keyboardType = KeyboardType.Number,
                        )
                        Text(
                            text = "Tabungan berhenti otomatis setelah jangka waktu selesai.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            letterSpacing = 0.5.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                } else if (planType == RoutinePlanType.TARGET_EMAS) {
                    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                        FieldLabel(label = "Target emas", isRequired = true)
                        GoldinTextInput(
                            value = targetGrams,
                            onValueChange = { input ->
                                val clean = input.replace('.', ',').filter { it.isDigit() || it == ',' }
                                val parts = clean.split(',')
                                targetGrams = if (parts.size > 1) {
                                    "${parts[0].take(4)},${parts.drop(1).joinToString("").take(4)}"
                                } else {
                                    clean.take(4)
                                }
                            },
                            placeholder = "1",
                            suffix = "gr",
                            keyboardType = KeyboardType.Decimal,
                        )
                        Text(
                            text = "Tabungan berhenti otomatis saat target tercapai.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            letterSpacing = 0.5.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // 4. Nominal per bulan*
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    FieldLabel(label = "Nominal per bulan", isRequired = true)
                    GoldinTextInput(
                        value = if (monthlyAmountRaw.isNotEmpty()) formatNumberOnly(amountNum) else "",
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(10)
                            monthlyAmountRaw = digits
                        },
                        placeholder = "0",
                        prefix = "Rp",
                        isError = isAmountError,
                        keyboardType = KeyboardType.Number,
                    )
                    if (isAmountError) {
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
                                text = "Minimal nominal per bulan Rp10.000",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                letterSpacing = 0.5.sp,
                                color = Palette.Critical,
                            )
                        }
                    } else {
                        Text(
                            text = "Minimal nominal per bulan Rp10.000",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            letterSpacing = 0.5.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // 5. Tanggal debit*
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    FieldLabel(label = "Tanggal debit", isRequired = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(GoldinControl.SizeMd)
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                            .clickable { showDateSheet = true }
                            .padding(horizontal = GoldinSpace.Lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (debitDate != null) "Tanggal $debitDate" else "Pilih tanggal",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            color = if (debitDate != null) Palette.Ink else Palette.InkSoft,
                        )
                        Icon(
                            imageVector = PrototypeIcons.Calendar,
                            contentDescription = null,
                            tint = Palette.InkSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // 6. Sumber dana* (Single card + "+ Tambah rekening" link)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    FieldLabel(label = "Sumber dana", isRequired = true)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                    ) {
                        // Opsi 1: Saldo Kas
                        RoutineFundingRadioRow(
                            title = "Saldo Kas",
                            subtitle = "Saldo ${rupiah(state.walletBalance)}",
                            isSelected = selectedSource == "kas",
                            onClick = { selectedSource = "kas" },
                        )

                        // Opsi 2..N: Bank accounts
                        state.bankAccounts.forEach { bank ->
                            HorizontalDivider(
                                thickness = GoldinBorder.Hairline,
                                color = Palette.Line,
                            )
                            RoutineBankFundingRadioRow(
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

                    // "+ Tambah rekening" button
                    val addBankInteraction = remember { MutableInteractionSource() }
                    val isAddHovered by addBankInteraction.collectIsHoveredAsState()
                    val isAddPressed by addBankInteraction.collectIsPressedAsState()
                    val addBg by animateColorAsState(
                        targetValue = when {
                            isAddPressed -> Palette.Primary100
                            isAddHovered -> Palette.Primary50
                            else -> Color.Transparent
                        },
                        animationSpec = tween(GoldinMotion.DurationHover),
                        label = "addBankBg",
                    )
                    Row(
                        modifier = Modifier
                            .height(GoldinControl.TouchMin)
                            .clip(RoundedCornerShape(GoldinRadius.Full))
                            .background(addBg)
                            .clickable(
                                interactionSource = addBankInteraction,
                                indication = null,
                                role = Role.Button,
                                onClick = onAddBank,
                            )
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

        // Fixed Bottom Action Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(
                    width = GoldinBorder.Hairline,
                    color = Palette.Line,
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Lg)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                text = "Aktifkan tabungan",
                enabled = isFormValid,
                onClick = { showPinDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Sheet Picker Tanggal Debit 1..28
    if (showDateSheet) {
        TanggalDebitSheet(
            currentDate = debitDate,
            onSelectDate = {
                debitDate = it
                showDateSheet = false
            },
            onDismiss = { showDateSheet = false },
        )
    }

    // Modal Konfirmasi Aktivasi Auto-Debit Bank
    pendingAutoDebitBank?.let { bank ->
        AutoDebitModal(
            bank = bank,
            onConfirm = {
                val targetBank = bank
                pendingAutoDebitBank = null
                pendingOtpBank = targetBank
            },
            onDismiss = { pendingAutoDebitBank = null },
        )
    }

    // Dialog Input OTP Aktivasi Auto-Debit Bank
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

    // Dialog PIN Transaksi 6 Digit
    if (showPinDialog) {
        PinDialog(
            env = env,
            title = "Konfirmasi PIN",
            onDismiss = { showPinDialog = false },
            onVerified = {
                showPinDialog = false
                val monthsInt = targetMonths.toIntOrNull() ?: 12
                val targetGramsDouble = targetGrams.replace(',', '.').toDoubleOrNull() ?: 1.0
                try {
                    val created = env.repo.createRoutinePlan(
                        name = planName,
                        type = planType ?: RoutinePlanType.PERIODE,
                        targetMonths = monthsInt,
                        targetGrams = targetGramsDouble,
                        monthlyAmount = amountNum,
                        debitDate = debitDate ?: 5,
                        source = selectedSource,
                    )
                    Toast.makeText(context, "Tabungan rutin aktif", Toast.LENGTH_SHORT).show()
                    onPlanCreated(created.id)
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "Gagal membuat tabungan", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

@Composable
private fun RoutineTypeRadioRow(
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
                lineHeight = 20.sp,
                color = Palette.Ink,
            )
            Text(
                text = subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                color = Palette.InkSecondary,
            )
        }
        GoldinRadioButton(isSelected = isSelected, onClick = onClick)
    }
}

@Composable
private fun RoutineFundingRadioRow(
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
                lineHeight = 20.sp,
                color = Palette.Ink,
            )
            Text(
                text = subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Palette.InkSecondary,
            )
        }
        GoldinRadioButton(isSelected = isSelected, onClick = onClick)
    }
}

@Composable
private fun RoutineBankFundingRadioRow(
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
                lineHeight = 20.sp,
                color = Palette.Ink,
            )
            Text(
                text = if (bank.isAutoDebitActive) "Auto-debit aktif" else "Aktifkan auto-debit",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = if (bank.isAutoDebitActive) Palette.Good700 else Palette.Primary700,
            )
        }
        GoldinRadioButton(isSelected = isSelected, onClick = onClick)
    }
}

@Composable
private fun GoldinRadioButton(
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 6.dp else 1.5.dp,
                color = if (isSelected) Palette.Primary700 else Palette.Neutral300,
                shape = CircleShape,
            )
            .background(Color.White)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun FieldLabel(
    label: String,
    isRequired: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
        )
        if (isRequired) {
            Text(
                text = "*",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.Critical,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
private fun GoldinTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String? = null,
    suffix: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val borderColor = if (isError) Palette.Critical else Palette.Line

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(GoldinControl.SizeMd)
            .clip(RoundedCornerShape(GoldinRadius.Lg))
            .background(Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(GoldinRadius.Lg))
            .padding(horizontal = GoldinSpace.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (prefix != null) {
            Text(
                text = prefix,
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                color = Palette.Ink,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = Palette.InkSoft,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = Palette.Ink,
                ),
                singleLine = true,
                cursorBrush = SolidColor(Palette.Primary700),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (suffix != null) {
            Text(
                text = suffix,
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                color = Palette.InkSecondary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun TanggalDebitSheet(
    currentDate: Int?,
    onSelectDate: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    GoldinBottomSheet(
        title = "Tanggal debit",
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GoldinSpace.Xxl),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg),
        ) {
            Text(
                text = "Pilih tanggal debit otomatis setiap bulan (1–28):",
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                color = Palette.InkSecondary,
            )

            // Grid of 28 dates (7 columns x 4 rows)
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items((1..28).toList()) { d ->
                    val isSelected = currentDate == d
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) Palette.Primary50 else Color.White,
                        animationSpec = tween(GoldinMotion.DurationHover),
                        label = "chipBg",
                    )
                    val chipBorder = if (isSelected) Palette.Primary700 else Palette.Line
                    val textColor = if (isSelected) Palette.Primary700 else Palette.Ink

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectDate(d) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$d",
                            fontFamily = MonoFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================================
// SCREEN 27C: SUKSES TABUNGAN RUTIN (`27c Sukses tabungan rutin` / `plan-done/{planId}`)
// =========================================================================================

@Composable
fun RoutinePlanDoneScreen(
    env: AppEnv,
    planId: String,
    onSeePlan: (String) -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val plan = state.routinePlans.firstOrNull { it.id == planId }

    // First debit date calculation
    val firstDebitMs = if (plan != null) {
        calculateNextDebitDate(plan.debitDate, plan.createdAtMs)
    } else {
        System.currentTimeMillis() + 15L * 86_400_000L
    }
    val firstDebitFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))
        .format(Date(firstDebitMs))

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
                text = "Tabungan rutin aktif",
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
                text = "Debit pertama $firstDebitFormatted.",
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
                text = "Lihat tabungan",
                onClick = { onSeePlan(planId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// =========================================================================================
// SCREEN 27D: DETAIL TABUNGAN RUTIN (`27d Detail tabungan rutin` / `plan-detail/{planId}`)
// =========================================================================================

@Composable
fun RoutinePlanDetailScreen(
    env: AppEnv,
    planId: String,
    onBack: () -> Unit,
    onOpenReceipt: (String) -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val plan = state.routinePlans.firstOrNull { it.id == planId }
    val context = LocalContext.current

    var showInfoSheet by remember { mutableStateOf(false) }
    var showPauseModal by remember { mutableStateOf(false) }
    var showStopModal by remember { mutableStateOf(false) }

    if (plan == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Paper)
                .statusBarsPadding(),
        ) {
            TopTitleBar(title = "Detail tabungan", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tabungan tidak ditemukan", color = Palette.InkSecondary)
            }
        }
        return
    }

    val isPaused = plan.status == RoutinePlanStatus.PAUSED
    val pctFraction = (plan.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val pctLabel = "${plan.progressPercent.coerceIn(0.0, 100.0).roundToInt()}%"
    val targetText = if (plan.type == RoutinePlanType.PERIODE) {
        "${plan.mutations.size} dari ${plan.targetMonths} bulan"
    } else {
        "$pctLabel dari target ${formatGrams4(plan.targetGrams)}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // Top App Bar with info button "i"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GoldinControl.SizeMd)
                .padding(horizontal = GoldinSpace.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(GoldinControl.TouchMin)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    tint = Palette.Ink,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = plan.name,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Palette.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            Box(
                modifier = Modifier
                    .size(GoldinControl.TouchMin)
                    .clip(CircleShape)
                    .clickable { showInfoSheet = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PrototypeIcons.InfoCircle,
                    contentDescription = "Info tabungan",
                    tint = Palette.Ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = GoldinSpace.Xl,
                end = GoldinSpace.Xl,
                top = GoldinSpace.Lg,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xl),
        ) {
            // 1. Kartu Terkumpul Soft Purple (Handoff §10 & Prototipe L2280)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Palette.Primary50)
                        .border(1.dp, Palette.Primary100, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Lg),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Terkumpul",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Palette.InkSecondary,
                        )
                        if (isPaused) {
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                    .background(Palette.Neutral200)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Dijeda",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    lineHeight = 20.sp,
                                    color = Palette.InkSecondary,
                                )
                            }
                        }
                    }

                    // Big Grams Display
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatGrams4(plan.savedGrams).replace(" gr", ""),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            color = Palette.Ink,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "gr",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Palette.Ink,
                        )
                    }

                    // Progress bar
                    if (plan.hasTarget) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                    .background(Palette.Primary100),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pctFraction)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(GoldinRadius.Full))
                                        .background(Palette.Gold500),
                                )
                            }
                            Text(
                                text = targetText,
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                letterSpacing = 0.5.sp,
                                color = Palette.InkSecondary,
                            )
                        }
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(
                    thickness = GoldinBorder.Hairline,
                    color = Palette.Line,
                )
            }

            // 2. Seksi Mutasi
            item {
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
                    Text(
                        text = "Mutasi",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = Palette.Ink,
                    )
                }
            }

            if (plan.mutations.isNotEmpty()) {
                itemsIndexed(plan.mutations.reversed()) { index, item ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (index > 0) {
                            HorizontalDivider(
                                thickness = GoldinBorder.Hairline,
                                color = Palette.Line,
                            )
                        }
                        RoutineMutationRow(
                            mutation = item,
                            onClick = {
                                val tx = item.txId
                                if (!tx.isNullOrBlank()) {
                                    onOpenReceipt(tx)
                                }
                            },
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Palette.Primary50),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.Receipt,
                                contentDescription = null,
                                tint = Palette.Primary700,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Text(
                            text = "Belum ada debit",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Palette.Ink,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        // Fixed Bottom Action Bar: [Hentikan] · [Jeda / Lanjutkan]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(
                    width = GoldinBorder.Hairline,
                    color = Palette.Line,
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Lg)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                // Button Hentikan (Destructive Outline)
                val stopInteraction = remember { MutableInteractionSource() }
                val isStopHovered by stopInteraction.collectIsHoveredAsState()
                val isStopPressed by stopInteraction.collectIsPressedAsState()
                val stopBg by animateColorAsState(
                    targetValue = when {
                        isStopPressed -> Palette.Primary100
                        isStopHovered -> Palette.Primary50
                        else -> Color.Transparent
                    },
                    animationSpec = tween(GoldinMotion.DurationHover),
                    label = "stopBg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(GoldinControl.SizeMd)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(stopBg)
                        .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                        .clickable(
                            interactionSource = stopInteraction,
                            indication = null,
                            role = Role.Button,
                            onClick = { showStopModal = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Hentikan",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Primary700,
                    )
                }

                // Button Jeda / Lanjutkan
                PrimaryButton(
                    text = if (isPaused) "Lanjutkan" else "Jeda",
                    onClick = {
                        if (isPaused) {
                            env.repo.resumeRoutinePlan(plan.id)
                            Toast.makeText(context, "Tabungan rutin dilanjutkan", Toast.LENGTH_SHORT).show()
                        } else {
                            showPauseModal = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Modal Konfirmasi Jeda
    if (showPauseModal) {
        GoldinModal(
            title = "Jeda tabungan rutin?",
            message = "Debit otomatis berhenti sementara. Kamu bisa melanjutkannya kapan saja.",
            confirmLabel = "Jeda",
            onConfirm = {
                showPauseModal = false
                env.repo.pauseRoutinePlan(plan.id)
                Toast.makeText(context, "Tabungan rutin dijeda", Toast.LENGTH_SHORT).show()
            },
            cancelLabel = "Batal",
            onCancel = { showPauseModal = false },
        )
    }

    // Modal Konfirmasi Hentikan
    if (showStopModal) {
        GoldinModal(
            title = "Hentikan tabungan rutin?",
            message = "Debit otomatis akan berhenti. Emas yang sudah terkumpul tetap ada di Tabungan Emas.",
            confirmLabel = "Hentikan",
            isDestructive = true,
            onConfirm = {
                showStopModal = false
                env.repo.stopRoutinePlan(plan.id)
                Toast.makeText(context, "Tabungan rutin dihentikan", Toast.LENGTH_SHORT).show()
                onBack()
            },
            cancelLabel = "Batal",
            onCancel = { showStopModal = false },
        )
    }

    // Drawer Info Tabungan
    if (showInfoSheet) {
        val nextDebitFormatted = if (isPaused) {
            "Dijeda"
        } else {
            val nextMs = calculateNextDebitDate(plan.debitDate, System.currentTimeMillis())
            SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(nextMs))
        }

        val fundingSourceLabel = if (plan.fundingSource == "kas") {
            "Saldo Kas"
        } else {
            val b = state.bankAccounts.firstOrNull { it.id == plan.fundingSource }
            if (b != null) "${b.bank} ${b.maskedNumber}" else "Saldo Kas"
        }

        GoldinBottomSheet(
            title = "Info tabungan",
            onDismiss = { showInfoSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = GoldinSpace.Xxl),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                InfoDrawerRow(
                    label = "Jenis",
                    value = plan.type.label,
                )
                if (plan.type == RoutinePlanType.PERIODE) {
                    InfoDrawerRow(
                        label = "Jangka waktu",
                        value = "${plan.targetMonths} bulan",
                    )
                } else {
                    InfoDrawerRow(
                        label = "Target emas",
                        value = "${formatGrams4(plan.targetGrams)}",
                        isMono = true,
                    )
                }
                InfoDrawerRow(
                    label = "Nominal per bulan",
                    value = rupiah(plan.monthlyAmount),
                    isMono = true,
                )
                InfoDrawerRow(
                    label = "Tanggal debit",
                    value = "Setiap tanggal ${plan.debitDate}",
                )
                InfoDrawerRow(
                    label = "Debit berikutnya",
                    value = nextDebitFormatted,
                )
                InfoDrawerRow(
                    label = "Sumber dana",
                    value = fundingSourceLabel,
                    isMono = plan.fundingSource != "kas",
                )
            }
        }
    }
}

@Composable
private fun RoutineMutationRow(
    mutation: RoutineMutation,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Neutral200
            isHovered -> Palette.Neutral100
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "mutationRowBg",
    )

    val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        .format(Date(mutation.atMs))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = GoldinSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Debit tabungan",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.Ink,
            )
            Text(
                text = dateFormatted,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                color = Palette.InkSoft,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "+${formatGrams4(mutation.grams)}",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.Good700,
            )
            Text(
                text = rupiah(mutation.amount),
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Palette.InkSoft,
            )
        }
    }
}

@Composable
private fun InfoDrawerRow(
    label: String,
    value: String,
    isMono: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.InkSecondary,
            modifier = Modifier.padding(end = GoldinSpace.Md),
        )
        Text(
            text = value,
            fontFamily = if (isMono) MonoFontFamily else BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
            textAlign = TextAlign.End,
        )
    }
}
