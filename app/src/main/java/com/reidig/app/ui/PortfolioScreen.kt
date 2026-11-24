package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.app.R
import com.reidig.shared.BankAccount
import com.reidig.shared.Tx
import com.reidig.shared.TxType
import com.reidig.shared.gram
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Utama Portofolio (Screen 20 Portofolio / Route "wallet")
 *
 * Mengimplementasikan spesifikasi Design System v2, Handoff §3 & §9,
 * dan prototipe interaktif `scratch/Main.dc.html` L1910–2052.
 */
@Suppress("DEPRECATION")
@Composable
fun PortfolioScreen(
    env: AppEnv,
    onBuyGold: () -> Unit,
    onRoutinePlans: () -> Unit,
    onInstallmentList: () -> Unit,
    onTopup: () -> Unit,
    onWithdraw: () -> Unit,
    onAddBank: () -> Unit,
    onOpenLedger: (tab: String) -> Unit,
    onOpenReceipt: (txId: String) -> Unit,
    onStartKyc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val user = state.user
    val clipboardManager = LocalClipboardManager.current

    // State untuk PIN & Detail Rekening Drawer
    var bankToVerifyPin by remember { mutableStateOf<BankAccount?>(null) }
    var selectedBankForDetail by remember { mutableStateOf<BankAccount?>(null) }

    // Hitung transaksi aktif (pending pembayaran topup/order atau withdraw in process)
    val activeTxs = remember(state.transactions) {
        state.transactions.filter { it.status == "pending" || it.status == "process" }
    }
    val latestActiveTx = activeTxs.firstOrNull()

    // Perhitungan Emas & Nilai Estimasi
    val goldGrams = state.goldBalanceGrams
    val avgBuyPrice = state.avgBuyPricePerGram
    val bidPrice = state.buybackPerGram
    val estimatedGoldRp = (goldGrams * bidPrice).toLong()

    // Growth calculation jika ada saldo dan avgBuyPrice > 0
    val hasAvg = avgBuyPrice > 0L && goldGrams > 0.0
    val growthPct = if (hasAvg) ((bidPrice - avgBuyPrice).toDouble() / avgBuyPrice.toDouble() * 100.0) else 0.0
    val growthRp = if (hasAvg) (goldGrams * (bidPrice - avgBuyPrice)).toLong() else 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        val lazyListState = rememberLazyListState()

        // Sticky Header 44dp (Main.dc.html L1912-1915, Handoff §3)
        PortfolioHeader(
            activeTxCount = activeTxs.size,
            onOpenLedger = { onOpenLedger("all") },
            lazyListState = lazyListState,
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. Active Transaction Banner (Kuning / Amber)
            if (latestActiveTx != null) {
                item(key = "active_banner") {
                    ActiveTransactionBanner(
                        tx = latestActiveTx,
                        onClick = { onOpenReceipt(latestActiveTx.id) },
                    )
                }
            }

            // 2. e-KYC Promo Card (jika belum verified)
            if (user?.verified != true && user?.isKycPending != true) {
                item(key = "kyc_banner") {
                    KycPromoCard(onStartKyc = onStartKyc)
                }
            }

            // 3. Kartu Tabungan Emas (Main.dc.html L1944-1974)
            item(key = "gold_card") {
                GoldSavingsCard(
                    goldGrams = goldGrams,
                    estimatedRp = estimatedGoldRp,
                    avgBuyPrice = avgBuyPrice,
                    hasAvg = hasAvg,
                    growthPct = growthPct,
                    growthRp = growthRp,
                    lockedGrams = state.lockedGoldGrams,
                    isHidden = state.hidePortfolioGold,
                    onToggleHidden = { env.repo.toggleHidePortfolioGold() },
                    onBuyGold = onBuyGold,
                    onRoutinePlans = onRoutinePlans,
                    onInstallmentList = onInstallmentList,
                )
            }

            // 4. Kartu Saldo Kas (Main.dc.html L1976-1989)
            item(key = "cash_card") {
                CashBalanceCard(
                    walletBalance = state.walletBalance,
                    isHidden = state.hidePortfolioCash,
                    onToggleHidden = { env.repo.toggleHidePortfolioCash() },
                    onTopup = onTopup,
                    onWithdraw = onWithdraw,
                    onOpenHistory = { onOpenLedger("kas") },
                )
            }

            // Separator Hairline
            item(key = "sep_bank") {
                HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)
            }

            // 5. Seksi Rekening Bank (Main.dc.html L1993-2019)
            item(key = "bank_section") {
                BankAccountsSection(
                    bankAccounts = state.bankAccounts,
                    onAddBank = onAddBank,
                    onSelectBank = { bank ->
                        // Buka PIN dialog sebelum membuka detail rekening (Handoff §9 / Main.dc.html L4521)
                        bankToVerifyPin = bank
                    },
                )
            }

            // Separator Hairline
            item(key = "sep_mutasi") {
                HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)
            }

            // 6. Seksi Mutasi Terbaru (Main.dc.html L2023-2050)
            item(key = "mutasi_section") {
                RecentMutationsSection(
                    transactions = state.transactions,
                    onSeeAll = { onOpenLedger("all") },
                    onOpenReceipt = onOpenReceipt,
                )
            }
        }
    }

    // PIN Dialog untuk otorisasi lihat detail rekening
    bankToVerifyPin?.let { bank ->
        PinDialog(
            env = env,
            title = "Detail rekening ${bank.displayLabel}",
            onDismiss = { bankToVerifyPin = null },
            onVerified = {
                val targetBank = bank
                bankToVerifyPin = null
                selectedBankForDetail = targetBank
            },
        )
    }

    // Detail Rekening Bottom Sheet Drawer
    selectedBankForDetail?.let { bank ->
        BankDetailDrawer(
            bank = bank,
            env = env,
            onDismiss = { selectedBankForDetail = null },
            onCopyNumber = {
                clipboardManager.setText(AnnotatedString(bank.number))
                env.notify("Nomor rekening disalin")
            },
            onDelete = {
                env.repo.deleteBankAccount(bank.id)
                env.notify("Rekening dihapus")
                selectedBankForDetail = null
            },
            onAutoDebitToggled = {
                // state is updated in env.repo
            },
        )
    }
}

/**
 * Sticky Header 44dp dengan Judul "Portofolio" di tengah dan
 * tombol jam riwayat transaksi dengan badge transaksi aktif di kanan.
 * Mengimplementasikan semibold body font 18sp, sticky hide on scroll down,
 * reappear on scroll up, dan hairline saat digulir sesuai Handoff §3.
 */
@Composable
private fun PortfolioHeader(
    activeTxCount: Int,
    onOpenLedger: () -> Unit,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var hidden by remember { mutableStateOf(false) }
    var scrolled by remember { mutableStateOf(false) }
    var lastIndex by remember { mutableIntStateOf(0) }
    var lastOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .collect { (idx, off) ->
                if (idx == 0 && off <= 8) {
                    hidden = false
                    scrolled = false
                } else {
                    scrolled = true
                    val dy = if (idx != lastIndex) (idx - lastIndex) * 100 + (off - lastOffset) else off - lastOffset
                    if (dy > 10) hidden = true
                    else if (dy < -6) hidden = false
                }
                lastIndex = idx
                lastOffset = off
            }
    }

    val translateFraction by animateFloatAsState(
        targetValue = if (hidden) -1f else 0f,
        animationSpec = tween(GoldinMotion.DurationModal),
        label = "portHdrHide",
    )
    val borderColor by animateColorAsState(
        targetValue = if (scrolled) Palette.Line else Color.Transparent,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "portHdrBorder",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = size.height * translateFraction }
            .background(Palette.Paper)
            .border(width = 0.5.dp, color = borderColor)
            .height(44.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Portofolio",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Palette.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
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
                animationSpec = tween(GoldinMotion.DurationColor),
                label = "histBtnBg",
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(1.dp, Palette.Line, CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onOpenLedger,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PrototypeIcons.HistoryClock,
                    contentDescription = "Riwayat transaksi",
                    tint = Palette.Ink,
                    modifier = Modifier.size(20.dp),
                )

                if (activeTxCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Palette.Critical)
                            .border(2.dp, Palette.Paper, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = activeTxCount.toString(),
                            color = Color.White,
                            fontFamily = BodyFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Banner transaksi aktif berwarna kuning / amber dengan ikon jam,
 * judul status, subtitle nominal/metode, dan live countdown timer.
 */
@Composable
private fun ActiveTransactionBanner(
    tx: Tx,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "actBannerScale",
    )

    val isPending = tx.status == "pending"
    val title = if (isPending) "Menunggu pembayaran" else "Penarikan diproses"
    val sub = "${tx.method} · ${rupiah(tx.amount)}"

    // Countdown timer untuk transaksi pending (15 menit batas waktu)
    val expiryMs = if ((tx.expiresAtMs ?: 0L) > 0L) (tx.expiresAtMs ?: 0L) else (tx.atMs + 15 * 60 * 1000L)
    var remainingSeconds by remember(tx.id, expiryMs) {
        mutableLongStateOf(maxOf(0L, (expiryMs - System.currentTimeMillis()) / 1000L))
    }

    LaunchedEffect(tx.id, expiryMs, isPending) {
        if (isPending) {
            while (isActive && remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds = maxOf(0L, (expiryMs - System.currentTimeMillis()) / 1000L)
            }
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.WarnSoft)
            .border(1.dp, Palette.Warn.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Ink,
                    lineHeight = 20.sp,
                )
                Text(
                    text = sub,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    color = Palette.InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
            }

            if (isPending) {
                Text(
                    text = timerText,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Warn,
                )
            } else {
                Text(
                    text = "Diproses",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
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
}

/**
 * Kartu Promosi e-KYC untuk pengguna yang belum terverifikasi.
 */
@Composable
private fun KycPromoCard(
    onStartKyc: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Primary50)
            .border(1.dp, Palette.Primary100, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
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
                        imageVector = PrototypeIcons.ShieldCheck,
                        contentDescription = null,
                        tint = Palette.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Verifikasi akunmu sekarang",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                        lineHeight = 20.sp,
                    )
                    Text(
                        text = "Cukup 2 menit untuk mulai beli emas pertamamu.",
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        color = Palette.InkSecondary,
                        lineHeight = 18.sp,
                    )
                }
            }

            PrimaryButton(
                text = "Verifikasi sekarang",
                onClick = onStartKyc,
                modifier = Modifier.fillMaxWidth(),
                height = 40.dp,
            )
        }
    }
}

/**
 * Kartu Tabungan Emas (Main.dc.html L1944-1974)
 */
@Composable
private fun GoldSavingsCard(
    goldGrams: Double,
    estimatedRp: Long,
    avgBuyPrice: Long,
    hasAvg: Boolean,
    growthPct: Double,
    growthRp: Long,
    lockedGrams: Double,
    isHidden: Boolean,
    onToggleHidden: () -> Unit,
    onBuyGold: () -> Unit,
    onRoutinePlans: () -> Unit,
    onInstallmentList: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x140F052C))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header: Icon emas + Title + Eye toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Palette.GoldSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.GoldIngots,
                            contentDescription = null,
                            tint = Palette.Gold,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "Tabungan Emas",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleHidden),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isHidden) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                        contentDescription = if (isHidden) "Tampilkan saldo emas" else "Sembunyikan saldo emas",
                        tint = Palette.InkSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)

            // Balance Display: Grams + Estimated Rp + Growth Line
            Column {
                if (isHidden) {
                    Text(
                        text = "••••••",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = Palette.Ink,
                        lineHeight = 32.sp,
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.forLanguageTag("id-ID"), "%.4f", goldGrams),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            color = Palette.Ink,
                            lineHeight = 32.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "gr",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Palette.Ink,
                            lineHeight = 24.sp,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (isHidden) "≈ Rp••••••" else "≈ ${rupiah(estimatedRp)}",
                        fontFamily = MonoFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                        lineHeight = 20.sp,
                    )

                    if (!isHidden && hasAvg) {
                        val sign = if (growthPct >= 0) "+" else "−"
                        val rpSign = if (growthRp >= 0) "+" else "−"
                        val growthColor = if (growthPct >= 0) Palette.Good700 else Palette.Critical
                        val growthText = "$sign${String.format(Locale.forLanguageTag("id-ID"), "%.2f", Math.abs(growthPct))}% ($rpSign${rupiah(Math.abs(growthRp))})"

                        Text(
                            text = growthText,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = growthColor,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }

            // Optional: Harga Beli Rata-Rata
            if (hasAvg) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Harga beli rata-rata",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                    )
                    Text(
                        text = if (isHidden) "Rp••••••" else "${rupiah(avgBuyPrice)}/gr",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                }
            }

            // Optional: Terkunci (Cicilan) - Klik membuka daftar cicilan
            if (lockedGrams > 0.0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Sm))
                        .clickable(onClick = onInstallmentList)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.Lock,
                            contentDescription = null,
                            tint = Palette.Gold,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Terkunci (cicilan)",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                    Text(
                        text = if (isHidden) "••••••" else "${String.format(Locale.forLanguageTag("id-ID"), "%.4f", lockedGrams)} gr",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                }
            }

            // Grid 3 Tombol Aksi: Beli emas · Nabung rutin · Cicil emas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PortfolioActionButton(
                    label = "Beli emas",
                    icon = PrototypeIcons.Cart,
                    onClick = onBuyGold,
                    modifier = Modifier.weight(1f),
                )
                PortfolioActionButton(
                    label = "Nabung rutin",
                    icon = PrototypeIcons.Repeat,
                    onClick = onRoutinePlans,
                    modifier = Modifier.weight(1f),
                )
                PortfolioActionButton(
                    label = "Cicil emas",
                    icon = PrototypeIcons.Calendar,
                    onClick = onInstallmentList,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Kartu Saldo Kas (Main.dc.html L1976-1989)
 */
@Composable
private fun CashBalanceCard(
    walletBalance: Long,
    isHidden: Boolean,
    onToggleHidden: () -> Unit,
    onTopup: () -> Unit,
    onWithdraw: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x140F052C))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header: Icon dompet + Title + Eye toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
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
                            tint = Palette.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "Saldo Kas",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleHidden),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isHidden) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                        contentDescription = if (isHidden) "Tampilkan Saldo Kas" else "Sembunyikan Saldo Kas",
                        tint = Palette.InkSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)

            // Balance Display
            Text(
                text = if (isHidden) "Rp •••••••" else rupiah(walletBalance),
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = Palette.Ink,
                lineHeight = 32.sp,
            )

            // Grid 3 Tombol Aksi: Top up · Tarik dana · Riwayat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PortfolioActionButton(
                    label = "Top up",
                    icon = PrototypeIcons.Plus,
                    onClick = onTopup,
                    modifier = Modifier.weight(1f),
                )
                PortfolioActionButton(
                    label = "Tarik dana",
                    icon = PrototypeIcons.CashBanknote,
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                )
                PortfolioActionButton(
                    label = "Riwayat",
                    icon = PrototypeIcons.HistoryClock,
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Tombol aksi bergrid 3-kolom dengan feedback tactile scale(0.97f) dan hover/press tint.
 */
@Composable
private fun PortfolioActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "portActionScale",
    )
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary100.copy(alpha = 0.7f)
            else -> Palette.Primary50
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "portActionBg",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, Palette.Primary100, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Palette.Primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Palette.Primary,
                maxLines = 1,
            )
        }
    }
}

/**
 * Seksi Rekening Bank (Main.dc.html L1993-2019)
 */
@Composable
private fun BankAccountsSection(
    bankAccounts: List<BankAccount>,
    onAddBank: () -> Unit,
    onSelectBank: (BankAccount) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rekening bank",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Palette.Ink,
            )

            if (bankAccounts.isNotEmpty()) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val bg by animateColorAsState(
                    targetValue = if (isHovered) Palette.Primary50 else Color.Transparent,
                    animationSpec = tween(GoldinMotion.DurationColor),
                    label = "addBankBg",
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onAddBank,
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = PrototypeIcons.Plus,
                        contentDescription = null,
                        tint = Palette.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Tambah",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Primary,
                    )
                }
            }
        }

        // List Rekening atau Empty State
        if (bankAccounts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(12.dp)),
            ) {
                Column {
                    bankAccounts.forEachIndexed { index, bank ->
                        if (index > 0) {
                            HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val rowBg by animateColorAsState(
                            targetValue = when {
                                isPressed -> Palette.Neutral200
                                isHovered -> Palette.Neutral100
                                else -> Color.Transparent
                            },
                            animationSpec = tween(GoldinMotion.DurationColor),
                            label = "bankRowBg_$index",
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelectBank(bank) },
                                )
                                .padding(start = 4.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Logo Bank 72x40dp
                            GoldinBankLogoBadge(bankName = bank.bank)

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bank.displayLabel,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                                Text(
                                    text = bank.holder,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    color = Palette.InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
                }
            }
        } else {
            // Empty State Rekening Bank
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Palette.Primary50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.BuildingBank,
                            contentDescription = null,
                            tint = Palette.Primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Text(
                        text = "Belum ada rekening. Tambahkan rekening atas namamu untuk menarik dana.",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )

                    SecondaryButton(
                        text = "Tambah rekening",
                        onClick = onAddBank,
                        modifier = Modifier.fillMaxWidth(),
                        height = 40.dp,
                        leadingIcon = PrototypeIcons.Plus,
                    )
                }
            }
        }
    }
}

/**
 * Seksi Mutasi Terbaru (Main.dc.html L2023-2050)
 */
@Composable
private fun RecentMutationsSection(
    transactions: List<Tx>,
    onSeeAll: () -> Unit,
    onOpenReceipt: (txId: String) -> Unit,
) {
    val recentTxs = remember(transactions) {
        transactions.sortedByDescending { it.atMs }.take(3)
    }
    val timeFormat = remember { SimpleDateFormat("d MMM, HH.mm", Locale.forLanguageTag("id-ID")) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mutasi terbaru",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Palette.Ink,
            )

            if (recentTxs.isNotEmpty()) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val bg by animateColorAsState(
                    targetValue = if (isHovered) Palette.Primary50 else Color.Transparent,
                    animationSpec = tween(GoldinMotion.DurationColor),
                    label = "seeAllBg",
                )

                Text(
                    text = "Lihat semua",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onSeeAll,
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        if (recentTxs.isNotEmpty()) {
            Column {
                recentTxs.forEachIndexed { index, tx ->
                    if (index > 0) {
                        HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)
                    }

                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val rowBg by animateColorAsState(
                        targetValue = when {
                            isPressed -> Palette.Neutral200
                            isHovered -> Palette.Neutral100
                            else -> Color.Transparent
                        },
                        animationSpec = tween(GoldinMotion.DurationColor),
                        label = "txRowBg_$index",
                    )

                    // Konfigurasi ikon & warna mutasi sesuai tipe transaksi
                    val isBuy = tx.title.contains("Beli", ignoreCase = true) || tx.type == TxType.BUY
                    val isSell = tx.title.contains("Jual", ignoreCase = true) || tx.type == TxType.SELL
                    val isTopup = tx.title.contains("Top up", ignoreCase = true) || tx.type == TxType.TOPUP
                    val isWithdraw = tx.title.contains("Tarik", ignoreCase = true) || tx.type == TxType.WITHDRAW
                    val isPrint = tx.title.contains("Cetak", ignoreCase = true) || tx.type == TxType.PRINT

                    val (iconBg, iconColor, icon) = when {
                        isTopup -> Triple(Palette.GoodSoft, Palette.Good700, PrototypeIcons.ArrowDownLeft)
                        isWithdraw -> Triple(Palette.WarnSoft, Palette.Warn, PrototypeIcons.ArrowUpRight)
                        isBuy -> Triple(Palette.Primary50, Palette.Primary, PrototypeIcons.Cart)
                        isSell -> Triple(Palette.GoldSoft, Palette.Gold, PrototypeIcons.PriceTag)
                        isPrint -> Triple(Palette.Primary50, Palette.Primary, PrototypeIcons.GoldIngots)
                        else -> Triple(Palette.Neutral100, Palette.InkSecondary, PrototypeIcons.ArrowsExchange)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onOpenReceipt(tx.id) },
                            )
                            .padding(vertical = 12.dp, horizontal = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Icon 40dp Circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Title & Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${timeFormat.format(Date(tx.atMs))}${if (tx.method.isNotBlank()) " · ${tx.method}" else ""}",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = Palette.InkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Nominal & Status Pill
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            val isPositive = isTopup || isBuy
                            val mainText = when {
                                tx.grams > 0.0 -> "${if (isBuy) "+" else "−"}${String.format(Locale.forLanguageTag("id-ID"), "%.4f gr", tx.grams)}"
                                tx.amount != 0L -> "${if (tx.amount > 0) "+" else ""}${rupiah(tx.amount)}"
                                else -> "–"
                            }
                            val mainColor = when {
                                isTopup -> Palette.Good700
                                isWithdraw -> Palette.Ink
                                isBuy -> Palette.Ink
                                else -> Palette.Ink
                            }

                            Text(
                                text = mainText,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = mainColor,
                            )

                            // Status Pill jika bukan ok
                            when (tx.status) {
                                "pending" -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Palette.WarnSoft)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "Menunggu",
                                            fontFamily = BodyFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Palette.Warn,
                                        )
                                    }
                                }
                                "process" -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Palette.Info50)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "Diproses",
                                            fontFamily = BodyFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Palette.Info700,
                                        )
                                    }
                                }
                                "fail" -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Palette.CriticalSoft)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "Gagal",
                                            fontFamily = BodyFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Palette.Critical,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Empty State Mutasi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Palette.Primary50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.ReceiptMoney,
                            contentDescription = null,
                            tint = Palette.Primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Text(
                        text = "Belum ada mutasi. Top up dan transaksi emasmu akan tercatat di sini.",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}



@ScreenPreview
@Composable
private fun PortfolioPreview() = ReidigTheme {
    PortfolioScreen(
        env = rememberPreviewEnv(),
        onBuyGold = {},
        onRoutinePlans = {},
        onInstallmentList = {},
        onTopup = {},
        onWithdraw = {},
        onAddBank = {},
        onOpenLedger = {},
        onOpenReceipt = {},
        onStartKyc = {},
    )
}
