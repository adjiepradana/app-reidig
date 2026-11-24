package com.reidig.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reidig.shared.MockRepository
import com.reidig.shared.ReidigException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppEnv(
    val repo: MockRepository,
    val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    /** Menjalankan aksi repository dan menampilkan pesan kalau ditolak. */
    fun <T> attempt(block: () -> T): T? = try {
        block()
    } catch (e: ReidigException) {
        notify(e.message ?: "Terjadi kesalahan.")
        null
    }
}

class SignupDraft {
    var email: String = ""
    var name: String = ""
    var username: String = ""
    var dob: String = ""
    var password: String = ""
    var viaGoogle: Boolean = false
    var emailTicket: String? = null
    var devCode: String? = null

    fun reset() {
        email = ""
        name = ""
        username = ""
        dob = ""
        password = ""
        viaGoogle = false
        emailTicket = null
        devCode = null
    }
}

/** Membawa nilai dari Simulasi Transaksi Beranda ke layar Transaksi (Handoff §6). */
class TradePrefillDraft {
    var mode: String = "beli" // "beli" | "jual"
    var unit: String = "rp"   // "rp" | "gr"
    var value: String = ""
    var hasPending: Boolean = false

    fun set(mode: String, unit: String, value: String) {
        this.mode = mode
        this.unit = unit
        this.value = value
        this.hasPending = true
    }

    fun consume(): Triple<String, String, String>? {
        if (!hasPending) return null
        hasPending = false
        return Triple(mode, unit, value)
    }
}

/** Membawa nominal & target emas dari kartu promo Tabungan Rutin Beranda ke form buat plan (Handoff §6). */
class RoutinePlanPrefillDraft {
    var monthlyAmount: Long = 0L
    var targetGrams: Double? = null
    var hasPending: Boolean = false

    fun set(monthlyAmount: Long, targetGrams: Double?) {
        this.monthlyAmount = monthlyAmount
        this.targetGrams = targetGrams
        this.hasPending = true
    }

    fun consume(): Pair<Long, Double?>? {
        if (!hasPending) return null
        hasPending = false
        return monthlyAmount to targetGrams
    }
}

/** Membawa state aliran pengajuan Cicil Emas (Screen 40-42). */
class InstallmentApplicationDraft {
    var grams: Double = 1.0
    var tenorMonths: Int = 12
    var dpPercent: Int = 20
    var paymentMode: String = "auto"
    var fundingSource: String = "kas"

    fun reset(defaultGrams: Double = 1.0) {
        grams = defaultGrams
        tenorMonths = 12
        dpPercent = 20
        paymentMode = "auto"
        fundingSource = "kas"
    }
}

class KycDraft {
    var consentAgreed: Boolean = false
    var phone: String = ""
    var channel: String = "WhatsApp"

    // Step 2 & 3: e-KTP OCR & Data diri
    var ktpPhoto: Bitmap? = null
    var nik: String = "3671 0151 0203 0002"
    var name: String = ""
    var gender: String = "Perempuan"
    var pob: String = "Tangerang"
    var dob: String = "11/02/2003"
    var dobUnder: Boolean = false
    var agama: String = "Islam"
    var kawin: String = "Belum kawin"
    var ibuKandung: String = ""

    // Step 4: Alamat e-KTP & Domisili
    var prov: String = "Banten"
    var kota: String = "Kota Tangerang"
    var kec: String = "Karawaci"
    var kel: String = "Bugel"
    var alamat: String = "Vila Tangerang Elok A-19/10"
    var rt: String = "008"
    var rw: String = "007"
    var kodepos: String = "15113"

    var domSame: Boolean = true
    var dProv: String = ""
    var dKota: String = ""
    var dKec: String = ""
    var dKel: String = ""
    var dAlamat: String = ""
    var dRt: String = ""
    var dRw: String = ""
    var dKodepos: String = ""

    // Step 5: Informasi tambahan
    var pendidikan: String = ""
    var pekerjaan: String = ""
    var penghasilan: String = ""
    var sumberDana: String = ""
    var tujuan: String = ""

    // Step 7: PIN & Biometrik
    var pin: String = ""
    var biometricEnabled: Boolean = false

    fun reset(defaultName: String = "", defaultDob: String = "") {
        consentAgreed = false
        phone = ""
        channel = "WhatsApp"
        ktpPhoto = null
        nik = "3671 0151 0203 0002"
        name = defaultName.trim().ifBlank { "Indriana Putri" }
        gender = "Perempuan"
        pob = "Tangerang"
        dob = defaultDob.ifBlank { "11/02/2003" }
        dobUnder = false
        agama = "Islam"
        kawin = "Belum kawin"
        ibuKandung = ""
        prov = "Banten"
        kota = "Kota Tangerang"
        kec = "Karawaci"
        kel = "Bugel"
        alamat = "Vila Tangerang Elok A-19/10"
        rt = "008"
        rw = "007"
        kodepos = "15113"
        domSame = true
        dProv = ""
        dKota = ""
        dKec = ""
        dKel = ""
        dAlamat = ""
        dRt = ""
        dRw = ""
        dKodepos = ""
        pendidikan = ""
        pekerjaan = ""
        penghasilan = ""
        sumberDana = ""
        tujuan = ""
        pin = ""
        biometricEnabled = false
    }
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", Locale.forLanguageTag("id-ID"))
    .withZone(ZoneId.of("Asia/Jakarta"))

fun dateTime(ms: Long): String = DATE_FORMAT.format(Instant.ofEpochMilli(ms)) + " WIB"

/** Konfirmasi PIN dengan gaya bottom-sheet seperti pada prototipe Goldin v1. */
@Composable
fun PinDialog(env: AppEnv, title: String, onDismiss: () -> Unit, onVerified: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize()
                .background(Color(0x660F052C))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Palette.Line))
                Spacer(Modifier.height(18.dp))
                Text(
                    "Masukkan PIN",
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Palette.Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    fontFamily = BodyFontFamily,
                    color = Palette.InkSecondary,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                CodeBoxes(code = code, masked = true, error = error != null)
                Spacer(Modifier.height(10.dp))
                if (error != null) {
                    ErrorLine(error!!)
                } else {
                    Text(
                        "Masukkan 6 digit PIN keamananmu",
                        fontFamily = BodyFontFamily,
                        color = Palette.InkSoft,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
                NumberPad(
                    onDigit = { digit ->
                        if (code.length < 6) {
                            error = null
                            code += digit
                            if (code.length == 6) {
                                try {
                                    if (env.repo.verifyPin(code)) {
                                        onVerified()
                                    } else {
                                        error = "PIN salah. Coba lagi."
                                    }
                                } catch (e: ReidigException) {
                                    error = e.message
                                }
                                code = ""
                            }
                        }
                    },
                    onDelete = { code = code.dropLast(1) },
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun PinDialogPreview() = ReidigTheme { PinDialog(rememberPreviewEnv(), "Bayar Rp 2.513.500", {}, {}) }
