package com.jinuo.kiosk.ui

import com.jinuo.kiosk.data.Product
import com.jinuo.kiosk.data.unitPriceOf

/** One line in the cart: a configured drink and its quantity. */
data class CartItem(
    val product: Product,
    val selections: List<Int>,
    val qty: Int = 1,
) {
    /** Unit price including configuration surcharges. */
    val unitPrice: Double get() = unitPriceOf(product, selections)
    val lineTotal: Double get() = unitPrice * qty

    /** Short spec line, e.g. "Medium · Regular sugar · No ice". */
    val spec: String
        get() {
            val groups = com.jinuo.kiosk.data.schemaOf(product)
            return groups.mapIndexed { i, g ->
                g.options.getOrElse(selections.getOrElse(i) { 1 }) {
                    g.options.getOrElse(1) { g.options.first() }
                }
            }.joinToString(" · ")
        }
}

/** Top-level navigation routes for the kiosk flow. */
sealed class Screen {
    data object Home : Screen()          // 01-home
    data object Lang : Screen()          // 02-lang — home carousel + language picker
    data object Help : Screen()          // 03-help — support modal
    data object Shopping : Screen()      // 04-shopping
    data class Custom(val productId: String) : Screen()  // 05-custom
    data class Cart(val fromProductId: String = "americano") : Screen()  // 06-cart (drawer)
    data object CardPay : Screen()       // 11-card
    data object Wrong : Screen()         // 15-wrong
    data object PaySuccess : Screen()    // 16-pay
    data object Making : Screen()        // 17-making
    data object Pickup : Screen()        // 17b-pickup — "please collect your drink"
    data object Ready : Screen()         // 18-ready
    data object OrderConfirm : Screen()  // 19-order-confirm
}
