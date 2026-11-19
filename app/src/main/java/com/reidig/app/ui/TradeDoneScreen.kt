package com.reidig.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.TxType
import com.reidig.shared.formatGrams4
import com.reidig.shared.rupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar `16 Transaksi · Berhasil` sesuai prototipe Goldin v1.2 (`Main.dc.html` L1735–1758) & Handoff §7.
 * Fitur utama:
 * - Badge hero 88dp dengan ring emas & ikon centang tebal emas `Palette.Gold`
 * - Judul "Transaksi berhasil!" (Lato Black 24sp `Palette.Primary700`)
 * - Subjudul deskriptif sesuai Beli / Jual
 * - Kartu ringkasan detail emas & rincian pembayaran / penjualan (Nomor transaksi, Waktu, Metode, Subtotal, Admin fee, Pajak PPh 22, Total)
 * - Tombol sekunder "Lihat bukti transaksi" (membuka Struk / Bukti Transaksi)
 * - Tombol utama "Kembali ke beranda"
 */
@Composable
fun TradeDoneScreen(
    env: AppEnv,
    txId: String,
    onHome: () -> Unit,
    onPortfolio: () -> Unit,
    onReceipt: () -> Unit,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val tx = state.transactions.firstOrNull { it.id == txId }
    val isBuy = tx?.type != TxType.SELL

    val formattedTime = remember(tx?.atMs) {
        val ms = tx?.atMs ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("d MMM yyyy, HH.mm 'WIB'", Locale("id", "ID"))
        sdf.format(Date(ms))
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
            // Konten Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = GoldinSpace.Lg)
                    .padding(top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Hero Icon Badge 88dp dengan breathing halo ring (`Main.dc.html` L1738–1741)
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

                Box(
                    modifier = Modifier.size(88.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Outer breathing ring halo
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
                    // Inner Circle
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Palette.Primary700),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.Check,
                            contentDescription = "Berhasil",
                            tint = Palette.Gold,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                // 2. Headline & Subtitle (`Main.dc.html` L1742–1743)
                Text(
                    text = "Transaksi berhasil!",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    color = Palette.Primary700,
                    textAlign = TextAlign.Center,
                )

                val subText = if (isBuy) {
                    "Pembelian ${formatGrams4(tx?.grams ?: 0.0)} emas berhasil. Emasmu sudah tersimpan di brankas digital dan tercatat dalam portofolio."
                } else {
                    "Penjualan ${formatGrams4(tx?.grams ?: 0.0)} emas berhasil. Dana sebesar ${rupiah(tx?.effectiveTotal ?: 0L)} telah berhasil ditambahkan ke Saldo Kas-mu."
                }

                Text(
                    text = subText,
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(Modifier.height(8.dp))

                // 3. Kartu Rincian Transaksi (`Main.dc.html` L1744–1751)
                if (tx != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        TradeDoneDetailRow("Berat emas", formatGrams4(tx.grams))
                        TradeDoneDetailRow("Harga per gram", rupiah(tx.pricePerGram))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(GoldinBorder.Hairline)
                                .background(Palette.Line),
                        )

                        // Rincian Transaksi
                        Text(
                            text = if (isBuy) "Rincian pembayaran" else "Rincian penjualan",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Palette.Ink,
                        )
                        TradeDoneDetailRow("Nomor transaksi", tx.id)
                        TradeDoneDetailRow("Waktu transaksi", formattedTime)
                        TradeDoneDetailRow("Metode pembayaran", tx.method.ifBlank { "Saldo Kas" })
                        TradeDoneDetailRow("Subtotal", rupiah(tx.effectiveSubtotal))
                        TradeDoneDetailRow("Biaya admin", if (isBuy) rupiah(tx.fee) else "- ${rupiah(tx.fee)}")
                        TradeDoneDetailRow("Pajak PPh 22 (0,25%)", if (isBuy) rupiah(tx.tax) else "- ${rupiah(tx.tax)}")

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
                                text = rupiah(tx.effectiveTotal),
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = Palette.Primary700,
                            )
                        }
                    }
                }
            }

            // 4. Footer Tombol Aksi (`Main.dc.html` L1753–1756)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Paper)
                    .navigationBarsPadding()
                    .padding(horizontal = GoldinSpace.Lg, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tombol Sekunder "Lihat bukti transaksi"
                SecondaryButton(
                    text = "Lihat bukti transaksi",
                    onClick = onReceipt,
                    height = 44.dp,
                )

                // Tombol Utama "Kembali ke beranda"
                PrimaryButton(
                    text = "Kembali ke beranda",
                    onClick = onHome,
                    height = 44.dp,
                )
            }
        }
    }
}

@Composable
private fun TradeDoneDetailRow(
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
