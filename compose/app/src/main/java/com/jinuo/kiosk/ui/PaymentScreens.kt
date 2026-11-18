package com.jinuo.kiosk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinuo.kiosk.R
import com.jinuo.kiosk.data.money
import com.jinuo.kiosk.ui.theme.Cream
import com.jinuo.kiosk.ui.theme.Fraunces
import com.jinuo.kiosk.ui.theme.Hairline
import com.jinuo.kiosk.ui.theme.Ink
import com.jinuo.kiosk.ui.theme.InstrumentSans
import com.jinuo.kiosk.ui.theme.Muted
import com.jinuo.kiosk.ui.theme.PriceRed
import com.jinuo.kiosk.ui.theme.SoftBorder
import com.jinuo.kiosk.ui.theme.WarmSand

private val PAY_RED = androidx.compose.ui.graphics.Color(0xFFB33856)

/* ---------------- 19-order-confirm ---------------- */
@Composable
fun OrderConfirmScreen(
    modifier: Modifier = Modifier,
    items: List<CartItem>,
    langLabel: String = "English",
    langFlag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    onBack: () -> Unit = {},
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
    onPay: (Double) -> Unit = {},
) {
    val subtotal = items.sumOf { it.lineTotal }
    val discount = Math.round(subtotal * 0.10 * 100) / 100.0
    val vat = subtotal
    val total = Math.round((subtotal - discount + 0.0) * 100) / 100.0
    val payAmount = if (total > 0) total else 8.0

    Artboard(modifier = modifier) {
        PayHeader(timeLabel = "14:00", langLabel = langLabel, langFlag = langFlag,
            onBack = onBack, onLang = onLang, onHelp = onHelp)

        Text(
            "ORDER CONFIRMATION",
            modifier = Modifier.offset(64.dp, 162.dp).size(600.dp, 46.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 32.sp, color = Ink,
        )
        Text(
            "ID202611082976",
            modifier = Modifier.offset(64.dp, 208.dp).size(400.dp, 24.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600, fontSize = 14.sp,
            letterSpacing = 2.8.sp, color = Muted,
        )

        // order card 64,234 952x709
        Box(
            modifier = Modifier.offset(64.dp, 234.dp).size(952.dp, 709.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Cream)
                .border(1.dp, Hairline, RoundedCornerShape(28.dp)),
        ) {
            Text("Order List", modifier = Modifier.offset(32.dp, 30.dp).size(400.dp, 32.dp),
                fontFamily = Fraunces, fontSize = 22.sp, color = Ink)
            Text("${items.sumOf { it.qty }} items", modifier = Modifier.offset(770.dp, 34.dp).size(150.dp, 26.dp),
                fontFamily = InstrumentSans, fontSize = 14.sp, color = Muted)

            // v15.5 · order list is exactly 3 product rows (HTML f45: 886.78x337.25,
            // three 112.62px rows). Each row carries a hairline like the Figma art.
            val shown = items.take(3)
            for (i in 0 until 3) {
                Box(
                    modifier = Modifier.offset(33.dp, (78 + i * 112.62f).dp)
                        .size(886.dp, 112.dp)
                        .border(1.dp, Hairline, RoundedCornerShape(0.dp)),
                )
            }
            shown.forEachIndexed { i, item ->
                Image(
                    painter = painterResource(item.product.card),
                    contentDescription = item.product.name,
                    modifier = Modifier.offset(48.dp, (92 + (i * 112.62f).toInt()).dp).size(80.dp, 80.dp)
                        .clip(RoundedCornerShape(16.dp)).background(WarmSand),
                    contentScale = ContentScale.Crop,
                )
                Text(item.product.name,
                    modifier = Modifier.offset(145.dp, (100 + (i * 112.62f).toInt()).dp).size(430.dp, 30.dp),
                    fontFamily = Fraunces, fontSize = 22.sp, color = Ink, maxLines = 1)
                Text(item.spec,
                    modifier = Modifier.offset(145.dp, (134 + (i * 112.62f).toInt()).dp).size(430.dp, 26.dp),
                    fontFamily = InstrumentSans, fontSize = 16.sp, color = Muted, maxLines = 1)
                Text("×${item.qty}",
                    modifier = Modifier.offset(600.dp, (104 + (i * 112.62f).toInt()).dp).size(70.dp, 28.dp),
                    fontFamily = InstrumentSans, fontWeight = FontWeight.W600, fontSize = 16.sp, color = Ink)
                Text(money(item.lineTotal),
                    modifier = Modifier.offset(720.dp, (104 + (i * 112.62f).toInt()).dp).size(180.dp, 28.dp),
                    fontFamily = Fraunces, fontSize = 18.sp, color = Ink,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }

            // totals
            ConfirmTotal("Subtotal:", money(subtotal), 452f)
            ConfirmTotal("Discount (2nd drink at CHF 1):", "-" + money(discount), 498f, red = true)
            ConfirmTotal("VAT Amount (7.7%):", money(vat), 544f)
            // v15.5 · Total bar and PAY button share the exact same geometry (886x96).
            Box(
                modifier = Modifier.offset(33.dp, 590.dp).size(886.dp, 96.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Muted.copy(alpha = 0.75f)),
            ) {
                Text("Total (incl. VAT):",
                    modifier = Modifier.offset(32.dp, 26.dp).size(500.dp, 44.dp),
                    fontFamily = InstrumentSans,
                    fontWeight = FontWeight.W600, fontSize = 24.sp, color = Cream)
                Text(money(total),
                    modifier = Modifier.offset(550.dp, 24.dp).size(300.dp, 48.dp),
                    fontFamily = Fraunces, fontSize = 30.sp,
                    color = Cream, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }

        // PAYMENT section
        Text("PAYMENT", modifier = Modifier.offset(64.dp, 980.dp).size(400.dp, 44.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 32.sp, color = Ink)
        Box(
            modifier = Modifier.offset(64.dp, 1036.dp).size(952.dp, 640.dp)
                .clip(RoundedCornerShape(28.dp)).background(Cream)
                .border(1.dp, Hairline, RoundedCornerShape(28.dp)),
        ) {
            Text("Tap, Insert or Scan to Pay",
                modifier = Modifier.offset(64.dp, 40.dp).size(824.dp, 48.dp),
                fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 30.sp,
                color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Follow the instruction on the POS terminal...",
                modifier = Modifier.offset(64.dp, 96.dp).size(824.dp, 30.dp),
                fontFamily = InstrumentSans, fontSize = 18.sp, color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Image(
                painter = painterResource(R.drawable.pay_qr),
                contentDescription = "QR code",
                modifier = Modifier.offset(366.dp, 146.dp).size(220.dp, 220.dp),
                contentScale = ContentScale.Fit,
            )
            Text("SUPPORTED", modifier = Modifier.offset(402.dp, 386.dp).size(148.dp, 36.dp),
                fontFamily = InstrumentSans, fontWeight = FontWeight.W600, fontSize = 18.sp,
                letterSpacing = 3.sp, color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            PayLogos(y = 430)
        }

        // PAY button 97,1723 886x96
        Box(
            modifier = Modifier.offset(97.dp, 1723.dp).size(886.dp, 96.dp)
                .clip(RoundedCornerShape(16.dp)).background(PAY_RED)
                .clickable { onPay(payAmount) },
        ) {
            Text("PAY",
                modifier = Modifier.offset(48.dp, 28.dp).size(200.dp, 44.dp),
                fontFamily = Fraunces, fontWeight = FontWeight.W600,
                fontSize = 32.sp, color = Cream)
            Text(money(payAmount),
                modifier = Modifier.offset(500.dp, 28.dp).size(260.dp, 44.dp),
                fontFamily = Fraunces, fontWeight = FontWeight.W600,
                fontSize = 32.sp, color = Cream,
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Text("→",
                modifier = Modifier.offset(790.dp, 26.dp).size(48.dp, 44.dp),
                fontFamily = InstrumentSans, fontSize = 32.sp,
                color = Cream, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun ConfirmTotal(label: String, value: String, y: Float, red: Boolean = false) {
    Text(label, modifier = Modifier.offset(32.dp, y.dp).size(600.dp, 30.dp),
        fontFamily = InstrumentSans, fontSize = 16.sp, color = Muted)
    Text(value, modifier = Modifier.offset(700.dp, y.dp).size(220.dp, 30.dp),
        fontFamily = Fraunces, fontSize = 18.sp,
        color = if (red) PriceRed else Ink,
        textAlign = androidx.compose.ui.text.style.TextAlign.End)
}

@Composable
private fun PayLogos(y: Int) {
    val logos = listOf(R.drawable.pay_logo_1, R.drawable.pay_logo_2, R.drawable.pay_logo_3,
        R.drawable.pay_logo_4, R.drawable.pay_logo_5)
    Row(
        modifier = Modifier.offset(64.dp, y.dp).size(824.dp, 150.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        logos.forEach {
            Image(painter = painterResource(it), contentDescription = null,
                modifier = Modifier.size(150.dp, 90.dp), contentScale = ContentScale.Fit)
        }
    }
}

/* ---------------- 11-card (POS waiting) ---------------- */
@Composable
fun CardPayScreen(
    modifier: Modifier = Modifier,
    amount: Double = 8.0,
    langLabel: String = "English",
    langFlag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    onBack: () -> Unit = {},
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    Artboard(modifier = modifier) {
        PayHeader(timeLabel = "14:02", langLabel = langLabel, langFlag = langFlag,
            onBack = onBack, onLang = onLang, onHelp = onHelp)

        Text("COMPLETE PAYMENT",
            modifier = Modifier.offset(140.dp, 160.dp).size(800.dp, 48.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 40.sp,
            color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("Go to the POS terminal to complete payment.",
            modifier = Modifier.offset(160.dp, 220.dp).size(760.dp, 34.dp),
            fontFamily = InstrumentSans, fontSize = 24.sp, color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        // v15.6 · the static card-reader illustration is replaced by the native
        // looping contactless animation (SVG/CSS reference → Compose Canvas).
        // It shows a card approaching the reader, three waves spreading out
        // and the halo fading — guiding the guest to the POS terminal.
        TapCardAnimation(
            modifier = Modifier.offset(220.dp, 280.dp).size(640.dp),
        )

        Text("→  GO TO THE POS TERMINAL",
            modifier = Modifier.offset(210.dp, 856.dp).size(660.dp, 56.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 30.sp,
            color = PAY_RED,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("Tap your card, then follow the screen on the terminal",
            modifier = Modifier.offset(160.dp, 920.dp).size(760.dp, 40.dp),
            fontFamily = InstrumentSans, fontSize = 19.sp, color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        // amount card 260,1018 560x160
        Box(Modifier.offset(260.dp, 1018.dp).size(560.dp, 160.dp)
            .clip(RoundedCornerShape(22.dp)).background(Cream)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))) {
            Text("AMOUNT TO PAY", modifier = Modifier.offset(0.dp, 30.dp).fillMaxWidth(),
                fontFamily = InstrumentSans, fontWeight = FontWeight.W600, fontSize = 18.sp,
                letterSpacing = 3.2.sp, color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(money(amount), modifier = Modifier.offset(0.dp, 70.dp).fillMaxWidth(),
                fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 45.sp,
                color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        Text("Waiting for payment ...", modifier = Modifier.offset(280.dp, 1230.dp).size(520.dp, 48.dp),
            fontFamily = InstrumentSans, fontSize = 30.sp, color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("SUPPORTED", modifier = Modifier.offset(400.dp, 1360.dp).size(280.dp, 48.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600, fontSize = 18.sp,
            letterSpacing = 5.sp, color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        PayLogosPayouts()

        // whole screen tap to finish (mirrors HTML finishPay)
        Box(Modifier.size(ART_W, ART_H).clickable { onDone() })
    }
}

@Composable
private fun PayLogosPayouts() {
    val logos = listOf(R.drawable.pay_logo_1, R.drawable.pay_logo_2, R.drawable.pay_logo_3,
        R.drawable.pay_logo_4, R.drawable.pay_logo_5)
    Row(modifier = Modifier.offset(0.dp, 1458.dp).size(ART_W, 200.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically) {
        logos.forEach {
            Image(painter = painterResource(it), contentDescription = null,
                modifier = Modifier.size(180.dp, 100.dp), contentScale = ContentScale.Fit)
        }
    }
}

/* ---------------- 16-pay (success) ---------------- */
@Composable
fun PaySuccessScreen(
    modifier: Modifier = Modifier,
    langLabel: String = "English",
    langFlag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    onBack: () -> Unit = {},
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    Artboard(modifier = modifier) {
        PayHeader(timeLabel = "14:20", langLabel = langLabel, langFlag = langFlag,
            onBack = onBack, onLang = onLang, onHelp = onHelp)

        Text("PAYMENT / SUCCESSFUL",
            modifier = Modifier.offset(64.dp, 188.dp).size(952.dp, 32.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600, fontSize = 20.sp,
            letterSpacing = 3.8.sp, color = com.jinuo.kiosk.ui.theme.Caramel)
        Text("Payment completed successfully.",
            modifier = Modifier.offset(64.dp, 242.dp).size(952.dp, 90.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 46.sp,
            color = Ink, lineHeight = 50.sp)
        Text("Your payment was successful.",
            modifier = Modifier.offset(64.dp, 336.dp).size(952.dp, 44.dp),
            fontFamily = InstrumentSans, fontSize = 30.sp, color = Muted)

        // success ring 400,806 280x280 (moved +140 in v14 → y=946)
        Box(Modifier.offset(400.dp, 946.dp).size(280.dp, 280.dp)
            .clip(CircleShape).background(WarmSand)
            .border(4.dp, androidx.compose.ui.graphics.Color(0xFFDCE3F1), CircleShape),
            contentAlignment = Alignment.Center) {
            Text("✓", fontFamily = Fraunces, fontSize = 140.sp,
                color = com.jinuo.kiosk.ui.theme.Caramel)
        }

        Text("Your payment was successful.",
            modifier = Modifier.offset(90.dp, 1300.dp).size(900.dp, 54.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 46.sp,
            color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("Your drink is being prepared now.",
            modifier = Modifier.offset(90.dp, 1370.dp).size(900.dp, 40.dp),
            fontFamily = InstrumentSans, fontSize = 28.sp, color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Text("© 2026 VendSolution. ALL RIGHTS RESERVED.",
            modifier = Modifier.offset(220.dp, 1820.dp).size(640.dp, 30.dp),
            fontFamily = InstrumentSans, fontWeight = FontWeight.W600, fontSize = 18.sp,
            letterSpacing = 1.6.sp, color = Muted.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Box(Modifier.size(ART_W, ART_H).clickable { onContinue() })
    }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp = 540, heightDp = 960, showBackground = true)
@Composable
private fun PreviewOrderConfirm() {
    com.jinuo.kiosk.ui.theme.KioskTheme {
        OrderConfirmScreen(items = listOf(
            CartItem(com.jinuo.kiosk.data.CATALOG[0], listOf(1, 1, 1)),
            CartItem(com.jinuo.kiosk.data.CATALOG[1], listOf(1, 1, 1)),
        ))
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Card Pay", widthDp = 540, heightDp = 960, showBackground = true)
@Composable
private fun PreviewCardPay() {
    com.jinuo.kiosk.ui.theme.KioskTheme { CardPayScreen() }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Pay Success", widthDp = 540, heightDp = 960, showBackground = true)
@Composable
private fun PreviewPaySuccess() {
    com.jinuo.kiosk.ui.theme.KioskTheme { PaySuccessScreen() }
}
