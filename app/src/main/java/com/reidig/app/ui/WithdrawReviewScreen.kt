package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.formatNumberOnly
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WD_FEE = 2_500L

/**
 * Layar Konfirmasi & Detail Penarikan Dana (Screen 26b Tarik dana · Detail penarikan)
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2488–2512, L4574–4579, L3874–3890
 * dan spesifikasi Design System v2 & Handoff §9 / §14.
 */
@Composable
fun WithdrawReviewScreen(
    env: AppEnv,
    bankAccountId: String,
    amount: Long,
    onBack: () -> Unit,
    onSuccess: (txId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val bankAccount = state.bankAccounts.firstOrNull { it.id == bankAccountId }
    val total = amount + WD_FEE

    var showPinDialog by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
            // Header 44dp dengan tombol kembali
            TopTitleBar(
                title = "Detail penarikan",
                onBack = { if (!isSending) onBack() },
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Card 1: Rekening Tujuan & Rincian (Rp)
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
                            // Sub-seksi Rekening Tujuan
                            Text(
                                text = "Rekening tujuan",
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Palette.Ink,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Logo Bank Badge 72x40dp
                                GoldinBankLogoBadge(bankName = bankAccount?.bank ?: "Bank")

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${bankAccount?.bank.orEmpty()} ••••${bankAccount?.number.orEmpty().takeLast(4)}",
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Palette.Ink,
                                    )
                                    Text(
                                        text = bankAccount?.holder.orEmpty().ifBlank {
                                            state.user?.name ?: "Pengguna"
                                        },
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Palette.InkSecondary,
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Palette.Line,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )

                            // Sub-seksi Rincian (Rp)
                            Text(
                                text = "Rincian (Rp)",
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Palette.Ink,
                            )

                            // Baris Nominal Penarikan
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Nominal penarikan",
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

                            // Baris Biaya Admin
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
                                    text = formatNumberOnly(WD_FEE),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                            }

                            HorizontalDivider(
                                color = Palette.Line,
                                thickness = 0.5.dp,
                            )

                            // Baris Total
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
                        }
                    }
                }

                // Card 2: Info Box Estimasi Dana Masuk (Info 50)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.Primary50)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.Clock,
                                contentDescription = null,
                                tint = Palette.Primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Dana masuk ke rekening tujuan maks. 1x24 jam kerja setelah penarikan dikonfirmasi.",
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                letterSpacing = 0.5.sp,
                                color = Palette.InkSecondary,
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.AlertCircle,
                                contentDescription = null,
                                tint = Palette.Critical,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                color = Palette.Critical,
                            )
                        }
                    }
                }
            }

            // Sticky Footer CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 0.5.dp, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = "Tarik dana",
                    onClick = { showPinDialog = true },
                    enabled = !isSending && bankAccount != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Overlay Animasi Sending Spinner 1.4s (Memproses penarikan dana…)
        AnimatedVisibility(
            visible = isSending,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = Palette.Primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "Memproses penarikan dana…",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        )
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi PIN Transaksi
    if (showPinDialog && bankAccount != null) {
        PinDialog(
            env = env,
            title = "Masukkan PIN 6 digit untuk mengonfirmasi transaksi ini.",
            onDismiss = { showPinDialog = false },
            onVerified = {
                showPinDialog = false
                isSending = true
                errorMessage = null
                scope.launch {
                    try {
                        // Simulasi jeda pengiriman transaksi ke Kliring 1.4 detik sesuai prototipe L3880
                        delay(1400L)
                        val tx = env.repo.requestWithdrawal(
                            amount = amount,
                            bankAccountId = bankAccount.id,
                        )
                        isSending = false
                        onSuccess(tx.id)
                    } catch (e: Exception) {
                        isSending = false
                        errorMessage = e.message ?: "Gagal memproses penarikan dana."
                    }
                }
            },
        )
    }
}
