package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.formatNumberOnly
import com.reidig.shared.rupiah

private const val WD_MIN = 50_000L
private const val WD_FEE = 2_500L

/**
 * Layar Input Tarik Dana Kas (Screen 26 Tarik dana)
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2444–2486, L4567–4579
 * dan spesifikasi Design System v2 & Handoff §9 / §14.
 */
@Composable
fun WithdrawScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onAddBank: () -> Unit,
    onReview: (bankAccountId: String, amount: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        try {
            env.repo.fetchWallet()
        } catch (_: Exception) {}
    }
    val bankAccounts = state.bankAccounts
    val kasBal = state.walletBalance

    // Auto-select rekening pertama jika belum ada yang terpilih atau rekening sebelumnya terhapus
    var selectedAccountId by rememberSaveable(bankAccounts) {
        mutableStateOf(bankAccounts.firstOrNull()?.id)
    }

    var rawInput by rememberSaveable { mutableStateOf("") }
    val amount = rawInput.toLongOrNull() ?: 0L

    // Validasi input nominal penarikan sesuai aturan prototipe (L4494)
    val wdErr = when {
        rawInput.isEmpty() -> null
        amount < WD_MIN -> "Minimal penarikan ${rupiah(WD_MIN)}"
        amount + WD_FEE > kasBal -> "Saldo Kas tidak cukup. Maksimal ${rupiah(maxOf(0L, kasBal - WD_FEE))} (sudah termasuk biaya admin)"
        else -> null
    }
    val hasError = wdErr != null
    val wdOk = amount >= WD_MIN && !hasError && selectedAccountId != null

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
                title = "Tarik dana",
                onBack = onBack,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Kartu Saldo Kas
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Saldo Kas",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.InkSecondary,
                            )
                            Text(
                                text = rupiah(kasBal),
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Palette.Ink,
                            )
                        }
                    }
                }

                // 2. Seksi Rekening Tujuan
                if (bankAccounts.isEmpty()) {
                    // Empty state rekening
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
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
                                        imageVector = PrototypeIcons.BuildingBank,
                                        contentDescription = null,
                                        tint = Palette.Primary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }

                                Text(
                                    text = "Belum ada rekening. Tambahkan rekening atas namamu untuk menarik dana.",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Palette.InkSecondary,
                                    textAlign = TextAlign.Center,
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .border(1.dp, Palette.Primary, RoundedCornerShape(999.dp))
                                        .clickable(onClick = onAddBank),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = PrototypeIcons.Plus,
                                            contentDescription = null,
                                            tint = Palette.Primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "Tambah rekening",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Palette.Primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Kartu Radio Rekening Bank Tersimpan
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Rekening tujuan",
                                    fontFamily = TitleFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Palette.Ink,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    bankAccounts.forEach { acc ->
                                        val isSelected = selectedAccountId == acc.id
                                        val borderColor = if (isSelected) Palette.Primary else Palette.Line
                                        val bgColor = if (isSelected) Palette.Primary50 else Color.White

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(bgColor)
                                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                                .clickable { selectedAccountId = acc.id }
                                                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                // Logo Bank Badge 72x40dp
                                                GoldinBankLogoBadge(bankName = acc.bank)

                                                // Deskripsi Rekening & Nama Pemilik
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${acc.bank} ••••${acc.number.takeLast(4)}",
                                                        fontFamily = MonoFontFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        color = Palette.Ink,
                                                    )
                                                    Text(
                                                        text = acc.holder.ifBlank { state.user?.name ?: "Pengguna" },
                                                        fontFamily = BodyFontFamily,
                                                        fontSize = 12.sp,
                                                        color = Palette.InkSecondary,
                                                        letterSpacing = 0.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }

                                                // Radio Indicator Ring 20x20dp
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) Palette.Primary else Palette.Line,
                                                            CircleShape,
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(Palette.Primary),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Tombol Tambah rekening lain
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onAddBank)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = PrototypeIcons.Plus,
                                            contentDescription = null,
                                            tint = Palette.Primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = "Tambah rekening lain",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Palette.Primary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Kartu Nominal Penarikan
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Nominal penarikan",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Palette.Ink,
                                        )
                                        Text(
                                            text = " *",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Palette.Critical,
                                        )
                                    }

                                    // Tombol Tarik semua (kasBal - WD_FEE)
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable {
                                                val maxWithdrawable = maxOf(0L, kasBal - WD_FEE)
                                                rawInput = if (maxWithdrawable > 0L) maxWithdrawable.toString() else "0"
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Tarik semua",
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Palette.Primary,
                                        )
                                    }
                                }

                                // Input Field dengan Prefix "Rp"
                                val inputBorderColor = if (hasError) Palette.Critical else Palette.Line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "Rp",
                                            fontFamily = MonoFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = Palette.InkSecondary,
                                        )
                                        Spacer(Modifier.width(8.dp))

                                        BasicTextField(
                                            value = if (rawInput.isNotEmpty()) formatNumberOnly(amount) else "",
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }.take(12).trimStart('0')
                                                rawInput = digits
                                            },
                                            textStyle = TextStyle(
                                                fontFamily = BodyFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = Palette.Ink,
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            cursorBrush = SolidColor(Palette.Primary),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (rawInput.isEmpty()) {
                                                    Text(
                                                        text = "0",
                                                        fontFamily = BodyFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 14.sp,
                                                        color = Palette.Disabled,
                                                    )
                                                }
                                                innerTextField()
                                            },
                                        )
                                    }
                                }

                                // Validasi Pesan Error atau Helper Note
                                if (wdErr != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = PrototypeIcons.AlertCircle,
                                            contentDescription = null,
                                            tint = Palette.Critical,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = wdErr,
                                            fontFamily = BodyFontFamily,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp,
                                            color = Palette.Critical,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Minimal penarikan Rp50.000 · biaya admin Rp2.500",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Palette.InkSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Footer CTA
            if (bankAccounts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(width = 0.5.dp, color = Palette.Line)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    val ctaText = if (selectedAccountId == null) "Pilih rekening tujuan" else "Selanjutnya"
                    PrimaryButton(
                        text = ctaText,
                        onClick = {
                            val accId = selectedAccountId
                            if (accId != null && wdOk) {
                                onReview(accId, amount)
                            }
                        },
                        enabled = wdOk,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
