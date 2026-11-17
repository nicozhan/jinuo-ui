package com.jinuo.kiosk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinuo.kiosk.R
import com.jinuo.kiosk.ui.theme.Caramel
import com.jinuo.kiosk.ui.theme.Cream
import com.jinuo.kiosk.ui.theme.Fraunces
import com.jinuo.kiosk.ui.theme.Hairline
import com.jinuo.kiosk.ui.theme.Ink
import com.jinuo.kiosk.ui.theme.InstrumentSans
import com.jinuo.kiosk.ui.theme.Muted
import com.jinuo.kiosk.ui.theme.Scrim
import com.jinuo.kiosk.ui.theme.SoftBorder
import com.jinuo.kiosk.ui.theme.WarmSand

/* ===========================================================================
   The five remaining HTML pages, ported 1:1 to Compose:
     02-lang · 03-help · 15-wrong · 17-making · 18-ready
   All geometry is 1080x1920 design pixels; see Artboard. Interface is
   English-only per project convention (the Figma zh sub-lines are rendered
   in English).
   =========================================================================== */

/* ---------------------------- 02-lang ----------------------------------- */
private data class LangSlide(val eyebrow: String, val headline: String)
private val LANG_SLIDES = listOf(
    LangSlide("JAVA · FRESH DAILY", "Snacks for the\nnext move."),
    LangSlide("JAVA · CRAFTED DAILY", "Brewed for\nthe moment."),
    LangSlide("JAVA · SERVED FRESH", "A better cup,\nevery time."),
)

/**
 * 02-language: an entry carousel that mirrors 01-home but whose language
 * pill is the focus. Choosing a language advances to shopping (matching
 * chooseLang() in kiosk.js, which routes home/lang → 04-shopping).
 */
@Composable
fun LangScreen(
    modifier: Modifier = Modifier,
    onStart: () -> Unit = {},
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    Artboard(modifier = modifier) {
        StatusText("MACHINE ID · JAV-08-221", 64f, 300f)
        StatusText("SUPPORT · +86 400 820 2006", 351f, 340f)
        StatusText("14:00", 924f, 100f)
        Box(Modifier.offset(0.dp, 66.dp).size(ART_W, 1.dp).background(Hairline))

        Image(
            painter = painterResource(R.drawable.kiosk_logo),
            contentDescription = "Jinuo Coffee",
            modifier = Modifier.offset(440.dp, 106.dp).size(200.dp, 94.dp),
            contentScale = ContentScale.Fit,
        )

        var slide by remember { mutableStateOf(0) }
        Box(
            modifier = Modifier.offset(64.dp, 240.dp).size(952.dp, 952.dp)
                .clip(RoundedCornerShape(28.dp)).background(Cream),
        ) {
            Image(
                painter = painterResource(R.drawable.kiosk_banner),
                contentDescription = null,
                modifier = Modifier.size(952.dp, 952.dp),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.offset(0.dp, 600.dp).size(952.dp, 352.dp)
                .background(Color(0x99000000)))
            Text(LANG_SLIDES[slide].eyebrow,
                modifier = Modifier.offset(48.dp, 715.dp).size(856.dp, 30.dp),
                fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
                fontSize = 19.sp, letterSpacing = 4.56.sp, color = Cream)
            Text(LANG_SLIDES[slide].headline,
                modifier = Modifier.offset(48.dp, 752.dp).size(800.dp, 170.dp),
                fontFamily = Fraunces, fontWeight = FontWeight.W600,
                fontSize = 72.sp, lineHeight = 76.sp, color = Cream)
        }
        Box(Modifier.offset(88.dp, 684.dp).size(64.dp, 64.dp).clip(CircleShape)
            .background(Cream.copy(alpha = 0.94f))
            .clickable { slide = (slide + LANG_SLIDES.size - 1) % LANG_SLIDES.size },
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_chevron_left), "Previous", Modifier.size(28.dp))
        }
        Box(Modifier.offset(928.dp, 684.dp).size(64.dp, 64.dp).clip(CircleShape)
            .background(Cream.copy(alpha = 0.94f))
            .clickable { slide = (slide + 1) % LANG_SLIDES.size },
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_chevron_right), "Next", Modifier.size(28.dp))
        }
        Row(Modifier.offset(0.dp, 1218.dp).size(ART_W, 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            LANG_SLIDES.indices.forEach { i ->
                Box(Modifier.size(if (i == slide) 36.dp else 12.dp, 10.dp).clip(CircleShape)
                    .background(if (i == slide) Caramel else Ink.copy(alpha = 0.55f)))
                if (i < LANG_SLIDES.size - 1) Box(Modifier.size(12.dp, 1.dp))
            }
        }

        Text("ACCEPTED PAYMENTS",
            modifier = Modifier.offset(190.dp, 1307.dp).size(700.dp, 34.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
            fontSize = 28.sp, letterSpacing = 3.2.sp, color = Muted)
        LangPayCard(64f, R.drawable.ic_card, "Card")
        LangPayCard(388f, R.drawable.ic_tap, "Tap")
        LangPayCard(712f, R.drawable.ic_qr, "QR Pay")

        Box(Modifier.offset(64.dp, 1582.dp).size(952.dp, 90.dp)
            .clip(RoundedCornerShape(22.dp)).background(Ink).clickable { onStart() },
            contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start Shopping", fontFamily = Fraunces, fontWeight = FontWeight.W600,
                    fontSize = 34.sp, color = Cream)
                Text("→", fontFamily = InstrumentSans, fontSize = 34.sp, color = Cream,
                    modifier = Modifier.offset(16.dp, 0.dp))
            }
        }

        // Bottom pills: the language pill is active/primary on this page.
        Box(Modifier.offset(60.dp, 1755.dp).size(480.dp, 118.dp)
            .clip(RoundedCornerShape(20.dp)).background(WarmSand)
            .border(1.dp, Caramel.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onLang() }, contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83C\uDDEC\uD83C\uDDE7", fontSize = 30.sp)
                Text("English", modifier = Modifier.offset(16.dp, 0.dp),
                    fontFamily = Fraunces, fontWeight = FontWeight.W600,
                    fontSize = 30.sp, color = Ink)
                Text("⌄", modifier = Modifier.offset(20.dp, (-4).dp),
                    fontFamily = InstrumentSans, fontSize = 30.sp, color = Caramel)
            }
        }
        Box(Modifier.offset(556.dp, 1755.dp).size(464.dp, 118.dp)
            .clip(RoundedCornerShape(20.dp)).background(Cream)
            .border(1.dp, SoftBorder, RoundedCornerShape(20.dp))
            .clickable { onHelp() }, contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_help), "Help", Modifier.size(34.dp))
                Text("Help", modifier = Modifier.offset(16.dp, 0.dp),
                    fontFamily = Fraunces, fontWeight = FontWeight.W600,
                    fontSize = 30.sp, color = Ink)
            }
        }

        if (onBack != null) {
            Box(Modifier.offset(45.dp, 130.dp).size(90.dp, 64.dp).clip(CircleShape)
                .background(Cream).border(1.dp, SoftBorder, CircleShape)
                .clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontFamily = InstrumentSans, fontSize = 30.sp, color = Ink)
            }
        }
    }
}

@Composable
private fun LangPayCard(x: Float, icon: Int, label: String) {
    Box(Modifier.offset(x.dp, 1390.dp).size(304.dp, 112.dp)
        .clip(RoundedCornerShape(20.dp)).background(Cream)
        .border(1.dp, Hairline, RoundedCornerShape(20.dp))) {
        Image(painterResource(icon), null, Modifier.offset(28.dp, 36.dp).size(40.dp, 40.dp))
        Text(label, modifier = Modifier.offset(84.dp, 38.dp).size(200.dp, 40.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 28.sp, color = Ink)
    }
}

/* ---------------------------- 03-help ----------------------------------- */
/** Support modal: dimmed background + white dialog, matching the Figma panel. */
@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
) {
    Artboard(modifier = modifier) {
        // dim the underlying screen
        Box(Modifier.size(ART_W, ART_H).background(Scrim).clickable { onClose() })

        // dialog 150,509 780x901 radius 30
        Box(Modifier.offset(150.dp, 510.dp).size(780.dp, 900.dp)
            .clip(RoundedCornerShape(30.dp)).background(Cream)) {
            // orange top hairline
            Box(Modifier.fillMaxWidth().size(ART_W, 6.dp).background(Caramel))

            Text("NEED ASSISTANCE?",
                modifier = Modifier.offset(54.dp, 50.dp).size(560.dp, 24.dp),
                fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
                fontSize = 16.sp, letterSpacing = 2.88.sp, color = Caramel)
            Text("We're here to help.",
                modifier = Modifier.offset(54.dp, 86.dp).size(600.dp, 62.dp),
                fontFamily = Fraunces, fontWeight = FontWeight.W600,
                fontSize = 48.sp, color = Ink)
            Box(Modifier.offset(54.dp, 172.dp).size(672.dp, 1.dp)
                .background(Ink.copy(alpha = 0.10f)))

            // QR + contact block
            Box(Modifier.offset(54.dp, 205.dp).size(180.dp, 180.dp)
                .clip(RoundedCornerShape(16.dp)).background(WarmSand)
                .border(1.dp, Hairline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.help_qr), "Support QR",
                    Modifier.size(150.dp, 150.dp), contentScale = ContentScale.Fit)
            }
            Text("Scan to connect",
                modifier = Modifier.offset(54.dp, 395.dp).size(180.dp, 30.dp),
                fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
                fontSize = 20.sp, color = Ink, textAlign = TextAlign.Center)

            // phone / email rows (right column)
            HelpInfoRow(270f, 205f, "PHONE", "+86 400 820 2006", 210f)
            HelpInfoRow(270f, 278f, "EMAIL", "support@1910coffee.com", 300f)

            Box(Modifier.offset(54.dp, 451.dp).size(672.dp, 1.dp)
                .background(Ink.copy(alpha = 0.10f)))
            HelpInfoRow(54f, 479f, "ORDER NUMBER", "#ORD-20260818-8841", 340f)

            Box(Modifier.offset(54.dp, 553.dp).size(672.dp, 1.dp)
                .background(Ink.copy(alpha = 0.10f)))
            HelpInfoRow(54f, 581f, "MACHINE ADDRESS",
                "Rämistrasse 101, 8092 Zurich, Switzerland", 560f)

            // OK button
            Box(Modifier.offset(54.dp, 747.dp).size(672.dp, 90.dp)
                .clip(RoundedCornerShape(9999.dp)).background(Ink)
                .clickable { onClose() }, contentAlignment = Alignment.Center) {
                Text("OK", fontFamily = Fraunces, fontWeight = FontWeight.W600,
                    fontSize = 30.sp, color = Cream)
            }

            // close X
            Box(Modifier.offset(696.dp, 24.dp).size(48.dp, 48.dp).clip(CircleShape)
                .background(WarmSand).clickable { onClose() },
                contentAlignment = Alignment.Center) {
                Text("×", fontFamily = InstrumentSans, fontSize = 38.sp,
                    fontWeight = FontWeight.W300, color = Muted)
            }
        }
    }
}

@Composable
private fun HelpInfoRow(x: Float, y: Float, label: String, value: String, labelW: Float) {
    Text(label, modifier = Modifier.offset(x.dp, y.dp).size(labelW.dp, 22.dp),
        fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
        fontSize = 20.sp, letterSpacing = 2.24.sp, color = Muted)
    Text(value, modifier = Modifier.offset(x.dp, (y + 26).dp).size(600.dp, 30.dp),
        fontFamily = InstrumentSans, fontSize = 24.sp, color = Ink)
}

/* ---------------------------- 15-wrong ---------------------------------- */
/** Error / status page with concentric rings and two actions. */
@Composable
fun WrongScreen(
    modifier: Modifier = Modifier,
    onHelp: () -> Unit = {},
    onHome: () -> Unit = {},
) {
    Artboard(modifier = modifier) {
        StatusText("MACHINE ID · JAV-08-221", 64f, 300f)
        StatusText("SUPPORT · +86 400 820 2006", 359f, 360f)
        StatusText("TIME · 14:20", 865f, 151f)
        Box(Modifier.offset(0.dp, 66.dp).size(ART_W, 1.dp).background(Hairline))

        Text("ERROR / STATUS",
            modifier = Modifier.offset(64.dp, 114.dp).size(400.dp, 29.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
            fontSize = 19.sp, letterSpacing = 3.8.sp, color = Caramel)
        Text("Something went wrong.",
            modifier = Modifier.offset(64.dp, 143.dp).size(952.dp, 60.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600,
            fontSize = 48.sp, color = Ink)
        Text("Please try again.",
            modifier = Modifier.offset(64.dp, 234.dp).size(952.dp, 44.dp),
            fontFamily = InstrumentSans, fontSize = 32.sp, color = Muted)

        // concentric error rings, centered at x=540
        ErrorRings(cx = 540f, cy = 878f)
        // circle with a diagonal "error" slash (matches the Figma symbol)
        androidx.compose.foundation.Canvas(
            modifier = Modifier.offset(480.dp, 818.dp).size(120.dp, 120.dp)
        ) {
            val r = size.minDimension / 2f
            drawCircle(color = Ink, radius = r - 8f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f))
            val m = 34f
            drawLine(color = Ink,
                start = androidx.compose.ui.geometry.Offset(m, size.height - m),
                end = androidx.compose.ui.geometry.Offset(size.width - m, m),
                strokeWidth = 8f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }

        Text("Something went wrong.",
            modifier = Modifier.offset(240.dp, 1150.dp).size(600.dp, 50.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600,
            fontSize = 46.sp, color = Ink, textAlign = TextAlign.Center)
        Text("Please try again.",
            modifier = Modifier.offset(240.dp, 1210.dp).size(600.dp, 40.dp),
            fontFamily = InstrumentSans, fontSize = 26.sp, color = Muted,
            textAlign = TextAlign.Center)

        // primary HELP button 220,1224 640x92
        Box(Modifier.offset(220.dp, 1290.dp).size(640.dp, 92.dp)
            .clip(RoundedCornerShape(24.dp)).background(Ink)
            .clickable { onHelp() }, contentAlignment = Alignment.Center) {
            Text("HELP", fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
                fontSize = 28.sp, color = Cream)
        }
        // secondary Return to Menu
        Box(Modifier.offset(220.dp, 1408.dp).size(640.dp, 92.dp)
            .clip(RoundedCornerShape(24.dp)).background(Cream)
            .border(1.dp, Ink.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .clickable { onHome() }, contentAlignment = Alignment.Center) {
            Text("← Return to Menu", fontFamily = Fraunces,
                fontWeight = FontWeight.W600, fontSize = 28.sp, color = Ink)
        }

        Text("© 2026 VendSolution. ALL RIGHTS RESERVED.",
            modifier = Modifier.offset(288.dp, 1829.dp).size(503.dp, 24.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
            fontSize = 18.sp, letterSpacing = 1.6.sp,
            color = Muted.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorRings(cx: Float, cy: Float) {
    Box(Modifier.offset((cx - 192).dp, (cy - 192).dp).size(384.dp, 384.dp)
        .clip(CircleShape).border(1.dp, Caramel.copy(alpha = 0.12f), CircleShape)
        .background(Caramel.copy(alpha = 0.04f)))
    Box(Modifier.offset((cx - 166).dp, (cy - 166).dp).size(332.dp, 332.dp)
        .clip(CircleShape).border(1.dp, Caramel.copy(alpha = 0.20f), CircleShape)
        .background(Caramel.copy(alpha = 0.07f)))
    Box(Modifier.offset((cx - 140).dp, (cy - 140).dp).size(280.dp, 280.dp)
        .clip(CircleShape).background(WarmSand))
}

/* ---------------------------- 17-making --------------------------------- */
private val BREW_STEPS = listOf("01 · PREPARE", "02 · BREW", "03 · MIX", "04 · SERVE")
private val GRADIENT = Brush.horizontalGradient(
    listOf(Color(0xFF5A4432), Color(0xFF684B34), Color(0xFF765337), Color(0xFF845B39),
        Color(0xFF93623A), Color(0xFFA26A3C), Color(0xFFB1723D), Color(0xFFC07A3E),
        Color(0xFFB1723D), Color(0xFFA26A3C), Color(0xFF93623A), Color(0xFF845B39),
        Color(0xFF765337), Color(0xFF684B34), Color(0xFF5A4432))
)

/** Preparing page: centred logo, brew photo and a four-stage progress strip. */
@Composable
fun MakingScreen(
    modifier: Modifier = Modifier,
    stage: Int = 0,
    onDone: () -> Unit = {},
) {
    Artboard(modifier = modifier) {
        StatusText("MACHINE ID · JAV-08-221", 64f, 300f)
        StatusText("SUPPORT · +86 400 820 2006", 443f, 272f)
        StatusText("TIME · 14:05", 865f, 151f)
        Box(Modifier.offset(0.dp, 66.dp).size(ART_W, 1.dp).background(Hairline))

        // centred brand logo 433.5,98 213x80
        Image(painterResource(R.drawable.kiosk_logo), "Jinuo Coffee",
            Modifier.offset(433.dp, 98.dp).size(213.dp, 80.dp), contentScale = ContentScale.Fit)

        Text("Preparing your coffee…",
            modifier = Modifier.offset(64.dp, 226.dp).size(952.dp, 52.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600,
            fontSize = 40.sp, letterSpacing = 0.4.sp, color = Ink)
        Text("Please wait a moment",
            modifier = Modifier.offset(64.dp, 282.dp).size(952.dp, 50.dp),
            fontFamily = InstrumentSans, fontSize = 40.sp, color = Muted.copy(alpha = 0.8f))

        // brew photo 140,661 800x800 (matches Figma nested offset)
        Box(Modifier.offset(140.dp, 661.dp).size(800.dp, 800.dp)
            .clip(RoundedCornerShape(28.dp)).background(WarmSand),
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.brew_coffee), "Brewing coffee",
                Modifier.size(800.dp, 800.dp), contentScale = ContentScale.Crop)
        }

        // four-stage strip
        Text("PREPARING YOUR CUP · $stage/4",
            modifier = Modifier.offset(64.dp, 1656.dp).size(952.dp, 34.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
            fontSize = 24.sp, letterSpacing = 2.4.sp, color = Muted,
            textAlign = TextAlign.Center)
        BrewStrip(stage = stage, filled = false)

        // tap anywhere to advance (mirrors the HTML auto-flow / skip)
        Box(Modifier.offset(0.dp, 0.dp).size(ART_W, 300.dp).clickable { onDone() })
    }
}

@Composable
internal fun BrewStrip(stage: Int, filled: Boolean) {
    Row(Modifier.offset(64.dp, 1705.dp).size(952.dp, 120.dp)) {
        BREW_STEPS.forEachIndexed { i, label ->
            Box(Modifier.size(238.dp, 120.dp)) {
                val barOn = filled || i < stage
                Box(Modifier.offset(0.dp, 0.dp).size(220.dp, 8.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .then(if (barOn) Modifier.background(GRADIENT)
                          else Modifier.background(WarmSand)))
                Text(label, modifier = Modifier.offset(0.dp, 20.dp).size(220.dp, 30.dp),
                    fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
                    fontSize = 18.sp, letterSpacing = 1.2.sp, color = Muted)
            }
        }
    }
}

/* ---------------------------- 18-ready ---------------------------------- */
/** Ready page: celebration gradient title, brew photo, steps, new-order CTA. */
@Composable
fun ReadyScreen(
    modifier: Modifier = Modifier,
    onNewOrder: () -> Unit = {},
) {
    Artboard(modifier = modifier) {
        StatusText("MACHINE ID · JAV-08-221", 64f, 300f)
        StatusText("SUPPORT · +86 400 820 2006", 443f, 272f)
        StatusText("TIME · 14:06", 865f, 151f)
        Box(Modifier.offset(0.dp, 66.dp).size(ART_W, 1.dp).background(Hairline))

        Image(painterResource(R.drawable.kiosk_logo), "Jinuo Coffee",
            Modifier.offset(433.dp, 98.dp).size(213.dp, 80.dp), contentScale = ContentScale.Fit)

        Text("YOUR COFFEE IS READY",
            modifier = Modifier.offset(64.dp, 200.dp).size(952.dp, 53.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600,
            fontSize = 50.sp, letterSpacing = 0.5.sp,
            color = Ink.copy(alpha = 0.36f))
        Text("Please collect your cup",
            modifier = Modifier.offset(64.dp, 260.dp).size(952.dp, 45.dp),
            fontFamily = InstrumentSans, fontSize = 40.sp,
            color = Muted.copy(alpha = 0.6f))
        Box(Modifier.offset(248.dp, 312.dp).size(584.dp, 8.dp)
            .clip(RoundedCornerShape(9999.dp)).background(GRADIENT))

        // brew photo 140,626 800x800 (matches Figma nested offset)
        Box(Modifier.offset(140.dp, 626.dp).size(800.dp, 800.dp)
            .clip(RoundedCornerShape(28.dp)).background(WarmSand),
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.brew_coffee), "Ready drink",
                Modifier.size(800.dp, 800.dp), contentScale = ContentScale.Crop)
        }

        // four completed steps + gradient rule
        BrewStrip(stage = 4, filled = true)

        Box(Modifier.offset(64.dp, 1800.dp).size(952.dp, 90.dp)
            .clip(RoundedCornerShape(22.dp)).background(Ink)
            .clickable { onNewOrder() }, contentAlignment = Alignment.Center) {
            Text("Start a New Order", fontFamily = Fraunces,
                fontWeight = FontWeight.W600, fontSize = 32.sp, color = Cream)
        }
    }
}

/* --------------------------- previews ----------------------------------- */
@androidx.compose.ui.tooling.preview.Preview(name = "02 Lang", widthDp = 540, heightDp = 960, showBackground = true)
@Composable private fun PreviewLang() { com.jinuo.kiosk.ui.theme.KioskTheme { LangScreen() } }

@androidx.compose.ui.tooling.preview.Preview(name = "03 Help", widthDp = 540, heightDp = 960, showBackground = true)
@Composable private fun PreviewHelp() { com.jinuo.kiosk.ui.theme.KioskTheme { HelpScreen() } }

@androidx.compose.ui.tooling.preview.Preview(name = "15 Wrong", widthDp = 540, heightDp = 960, showBackground = true)
@Composable private fun PreviewWrong() { com.jinuo.kiosk.ui.theme.KioskTheme { WrongScreen() } }

@androidx.compose.ui.tooling.preview.Preview(name = "17 Making", widthDp = 540, heightDp = 960, showBackground = true)
@Composable private fun PreviewMaking() { com.jinuo.kiosk.ui.theme.KioskTheme { MakingScreen() } }

@androidx.compose.ui.tooling.preview.Preview(name = "18 Ready", widthDp = 540, heightDp = 960, showBackground = true)
@Composable private fun PreviewReady() { com.jinuo.kiosk.ui.theme.KioskTheme { ReadyScreen() } }
