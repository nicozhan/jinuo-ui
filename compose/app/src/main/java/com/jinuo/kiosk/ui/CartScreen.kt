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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.jinuo.kiosk.data.money
import com.jinuo.kiosk.ui.theme.Cream
import com.jinuo.kiosk.ui.theme.Fraunces
import com.jinuo.kiosk.ui.theme.Ink
import com.jinuo.kiosk.ui.theme.InstrumentSans
import com.jinuo.kiosk.ui.theme.Muted
import com.jinuo.kiosk.ui.theme.Scrim
import com.jinuo.kiosk.ui.theme.SoftBorder
import com.jinuo.kiosk.ui.theme.WarmSand

private val PAY_RED = androidx.compose.ui.graphics.Color(0xFFB33856)

/**
 * Cart drawer over the shopping grid (v14: 06-cart is a drawer on 04-shopping).
 * [items] is the live cart; [onQty] updates quantity (delta -1 / +1);
 * [onClose] returns to shopping; [onPay] goes to order confirmation.
 */
@Composable
fun CartScreen(
    modifier: Modifier = Modifier,
    items: List<CartItem>,
    onQty: (Int, Int) -> Unit,
    onClose: () -> Unit,
    onPay: () -> Unit,
) {
    val count = items.sumOf { it.qty }
    val subtotal = items.sumOf { it.lineTotal }
    // The HTML mock shows a fixed discount/VAT example; for a live cart we show
    // a simple 10% markdown example line, keeping the same visual structure.
    val discount = Math.round(subtotal * 0.10 * 100) / 100.0
    val total = Math.round((subtotal - discount) * 100) / 100.0

    Artboard(modifier = modifier) {
        KioskBackground(category = "CLASSICS", onCategory = {}, onProduct = {})

        // scrim
        Box(Modifier.size(ART_W, ART_H).background(Scrim).clickable { onClose() })

        // drawer panel 90,1171 900x750, rounded top
        Box(
            modifier = Modifier.offset(90.dp, 1171.dp).size(900.dp, 749.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Cream),
        ) {
            // drag handle
            Box(
                Modifier.offset(410.dp, 16.dp).size(80.dp, 6.dp)
                    .clip(CircleShape).background(SoftBorder),
            )
            Text(
                "Your Cart",
                modifier = Modifier.offset(36.dp, 44.dp).size(500.dp, 44.dp),
                fontFamily = Fraunces,
                fontWeight = FontWeight.W600,
                fontSize = 30.sp,
                color = Ink,
            )
            Text(
                "$count items in your cart",
                modifier = Modifier.offset(36.dp, 92.dp).size(500.dp, 26.dp),
                fontFamily = InstrumentSans,
                fontSize = 15.sp,
                color = Muted,
            )
            // close X
            Box(
                modifier = Modifier.offset(822.dp, 44.dp).size(42.dp, 42.dp)
                    .clip(CircleShape).border(1.dp, SoftBorder, CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { CloseIcon() }

            // item rows
            LazyColumn(
                modifier = Modifier.offset(36.dp, 132.dp).size(828.dp, 300.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(items) { idx, item ->
                    CartRow(idx, item, onQty)
                }
            }

            // totals block
            Box(Modifier.offset(0.dp, 452.dp).size(900.dp, 1.dp).background(SoftBorder))
            TotalLine("Subtotal", money(subtotal), 472f)
            TotalLine("Discount (2nd drink at CHF 1)", "-" + money(discount), 512f, red = true)
            TotalLine("Total", money(total), 552f, bold = true)

            // Proceed to Pay 126,1750 828x96 (relative to drawer: 36,579)
            Box(
                modifier = Modifier.offset(36.dp, 579.dp).size(828.dp, 96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PAY_RED)
                    .clickable { onPay() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Proceed to Pay",
                        fontFamily = InstrumentSans,
                        fontWeight = FontWeight.W600,
                        fontSize = 32.sp,
                        color = Cream,
                    )
                    Text("→", fontFamily = InstrumentSans, fontSize = 32.sp,
                        color = Cream, modifier = Modifier.offset(240.dp, 0.dp))
                }
            }
        }
    }
}

@Composable
private fun CartRow(index: Int, item: CartItem, onQty: (Int, Int) -> Unit) {
    // Pure absolute layout inside the 828 px row so nothing is pushed off-edge.
    Box(Modifier.fillMaxWidth().size(828.dp, 104.dp)) {
        Image(
            painter = painterResource(item.product.card),
            contentDescription = item.product.name,
            modifier = Modifier.offset(0.dp, 12.dp).size(80.dp, 80.dp)
                .clip(RoundedCornerShape(16.dp)).background(WarmSand),
            contentScale = ContentScale.Crop,
        )
        Text(
            item.product.name,
            modifier = Modifier.offset(96.dp, 18.dp).size(330.dp, 32.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 22.sp,
            color = Ink, maxLines = 1,
        )
        Text(
            item.spec,
            modifier = Modifier.offset(96.dp, 54.dp).size(330.dp, 28.dp),
            fontFamily = InstrumentSans, fontSize = 14.sp, color = Muted, maxLines = 1,
        )
        Text(
            money(item.lineTotal),
            modifier = Modifier.offset(440.dp, 30.dp).size(150.dp, 40.dp),
            fontFamily = Fraunces, fontSize = 18.sp, fontWeight = FontWeight.W600, color = Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        // quantity control
        QtyButton("−", SoftBorder, Ink, 610f) { onQty(index, -1) }
        Text(
            "${item.qty}",
            modifier = Modifier.offset(656.dp, 32.dp).size(52.dp, 40.dp),
            fontFamily = InstrumentSans, fontSize = 20.sp, color = Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        QtyButton("+", Ink, Cream, 714f) { onQty(index, 1) }
    }
}

@Composable
private fun QtyButton(label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, x: Float, onClick: () -> Unit) {
    Box(
        modifier = Modifier.offset(x.dp, 32.dp).size(40.dp, 40.dp).clip(RoundedCornerShape(12.dp))
            .background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = InstrumentSans, fontSize = 22.sp, color = fg)
    }
}

@Composable
private fun TotalLine(label: String, value: String, y: Float, bold: Boolean = false, red: Boolean = false) {
    Text(
        label,
        modifier = Modifier.offset(36.dp, y.dp).size(560.dp, 32.dp),
        fontFamily = InstrumentSans,
        fontSize = if (bold) 20.sp else 15.sp,
        fontWeight = if (bold) FontWeight.W600 else FontWeight.Normal,
        color = if (bold) Ink else Muted,
    )
    Text(
        value,
        modifier = Modifier.offset(600.dp, y.dp).size(264.dp, 32.dp),
        fontFamily = Fraunces,
        fontSize = if (bold) 24.sp else 16.sp,
        fontWeight = FontWeight.W600,
        color = if (red) com.jinuo.kiosk.ui.theme.PriceRed else Ink,
        textAlign = androidx.compose.ui.text.style.TextAlign.End,
    )
}

@androidx.compose.ui.tooling.preview.Preview(widthDp = 540, heightDp = 960, showBackground = true)
@Composable
private fun PreviewCart() {
    com.jinuo.kiosk.ui.theme.KioskTheme {
        CartScreen(
            items = listOf(
                CartItem(com.jinuo.kiosk.data.CATALOG[0], listOf(1, 1, 1)),
                CartItem(com.jinuo.kiosk.data.CATALOG[1], listOf(1, 1, 1)),
                CartItem(com.jinuo.kiosk.data.CATALOG[2], listOf(1, 1, 1)),
            ),
            onQty = { _, _ -> }, onClose = {}, onPay = {},
        )
    }
}
