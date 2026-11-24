package com.reidig.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.PhysicalOrderStatus
import com.reidig.shared.Tx
import com.reidig.shared.TxType
import com.reidig.shared.formatGrams4
import com.reidig.shared.formatNumberOnly
import com.reidig.shared.rupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar `19 Bukti transaksi` (Struk) sesuai prototipe Goldin v1.2 (`Main.dc.html` L1859–1906) & Handoff §8.
 * Fitur utama:
 * - Desain kartu tunggal struk dengan pemisah garis putus-putus (*dashed divider*)
 * - Header status transaksi 56dp (ikon status, label, nominal tebal Geist Mono 24sp, subtitel)
 * - Rincian detail transaksi (Waktu transaksi, No. order, Jenis transaksi, Metode pembayaran)
 * - Detail emas (Berat emas, Harga per gram)
 * - Rincian finansial (Subtotal, Biaya admin, Pajak PPh 22, Total)
 * - Bukti pencatatan resmi bursa & kliring ber-lencana aman (`ti-shield-check`) & tombol salin
 * - Tombol footer "Unduh bukti (PDF)" dengan toast feedback
 */
@Composable
fun ReceiptScreen(
    env: AppEnv,
    txId: String,
    onBack: () -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    val matchingCetak = remember(txId, state.physicalOrders) {
        state.physicalOrders.firstOrNull { it.id == txId || it.orderNumber == txId || it.txId == txId }
    }

    val tx = state.transactions.firstOrNull { it.id == txId }
        ?: state.transactions.firstOrNull { t ->
            state.physicalOrders.any { o -> (o.id == txId || o.orderNumber == txId) && o.txId == t.id }
        }
        ?: matchingCetak?.let { order ->
            Tx(
                id = order.txId ?: ("TX-" + order.id),
                type = TxType.WITHDRAW,
                title = "Cetak emas ${order.brand.displayName} ${formatGrams4(order.weightGrams)}",
                amount = -order.totalPayment,
                atMs = order.createdAtMs,
                status = if (order.status == PhysicalOrderStatus.WAITING_PAYMENT) "pending" else "ok",
                method = order.paymentMethod.displayName,
                paymentMethod = order.paymentMethod,
                grams = order.weightGrams,
                subtotal = order.printFee,
                fee = order.shippingInsuranceFee,
                total = order.totalPayment,
                brand = order.brand.displayName,
                destination = order.branchName ?: order.destinationAddress,
            )
        }

    if (tx == null) {
        LaunchedEffect(Unit) {
            env.notify("Transaksi tidak ditemukan.")
            onBack()
        }
        Box(Modifier.fillMaxSize().background(Palette.Paper))
        return
    }

    val isBuy = tx.type != TxType.SELL
    val isOk = tx.status == "ok"
    val isPending = tx.status == "pending" || tx.status == "process"

    val formattedFullTime = remember(tx.atMs) {
        val sdf = SimpleDateFormat("d MMM yyyy, HH.mm 'WIB'", Locale("id", "ID"))
        sdf.format(Date(tx.atMs))
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
            // 1. Top Bar 44dp (`Main.dc.html` L1861)
            TopTitleBar(
                title = "Bukti transaksi",
                onBack = onBack,
            )

            // Konten Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Sm),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 2. Kartu Struk Tunggal (`Main.dc.html` L1863–1899)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Md),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 2.1 Status Header Struk
                    val (statusBg, statusFg, statusIcon, statusLabel) = when {
                        matchingCetak != null -> Quadruple(Palette.WarnSoft, Palette.Warn, PrototypeIcons.Clock, "Diproses")
                        isOk -> Quadruple(Palette.GoodSoft, Palette.Good, PrototypeIcons.Check, "Transaksi Berhasil")
                        isPending -> Quadruple(Palette.WarnSoft, Palette.Warn, PrototypeIcons.Clock, "Menunggu Pembayaran")
                        else -> Quadruple(Palette.CriticalSoft, Palette.Critical, PrototypeIcons.XClose, "Transaksi Gagal")
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(statusBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusFg,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Text(
                            text = statusLabel,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = statusFg,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        Text(
                            text = if (matchingCetak != null) "${if (matchingCetak.weightGrams % 1.0 == 0.0) matchingCetak.weightGrams.toInt() else matchingCetak.weightGrams} gr" else rupiah(tx.effectiveTotal),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            color = Palette.Ink,
                        )

                        val subDetail = if (matchingCetak != null) {
                            "Cetak emas · ${rupiah(matchingCetak.totalPayment)}"
                        } else if (tx.grams > 0.0) {
                            "${tx.title} ${formatGrams4(tx.grams)}"
                        } else tx.title

                        Text(
                            text = subDetail,
                            fontFamily = BodyFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                        )
                    }

                    // Dashed Divider
                    ReceiptDashedDivider()

                    // 2.2 Detail Transaksi
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Detail transaksi",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Palette.Ink,
                        )
                        if (matchingCetak != null) {
                            ReceiptDetailRow("No. order", matchingCetak.orderNumber)
                            ReceiptDetailRow("Metode pembayaran", matchingCetak.paymentMethod.displayName)
                            ReceiptDetailRow("Pengambilan", if (matchingCetak.isPickup) "Ambil di cabang" else "Dikirim ke alamat")
                            if (matchingCetak.isPickup) {
                                ReceiptDetailRow("Cabang", matchingCetak.branchName ?: "Raja Emas BSD")
                            } else {
                                ReceiptDetailRow("Dikirim ke", matchingCetak.destinationAddress ?: "–")
                            }
                            ReceiptDetailRow("Order dibuat", formattedFullTime)
                        } else {
                            ReceiptDetailRow("Waktu transaksi", formattedFullTime)
                            ReceiptDetailRow("Nomor order", tx.id)
                            ReceiptDetailRow("Jenis transaksi", tx.type.label)
                            ReceiptDetailRow("Metode pembayaran", tx.method.ifBlank { "Saldo Kas" })
                        }
                    }

                    // 2.3 Detail Emas
                    if (matchingCetak != null) {
                        ReceiptDashedDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Detail emas",
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = Palette.Ink,
                            )
                            ReceiptDetailRow("Brand", matchingCetak.brand.displayName)
                            ReceiptDetailRow("Berat emas", "${if (matchingCetak.weightGrams % 1.0 == 0.0) matchingCetak.weightGrams.toInt() else matchingCetak.weightGrams} gr")
                        }
                    } else if (tx.grams > 0.0) {
                        ReceiptDashedDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Detail emas",
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = Palette.Ink,
                            )
                            ReceiptDetailRow("Berat emas", formatGrams4(tx.grams))
                            ReceiptDetailRow("Harga per gram", rupiah(tx.pricePerGram))
                        }
                    }

                    // 2.4 Rincian Order Finansial
                    ReceiptDashedDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (matchingCetak != null) "Rincian (Rp)" else if (isBuy) "Rincian pembayaran" else "Rincian penjualan",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Palette.Ink,
                        )
                        if (matchingCetak != null) {
                            ReceiptDetailRow("Biaya cetak", formatNumberOnly(matchingCetak.printFee))
                            if (!matchingCetak.isPickup) {
                                ReceiptDetailRow("Ongkir & asuransi", formatNumberOnly(matchingCetak.shippingInsuranceFee))
                            }
                        } else {
                            ReceiptDetailRow("Subtotal", rupiah(tx.effectiveSubtotal))
                            ReceiptDetailRow("Biaya admin", if (isBuy) rupiah(tx.fee) else "- ${rupiah(tx.fee)}")
                            ReceiptDetailRow("Pajak PPh 22 (0,25%)", if (isBuy) rupiah(tx.tax) else "- ${rupiah(tx.tax)}")
                        }

                        ReceiptDashedDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (matchingCetak != null) "Total" else if (isBuy) "Total pembayaran" else "Total dana diterima",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = Palette.Ink,
                            )
                            Text(
                                text = if (matchingCetak != null) rupiah(matchingCetak.totalPayment) else rupiah(tx.effectiveTotal),
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = Palette.Primary700,
                            )
                        }
                    }

                    // 2.5 Bukti Pencatatan Resmi Bursa & Kliring (`Main.dc.html` L1888–1898)
                    if (isOk && tx.isTrade && matchingCetak == null) {
                        ReceiptDashedDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Bukti pencatatan",
                                    fontFamily = TitleFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp,
                                    color = Palette.Ink,
                                )
                                Text(
                                    text = "Transaksi ini tercatat di ledger terenkripsi dan bisa diverifikasi.",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Palette.InkSecondary,
                                )
                            }

                            // Ref Bursa
                            val bursa = tx.bursaRef ?: "BRS-261119-00123"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ref. Bursa",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = Palette.InkSecondary,
                                    )
                                    Text(
                                        text = bursa,
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = Palette.Ink,
                                    )
                                }
                                GoldinIconButton(
                                    icon = PrototypeIcons.Copy,
                                    contentDescription = "Salin referensi bursa",
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(bursa))
                                        env.notify("Nomor referensi bursa disalin")
                                    },
                                    tint = Palette.Primary700,
                                    hoverColor = Palette.Primary50,
                                )
                            }

                            // Ref Kliring
                            val kliring = tx.kliringRef ?: "KLR-261119-00456"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ref. Kliring",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = Palette.InkSecondary,
                                    )
                                    Text(
                                        text = kliring,
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = Palette.Ink,
                                    )
                                }
                                GoldinIconButton(
                                    icon = PrototypeIcons.Copy,
                                    contentDescription = "Salin referensi kliring",
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(kliring))
                                        env.notify("Nomor referensi kliring disalin")
                                    },
                                    tint = Palette.Primary700,
                                    hoverColor = Palette.Primary50,
                                )
                            }

                            // Banner Verifikasi Aman Hijau (`ti-shield-check`)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.ShieldCheck,
                                    contentDescription = null,
                                    tint = Palette.Good700,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Tercatat dan diverifikasi di ledger kliring resmi.",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Palette.Good700,
                                )
                            }
                        }
                    }
                }

                // 3. Info Biru jika Pending/Gagal (`Main.dc.html` L1900–1902)
                if (!isOk && matchingCetak == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.Info50)
                            .border(1.dp, Palette.Info200, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.Clock,
                            contentDescription = null,
                            tint = Palette.Info700,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Bukti pencatatan di bursa dan kliring akan tersedia setelah pembayaran selesai diverifikasi.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // 4. Sticky Footer Tombol "Unduh bukti (PDF)" (`Main.dc.html` L1904)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Paper)
                    .border(width = GoldinBorder.Hairline, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = GoldinSpace.Lg, vertical = 16.dp),
            ) {
                SecondaryButton(
                    text = "Unduh bukti (PDF)",
                    leadingIcon = PrototypeIcons.Download,
                    onClick = {
                        env.notify("Bukti transaksi (${tx.id}) berhasil diunduh ke format PDF")
                    },
                    height = 44.dp,
                )
            }
        }
    }
}

/** Pemisah garis putus-putus (*dashed divider*) sesuai Handoff §8 */
@Composable
private fun ReceiptDashedDivider(
    color: Color = Palette.Line,
    thickness: Dp = 1.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness),
    ) {
        val strokeWidth = thickness.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f)
        drawLine(
            color = color,
            start = Offset(0f, strokeWidth / 2),
            end = Offset(size.width, strokeWidth / 2),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect,
        )
    }
}

/** Baris detail struk k/v */
@Composable
private fun ReceiptDetailRow(
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

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
