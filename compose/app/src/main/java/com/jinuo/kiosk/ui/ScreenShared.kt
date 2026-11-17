package com.jinuo.kiosk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinuo.kiosk.R
import com.jinuo.kiosk.ui.theme.Cream
import com.jinuo.kiosk.ui.theme.Fraunces
import com.jinuo.kiosk.ui.theme.Hairline
import com.jinuo.kiosk.ui.theme.Ink
import com.jinuo.kiosk.ui.theme.InstrumentSans
import com.jinuo.kiosk.ui.theme.Scrim
import com.jinuo.kiosk.ui.theme.SoftBorder
import com.jinuo.kiosk.ui.theme.WarmSand

/**
 * v14 shared header used by the payment pages (order-confirm / card / pay).
 * Status row y=0..66, then a 140px-tall header: back arrow at left, centred
 * logo, and English / Help pills at right. [showBack] is false only where the
 * page owns its own back control.
 */
@Composable
internal fun PayHeader(
    timeLabel: String = "14:02",
    showBack: Boolean = true,
    langLabel: String = "English",
    langFlag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    onBack: () -> Unit = {},
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
) {
    // status row
    StatusText("MACHINE ID · JAV-08-221", 64f, 300f)
    StatusText("SUPPORT · +86 400 820 2006", 359f, 360f)
    StatusText("TIME · $timeLabel", 865f, 151f)
    Box(Modifier.offset(0.dp, 66.dp).size(ART_W, 1.dp).background(Hairline))

    // back arrow — Figma white circle at 45,66
    if (showBack) {
        Box(
            modifier = Modifier.offset(45.dp, 66.dp).size(90.dp, 64.dp)
                .clip(CircleShape)
                .background(Cream)
                .border(1.dp, SoftBorder, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text("←", fontFamily = InstrumentSans, fontSize = 30.sp, color = Ink)
        }
    }

    // centred logo 472,66 136x64
    Image(
        painter = painterResource(R.drawable.kiosk_logo),
        contentDescription = "Jinuo Coffee",
        modifier = Modifier.offset(472.dp, 66.dp).size(136.dp, 64.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
    )

    // English / Help pills
    PayPill(716f, "$langFlag $langLabel", onLang)
    PayPill(888f, "Help", onHelp, width = 128f)
}

@Composable
private fun PayPill(x: Float, text: String, onClick: () -> Unit, width: Float = 160f) {
    Box(
        modifier = Modifier.offset(x.dp, 66.dp).size(width.dp, 60.dp)
            .clip(CircleShape)
            .background(WarmSand.copy(alpha = 0.94f))
            .border(1.dp, Hairline, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontFamily = Fraunces,
            fontWeight = FontWeight.W600,
            fontSize = 22.sp,
            color = Ink,
            maxLines = 1,
        )
    }
}

/** v14 "return to home" confirmation modal (English interface). */
@Composable
internal fun HomeConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(Modifier.size(ART_W, ART_H).background(Scrim).clickable { })
    Box(
        modifier = Modifier.offset(190.dp, 690.dp).size(700.dp, 540.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Cream),
    ) {
        Text(
            "Go back to home?",
            modifier = Modifier.offset(48.dp, 56.dp).size(604.dp, 60.dp),
            fontFamily = Fraunces,
            fontWeight = FontWeight.W600,
            fontSize = 40.sp,
            color = Ink,
        )
        Text(
            "Your cart and personalisation will be cleared.",
            modifier = Modifier.offset(48.dp, 136.dp).size(604.dp, 120.dp),
            fontFamily = InstrumentSans,
            fontSize = 26.sp,
            color = com.jinuo.kiosk.ui.theme.Muted,
            lineHeight = 36.sp,
        )
        Box(
            modifier = Modifier.offset(48.dp, 390.dp).size(292.dp, 84.dp)
                .clip(CircleShape)
                .background(WarmSand)
                .border(1.dp, SoftBorder, CircleShape)
                .clickable { onCancel() },
            contentAlignment = Alignment.Center,
        ) {
            Text("Cancel", fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
                fontSize = 28.sp, color = Ink)
        }
        Box(
            modifier = Modifier.offset(360.dp, 390.dp).size(292.dp, 84.dp)
                .clip(CircleShape)
                .background(Ink)
                .clickable { onConfirm() },
            contentAlignment = Alignment.Center,
        ) {
            Text("Go Back", fontFamily = InstrumentSans, fontWeight = FontWeight.W600,
                fontSize = 28.sp, color = Cream)
        }
    }
}
