package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.Tx
import com.reidig.shared.TxType
import com.reidig.shared.formatGrams4
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar `18 Riwayat transaksi` sesuai prototipe Goldin v1.2 (`Main.dc.html` L1790–1856) & Handoff §8.
 * Fitur utama:
 * - Top header 44dp tombol kembali + tombol filter dengan dot indikator aktif
 * - Tab switcher jenis transaksi horizontal (Semua, Beli, Jual, Cetak, Kas)
 * - Seksi pinned "Transaksi aktif" dengan live timer countdown untuk order pending
 * - Pengelompokan riwayat per bulan (e.g. "November 2026", "Oktober 2026")
 * - Item baris transaksi 40dp icon jenis, judul, status/jam, nominal Geist Mono (warna ink/hijau), pill status
 * - Bottom sheet filter riwayat (`Sheet · Filter riwayat`) dengan filter rentang waktu & status
 * - Empty state jika tidak ada riwayat yang cocok
 */
@Composable
fun LedgerScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onOpenReceipt: (txId: String) -> Unit,
    onOpenPendingPay: (txId: String) -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var selectedTab by remember { mutableStateOf("all") } // "all", "beli", "jual", "cetak", "kas"
    var selectedRangeDays by remember { mutableIntStateOf(0) } // 0 = semua waktu, 30 = 30 hari, 90 = 90 hari
    var selectedStatus by remember { mutableStateOf("all") } // "all", "process", "ok", "fail"

    var showFilterSheet by remember { mutableStateOf(false) }

    val isFilterActive = selectedRangeDays > 0 || selectedStatus != "all"

    // Live timer per detik untuk transaksi aktif
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Ambil daftar transaksi aktif
    val activeTransactions = remember(state.transactions, currentTimeMs) {
        env.repo.getActiveTransactions()
    }

    // Filter transaksi untuk daftar riwayat
    val filteredTransactions = remember(state.transactions, selectedTab, selectedStatus, selectedRangeDays) {
        env.repo.filterTransactions(
            tab = selectedTab,
            statusFilter = selectedStatus,
            rangeDays = selectedRangeDays,
        )
    }

    // Kelompokkan per bulan (e.g. "November 2026")
    val monthGroups = remember(filteredTransactions) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        filteredTransactions.groupBy { tx ->
            sdf.format(Date(tx.atMs))
        }.toList()
    }

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
            // 1. Header 44dp + Tombol Filter (`Main.dc.html` L1793–1801)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = GoldinSpace.Md),
                contentAlignment = Alignment.Center,
            ) {
                // Tombol Kembali
                GoldinIconButton(
                    icon = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                )

                // Judul Tengah
                Text(
                    text = "Riwayat transaksi",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Palette.Ink,
                )

                // Tombol Filter di Kanan
                GoldinIconButton(
                    icon = PrototypeIcons.FilterSliders,
                    contentDescription = "Filter riwayat",
                    onClick = { showFilterSheet = true },
                    tint = if (isFilterActive) Palette.Primary700 else Palette.InkSecondary,
                    showDot = isFilterActive,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

            // 2. Tab Bar Jenis Transaksi (`Main.dc.html` L1795–1799)
            val tabs = listOf(
                "all" to "Semua",
                "beli" to "Beli",
                "jual" to "Jual",
                "cetak" to "Cetak",
                "kas" to "Kas",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GoldinSpace.Lg)
                    .border(width = GoldinBorder.Hairline, color = Palette.Line),
            ) {
                tabs.forEach { (tabKey, tabLabel) ->
                    val isSelected = selectedTab == tabKey
                    val lineColor by animateColorAsState(
                        targetValue = if (isSelected) Palette.Primary700 else Color.Transparent,
                        animationSpec = tween(GoldinMotion.DurationColor),
                        label = "tabLine",
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Palette.Primary700 else Palette.InkSecondary,
                        animationSpec = tween(GoldinMotion.DurationColor),
                        label = "tabText",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { selectedTab = tabKey }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tabLabel,
                            fontFamily = BodyFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textColor,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(lineColor),
                        )
                    }
                }
            }

            // Konten Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 3. Seksi Pinned "Transaksi aktif" (`Main.dc.html` L1804–1824)
                if (activeTransactions.isNotEmpty() && selectedTab in listOf("all", "beli", "jual")) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Transaksi aktif",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                        )

                        activeTransactions.forEach { tx ->
                            val expMs = tx.expiresAtMs
                            val timerString = if (expMs != null && expMs > currentTimeMs) {
                                val leftSec = ((expMs - currentTimeMs) / 1000L).toInt()
                                val mm = (leftSec / 60).toString().padStart(2, '0')
                                val ss = (leftSec % 60).toString().padStart(2, '0')
                                " · $mm:$ss"
                            } else ""

                            val activeRowInteraction = remember { MutableInteractionSource() }
                            val activeRowPressed by activeRowInteraction.collectIsPressedAsState()
                            val activeRowHovered by activeRowInteraction.collectIsHoveredAsState()
                            val activeRowBg by animateColorAsState(
                                targetValue = if (activeRowPressed || activeRowHovered) Palette.WarnSoft.copy(alpha = 0.5f) else Color.White,
                                animationSpec = tween(GoldinMotion.DurationColor),
                                label = "activeRowBg",
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GoldinRadius.Lg))
                                    .background(activeRowBg)
                                    .border(GoldinBorder.Default, Palette.WarnBorder, RoundedCornerShape(GoldinRadius.Lg))
                                    .clickable(
                                        interactionSource = activeRowInteraction,
                                        indication = null,
                                        onClick = {
                                            if (tx.status == "pending") onOpenPendingPay(tx.id)
                                            else onOpenReceipt(tx.id)
                                        },
                                    )
                                    .padding(horizontal = GoldinSpace.Md, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Ikon Bulat 40dp
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Palette.WarnSoft),
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
                                        text = tx.title,
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = Palette.Ink,
                                    )
                                    Text(
                                        text = "Menunggu bayar$timerString",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = Palette.Warn,
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = rupiah(tx.effectiveTotal),
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = Palette.Ink,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(GoldinRadius.Full))
                                            .background(Palette.WarnSoft)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "Menunggu",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = Palette.Warn,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Daftar Riwayat Dikelompokkan per Bulan (`Main.dc.html` L1826–1848)
                if (filteredTransactions.isEmpty() && activeTransactions.isEmpty()) {
                    // Empty State (`Main.dc.html` L1850–1854)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Palette.Primary50),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.HistoryClock,
                                contentDescription = null,
                                tint = Palette.Primary700,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Text(
                            text = "Belum ada transaksi",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "Transaksi yang kamu lakukan akan muncul di sini.",
                            fontFamily = BodyFontFamily,
                            fontSize = 13.sp,
                            color = Palette.InkSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    val timeFormat = remember { SimpleDateFormat("d MMM, HH.mm", Locale("id", "ID")) }

                    monthGroups.forEach { (monthLabel, txList) ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = monthLabel,
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Palette.InkSecondary,
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GoldinRadius.Xl))
                                    .background(Color.White)
                                    .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl)),
                            ) {
                                txList.forEachIndexed { index, tx ->
                                    val isIncome = tx.type == TxType.SELL || tx.type == TxType.TOPUP
                                    val amountPrefix = if (isIncome) "+ " else "- "
                                    val amountColor = if (isIncome) Palette.Good700 else Palette.Ink

                                    LedgerTxRow(
                                        tx = tx,
                                        currentTimeMs = currentTimeMs,
                                        timeFormat = timeFormat,
                                        onOpenPendingPay = onOpenPendingPay,
                                        onOpenReceipt = onOpenReceipt,
                                    )

                                    if (index < txList.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = GoldinSpace.Md)
                                                .height(GoldinBorder.Hairline)
                                                .background(Palette.Line),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Bottom Sheet Filter Riwayat (`Sheet · Filter riwayat` `Main.dc.html` L2900–2954)
        if (showFilterSheet) {
            LedgerFilterDrawer(
                initialRangeDays = selectedRangeDays,
                initialStatus = selectedStatus,
                onDismiss = { showFilterSheet = false },
                onApply = { rangeDays, status ->
                    selectedRangeDays = rangeDays
                    selectedStatus = status
                    showFilterSheet = false
                },
                onReset = {
                    selectedRangeDays = 0
                    selectedStatus = "all"
                    showFilterSheet = false
                },
            )
        }
    }
}

/** Bottom Sheet Filter Riwayat Transaksi (`Main.dc.html` L2900–2954) */
@Composable
private fun LedgerFilterDrawer(
    initialRangeDays: Int,
    initialStatus: String,
    onDismiss: () -> Unit,
    onApply: (rangeDays: Int, status: String) -> Unit,
    onReset: () -> Unit,
) {
    var rangeDays by remember { mutableIntStateOf(initialRangeDays) }
    var status by remember { mutableStateOf(initialStatus) }

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
                        onClick = {},
                    )
                    .clip(RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = GoldinSpace.Lg, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(Palette.Line)
                        .align(Alignment.CenterHorizontally),
                )

                // Header Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Filter",
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

                // Grup Rentang Waktu
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rentang waktu",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            0 to "Semua waktu",
                            30 to "30 hari terakhir",
                            90 to "90 hari terakhir",
                        ).forEach { (rVal, rLabel) ->
                            val selected = rangeDays == rVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                    .background(if (selected) Palette.Primary50 else Color.White)
                                    .border(
                                        1.dp,
                                        if (selected) Palette.Primary700 else Palette.Line,
                                        RoundedCornerShape(GoldinRadius.Full),
                                    )
                                    .clickable { rangeDays = rVal }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = rLabel,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (selected) Palette.Primary700 else Palette.Ink,
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GoldinBorder.Hairline)
                        .background(Palette.Line),
                )

                // Grup Status
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Status",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "all" to "Semua",
                            "process" to "Diproses",
                            "ok" to "Selesai",
                            "fail" to "Gagal",
                        ).forEach { (sKey, sLabel) ->
                            val selected = status == sKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                    .background(if (selected) Palette.Primary50 else Color.White)
                                    .border(
                                        1.dp,
                                        if (selected) Palette.Primary700 else Palette.Line,
                                        RoundedCornerShape(GoldinRadius.Full),
                                    )
                                    .clickable { status = sKey }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = sLabel,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (selected) Palette.Primary700 else Palette.Ink,
                                )
                            }
                        }
                    }
                }

                // Footer Tombol Reset & Terapkan (`Main.dc.html` L2948–2952)
                PairedButtons(
                    leftText = "Reset",
                    onLeftClick = onReset,
                    rightText = "Terapkan",
                    onRightClick = { onApply(rangeDays, status) },
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
        }
    }
}

/**
 * Baris item transaksi riwayat dengan umpan balik visual hover & press sesuai Handoff §3 & `Main.dc.html`.
 */
@Composable
private fun LedgerTxRow(
    tx: Tx,
    currentTimeMs: Long,
    timeFormat: SimpleDateFormat,
    onOpenPendingPay: (String) -> Unit,
    onOpenReceipt: (String) -> Unit,
) {
    val rowInteraction = remember { MutableInteractionSource() }
    val rowPressed by rowInteraction.collectIsPressedAsState()
    val rowHovered by rowInteraction.collectIsHoveredAsState()
    val rowBg by animateColorAsState(
        targetValue = if (rowPressed || rowHovered) Palette.Neutral100 else Color.White,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "rowBg",
    )

    val isIncome = tx.type == TxType.SELL || tx.type == TxType.TOPUP
    val amountPrefix = if (isIncome) "+ " else "- "
    val amountColor = if (isIncome) Palette.Good700 else Palette.Ink

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                onClick = {
                    if (tx.status == "pending" && tx.isAct(currentTimeMs)) {
                        onOpenPendingPay(tx.id)
                    } else {
                        onOpenReceipt(tx.id)
                    }
                },
            )
            .padding(horizontal = GoldinSpace.Md, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ikon bulat 40dp per jenis transaksi
        val iconBg = when (tx.type) {
            TxType.BUY, TxType.ROUTINE_DEBIT -> Palette.Primary50
            TxType.SELL -> Palette.Gold50
            TxType.PRINT -> Palette.Info50
            TxType.TOPUP, TxType.WITHDRAW -> Palette.Neutral100
            else -> Palette.Neutral100
        }
        val iconTint = when (tx.type) {
            TxType.BUY, TxType.ROUTINE_DEBIT -> Palette.Primary700
            TxType.SELL -> Palette.Gold700
            TxType.PRINT -> Palette.Info700
            TxType.TOPUP, TxType.WITHDRAW -> Palette.Ink
            else -> Palette.Ink
        }
        val iconVector = when (tx.type) {
            TxType.BUY, TxType.ROUTINE_DEBIT -> PrototypeIcons.Cart
            TxType.SELL -> PrototypeIcons.PriceTag
            TxType.PRINT -> PrototypeIcons.GoldIngots
            TxType.TOPUP -> PrototypeIcons.ArrowDownLeft
            TxType.WITHDRAW -> PrototypeIcons.ArrowUpRight
            else -> PrototypeIcons.ArrowsExchange
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.Ink,
            )
            Text(
                text = timeFormat.format(Date(tx.atMs)),
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Palette.InkSoft,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "$amountPrefix${rupiah(tx.effectiveTotal)}",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = amountColor,
            )
            if (tx.status != "ok") {
                val (pillBg, pillFg, pillText) = when (tx.status) {
                    "pending", "process" -> Triple(Palette.WarnSoft, Palette.Warn, "Diproses")
                    "fail" -> Triple(Palette.CriticalSoft, Palette.Critical, "Gagal")
                    else -> Triple(Palette.Neutral100, Palette.InkSecondary, tx.status)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(pillBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = pillText,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = pillFg,
                    )
                }
            }
        }
    }
}
