package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.app.R
import com.reidig.shared.MockRepository
import com.reidig.shared.PaymentMethod
import com.reidig.shared.PRICE_LOCK_SECONDS
import com.reidig.shared.TradeQuote
import com.reidig.shared.formatGrams4
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Layar `14 Transaksi · Konfirmasi` (Checkout) sesuai prototipe Goldin v1.2 (`Main.dc.html` L1613–1680) & Handoff §7.
 * Fitur utama:
 * - Top bar 44dp tombol kembali & judul tengah "Konfirmasi Pembelian" / "Konfirmasi Penjualan"
 * - Ring melingkar 44dp timer 30 detik pengunci harga (ungu >10s, kuning ≤10s, expired 0s)
 * - Tombol "Refresh harga" beranimasi putar 800ms saat expired
 * - Kartu rincian order (Detail emas, Subtotal, Biaya admin Rp1.000, Pajak PPh 22 0,25%, Total bayar / diterima)
 * - Radiogroup metode pembayaran (Saldo Kas, VA BCA, VA BRI, VA Mandiri, VA BNI, QRIS, Cicil Emas)
 * - PIN Drawer bottom sheet 6 digit dengan keypad in-sheet, animasi pop dot, caret berkedip & shake animation
 * - Overlay fullscreen "Mengirim order ke Bursa & Kliring…" (1.6 detik) sebelum diarahkan ke Pay / Done Screen
 */
@Composable
fun TradeCheckoutScreen(
    env: AppEnv,
    quoteId: String,
    onBack: () -> Unit,
    onPayWait: (txId: String) -> Unit,
    onDone: (txId: String) -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var currentQuote by remember { mutableStateOf(env.repo.getTradeQuote(quoteId)) }

    // Jika quote tidak ditemukan, kembali
    if (currentQuote == null) {
        LaunchedEffect(Unit) {
            env.notify("Data transaksi tidak ditemukan atau sudah kedaluwarsa.")
            onBack()
        }
        Box(Modifier.fillMaxSize().background(Palette.Paper))
        return
    }

    val quote = currentQuote!!
    val isBuy = quote.isBuy

    // Hitung mundur live timer 30 detik
    var timeLeftSec by remember(quote.expiresAtMs) {
        val remaining = ((quote.expiresAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
        mutableIntStateOf(remaining)
    }

    LaunchedEffect(quote.expiresAtMs) {
        while (timeLeftSec > 0) {
            delay(1000L)
            timeLeftSec = ((quote.expiresAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
        }
    }

    val isLockExpired = timeLeftSec <= 0
    var isRefreshingQuote by remember { mutableStateOf(false) }
    val refreshAngle by animateFloatAsState(
        targetValue = if (isRefreshingQuote) 360f else 0f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "quoteRefreshAngle",
    )

    // Pemilihan metode pembayaran (khusus beli)
    val canPayWithWallet = state.walletBalance >= quote.total
    var selectedMethod by remember {
        mutableStateOf(if (canPayWithWallet) PaymentMethod.WALLET else PaymentMethod.VA_BCA)
    }

    var showPinDrawer by remember { mutableStateOf(false) }
    var isSubmittingOrder by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // 1. Top Bar 44dp (`Main.dc.html` L1615)
            TopTitleBar(
                title = if (isBuy) "Konfirmasi Pembelian" else "Konfirmasi Penjualan",
                onBack = onBack,
            )

            // Konten Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                // 2. Kotak Status Price-Lock 30 Detik (`Main.dc.html` L1617–1631)
                val lockBoxBg by animateColorAsState(
                    targetValue = if (isLockExpired || timeLeftSec <= 10) Palette.WarnSoft else Palette.Primary50,
                    label = "lockBoxBg",
                )
                val lockBoxBorder by animateColorAsState(
                    targetValue = if (isLockExpired || timeLeftSec <= 10) Palette.WarnBorder else Palette.Primary100,
                    label = "lockBoxBorder",
                )
                val lockAccentColor by animateColorAsState(
                    targetValue = if (isLockExpired || timeLeftSec <= 10) Palette.Warn else Palette.Primary700,
                    label = "lockAccentColor",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(lockBoxBg)
                        .border(GoldinBorder.Default, lockBoxBorder, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(horizontal = GoldinSpace.Md, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!isLockExpired) {
                        // Circular Ring 44dp countdown timer
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val sweepFraction = (timeLeftSec / 30f).coerceIn(0f, 1f)
                            val animatedSweep by animateFloatAsState(
                                targetValue = sweepFraction,
                                animationSpec = tween(1000, easing = LinearEasing),
                                label = "sweepAnim",
                            )
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color(0xFFEDE9FE), // Primary100 track
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx()),
                                )
                                drawArc(
                                    color = lockAccentColor,
                                    startAngle = -90f,
                                    sweepAngle = animatedSweep * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                )
                            }
                            Text(
                                text = timeLeftSec.toString(),
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = lockAccentColor,
                            )
                        }
                    } else {
                        // Ikon jam bulat jika expired
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.Clock,
                                contentDescription = null,
                                tint = Palette.Warn,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!isLockExpired) "Harga dikunci 30 detik" else "Waktu penguncian harga habis",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = if (!isLockExpired) "Selesaikan pesanan sebelum waktu habis." else "Harga emas telah diperbarui. Silakan refresh.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }

                // Tombol Refresh Harga saat Expired (`Main.dc.html` L1631)
                if (isLockExpired) {
                    SecondaryButton(
                        text = if (isRefreshingQuote) "Memperbarui harga…" else "Refresh harga",
                        leadingIcon = PrototypeIcons.Refresh,
                        enabled = !isRefreshingQuote,
                        onClick = {
                            if (!isRefreshingQuote) {
                                isRefreshingQuote = true
                                scope.launch {
                                    val refreshed = env.repo.refreshTradeQuote(quote.id)
                                    delay(800L)
                                    currentQuote = refreshed
                                    isRefreshingQuote = false
                                }
                            }
                        },
                        height = 44.dp,
                    )
                }

                // 3. Kartu Rincian Order (`Main.dc.html` L1633–1640)
                val cardOpacity by animateFloatAsState(
                    targetValue = if (isLockExpired) 0.5f else 1f,
                    label = "cardDim",
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = cardOpacity }
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Md),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                ) {
                    // Detail Emas
                    Text(
                        text = "Detail emas",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = Palette.Ink,
                    )
                    TradeDetailRow("Berat emas", formatGrams4(quote.grams))
                    TradeDetailRow("Harga per gram", rupiah(quote.lockedPricePerGram))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(GoldinBorder.Hairline)
                            .background(Palette.Line),
                    )

                    // Rincian Finansial
                    Text(
                        text = if (isBuy) "Rincian pembayaran" else "Rincian penjualan",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = Palette.Ink,
                    )
                    TradeDetailRow("Subtotal", rupiah(quote.subtotal))
                    TradeDetailRow("Biaya admin", if (isBuy) rupiah(quote.adminFee) else "- ${rupiah(quote.adminFee)}")
                    TradeDetailRow("Pajak PPh 22 (0,25%)", if (isBuy) rupiah(quote.tax) else "- ${rupiah(quote.tax)}")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(GoldinBorder.Hairline)
                            .background(Palette.Line),
                    )

                    // Baris Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isBuy) "Total pembayaran" else "Total dana diterima",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = rupiah(quote.total),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Palette.Primary700,
                        )
                    }
                }

                // 4. Metode Pembayaran (Jika Beli) atau Info Dana Masuk (Jika Jual)
                if (isBuy) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = cardOpacity }
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(GoldinSpace.Md),
                        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                    ) {
                        Text(
                            text = "Metode pembayaran",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Palette.Ink,
                        )

                        // 4.1 Saldo Kas
                        TradePaymentMethodRadio(
                            title = "Saldo Kas",
                            subtitle = if (canPayWithWallet) "Saldo ${rupiah(state.walletBalance)}"
                            else "Saldo tidak mencukupi (${rupiah(state.walletBalance)})",
                            isSelected = selectedMethod == PaymentMethod.WALLET,
                            isEnabled = canPayWithWallet,
                            isSubError = !canPayWithWallet,
                            logoContent = {
                                Icon(
                                    imageVector = PrototypeIcons.Wallet,
                                    contentDescription = null,
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onSelect = { selectedMethod = PaymentMethod.WALLET },
                        )

                        // Header VA
                        Text(
                            text = "Transfer Virtual Account",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        // VA BCA
                        TradePaymentMethodRadio(
                            title = "BCA Virtual Account",
                            subtitle = "Konfirmasi otomatis",
                            isSelected = selectedMethod == PaymentMethod.VA_BCA,
                            logoContent = {
                                Image(
                                    painter = painterResource(R.drawable.logo_bca),
                                    contentDescription = "BCA",
                                    modifier = Modifier.size(width = 60.dp, height = 24.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            onSelect = { selectedMethod = PaymentMethod.VA_BCA },
                        )

                        // VA BRI
                        TradePaymentMethodRadio(
                            title = "BRI Virtual Account",
                            subtitle = "Konfirmasi otomatis",
                            isSelected = selectedMethod == PaymentMethod.VA_BRI,
                            logoContent = {
                                Image(
                                    painter = painterResource(R.drawable.logo_bri),
                                    contentDescription = "BRI",
                                    modifier = Modifier.size(width = 60.dp, height = 24.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            onSelect = { selectedMethod = PaymentMethod.VA_BRI },
                        )

                        // VA Mandiri
                        TradePaymentMethodRadio(
                            title = "Mandiri Virtual Account",
                            subtitle = "Konfirmasi otomatis",
                            isSelected = selectedMethod == PaymentMethod.VA_MANDIRI,
                            logoContent = {
                                Image(
                                    painter = painterResource(R.drawable.logo_mandiri),
                                    contentDescription = "Mandiri",
                                    modifier = Modifier.size(width = 60.dp, height = 24.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            onSelect = { selectedMethod = PaymentMethod.VA_MANDIRI },
                        )

                        // VA BNI
                        TradePaymentMethodRadio(
                            title = "BNI Virtual Account",
                            subtitle = "Konfirmasi otomatis",
                            isSelected = selectedMethod == PaymentMethod.VA_BNI,
                            logoContent = {
                                Image(
                                    painter = painterResource(R.drawable.logo_bni),
                                    contentDescription = "BNI",
                                    modifier = Modifier.size(width = 60.dp, height = 24.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            onSelect = { selectedMethod = PaymentMethod.VA_BNI },
                        )

                        // Header E-Wallet / QRIS
                        Text(
                            text = "E-Wallet & QRIS",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        // QRIS
                        TradePaymentMethodRadio(
                            title = "QRIS",
                            subtitle = "Gopay, OVO, Dana, ShopeePay, BCA, dll",
                            isSelected = selectedMethod == PaymentMethod.QRIS,
                            logoContent = {
                                Image(
                                    painter = painterResource(R.drawable.logo_qris),
                                    contentDescription = "QRIS",
                                    modifier = Modifier.size(width = 60.dp, height = 24.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            onSelect = { selectedMethod = PaymentMethod.QRIS },
                        )
                    }
                } else {
                    // Card Dana Masuk ke Saldo Kas (Jika Jual)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = cardOpacity }
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(GoldinSpace.Md),
                        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                    ) {
                        Text(
                            text = "Dana masuk ke",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Palette.Ink,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GoldinRadius.Lg))
                                .background(Palette.Primary50)
                                .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Lg))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Palette.Primary100),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.Wallet,
                                    contentDescription = null,
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saldo Kas",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Palette.Ink,
                                )
                                Text(
                                    text = "Saldo saat ini ${rupiah(state.walletBalance)}",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Palette.InkSecondary,
                                )
                            }
                            Icon(
                                imageVector = PrototypeIcons.CircleCheckFilled,
                                contentDescription = null,
                                tint = Palette.Primary700,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // 5. Sticky Footer CTA Button (`Main.dc.html` L1675–1678)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = GoldinBorder.Hairline, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = GoldinSpace.Lg, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val ctaText = when {
                    !isBuy -> "Konfirmasi penjualan"
                    selectedMethod == PaymentMethod.WALLET -> "Bayar ${rupiah(quote.total)}"
                    else -> "Lanjut ke pembayaran"
                }

                val isCtaEnabled = !isLockExpired && !(isBuy && selectedMethod == PaymentMethod.WALLET && !canPayWithWallet)

                PrimaryButton(
                    text = ctaText,
                    onClick = {
                        if (isCtaEnabled) {
                            showPinDrawer = true
                        }
                    },
                    enabled = isCtaEnabled,
                    height = 44.dp,
                )

                Text(
                    text = "Harga terkunci 30 detik saat konfirmasi.",
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Palette.InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 6. PIN Drawer Sheet (`Sheet · PIN transaksi` `Main.dc.html` L3041–3067)
        if (showPinDrawer) {
            TradePinDrawer(
                onDismiss = { showPinDrawer = false },
                onPinConfirmed = {
                    showPinDrawer = false
                    isSubmittingOrder = true
                    scope.launch {
                        delay(1600L) // Simulasi "Mengirim order ke Bursa & Kliring…"
                        val tx = env.repo.submitTradeOrder(quote.id, selectedMethod)
                        isSubmittingOrder = false
                        if (tx.status == "ok") {
                            onDone(tx.id)
                        } else {
                            onPayWait(tx.id)
                        }
                    }
                },
            )
        }

        // 7. Fullscreen Overlay "Mengirim order ke Bursa & Kliring…" (`Main.dc.html` L3069–3075)
        if (isSubmittingOrder) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Palette.Paper),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 40.dp),
                ) {
                    GoldinSpinner(
                        size = 40.dp,
                        color = Palette.Primary700,
                    )
                    Text(
                        text = "Mengirim order ke Bursa & Kliring…",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Palette.Ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Jangan tutup aplikasi sampai proses selesai.",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Palette.InkSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Baris detail order k/v pair */
@Composable
private fun TradeDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.InkSecondary,
        )
        Text(
            text = value,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
            textAlign = TextAlign.End,
        )
    }
}

/** Item radio metode pembayaran (`Main.dc.html` L1648–1658) */
@Composable
private fun TradePaymentMethodRadio(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isSubError: Boolean = false,
    logoContent: @Composable () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Palette.Primary700 else Palette.Line,
        label = "radioBorder",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Palette.Primary50 else Color.White,
        label = "radioBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Lg))
            .background(bgColor)
            .border(GoldinBorder.Default, borderColor, RoundedCornerShape(GoldinRadius.Lg))
            .clickable(enabled = isEnabled, onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(GoldinBorder.Hairline, Palette.Line, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            logoContent()
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = if (isEnabled) Palette.Ink else Palette.InkSoft,
            )
            Text(
                text = subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = if (isSubError) Palette.Bad else Palette.InkSecondary,
            )
        }

        // Lingkaran Radio Button
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Palette.Primary700 else Palette.Line,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Palette.Primary700),
                )
            }
        }
    }
}

/**
 * Bottom Sheet PIN Transaksi (`Sheet · PIN transaksi` `Main.dc.html` L3041–3067).
 * - Keypad 3x4 in-sheet (1-9, 0, Backspace).
 * - 6 kotak indikator PIN dengan dot animasi & blinking caret di kotak aktif.
 * - Shake animation saat PIN salah.
 */
@Composable
fun TradePinDrawer(
    onDismiss: () -> Unit,
    onPinConfirmed: () -> Unit,
) {
    val drawerScope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shakeOffset by remember { mutableStateOf(0f) }

    val animatedShakeOffset by animateFloatAsState(
        targetValue = shakeOffset,
        animationSpec = tween(60, easing = LinearEasing),
        label = "pinShakeAnim",
    )

    fun handleDigit(digit: String) {
        if (pin.length < 6 && !isChecking) {
            val updated = pin + digit
            pin = updated
            errorMessage = null
            if (updated.length == 6) {
                isChecking = true
                // Demo PIN verifikasi (menerima PIN demo atau PIN apa pun untuk testing kecuali '000000')
                if (updated == "000000") {
                    isChecking = false
                    errorMessage = "PIN transaksi salah. Silakan coba lagi."
                    pin = ""
                    drawerScope.launch {
                        shakeOffset = -8f
                        delay(60)
                        shakeOffset = 8f
                        delay(60)
                        shakeOffset = -6f
                        delay(60)
                        shakeOffset = 6f
                        delay(60)
                        shakeOffset = 0f
                    }
                } else {
                    onPinConfirmed()
                }
            }
        }
    }

    fun handleBackspace() {
        if (pin.isNotEmpty() && !isChecking) {
            pin = pin.dropLast(1)
            errorMessage = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // cegah dismiss saat sheet disentuh
                    )
                    .clip(RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = GoldinSpace.Lg, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(Palette.Line),
                )

                // Header Sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Masukkan PIN",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = Palette.Ink,
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.XClose,
                            contentDescription = "Tutup",
                            tint = Palette.InkSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // 6 Kotak Indikator PIN dengan Pop Animation & 4-Cycle Shake
                Row(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .offset(x = animatedShakeOffset.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(6) { index ->
                        val hasValue = index < pin.length
                        val isCurrent = index == pin.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (hasValue) 1f else 0.3f,
                            animationSpec = tween(160, easing = FastOutSlowInEasing),
                            label = "dotPop_$index",
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(GoldinRadius.Lg))
                                .background(Color.White)
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) Palette.Primary700 else Palette.Line,
                                    shape = RoundedCornerShape(GoldinRadius.Lg),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hasValue) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                                        .clip(CircleShape)
                                        .background(Palette.Primary700),
                                )
                            }
                        }
                    }
                }

                // Status Text / Pesan Error
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.Bad,
                            textAlign = TextAlign.Center,
                        )
                    } else if (isChecking) {
                        Text(
                            text = "Memeriksa PIN…",
                            fontFamily = BodyFontFamily,
                            fontSize = 13.sp,
                            color = Palette.InkSecondary,
                        )
                    } else {
                        Text(
                            text = "Lupa PIN?",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Palette.Primary700,
                            modifier = Modifier.clickable { /* Hook Lupa PIN */ },
                        )
                    }
                }

                // Numeric Keypad 3x4 (`Main.dc.html` L3060–3064) dengan feedback tactile scale(0.94) & Primary100 bg
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "DEL"),
                    )

                    keyRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { key ->
                                KeypadDigitButton(
                                    key = key,
                                    onClick = {
                                        if (key == "DEL") handleBackspace()
                                        else if (key.isNotBlank()) handleDigit(key)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tombol keypad dengan feedback taktil scale 0.94f dan background Primary100 saat ditekan (`Main.dc.html` L3062) */
@Composable
private fun KeypadDigitButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && key.isNotBlank()) 0.94f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "kpScale",
    )
    val bg by animateColorAsState(
        targetValue = when {
            isPressed && key.isNotBlank() -> Palette.Primary100
            isHovered && key.isNotBlank() -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "kpBg",
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = key.isNotBlank(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (key == "DEL") {
            Icon(
                imageVector = PrototypeIcons.Backspace,
                contentDescription = "Hapus",
                tint = Palette.Ink,
                modifier = Modifier.size(24.dp),
            )
        } else if (key.isNotBlank()) {
            Text(
                text = key,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                color = Palette.Ink,
            )
        }
    }
}
