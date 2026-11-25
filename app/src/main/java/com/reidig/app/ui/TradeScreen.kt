package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.MIN_TRADE_RP
import com.reidig.shared.TxType
import com.reidig.shared.calculateTradeAmounts
import com.reidig.shared.floorGrams4
import com.reidig.shared.formatGrams4
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Layar `13 Transaksi` (Beli emas / Jual emas) sesuai prototipe Goldin v1.2 (`Main.dc.html` L1483–1611) & Handoff §7.
 * Fitur utama:
 * - Sticky header 44dp + ikon riwayat ber-badge merah
 * - Banner kuning-oranye transaksi aktif (`actBanner`) dengan live timer countdown & direct open
 * - Kartu verifikasi akun (e-KYC) ungu lembut jika belum terverifikasi
 * - Toggle sliding pill Beli / Jual + info saldo kas / saldo emas tersedia
 * - Kartu harga live + tombol refresh 44dp (animasi putar 800ms) + dot indikator merah
 * - Kartu kalkulator dual-field dengan tombol swap rotasi 180°, satuan gr/mg, validasi min Rp10.000 & batas saldo emas
 * - Tombol aksi inline ("Beli emas" / "Jual emas")
 * - Konsumsi otomatis draft prefill dari simulasi Beranda (`TradePrefillDraft`)
 */
@Composable
fun TradeScreen(
    env: AppEnv,
    initialMode: String = "beli",
    tradePrefill: TradePrefillDraft? = null,
    onOpenLedger: () -> Unit = {},
    onOpenPendingTx: (String) -> Unit = {},
    onCheckout: (isBuy: Boolean, quoteId: String) -> Unit = { _, _ -> },
    onStartKyc: () -> Unit = {},
    onPendingKycDrawer: () -> Unit = {},
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var isBuy by remember { mutableStateOf(initialMode != "jual") }
    var wTop by remember { mutableStateOf(initialMode == "jual") } // Jual emas default input berat di atas
    var unit by remember { mutableStateOf("gr") } // "gr" | "mg"
    var rpText by remember { mutableStateOf("") }
    var wText by remember { mutableStateOf("") }
    var isTypingRp by remember { mutableStateOf(!wTop) }

    // Konsumsi prefill dari Beranda jika ada & sinkronkan data pasar dan portofolio terbaru
    LaunchedEffect(Unit) {
        try {
            env.repo.fetchGoldPrice()
            env.repo.fetchWallet()
            env.repo.fetchTokens()
        } catch (_: Exception) {}
        tradePrefill?.consume()?.let { (pMode, pUnit, pValue) ->
            isBuy = (pMode != "jual")
            if (pUnit == "rp") {
                wTop = false
                isTypingRp = true
                rpText = pValue
            } else {
                wTop = true
                isTypingRp = false
                wText = pValue.replace('.', ',')
                unit = "gr"
            }
        }
    }

    // Refresh harga emas
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "refreshAngle",
    )

    // Hitung mundur live timer untuk transaksi aktif pertama (jika pending)
    val activeTransactions = remember(state.transactions) {
        env.repo.getActiveTransactions(listOf(TxType.BUY, TxType.SELL))
    }
    val activeCount = activeTransactions.size
    val firstActive = activeTransactions.firstOrNull()

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val firstExpiresAt = firstActive?.expiresAtMs
    LaunchedEffect(firstExpiresAt) {
        if (firstActive?.status == "pending" && firstExpiresAt != null) {
            while (true) {
                currentTimeMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val activeTimerText = remember(firstActive, currentTimeMs) {
        val expMs = firstActive?.expiresAtMs
        if (firstActive != null && firstActive.status == "pending" && expMs != null) {
            val leftSec = ((expMs - currentTimeMs).coerceAtLeast(0L) / 1000L).toInt()
            val mm = (leftSec / 60).toString().padStart(2, '0')
            val ss = (leftSec % 60).toString().padStart(2, '0')
            "$mm:$ss"
        } else ""
    }

    val hasPriceDot = remember(state.priceUpdatedAtMs, currentTimeMs) {
        state.priceUpdatedAtMs > 0 && (currentTimeMs - state.priceUpdatedAtMs > 10_000L)
    }

    // Perhitungan kalkulator
    val currentPrice = if (isBuy) state.askPricePerGram else state.buybackPerGram
    val calc = remember(isBuy, currentPrice, isTypingRp, rpText, wText, unit) {
        if (isTypingRp) {
            val subtotal = rpText.filter { it.isDigit() }.toLongOrNull() ?: 0L
            calculateTradeAmounts(isBuy = isBuy, pricePerGram = currentPrice, inputSubtotal = subtotal)
        } else {
            val rawDouble = wText.replace(',', '.').toDoubleOrNull() ?: 0.0
            val grams = if (unit == "mg") rawDouble / 1000.0 else rawDouble
            calculateTradeAmounts(isBuy = isBuy, pricePerGram = currentPrice, inputGrams = grams)
        }
    }

    // Nilai terhitung untuk bidang otomatis (read-only)
    val calculatedRpDisplay = remember(calc.subtotal) {
        if (calc.subtotal > 0L) calc.subtotal.toString() else ""
    }
    val calculatedWDisplay = remember(calc.grams, unit) {
        if (calc.grams > 0.0) {
            if (unit == "mg") {
                val mgVal = calc.grams * 1000.0
                String.format(Locale.GERMAN, "%.1f", mgVal)
            } else {
                String.format(Locale.GERMAN, "%.4f", calc.grams)
            }
        } else ""
    }

    // Validasi
    val hasEnteredValue = if (isTypingRp) rpText.isNotBlank() && rpText.any { it.isDigit() } else wText.isNotBlank()
    val isAmountTooLow = hasEnteredValue && calc.subtotal < MIN_TRADE_RP
    val isGoldInsufficient = !isBuy && hasEnteredValue && calc.grams > state.freeGoldGrams

    val validationError = when {
        isAmountTooLow -> "Minimal transaksi ${rupiah(MIN_TRADE_RP)}"
        isGoldInsufficient -> "Saldo emas tidak mencukupi"
        else -> null
    }

    val isCtaEnabled = hasEnteredValue && validationError == null && calc.subtotal >= MIN_TRADE_RP

    // Rotasi tombol swap
    val swapRotation by animateFloatAsState(
        targetValue = if (wTop) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "swapRot",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Sticky Header Transaksi 44dp (`Main.dc.html` L1486–1492)
            StickyTabHeader(
                title = "Transaksi",
                isTabHeader = true,
                scrollState = scrollState,
                rightAction = {
                    GoldinIconButton(
                        icon = PrototypeIcons.HistoryClock,
                        contentDescription = "Riwayat transaksi",
                        onClick = onOpenLedger,
                        badgeCount = activeCount,
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .border(GoldinBorder.Default, Palette.Line, CircleShape),
                    )
                },
            )

            // Konten Scrollable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = GoldinSpace.Lg)
                    .padding(top = GoldinSpace.Sm, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                // 2. Banner Transaksi Aktif (`Main.dc.html` L1494–1504)
                if (activeCount > 0 && firstActive != null) {
                    val bannerTitle = if (activeCount == 1) firstActive.title else "$activeCount transaksi aktif"
                    val bannerSub = if (activeCount == 1) {
                        "${firstActive.type.label} · ${rupiah(firstActive.effectiveTotal)}"
                    } else "Lanjutkan dari Riwayat transaksi"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Palette.WarnSoft)
                            .border(GoldinBorder.Default, Palette.WarnBorder, RoundedCornerShape(GoldinRadius.Xl))
                            .clickable {
                                if (activeCount == 1) onOpenPendingTx(firstActive.id)
                                else onOpenLedger()
                            }
                            .padding(horizontal = GoldinSpace.Md, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.Clock,
                                contentDescription = null,
                                tint = Palette.Warn,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bannerTitle,
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Palette.Ink,
                            )
                            Text(
                                text = bannerSub,
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Palette.InkSecondary,
                                maxLines = 1,
                            )
                        }
                        if (activeTimerText.isNotBlank()) {
                            Text(
                                text = activeTimerText,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Palette.Warn,
                            )
                        }
                        Icon(
                            imageVector = PrototypeIcons.ChevronRight,
                            contentDescription = null,
                            tint = Palette.InkSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // 3. Kartu Verifikasi Akun (KYC) bila belum verifikasi (`Main.dc.html` L1506–1519)
                val user = state.user
                if (user?.verified != true && user?.isKycPending != true) {
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
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Verifikasi akunmu sekarang",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Palette.Ink,
                                )
                                Text(
                                    text = "Cukup 2 menit untuk mulai beli emas pertamamu.",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Palette.InkSecondary,
                                )
                            }
                        }
                        PrimaryButton(
                            text = "Verifikasi sekarang",
                            onClick = onStartKyc,
                            height = 40.dp,
                        )
                    }
                }

                // 4. Segmented Control Slider Beli / Jual (`Main.dc.html` L1523–1528)
                TradeSlidingSegment(
                    isBuy = isBuy,
                    onSelectBuy = {
                        if (!isBuy) {
                            isBuy = true
                            wTop = false
                            isTypingRp = true
                            rpText = ""
                            wText = ""
                        }
                    },
                    onSelectSell = {
                        if (isBuy) {
                            isBuy = false
                            wTop = true
                            isTypingRp = false
                            rpText = ""
                            wText = ""
                        }
                    },
                )

                // 5. Kartu Harga Hari Ini + Info Saldo (`Main.dc.html` L1530–1542)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Md),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isBuy) "Harga beli per gram" else "Harga jual per gram",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Palette.InkSecondary,
                            )
                            Text(
                                text = rupiah(currentPrice),
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = Palette.Ink,
                            )
                            Text(
                                text = "Diperbarui ${state.priceUpdatedAtLabel}",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Palette.InkSoft,
                            )
                        }
                        val refreshInteraction = remember { MutableInteractionSource() }
                        val refreshPressed by refreshInteraction.collectIsPressedAsState()
                        val refreshHovered by refreshInteraction.collectIsHoveredAsState()
                        val refreshScale by animateFloatAsState(
                            targetValue = if (refreshPressed) 0.94f else 1f,
                            animationSpec = tween(GoldinMotion.DurationPress),
                            label = "refScale",
                        )
                        val refreshBg by animateColorAsState(
                            targetValue = when {
                                refreshPressed -> Palette.Primary100
                                refreshHovered -> Palette.Primary50
                                else -> Color.Transparent
                            },
                            animationSpec = tween(GoldinMotion.DurationColor),
                            label = "refBg",
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer {
                                    scaleX = refreshScale
                                    scaleY = refreshScale
                                }
                                .clip(CircleShape)
                                .background(refreshBg)
                                .clickable(
                                    interactionSource = refreshInteraction,
                                    indication = null,
                                    onClick = {
                                        if (!isRefreshing) {
                                            isRefreshing = true
                                            scope.launch {
                                                env.repo.refreshGoldPrice()
                                                delay(800L)
                                                isRefreshing = false
                                            }
                                        }
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.Refresh,
                                contentDescription = "Refresh harga",
                                tint = Palette.Primary700,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = refreshAngle },
                            )
                            if (hasPriceDot) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-8).dp, y = 8.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Palette.Bad)
                                        .border(1.5.dp, Color.White, CircleShape),
                                )
                            }
                        }
                    }

                    // Baris Saldo Tersedia
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = GoldinSpace.Xs)
                            .height(GoldinBorder.Hairline)
                            .background(Palette.Line),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isBuy) "Saldo kas tersedia" else "Saldo emas tersedia",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                        )
                        Text(
                            text = if (isBuy) rupiah(state.walletBalance)
                            else "${formatGrams4(state.freeGoldGrams)} (≈ ${rupiah((state.freeGoldGrams * state.buybackPerGram).toLong())})",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.Ink,
                        )
                    }
                }

                // 6. Kartu Kalkulator Dual-Field (`Main.dc.html` L1544–1608)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Md),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
                ) {
                    if (!wTop) {
                        // Posisi Normal: Nominal (Atas/Editable) & Berat Emas (Bawah/Otomatis)
                        TradeRpInputField(
                            value = rpText,
                            onValueChange = {
                                isTypingRp = true
                                val cleaned = it.filter { ch -> ch.isDigit() }.take(12)
                                rpText = cleaned
                            },
                            isError = validationError != null,
                            errorMessage = validationError,
                        )

                        // Tombol Swap Rotasi 180°
                        TradeSwapDivider(
                            rotation = swapRotation,
                            onClick = {
                                wTop = true
                                isTypingRp = false
                                if (calc.grams > 0.0) {
                                    wText = calculatedWDisplay
                                }
                            },
                        )

                        TradeWeightInputField(
                            value = if (isTypingRp) calculatedWDisplay else wText,
                            onValueChange = {
                                isTypingRp = false
                                wText = it
                            },
                            unit = unit,
                            onUnitChange = { unit = it },
                            isEditable = false,
                            isError = false,
                            errorMessage = null,
                        )
                    } else {
                        // Posisi Dibalik: Berat Emas (Atas/Editable) & Nominal (Bawah/Otomatis)
                        TradeWeightInputField(
                            value = wText,
                            onValueChange = {
                                isTypingRp = false
                                wText = it
                            },
                            unit = unit,
                            onUnitChange = { unit = it },
                            isEditable = true,
                            isError = validationError != null,
                            errorMessage = validationError,
                        )

                        // Tombol Swap Rotasi 180°
                        TradeSwapDivider(
                            rotation = swapRotation,
                            onClick = {
                                wTop = false
                                isTypingRp = true
                                if (calc.subtotal > 0L) {
                                    rpText = calculatedRpDisplay
                                }
                            },
                        )

                        TradeRpInputField(
                            value = if (!isTypingRp) calculatedRpDisplay else rpText,
                            onValueChange = {
                                isTypingRp = true
                                rpText = it.filter { ch -> ch.isDigit() }.take(12)
                            },
                            isEditable = false,
                            isError = false,
                            errorMessage = null,
                        )
                    }

                    // Tombol Aksi Utama di dalam Kartu
                    val ctaText = if (isBuy) "Beli emas" else "Jual emas"
                    PrimaryButton(
                        text = ctaText,
                        onClick = {
                            if (!isCtaEnabled) return@PrimaryButton
                            if (user?.verified != true) {
                                if (user?.isKycPending == true) onPendingKycDrawer()
                                else onStartKyc()
                                return@PrimaryButton
                            }
                            try {
                                val quote = env.repo.createTradeQuote(
                                    isBuy = isBuy,
                                    inputGrams = calc.grams,
                                    inputSubtotal = calc.subtotal,
                                )
                                onCheckout(isBuy, quote.id)
                            } catch (e: Exception) {
                                env.notify(e.message ?: "Gagal membuat kuotasi transaksi.")
                            }
                        },
                        enabled = isCtaEnabled,
                        height = 44.dp,
                    )
                }
            }
        }
    }
}

/** Segmented control sliding pill: Beli emas / Jual emas (`Main.dc.html` L1523–1528) */
@Composable
private fun TradeSlidingSegment(
    isBuy: Boolean,
    onSelectBuy: () -> Unit,
    onSelectSell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(GoldinRadius.Full))
            .background(Palette.Neutral200)
            .padding(4.dp),
    ) {
        // Sliding thumb pill
        val thumbOffsetFraction by animateFloatAsState(
            targetValue = if (isBuy) 0f else 1f,
            animationSpec = tween(GoldinMotion.DurationModal, easing = FastOutSlowInEasing),
            label = "thumbOffset",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (this.size.width) * thumbOffsetFraction
                }
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(GoldinRadius.Full))
                .clip(RoundedCornerShape(GoldinRadius.Full))
                .background(Color.White),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(GoldinRadius.Full))
                    .clickable(onClick = onSelectBuy),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Beli emas",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isBuy) Palette.Ink else Palette.InkSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(GoldinRadius.Full))
                    .clickable(onClick = onSelectSell),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Jual emas",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (!isBuy) Palette.Ink else Palette.InkSecondary,
                )
            }
        }
    }
}

/** Bidang input nominal Rupiah dengan formatting ribuan otomatis */
@Composable
private fun TradeRpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
    ) {
        Text(
            text = "Nominal",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
        )

        val border = when {
            isError -> Palette.Bad
            !isEditable -> Palette.Line
            else -> Palette.Primary700
        }
        val bg = if (isEditable) Color.White else Palette.Neutral100
        val textColor = if (isEditable) Palette.Ink else Palette.InkSecondary

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .background(bg)
                .border(GoldinBorder.Default, border, RoundedCornerShape(GoldinRadius.Lg))
                .padding(horizontal = GoldinSpace.Md),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Rp",
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Palette.InkSecondary,
                )
                if (isEditable) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = textColor,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = IndonesianThousandsVisualTransformation,
                        cursorBrush = SolidColor(Palette.Primary700),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Palette.InkSoft,
                                )
                            }
                            innerTextField()
                        },
                    )
                } else {
                    val formatted = value.toLongOrNull()?.let { rupiah(it).removePrefix("Rp") } ?: "0"
                    Text(
                        text = formatted,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = textColor,
                    )
                }
            }
        }

        // Pesan Helper / Error
        if (isError && errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = PrototypeIcons.AlertCircle,
                    contentDescription = null,
                    tint = Palette.Bad,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = errorMessage,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Palette.Bad,
                )
            }
        } else if (isEditable) {
            Text(
                text = "Minimal transaksi ${rupiah(MIN_TRADE_RP)}",
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Palette.InkSecondary,
            )
        }
    }
}

/** Bidang input berat emas dengan switcher satuan gr / mg */
@Composable
private fun TradeWeightInputField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Berat emas",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.Ink,
            )
            // Kapsul pemilihan satuan gr / mg
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(GoldinRadius.Full))
                    .background(Palette.Neutral100)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                listOf("gr" to "gr", "mg" to "mg").forEach { (uKey, uLabel) ->
                    val selected = unit == uKey
                    val bg by animateColorAsState(
                        targetValue = if (selected) Color.White else Color.Transparent,
                        animationSpec = tween(GoldinMotion.DurationHover),
                        label = "uBg_$uKey",
                    )
                    val fg by animateColorAsState(
                        targetValue = if (selected) Palette.Primary700 else Palette.InkSecondary,
                        animationSpec = tween(GoldinMotion.DurationHover),
                        label = "uFg_$uKey",
                    )
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 44.dp)
                            .height(32.dp)
                            .then(
                                if (selected) Modifier.shadow(1.dp, RoundedCornerShape(GoldinRadius.Full))
                                else Modifier
                            )
                            .clip(RoundedCornerShape(GoldinRadius.Full))
                            .background(bg)
                            .clickable { onUnitChange(uKey) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uLabel,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = fg,
                        )
                    }
                }
            }
        }

        val border = when {
            isError -> Palette.Bad
            !isEditable -> Palette.Line
            else -> Palette.Primary700
        }
        val bg = if (isEditable) Color.White else Palette.Neutral100
        val textColor = if (isEditable) Palette.Ink else Palette.InkSecondary

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .background(bg)
                .border(GoldinBorder.Default, border, RoundedCornerShape(GoldinRadius.Lg))
                .padding(horizontal = GoldinSpace.Md),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (isEditable) {
                        BasicTextField(
                            value = value,
                            onValueChange = { input ->
                                // Filter format desimal 0,0000 atau 0.0000
                                val cleaned = input.replace('.', ',').filter { ch -> ch.isDigit() || ch == ',' }
                                val parts = cleaned.split(',')
                                val formatted = if (parts.size <= 2) {
                                    val intPart = parts[0].take(if (unit == "mg") 7 else 4)
                                    val decPart = if (parts.size == 2) "," + parts[1].take(if (unit == "mg") 1 else 4) else ""
                                    intPart + decPart
                                } else value
                                onValueChange(formatted)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = textColor,
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            cursorBrush = SolidColor(Palette.Primary700),
                            decorationBox = { innerTextField ->
                                if (value.isEmpty()) {
                                    Text(
                                        text = if (unit == "mg") "0" else "0,0000",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = Palette.InkSoft,
                                    )
                                }
                                innerTextField()
                            },
                        )
                    } else {
                        Text(
                            text = value.ifEmpty { if (unit == "mg") "0" else "0,0000" },
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = textColor,
                        )
                    }
                }
                Text(
                    text = unit,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Palette.InkSecondary,
                )
            }
        }

        // Pesan Error jika input berat tidak valid
        if (isError && errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = PrototypeIcons.AlertCircle,
                    contentDescription = null,
                    tint = Palette.Bad,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = errorMessage,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Palette.Bad,
                )
            }
        }
    }
}

/** Divider tengah dengan tombol swap rotasi 180° (`Main.dc.html` L1554–1558) */
@Composable
private fun TradeSwapDivider(
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(GoldinBorder.Hairline)
                .background(Palette.Line),
        )
        val swapInteraction = remember { MutableInteractionSource() }
        val swapPressed by swapInteraction.collectIsPressedAsState()
        val swapHovered by swapInteraction.collectIsHoveredAsState()
        val swapScale by animateFloatAsState(
            targetValue = if (swapPressed) 0.92f else 1f,
            animationSpec = tween(GoldinMotion.DurationPress),
            label = "swapScale",
        )
        val swapBg by animateColorAsState(
            targetValue = when {
                swapPressed -> Palette.Primary100
                swapHovered -> Palette.Primary100.copy(alpha = 0.5f)
                else -> Palette.Primary50
            },
            animationSpec = tween(GoldinMotion.DurationColor),
            label = "swapBg",
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    interactionSource = swapInteraction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Tukar posisi input",
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = swapScale
                        scaleY = swapScale
                    }
                    .clip(CircleShape)
                    .background(swapBg)
                    .border(1.dp, Palette.Primary200, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PrototypeIcons.ArrowsUpDown,
                    contentDescription = null,
                    tint = Palette.Primary700,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = rotation },
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(GoldinBorder.Hairline)
                .background(Palette.Line),
        )
    }
}
