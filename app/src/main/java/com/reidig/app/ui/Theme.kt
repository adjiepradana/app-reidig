package com.reidig.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.app.R

/**
 * Font keluarga dari prototipe Goldin v1.2:
 * - Lato (--font-title) untuk judul layar & kartu
 * - Geist (--font-body) untuk teks isi, label tombol, dan input
 * - Geist Mono (--font-mono) HANYA untuk angka read-only (nominal, gramasi, timer, struk)
 */
val TitleFontFamily = FontFamily(
    Font(R.font.lato_bold, FontWeight.SemiBold),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_black, FontWeight.ExtraBold),
    Font(R.font.lato_black, FontWeight.Black),
)

val BodyFontFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_semibold, FontWeight.Bold),
    Font(R.font.lato_black, FontWeight.ExtraBold),
)

val MonoFontFamily = FontFamily(
    Font(R.font.geist_mono_medium, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_semibold, FontWeight.SemiBold),
    Font(R.font.geist_mono_semibold, FontWeight.Bold),
    Font(R.font.geist_mono_semibold, FontWeight.ExtraBold),
)

/** Token skala jarak (spacing) sesuai tokens.json */
object GoldinSpace {
    val None = 0.dp
    val Xxs = 4.dp
    val Xs = 8.dp
    val Sm = 12.dp
    val Md = 16.dp
    val Lg = 20.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 40.dp
    val Xxxxl = 48.dp
    val Xxxxxl = 64.dp
}

/** Token radius sudut sesuai tokens.json */
object GoldinRadius {
    val None = 0.dp
    val Xs = 2.dp
    val Sm = 4.dp
    val Md = 8.dp
    val Lg = 12.dp
    val Xl = 16.dp
    val Xxl = 24.dp
    val Full = 999.dp
}

/** Token ukuran kontrol dan area sentuh */
object GoldinControl {
    val SizeLg = 44.dp       // buttons & inputs (handoff §3)
    val SizeMd = 40.dp       // in-content secondary buttons & filter chips
    val SizeSm = 32.dp       // quick amount chips & small pills
    val TouchMin = 44.dp     // minimum tap area
    val OtpBox = 48.dp       // OTP / PIN boxes (48x48)
    val IconSm = 16.dp
    val IconMd = 20.dp
    val IconLg = 24.dp
    const val IconStroke = 1.75f
}

/** Token ketebalan garis tepi & pemisah sesuai tokens.json */
object GoldinBorder {
    val None = 0.dp
    val Hairline = 0.5.dp    // dividers only
    val Default = 1.dp       // inputs, cards, buttons everywhere
    val FocusRing = 3.dp     // shadow-focus outer halo (0 0 0 3px #d7d4fc)
}

/** Token animasi & easing presisi sesuai CSS @keyframes prototipe Goldin v1.2 */
object GoldinMotion {
    val EaseOutSheet = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    const val DurationPress = 120
    const val DurationColor = 150
    const val DurationHover = 200
    const val DurationModal = 250
    const val DurationSheet = 350
    const val DurationFadeUp = 400
    const val DurationPop = 500
    const val DurationShake = 400
    const val DurationSpin = 800
    const val DurationPulseRing = 2000
    const val DurationFloat = 2500
}

/** Token warna dari Goldin Design System v2 (tokens.json) + contrast trial prototipe. */
object Palette {
    // Purple / Primary scale (brand purple)
    val Primary50 = Color(0xFFF6F6FF)
    val Primary100 = Color(0xFFECEAFE)
    val Primary200 = Color(0xFFD7D4FC)
    val Primary300 = Color(0xFFB3ABF2)
    val Primary400 = Color(0xFF9280EE)
    val Primary500 = Color(0xFF7255D7)
    val Primary600 = Color(0xFF5436AA)
    val Primary = Color(0xFF371F76)       // primary-700 (background-dark-purple)
    val Primary700 = Color(0xFF371F76)
    val Primary800 = Color(0xFF271358)
    val Primary900 = Color(0xFF17083B)
    val Primary950 = Color(0xFF0F052C)

    // Gold / Secondary scale (komoditas emas)
    val Gold50 = Color(0xFFFBF7EC)
    val Gold100 = Color(0xFFF6EFD9)
    val Gold200 = Color(0xFFF0E3B8)
    val Gold300 = Color(0xFFEAD48E)
    val Gold400 = Color(0xFFE1C162)
    val Gold = Color(0xFFE1C162)          // secondary-400 (aksen emas)
    val Gold500 = Color(0xFFD4AF37)
    val Gold600 = Color(0xFFB78E1A)
    val Gold700 = Color(0xFF966E0E)
    val GoldDeep = Color(0xFF966E0E)      // secondary-700
    val GoldSoft = Color(0xFFFBF7EC)      // secondary-50

    // Neutral & Contrast Trial scale
    val Ink = Color(0xFF0F052C)           // text-primary (purple-950)
    val InkSecondary = Color(0xFF525252)  // text-secondary (neutral-600)
    val InkSoft = Color(0xFF737373)       // text-tertiary (neutral-500)
    val Neutral50 = Color(0xFFFAFAFA)
    val Neutral100 = Color(0xFFF5F5F5)
    val Paper = Color(0xFFF5F5F5)         // background-default (neutral-100 contrast trial)
    val Neutral200 = Color(0xFFE5E5E5)    // list row hover/press bg
    val Line = Color(0xFFD4D4D4)          // border-subtle / border-input (neutral-300)
    val FieldBorder = Color(0xFFD4D4D4)
    val Disabled = Color(0xFFA3A3A3)      // neutral-400
    val DisabledText = Color(0xFFA3A3A3)
    val Neutral700 = Color(0xFF404040)
    val Neutral800 = Color(0xFF262626)
    val Neutral900 = Color(0xFF171717)    // toast pill bg
    val Neutral950 = Color(0xFF0A0A0A)    // dark camera background
    val Scrim = Color(0x660F052C)         // rgba(15, 5, 44, 0.4)

    // Status / Semantic colors
    val Good = Color(0xFF16A34A)          // success-600 (icons & borders)
    val Good100 = Color(0xFFDCFCE7)
    val Good200 = Color(0xFFBBF7D0)
    val Good700 = Color(0xFF15803D)       // success-700 (text)
    val GoodSoft = Color(0xFFF0FDF4)      // success-50
    val Mint = Color(0xFF22C55E)          // success-500

    val Critical = Color(0xFFDC2626)      // error-600
    val Critical100 = Color(0xFFFFE2E2)
    val Critical200 = Color(0xFFFECACA)
    val Critical700 = Color(0xFFB91C1C)
    val CriticalBorder = Color(0xFFDC2626)
    val CriticalSoft = Color(0xFFFEF2F2)  // error-50

    val Warn = Color(0xFFA16207)          // warning-700
    val Warn100 = Color(0xFFFEF9C3)
    val Warn600 = Color(0xFFCA8A04)       // warning-600
    val WarnSoft = Color(0xFFFEFCE8)      // warning-50
    val WarnBorder = Color(0xFFFEF08A)    // warning-200

    val Info50 = Color(0xFFEFF6FF)
    val Info100 = Color(0xFFDBEAFE)
    val Info200 = Color(0xFFBFDBFE)
    val Info300 = Color(0xFF93C5FD)
    val Info400 = Color(0xFF60A5FA)
    val Info500 = Color(0xFF3B82F6)
    val Info600 = Color(0xFF2563EB)
    val Info700 = Color(0xFF1D4ED8)

    val Link = Color(0xFF371F76)

    // Aliases for compatibility
    val Bad = Critical
    val BadSoft = CriticalSoft
    val Neutral300 = Line
    val Neutral400 = Disabled
    val Neutral500 = InkSoft
    val Neutral600 = InkSecondary
    val PrimaryPressed = Primary800
    val Lavender = Primary50
    val Ring = Primary100
    val Focus = Primary600
    val Night = Primary950
    val NightSoft = Primary
}

val NightGradient = Brush.verticalGradient(listOf(Palette.Primary, Palette.Primary800))

/** Tipografi lengkap sesuai hierarki Goldin Design System v2 */
object GoldinTypography {
    // Title Group (Lato)
    val title4xl = TextStyle(fontFamily = TitleFontFamily, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp)
    val title3xl = TextStyle(fontFamily = TitleFontFamily, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp)
    val title2xl = TextStyle(fontFamily = TitleFontFamily, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 32.sp)
    val titleXl = TextStyle(fontFamily = TitleFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp)
    val titleLg = TextStyle(fontFamily = TitleFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp)

    // Body Group (Geist)
    val bodyLg = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val bodyMd = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val bodySm = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp)
    val bodyXs = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp)

    // Label Group (Geist)
    val labelLg = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp)
    val labelMd = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val labelSm = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp)

    // Numeric Group (Geist Mono) — HANYA untuk angka read-only
    val numericXl = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp)
    val numericMd = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp)
    val numericSm = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 18.sp)
}

private val ReidigTypography = Typography(
    displayLarge = GoldinTypography.title4xl,
    displayMedium = GoldinTypography.title3xl,
    displaySmall = GoldinTypography.title2xl,
    headlineLarge = GoldinTypography.title2xl,
    headlineMedium = GoldinTypography.titleXl,
    headlineSmall = GoldinTypography.titleLg,
    titleLarge = GoldinTypography.titleLg,
    titleMedium = GoldinTypography.labelLg,
    titleSmall = GoldinTypography.labelMd,
    bodyLarge = GoldinTypography.bodyLg,
    bodyMedium = GoldinTypography.bodyMd,
    bodySmall = GoldinTypography.bodySm,
    labelLarge = GoldinTypography.labelLg,
    labelMedium = GoldinTypography.labelMd,
    labelSmall = GoldinTypography.labelSm,
)

@Composable
fun GoldinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Primary,
            onPrimary = Color.White,
            secondary = Palette.Gold,
            background = Palette.Paper,
            surface = Color.White,
            onSurface = Palette.Ink,
            onBackground = Palette.Ink,
            error = Palette.Critical,
        ),
        typography = ReidigTypography,
        shapes = Shapes(
            small = RoundedCornerShape(GoldinRadius.Lg),
            medium = RoundedCornerShape(GoldinRadius.Xl),
            large = RoundedCornerShape(GoldinRadius.Xxl),
        ),
        content = content,
    )
}

@Composable
fun ReidigTheme(content: @Composable () -> Unit) = GoldinTheme(content)
