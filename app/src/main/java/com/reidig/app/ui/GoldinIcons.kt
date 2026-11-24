package com.reidig.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Kumpulan aset ikon resmi Goldin (Design System v2 & Handoff §3).
 *
 * Menggunakan stroke 1.75dp standar (GoldinControl.IconStroke), ujung bulat (StrokeCap.Round),
 * dan ukuran dasar 24x24dp. Seluruh ikon di-parse secara hardware-accelerated dan di-cache dalam memori.
 */
object GoldinIcons {
    private fun strokeVector(
        name: String,
        paths: List<String>,
        strokeWidth: Float = GoldinControl.IconStroke,
        viewportSize: Float = 24f,
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = viewportSize.dp,
            defaultHeight = viewportSize.dp,
            viewportWidth = viewportSize,
            viewportHeight = viewportSize,
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

    private fun fillVector(
        name: String,
        paths: List<String>,
        viewportSize: Float = 24f,
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = viewportSize.dp,
            defaultHeight = viewportSize.dp,
            viewportWidth = viewportSize,
            viewportHeight = viewportSize,
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = SolidColor(Color.Black),
                stroke = null,
            )
        }
        return builder.build()
    }

    val AdjustmentsHorizontal: ImageVector by lazy {
        strokeVector(
            "AdjustmentsHorizontal",
            listOf(
                "M12 6a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M4 6l8 0",
            "M16 6l4 0",
            "M6 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M4 12l2 0",
            "M10 12l10 0",
            "M15 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M4 18l11 0",
            "M19 18l1 0"
            ),
        )
    }

    val FilterSliders: ImageVector get() = AdjustmentsHorizontal

    val AlertCircle: ImageVector by lazy {
        strokeVector(
            "AlertCircle",
            listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0",
            "M12 8v4",
            "M12 16h.01"
            ),
        )
    }

    val ArrowDownLeft: ImageVector by lazy {
        strokeVector(
            "ArrowDownLeft",
            listOf(
                "M17 7l-10 10",
            "M16 17l-9 0l0 -9"
            ),
        )
    }

    val ArrowRight: ImageVector by lazy {
        strokeVector(
            "ArrowRight",
            listOf(
                "M5 12l14 0",
            "M13 18l6 -6",
            "M13 6l6 6"
            ),
        )
    }

    val ArrowUpRight: ImageVector by lazy {
        strokeVector(
            "ArrowUpRight",
            listOf(
                "M17 7l-10 10",
            "M8 7l9 0l0 9"
            ),
        )
    }

    val ArrowsDownUp: ImageVector by lazy {
        strokeVector(
            "ArrowsDownUp",
            listOf(
                "M17 3l0 18",
            "M10 18l-3 3l-3 -3",
            "M7 21l0 -18",
            "M20 6l-3 -3l-3 3"
            ),
        )
    }

    val ArrowsUpDown: ImageVector get() = ArrowsDownUp

    val ArrowsExchange: ImageVector by lazy {
        strokeVector(
            "ArrowsExchange",
            listOf(
                "M7 10h14l-4 -4",
            "M17 14h-14l4 4"
            ),
        )
    }

    val AtSign: ImageVector by lazy {
        strokeVector(
            "AtSign",
            listOf(
                "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
            "M16 12v1.5a2.5 2.5 0 0 0 5 0v-1.5a9 9 0 1 0 -5.5 8.28"
            ),
        )
    }

    val At: ImageVector get() = AtSign

    val Backspace: ImageVector by lazy {
        strokeVector(
            "Backspace",
            listOf(
                "M20 6a1 1 0 0 1 1 1v10a1 1 0 0 1 -1 1h-11l-5 -5a1.5 1.5 0 0 1 0 -2l5 -5l11 0",
            "M12 10l4 4m0 -4l-4 4"
            ),
        )
    }

    val Bell: ImageVector by lazy {
        strokeVector(
            "Bell",
            listOf(
                "M10 5a2 2 0 1 1 4 0a7 7 0 0 1 4 6v3a4 4 0 0 0 2 3h-16a4 4 0 0 0 2 -3v-3a7 7 0 0 1 4 -6",
            "M9 17v1a3 3 0 0 0 6 0v-1"
            ),
        )
    }

    val Box: ImageVector by lazy {
        strokeVector(
            "Box",
            listOf(
                "M12 3l8 4.5l0 9l-8 4.5l-8 -4.5l0 -9l8 -4.5",
            "M12 12l8 -4.5",
            "M12 12l0 9",
            "M12 12l-8 -4.5"
            ),
        )
    }

    val BuildingBank: ImageVector by lazy {
        strokeVector(
            "BuildingBank",
            listOf(
                "M3 21l18 0",
            "M3 10l18 0",
            "M5 6l7 -3l7 3",
            "M4 10l0 11",
            "M20 10l0 11",
            "M8 14l0 3",
            "M12 14l0 3",
            "M16 14l0 3"
            ),
        )
    }

    val BuildingStore: ImageVector by lazy {
        strokeVector(
            "BuildingStore",
            listOf(
                "M3 21l18 0",
            "M3 7v1a3 3 0 0 0 6 0v-1m0 1a3 3 0 0 0 6 0v-1m0 1a3 3 0 0 0 6 0v-1h-18l2 -4h14l2 4",
            "M5 21l0 -10.15",
            "M19 21l0 -10.15",
            "M9 21v-4a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v4"
            ),
        )
    }

    val Calendar: ImageVector by lazy {
        strokeVector(
            "Calendar",
            listOf(
                "M4 7a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-12z",
            "M16 3v4",
            "M8 3v4",
            "M4 11h16"
            ),
        )
    }

    val CaretDownFilled: ImageVector by lazy {
        fillVector(
            "CaretDownFilled",
            listOf(
                "M18 9c.852 0 1.297 .986 .783 1.623l-.076 .084l-6 6a1 1 0 0 1 -1.32 .083l-.094 -.083l-6 -6l-.083 -.094l-.054 -.077l-.054 -.096l-.017 -.036l-.027 -.067l-.032 -.108l-.01 -.053l-.01 -.06l-.004 -.057v-.118l.005 -.058l.009 -.06l.01 -.052l.032 -.108l.027 -.067l.07 -.132l.065 -.09l.073 -.081l.094 -.083l.077 -.054l.096 -.054l.036 -.017l.067 -.027l.108 -.032l.053 -.01l.06 -.01l.057 -.004l12.059 -.002z"
            ),
        )
    }

    val CaretUpFilled: ImageVector by lazy {
        fillVector(
            "CaretUpFilled",
            listOf(
                "M11.293 7.293a1 1 0 0 1 1.32 -.083l.094 .083l6 6l.083 .094l.054 .077l.054 .096l.017 .036l.027 .067l.032 .108l.01 .053l.01 .06l.004 .057l.002 .059l-.002 .059l-.005 .058l-.009 .06l-.01 .052l-.032 .108l-.027 .067l-.07 .132l-.065 .09l-.073 .081l-.094 .083l-.077 .054l-.096 .054l-.036 .017l-.067 .027l-.108 .032l-.053 .01l-.06 .01l-.057 .004l-.059 .002h-12c-.852 0 -1.297 -.986 -.783 -1.623l.076 -.084l6 -6z"
            ),
        )
    }

    val Cash: ImageVector by lazy {
        strokeVector(
            "Cash",
            listOf(
                "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
            "M3 8a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2z",
            "M18 12h.01",
            "M6 12h.01"
            ),
        )
    }

    val CashBanknote: ImageVector get() = Cash

    val Portfolio: ImageVector by lazy {
        strokeVector(
            "Portfolio",
            listOf(
                "M10 3.2a9 9 0 1 0 10.8 10.8a1 1 0 0 0 -1 -1h-6.8a2 2 0 0 1 -2 -2v-7a.9 .9 0 0 0 -1 -.8",
            "M15 3.5a9 9 0 0 1 5.5 5.5h-4.5a1 1 0 0 1 -1 -1v-4.5"
            ),
        )
    }

    val PieChart: ImageVector get() = Portfolio

    val ChartPie: ImageVector get() = Portfolio

    val Check: ImageVector by lazy {
        strokeVector(
            "Check",
            listOf(
                "M5 12l5 5l10 -10"
            ),
        )
    }

    val CheckStroke3: ImageVector by lazy {
        strokeVector(
            "CheckStroke3",
            listOf(
                "M5 12l5 5l10 -10"
            ),
            strokeWidth = 3f,
        )
    }

    val ChevronDown: ImageVector by lazy {
        strokeVector(
            "ChevronDown",
            listOf(
                "M6 9l6 6l6 -6"
            ),
        )
    }

    val ChevronLeft: ImageVector by lazy {
        strokeVector(
            "ChevronLeft",
            listOf(
                "M15 6l-6 6l6 6"
            ),
        )
    }

    val ChevronRight: ImageVector by lazy {
        strokeVector(
            "ChevronRight",
            listOf(
                "M9 6l6 6l-6 6"
            ),
        )
    }

    val Circle: ImageVector by lazy {
        strokeVector(
            "Circle",
            listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0"
            ),
        )
    }

    val CircleOutline: ImageVector get() = Circle

    val CircleCheckFilled: ImageVector by lazy {
        fillVector(
            "CircleCheckFilled",
            listOf(
                "M17 3.34a10 10 0 1 1 -14.995 8.984l-.005 -.324l.005 -.324a10 10 0 0 1 14.995 -8.336zm-1.293 5.953a1 1 0 0 0 -1.32 -.083l-.094 .083l-3.293 3.292l-1.293 -1.292l-.094 -.083a1 1 0 0 0 -1.403 1.403l.083 .094l2 2l.094 .083a1 1 0 0 0 1.226 0l.094 -.083l4 -4l.083 -.094a1 1 0 0 0 -.083 -1.32z"
            ),
        )
    }

    val CircleXFilled: ImageVector by lazy {
        fillVector(
            "CircleXFilled",
            listOf(
                "M17 3.34a10 10 0 1 1 -14.995 8.984l-.005 -.324l.005 -.324a10 10 0 0 1 14.995 -8.336zm-6.489 5.8a1 1 0 0 0 -1.218 1.567l1.292 1.293l-1.292 1.293l-.083 .094a1 1 0 0 0 1.497 1.32l1.293 -1.292l1.293 1.292l.094 .083a1 1 0 0 0 1.32 -1.497l-1.292 -1.293l1.292 -1.293l.083 -.094a1 1 0 0 0 -1.497 -1.32l-1.293 1.292l-1.293 -1.292l-.094 -.083z"
            ),
        )
    }

    val Clock: ImageVector by lazy {
        strokeVector(
            "Clock",
            listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0",
            "M12 7v5l3 3"
            ),
        )
    }

    val Coin: ImageVector by lazy {
        strokeVector(
            "Coin",
            listOf(
                "M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
            "M14.8 9a2 2 0 0 0 -1.8 -1h-2a2 2 0 1 0 0 4h2a2 2 0 1 1 0 4h-2a2 2 0 0 1 -1.8 -1",
            "M12 7v10"
            ),
        )
    }

    val Copy: ImageVector by lazy {
        strokeVector(
            "Copy",
            listOf(
                "M7 9.667a2.667 2.667 0 0 1 2.667 -2.667h8.666a2.667 2.667 0 0 1 2.667 2.667v8.666a2.667 2.667 0 0 1 -2.667 2.667h-8.666a2.667 2.667 0 0 1 -2.667 -2.667z",
            "M4.012 16.737a2.005 2.005 0 0 1 -1.012 -1.737v-10c0 -1.1 .9 -2 2 -2h10c.75 0 1.158 .385 1.5 1"
            ),
        )
    }

    val Diamond: ImageVector by lazy {
        strokeVector(
            "Diamond",
            listOf(
                "M6 5h12l3 5l-8.5 9.5a.7 .7 0 0 1 -1 0l-8.5 -9.5l3 -5",
            "M10 12l-2 -2.2l.6 -1"
            ),
        )
    }

    val Download: ImageVector by lazy {
        strokeVector(
            "Download",
            listOf(
                "M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2",
            "M7 11l5 5l5 -5",
            "M12 4l0 12"
            ),
        )
    }

    val Eye: ImageVector by lazy {
        strokeVector(
            "Eye",
            listOf(
                "M10 12a2 2 0 1 0 4 0a2 2 0 0 0 -4 0",
            "M21 12c-2.4 4 -5.4 6 -9 6c-3.6 0 -6.6 -2 -9 -6c2.4 -4 5.4 -6 9 -6c3.6 0 6.6 2 9 6"
            ),
        )
    }

    val EyeOff: ImageVector by lazy {
        strokeVector(
            "EyeOff",
            listOf(
                "M10.585 10.587a2 2 0 0 0 2.829 2.828",
            "M16.681 16.673a8.717 8.717 0 0 1 -4.681 1.327c-3.6 0 -6.6 -2 -9 -6c1.272 -2.12 2.712 -3.678 4.32 -4.674m2.86 -1.146a9.055 9.055 0 0 1 1.82 -.18c3.6 0 6.6 2 9 6c-.666 1.11 -1.379 2.067 -2.138 2.87",
            "M3 3l18 18"
            ),
        )
    }

    val FaceId: ImageVector by lazy {
        strokeVector(
            "FaceId",
            listOf(
                "M4 8v-2a2 2 0 0 1 2 -2h2",
            "M4 16v2a2 2 0 0 0 2 2h2",
            "M16 4h2a2 2 0 0 1 2 2v2",
            "M16 20h2a2 2 0 0 0 2 -2v-2",
            "M9 10h.01",
            "M15 10h.01",
            "M9.5 15a3.5 3.5 0 0 0 5 0"
            ),
        )
    }

    val Fingerprint: ImageVector by lazy {
        strokeVector(
            "Fingerprint",
            listOf(
                "M18.9 7a8 8 0 0 1 1.1 5v1a6 6 0 0 0 .8 3",
            "M8 11a4 4 0 0 1 8 0v1a10 10 0 0 0 2 6",
            "M12 11v2a14 14 0 0 0 2.5 8",
            "M8 15a18 18 0 0 0 1.8 6",
            "M4.9 19a22 22 0 0 1 -.9 -7v-1a8 8 0 0 1 12 -6.95"
            ),
        )
    }

    val GoldBars: ImageVector by lazy {
        strokeVector(
            "GoldBars",
            listOf(
                "M9.2 4.5h5.6l1.7 5.5h-9z",
            "M4.2 13.5h5.6l1.7 5.5h-9z",
            "M14.2 13.5h5.6l1.7 5.5h-9z"
            ),
        )
    }

    val GoldIngots: ImageVector get() = GoldBars

    val GoldinLogoMark: ImageVector by lazy {
        strokeVector(
            "GoldinLogoMark",
            listOf(
                "M9.2 4.5h5.6l1.7 5.5h-9z",
            "M4.2 13.5h5.6l1.7 5.5h-9z",
            "M14.2 13.5h5.6l1.7 5.5h-9z"
            ),
        )
    }

    val History: ImageVector by lazy {
        strokeVector(
            "History",
            listOf(
                "M12 8l0 4l2 2",
            "M3.05 11a9 9 0 1 1 .5 4m-.5 5v-5h5"
            ),
        )
    }

    val HistoryClock: ImageVector get() = History

    val Home: ImageVector by lazy {
        strokeVector(
            "Home",
            listOf(
                "M5 12h-2l9 -9l9 9h-2",
            "M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-7",
            "M9 21v-6a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v6"
            ),
        )
    }

    val IdCard: ImageVector by lazy {
        strokeVector(
            "IdCard",
            listOf(
                "M3 7a3 3 0 0 1 3 -3h12a3 3 0 0 1 3 3v10a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3z",
            "M7 10a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M15 8h2",
            "M15 12h2",
            "M7 16h10"
            ),
        )
    }

    val IdBadge: ImageVector get() = IdCard

    val Id: ImageVector get() = IdCard

    val InfoCircle: ImageVector by lazy {
        strokeVector(
            "InfoCircle",
            listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0",
            "M12 9h.01",
            "M11 12h1v4h1"
            ),
        )
    }

    val Loader2: ImageVector by lazy {
        strokeVector(
            "Loader2",
            listOf(
                "M12 3a9 9 0 1 0 9 9"
            ),
            strokeWidth = 2.5f,
        )
    }

    val Lock: ImageVector by lazy {
        strokeVector(
            "Lock",
            listOf(
                "M5 13a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-6z",
            "M11 16a1 1 0 1 0 2 0a1 1 0 0 0 -2 0",
            "M8 11v-4a4 4 0 1 1 8 0v4"
            ),
        )
    }

    val LockCheck: ImageVector by lazy {
        strokeVector(
            "LockCheck",
            listOf(
                "M11.5 21h-4.5a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v.5",
            "M11 16a1 1 0 1 0 2 0a1 1 0 0 0 -2 0",
            "M8 11v-4a4 4 0 1 1 8 0v4",
            "M15 19l2 2l4 -4"
            ),
        )
    }

    val LockFilled: ImageVector by lazy {
        fillVector(
            "LockFilled",
            listOf(
                "M12 2a5 5 0 0 1 5 5v3a3 3 0 0 1 3 3v6a3 3 0 0 1 -3 3h-10a3 3 0 0 1 -3 -3v-6a3 3 0 0 1 3 -3v-3a5 5 0 0 1 5 -5m0 12a2 2 0 0 0 -1.995 1.85l-.005 .15a2 2 0 1 0 2 -2m0 -10a3 3 0 0 0 -3 3v3h6v-3a3 3 0 0 0 -3 -3"
            ),
        )
    }

    val Logout: ImageVector by lazy {
        strokeVector(
            "Logout",
            listOf(
                "M14 8v-2a2 2 0 0 0 -2 -2h-7a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h7a2 2 0 0 0 2 -2v-2",
            "M9 12h12l-3 -3",
            "M18 15l3 -3"
            ),
        )
    }

    val Mail: ImageVector by lazy {
        strokeVector(
            "Mail",
            listOf(
                "M3 7a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-10",
            "M3 7l9 6l9 -6"
            ),
        )
    }

    val MapPin: ImageVector by lazy {
        strokeVector(
            "MapPin",
            listOf(
                "M9 11a3 3 0 1 0 6 0a3 3 0 0 0 -6 0",
            "M17.657 16.657l-4.243 4.243a2 2 0 0 1 -2.827 0l-4.244 -4.243a8 8 0 1 1 11.314 0z"
            ),
        )
    }

    val SmsMessage: ImageVector by lazy {
        strokeVector(
            "SmsMessage",
            listOf(
                "M8 9h8",
            "M8 13h6",
            "M18 4a3 3 0 0 1 3 3v8a3 3 0 0 1 -3 3h-5l-5 3v-3h-2a3 3 0 0 1 -3 -3v-8a3 3 0 0 1 3 -3h12z"
            ),
        )
    }

    val Message: ImageVector get() = SmsMessage

    val PackageBox: ImageVector by lazy {
        strokeVector(
            "PackageBox",
            listOf(
                "M12 3l8 4.5l0 9l-8 4.5l-8 -4.5l0 -9l8 -4.5",
            "M12 12l8 -4.5",
            "M12 12l0 9",
            "M12 12l-8 -4.5",
            "M16 5.25l-8 4.5"
            ),
        )
    }

    val Plus: ImageVector by lazy {
        strokeVector(
            "Plus",
            listOf(
                "M12 5l0 14",
            "M5 12l14 0"
            ),
        )
    }

    val Printer: ImageVector by lazy {
        strokeVector(
            "Printer",
            listOf(
                "M17 17h2a2 2 0 0 0 2 -2v-4a2 2 0 0 0 -2 -2h-14a2 2 0 0 0 -2 2v4a2 2 0 0 0 2 2h2",
            "M17 9v-4a2 2 0 0 0 -2 -2h-6a2 2 0 0 0 -2 2v4",
            "M7 13m0 2a2 2 0 0 1 2 -2h6a2 2 0 0 1 2 2v4a2 2 0 0 1 -2 2h-6a2 2 0 0 1 -2 -2z"
            ),
        )
    }

    val Receipt: ImageVector by lazy {
        strokeVector(
            "Receipt",
            listOf(
                "M5 21v-16a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v16l-3 -2l-2 2l-2 -2l-2 2l-2 -2l-3 2",
            "M9 7h6",
            "M9 11h6",
            "M9 15h4"
            ),
        )
    }

    val ReceiptMoney: ImageVector by lazy {
        strokeVector(
            "ReceiptMoney",
            listOf(
                "M5 21v-16a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v16l-3 -2l-2 2l-2 -2l-2 2l-2 -2l-3 2",
            "M14 8h-2.5a1.5 1.5 0 0 0 0 3h1a1.5 1.5 0 0 1 0 3h-2.5m2 0v1.5m0 -9v1.5"
            ),
        )
    }

    val Receipt2: ImageVector get() = ReceiptMoney

    val Refresh: ImageVector by lazy {
        strokeVector(
            "Refresh",
            listOf(
                "M20 11a8.1 8.1 0 0 0 -15.5 -2m-.5 -4v4h4",
            "M4 13a8.1 8.1 0 0 0 15.5 2m.5 4v-4h-4"
            ),
        )
    }

    val Repeat: ImageVector by lazy {
        strokeVector(
            "Repeat",
            listOf(
                "M4 12v-3a3 3 0 0 1 3 -3h13m-3 -3l3 3l-3 3",
            "M20 12v3a3 3 0 0 1 -3 3h-13m3 3l-3 -3l3 -3"
            ),
        )
    }

    val Search: ImageVector by lazy {
        strokeVector(
            "Search",
            listOf(
                "M3 10a7 7 0 1 0 14 0a7 7 0 1 0 -14 0",
            "M21 21l-6 -6"
            ),
        )
    }

    val ShieldCheck: ImageVector by lazy {
        strokeVector(
            "ShieldCheck",
            listOf(
                "M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3",
            "M9 12l2 2l4 -4"
            ),
        )
    }

    val ShieldLock: ImageVector by lazy {
        strokeVector(
            "ShieldLock",
            listOf(
                "M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3",
            "M11 11a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
            "M12 12l0 2.5"
            ),
        )
    }

    val ShoppingCart: ImageVector by lazy {
        strokeVector(
            "ShoppingCart",
            listOf(
                "M4 19a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M15 19a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M17 17h-11v-14h-2",
            "M6 5l14 1l-1 7h-13"
            ),
        )
    }

    val Cart: ImageVector get() = ShoppingCart

    val ShoppingCartAlt: ImageVector by lazy {
        strokeVector(
            "ShoppingCartAlt",
            listOf(
                "M6 19m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M17 19m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M17 17h-11v-14h-2",
            "M6 5l14 1l-1 7h-13"
            ),
        )
    }

    val PriceTag: ImageVector by lazy {
        strokeVector(
            "PriceTag",
            listOf(
                "M5.5 7.5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
            "M3 6v5.172a2 2 0 0 0 .586 1.414l7.71 7.71a2.41 2.41 0 0 0 3.408 0l5.592 -5.592a2.41 2.41 0 0 0 0 -3.408l-7.71 -7.71a2 2 0 0 0 -1.414 -.586h-5.172a3 3 0 0 0 -3 3z"
            ),
        )
    }

    val Tag: ImageVector get() = PriceTag

    val Trash: ImageVector by lazy {
        strokeVector(
            "Trash",
            listOf(
                "M4 7l16 0",
            "M10 11l0 6",
            "M14 11l0 6",
            "M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2 -2l1 -12",
            "M9 7v-3a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v3"
            ),
        )
    }

    val TruckDelivery: ImageVector by lazy {
        strokeVector(
            "TruckDelivery",
            listOf(
                "M5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M15 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M5 17h-2v-11a1 1 0 0 1 1 -1h9v12m-4 0h6m4 0h2v-6h-8m0 -5h5l3 5"
            ),
        )
    }

    val Truck: ImageVector get() = TruckDelivery

    val User: ImageVector by lazy {
        strokeVector(
            "User",
            listOf(
                "M8 7a4 4 0 1 0 8 0a4 4 0 0 0 -8 0",
            "M6 21v-2a4 4 0 0 1 4 -4h4a4 4 0 0 1 4 4v2"
            ),
        )
    }

    val UserCircle: ImageVector by lazy {
        strokeVector(
            "UserCircle",
            listOf(
                "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
            "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
            "M6.168 18.849a4 4 0 0 1 3.832 -2.849h4a4 4 0 0 1 3.834 2.855"
            ),
        )
    }

    val Wallet: ImageVector by lazy {
        strokeVector(
            "Wallet",
            listOf(
                "M17 8v-3a1 1 0 0 0 -1 -1h-10a2 2 0 0 0 0 4h12a1 1 0 0 1 1 1v3m0 4v3a1 1 0 0 1 -1 1h-12a2 2 0 0 1 -2 -2v-12",
            "M20 12v4h-4a2 2 0 0 1 0 -4h4"
            ),
        )
    }

    val Close: ImageVector by lazy {
        strokeVector(
            "Close",
            listOf(
                "M18 6l-12 12",
            "M6 6l12 12"
            ),
        )
    }

    val XClose: ImageVector get() = Close

    val X: ImageVector get() = Close

    val Minus: ImageVector by lazy {
        strokeVector("Minus", listOf("M5 12l14 0"))
    }

    val QrCode: ImageVector by lazy {
        strokeVector(
            "QrCode",
            listOf(
                "M4 5a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1z",
                "M7 7l0 .01",
                "M14 5a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1z",
                "M17 7l0 .01",
                "M4 15a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1z",
                "M7 17l0 .01",
                "M14 14l3 0",
                "M20 14l0 .01",
                "M14 14l0 3",
                "M14 20l3 0",
                "M17 17l3 0",
                "M20 17l0 3",
            ),
        )
    }
}
