package com.jinuo.kiosk.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Navigation root for the whole kiosk prototype. Owns the cart and the current
 * screen, and wires all twelve HTML pages together:
 *
 *   01-home → 02-lang → 04-shopping → 05-custom → 06-cart(drawer)
 *          → 19-order-confirm → 11-card → 16-pay → 17-making → 18-ready → home
 *
 *   03-help and 15-wrong are reachable from the shared header / payment result.
 */
@Composable
fun KioskApp(initialScreen: Screen = Screen.Home, initialCart: Boolean = false) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(initialScreen) }
    var returnScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val cart = remember {
        mutableStateListOf<CartItem>().apply {
            if (initialCart) {
                add(CartItem(com.jinuo.kiosk.data.CATALOG[0], listOf(1, 1, 1)))
                add(CartItem(com.jinuo.kiosk.data.CATALOG[1], listOf(1, 1, 1)))
                add(CartItem(com.jinuo.kiosk.data.CATALOG[2], com.jinuo.kiosk.data.defaultSelections(com.jinuo.kiosk.data.CATALOG[2])))
            }
        }
    }
    var showHomeConfirm by remember { mutableStateOf(false) }
    var payAmount by remember { mutableStateOf<Double>(8.0) }
    var brewStage by remember { mutableStateOf(0) }

    val cartCount = cart.sumOf { it.qty }
    val cartTotal = cart.sumOf { it.lineTotal }

    fun addToCart(item: CartItem) {
        val idx = cart.indexOfFirst { it.product.id == item.product.id && it.selections == item.selections }
        if (idx >= 0) cart[idx] = cart[idx].copy(qty = cart[idx].qty + item.qty)
        else cart.add(item)
    }

    fun changeQty(index: Int, delta: Int) {
        if (index !in cart.indices) return
        val next = cart[index].qty + delta
        if (next <= 0) cart.removeAt(index) else cart[index] = cart[index].copy(qty = next)
    }

    fun askHomeConfirm() { showHomeConfirm = true }

    fun goHome() {
        cart.clear()
        showHomeConfirm = false
        screen = Screen.Home
    }

    fun openHelp() {
        returnScreen = screen
        screen = Screen.Help
    }

    fun closeHelp() {
        screen = if (returnScreen is Screen.Help) Screen.Home else returnScreen
    }

    val langToast: () -> Unit = {
        Toast.makeText(context, "Language selection (English only in prototype)", Toast.LENGTH_SHORT).show()
    }

    when (val s = screen) {
        is Screen.Home -> HomeScreen(
            onStart = { screen = Screen.Shopping },
            onLang = { screen = Screen.Lang },
            onHelp = { openHelp() },
        )

        is Screen.Lang -> LangScreen(
            onStart = { screen = Screen.Shopping },
            onLang = langToast,
            onHelp = { openHelp() },
            onBack = { screen = Screen.Home },
        )

        is Screen.Help -> HelpScreen(onClose = { closeHelp() })

        is Screen.Shopping -> CustomScreen(
            initialProductId = "americano",
            startWithModal = false,
            cartCount = cartCount,
            cartTotal = cartTotal,
            onAddToCart = { addToCart(it) },
            onCheckout = { screen = Screen.Cart() },
            onCartClick = { screen = Screen.Cart() },
            onLang = langToast,
            onHelp = { openHelp() },
            onBack = { askHomeConfirm() },
        )

        is Screen.Custom -> CustomScreen(
            initialProductId = s.productId,
            startWithModal = true,
            cartCount = cartCount,
            cartTotal = cartTotal,
            onAddToCart = { addToCart(it) },
            onCheckout = { screen = Screen.Cart(s.productId) },
            onCartClick = { screen = Screen.Cart(s.productId) },
            onLang = langToast,
            onHelp = { openHelp() },
            onBack = { askHomeConfirm() },
        )

        is Screen.Cart -> CartScreen(
            items = cart,
            onQty = { index, delta -> changeQty(index, delta) },
            onClose = { screen = Screen.Shopping },
            onPay = { screen = Screen.OrderConfirm },
        )

        is Screen.OrderConfirm -> OrderConfirmScreen(
            items = cart,
            onBack = { askHomeConfirm() },
            onPay = {
                payAmount = cart.sumOf { it.lineTotal }.let { if (it > 0) it else 8.0 }
                screen = Screen.CardPay
            },
        )

        is Screen.CardPay -> CardPayScreen(
            amount = payAmount,
            onBack = { askHomeConfirm() },
            onDone = {
                // finishPayment(): success → 16-pay, otherwise → 15-wrong.
                // The prototype's happy path is success; error page is also
                // reachable directly via deep link (`screen=wrong`).
                brewStage = 0
                screen = Screen.PaySuccess
            },
        )

        is Screen.Wrong -> WrongScreen(
            onHelp = { openHelp() },
            onHome = { goHome() },
        )

        is Screen.PaySuccess -> PaySuccessScreen(
            onBack = { askHomeConfirm() },
            onContinue = { brewStage = 0; screen = Screen.Making },
        )

        is Screen.Making -> MakingScreen(
            stage = brewStage.coerceIn(0, 4),
            onDone = {
                if (brewStage < 4) brewStage += 1
                else screen = Screen.Ready
            },
        )

        is Screen.Ready -> ReadyScreen(
            onNewOrder = { goHome() },
        )
    }

    if (showHomeConfirm) {
        HomeConfirmDialog(onCancel = { showHomeConfirm = false }, onConfirm = { goHome() })
    }
}
