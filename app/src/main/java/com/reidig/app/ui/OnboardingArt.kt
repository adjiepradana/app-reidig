package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.app.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Adapter backward-compatible yang mendelegasikan ke [GoldinIcons]. */
object PrototypeIcons {
    val GoldIngots: ImageVector get() = GoldinIcons.GoldIngots
    val Cart: ImageVector get() = GoldinIcons.Cart
    val PriceTag: ImageVector get() = GoldinIcons.PriceTag
    val CashBanknote: ImageVector get() = GoldinIcons.CashBanknote
    val Home: ImageVector get() = GoldinIcons.Home
    val Wallet: ImageVector get() = GoldinIcons.Wallet
    val ArrowsExchange: ImageVector get() = GoldinIcons.ArrowsExchange
    val User: ImageVector get() = GoldinIcons.User
    val Bell: ImageVector get() = GoldinIcons.Bell
    val IdBadge: ImageVector get() = GoldinIcons.IdBadge
    val FaceId: ImageVector get() = GoldinIcons.FaceId
    val HistoryClock: ImageVector get() = GoldinIcons.HistoryClock
    val ArrowDownLeft: ImageVector get() = GoldinIcons.ArrowDownLeft
    val ArrowUpRight: ImageVector get() = GoldinIcons.ArrowUpRight
    val ShieldCheck: ImageVector get() = GoldinIcons.ShieldCheck
    val Mail: ImageVector get() = GoldinIcons.Mail
    val Lock: ImageVector get() = GoldinIcons.Lock
    val Eye: ImageVector get() = GoldinIcons.Eye
    val EyeOff: ImageVector get() = GoldinIcons.EyeOff
    val Calendar: ImageVector get() = GoldinIcons.Calendar
    val Refresh: ImageVector get() = GoldinIcons.Refresh
    val ChevronLeft: ImageVector get() = GoldinIcons.ChevronLeft
    val ChevronRight: ImageVector get() = GoldinIcons.ChevronRight
    val ChevronDown: ImageVector get() = GoldinIcons.ChevronDown
    val Check: ImageVector get() = GoldinIcons.Check
    val ArrowsUpDown: ImageVector get() = GoldinIcons.ArrowsUpDown
    val SmsMessage: ImageVector get() = GoldinIcons.SmsMessage
    val PieChart: ImageVector get() = GoldinIcons.PieChart
    val InfoCircle: ImageVector get() = GoldinIcons.InfoCircle
    val AlertCircle: ImageVector get() = GoldinIcons.AlertCircle
    val XClose: ImageVector get() = GoldinIcons.XClose
    val BuildingBank: ImageVector get() = GoldinIcons.BuildingBank
    val Trash: ImageVector get() = GoldinIcons.Trash
    val Copy: ImageVector get() = GoldinIcons.Copy
    val Diamond: ImageVector get() = GoldinIcons.Diamond
    val PackageBox: ImageVector get() = GoldinIcons.PackageBox
    val Backspace: ImageVector get() = GoldinIcons.Backspace
    val Fingerprint: ImageVector get() = GoldinIcons.Fingerprint
    val Loader2: ImageVector get() = GoldinIcons.Loader2
    val CircleCheckFilled: ImageVector get() = GoldinIcons.CircleCheckFilled
    val CircleXFilled: ImageVector get() = GoldinIcons.CircleXFilled
    val Clock: ImageVector get() = GoldinIcons.Clock
    val QrCode: ImageVector get() = GoldinIcons.QrCode
    val ArrowRight: ImageVector get() = GoldinIcons.ArrowRight
    val Plus: ImageVector get() = GoldinIcons.Plus
    val Repeat: ImageVector get() = GoldinIcons.Repeat
    val FilterSliders: ImageVector get() = GoldinIcons.FilterSliders
    val Download: ImageVector get() = GoldinIcons.Download
    val Receipt: ImageVector get() = GoldinIcons.Receipt
    val ReceiptMoney: ImageVector get() = GoldinIcons.ReceiptMoney
    val MapPin: ImageVector get() = GoldinIcons.MapPin
    val Search: ImageVector get() = GoldinIcons.Search
    val BuildingStore: ImageVector get() = GoldinIcons.BuildingStore
    val TruckDelivery: ImageVector get() = GoldinIcons.TruckDelivery
    val Logout: ImageVector get() = GoldinIcons.Logout
    val AtSign: ImageVector get() = GoldinIcons.AtSign
    val Printer: ImageVector get() = GoldinIcons.Printer
    val Coin: ImageVector get() = GoldinIcons.Coin
    val ShieldLock: ImageVector get() = GoldinIcons.ShieldLock
    val LockCheck: ImageVector get() = GoldinIcons.LockCheck
    val CircleOutline: ImageVector get() = GoldinIcons.CircleOutline
    val CaretUpFilled: ImageVector get() = GoldinIcons.CaretUpFilled
    val CaretDownFilled: ImageVector get() = GoldinIcons.CaretDownFilled
}

/** Latar gelap dengan cahaya ungu di tengah. */
val GlowGradient = Brush.radialGradient(
    listOf(Color(0xFF5436AA), Color.Transparent),
    radius = 900f,
)

/** Ilustrasi 3D emas batangan dari aset PNG prototipe Goldin v1 dengan animasi melayang halus. */
@Composable
fun GoldBarArt(height: Dp = 280.dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "float")
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse),
        label = "floatY",
    )
    Image(
        painter = painterResource(R.drawable.img_gold_bars_hero),
        contentDescription = "Ilustrasi emas batangan",
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height).offset(y = offsetY.dp),
    )
}

/** Lencana 3 trapesium emas di dalam kotak ungu sesuai header 07 Beranda & 20 Dompet prototipe. */
@Composable
fun GoldBarsBadge(size: Dp = 40.dp, cornerRadius: Dp = 12.dp, iconSize: Dp = 24.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(cornerRadius)).background(Palette.Primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            PrototypeIcons.GoldIngots,
            contentDescription = null,
            tint = Palette.Gold,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Taburan konfeti statis untuk layar selesai verifikasi. */
fun DrawScope.drawConfetti() {
    val colors = listOf(Palette.Gold, Palette.Primary300)
    repeat(26) { index ->
        val angle = index * 2.4f
        val distance = 0.28f + (index % 7) * 0.1f
        val x = size.width * (0.5f + distance * cos(angle) * 0.75f)
        val y = size.height * (0.32f + distance * sin(angle) * 0.42f)
        val piece = 7.dp.toPx()
        rotateAround(angle * 180f / PI.toFloat(), Offset(x, y)) {
            drawRoundRect(
                colors[index % colors.size].copy(alpha = 0.75f),
                Offset(x, y),
                Size(piece * 1.5f, piece),
                CornerRadius(2.dp.toPx()),
            )
        }
    }
}

private inline fun DrawScope.rotateAround(degrees: Float, pivot: Offset, block: DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.transform.rotate(degrees, pivot)
    block()
    drawContext.canvas.restore()
}

/** Tombol masuk/daftar dengan akun Google sesuai gaya outlined button prototipe. */
@Composable
fun GoogleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "googleScale",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "googleBg",
    )

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Palette.Primary700),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = Palette.Primary700,
        ),
    ) {
        Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Palette.Primary700)
    }
}

/** Bar hijau penanda email sudah terverifikasi. */
@Composable
fun VerifiedBanner(text: AnnotatedString) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.GoodSoft)
            .border(1.dp, Palette.Good.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(Palette.Good), contentAlignment = Alignment.Center) {
            Icon(PrototypeIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = BodyFontFamily, color = Palette.Good700, fontSize = 13.5.sp)
    }
}

@ScreenPreview
@Composable
private fun GoldBarPreview() = GoldinTheme {
    Box(Modifier.background(Palette.Paper).padding(24.dp)) { GoldBarArt(260.dp) }
}
