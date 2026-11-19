package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.app.R
import com.reidig.shared.SUPPORTED_BANKS
import com.reidig.shared.SupportedBank

/**
 * Layar Pilih Metode Top Up Saldo Kas (Screen 21 Top up · Pilih metode)
 *
 * Mengimplementasikan prototipe `scratch/Main.dc.html` L2055–2081 dan
 * spesifikasi Design System v2 & Handoff §9 / §14.
 */
@Composable
fun TopupMethodScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onSelectVa: (bankKey: String) -> Unit,
    onSelectQris: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // Header 44dp
        TopTitleBar(
            title = "Top up Saldo Kas",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Card Kelompok Metode Top Up
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column {
                        // Section Virtual Account
                        Text(
                            text = "Virtual Account",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.InkSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

                        SUPPORTED_BANKS.forEach { bank ->
                            MethodRow(
                                label = "${bank.name} Virtual Account",
                                leading = { GoldinBankLogoBadge(bankName = bank.name) },
                                onClick = { onSelectVa(bank.key) },
                            )
                        }

                        // Garis pemisah hairline
                        HorizontalDivider(
                            color = Palette.Line,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

                        // Section QRIS
                        Text(
                            text = "QRIS",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.InkSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

                        MethodRow(
                            label = "QRIS",
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(40.dp)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.logo_qris),
                                        contentDescription = "QRIS",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp),
                                    )
                                }
                            },
                            onClick = onSelectQris,
                        )
                    }
                }
            }

            // Info Note Biaya Admin & Minimal Top Up
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.Info50)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.AlertCircle,
                            contentDescription = null,
                            tint = Palette.Info700,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Biaya admin Rp1.000 per top up. Minimal top up Rp10.000.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                            lineHeight = 18.sp,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Baris item pilihan metode top up dengan hover/press feedback.
 */
@Composable
private fun MethodRow(
    label: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Neutral200
            isHovered -> Palette.Neutral100
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "methodRowBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading()

        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Palette.Ink,
            modifier = Modifier.weight(1f),
        )

        Icon(
            imageVector = PrototypeIcons.ChevronRight,
            contentDescription = null,
            tint = Palette.InkSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@ScreenPreview
@Composable
private fun TopupMethodPreview() = ReidigTheme {
    TopupMethodScreen(
        env = rememberPreviewEnv(),
        onBack = {},
        onSelectVa = {},
        onSelectQris = {},
    )
}
