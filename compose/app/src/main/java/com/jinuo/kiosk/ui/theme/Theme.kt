package com.jinuo.kiosk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jinuo.kiosk.R

/* Exact artboard tokens (from 05-custom.html / kiosk.css). */
val Ink = Color(0xFF3A2A1E)
val Cream = Color(0xFFFFFDF9)
val WarmSand = Color(0xFFFBF8F3)
val ChipBg = Color(0xFFF3EDE4)
val Caramel = Color(0xFFC07A3E)
val PriceRed = Color(0xFFA0002D)
val Muted = Color(0xFF7C6650)
val Outer = Color(0xFF1A120B)
val Hairline = Color(0x1A3A2A1E)          // ink @ 10%
val SoftBorder = Color(0x263A2A1E)        // ink @ 15%
val CardShadow = Color(0x263A2A1E)
val Scrim = Color(0x8C3A2A1E)              // ink @ 55%
val ModalShadow = Color(0x473A2A1E)        // ink @ 28%

/** Fraunces: display face for titles, option labels and prices. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_400, FontWeight.W400),
    Font(R.font.fraunces_500, FontWeight.W500),
    Font(R.font.fraunces_600, FontWeight.W600),
)

/** Instrument Sans: UI face for status, labels, badges and CTAs. */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_400, FontWeight.W400),
    Font(R.font.instrument_sans_600, FontWeight.W600),
)

@Composable
fun KioskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Caramel,
            background = Cream,
            onBackground = Ink,
            surface = Cream,
            onSurface = Ink,
        ),
        content = content,
    )
}
