package com.reidig.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.app.R
import com.reidig.shared.MockRepository
import com.reidig.shared.PinVerifyResult
import com.reidig.shared.ReidigException
import com.reidig.shared.Rules
import com.reidig.shared.formatGrams4
import com.reidig.shared.rupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
fun BrandMark(size: Int = 40) {
    Image(
        painterResource(R.drawable.logo_rei),
        contentDescription = "Logo Raja Emas Indonesia",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size / 3.5f).dp)),
    )
}

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(600)
        onDone()
    }
    Column(
        Modifier.fillMaxSize().background(Palette.Paper).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        GoldBarArt(220.dp)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoldBarsBadge(40.dp, 22.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Goldin",
                color = Palette.Primary,
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Tabungan & Cicilan Emas Fisik 24K",
            color = Palette.InkSecondary,
            fontFamily = BodyFontFamily,
            fontSize = 14.sp,
        )
        Spacer(Modifier.weight(1f))
        GoldinSpinner(size = 24.dp, color = Palette.Primary700)
        Spacer(Modifier.height(32.dp))
    }
}

@ScreenPreview
@Composable
private fun SplashPreview() = GoldinTheme { SplashScreen {} }

@Composable
fun WelcomeScreen(env: AppEnv, onLogin: () -> Unit, onRegister: () -> Unit) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val buyPrice = state.askPricePerGram.takeIf { it > 0 } ?: state.sku("LMRE-1G").buyPrice
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // Auto-advance slide setiap 4.5 detik sesuai prototipe
    // Gunakan LaunchedEffect(pagerState) dengan loop while agar tidak ter-cancel di tengah-tengah animasi saat currentPage berpindah
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4500)
            if (!pagerState.isScrollInProgress) {
                val targetPage = (pagerState.currentPage + 1) % 3
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    // Slide 2: simulasi nominal berputar otomatis setiap 1.5 detik jika berada di slide 2
    val simulationAmounts = remember { listOf(10_000L, 50_000L, 100_000L, 500_000L) }
    var selectedAmountIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            while (true) {
                delay(1500)
                selectedAmountIndex = (selectedAmountIndex + 1) % simulationAmounts.size
            }
        }
    }
    val currentSimAmount = simulationAmounts[selectedAmountIndex]

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top header: Brand "Goldin" (Lato Black 22sp) + Chip Harga Emas Terkini (pilihan 44dp tinggi)
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Goldin",
                color = Palette.Primary,
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.3).sp,
            )
            Row(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .border(1.dp, Palette.Neutral200, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                // Titik hijau berdenyut halus
                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "pulseScale",
                )
                Box(
                    Modifier
                        .size((7 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(Palette.Good),
                )
                Text(
                    "Emas",
                    color = Palette.InkSecondary,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                )
                Text(
                    "${rupiah(buyPrice)}/gr",
                    color = Palette.Ink,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
                Text(
                    "▲ 0,4%",
                    color = Palette.Good,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                )
            }
        }

        Spacer(Modifier.weight(0.5f))

        // 3-Slide Interactive Carousel
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (page) {
                    0 -> {
                        // Slide 1: Hero Emas Batangan 3D dengan animasi melayang 5s
                        Box(Modifier.height(300.dp), contentAlignment = Alignment.Center) {
                            GoldBarArt(280.dp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(
                            Modifier.height(88.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Nabung emas tanpa ribet",
                                color = Palette.Primary,
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Beli emas kapan saja, cukup dari ponsel.",
                                color = Palette.InkSecondary,
                                fontFamily = BodyFontFamily,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    1 -> {
                        // Slide 2: Interactive Simulation Card Preview (Lingkaran konsentris + Kartu simulasi)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Lingkaran luar ungu muda (290dp)
                            Box(
                                Modifier
                                    .size(290.dp)
                                    .clip(CircleShape)
                                    .background(Palette.Primary50),
                            )
                            // Lingkaran tengah putus-putus (220dp)
                            Box(
                                Modifier
                                    .size(220.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = Palette.Primary200,
                                            style = Stroke(
                                                width = 1.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f),
                                            ),
                                        )
                                    },
                            )
                            // Kartu kalkulator simulasi
                            Column(
                                Modifier
                                    .width(260.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Xxl))
                                    .background(Color.White)
                                    .border(1.dp, Palette.Neutral200, RoundedCornerShape(GoldinRadius.Xxl))
                                    .shadow(8.dp, RoundedCornerShape(GoldinRadius.Xxl), ambientColor = Palette.Primary950.copy(alpha = 0.08f), spotColor = Palette.Primary950.copy(alpha = 0.08f))
                                    .padding(16.dp),
                            ) {
                                Text(
                                    "Kamu nabung",
                                    color = Palette.InkSecondary,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    rupiah(currentSimAmount),
                                    color = Palette.Primary,
                                    fontFamily = TitleFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    lineHeight = 32.sp,
                                )
                                // Divider dengan badge panah bolak-balik
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(Modifier.weight(1f).height(1.dp).background(Palette.Neutral200))
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Palette.GoldSoft),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            PrototypeIcons.ArrowsUpDown,
                                            contentDescription = null,
                                            tint = Palette.GoldDeep,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Box(Modifier.weight(1f).height(1.dp).background(Palette.Neutral200))
                                }
                                Text(
                                    "Kamu dapat",
                                    color = Palette.InkSecondary,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                )
                                Spacer(Modifier.height(2.dp))
                                val estimatedGrams = currentSimAmount.toDouble() / buyPrice.toDouble()
                                Text(
                                    formatGrams4(estimatedGrams),
                                    color = Palette.GoldDeep,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                )
                                Spacer(Modifier.height(16.dp))
                                // Chips pilihan nominal
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf("10rb", "50rb", "100rb", "500rb").forEachIndexed { idx, label ->
                                        val isSelected = selectedAmountIndex == idx
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (isSelected) Palette.Primary else Color.White)
                                                .border(1.dp, if (isSelected) Palette.Primary else Palette.Line, RoundedCornerShape(999.dp))
                                                .clickable { selectedAmountIndex = idx },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                label,
                                                color = if (isSelected) Palette.Primary50 else Palette.InkSecondary,
                                                fontFamily = BodyFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(
                            Modifier.height(88.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Mulai dari Rp10.000",
                                color = Palette.Primary,
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Nabung emas mulai dari nominal kecil",
                                color = Palette.InkSecondary,
                                fontFamily = BodyFontFamily,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    2 -> {
                        // Slide 3: Keamanan & Brankas (Badge perisai besar + 3 floating badges beranimasi)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Lingkaran luar ungu muda
                            Box(
                                Modifier
                                    .size(290.dp)
                                    .clip(CircleShape)
                                    .background(Palette.Primary50),
                            )
                            // Badge Perisai Utama di tengah
                            Box(
                                Modifier
                                    .size(104.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Palette.Primary)
                                    .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Palette.Primary.copy(alpha = 0.35f), spotColor = Palette.Primary.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    PrototypeIcons.ShieldCheck,
                                    contentDescription = null,
                                    tint = Palette.Gold,
                                    modifier = Modifier.size(56.dp),
                                )
                            }
                            // 3 Floating Security Badges dengan delay bertingkat
                            AnimatedSecurityPill(
                                text = "Emas 24 karat",
                                icon = PrototypeIcons.Diamond,
                                iconBg = Palette.GoldSoft,
                                iconTint = Palette.GoldDeep,
                                durationMillis = 4000,
                                delayMillis = 0,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 48.dp, start = 4.dp),
                            )
                            AnimatedSecurityPill(
                                text = "Disimpan di brankas",
                                icon = PrototypeIcons.BuildingBank,
                                iconBg = Palette.Primary100,
                                iconTint = Palette.Primary,
                                durationMillis = 4000,
                                delayMillis = 1300,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(top = 16.dp, end = 2.dp),
                            )
                            AnimatedSecurityPill(
                                text = "Bisa tarik fisik",
                                icon = PrototypeIcons.PackageBox,
                                iconBg = Palette.GoodSoft,
                                iconTint = Palette.Good,
                                durationMillis = 4000,
                                delayMillis = 600,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(bottom = 36.dp, start = 20.dp),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(
                            Modifier.height(88.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Aman dan Tercatat",
                                color = Palette.Primary,
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Setiap gram tersimpan di brankas, siap ditarik kapan saja.",
                                color = Palette.InkSecondary,
                                fontFamily = BodyFontFamily,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Indicator Dots Carousel
        Row(
            Modifier.padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { idx ->
                val active = pagerState.currentPage == idx
                val width by animateDpAsState(
                    targetValue = if (active) 24.dp else 8.dp,
                    animationSpec = tween(350),
                    label = "dotWidth",
                )
                val color by animateColorAsState(
                    targetValue = if (active) Palette.Primary else Palette.Primary200,
                    animationSpec = tween(350),
                    label = "dotColor",
                )
                Box(
                    Modifier
                        .width(width)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color)
                        .clickable { scope.launch { pagerState.animateScrollToPage(idx) } },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Tombol Aksi di Bawah (Masuk di atas, Buat akun di bawah)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                text = "Masuk",
                onClick = onLogin,
            )
            SecondaryButton(
                text = "Buat akun",
                onClick = onRegister,
            )
        }
    }
}

/** Pill Keamanan mengambang dengan animasi float halus dan staggered delay */
@Composable
private fun AnimatedSecurityPill(
    text: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    durationMillis: Int = 4000,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
) {
    val floatTransition = rememberInfiniteTransition(label = "floatAnim")
    val offsetY by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pillFloatY",
    )

    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .offset(y = offsetY.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Palette.Neutral200, shape)
            .shadow(4.dp, shape, ambientColor = Palette.Primary950.copy(alpha = 0.08f), spotColor = Palette.Primary950.copy(alpha = 0.08f))
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Palette.Ink,
        )
    }
}

@ScreenPreview
@Composable
private fun WelcomePreview() = GoldinTheme { WelcomeScreen(rememberPreviewEnv(false), {}, {}) }

/**
 * Helper animasi fadeUp masuk (opacity 0 -> 1, translationY 12dp -> 0dp)
 * persis sesuai prototipe CSS @keyframes fadeUp { from { opacity:0; transform:translateY(12px) } to { opacity:1; transform:none } }
 */
@Composable
private fun rememberFadeUpModifier(delayMillis: Int, durationMillis: Int = 400): Modifier {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        startAnim = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "fadeUpAlpha$delayMillis",
    )
    val offsetY by animateDpAsState(
        targetValue = if (startAnim) 0.dp else 12.dp,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "fadeUpOffsetY$delayMillis",
    )
    return Modifier.graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetY.toPx()
    }
}

@Composable
fun LoginScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onGoogle: () -> Unit,
    toastMessage: String? = null,
    onToastDismissed: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var credentialError by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    suspend fun triggerShake() {
        shakeOffset.snapTo(0f)
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(0f, tween(70, easing = LinearEasing))
    }

    fun handleSubmit() {
        if (loading) return
        val trimmedEmail = email.trim()
        var hasValidationErr = false

        if (trimmedEmail.isEmpty()) {
            emailError = "Email wajib diisi"
            hasValidationErr = true
        } else if (!com.reidig.shared.Rules.isValidEmail(trimmedEmail)) {
            emailError = "Format email belum benar"
            hasValidationErr = true
        }

        if (password.isEmpty()) {
            passwordError = "Kata sandi wajib diisi"
            hasValidationErr = true
        }

        if (hasValidationErr) {
            scope.launch { triggerShake() }
            return
        }

        loading = true
        scope.launch {
            kotlinx.coroutines.delay(900)
            try {
                env.repo.login(trimmedEmail, password)
                loading = false
                onLoggedIn()
            } catch (e: ReidigException) {
                loading = false
                credentialError = true
                triggerShake()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Palette.Paper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
        // Tombol Kembali bulat 44dp dengan hover/press sesuai prototipe
        GoldinIconButton(
            icon = PrototypeIcons.ChevronLeft,
            contentDescription = "Kembali",
            onClick = onBack,
            hoverColor = Palette.Neutral100,
            modifier = Modifier.offset(x = (-10).dp),
        )

        // Title dengan animasi fadeUp delay 0ms
        Text(
            text = "Halo,\nSelamat datang kembali!",
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            color = Palette.Primary700,
            modifier = Modifier
                .padding(top = 24.dp)
                .then(rememberFadeUpModifier(delayMillis = 0)),
        )

        // Container Form Input dengan animasi shake
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .graphicsLayer {
                    translationX = shakeOffset.value.dp.toPx()
                },
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Field Email dengan animasi fadeUp delay 60ms
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rememberFadeUpModifier(delayMillis = 60)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Email",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Ink,
                    )
                    Text(
                        "*",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Bad,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                GoldinInputField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                        credentialError = false
                    },
                    placeholder = "Masukkan email",
                    leadingIcon = PrototypeIcons.Mail,
                    keyboardType = KeyboardType.Email,
                    isError = emailError != null || credentialError,
                )
                if (emailError != null) {
                    FieldErrorMessage(emailError!!)
                }
            }

            // Field Kata Sandi dengan animasi fadeUp delay 120ms
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rememberFadeUpModifier(delayMillis = 120)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Kata sandi",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Ink,
                    )
                    Text(
                        "*",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Bad,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                GoldinInputField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                        credentialError = false
                    },
                    placeholder = "Masukkan kata sandi",
                    leadingIcon = PrototypeIcons.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null || credentialError,
                    trailingIcon = {
                        GoldinIconButton(
                            icon = if (showPassword) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                            contentDescription = if (showPassword) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                            onClick = { showPassword = !showPassword },
                            tint = Palette.InkSecondary,
                            hoverColor = Palette.Neutral100,
                        )
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        when {
                            passwordError != null -> FieldErrorMessage(passwordError!!)
                            credentialError -> FieldErrorMessage("Email atau kata sandi salah")
                        }
                    }
                    TextLink(
                        text = "Lupa kata sandi?",
                        onClick = onForgot,
                    )
                }
            }
        }

        // Tombol Masuk dengan animasi fadeUp delay 180ms
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .then(rememberFadeUpModifier(delayMillis = 180)),
        ) {
            PrimaryButton(
                text = "Masuk",
                onClick = { handleSubmit() },
                loading = loading,
            )
        }

        // Bagian Bawah: Statis tanpa animasi fadeUp!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.weight(1f).height(1.5.dp).background(Palette.Neutral300))
            Text("atau", fontFamily = BodyFontFamily, fontSize = 14.sp, color = Palette.Neutral600)
            Box(Modifier.weight(1f).height(1.5.dp).background(Palette.Neutral300))
        }

        GoogleButton(
            text = "Masuk dengan Google",
            onClick = onGoogle,
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Belum memiliki akun?",
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                color = Palette.Neutral600,
            )
            TextLink("Daftar", onRegister)
        }
        }
        GoldinToast(
            message = toastMessage,
            onDismiss = onToastDismissed,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@ScreenPreview
@Composable
private fun LoginPreview() = GoldinTheme { LoginScreen(rememberPreviewEnv(false), {}, {}, {}, {}, {}) }

private sealed class PinModalType {
    object BioLogin : PinModalType()
    data class PinLock(val minutes: Int) : PinModalType()
    object AccountLock : PinModalType()
    object BioIn : PinModalType()
}

/**
 * Layar Masukkan PIN sesuai prototipe 02b Login · PIN & dokumen handoff v1.2.
 * Menampilkan:
 * - Header tombol kembali bundar (hanya saat isFirstTime == true)
 * - Logo box ungu 56x56dp dengan 3 batang emas + wordmark "Goldin" Lato 900 24sp
 * - Judul "Masukkan PIN" Lato 700 18sp
 * - 6 Box PIN: 48x48dp, corner 12dp. Kotak aktif memiliki outer halo ring ungu muda 3.5dp (sama seperti focus input form).
 * - Animasi pop bouncy saat digit PIN diisi (pop .2s)
 * - Animasi shake saat PIN salah
 * - Animasi bottom sheet slide-up (sheetUp .35s bezier) saat 3x salah memasukkan PIN ("PIN terkunci sementara")
 * - Pesan error & sisa kesempatan di bawah kotak PIN
 * - Link "Lupa PIN?"
 * - Keypad numerik 3x4 (1-9, 0, backspace, tombol biometrik jika bukan login pertama)
 */
@Composable
fun LoginPinScreen(
    env: AppEnv,
    isFirstTime: Boolean = true,
    autoOpenBiometric: Boolean = false,
    toastMessage: String? = null,
    onToastDismissed: () -> Unit = {},
    onBack: () -> Unit,
    onPinSuccess: () -> Unit,
    onForgotPin: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf(if (autoOpenBiometric) "123456" else "") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var activeModal by remember {
        mutableStateOf<PinModalType?>(
            when {
                autoOpenBiometric -> PinModalType.BioLogin
                !isFirstTime -> PinModalType.BioIn
                else -> null
            },
        )
    }

    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    suspend fun triggerShake() {
        shakeOffset.snapTo(0f)
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(0f, tween(70, easing = LinearEasing))
    }

    fun handleDigit(d: String) {
        if (pin.length >= 6 || isChecking || activeModal != null) return
        val nextPin = pin + d
        pin = nextPin
        errorText = null

        if (nextPin.length == 6) {
            isChecking = true
            scope.launch {
                delay(400)
                when (val res = env.repo.checkLoginPin(nextPin)) {
                    is PinVerifyResult.Success -> {
                        isChecking = false
                        if (isFirstTime) {
                            activeModal = PinModalType.BioLogin
                        } else {
                            onPinSuccess()
                        }
                    }
                    is PinVerifyResult.Incorrect -> {
                        isChecking = false
                        errorText = "PIN salah. Sisa ${res.remainingAttempts} kesempatan lagi"
                        triggerShake()
                        delay(700)
                        pin = ""
                    }
                    is PinVerifyResult.TemporarilyLocked -> {
                        isChecking = false
                        // Animasi shake getar saat percobaan ke-3 salah sebelum drawer muncul
                        triggerShake()
                        delay(400)
                        pin = ""
                        errorText = null
                        activeModal = PinModalType.PinLock(res.lockMinutes)
                    }
                    is PinVerifyResult.AccountLocked -> {
                        isChecking = false
                        triggerShake()
                        delay(400)
                        pin = ""
                        errorText = null
                        activeModal = PinModalType.AccountLock
                    }
                }
            }
        }
    }

    fun handleDelete() {
        if (pin.isNotEmpty() && !isChecking && activeModal == null) {
            pin = pin.dropLast(1)
            errorText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (isFirstTime) {
                    GoldinIconButton(
                        icon = PrototypeIcons.ChevronLeft,
                        contentDescription = "Kembali",
                        onClick = onBack,
                        hoverColor = Palette.Neutral100,
                        modifier = Modifier.offset(x = (-10).dp),
                    )
                }
            }

            // Logo Box + Wordmark dengan animasi fadeUp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .then(rememberFadeUpModifier(delayMillis = 0)),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Primary700),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PrototypeIcons.GoldIngots,
                        contentDescription = null,
                        tint = Palette.Gold,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Goldin",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.3).sp,
                    color = Palette.Primary700,
                )
            }

            // Judul
            Text(
                text = "Masukkan PIN",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Palette.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp),
            )

            val caretTransition = rememberInfiniteTransition(label = "caretTransition")
            val caretAlpha by caretTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        1f at 0
                        1f at 499
                        0f at 500
                        0f at 999
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "caretBlink",
            )

            val innerShape = RoundedCornerShape(12.dp)
            val outerShape = RoundedCornerShape(15.5.dp)

            // 6 Kotak PIN dengan outer halo ring konsentris 3.5dp dan animasi shake
            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .graphicsLayer {
                        translationX = shakeOffset.value.dp.toPx()
                    },
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(6) { index ->
                    val hasDigit = index < pin.length
                    val isActive = !isChecking && errorText == null && index == pin.length
                    val isError = errorText != null

                    val borderColor = when {
                        isError -> Palette.Bad
                        isActive || hasDigit -> Palette.Primary700
                        else -> Palette.Neutral300
                    }

                    // Kontainer 55x55dp: saat aktif, background Primary200 membentuk outer halo ring ungu muda 3.5dp konsentris.
                    // Saat tidak aktif atau terisi, background Transparent (bersih tanpa residu abu-abu).
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .background(
                                color = if (isActive && !isError) Palette.Primary200 else Color.Transparent,
                                shape = outerShape,
                            )
                            .padding(3.5.dp)
                            .background(Color.White, innerShape)
                            .border(
                                width = if (isActive || hasDigit || isError) 1.2.dp else 1.dp,
                                color = borderColor,
                                shape = innerShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasDigit) {
                            // Animasi pop bouncy saat digit diisi
                            PopPinDot(isError = isError)
                        } else if (isActive) {
                            // Garis kursor vertikal berkedip (blink 1s) sesuai pedoman prototype
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .graphicsLayer { alpha = caretAlpha }
                                    .background(Palette.Primary700),
                            )
                        }
                    }
                }
            }

            // Status Error / Loader Checking
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isChecking -> {
                        GoldinSpinner(size = 20.dp, color = Palette.Primary700)
                    }
                    errorText != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.then(rememberFadeUpModifier(delayMillis = 0, durationMillis = 200)),
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.AlertCircle,
                                contentDescription = null,
                                tint = Palette.Bad,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = errorText!!,
                                fontFamily = BodyFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                letterSpacing = 0.5.sp,
                                color = Palette.Bad,
                            )
                        }
                    }
                }
            }

            // Link Lupa PIN?
            TextLink(text = "Lupa PIN?", onClick = onForgotPin)

            Spacer(Modifier.weight(1f))

            // Keypad Numerik 3x4
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PinKeypadButton("1", Modifier.weight(1f)) { handleDigit("1") }
                    PinKeypadButton("2", Modifier.weight(1f)) { handleDigit("2") }
                    PinKeypadButton("3", Modifier.weight(1f)) { handleDigit("3") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PinKeypadButton("4", Modifier.weight(1f)) { handleDigit("4") }
                    PinKeypadButton("5", Modifier.weight(1f)) { handleDigit("5") }
                    PinKeypadButton("6", Modifier.weight(1f)) { handleDigit("6") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PinKeypadButton("7", Modifier.weight(1f)) { handleDigit("7") }
                    PinKeypadButton("8", Modifier.weight(1f)) { handleDigit("8") }
                    PinKeypadButton("9", Modifier.weight(1f)) { handleDigit("9") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isFirstTime) {
                        PinKeypadButton(
                            icon = PrototypeIcons.Fingerprint,
                            iconTint = Palette.Primary700,
                            modifier = Modifier.weight(1f),
                            onClick = { activeModal = PinModalType.BioIn },
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    PinKeypadButton("0", Modifier.weight(1f)) { handleDigit("0") }
                    PinKeypadButton(
                        icon = PrototypeIcons.Backspace,
                        iconTint = Palette.Ink,
                        modifier = Modifier.weight(1f),
                        onClick = { handleDelete() },
                    )
                }
            }
        }

        // Modal / BottomSheet Overlay dengan Animasi Slide-Up (sheetUp .35s bezier) dan Fade Scrim
        val showModal = activeModal != null
        var lastModal by remember { mutableStateOf<PinModalType?>(null) }
        if (activeModal != null) {
            lastModal = activeModal
        }

        val scrimAlpha by animateFloatAsState(
            targetValue = if (showModal) 0.4f else 0f,
            animationSpec = tween(GoldinMotion.DurationHover, easing = LinearEasing),
            label = "scrimAlpha",
        )

        if (scrimAlpha > 0f || showModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Palette.Primary950.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (activeModal !is PinModalType.AccountLock) {
                                val wasBioLogin = activeModal is PinModalType.BioLogin
                                activeModal = null
                                if (wasBioLogin) {
                                    env.repo.setBiometricEnabled(false)
                                    onPinSuccess()
                                }
                            }
                        },
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                AnimatedVisibility(
                    visible = showModal,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(GoldinMotion.DurationSheet, easing = GoldinMotion.EaseOutSheet),
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(GoldinMotion.DurationModal, easing = GoldinMotion.EaseOutSheet),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                            .background(Color.White)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            )
                            .navigationBarsPadding()
                            .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Xs, bottom = GoldinSpace.Xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Handle bar (36x4dp neutral-300 sesuai prototipe)
                        Box(
                            Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(GoldinRadius.Full))
                                .background(Palette.Neutral300),
                        )
                        Spacer(Modifier.height(GoldinSpace.Xl))

                        when (val modal = activeModal ?: lastModal) {
                            is PinModalType.BioLogin -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Palette.Primary50),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = PrototypeIcons.Fingerprint,
                                        contentDescription = null,
                                        tint = Palette.Primary700,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(Modifier.height(GoldinSpace.Md))
                                Text(
                                    text = "Aktifkan biometrik?",
                                    style = GoldinTypography.titleLg,
                                    color = Palette.Ink,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xs))
                                Text(
                                    text = "Masuk dan konfirmasi transaksi lebih cepat pakai sidik jari atau wajah.",
                                    style = GoldinTypography.bodyMd,
                                    color = Palette.InkSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xl))
                                PairedButtons(
                                    leftText = "Nanti saja",
                                    onLeftClick = {
                                        activeModal = null
                                        env.repo.setBiometricEnabled(false)
                                        onPinSuccess()
                                    },
                                    rightText = "Aktifkan",
                                    onRightClick = {
                                        activeModal = null
                                        env.repo.setBiometricEnabled(true)
                                        onPinSuccess()
                                    },
                                )
                            }
                            is PinModalType.PinLock -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Palette.BadSoft),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = PrototypeIcons.AlertCircle,
                                        contentDescription = null,
                                        tint = Palette.Bad,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(Modifier.height(GoldinSpace.Md))
                                Text(
                                    text = "PIN terkunci sementara",
                                    style = GoldinTypography.titleLg,
                                    color = Palette.Ink,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xs))
                                Text(
                                    text = "Kamu sudah 3 kali salah memasukkan PIN. Coba lagi dalam ${modal.minutes} menit.",
                                    style = GoldinTypography.bodyMd,
                                    color = Palette.InkSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xl))
                                PrimaryButton(
                                    text = "Tutup",
                                    onClick = {
                                        activeModal = null
                                        errorText = null
                                    },
                                )
                            }
                            is PinModalType.AccountLock -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Palette.BadSoft),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = PrototypeIcons.AlertCircle,
                                        contentDescription = null,
                                        tint = Palette.Bad,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(Modifier.height(GoldinSpace.Md))
                                Text(
                                    text = "Akun terkunci",
                                    style = GoldinTypography.titleLg,
                                    color = Palette.Ink,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xs))
                                Text(
                                    text = "Atur ulang PIN untuk membuka akunmu.",
                                    style = GoldinTypography.bodyMd,
                                    color = Palette.InkSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xl))
                                PrimaryButton(
                                    text = "Atur ulang PIN",
                                    onClick = {
                                        activeModal = null
                                        onForgotPin()
                                    },
                                )
                            }
                            is PinModalType.BioIn -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Palette.Primary50)
                                        .clickable {
                                            activeModal = null
                                            env.repo.setBiometricEnabled(true)
                                            onPinSuccess()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = PrototypeIcons.Fingerprint,
                                        contentDescription = "Pindai sidik jari",
                                        tint = Palette.Primary700,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(Modifier.height(GoldinSpace.Md))
                                Text(
                                    text = "Masuk dengan biometrik",
                                    style = GoldinTypography.titleLg,
                                    color = Palette.Ink,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xs))
                                Text(
                                    text = "Gunakan sidik jari atau wajah untuk masuk.",
                                    style = GoldinTypography.bodyMd,
                                    color = Palette.InkSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(GoldinSpace.Xl))
                                SecondaryButton(
                                    text = "Masuk dengan PIN",
                                    filledBackground = Color.Transparent,
                                    onClick = {
                                        activeModal = null
                                        onPinSuccess()
                                    },
                                )
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
        GoldinToast(
            message = toastMessage,
            onDismiss = onToastDismissed,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PinKeypadButton(
    label: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = Palette.Ink,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(100),
        label = "keyScale",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "keyBg",
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(
                text = label,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                color = Palette.Ink,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun LoginPinPreview() = GoldinTheme { LoginPinScreen(rememberPreviewEnv(false), isFirstTime = true, onBack = {}, onPinSuccess = {}, onForgotPin = {}) }

/**
 * Layar Form Lupa PIN sesuai Gambar 1 & Pedoman Prototype:
 * - Header tombol kembali
 * - Judul "Lupa PIN" Lato 900 24sp rata kiri tanpa subtitle dengan animasi fade up
 * - Form email & kata sandi dengan animasi shake jika submit gagal / kredensial salah
 * - Input email & kata sandi memiliki outer halo ring ungu muda 3.5dp konsentris saat fokus/hover
 * - Tautan "Lupa kata sandi?" rata kanan
 * - Tombol "Lanjut" (PrimaryButton 44dp pill)
 */
@Composable
fun ForgotPinPasswordScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onVerified: (email: String) -> Unit,
    toastMessage: String? = null,
    onToastDismissed: () -> Unit = {},
) {
    val userState by env.repo.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf(userState.user?.email ?: "reiuser@gmail.com") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val formShakeOffset = remember { Animatable(0f) }

    suspend fun triggerFormShake() {
        formShakeOffset.snapTo(0f)
        formShakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        formShakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        formShakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        formShakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        formShakeOffset.animateTo(0f, tween(70, easing = LinearEasing))
    }

    fun handleSubmit() {
        if (loading) return
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email dan kata sandi wajib diisi"
            scope.launch { triggerFormShake() }
            return
        }
        loading = true
        errorMessage = null
        scope.launch {
            delay(300)
            try {
                env.repo.verifyAccountForResetPin(email, password)
                loading = false
                onVerified(email.trim())
            } catch (e: Exception) {
                loading = false
                errorMessage = e.message ?: "Email atau kata sandi salah"
                triggerFormShake()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // Header: Tombol Kembali
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GoldinControl.TouchMin),
                contentAlignment = Alignment.CenterStart,
            ) {
                GoldinIconButton(
                    icon = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    hoverColor = Palette.Neutral100,
                    modifier = Modifier.offset(x = (-10).dp),
                )
            }

            // Judul "Lupa PIN" dengan animasi fadeUp
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .then(rememberFadeUpModifier(delayMillis = 0, durationMillis = 400)),
            ) {
                Text(
                    text = "Lupa PIN",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Palette.Primary700,
                )
            }

            // Form Isian dengan Animasi Shake
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .graphicsLayer {
                        translationX = formShakeOffset.value.dp.toPx()
                    },
            ) {
                // Field Email
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Email",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                    Text(
                        text = " *",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Palette.Bad,
                    )
                }
                Spacer(Modifier.height(8.dp))
                GoldinInputField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    placeholder = "Masukkan email",
                    leadingIcon = PrototypeIcons.Mail,
                    keyboardType = KeyboardType.Email,
                    isError = errorMessage != null,
                )

                Spacer(Modifier.height(18.dp))

                // Field Kata sandi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kata sandi",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                    Text(
                        text = " *",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Palette.Bad,
                    )
                }
                Spacer(Modifier.height(8.dp))
                GoldinInputField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    placeholder = "Masukkan kata sandi",
                    leadingIcon = PrototypeIcons.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        GoldinIconButton(
                            icon = if (passwordVisible) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                            contentDescription = if (passwordVisible) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                            onClick = { passwordVisible = !passwordVisible },
                            tint = Palette.InkSecondary,
                            hoverColor = Palette.Neutral100,
                        )
                    },
                    isError = errorMessage != null,
                )

                // Link "Lupa kata sandi?" rata kanan
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextLink(
                        text = "Lupa kata sandi?",
                        onClick = onForgotPassword,
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    FieldErrorMessage(errorMessage ?: "")
                }
            }

            Spacer(Modifier.weight(1f))

            // Tombol "Lanjut"
            PrimaryButton(
                text = "Lanjut",
                enabled = email.isNotBlank() && password.isNotBlank() && !loading,
                loading = loading,
                onClick = ::handleSubmit,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        GoldinToast(
            message = toastMessage,
            onDismiss = onToastDismissed,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private sealed class ForgotOtpState {
    object Idle : ForgotOtpState()
    object Verifying : ForgotOtpState()
    object Success : ForgotOtpState()
}

/**
 * Layar Verifikasi OTP sesuai Gambar 2, Gambar 3 & Pedoman Prototype:
 * - Animasi fade up pada title & keterangan
 * - 6 Kotak OTP 48x48dp dengan outer halo ring 3.5dp konsentris & blinking caret
 * - Saat 6 digit selesai dimasukkan: loading spinner + teks "Memverifikasi…"
 * - Setelah loading: verifikasi berhasil -> kotak berubah warna hijau (border & soft bg) + badge centang hijau "Kode terverifikasi"
 * - Transisi otomatis ke layar buat PIN baru
 */
@Composable
fun ForgotPinOtpScreen(
    email: String,
    onBack: () -> Unit,
    onVerified: () -> Unit,
) {
    var otp by rememberSaveable { mutableStateOf("") }
    var otpState by remember { mutableStateOf<ForgotOtpState>(ForgotOtpState.Idle) }
    var timeLeft by rememberSaveable { mutableIntStateOf(30) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(timeLeft, otpState) {
        if (otpState is ForgotOtpState.Idle && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
        }
    }

    suspend fun triggerShake() {
        shakeOffset.snapTo(0f)
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(0f, tween(70, easing = LinearEasing))
    }

    fun handleDigit(d: Char) {
        if (otpState !is ForgotOtpState.Idle) return
        if (otp.length >= 6) return
        val nextOtp = otp + d
        otp = nextOtp
        isError = false
        errorMessage = null

        if (nextOtp.length == 6) {
            scope.launch {
                delay(200)
                otpState = ForgotOtpState.Verifying
                delay(1100)
                if (nextOtp == MockRepository.DEMO_EMAIL_OTP || nextOtp.all { it.isDigit() }) {
                    otpState = ForgotOtpState.Success
                    delay(800)
                    onVerified()
                } else {
                    otpState = ForgotOtpState.Idle
                    isError = true
                    errorMessage = "Kode OTP tidak valid"
                    triggerShake()
                    delay(700)
                    otp = ""
                    isError = false
                }
            }
        }
    }

    fun handleDelete() {
        if (otpState is ForgotOtpState.Idle && otp.isNotEmpty()) {
            otp = otp.dropLast(1)
            isError = false
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // Header: Tombol Kembali
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GoldinControl.TouchMin),
                contentAlignment = Alignment.CenterStart,
            ) {
                GoldinIconButton(
                    icon = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    hoverColor = Palette.Neutral100,
                    modifier = Modifier.offset(x = (-10).dp),
                )
            }

            // Title & Subtitle dengan Animasi Fade Up
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .then(rememberFadeUpModifier(delayMillis = 0, durationMillis = 400)),
            ) {
                Text(
                    text = "Verifikasi OTP",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Palette.Primary700,
                )

                Spacer(Modifier.height(8.dp))

                val subtitle = buildAnnotatedString {
                    append("Masukkan kode verifikasi yang dikirimkan melalui email ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Palette.Primary700)) {
                        append(email)
                    }
                }
                Text(
                    text = subtitle,
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSecondary,
                )
            }

            // 6 Kotak OTP 48x48dp dengan Outer Halo 3.5dp, Pop Digit & Caret
            Box(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeOffset.value.dp.toPx()
                    },
                contentAlignment = Alignment.Center,
            ) {
                CodeBoxes(
                    code = otp,
                    total = 6,
                    masked = false,
                    error = isError,
                    verified = otpState is ForgotOtpState.Success,
                    opacity = if (otpState is ForgotOtpState.Verifying) 0.7f else 1f,
                )
            }

            // Status Row di Bawah Kotak (Timer / Memverifikasi... / Kode terverifikasi / Error)
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (otpState) {
                    is ForgotOtpState.Verifying -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = rememberFadeUpModifier(0, 200),
                        ) {
                            GoldinSpinner(size = 18.dp, color = Palette.Primary700)
                            Text(
                                text = "Memverifikasi…",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.InkSecondary,
                            )
                        }
                    }
                    is ForgotOtpState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = rememberFadeUpModifier(0, 250),
                        ) {
                            Icon(
                                imageVector = PrototypeIcons.CircleCheckFilled,
                                contentDescription = null,
                                tint = Palette.Good,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Kode terverifikasi",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Good700,
                            )
                        }
                    }
                    is ForgotOtpState.Idle -> {
                        if (errorMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = rememberFadeUpModifier(0, 200),
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.AlertCircle,
                                    contentDescription = null,
                                    tint = Palette.Bad,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    color = Palette.Bad,
                                )
                            }
                        } else if (timeLeft > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = PrototypeIcons.Clock,
                                    contentDescription = null,
                                    tint = Palette.InkSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Kirim ulang kode dalam ",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.InkSecondary,
                                )
                                Text(
                                    text = String.format(Locale.US, "0:%02d", timeLeft),
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "Tidak menerima kode?",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 14.sp,
                                    color = Palette.InkSecondary,
                                )
                                TextLink(
                                    text = "Kirim ulang",
                                    onClick = {
                                        timeLeft = 30
                                        otp = ""
                                        isError = false
                                        errorMessage = null
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Keypad In-App 3x4
            NumberPad(
                onDigit = { d -> handleDigit(d) },
                onDelete = { handleDelete() },
                enabled = otpState is ForgotOtpState.Idle,
            )
        }
    }
}

/**
 * Layar Buat PIN Baru & Konfirmasi PIN Baru sesuai Gambar 4, Gambar 5 & Pedoman Prototype:
 * - Animasi fade up pada teks "Buat pin baru" / "Konfirmasi PIN baru"
 * - 6 Kotak PIN dengan outer halo ring 3.5dp ungu muda konsentris & pop dot bouncy
 * - Konfirmasi tidak sama: animasi shake pada kotak + border & dot warna merah + badge icon (!) "PIN tidak sama"
 * - Konfirmasi cocok: reset lock akun & kembali ke PIN screen dengan drawer biometrik
 */
@Composable
fun ForgotPinNewScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onPinSaved: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(1) } // 1: Buat PIN baru, 2: Konfirmasi PIN baru
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    suspend fun triggerShake() {
        shakeOffset.snapTo(0f)
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(-8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(8f, tween(70, easing = LinearEasing))
        shakeOffset.animateTo(0f, tween(70, easing = LinearEasing))
    }

    fun handleDigit(d: Char) {
        if (isChecking) return

        if (step == 1) {
            if (newPin.length >= 6) return
            val next = newPin + d
            newPin = next
            isError = false
            errorMessage = null

            if (next.length == 6) {
                isChecking = true
                scope.launch {
                    delay(200)
                    isChecking = false
                    step = 2
                }
            }
        } else {
            if (confirmPin.length >= 6) return
            val next = confirmPin + d
            confirmPin = next
            isError = false
            errorMessage = null

            if (next.length == 6) {
                isChecking = true
                scope.launch {
                    delay(200)
                    if (next == newPin) {
                        isChecking = false
                        env.repo.unlockAccountWithResetPin(newPin)
                        onPinSaved()
                    } else {
                        isChecking = false
                        isError = true
                        errorMessage = "PIN tidak sama"
                        triggerShake()
                        delay(700)
                        confirmPin = ""
                        isError = false
                        errorMessage = null
                    }
                }
            }
        }
    }

    fun handleDelete() {
        if (isChecking) return
        if (step == 1) {
            if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
        }
        isError = false
        errorMessage = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // Header: Tombol Kembali
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GoldinControl.TouchMin),
                contentAlignment = Alignment.CenterStart,
            ) {
                GoldinIconButton(
                    icon = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    onClick = {
                        if (step == 2) {
                            step = 1
                            confirmPin = ""
                            isError = false
                            errorMessage = null
                        } else {
                            onBack()
                        }
                    },
                    hoverColor = Palette.Neutral100,
                    modifier = Modifier.offset(x = (-10).dp),
                )
            }

            // Judul dengan Animasi Fade Up
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .then(rememberFadeUpModifier(delayMillis = 0, durationMillis = 400)),
            ) {
                Text(
                    text = if (step == 1) "Buat PIN baru" else "Konfirmasi PIN baru",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Palette.Primary700,
                )
            }

            // 6 Kotak PIN dengan Outer Halo 3.5dp, Pop Dot & Animasi Shake saat mismatch
            Box(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeOffset.value.dp.toPx()
                    },
                contentAlignment = Alignment.Center,
            ) {
                CodeBoxes(
                    code = if (step == 1) newPin else confirmPin,
                    total = 6,
                    masked = true,
                    error = isError,
                )
            }

            // Status Baris di Bawah Kotak PIN: Badge (!) "PIN tidak sama" saat mismatch
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(32.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (isError && errorMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = rememberFadeUpModifier(0, 200),
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.AlertCircle,
                            contentDescription = null,
                            tint = Palette.Bad,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = errorMessage ?: "",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            color = Palette.Bad,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Keypad In-App 3x4
            NumberPad(
                onDigit = { d -> handleDigit(d) },
                onDelete = { handleDelete() },
                enabled = !isChecking,
            )
        }
    }
}

@ScreenPreview
@Composable
private fun ForgotPinPasswordPreview() = GoldinTheme {
    ForgotPinPasswordScreen(rememberPreviewEnv(false), onBack = {}, onForgotPassword = {}, onVerified = {})
}

@ScreenPreview
@Composable
private fun ForgotPinOtpPreview() = GoldinTheme {
    ForgotPinOtpScreen(email = "reiuser@gmail.com", onBack = {}, onVerified = {})
}

@ScreenPreview
@Composable
private fun ForgotPinNewPreview() = GoldinTheme {
    ForgotPinNewScreen(rememberPreviewEnv(false), onBack = {}, onPinSaved = {})
}

private enum class GoogleSheetStep { CHOOSE, EMAIL, PASSWORD, CONSENT }

/**
 * Alur pemilih akun Google bergaya System UI sesuai `Main.dc.html` L3151–3231 & Handoff §3–§4:
 * - Choose an account (`Indriana` / `Use another account`) -> Email -> Password -> Consent (`Sign in to Goldin`)
 * - Mempertahankan gaya System UI Google (`#0B57D0`, `#007AFF`, `#F9F9F9`)
 * - Di versi produksi dapat diganti Credential Manager bawaan Android.
 */
@Composable
fun GoogleAccountScreen(
    onBack: () -> Unit,
    onSelected: (email: String, name: String) -> Unit,
) {
    var step by remember { mutableStateOf(GoogleSheetStep.CHOOSE) }
    var gEmail by rememberSaveable { mutableStateOf("") }
    var gName by rememberSaveable { mutableStateOf("") }
    var gPw by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    val googleBlue = Color(0xFF0B57D0)
    val googleBlueHover = Color(0xFF0A4FBC)
    val iosBlue = Color(0xFF007AFF)
    val googleInk = Color(0xFF1F1F1F)
    val googleSub = Color(0xFF444746)
    val googleBorder = Color(0xFF747775)
    val googleDivider = Color(0xFFE3E3E3)
    val avatarBg = if (gEmail == "indriana.123@gmail.com") Color(0xFF7B1FA2) else googleBlue
    val initial = (gName.ifBlank { gEmail }.ifBlank { "G" }).first().uppercaseChar().toString()

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val sheetOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 1f,
        animationSpec = tween(GoldinMotion.DurationSheet, easing = GoldinMotion.EaseOutSheet),
        label = "googleSheetUp",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .statusBarsPadding(),
    ) {
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer { translationY = size.height * sheetOffset }
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Browser / System Auth Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0xFFF9F9F9))
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = "Cancel",
                    color = iosBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                )
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(PrototypeIcons.Lock, null, tint = Color(0xFF6B6B6B), modifier = Modifier.size(13.dp))
                    Text("accounts.google.com", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Icon(
                    PrototypeIcons.Refresh,
                    contentDescription = null,
                    tint = iosBlue,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(18.dp),
                )
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x4A3C3C43)))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp),
            ) {
                Image(painterResource(R.drawable.ic_google), contentDescription = "Google", modifier = Modifier.size(24.dp))

                when (step) {
                    GoogleSheetStep.CHOOSE -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Choose an account", color = googleInk, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            buildAnnotatedString {
                                append("to continue to ")
                                withStyle(SpanStyle(color = googleBlue, fontWeight = FontWeight.Medium)) { append("Goldin") }
                            },
                            color = googleInk,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                        Spacer(Modifier.height(28.dp))

                        GoogleAccountRow(
                            onClick = {
                                gEmail = "indriana.123@gmail.com"
                                gName = "Indriana"
                                step = GoogleSheetStep.CONSENT
                            },
                            dividerColor = googleDivider,
                        ) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF7B1FA2)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("I", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Indriana", color = googleInk, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                                Text("indriana.123@gmail.com", color = googleSub, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }

                        GoogleAccountRow(
                            onClick = {
                                gEmail = ""
                                gPw = ""
                                step = GoogleSheetStep.EMAIL
                            },
                            dividerColor = googleDivider,
                        ) {
                            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                Icon(PrototypeIcons.User, null, tint = googleSub, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Use another account", color = googleInk, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(Modifier.height(28.dp))
                        Text(
                            buildAnnotatedString {
                                append("Before using this app, you can review Goldin’s ")
                                withStyle(SpanStyle(color = googleBlue, fontWeight = FontWeight.Medium)) { append("privacy policy") }
                                append(" and ")
                                withStyle(SpanStyle(color = googleBlue, fontWeight = FontWeight.Medium)) { append("terms of service") }
                                append(".")
                            },
                            color = googleSub,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                    }

                    GoogleSheetStep.EMAIL -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Sign in", color = googleInk, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            buildAnnotatedString {
                                append("to continue to ")
                                withStyle(SpanStyle(color = googleBlue, fontWeight = FontWeight.Medium)) { append("Goldin") }
                            },
                            color = googleInk,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                        Spacer(Modifier.height(28.dp))
                        GoogleSystemTextField(
                            value = gEmail,
                            onValueChange = { gEmail = it },
                            placeholder = "Email or phone",
                            keyboardType = KeyboardType.Email,
                            borderColor = googleBorder,
                            focusColor = googleBlue,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Forgot email?", color = googleBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(32.dp))
                        Text(
                            "Not your device? Use a private browsing window to sign in.",
                            color = googleSub,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        Spacer(Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Create account", color = googleBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            GooglePrimaryPillButton(
                                text = "Next",
                                enabled = gEmail.trim().isNotEmpty(),
                                bg = googleBlue,
                                hoverBg = googleBlueHover,
                                onClick = {
                                    gName = gEmail.trim().substringBefore('@').replaceFirstChar { it.uppercase() }
                                    step = GoogleSheetStep.PASSWORD
                                },
                            )
                        }
                    }

                    GoogleSheetStep.PASSWORD -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Welcome", color = googleInk, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
                        Spacer(Modifier.height(12.dp))
                        GoogleAccountChip(initial = initial, email = gEmail, avatarBg = googleBlue, borderColor = googleBorder, ink = googleInk)
                        Spacer(Modifier.height(28.dp))
                        GoogleSystemTextField(
                            value = gPw,
                            onValueChange = { gPw = it },
                            placeholder = "Enter your password",
                            keyboardType = KeyboardType.Password,
                            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                            borderColor = googleBorder,
                            focusColor = googleBlue,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showPw = !showPw },
                        ) {
                            Checkbox(
                                checked = showPw,
                                onCheckedChange = { showPw = it },
                                colors = CheckboxDefaults.colors(checkedColor = googleBlue, uncheckedColor = googleBorder),
                            )
                            Text("Show password", color = googleInk, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Forgot password?", color = googleBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            GooglePrimaryPillButton(
                                text = "Next",
                                enabled = gPw.isNotEmpty(),
                                bg = googleBlue,
                                hoverBg = googleBlueHover,
                                onClick = { step = GoogleSheetStep.CONSENT },
                            )
                        }
                    }

                    GoogleSheetStep.CONSENT -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Sign in to Goldin", color = googleInk, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
                        Spacer(Modifier.height(16.dp))
                        GoogleAccountChip(initial = initial, email = gEmail, avatarBg = avatarBg, borderColor = googleBorder, ink = googleInk)
                        Spacer(Modifier.height(28.dp))
                        Text(
                            buildAnnotatedString {
                                append("By continuing, Google will share your name, email address, language preference, and profile picture with Goldin. See Goldin’s ")
                                withStyle(SpanStyle(color = googleBlue, fontWeight = FontWeight.Medium)) { append("Privacy Policy") }
                                append(" and ")
                                withStyle(SpanStyle(color = googleBlue, fontWeight = FontWeight.Medium)) { append("Terms of Service") }
                                append(".")
                            },
                            color = googleInk,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            buildAnnotatedString {
                                append("You can manage Sign in with Google in your ")
                                withStyle(SpanStyle(color = googleBlue, fontWeight = FontWeight.Medium)) { append("Google Account") }
                                append(".")
                            },
                            color = googleInk,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                Spacer(Modifier.weight(1f).height(32.dp))
                if (step == GoogleSheetStep.CONSENT) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, googleBorder, RoundedCornerShape(999.dp))
                                .clickable(onClick = onBack)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Cancel", color = googleBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        GooglePrimaryPillButton(
                            text = "Continue",
                            enabled = true,
                            bg = googleBlue,
                            hoverBg = googleBlueHover,
                            onClick = { onSelected(gEmail.ifBlank { "indriana.123@gmail.com" }, gName.ifBlank { "Indriana" }) },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("English (United States)", color = googleSub, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Help", color = googleSub, fontSize = 12.sp)
                            Text("Privacy", color = googleSub, fontSize = 12.sp)
                            Text("Terms", color = googleSub, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleAccountRow(
    onClick: () -> Unit,
    dividerColor: Color,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg by animateColorAsState(
        if (isPressed || isHovered) Color(0xFFF2F2F2) else Color.Transparent,
        tween(120),
        label = "gRowBg",
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
    }
}

@Composable
private fun GoogleAccountChip(initial: String, email: String, avatarBg: Color, borderColor: Color, ink: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(avatarBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text(email, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Icon(PrototypeIcons.ChevronDown, null, tint = ink, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun GoogleSystemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    borderColor: Color,
    focusColor: Color,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) focusColor else borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = Color(0xFF444746), fontSize = 16.sp)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF1F1F1F), fontSize = 16.sp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun GooglePrimaryPillButton(
    text: String,
    enabled: Boolean,
    bg: Color,
    hoverBg: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = if (isHovered || isPressed) hoverBg else bg,
        animationSpec = tween(150),
        label = "gBtnBg",
    )
    Box(
        modifier = Modifier
            .height(40.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(999.dp))
            .background(fill)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 02a Login Google · Memuat (`gLoading`) sesuai Handoff §4 & `Main.dc.html` L3087–3092:
 * Halaman penuh (background-default) berisi logo Goldin 56dp + wordmark Lato Black 24sp + spinner 24dp (~900ms), TANPA teks.
 */
@Composable
fun GoogleReturnLoadingScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(900)
        onFinished()
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "gLoadFade",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GoldBarsBadge(size = 56.dp, cornerRadius = GoldinRadius.Xl, iconSize = 32.dp)
        Spacer(Modifier.height(GoldinSpace.Sm))
        Text(
            text = "Goldin",
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.3).sp,
            color = Palette.Primary700,
        )
        Spacer(Modifier.height(GoldinSpace.Xxl))
        GoldinSpinner(size = 24.dp, color = Palette.Primary700)
    }
}

@ScreenPreview
@Composable
private fun GoogleAccountPreview() = GoldinTheme { GoogleAccountScreen(onBack = {}, onSelected = { _, _ -> }) }

@ScreenPreview
@Composable
private fun GoogleReturnLoadingPreview() = GoldinTheme { GoogleReturnLoadingScreen {} }

/**
 * 02c Lupa kata sandi · Email (`fpEmail`) sesuai prototipe Goldin v1.2 (`Main.dc.html` L734–749 & L4820–4826):
 * - Back button 44dp, judul Lato Black 24sp "Lupa kata sandi" tanpa subtitle
 * - Email* (44dp, radius 12dp, halo fokus 3dp, ikon Mail 20dp)
 * - Validasi format + cek email terdaftar ("Email belum terdaftar") + animasi shake
 * - Tombol bawah "Kirim kode" (loading spinner di dalam tombol)
 */
@Composable
fun ForgotPasswordEmailScreen(
    env: AppEnv,
    initialEmail: String = "",
    onBack: () -> Unit,
    onCodeSent: (email: String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun handleSubmit() {
        if (loading) return
        val em = email.trim()
        val err = when {
            em.isEmpty() -> "Email wajib diisi"
            !Rules.isValidEmail(em) -> "Format email belum benar"
            !env.repo.isRegisteredEmail(em) -> "Email belum terdaftar"
            else -> null
        }
        emailErr = err
        if (err != null) {
            shakeTrigger++
            return
        }
        loading = true
        scope.launch {
            delay(900)
            loading = false
            onCodeSent(em)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = GoldinSpace.Lg),
    ) {
        Spacer(Modifier.height(GoldinSpace.Xs))
        GoldinIconButton(
            icon = PrototypeIcons.ChevronLeft,
            contentDescription = "Kembali",
            onClick = onBack,
            hoverColor = Palette.Neutral100,
            modifier = Modifier.offset(x = (-10).dp),
        )
        Spacer(Modifier.height(GoldinSpace.Xl))
        GoldinFadeUp {
            Text(
                text = "Lupa kata sandi",
                style = GoldinTypography.title2xl,
                color = Palette.Primary700,
            )
        }
        Spacer(Modifier.height(GoldinSpace.Xxl))
        GoldinShakeBox(trigger = shakeTrigger) {
            Column {
                GoldinFieldLabel(label = "Email", mandatory = true, isError = emailErr != null)
                Spacer(Modifier.height(GoldinSpace.Xs))
                GoldinInputField(
                    value = email,
                    onValueChange = { email = it; emailErr = null },
                    placeholder = "Masukkan email",
                    leadingIcon = PrototypeIcons.Mail,
                    keyboardType = KeyboardType.Email,
                    isError = emailErr != null,
                )
                if (emailErr != null) {
                    FieldErrorMessage(emailErr!!)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(bottom = GoldinSpace.Xl)) {
            PrimaryButton(
                text = "Kirim kode",
                onClick = ::handleSubmit,
                loading = loading,
                loadingText = "Mengirim…",
            )
        }
    }
}

/**
 * 02d Lupa kata sandi · Kata sandi baru (`fpNew`) sesuai prototipe Goldin v1.2 (`Main.dc.html` L751–780 & L4827–4828):
 * - Judul Lato Black 24sp "Buat kata sandi baru"
 * - Kata sandi baru* (Lock + eye toggle) + 2 aturan live beranimasi pop ("Minimal 8 karakter" & "Mengandung huruf kapital")
 * - Konfirmasi kata sandi* (LockCheck + eye toggle, error "Kata sandi tidak sama")
 * - Tombol "Simpan" (opacity 0.4 sampai valid, loading 900ms) -> resetPassword -> kembali & toast "Kata sandi berhasil diubah"
 */
@Composable
fun ForgotPasswordNewScreen(
    env: AppEnv,
    email: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var showPw2 by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val r8 = Rules.hasMinLength(password)
    val rCap = Rules.hasCapitalLetter(password)
    val pwMismatch = confirm.isNotEmpty() && confirm != password
    val pwMatch = confirm.isNotEmpty() && confirm == password
    val canSave = r8 && rCap && pwMatch

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GoldinSpace.Lg),
        ) {
            Spacer(Modifier.height(GoldinSpace.Xs))
            GoldinIconButton(
                icon = PrototypeIcons.ChevronLeft,
                contentDescription = "Kembali",
                onClick = onBack,
                hoverColor = Palette.Neutral100,
                modifier = Modifier.offset(x = (-10).dp),
            )
            Spacer(Modifier.height(GoldinSpace.Xl))
            GoldinFadeUp {
                Text(
                    text = "Buat kata sandi baru",
                    style = GoldinTypography.title2xl,
                    color = Palette.Primary700,
                )
            }
            Spacer(Modifier.height(GoldinSpace.Xxl))
            Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg)) {
                Column {
                    GoldinFieldLabel(label = "Kata sandi baru", mandatory = true)
                    Spacer(Modifier.height(GoldinSpace.Xs))
                    GoldinInputField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Buat kata sandi",
                        leadingIcon = PrototypeIcons.Lock,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            GoldinIconButton(
                                icon = if (showPw) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                                contentDescription = "Tampilkan kata sandi",
                                onClick = { showPw = !showPw },
                                tint = Palette.InkSecondary,
                                hoverColor = Palette.Neutral100,
                            )
                        },
                    )
                    Spacer(Modifier.height(GoldinSpace.Xs))
                    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs)) {
                        GoldinPasswordRuleRow("Minimal 8 karakter", r8)
                        GoldinPasswordRuleRow("Mengandung huruf kapital", rCap)
                    }
                }
                Column {
                    GoldinFieldLabel(label = "Konfirmasi kata sandi", mandatory = true, isError = pwMismatch)
                    Spacer(Modifier.height(GoldinSpace.Xs))
                    GoldinInputField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        placeholder = "Masukkan kata sandi",
                        leadingIcon = PrototypeIcons.LockCheck,
                        isError = pwMismatch,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (showPw2) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            GoldinIconButton(
                                icon = if (showPw2) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                                contentDescription = "Tampilkan konfirmasi kata sandi",
                                onClick = { showPw2 = !showPw2 },
                                tint = Palette.InkSecondary,
                                hoverColor = Palette.Neutral100,
                            )
                        },
                    )
                    if (pwMismatch) {
                        FieldErrorMessage("Kata sandi tidak sama")
                    }
                }
            }
            Spacer(Modifier.height(GoldinSpace.Xl))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.Paper)
                .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Md, bottom = GoldinSpace.Xl),
        ) {
            PrimaryButton(
                text = "Simpan",
                enabled = canSave,
                loading = loading,
                loadingText = "Menyimpan…",
                onClick = {
                    if (!canSave || loading) return@PrimaryButton
                    loading = true
                    scope.launch {
                        delay(900)
                        env.attempt { env.repo.resetPassword(email, password) }
                        loading = false
                        onSaved()
                    }
                },
            )
        }
    }
}

@ScreenPreview
@Composable
private fun ForgotPasswordEmailPreview() = GoldinTheme {
    ForgotPasswordEmailScreen(rememberPreviewEnv(false), onBack = {}, onCodeSent = {})
}

@ScreenPreview
@Composable
private fun ForgotPasswordNewPreview() = GoldinTheme {
    ForgotPasswordNewScreen(rememberPreviewEnv(false), email = "reiuser@gmail.com", onBack = {}, onSaved = {})
}

/**
 * 03 Registrasi (`reg1`) sesuai prototipe Goldin v1.2 (`Main.dc.html` L783–831):
 * - Back button di atas Stepper 3 langkah (Buat akun · Verifikasi · Data diri)
 * - Nama lengkap* + Email* (tinggi 44dp, radius 12dp, focus halo 3dp, shake saat error)
 * - Tombol Daftar (loading spinner di dalam tombol), tautan Syarat dan Ketentuan -> TermsSheet
 * - Divider "atau" 1.5dp neutral-300, Daftar dengan Google, footer Masuk
 */
@Composable
fun RegisterEmailScreen(
    signup: SignupDraft,
    onBack: () -> Unit,
    onSent: () -> Unit,
    onGoogle: () -> Unit,
    onLogin: () -> Unit = onBack,
) {
    var name by rememberSaveable { mutableStateOf(signup.name) }
    var email by rememberSaveable { mutableStateOf(signup.email) }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var showTerms by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun handleSubmit() {
        if (loading) return
        val nErr = if (name.trim().length < 2) "Nama lengkap wajib diisi" else null
        val eTrim = email.trim()
        val eErr = when {
            eTrim.isEmpty() -> "Email wajib diisi"
            !Rules.isValidEmail(eTrim) -> "Format email belum benar"
            else -> null
        }
        nameErr = nErr
        emailErr = eErr
        if (nErr != null || eErr != null) {
            shakeTrigger++
            return
        }
        loading = true
        scope.launch {
            delay(900)
            signup.name = name.trim()
            signup.email = eTrim
            signup.viaGoogle = false
            loading = false
            onSent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GoldinSpace.Lg),
    ) {
        Spacer(Modifier.height(GoldinSpace.Xs))
        GoldinIconButton(
            icon = PrototypeIcons.ChevronLeft,
            contentDescription = "Kembali",
            onClick = onBack,
            hoverColor = Palette.Neutral100,
            modifier = Modifier.offset(x = (-10).dp),
        )
        Spacer(Modifier.height(GoldinSpace.Xs))
        LabeledStepBar(current = 1, labels = listOf("Buat akun", "Verifikasi", "Data diri"))

        Spacer(Modifier.height(GoldinSpace.Xl))
        GoldinFadeUp {
            Column {
                Text(
                    text = "Halo, selamat datang!",
                    style = GoldinTypography.title2xl,
                    color = Palette.Primary700,
                )
                Spacer(Modifier.height(GoldinSpace.Xs))
                Text(
                    text = "Buat akun dan mulai nabung emas dalam 2 menit.",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSecondary,
                )
            }
        }

        Spacer(Modifier.height(GoldinSpace.Xxl))
        GoldinShakeBox(trigger = shakeTrigger) {
            Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg)) {
                GoldinFadeUp(delayMs = 60) {
                    Column {
                        GoldinFieldLabel(label = "Nama lengkap", mandatory = true, isError = nameErr != null)
                        Spacer(Modifier.height(GoldinSpace.Xs))
                        GoldinInputField(
                            value = name,
                            onValueChange = { name = it; nameErr = null },
                            placeholder = "Masukkan nama lengkap",
                            leadingIcon = PrototypeIcons.User,
                            isError = nameErr != null,
                        )
                        if (nameErr != null) FieldErrorMessage(nameErr!!)
                    }
                }
                GoldinFadeUp(delayMs = 120) {
                    Column {
                        GoldinFieldLabel(label = "Email", mandatory = true, isError = emailErr != null)
                        Spacer(Modifier.height(GoldinSpace.Xs))
                        GoldinInputField(
                            value = email,
                            onValueChange = { email = it; emailErr = null },
                            placeholder = "Masukkan email",
                            leadingIcon = PrototypeIcons.Mail,
                            keyboardType = KeyboardType.Email,
                            isError = emailErr != null,
                        )
                        if (emailErr != null) FieldErrorMessage(emailErr!!)
                    }
                }
            }
        }

        Spacer(Modifier.height(GoldinSpace.Xl))
        GoldinFadeUp(delayMs = 180) {
            PrimaryButton(
                text = "Daftar",
                onClick = ::handleSubmit,
                loading = loading,
                loadingText = "Memproses…",
            )
        }

        Spacer(Modifier.height(GoldinSpace.Sm))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Dengan membuat akun, Anda setuju dengan",
                style = GoldinTypography.bodyMd,
                color = Palette.InkSecondary,
                textAlign = TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val tncInteraction = remember { MutableInteractionSource() }
                val tncHovered by tncInteraction.collectIsHoveredAsState()
                val tncColor by animateColorAsState(
                    if (tncHovered) Palette.Primary600 else Palette.Primary700,
                    tween(GoldinMotion.DurationColor),
                    label = "tncColor",
                )
                Text(
                    text = "Syarat dan Ketentuan",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = tncColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(GoldinRadius.Sm))
                        .clickable(interactionSource = tncInteraction, indication = null) { showTerms = true }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                )
                Text(
                    text = " yang berlaku",
                    style = GoldinTypography.bodyMd,
                    color = Palette.InkSecondary,
                )
            }
        }

        Spacer(Modifier.height(GoldinSpace.Lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
        ) {
            Box(Modifier.weight(1f).height(1.5.dp).background(Palette.Neutral300))
            Text("atau", style = GoldinTypography.bodyMd, color = Palette.InkSecondary)
            Box(Modifier.weight(1f).height(1.5.dp).background(Palette.Neutral300))
        }

        Spacer(Modifier.height(GoldinSpace.Lg))
        GoogleButton(text = "Daftar dengan Google", onClick = onGoogle)

        Spacer(Modifier.weight(1f).height(GoldinSpace.Xl))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = GoldinSpace.Xl),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sudah memiliki akun?", style = GoldinTypography.bodyMd, color = Palette.InkSecondary)
            TextLink("Masuk", onLogin)
        }
    }

    if (showTerms) {
        TermsSheet(onDismiss = { showTerms = false })
    }
}

/**
 * Bottom sheet Syarat dan Ketentuan sesuai Handoff §4 & `Main.dc.html` L3095–3127:
 * Hanya judul + tombol X (tanpa tombol footer; ketuk luar juga menutup).
 */
@Composable
fun TermsSheet(onDismiss: () -> Unit) {
    val sections = remember {
        listOf(
            "Akun" to "Gunakan data yang benar. Jaga kerahasiaan kata sandi dan PIN-mu.",
            "Transaksi emas" to "Harga beli dan jual mengikuti harga saat transaksi. Transaksi yang sudah berhasil tidak bisa dibatalkan.",
            "Penyimpanan emas" to "Emasmu tersimpan di brankas mitra dan bisa dijual atau dicetak kapan saja.",
            "Biaya" to "Biaya transaksi ditampilkan sebelum kamu mengonfirmasi pembayaran.",
            "Perubahan ketentuan" to "Goldin dapat mengubah ketentuan ini dan akan memberi tahu lewat aplikasi.",
        )
    }
    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = "Syarat dan Ketentuan",
        showCloseButton = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
        ) {
            Text(
                text = "Dengan membuat akun Goldin, kamu menyetujui ketentuan berikut.",
                style = GoldinTypography.bodyMd,
                color = Palette.Ink,
            )
            sections.forEach { (heading, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs)) {
                    Text(heading, style = GoldinTypography.labelMd, color = Palette.Ink)
                    Text(body, style = GoldinTypography.bodyMd, color = Palette.InkSecondary)
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun RegisterEmailPreview() = GoldinTheme { RegisterEmailScreen(previewSignup(), {}, {}, {}) }

@ScreenPreview
@Composable
private fun TermsSheetPreview() = GoldinTheme { TermsSheet {} }

/**
 * 04 Verifikasi OTP (`otp`) sesuai prototipe Goldin v1.2 (`Main.dc.html` L834–878):
 * - Dipakai bersama untuk Registrasi (dengan stepper 3 langkah), e-KYC, dan Lupa Kata Sandi (`step = null`)
 * - 6 kotak 48x48dp, blinking caret, PopDigit, verifying spinner, success CircleCheckFilled
 * - Countdown kirim ulang dalam font **Geist** (aturan Handoff §3)
 */
@Composable
fun OtpScreen(
    title: String,
    step: Pair<Int, Int>? = null,
    sentVia: String = "Masukkan kode verifikasi yang dikirimkan melalui email",
    target: String,
    demoCode: String,
    stepLabels: List<String>? = null,
    onBack: () -> Unit,
    onVerified: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var otpState by remember { mutableStateOf("idle") } // idle, verifying, success, error
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(30) }
    var resendRound by remember { mutableIntStateOf(0) }

    LaunchedEffect(resendRound) {
        seconds = 30
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
    }
    LaunchedEffect(otpState) {
        when (otpState) {
            "verifying" -> {
                delay(900)
                // Sesuai Handoff §2 & Main.dc.html: kode 6 digit apa pun lulus (kecuali "000000" untuk simulasi error)
                if (code != "000000") {
                    otpState = "success"
                } else {
                    shakeTrigger++
                    otpState = "error"
                }
            }
            "success" -> {
                delay(600)
                onVerified()
            }
            "error" -> {
                delay(650)
                code = ""
                otpState = "idle"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = GoldinSpace.Lg),
    ) {
        Spacer(Modifier.height(GoldinSpace.Xs))
        GoldinIconButton(
            icon = PrototypeIcons.ChevronLeft,
            contentDescription = "Kembali",
            onClick = onBack,
            hoverColor = Palette.Neutral100,
            modifier = Modifier.offset(x = (-10).dp),
        )
        if (step != null) {
            Spacer(Modifier.height(GoldinSpace.Xs))
            if (stepLabels != null) {
                LabeledStepBar(current = step.first, labels = stepLabels)
            } else {
                SegmentedStepBar(current = step.first, total = step.second)
            }
        }

        Spacer(Modifier.height(GoldinSpace.Xl))
        GoldinFadeUp {
            Column {
                Text(
                    text = if (title == "Verifikasi email") "Verifikasi OTP" else title,
                    style = GoldinTypography.title2xl,
                    color = Palette.Primary700,
                )
                Spacer(Modifier.height(GoldinSpace.Xs))
                Text(
                    text = buildAnnotatedString {
                        append("$sentVia ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Palette.Primary700)) {
                            append(target.ifBlank { "email@contoh.com" })
                        }
                    },
                    style = GoldinTypography.bodyMd,
                    color = Palette.InkSecondary,
                )
            }
        }

        Spacer(Modifier.height(GoldinSpace.Xxl))
        GoldinShakeBox(trigger = shakeTrigger) {
            CodeBoxes(
                code = code,
                error = otpState == "error",
                verified = otpState == "success",
                opacity = if (otpState == "verifying") 0.6f else 1f,
            )
        }

        Spacer(Modifier.height(GoldinSpace.Md))
        Box(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (otpState) {
                "verifying" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        GoldinSpinner(size = 18.dp, color = Palette.Primary700)
                        Text("Memverifikasi…", style = GoldinTypography.bodyMd, color = Palette.InkSecondary)
                    }
                }
                "success" -> {
                    GoldinFadeUp(durationMs = 250) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(PrototypeIcons.CircleCheckFilled, null, tint = Palette.Good, modifier = Modifier.size(18.dp))
                            Text(
                                "Kode terverifikasi",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Good700,
                            )
                        }
                    }
                }
                "error" -> {
                    ErrorLine("Kode verifikasi salah. Coba lagi.")
                }
                else -> {
                    if (seconds > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(PrototypeIcons.Clock, null, tint = Palette.InkSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = buildAnnotatedString {
                                    append("Kirim ulang kode dalam ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Palette.Ink)) {
                                        append("0:${seconds.toString().padStart(2, '0')}")
                                    }
                                },
                                style = GoldinTypography.bodyMd,
                                color = Palette.InkSecondary,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tidak menerima kode?", style = GoldinTypography.bodyMd, color = Palette.InkSecondary)
                            TextLink("Kirim ulang", { code = ""; resendRound++ })
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(bottom = GoldinSpace.Md)) {
            NumberPad(
                onDigit = { digit ->
                    if (code.length < 6 && otpState == "idle") {
                        code += digit
                        if (code.length == 6) otpState = "verifying"
                    }
                },
                onDelete = {
                    if (otpState == "idle") code = code.dropLast(1)
                },
                enabled = otpState == "idle",
            )
        }
    }
}

@ScreenPreview
@Composable
private fun EmailOtpPreview() = GoldinTheme {
    OtpScreen(
        title = "Verifikasi OTP",
        step = 2 to 3,
        sentVia = "Masukkan kode verifikasi yang dikirimkan melalui email",
        target = "reiuser@gmail.com",
        demoCode = DEMO_EMAIL_OTP,
        stepLabels = listOf("Buat akun", "Verifikasi", "Data diri"),
        onBack = {},
        onVerified = {},
    )
}

/** Baris indikator aturan kata sandi dengan animasi pop pada ikon `CircleCheckFilled` sesuai `Main.dc.html`. */
@Composable
fun GoldinPasswordRuleRow(label: String, met: Boolean) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(met) {
        if (met) {
            scale.snapTo(0.4f)
            scale.animateTo(1.15f, tween(160, easing = GoldinMotion.EaseOutSheet))
            scale.animateTo(1f, tween(90))
        }
    }
    val textColor by animateColorAsState(
        targetValue = if (met) Palette.Good700 else Palette.InkSecondary,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "pwRuleColor",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (met) PrototypeIcons.CircleCheckFilled else PrototypeIcons.CircleOutline,
            contentDescription = null,
            tint = if (met) Palette.Good else Palette.InkSecondary,
            modifier = Modifier
                .size(GoldinControl.IconSm)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        )
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = textColor,
        )
    }
}

private enum class UsernameCheckState { EMPTY, SHORT, CHECKING, OK, TAKEN }

/**
 * 05 Daftar Akun (`reg2`) sesuai prototipe Goldin v1.2 (`Main.dc.html` L881–958):
 * - Stepper 3 langkah (manual) atau 2 langkah (Google) + kartu "Terhubung dengan Google"
 * - Username* dengan live availability check 700ms (admin, emdig, indriana, emas -> terpakai)
 * - Tanggal lahir* dengan Bottom Sheet Date Picker Wheel 3 kolom + validasi usia >= 18 tahun
 * - Kata sandi* (Minimal 8 karakter + Mengandung huruf kapital) & Konfirmasi kata sandi* (`LockCheck`)
 * - Footer PairedButtons: Sebelumnya (outline) & Selesai (filled, opacity 0.4 sampai valid, loading 1000ms)
 */
@Composable
fun RegisterProfileScreen(env: AppEnv, signup: SignupDraft, onBack: () -> Unit, onDone: () -> Unit) {
    val viaGoogle = signup.viaGoogle
    var username by rememberSaveable { mutableStateOf(signup.username) }
    var uStatus by remember { mutableStateOf(if (signup.username.length >= 4 && !Rules.isUsernameTaken(signup.username)) UsernameCheckState.OK else UsernameCheckState.EMPTY) }

    var dob by rememberSaveable { mutableStateOf(signup.dob) }
    var dobUnder by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf(signup.password) }
    var confirm by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var showPw2 by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(username) {
        val clean = Rules.normalizeUsername(username)
        when {
            clean.isEmpty() -> uStatus = UsernameCheckState.EMPTY
            clean.length < 4 -> uStatus = UsernameCheckState.SHORT
            else -> {
                uStatus = UsernameCheckState.CHECKING
                delay(700)
                uStatus = if (Rules.isUsernameTaken(clean)) UsernameCheckState.TAKEN else UsernameCheckState.OK
            }
        }
    }

    val r8 = Rules.hasMinLength(password)
    val rCap = Rules.hasCapitalLetter(password)
    val pwMismatch = confirm.isNotEmpty() && confirm != password
    val pwMatch = confirm.isNotEmpty() && confirm == password
    val formValid = uStatus == UsernameCheckState.OK &&
        dob.isNotEmpty() && !dobUnder &&
        (viaGoogle || (r8 && rCap && pwMatch))

    val steps = if (viaGoogle) listOf("Buat akun", "Data diri") else listOf("Buat akun", "Verifikasi", "Data diri")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GoldinSpace.Lg),
        ) {
            Spacer(Modifier.height(GoldinSpace.Xs))
            GoldinIconButton(
                icon = PrototypeIcons.ChevronLeft,
                contentDescription = "Kembali",
                onClick = onBack,
                hoverColor = Palette.Neutral100,
                modifier = Modifier.offset(x = (-10).dp),
            )
            Spacer(Modifier.height(GoldinSpace.Xs))
            LabeledStepBar(current = steps.size, labels = steps)

            if (viaGoogle) {
                Spacer(Modifier.height(GoldinSpace.Lg))
                GoldinFadeUp(delayMs = 40) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Xl))
                            .background(Color.White)
                            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                            .padding(horizontal = GoldinSpace.Md, vertical = GoldinSpace.Sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                    ) {
                        Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Terhubung dengan Google", style = GoldinTypography.bodySm, color = Palette.InkSecondary)
                            Text(signup.email.ifBlank { "indriana.123@gmail.com" }, style = GoldinTypography.labelMd, color = Palette.Ink, maxLines = 1)
                        }
                        Icon(PrototypeIcons.CircleCheckFilled, null, tint = Palette.Good, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(GoldinSpace.Xl))
            Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg)) {
                // 1. Username*
                GoldinFadeUp(delayMs = 60) {
                    Column {
                        GoldinFieldLabel(label = "Username", mandatory = true, isError = uStatus == UsernameCheckState.TAKEN)
                        Spacer(Modifier.height(GoldinSpace.Xs))
                        GoldinInputField(
                            value = username,
                            onValueChange = { username = Rules.normalizeUsername(it) },
                            placeholder = "Buat username",
                            leadingIcon = PrototypeIcons.AtSign,
                            isError = uStatus == UsernameCheckState.TAKEN,
                            isSuccess = uStatus == UsernameCheckState.OK,
                            trailingIcon = when (uStatus) {
                                UsernameCheckState.CHECKING -> {
                                    { GoldinSpinner(size = 20.dp, color = Palette.InkSecondary) }
                                }
                                UsernameCheckState.OK -> {
                                    { PopStatusIcon(PrototypeIcons.CircleCheckFilled, Palette.Good700) }
                                }
                                UsernameCheckState.TAKEN -> {
                                    { PopStatusIcon(PrototypeIcons.CircleXFilled, Palette.Critical) }
                                }
                                else -> null
                            },
                        )
                        when (uStatus) {
                            UsernameCheckState.CHECKING -> Text(
                                "Mengecek ketersediaan…",
                                style = GoldinTypography.bodySm,
                                color = Palette.InkSecondary,
                                modifier = Modifier.padding(top = GoldinSpace.Xxs),
                            )
                            UsernameCheckState.OK -> Text(
                                "Username tersedia",
                                style = GoldinTypography.bodySm,
                                color = Palette.Good700,
                                modifier = Modifier.padding(top = GoldinSpace.Xxs),
                            )
                            UsernameCheckState.TAKEN -> Text(
                                "Username sudah dipakai, coba yang lain",
                                style = GoldinTypography.bodySm,
                                color = Palette.Critical,
                                modifier = Modifier.padding(top = GoldinSpace.Xxs),
                            )
                            else -> {}
                        }
                    }
                }

                // 2. Tanggal lahir* (Wheel Date Picker Sheet)
                GoldinFadeUp(delayMs = 100) {
                    val dobInteraction = remember { MutableInteractionSource() }
                    val dobHovered by dobInteraction.collectIsHoveredAsState()
                    val dobRingColor by animateColorAsState(
                        if (showDatePicker) Palette.Primary200 else Color.Transparent,
                        tween(GoldinMotion.DurationColor),
                        label = "dobRing",
                    )
                    val dobBorderColor by animateColorAsState(
                        when {
                            showDatePicker -> Palette.Primary700
                            dobUnder -> Palette.Critical
                            dob.isNotEmpty() -> Palette.Good
                            dobHovered -> Palette.Primary600
                            else -> Palette.Neutral300
                        },
                        tween(GoldinMotion.DurationColor),
                        label = "dobBorder",
                    )
                    val innerShape = RoundedCornerShape(GoldinRadius.Lg)

                    Column {
                        GoldinFieldLabel(label = "Tanggal lahir", mandatory = true, isError = dobUnder)
                        Spacer(Modifier.height(GoldinSpace.Xs))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(GoldinControl.SizeLg)
                                .goldinFocusHalo(dobRingColor)
                                .clip(innerShape)
                                .background(Color.White)
                                .border(GoldinBorder.Default, dobBorderColor, innerShape)
                                .clickable(interactionSource = dobInteraction, indication = null) { showDatePicker = true }
                                .padding(horizontal = GoldinSpace.Md),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    PrototypeIcons.Calendar,
                                    contentDescription = null,
                                    tint = if (showDatePicker) Palette.Primary700 else Palette.InkSoft,
                                    modifier = Modifier.size(GoldinControl.IconMd),
                                )
                                Spacer(Modifier.width(GoldinSpace.Sm))
                                Text(
                                    text = dob.ifEmpty { "DD/MM/YYYY" },
                                    style = GoldinTypography.bodyMd,
                                    color = if (dob.isNotEmpty()) Palette.Ink else Palette.InkSoft,
                                )
                            }
                        }
                        if (dobUnder) {
                            FieldErrorMessage("Kamu harus berusia minimal 18 tahun untuk membuat akun")
                        }
                    }
                }

                // 3 & 4. Kata sandi* + Konfirmasi kata sandi* (manual saja)
                if (!viaGoogle) {
                    GoldinFadeUp(delayMs = 140) {
                        Column {
                            GoldinFieldLabel(label = "Kata sandi", mandatory = true)
                            Spacer(Modifier.height(GoldinSpace.Xs))
                            GoldinInputField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = "Buat kata sandi",
                                leadingIcon = PrototypeIcons.Lock,
                                keyboardType = KeyboardType.Password,
                                visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    GoldinIconButton(
                                        icon = if (showPw) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                                        contentDescription = "Tampilkan kata sandi",
                                        onClick = { showPw = !showPw },
                                        tint = Palette.InkSecondary,
                                        hoverColor = Palette.Neutral100,
                                    )
                                },
                            )
                            Spacer(Modifier.height(GoldinSpace.Xs))
                            Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs)) {
                                GoldinPasswordRuleRow("Minimal 8 karakter", r8)
                                GoldinPasswordRuleRow("Mengandung huruf kapital", rCap)
                            }
                        }
                    }

                    GoldinFadeUp(delayMs = 180) {
                        Column {
                            GoldinFieldLabel(label = "Konfirmasi kata sandi", mandatory = true, isError = pwMismatch)
                            Spacer(Modifier.height(GoldinSpace.Xs))
                            GoldinInputField(
                                value = confirm,
                                onValueChange = { confirm = it },
                                placeholder = "Masukkan kata sandi",
                                leadingIcon = PrototypeIcons.LockCheck,
                                isError = pwMismatch,
                                keyboardType = KeyboardType.Password,
                                visualTransformation = if (showPw2) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    GoldinIconButton(
                                        icon = if (showPw2) PrototypeIcons.EyeOff else PrototypeIcons.Eye,
                                        contentDescription = "Tampilkan konfirmasi kata sandi",
                                        onClick = { showPw2 = !showPw2 },
                                        tint = Palette.InkSecondary,
                                        hoverColor = Palette.Neutral100,
                                    )
                                },
                            )
                            if (pwMismatch) {
                                FieldErrorMessage("Kata sandi tidak sama")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(GoldinSpace.Xl))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.Paper)
                .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
        ) {
            PairedButtons(
                leftText = "Sebelumnya",
                onLeftClick = onBack,
                rightText = "Selesai",
                onRightClick = {
                    if (!formValid || loading) return@PairedButtons
                    loading = true
                    scope.launch {
                        delay(1000)
                        signup.username = username
                        signup.dob = dob
                        signup.password = password.ifBlank { MockRepository.GOLDIN_DEMO_PASSWORD }
                        env.repo.register(
                            name = signup.name.ifBlank { username },
                            email = signup.email.ifBlank { "$username@gmail.com" },
                            password = signup.password,
                            username = username,
                            birthDate = dob,
                        )
                        loading = false
                        onDone()
                    }
                },
                rightEnabled = formValid,
                rightLoading = loading,
                rightLoadingText = "Menyimpan…",
            )
        }
    }

    if (showDatePicker) {
        GoldinDatePickerSheet(
            initialDob = dob,
            onDismiss = { showDatePicker = false },
            onSave = { formattedDob, isUnderAge ->
                dob = formattedDob
                dobUnder = isUnderAge
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun PopStatusIcon(icon: ImageVector, tint: Color) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(icon) {
        scale.snapTo(0.4f)
        scale.animateTo(1.15f, tween(160, easing = GoldinMotion.EaseOutSheet))
        scale.animateTo(1f, tween(90))
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(GoldinControl.IconMd)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
}

private val INDONESIAN_MONTHS = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

/**
 * Bottom Sheet Date Picker Wheel 3 kolom sesuai `Main.dc.html` L2806–2835:
 * - Tinggi 200dp (5 baris x 40dp), band sorotan tengah `primary-50` + border `primary-100` (`radius-lg 12dp`)
 * - Kolom Tanggal (64dp, Geist Mono 18sp) · Bulan (flex, Geist 18sp) · Tahun (80dp, Geist Mono 18sp)
 * - Scroll-snap + tap-to-pick + fade gradien atas & bawah + validasi usia >= 18 tahun
 */
@Composable
fun GoldinDatePickerSheet(
    initialDob: String,
    onDismiss: () -> Unit,
    onSave: (formattedDob: String, isUnderAge: Boolean) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val yearStart = 1930
    val yearEnd = today.year
    val years = remember(yearEnd) { (yearStart..yearEnd).map { it.toString() } }

    val parsed = remember(initialDob) {
        val parts = initialDob.split("/")
        val d = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 31) ?: 11
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 2
        val y = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(yearStart, yearEnd) ?: 2003
        Triple(d - 1, m - 1, y - yearStart)
    }

    var selDayIdx by remember { mutableIntStateOf(parsed.first) }
    var selMonthIdx by remember { mutableIntStateOf(parsed.second) }
    var selYearIdx by remember { mutableIntStateOf(parsed.third) }

    val selectedYear = yearStart + selYearIdx
    val maxDays = remember(selectedYear, selMonthIdx) {
        val month = selMonthIdx + 1
        when (month) {
            2 -> if ((selectedYear % 4 == 0 && selectedYear % 100 != 0) || selectedYear % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
    val days = remember(maxDays) { (1..maxDays).map { it.toString().padStart(2, '0') } }
    if (selDayIdx >= maxDays) selDayIdx = maxDays - 1

    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = "Pilih tanggal lahir",
        showCloseButton = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            // Band sorotan baris tengah (top = 80dp, height = 40dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .height(40.dp)
                    .clip(RoundedCornerShape(GoldinRadius.Lg))
                    .background(Palette.Primary50)
                    .border(GoldinBorder.Default, Palette.Primary100, RoundedCornerShape(GoldinRadius.Lg)),
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
            ) {
                WheelPickerColumn(
                    items = days,
                    selectedIndex = selDayIdx,
                    onSelectedChange = { selDayIdx = it },
                    fontFamily = MonoFontFamily,
                    modifier = Modifier.width(64.dp),
                )
                WheelPickerColumn(
                    items = INDONESIAN_MONTHS,
                    selectedIndex = selMonthIdx,
                    onSelectedChange = { selMonthIdx = it },
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.weight(1f),
                )
                WheelPickerColumn(
                    items = years,
                    selectedIndex = selYearIdx,
                    onSelectedChange = { selYearIdx = it },
                    fontFamily = MonoFontFamily,
                    modifier = Modifier.width(80.dp),
                )
            }

            // Gradien fade atas & bawah sesuai CSS mask-image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0f)))),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0f), Color.White))),
            )
        }

        Spacer(Modifier.height(GoldinSpace.Xl))
        PrimaryButton(
            text = "Simpan",
            onClick = {
                val d = (selDayIdx + 1).coerceIn(1, maxDays)
                val m = selMonthIdx + 1
                val y = yearStart + selYearIdx
                val formatted = "${d.toString().padStart(2, '0')}/${m.toString().padStart(2, '0')}/$y"
                val isAdult = Rules.isAdult(formatted, today.year, today.monthValue, today.dayOfMonth)
                onSave(formatted, !isAdult)
            },
        )
    }
}

@Composable
private fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    modifier: Modifier = Modifier,
) {
    val safeIndex = selectedIndex.coerceIn(0, (items.lastIndex).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    val centerIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val approx = if (offset > 50) first + 1 else first
            approx.coerceIn(0, (items.lastIndex).coerceAtLeast(0))
        }
    }

    LaunchedEffect(listState.isScrollInProgress, centerIndex) {
        if (!listState.isScrollInProgress && centerIndex != selectedIndex) {
            onSelectedChange(centerIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = 80.dp),
        modifier = modifier.height(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(items) { index, label ->
            val isSelected = index == centerIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        onSelectedChange(index)
                        scope.launch { listState.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = fontFamily,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Palette.Ink else Palette.InkSoft,
                    maxLines = 1,
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun RegisterProfilePreview() = GoldinTheme {
    RegisterProfileScreen(rememberPreviewEnv(false), previewSignup(), {}, {})
}

@ScreenPreview
@Composable
private fun RegisterProfileGooglePreview() = GoldinTheme {
    val googleSignup = SignupDraft().apply {
        email = "indriana.123@gmail.com"
        name = "Indriana"
        viaGoogle = true
    }
    RegisterProfileScreen(rememberPreviewEnv(false), googleSignup, {}, {})
}

@ScreenPreview
@Composable
private fun DatePickerSheetPreview() = GoldinTheme {
    GoldinDatePickerSheet(initialDob = "11/02/2003", onDismiss = {}, onSave = { _, _ -> })
}

/**
 * 06 Selesai (`done`) sesuai prototipe Goldin v1.2 (`Main.dc.html` L961–985):
 * - Menggunakan `GoldinSuccessLayout` (dual pulsing gold rings + animasi fadeUp bertahap)
 * - Judul "Halo, [username]!" + "Yuk, mulai tabungan emasmu sekarang."
 * - HeroPurpleCard: Tabungan Emas 0,0000 gr, ≈ Rp0, Harga beli hari ini
 * - CTA "Mulai nabung" -> Beranda unverified
 */
@Composable
fun RegisterDoneScreen(env: AppEnv, signup: SignupDraft, onStart: () -> Unit) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val displayName = signup.username.ifBlank {
        state.user?.username?.ifBlank { null }
            ?: signup.name.substringBefore(' ').ifBlank { "Sobat Goldin" }
    }
    val buyPrice = state.askPricePerGram

    GoldinSuccessLayout(
        title = "Halo, $displayName!",
        body = "Yuk, mulai tabungan emasmu sekarang.",
        ctaLabel = "Mulai nabung",
        onCtaClick = onStart,
        icon = PrototypeIcons.Check,
    ) {
        HeroPurpleCard(shape = RoundedCornerShape(GoldinRadius.Xxl)) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Tabungan Emas",
                    style = GoldinTypography.bodyMd,
                    color = Palette.Primary200,
                )
                Spacer(Modifier.height(GoldinSpace.Xs))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "0,0000",
                        color = Palette.Gold,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                    )
                    Text(
                        "gr",
                        color = Palette.Gold,
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Text(
                    "≈ Rp0",
                    style = GoldinTypography.bodyMd,
                    color = Palette.Primary100,
                )
                Spacer(Modifier.height(GoldinSpace.Md))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Palette.Primary50.copy(alpha = 0.16f)),
                )
                Spacer(Modifier.height(GoldinSpace.Sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Harga beli hari ini", style = GoldinTypography.bodySm, color = Palette.Primary200)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold)) {
                                append(rupiah(buyPrice))
                            }
                            withStyle(SpanStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold)) {
                                append("/gr")
                            }
                        },
                        color = Palette.Primary50,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun RegisterDonePreview() = GoldinTheme { RegisterDoneScreen(rememberPreviewEnv(false), previewSignup()) {} }

const val DEMO_EMAIL_OTP = MockRepository.DEMO_EMAIL_OTP
