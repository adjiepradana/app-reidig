package com.reidig.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.GoldBranch
import com.reidig.shared.PhysicalOrderStatus
import com.reidig.shared.PhysicalPrintOrder
import com.reidig.shared.formatGrams4
import com.reidig.shared.rupiah
import java.util.Calendar
import java.util.Locale

/**
 * SCREEN 53: DETAIL TIKET PESANAN CETAK (`37 Cetak · Detail pesanan` / `ctTicket`)
 *
 * Mengimplementasikan:
 * - Header halaman 44dp dengan tombol kembali
 * - Hero Card Tiket QR (Khusus Pickup di Cabang saat belum selesai):
 *   - Vektor Canvas QR Code 168x168dp
 *   - Kode tiket mono tebal (misal `TKT-REI-839102`) + tombol salin clipboard
 *   - Edukasi pengambilan ("Tunjukkan QR ini di cabang..." & "Bawa e-KTP fisik asli")
 * - Seksi & Bottom Sheet "Atur Jadwal Ambil":
 *   - Menampilkan jadwal aktif jika sudah diatur
 *   - Tombol "Atur jadwal ambil" / "Ubah" yang membuka drawer 7 hari kerja ke depan & slot 1 jam
 * - Timeline Status Pesanan 5 Tahap (Pesanan dibuat -> Sedang disiapkan -> Dikirim/ke cabang -> Siap diambil/Perjalanan -> Selesai)
 *   - Menampilkan nomor resi JNE jika pengiriman ke alamat
 *   - Menampilkan nomor seri keping fisik jika pesanan selesai
 * - Ringkasan Pesanan Lengkap (Emas, Pengambilan, No Transaksi, Total Pembayaran)
 * - Footer tombol "Lihat struk" bukti transaksi
 */
@Composable
fun CetakTicketScreen(
    env: AppEnv,
    orderId: String,
    onBack: () -> Unit,
    onOpenReceipt: (txId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val order = state.physicalOrders.firstOrNull { it.id == orderId } ?: env.repo.getPhysicalPrintOrder(orderId)
    val clipboardManager = LocalClipboardManager.current

    var showScheduleDrawer by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    if (order == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Palette.Paper)
                .statusBarsPadding(),
        ) {
            TopTitleBar(title = "Detail pesanan", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Pesanan cetak tidak ditemukan.",
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = Palette.InkSecondary,
                )
            }
        }
        return
    }

    val isPickup = order.isPickup
    val isDone = order.status.isDone
    val branch = remember(order.branchId) {
        val bId = order.branchId
        if (bId != null) env.repo.getGoldBranch(bId) else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // 1. Top Bar Header
        TopTitleBar(
            title = "Detail pesanan",
            onBack = onBack,
        )

        // 2. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // A. Hero Card Tiket QR (Khusus Pickup & Belum Selesai)
            if (isPickup && !isDone) {
                item(key = "ticket_qr_hero_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // QR Code Container Box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(GoldinRadius.Lg))
                                .background(Color.White)
                                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CetakQrCanvas(
                                payload = order.qrCodePayload.ifBlank { order.ticketCode },
                                modifier = Modifier.size(168.dp),
                            )
                        }

                        // Kode Tiket Mono & Tombol Salin
                        val displayCode = order.orderNumber.ifBlank { order.ticketCode.ifBlank { order.id } }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = displayCode,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Palette.Ink,
                            )

                            Spacer(Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(displayCode))
                                        env.notify("Kode pesanan disalin")
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = GoldinIcons.Copy,
                                    contentDescription = "Salin kode tiket",
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        // Petunjuk Pengambilan
                        Text(
                            text = "Tunjukkan QR ini di cabang untuk ambil emas.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                            textAlign = TextAlign.Center,
                        )

                        // Reservasi / Atur Jadwal Ambil
                        if (order.pickupScheduleDate.isNullOrBlank()) {
                            // Belum ada jadwal -> Tombol Atur Jadwal Ambil
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                    .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                                    .clickable { showScheduleDrawer = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = GoldinIcons.Calendar,
                                        contentDescription = null,
                                        tint = Palette.Primary700,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "Atur jadwal ambil",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Palette.Primary700,
                                    )
                                }
                            }
                        } else {
                            // Sudah ada jadwal -> Tampilkan Jadwal + Tombol Ubah
                            HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = GoldinIcons.Calendar,
                                    contentDescription = null,
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(20.dp),
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Jadwal ambil",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.InkSecondary,
                                    )
                                    Text(
                                        text = "${order.pickupScheduleDate} · ${order.pickupScheduleTimeSlot}",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Palette.Ink,
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(GoldinRadius.Full))
                                        .clickable { showScheduleDrawer = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Ubah",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Palette.Primary700,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // B. Status Pesanan (5-Step Timeline)
            item(key = "order_timeline_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Status pesanan",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Palette.Ink,
                    )

                    val timelineSteps = if (isPickup) {
                        listOf(
                            TimelineStepItem("Pesanan dibuat", 0),
                            TimelineStepItem("Sedang disiapkan", 1),
                            TimelineStepItem("Dikirim ke cabang", 2),
                            TimelineStepItem("Siap diambil", 3),
                            TimelineStepItem(
                                label = "Selesai",
                                stepIndex = 4,
                                sub = if (order.serialNumber != null) "No. seri ${order.serialNumber}" else null,
                            ),
                        )
                    } else {
                        listOf(
                            TimelineStepItem("Pesanan dibuat", 0),
                            TimelineStepItem("Sedang disiapkan", 1),
                            TimelineStepItem(
                                label = "Dikirim",
                                stepIndex = 2,
                                sub = if (order.trackingNumber != null) "No. resi ${order.trackingNumber}" else null,
                            ),
                            TimelineStepItem("Dalam perjalanan", 3),
                            TimelineStepItem(
                                label = "Diterima",
                                stepIndex = 4,
                                sub = if (order.serialNumber != null) "No. seri ${order.serialNumber}" else null,
                            ),
                        )
                    }

                    val currentStageIndex = when (order.status) {
                        PhysicalOrderStatus.WAITING_PAYMENT, PhysicalOrderStatus.CREATED -> 0
                        PhysicalOrderStatus.PREPARING -> 1
                        PhysicalOrderStatus.SHIPPED_TO_BRANCH -> 2
                        PhysicalOrderStatus.IN_TRANSIT -> if (isPickup) 2 else 3
                        PhysicalOrderStatus.READY_PICKUP -> 3
                        PhysicalOrderStatus.DONE, PhysicalOrderStatus.DELIVERED -> 4
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        timelineSteps.forEachIndexed { index, step ->
                            val isStepDone = if (order.status.isDone) true else index < currentStageIndex
                            val isCurrent = index == currentStageIndex && !order.status.isDone
                            val isLast = index == timelineSteps.size - 1

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Vertical Indicator Node
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(20.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isStepDone) Palette.Primary700 else Color.White)
                                            .border(
                                                width = 2.dp,
                                                color = if (isStepDone || isCurrent) Palette.Primary700 else Palette.Neutral300,
                                                shape = CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isStepDone) {
                                            Icon(
                                                imageVector = GoldinIcons.Check,
                                                contentDescription = null,
                                                tint = Palette.Primary50,
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }

                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(24.dp)
                                                .background(if (index < currentStageIndex || order.status.isDone) Palette.Primary700 else Palette.Neutral300),
                                        )
                                    }
                                }

                                // Step Text Content
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = if (isLast) 0.dp else 12.dp),
                                ) {
                                    Text(
                                        text = step.label,
                                        fontFamily = BodyFontFamily,
                                        fontWeight = if (isCurrent || isStepDone) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = if (isCurrent || isStepDone) Palette.Ink else Palette.InkSoft,
                                    )

                                    if (step.sub != null) {
                                        Text(
                                            text = step.sub,
                                            fontFamily = MonoFontFamily,
                                            fontSize = 12.sp,
                                            color = Palette.InkSecondary,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C. Ringkasan Pesanan (Order Summary)
            item(key = "order_summary_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Ringkasan",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Palette.Ink,
                    )

                    SummaryItemRow(
                        label = "Emas",
                        value = "${order.brand.displayName} · ${if (order.weightGrams % 1.0 == 0.0) order.weightGrams.toInt() else order.weightGrams} gr",
                    )

                    SummaryItemRow(
                        label = "Pengambilan",
                        value = if (isPickup) "Ambil di cabang" else "Dikirim ke alamat",
                    )

                    if (isPickup) {
                        SummaryItemRow(
                            label = "Cabang",
                            value = order.branchName ?: branch?.name ?: "Raja Emas Karawaci",
                        )
                    } else {
                        SummaryItemRow(
                            label = "Dikirim ke",
                            value = "${order.recipientName ?: "Penerima"} · ${order.destinationAddress ?: ""}",
                        )
                    }

                    SummaryItemRow(
                        label = "No. pesanan",
                        value = order.orderNumber,
                        isMono = true,
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Total",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = rupiah(order.totalPayment),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Palette.Primary700,
                        )
                    }
                }
            }
        }

        // 3. Footer Bar ("Lihat Struk")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 0.5.dp, color = Palette.Line)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(GoldinRadius.Full))
                    .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                    .clickable { onOpenReceipt(order.txId ?: order.id) },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = GoldinIcons.Receipt,
                        contentDescription = null,
                        tint = Palette.Primary700,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Lihat struk",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Primary700,
                    )
                }
            }
        }
    }

    // 4. Bottom Sheet Drawer "Atur Jadwal Ambil" (7 Hari ke Depan & Slot 1 Jam)
    if (showScheduleDrawer) {
        CetakScheduleDrawer(
            branch = branch ?: GoldBranch("br_karawaci", "Raja Emas Karawaci", "Supermal Karawaci", "Tangerang", "Banten", 1.2, "10.00–21.00", true),
            initialDate = order.pickupScheduleDate,
            initialSlot = order.pickupScheduleTimeSlot,
            onDismiss = { showScheduleDrawer = false },
            onSave = { date, slot ->
                env.repo.updatePickupSchedule(order.id, date, slot)
                env.notify("Jadwal ambil tersimpan")
                showScheduleDrawer = false
            },
        )
    }
}

/**
 * Bottom Sheet Drawer untuk mengatur jadwal pengambilan emas fisik pada cabang Raja Emas.
 * Menyediakan 7 hari buka ke depan (skip hari Minggu jika cabang beroperasi Senin-Sabtu)
 * serta pecahan slot waktu 1 jam.
 */
@Composable
private fun CetakScheduleDrawer(
    branch: GoldBranch,
    initialDate: String?,
    initialSlot: String?,
    onDismiss: () -> Unit,
    onSave: (date: String, slot: String) -> Unit,
) {
    val openDays = remember(branch.isSevenDays) { generateOpenDays(branch.isSevenDays) }
    val timeSlots = remember(branch.openingHours) { generateTimeSlots(branch.openingHours) }

    var selectedDay by rememberSaveable { mutableStateOf(initialDate ?: openDays.firstOrNull()?.fullDate ?: "") }
    var selectedSlot by rememberSaveable { mutableStateOf(initialSlot ?: timeSlots.firstOrNull() ?: "") }

    val canSave = selectedDay.isNotBlank() && selectedSlot.isNotBlank()

    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = "Atur jadwal ambil",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Informasi Cabang
            Text(
                text = "${branch.name} · Buka ${branch.openingHours} WIB",
                fontFamily = BodyFontFamily,
                fontSize = 13.sp,
                color = Palette.InkSecondary,
            )

            // Seksi 1: Pilih Tanggal (7 Hari Kerja)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih tanggal",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Ink,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    openDays.forEach { day ->
                        val isSelected = selectedDay == day.fullDate
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(GoldinRadius.Md))
                                .background(if (isSelected) Palette.Primary700 else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Palette.Primary700 else Palette.Line,
                                    shape = RoundedCornerShape(GoldinRadius.Md),
                                )
                                .clickable { selectedDay = day.fullDate }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = day.dayLabel,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Palette.Primary100 else Palette.InkSecondary,
                                )
                                Text(
                                    text = day.dateLabel,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Palette.Primary50 else Palette.Ink,
                                )
                            }
                        }
                    }
                }
            }

            // Seksi 2: Pilih Jam Pengambilan (Slot 1 Jam)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih jam pengambilan",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Ink,
                )

                val chunkedSlots = timeSlots.chunked(2)
                chunkedSlots.forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowSlots.forEach { slot ->
                            val isSelected = selectedSlot == slot
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Md))
                                    .background(if (isSelected) Palette.Primary50 else Color.White)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Palette.Primary700 else Palette.Line,
                                        shape = RoundedCornerShape(GoldinRadius.Md),
                                    )
                                    .clickable { selectedSlot = slot },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = slot,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Palette.Primary700 else Palette.Ink,
                                )
                            }
                        }
                        if (rowSlots.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            PrimaryButton(
                text = "Simpan jadwal ambil",
                enabled = canSave,
                onClick = { onSave(selectedDay, selectedSlot) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Komponen Vektor Canvas untuk me-render QR Code tiket cetak fisik.
 */
@Composable
private fun CetakQrCanvas(
    payload: String,
    modifier: Modifier = Modifier,
) {
    val matrixSize = 25
    val qrMatrix = remember(payload) {
        generateQrMatrix(payload, matrixSize)
    }

    Canvas(modifier = modifier) {
        val cellSize = size.width / matrixSize
        val darkColor = Color(0xFF0F052C) // Palette.Ink

        // Render black modules
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (qrMatrix[r][c]) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize),
                    )
                }
            }
        }
    }
}

/**
 * Menghasilkan matriks QR 25x25 deterministik dengan 3 finder pattern di sudut.
 */
private fun generateQrMatrix(payload: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    fun drawFinder(topRow: Int, leftCol: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                matrix[topRow + r][leftCol + c] = isOuter || isInner
            }
        }
    }

    // 1. Position Detection Patterns di 3 sudut
    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // 2. Timing Patterns
    for (i in 7 until size - 7) {
        matrix[6][i] = i % 2 == 0
        matrix[i][6] = i % 2 == 0
    }

    // 3. Alignment Pattern di sudut kanan bawah
    val alignRow = size - 9
    val alignCol = size - 9
    for (r in 0 until 5) {
        for (c in 0 until 5) {
            val isOuter = r == 0 || r == 4 || c == 0 || c == 4
            val isCenter = r == 2 && c == 2
            matrix[alignRow + r][alignCol + c] = isOuter || isCenter
        }
    }

    // 4. Data Modules deterministik berdasarkan hash string
    var seed = payload.hashCode().toLong()
    for (r in 0 until size) {
        for (c in 0 until size) {
            // Hindari menimpa area finder pattern
            val inFinder1 = r < 8 && c < 8
            val inFinder2 = r < 8 && c >= size - 8
            val inFinder3 = r >= size - 8 && c < 8
            val inAlign = r in (alignRow - 1)..(alignRow + 5) && c in (alignCol - 1)..(alignCol + 5)
            val inTiming = r == 6 || c == 6

            if (!inFinder1 && !inFinder2 && !inFinder3 && !inAlign && !inTiming) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                matrix[r][c] = (seed % 2L) == 1L
            }
        }
    }

    return matrix
}

private data class OpenDayItem(val dayLabel: String, val dateLabel: String, val fullDate: String)

/**
 * Menghitung 7 hari operasional ke depan mulai besok.
 * Melewati hari Minggu jika `isSevenDays == false`.
 */
private fun generateOpenDays(isSevenDays: Boolean): List<OpenDayItem> {
    val dayNamesShort = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    val dayNamesLong = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    val list = mutableListOf<OpenDayItem>()
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, 1)

    var attempts = 0
    while (list.size < 7 && attempts < 14) {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ...
        val isSunday = dayOfWeek == Calendar.SUNDAY

        if (isSevenDays || !isSunday) {
            val dayIdx = (dayOfWeek - 1).coerceIn(0, 6)
            val dayShort = dayNamesShort[dayIdx]
            val dayLong = dayNamesLong[dayIdx]
            val dateNum = cal.get(Calendar.DAY_OF_MONTH)
            val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
            val monthName = monthNames[monthIdx]
            val year = cal.get(Calendar.YEAR)

            val top = dayShort
            val bot = "$dateNum $monthName"
            val full = "$dayLong, $dateNum $monthName $year"
            list.add(OpenDayItem(top, bot, full))
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        attempts++
    }
    return list
}

/**
 * Menghasilkan pecahan slot waktu 1 jam sesuai jam buka cabang.
 */
private fun generateTimeSlots(openingHours: String): List<String> {
    val parts = openingHours.split("–", "-")
    val startHour = parts.getOrNull(0)?.substringBefore(".")?.trim()?.toIntOrNull() ?: 10
    val endHour = parts.getOrNull(1)?.substringBefore(".")?.trim()?.toIntOrNull() ?: 21

    val slots = mutableListOf<String>()
    for (h in startHour until endHour) {
        val startStr = String.format(Locale.US, "%02d.00", h)
        val endStr = String.format(Locale.US, "%02d.00", h + 1)
        slots.add("$startStr–$endStr WIB")
    }
    return slots
}

private data class TimelineStepItem(
    val label: String,
    val stepIndex: Int,
    val sub: String? = null,
)

@Composable
private fun SummaryItemRow(
    label: String,
    value: String,
    isMono: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
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
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp),
        )
    }
}
