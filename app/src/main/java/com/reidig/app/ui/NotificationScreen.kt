package com.reidig.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reidig.shared.NotificationCategory
import com.reidig.shared.NotificationItem
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Layar `17 Notifikasi` sesuai prototipe Goldin v1.2 (`Main.dc.html` L1760–1788, L4357–4381) & Handoff §13:
 * - Header 44dp: tombol kembali `ChevronLeft`, judul terpusat `"Notifikasi"`, dan tombol `"Tandai dibaca"` bila ada unread.
 * - Tab 2 kolom underline: `Transaksi` · `Pengumuman`.
 * - Plain full-bleed list dengan hairline divider:
 *   - Unread: latar `Primary50`, judul `SemiBold`, tanpa titik/dot.
 *   - Read: latar `Paper`, judul `Medium`.
 *   - Swipe-left hapus: latar merah `Critical` + ikon `Trash` + `"Hapus"`, ambang 96dp → hapus + `GoldinToast("Notifikasi dihapus")`.
 * - Empty state per tab: lingkaran `64dp Primary50` + ikon `Bell 28dp Primary700` + `"Belum ada notifikasi"`.
 */
@Composable
fun NotificationScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onOpenTransaction: (String?) -> Unit = {},
    initialCategory: NotificationCategory = NotificationCategory.TRANSACTION,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    var selectedCategoryName by rememberSaveable { mutableStateOf(initialCategory.name) }
    val selectedCategory = remember(selectedCategoryName) {
        NotificationCategory.entries.firstOrNull { it.name == selectedCategoryName } ?: NotificationCategory.TRANSACTION
    }

    var toastNonce by remember { mutableIntStateOf(0) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val visibleItems = remember(state.notifications, selectedCategory) {
        state.notifications.filter { it.category == selectedCategory }
    }
    val hasUnread = state.unreadNotificationCount > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Header 44dp (`Main.dc.html` L1762)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Xs)
                    .height(GoldinControl.TouchMin),
            ) {
                GoldinIconButton(
                    icon = PrototypeIcons.ChevronLeft,
                    contentDescription = "Kembali",
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-10).dp),
                )
                Text(
                    text = "Notifikasi",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Palette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 52.dp),
                )
                if (hasUnread) {
                    MarkAllReadButton(
                        onClick = { env.repo.markAllNotificationsRead() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 10.dp),
                    )
                }
            }

            // 2. Underline Tab Bar 2 Kolom: Transaksi · Pengumuman (`Main.dc.html` L1763–1767)
            NotificationTabBar(
                selected = selectedCategory,
                onSelect = { next -> selectedCategoryName = next.name },
            )

            // 3. Daftar Notifikasi Full-Bleed atau Empty State
            if (visibleItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    GoldinEmptyState(
                        icon = PrototypeIcons.Bell,
                        title = "Belum ada notifikasi",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = GoldinSpace.Xxl),
                ) {
                    itemsIndexed(
                        items = visibleItems,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        NotificationSwipeRow(
                            item = item,
                            showTopDivider = index > 0,
                            onClick = {
                                env.repo.markNotificationRead(item.id)
                                if (item.txId != null) {
                                    onOpenTransaction(item.txId)
                                }
                            },
                            onDelete = {
                                env.repo.deleteNotification(item.id)
                                toastNonce += 1
                                toastMessage = "Notifikasi dihapus"
                            },
                        )
                    }
                }
            }
        }

        // Toast mengambang bawah (`GoldinToast`)
        key(toastNonce) {
            GoldinToast(
                message = toastMessage,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun MarkAllReadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            isPressed -> Palette.Primary100
            isHovered -> Palette.Primary50
            else -> Color.Transparent
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "markAllBg",
    )
    Box(
        modifier = modifier
            .height(GoldinControl.TouchMin)
            .clip(RoundedCornerShape(GoldinRadius.Full))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Tandai dibaca",
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Palette.Primary700,
        )
    }
}

@Composable
private fun NotificationTabBar(
    selected: NotificationCategory,
    onSelect: (NotificationCategory) -> Unit,
) {
    val tabs = listOf(NotificationCategory.TRANSACTION, NotificationCategory.ANNOUNCEMENT)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = GoldinSpace.Lg, end = GoldinSpace.Lg, top = GoldinSpace.Xxs),
    ) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = GoldinBorder.Default,
            color = Palette.Line,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { category ->
                val isSelected = selected == category
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Palette.Primary700 else Palette.InkSecondary,
                    animationSpec = tween(GoldinMotion.DurationHover),
                    label = "notifTabText_${category.name}",
                )
                val indicatorColor by animateColorAsState(
                    targetValue = if (isSelected) Palette.Primary700 else Color.Transparent,
                    animationSpec = tween(GoldinMotion.DurationHover),
                    label = "notifTabLine_${category.name}",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(GoldinControl.TouchMin)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) { onSelect(category) },
                ) {
                    Text(
                        text = category.label,
                        fontFamily = BodyFontFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(indicatorColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSwipeRow(
    item: NotificationItem,
    showTopDivider: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val deleteThresholdPx = with(density) { 96.dp.toPx() }
    val dismissTargetPx = with(density) { 420.dp.toPx() }

    val offsetX = remember(item.id) { Animatable(0f) }
    var isDeleting by remember(item.id) { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val rowBg by animateColorAsState(
        targetValue = when {
            isPressed || isHovered -> Palette.Neutral200
            !item.isRead -> Palette.Primary50
            else -> Palette.Paper
        },
        animationSpec = tween(GoldinMotion.DurationHover),
        label = "notifRowBg_${item.id}",
    )

    val formattedTime = remember(item.atMs) { formatNotificationTimestamp(item.atMs) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        // Lapisan aksi hapus merah di balik baris (muncul saat digeser ke kiri)
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(if (offsetX.value < 0f) 1f else 0f)
                .background(Palette.Critical)
                .padding(end = GoldinSpace.Lg),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = PrototypeIcons.Trash,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(GoldinControl.IconMd),
                )
                Text(
                    text = "Hapus",
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.White,
                )
            }
        }

        // Lapisan konten baris notifikasi (full-bleed, tanpa dot unread)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(rowBg)
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            if (isDeleting) return@detectHorizontalDragGestures
                            val next = (offsetX.value + dragAmount).coerceAtMost(0f)
                            if (next < 0f || offsetX.value < 0f) {
                                change.consume()
                                scope.launch { offsetX.snapTo(next) }
                            }
                        },
                        onDragEnd = {
                            if (isDeleting) return@detectHorizontalDragGestures
                            if (offsetX.value < -deleteThresholdPx) {
                                isDeleting = true
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = -dismissTargetPx,
                                        animationSpec = tween(200, easing = GoldinMotion.EaseOutSheet),
                                    )
                                    onDelete()
                                }
                            } else {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(200, easing = GoldinMotion.EaseOutSheet),
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            if (!isDeleting) {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(200, easing = GoldinMotion.EaseOutSheet),
                                    )
                                }
                            }
                        },
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    enabled = !isDeleting,
                    onClick = {
                        if (!isDeleting && offsetX.value == 0f) {
                            onClick()
                        }
                    },
                ),
        ) {
            if (showTopDivider) {
                HorizontalDivider(
                    thickness = GoldinBorder.Hairline,
                    color = Palette.Line,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GoldinSpace.Lg, vertical = GoldinSpace.Md),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GoldinSpace.Xs),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.title,
                        fontFamily = BodyFontFamily,
                        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Palette.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formattedTime,
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = Palette.InkSoft,
                        maxLines = 1,
                    )
                }
                Text(
                    text = item.message,
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.InkSecondary,
                )
            }
        }
    }
}

private val NOTIF_MONTHS_SHORT = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/**
 * Format waktu notifikasi sesuai `Main.dc.html` L4368 & Handoff §13:
 * - Hari yang sama → `HH.MM` (mis. `14.30`)
 * - Hari sebelumnya → `dd MMM yyyy` (mis. `16 Nov 2026`)
 */
private fun formatNotificationTimestamp(atMs: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = atMs }
    val today = Calendar.getInstance()
    val isSameDay =
        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    return if (isSameDay) {
        val hh = target.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val mm = target.get(Calendar.MINUTE).toString().padStart(2, '0')
        "$hh.$mm"
    } else {
        val dd = target.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val mon = NOTIF_MONTHS_SHORT[target.get(Calendar.MONTH)]
        val yyyy = target.get(Calendar.YEAR)
        "$dd $mon $yyyy"
    }
}

@ScreenPreview
@Composable
private fun NotificationTransaksiPreview() = GoldinTheme {
    NotificationScreen(
        env = rememberPreviewEnv(verified = true),
        onBack = {},
        initialCategory = NotificationCategory.TRANSACTION,
    )
}

@ScreenPreview
@Composable
private fun NotificationPengumumanPreview() = GoldinTheme {
    NotificationScreen(
        env = rememberPreviewEnv(verified = true),
        onBack = {},
        initialCategory = NotificationCategory.ANNOUNCEMENT,
    )
}

@ScreenPreview
@Composable
private fun NotificationEmptyPreview() = GoldinTheme {
    NotificationScreen(
        env = rememberPreviewEnv(verified = false),
        onBack = {},
        initialCategory = NotificationCategory.TRANSACTION,
    )
}
