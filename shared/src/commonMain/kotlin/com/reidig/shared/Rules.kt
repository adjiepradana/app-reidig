package com.reidig.shared

object Rules {
    const val PICKUP_MIN_WEIGHT_MG = 2_000L
    const val HOLDING_PERIOD_MS = 24L * 60 * 60 * 1000
    const val QUOTE_TTL_MS = 60_000L
    const val PAYMENT_WINDOW_MS = 15L * 60 * 1000
    const val INSTALLMENT_AGREEMENT_VERSION = "CICIL-1"
    const val MAX_PIN_ATTEMPTS = 5
    /** Situs menampilkan buyback di bawah nilai ini bebas PPh 22. Perlu konfirmasi konsultan pajak. */
    const val TAX_FREE_SELL_LIMIT = 10_000_000L
    /** Nonaktif sampai cakupan izin saldo terkonfirmasi. */
    const val TOPUP_ENABLED = false

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

    fun isValidEmail(value: String) = EMAIL.matches(value.trim())

    fun hasMinLength(password: String) = password.length >= 8

    fun hasLettersAndNumbers(password: String) = password.any { it.isLetter() } && password.any { it.isDigit() }

    fun hasCapitalLetter(password: String) = password.any { it.isUpperCase() }

    fun isValidPassword(password: String) = hasMinLength(password) && hasLettersAndNumbers(password)

    /** Aturan kata sandi Goldin v1.2: Minimal 8 karakter dan Mengandung huruf kapital. */
    fun isValidGoldinPassword(password: String) = hasMinLength(password) && hasCapitalLetter(password)

    /** Daftar username terpakai pada prototipe Goldin v1.2 untuk uji live availability check. */
    val TAKEN_USERNAMES = setOf("admin", "emdig", "indriana", "emas")

    fun normalizeUsername(raw: String): String = raw.replace(Regex("\\s+"), "").lowercase()

    fun isUsernameMinLength(username: String): Boolean = normalizeUsername(username).length >= 4

    fun isUsernameTaken(username: String): Boolean = normalizeUsername(username) in TAKEN_USERNAMES

    /** Durasi kunci PIN bertingkat (menit): 10 menit, 30 menit, 60 menit (1 jam). */
    val PIN_LOCK_DURATIONS_MINUTES = listOf(10, 30, 60)

    /** Nomor setelah +62, tanpa angka 0 di depan. */
    fun isValidIndonesianPhone(digits: String) = digits.all { it in '0'..'9' } && digits.startsWith("8") && digits.length in 9..12

    fun isValidNik(nik: String) = nik.length == 16 && nik.all { it in '0'..'9' }

    fun isValidFullName(name: String) = name.trim().length >= 3 && name.trim().any { it.isLetter() }

    private val LETTERS_ONLY_NAME = Regex("^[A-Za-z\\u00C0-\\u024F .'\\-]+$")

    fun isValidNameOnlyLetters(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.length >= 3 && LETTERS_ONLY_NAME.matches(trimmed)
    }

    /**
     * Pesan error spesifik pembuatan PIN e-KYC sesuai prototipe Goldin v1.2 (`Main.dc.html` L3482–3486):
     * - Angka sama semua (111111)
     * - Angka berurutan (123456 / 654321)
     * - Kombinasi tanggal lahir (DDMMYY / YYMMDD / MMDDYY)
     */
    fun pinValidationError(pin: String, dob: String? = null): String? {
        if (pin.length != 6 || !pin.all { it in '0'..'9' }) return "PIN harus terdiri dari 6 angka"
        if (pin.all { it == pin[0] }) return "PIN tidak boleh berisi angka yang sama semua"
        val steps = pin.zipWithNext { a, b -> b - a }
        if (steps.all { it == 1 } || steps.all { it == -1 }) return "PIN tidak boleh berisi angka berurutan"
        if (!dob.isNullOrBlank()) {
            val parts = dob.split("/")
            if (parts.size == 3) {
                val dd = parts[0].padStart(2, '0')
                val mm = parts[1].padStart(2, '0')
                val yyyy = parts[2]
                val yy = if (yyyy.length >= 4) yyyy.substring(2) else ""
                val birthCodes = setOf(dd + mm + yy, yy + mm + dd, mm + dd + yy)
                if (pin in birthCodes) return "PIN tidak boleh memakai tanggal lahirmu"
            }
        }
        return null
    }

    /** Format DD/MM/YYYY, minimal 18 tahun pada tanggal acuan. */
    fun isAdult(dob: String, todayYear: Int, todayMonth: Int, todayDay: Int): Boolean {
        val parts = dob.split("/")
        if (parts.size != 3) return false
        val day = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val year = parts[2].toIntOrNull() ?: return false
        if (month !in 1..12 || day !in 1..daysInMonth(month, year) || year < 1900) return false
        var age = todayYear - year
        if (todayMonth < month || (todayMonth == month && todayDay < day)) age--
        return age >= 18
    }

    private fun daysInMonth(month: Int, year: Int) = when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    fun isWeakPin(pin: String, dob: String? = null): Boolean {
        if (pin.length != 6 || !pin.all { it in '0'..'9' }) return true
        if (pin.toSet().size <= 2) return true
        val steps = pin.zipWithNext { a, b -> b - a }
        if (steps.all { it == 1 } || steps.all { it == -1 }) return true

        if (dob != null) {
            val parts = dob.split("/")
            if (parts.size == 3) {
                val dd = parts[0].padStart(2, '0')
                val mm = parts[1].padStart(2, '0')
                val yyyy = parts[2]
                val yy = if (yyyy.length >= 4) yyyy.substring(2) else ""
                val birthCodes = setOf(dd + mm + yy, yy + mm + dd, mm + dd + yy)
                if (pin in birthCodes) return true
            }
        }
        return false
    }

    fun isPickupEligible(totalWeightMg: Long) = totalWeightMg >= PICKUP_MIN_WEIGHT_MG

    fun canSellAt(token: GoldToken, nowMs: Long) = token.status == TokenStatus.ACTIVE && nowMs - token.acquiredAtMs >= HOLDING_PERIOD_MS
}

/** Opsi dropdown e-KYC dan Alamat tersimpan sesuai `OPTS` di prototipe Goldin v1.2 (`Main.dc.html` L3269–3282). */
object KycOptions {
    val gender = listOf("Laki-laki", "Perempuan")
    val agama = listOf("Islam", "Kristen", "Katolik", "Hindu", "Buddha", "Konghucu", "Kepercayaan terhadap Tuhan YME")
    val kawin = listOf("Belum kawin", "Kawin", "Cerai hidup", "Cerai mati")
    val prov = listOf("Banten", "DKI Jakarta", "Jawa Barat", "Jawa Tengah", "Jawa Timur")
    val kota = listOf("Kota Tangerang", "Kabupaten Tangerang", "Kota Tangerang Selatan", "Kota Serang")
    val kec = listOf("Karawaci", "Cibodas", "Cipondoh", "Tangerang")
    val kel = listOf("Bugel", "Nusa Jaya", "Sumur Pacing", "Cimone")
    val pendidikan = listOf("SD", "SMP", "SMA/SMK", "Diploma (D1–D3)", "Sarjana (S1)", "Magister (S2)", "Doktor (S3)")
    val pekerjaan = listOf("Karyawan swasta", "PNS/TNI/Polri", "Wiraswasta", "Profesional", "Pelajar/mahasiswa", "Ibu rumah tangga", "Lainnya")
    val penghasilan = listOf("Di bawah Rp5 juta", "Rp5–10 juta", "Rp10–25 juta", "Rp25–50 juta", "Di atas Rp50 juta")
    val sumber = listOf("Gaji", "Hasil usaha", "Hasil investasi", "Warisan atau hibah", "Lainnya")
    val tujuan = listOf("Investasi", "Tabungan jangka panjang", "Dana darurat", "Lainnya")
}

fun rupiah(value: Long): String {
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return (if (value < 0) "-Rp" else "Rp") + grouped
}

/** Format angka murni tanpa awalan Rp, dengan pemisah titik (digunakan pada rincian struk). */
fun formatNumberOnly(value: Long): String {
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return (if (value < 0) "-" else "") + grouped
}

/** Format berat emas 4 desimal dengan koma sesuai prototipe Goldin v1.2: misal 2,4518 gr. */
fun formatGrams4(grams: Double): String {
    val intPart = grams.toLong()
    val fracPart = kotlin.math.round((grams - intPart) * 10000).toLong().coerceIn(0, 9999)
    val fracStr = fracPart.toString().padStart(4, '0')
    return "$intPart,$fracStr gr"
}

fun gram(weightMg: Long): String {
    val whole = weightMg / 1000
    val fraction = (weightMg % 1000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$whole gr" else "$whole,$fraction gr"
}

fun percent(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10},${tenths % 10}%"
}
