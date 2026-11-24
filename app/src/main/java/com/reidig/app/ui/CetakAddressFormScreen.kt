package com.reidig.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reidig.shared.SavedAddress

/**
 * Pilihan field wilayah bertingkat untuk BottomSheet seleksi.
 */
enum class RegionField(val title: String) {
    PROVINCE("Provinsi"),
    CITY("Kota/kabupaten"),
    DISTRICT("Kecamatan"),
    SUB_DISTRICT("Kelurahan/desa"),
}

/**
 * Screen 42: Form Tambah / Ubah Alamat Pengiriman (`42 Alamat · Form` / `addrForm`)
 *
 * Mengimplementasikan 100% presisi Design System v2, prototipe `Main.dc.html` (L2758–L2775),
 * dan dokumen handoff UI/UX §12 & §13.
 */
@Composable
fun CetakAddressFormScreen(
    onBack: () -> Unit,
    onSave: (SavedAddress) -> Unit,
    existingAddress: SavedAddress? = null,
    modifier: Modifier = Modifier,
) {
    var label by rememberSaveable { mutableStateOf(existingAddress?.label ?: "") }
    var recipientName by rememberSaveable { mutableStateOf(existingAddress?.recipientName ?: "") }
    var phone by rememberSaveable { mutableStateOf(existingAddress?.phoneNumber ?: "") }
    var province by rememberSaveable { mutableStateOf(existingAddress?.province ?: "") }
    var city by rememberSaveable { mutableStateOf(existingAddress?.city ?: "") }
    var district by rememberSaveable { mutableStateOf(existingAddress?.district ?: "") }
    var subDistrict by rememberSaveable { mutableStateOf(existingAddress?.subDistrict ?: "") }
    var postalCode by rememberSaveable { mutableStateOf(existingAddress?.postalCode ?: "") }
    var details by rememberSaveable { mutableStateOf(existingAddress?.details ?: "") }
    var isPrimary by rememberSaveable { mutableStateOf(existingAddress?.isPrimary ?: false) }

    // State untuk BottomSheet pemilih wilayah bertingkat
    var activeRegionSelector by remember { mutableStateOf<RegionField?>(null) }

    val isFormValid = label.isNotBlank() &&
            recipientName.isNotBlank() &&
            phone.length >= 10 &&
            province.isNotBlank() &&
            city.isNotBlank() &&
            district.isNotBlank() &&
            subDistrict.isNotBlank() &&
            postalCode.length == 5 &&
            details.trim().length >= 5

    BackHandler(onBack = onBack)

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
            // 1. Top Bar 44dp dengan tombol back dan judul terpusat
            AddressFormTopBar(
                title = if (existingAddress != null) "Ubah alamat" else "Tambah alamat",
                onBack = onBack,
            )

            // 2. Konten Form Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Field 1: Label alamat *
                AddressFormField(
                    label = "Label alamat",
                    value = label,
                    onValueChange = { label = it.take(30) },
                    placeholder = "Contoh: Rumah",
                )

                // Field 2: Nama penerima *
                AddressFormField(
                    label = "Nama penerima",
                    value = recipientName,
                    onValueChange = { recipientName = it.take(60) },
                    placeholder = "Nama lengkap",
                )

                // Field 3: Nomor HP *
                AddressFormField(
                    label = "Nomor HP",
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(14) },
                    placeholder = "08xxxxxxxxxx",
                    keyboardType = KeyboardType.Phone,
                )

                // Field 4: Provinsi *
                AddressFormSelector(
                    label = "Provinsi",
                    value = province,
                    placeholder = "Pilih provinsi",
                    onClick = { activeRegionSelector = RegionField.PROVINCE },
                )

                // Field 5: Kota/kabupaten *
                AddressFormSelector(
                    label = "Kota/kabupaten",
                    value = city,
                    placeholder = "Pilih kota/kabupaten",
                    onClick = { activeRegionSelector = RegionField.CITY },
                )

                // Field 6: Kecamatan *
                AddressFormSelector(
                    label = "Kecamatan",
                    value = district,
                    placeholder = "Pilih kecamatan",
                    onClick = { activeRegionSelector = RegionField.DISTRICT },
                )

                // Field 7: Kelurahan/desa *
                AddressFormSelector(
                    label = "Kelurahan/desa",
                    value = subDistrict,
                    placeholder = "Pilih kelurahan/desa",
                    onClick = { activeRegionSelector = RegionField.SUB_DISTRICT },
                )

                // Field 8: Kode pos *
                AddressFormField(
                    label = "Kode pos",
                    value = postalCode,
                    onValueChange = { postalCode = it.filter { ch -> ch.isDigit() }.take(5) },
                    placeholder = "00000",
                    keyboardType = KeyboardType.Number,
                )

                // Field 9: Detail alamat *
                AddressFormTextarea(
                    label = "Detail alamat",
                    value = details,
                    onValueChange = { details = it.take(200) },
                    placeholder = "Nama jalan, nomor rumah, RT/RW",
                )

                // Checkbox: Jadikan alamat utama
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GoldinRadius.Md))
                        .clickable { isPrimary = !isPrimary }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPrimary) Palette.Primary700 else Color.White)
                            .border(
                                width = if (isPrimary) 0.dp else 1.5.dp,
                                color = if (isPrimary) Color.Transparent else Palette.Line,
                                shape = RoundedCornerShape(4.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isPrimary) {
                            Icon(
                                imageVector = GoldinIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }

                    Text(
                        text = "Jadikan alamat utama",
                        fontFamily = BodyFontFamily,
                        fontSize = 14.sp,
                        color = Palette.Ink,
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // 3. Floating Footer: Tombol "Simpan alamat"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = GoldinBorder.Hairline, color = Palette.Line)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(GoldinRadius.Full))
                        .background(
                            if (isFormValid) Palette.Primary700 else Palette.Primary700.copy(alpha = 0.4f),
                        )
                        .clickable(enabled = isFormValid) {
                            val newAddr = SavedAddress(
                                id = existingAddress?.id ?: "addr-${System.currentTimeMillis()}",
                                label = label.trim(),
                                recipientName = recipientName.trim(),
                                phoneNumber = phone.trim(),
                                province = province.trim(),
                                city = city.trim(),
                                district = district.trim(),
                                subDistrict = subDistrict.trim(),
                                postalCode = postalCode.trim(),
                                details = details.trim(),
                                isPrimary = isPrimary,
                            )
                            onSave(newAddr)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Simpan alamat",
                        fontFamily = BodyFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Palette.Primary50,
                    )
                }
            }
        }

        // 4. Selector Bottom Sheet untuk Wilayah Bertingkat (Provinsi, Kota, Kecamatan, Kelurahan)
        activeRegionSelector?.let { field ->
            val options = remember(field, province, city) {
                getRegionOptions(field, province, city)
            }
            val currentValue = when (field) {
                RegionField.PROVINCE -> province
                RegionField.CITY -> city
                RegionField.DISTRICT -> district
                RegionField.SUB_DISTRICT -> subDistrict
            }

            GoldinBottomSheet(
                title = field.title,
                onDismiss = { activeRegionSelector = null },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                ) {
                    options.forEach { option ->
                        val isSelected = option.equals(currentValue, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    when (field) {
                                        RegionField.PROVINCE -> {
                                            province = option
                                            city = ""
                                            district = ""
                                            subDistrict = ""
                                        }
                                        RegionField.CITY -> {
                                            city = option
                                            district = ""
                                            subDistrict = ""
                                        }
                                        RegionField.DISTRICT -> {
                                            district = option
                                            subDistrict = ""
                                        }
                                        RegionField.SUB_DISTRICT -> {
                                            subDistrict = option
                                        }
                                    }
                                    activeRegionSelector = null
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = option,
                                fontFamily = BodyFontFamily,
                                fontSize = 14.sp,
                                color = Palette.Ink,
                                modifier = Modifier.weight(1f),
                            )

                            // Radio Indicator 20dp
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Palette.Primary700 else Palette.Line,
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
                        HorizontalDivider(thickness = GoldinBorder.Hairline, color = Palette.Line)
                    }
                }
            }
        }
    }
}

/**
 * TopBar 44dp dengan tombol back `<` dan judul terpusat sesuai Screen 42.
 */
@Composable
private fun AddressFormTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = GoldinIcons.ChevronLeft,
                contentDescription = "Kembali",
                tint = Palette.Ink,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = title,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * Label dengan penanda wajib merah `*` sesuai spesifikasi Design System & Handoff.
 */
@Composable
private fun FormFieldLabel(
    text: String,
    isRequired: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = text,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Palette.Ink,
        )
        if (isRequired) {
            Text(
                text = "*",
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Palette.Critical,
            )
        }
    }
}

/**
 * Komponen Input Text Field 44dp dengan border 1dp dan radius 12dp.
 */
@Composable
private fun AddressFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormFieldLabel(text = label)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .background(Color.White)
                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = Palette.InkSoft,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    color = Palette.Ink,
                ),
                cursorBrush = SolidColor(Palette.Primary700),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Komponen Selector Field 44dp dengan tombol chevron-down dan membuka BottomSheet.
 */
@Composable
private fun AddressFormSelector(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormFieldLabel(text = label)

        val hasValue = value.isNotBlank()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .background(Color.White)
                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (hasValue) value else placeholder,
                fontFamily = BodyFontFamily,
                fontSize = 14.sp,
                color = if (hasValue) Palette.Ink else Palette.InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = GoldinIcons.ChevronDown,
                contentDescription = null,
                tint = Palette.InkSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Komponen Textarea Multi-line (min 88dp) untuk detail alamat lengkap.
 */
@Composable
private fun AddressFormTextarea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormFieldLabel(text = label)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .clip(RoundedCornerShape(GoldinRadius.Lg))
                .background(Color.White)
                .border(1.dp, Palette.Line, RoundedCornerShape(GoldinRadius.Lg))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopStart,
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
                minLines = 3,
                maxLines = 4,
                textStyle = TextStyle(
                    fontFamily = BodyFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Palette.Ink,
                ),
                cursorBrush = SolidColor(Palette.Primary700),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Data opsi wilayah bertingkat sesuai prototype Main.dc.html L3273-3276.
 */
private fun getRegionOptions(field: RegionField, province: String, city: String): List<String> {
    return when (field) {
        RegionField.PROVINCE -> listOf(
            "Banten",
            "DKI Jakarta",
            "Jawa Barat",
            "Jawa Tengah",
            "Jawa Timur",
            "DI Yogyakarta",
            "Bali",
        )
        RegionField.CITY -> when (province) {
            "DKI Jakarta" -> listOf("Jakarta Selatan", "Jakarta Pusat", "Jakarta Barat", "Jakarta Timur", "Jakarta Utara")
            "Jawa Barat" -> listOf("Kota Bogor", "Kabupaten Bogor", "Kota Depok", "Kota Bekasi", "Kota Bandung")
            else -> listOf("Kota Tangerang", "Kabupaten Tangerang", "Kota Tangerang Selatan", "Kota Serang", "Kota Cilegon")
        }
        RegionField.DISTRICT -> listOf(
            "Karawaci",
            "Cibodas",
            "Cipondoh",
            "Tangerang",
            "Kelapa Dua",
            "Serpong",
            "Serpong Utara",
            "Kebayoran Baru",
            "Bogor Tengah",
        )
        RegionField.SUB_DISTRICT -> listOf(
            "Bugel",
            "Nusa Jaya",
            "Sumur Pacing",
            "Cimone",
            "Bencongan",
            "Panunggangan",
            "Lengkong Gudang",
            "Melawai",
            "Pabaton",
        )
    }
}
