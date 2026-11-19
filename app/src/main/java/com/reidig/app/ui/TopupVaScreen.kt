package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.shared.CUST_VA_NUMBER
import com.reidig.shared.SUPPORTED_BANKS
import com.reidig.shared.findSupportedBank
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class VaTransferTab(val label: String) {
    M_BANKING("m-Banking"),
    ATM("ATM"),
    INTERNET_BANKING("Internet banking")
}

/**
 * Layar Instruksi Top Up Virtual Account (Screen 22 Top up · Virtual Account)
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2083–2116 dan
 * spesifikasi Design System v2 & Handoff §9.
 */
@Suppress("DEPRECATION")
@Composable
fun TopupVaScreen(
    env: AppEnv,
    bankKey: String,
    onBack: () -> Unit,
    onTopupSuccess: (txId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bank = remember(bankKey) {
        findSupportedBank(bankKey) ?: SUPPORTED_BANKS.first()
    }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(VaTransferTab.M_BANKING) }
    var isLoading by remember { mutableStateOf(false) }

    // Format nomor VA per 4 digit (e.g. "3901 1002 4817 3561")
    val rawVa = remember(bank) { "${bank.vaPrefix}$CUST_VA_NUMBER" }
    val formattedVa = remember(rawVa) {
        rawVa.chunked(4).joinToString(" ")
    }

    // Langkah instruksi dinamis sesuai bank & tab
    val steps = remember(bank, selectedTab) {
        val b = bank.name
        when (selectedTab) {
            VaTransferTab.M_BANKING -> listOf(
                "Buka aplikasi mobile banking $b, lalu masuk ke akunmu.",
                "Pilih menu Transfer, lalu Virtual Account.",
                "Masukkan nomor Virtual Account di atas.",
                "Masukkan nominal top up, lalu periksa nama penerima.",
                "Konfirmasi dengan PIN mobile banking. Saldo Kas bertambah otomatis.",
            )
            VaTransferTab.ATM -> listOf(
                "Masukkan kartu ATM $b dan PIN-mu.",
                "Pilih Transaksi lainnya, Transfer, lalu Virtual Account.",
                "Masukkan nomor Virtual Account di atas.",
                "Masukkan nominal top up, lalu periksa nama penerima.",
                "Konfirmasi transaksi dan simpan struknya.",
            )
            VaTransferTab.INTERNET_BANKING -> listOf(
                "Masuk ke internet banking $b.",
                "Pilih menu Transfer, lalu Virtual Account.",
                "Masukkan nomor Virtual Account di atas.",
                "Masukkan nominal top up, lalu periksa nama penerima.",
                "Konfirmasi dengan token atau kode OTP.",
            )
        }
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
                title = "Top up via ${bank.name}",
                onBack = onBack,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Card 1: Nomor Virtual Account & Tombol Salin
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Logo Bank & Label VA
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                GoldinBankLogoBadge(bankName = bank.name)
                                Text(
                                    text = "${bank.name} Virtual Account",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Palette.Ink,
                                )
                            }

                            // Nomor VA + Copy Button
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Nomor Virtual Account",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    color = Palette.InkSecondary,
                                    letterSpacing = 0.5.sp,
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
                                        fontSize = 20.sp,
                                        color = Palette.Ink,
                                        maxLines = 1,
                                    )

                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isHovered by interactionSource.collectIsHoveredAsState()
                                    val copyBg by animateColorAsState(
                                        targetValue = if (isHovered) Palette.Primary50 else Color.Transparent,
                                        animationSpec = tween(GoldinMotion.DurationColor),
                                        label = "copyBg",
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(copyBg)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(rawVa))
                                                    env.notify("Nomor virtual account disalin")
                                                },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = PrototypeIcons.Copy,
                                            contentDescription = "Salin nomor virtual account",
                                            tint = Palette.Primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Card 2: Cara Top Up dengan Segmented Tab 3 Opsi
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Cara top up",
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Palette.Ink,
                            )

                            // Segmented Tabs Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(Palette.Neutral100)
                                    .padding(2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    VaTransferTab.entries.forEach { tab ->
                                        val isSelected = selectedTab == tab
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier
                                                            .shadow(2.dp, CircleShape)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                    } else {
                                                        Modifier
                                                            .clip(CircleShape)
                                                            .background(Color.Transparent)
                                                            .clickable { selectedTab = tab }
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = tab.label,
                                                fontFamily = BodyFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) Palette.Primary else Palette.InkSecondary,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }

                            // 5 Langkah Instruksi
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                steps.forEachIndexed { index, stepText ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Palette.Primary50),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontFamily = MonoFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = Palette.Primary,
                                            )
                                        }

                                        Text(
                                            text = stepText,
                                            fontFamily = BodyFontFamily,
                                            fontSize = 14.sp,
                                            color = Palette.Ink,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Footer: Cek status top up
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 0.5.dp, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = if (isLoading) "" else "Cek status top up",
                    onClick = {
                        if (!isLoading) {
                            isLoading = true
                            scope.launch {
                                delay(800L) // simulasi pengecekan transfer
                                val tx = env.repo.requestTopupVa(bank.key)
                                val completedTx = env.repo.simulateVaTopupSuccess(tx.id, 500_000L)
                                isLoading = false
                                onTopupSuccess(completedTx.id)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    leadingIcon = if (isLoading) null else null,
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
}

@ScreenPreview
@Composable
private fun TopupVaPreview() = ReidigTheme {
    TopupVaScreen(
        env = rememberPreviewEnv(),
        bankKey = "bca",
        onBack = {},
        onTopupSuccess = {},
    )
}
