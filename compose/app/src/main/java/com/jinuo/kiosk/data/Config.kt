package com.jinuo.kiosk.data

import java.util.Locale

/**
 * Secondary configuration (二次配置) for the 05-custom screen.
 *
 * Ported 1:1 from kiosk.js: OPT_SCHEMAS + OPT_OVERRIDES + OPT_ADD.
 * The track is not a fixed 3-chip artwork — N options divide the full 672 px
 * track, so 3 / 4 / 5 steps all fit. Caramel Macchiato is the 4-step demo.
 */
data class OptionGroup(
    val key: String,
    val title: String,
    val options: List<String>,
)

private val STRENGTH = OptionGroup("k.grp.strength", "Strength", listOf("Mild", "Medium", "Bold"))
private val SHOTS = OptionGroup("k.grp.shot", "Shots", listOf("1 Shot", "2 Shots", "3 Shots"))
private val MILK = OptionGroup("k.grp.milk", "Milk", listOf("Whole", "Oat", "Almond"))
private val SUGAR = OptionGroup("k.grp.sugar", "Sweetness", listOf("Less sugar", "Regular sugar", "Extra sugar"))
private val SUGAR_4 = OptionGroup("k.grp.sugar", "Sweetness", listOf("No Sugar", "Light", "Less Sugar", "Regular"))
private val TEMP = OptionGroup("k.grp.temp", "Temperature", listOf("Iced", "No ice", "Hot"))
private val TOPPING = OptionGroup("k.grp.topping", "Topping", listOf("None", "Pearl", "Jelly"))
private val CREAM = OptionGroup("k.grp.cream", "Cream", listOf("No Cream", "Light", "Extra"))

private val SCHEMAS: Map<String, List<OptionGroup>> = mapOf(
    "classic" to listOf(STRENGTH, SUGAR, TEMP),
    "espresso" to listOf(SHOTS, SUGAR, TEMP),
    "milk" to listOf(MILK, SUGAR, TEMP),
    "tea" to listOf(SUGAR, TEMP, TOPPING),
    "frape" to listOf(CREAM, SUGAR, TEMP),
)

/** Index-based surcharge. Any group key not listed here is free. */
private val SURCHARGES: Map<String, List<Double>> = mapOf(
    "k.grp.shot" to listOf(0.0, 0.0, 0.80),
    "k.grp.milk" to listOf(0.0, 0.0, 0.60),
    "k.grp.topping" to listOf(0.0, 0.0, 0.50),
    "k.grp.cream" to listOf(0.0, 0.0, 0.50),
)

fun schemaOf(product: Product): List<OptionGroup> {
    val base = SCHEMAS[product.kind] ?: SCHEMAS.getValue("classic")
    if (product.id != "caramel-macchiato") return base
    return base.toMutableList().also { it[1] = SUGAR_4 }
}

fun defaultSelections(product: Product): List<Int> = schemaOf(product).map { 1 }

/** One total only: catalog price + surcharges of the selected options. */
fun unitPriceOf(product: Product, selections: List<Int>): Double {
    var total = product.price
    schemaOf(product).forEachIndexed { groupIndex, group ->
        val tariff = SURCHARGES[group.key] ?: return@forEachIndexed
        val index = selections.getOrNull(groupIndex) ?: 1
        if (index in tariff.indices) total += tariff[index]
    }
    return Math.round(total * 100.0) / 100.0
}

fun money(value: Double): String = CURRENCY + " " + String.format(Locale.US, "%.2f", value)

/** 30 visual slots: own-category drinks first, then the rest, cycling. */
fun listFor(category: String): List<Product> {
    val pool = CATALOG.filter { it.category == category } + CATALOG.filter { it.category != category }
    return List(30) { pool[it % pool.size] }
}
