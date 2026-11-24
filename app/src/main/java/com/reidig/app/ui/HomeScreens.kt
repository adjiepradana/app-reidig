package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.app.R
import com.reidig.shared.GoldToken
import com.reidig.shared.PaymentMethod
import com.reidig.shared.PendingOrder
import com.reidig.shared.RoutinePlanStatus
import com.reidig.shared.RoutinePlanType
import com.reidig.shared.RoutineSavingsPlan
import com.reidig.shared.Sku
import com.reidig.shared.gram
import com.reidig.shared.percent
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** VisualTransformation pemisah ribuan titik (`100000` -> `100.000`) dengan pemetaan kursor presisi. */
internal object IndonesianThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val n = digits.length
        val sb = StringBuilder(n + n / 3)
        val origToTrans = IntArray(n + 1)
        val transToOrigList = ArrayList<Int>(n + n / 3 + 1)

        for (i in 0 until n) {
            origToTrans[i] = sb.length
            val rem = n - i
            if (i > 0 && rem % 3 == 0) {
                sb.append('.')
                transToOrigList.add(i)
            }
            origToTrans[i] = sb.length
            sb.append(digits[i])
            transToOrigList.add(i)
        }
        origToTrans[n] = sb.length
        transToOrigList.add(n)

        val formatted = sb.toString()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, n)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrigList[offset.coerceIn(0, formatted.length)]
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

private fun formatGramsPlain4(grams: Double): String =
    String.format(Locale.forLanguageTag("id-ID"), "%.4f", grams)

@Composable
fun HomeScreen(
    env: AppEnv,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    onPrint: () -> Unit,
    onInstallment: () -> Unit,
    onWithdraw: () -> Unit,
    onVerify: () -> Unit,
    onPrices: () -> Unit,
    onNotifications: () -> Unit = onPrices,
    tradePrefill: TradePrefillDraft? = null,
    routinePlanPrefill: RoutinePlanPrefillDraft? = null,
    onOpenRoutinePlans: () -> Unit = onWithdraw,
    onOpenRoutinePlanDetail: (String) -> Unit = { onWithdraw() },
    onCreateRoutinePlan: () -> Unit = onVerify,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current

    val isVerified = state.user?.verified == true
    val isKycPending = state.user?.verified == false && state.user?.isKycPending == true
    val hideBalance = state.hideHomeBalance
    val goldGrams = if (isVerified) state.goldBalanceGrams else 0.0
    val goldValueRp = if (isVerified) state.portfolioValue else 0L
    val cashBalance = if (isVerified) state.walletBalance else 0L

    // Simulation state (`Main.dc.html` L3980–4012)
    var simMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Beli, 1 = Jual
    var simUnit by rememberSaveable { mutableStateOf("rp") } // "rp" | "gr"
    var simRaw by rememberSaveable { mutableStateOf("") }

    // Price chart & refresh state (`Main.dc.html` L3969–3977, L4025–4053)
    var chartRange by rememberSaveable { mutableStateOf("1B") }
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }
    var isRefreshingPrice by remember { mutableStateOf(false) }
    var hasPriceUpdate by remember { mutableStateOf(false) }
    var lastRefreshAtMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Promo Tabungan Rutin state (`Main.dc.html` L4731–4739)
    var hpAmtRaw by rememberSaveable { mutableStateOf("") }
    var hpTargetRaw by rememberSaveable { mutableStateOf("") }

    // Tampilkan titik merah pada tombol refresh setelah ~10 detik sejak pembaruan terakhir (`Main.dc.html` L3527)
    LaunchedEffect(lastRefreshAtMs, isRefreshingPrice, isPreview) {
        if (isPreview || isRefreshingPrice) return@LaunchedEffect
        hasPriceUpdate = false
        delay(10_000L)
        if (!isRefreshingPrice) {
            hasPriceUpdate = true
        }
    }

    val simIsBeli = simMode == 0
    val simIsRp = simUnit == "rp"
    val simRate = if (simIsBeli) state.askPricePerGram else state.buybackPerGram
    val simNum = if (simIsRp) {
        simRaw.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    } else {
        simRaw.replace(',', '.').toDoubleOrNull() ?: 0.0
    }
    val simGrams = if (simIsRp) {
        if (simRate > 0) simNum / simRate.toDouble() else 0.0
    } else {
        simNum
    }
    val simRp = if (simIsRp) simNum.toLong() else kotlin.math.round(simNum * simRate).toLong()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = 64.dp, bottom = GoldinSpace.Xl),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg),
        ) {
            // 2. Banner Status e-KYC: Belum verifikasi (`homeKycNeed`) atau Sedang ditinjau (`homeKycWait`)
            val currentUser = state.user
            if (currentUser != null && !currentUser.verified) {
                if (currentUser.isKycPending) {
                    GoldinFadeUp {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GoldinRadius.Xl))
                                .background(Palette.Info50)
                                .border(GoldinBorder.Default, Palette.Info100, RoundedCornerShape(GoldinRadius.Xl))
                                .padding(GoldinSpace.Md),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Palette.Info100),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.Clock,
                                    contentDescription = null,
                                    tint = Palette.Info700,
                                    modifier = Modifier.size(GoldinControl.IconMd),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs),
                            ) {
                                Text(
                                    text = "Verifikasi sedang ditinjau",
                                    color = Palette.Ink,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                                Text(
                                    text = "Proses verifikasi membutuhkan waktu maksimal 1x24 jam.",
                                    color = Palette.InkSecondary,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }
                } else {
                    GoldinFadeUp {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GoldinRadius.Xl))
                                .background(Palette.Primary50)
                                .border(GoldinBorder.Default, Palette.Primary100, RoundedCornerShape(GoldinRadius.Xl))
                                .padding(GoldinSpace.Md),
                            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Palette.Primary100),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = PrototypeIcons.ShieldCheck,
                                        contentDescription = null,
                                        tint = Palette.Primary700,
                                        modifier = Modifier.size(GoldinControl.IconMd),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs),
                                ) {
                                    Text(
                                        text = "Verifikasi akunmu sekarang",
                                        color = Palette.Ink,
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                    )
                                    Text(
                                        text = "Cukup 2 menit untuk mulai beli emas pertamamu.",
                                        color = Palette.InkSecondary,
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        letterSpacing = 0.5.sp,
                                    )
                                }
                            }
                            PrimaryButton(
                                text = "Verifikasi sekarang",
                                onClick = onVerify,
                                height = GoldinControl.SizeMd,
                            )
                        }
                    }
                }
            }

            // 3. Kartu Saldo (`Main.dc.html` L1027–1039, tersembunyi default & sinkron dengan Portofolio)
            HeroPurpleCard(shape = RoundedCornerShape(GoldinRadius.Xxl)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Saldo emas",
                            style = GoldinTypography.bodyMd,
                            color = Palette.Primary200,
                        )
                        GoldinIconButton(
                            icon = if (hideBalance) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                            contentDescription = if (hideBalance) "Tampilkan saldo" else "Sembunyikan saldo",
                            onClick = { env.repo.toggleHomeBalanceVisibility() },
                            tint = Palette.Primary100,
                            hoverColor = Palette.Primary50.copy(alpha = 0.10f),
                            modifier = Modifier.offset(x = 12.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (hideBalance) "••••••" else formatGramsPlain4(goldGrams),
                            color = Palette.Gold400,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 28.sp,
                            lineHeight = 36.sp,
                        )
                        Text(
                            text = "gr",
                            color = Palette.Gold400,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        text = if (hideBalance) "≈ Rp••••••" else "≈ ${rupiah(goldValueRp)}",
                        style = GoldinTypography.bodyMd,
                        color = Palette.Primary100,
                    )
                    Spacer(Modifier.height(GoldinSpace.Md))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(GoldinBorder.Hairline)
                            .background(Palette.Primary50.copy(alpha = 0.24f)),
                    )
                    Spacer(Modifier.height(GoldinSpace.Md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Saldo kas",
                            style = GoldinTypography.bodyMd,
                            color = Palette.Primary200,
                        )
                        Text(
                            text = if (hideBalance) "Rp••••••" else rupiah(cashBalance),
                            color = Palette.Primary50,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                    }
                }
            }

            // 4. 4 Aksi Cepat Goldin v1.2 (`Main.dc.html` L1041–1058, Handoff §6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
            ) {
                QuickActionTile("Beli emas", PrototypeIcons.Cart, Modifier.weight(1f), onBuy)
                QuickActionTile("Jual emas", PrototypeIcons.PriceTag, Modifier.weight(1f), onSell)
                QuickActionTile("Cetak emas", PrototypeIcons.GoldIngots, Modifier.weight(1f), onPrint)
                QuickActionTile("Portofolio", PrototypeIcons.PieChart, Modifier.weight(1f), onWithdraw)
            }

            // 5. Kartu Simulasi Transaksi (`Main.dc.html` L1060–1090)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GoldinRadius.Xl))
                    .background(Color.White)
                    .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                    .padding(GoldinSpace.Md),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                Text(
                    text = "Simulasi transaksi",
                    style = GoldinTypography.titleLg,
                    color = Palette.Ink,
                )

                // Segmen sliding pill full-width: Beli / Jual
                HomeSlidingSegment(
                    leftLabel = "Beli",
                    rightLabel = "Jual",
                    selectedIndex = simMode,
                    onSelect = { simMode = it },
                )

                // Baris label + sakelar satuan Rp / gr + input 44dp font Geist
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (simIsRp) "Nominal" else "Berat emas",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Palette.Ink,
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(GoldinRadius.Full))
                                .background(Palette.Neutral100)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            listOf("rp" to "Rp", "gr" to "gr").forEach { (unitKey, unitLabel) ->
                                val selected = simUnit == unitKey
                                val pillBg by animateColorAsState(
                                    targetValue = if (selected) Color.White else Color.Transparent,
                                    animationSpec = tween(GoldinMotion.DurationHover),
                                    label = "simUnitBg_$unitKey",
                                )
                                val pillText by animateColorAsState(
                                    targetValue = if (selected) Palette.Primary700 else Palette.InkSecondary,
                                    animationSpec = tween(GoldinMotion.DurationHover),
                                    label = "simUnitTx_$unitKey",
                                )
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 44.dp)
                                        .height(GoldinControl.SizeSm)
                                        .then(
                                            if (selected) {
                                                Modifier.shadow(2.dp, RoundedCornerShape(GoldinRadius.Full))
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clip(RoundedCornerShape(GoldinRadius.Full))
                                        .background(pillBg)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            role = Role.Tab,
                                        ) {
                                            if (simUnit != unitKey) {
                                                simRaw = if (simNum <= 0.0) {
                                                    ""
                                                } else if (unitKey == "gr") {
                                                    String.format(Locale.forLanguageTag("id-ID"), "%.4f", simGrams)
                                                } else {
                                                    simRp.toString()
                                                }
                                                simUnit = unitKey
                                            }
                                        }
                                        .padding(horizontal = GoldinSpace.Sm),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = unitLabel,
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = pillText,
                                    )
                                }
                            }
                        }
                    }

                    if (simIsRp) {
                        GoldinInputField(
                            value = simRaw,
                            onValueChange = { raw ->
                                simRaw = raw.filter { it.isDigit() }.trimStart('0').take(12)
                            },
                            placeholder = "0",
                            prefix = "Rp",
                            keyboardType = KeyboardType.Number,
                            visualTransformation = IndonesianThousandsVisualTransformation,
                        )
                    } else {
                        GoldinInputField(
                            value = simRaw,
                            onValueChange = { raw ->
                                val norm = raw.replace('.', ',').filter { it.isDigit() || it == ',' }
                                val parts = norm.split(',')
                                val intPart = parts[0].take(4)
                                simRaw = if (parts.size > 1) {
                                    "$intPart,${parts.drop(1).joinToString("").take(4)}"
                                } else {
                                    intPart
                                }
                            },
                            placeholder = "0,0000",
                            keyboardType = KeyboardType.Decimal,
                            trailingIcon = {
                                Text(
                                    text = "gr",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Palette.InkSecondary,
                                )
                            },
                        )
                    }
                }

                // Kotak hasil kalkulasi (`Primary50`, radius 12dp)
                val simResultLabel = when {
                    simIsBeli && simIsRp -> "Emas yang didapat"
                    simIsBeli -> "Harga beli"
                    simIsRp -> "Emas yang dijual"
                    else -> "Harga jual"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Palette.Primary50)
                        .padding(horizontal = GoldinSpace.Md, vertical = GoldinSpace.Sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = simResultLabel,
                        style = GoldinTypography.bodyMd,
                        color = Palette.InkSecondary,
                    )
                    if (simIsRp) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold)) {
                                    append(formatGramsPlain4(simGrams))
                                }
                                withStyle(SpanStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold)) {
                                    append(" gr")
                                }
                            },
                            color = Palette.Primary700,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                    } else {
                        Text(
                            text = rupiah(simRp),
                            color = Palette.Primary700,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                    }
                }

                // Tombol CTA Outline di dalam kartu Simulasi
                SecondaryButton(
                    text = if (simIsBeli) "Beli emas" else "Jual emas",
                    onClick = {
                        tradePrefill?.set(
                            mode = if (simIsBeli) "beli" else "jual",
                            unit = simUnit,
                            value = simRaw,
                        )
                        if (simIsBeli) onBuy() else onSell()
                    },
                    filledBackground = Color.Transparent,
                )
            }

            // 6. Kartu Harga Emas Hari Ini & Grafik Scrub Interaktif (`Main.dc.html` L1092–1137)
            GoldPriceTodayCard(
                askPrice = state.askPricePerGram,
                buybackPrice = state.buybackPerGram,
                dayChangePct = state.dayPriceChangePct,
                priceUpdatedLabel = state.priceUpdatedAtLabel,
                priceFlash = state.priceFlash,
                isRefreshing = isRefreshingPrice,
                hasPriceUpdate = hasPriceUpdate,
                selectedRangeKey = chartRange,
                scrubbedIndex = scrubbedIndex,
                onSelectRange = { nextKey ->
                    chartRange = nextKey
                    scrubbedIndex = null
                },
                onScrubChange = { nextIdx -> scrubbedIndex = nextIdx },
                onRefresh = {
                    if (!isRefreshingPrice) {
                        isRefreshingPrice = true
                        hasPriceUpdate = false
                        scope.launch {
                            delay(700L)
                            val nowHm = SimpleDateFormat("HH.mm", Locale.forLanguageTag("id-ID")).format(Date())
                            env.repo.refreshGoldPrice(formattedHm = nowHm)
                            isRefreshingPrice = false
                            lastRefreshAtMs = System.currentTimeMillis()
                            delay(700L)
                            env.repo.clearPriceFlash()
                        }
                    }
                },
            )

            // 7. Seksi Tabungan Rutin (`Main.dc.html` L1139–1167, Handoff §6)
            val routinePlans = if (isVerified) state.routinePlans else emptyList()
            val showRoutineSection = routinePlans.isNotEmpty() || !isKycPending
            if (showRoutineSection) {
                HorizontalDivider(
                    thickness = GoldinBorder.Hairline,
                    color = Palette.Line,
                )
                if (routinePlans.isNotEmpty()) {
                    HomeRoutinePlanListSection(
                        plans = routinePlans,
                        onSeeAll = onOpenRoutinePlans,
                        onOpenPlan = onOpenRoutinePlanDetail,
                    )
                } else {
                    HomeRoutinePlanPromoCard(
                        amountRaw = hpAmtRaw,
                        onAmountChange = { input ->
                            hpAmtRaw = input.filter { it.isDigit() }.trimStart('0').take(10)
                        },
                        targetRaw = hpTargetRaw,
                        onTargetChange = { input ->
                            val clean = input.replace('.', ',').filter { it.isDigit() || it == ',' }
                            val parts = clean.split(',')
                            hpTargetRaw = if (parts.size <= 1) {
                                parts[0].take(4)
                            } else {
                                parts[0].take(4) + "," + parts.drop(1).joinToString("").take(4)
                            }
                        },
                        onCreate = {
                            if (!isVerified) {
                                onCreateRoutinePlan()
                            } else {
                                val amt = hpAmtRaw.toLongOrNull() ?: 0L
                                if (amt in 1L until 10_000L) return@HomeRoutinePlanPromoCard
                                routinePlanPrefill?.set(
                                    monthlyAmount = amt,
                                    targetGrams = hpTargetRaw.replace(',', '.').toDoubleOrNull(),
                                )
                                onCreateRoutinePlan()
                            }
                        },
                    )
                }
            }

            // 8. Seksi Cicil Emas (Screen 40 / 46 Entry Point)
            val installmentContracts = if (isVerified) state.installmentContracts else emptyList()
            val showInstallmentSection = installmentContracts.isNotEmpty() || !isKycPending
            if (showInstallmentSection) {
                HorizontalDivider(
                    thickness = GoldinBorder.Hairline,
                    color = Palette.Line,
                )
                if (installmentContracts.isNotEmpty()) {
                    HomeInstallmentListSection(
                        contracts = installmentContracts,
                        onSeeAll = onInstallment,
                    )
                } else {
                    HomeInstallmentPromoCard(
                        onSimulate = onInstallment,
                    )
                }
            }
        }

        // 1. Sticky Scroll-Aware Header (`Main.dc.html` L992–1001): logo + wordmark Goldin + lonceng badge unread
        StickyTabHeader(
            title = "",
            isTabHeader = true,
            scrollState = scrollState,
            leftContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                ) {
                    GoldBarsBadge(size = 40.dp, cornerRadius = 12.dp, iconSize = 24.dp)
                    Text(
                        text = "Goldin",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp,
                        color = Palette.Primary700,
                    )
                }
            },
            rightAction = {
                HomeBellButton(
                    unreadCount = state.unreadNotificationCount,
                    onClick = onNotifications,
                )
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun HomeBellButton(
    unreadCount: Int,
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
        label = "bellBg",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "bellScale",
    )
    val badgeScale = remember { Animatable(1f) }
    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            badgeScale.snapTo(0.4f)
            badgeScale.animateTo(1f, tween(300, easing = GoldinMotion.EaseOutSheet))
        }
    }

    Box(modifier = Modifier.size(GoldinControl.TouchMin)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(bg)
                .border(GoldinBorder.Default, Palette.Line, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = if (unreadCount > 0) "Notifikasi, $unreadCount belum dibaca" else "Notifikasi",
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PrototypeIcons.Bell,
                contentDescription = null,
                tint = Palette.Ink,
                modifier = Modifier.size(GoldinControl.IconMd),
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .graphicsLayer { scaleX = badgeScale.value; scaleY = badgeScale.value }
                    .clip(CircleShape)
                    .background(Palette.Critical)
                    .border(2.dp, Palette.Paper, CircleShape)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun HomeSlidingSegment(
    leftLabel: String,
    rightLabel: String,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(GoldinControl.SizeLg)
            .clip(RoundedCornerShape(GoldinRadius.Full))
            .background(Palette.Neutral200)
            .padding(4.dp),
    ) {
        val halfWidth = (maxWidth - 4.dp) / 2
        val thumbOffset by animateDpAsState(
            targetValue = if (selectedIndex == 0) 0.dp else halfWidth + 4.dp,
            animationSpec = tween(300, easing = GoldinMotion.EaseOutSheet),
            label = "simThumbOffset",
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(halfWidth)
                .height(36.dp)
                .shadow(2.dp, RoundedCornerShape(GoldinRadius.Full))
                .clip(RoundedCornerShape(GoldinRadius.Full))
                .background(Color.White),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(leftLabel, rightLabel).forEachIndexed { idx, label ->
                val active = selectedIndex == idx
                val textColor by animateColorAsState(
                    targetValue = if (active) Palette.Primary700 else Palette.InkSecondary,
                    animationSpec = tween(GoldinMotion.DurationHover),
                    label = "simSegColor$idx",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) { onSelect(idx) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "qaScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHovered || isPressed) Palette.Primary600 else Palette.Line,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "qaBorder",
    )
    val boxBg by animateColorAsState(
        targetValue = if (isPressed) Palette.Primary50 else Color.White,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "qaBg",
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = GoldinSpace.Xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(GoldinRadius.Xl))
                .background(boxBg)
                .border(GoldinBorder.Default, borderColor, RoundedCornerShape(GoldinRadius.Xl)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Palette.Primary700,
                modifier = Modifier.size(GoldinControl.IconMd),
            )
        }
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Medium,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private data class GoldPriceRangeSpec(
    val key: String,
    val name: String,
    val count: Int,
    val stepMs: Long,
    val drift: Double,
    val vol: Double,
    val formatKind: String, // "time" | "day" | "month"
)

private object GoldPriceSeries {
    private val monthsShort = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    val ranges: List<GoldPriceRangeSpec> = listOf(
        GoldPriceRangeSpec("1H", "1 hari", 25, 3_600_000L, 0.004, 0.0012, "time"),
        GoldPriceRangeSpec("1M", "1 minggu", 29, 6L * 3_600_000L, 0.012, 0.002, "day"),
        GoldPriceRangeSpec("1B", "1 bulan", 31, 86_400_000L, 0.03, 0.004, "day"),
        GoldPriceRangeSpec("6B", "6 bulan", 27, 7L * 86_400_000L, 0.09, 0.008, "month"),
        GoldPriceRangeSpec("1T", "1 tahun", 53, 7L * 86_400_000L, 0.22, 0.01, "month"),
    )

    private val ratiosByKey: Map<String, DoubleArray> = buildMap {
        ranges.forEachIndexed { ri, spec ->
            var seed = 7 + ri * 31
            fun nextRandom(): Double {
                seed += 0x6D2B79F5
                var t = (seed xor (seed ushr 15)) * (seed or 1)
                t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
                val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
                return unsigned.toDouble() / 4294967296.0
            }
            val arr = DoubleArray(spec.count)
            arr[spec.count - 1] = 1.0
            for (i in spec.count - 2 downTo 0) {
                arr[i] = arr[i + 1] * (1.0 - spec.drift / (spec.count - 1) + (nextRandom() - 0.5) * 2.0 * spec.vol)
            }
            put(spec.key, arr)
        }
    }

    fun rangeFor(key: String): GoldPriceRangeSpec =
        ranges.firstOrNull { it.key == key } ?: ranges[2]

    fun valuesFor(spec: GoldPriceRangeSpec, askPrice: Long): LongArray {
        val ratios = ratiosByKey[spec.key] ?: DoubleArray(spec.count) { 1.0 }
        val result = LongArray(spec.count) { idx ->
            val raw = (ratios[idx] * askPrice.toDouble()).roundToLong()
            ((raw + 250L) / 500L) * 500L
        }
        result[spec.count - 1] = askPrice
        return result
    }

    fun anchorNowHourMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun pointCalendar(spec: GoldPriceRangeSpec, index: Int, anchorMs: Long): Calendar {
        val ts = anchorMs - (spec.count - 1 - index).toLong() * spec.stepMs
        return Calendar.getInstance().apply { timeInMillis = ts }
    }

    private fun pad2(v: Int): String = v.toString().padStart(2, '0')

    fun fullDateLabel(spec: GoldPriceRangeSpec, index: Int, anchorMs: Long): String {
        val cal = pointCalendar(spec, index, anchorMs)
        val base = "${pad2(cal.get(Calendar.DAY_OF_MONTH))} ${monthsShort[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
        return when {
            spec.formatKind == "day" && spec.key == "1B" -> base
            spec.formatKind == "month" -> base
            else -> "$base, ${pad2(cal.get(Calendar.HOUR_OF_DAY))}.00"
        }
    }

    fun shortDateLabel(spec: GoldPriceRangeSpec, index: Int, anchorMs: Long): String {
        val cal = pointCalendar(spec, index, anchorMs)
        return when (spec.formatKind) {
            "time" -> "${pad2(cal.get(Calendar.HOUR_OF_DAY))}.00"
            "day" -> "${pad2(cal.get(Calendar.DAY_OF_MONTH))} ${monthsShort[cal.get(Calendar.MONTH)]}"
            else -> "${monthsShort[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
        }
    }
}

private fun formatPct1(value: Double): String =
    String.format(Locale.forLanguageTag("id-ID"), "%.1f", value)

@Composable
private fun GoldPriceTodayCard(
    askPrice: Long,
    buybackPrice: Long,
    dayChangePct: Double,
    priceUpdatedLabel: String,
    priceFlash: String?,
    isRefreshing: Boolean,
    hasPriceUpdate: Boolean,
    selectedRangeKey: String,
    scrubbedIndex: Int?,
    onSelectRange: (String) -> Unit,
    onScrubChange: (Int?) -> Unit,
    onRefresh: () -> Unit,
) {
    val rangeSpec = remember(selectedRangeKey) { GoldPriceSeries.rangeFor(selectedRangeKey) }
    val values = remember(rangeSpec, askPrice) { GoldPriceSeries.valuesFor(rangeSpec, askPrice) }
    val anchorMs = remember { GoldPriceSeries.anchorNowHourMs() }

    val activeIndex = scrubbedIndex?.coerceIn(0, values.lastIndex) ?: values.lastIndex
    val activeValue = values[activeIndex]
    val delta = activeValue - values[0]
    val deltaPct = if (values[0] > 0L) delta.toDouble() / values[0].toDouble() * 100.0 else 0.0
    val rangeUp = delta >= 0L
    val rangeColor = if (rangeUp) Palette.Good700 else Palette.Critical
    val rangeText = "${if (rangeUp) "+" else "−"}${rupiah(abs(delta))} (${formatPct1(abs(deltaPct))}%)"

    val dayUp = dayChangePct >= 0.0
    val dayColor = if (dayUp) Palette.Good700 else Palette.Critical
    val dayText = "${formatPct1(abs(dayChangePct))}%"

    val flashBg by animateColorAsState(
        targetValue = when (priceFlash) {
            "up" -> Palette.GoodSoft
            "down" -> Palette.CriticalSoft
            else -> Palette.Paper
        },
        animationSpec = tween(600),
        label = "priceFlashBg",
    )

    val updatedCaption = remember(priceUpdatedLabel) {
        if (priceUpdatedLabel.startsWith("Diperbarui", ignoreCase = true)) {
            priceUpdatedLabel
        } else {
            "Diperbarui $priceUpdatedLabel"
        }
    }

    val chartCaption = remember(rangeSpec, scrubbedIndex, anchorMs) {
        if (scrubbedIndex != null) {
            GoldPriceSeries.fullDateLabel(rangeSpec, scrubbedIndex.coerceIn(0, rangeSpec.count - 1), anchorMs)
        } else {
            "Harga emas ${rangeSpec.name} terakhir"
        }
    }

    val xStartLabel = remember(rangeSpec, anchorMs) {
        GoldPriceSeries.shortDateLabel(rangeSpec, 0, anchorMs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(Color.White)
            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
            .padding(GoldinSpace.Md),
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
    ) {
        // Header kartu: judul + timestamp + tombol refresh berputar & dot merah
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
                    text = "Harga emas hari ini",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = Palette.Ink,
                )
                Text(
                    text = updatedCaption,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.5.sp,
                    color = Palette.InkSoft,
                )
            }
            PriceRefreshButton(
                isRefreshing = isRefreshing,
                hasPriceUpdate = hasPriceUpdate,
                onClick = onRefresh,
            )
        }

        // 2 Kotak harga (Harga beli & Harga jual) dengan animasi flash 600ms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
        ) {
            GoldPriceStatBox(
                label = "Harga beli",
                price = rupiah(askPrice),
                changeText = dayText,
                isUp = dayUp,
                changeColor = dayColor,
                backgroundColor = flashBg,
                modifier = Modifier.weight(1f),
            )
            GoldPriceStatBox(
                label = "Harga jual",
                price = rupiah(buybackPrice),
                changeText = dayText,
                isUp = dayUp,
                changeColor = dayColor,
                backgroundColor = flashBg,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(
            thickness = GoldinBorder.Hairline,
            color = Palette.Line,
        )

        // Caption dinamis, nilai titik aktif, dan delta rentang
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = chartCaption,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                color = Palette.InkSecondary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
            ) {
                Text(
                    text = rupiah(activeValue),
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    color = Palette.Ink,
                    maxLines = 1,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = if (rangeUp) PrototypeIcons.CaretUpFilled else PrototypeIcons.CaretDownFilled,
                        contentDescription = null,
                        tint = rangeColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = rangeText,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = rangeColor,
                        maxLines = 1,
                    )
                }
            }
        }

        // Grafik scrubbing interaktif + sumbu X
        Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xs)) {
            InteractiveGoldPriceChart(
                values = values,
                scrubbedIndex = scrubbedIndex,
                onScrubChange = onScrubChange,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = xStartLabel,
                    fontFamily = BodyFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.3.sp,
                    color = Palette.InkSoft,
                )
                Text(
                    text = "Sekarang",
                    fontFamily = BodyFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.3.sp,
                    color = Palette.InkSoft,
                )
            }
        }

        // Pemilih rentang 5 kolom: 1H · 1M · 1B · 6B · 1T
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GoldinRadius.Full))
                .background(Palette.Neutral100)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GoldPriceSeries.ranges.forEach { spec ->
                val active = spec.key == rangeSpec.key
                val pillBg by animateColorAsState(
                    targetValue = if (active) Color.White else Color.Transparent,
                    animationSpec = tween(GoldinMotion.DurationHover),
                    label = "rangeBg_${spec.key}",
                )
                val pillText by animateColorAsState(
                    targetValue = if (active) Palette.Primary700 else Palette.InkSecondary,
                    animationSpec = tween(GoldinMotion.DurationHover),
                    label = "rangeTxt_${spec.key}",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(GoldinControl.SizeSm)
                        .then(
                            if (active) Modifier.shadow(2.dp, RoundedCornerShape(GoldinRadius.Full))
                            else Modifier,
                        )
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(pillBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                        ) { onSelectRange(spec.key) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = spec.key,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = pillText,
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRefreshButton(
    isRefreshing: Boolean,
    hasPriceUpdate: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "refreshBg",
    )
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(GoldinMotion.DurationSpin, easing = LinearEasing),
                )
            }
        } else {
            rotation.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .offset(x = 6.dp)
            .size(GoldinControl.TouchMin)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = if (hasPriceUpdate) "Harga terbaru tersedia, perbarui harga" else "Perbarui harga",
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = PrototypeIcons.Refresh,
            contentDescription = null,
            tint = Palette.Primary700,
            modifier = Modifier
                .size(GoldinControl.IconMd)
                .graphicsLayer { rotationZ = rotation.value },
        )
        if (hasPriceUpdate && !isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-9).dp, y = 9.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Palette.Critical)
                    .border(1.5.dp, Color.White, CircleShape),
            )
        }
    }
}

@Composable
private fun GoldPriceStatBox(
    label: String,
    price: String,
    changeText: String,
    isUp: Boolean,
    changeColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GoldinRadius.Lg))
            .background(backgroundColor)
            .padding(GoldinSpace.Sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = Palette.InkSecondary,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp,
        )
        Text(
            text = price,
            color = Palette.Ink,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            maxLines = 1,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = if (isUp) PrototypeIcons.CaretUpFilled else PrototypeIcons.CaretDownFilled,
                contentDescription = null,
                tint = changeColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = changeText,
                color = changeColor,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun InteractiveGoldPriceChart(
    values: LongArray,
    scrubbedIndex: Int?,
    onScrubChange: (Int?) -> Unit,
) {
    val minVal = remember(values) { values.minOrNull() ?: 0L }
    val maxVal = remember(values) { values.maxOrNull() ?: 1L }
    val span = (maxVal - minVal).coerceAtLeast(1L).toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(values.size) {
                if (values.size < 2) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val padX = 6.dp.toPx()
                    val usableW = (size.width.toFloat() - padX * 2f).coerceAtLeast(1f)
                    fun indexFor(x: Float): Int {
                        val frac = ((x - padX) / usableW).coerceIn(0f, 1f)
                        return (frac * (values.size - 1)).roundToInt().coerceIn(0, values.lastIndex)
                    }

                    onScrubChange(indexFor(down.position.x))
                    val pointerId = down.id
                    val touchSlop = viewConfiguration.touchSlop
                    var horizontalLocked = false
                    var accDx = 0f
                    var accDy = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            onScrubChange(null)
                            break
                        }
                        val dx = change.position.x - change.previousPosition.x
                        val dy = change.position.y - change.previousPosition.y
                        if (!horizontalLocked) {
                            accDx += abs(dx)
                            accDy += abs(dy)
                            if (accDy > touchSlop && accDy > accDx) {
                                onScrubChange(null)
                                break
                            }
                            if (accDx > touchSlop && accDx >= accDy) {
                                horizontalLocked = true
                            }
                        }
                        if (horizontalLocked) {
                            change.consume()
                            onScrubChange(indexFor(change.position.x))
                        }
                    }
                }
            },
    ) {
        if (values.size < 2) return@Canvas
        val padX = 6.dp.toPx()
        val padY = 8.dp.toPx()
        val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
        val usableH = (size.height - padY * 2f).coerceAtLeast(1f)

        fun pointAt(idx: Int): Offset {
            val x = padX + idx.toFloat() / (values.size - 1).toFloat() * usableW
            val norm = (values[idx] - minVal).toFloat() / span
            val y = padY + (1f - norm) * usableH
            return Offset(x, y)
        }

        val linePath = Path()
        val areaPath = Path()
        for (i in values.indices) {
            val p = pointAt(i)
            if (i == 0) {
                linePath.moveTo(p.x, p.y)
                areaPath.moveTo(p.x, size.height)
                areaPath.lineTo(p.x, p.y)
            } else {
                linePath.lineTo(p.x, p.y)
                areaPath.lineTo(p.x, p.y)
            }
        }
        val lastPoint = pointAt(values.lastIndex)
        areaPath.lineTo(lastPoint.x, size.height)
        areaPath.close()

        drawPath(
            path = areaPath,
            color = Palette.Primary700.copy(alpha = 0.06f),
        )
        drawPath(
            path = linePath,
            color = Palette.Primary700,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val activeIdx = scrubbedIndex?.coerceIn(0, values.lastIndex) ?: values.lastIndex
        val activePoint = pointAt(activeIdx)

        if (scrubbedIndex != null) {
            drawLine(
                color = Palette.Neutral300,
                start = Offset(activePoint.x, 0f),
                end = Offset(activePoint.x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Titik indikator 12dp (putih + border 2dp Primary700)
        val strokePx = 2.dp.toPx()
        val dotRadius = 6.dp.toPx() - strokePx / 2f
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = activePoint,
        )
        drawCircle(
            color = Palette.Primary700,
            radius = dotRadius,
            center = activePoint,
            style = Stroke(width = strokePx),
        )
    }
}

@Composable
private fun HomeRoutinePlanListSection(
    plans: List<RoutineSavingsPlan>,
    onSeeAll: () -> Unit,
    onOpenPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tabungan rutin",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Palette.Ink,
            )
            val seeAllInteraction = remember { MutableInteractionSource() }
            val seeAllHovered by seeAllInteraction.collectIsHoveredAsState()
            val seeAllPressed by seeAllInteraction.collectIsPressedAsState()
            val seeAllBg by animateColorAsState(
                targetValue = when {
                    seeAllPressed -> Palette.Primary100
                    seeAllHovered -> Palette.Primary50
                    else -> Color.Transparent
                },
                animationSpec = tween(GoldinMotion.DurationHover),
                label = "seeAllBg",
            )
            Box(
                modifier = Modifier
                    .offset(x = 10.dp)
                    .height(GoldinControl.TouchMin)
                    .clip(RoundedCornerShape(GoldinRadius.Full))
                    .background(seeAllBg)
                    .clickable(
                        interactionSource = seeAllInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onSeeAll,
                    )
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Lihat semua",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.Primary700,
                )
            }
        }

        plans.forEach { plan ->
            RoutinePlanHomeCard(
                plan = plan,
                onClick = { onOpenPlan(plan.id) },
            )
        }
    }
}

@Composable
private fun RoutinePlanHomeCard(
    plan: RoutineSavingsPlan,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (isHovered || isPressed) Palette.Neutral100 else Color.White,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "planCardBg",
    )
    val isPaused = plan.status == RoutinePlanStatus.PAUSED
    val hasTarget = when (plan.type) {
        RoutinePlanType.PERIODE -> plan.targetMonths > 0
        RoutinePlanType.TARGET_EMAS -> plan.targetGrams > 0.0
    }
    val pctFraction = (plan.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val pctLabel = "${plan.progressPercent.coerceIn(0.0, 100.0).roundToInt()}%"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Lg))
            .background(bg)
            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = GoldinSpace.Md, vertical = GoldinSpace.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
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
                            .padding(horizontal = GoldinSpace.Xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Dijeda",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = Palette.InkSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
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
            if (hasTarget) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
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
                                .fillMaxHeight()
                                .fillMaxWidth(pctFraction)
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
            modifier = Modifier.size(GoldinControl.IconMd),
        )
    }
}

@Composable
private fun HomeRoutinePlanPromoCard(
    amountRaw: String,
    onAmountChange: (String) -> Unit,
    targetRaw: String,
    onTargetChange: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val amountLong = amountRaw.toLongOrNull() ?: 0L
    val isAmountInvalid = amountLong in 1L until 10_000L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(Color.White)
            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
            .padding(GoldinSpace.Md),
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Tabungan rutin",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Palette.Ink,
            )
            Text(
                text = "Mulai nabung emas otomatis setiap bulan.",
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                color = Palette.InkSecondary,
            )
        }

        FormField(
            label = "Nominal per debit",
            value = amountRaw,
            onValueChange = onAmountChange,
            placeholder = "0",
            prefix = "Rp",
            mandatory = true,
            keyboardType = KeyboardType.Number,
            transformation = IndonesianThousandsVisualTransformation,
            error = if (isAmountInvalid) "Minimal Rp10.000" else null,
            hint = if (!isAmountInvalid) "Minimal Rp10.000" else null,
        )

        FormField(
            label = "Target emas",
            value = targetRaw,
            onValueChange = onTargetChange,
            placeholder = "0",
            keyboardType = KeyboardType.Decimal,
            trailing = {
                Text(
                    text = "gr",
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSecondary,
                )
            },
        )

        SecondaryButton(
            text = "Buat tabungan rutin",
            onClick = onCreate,
            filledBackground = Color.Transparent,
        )
    }
}

@ScreenPreview
@Composable
private fun HomeVerifiedPreview() = GoldinTheme { HomeScreen(rememberPreviewEnv(true), {}, {}, {}, {}, {}, {}, {}) }

@ScreenPreview
@Composable
private fun HomeUnverifiedPreview() = GoldinTheme { HomeScreen(rememberPreviewEnv(false), {}, {}, {}, {}, {}, {}, {}) }

@ScreenPreview
@Composable
private fun HomePendingVerificationPreview() = GoldinTheme {
    val env = rememberPreviewEnv(false)
    env.repo.submitKycPending()
    HomeScreen(env, {}, {}, {}, {}, {}, {}, {})
}

@ScreenPreview
@Composable
private fun HomeEmptyRoutinePlanPreview() = GoldinTheme {
    val env = rememberPreviewEnv(true)
    env.repo.state.value.routinePlans.forEach { env.repo.stopRoutinePlan(it.id) }
    HomeScreen(env, {}, {}, {}, {}, {}, {}, {})
}

/** Harga beli dan harga jual (buyback) semua pecahan. */
@Composable
fun PriceTableScreen(env: AppEnv, onBack: () -> Unit) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Palette.Paper).statusBarsPadding()) {
        TopTitleBar("Tabel Harga Emas", onBack)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)) {
            item {
                SurfaceCard {
                    Text("Diperbarui ${state.priceUpdatedAtLabel}", color = Palette.InkSoft, fontFamily = BodyFontFamily, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    PriceTableRow("Berat", "Harga beli", "Harga jual", header = true)
                    HorizontalDivider(color = Palette.Line)
                    state.skus.forEachIndexed { idx, sku ->
                        PriceTableRow(gram(sku.weightMg), rupiah(sku.buyPrice), rupiah(sku.buybackPrice))
                        if (idx < state.skus.lastIndex) {
                            HorizontalDivider(color = Palette.Line.copy(alpha = 0.5f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(state.skus.firstOrNull()?.product ?: "", color = Palette.InkSoft, fontFamily = BodyFontFamily, fontSize = 12.5.sp)
            }
        }
    }
}

@ScreenPreview
@Composable
private fun PriceTablePreview() = GoldinTheme { PriceTableScreen(rememberPreviewEnv()) {} }

@Composable
private fun PriceTableRow(weight: String, buy: String, sell: String, header: Boolean = false) {
    val color = if (header) Palette.InkSoft else Palette.Ink
    val fontSize = if (header) 12.sp else 13.sp
    Row(Modifier.fillMaxWidth().padding(vertical = if (header) 8.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            weight,
            color = color,
            fontFamily = BodyFontFamily,
            fontSize = fontSize,
            fontWeight = if (header) FontWeight.Medium else FontWeight.SemiBold,
            modifier = Modifier.width(64.dp),
        )
        Text(
            buy,
            color = color,
            fontFamily = if (header) BodyFontFamily else MonoFontFamily,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            sell,
            color = color,
            fontFamily = if (header) BodyFontFamily else MonoFontFamily,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun CatalogScreen(env: AppEnv, header: @Composable () -> Unit = {}, onSelect: (String) -> Unit) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val sku1g = state.sku("LMRE-1G")
    Column(Modifier.fillMaxSize().background(Palette.Paper).statusBarsPadding()) {
        TopTitleBar("Transaksi", null)
        header()
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                // Summary card matching 13 Transaksi prototype
                SurfaceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Saldo kas tersedia", color = Palette.InkSoft, fontFamily = BodyFontFamily, fontSize = 12.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                rupiah(state.walletBalance),
                                color = Palette.Ink,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Harga beli / gr", color = Palette.InkSoft, fontFamily = BodyFontFamily, fontSize = 12.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                rupiah(sku1g.buyPrice),
                                color = Palette.Primary,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Pilih pecahan emas fisik 24K",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Palette.Ink,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(state.skus, key = { it.code }) { sku ->
                CatalogSkuCard(sku) { onSelect(sku.code) }
            }
        }
    }
}

@Composable
private fun CatalogSkuCard(sku: Sku, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.GoldSoft)
                    .border(1.dp, Palette.Gold200, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    gram(sku.weightMg).removeSuffix(" gr"),
                    color = Palette.GoldDeep,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${sku.product} ${gram(sku.weightMg)}",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = Palette.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    rupiah(sku.buyPrice),
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Primary,
                )
            }
            if (sku.stock == 0) {
                StatusChip("Stok habis", ChipTone.WARN, GoldinIcons.AlertCircle)
            } else {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Palette.Primary50).padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "Stok ${sku.stock}",
                        color = Palette.Primary,
                        fontFamily = BodyFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun CatalogPreview() = ReidigTheme { CatalogScreen(rememberPreviewEnv()) {} }

@Composable
fun BuyScreen(env: AppEnv, skuCode: String, onBack: () -> Unit, onDone: (List<GoldToken>) -> Unit) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val sku = state.sku(skuCode)
    var quantity by remember { mutableIntStateOf(1) }
    var method by remember { mutableStateOf(PaymentMethod.QRIS) }
    val inPreview = LocalInspectionMode.current
    var quote by remember { mutableStateOf(if (inPreview) env.repo.quoteBuy(skuCode, 1) else null) }
    var askPin by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableLongStateOf(60L) }
    var pendingOrder by remember { mutableStateOf<PendingOrder?>(null) }

    // Harga dikunci 60 detik, lalu diperbarui otomatis.
    LaunchedEffect(quantity, pendingOrder) {
        while (pendingOrder == null) {
            val current = env.attempt { env.repo.quoteBuy(skuCode, quantity) }
            quote = current
            if (current == null) break
            while (true) {
                secondsLeft = (current.expiresAtMs - System.currentTimeMillis()) / 1000
                if (secondsLeft <= 0) break
                delay(1000)
            }
        }
    }

    val current = quote
    val insufficient = method == PaymentMethod.WALLET && current != null && state.walletBalance < current.total

    FormScreen(
        "Konfirmasi transaksi",
        onBack,
        bottom = {
            PrimaryButton(
                "Konfirmasi & bayar ${current?.let { rupiah(it.total) } ?: ""}",
                { askPin = true },
                enabled = current != null && !insufficient && sku.stock > 0,
            )
        },
    ) {
        // Price Lock Timer Banner (matching 14 Konfirmasi prototype)
        if (current != null) {
            val sec = secondsLeft.coerceIn(0, 60)
            SurfaceCard(tint = Palette.Primary50, outline = Palette.Primary100) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { sec / 60f },
                            modifier = Modifier.size(44.dp),
                            color = if (sec <= 15) Palette.Critical else Palette.Primary,
                            trackColor = Palette.Primary200,
                            strokeWidth = 3.5.dp,
                        )
                        Text(
                            "${sec}d",
                            fontSize = 11.sp,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sec <= 15) Palette.Critical else Palette.Primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Harga dikunci ${sec} detik",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            "Harga akan diperbarui otomatis jika waktu habis.",
                            color = Palette.InkSecondary,
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Detail Emas & Rincian Order Card
        SurfaceCard {
            Text(
                "DETAIL EMAS",
                color = Palette.InkSoft,
                fontFamily = BodyFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(8.dp))
            SummaryRow("Jenis transaksi", "Beli Emas")
            SummaryRow("Produk", "${sku.product} ${gram(sku.weightMg)}", strong = true, valueColor = Palette.Primary)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Jumlah keping (Stok ${sku.stock})",
                    color = Palette.InkSecondary,
                    fontFamily = BodyFontFamily,
                    fontSize = 13.5.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { if (quantity > 1) quantity-- }, enabled = quantity > 1, modifier = Modifier.size(32.dp)) {
                    Icon(GoldinIcons.Minus, "Kurangi", modifier = Modifier.size(18.dp))
                }
                Text(
                    "$quantity",
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                IconButton(onClick = { if (quantity < sku.stock) quantity++ }, enabled = quantity < sku.stock, modifier = Modifier.size(32.dp)) {
                    Icon(GoldinIcons.Plus, "Tambah", modifier = Modifier.size(18.dp))
                }
            }

            if (current != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Palette.Line)
                Spacer(Modifier.height(10.dp))
                Text(
                    "RINCIAN ORDER (RP)",
                    color = Palette.InkSoft,
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(8.dp))
                SummaryRow("Harga per keping", rupiah(current.unitPrice))
                SummaryRow("Biaya admin", "Gratis", valueColor = Palette.Good)
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Palette.Line)
                Spacer(Modifier.height(6.dp))
                SummaryRow("Total pembayaran", rupiah(current.total), strong = true)
            }
        }

        if (current != null) {
            Spacer(Modifier.height(12.dp))
            SurfaceCard(tint = Palette.WarnSoft, outline = Palette.WarnBorder) {
                Text(
                    "Estimasi buyback saat ini",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Palette.Warn,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Jika dijual kembali sekarang senilai ${rupiah(current.buybackValueNow)} (selisih ${percent(sku.spreadPercent)}). Token dapat dijual 24 jam setelah pembelian.",
                    color = Palette.InkSecondary,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Metode pembayaran",
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Palette.Ink,
        )
        Spacer(Modifier.height(8.dp))
        PaymentMethod.entries.forEach { option ->
            val detail = if (option == PaymentMethod.WALLET) "Saldo tersedia ${rupiah(state.walletBalance)}" else "Konfirmasi otomatis"
            val logoRes = when (option) {
                PaymentMethod.QRIS -> R.drawable.logo_qris
                PaymentMethod.VIRTUAL_ACCOUNT, PaymentMethod.VA_BCA -> R.drawable.logo_bca
                PaymentMethod.VA_BRI -> R.drawable.logo_bri
                PaymentMethod.VA_MANDIRI -> R.drawable.logo_mandiri
                PaymentMethod.VA_BNI -> R.drawable.logo_bni
                else -> null
            }
            PaymentOption(option.label, detail, option == method, logoRes = logoRes) { method = option }
            Spacer(Modifier.height(8.dp))
        }
        if (insufficient) ErrorLine("Saldo tidak mencukupi. Pilih metode lain.")
        Spacer(Modifier.height(6.dp))
    }

    if (askPin && current != null) {
        PinDialog(env, "Konfirmasi Beli Emas senilai ${rupiah(current.total)}", { askPin = false }) {
            askPin = false
            env.attempt { env.repo.createOrder(current.id, method) }?.let { order ->
                if (method == PaymentMethod.WALLET) {
                    env.attempt { env.repo.confirmPayment(order.id) }?.let(onDone)
                } else {
                    pendingOrder = order
                }
            }
        }
    }
    pendingOrder?.let { order ->
        PaymentWaitDialog(
            env,
            order.quote.total,
            order.method,
            order.paymentCode,
            order.payBeforeMs,
            "Batalkan pesanan",
            onCancel = {
                env.repo.cancelOrder(order.id)
                pendingOrder = null
            },
            onConfirm = {
                env.attempt { env.repo.confirmPayment(order.id) }?.let { minted ->
                    pendingOrder = null
                    onDone(minted)
                }
            },
        )
    }
}

@ScreenPreview
@Composable
private fun BuyPreview() = ReidigTheme { BuyScreen(rememberPreviewEnv(), "LMRE-1G", {}, {}) }

/** Menampilkan QR atau nomor VA sesuai layar 15 Menunggu Pembayaran prototipe. */
@Composable
internal fun PaymentWaitDialog(
    env: AppEnv,
    amount: Long,
    method: PaymentMethod,
    paymentCode: String,
    payBeforeMs: Long,
    cancelLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    var secondsLeft by remember { mutableLongStateOf(0L) }
    LaunchedEffect(payBeforeMs) {
        while (true) {
            secondsLeft = ((payBeforeMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            if (secondsLeft == 0L) {
                env.notify("Batas waktu pembayaran habis. Keping dilepas kembali.")
                onCancel()
                break
            }
            delay(1000)
        }
    }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Palette.Paper).statusBarsPadding().navigationBarsPadding()) {
            TopTitleBar("Menunggu Pembayaran", onCancel)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SurfaceCard(tint = Palette.WarnSoft, outline = Palette.WarnBorder) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Selesaikan pembayaran dalam", color = Palette.Warn, fontFamily = BodyFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
                            color = Palette.Warn,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SurfaceCard {
                    Text("Total pembayaran", color = Palette.InkSoft, fontFamily = BodyFontFamily, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        rupiah(amount),
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        color = Palette.Primary,
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Palette.Line)
                    Spacer(Modifier.height(14.dp))
                    if (method == PaymentMethod.QRIS) {
                        Image(
                            painterResource(R.drawable.logo_qris),
                            contentDescription = "QRIS",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(22.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Palette.Primary50)
                                    .border(1.dp, Palette.Primary100, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(GoldinIcons.QrCode, "Kode QRIS", tint = Palette.Primary, modifier = Modifier.size(164.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Pindai QRIS dengan aplikasi mobile banking atau e-wallet.",
                            color = Palette.InkSecondary,
                            fontFamily = BodyFontFamily,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(R.drawable.logo_bca),
                                contentDescription = "BCA",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Nomor Virtual Account BCA", color = Palette.InkSoft, fontFamily = BodyFontFamily, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            paymentCode,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            color = Palette.Ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Transfer sesuai nominal tepat hingga digit terakhir.",
                            color = Palette.InkSecondary,
                            fontFamily = BodyFontFamily,
                            fontSize = 12.5.sp,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Harga dan keping emas ditahan untukmu sampai batas waktu pembayaran berakhir.",
                    color = Palette.InkSoft,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                PrimaryButton("Saya sudah bayar", onConfirm)
                Spacer(Modifier.height(10.dp))
                SecondaryButton(cancelLabel, onCancel)
            }
        }
    }
}

@Composable
internal fun PaymentOption(
    title: String,
    detail: String,
    selected: Boolean,
    logoRes: Int? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (selected) Palette.Primary50 else Color.White)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) Palette.Primary else Palette.Line, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (logoRes != null) {
            Box(
                Modifier.width(46.dp).height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(logoRes),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Box(
                Modifier.width(46.dp).height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.Primary50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PrototypeIcons.Wallet, null, tint = Palette.Primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Palette.Ink)
            if (detail.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(detail, color = Palette.InkSecondary, fontFamily = BodyFontFamily, fontSize = 12.sp)
            }
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Palette.Primary, unselectedColor = Palette.Line),
        )
    }
}

/** Layar hasil bersama untuk transaksi yang berhasil (menggunakan DoubleRingSuccessBadge). */
@Composable
fun SuccessScreen(title: String, message: String, buttonLabel: String, onDone: () -> Unit, details: @Composable () -> Unit = {}) {
    FormScreen("", null, bottom = { PrimaryButton(buttonLabel, onDone) }) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DoubleRingSuccessBadge(PrototypeIcons.Check)
        }
        Spacer(Modifier.height(22.dp))
        Headline(title, message, centered = true, titleColor = Palette.Primary)
        Spacer(Modifier.height(20.dp))
        details()
    }
}
