package com.jinuo.kiosk.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Navigation root for the whole kiosk prototype. All twelve HTML pages are
 * wired, and the three global UX layers — language picker, help modal and cart
 * sheet — are overlays stacked over the current page (never page replacements):
 *
 *   01-home → 02-lang → 04-shopping → 05-custom → 06-cart(drawer)
 *          → 19-order-confirm → 11-card → 16-pay → 17-making → 18-ready → home
 *
 * The English/French/Chinese picker defaults to English; interface copy stays
 * English per project convention. Only the pill label/flag follow the choice.
 */
@Composable
fun KioskApp(
    initialScreen: Screen = Screen.Home,
    initialCart: Boolean = false,
    initialHelp: Boolean = false,
    initialLangOpen: Boolean = false,
    initialLangAnchor: Pair<Float, Float> = Pair(66f, 1380f),
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(initialScreen) }

    // cart is shared by every page
    val cart = remember {
        mutableStateListOf<CartItem>().apply {
            if (initialCart || initialScreen is Screen.OrderConfirm) {
                add(CartItem(com.jinuo.kiosk.data.CATALOG[0], listOf(1, 1, 1)))
                add(CartItem(com.jinuo.kiosk.data.CATALOG[1], listOf(1, 1, 1)))
                add(CartItem(com.jinuo.kiosk.data.CATALOG[2], com.jinuo.kiosk.data.defaultSelections(com.jinuo.kiosk.data.CATALOG[2])))
            }
        }
    }

    // --- global overlays ----------------------------------------------------
    var langCode by remember { mutableStateOf("en") }          // default English
    var langOpen by remember { mutableStateOf(initialLangOpen) }
    var langAnchor by remember { mutableStateOf(initialLangAnchor) } // home/lang pill
    var helpOpen by remember { mutableStateOf(initialHelp || initialScreen is Screen.Help) }
    var cartOpen by remember { mutableStateOf(initialCart || initialScreen is Screen.Cart) }

    var showHomeConfirm by remember { mutableStateOf(false) }
    var payAmount by remember { mutableStateOf<Double>(8.0) }
    var brewStage by remember { mutableStateOf(1) }   // v15.5 · states 1..4

    val cartCount = cart.sumOf { it.qty }
    val cartTotal = cart.sumOf { it.lineTotal }
    val lang = langByCode(langCode)

    // v15.5 · cart is capped at 3 distinct product lines (matches the 3-row
    // order layout). Exceeding it is rejected with a visible prompt.
    val maxCartItems = 3
    var cartLimitHint by remember { mutableStateOf(false) }

    fun addToCart(item: CartItem) {
        if (cart.size >= maxCartItems &&
            cart.none { it.product.id == item.product.id && it.selections == item.selections }
        ) {
            cartLimitHint = true
            Toast.makeText(context, "Cart limit reached · maximum 3 items per order", Toast.LENGTH_SHORT).show()
            return
        }
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
        helpOpen = false
        cartOpen = false
        langOpen = false
        screen = Screen.Home
    }

    // --- language dropdown --------------------------------------------------
    fun openLang(x: Float, y: Float) {
        langAnchor = Pair(x, y)
        langOpen = true
    }

    fun chooseLang(choice: LangOption) {
        val fromHome = screen is Screen.Home || screen is Screen.Lang
        langCode = choice.code
        langOpen = false
        Toast.makeText(
            context,
            if (fromHome) "${choice.label} selected · opening menu" else "Language: ${choice.label}",
            Toast.LENGTH_SHORT,
        ).show()
        if (fromHome) screen = Screen.Shopping
    }

    // --- cart sheet ---------------------------------------------------------
    fun openCart() { cartOpen = true }
    fun closeCart() { cartOpen = false }
    fun goPay() {
        cartOpen = false
        payAmount = if (cartTotal > 0) cartTotal else 8.0
        screen = Screen.OrderConfirm
    }

    Box(Modifier.fillMaxSize()) {
        // ---- base page -----------------------------------------------------
        when (val s = screen) {
            is Screen.Home -> HomeScreen(
                langLabel = lang.label, langFlag = lang.flag,
                onStart = { screen = Screen.Shopping },
                onLang = { openLang(66f, 1380f) },
                onHelp = { helpOpen = true },
            )

            is Screen.Lang -> LangScreen(
                langLabel = lang.label, langFlag = lang.flag,
                onStart = { screen = Screen.Shopping },
                onLang = { openLang(66f, 1380f) },
                onHelp = { helpOpen = true },
                onBack = { screen = Screen.Home },
            )

            is Screen.Shopping -> CustomScreen(
                initialProductId = "americano",
                startWithModal = false,
                langLabel = lang.label, langFlag = lang.flag,
                cartCount = cartCount, cartTotal = cartTotal,
                onAddToCart = { addToCart(it) },
                onCheckout = { openCart() },
                onCartClick = { openCart() },
                onLang = { openLang(480f, 155f) },
                onHelp = { helpOpen = true },
            )

            is Screen.Custom -> CustomScreen(
                initialProductId = s.productId,
                startWithModal = true,
                langLabel = lang.label, langFlag = lang.flag,
                cartCount = cartCount, cartTotal = cartTotal,
                onAddToCart = { addToCart(it) },
                onCheckout = { openCart() },
                onCartClick = { openCart() },
                onLang = { openLang(765f, 155f) },
                onHelp = { helpOpen = true },
            )

            is Screen.OrderConfirm -> OrderConfirmScreen(
                items = cart, langLabel = lang.label, langFlag = lang.flag,
                onBack = { askHomeConfirm() },
                onLang = { openLang(700f, 155f) },
                onHelp = { helpOpen = true },
                onPay = { amount -> payAmount = amount; screen = Screen.CardPay },
            )

            is Screen.CardPay -> CardPayScreen(
                amount = payAmount, langLabel = lang.label, langFlag = lang.flag,
                onBack = { askHomeConfirm() },
                onLang = { openLang(700f, 155f) },
                onHelp = { helpOpen = true },
                onDone = {
                    // happy path → 16-pay; error page reachable via deep link
                    brewStage = 1
                    screen = Screen.PaySuccess
                },
            )

            is Screen.Wrong -> WrongScreen(
                onHelp = { helpOpen = true },
                onHome = { goHome() },
            )

            is Screen.PaySuccess -> PaySuccessScreen(
                langLabel = lang.label, langFlag = lang.flag,
                onBack = { askHomeConfirm() },
                onLang = { openLang(700f, 155f) },
                onHelp = { helpOpen = true },
                onContinue = { brewStage = 1; screen = Screen.Making },
            )

            is Screen.Making -> MakingScreen(
                stage = brewStage.coerceIn(1, 4),
                onDone = {
                    if (brewStage < 4) brewStage += 1 else screen = Screen.Ready
                },
            )

            is Screen.Ready -> ReadyScreen(onNewOrder = { goHome() })

            is Screen.Help -> HelpScreen(onClose = { helpOpen = false })
            is Screen.Cart -> CartScreen(
                items = cart,
                onQty = { index, delta -> changeQty(index, delta) },
                onClose = { screen = Screen.Shopping },
                onPay = { goPay() },
            )
        }

        // ---- global overlays (stacked above the base page) -----------------
        // Order: cart sheet < help modal < language picker < home confirm.
        if (cartOpen && screen !is Screen.Cart) {
            CartOverlay(
                items = cart,
                onQty = { index, delta -> changeQty(index, delta) },
                onClose = { closeCart() },
                onPay = { goPay() },
            )
        }
        if (helpOpen && screen !is Screen.Help) {
            HelpOverlay(onClose = { helpOpen = false })
        }
        if (langOpen) {
            LangPicker(
                x = langAnchor.first, y = langAnchor.second,
                selectedCode = langCode,
                onSelect = { chooseLang(it) },
                onDismiss = { langOpen = false },
            )
        }
        if (showHomeConfirm) {
            HomeConfirmDialog(onCancel = { showHomeConfirm = false }, onConfirm = { goHome() })
        }
    }
}
