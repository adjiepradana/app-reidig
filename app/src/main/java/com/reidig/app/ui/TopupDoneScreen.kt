package com.reidig.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import com.reidig.shared.formatNumberOnly
import com.reidig.shared.rupiah

/**
 * Layar Sukses Top Up Saldo Kas (Screen 24b Top up · Berhasil)
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2171–2194 dan
 * spesifikasi Design System v2 & Handoff §9 / §14.
 */
@Composable
fun TopupDoneScreen(
    env: AppEnv,
    txId: String,
    onReceipt: (txId: String) -> Unit,
    onPortfolio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val tx = remember(state.transactions, txId) {
        state.transactions.firstOrNull { it.id == txId }
    }
    val amount = tx?.amount ?: 500_000L
    val fee = tx?.fee ?: 1_000L
    val total = tx?.total ?: (amount + fee)
    val method = tx?.method ?: "Virtual Account"

    // Animasi Pulse Halo Ring (GoldinMotion.DurationPulseRing = 2000ms)
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
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Hero 88dp dengan breathing halo ring & centang emas
                item {
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
                        // Inner Circle 76dp
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Palette.Primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.Check,
                                contentDescription = null,
                                tint = Palette.Gold,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }

                // 2. Judul & Subtitle
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Top up berhasil!",
                            fontFamily = TitleFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Palette.Primary,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                        )
                        Text(
                            text = "${rupiah(amount)} sudah masuk ke Saldo Kas.",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            color = Palette.InkSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                        )
                    }
                }

                // 3. Card Rincian (Rp)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Rincian (Rp)",
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Palette.Ink,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Nominal top up",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.InkSecondary,
                                )
                                Text(
                                    text = formatNumberOnly(amount),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Biaya admin",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.InkSecondary,
                                )
                                Text(
                                    text = formatNumberOnly(fee),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                            }

                            HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)

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
                                    text = rupiah(total),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = Palette.Primary,
                                )
                            }

                            HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Metode pembayaran",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.InkSecondary,
                                )
                                Text(
                                    text = method,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "No. transaksi",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.InkSecondary,
                                )
                                Text(
                                    text = txId,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Sticky Actions: Lihat bukti transaksi & Kembali ke portofolio
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.Paper)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    text = "Lihat bukti transaksi",
                    onClick = { onReceipt(txId) },
                    modifier = Modifier.fillMaxWidth(),
                )

                PrimaryButton(
                    text = "Kembali ke portofolio",
                    onClick = onPortfolio,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun TopupDonePreview() = ReidigTheme {
    TopupDoneScreen(
        env = rememberPreviewEnv(),
        txId = "TRX-TU-PREVIEW",
        onReceipt = {},
        onPortfolio = {},
    )
}
