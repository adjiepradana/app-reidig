package com.reidig.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.app.R
import com.reidig.shared.GoldinInstallmentContract
import com.reidig.shared.InstallmentScheduleItem
import com.reidig.shared.MockRepository
import com.reidig.shared.PaymentMethod
import com.reidig.shared.formatGrams4
import com.reidig.shared.parseDateComponents
import com.reidig.shared.rupiah

private val MONTH_NAMES_ID = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

fun formatDueDateIndonesian(ms: Long): String {
    if (ms <= 0L) return "–"
    val comp = parseDateComponents(ms)
    val monthName = MONTH_NAMES_ID.getOrElse(comp.month - 1) { "Bln" }
    return "${comp.day} $monthName ${comp.year}"
}

// =========================================================================================
// SCREEN 46: DAFTAR CICILAN EMAS (`46 Cicilan` / `ccList` / `cicil/list`)
// =========================================================================================

/**
 * Layar Daftar Cicilan Emas sesuai prototipe Goldin v1.2 (`Main.dc.html` L2395–2404) & Handoff §11.
 * Fitur:
 * - Top title bar "Cicilan" dengan tombol kembali
 * - Banner total emas terkunci (hanya tampil saat lockedGoldGrams > 0)
 * - Tab switcher filter status: "Semua", "Aktif", "Lunas"
 * - Card list cicilan: gramatur, pill status (Aktif/Lunas), angsuran/bln, progress bar, tanggal jatuh tempo
 * - Empty state jika belum ada cicilan aktif/riwayat
 * - Bottom action bar "+ Ajukan cicil emas"
 */
@Composable
fun InstallmentListScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onOpenDetail: (contractId: String) -> Unit,
    onCreateInstallment: () -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    var selectedFilterIndex by rememberSaveable { mutableIntStateOf(0) } // 0: Semua, 1: Aktif, 2: Lunas

    val allContracts = state.installmentContracts
    val filteredContracts = remember(allContracts, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> allContracts.filter { !it.isDone }
            2 -> allContracts.filter { it.isDone }
            else -> allContracts
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // 1. Top App Bar
        TopTitleBar(
            title = "Cicilan",
            onBack = onBack,
        )

        // 2. Konten List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = GoldinSpace.Xl,
                end = GoldinSpace.Xl,
                top = GoldinSpace.Md,
                bottom = if (allContracts.isNotEmpty()) 100.dp else GoldinSpace.Xl,
            ),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
        ) {
            // 2.1 Banner Ringkasan Emas Terkunci (jika ada)
            if (state.lockedGoldGrams > 0.0) {
                item {
                    LockedGoldSummaryCard(lockedGrams = state.lockedGoldGrams)
                }
            }

            // 2.2 Filter Status Tab (jika memiliki kontrak)
            if (allContracts.isNotEmpty()) {
                item {
                    InstallmentFilterTabs(
                        selectedIndex = selectedFilterIndex,
                        onSelect = { selectedFilterIndex = it },
                        totalAll = allContracts.size,
                        totalActive = allContracts.count { !it.isDone },
                        totalDone = allContracts.count { it.isDone },
                    )
                }
            }

            // 2.3 Daftar Item Kontrak
            if (filteredContracts.isNotEmpty()) {
                items(filteredContracts, key = { it.id }) { contract ->
                    InstallmentContractCard(
                        contract = contract,
                        onClick = { onOpenDetail(contract.id) },
                    )
                }
            } else if (allContracts.isNotEmpty()) {
                // Empty filter state
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(GoldinSpace.Xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                    ) {
                        Text(
                            text = "Tidak ada cicilan pada tab ini",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            } else {
                // 2.4 Empty State Penuh (Belum ada cicilan sama sekali)
                item {
                    InstallmentEmptyState(onCreateClick = onCreateInstallment)
                }
            }
        }

        // 3. Bottom Action Bar (Jika ada kontrak dalam daftar)
        if (allContracts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Palette.Line)
                    .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Md)
                    .navigationBarsPadding(),
            ) {
                PrimaryButton(
                    text = "Ajukan cicil emas",
                    onClick = onCreateInstallment,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Kartu Ringkasan Emas Terkunci di Brankas */
@Composable
private fun LockedGoldSummaryCard(lockedGrams: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(Palette.Primary50)
            .border(1.dp, Palette.Primary200, RoundedCornerShape(GoldinRadius.Xl))
            .padding(GoldinSpace.Lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Palette.Primary100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PrototypeIcons.Lock,
                contentDescription = null,
                tint = Palette.Primary700,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Total emas terkunci",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Palette.Primary800,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatGrams4(lockedGrams),
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Palette.Primary900,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "emas fisik",
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    color = Palette.Primary800,
                )
            }
            Text(
                text = "Tersimpan aman di brankas kliring. Otomatis dilepas ke tabungan saat lunas.",
                fontFamily = BodyFontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Palette.InkSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Tab switcher filter: Semua · Aktif · Lunas */
@Composable
private fun InstallmentFilterTabs(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    totalAll: Int,
    totalActive: Int,
    totalDone: Int,
) {
    val labels = listOf("Semua ($totalAll)", "Aktif ($totalActive)", "Lunas ($totalDone)")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Lg))
            .background(Palette.Neutral200)
            .padding(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = selectedIndex == index
            val bg by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.Transparent,
                label = "filterTabBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Palette.Ink else Palette.InkSecondary,
                label = "filterTabText",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(GoldinRadius.Md))
                    .background(bg)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = BodyFontFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = textColor,
                )
            }
        }
    }
}

/** Item Kartu Kontrak Cicilan Emas (`Main.dc.html` L2399) */
@Composable
private fun InstallmentContractCard(
    contract: GoldinInstallmentContract,
    onClick: () -> Unit,
) {
    val isDone = contract.isDone
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardBg = when {
        isPressed -> Palette.Neutral100
        isHovered -> Palette.Neutral50
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(cardBg)
            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(GoldinSpace.Lg),
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
        ) {
            // Icon lock/vault circular
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDone) Palette.Neutral200 else Palette.Gold100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PrototypeIcons.Lock,
                    contentDescription = null,
                    tint = if (isDone) Palette.InkSecondary else Palette.Gold700,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${formatGrams4(contract.goldGrams)} emas",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Palette.Ink,
                    )
                    // Status Pill
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .clip(CircleShape)
                            .background(if (isDone) Palette.Neutral200 else Palette.GoodSoft)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isDone) "Lunas" else "Aktif",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = if (isDone) Palette.InkSecondary else Palette.Good,
                        )
                    }
                }

                Text(
                    text = "${rupiah(contract.monthlyInstallment)}/bulan · ${contract.paidMonths} dari ${contract.tenorMonths} angsuran",
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    color = Palette.InkSecondary,
                )
            }

            Icon(
                imageVector = PrototypeIcons.ChevronRight,
                contentDescription = "Detail",
                tint = Palette.InkSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        // Progress bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LinearProgressIndicator(
                progress = { (contract.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isDone) Palette.Good else Palette.Primary700,
                trackColor = Palette.Neutral200,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isDone) "Pelunasan selesai"
                    else {
                        val nextItem = contract.nextDueItem
                        if (nextItem != null) "Jatuh tempo: ${formatDueDateIndonesian(nextItem.dueDateMs)}"
                        else "Semua angsuran dibayar"
                    },
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                    color = Palette.InkSoft,
                )
                Text(
                    text = "${contract.progressPercent.toInt()}%",
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = if (isDone) Palette.Good else Palette.Primary700,
                )
            }
        }
    }
}

/** Tampilan Kosong Belum Ada Cicilan (`Main.dc.html` L2400) */
@Composable
private fun InstallmentEmptyState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(Color.White)
            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
            .padding(vertical = 32.dp, horizontal = GoldinSpace.Xl),
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
                imageVector = PrototypeIcons.Lock,
                contentDescription = null,
                tint = Palette.Primary700,
                modifier = Modifier.size(28.dp),
            )
        }

        Text(
            text = "Belum ada cicilan.",
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Miliki emas batangan sekarang, cicil dengan margin syariah flat 0,9%/bln dari mitra resmi BSI Finance.",
            fontFamily = BodyFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Palette.InkSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        SecondaryButton(
            text = "+ Ajukan cicil emas",
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// =========================================================================================
// SCREEN 47: DETAIL KONTRAK CICILAN EMAS (`47 Cicilan · Detail` / `ccDetail` / `cicil/detail`)
// =========================================================================================

/**
 * Layar Detail Cicilan Emas sesuai prototipe Goldin v1.2 (`Main.dc.html` L2406–2420) & Handoff §11.
 * Fitur:
 * - Top title bar "Detail cicilan" dengan tombol kembali
 * - Kartu Status Emas Terkunci (atau "Emas sudah bisa digunakan" jika lunas)
 * - Kartu Rincian Pembiayaan (Angsuran/bln, Sisa pembayaran, Tanggal jatuh tempo, Pembayaran, Mitra)
 * - Kartu Jadwal Angsuran: Daftar bulan ke-1..N dengan status pill Lunas / Belum dibayar
 * - Fixed Bottom Action Bar: Tombol "Bayar angsuran" (hanya muncul saat aktif & belum lunas)
 * - Bottom Sheet Drawer "Bayar angsuran ke-X" dengan pilihan Saldo Kas, Virtual Account, QRIS
 * - Dialog PIN Transaksi 6 Digit
 * - Pelepasan emas otomatis ke tabungan aktif saat angsuran terakhir lunas
 */
@Composable
fun InstallmentDetailScreen(
    env: AppEnv,
    contractId: String,
    onBack: () -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val contract = state.installmentContracts.firstOrNull { it.id == contractId }
    val context = LocalContext.current

    var showPaySheet by remember { mutableStateOf(false) }
    var showPayoffModal by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var isPayoffAction by remember { mutableStateOf(false) }
    var selectedPayMethod by rememberSaveable { mutableStateOf<PaymentMethod>(PaymentMethod.WALLET) }

    BackHandler { onBack() }

    if (contract == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Paper)
                .statusBarsPadding()
                .padding(GoldinSpace.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Kontrak cicilan tidak ditemukan.",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Palette.Ink,
            )
            Spacer(Modifier.height(GoldinSpace.Lg))
            PrimaryButton(text = "Kembali", onClick = onBack)
        }
        return
    }

    val isDone = contract.isDone
    val nextItem = contract.nextDueItem

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // 1. Top Bar
        TopTitleBar(
            title = "Detail cicilan",
            onBack = onBack,
        )

        // 2. Konten Scrollable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = GoldinSpace.Xl,
                end = GoldinSpace.Xl,
                top = GoldinSpace.Md,
                bottom = if (!isDone) 120.dp else GoldinSpace.Xl,
            ),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg),
        ) {
            // 2.1 Kartu 1: Status Emas Terkunci / Emas Siap Pakai
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isDone) "Emas sudah bisa digunakan" else "Emas terkunci",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Palette.InkSecondary,
                        )
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .clip(CircleShape)
                                .background(if (isDone) Palette.Neutral200 else Palette.GoodSoft)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isDone) "Lunas" else "Aktif",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (isDone) Palette.InkSecondary else Palette.Good,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = formatGrams4(contract.goldGrams),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 36.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = "gr",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Palette.Ink,
                        )
                    }

                    // Progress bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { (contract.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (isDone) Palette.Good else Palette.Primary700,
                            trackColor = Palette.Neutral200,
                        )

                        Text(
                            text = "${contract.paidMonths} dari ${contract.tenorMonths} angsuran dibayar (${contract.progressPercent.toInt()}%)",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // 2.2 Kartu 2: Rincian Pembiayaan
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
                        text = "Rincian pembiayaan",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Palette.Ink,
                    )

                    DetailInfoRow("Angsuran per bulan", rupiah(contract.monthlyInstallment), isMono = true)
                    DetailInfoRow(
                        "Sisa pembayaran",
                        if (isDone) "Rp0" else rupiah(contract.remainingPayment),
                        isMono = true,
                    )
                    DetailInfoRow(
                        "Jatuh tempo berikutnya",
                        if (isDone) "–" else if (nextItem != null) formatDueDateIndonesian(nextItem.dueDateMs) else "–",
                        isMono = !isDone,
                    )
                    DetailInfoRow(
                        "Pembayaran angsuran",
                        if (contract.paymentMode == "auto") {
                            val src = if (contract.autoDebitSource == "kas") "Saldo Kas" else "Rekening Bank"
                            "Auto-debit · $src"
                        } else "Bayar manual",
                    )
                    DetailInfoRow("Mitra pembiayaan", contract.partner)
                    DetailInfoRow("Tanggal akad", formatDueDateIndonesian(contract.agreedAtMs))
                    DetailInfoRow("Nomor kontrak", contract.id, isMono = true)
                }
            }

            // 2.3 Kartu 3: Jadwal Angsuran
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(vertical = GoldinSpace.Md),
                ) {
                    Text(
                        text = "Jadwal angsuran",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Palette.Ink,
                        modifier = Modifier.padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Sm),
                    )

                    contract.schedule.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = Palette.Line,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = GoldinSpace.Lg),
                            )
                        }

                        ScheduleItemRow(
                            item = item,
                            modifier = Modifier.padding(horizontal = GoldinSpace.Lg, vertical = 12.dp),
                        )
                    }
                }
            }
        }

        // 3. Fixed Bottom Action Bar (Jika belum lunas)
        if (!isDone) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Palette.Line)
                    .padding(horizontal = GoldinSpace.Xl, vertical = GoldinSpace.Md)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
            ) {
                PrimaryButton(
                    text = "Bayar angsuran ke-${contract.paidMonths + 1}",
                    onClick = {
                        isPayoffAction = false
                        showPaySheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Tombol lunasi dipercepat jika sisa bulan > 1
                if ((contract.tenorMonths - contract.paidMonths) > 1) {
                    SecondaryButton(
                        text = "Lunasi seluruh sisa (${contract.tenorMonths - contract.paidMonths} bulan)",
                        onClick = {
                            isPayoffAction = true
                            showPayoffModal = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // 4. Bottom Sheet: Drawer Bayar Angsuran
    if (showPaySheet && !isDone && nextItem != null) {
        GoldinBottomSheet(
            onDismiss = { showPaySheet = false },
            title = "Bayar angsuran ke-${contract.paidMonths + 1}",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GoldinSpace.Xl)
                    .padding(bottom = GoldinSpace.Xl),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg),
            ) {
                // Info Nominal & Jatuh Tempo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Palette.Primary50)
                        .padding(GoldinSpace.Md),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Nominal angsuran",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.Primary800,
                        )
                        Text(
                            text = "Jatuh tempo ${formatDueDateIndonesian(nextItem.dueDateMs)}",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.Primary800,
                        )
                    }
                    Text(
                        text = rupiah(contract.monthlyInstallment),
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Palette.Primary900,
                    )
                }

                // Pilihan Metode Pembayaran
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                ) {
                    Text(
                        text = "Metode pembayaran",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Palette.Ink,
                    )

                    val isWalletSufficient = state.walletBalance >= contract.monthlyInstallment

                    // 1. Saldo Kas
                    ManagePaymentRadioRow(
                        title = "Saldo Kas",
                        subtitle = if (isWalletSufficient) "Saldo ${rupiah(state.walletBalance)}" else "Saldo tidak mencukupi (${rupiah(state.walletBalance)})",
                        isSelected = selectedPayMethod == PaymentMethod.WALLET,
                        isEnabled = isWalletSufficient,
                        isError = !isWalletSufficient,
                        onClick = { selectedPayMethod = PaymentMethod.WALLET },
                    )

                    // 2. VA BCA
                    ManagePaymentRadioRow(
                        title = "BCA Virtual Account",
                        subtitle = "Konfirmasi otomatis",
                        isSelected = selectedPayMethod == PaymentMethod.VA_BCA,
                        onClick = { selectedPayMethod = PaymentMethod.VA_BCA },
                    )

                    // 3. VA Mandiri
                    ManagePaymentRadioRow(
                        title = "Mandiri Virtual Account",
                        subtitle = "Konfirmasi otomatis",
                        isSelected = selectedPayMethod == PaymentMethod.VA_MANDIRI,
                        onClick = { selectedPayMethod = PaymentMethod.VA_MANDIRI },
                    )

                    // 4. QRIS
                    ManagePaymentRadioRow(
                        title = "QRIS",
                        subtitle = "Gopay, OVO, Dana, ShopeePay, BCA, dll",
                        isSelected = selectedPayMethod == PaymentMethod.QRIS,
                        onClick = { selectedPayMethod = PaymentMethod.QRIS },
                    )
                }

                // Tombol Eksekusi Bayar
                PrimaryButton(
                    text = "Bayar",
                    onClick = {
                        showPaySheet = false
                        showPinDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // 5. Modal Konfirmasi Pelunasan Dipercepat
    if (showPayoffModal && !isDone) {
        val remainingMonths = contract.tenorMonths - contract.paidMonths
        val payoffTotal = contract.remainingPayment

        GoldinModal(
            title = "Lunasi seluruh cicilan?",
            message = "Sisa $remainingMonths bulan angsuran total ${rupiah(payoffTotal)}. Emas ${formatGrams4(contract.goldGrams)} fisik akan otomatis dilepas ke portofolio aktifmu.",
            confirmLabel = "Lunasi sekarang",
            cancelLabel = "Batal",
            onConfirm = {
                showPayoffModal = false
                if (selectedPayMethod == PaymentMethod.WALLET && state.walletBalance < payoffTotal) {
                    selectedPayMethod = PaymentMethod.QRIS
                }
                showPinDialog = true
            },
            onCancel = { showPayoffModal = false },
        )
    }

    // 6. Dialog PIN Transaksi 6 Digit
    if (showPinDialog) {
        PinDialog(
            env = env,
            title = if (isPayoffAction) "Konfirmasi Pelunasan" else "Konfirmasi Pembayaran Angsuran",
            onDismiss = { showPinDialog = false },
            onVerified = {
                showPinDialog = false
                try {
                    if (isPayoffAction) {
                        env.repo.payoffInstallmentContract(
                            contractId = contract.id,
                            paymentMethod = selectedPayMethod,
                            pin = MockRepository.GOLDIN_DEMO_PIN,
                        )
                        Toast.makeText(context, "Cicilan lunas. Emas sudah bisa digunakan.", Toast.LENGTH_LONG).show()
                    } else {
                        val nextPaid = contract.paidMonths + 1
                        val updated = env.repo.payInstallmentMonthly(
                            contractId = contract.id,
                            paymentMethod = selectedPayMethod,
                            pin = MockRepository.GOLDIN_DEMO_PIN,
                        )
                        if (updated.isDone) {
                            Toast.makeText(context, "Cicilan lunas. Emas sudah bisa digunakan.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Angsuran ke-$nextPaid berhasil dibayar", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "Pembayaran gagal.", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

/** Baris Rincian Key-Value */
@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    isMono: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 14.sp,
            color = Palette.InkSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontFamily = if (isMono) MonoFontFamily else BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Palette.Ink,
            textAlign = TextAlign.End,
        )
    }
}

/** Baris Item Jadwal Angsuran (`Main.dc.html` L2416) */
@Composable
private fun ScheduleItemRow(
    item: InstallmentScheduleItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Angsuran ke-${item.monthNumber}",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Palette.Ink,
            )
            Text(
                text = formatDueDateIndonesian(item.dueDateMs),
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = Palette.InkSecondary,
            )
        }

        Text(
            text = rupiah(item.amount),
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Palette.Ink,
        )

        Box(
            modifier = Modifier
                .height(20.dp)
                .clip(CircleShape)
                .background(if (item.isPaid) Palette.GoodSoft else Palette.Neutral200)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (item.isPaid) "Lunas" else "Belum dibayar",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = if (item.isPaid) Palette.Good else Palette.InkSecondary,
            )
        }
    }
}

/** Radio Item Metode Pembayaran Angsuran di Drawer */
@Composable
private fun ManagePaymentRadioRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    isError: Boolean = false,
) {
    val borderColor = if (isSelected) Palette.Primary700 else Palette.Line
    val bg = if (isSelected) Palette.Primary50 else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Lg))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(GoldinRadius.Lg))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(GoldinSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isEnabled) Palette.Ink else Palette.InkSecondary,
            )
            Text(
                text = subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = if (isError) Palette.Critical else Palette.InkSecondary,
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

// =========================================================================================
// HOMESCREEN INTEGRATION COMPONENTS (Screen 40 / 46 Entry Points)
// =========================================================================================

@Composable
fun HomeInstallmentListSection(
    contracts: List<GoldinInstallmentContract>,
    onSeeAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cicil emas",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Palette.Ink,
            )
            val seeAllInteraction = remember { MutableInteractionSource() }
            val seeAllHovered by seeAllInteraction.collectIsHoveredAsState()
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(GoldinRadius.Sm))
                    .clickable(
                        interactionSource = seeAllInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onSeeAll,
                    )
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Lihat semua",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (seeAllHovered) Palette.Primary800 else Palette.Primary700,
                )
                Icon(
                    imageVector = PrototypeIcons.ChevronRight,
                    contentDescription = null,
                    tint = if (seeAllHovered) Palette.Primary800 else Palette.Primary700,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        contracts.take(2).forEach { contract ->
            HomeInstallmentContractCard(
                contract = contract,
                onClick = onSeeAll,
            )
        }
    }
}

@Composable
private fun HomeInstallmentContractCard(
    contract: GoldinInstallmentContract,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Neutral100
            isHovered -> Palette.Neutral50
            else -> Color.White
        },
        label = "contractCardBg",
    )
    val isDone = contract.isDone
    val pctFraction = (contract.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val pctLabel = "${contract.progressPercent.toInt()}%"

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
                    text = "${formatGrams4(contract.goldGrams)} emas",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(if (isDone) Palette.Neutral200 else Palette.GoodSoft)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isDone) "Lunas" else "Aktif",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (isDone) Palette.InkSecondary else Palette.Good,
                    )
                }
            }

            Text(
                text = "${rupiah(contract.monthlyInstallment)}/bulan · ${contract.paidMonths} dari ${contract.tenorMonths} angsuran",
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Palette.InkSecondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
            ) {
                LinearProgressIndicator(
                    progress = { pctFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full)),
                    color = if (isDone) Palette.Good else Palette.Gold500,
                    trackColor = Palette.Neutral200,
                )
                Text(
                    text = pctLabel,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Palette.InkSecondary,
                )
            }
        }

        Icon(
            imageVector = PrototypeIcons.ChevronRight,
            contentDescription = null,
            tint = Palette.InkSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun HomeInstallmentPromoCard(
    onSimulate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(Color.White)
            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
            .padding(GoldinSpace.Md),
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Palette.Gold100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PrototypeIcons.Calendar,
                    contentDescription = null,
                    tint = Palette.Gold700,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cicil Emas Syariah",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Palette.Ink,
                )
                Text(
                    text = "Mitra BSI Finance · Margin flat 0,9%/bln",
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    color = Palette.InkSecondary,
                )
            }
        }

        Text(
            text = "Miliki emas batangan sekarang dengan DP mulai 10% dan tenor fleksibel 3 hingga 24 bulan.",
            fontFamily = BodyFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Palette.InkSecondary,
        )

        SecondaryButton(
            text = "+ Simulasi cicil emas",
            onClick = onSimulate,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
