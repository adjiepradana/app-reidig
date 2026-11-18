package com.reidig.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.reidig.app.R
import com.reidig.shared.KycOptions
import com.reidig.shared.MockRepository
import com.reidig.shared.Rules
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val KYC_TOTAL_STEPS = 7

/**
 * Modal konfirmasi keluar e-KYC sesuai Handoff §5 & `Main.dc.html` L4150:
 * - Centered modal (tanpa handle), warning tone, tombol Keluar · Lanjut verifikasi
 */
@Composable
fun KycExitConfirmModal(
    onConfirmExit: () -> Unit,
    onContinue: () -> Unit,
) {
    GoldinModal(
        title = "Yakin ingin keluar dari verifikasi?",
        message = "Data yang sudah diisi akan hilang dan kamu harus mengulang verifikasi dari awal.",
        cancelLabel = "Keluar",
        onCancel = onConfirmExit,
        confirmLabel = "Lanjut verifikasi",
        onConfirm = onContinue,
        tone = GoldinDialogTone.WARNING,
        icon = PrototypeIcons.AlertCircle,
    )
}

/**
 * Kerangka standar 7 langkah e-KYC Goldin v1.2 (`Main.dc.html` L1176–1290):
 * - Header: tombol kembali bulat 44dp (hover neutral-200) -> membuka modal konfirmasi keluar,
 *   judul terpusat Geist SemiBold 16sp, counter kanan Geist Mono 12sp (`step/7`)
 * - Progress bar 7 segmen (`4dp`, gap `4dp`, animasi warna `300ms`)
 * - Area konten scrollable selalu mulai dari atas (scroll = 0)
 * - Footer tetap berlatar putih dengan hairline divider atas & `PairedButtons`
 */
/**
 * Header & 7-segment progress bar standar e-KYC Goldin v1.2 (`Main.dc.html` L1176–1181).
 */
@Composable
internal fun KycStepHeader(
    step: Int,
    title: String,
    onRequestExit: () -> Unit,
    totalSteps: Int = KYC_TOTAL_STEPS,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GoldinSpace.Lg),
    ) {
        Spacer(Modifier.height(GoldinSpace.Xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GoldinControl.TouchMin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoldinIconButton(
                icon = PrototypeIcons.ChevronLeft,
                contentDescription = "Keluar dari verifikasi",
                onClick = onRequestExit,
                hoverColor = Palette.Neutral200,
                modifier = Modifier.offset(x = (-10).dp),
            )
            Text(
                text = title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Palette.Ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.size(GoldinControl.TouchMin),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$step/$totalSteps",
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Palette.InkSecondary,
                )
            }
        }
        Spacer(Modifier.height(GoldinSpace.Xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs),
        ) {
            for (i in 1..totalSteps) {
                val segColor by animateColorAsState(
                    targetValue = if (i <= step) Palette.Primary700 else Palette.Neutral300,
                    animationSpec = tween(300),
                    label = "kycSeg$i",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(segColor),
                )
            }
        }
    }
}

/**
 * Panel keypad tap-to-open e-KYC (`07c OTP nomor HP` & `11/11b Buat PIN`),
 * animasi `sheetUp 300ms` dengan sudut atas `24dp` dan bayangan overlay (`Main.dc.html` L1471–1479).
 */
@Composable
internal fun KycKeypadSheet(
    visible: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(300, easing = GoldinMotion.EaseOutSheet),
            initialOffsetY = { it },
        ) + fadeIn(tween(180)),
        exit = slideOutVertically(
            animationSpec = tween(220, easing = GoldinMotion.EaseOutSheet),
            targetOffsetY = { it },
        ) + fadeOut(tween(150)),
    ) {
        val shape = RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = Palette.Primary950.copy(alpha = 0.16f),
                    spotColor = Palette.Primary950.copy(alpha = 0.16f),
                )
                .clip(shape)
                .background(Color.White)
                .navigationBarsPadding()
                .padding(start = GoldinSpace.Md, end = GoldinSpace.Md, top = GoldinSpace.Sm, bottom = GoldinSpace.Xl),
        ) {
            NumberPad(
                onDigit = onDigit,
                onDelete = onDelete,
                enabled = enabled,
            )
        }
    }
}

/**
 * Checkbox inline standar Goldin Design System v2 (`20x20dp`, hover `primary-600`, checked `primary-700`, tap area >= 44dp).
 */
@Composable
internal fun GoldinInlineCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val boxBg by animateColorAsState(
        targetValue = if (checked) Palette.Primary700 else Color.White,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "inlineCheckBg",
    )
    val boxBorder by animateColorAsState(
        targetValue = when {
            checked -> Palette.Primary700
            isHovered -> Palette.Primary600
            else -> Palette.Neutral300
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "inlineCheckBorder",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GoldinControl.TouchMin)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
            ) { onCheckedChange(!checked) }
            .padding(vertical = GoldinSpace.Xxs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(boxBg)
                .border(1.5.dp, boxBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = PrototypeIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = label,
            style = GoldinTypography.bodyMd,
            color = Palette.Ink,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Kerangka standar 7 langkah e-KYC Goldin v1.2 (`Main.dc.html` L1176–1290):
 * - Header: tombol kembali bulat 44dp (hover neutral-200) -> membuka modal konfirmasi keluar,
 *   judul terpusat Geist SemiBold 16sp, counter kanan Geist Mono 12sp (`step/7`)
 * - Progress bar 7 segmen (`4dp`, gap `4dp`, animasi warna `300ms`)
 * - Area konten scrollable selalu mulai dari atas (scroll = 0)
 * - Footer tetap berlatar putih dengan hairline divider atas & `PairedButtons`
 */
@Composable
fun KycStepScaffold(
    step: Int,
    title: String,
    onExitConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    totalSteps: Int = KYC_TOTAL_STEPS,
    scrollable: Boolean = true,
    scrollState: ScrollState = rememberScrollState(),
    onBodyTopPositioned: ((Float) -> Unit)? = null,
    footerSurface: Color = Color.White,
    showFooterDivider: Boolean = true,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var showExitModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .imePadding(),
    ) {
        KycStepHeader(
            step = step,
            totalSteps = totalSteps,
            title = title,
            onRequestExit = { showExitModal = true },
        )

        // Body
        val bodyMod = Modifier
            .weight(1f)
            .fillMaxWidth()
            .then(
                if (onBodyTopPositioned != null) {
                    Modifier.onGloballyPositioned { onBodyTopPositioned(it.positionInRoot().y) }
                } else {
                    Modifier
                },
            )
            .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(horizontal = GoldinSpace.Lg)
        Column(modifier = bodyMod, content = content)

        // Fixed footer
        if (footer != null) {
            if (showFooterDivider) {
                Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(footerSurface)
                    .navigationBarsPadding()
                    .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Md, bottom = GoldinSpace.Xl),
                content = footer,
            )
        }
    }

    if (showExitModal) {
        KycExitConfirmModal(
            onConfirmExit = {
                showExitModal = false
                onExitConfirmed()
            },
            onContinue = { showExitModal = false },
        )
    }
}

@ScreenPreview
@Composable
private fun KycStepScaffoldPreview() = GoldinTheme {
    KycStepScaffold(
        step = 3,
        title = "Data diri",
        onExitConfirmed = {},
        footer = {
            PairedButtons(
                leftText = "Sebelumnya",
                onLeftClick = {},
                rightText = "Selanjutnya",
                onRightClick = {},
            )
        },
    ) {
        Spacer(Modifier.height(GoldinSpace.Xl))
        Text("Contoh konten langkah e-KYC", style = GoldinTypography.bodyMd, color = Palette.InkSecondary)
    }
}

/**
 * 07a e-KYC · Persetujuan data pribadi (UU No. 27 Tahun 2022 / UU PDP).
 * Di luar progress bar 7 langkah; tombol kembali langsung keluar tanpa modal konfirmasi (`Main.dc.html` L1378–1411).
 */
@Composable
fun KycConsentScreen(onBack: () -> Unit, onNext: () -> Unit) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // Header di luar step bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GoldinSpace.Lg),
        ) {
            Spacer(Modifier.height(GoldinSpace.Xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GoldinControl.TouchMin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoldinIconButton(
                    icon = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    hoverColor = Palette.Neutral200,
                    modifier = Modifier.offset(x = (-10).dp),
                )
                Text(
                    text = "Persetujuan data pribadi",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Palette.Ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(GoldinControl.TouchMin))
            }
        }

        // Scrollable UU PDP content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Md, bottom = GoldinSpace.Xl),
            verticalArrangement = Arrangement.spacedBy(GoldinSpace.Lg),
        ) {
            Text(
                text = "Untuk verifikasi akun, Goldin perlu mengumpulkan dan memproses data pribadimu sesuai UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi.",
                style = GoldinTypography.bodyMd,
                color = Palette.Ink,
            )

            PdpClause(
                title = "Data yang dikumpulkan",
                body = "Nomor HP, foto dan data e-KTP, foto wajah, alamat, serta informasi pekerjaan dan keuangan.",
            )
            PdpClause(
                title = "Tujuan penggunaan",
                body = "Verifikasi identitas, pencocokan data dengan Dukcapil, pencegahan penipuan dan pencucian uang, serta pemenuhan kewajiban hukum.",
            )
            PdpClause(
                title = "Pihak penerima data",
                body = "Dukcapil dan mitra verifikasi identitas, hanya untuk tujuan di atas.",
            )
            PdpClause(
                title = "Penyimpanan",
                body = "Data disimpan terenkripsi selama akunmu aktif dan selama jangka waktu yang diwajibkan peraturan.",
            )
            PdpClause(
                title = "Hakmu",
                body = "Kamu bisa meminta akses, perbaikan, atau penghapusan data, dan menarik persetujuan kapan saja. Tanpa persetujuan, akunmu tidak bisa bertransaksi.",
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                ) { showPrivacySheet = true },
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Selengkapnya di ")
                        withStyle(
                            SpanStyle(
                                color = Palette.Primary700,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append("Kebijakan Privasi")
                        }
                        append(".")
                    },
                    style = GoldinTypography.bodyMd,
                    color = Palette.InkSecondary,
                )
            }

            Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))

            GoldinInlineCheckbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                label = "Saya sudah membaca dan menyetujui pemrosesan data pribadi saya.",
            )
        }

        // Fixed footer
        Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Md, bottom = GoldinSpace.Xxl),
        ) {
            PairedButtons(
                leftText = "Batalkan",
                onLeftClick = onBack,
                rightText = "Selanjutnya",
                onRightClick = onNext,
                rightEnabled = agreed,
            )
        }
    }

    if (showPrivacySheet) {
        GoldinBottomSheet(
            title = "Kebijakan Privasi",
            onDismiss = { showPrivacySheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            ) {
                PdpClause(
                    title = "1. Dasar hukum pemrosesan",
                    body = "Goldin memproses data pribadimu berdasarkan persetujuan eksplisit dan ketentuan UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi serta peraturan perdagangan fisik emas digital di Indonesia.",
                )
                PdpClause(
                    title = "2. Keamanan & enkripsi",
                    body = "Seluruh dokumen identitas dan biometrik disimpan terenkripsi dan hanya digunakan untuk verifikasi akun serta kepatuhan APU-PPT.",
                )
                PdpClause(
                    title = "3. Kontak pelindungan data",
                    body = "Permintaan akses, koreksi, atau penghapusan data pribadi dapat diajukan melalui menu Pusat Bantuan di aplikasi Goldin.",
                )
                Spacer(Modifier.height(GoldinSpace.Xs))
                PrimaryButton(
                    text = "Mengerti",
                    onClick = { showPrivacySheet = false },
                )
            }
        }
    }
}

@Composable
private fun PdpClause(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs)) {
        Text(
            text = title,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
        )
        Text(
            text = body,
            style = GoldinTypography.bodyMd,
            color = Palette.InkSecondary,
        )
    }
}

@ScreenPreview
@Composable
private fun KycConsentPreview() = GoldinTheme { KycConsentScreen({}, {}) }

@Composable
internal fun ConsentRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .clip(shape)
            .background(if (checked) Palette.Primary50 else Color.White)
            .border(if (checked) 2.dp else 1.dp, if (checked) Palette.Primary else Palette.Line, shape)
            .clickable(role = Role.Checkbox) { onChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, null, colors = CheckboxDefaults.colors(checkedColor = Palette.Primary, uncheckedColor = Palette.Line))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = BodyFontFamily, fontSize = 13.5.sp, lineHeight = 19.sp, color = Palette.Ink)
    }
}

/** VisualTransformation pengelompokan nomor HP Indonesia (`812 3456 7890`) tanpa mengganggu posisi kursor. */
private object IndonesianPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = buildString {
            digits.forEachIndexed { i, ch ->
                if (i == 3 || i == 7) append(' ')
                append(ch)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val mapped = when {
                    offset <= 3 -> offset
                    offset <= 7 -> offset + 1
                    else -> offset + 2
                }
                return mapped.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val mapped = when {
                    offset <= 3 -> offset
                    offset <= 8 -> offset - 1
                    else -> offset - 2
                }
                return mapped.coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

private fun formatIndonesianPhoneDash(digits: String): String {
    val clean = digits.filter { it.isDigit() }
    if (clean.isEmpty()) return "+62 812-3456-7890"
    val grouped = when {
        clean.length <= 3 -> clean
        clean.length <= 7 -> "${clean.substring(0, 3)}-${clean.substring(3)}"
        else -> "${clean.substring(0, 3)}-${clean.substring(3, 7)}-${clean.substring(7)}"
    }
    return "+62 $grouped"
}

/**
 * 07b e-KYC · Verifikasi nomor HP (Langkah 1/7).
 * Gabungan input nomor HP `+62` + pilihan kanal 2 kolom WhatsApp/SMS (`Main.dc.html` L1413–1444).
 */
@Composable
fun KycPhoneScreen(
    kyc: KycDraft,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit = onBack,
) {
    val scope = rememberCoroutineScope()
    var phoneDigits by rememberSaveable { mutableStateOf(kyc.phone) }
    var channel by rememberSaveable { mutableStateOf(if (kyc.channel == "SMS") "SMS" else "WhatsApp") }
    var phoneErr by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun sanitizePhoneInput(raw: String): String {
        var digits = raw.filter { it.isDigit() }
        if (digits.startsWith("62")) digits = digits.drop(2)
        if (digits.startsWith("0")) digits = digits.drop(1)
        return digits.take(12)
    }

    fun submit() {
        if (loading || phoneDigits.isEmpty()) return
        if (!Rules.isValidIndonesianPhone(phoneDigits)) {
            phoneErr = "Nomor HP belum sesuai"
            return
        }
        loading = true
        scope.launch {
            delay(550)
            kyc.phone = phoneDigits
            kyc.channel = channel
            loading = false
            onNext()
        }
    }

    KycStepScaffold(
        step = 1,
        title = "Verifikasi nomor HP",
        onExitConfirmed = onExit,
        footerSurface = Palette.Paper,
        showFooterDivider = false,
        footer = {
            PairedButtons(
                leftText = "Sebelumnya",
                onLeftClick = onBack,
                rightText = "Kirim kode",
                onRightClick = ::submit,
                rightEnabled = phoneDigits.isNotEmpty() && !loading,
                rightLoading = loading,
            )
        },
    ) {
        Spacer(Modifier.height(GoldinSpace.Xl))
        FormField(
            label = "Nomor HP",
            value = phoneDigits,
            onValueChange = {
                phoneDigits = sanitizePhoneInput(it)
                phoneErr = null
            },
            prefix = "+62",
            placeholder = "812 3456 7890",
            keyboardType = KeyboardType.Phone,
            transformation = IndonesianPhoneVisualTransformation,
            mandatory = true,
            error = phoneErr,
        )

        Spacer(Modifier.height(GoldinSpace.Xl))
        Text(
            text = "Kirim kode melalui",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
        )
        Spacer(Modifier.height(GoldinSpace.Xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
        ) {
            KycChannelRadioCard(
                label = "WhatsApp",
                selected = channel == "WhatsApp",
                onClick = { channel = "WhatsApp" },
                modifier = Modifier.weight(1f),
                icon = {
                    Image(
                        painter = painterResource(R.drawable.ic_whatsapp),
                        contentDescription = null,
                        modifier = Modifier.size(GoldinControl.IconMd),
                    )
                },
            )
            KycChannelRadioCard(
                label = "SMS",
                selected = channel == "SMS",
                onClick = { channel = "SMS" },
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = PrototypeIcons.SmsMessage,
                        contentDescription = null,
                        tint = Palette.Primary700,
                        modifier = Modifier.size(GoldinControl.IconMd),
                    )
                },
            )
        }
    }
}

@Composable
private fun KycChannelRadioCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "chScale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) Palette.Primary50 else Color.White,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "chBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> Palette.Primary700
            isHovered -> Palette.Primary600
            else -> Palette.Neutral300
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "chBorder",
    )
    val radioRingColor by animateColorAsState(
        targetValue = if (selected) Palette.Primary700 else Palette.Neutral300,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "chRing",
    )
    val shape = RoundedCornerShape(GoldinRadius.Lg)

    Row(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bgColor)
            .border(GoldinBorder.Default, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = GoldinSpace.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
    ) {
        icon()
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(GoldinBorder.Default, radioRingColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Palette.Primary700),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun KycPhonePreview() = GoldinTheme { KycPhoneScreen(previewKyc(), {}, {}) }

/**
 * 07c e-KYC · OTP nomor HP (Langkah 1/7, tap-to-open keypad sesuai Handoff §3 & §5, `Main.dc.html` L1446–1481).
 * - Ketuk 6 kotak OTP -> membuka panel keypad `sheetUp 300ms` (footer bawah disembunyikan).
 * - Ketuk area kosong luar -> menutup keypad & menampilkan `PairedButtons` (`Sebelumnya` & `Selanjutnya` disabled).
 * - Countdown `0:30` memakai font Geist (`BodyFontFamily`), auto-verify pada digit ke-6.
 */
@Composable
fun KycPhoneOtpScreen(
    kyc: KycDraft,
    onBack: () -> Unit,
    onVerified: () -> Unit,
    onExit: () -> Unit = onBack,
    initialKeypadOpen: Boolean = false,
) {
    var code by remember { mutableStateOf("") }
    var otpState by remember { mutableStateOf("idle") } // idle, verifying, success, error
    var kpOpen by remember { mutableStateOf(initialKeypadOpen) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(30) }
    var resendRound by remember { mutableIntStateOf(0) }
    var showExitModal by remember { mutableStateOf(false) }

    val channelLabel = if (kyc.channel == "SMS") "SMS" else "WhatsApp"
    val formattedPhone = remember(kyc.phone) { formatIndonesianPhoneDash(kyc.phone) }

    BackHandler(enabled = kpOpen) { kpOpen = false }

    LaunchedEffect(resendRound) {
        seconds = 30
        while (seconds > 0) {
            delay(1000)
            if (otpState == "idle") seconds--
        }
    }

    LaunchedEffect(otpState) {
        when (otpState) {
            "verifying" -> {
                delay(900)
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
            .statusBarsPadding(),
    ) {
        KycStepHeader(
            step = 1,
            title = "Verifikasi nomor HP",
            onRequestExit = { showExitModal = true },
        )

        // Subjudul + Kotak OTP + Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GoldinSpace.Lg),
        ) {
            Spacer(Modifier.height(GoldinSpace.Xl))
            GoldinFadeUp {
                Text(
                    text = buildAnnotatedString {
                        append("Masukkan kode verifikasi yang dikirimkan melalui $channelLabel ke nomor ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Palette.Primary700)) {
                            append(formattedPhone)
                        }
                    },
                    style = GoldinTypography.bodyLg,
                    color = Palette.InkSecondary,
                )
            }

            Spacer(Modifier.height(GoldinSpace.Xxl))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                    ) {
                        if (otpState == "idle") kpOpen = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                GoldinShakeBox(trigger = shakeTrigger) {
                    CodeBoxes(
                        code = code,
                        error = otpState == "error",
                        verified = otpState == "success",
                        opacity = if (otpState == "verifying") 0.6f else 1f,
                        showCaret = kpOpen,
                    )
                }
            }

            Spacer(Modifier.height(GoldinSpace.Md))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
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
                                Icon(
                                    imageVector = PrototypeIcons.CircleCheckFilled,
                                    contentDescription = null,
                                    tint = Palette.Good,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Nomor terverifikasi",
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Palette.Good,
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
                                Icon(
                                    imageVector = PrototypeIcons.Clock,
                                    contentDescription = null,
                                    tint = Palette.InkSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
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
        }

        // Area kosong luar: mengetuk menutup keypad
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (kpOpen) kpOpen = false
                },
        )

        // Footer saat keypad tertutup
        if (!kpOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, bottom = GoldinSpace.Xxl),
            ) {
                PairedButtons(
                    leftText = "Sebelumnya",
                    onLeftClick = onBack,
                    rightText = "Selanjutnya",
                    onRightClick = {},
                    rightEnabled = false,
                )
            }
        }

        // Panel keypad saat terbuka (sheetUp 300ms)
        KycKeypadSheet(
            visible = kpOpen,
            enabled = otpState == "idle",
            onDigit = { digit ->
                if (code.length < 6 && otpState == "idle") {
                    code += digit
                    if (code.length == 6) otpState = "verifying"
                }
            },
            onDelete = {
                if (otpState == "idle") code = code.dropLast(1)
            },
        )
    }

    if (showExitModal) {
        KycExitConfirmModal(
            onConfirmExit = {
                showExitModal = false
                onExit()
            },
            onContinue = { showExitModal = false },
        )
    }
}

@ScreenPreview
@Composable
private fun KycPhoneOtpClosedPreview() = GoldinTheme {
    KycPhoneOtpScreen(previewKyc(), onBack = {}, onVerified = {}, initialKeypadOpen = false)
}

@ScreenPreview
@Composable
private fun KycPhoneOtpKeypadPreview() = GoldinTheme {
    KycPhoneOtpScreen(previewKyc(), onBack = {}, onVerified = {}, initialKeypadOpen = true)
}

/**
 * Ilustrasi hero (lingkaran primary-50 200dp + cincin putus-putus primary-200 150dp + kotak ungu 96dp radius 32dp berikon emas 48dp)
 * + subjudul + 3 kartu tips standar layar intro e-KYC (`08 Foto e-KTP` & `10 Verifikasi wajah`, `Main.dc.html` L1182–1200).
 */
@Composable
internal fun KycIntroHeroAndTips(
    icon: ImageVector,
    subtitle: String,
    tips: List<String>,
) {
    Spacer(Modifier.height(GoldinSpace.Xl))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Palette.Primary50),
        )
        Canvas(modifier = Modifier.size(150.dp)) {
            drawCircle(
                color = Palette.Primary200,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                ),
            )
        }
        val boxShape = RoundedCornerShape(32.dp)
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = boxShape,
                    ambientColor = Palette.Primary700.copy(alpha = 0.30f),
                    spotColor = Palette.Primary700.copy(alpha = 0.30f),
                )
                .clip(boxShape)
                .background(Palette.Primary700),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Palette.Gold400,
                modifier = Modifier.size(48.dp),
            )
        }
    }

    Spacer(Modifier.height(GoldinSpace.Xl))
    GoldinFadeUp(delayMs = 60) {
        Text(
            text = subtitle,
            style = GoldinTypography.bodyLg,
            color = Palette.InkSecondary,
        )
    }

    Spacer(Modifier.height(GoldinSpace.Xl))
    GoldinFadeUp(delayMs = 120) {
        Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
            tips.forEach { tip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Color.White)
                        .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                        .padding(horizontal = GoldinSpace.Md, vertical = GoldinSpace.Sm),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Palette.Primary50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PrototypeIcons.Check,
                            contentDescription = null,
                            tint = Palette.Primary700,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = tip,
                        style = GoldinTypography.bodyMd,
                        color = Palette.Ink,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(GoldinSpace.Lg))
}

/**
 * 08 e-KYC · Intro Foto e-KTP (Langkah 2/7, `Main.dc.html` L1174–1203).
 */
@Composable
fun KycKtpIntroScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit = onBack,
) {
    KycStepScaffold(
        step = 2,
        title = "Foto e-KTP",
        onExitConfirmed = onExit,
        footerSurface = Palette.Paper,
        showFooterDivider = false,
        footer = {
            PairedButtons(
                leftText = "Sebelumnya",
                onLeftClick = onBack,
                rightText = "Selanjutnya",
                onRightClick = onNext,
            )
        },
    ) {
        KycIntroHeroAndTips(
            icon = PrototypeIcons.IdBadge,
            subtitle = "Siapkan e-KTP asli milikmu.",
            tips = listOf(
                "Cari tempat yang cukup terang",
                "Pastikan e-KTP tidak rusak",
                "Hindari pantulan cahaya saat mengambil foto",
            ),
        )
    }
}

@ScreenPreview
@Composable
private fun KycKtpIntroPreview() = GoldinTheme { KycKtpIntroScreen({}, {}) }

/** Jenis error pemindaian e-KTP yang memicu Error Drawer (`Main.dc.html` L4153–4156). */
enum class KycKtpError { BLURRY, DUKCAPIL }

/** Ilustrasi skematik kartu e-KTP sesuai prototipe (`Main.dc.html` L1215–1225, L1255–1258, L1295–1298). */
@Composable
private fun KtpCardSchematic(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    large: Boolean = false,
) {
    val radius = when {
        compact -> 8.dp
        large -> 16.dp
        else -> 12.dp
    }
    val pad = when {
        compact -> 7.dp
        large -> 24.dp
        else -> 14.dp
    }
    val gap = when {
        compact -> 6.dp
        large -> 20.dp
        else -> 12.dp
    }
    val lineGap = when {
        compact -> 5.dp
        large -> 18.dp
        else -> 9.dp
    }
    val lineRadius = if (large) 7.dp else 2.dp
    val photoW = when {
        compact -> 20.dp
        large -> 70.dp
        else -> 64.dp
    }
    val photoH = when {
        compact -> 28.dp
        large -> 98.dp
        else -> 82.dp
    }
    val lineHeights = when {
        compact -> listOf(5.dp, 4.dp, 4.dp, 4.dp)
        large -> listOf(18.dp, 14.dp, 14.dp, 14.dp)
        else -> listOf(8.dp, 6.dp, 6.dp, 6.dp, 6.dp, 6.dp)
    }
    val lineFractions = if (compact || large) {
        listOf(0.70f, 0.90f, 0.76f, 0.60f)
    } else {
        listOf(0.62f, 0.88f, 0.74f, 0.82f, 0.56f, 0.68f)
    }
    val lineColors = if (compact || large) {
        listOf(Palette.Info500, Palette.Info400, Palette.Info300, Palette.Info300)
    } else {
        listOf(Palette.Info500, Palette.Info400, Palette.Info300, Palette.Info300, Palette.Info300, Palette.Info300)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Palette.Info200)
            .padding(pad),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(lineGap),
        ) {
            lineFractions.forEachIndexed { idx, frac ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(lineHeights[idx])
                        .clip(RoundedCornerShape(lineRadius))
                        .background(lineColors[idx]),
                )
            }
        }
        Box(
            modifier = Modifier
                .width(photoW)
                .height(photoH)
                .clip(RoundedCornerShape(if (large) 7.dp else if (compact) 2.dp else 4.dp))
                .background(Palette.Info300),
        )
    }
}

/**
 * 08b e-KYC · Kamera e-KTP (`Main.dc.html` L1205–1240, latar `neutral-950`).
 * Mendukung kamera sungguhan (CameraX) bila izin diberikan + pratinjau kartu e-KTP fallback,
 * overlay loading `"Membaca data e-KTP…"` (`1300ms`), shutter `72dp` beranimasi `scale 0.92`,
 * serta Error Drawer (`BLURRY` & `DUKCAPIL`) berbasis `GoldinActionDrawer`.
 */
@Composable
fun KycScanIdScreen(
    onBack: () -> Unit,
    onCaptured: () -> Unit,
    kyc: KycDraft? = null,
    initialError: KycKtpError? = null,
    simulatedError: KycKtpError? = null,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()

    fun hasCameraPermission() =
        !inPreview && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(hasCameraPermission()) }
    var cameraReady by remember { mutableStateOf(false) }
    var camBusy by remember { mutableStateOf(false) }
    var capturedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var activeError by remember { mutableStateOf(initialError) }
    var errorTriggeredOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!inPreview && !hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageCapture = remember(hasPermission, inPreview) {
        if (!inPreview && hasPermission) {
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
        } else {
            null
        }
    }
    val previewView = if (!inPreview && hasPermission) {
        rememberBoundPreview(
            selector = CameraSelector.DEFAULT_BACK_CAMERA,
            imageCapture = imageCapture,
            onReady = { cameraReady = true },
            onError = { cameraReady = false },
        )
    } else {
        null
    }

    fun finishOcr() {
        scope.launch {
            delay(1300)
            camBusy = false
            if (simulatedError != null && !errorTriggeredOnce) {
                errorTriggeredOnce = true
                capturedPhoto = null
                activeError = simulatedError
            } else {
                onCaptured()
            }
        }
    }

    fun triggerCapture() {
        if (camBusy) return
        camBusy = true
        val capture = imageCapture
        if (capture != null && cameraReady) {
            capture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees
                        val raw = image.toBitmap()
                        image.close()
                        val oriented = if (rotation == 0) raw else {
                            Bitmap.createBitmap(
                                raw, 0, 0, raw.width, raw.height,
                                Matrix().apply { postRotate(rotation.toFloat()) },
                                true,
                            )
                        }
                        capturedPhoto = oriented
                        kyc?.ktpPhoto = oriented
                        finishOcr()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // Fallback ke alur simulasi agar tidak macet di perangkat/emulator
                        finishOcr()
                    }
                },
            )
        } else {
            finishOcr()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Neutral950)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header: X putih + "Foto e-KTP"
        Spacer(Modifier.height(GoldinSpace.Xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GoldinControl.TouchMin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoldinIconButton(
                icon = PrototypeIcons.XClose,
                contentDescription = "Tutup kamera",
                onClick = onBack,
                tint = Color.White,
                hoverColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.offset(x = (-10).dp),
            )
            Text(
                text = "Foto e-KTP",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(GoldinControl.TouchMin))
        }

        // Instruksi
        Spacer(Modifier.height(GoldinSpace.Xl))
        Text(
            text = "Posisikan e-KTP asli di dalam bingkai dan pastikan tulisannya terbaca.",
            style = GoldinTypography.bodyLg,
            color = Palette.Neutral200,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = GoldinSpace.Xs),
        )

        // Bingkai e-KTP 326x206dp + 4 sudut putih 3dp + loading overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 326.dp)
                    .aspectRatio(326f / 206f),
                contentAlignment = Alignment.Center,
            ) {
                val frameModifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(GoldinRadius.Lg))

                val photo = capturedPhoto
                when {
                    photo != null -> {
                        Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = "Foto e-KTP",
                            contentScale = ContentScale.Crop,
                            modifier = frameModifier,
                        )
                    }
                    previewView != null && cameraReady -> {
                        AndroidView(factory = { previewView }, modifier = frameModifier)
                    }
                    else -> {
                        KtpCardSchematic(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .graphicsLayer { alpha = 0.9f },
                        )
                    }
                }

                // 4 Sudut bingkai putih 3dp melengkung 12dp (`Main.dc.html` L1226)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 3.dp.toPx()
                    val r = 12.dp.toPx()
                    val leg = 30.dp.toPx()
                    val inset = strokePx / 2f
                    val w = size.width
                    val h = size.height
                    val style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)

                    // Kiri atas
                    drawPath(
                        Path().apply {
                            moveTo(inset, inset + leg)
                            lineTo(inset, inset + r)
                            quadraticTo(inset, inset, inset + r, inset)
                            lineTo(inset + leg, inset)
                        },
                        color = Color.White,
                        style = style,
                    )
                    // Kanan atas
                    drawPath(
                        Path().apply {
                            moveTo(w - inset - leg, inset)
                            lineTo(w - inset - r, inset)
                            quadraticTo(w - inset, inset, w - inset, inset + r)
                            lineTo(w - inset, inset + leg)
                        },
                        color = Color.White,
                        style = style,
                    )
                    // Kanan bawah
                    drawPath(
                        Path().apply {
                            moveTo(w - inset, h - inset - leg)
                            lineTo(w - inset, h - inset - r)
                            quadraticTo(w - inset, h - inset, w - inset - r, h - inset)
                            lineTo(w - inset - leg, h - inset)
                        },
                        color = Color.White,
                        style = style,
                    )
                    // Kiri bawah
                    drawPath(
                        Path().apply {
                            moveTo(inset + leg, h - inset)
                            lineTo(inset + r, h - inset)
                            quadraticTo(inset, h - inset, inset, h - inset - r)
                            lineTo(inset, h - inset - leg)
                        },
                        color = Color.White,
                        style = style,
                    )
                }

                // Overlay loading "Membaca data e-KTP…"
                androidx.compose.animation.AnimatedVisibility(
                    visible = camBusy,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Palette.Primary950.copy(alpha = 0.65f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        GoldinSpinner(size = 28.dp, color = Color.White)
                        Spacer(Modifier.height(GoldinSpace.Xs))
                        Text(
                            text = "Membaca data e-KTP…",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // Tombol Shutter bulat 72dp dengan halo putih 30% & scale 0.92 saat ditekan
        Spacer(Modifier.height(GoldinSpace.Xl))
        KtpShutterButton(
            busy = camBusy,
            onClick = ::triggerCapture,
        )
    }

    // Error Drawers (Foto buram & Cek Dukcapil)
    when (activeError) {
        KycKtpError.BLURRY -> {
            GoldinActionDrawer(
                title = "Foto e-KTP kurang jelas",
                body = "Tulisan di e-KTP belum terbaca. Foto ulang di tempat yang terang dan pastikan tidak ada pantulan cahaya.",
                tone = GoldinDialogTone.ERROR,
                icon = PrototypeIcons.AlertCircle,
                onDismiss = { activeError = null },
                buttons = listOf(
                    GoldinOverlayButton(label = "Foto ulang", primary = true, onClick = { activeError = null }),
                ),
            )
        }
        KycKtpError.DUKCAPIL -> {
            GoldinActionDrawer(
                title = "Data belum sesuai",
                body = "NIK atau nama belum sesuai dengan data Dukcapil. Pastikan foto e-KTP jelas dan tidak buram.",
                tone = GoldinDialogTone.ERROR,
                icon = PrototypeIcons.AlertCircle,
                onDismiss = { activeError = null },
                buttons = listOf(
                    GoldinOverlayButton(label = "Foto ulang", primary = true, onClick = { activeError = null }),
                ),
            )
        }
        null -> Unit
    }
}

@Composable
private fun KtpShutterButton(
    busy: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !busy) 0.92f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "shutterScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (busy) 0.5f else 1f,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "shutterAlpha",
    )

    Box(
        modifier = Modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.30f))
            .clickable(
                enabled = !busy,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Ambil foto",
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@ScreenPreview
@Composable
private fun KycScanIdPreview() = GoldinTheme { KycScanIdScreen({}, {}) }

@ScreenPreview
@Composable
private fun KycScanIdBlurryErrorPreview() = GoldinTheme {
    KycScanIdScreen({}, {}, initialError = KycKtpError.BLURRY)
}

/**
 * Field terkunci (`neutral-200`, border `1dp neutral-300`, tinggi `44dp`, ikon gembok `20dp` kanan)
 * untuk NIK (Geist Mono) & Nama lengkap (Geist) hasil OCR e-KTP (`Main.dc.html` L1274).
 */
@Composable
internal fun KycLockedField(
    label: String,
    value: String,
    mono: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(GoldinRadius.Lg)
    Column(modifier.fillMaxWidth()) {
        GoldinFieldLabel(label = label, mandatory = false)
        Spacer(Modifier.height(GoldinSpace.Xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GoldinControl.SizeLg)
                .clip(shape)
                .background(Palette.Neutral200)
                .border(GoldinBorder.Default, Palette.Line, shape)
                .padding(horizontal = GoldinSpace.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
        ) {
            Text(
                text = value,
                fontFamily = if (mono) MonoFontFamily else BodyFontFamily,
                fontWeight = if (mono) FontWeight.Medium else FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = PrototypeIcons.Lock,
                contentDescription = "Terkunci",
                tint = Palette.InkSecondary,
                modifier = Modifier.size(GoldinControl.IconMd),
            )
        }
    }
}

/**
 * 09 e-KYC · Data diri (Langkah 3/7, `Main.dc.html` L1242–1303 & L4075–4089).
 * - Kartu thumbnail e-KTP `88x56dp` (ketuk -> Full-Screen Photo Viewer) + tombol pil `32dp` `"Foto ulang"`.
 * - Seksi Data diri: NIK (terkunci mono), Nama lengkap (terkunci), Jenis kelamin*, Tempat lahir* + Tanggal lahir* (`GoldinDatePickerSheet`).
 * - Seksi Data tambahan: Agama*, Status perkawinan*, Nama ibu kandung* (validasi huruf min 3 karakter).
 */
@Composable
fun KycDataScreen(
    signup: SignupDraft,
    kyc: KycDraft,
    onBack: () -> Unit,
    onRetakeKtp: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit = onBack,
) {
    val ktpUpperName = remember(kyc.name, signup.name) {
        (kyc.name.ifBlank { signup.name }.ifBlank { "Indriana Putri" }).trim().uppercase()
    }
    var gender by rememberSaveable { mutableStateOf(kyc.gender) }
    var pob by rememberSaveable { mutableStateOf(kyc.pob) }
    var dob by rememberSaveable { mutableStateOf(kyc.dob.ifBlank { signup.dob }.ifBlank { "11/02/2003" }) }
    var dobUnder by rememberSaveable { mutableStateOf(kyc.dobUnder) }
    var agama by rememberSaveable { mutableStateOf(kyc.agama) }
    var kawin by rememberSaveable { mutableStateOf(kyc.kawin) }
    var ibu by rememberSaveable { mutableStateOf(kyc.ibuKandung) }

    var pobErr by rememberSaveable { mutableStateOf<String?>(null) }
    var dobErr by rememberSaveable { mutableStateOf<String?>(null) }
    var ibuErr by rememberSaveable { mutableStateOf<String?>(null) }

    var activeSheet by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showKtpViewer by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var bodyTopY by remember { mutableFloatStateOf(0f) }
    val fieldY = remember { mutableMapOf<String, Int>() }

    val canSubmit = gender.isNotBlank() && pob.isNotBlank() && dob.isNotBlank() &&
        agama.isNotBlank() && kawin.isNotBlank() && ibu.isNotBlank()

    BackHandler(enabled = showKtpViewer) { showKtpViewer = false }

    fun validateAndNext() {
        val trimmedPob = pob.trim()
        val trimmedIbu = ibu.trim()
        var firstErrKey: String? = null
        if (!Rules.isValidNameOnlyLetters(trimmedPob)) {
            pobErr = "Tempat lahir hanya boleh berisi huruf"
            if (firstErrKey == null) firstErrKey = "pob"
        }
        if (dobUnder) {
            dobErr = "Kamu harus berusia minimal 18 tahun"
            if (firstErrKey == null) firstErrKey = "dob"
        }
        if (!Rules.isValidNameOnlyLetters(trimmedIbu)) {
            ibuErr = "Nama ibu kandung hanya boleh berisi huruf, minimal 3 karakter"
            if (firstErrKey == null) firstErrKey = "ibu"
        }
        if (firstErrKey != null) {
            fieldY[firstErrKey]?.let { y ->
                scope.launch { scrollState.animateScrollTo((y - 96).coerceAtLeast(0)) }
            }
            return
        }

        kyc.name = ktpUpperName
        kyc.gender = gender
        kyc.pob = trimmedPob
        kyc.dob = dob
        kyc.dobUnder = dobUnder
        kyc.agama = agama
        kyc.kawin = kawin
        kyc.ibuKandung = trimmedIbu
        onNext()
    }

    Box(Modifier.fillMaxSize()) {
        KycStepScaffold(
            step = 3,
            title = "Data diri",
            onExitConfirmed = onExit,
            scrollState = scrollState,
            onBodyTopPositioned = { bodyTopY = it },
            footerSurface = Color.White,
            showFooterDivider = true,
            footer = {
                PairedButtons(
                    leftText = "Sebelumnya",
                    onLeftClick = onBack,
                    rightText = "Selanjutnya",
                    onRightClick = ::validateAndNext,
                    rightEnabled = canSubmit,
                )
            },
        ) {
            Spacer(Modifier.height(GoldinSpace.Xl))

            // Kartu thumbnail e-KTP + "Foto ulang"
            KycKtpThumbnailCard(
                photo = kyc.ktpPhoto,
                onViewPhoto = { showKtpViewer = true },
                onRetake = onRetakeKtp,
            )

            // Seksi 1: Data diri
            Spacer(Modifier.height(GoldinSpace.Xl))
            Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md)) {
                Text(
                    text = "Data diri",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = Palette.Ink,
                )

                KycLockedField(label = "NIK", value = kyc.nik, mono = true)
                KycLockedField(label = "Nama lengkap", value = ktpUpperName, mono = false)

                GoldinSelectField(
                    label = "Jenis kelamin",
                    value = gender,
                    placeholder = "Pilih jenis kelamin",
                    mandatory = true,
                    onClick = { activeSheet = "gender" },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    FormField(
                        label = "Tempat lahir",
                        value = pob,
                        onValueChange = {
                            pob = it
                            pobErr = null
                        },
                        placeholder = "Kota lahir",
                        mandatory = true,
                        error = pobErr,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned {
                                fieldY["pob"] = (it.positionInRoot().y - bodyTopY + scrollState.value).toInt()
                            },
                    )
                    GoldinSelectField(
                        label = "Tanggal lahir",
                        value = dob,
                        placeholder = "DD/MM/YYYY",
                        mandatory = true,
                        trailingIcon = PrototypeIcons.Calendar,
                        error = dobErr,
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned {
                                fieldY["dob"] = (it.positionInRoot().y - bodyTopY + scrollState.value).toInt()
                            },
                    )
                }
            }

            // Pemisah hairline + Seksi 2: Data tambahan
            Spacer(Modifier.height(GoldinSpace.Xl))
            Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))
            Spacer(Modifier.height(GoldinSpace.Xl))

            Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md)) {
                Text(
                    text = "Data tambahan",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = Palette.Ink,
                )

                GoldinSelectField(
                    label = "Agama",
                    value = agama,
                    placeholder = "Pilih agama",
                    mandatory = true,
                    onClick = { activeSheet = "agama" },
                )
                GoldinSelectField(
                    label = "Status perkawinan",
                    value = kawin,
                    placeholder = "Pilih status perkawinan",
                    mandatory = true,
                    onClick = { activeSheet = "kawin" },
                )
                FormField(
                    label = "Nama ibu kandung",
                    value = ibu,
                    onValueChange = {
                        ibu = it
                        ibuErr = null
                    },
                    placeholder = "Masukkan nama ibu kandung",
                    mandatory = true,
                    error = ibuErr,
                    modifier = Modifier.onGloballyPositioned {
                        fieldY["ibu"] = (it.positionInRoot().y - bodyTopY + scrollState.value).toInt()
                    },
                )
            }

            Spacer(Modifier.height(GoldinSpace.Xxl))
        }

        // Full-Screen Photo Viewer e-KTP (`Main.dc.html` L1292–1301)
        AnimatedVisibility(
            visible = showKtpViewer,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEB0A0A0A))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showKtpViewer = false }
                    .padding(GoldinSpace.Xl),
            ) {
                GoldinIconButton(
                    icon = PrototypeIcons.XClose,
                    contentDescription = "Tutup",
                    onClick = { showKtpViewer = false },
                    tint = Color.White,
                    hoverColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GoldinSpace.Xl),
                ) {
                    val photo = kyc.ktpPhoto
                    val cardMod = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 340.dp)
                        .aspectRatio(340f / 214f)
                        .shadow(20.dp, RoundedCornerShape(GoldinRadius.Xl))
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                    if (photo != null) {
                        Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = "Foto e-KTP",
                            contentScale = ContentScale.Crop,
                            modifier = cardMod,
                        )
                    } else {
                        KtpCardSchematic(modifier = cardMod, large = true)
                    }
                    Text(
                        text = "Foto e-KTP",
                        style = GoldinTypography.bodyMd,
                        color = Palette.Neutral300,
                    )
                }
            }
        }
    }

    // Bottom sheets untuk dropdown
    when (activeSheet) {
        "gender" -> GoldinSelectSheet(
            title = "Jenis kelamin",
            options = KycOptions.gender,
            selected = gender,
            onSelect = { gender = it },
            onDismiss = { activeSheet = null },
        )
        "agama" -> GoldinSelectSheet(
            title = "Agama",
            options = KycOptions.agama,
            selected = agama,
            onSelect = { agama = it },
            onDismiss = { activeSheet = null },
        )
        "kawin" -> GoldinSelectSheet(
            title = "Status perkawinan",
            options = KycOptions.kawin,
            selected = kawin,
            onSelect = { kawin = it },
            onDismiss = { activeSheet = null },
        )
    }

    if (showDatePicker) {
        GoldinDatePickerSheet(
            initialDob = dob,
            onDismiss = { showDatePicker = false },
            onSave = { formatted, isUnder ->
                dob = formatted
                dobUnder = isUnder
                dobErr = if (isUnder) "Kamu harus berusia minimal 18 tahun" else null
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun KycKtpThumbnailCard(
    photo: Bitmap?,
    onViewPhoto: () -> Unit,
    onRetake: () -> Unit,
) {
    val thumbInteraction = remember { MutableInteractionSource() }
    val thumbPressed by thumbInteraction.collectIsPressedAsState()
    val thumbScale by animateFloatAsState(
        targetValue = if (thumbPressed) 0.97f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "ktpThumbScale",
    )

    val retakeInteraction = remember { MutableInteractionSource() }
    val retakeHovered by retakeInteraction.collectIsHoveredAsState()
    val retakePressed by retakeInteraction.collectIsPressedAsState()
    val retakeBg by animateColorAsState(
        targetValue = when {
            retakePressed -> Palette.Primary100
            retakeHovered -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "retakeBg",
    )
    val retakeScale by animateFloatAsState(
        targetValue = if (retakePressed) 0.98f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "retakeScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(Color.White)
            .border(GoldinBorder.Default, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
            .padding(GoldinSpace.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
    ) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(56.dp)
                .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                .clip(RoundedCornerShape(GoldinRadius.Md))
                .clickable(
                    interactionSource = thumbInteraction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Lihat foto e-KTP",
                    onClick = onViewPhoto,
                ),
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "Lihat foto e-KTP",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                KtpCardSchematic(modifier = Modifier.fillMaxSize(), compact = true)
            }
        }

        Text(
            text = "e-KTP",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Ink,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .height(GoldinControl.SizeSm)
                .graphicsLayer { scaleX = retakeScale; scaleY = retakeScale }
                .clip(RoundedCornerShape(GoldinRadius.Full))
                .background(retakeBg)
                .border(GoldinBorder.Default, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                .clickable(
                    interactionSource = retakeInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = onRetake,
                )
                .padding(horizontal = GoldinSpace.Sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Foto ulang",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Palette.Primary700,
            )
        }
    }
}

@ScreenPreview
@Composable
private fun KycDataPreview() = GoldinTheme {
    KycDataScreen(previewSignup(), previewKyc(), onBack = {}, onRetakeKtp = {}, onNext = {})
}

/**
 * 09b e-KYC · Alamat (Langkah 4/7, `Main.dc.html` L4069–4103).
 * - Seksi Alamat e-KTP: Provinsi*, Kota/kabupaten*, Kecamatan*, Kelurahan/desa*, Detail alamat*, RT* + RW*, Kode pos*.
 * - Checkbox `"Alamat domisili sama dengan e-KTP"` (tap area >= 44dp); bila tidak dicentang, muncul seksi Alamat domisili.
 */
@Composable
fun KycAddressScreen(
    kyc: KycDraft,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit = onBack,
) {
    // Alamat e-KTP
    var prov by rememberSaveable { mutableStateOf(kyc.prov) }
    var kota by rememberSaveable { mutableStateOf(kyc.kota) }
    var kec by rememberSaveable { mutableStateOf(kyc.kec) }
    var kel by rememberSaveable { mutableStateOf(kyc.kel) }
    var alamat by rememberSaveable { mutableStateOf(kyc.alamat) }
    var rt by rememberSaveable { mutableStateOf(kyc.rt) }
    var rw by rememberSaveable { mutableStateOf(kyc.rw) }
    var kodepos by rememberSaveable { mutableStateOf(kyc.kodepos) }

    // Alamat domisili
    var domSame by rememberSaveable { mutableStateOf(kyc.domSame) }
    var dProv by rememberSaveable { mutableStateOf(kyc.dProv) }
    var dKota by rememberSaveable { mutableStateOf(kyc.dKota) }
    var dKec by rememberSaveable { mutableStateOf(kyc.dKec) }
    var dKel by rememberSaveable { mutableStateOf(kyc.dKel) }
    var dAlamat by rememberSaveable { mutableStateOf(kyc.dAlamat) }
    var dRt by rememberSaveable { mutableStateOf(kyc.dRt) }
    var dRw by rememberSaveable { mutableStateOf(kyc.dRw) }
    var dKodepos by rememberSaveable { mutableStateOf(kyc.dKodepos) }

    // Errors
    var alamatErr by rememberSaveable { mutableStateOf<String?>(null) }
    var rtErr by rememberSaveable { mutableStateOf<String?>(null) }
    var rwErr by rememberSaveable { mutableStateOf<String?>(null) }
    var kodeposErr by rememberSaveable { mutableStateOf<String?>(null) }

    var dAlamatErr by rememberSaveable { mutableStateOf<String?>(null) }
    var dRtErr by rememberSaveable { mutableStateOf<String?>(null) }
    var dRwErr by rememberSaveable { mutableStateOf<String?>(null) }
    var dKodeposErr by rememberSaveable { mutableStateOf<String?>(null) }

    // Active select sheet key: "prov", "kota", "kec", "kel", "dprov", "dkota", "dkec", "dkel"
    var activeSheet by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var bodyTopY by remember { mutableFloatStateOf(0f) }
    val fieldY = remember { mutableMapOf<String, Int>() }

    val ktpFilled = prov.isNotBlank() && kota.isNotBlank() && kec.isNotBlank() && kel.isNotBlank() &&
        alamat.isNotBlank() && rt.isNotBlank() && rw.isNotBlank() && kodepos.isNotBlank()
    val domFilled = dProv.isNotBlank() && dKota.isNotBlank() && dKec.isNotBlank() && dKel.isNotBlank() &&
        dAlamat.isNotBlank() && dRt.isNotBlank() && dRw.isNotBlank() && dKodepos.isNotBlank()
    val canSubmit = ktpFilled && (domSame || domFilled)

    fun validateAndNext() {
        var firstErrKey: String? = null
        if (alamat.trim().length < 5) {
            alamatErr = "Detail alamat terlalu pendek, tambahkan nama jalan dan nomor rumah"
            if (firstErrKey == null) firstErrKey = "alamat"
        }
        if (rt.length != 3) {
            rtErr = "RT harus 3 digit, contoh 008"
            if (firstErrKey == null) firstErrKey = "rt"
        }
        if (rw.length != 3) {
            rwErr = "RW harus 3 digit, contoh 007"
            if (firstErrKey == null) firstErrKey = "rw"
        }
        if (kodepos.length != 5) {
            kodeposErr = "Kode pos harus 5 digit"
            if (firstErrKey == null) firstErrKey = "kodepos"
        }
        if (!domSame) {
            if (dAlamat.trim().length < 5) {
                dAlamatErr = "Detail alamat terlalu pendek, tambahkan nama jalan dan nomor rumah"
                if (firstErrKey == null) firstErrKey = "dalamat"
            }
            if (dRt.length != 3) {
                dRtErr = "RT harus 3 digit, contoh 008"
                if (firstErrKey == null) firstErrKey = "drt"
            }
            if (dRw.length != 3) {
                dRwErr = "RW harus 3 digit, contoh 007"
                if (firstErrKey == null) firstErrKey = "drw"
            }
            if (dKodepos.length != 5) {
                dKodeposErr = "Kode pos harus 5 digit"
                if (firstErrKey == null) firstErrKey = "dkodepos"
            }
        }
        if (firstErrKey != null) {
            fieldY[firstErrKey]?.let { y ->
                scope.launch { scrollState.animateScrollTo((y - 96).coerceAtLeast(0)) }
            }
            return
        }

        kyc.prov = prov
        kyc.kota = kota
        kyc.kec = kec
        kyc.kel = kel
        kyc.alamat = alamat.trim()
        kyc.rt = rt
        kyc.rw = rw
        kyc.kodepos = kodepos
        kyc.domSame = domSame
        kyc.dProv = dProv
        kyc.dKota = dKota
        kyc.dKec = dKec
        kyc.dKel = dKel
        kyc.dAlamat = dAlamat.trim()
        kyc.dRt = dRt
        kyc.dRw = dRw
        kyc.dKodepos = dKodepos
        onNext()
    }

    KycStepScaffold(
        step = 4,
        title = "Alamat",
        onExitConfirmed = onExit,
        scrollState = scrollState,
        onBodyTopPositioned = { bodyTopY = it },
        footerSurface = Color.White,
        showFooterDivider = true,
        footer = {
            PairedButtons(
                leftText = "Sebelumnya",
                onLeftClick = onBack,
                rightText = "Selanjutnya",
                onRightClick = ::validateAndNext,
                rightEnabled = canSubmit,
            )
        },
    ) {
        Spacer(Modifier.height(GoldinSpace.Xl))

        KycAddressFieldsSection(
            title = "Alamat e-KTP",
            fieldKeyPrefix = "",
            onFieldPositioned = { k, rootY -> fieldY[k] = (rootY - bodyTopY + scrollState.value).toInt() },
            prov = prov,
            kota = kota,
            kec = kec,
            kel = kel,
            alamat = alamat,
            rt = rt,
            rw = rw,
            kodepos = kodepos,
            alamatErr = alamatErr,
            rtErr = rtErr,
            rwErr = rwErr,
            kodeposErr = kodeposErr,
            onOpenProv = { activeSheet = "prov" },
            onOpenKota = { activeSheet = "kota" },
            onOpenKec = { activeSheet = "kec" },
            onOpenKel = { activeSheet = "kel" },
            onAlamatChange = { alamat = it; alamatErr = null },
            onRtChange = { rt = it.filter { c -> c.isDigit() }.take(3); rtErr = null },
            onRwChange = { rw = it.filter { c -> c.isDigit() }.take(3); rwErr = null },
            onKodeposChange = { kodepos = it.filter { c -> c.isDigit() }.take(5); kodeposErr = null },
        )

        Spacer(Modifier.height(GoldinSpace.Md))
        GoldinInlineCheckbox(
            checked = domSame,
            onCheckedChange = {
                domSame = it
                if (it) {
                    dAlamatErr = null
                    dRtErr = null
                    dRwErr = null
                    dKodeposErr = null
                }
            },
            label = "Alamat domisili sama dengan e-KTP",
        )

        if (!domSame) {
            Spacer(Modifier.height(GoldinSpace.Xl))
            Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))
            Spacer(Modifier.height(GoldinSpace.Xl))

            KycAddressFieldsSection(
                title = "Alamat domisili",
                fieldKeyPrefix = "d",
                onFieldPositioned = { k, rootY -> fieldY[k] = (rootY - bodyTopY + scrollState.value).toInt() },
                prov = dProv,
                kota = dKota,
                kec = dKec,
                kel = dKel,
                alamat = dAlamat,
                rt = dRt,
                rw = dRw,
                kodepos = dKodepos,
                alamatErr = dAlamatErr,
                rtErr = dRtErr,
                rwErr = dRwErr,
                kodeposErr = dKodeposErr,
                onOpenProv = { activeSheet = "dprov" },
                onOpenKota = { activeSheet = "dkota" },
                onOpenKec = { activeSheet = "dkec" },
                onOpenKel = { activeSheet = "dkel" },
                onAlamatChange = { dAlamat = it; dAlamatErr = null },
                onRtChange = { dRt = it.filter { c -> c.isDigit() }.take(3); dRtErr = null },
                onRwChange = { dRw = it.filter { c -> c.isDigit() }.take(3); dRwErr = null },
                onKodeposChange = { dKodepos = it.filter { c -> c.isDigit() }.take(5); dKodeposErr = null },
            )
        }

        Spacer(Modifier.height(GoldinSpace.Xxl))
    }

    when (activeSheet) {
        "prov" -> GoldinSelectSheet("Provinsi", KycOptions.prov, prov, { prov = it }) { activeSheet = null }
        "kota" -> GoldinSelectSheet("Kota/kabupaten", KycOptions.kota, kota, { kota = it }) { activeSheet = null }
        "kec" -> GoldinSelectSheet("Kecamatan", KycOptions.kec, kec, { kec = it }) { activeSheet = null }
        "kel" -> GoldinSelectSheet("Kelurahan/desa", KycOptions.kel, kel, { kel = it }) { activeSheet = null }
        "dprov" -> GoldinSelectSheet("Provinsi", KycOptions.prov, dProv, { dProv = it }) { activeSheet = null }
        "dkota" -> GoldinSelectSheet("Kota/kabupaten", KycOptions.kota, dKota, { dKota = it }) { activeSheet = null }
        "dkec" -> GoldinSelectSheet("Kecamatan", KycOptions.kec, dKec, { dKec = it }) { activeSheet = null }
        "dkel" -> GoldinSelectSheet("Kelurahan/desa", KycOptions.kel, dKel, { dKel = it }) { activeSheet = null }
    }
}

@Composable
private fun KycAddressFieldsSection(
    title: String,
    fieldKeyPrefix: String,
    onFieldPositioned: (String, Float) -> Unit,
    prov: String,
    kota: String,
    kec: String,
    kel: String,
    alamat: String,
    rt: String,
    rw: String,
    kodepos: String,
    alamatErr: String?,
    rtErr: String?,
    rwErr: String?,
    kodeposErr: String?,
    onOpenProv: () -> Unit,
    onOpenKota: () -> Unit,
    onOpenKec: () -> Unit,
    onOpenKel: () -> Unit,
    onAlamatChange: (String) -> Unit,
    onRtChange: (String) -> Unit,
    onRwChange: (String) -> Unit,
    onKodeposChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md)) {
        Text(
            text = title,
            fontFamily = TitleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = Palette.Ink,
        )
        GoldinSelectField(
            label = "Provinsi",
            value = prov,
            placeholder = "Pilih provinsi",
            mandatory = true,
            onClick = onOpenProv,
        )
        GoldinSelectField(
            label = "Kota/kabupaten",
            value = kota,
            placeholder = "Pilih kota/kabupaten",
            mandatory = true,
            onClick = onOpenKota,
        )
        GoldinSelectField(
            label = "Kecamatan",
            value = kec,
            placeholder = "Pilih kecamatan",
            mandatory = true,
            onClick = onOpenKec,
        )
        GoldinSelectField(
            label = "Kelurahan/desa",
            value = kel,
            placeholder = "Pilih kelurahan/desa",
            mandatory = true,
            onClick = onOpenKel,
        )
        FormField(
            label = "Detail alamat",
            value = alamat,
            onValueChange = onAlamatChange,
            placeholder = "Nama jalan, nomor rumah, blok",
            mandatory = true,
            error = alamatErr,
            modifier = Modifier.onGloballyPositioned {
                onFieldPositioned("${fieldKeyPrefix}alamat", it.positionInRoot().y)
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
            verticalAlignment = Alignment.Top,
        ) {
            FormField(
                label = "RT",
                value = rt,
                onValueChange = onRtChange,
                placeholder = "000",
                keyboardType = KeyboardType.Number,
                mandatory = true,
                error = rtErr,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned {
                        onFieldPositioned("${fieldKeyPrefix}rt", it.positionInRoot().y)
                    },
            )
            FormField(
                label = "RW",
                value = rw,
                onValueChange = onRwChange,
                placeholder = "000",
                keyboardType = KeyboardType.Number,
                mandatory = true,
                error = rwErr,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned {
                        onFieldPositioned("${fieldKeyPrefix}rw", it.positionInRoot().y)
                    },
            )
        }
        FormField(
            label = "Kode pos",
            value = kodepos,
            onValueChange = onKodeposChange,
            placeholder = "00000",
            keyboardType = KeyboardType.Number,
            mandatory = true,
            error = kodeposErr,
            modifier = Modifier.onGloballyPositioned {
                onFieldPositioned("${fieldKeyPrefix}kodepos", it.positionInRoot().y)
            },
        )
    }
}

@ScreenPreview
@Composable
private fun KycAddressPreview() = GoldinTheme { KycAddressScreen(previewKyc(), {}, {}) }

/**
 * 09c e-KYC · Informasi tambahan (Langkah 5/7, `Main.dc.html` L4104–4107).
 * - Seksi Pendidikan & pekerjaan: Pendidikan terakhir*, Pekerjaan*.
 * - Seksi Keuangan: Penghasilan per bulan*, Sumber dana*, Maksud dan tujuan*.
 */
@Composable
fun KycInfoScreen(
    kyc: KycDraft,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit = onBack,
) {
    var pendidikan by rememberSaveable { mutableStateOf(kyc.pendidikan) }
    var pekerjaan by rememberSaveable { mutableStateOf(kyc.pekerjaan) }
    var penghasilan by rememberSaveable { mutableStateOf(kyc.penghasilan) }
    var sumberDana by rememberSaveable { mutableStateOf(kyc.sumberDana) }
    var tujuan by rememberSaveable { mutableStateOf(kyc.tujuan) }

    var activeSheet by remember { mutableStateOf<String?>(null) }

    val canSubmit = pendidikan.isNotBlank() && pekerjaan.isNotBlank() &&
        penghasilan.isNotBlank() && sumberDana.isNotBlank() && tujuan.isNotBlank()

    KycStepScaffold(
        step = 5,
        title = "Informasi tambahan",
        onExitConfirmed = onExit,
        footerSurface = Color.White,
        showFooterDivider = true,
        footer = {
            PairedButtons(
                leftText = "Sebelumnya",
                onLeftClick = onBack,
                rightText = "Selanjutnya",
                onRightClick = {
                    kyc.pendidikan = pendidikan
                    kyc.pekerjaan = pekerjaan
                    kyc.penghasilan = penghasilan
                    kyc.sumberDana = sumberDana
                    kyc.tujuan = tujuan
                    onNext()
                },
                rightEnabled = canSubmit,
            )
        },
    ) {
        Spacer(Modifier.height(GoldinSpace.Xl))

        Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md)) {
            Text(
                text = "Pendidikan & pekerjaan",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Palette.Ink,
            )
            GoldinSelectField(
                label = "Pendidikan terakhir",
                value = pendidikan,
                placeholder = "Pilih pendidikan terakhir",
                mandatory = true,
                onClick = { activeSheet = "pendidikan" },
            )
            GoldinSelectField(
                label = "Pekerjaan",
                value = pekerjaan,
                placeholder = "Pilih pekerjaan",
                mandatory = true,
                onClick = { activeSheet = "pekerjaan" },
            )
        }

        Spacer(Modifier.height(GoldinSpace.Xl))
        Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))
        Spacer(Modifier.height(GoldinSpace.Xl))

        Column(verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md)) {
            Text(
                text = "Keuangan",
                fontFamily = TitleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Palette.Ink,
            )
            GoldinSelectField(
                label = "Penghasilan per bulan",
                value = penghasilan,
                placeholder = "Pilih penghasilan per bulan",
                mandatory = true,
                onClick = { activeSheet = "penghasilan" },
            )
            GoldinSelectField(
                label = "Sumber dana",
                value = sumberDana,
                placeholder = "Pilih sumber dana",
                mandatory = true,
                onClick = { activeSheet = "sumber" },
            )
            GoldinSelectField(
                label = "Maksud dan tujuan",
                value = tujuan,
                placeholder = "Pilih maksud dan tujuan",
                mandatory = true,
                onClick = { activeSheet = "tujuan" },
            )
        }

        Spacer(Modifier.height(GoldinSpace.Xxl))
    }

    when (activeSheet) {
        "pendidikan" -> GoldinSelectSheet("Pendidikan terakhir", KycOptions.pendidikan, pendidikan, { pendidikan = it }) { activeSheet = null }
        "pekerjaan" -> GoldinSelectSheet("Pekerjaan", KycOptions.pekerjaan, pekerjaan, { pekerjaan = it }) { activeSheet = null }
        "penghasilan" -> GoldinSelectSheet("Penghasilan per bulan", KycOptions.penghasilan, penghasilan, { penghasilan = it }) { activeSheet = null }
        "sumber" -> GoldinSelectSheet("Sumber dana", KycOptions.sumber, sumberDana, { sumberDana = it }) { activeSheet = null }
        "tujuan" -> GoldinSelectSheet("Maksud dan tujuan", KycOptions.tujuan, tujuan, { tujuan = it }) { activeSheet = null }
    }
}

@ScreenPreview
@Composable
private fun KycInfoPreview() = GoldinTheme { KycInfoScreen(previewKyc(), {}, {}) }

/**
 * 10 e-KYC · Intro Verifikasi wajah (Langkah 6/7, `Main.dc.html` L1174–1203 & L4164–4171).
 */
@Composable
fun KycFaceIntroScreen(
    onBack: () -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit = onBack,
) {
    KycStepScaffold(
        step = 6,
        title = "Verifikasi wajah",
        onExitConfirmed = onExit,
        footerSurface = Palette.Paper,
        showFooterDivider = false,
        footer = {
            PairedButtons(
                leftText = "Sebelumnya",
                onLeftClick = onBack,
                rightText = "Selanjutnya",
                onRightClick = onStart,
            )
        },
    ) {
        KycIntroHeroAndTips(
            icon = PrototypeIcons.FaceId,
            subtitle = "Verifikasi wajah akan dilakukan untuk mencocokkan dengan foto e-KTP",
            tips = listOf(
                "Cari tempat yang cukup terang",
                "Posisikan wajahmu di dalam bingkai",
                "Lepas masker, kacamata, atau aksesori lain yang menutupi wajah",
            ),
        )
    }
}

@ScreenPreview
@Composable
private fun KycFaceIntroPreview() = GoldinTheme { KycFaceIntroScreen({}, {}) }

/** Tahap Active Liveness kamera wajah (`Main.dc.html` L3417, L3448–3468). */
enum class LivenessStage { IDLE, BLINK, LEFT, RIGHT, SCANNING, VERIFYING }

private val OvalClipShape = GenericShape { size, _ ->
    addOval(Rect(0f, 0f, size.width, size.height))
}

/**
 * 10b e-KYC · Kamera wajah — Active Liveness (`Main.dc.html` L1305–1330, latar putih `Color.White`).
 * - Instruksi dinamis: `"Kedipkan mata"` -> `"Tolehkan kepala ke kiri"` -> `"Tolehkan kepala ke kanan"`.
 * - 3 indikator langkah `Kedip · Kiri · Kanan` berubah menjadi `CircleCheckFilled` hijau tiap selesai.
 * - Bingkai oval `248x320dp` (CameraX kamera depan / siluet fallback) dengan ring elips progres ungu `4dp` terisi bertahap.
 * - Status bawah `"Tahan posisimu…"` -> `GoldinSpinner` + `"Mencocokkan wajah dengan e-KTP…"` + Error Drawer.
 */
@Composable
fun KycFaceScanScreen(
    onBack: () -> Unit,
    onVerified: () -> Unit,
    initialStage: LivenessStage = LivenessStage.IDLE,
    initialError: Boolean = false,
    simulatedError: Boolean = false,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current

    fun hasCameraPermission() =
        !inPreview && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(hasCameraPermission()) }
    var cameraReady by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(initialStage) }
    var showErrorDrawer by remember { mutableStateOf(initialError) }
    var errorTriggeredOnce by remember { mutableStateOf(false) }
    var runId by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!inPreview && !hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = if (!inPreview && hasPermission) {
        rememberBoundPreview(
            selector = CameraSelector.DEFAULT_FRONT_CAMERA,
            imageCapture = null,
            onReady = { cameraReady = true },
            onError = { cameraReady = false },
        )
    } else {
        null
    }

    // Urutan Active Liveness persis `startFace()` di `Main.dc.html` L3448–3468
    LaunchedEffect(runId) {
        if (inPreview || initialError) return@LaunchedEffect
        stage = LivenessStage.IDLE
        delay(600)
        stage = LivenessStage.BLINK
        delay(1600)
        stage = LivenessStage.LEFT
        delay(1600)
        stage = LivenessStage.RIGHT
        delay(1600)
        stage = LivenessStage.SCANNING
        delay(1200)
        stage = LivenessStage.VERIFYING
        delay(1300)
        if (simulatedError && !errorTriggeredOnce) {
            errorTriggeredOnce = true
            stage = LivenessStage.IDLE
            showErrorDrawer = true
        } else {
            onVerified()
        }
    }

    val isActionStage = stage == LivenessStage.BLINK || stage == LivenessStage.LEFT || stage == LivenessStage.RIGHT
    val instruction = when (stage) {
        LivenessStage.BLINK -> "Kedipkan mata"
        LivenessStage.LEFT -> "Tolehkan kepala ke kiri"
        LivenessStage.RIGHT -> "Tolehkan kepala ke kanan"
        else -> "Hadapkan wajahmu ke kamera dan tetap di dalam bingkai"
    }
    val instrColor by animateColorAsState(
        targetValue = if (isActionStage) Palette.Ink else Palette.InkSecondary,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "faceInstrColor",
    )

    val targetProgress = when (stage) {
        LivenessStage.IDLE -> 0f
        LivenessStage.BLINK -> 0.25f
        LivenessStage.LEFT -> 0.50f
        LivenessStage.RIGHT -> 0.75f
        LivenessStage.SCANNING, LivenessStage.VERIFYING -> 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "faceOvalProgress",
    )

    val stepItems = listOf(
        "Kedip" to 0,
        "Kiri" to 1,
        "Kanan" to 2,
    )
    val activeStepIdx = when (stage) {
        LivenessStage.BLINK -> 0
        LivenessStage.LEFT -> 1
        LivenessStage.RIGHT -> 2
        else -> -1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header: X + "Verifikasi wajah"
        Spacer(Modifier.height(GoldinSpace.Xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GoldinControl.TouchMin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoldinIconButton(
                icon = PrototypeIcons.XClose,
                contentDescription = "Tutup kamera",
                onClick = onBack,
                tint = Palette.Ink,
                hoverColor = Palette.Neutral100,
                modifier = Modifier.offset(x = (-10).dp),
            )
            Text(
                text = "Verifikasi wajah",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Palette.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(GoldinControl.TouchMin))
        }

        // Instruksi dinamis
        Spacer(Modifier.height(GoldinSpace.Xl))
        Text(
            text = instruction,
            fontFamily = BodyFontFamily,
            fontWeight = if (isActionStage) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = instrColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = GoldinSpace.Xs),
        )

        // 3 Indikator langkah: Kedip · Kiri · Kanan
        Spacer(Modifier.height(GoldinSpace.Sm))
        Row(
            horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stepItems.forEach { (label, idx) ->
                val done = stage == LivenessStage.SCANNING || stage == LivenessStage.VERIFYING ||
                    (activeStepIdx > -1 && idx < activeStepIdx)
                val on = activeStepIdx == idx
                val stepColor by animateColorAsState(
                    targetValue = when {
                        done -> Palette.Good700
                        on -> Palette.Primary700
                        else -> Palette.InkSoft
                    },
                    animationSpec = tween(GoldinMotion.DurationHover),
                    label = "faceStepColor$idx",
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs),
                ) {
                    Icon(
                        imageVector = if (done) PrototypeIcons.CircleCheckFilled else PrototypeIcons.CircleOutline,
                        contentDescription = null,
                        tint = stepColor,
                        modifier = Modifier.size(GoldinControl.IconSm),
                    )
                    Text(
                        text = label,
                        fontFamily = BodyFontFamily,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = stepColor,
                    )
                }
            }
        }

        // Bingkai oval 248x320dp + ring elips progres ungu 4dp
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(248.dp)
                    .height(320.dp),
                contentAlignment = Alignment.Center,
            ) {
                val ovalModifier = Modifier
                    .fillMaxSize()
                    .padding(GoldinSpace.Xs)
                    .clip(OvalClipShape)

                if (previewView != null && cameraReady) {
                    AndroidView(factory = { previewView }, modifier = ovalModifier)
                } else {
                    // Siluet fallback sesuai `Main.dc.html` L1316–1318
                    Box(
                        modifier = ovalModifier.background(Palette.Neutral800),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Canvas(modifier = Modifier.width(200.dp).height(250.dp)) {
                            val sx = size.width / 200f
                            val sy = size.height / 250f
                            drawCircle(
                                color = Palette.Neutral600,
                                radius = 52f * sx,
                                center = Offset(100f * sx, 108f * sy),
                            )
                            val shoulders = Path().apply {
                                moveTo(18f * sx, 250f * sy)
                                cubicTo(18f * sx, 198f * sy, 54f * sx, 168f * sy, 100f * sx, 168f * sy)
                                cubicTo(146f * sx, 168f * sy, 182f * sx, 198f * sy, 182f * sx, 250f * sy)
                                close()
                            }
                            drawPath(shoulders, color = Palette.Neutral600)
                        }
                    }
                }

                // Ring track netral + progress arc ungu 4dp
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 4.dp.toPx()
                    val half = strokePx / 2f
                    val arcTopLeft = Offset(half, half)
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    drawOval(
                        color = Palette.Neutral300,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx),
                    )
                    if (animatedProgress > 0f) {
                        drawArc(
                            color = Palette.Primary700,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }

        // Status bawah: "Tahan posisimu…" -> Spinner + "Mencocokkan wajah dengan e-KTP…"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (stage) {
                LivenessStage.SCANNING -> {
                    Text(
                        text = "Tahan posisimu…",
                        style = GoldinTypography.bodyMd,
                        color = Palette.InkSecondary,
                    )
                }
                LivenessStage.VERIFYING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
                    ) {
                        GoldinSpinner(size = 18.dp, color = Palette.Primary700)
                        Text(
                            text = "Mencocokkan wajah dengan e-KTP…",
                            style = GoldinTypography.bodyMd,
                            color = Palette.InkSecondary,
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    // Error Drawer wajah belum terverifikasi (`Main.dc.html` L4157)
    if (showErrorDrawer) {
        GoldinActionDrawer(
            title = "Wajah belum terverifikasi",
            body = "Pastikan wajahmu terlihat jelas. Lepas masker dan kacamata, lalu cari tempat yang terang.",
            tone = GoldinDialogTone.ERROR,
            icon = PrototypeIcons.AlertCircle,
            onDismiss = {
                showErrorDrawer = false
                runId++
            },
            buttons = listOf(
                GoldinOverlayButton(
                    label = "Coba lagi",
                    primary = true,
                    onClick = {
                        showErrorDrawer = false
                        runId++
                    },
                ),
            ),
        )
    }
}

@ScreenPreview
@Composable
private fun KycFaceScanPreview() = GoldinTheme {
    KycFaceScanScreen(onBack = {}, onVerified = {}, initialStage = LivenessStage.LEFT)
}

@ScreenPreview
@Composable
private fun KycFaceScanErrorPreview() = GoldinTheme {
    KycFaceScanScreen(onBack = {}, onVerified = {}, initialStage = LivenessStage.IDLE, initialError = true)
}

@Composable
private fun rememberBoundPreview(
    selector: CameraSelector,
    imageCapture: ImageCapture? = null,
    onReady: () -> Unit,
    onError: () -> Unit,
): PreviewView? {
    if (LocalInspectionMode.current) return null
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                provider.unbindAll()
                val useCases = listOfNotNull(preview, imageCapture).toTypedArray()
                provider.bindToLifecycle(lifecycleOwner, selector, *useCases)
                onReady()
            } catch (e: Exception) {
                onError()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            if (future.isDone) runCatching { future.get().unbindAll() }
        }
    }
    return previewView
}

/**
 * 11 Buat PIN & 11b Konfirmasi PIN (Langkah 7/7, tap-to-open keypad sesuai `Main.dc.html` L1332–1361 & L3469–3499).
 * - Ketuk 6 kotak PIN -> membuka `KycKeypadSheet` (`sheetUp 300ms`).
 * - Ketuk area luar / Back -> menutup keypad & menampilkan `PairedButtons` (`Sebelumnya` & `Selanjutnya`/`Selesai` disabled `0.4`).
 * - Validasi PIN via `Rules.pinValidationError(pin, kyc.dob)` + animasi `GoldinShakeBox`.
 * - Setelah Konfirmasi PIN cocok -> tutup keypad -> Drawer `"Aktifkan biometrik?"` -> `repo.submitKycPending(...)` -> `onDone()`.
 */
@Composable
fun KycPinScreen(
    env: AppEnv,
    kyc: KycDraft,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onExit: () -> Unit = onBack,
    initialKeypadOpen: Boolean = false,
    initialShowBiometricDrawer: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var firstPin by rememberSaveable { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var pinErr by remember { mutableStateOf<String?>(null) }
    var pinLock by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var kpOpen by remember { mutableStateOf(initialKeypadOpen) }
    var showBioDrawer by remember { mutableStateOf(initialShowBiometricDrawer) }
    var showExitModal by remember { mutableStateOf(false) }

    val confirming = firstPin != null

    BackHandler(enabled = kpOpen) { kpOpen = false }

    fun finishWithBiometric(enable: Boolean) {
        kyc.biometricEnabled = enable
        showBioDrawer = false
        env.repo.submitKycPending(
            pin = kyc.pin.ifBlank { null },
            fullName = kyc.name.ifBlank { null },
            phone = kyc.phone.ifBlank { null },
            birthDate = kyc.dob.ifBlank { null },
            nik = kyc.nik.ifBlank { null },
            biometricEnabled = enable,
        )
        onDone()
    }

    fun evaluateSixDigits(entered: String) {
        pinLock = true
        scope.launch {
            delay(200)
            val err = if (!confirming) {
                Rules.pinValidationError(entered, kyc.dob)
            } else if (entered != firstPin) {
                "PIN tidak sama. Coba lagi"
            } else {
                null
            }
            if (err != null) {
                pinErr = err
                shakeTrigger++
                delay(700)
                code = ""
                pinLock = false
                return@launch
            }
            if (!confirming) {
                firstPin = entered
                code = ""
                pinErr = null
                pinLock = false
            } else {
                kyc.pin = entered
                pinLock = false
                kpOpen = false
                showBioDrawer = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        KycStepHeader(
            step = 7,
            title = "Buat PIN",
            onRequestExit = { showExitModal = true },
        )

        // Subjudul + 6 Kotak PIN + Error
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GoldinSpace.Lg),
        ) {
            Spacer(Modifier.height(GoldinSpace.Xl))
            GoldinFadeUp {
                Text(
                    text = if (confirming) {
                        "Konfirmasi PIN yang sudah dibuat"
                    } else {
                        "PIN diperlukan untuk melakukan transaksi."
                    },
                    style = GoldinTypography.bodyLg,
                    color = Palette.InkSecondary,
                )
            }

            Spacer(Modifier.height(GoldinSpace.Xxl))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                    ) {
                        if (!pinLock && !showBioDrawer) kpOpen = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                GoldinShakeBox(trigger = shakeTrigger) {
                    CodeBoxes(
                        code = code,
                        masked = true,
                        error = pinErr != null,
                        showCaret = kpOpen && !pinLock,
                    )
                }
            }

            if (pinErr != null) {
                Spacer(Modifier.height(GoldinSpace.Sm))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ErrorLine(pinErr!!)
                }
            }
        }

        // Area kosong luar: mengetuk menutup keypad
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (kpOpen) kpOpen = false
                },
        )

        // Footer saat keypad tertutup
        if (!kpOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, bottom = GoldinSpace.Xxl),
            ) {
                PairedButtons(
                    leftText = "Sebelumnya",
                    onLeftClick = {
                        if (confirming) {
                            firstPin = null
                            code = ""
                            pinErr = null
                        } else {
                            onBack()
                        }
                    },
                    rightText = if (confirming) "Selesai" else "Selanjutnya",
                    onRightClick = {},
                    rightEnabled = false,
                )
            }
        }

        // Panel keypad tap-to-open (sheetUp 300ms)
        KycKeypadSheet(
            visible = kpOpen,
            enabled = !pinLock,
            onDigit = { digit ->
                if (!pinLock && code.length < 6) {
                    pinErr = null
                    val next = code + digit
                    code = next
                    if (next.length == 6) evaluateSixDigits(next)
                }
            },
            onDelete = {
                if (!pinLock) {
                    pinErr = null
                    code = code.dropLast(1)
                }
            },
        )
    }

    if (showBioDrawer) {
        GoldinActionDrawer(
            title = "Aktifkan biometrik?",
            body = "Masuk dan konfirmasi transaksi lebih cepat pakai sidik jari atau wajah.",
            tone = GoldinDialogTone.PRIMARY,
            icon = PrototypeIcons.Fingerprint,
            dismissOnScrimClick = false,
            onDismiss = {},
            buttons = listOf(
                GoldinOverlayButton(label = "Nanti saja", primary = false) { finishWithBiometric(false) },
                GoldinOverlayButton(label = "Aktifkan", primary = true) { finishWithBiometric(true) },
            ),
        )
    }

    if (showExitModal) {
        KycExitConfirmModal(
            onConfirmExit = {
                showExitModal = false
                onExit()
            },
            onContinue = { showExitModal = false },
        )
    }
}

@ScreenPreview
@Composable
private fun KycPinPreview() = GoldinTheme {
    KycPinScreen(rememberPreviewEnv(false), previewKyc(), onBack = {}, onDone = {})
}

@ScreenPreview
@Composable
private fun KycPinBiometricDrawerPreview() = GoldinTheme {
    KycPinScreen(
        rememberPreviewEnv(false),
        previewKyc(),
        onBack = {},
        onDone = {},
        initialShowBiometricDrawer = true,
    )
}

/**
 * 12 e-KYC · Verifikasi sedang diproses (Pending Review 1x24 jam, `Main.dc.html` L1363–1376).
 * Memakai `GoldinSuccessLayout` dengan ikon `PrototypeIcons.Clock` emas + dua cincin emas berdenyut, tanpa kartu Wallet ID.
 */
@Composable
fun KycVerifiedScreen(onDone: () -> Unit) {
    BackHandler { onDone() }
    GoldinSuccessLayout(
        title = "Verifikasi sedang diproses",
        body = "Proses verifikasi membutuhkan waktu 1x24 jam. Sambil menunggu, silakan jelajahi fitur lainnya.",
        ctaLabel = "Jelajahi Goldin",
        onCtaClick = onDone,
        icon = PrototypeIcons.Clock,
    )
}

@ScreenPreview
@Composable
private fun KycVerifiedPreview() = GoldinTheme { KycVerifiedScreen {} }

const val DEMO_PHONE_OTP = MockRepository.DEMO_PHONE_OTP
