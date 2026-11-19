package com.reidig.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.app.R
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Pembayaran QRIS Top Up (Screen 24 Top up · Bayar QRIS)
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2146–2169 dan
 * spesifikasi Design System v2 & Handoff §9.
 */
@Composable
fun TopupQrPayScreen(
    env: AppEnv,
    txId: String,
    onBack: () -> Unit,
    onPaySuccess: (txId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val tx = remember(state.transactions, txId) {
        state.transactions.firstOrNull { it.id == txId }
    }
    val scope = rememberCoroutineScope()
    var showExitModal by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Hitung sisa waktu pembayaran (15 menit countdown)
    val expiryMs = tx?.expiresAtMs ?: (System.currentTimeMillis() + 15 * 60 * 1000L)
    var remainingSeconds by remember(expiryMs) {
        mutableLongStateOf(maxOf(0L, (expiryMs - System.currentTimeMillis()) / 1000L))
    }

    LaunchedEffect(expiryMs) {
        while (isActive && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds = maxOf(0L, (expiryMs - System.currentTimeMillis()) / 1000L)
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val deadlineText = remember(expiryMs) {
        SimpleDateFormat("d MMM yyyy, HH.mm 'WIB'", Locale.forLanguageTag("id-ID")).format(Date(expiryMs))
    }

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
            // Header 44dp
            TopTitleBar(
                title = "Selesaikan pembayaran",
                onBack = { showExitModal = true },
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Card 1: Countdown Timer
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Bayar sebelum",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.InkSecondary,
                            )
                            Text(
                                text = timerText,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 24.sp,
                                color = Palette.Warn,
                            )
                            Text(
                                text = deadlineText,
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = Palette.InkSoft,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }

                // Card 2: QRIS Image & Visual QR Code & Detail Pembayaran
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Logo Resmi QRIS
                            Image(
                                painter = painterResource(R.drawable.logo_qris),
                                contentDescription = "Logo QRIS",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(32.dp),
                            )

                            // QR Code 200x200dp
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, Palette.Line, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.QrCode,
                                    contentDescription = "Kode QR Pembayaran",
                                    tint = Palette.Neutral900,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            Text(
                                text = "Scan kode QR ini pakai aplikasi bank atau e-wallet yang mendukung QRIS.",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.InkSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                            )

                            // Rincian Pembayaran
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                HorizontalDivider(color = Palette.Line, thickness = 0.5.dp)

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
                                        text = rupiah(tx?.amount ?: 0L),
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
                                        text = "Total",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = Palette.Ink,
                                    )
                                    Text(
                                        text = rupiah(tx?.total ?: (tx?.amount ?: 0L)),
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        color = Palette.Primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Footer: Cek status pembayaran
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 0.5.dp, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = if (isLoading) "" else "Cek status pembayaran",
                    onClick = {
                        if (!isLoading && tx != null) {
                            isLoading = true
                            scope.launch {
                                delay(800L) // simulasi verifikasi webhook QRIS
                                val completedTx = env.repo.simulateQrisPaymentSuccess(tx.id)
                                isLoading = false
                                onPaySuccess(completedTx.id)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && tx != null,
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center),
                        color = Palette.Primary50,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }

    // Modal Konfirmasi Keluar dari Pembayaran
    if (showExitModal) {
        GoldinModal(
            title = "Keluar dari pembayaran?",
            message = "Pembayaran belum selesai. Segera selesaikan pembayaran sebelum batas waktu.",
            confirmLabel = "Keluar",
            cancelLabel = "Lanjut bayar",
            isDestructive = false,
            onConfirm = {
                showExitModal = false
                onBack()
            },
            onCancel = { showExitModal = false },
        )
    }
}

@ScreenPreview
@Composable
private fun TopupQrPayPreview() = ReidigTheme {
    TopupQrPayScreen(
        env = rememberPreviewEnv(),
        txId = "TRX-PREVIEW",
        onBack = {},
        onPaySuccess = {},
    )
}
