package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.shared.MIN_TOPUP_RP
import com.reidig.shared.TOPUP_FEE
import com.reidig.shared.formatNumberOnly
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val QUICK_TU_AMOUNTS = listOf(50_000L, 100_000L, 250_000L, 500_000L)

/**
 * Layar Input Nominal Top Up QRIS (Screen 23 Top up · QRIS nominal)
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2118–2144 dan
 * spesifikasi Design System v2 & Handoff §9 / §14.
 */
@Composable
fun TopupQrisScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onQrCreated: (txId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var rawInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val amount = rawInput.toLongOrNull() ?: 0L
    val hasError = amount > 0L && amount < MIN_TOPUP_RP
    val errorMessage = if (hasError) "Minimal top up ${rupiah(MIN_TOPUP_RP)}" else null
    val isValid = amount >= MIN_TOPUP_RP

    // Formatter ribuan
    val decimalFormat = remember {
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.forLanguageTag("id-ID")))
    }
    val formattedDisplay = if (amount > 0L) decimalFormat.format(amount) else ""

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
                title = "Top up via QRIS",
                onBack = onBack,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Card 1: Input Field Nominal Top Up & Quick Chips
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nominal top up",
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

                            // Input Field dengan Prefix "Rp"
                            val borderColor = if (hasError) Palette.Critical else Palette.Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
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
                                        value = formattedDisplay,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() }.take(10).trimStart('0')
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

                            // Pesan Error atau Help Text
                            if (hasError && errorMessage != null) {
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
                                        text = errorMessage,
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.Critical,
                                    )
                                }
                            } else {
                                Text(
                                    text = "Minimal top up ${rupiah(MIN_TOPUP_RP)}",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    color = Palette.InkSecondary,
                                    letterSpacing = 0.5.sp,
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            // Grid 2x2 Quick Chips
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    QuickChip(
                                        amount = QUICK_TU_AMOUNTS[0],
                                        isSelected = amount == QUICK_TU_AMOUNTS[0],
                                        onClick = { rawInput = QUICK_TU_AMOUNTS[0].toString() },
                                        modifier = Modifier.weight(1f),
                                    )
                                    QuickChip(
                                        amount = QUICK_TU_AMOUNTS[1],
                                        isSelected = amount == QUICK_TU_AMOUNTS[1],
                                        onClick = { rawInput = QUICK_TU_AMOUNTS[1].toString() },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    QuickChip(
                                        amount = QUICK_TU_AMOUNTS[2],
                                        isSelected = amount == QUICK_TU_AMOUNTS[2],
                                        onClick = { rawInput = QUICK_TU_AMOUNTS[2].toString() },
                                        modifier = Modifier.weight(1f),
                                    )
                                    QuickChip(
                                        amount = QUICK_TU_AMOUNTS[3],
                                        isSelected = amount == QUICK_TU_AMOUNTS[3],
                                        onClick = { rawInput = QUICK_TU_AMOUNTS[3].toString() },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Rincian (Rp) (Hanya muncul jika amount > 0)
                item {
                    AnimatedVisibility(
                        visible = amount > 0L,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
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
                                        text = formatNumberOnly(TOPUP_FEE),
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
                                        text = rupiah(amount + TOPUP_FEE),
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

            // Sticky Footer: Buat kode QR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 0.5.dp, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = if (isLoading) "" else "Buat kode QR",
                    onClick = {
                        if (isValid && !isLoading) {
                            isLoading = true
                            scope.launch {
                                delay(600L) // simulasi pembuatan order QRIS
                                val tx = env.repo.createTopupQris(amount)
                                isLoading = false
                                onQrCreated(tx.id)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isValid && !isLoading,
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

/**
 * Chip pilihan nominal cepat dengan format Geist Mono 14sp.
 */
@Composable
private fun QuickChip(
    amount: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) Palette.Primary else Color.White
    val border = if (isSelected) Palette.Primary else Palette.Line
    val textColor = if (isSelected) Color.White else Palette.Ink

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rupiah(amount),
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = textColor,
        )
    }
}

@ScreenPreview
@Composable
private fun TopupQrisPreview() = ReidigTheme {
    TopupQrisScreen(
        env = rememberPreviewEnv(),
        onBack = {},
        onQrCreated = {},
    )
}
