package com.jinuo.kiosk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.jinuo.kiosk.ui.theme.SoftBorder

private data class Slide(val eyebrow: String, val headline: String)

private val SLIDES = listOf(
    Slide("JAVA · FRESH DAILY", "Snacks for the\nnext move."),
    Slide("JAVA · CRAFTED DAILY", "Brewed for\nthe moment."),
    Slide("JAVA · SERVED FRESH", "A better cup,\nevery time."),
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    langLabel: String = "English",
    langFlag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    onStart: () -> Unit = {},
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
) {
    Artboard(modifier = modifier) {
        // status row
        StatusText("MACHINE ID · JAV-08-221", 64f, 300f)
        StatusText("SUPPORT · +86 400 820 2006", 351f, 340f)
        StatusText("14:00", 924f, 100f)
        Box(Modifier.offset(0.dp, 66.dp).size(ART_W, 1.dp).background(Hairline))

        // logo 64,73 136x64
        Image(
            painter = painterResource(R.drawable.kiosk_logo),
            contentDescription = "Jinuo Coffee",
            modifier = Modifier.offset(64.dp, 73.dp).size(136.dp, 64.dp),
            contentScale = ContentScale.Fit,
        )

        var slide by remember { mutableStateOf(0) }

        // hero banner 64,240 952x952
        Box(
            modifier = Modifier.offset(64.dp, 240.dp).size(952.dp, 952.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Cream),
        ) {
            Image(
                painter = painterResource(R.drawable.kiosk_banner),
                contentDescription = null,
                modifier = Modifier.size(952.dp, 952.dp),
                contentScale = ContentScale.Crop,
            )
            // dark gradient at the bottom for text legibility
            Box(
                Modifier.offset(0.dp, 600.dp).size(952.dp, 352.dp)
                    .background(androidx.compose.ui.graphics.Color(0x99000000)),
            )

            // eyebrow + headline near the bottom of the banner
            Text(
                SLIDES[slide].eyebrow,
                modifier = Modifier.offset(48.dp, 715.dp).size(856.dp, 30.dp),
                fontFamily = InstrumentSans,
                fontWeight = FontWeight.W600,
                fontSize = 19.sp,
                letterSpacing = 4.56.sp,
                color = Cream,
            )
            Text(
                SLIDES[slide].headline,
                modifier = Modifier.offset(48.dp, 752.dp).size(800.dp, 170.dp),
                fontFamily = Fraunces,
                fontWeight = FontWeight.W600,
                fontSize = 72.sp,
                lineHeight = 76.sp,
                color = Cream,
            )

            // prev / next chevrons
        }
        Box(
            modifier = Modifier.offset(88.dp, 684.dp).size(64.dp, 64.dp)
                .clip(CircleShape)
                .background(Cream.copy(alpha = 0.94f))
                .clickable { slide = (slide + SLIDES.size - 1) % SLIDES.size },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "Previous",
                modifier = Modifier.size(28.dp),
            )
        }
        Box(
            modifier = Modifier.offset(928.dp, 684.dp).size(64.dp, 64.dp)
                .clip(CircleShape)
                .background(Cream.copy(alpha = 0.94f))
                .clickable { slide = (slide + 1) % SLIDES.size },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = "Next",
                modifier = Modifier.size(28.dp),
            )
        }

        // carousel dots
        Row(
            modifier = Modifier.offset(0.dp, 1218.dp).size(ART_W, 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SLIDES.indices.forEach { i ->
                Box(
                    Modifier.size(if (i == slide) 36.dp else 12.dp, 10.dp)
                        .clip(CircleShape)
                        .background(if (i == slide) Caramel else Ink.copy(alpha = 0.55f)),
                )
                if (i < SLIDES.size - 1) {
                    Box(Modifier.size(12.dp, 1.dp))
                }
            }
        }

        // Accepted Payments
        Text(
            "ACCEPTED PAYMENTS",
            modifier = Modifier.offset(190.dp, 1307.dp).size(700.dp, 34.dp),
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.W600,
            fontSize = 28.sp,
            letterSpacing = 3.2.sp,
            color = Muted,
        )
        PayCard(64f, R.drawable.ic_card, "Card")
        PayCard(388f, R.drawable.ic_tap, "Tap")
        PayCard(712f, R.drawable.ic_qr, "QR Pay")

        // Start Shopping CTA 64,1582 952x90
        Box(
            modifier = Modifier.offset(64.dp, 1582.dp).size(952.dp, 90.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Ink)
                .clickable { onStart() },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Start Shopping",
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.W600,
                    fontSize = 34.sp,
                    color = Cream,
                )
                Text("→", fontFamily = InstrumentSans, fontSize = 34.sp, color = Cream,
                    modifier = Modifier.offset(16.dp, 0.dp))
            }
        }

        // bottom language / help buttons
        BottomPill(64f, langFlag, langLabel, onLang)
        BottomPill(550f, "?", "Help", onHelp)
    }
}

@Composable
private fun PayCard(x: Float, icon: Int, label: String) {
    Box(
        modifier = Modifier.offset(x.dp, 1390.dp).size(304.dp, 112.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Cream)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp)),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.offset(28.dp, 36.dp).size(40.dp, 40.dp),
        )
        Text(
            label,
            modifier = Modifier.offset(84.dp, 38.dp).size(200.dp, 40.dp),
            fontFamily = Fraunces,
            fontWeight = FontWeight.W600,
            fontSize = 28.sp,
            color = Ink,
        )
    }
}

@Composable
private fun BottomPill(x: Float, icon: String, label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier.offset(x.dp, 1765.dp).size(466.dp, 86.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Cream)
            .border(1.dp, SoftBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.offset(30.dp, 25.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 30.sp, color = Caramel)
            Text(
                label,
                modifier = Modifier.offset(20.dp, 0.dp),
                fontFamily = Fraunces,
                fontWeight = FontWeight.W600,
                fontSize = 32.sp,
                color = Ink,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp = 540, heightDp = 960, showBackground = true)
@Composable
private fun PreviewHome() {
    com.jinuo.kiosk.ui.theme.KioskTheme { HomeScreen() }
}
