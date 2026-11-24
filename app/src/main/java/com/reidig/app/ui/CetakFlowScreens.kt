package com.reidig.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import com.reidig.shared.GoldBranch
import com.reidig.shared.GoldBrand
import com.reidig.shared.MockRepository
import com.reidig.shared.PaymentMethod
import com.reidig.shared.PhysicalPrintOrder
import com.reidig.shared.PrintMethod
import com.reidig.shared.SavedAddress
import com.reidig.shared.formatGrams4
import com.reidig.shared.rupiah

/**
 * Data holder untuk alur multi-step transaksi Cetak Emas Fisik (Screens 49 - 52).
 */
class CetakFlowDraft {
    var brand by mutableStateOf<GoldBrand>(GoldBrand.LM_REI)
    var sizeGrams by mutableStateOf<Double?>(null)
    var method by mutableStateOf(PrintMethod.BRANCH)
    var branchId by mutableStateOf<String?>("br_karawaci")
    var deliveryAddress by mutableStateOf<String?>(null)
    var recipientName by mutableStateOf<String?>(null)
    var recipientPhone by mutableStateOf<String?>(null)
    var paymentMethod by mutableStateOf<PaymentMethod>(PaymentMethod.WALLET)
    var orderId by mutableStateOf("")

    fun reset(prefillBranchId: String? = null) {
        brand = GoldBrand.LM_REI
        sizeGrams = null
        method = PrintMethod.BRANCH
        branchId = prefillBranchId ?: "br_karawaci"
        deliveryAddress = null
        recipientName = null
        recipientPhone = null
        paymentMethod = PaymentMethod.WALLET
        orderId = ""
    }
}

// 8 Pecahan keping standar cetak fisik (Handoff §12 L120 & Main.dc.html CT_SIZES)
val CETAK_GOLD_SIZES = listOf(1.0, 2.0, 3.0, 5.0, 10.0, 25.0, 50.0, 100.0)

/**
 * SCREEN 49: STEPPER 1 - PILIH BRAND & UKURAN KEPING (`31 Cetak · Pilih brand & ukuran` / `ctBrand`)
 *
 * Mengimplementasikan:
 * - Stepper bar 3-langkah (1. Brand & ukuran, 2. Metode, 3. Konfirmasi)
 * - Kartu ringkasan saldo emas bebas cetak (Free Gold Grams)
 * - Accordion 3 brand resmi (LM REI, Antam, Galeri 24) dengan animasi rotasi panah
 * - Grid pecahan keping 4 kolom (1, 2, 3, 5, 10, 25, 50, 100 gr)
 * - Validasi saldo emas bebas ("Saldo kurang") & ketersediaan stok ("Habis")
 * - Footer 2 tombol (Batalkan & Selanjutnya)
 * - Modal konfirmasi keluar `GoldinModal` ("Yakin ingin keluar dari cetak emas?")
 */
@Composable
fun CetakBrandSizeScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onNext: (brand: GoldBrand, sizeGrams: Double) -> Unit,
    initialBrand: GoldBrand = GoldBrand.LM_REI,
    initialSize: Double? = null,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val freeGoldGrams = state.freeGoldGrams

    var selectedBrand by rememberSaveable { mutableStateOf(initialBrand) }
    var selectedSize by rememberSaveable { mutableStateOf(initialSize) }
    var openBrand by rememberSaveable { mutableStateOf<GoldBrand?>(initialBrand) }
    var showExitModal by remember { mutableStateOf(false) }

    // Cegah keluar tanpa konfirmasi
    BackHandler { showExitModal = true }

    val isStepValid = selectedSize != null && freeGoldGrams >= (selectedSize ?: 0.0) &&
            env.repo.isStockAvailable(selectedBrand, selectedSize ?: 0.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // 1. Top Bar Header
        TopTitleBar(
            title = "Pilih brand & ukuran",
            onBack = { showExitModal = true },
        )

        // 2. Stepper 3 Langkah
        LabeledStepBar(
            current = 1,
            labels = listOf("Brand & ukuran", "Metode", "Konfirmasi"),
        )

        Spacer(Modifier.height(8.dp))

        // 3. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // A. Kartu Saldo Emas Bebas
            item(key = "free_gold_card") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Saldo emas",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.InkSecondary,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatGrams4(freeGoldGrams).replace(" gr", ""),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Palette.Ink,
                        )
                        Text(
                            text = " gr",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Palette.Ink,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }
                }
            }

            // B. Kartu Accordion 3 Brand Emas
            item(key = "brands_accordion") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Lg))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                ) {
                    val brands = listOf(GoldBrand.LM_REI, GoldBrand.ANTAM, GoldBrand.GALERI_24)

                    brands.forEachIndexed { index, brand ->
                        if (index > 0) {
                            HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)
                        }

                        val isOpen = openBrand == brand
                        val isBrandSelected = selectedBrand == brand && selectedSize != null
                        val arrowRotation by animateFloatAsState(
                            targetValue = if (isOpen) 180f else 0f,
                            animationSpec = tween(durationMillis = 200),
                            label = "accordion_arrow_${brand.name}",
                        )

                        // Accordion Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    openBrand = if (isOpen) null else brand
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Circular Gold Ingot Badge
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Palette.Gold100),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = GoldinIcons.GoldBars,
                                    contentDescription = null,
                                    tint = Palette.GoldDeep,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            // Brand Information
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = brand.displayName,
                                    fontFamily = BodyFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Palette.Ink,
                                )

                                if (isBrandSelected) {
                                    val sizeLabel = if ((selectedSize ?: 0.0) % 1.0 == 0.0) {
                                        "${(selectedSize ?: 0.0).toInt()} gr"
                                    } else {
                                        "${selectedSize} gr"
                                    }
                                    Text(
                                        text = "$sizeLabel terpilih",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.Primary700,
                                        fontWeight = FontWeight.Medium,
                                    )
                                } else {
                                    Text(
                                        text = when (brand) {
                                            GoldBrand.LM_REI -> "Emas murni 99.99% standar LBMA"
                                            GoldBrand.ANTAM -> "CertiCard terintegrasi resmi Antam"
                                            GoldBrand.GALERI_24 -> "Kemasan berteknologi pengaman"
                                        },
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.InkSoft,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            // Chevron indicator
                            Icon(
                                imageVector = GoldinIcons.ChevronDown,
                                contentDescription = if (isOpen) "Tutup pilihan ukuran" else "Buka pilihan ukuran",
                                tint = Palette.InkSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation),
                            )
                        }

                        // Accordion Expandable Grid of Sizes
                        AnimatedVisibility(
                            visible = isOpen,
                            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Grid 4 kolom x 2 baris
                                val rows = CETAK_GOLD_SIZES.chunked(4)
                                rows.forEach { rowSizes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowSizes.forEach { size ->
                                            val inStock = env.repo.isStockAvailable(brand, size)
                                            val hasEnoughGold = freeGoldGrams >= size
                                            val isSelected = selectedBrand == brand && selectedSize == size
                                            val isEnabled = inStock && hasEnoughGold

                                            CetakSizeChip(
                                                sizeGrams = size,
                                                isSelected = isSelected,
                                                isEnabled = isEnabled,
                                                inStock = inStock,
                                                hasEnoughGold = hasEnoughGold,
                                                onClick = {
                                                    selectedBrand = brand
                                                    selectedSize = size
                                                },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Bottom Action Bar (Batalkan vs Selanjutnya)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 0.5.dp, color = Palette.Line)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tombol Batalkan (Outline)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                        .clickable { showExitModal = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Batalkan",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Primary700,
                    )
                }

                // Tombol Selanjutnya (Primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(if (isStepValid) Palette.Primary700 else Palette.Neutral300)
                        .clickable(enabled = isStepValid) {
                            val size = selectedSize
                            if (size != null) {
                                onNext(selectedBrand, size)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Selanjutnya",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isStepValid) Palette.Primary50 else Palette.InkSoft,
                    )
                }
            }
        }
    }

    // Modal Konfirmasi Keluar Alur Cetak Emas (`ctexit`)
    if (showExitModal) {
        GoldinModal(
            title = "Yakin ingin keluar dari cetak emas?",
            message = "Pilihan yang sudah kamu buat akan hilang.",
            confirmLabel = "Keluar",
            isDestructive = true,
            onConfirm = {
                showExitModal = false
                onBack()
            },
            cancelLabel = "Lanjut cetak",
            onCancel = { showExitModal = false },
        )
    }
}

/**
 * Chip pecahan keping emas pada Stepper 1.
 * Mengikuti spesifikasi Handoff §12 L120 & Main.dc.html L2598.
 */
@Composable
private fun CetakSizeChip(
    sizeGrams: Double,
    isSelected: Boolean,
    isEnabled: Boolean,
    inStock: Boolean,
    hasEnoughGold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizeText = if (sizeGrams % 1.0 == 0.0) sizeGrams.toInt().toString() else sizeGrams.toString()

    val style = when {
        !inStock -> CetakChipStyle(
            bg = Palette.Neutral100,
            border = Palette.Line,
            text = Palette.InkSoft,
            note = "Habis",
        )
        !hasEnoughGold -> CetakChipStyle(
            bg = Palette.Neutral100,
            border = Palette.Line,
            text = Palette.InkSoft,
            note = "Saldo kurang",
        )
        isSelected -> CetakChipStyle(
            bg = Palette.Primary50,
            border = Palette.Primary700,
            text = Palette.Primary700,
            note = null,
        )
        else -> CetakChipStyle(
            bg = Color.White,
            border = Palette.Line,
            text = Palette.Ink,
            note = null,
        )
    }

    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val alpha = if (isEnabled) 1.0f else 0.55f

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(GoldinRadius.Md))
            .background(style.bg)
            .border(borderWidth, style.border, RoundedCornerShape(GoldinRadius.Md))
            .alpha(alpha)
            .clickable(enabled = isEnabled, onClick = onClick)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = "$sizeText gram${if (!inStock) ", stok habis" else if (!hasEnoughGold) ", saldo emas kurang" else ""}"
            }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = sizeText,
                    fontFamily = MonoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = style.text,
                )
                Text(
                    text = " gr",
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = style.text,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }

            if (style.note != null) {
                Text(
                    text = style.note,
                    fontFamily = BodyFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Palette.InkSoft,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Styling helper untuk chip pecahan keping emas.
 */
private data class CetakChipStyle(
    val bg: Color,
    val border: Color,
    val text: Color,
    val note: String?,
)

/**
 * SCREEN 50: STEPPER 2 - METODE PENGAMBILAN / PENGIRIMAN (`33 Cetak · Pilih metode` / `ctMethod`)
 *
 * Mengimplementasikan:
 * - Stepper bar 3-langkah (Langkah 2: Metode Aktif)
 * - Toggle Side-by-Side:
 *   1. "Ambil di cabang" (Raja Emas · Rp0, ikon BuildingStore)
 *   2. "Dikirim ke alamat" (Kurir berasuransi · Rp35.000, ikon Truck)
 * - Mode Ambil di Cabang:
 *   - Radio list cabang (3 cabang terdekat + cabang pilihan katalog jika ada)
 *   - Tombol "+ Pilih cabang lain" (membuka katalog 8 cabang Raja Emas `ctBrAll`)
 * - Mode Dikirim ke Alamat:
 *   - Banner edukasi asuransi kehilangan 100% (Info50 / ShieldCheck)
 *   - Radio list alamat pengiriman tersimpan dengan pill hijau "Utama"
 *   - Tombol "+ Tambah alamat baru" yang memicu drawer input alamat cepat
 *   - Empty state informatif jika belum ada alamat tersimpan
 * - Footer 2 tombol (Sebelumnya & Selanjutnya)
 */
@Composable
fun CetakMethodScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onNext: (method: PrintMethod, branchId: String?, addressId: String?) -> Unit,
    onOpenAllBranches: () -> Unit,
    initialMethod: PrintMethod = PrintMethod.BRANCH,
    initialBranchId: String? = null,
    initialAddressId: String? = null,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val allBranches = remember { env.repo.getGoldBranches() }
    val savedAddresses = state.savedAddresses

    var selectedMethod by rememberSaveable { mutableStateOf(initialMethod) }
    var selectedBranchId by rememberSaveable { mutableStateOf(initialBranchId ?: "br_karawaci") }
    var selectedAddressId by rememberSaveable { mutableStateOf(initialAddressId) }
    var showAddAddressSheet by remember { mutableStateOf(false) }

    // Auto-select alamat utama jika belum ada yang terpilih
    LaunchedEffect(savedAddresses) {
        if (selectedAddressId == null && savedAddresses.isNotEmpty()) {
            selectedAddressId = savedAddresses.firstOrNull { it.isPrimary }?.id ?: savedAddresses.first().id
        }
    }

    // Cabang yang ditampilkan: top 3 terdekat + cabang yang terpilih jika di luar top 3
    val displayBranches = remember(allBranches, selectedBranchId) {
        val nearby = allBranches.sortedBy { it.distanceKm }.take(3).toMutableList()
        val currentChosen = allBranches.firstOrNull { it.id == selectedBranchId }
        if (currentChosen != null && nearby.none { it.id == currentChosen.id }) {
            nearby.add(currentChosen)
        }
        nearby
    }

    val isStepValid = when (selectedMethod) {
        PrintMethod.BRANCH -> allBranches.any { it.id == selectedBranchId }
        PrintMethod.DELIVERY, PrintMethod.JNE -> savedAddresses.any { it.id == selectedAddressId }
    }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // 1. Top Bar Header
        TopTitleBar(
            title = "Pilih metode",
            onBack = onBack,
        )

        // 2. Stepper 3 Langkah
        LabeledStepBar(
            current = 2,
            labels = listOf("Brand & ukuran", "Metode", "Konfirmasi"),
        )

        Spacer(Modifier.height(8.dp))

        // 3. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // A. Mode Toggle Side-by-Side (Ambil di Cabang vs Dikirim ke Alamat)
            item(key = "method_toggle") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val isBranch = selectedMethod == PrintMethod.BRANCH
                    val isDelivery = selectedMethod == PrintMethod.DELIVERY || selectedMethod == PrintMethod.JNE

                    // Card Ambil di Cabang
                    CetakMethodCard(
                        title = "Ambil di cabang",
                        subtitle = "Raja Emas · Rp0",
                        icon = GoldinIcons.BuildingStore,
                        isSelected = isBranch,
                        onClick = { selectedMethod = PrintMethod.BRANCH },
                        modifier = Modifier.weight(1f),
                    )

                    // Card Dikirim ke Alamat
                    CetakMethodCard(
                        title = "Dikirim ke alamat",
                        subtitle = "Kurir berasuransi · Rp35.000",
                        icon = GoldinIcons.Truck,
                        isSelected = isDelivery,
                        onClick = {
                            selectedMethod = PrintMethod.DELIVERY
                            if (selectedAddressId == null && savedAddresses.isNotEmpty()) {
                                selectedAddressId = savedAddresses.firstOrNull { it.isPrimary }?.id ?: savedAddresses.first().id
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // B. Konten Bersyarat: Mode Ambil di Cabang
            if (selectedMethod == PrintMethod.BRANCH) {
                item(key = "section_branch_title") {
                    Text(
                        text = "Pilih cabang",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                item(key = "branch_radio_list") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Color.White)
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                    ) {
                        displayBranches.forEachIndexed { index, branch ->
                            if (index > 0) {
                                HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)
                            }

                            val isSelected = selectedBranchId == branch.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBranchId = branch.id }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Pin Icon Circular Badge
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Palette.Primary100),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = GoldinIcons.MapPin,
                                        contentDescription = null,
                                        tint = Palette.Primary700,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }

                                // Branch Meta
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = branch.name,
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Palette.Ink,
                                    )
                                    Text(
                                        text = branch.address,
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.InkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${branch.distanceKm} km · Buka ${branch.openingHours} WIB",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.InkSoft,
                                    )
                                }

                                // Radio Button Indicator
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Palette.Primary700 else Palette.Neutral300,
                                            shape = CircleShape,
                                        ),
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
                        }
                    }
                }

                item(key = "branch_select_other_button") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAllBranches() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = GoldinIcons.Plus,
                            contentDescription = null,
                            tint = Palette.Primary700,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Pilih cabang lain",
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Palette.Primary700,
                        )
                    }
                }
            }

            // C. Konten Bersyarat: Mode Dikirim ke Alamat
            if (selectedMethod == PrintMethod.DELIVERY || selectedMethod == PrintMethod.JNE) {
                // Info Asuransi Kehilangan 100%
                item(key = "delivery_insurance_banner") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .background(Palette.Info50)
                            .border(1.dp, Palette.Info100, RoundedCornerShape(GoldinRadius.Lg))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Palette.Info100),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = GoldinIcons.ShieldCheck,
                                contentDescription = null,
                                tint = Palette.Info700,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = "Pengiriman dilindungi asuransi kehilangan 100% hingga sampai di tanganmu.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.Info700,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item(key = "section_address_title") {
                    Text(
                        text = "Alamat pengiriman",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (savedAddresses.isNotEmpty()) {
                    items(
                        count = savedAddresses.size,
                        key = { idx -> "saved_addr_${savedAddresses[idx].id}" },
                    ) { idx ->
                        val addr = savedAddresses[idx]
                        val isSelected = selectedAddressId == addr.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GoldinRadius.Xl))
                                .background(if (isSelected) Palette.Primary50 else Color.White)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Palette.Primary700 else Palette.Line,
                                    shape = RoundedCornerShape(GoldinRadius.Xl),
                                )
                                .clickable { selectedAddressId = addr.id }
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = addr.label,
                                            fontFamily = BodyFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Palette.Ink,
                                        )

                                        if (addr.isPrimary) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                                    .background(Palette.Primary100)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    text = "Utama",
                                                    fontFamily = BodyFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = Palette.Primary700,
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = "${addr.recipientName} · ${addr.phoneNumber}",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.InkSecondary,
                                    )

                                    Text(
                                        text = "${addr.details}, ${addr.subDistrict}, ${addr.district}, ${addr.city}, ${addr.province} ${addr.postalCode}",
                                        fontFamily = BodyFontFamily,
                                        fontSize = 12.sp,
                                        color = Palette.InkSecondary,
                                        lineHeight = 18.sp,
                                    )
                                }

                                // Radio Button Indicator
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Palette.Primary700 else Palette.Neutral300,
                                            shape = CircleShape,
                                        ),
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
                        }
                    }

                    item(key = "add_address_button") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddAddressSheet = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = GoldinIcons.Plus,
                                contentDescription = null,
                                tint = Palette.Primary700,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Tambah alamat baru",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Palette.Primary700,
                            )
                        }
                    }
                } else {
                    // Empty state alamat pengiriman
                    item(key = "empty_address_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GoldinRadius.Xl))
                                .background(Color.White)
                                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                                .padding(24.dp),
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
                                    imageVector = GoldinIcons.MapPin,
                                    contentDescription = null,
                                    tint = Palette.Primary700,
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            Text(
                                text = "Belum ada alamat. Tambahkan alamat untuk pengiriman emas.",
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.InkSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(GoldinRadius.Full))
                                    .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                                    .clickable { showAddAddressSheet = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = GoldinIcons.Plus,
                                        contentDescription = null,
                                        tint = Palette.Primary700,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Tambah alamat",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Palette.Primary700,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Bottom Action Bar (Sebelumnya vs Selanjutnya)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 0.5.dp, color = Palette.Line)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tombol Sebelumnya (Outline)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sebelumnya",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Primary700,
                    )
                }

                // Tombol Selanjutnya (Primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(if (isStepValid) Palette.Primary700 else Palette.Neutral300)
                        .clickable(enabled = isStepValid) {
                            if (isStepValid) {
                                onNext(selectedMethod, selectedBranchId, selectedAddressId)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Selanjutnya",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isStepValid) Palette.Primary50 else Palette.InkSoft,
                    )
                }
            }
        }
    }

    // 5. Drawer / Bottom Sheet Tambah Alamat Pengiriman Cepat
    if (showAddAddressSheet) {
        CetakAddAddressSheet(
            onDismiss = { showAddAddressSheet = false },
            onSave = { newAddress ->
                env.repo.addSavedAddress(newAddress)
                selectedAddressId = newAddress.id
                showAddAddressSheet = false
            },
        )
    }
}

/**
 * Kartu pilihan metode (Ambil di cabang vs Dikirim ke alamat).
 */
@Composable
private fun CetakMethodCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(136.dp)
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(if (isSelected) Palette.Primary50 else Color.White)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Palette.Primary700 else Palette.Line,
                shape = RoundedCornerShape(GoldinRadius.Xl),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Palette.Primary100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Palette.Primary700,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Palette.Ink,
                )
                Text(
                    text = subtitle,
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    color = Palette.InkSecondary,
                )
            }
        }
    }
}

/**
 * Bottom Sheet Drawer Tambah Alamat Pengiriman Cepat.
 */
@Composable
fun CetakAddAddressSheet(
    onDismiss: () -> Unit,
    onSave: (SavedAddress) -> Unit,
) {
    var label by rememberSaveable { mutableStateOf("Rumah") }
    var recipientName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var streetDetails by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("Tangerang") }
    var postalCode by rememberSaveable { mutableStateOf("") }

    val isFormValid = recipientName.isNotBlank() && phone.length >= 10 && streetDetails.isNotBlank()

    GoldinBottomSheet(
        onDismiss = onDismiss,
        title = "Tambah alamat pengiriman",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormField(
                label = "Label alamat",
                value = label,
                onValueChange = { label = it },
                placeholder = "Contoh: Rumah, Kantor",
            )

            FormField(
                label = "Nama penerima",
                value = recipientName,
                onValueChange = { recipientName = it },
                placeholder = "Nama lengkap penerima",
            )

            FormField(
                label = "Nomor handphone",
                value = phone,
                onValueChange = { phone = it.filter { char -> char.isDigit() }.take(14) },
                placeholder = "Contoh: 081234567890",
            )

            FormField(
                label = "Alamat lengkap",
                value = streetDetails,
                onValueChange = { streetDetails = it },
                placeholder = "Nama jalan, nomor rumah, RT/RW",
            )

            FormField(
                label = "Kota / Kabupaten",
                value = city,
                onValueChange = { city = it },
                placeholder = "Kota penerima",
            )

            FormField(
                label = "Kode pos",
                value = postalCode,
                onValueChange = { postalCode = it.filter { char -> char.isDigit() }.take(5) },
                placeholder = "5 digit kode pos",
            )

            Spacer(Modifier.height(8.dp))

            PrimaryButton(
                text = "Simpan & gunakan alamat",
                enabled = isFormValid,
                onClick = {
                    val newAddr = SavedAddress(
                        id = "addr-${System.currentTimeMillis()}",
                        label = label.ifBlank { "Alamat" },
                        recipientName = recipientName.trim(),
                        phoneNumber = phone.trim(),
                        province = "Banten",
                        city = city.trim(),
                        district = "Kelapa Dua",
                        subDistrict = "Bencongan",
                        postalCode = postalCode.ifBlank { "15810" },
                        details = streetDetails.trim(),
                        isPrimary = false,
                    )
                    onSave(newAddr)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * SCREEN 52: STEPPER 3 - KONFIRMASI PESANAN CETAK (`36 Cetak · Konfirmasi` / `ctReview` / `cetak/confirm`)
 *
 * Mengimplementasikan:
 * - Stepper bar 3-langkah (Langkah 3: Konfirmasi Aktif)
 * - Kartu Rincian Detail Cetak & Biaya Transaksi:
 *   - Brand, Ukuran (gr), Metode penyerahan, Lokasi (Cabang / Alamat), dan Sisa saldo emas bebas
 *   - Biaya cetak (Rp50.000) + Ongkir & asuransi (Rp0 jika ambil cabang / Rp35.000 jika kirim)
 *   - Total pembayaran Rupiah (Rp50.000 atau Rp85.000)
 * - Pilihan Metode Pembayaran Rupiah (Card ber-divider):
 *   1. Saldo Kas (menampilkan saldo aktif, e.g. Rp1.250.000, atau peringatan merah jika kurang)
 *   2. Virtual Account (BCA, BRI, Mandiri, BNI)
 *   3. QRIS (ShopeePay, GoPay, OVO, Dana)
 * - Footer 2 tombol (Sebelumnya & Selesai)
 * - Modal Konfirmasi Pesanan (`GoldinModal` · `ctconfirm`):
 *   - Judul: "Konfirmasi pesanan"
 *   - Pesan: "Pesanan tidak bisa dibatalkan dan [cabang/alamat] tidak bisa diganti setelah dikonfirmasi."
 *   - Checkbox persetujuan: "[✓] Saya mengerti dan setuju bahwa saldo emas saya akan langsung dipotong sebesar [X,0000] gr untuk mencetak keping fisik ini."
 *   - Tombol "Cetak emas" aktif hanya jika checkbox dicentang
 * - PIN Dialog (`PinDialog`):
 *   - 6 digit, NumberPad in-app tanpa soft keyboard HP
 *   - Eksekusi `repo.createPhysicalPrintOrder`
 *   - Pengurangan saldo emas & saldo kas secara atomik
 *   - Navigasi ke Layar Tiket Pesanan Cetak (`ctTicket`)
 */
@Composable
fun CetakReviewScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onSuccess: (orderId: String) -> Unit,
    brand: GoldBrand,
    sizeGrams: Double,
    method: PrintMethod,
    branchId: String?,
    addressId: String?,
    initialPaymentMethod: PaymentMethod = PaymentMethod.WALLET,
    modifier: Modifier = Modifier,
) {
    val state by env.repo.state.collectAsStateWithLifecycle()
    val freeGoldGrams = state.freeGoldGrams
    val walletBalance = state.walletBalance

    val isPickup = method == PrintMethod.BRANCH
    val branch = remember(branchId) { if (isPickup && branchId != null) env.repo.getGoldBranch(branchId) else null }
    val address = remember(addressId, state.savedAddresses) {
        if (!isPickup) state.savedAddresses.firstOrNull { it.id == addressId } else null
    }

    val printFee = 50_000L
    val shippingFee = if (isPickup) 0L else 35_000L
    val totalPayment = printFee + shippingFee

    val remainingGold = (freeGoldGrams - sizeGrams).coerceAtLeast(0.0)

    var selectedPm by rememberSaveable { mutableStateOf(initialPaymentMethod) }
    var showConfirmModal by remember { mutableStateOf(false) }
    var isAgreed by rememberSaveable { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    val isKasShort = selectedPm == PaymentMethod.WALLET && walletBalance < totalPayment
    val canFinish = !isKasShort

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding(),
    ) {
        // 1. Top Bar Header
        TopTitleBar(
            title = "Konfirmasi cetak",
            onBack = onBack,
        )

        // 2. Stepper 3 Langkah
        LabeledStepBar(
            current = 3,
            labels = listOf("Brand & ukuran", "Metode", "Konfirmasi"),
        )

        Spacer(Modifier.height(8.dp))

        // 3. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // A. Card Rincian Detail Cetak & Biaya Rupiah
            item(key = "review_details_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Detail cetak",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Palette.Ink,
                    )

                    // Detail rows
                    ReviewDetailRow(label = "Brand", value = brand.displayName)
                    ReviewDetailRow(
                        label = "Ukuran",
                        value = "${if (sizeGrams % 1.0 == 0.0) sizeGrams.toInt() else sizeGrams} gr",
                        isMono = true,
                    )
                    ReviewDetailRow(
                        label = "Metode",
                        value = if (isPickup) "Ambil di cabang" else "Dikirim ke alamat",
                    )

                    if (isPickup) {
                        ReviewDetailRow(
                            label = "Cabang",
                            value = branch?.name ?: "Raja Emas Karawaci",
                        )
                    } else {
                        ReviewDetailRow(
                            label = "Alamat",
                            value = address?.let { "${it.label} · ${it.details}" } ?: "Alamat pengiriman",
                        )
                    }

                    ReviewDetailRow(
                        label = "Sisa saldo emas",
                        value = "${formatGrams4(remainingGold).replace(" gr", "")} gr",
                        isMono = true,
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)

                    Text(
                        text = "Rincian (Rp)",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Palette.Ink,
                    )

                    ReviewDetailRow(
                        label = "Biaya cetak",
                        value = rupiah(printFee),
                        isMono = true,
                    )

                    if (!isPickup) {
                        ReviewDetailRow(
                            label = "Ongkir & asuransi",
                            value = rupiah(shippingFee),
                            isMono = true,
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)

                    // Total
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
                            text = rupiah(totalPayment),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Palette.Primary700,
                        )
                    }
                }
            }

            // B. Card Pilihan Metode Pembayaran
            item(key = "review_payment_methods_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Xl))
                        .background(Color.White)
                        .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Xl))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Metode pembayaran",
                        fontFamily = TitleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Palette.Ink,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GoldinRadius.Lg))
                            .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg)),
                    ) {
                        // 1. Saldo Kas
                        PaymentMethodRow(
                            title = "Saldo Kas",
                            subtitle = if (walletBalance < totalPayment) "Saldo tidak cukup · ${rupiah(walletBalance)}" else "Saldo ${rupiah(walletBalance)}",
                            isSubtitleError = walletBalance < totalPayment,
                            icon = GoldinIcons.Cash,
                            isSelected = selectedPm == PaymentMethod.WALLET,
                            onClick = { selectedPm = PaymentMethod.WALLET },
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)

                        // 2. Virtual Account
                        PaymentMethodRow(
                            title = "Virtual Account",
                            subtitle = "BCA, BRI, Mandiri, BNI",
                            isSubtitleError = false,
                            icon = GoldinIcons.BuildingBank,
                            isSelected = selectedPm == PaymentMethod.VA_BCA || selectedPm == PaymentMethod.VIRTUAL_ACCOUNT,
                            onClick = { selectedPm = PaymentMethod.VA_BCA },
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)

                        // 3. QRIS
                        PaymentMethodRow(
                            title = "QRIS",
                            subtitle = "ShopeePay, GoPay, OVO, Dana",
                            isSubtitleError = false,
                            icon = GoldinIcons.Receipt,
                            isSelected = selectedPm == PaymentMethod.QRIS,
                            onClick = { selectedPm = PaymentMethod.QRIS },
                        )
                    }
                }
            }
        }

        // 4. Bottom Action Bar (Sebelumnya vs Selesai)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 0.5.dp, color = Palette.Line)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tombol Sebelumnya (Outline)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .border(1.dp, Palette.Primary700, RoundedCornerShape(GoldinRadius.Full))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sebelumnya",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Primary700,
                    )
                }

                // Tombol Selesai (Primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(if (canFinish) Palette.Primary700 else Palette.Neutral300)
                        .clickable(enabled = canFinish) {
                            showConfirmModal = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Selesai",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (canFinish) Palette.Primary50 else Palette.InkSoft,
                    )
                }
            }
        }
    }

    // 5. Modal Konfirmasi Pesanan (`GoldinModal` · `ctconfirm`)
    if (showConfirmModal) {
        val destinationLabel = if (isPickup) "cabang" else "alamat"
        GoldinModal(
            title = "Konfirmasi pesanan",
            message = "Pesanan tidak bisa dibatalkan dan $destinationLabel tidak bisa diganti setelah dikonfirmasi.",
            checkboxLabel = "Saya mengerti dan setuju bahwa saldo emas saya akan langsung dipotong sebesar ${formatGrams4(sizeGrams)} untuk mencetak keping fisik ini.",
            checkboxChecked = isAgreed,
            onCheckboxChange = { isAgreed = it },
            confirmLabel = "Cetak emas",
            confirmEnabled = isAgreed,
            isDestructive = false,
            tone = GoldinDialogTone.WARNING,
            onConfirm = {
                showConfirmModal = false
                showPinDialog = true
            },
            cancelLabel = "Batal",
            onCancel = {
                showConfirmModal = false
            },
        )
    }

    // 6. Dialog Verifikasi PIN (In-App NumberPad, Zero Soft-Keyboard)
    if (showPinDialog) {
        PinDialog(
            env = env,
            title = "Masukkan PIN 6 digit untuk mencetak keping emas fisik.",
            onDismiss = { showPinDialog = false },
            onVerified = {
                showPinDialog = false
                val createdOrder = env.repo.createPhysicalPrintOrder(
                    brand = brand,
                    weightGrams = sizeGrams,
                    method = method,
                    branchId = branch?.id,
                    deliveryAddress = address?.let {
                        "${it.details}, ${it.subDistrict}, ${it.district}, ${it.city}, ${it.province} ${it.postalCode}"
                    },
                    recipientName = address?.recipientName,
                    recipientPhone = address?.phoneNumber,
                    paymentMethod = selectedPm,
                    pin = MockRepository.GOLDIN_DEMO_PIN,
                )
                onSuccess(createdOrder.id)
            },
        )
    }
}

/**
 * Baris item pada ringkasan detail pesanan cetak.
 */
@Composable
private fun ReviewDetailRow(
    label: String,
    value: String,
    isMono: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 14.sp,
            color = Palette.InkSecondary,
        )
        Text(
            text = value,
            fontFamily = if (isMono) MonoFontFamily else BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Palette.Ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp),
        )
    }
}

/**
 * Baris pilihan metode pembayaran rupiah pada Stepper 3.
 */
@Composable
private fun PaymentMethodRow(
    title: String,
    subtitle: String,
    isSubtitleError: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) Palette.Primary50 else Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon circular badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Palette.Primary100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Palette.Primary700,
                modifier = Modifier.size(20.dp),
            )
        }

        // Title and Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Palette.Ink,
            )
            Text(
                text = subtitle,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                color = if (isSubtitleError) Palette.Critical else Palette.InkSecondary,
            )
        }

        // Radio Button Indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isSelected) Palette.Primary700 else Palette.Neutral300,
                    shape = CircleShape,
                ),
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
}
