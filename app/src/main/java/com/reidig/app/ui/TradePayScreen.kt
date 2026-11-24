package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.app.R
import com.reidig.shared.PaymentMethod
import com.reidig.shared.formatVaWithSpaces
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar `15 Transaksi · Menunggu pembayaran` sesuai prototipe Goldin v1.2 (`Main.dc.html` L1682–1732) & Handoff §7.
 * Fitur utama:
 * - Top bar tombol kembali dengan modal dialog konfirmasi keluar `GoldinModal` ("Keluar dari pembayaran?")
 * - Kartu countdown timer 15 menit Geist Mono 24sp `Palette.Warn`
 * - Tampilan nomor Virtual Account format spasi (BCA, BRI, Mandiri, BNI) + tombol salin nomor VA dan salin total bayar
 * - Tampilan QRIS dengan logo resmi, frame QR code crisp, instruksi scan, dan total bayar
 * - Kartu info biru harga emas terkunci
 * - Sticky footer dengan tombol "Cek status pembayaran" (spinner 800ms) & tombol "Batalkan pesanan"
 */
@Composable
fun TradePayScreen(
    env: AppEnv,
    txId: String,
    onBack: () -> Unit,
    onDone: (txId: String) -> Unit,
    onCancelOrder: () -> Unit = onBack,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val tx = state.transactions.firstOrNull { it.id == txId }

    if (tx == null) {
        LaunchedEffect(Unit) {
            env.notify("Transaksi tidak ditemukan.")
            onBack()
        }
        Box(Modifier.fillMaxSize().background(Palette.Paper))
        return
    }

    // Status loading & navigasi aman
    var isCheckingStatus by remember { mutableStateOf(false) }
    var hasNavigated by remember { mutableStateOf(false) }

    // Jika transaksi sudah selesai, langsung arahkan ke selesai
    if (tx.status == "ok" && !isCheckingStatus && !hasNavigated) {
        LaunchedEffect(Unit) {
            hasNavigated = true
            onDone(tx.id)
        }
        Box(Modifier.fillMaxSize().background(Palette.Paper))
        return
    }

    // Modal dialog konfirmasi keluar
    var showExitModal by remember { mutableStateOf(false) }

    // Live Countdown Timer (15 Menit)
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val expiresAt = tx.expiresAtMs ?: (tx.atMs + 15 * 60 * 1000L)

    LaunchedEffect(expiresAt) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            if (currentTimeMs >= expiresAt) break
            delay(1000L)
        }
    }

    val remainingSec = ((expiresAt - currentTimeMs).coerceAtLeast(0L) / 1000L).toInt()
    val timerText = remember(remainingSec) {
        val mm = (remainingSec / 60).toString().padStart(2, '0')
        val ss = (remainingSec % 60).toString().padStart(2, '0')
        "$mm:$ss"
    }

    val deadlineText = remember(expiresAt) {
        val sdf = SimpleDateFormat("d MMM yyyy, HH.mm 'WIB'", Locale("id", "ID"))
        sdf.format(Date(expiresAt))
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
            // 1. Top Bar 44dp (`Main.dc.html` L1684)
            TopTitleBar(
                title = "Selesaikan pembayaran",
                onBack = { showExitModal = true },
            )

            // Konten Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                // 2. Kartu Countdown Timer 15 Menit (`Main.dc.html` L1686–1690)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(GoldinSpace.Md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Bayar sebelum",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Palette.InkSecondary,
                    )
                    Text(
                        text = timerText,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        color = Palette.Warn,
                    )
                    Text(
                        text = deadlineText,
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Palette.InkSoft,
                    )
                }

                // 3. Tampilan Detail Pembayaran (VA atau QRIS)
                val method = tx.paymentMethod ?: PaymentMethod.VA_BCA
                if (method.isVa || tx.virtualAccountNumber != null) {
                    // 3.1 Kartu Virtual Account (`Main.dc.html` L1692–1712)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(GoldinSpace.Md),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Logo & Nama Bank
                        val logoRes = when (method) {
                            PaymentMethod.VA_BCA -> R.drawable.logo_bca
                            PaymentMethod.VA_BRI -> R.drawable.logo_bri
                            PaymentMethod.VA_MANDIRI -> R.drawable.logo_mandiri
                            PaymentMethod.VA_BNI -> R.drawable.logo_bni
                            else -> R.drawable.logo_bca
                        }

                        Row(
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
                                Image(
                                    painter = painterResource(logoRes),
                                    contentDescription = method.label,
                                    modifier = Modifier.size(width = 60.dp, height = 24.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            Text(
                                text = method.label,
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = Palette.Ink,
                            )
                        }

                        // Baris Nomor VA
                        val rawVa = tx.virtualAccountNumber?.replace(" ", "") ?: "3935801924982261"
                        val formattedVa = formatVaWithSpaces(rawVa)

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Nomor Virtual Account",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Palette.InkSecondary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = formattedVa,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp,
                                    color = Palette.Ink,
                                )
                                GoldinIconButton(
                                    icon = PrototypeIcons.Copy,
                                    contentDescription = "Salin nomor VA",
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(rawVa))
                                        env.notify("Nomor VA disalin")
                                    },
                                    tint = Palette.Primary700,
                                    hoverColor = Palette.Primary50,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(GoldinBorder.Hairline)
                                .background(Palette.Line),
                        )

                        // Baris Total
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Total",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Palette.InkSecondary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = rupiah(tx.effectiveTotal),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                    color = Palette.Primary700,
                                )
                                GoldinIconButton(
                                    icon = PrototypeIcons.Copy,
                                    contentDescription = "Salin total pembayaran",
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(tx.effectiveTotal.toString()))
                                        env.notify("Total pembayaran disalin")
                                    },
                                    tint = Palette.Primary700,
                                    hoverColor = Palette.Primary50,
                                )
                            }
                        }
                    }
                } else if (method == PaymentMethod.QRIS || tx.qrisPayload != null) {
                    // 3.2 Kartu QRIS (`Main.dc.html` L1714–1723)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(GoldinSpace.Md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_qris),
                            contentDescription = "QRIS",
                            modifier = Modifier.height(30.dp),
                            contentScale = ContentScale.Fit,
                        )

                        // Box QR Code 200x200dp
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                                .padding(24.dp),
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
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Palette.InkSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(GoldinBorder.Hairline)
                                .background(Palette.Line),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Total",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.InkSecondary,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = rupiah(tx.effectiveTotal),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp,
                                    color = Palette.Primary700,
                                )
                                GoldinIconButton(
                                    icon = PrototypeIcons.Copy,
                                    contentDescription = "Salin total pembayaran",
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(tx.effectiveTotal.toString()))
                                        env.notify("Total pembayaran disalin")
                                    },
                                    tint = Palette.Primary700,
                                    hoverColor = Palette.Primary50,
                                )
                            }
                        }
                    }
                }

                // 4. Catatan Info Biru Harga Emas Terkunci (`Main.dc.html` L1724–1727) - Hanya untuk Beli/Jual
                if (tx.isTrade && tx.pricePerGram > 0) {
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
                            imageVector = PrototypeIcons.Lock,
                            contentDescription = null,
                            tint = Palette.Info700,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Harga emas terkunci di ${rupiah(tx.pricePerGram)}. Segera selesaikan pembayaran untuk tetap mendapatkan harga ini.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }

            // 5. Sticky Footer (`Main.dc.html` L1729–1732)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = GoldinBorder.Hairline, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = GoldinSpace.Lg, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrimaryButton(
                    text = "Cek status pembayaran",
                    loading = isCheckingStatus,
                    loadingText = "Memeriksa pembayaran…",
                    onClick = {
                        if (!isCheckingStatus && !hasNavigated) {
                            isCheckingStatus = true
                            scope.launch {
                                delay(800L) // simulasi request jaringan
                                try {
                                    val completed = env.repo.confirmTradePayment(tx.id)
                                    isCheckingStatus = false
                                    hasNavigated = true
                                    onDone(completed.id)
                                } catch (e: Exception) {
                                    isCheckingStatus = false
                                    env.notify(e.message ?: "Pembayaran belum diterima. Coba beberapa saat lagi.")
                                }
                            }
                        }
                    },
                    height = 44.dp,
                )

                if (tx.isTrade) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable {
                                env.repo.cancelTradeOrder(tx.id)
                                env.notify("Pesanan dibatalkan")
                                onCancelOrder()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Batalkan pesanan",
                            fontFamily = BodyFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Palette.InkSecondary,
                        )
                    }
                }
            }
        }

        // 6. Modal Dialog Konfirmasi Keluar Pembayaran (`askLeavePay` `Main.dc.html` L4125–4126)
        if (showExitModal) {
            GoldinModal(
                title = "Keluar dari pembayaran?",
                message = "Pembayaran belum selesai. Segera selesaikan pembayaran sebelum $deadlineText.",
                confirmLabel = "Keluar",
                cancelLabel = "Lanjut bayar",
                onConfirm = {
                    showExitModal = false
                    onBack()
                },
                onCancel = { showExitModal = false },
                isDestructive = false,
                tone = GoldinDialogTone.WARNING,
                icon = PrototypeIcons.AlertCircle,
            )
        }
    }
}
