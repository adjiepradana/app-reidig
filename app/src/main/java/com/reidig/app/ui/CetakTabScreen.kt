package com.reidig.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.GoldBarStock
import com.reidig.shared.GoldBranch
import com.reidig.shared.GoldBrand
import com.reidig.shared.PhysicalOrderStatus
import com.reidig.shared.PhysicalPrintOrder
import com.reidig.shared.formatGrams4

/**
 * Screen 30: Tab Utama Cetak Emas (`30 Cetak` / `cetak` / `print-tab`)
 *
 * Mengimplementasikan 100% presisi Design System v2, Dokumen Handoff §12,
 * dan prototipe kanvas `Main.dc.html` (L2514–L2567).
 */
@Composable
fun CetakTabScreen(
    env: AppEnv,
    onStartCetak: (selectedBranchId: String?) -> Unit,
    onOpenBranchCatalog: () -> Unit,
    onOpenTicket: (orderId: String) -> Unit,
    onStartKyc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val user = state.user
    val isKycApproved = user?.verified == true
    val isKycPending = user?.isKycPending == true

    val freeGoldGrams = state.freeGoldGrams
    var branches by remember { mutableStateOf(env.repo.getGoldBranches()) }
    var stockList by remember { mutableStateOf(env.repo.getGoldBarStock()) }
    val nearbyBranches = remember(branches) { branches.sortedBy { it.distanceKm }.take(3) }
    val printOrders = state.physicalOrders

    LaunchedEffect(Unit) {
        try {
            env.repo.fetchCatalogSkus()
            env.repo.fetchBranches()
            branches = env.repo.getGoldBranches()
            stockList = env.repo.getGoldBarStock()
        } catch (_: Exception) {}
    }

    var showAllOrdersSheet by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // Sticky Header 44dp sesuai Handoff §3 (18px judul tab di tengah)
        CetakStickyHeader(lazyListState = lazyListState)

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 0. KYC Prompt jika pengguna belum terverifikasi (ctNeedKyc)
            if (!isKycApproved && !isKycPending) {
                item(key = "kyc_banner") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Palette.Primary50)
                            .border(1.dp, Palette.Primary100, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
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
                                    imageVector = GoldinIcons.ShieldCheck,
                                    contentDescription = null,
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Verifikasi akunmu sekarang",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                                Text(
                                    text = "Cukup 2 menit untuk mulai beli emas pertamamu.",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    color = Palette.InkSecondary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(GoldinRadius.Full))
                                .background(Palette.Primary700)
                                .clickable(onClick = onStartKyc),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Verifikasi sekarang",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Primary50,
                            )
                        }
                    }
                }
            } else if (isKycPending) {
                item(key = "kyc_pending_banner") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Palette.Info50)
                            .border(1.dp, Palette.Info100, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Palette.Info100),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = GoldinIcons.Clock,
                                contentDescription = null,
                                tint = Palette.Info700,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verifikasi sedang ditinjau",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Info700,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Pengajuan e-KYC sedang diproses. Fitur cetak emas akan aktif setelah disetujui.",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = Palette.InkSecondary,
                            )
                        }
                    }
                }
            }

            // 1. Kartu Saldo Emas (Screen 30: ct-bal)
            item(key = "free_gold_card") {
                CetakFreeGoldCard(
                    freeGoldGrams = freeGoldGrams,
                    canPrint = isKycApproved && freeGoldGrams >= 1.0,
                    onStartCetak = { onStartCetak(null) },
                )
            }

            // Garis Pemisah Hairline Antar Seksi
            item(key = "divider_1") {
                HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
            }

            // 2. Seksi Pesanan Cetak (Screen 30: ct-act)
            item(key = "orders_section") {
                CetakOrdersSection(
                    orders = printOrders,
                    onOpenTicket = onOpenTicket,
                    onSeeAll = { showAllOrdersSheet = true },
                )
            }

            // Garis Pemisah Hairline Antar Seksi
            item(key = "divider_2") {
                HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
            }

            // 3. Seksi Brand & Ukuran (Screen 30: ct-stock)
            item(key = "stock_catalog") {
                CetakStockCatalogSection(stockList = stockList)
            }

            // Garis Pemisah Hairline Antar Seksi
            item(key = "divider_3") {
                HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
            }

            // 4. Seksi Cabang Terdekat (Screen 30: ct-br)
            item(key = "nearby_branches") {
                CetakNearbyBranchesSection(
                    branches = nearbyBranches,
                    onBranchClick = { branch ->
                        if (freeGoldGrams >= 1.0 || !isKycApproved) {
                            onStartCetak(branch.id)
                        }
                    },
                    onSeeAll = onOpenBranchCatalog,
                )
            }
        }
    }

    // Sheet Semua Pesanan Cetak (ketika "Lihat semua" ditekan)
    if (showAllOrdersSheet) {
        GoldinBottomSheet(
            title = "Pesanan cetak",
            onDismiss = { showAllOrdersSheet = false },
        ) {
            if (printOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Belum ada pesanan cetak.",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl)),
                ) {
                    printOrders.forEachIndexed { index, order ->
                        if (index > 0) {
                            HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                        }
                        CetakOrderItemRow(
                            order = order,
                            onClick = {
                                showAllOrdersSheet = false
                                onOpenTicket(order.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sticky Header 44dp untuk Tab Cetak sesuai aturan Handoff §3 & Main.dc.html:
 * - Ukuran font 18px SemiBold, terpusat
 * - Border hairline tipis (0.5dp) muncul saat konten di-scroll
 */
@Composable
private fun CetakStickyHeader(lazyListState: LazyListState) {
    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 8
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Palette.Paper),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Cetak",
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
        )

        if (isScrolled) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = GoldinBorder.Hairline,
                color = Palette.Line,
            )
        }
    }
}

/**
 * Kartu Saldo Emas (Screen 30: `ct-bal`):
 * - Ikon ingot emas di lingkaran kuning muda (`secondary-100` / `#FFF3D6`) di kiri
 * - Judul "Saldo emas" (14sp SemiBold)
 * - Pemisah hairline tipis
 * - Angka saldo format monospaced: `2 , 4518 gr`
 * - Tombol "Cetak emas" bernuansa ungu muda lembut (`primary-50` / `primary-700`) dengan ikon printer
 * - Catatan edukasi "Minimal saldo emas 1 gr untuk cetak." jika saldo kurang
 */
@Composable
private fun CetakFreeGoldCard(
    freeGoldGrams: Double,
    canPrint: Boolean,
    onStartCetak: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(Color.White)
            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Baris Header: Ikon Ingot Emas + Judul "Saldo emas"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Palette.Gold100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GoldinIcons.GoldBars,
                    contentDescription = null,
                    tint = Palette.GoldDeep,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = "Saldo emas",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Palette.Ink,
            )
        }

        // Garis Pemisah Hairline dalam kartu
        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)

        // Angka Saldo Emas: e.g. "2 , 4518 gr"
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = formatGrams4(freeGoldGrams).replace(" gr", "").replace(",", " , "),
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                color = Palette.Ink,
            )
            Text(
                text = "gr",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Palette.Ink,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        // Tombol "Cetak emas" (Lavender lembut: primary-50, border primary-100, teks & ikon primary-700)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .border(
                    width = 1.dp,
                    color = if (canPrint) Palette.Primary100 else Palette.Neutral200,
                    shape = RoundedCornerShape(GoldinRadius.Lg),
                )
                .background(if (canPrint) Palette.Primary50 else Palette.Neutral100)
                .clickable(enabled = canPrint, onClick = onStartCetak),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = GoldinIcons.Printer,
                    contentDescription = null,
                    tint = if (canPrint) Palette.Primary700 else Palette.DisabledText,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Cetak emas",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (canPrint) Palette.Primary700 else Palette.DisabledText,
                )
            }
        }

        // Edukasi jika saldo emas < 1 gr
        if (freeGoldGrams < 1.0) {
            Text(
                text = "Minimal saldo emas 1 gr untuk cetak.",
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = Palette.InkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Seksi Pesanan Cetak (Screen 30: `ct-act`):
 * - Judul "Pesanan cetak" (18sp Lato Bold) + tombol "Lihat semua" jika ada pesanan
 * - Saat belum ada pesanan aktif: Empty State Card dengan ikon Box 56dp di lingkaran ungu + "Belum ada pesanan aktif."
 * - Saat ada pesanan aktif: Card daftar pesanan dengan 40dp circle gold ingot & pill status
 */
@Composable
private fun CetakOrdersSection(
    orders: List<PhysicalPrintOrder>,
    onOpenTicket: (orderId: String) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Pesanan cetak",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Palette.Ink,
            )
            if (orders.isNotEmpty()) {
                Text(
                    text = "Lihat semua",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Primary700,
                    modifier = Modifier.clickable(onClick = onSeeAll),
                )
            }
        }

        val activeOrders = remember(orders) {
            orders.filter { !it.status.isDone && it.status != PhysicalOrderStatus.DONE && it.status != PhysicalOrderStatus.DELIVERED }
        }

        if (activeOrders.isEmpty()) {
            // Empty State Card: Sesuai L2536 Main.dc.html
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GoldinRadius.Xl))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
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
                        imageVector = GoldinIcons.Box,
                        contentDescription = null,
                        tint = Palette.Primary700,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = "Belum ada pesanan aktif.",
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = Palette.InkSecondary,
                )
            }
        } else {
            // Daftar Pesanan Aktif: Sesuai L2535 Main.dc.html
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GoldinRadius.Xl))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl)),
            ) {
                activeOrders.forEachIndexed { index, order ->
                    if (index > 0) {
                        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                    }
                    CetakOrderItemRow(
                        order = order,
                        onClick = { onOpenTicket(order.id) },
                    )
                }
            }
        }
    }
}

/**
 * Baris item pesanan cetak sesuai L2535 Main.dc.html
 */
@Composable
private fun CetakOrderItemRow(
    order: PhysicalPrintOrder,
    onClick: () -> Unit,
) {
    val statusPillColors = remember(order.status) {
        when (order.status) {
            PhysicalOrderStatus.WAITING_PAYMENT -> Pair(Palette.WarnSoft, Palette.Warn)
            PhysicalOrderStatus.CREATED -> Pair(Palette.Neutral100, Palette.InkSecondary)
            PhysicalOrderStatus.PREPARING -> Pair(Palette.Info50, Palette.Info700)
            PhysicalOrderStatus.SHIPPED_TO_BRANCH, PhysicalOrderStatus.IN_TRANSIT -> Pair(Palette.Primary50, Palette.Primary700)
            PhysicalOrderStatus.READY_PICKUP -> Pair(Palette.Gold100, Palette.GoldDeep)
            PhysicalOrderStatus.DONE, PhysicalOrderStatus.DELIVERED -> Pair(Palette.GoodSoft, Palette.Good)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp, 16.dp),
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
                imageVector = GoldinIcons.GoldBars,
                contentDescription = null,
                tint = Palette.GoldDeep,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "${order.brand.displayName} ${formatGrams4(order.weightGrams)}",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Palette.Ink,
            )
            Text(
                text = if (order.isPickup) (order.branchName ?: "Ambil di Cabang") else "Dikirim ke alamat",
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = Palette.InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (order.serialNumber != null && order.status.isDone) {
                Text(
                    text = "No. seri: ${order.serialNumber}",
                    fontFamily = MonoFontFamily,
                    fontSize = 12.sp,
                    color = Palette.InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(GoldinRadius.Full))
                .background(statusPillColors.first)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = order.status.label,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = statusPillColors.second,
            )
        }
    }
}

/**
 * Seksi Brand & Ukuran (Screen 30: `ct-stock`):
 * - Judul "Brand & ukuran" (18sp Lato Bold)
 * - Card putih radius 12dp:
 *   - Tab Underline datar: "LM REI", "Antam", "Galeri 24"
 *   - Grid 4x2 chip pecahan: 1, 2, 3, 5, 10, 25, 50, 100 gr
 *   - Status teks "Tersedia" warna hijau (success-700) di background hijau muda (success-50)
 */
@Composable
private fun CetakStockCatalogSection(stockList: List<GoldBarStock>) {
    var selectedBrand by remember { mutableStateOf(GoldBrand.LM_REI) }
    val brands = listOf(GoldBrand.LM_REI, GoldBrand.ANTAM, GoldBrand.GALERI_24)

    val currentStocks = remember(selectedBrand, stockList) {
        val filtered = stockList.filter { it.brand == selectedBrand }
        if (filtered.isNotEmpty()) filtered else {
            listOf(1.0, 2.0, 3.0, 5.0, 10.0, 25.0, 50.0, 100.0).map { w ->
                GoldBarStock(selectedBrand, w, inStock = !(selectedBrand == GoldBrand.ANTAM && w == 100.0))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Brand & ukuran",
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Palette.Ink,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .background(Color.White)
                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
        ) {
            // Tablist Datar dengan Indikator Garis Bawah
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .drawBehind {
                        drawLine(
                            color = Palette.Line,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            ) {
                brands.forEach { brand ->
                    val isSelected = brand == selectedBrand
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedBrand = brand },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = brand.displayName,
                            fontFamily = BodyFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isSelected) Palette.Primary700 else Palette.InkSecondary,
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Palette.Primary700),
                            )
                        }
                    }
                }
            }

            // Grid 4 Kolom x 2 Baris Pecahan Gram
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val chunkedStocks = currentStocks.chunked(4)
                chunkedStocks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { stock ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Lg))
                                    .background(if (stock.inStock) Palette.GoodSoft else Palette.Neutral100),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = "${stock.weightGrams.toInt()}",
                                            fontFamily = MonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (stock.inStock) Palette.Ink else Palette.InkSoft,
                                        )
                                        Text(
                                            text = "gr",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = if (stock.inStock) Palette.Ink else Palette.InkSoft,
                                        )
                                    }
                                    Text(
                                        text = if (stock.inStock) "Tersedia" else "Stok habis",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = if (stock.inStock) Palette.Good700 else Palette.InkSoft,
                                    )
                                }
                            }
                        }
                        for (i in 0 until (4 - rowItems.size)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Seksi 3 Cabang Terdekat (Screen 30: `ct-br`):
 * - Judul "Cabang terdekat" (18sp Lato Bold) + tombol "Lihat semua"
 * - Card putih radius 12dp berisi 3 cabang terdekat
 * - Circle 40dp ungu muda + ikon MapPin
 */
@Composable
private fun CetakNearbyBranchesSection(
    branches: List<GoldBranch>,
    onBranchClick: (GoldBranch) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cabang terdekat",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Palette.Ink,
            )
            Text(
                text = "Lihat semua",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Palette.Primary700,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .background(Color.White)
                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
        ) {
            branches.forEachIndexed { index, branch ->
                if (index > 0) {
                    HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBranchClick(branch) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            imageVector = GoldinIcons.MapPin,
                            contentDescription = null,
                            tint = Palette.Primary700,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = branch.name,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = branch.address,
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                        )
                        Text(
                            text = "${branch.distanceKm} km · ${branch.openingHours}",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSoft,
                        )
                    }

                    Icon(
                        imageVector = GoldinIcons.ChevronRight,
                        contentDescription = null,
                        tint = Palette.InkSoft,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}
