package com.reidig.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.shared.GoldBranch

/**
 * SCREEN 51: KATALOG 8 CABANG RESMI RAJA EMAS (`30b Cetak · Semua cabang` / `ctBrAll` / `ctBranch`)
 *
 * Mengimplementasikan:
 * - Header halaman 44dp dengan tombol kembali
 * - Search bar interaktif pencarian real-time (nama cabang, jalan, kota, provinsi)
 * - Chip filter wilayah interaktif (Semua, Banten, DKI Jakarta, Jawa Barat)
 * - Katalog lengkap 8 Cabang Resmi Raja Emas (Handoff §12 L124)
 * - Kartu detail cabang (jarak km, jam operasional, status hari buka Senin-Sabtu vs Senin-Minggu)
 * - Tombol aksi "Pilih cabang ini" yang langsung mengaitkan cabang ke pesanan cetak
 * - Empty state jika pencarian tidak menemukan hasil
 */
@Composable
fun CetakBranchScreen(
    env: AppEnv,
    onBack: () -> Unit,
    onSelectBranch: (branchId: String) -> Unit,
    selectedBranchId: String? = null,
    modifier: Modifier = Modifier,
) {
    var allBranches by remember { mutableStateOf(env.repo.getGoldBranches()) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        try {
            env.repo.fetchBranches()
            allBranches = env.repo.getGoldBranches()
        } catch (_: Exception) {}
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedProvince by rememberSaveable { mutableStateOf("Semua") }

    val provinces = remember(allBranches) {
        val list = mutableListOf("Semua")
        val dynamicProvinces = allBranches.map { it.province.trim() }.filter { it.isNotEmpty() }.distinct()
        list.addAll(dynamicProvinces)
        if (!list.contains("Banten")) list.add("Banten")
        if (!list.contains("DKI Jakarta")) list.add("DKI Jakarta")
        if (!list.contains("Jawa Barat")) list.add("Jawa Barat")
        list.distinct()
    }

    val filteredBranches = remember(searchQuery, selectedProvince, allBranches) {
        val q = searchQuery.trim().lowercase()
        allBranches.filter { branch ->
            val matchProvince = selectedProvince == "Semua" || branch.province.equals(selectedProvince, ignoreCase = true)
            val matchQuery = q.isEmpty() ||
                    branch.name.lowercase().contains(q) ||
                    branch.address.lowercase().contains(q) ||
                    branch.city.lowercase().contains(q) ||
                    branch.province.lowercase().contains(q)
            matchProvince && matchQuery
        }.sortedBy { it.distanceKm }
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
            title = "Cabang Raja Emas",
            onBack = onBack,
        )

        // 2. Search & Filter Bar Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Search Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(GoldinRadius.Md))
                    .background(Color.White)
                    .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Md))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = GoldinIcons.Search,
                    contentDescription = null,
                    tint = Palette.InkSoft,
                    modifier = Modifier.size(20.dp),
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari cabang...",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            color = Palette.InkSoft,
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            color = Palette.Ink,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = GoldinIcons.X,
                        contentDescription = "Hapus pencarian",
                        tint = Palette.InkSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { searchQuery = "" },
                    )
                }
            }

            // Horizontal Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                provinces.forEach { province ->
                    val isSelected = selectedProvince == province
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(GoldinRadius.Full))
                            .background(if (isSelected) Palette.Primary700 else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Palette.Primary700 else Palette.Line,
                                shape = RoundedCornerShape(GoldinRadius.Full),
                            )
                            .clickable { selectedProvince = province }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = province,
                            fontFamily = BodyFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) Palette.Primary50 else Palette.InkSecondary,
                        )
                    }
                }
            }
        }

        // 3. Daftar Cabang Resmi
        if (filteredBranches.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "branch_count_info") {
                    Text(
                        text = "Menampilkan ${filteredBranches.size} cabang",
                        fontFamily = BodyFontFamily,
                        fontSize = 12.sp,
                        color = Palette.InkSecondary,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }

                itemsIndexed(
                    items = filteredBranches,
                    key = { _, b -> b.id },
                ) { _, branch ->
                    CetakBranchCard(
                        branch = branch,
                        isSelected = selectedBranchId == branch.id,
                        onSelect = { onSelectBranch(branch.id) },
                    )
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
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
                            imageVector = GoldinIcons.MapPin,
                            contentDescription = null,
                            tint = Palette.Primary700,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Text(
                        text = "Cabang tidak ditemukan",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Palette.Ink,
                    )

                    Text(
                        text = "Coba gunakan kata kunci pencarian lain atau pilih filter wilayah \"Semua\".",
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = Palette.InkSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

/**
 * Kartu Cabang Raja Emas pada katalog cabang.
 */
@Composable
private fun CetakBranchCard(
    branch: GoldBranch,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GoldinRadius.Xl))
            .background(if (isSelected) Palette.Primary50 else Color.White)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Palette.Primary700 else Palette.Line,
                shape = RoundedCornerShape(GoldinRadius.Xl),
            )
            .clickable(onClick = onSelect)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Row: Pin Icon, Branch Name, Distance Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = branch.name,
                            fontFamily = BodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Palette.Ink,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        // Distance Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(GoldinRadius.Full))
                                .background(Palette.Primary100)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${branch.distanceKm} km",
                                fontFamily = BodyFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = Palette.Primary700,
                            )
                        }
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = branch.address,
                        fontFamily = BodyFontFamily,
                        fontSize = 13.sp,
                        color = Palette.InkSecondary,
                        lineHeight = 18.sp,
                    )
                }
            }

            // Divider
            HorizontalDivider(thickness = 0.5.dp, color = Palette.Line)

            // Bottom Meta & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = GoldinIcons.Clock,
                            contentDescription = null,
                            tint = Palette.InkSoft,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${branch.openingHours} WIB",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSecondary,
                        )
                    }

                    Text(
                        text = if (branch.isSevenDays) "Buka setiap hari (Senin–Minggu)" else "Buka Senin–Sabtu (Minggu libur)",
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                        color = Palette.InkSoft,
                    )
                }

                // Tombol Pilih Cabang
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(if (isSelected) Palette.Primary700 else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = Palette.Primary700,
                            shape = RoundedCornerShape(GoldinRadius.Full),
                        )
                        .clickable(onClick = onSelect)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSelected) "Terpilih" else "Pilih cabang",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (isSelected) Palette.Primary50 else Palette.Primary700,
                    )
                }
            }
        }
    }
}
