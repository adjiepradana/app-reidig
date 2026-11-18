package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val ButtonHeight = GoldinControl.SizeLg
private val ButtonShape = RoundedCornerShape(GoldinRadius.Full)

/** Spinner ikon Tabler `loader-2` berputar 800ms linear tak terbatas sesuai CSS `@keyframes spin`. */
@Composable
fun GoldinSpinner(
    modifier: Modifier = Modifier,
    size: Dp = GoldinControl.IconMd,
    color: Color = Palette.Primary700,
) {
    val transition = rememberInfiniteTransition(label = "goldinSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(GoldinMotion.DurationSpin, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinAngle",
    )
    Icon(
        imageVector = PrototypeIcons.Loader2,
        contentDescription = null,
        tint = color,
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = angle },
    )
}

/** Pembungkus animasi masuk `fadeUp` (12dp -> 0dp, opacity 0 -> 1) sesuai prototipe. */
@Composable
fun GoldinFadeUp(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    durationMs: Int = GoldinMotion.DurationFadeUp,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        shown = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMs, easing = GoldinMotion.EaseOutSheet),
        label = "fadeUpAlpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (shown) 0.dp else 12.dp,
        animationSpec = tween(durationMs, easing = GoldinMotion.EaseOutSheet),
        label = "fadeUpOffset",
    )
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY.toPx()
        },
    ) {
        content()
    }
}

/** Pembungkus animasi getar horizontal `shake` (±8dp, 400ms) untuk error PIN/OTP. */
@Composable
fun GoldinShakeBox(
    trigger: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            offsetX.snapTo(0f)
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = GoldinMotion.DurationShake
                    (-8f) at 80
                    8f at 160
                    (-8f) at 240
                    8f at 320
                    0f at 400
                },
            )
        }
    }
    Box(
        modifier = modifier.graphicsLayer { translationX = offsetX.value.dp.toPx() },
    ) {
        content()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
    height: Dp = ButtonHeight,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val isInteractive = enabled && !loading
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInteractive) 0.98f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "btnScale",
    )
    val targetAlpha by animateFloatAsState(
        targetValue = if (!enabled && !loading) 0.4f else 1f,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "btnAlpha",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            loading -> Palette.Primary700
            !enabled -> Palette.Primary700
            isPressed -> Palette.Primary800
            isHovered -> Palette.Primary600
            else -> Palette.Primary700
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "btnBgColor",
    )

    Button(
        onClick = onClick,
        enabled = isInteractive,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = GoldinSpace.Lg),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = targetAlpha
            },
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Palette.Primary50,
            disabledContainerColor = Palette.Primary700,
            disabledContentColor = Palette.Primary50,
        ),
    ) {
        if (loading) {
            GoldinSpinner(size = 18.dp, color = Palette.Primary50)
            Spacer(Modifier.width(GoldinSpace.Xs))
            Text(
                loadingText ?: text,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, modifier = Modifier.size(GoldinControl.IconMd))
                Spacer(Modifier.width(GoldinSpace.Xs))
            }
            Text(
                text,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = ButtonHeight,
    filledBackground: Color = Color.White,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "secBtnScale",
    )
    val targetAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "secBtnAlpha",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> filledBackground
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary50
            else -> filledBackground
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "secBtnBg",
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = GoldinSpace.Lg),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = targetAlpha
            },
        shape = ButtonShape,
        border = BorderStroke(GoldinBorder.Default, Palette.Primary700),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = Palette.Primary700,
            disabledContainerColor = filledBackground,
            disabledContentColor = Palette.Primary700,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, modifier = Modifier.size(GoldinControl.IconMd))
            Spacer(Modifier.width(GoldinSpace.Xs))
        }
        Text(
            text,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

/** Pasangan tombol standar prototipe: Outline di kiri, Filled di kanan (gap 12dp). */
@Composable
fun PairedButtons(
    leftText: String,
    onLeftClick: () -> Unit,
    rightText: String,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier,
    leftEnabled: Boolean = true,
    rightEnabled: Boolean = true,
    rightLoading: Boolean = false,
    rightLoadingText: String? = null,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm)) {
        SecondaryButton(
            text = leftText,
            onClick = onLeftClick,
            enabled = leftEnabled,
            filledBackground = Color.Transparent,
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = rightText,
            onClick = onRightClick,
            enabled = rightEnabled,
            loading = rightLoading,
            loadingText = rightLoadingText,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    height: Dp = ButtonHeight,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "destScale",
    )
    val targetAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "destAlpha",
    )
    val bgColor by animateColorAsState(
        targetValue = if (filled) {
            when {
                isPressed -> Palette.Critical700
                isHovered -> Palette.Critical700
                else -> Palette.Critical
            }
        } else {
            when {
                isPressed -> Palette.Critical100
                isHovered -> Palette.CriticalSoft
                else -> Color.Transparent
            }
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "destBg",
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = GoldinSpace.Lg),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = targetAlpha
            },
        shape = ButtonShape,
        border = BorderStroke(GoldinBorder.Default, Palette.Critical),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = bgColor,
            contentColor = if (filled) Color.White else Palette.Critical,
            disabledContainerColor = if (filled) Palette.Critical else Color.Transparent,
            disabledContentColor = if (filled) Color.White else Palette.Critical,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, modifier = Modifier.size(GoldinControl.IconMd))
            Spacer(Modifier.width(GoldinSpace.Xs))
        }
        Text(
            text,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
fun LightButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.98f else 1f, tween(GoldinMotion.DurationPress), label = "lightScale")

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Palette.Primary700,
            disabledContainerColor = Color.White.copy(alpha = 0.22f),
            disabledContentColor = Color.White.copy(alpha = 0.75f),
        ),
    ) {
        Text(text, fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun GhostOnDarkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, tween(GoldinMotion.DurationPress), label = "ghostDarkScale")

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = ButtonShape,
        border = BorderStroke(GoldinBorder.Default, Color.White.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
    ) {
        leading?.invoke()
        Text(text, fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Palette.Primary700) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "linkBg",
    )
    val fg by animateColorAsState(
        targetValue = if (isHovered && color == Palette.Primary700) Palette.Primary600 else color,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "linkFg",
    )
    Text(
        text,
        color = fg,
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = modifier
            .clip(RoundedCornerShape(GoldinRadius.Full))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

/** Label field standar DS v2 (`label-md` 14sp SemiBold + tanda `*` merah untuk mandatory). */
@Composable
fun GoldinFieldLabel(
    label: String,
    mandatory: Boolean = false,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = if (isError) Palette.Critical else Palette.Ink,
        )
        if (mandatory) {
            Text(
                text = "*",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Palette.Critical,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

/**
 * Field form terstandar Goldin Design System v2 (tinggi 44dp, radius 12dp, focus ring halo 3dp, hover border).
 * Menggunakan `GoldinInputField` di bawahnya agar seluruh form di aplikasi memiliki ukuran presisi 44dp.
 */
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    hint: String? = null,
    error: String? = null,
    success: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    prefix: String? = null,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    transformation: VisualTransformation = VisualTransformation.None,
    mandatory: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            GoldinFieldLabel(label = label, mandatory = mandatory, isError = error != null)
            Spacer(Modifier.height(GoldinSpace.Xs))
        }
        GoldinInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            isError = error != null,
            isSuccess = success != null && error == null,
            enabled = enabled,
            readOnly = readOnly,
            prefix = prefix,
            leadingIcon = leadingIcon,
            trailingIcon = trailing,
            keyboardType = keyboardType,
            visualTransformation = if (password) PasswordVisualTransformation() else transformation,
            singleLine = singleLine,
        )
        when {
            error != null -> FieldErrorMessage(error)
            success != null -> FieldSuccessMessage(success)
            hint != null -> Text(
                hint,
                fontFamily = BodyFontFamily,
                color = Palette.InkSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = GoldinSpace.Xxs),
            )
        }
    }
}

/**
 * Menggambar halo fokus luar 3dp (`box-shadow: 0 0 0 3px #d7d4fc`) di belakang komponen
 * tanpa menggeser ukuran/perataan kotak `44dp` terhadap label di atasnya.
 */
internal fun Modifier.goldinFocusHalo(
    color: Color,
    cornerRadius: Dp = GoldinRadius.Lg,
    spread: Dp = GoldinBorder.FocusRing,
): Modifier = drawBehind {
    if (color.alpha <= 0f) return@drawBehind
    val s = spread.toPx()
    val r = cornerRadius.toPx() + s
    drawRoundRect(
        color = color,
        topLeft = Offset(-s, -s),
        size = Size(size.width + s * 2f, size.height + s * 2f),
        cornerRadius = CornerRadius(r, r),
    )
}

/**
 * Kolom input teks presisi sesuai Design System v2 & prototipe Goldin v1.2:
 * - Tinggi 44dp, radius 12dp, border 1dp
 * - Focus ring/halo luar konsentris 3dp (`box-shadow: 0 0 0 3px #d7d4fc`) tanpa menggeser layout
 * - Border: error-600 (error), success-600 (success), primary-700 (focus), primary-600 (hover), neutral-300 (idle)
 * - Font input & placeholder: Geist 14sp (bukan Geist Mono untuk input editabel!)
 */
@Composable
fun GoldinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    isSuccess: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    prefix: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minHeight: Dp = GoldinControl.SizeLg,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val innerShape = RoundedCornerShape(GoldinRadius.Lg)

    val ringColor by animateColorAsState(
        targetValue = if (isFocused && !isError && enabled) Palette.Primary200 else Color.Transparent,
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "inputRingColor",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Palette.Neutral300
            isError -> Palette.Bad
            isSuccess -> Palette.Good
            isFocused -> Palette.Primary700
            isHovered -> Palette.Primary600
            else -> Palette.Neutral300
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "inputBorder",
    )
    val bgColor = if (enabled) Color.White else Palette.Neutral100

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (singleLine) Modifier.height(minHeight) else Modifier
            )
            .goldinFocusHalo(ringColor)
            .background(bgColor, innerShape)
            .border(GoldinBorder.Default, borderColor, innerShape),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
    ) {
        Row(
            modifier = if (singleLine) Modifier.fillMaxSize() else Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            when {
                prefix != null -> {
                    Box(
                        modifier = Modifier.padding(start = GoldinSpace.Md, end = GoldinSpace.Sm),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = prefix,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Palette.InkSecondary,
                        )
                    }
                }
                leadingIcon != null -> {
                    Box(
                        modifier = Modifier
                            .padding(start = GoldinSpace.Md, end = GoldinSpace.Sm)
                            .size(GoldinControl.IconMd),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = if (isFocused) Palette.Primary700 else Palette.InkSecondary,
                            modifier = Modifier.size(GoldinControl.IconMd),
                        )
                    }
                }
                else -> Spacer(Modifier.width(GoldinSpace.Md))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (singleLine) Modifier.fillMaxHeight() else Modifier),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Palette.InkSoft,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    readOnly = readOnly,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = singleLine,
                    textStyle = TextStyle(
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = if (enabled) Palette.Ink else Palette.DisabledText,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(Palette.Primary700),
                    interactionSource = interactionSource,
                )
            }

            if (trailingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(GoldinControl.TouchMin),
                    contentAlignment = Alignment.Center,
                ) {
                    trailingIcon()
                }
            } else {
                Spacer(Modifier.width(GoldinSpace.Md))
            }
        }
    }
}

/** Field pilihan (dropdown / date picker) tinggi 44dp, radius 12dp, hover & press state sesuai prototipe. */
@Composable
fun GoldinSelectField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mandatory: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector = PrototypeIcons.ChevronDown,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(GoldinRadius.Lg)

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Palette.Neutral300
            error != null -> Palette.Critical
            isPressed -> Palette.Primary700
            isHovered -> Palette.Primary600
            else -> Palette.Neutral300
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "selectBorder",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled -> Palette.Neutral100
            isPressed -> Palette.Primary50
            else -> Color.White
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "selectBg",
    )

    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            GoldinFieldLabel(label = label, mandatory = mandatory, isError = error != null)
            Spacer(Modifier.height(GoldinSpace.Xs))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GoldinControl.SizeLg)
                .clip(shape)
                .background(bgColor)
                .border(GoldinBorder.Default, borderColor, shape)
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = GoldinSpace.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Palette.InkSecondary,
                    modifier = Modifier.size(GoldinControl.IconMd),
                )
                Spacer(Modifier.width(GoldinSpace.Sm))
            }
            Text(
                text = value.ifEmpty { placeholder },
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = when {
                    !enabled -> Palette.DisabledText
                    value.isEmpty() -> Palette.InkSoft
                    else -> Palette.Ink
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Spacer(Modifier.width(GoldinSpace.Xs))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = Palette.InkSecondary,
                modifier = Modifier.size(GoldinControl.IconMd),
            )
        }
        if (error != null) {
            FieldErrorMessage(error)
        }
    }
}

/** Area teks multi-baris (`minHeight 88dp`, radius 12dp, focus ring 3dp) sesuai prototipe `addrForm`. */
@Composable
fun GoldinTextArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mandatory: Boolean = false,
    error: String? = null,
    minHeight: Dp = 88.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val innerShape = RoundedCornerShape(GoldinRadius.Lg)
    val ringColor by animateColorAsState(
        if (isFocused && error == null) Palette.Primary200 else Color.Transparent,
        tween(GoldinMotion.DurationColor),
        label = "taRing",
    )
    val borderColor by animateColorAsState(
        when {
            error != null -> Palette.Critical
            isFocused -> Palette.Primary700
            isHovered -> Palette.Primary600
            else -> Palette.Neutral300
        },
        tween(GoldinMotion.DurationColor),
        label = "taBorder",
    )

    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            GoldinFieldLabel(label = label, mandatory = mandatory, isError = error != null)
            Spacer(Modifier.height(GoldinSpace.Xs))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .goldinFocusHalo(ringColor)
                .background(Color.White, innerShape)
                .border(GoldinBorder.Default, borderColor, innerShape)
                .padding(horizontal = GoldinSpace.Md, vertical = GoldinSpace.Sm),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSoft,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().height(minHeight - 24.dp),
                textStyle = TextStyle(
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.Ink,
                ),
                cursorBrush = SolidColor(Palette.Primary700),
                interactionSource = interactionSource,
            )
        }
        if (error != null) {
            FieldErrorMessage(error)
        }
    }
}

/** Pesan validasi sukses dengan ikon Tabler `circle-check-filled` & teks `success-700`. */
@Composable
fun FieldSuccessMessage(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = GoldinSpace.Xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xxs),
    ) {
        Icon(
            imageVector = PrototypeIcons.CircleCheckFilled,
            contentDescription = null,
            tint = Palette.Good,
            modifier = Modifier.size(GoldinControl.IconSm),
        )
        Text(
            text = message,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp,
            color = Palette.Good700,
        )
    }
}

@Composable
fun FieldErrorMessage(message: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message) { visible = true }
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(200), label = "errAlpha")
    val offsetY by animateDpAsState(targetValue = if (visible) 0.dp else 4.dp, animationSpec = tween(200), label = "errOffsetY")

    Row(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY.toPx()
            }
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = PrototypeIcons.AlertCircle,
            contentDescription = null,
            tint = Palette.Bad,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = message,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Palette.Bad,
        )
    }
}

@Composable
fun ErrorLine(message: String, modifier: Modifier = Modifier, color: Color = Palette.Bad) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(PrototypeIcons.AlertCircle, null, tint = color, modifier = Modifier.size(16.dp))
        Text(message, fontFamily = BodyFontFamily, color = color, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun RuleLine(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Box(
            Modifier.size(16.dp).clip(CircleShape).background(if (met) Palette.Good else Palette.Neutral200),
            contentAlignment = Alignment.Center,
        ) {
            if (met) Icon(PrototypeIcons.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            fontWeight = if (met) FontWeight.SemiBold else FontWeight.Normal,
            color = if (met) Palette.Good else Palette.InkSoft,
        )
    }
}

/** Kerangka layar: latar neutral-100 (#F5F5F5), top bar, indikator langkah opsional, isi gulir, dan aksi bawah. */
@Composable
fun FormScreen(
    title: String,
    onBack: (() -> Unit)?,
    step: Pair<Int, Int>? = null,
    stepLabels: List<String>? = null,
    bottomDivider: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Palette.Paper).statusBarsPadding().imePadding()) {
        TopTitleBar(
            title = title,
            onBack = onBack,
            trailing = if (step != null && stepLabels == null) "${step.first}/${step.second}" else null,
            trailingContent = trailingAction,
        )
        if (step != null) {
            if (stepLabels != null) {
                LabeledStepBar(step.first, stepLabels)
            } else {
                SegmentedStepBar(step.first, step.second)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
            content = content,
        )
        if (bottom != null) {
            if (bottomDivider) Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))
            Column(
                Modifier.fillMaxWidth().background(Palette.Paper).navigationBarsPadding().padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                content = bottom,
            )
        }
    }
}

/** Tombol ikon bulat 44x44dp dengan umpan balik hover/tekan (`neutral-200`) sesuai aturan Handoff §3. */
@Composable
fun GoldinIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Palette.Ink,
    iconSize: Dp = GoldinControl.IconMd,
    hoverColor: Color = Palette.Neutral200,
    badgeCount: Int = 0,
    showDot: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> hoverColor
            isHovered -> hoverColor.copy(alpha = 0.7f)
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "iconBtnBg",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(GoldinMotion.DurationPress),
        label = "iconBtnScale",
    )
    Box(
        modifier = modifier
            .size(GoldinControl.TouchMin)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Palette.Critical),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = Color.White,
                )
            }
        } else if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 9.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Palette.Critical),
            )
        }
    }
}

@Composable
fun TopTitleBar(
    title: String,
    onBack: (() -> Unit)?,
    trailing: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            GoldinIconButton(
                icon = PrototypeIcons.ChevronLeft,
                contentDescription = "Kembali",
                onClick = onBack,
                tint = Palette.Ink,
                iconSize = GoldinControl.IconMd,
            )
        } else {
            Spacer(Modifier.size(GoldinControl.TouchMin))
        }
        Text(
            title,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        when {
            trailingContent != null -> trailingContent()
            trailing != null -> {
                Box(Modifier.width(GoldinControl.TouchMin), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        trailing,
                        color = Palette.InkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MonoFontFamily,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
            }
            else -> Spacer(Modifier.size(GoldinControl.TouchMin))
        }
    }
}

/**
 * Stepper Design System v2 (28dp dot, ring-focus 4dp pada step aktif, garis 2dp teranimasi 500ms, label-sm).
 */
@Composable
fun LabeledStepBar(current: Int, labels: List<String>) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Xs)
            .clearAndSetSemantics { contentDescription = "Langkah $current dari ${labels.size}" },
        verticalAlignment = Alignment.Top,
    ) {
        labels.forEachIndexed { i, label ->
            val step = i + 1
            val done = step < current
            val active = step == current
            val dotBg by animateColorAsState(
                if (done || active) Palette.Primary700 else Color.White,
                tween(GoldinMotion.DurationHover),
                label = "stepDotBg",
            )
            val dotBorder by animateColorAsState(
                if (done || active) Palette.Primary700 else Palette.Neutral300,
                tween(GoldinMotion.DurationHover),
                label = "stepDotBorder",
            )
            val haloColor by animateColorAsState(
                if (active) Palette.Primary200 else Color.Transparent,
                tween(GoldinMotion.DurationHover),
                label = "stepHalo",
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(haloColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(dotBg)
                            .border(GoldinBorder.Default, dotBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) {
                            Icon(PrototypeIcons.Check, null, tint = Palette.Primary50, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                "$step",
                                fontFamily = BodyFontFamily,
                                color = if (active) Palette.Primary50 else Palette.InkSoft,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(GoldinSpace.Xxs))
                Text(
                    label,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = when {
                        active -> Palette.Ink
                        done -> Palette.Primary700
                        else -> Palette.InkSoft
                    },
                )
            }
            if (step < labels.size) {
                val fillFraction by animateFloatAsState(
                    targetValue = if (done) 1f else 0f,
                    animationSpec = tween(500, easing = GoldinMotion.EaseOutSheet),
                    label = "stepLineFill",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .padding(top = 17.dp, start = 4.dp, end = 4.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(Palette.Line),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fillFraction)
                            .fillMaxHeight()
                            .background(Palette.Primary700),
                    )
                }
            }
        }
    }
}

/** Bar progres terpisah (segmented) untuk alur multi-step (e-KYC 7 langkah, Cetak 3 langkah, Cicil 3 langkah). */
@Composable
fun SegmentedStepBar(current: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Xxs)
            .clearAndSetSemantics { contentDescription = "Langkah $current dari $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (step in 1..total) {
            val segColor by animateColorAsState(
                targetValue = if (step <= current) Palette.Primary700 else Palette.Neutral200,
                animationSpec = tween(GoldinMotion.DurationHover),
                label = "segStep",
            )
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(GoldinRadius.Full))
                    .background(segColor),
            )
        }
    }
}

/** Pilihan dua halaman di dalam satu tab (pil putih di atas latar neutral-200) dengan animasi hover & warna halus. */
@Composable
fun SegmentSwitch(options: List<String>, selected: Int, inset: Dp = GoldinSpace.Lg, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = inset, vertical = GoldinSpace.Xxs)
            .clip(RoundedCornerShape(GoldinRadius.Full))
            .background(Palette.Neutral200)
            .padding(GoldinSpace.Xxs),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val bg by animateColorAsState(
                targetValue = when {
                    active -> Color.White
                    isHovered -> Color.White.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
                animationSpec = tween(GoldinMotion.DurationColor),
                label = "segBg",
            )
            val fg by animateColorAsState(
                targetValue = if (active) Palette.Ink else Palette.InkSecondary,
                animationSpec = tween(GoldinMotion.DurationColor),
                label = "segFg",
            )
            Box(
                Modifier.weight(1f)
                    .height(36.dp)
                    .then(if (active) Modifier.shadow(2.dp, RoundedCornerShape(GoldinRadius.Full)) else Modifier)
                    .clip(RoundedCornerShape(GoldinRadius.Full))
                    .background(bg)
                    .clickable(interactionSource = interactionSource, indication = null) { if (!active) onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontFamily = BodyFontFamily,
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
fun Headline(
    title: String,
    subtitle: String? = null,
    centered: Boolean = false,
    titleColor: Color = Palette.Ink,
    subtitleRich: AnnotatedString? = null,
) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Text(
        title,
        style = GoldinTypography.title2xl,
        color = titleColor,
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
    if (subtitle != null) {
        Spacer(Modifier.height(GoldinSpace.Xs))
        Text(
            subtitle,
            style = GoldinTypography.bodyMd,
            color = Palette.InkSecondary,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (subtitleRich != null) {
        Spacer(Modifier.height(GoldinSpace.Xs))
        Text(
            subtitleRich,
            style = GoldinTypography.bodyMd,
            color = Palette.InkSecondary,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Dot PIN dengan animasi pop 200ms saat digit diisi sesuai prototipe:
 * @keyframes pop { 0% { transform:scale(.4); opacity:0 } 70% { transform:scale(1.15); opacity:1 } 100% { transform:scale(1) } }
 */
@Composable
fun PopPinDot(
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        }
        scale.animateTo(1.15f, tween(140, easing = FastOutSlowInEasing))
        scale.animateTo(1.0f, tween(60, easing = LinearEasing))
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .clip(CircleShape)
            .background(if (isError) Palette.Bad else Palette.Ink),
    )
}

/**
 * Digit OTP dengan animasi pop 200ms saat digit diisi sesuai prototipe.
 */
@Composable
fun PopDigit(
    digit: String,
    isVerified: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(digit) {
        launch {
            alpha.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        }
        scale.animateTo(1.15f, tween(140, easing = FastOutSlowInEasing))
        scale.animateTo(1.0f, tween(60, easing = LinearEasing))
    }

    Text(
        text = digit,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = if (isVerified) Palette.Good700 else Palette.Ink,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        },
    )
}

/** Kotak 6 digit untuk OTP dan PIN sesuai desain prototipe (48x48dp, corner 12dp, outer ring 3.5dp ungu muda). */
@Composable
fun CodeBoxes(
    code: String,
    total: Int = 6,
    masked: Boolean = false,
    error: Boolean = false,
    verified: Boolean = false,
    opacity: Float = 1f,
    showCaret: Boolean = true,
) {
    val caretTransition = rememberInfiniteTransition(label = "codeBoxesCaret")
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

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = opacity }
            .semantics { contentDescription = "${code.length} dari $total angka dimasukkan" },
        horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val char = code.getOrNull(index)
            val isActive = showCaret && index == code.length && !verified && !error

            val borderColor = when {
                verified -> Palette.Good
                error -> Palette.Bad
                isActive || char != null -> Palette.Primary700
                else -> Palette.Neutral300
            }
            val bgColor = when {
                verified -> Palette.GoodSoft
                error -> Palette.BadSoft
                else -> Color.White
            }

            Box(
                modifier = Modifier
                    .size(55.dp)
                    .background(
                        color = if (isActive) Palette.Primary200 else Color.Transparent,
                        shape = outerShape,
                    )
                    .padding(3.5.dp)
                    .background(bgColor, innerShape)
                    .border(
                        width = if (isActive || char != null || error || verified) 1.2.dp else 1.dp,
                        color = borderColor,
                        shape = innerShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    char != null && masked -> {
                        PopPinDot(isError = error)
                    }
                    char != null -> {
                        PopDigit(digit = char.toString(), isVerified = verified)
                    }
                    isActive -> {
                        Box(
                            Modifier
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
    }
}

@Composable
fun CodeDots(length: Int, total: Int = 6, error: Boolean = false, dotColor: Color = Palette.Primary) {
    CodeBoxes(code = "•".repeat(length.coerceAtMost(total)), total = total, masked = true, error = error)
}

/**
 * Toast pil gelap mengambang di bawah layar sesuai prototipe Goldin v1.2 (`neutral-900`, `radius-full`, `fadeUp .2s`).
 */
@Composable
fun GoldinToast(
    message: String?,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    durationMs: Long = 3000L,
) {
    if (message == null) return
    LaunchedEffect(message) {
        delay(durationMs)
        onDismiss()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = GoldinSpace.Lg, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        GoldinFadeUp(durationMs = GoldinMotion.DurationHover) {
            Row(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(GoldinRadius.Full))
                    .clip(RoundedCornerShape(GoldinRadius.Full))
                    .background(Palette.Neutral900)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = GoldinSpace.Md, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
            ) {
                Text(
                    text = message,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Keypad numerik in-app (OTP & PIN) dengan animasi tekan `primary-100` + `scale(0.94)` (100ms) sesuai prototipe.
 * Mendukung tombol kiri opsional (misal ikon sidik jari pada layar kunci PIN).
 */
@Composable
fun NumberPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    leftKey: (@Composable () -> Unit)? = null,
) {
    val rows = listOf("123", "456", "789")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { digit -> PadKey(digit.toString(), enabled, Modifier.weight(1f)) { onDigit(digit) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (leftKey != null) {
                Box(Modifier.weight(1f).height(52.dp), contentAlignment = Alignment.Center) { leftKey() }
            } else {
                Spacer(Modifier.weight(1f))
            }
            PadKey("0", enabled, Modifier.weight(1f)) { onDigit('0') }
            PadActionKey(
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = onDelete,
            ) {
                Icon(
                    PrototypeIcons.Backspace,
                    contentDescription = "Hapus",
                    tint = if (enabled) Palette.Ink else Palette.DisabledText,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun PadKey(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    PadActionKey(enabled = enabled, modifier = modifier, onClick = onClick) {
        Text(
            label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = BodyFontFamily,
            color = if (enabled) Palette.Ink else Palette.DisabledText,
        )
    }
}

@Composable
private fun PadActionKey(
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = tween(100),
        label = "padScale",
    )
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "padBg",
    )
    Box(
        modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(bg)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Gaya kartu permukaan bersih sesuai desain prototipe: latar putih/tint, garis tepi 1dp halus, dan sudut membulat 16dp.
 */
fun Modifier.cardDepth(
    face: Color = Color.White,
    shape: Shape = RoundedCornerShape(GoldinRadius.Xl),
    outline: Color? = null,
): Modifier = this
    .shadow(1.dp, shape, ambientColor = Palette.Primary950.copy(alpha = 0.05f), spotColor = Palette.Primary950.copy(alpha = 0.08f))
    .clip(shape)
    .background(face, shape)
    .border(GoldinBorder.Default, outline ?: Palette.Line, shape)

/** Kartu ungu tua (#371F76) sesuai desain prototipe. */
fun Modifier.darkCardDepth(
    face: Color = Palette.Primary700,
    shape: Shape = RoundedCornerShape(GoldinRadius.Xxl),
): Modifier = this
    .shadow(6.dp, shape, ambientColor = Palette.Primary700.copy(alpha = 0.25f), spotColor = Palette.Primary700.copy(alpha = 0.35f))
    .clip(shape)
    .background(face, shape)

/** Kartu hero ungu tua dengan lingkaran dekoratif di sudut kanan atas seperti pada prototipe. */
@Composable
fun HeroPurpleCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GoldinRadius.Xxl),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.fillMaxWidth().darkCardDepth(Palette.Primary700, shape),
    ) {
        Box(
            Modifier.size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Palette.Primary600.copy(alpha = 0.5f)),
        )
        Box(Modifier.fillMaxWidth().padding(GoldinSpace.Md), content = content)
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tint: Color = Color.White,
    outline: Color? = null,
    radius: Dp = GoldinRadius.Xl,
    padding: Dp = GoldinSpace.Md,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = outline ?: when (tint) {
        Palette.Primary50 -> Palette.Primary100
        Palette.GoldSoft -> Palette.Gold200
        Palette.WarnSoft -> Palette.WarnBorder
        Palette.GoodSoft -> Palette.Good200
        Palette.CriticalSoft -> Palette.Critical200
        Palette.Info50 -> Palette.Info100
        else -> Palette.Line
    }
    val shape = RoundedCornerShape(radius)
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        val isPressed by interactionSource.collectIsPressedAsState()
        val bg by animateColorAsState(
            targetValue = when {
                isPressed -> if (tint == Color.White) Palette.Neutral100 else tint
                isHovered -> if (tint == Color.White) Palette.Neutral50 else tint
                else -> tint
            },
            animationSpec = tween(GoldinMotion.DurationColor),
            label = "cardBg",
        )
        Column(
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bg, shape)
                .border(GoldinBorder.Default, border, shape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(padding),
            content = content,
        )
    } else {
        Column(
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(tint, shape)
                .border(GoldinBorder.Default, border, shape)
                .padding(padding),
            content = content,
        )
    }
}

enum class ChipTone { GOOD, WARN, CRITICAL, NEUTRAL, GOLD, INFO, PRIMARY }

/** Status selalu dengan ikon Tabler dan label, bukan warna saja. */
@Composable
fun StatusChip(
    text: String,
    tone: ChipTone,
    icon: ImageVector? = PrototypeIcons.CircleCheckFilled,
) {
    val (bg, fg) = when (tone) {
        ChipTone.GOOD -> Palette.GoodSoft to Palette.Good700
        ChipTone.WARN -> Palette.WarnSoft to Palette.Warn
        ChipTone.CRITICAL -> Palette.CriticalSoft to Palette.Critical
        ChipTone.NEUTRAL -> Palette.Neutral100 to Palette.InkSecondary
        ChipTone.PRIMARY -> Palette.Primary50 to Palette.Primary700
        ChipTone.GOLD -> Palette.GoldSoft to Palette.GoldDeep
        ChipTone.INFO -> Palette.Info50 to Palette.Info700
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(GoldinRadius.Full))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, fontFamily = BodyFontFamily, color = fg, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SummaryRow(label: String, value: String, strong: Boolean = false, valueColor: Color = Palette.Ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            fontFamily = BodyFontFamily,
            color = if (strong) Palette.Ink else Palette.InkSecondary,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = if (strong) 16.sp else 14.sp,
            lineHeight = if (strong) 24.sp else 20.sp,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(GoldinSpace.Sm))
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (strong) 16.sp else 14.sp,
            lineHeight = if (strong) 24.sp else 20.sp,
            fontFamily = MonoFontFamily,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun TokenCodeText(code: String, modifier: Modifier = Modifier) {
    Text(code, fontFamily = MonoFontFamily, fontSize = 12.sp, color = Palette.InkSoft, modifier = modifier)
}

/**
 * Lencana sukses berdenyut (pulsing gold rings) sesuai desain `06 Selesai` / `kycDone` prototipe Goldin v1.2:
 * - 2 cincin emas (`secondary-400`, 2dp) berdenyut bergantian (2000ms ease-out, delay 0ms & 1000ms, scale 0.8 -> 1.9, alpha 0.7 -> 0)
 * - Lingkaran ungu `96dp` (`primary-700`) masuk dengan animasi `pop` 500ms
 * - Ikon emas `48dp` masuk dengan animasi `pop` 400ms (delay 250ms)
 */
@Composable
fun DoubleRingSuccessBadge(icon: ImageVector = PrototypeIcons.Check) {
    val ringTransition = rememberInfiniteTransition(label = "successPulseRings")
    val ring1Progress by ringTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(GoldinMotion.DurationPulseRing, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring1",
    )
    val ring2Progress = (ring1Progress + 0.5f) % 1f

    val circleScale = remember { Animatable(0.4f) }
    val circleAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.4f) }
    val iconAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            circleAlpha.animateTo(1f, tween(250))
        }
        launch {
            circleScale.animateTo(1.12f, tween(320, easing = GoldinMotion.EaseOutSheet))
            circleScale.animateTo(1f, tween(180))
        }
        delay(250)
        launch {
            iconAlpha.animateTo(1f, tween(200))
        }
        iconScale.animateTo(1.15f, tween(240, easing = GoldinMotion.EaseOutSheet))
        iconScale.animateTo(1f, tween(160))
    }

    Box(
        modifier = Modifier.size(168.dp),
        contentAlignment = Alignment.Center,
    ) {
        listOf(ring1Progress, ring2Progress).forEach { progress ->
            val ringScale = 0.8f + (1.1f * progress)
            val ringAlpha = (0.7f * (1f - progress)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .border(2.dp, Palette.Gold, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = circleScale.value
                    scaleY = circleScale.value
                    alpha = circleAlpha.value
                }
                .clip(CircleShape)
                .background(Palette.Primary700),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Palette.Gold,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                        alpha = iconAlpha.value
                    },
            )
        }
    }
}

/**
 * Template layar sukses standar Goldin v1.2 (e-KYC, Registrasi, Tabungan Rutin, Cicilan, dll.)
 * dengan `DoubleRingSuccessBadge` (check / clock) + animasi `fadeUp` bertahap.
 */
@Composable
fun GoldinSuccessLayout(
    title: String,
    body: String,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    icon: ImageVector = PrototypeIcons.Check,
    ctaEnabled: Boolean = true,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DoubleRingSuccessBadge(icon = icon)
            Spacer(Modifier.height(GoldinSpace.Md))
            GoldinFadeUp(delayMs = 300) {
                Text(
                    text = title,
                    style = GoldinTypography.title2xl,
                    color = Palette.Primary700,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(GoldinSpace.Xs))
            GoldinFadeUp(delayMs = 380) {
                Text(
                    text = body,
                    style = GoldinTypography.bodyMd,
                    color = Palette.InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = GoldinSpace.Xs),
                )
            }
            if (extraContent != null) {
                Spacer(Modifier.height(GoldinSpace.Xl))
                GoldinFadeUp(delayMs = 460) {
                    Column(Modifier.fillMaxWidth(), content = extraContent)
                }
            }
        }
        GoldinFadeUp(delayMs = 540, modifier = Modifier.fillMaxWidth()) {
            PrimaryButton(text = ctaLabel, onClick = onCtaClick, enabled = ctaEnabled)
        }
    }
}

val ScreenPadding = PaddingValues(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md)

/** Menampilkan angka tanggal sebagai HH/BB/TTTT tanpa mengubah isi kolom, sehingga kursor tetap benar. */
object DateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatDateDigits(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val shifted = when {
                    offset <= 2 -> offset
                    offset <= 4 -> offset + 1
                    else -> offset + 2
                }
                return shifted.coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val shifted = when {
                    offset <= 2 -> offset
                    offset <= 5 -> offset - 1
                    else -> offset - 2
                }
                return shifted.coerceAtMost(text.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

fun formatDateDigits(digits: String) = buildString {
    digits.forEachIndexed { index, c ->
        if (index == 2 || index == 4) append('/')
        append(c)
    }
}

// =========================================================================
// Komponen Goldin Design System v2 & Handoff §3
// =========================================================================

/**
 * Menampilkan nominal uang dengan aturan tegas:
 * - "Rp" dalam font Geist (BodyFontFamily)
 * - Angka dalam font Geist Mono (MonoFontFamily)
 */
@Composable
fun GoldinMoneyText(
    amount: Long,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = GoldinTypography.numericMd,
    color: Color = Palette.Ink,
    showPrefix: Boolean = true,
) {
    val digits = kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    val isNegative = amount < 0

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        if (showPrefix) {
            Text(
                text = if (isNegative) "−Rp" else "Rp",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (textStyle.fontSize.value * 0.85f).sp,
                color = color,
                modifier = Modifier.padding(bottom = 1.dp, end = 1.dp),
            )
        } else if (isNegative) {
            Text(
                text = "−",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = textStyle.fontSize,
                color = color,
            )
        }
        Text(
            text = grouped,
            fontFamily = MonoFontFamily,
            fontWeight = textStyle.fontWeight ?: FontWeight.SemiBold,
            fontSize = textStyle.fontSize,
            lineHeight = textStyle.lineHeight,
            color = color,
        )
    }
}

/**
 * Menampilkan berat emas (4 desimal koma) dengan aturan tegas:
 * - Angka dalam font Geist Mono (MonoFontFamily)
 * - " gr" dalam font Geist (BodyFontFamily)
 */
@Composable
fun GoldinGramText(
    grams: Double,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = GoldinTypography.numericMd,
    color: Color = Palette.Ink,
) {
    val intPart = grams.toLong()
    val fracPart = kotlin.math.round((grams - intPart) * 10000).toLong().coerceIn(0, 9999)
    val fracStr = fracPart.toString().padStart(4, '0')

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "$intPart,$fracStr",
            fontFamily = MonoFontFamily,
            fontWeight = textStyle.fontWeight ?: FontWeight.SemiBold,
            fontSize = textStyle.fontSize,
            lineHeight = textStyle.lineHeight,
            color = color,
        )
        Text(
            text = " gr",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (textStyle.fontSize.value * 0.85f).sp,
            color = color,
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}

/**
 * Kartu Notice sesuai aturan Handoff §3:
 * - Kartu dengan tombol CTA = soft purple (primary-50) dengan border primary-100
 * - Jika aksi mandatory (misal verifikasi) -> tombol filled
 * - Jika aksi opsional -> tombol outline transparan (tanpa isian putih)
 */
@Composable
fun NoticeCard(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null,
    isMandatory: Boolean = false,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(GoldinRadius.Xl)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Primary50)
            .border(GoldinBorder.Default, Palette.Primary100, shape)
            .padding(GoldinSpace.Md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Palette.Primary100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Palette.Primary700, modifier = Modifier.size(GoldinControl.IconMd))
                }
                Spacer(Modifier.width(GoldinSpace.Sm))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) {
                    Text(
                        title,
                        style = GoldinTypography.labelMd,
                        color = Palette.Ink,
                    )
                    Spacer(Modifier.height(GoldinSpace.Xxs))
                }
                Text(
                    message,
                    style = GoldinTypography.bodySm,
                    color = Palette.InkSecondary,
                )
            }
        }
        if (ctaLabel != null && onCtaClick != null) {
            Spacer(Modifier.height(GoldinSpace.Md))
            if (isMandatory) {
                PrimaryButton(
                    text = ctaLabel,
                    onClick = onCtaClick,
                    height = GoldinControl.SizeMd,
                )
            } else {
                SecondaryButton(
                    text = ctaLabel,
                    onClick = onCtaClick,
                    height = GoldinControl.SizeMd,
                    filledBackground = Color.Transparent,
                )
            }
        }
    }
}

/**
 * Catatan informasi tanpa tombol aksi sesuai aturan Handoff §3:
 * Menggunakan latar biru muda (info-50), border info-100, lingkaran ikon info-100, dan ikon info-700.
 */
@Composable
fun InfoNote(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector = PrototypeIcons.InfoCircle,
) {
    val shape = RoundedCornerShape(GoldinRadius.Lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Info50)
            .border(GoldinBorder.Default, Palette.Info100, shape)
            .padding(horizontal = GoldinSpace.Md, vertical = GoldinSpace.Sm),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Palette.Info100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Palette.Info700,
                modifier = Modifier.size(GoldinControl.IconMd),
            )
        }
        Spacer(Modifier.width(GoldinSpace.Sm))
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    title,
                    style = GoldinTypography.labelMd,
                    color = Palette.Ink,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                message,
                style = GoldinTypography.bodySm,
                color = Palette.InkSecondary,
            )
        }
    }
}

/**
 * Chip filter sesuai aturan Handoff §3 (gaya filter Riwayat & Cabang):
 * - Terpilih: background primary-50, border primary-100, teks primary-700
 * - Tidak terpilih: background putih, border neutral-300, teks neutral-600
 * - Umpan balik hover & animasi warna 200ms
 */
@Composable
fun GoldinFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = GoldinControl.SizeMd,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(GoldinRadius.Full)

    val bg by animateColorAsState(
        targetValue = when {
            selected -> Palette.Primary50
            isPressed -> Palette.Neutral100
            isHovered -> Palette.Primary50.copy(alpha = 0.5f)
            else -> Color.White
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "filterChipBg",
    )
    val border by animateColorAsState(
        targetValue = when {
            selected -> Palette.Primary100
            isHovered -> Palette.Primary300
            else -> Palette.Line
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "filterChipBorder",
    )
    val textCol by animateColorAsState(
        targetValue = if (selected) Palette.Primary700 else Palette.InkSecondary,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "filterChipFg",
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bg)
            .border(GoldinBorder.Default, border, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = GoldinSpace.Md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (height >= GoldinControl.SizeMd) 14.sp else 12.sp,
            lineHeight = if (height >= GoldinControl.SizeMd) 20.sp else 18.sp,
            color = textCol,
        )
    }
}

/**
 * Chip pilihan cepat (misal nominal top up 10rb/50rb/100rb/500rb atau tenor/DP) sesuai DS v2 `Chip`:
 * - Tinggi visual 32dp dengan area sentuh minimal 44dp
 * - Terpilih: solid `primary-700`, teks `text-inverse`; diam: putih, border `border-input`, teks `primary-700`
 */
@Composable
fun GoldinChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(GoldinRadius.Full)
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, tween(GoldinMotion.DurationPress), label = "choiceScale")

    val bg by animateColorAsState(
        targetValue = when {
            selected -> Palette.Primary700
            isHovered || isPressed -> Palette.Primary50
            else -> Color.White
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "choiceBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected || isHovered) Palette.Primary700 else Palette.Line,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "choiceBorder",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Palette.Primary50 else Palette.Primary700,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "choiceFg",
    )

    Box(
        modifier = modifier
            .height(GoldinControl.TouchMin)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(GoldinControl.SizeSm)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(shape)
                .background(bg)
                .border(GoldinBorder.Default, border, shape)
                .padding(horizontal = GoldinSpace.Md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp,
                color = fg,
            )
        }
    }
}

/**
 * Baris daftar polos full-bleed sesuai aturan Handoff §3 (Notifikasi, Mutasi terbaru, Riwayat, Mutasi Tabungan):
 * Tanpa kartu, dengan hairline divider 0.5dp dan latar hover/tekan `neutral-200` (terlihat di atas `neutral-100`).
 */
@Composable
fun GoldinPlainListRow(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    showDivider: Boolean = true,
    horizontalPadding: Dp = GoldinSpace.Lg,
    verticalPadding: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            onClick != null && (isPressed || isHovered) -> Palette.Neutral200
            else -> backgroundColor
        },
        animationSpec = tween(GoldinMotion.DurationColor),
        label = "plainRowBg",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .then(
                    if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                    else Modifier
                )
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .height(GoldinBorder.Hairline)
                    .background(Palette.Line),
            )
        }
    }
}

data class GoldinRadioOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val enabled: Boolean = true,
    val leading: (@Composable () -> Unit)? = null,
)

/**
 * Daftar pilihan radio dalam SATU kartu (`radius-lg` 12dp) dengan pemisah hairline sesuai Handoff §3:
 * Baris terpilih berlatar `primary-50` + indikator radio 20dp `primary-700`.
 */
@Composable
fun <T> GoldinRadioCard(
    options: List<GoldinRadioOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(GoldinRadius.Lg)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(GoldinBorder.Default, Palette.Line, shape),
    ) {
        options.forEachIndexed { index, opt ->
            val isSelected = opt.value == selected
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val bg by animateColorAsState(
                targetValue = when {
                    !opt.enabled -> Color.White
                    isSelected -> Palette.Primary50
                    isHovered -> Palette.Neutral50
                    else -> Color.White
                },
                animationSpec = tween(GoldinMotion.DurationColor),
                label = "radioRowBg",
            )
            val ringColor by animateColorAsState(
                targetValue = if (isSelected) Palette.Primary700 else Palette.Line,
                animationSpec = tween(GoldinMotion.DurationColor),
                label = "radioRing",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .alpha(if (opt.enabled) 1f else 0.45f)
                    .clickable(enabled = opt.enabled, interactionSource = interactionSource, indication = null) {
                        onSelect(opt.value)
                    }
                    .padding(horizontal = GoldinSpace.Md, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (opt.leading != null) {
                    opt.leading.invoke()
                    Spacer(Modifier.width(GoldinSpace.Sm))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs)) {
                        Text(
                            text = opt.title,
                            style = GoldinTypography.labelMd,
                            color = Palette.Ink,
                        )
                        if (opt.badge != null) {
                            StatusChip(text = opt.badge, tone = ChipTone.GOOD, icon = null)
                        }
                    }
                    if (opt.subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = opt.subtitle,
                            style = GoldinTypography.bodySm,
                            color = Palette.InkSecondary,
                        )
                    }
                }
                Spacer(Modifier.width(GoldinSpace.Sm))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(GoldinBorder.Default, ringColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Palette.Primary700),
                        )
                    }
                }
            }
            if (index < options.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(GoldinBorder.Hairline)
                        .background(Palette.Line),
                )
            }
        }
    }
}

/**
 * Empty state standar Handoff §3:
 * Ikon lingkaran `64dp` (`primary-50`, ikon `primary-700` 28dp) di atas, judul + teks di bawah,
 * dan CTA outline full-width opsional bila ada aksi.
 */
@Composable
fun GoldinEmptyState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Palette.Primary50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Primary700, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(GoldinSpace.Md))
        Text(
            text = title,
            style = GoldinTypography.labelLg,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.height(GoldinSpace.Xxs))
            Text(
                text = message,
                style = GoldinTypography.bodyMd,
                color = Palette.InkSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (ctaLabel != null && onCtaClick != null) {
            Spacer(Modifier.height(GoldinSpace.Lg))
            SecondaryButton(
                text = ctaLabel,
                onClick = onCtaClick,
                filledBackground = Color.Transparent,
            )
        }
    }
}

// =========================================================================
// Unified Modal vs Bottom Sheet (Drawer) — Handoff §3 (Aturan 13 Nov)
// =========================================================================

enum class GoldinDialogTone { PRIMARY, INFO, WARNING, ERROR }

data class GoldinOverlayButton(
    val label: String,
    val primary: Boolean = false,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Bottom Sheet (Drawer) standar Design System v2 & prototipe Goldin v1.2:
 * - Scrim `rgba(15, 5, 44, 0.4)` `fadeIn 200ms`
 * - Panel sudut atas `radius-2xl` (24dp), handle `36x4dp` (`neutral-300`), padding 20dp samping / 32dp bawah
 * - Animasi masuk `sheetUp 350ms CubicBezier(0.2, 0.8, 0.2, 1)`
 */
@Composable
fun GoldinBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    showCloseButton: Boolean = true,
    dismissOnScrimClick: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "sheetScrim",
    )
    val sheetFraction by animateFloatAsState(
        targetValue = if (entered) 0f else 1f,
        animationSpec = tween(GoldinMotion.DurationSheet, easing = GoldinMotion.EaseOutSheet),
        label = "sheetSlide",
    )

    Dialog(
        onDismissRequest = { if (dismissOnScrimClick) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Scrim.copy(alpha = 0.4f * scrimAlpha)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        enabled = dismissOnScrimClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = size.height * sheetFraction }
                    .shadow(16.dp, RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                    .clip(RoundedCornerShape(topStart = GoldinRadius.Xxl, topEnd = GoldinRadius.Xxl))
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Xs, bottom = GoldinSpace.Xxl),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(Palette.Neutral300),
                )
                if (title != null) {
                    Spacer(Modifier.height(GoldinSpace.Md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = title,
                            style = GoldinTypography.titleLg,
                            color = Palette.Ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (showCloseButton) {
                            GoldinIconButton(
                                icon = PrototypeIcons.XClose,
                                contentDescription = "Tutup",
                                onClick = onDismiss,
                                tint = Palette.InkSecondary,
                                hoverColor = Palette.Neutral100,
                                modifier = Modifier.offset(x = 10.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(GoldinSpace.Md))
                }
                content()
            }
        }
    }
}

/** Konten bersama untuk Modal (tengah) dan Action Drawer (bawah) sesuai `msgSheet` prototipe Goldin v1.2. */
@Composable
private fun GoldinMessageContent(
    tone: GoldinDialogTone,
    icon: ImageVector,
    title: String,
    body: String,
    buttons: List<GoldinOverlayButton>,
    iconTopSpace: Dp,
    onIconClick: (() -> Unit)? = null,
    checkboxLabel: String? = null,
    checkboxChecked: Boolean = false,
    onCheckboxChange: ((Boolean) -> Unit)? = null,
) {
    val (iconBg, iconFg) = when (tone) {
        GoldinDialogTone.PRIMARY -> Palette.Primary50 to Palette.Primary700
        GoldinDialogTone.INFO -> Palette.Info50 to Palette.Info700
        GoldinDialogTone.WARNING -> Palette.WarnSoft to Palette.Warn
        GoldinDialogTone.ERROR -> Palette.CriticalSoft to Palette.Critical
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (iconTopSpace > 0.dp) Spacer(Modifier.height(iconTopSpace))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(iconBg)
                .then(if (onIconClick != null) Modifier.clickable(onClick = onIconClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconFg, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(GoldinSpace.Md))
        Text(
            text = title,
            style = GoldinTypography.titleLg,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(GoldinSpace.Xs))
        Text(
            text = body,
            style = GoldinTypography.bodyMd,
            color = Palette.InkSecondary,
            textAlign = TextAlign.Center,
        )
        if (checkboxLabel != null && onCheckboxChange != null) {
            Spacer(Modifier.height(GoldinSpace.Lg))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckboxChange(!checkboxChecked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = checkboxChecked,
                    onCheckedChange = onCheckboxChange,
                    colors = CheckboxDefaults.colors(checkedColor = Palette.Primary700, uncheckedColor = Palette.Line),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(GoldinSpace.Sm))
                Text(
                    text = checkboxLabel,
                    style = GoldinTypography.bodyMd,
                    color = Palette.Ink,
                )
            }
        }
        if (buttons.isNotEmpty()) {
            Spacer(Modifier.height(GoldinSpace.Xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Sm),
            ) {
                buttons.forEach { btn ->
                    when {
                        btn.destructive -> DestructiveButton(
                            text = btn.label,
                            onClick = btn.onClick,
                            enabled = btn.enabled,
                            filled = btn.primary,
                            modifier = Modifier.weight(1f),
                        )
                        btn.primary -> PrimaryButton(
                            text = btn.label,
                            onClick = btn.onClick,
                            enabled = btn.enabled,
                            modifier = Modifier.weight(1f),
                        )
                        else -> SecondaryButton(
                            text = btn.label,
                            onClick = btn.onClick,
                            enabled = btn.enabled,
                            filledBackground = Color.Transparent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal dialog konfirmasi terpusat (tanpa handle drawer, animasi `modalIn 250ms`) sesuai aturan Handoff §3:
 * Untuk konfirmasi ya/tidak aksi pengguna (Hentikan / Jeda tabungan, Hapus rekening, Keluar flow, Konfirmasi cetak).
 */
@Composable
fun GoldinModal(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String = "Batal",
    onCancel: () -> Unit,
    isDestructive: Boolean = false,
    tone: GoldinDialogTone = if (isDestructive) GoldinDialogTone.ERROR else GoldinDialogTone.WARNING,
    icon: ImageVector = if (isDestructive) PrototypeIcons.AlertCircle else PrototypeIcons.AlertCircle,
    checkboxLabel: String? = null,
    checkboxChecked: Boolean = false,
    onCheckboxChange: ((Boolean) -> Unit)? = null,
    confirmEnabled: Boolean = true,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = tween(GoldinMotion.DurationModal, easing = GoldinMotion.EaseOutSheet),
        label = "modalScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "modalAlpha",
    )

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.Scrim.copy(alpha = 0.4f * alpha))
                .padding(horizontal = GoldinSpace.Lg),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .shadow(16.dp, RoundedCornerShape(GoldinRadius.Xxl))
                    .clip(RoundedCornerShape(GoldinRadius.Xxl))
                    .background(Color.White)
                    .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Xl, bottom = GoldinSpace.Lg),
            ) {
                GoldinMessageContent(
                    tone = tone,
                    icon = icon,
                    title = title,
                    body = message,
                    iconTopSpace = 0.dp,
                    checkboxLabel = checkboxLabel,
                    checkboxChecked = checkboxChecked,
                    onCheckboxChange = onCheckboxChange,
                    buttons = listOf(
                        GoldinOverlayButton(label = cancelLabel, primary = false, onClick = onCancel),
                        GoldinOverlayButton(
                            label = confirmLabel,
                            primary = true,
                            destructive = isDestructive,
                            enabled = confirmEnabled,
                            onClick = onConfirm,
                        ),
                    ),
                )
            }
        }
    }
}

/**
 * Action/Error/Info Drawer (bottom sheet dengan handle 36x4dp, `sheetUp 350ms`) sesuai Handoff §3:
 * Untuk error drawer, info drawer ("Verifikasi sedang ditinjau", PIN terkunci), CTA verifikasi, dan prompt biometrik.
 */
@Composable
fun GoldinActionDrawer(
    title: String,
    body: String,
    buttons: List<GoldinOverlayButton>,
    onDismiss: () -> Unit,
    tone: GoldinDialogTone = GoldinDialogTone.PRIMARY,
    icon: ImageVector = PrototypeIcons.InfoCircle,
    dismissOnScrimClick: Boolean = true,
    onIconClick: (() -> Unit)? = null,
) {
    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = null,
        dismissOnScrimClick = dismissOnScrimClick,
    ) {
        GoldinMessageContent(
            tone = tone,
            icon = icon,
            title = title,
            body = body,
            buttons = buttons,
            iconTopSpace = GoldinSpace.Xl,
            onIconClick = onIconClick,
        )
    }
}

/** Drawer gate verifikasi saat pengguna belum e-KYC menekan aksi terproteksi (Handoff §5). */
@Composable
fun VerificationGateDrawer(
    onDismiss: () -> Unit,
    onVerifyNow: () -> Unit,
) {
    GoldinActionDrawer(
        title = "Verifikasi akunmu sekarang",
        body = "Cukup 2 menit untuk mulai beli emas pertamamu.",
        tone = GoldinDialogTone.PRIMARY,
        icon = PrototypeIcons.ShieldCheck,
        onDismiss = onDismiss,
        buttons = listOf(
            GoldinOverlayButton(label = "Verifikasi sekarang", primary = true, onClick = onVerifyNow),
        ),
    )
}

/** Drawer info saat e-KYC masih dalam peninjauan 1x24 jam (Handoff §5, `Main.dc.html` L4140). */
@Composable
fun PendingVerificationDrawer(
    onDismiss: () -> Unit,
) {
    GoldinActionDrawer(
        title = "Verifikasi sedang ditinjau",
        body = "Kamu bisa mulai transaksi setelah verifikasi selesai. Hasilnya akan kami kirim lewat notifikasi.",
        tone = GoldinDialogTone.PRIMARY,
        icon = PrototypeIcons.Clock,
        onDismiss = onDismiss,
        buttons = listOf(
            GoldinOverlayButton(label = "Mengerti", primary = true, onClick = onDismiss),
        ),
    )
}

/** Bottom sheet pemilih opsi (48dp row height, hairline divider, hover `neutral-100`, radio 20dp) sesuai `selSheet`. */
@Composable
fun GoldinSelectSheet(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    GoldinBottomSheet(onDismiss = onDismiss, title = title) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            options.forEachIndexed { idx, label ->
                val isSelected = label == selected
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val bg by animateColorAsState(
                    if (isHovered) Palette.Neutral100 else Color.Transparent,
                    tween(GoldinMotion.DurationColor),
                    label = "selOptBg",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Md))
                        .background(bg)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onSelect(label)
                            onDismiss()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        style = GoldinTypography.bodyMd,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Palette.Primary700 else Palette.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(GoldinSpace.Sm))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(GoldinBorder.Default, if (isSelected) Palette.Primary700 else Palette.Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Palette.Primary700))
                        }
                    }
                }
                if (idx < options.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(GoldinBorder.Hairline).background(Palette.Line))
                }
            }
        }
    }
}

/**
 * Header sticky sesuai aturan Handoff §3 & `Main.dc.html`:
 * - Judul terpusat Geist SemiBold: **18sp** pada 4 tab utama (Transaksi, Portofolio, Cetak, Profil), **16sp** pada halaman dalam
 * - Latar `background-default` (`neutral-100`), pemisah hairline 0.5dp muncul setelah halaman digulir
 * - Bila `scrollState` diberikan pada tab, header otomatis bersembunyi saat gulir ke bawah dan muncul kembali saat gulir ke atas
 */
@Composable
fun StickyTabHeader(
    title: String,
    modifier: Modifier = Modifier,
    isTabHeader: Boolean = true,
    scrollState: ScrollState? = null,
    onBack: (() -> Unit)? = null,
    leftContent: (@Composable () -> Unit)? = null,
    rightAction: (@Composable () -> Unit)? = null,
) {
    var hidden by remember { mutableStateOf(false) }
    var scrolled by remember { mutableStateOf(false) }
    var lastScroll by remember { mutableIntStateOf(0) }

    if (scrollState != null) {
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.value }.collect { y ->
                val dy = y - lastScroll
                lastScroll = y
                if (y <= 8) {
                    hidden = false
                    scrolled = false
                } else {
                    scrolled = true
                    if (isTabHeader && dy > 10) hidden = true
                    else if (dy < -6) hidden = false
                }
            }
        }
    }

    val translateFraction by animateFloatAsState(
        targetValue = if (hidden) -1f else 0f,
        animationSpec = tween(GoldinMotion.DurationModal),
        label = "hdrHide",
    )
    val borderColor by animateColorAsState(
        targetValue = if (scrolled || scrollState == null) Palette.Line else Color.Transparent,
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "hdrBorder",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = size.height * translateFraction }
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = GoldinSpace.Lg),
        ) {
            when {
                leftContent != null -> Box(Modifier.align(Alignment.CenterStart)) { leftContent() }
                onBack != null -> GoldinIconButton(
                    icon = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-10).dp),
                )
            }
            Text(
                text = title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (isTabHeader) 18.sp else 16.sp,
                lineHeight = if (isTabHeader) 26.sp else 24.sp,
                color = Palette.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
            if (rightAction != null) {
                Box(Modifier.align(Alignment.CenterEnd)) {
                    rightAction()
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GoldinBorder.Hairline)
                .background(borderColor),
        )
    }
}

@ScreenPreview
@Composable
private fun GoldinDesignSystemPreview() = GoldinTheme {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .verticalScroll(rememberScrollState())
            .padding(GoldinSpace.Lg),
        verticalArrangement = Arrangement.spacedBy(GoldinSpace.Md),
    ) {
        LabeledStepBar(current = 2, labels = listOf("Buat akun", "Verifikasi", "Data diri"))
        FormField(
            label = "Email",
            value = "reiuser@gmail.com",
            onValueChange = {},
            placeholder = "nama@email.com",
            mandatory = true,
            leadingIcon = PrototypeIcons.Mail,
            success = "Email tersedia",
        )
        GoldinSelectField(
            label = "Tanggal debit",
            value = "Tanggal 5",
            placeholder = "Pilih tanggal",
            mandatory = true,
            onClick = {},
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs)) {
            GoldinFilterChip("Semua", selected = true, onClick = {})
            GoldinFilterChip("Selesai", selected = false, onClick = {})
            GoldinChoiceChip("100rb", selected = true, onClick = {})
            GoldinChoiceChip("500rb", selected = false, onClick = {})
        }
        NoticeCard(
            title = "Verifikasi akunmu sekarang",
            message = "Cukup 2 menit untuk mulai beli emas pertamamu.",
            ctaLabel = "Verifikasi sekarang",
            onCtaClick = {},
            isMandatory = true,
            icon = PrototypeIcons.ShieldCheck,
        )
        InfoNote(
            title = "Verifikasi sedang ditinjau",
            message = "Proses verifikasi membutuhkan waktu maksimal 1x24 jam.",
            icon = PrototypeIcons.Clock,
        )
        GoldinRadioCard(
            options = listOf(
                GoldinRadioOption("periode", "Periode", "Nabung selama jangka waktu tertentu"),
                GoldinRadioOption("target", "Target emas", "Nabung sampai target gram tercapai"),
            ),
            selected = "target",
            onSelect = {},
        )
        PairedButtons(leftText = "Sebelumnya", onLeftClick = {}, rightText = "Selanjutnya", onRightClick = {})
        PrimaryButton(text = "Memproses…", onClick = {}, loading = true)
    }
}
