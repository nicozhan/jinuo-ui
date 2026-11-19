package com.jinuo.kiosk.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinuo.kiosk.R
import com.jinuo.kiosk.data.CATALOG
import com.jinuo.kiosk.data.OptionGroup
import com.jinuo.kiosk.data.Product
import com.jinuo.kiosk.data.defaultSelections
import com.jinuo.kiosk.data.money
import com.jinuo.kiosk.data.schemaOf
import com.jinuo.kiosk.data.unitPriceOf
import com.jinuo.kiosk.ui.theme.CardShadow
import com.jinuo.kiosk.ui.theme.Caramel
import com.jinuo.kiosk.ui.theme.ChipBg
import com.jinuo.kiosk.ui.theme.Cream
import com.jinuo.kiosk.ui.theme.Fraunces
import com.jinuo.kiosk.ui.theme.Hairline
import com.jinuo.kiosk.ui.theme.Ink
import com.jinuo.kiosk.ui.theme.InstrumentSans
import com.jinuo.kiosk.ui.theme.ModalShadow
import com.jinuo.kiosk.ui.theme.Muted
import com.jinuo.kiosk.ui.theme.Outer
import com.jinuo.kiosk.ui.theme.PriceRed
import com.jinuo.kiosk.ui.theme.Scrim
import com.jinuo.kiosk.ui.theme.SoftBorder
import com.jinuo.kiosk.ui.theme.WarmSand

/* ---------------------------------------------------------------------------
   Artboard: every child below uses 1080 x 1920 design coordinates.
   1 design px == 1 dp; the whole tree is uniformly scaled to the device.
   --------------------------------------------------------------------------- */
val ART_W = 1080.dp
val ART_H = 1920.dp

/* v18 · display-only category layer. The generated Catalog.kt keeps the raw
   categories (CLASSICS/ESPRESSO/MILK/TEA/FRAPPE); the kiosk UI relabels and
   regroups them into these 5 tabs so a catalog re-sync cannot wipe the change.
   "All" is a virtual tab that just pins the ALL DRINKS head at the very top. */
internal const val UI_ALL = "All"
internal val UI_CATS = listOf(
    UI_ALL,
    "Classic Coffee",
    "Milk Coffee",
    "Fruit Tea",
    "Light Milk Tea",
)

/** Which raw categories feed each display tab. */
private val UI_SOURCES: Map<String, List<String>> = mapOf(
    "Classic Coffee" to listOf("CLASSICS", "ESPRESSO"),
    "Milk Coffee" to listOf("MILK"),
    "Fruit Tea" to listOf("TEA"),
    "Light Milk Tea" to listOf("FRAPPE"),
)

/** Products for a display tab, in catalog order (All = the full 22). */
internal fun productsForUiCat(tab: String): List<Product> = when (tab) {
    UI_ALL -> CATALOG
    else -> {
        val raw = UI_SOURCES[tab].orEmpty()
        CATALOG.filter { it.category in raw }
    }
}

@Composable
internal fun Artboard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Outer),
    ) {
        // maxWidth/maxHeight are the available space in design dp; the artboard
        // itself is fixed at 1080x1920 dp. Scale uniformly to fit (contain).
        val scale = minOf(maxWidth / ART_W, maxHeight / ART_H)
        val left = (maxWidth - ART_W * scale) / 2
        val top = (maxHeight - ART_H * scale) / 2
        Box(
            modifier = Modifier
                .offset(left, top)
                // requiredSize ignores the parent's max constraint so the board
                // keeps its full 1080x1920 design size; graphicsLayer then scales
                // it to fit (scale is 1.0 when density is pinned to 1).
                .requiredSize(ART_W, ART_H)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .clip(RoundedCornerShape(40.dp))
                .background(Cream),
            content = content,
        )
    }
}

/* ---------------------------------------------------------------------------
   05-custom entry point. Initial state == the HTML debug URL: Americano,
   CLASSICS selected, modal open, 3 x 3 option grid, CHF 3.50.
   Closing the modal returns to the shopping grid; tapping a card reopens the
   modal for that drink (so Caramel Macchiato's 4-step sweetness is reachable).
   --------------------------------------------------------------------------- */
@Composable
fun CustomScreen(
    modifier: Modifier = Modifier,
    initialProductId: String = "americano",
    startWithModal: Boolean = true,
    langLabel: String = "English",
    langFlag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    cartCount: Int = 0,
    cartTotal: Double = 0.0,
    cartPulse: Int = 0,
    onAddToCart: (CartItem) -> Unit = {},
    onCheckout: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
) {
    var productId by remember { mutableStateOf(initialProductId) }
    var category by remember { mutableStateOf(UI_ALL) }
    var showModal by remember { mutableStateOf(startWithModal) }
    var selections by remember {
        mutableStateOf(defaultSelections(CATALOG.first { it.id == initialProductId }))
    }

    val product = CATALOG.first { it.id == productId }
    val groups = schemaOf(product)
    val price = unitPriceOf(product, selections)

    fun openProduct(next: Product) {
        productId = next.id
        selections = defaultSelections(next)
        showModal = true
    }

    Artboard(modifier = modifier) {
        KioskBackground(
            category = category,
            onCategory = { category = it },
            onProduct = { if (!showModal) openProduct(it) },
            langLabel = langLabel,
            langFlag = langFlag,
            onLang = onLang,
            onHelp = onHelp,
        )

        if (showModal) {
            // Full-screen scrim also blocks taps on the background grid.
            Box(Modifier.fillMaxSize().background(Scrim).clickable { })
            ConfigModal(
                product = product,
                groups = groups,
                selections = selections,
                price = price,
                onSelect = { group, option ->
                    selections = selections.toMutableList().also { it[group] = option }
                },
                onClose = { showModal = false },
                onAdd = {
                    // v15.5: add → dismiss the config modal → open & expand the cart sheet
                    onAddToCart(CartItem(product, selections.toList()))
                    showModal = false
                    onCheckout()
                },
                onConfirm = {
                    onAddToCart(CartItem(product, selections.toList()))
                    onCheckout()
                },
            )
        } else if (cartCount > 0) {
            // floating cart bar (v14: 04-shopping shows a "View Cart" strip)
            // v23: `key(pulse)` remounts the bar on every add / qty change so the
            // badge + bar pulse restarts from zero. Only the very first mount
            // (pulse == 0) plays the slide-up entry animation.
            key(cartPulse) {
                CartBar(
                    count = cartCount, total = cartTotal,
                    animateIn = cartPulse == 0, onClick = onCartClick,
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------
   Background = 04-shopping (v14). All coordinates from the CDP probe of
   pages/05-custom.html?debug=1.
   --------------------------------------------------------------------------- */
@Composable
internal fun KioskBackground(
    category: String,
    onCategory: (String) -> Unit,
    onProduct: (Product) -> Unit,
    langLabel: String = "English",
    langFlag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    onLang: () -> Unit = {},
    onHelp: () -> Unit = {},
) {
    /* v17 · one scrolling page, one section per category. Each section renders
       only its real products; rows of 1/2/3 cards never stretch and every row
       is centred. The 0..495 artboard top and the cart strip at y=1659 are
       untouched; everything below the category bar is vertically compacted. */
    val grid = remember {
        // item 0 is the ALL DRINKS head; the "All" tab pins it.
        // Four real display tabs below it, each with its relabelled products.
        val secs = UI_CATS.filter { it != UI_ALL }.map { tab ->
            val list = productsForUiCat(tab)
            GridSection(tab, list, adaptiveRows(list.size))
        }
        val idx = mutableMapOf<String, Int>(UI_ALL to 0)
        var c = 1
        secs.forEach { idx[it.cat] = c; c += 1 + it.rows.size }
        secs to idx
    }
    val sections = grid.first
    val headerIndex = grid.second
    val gridState = rememberLazyListState()
    var scrollTarget by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(scrollTarget) {
        val cat = scrollTarget ?: return@LaunchedEffect
        val i = headerIndex[cat] ?: return@LaunchedEffect
        gridState.animateScrollToItem(i)
    }

    /* v22 · banner collapse. Once true it NEVER goes back: picking any category
       OR scrolling the product list fades the 360px ad banner out and slides the
       category bar + grid up by 360px to fill the freed space (grid grows 360px
       taller so its bottom still lands at y=1920). 0 = expanded, 1 = collapsed. */
    var bannerCollapsed by remember { mutableStateOf(false) }
    val collapse by animateFloatAsState(
        targetValue = if (bannerCollapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "bannerCollapse",
    )
    // Any real scroll of the product grid collapses the banner for good.
    LaunchedEffect(Unit) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (idx, offset) ->
                if (idx > 0 || offset > 8) bannerCollapsed = true
            }
    }
    val bannerSlide = 360f * collapse

    // status row y = 0..66
    StatusText("MACHINE ID · JAV-08-221", 64f, 280f)
    StatusText("SUPPORT · +86 400 820 2006", 359f, 340f)
    StatusText("14:00", 924f, 100f)
    Box(Modifier.offset(0.dp, 66.dp).size(ART_W, 1.dp).background(Hairline))

    // header y = 66..130: logo + English / Help pills
    Image(
        painter = painterResource(R.drawable.kiosk_logo),
        contentDescription = "Jinuo Coffee",
        modifier = Modifier.offset(64.dp, 73.dp).size(136.dp, 64.dp),
        contentScale = ContentScale.FillBounds,
    )
    TopPill(x = 485f, text = langLabel, flag = langFlag, onClick = onLang)
    TopPill(x = 755f, text = "Help", withFlag = false, onClick = onHelp)

    // banner y = 132..492 — v22: fades out on collapse (never restored)
    Image(
        painter = painterResource(R.drawable.kiosk_banner),
        contentDescription = null,
        modifier = Modifier.offset(0.dp, 132.dp).size(1080.dp, 360.dp)
            .alpha(1f - collapse),
        contentScale = ContentScale.Crop,
    )

    // category bar y = 495..559 (v17: 64px tall, was 77)
    // v22: slides up with the banner collapse to fill the freed 360px.
    Box(
        Modifier.offset(0.dp, (495f - bannerSlide).dp).size(1080.dp, 64.dp)
            .background(Cream)
            .border(1.dp, Hairline),
    )
    // v18 · category tabs: text auto-fits (no truncation) and the strip scrolls
    // horizontally so longer labels stay fully readable.
    val catScroll = rememberScrollState()
    Row(
        modifier = Modifier.offset(40.dp, (496f - bannerSlide).dp).size(1000.dp, 60.dp)
            .horizontalScroll(catScroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UI_CATS.forEach { label ->
            val selected = label == category
            val w = catTabWidth(label)
            Box(
                modifier = Modifier
                    .size(w.dp, 60.dp)
                    .clickable {
                        onCategory(label)
                        scrollTarget = label
                        bannerCollapsed = true   // v22: any category tap collapses banner
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = InstrumentSans,
                    fontWeight = FontWeight.W600,
                    fontSize = 20.sp,
                    letterSpacing = 0.6.sp,
                    color = Ink.copy(alpha = if (selected) 1f else 0.45f),
                    maxLines = 1,
                    softWrap = false,
                )
                if (selected) {
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Ink),
                    )
                }
            }
        }
    }

    // v17 · one scrolling column of category sections, vertically compacted.
    // Card = 241 x 277 (uniform 0.80 scale of the Figma 301.34 x 346 token),
    // 1:1 image at 241, 19px gutters. Every row is centred inside the 952px
    // column so full and short rows share the same optical axis.
    LazyColumn(
        state = gridState,
        modifier = Modifier.offset(64.dp, (559f - bannerSlide).dp)
            .size(952.dp, (1361f + bannerSlide).dp),
        userScrollEnabled = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        /* Tail space must exceed the viewport height (1361dp) minus the last
           section's remaining content, otherwise FRAPPE cannot pin to the top. */
        contentPadding = PaddingValues(top = 8.dp, bottom = 1000.dp),
    ) {
        item(key = "all-head") {
            GridHead("ALL DRINKS", "${CATALOG.size} ITEMS")
        }
        var slot = 0
        sections.forEach { section ->
            item(key = "head-${section.cat}") {
                GridHead(section.cat, "${section.products.size} ITEMS", section = true)
            }
            var from = 0
            section.rows.forEachIndexed { rowNo, count ->
                val rowItems = section.products.subList(from, from + count)
                val firstSlot = slot + from
                item(key = "row-${section.cat}-$rowNo") {
                    /* v17 · three scaled cards no longer span the full 952px,
                       so every row (full or short) is centred. Cards keep their
                       fixed 241px width and are never stretched. */
                    Row(
                        modifier = Modifier.fillMaxWidth().height(277.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Top,
                    ) {
                        rowItems.forEachIndexed { i, product ->
                            if (i > 0) Spacer(Modifier.width(19.dp))
                            Box(Modifier.width(241.dp).height(277.dp)) {
                                ProductCard(firstSlot + i, product, onProduct)
                            }
                        }
                    }
                }
                from += count
            }
            slot += section.products.size
        }
        item(key = "end") {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                Text(
                    "END OF MENU",
                    fontFamily = InstrumentSans,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    letterSpacing = 2.2.sp,
                    color = Muted,
                )
            }
        }
    }
}

/** v18 · card promo labels, cycled by slot. The long price badge is kept
    on one line at 11sp so it never clips inside the 241px card. */
internal val CARD_BADGES = listOf(
    "2nd Cup 1.00 CHF",
    "Promotion",
    "Bestseller",
    "Seasonal",
)

/** Tab width follows the glyph count at 20sp Instrument Sans + padding, so
    every display label is shown in full and the strip can scroll when needed. */
private fun catTabWidth(label: String): Float =
    (label.length * 12.4f + 40f).coerceAtLeast(92f)

/** Row sizes for n products; a remainder of 1 becomes 2 + 2 so no lonely card. */
private fun adaptiveRows(n: Int): List<Int> {
    if (n <= 0) return emptyList()
    if (n <= 3) return listOf(n)
    val full = n / 3
    return when (n % 3) {
        0 -> List(full) { 3 }
        2 -> List(full) { 3 } + 2
        else -> List(full - 1) { 3 } + listOf(2, 2)
    }
}

private data class GridSection(val cat: String, val products: List<Product>, val rows: List<Int>)

@Composable
private fun GridHead(title: String, count: String, section: Boolean = false) {
    /* v17 · spacedBy(16) is the only gap between sections, so a 36dp head
       here reproduces the 16px HTML category margin. ALL DRINKS is 32dp. */
    Box(
        Modifier.fillMaxWidth().height(if (section) 36.dp else 32.dp),
    ) {
        Text(
            title,
            Modifier.align(Alignment.CenterStart),
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.W600,
            fontSize = 21.sp,
            letterSpacing = 2.4.sp,
            color = Ink,
        )
        Text(
            count,
            Modifier.align(Alignment.CenterEnd),
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.W600,
            fontSize = 16.sp,
            letterSpacing = 1.4.sp,
            color = Muted,
        )
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp)
                .background(Hairline),
        )
    }
}

@Composable
internal fun StatusText(text: String, x: Float, w: Float) {
    Text(
        text = text,
        modifier = Modifier.offset(x.dp, 21.dp).size(w.dp, 24.dp),
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        letterSpacing = 1.92.sp,
        lineHeight = 24.sp,
        color = Muted,
        maxLines = 1,
    )
}

@Composable
internal fun TopPill(
    x: Float,
    text: String,
    withFlag: Boolean = true,
    flag: String = "\uD83C\uDDEC\uD83C\uDDE7",
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier.offset(x.dp, 71.dp).size(250.dp, 55.dp)
            .clip(CircleShape)
            .background(WarmSand)
            .border(1.dp, Hairline, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (withFlag) {
                Text(flag, fontSize = 20.sp)
            } else {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).border(1.dp, SoftBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", fontSize = 15.sp, color = Ink, fontFamily = InstrumentSans)
                }
            }
            Text(
                text = text,
                modifier = Modifier.padding(start = 12.dp),
                fontFamily = InstrumentSans,
                fontWeight = FontWeight.W600,
                fontSize = 21.sp,
                color = Ink,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun ProductCard(index: Int, product: Product, onProduct: (Product) -> Unit) {
    // v18 · four promo labels cycling across the catalogue.
    val badge = CARD_BADGES[index % CARD_BADGES.size]
    val badgeHot = badge == "Promotion" || badge == "2nd Cup 1.00 CHF"
    /* v17 · uniform 0.80 scale of the Figma card. The image stays square
       (241 x 241) and the glass plate sits at y=205 with height 72. */
    Box(
        modifier = Modifier.fillMaxWidth().height(277.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Cream)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .clickable { onProduct(product) },
    ) {
        Image(
            painter = painterResource(product.card),
            contentDescription = product.name,
            modifier = Modifier.fillMaxWidth().height(241.dp).background(WarmSand),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier.offset(12.dp, 16.dp)
                .clip(CircleShape)
                .background(if (badgeHot) Caramel else WarmSand)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                badge,
                fontFamily = InstrumentSans,
                fontWeight = FontWeight.W600,
                fontSize = 11.sp,
                color = if (badgeHot) Cream else Ink,
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(
            modifier = Modifier.offset(0.dp, 205.dp).fillMaxWidth().height(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Cream.copy(alpha = 0.85f)),
        ) {
            Text(
                product.name,
                Modifier.align(Alignment.TopCenter).padding(top = 10.dp).fillMaxWidth(),
                fontFamily = Fraunces,
                fontSize = 19.sp,
                lineHeight = 23.sp,
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                money(product.price),
                Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).fillMaxWidth(),
                fontFamily = Fraunces,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                color = PriceRed,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/* ---------------------------------------------------------------------------
   The white 760 x 1024 configuration modal at (160, 448).
   --------------------------------------------------------------------------- */
@Composable
private fun ConfigModal(
    product: Product,
    groups: List<OptionGroup>,
    selections: List<Int>,
    price: Double,
    onSelect: (Int, Int) -> Unit,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier.offset(160.dp, 448.dp).size(760.dp, 1024.dp)
            .shadow(24.dp, RoundedCornerShape(34.dp), ambientColor = ModalShadow, spotColor = ModalShadow)
            .clip(RoundedCornerShape(34.dp))
            .background(Cream),
    ) {
        // title (204, 489) -> (44, 41)
        Text(
            product.name,
            Modifier.offset(44.dp, 41.dp).size(600.dp, 52.dp),
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleFontSize(product.name),
            lineHeight = 50.sp,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )

        // close (820, 488) -> (660, 40)
        Box(
            modifier = Modifier.offset(660.dp, 40.dp).size(56.dp, 56.dp)
                .clip(CircleShape)
                .border(1.dp, SoftBorder, CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            CloseIcon()
        }

        // hero (204, 572) -> (44, 124), 672 x 385
        Box(
            modifier = Modifier.offset(44.dp, 124.dp).size(672.dp, 385.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(WarmSand),
        ) {
            Image(
                painter = painterResource(product.hero),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        // Plan A option rows: title 24 px, then 52 px chips split by N.
        groups.forEachIndexed { groupIndex, group ->
            val rowY = 521f + groupIndex * 88f
            Text(
                group.title,
                Modifier.offset(44.dp, rowY.dp).size(672.dp, 24.dp),
                fontFamily = Fraunces,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                color = Ink,
                maxLines = 1,
            )
            val n = group.options.size
            val gap = 12f
            val chipW = (672f - gap * (n - 1)) / n
            group.options.forEachIndexed { optionIndex, label ->
                val chipX = 44f + optionIndex * (chipW + gap)
                val selected = selections.getOrElse(groupIndex) { 1 } == optionIndex
                Box(
                    modifier = Modifier.offset(chipX.dp, (rowY + 30f).dp).size(chipW.dp, 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Caramel else ChipBg)
                        .clickable { onSelect(groupIndex, optionIndex) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        Modifier.padding(horizontal = 6.dp),
                        fontFamily = Fraunces,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = chipFontSize(label, chipW),
                        lineHeight = chipFontSize(label, chipW) * 1.15f,
                        color = if (selected) Cream else Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }

        // unit-price bar (204, 1245) -> (44, 797), 672 x 52, English only
        Box(
            modifier = Modifier.offset(44.dp, 797.dp).size(672.dp, 52.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = CardShadow, spotColor = CardShadow)
                .clip(RoundedCornerShape(18.dp))
                .background(Cream.copy(alpha = 0.85f))
                .padding(horizontal = 18.dp),
        ) {
            Text(
                "Price",
                Modifier.align(Alignment.CenterStart),
                fontFamily = Fraunces,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = Ink,
            )
            Text(
                money(price),
                Modifier.align(Alignment.CenterEnd),
                fontFamily = Fraunces,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                color = PriceRed,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }

        // CTAs (204 / 550, 1337) -> (44 / 390, 889), 326 x 72
        CtaButton(44f, "Add to Cart", Caramel, onAdd)
        CtaButton(390f, "Confirm", Ink, onConfirm)
    }
}

@Composable
internal fun CtaButton(x: Float, label: String, background: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.offset(x.dp, 889.dp).size(326.dp, 72.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, if (background == Caramel) SoftBorder else background, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.W600,
            fontSize = 32.sp,
            color = Cream,
            maxLines = 1,
        )
    }
}

/**
 * v23 · floating cart strip on 04-shopping (90,1659 900x96), matched to the
 * reference HTML (pages/04-shopping.html + refine.css) both visually and in
 * motion:
 *   - deep-red fill rgba(160,0,45,.75), Tim-Hortons cup art at the left,
 *     brown (#C07A3E) round qty badge with a white number, "View Cart" label,
 *     price and a brown arrow chip — all at the Figma pixel coordinates.
 *   - ENTRY: slides up 190px + fades in (.34s) the first time the cart becomes
 *     non-empty (CSS `.k-cart-empty` transform/opacity transition).
 *   - PULSE: on every add / qty change the badge scales 1→1.75→.94→1.5→1 and
 *     turns gold (#FFE7A6) with a glow, while the bar grows a 6px gold ring —
 *     `@keyframes k-cart-pulse` / `k-bar-pulse`, .8s × 2, ease-in-out.
 */
@Composable
internal fun CartBar(count: Int, total: Double, animateIn: Boolean = false, onClick: () -> Unit) {
    val gold = androidx.compose.ui.graphics.Color(0xFFFFE7A6)
    val glowGold = androidx.compose.ui.graphics.Color(0xFFFFD166)
    val redFill = androidx.compose.ui.graphics.Color(0xBFA0002D)  // rgba(160,0,45,.75)
    val brown = androidx.compose.ui.graphics.Color(0xFFC07A3E)

    // The caller remounts this composable with key(pulse), so each fresh
    // instance drives p 0→1 once. A frame loop in LaunchedEffect guarantees
    // recombination on every tick (this Compose version's animateFloatAsState
    // has no initialValue and would skip the first animation).
    var p by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val frames = 40
        for (f in 0..frames) {
            p = f / frames.toFloat()
            delay(40)   // ~1.6s total
        }
    }
    // CSS runs the .8s keyframes twice; fold the 1.6s timeline into one cycle.
    val t = (p * 2f) % 1f
    // @keyframes k-cart-pulse: 0:1, .22:1.75, .46:.94, .70:1.5, 1:1
    val badgeScale = cartKeyScale(t)
    // gold at the two peaks (.22 / .70), white at 0/.46/1
    val goldMix = cartKeyGold(t)
    // @keyframes k-bar-pulse: ring 0 at 0/1, full at .30/.70
    val barRing = cartRing(t)

    // entry: only on the first mount (pulse == 0); pulse remounts start at 1.
    var entry by remember { mutableStateOf(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animateIn) return@LaunchedEffect
        for (f in 0..12) {
            entry = (f / 12f) * (f / 12f) * (3f - 2f * (f / 12f)) // ease-out
            delay(28)   // ~340ms
        }
        entry = 1f
    }

    Box(
        modifier = Modifier
            .offset(90.dp, (1659f + 190f * (1f - entry)).dp)
            .size(900.dp, 96.dp)
            .alpha(entry)
            .shadow(24.dp, RoundedCornerShape(16.dp), spotColor = androidx.compose.ui.graphics.Color(0x801D130C))
            .drawBehind {
                if (barRing > 0.001f) {
                    drawRoundRect(
                        color = glowGold.copy(alpha = 0.55f * barRing),
                        topLeft = androidx.compose.ui.geometry.Offset(-6.dp.toPx(), -6.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            size.width + 12.dp.toPx(), size.height + 12.dp.toPx()
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(6.dp.toPx()),
                    )
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(redFill)
            .border(1.48.dp, androidx.compose.ui.graphics.Color(0x1A3A2A1E), RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        // cup art: f253 at (0,0) inside f252 (21.48,-2) — 76x100, cropped
        Image(
            painter = painterResource(R.drawable.cart_cup),
            contentDescription = null,
            modifier = Modifier.offset(21.dp, (-2).dp).size(76.dp, 100.dp),
            contentScale = ContentScale.Crop,
        )
        // "View Cart" — f255 relative (111.48,35.5) 149x25, Fraunces 32 w600 white
        Text(
            "View Cart",
            modifier = Modifier.offset(111.dp, 28.dp).size(220.dp, 44.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 32.sp,
            color = Cream,
        )
        // price — f259 relative (711.52,35.5) 103x25; keep clear of the arrow chip
        Text(
            money(total),
            modifier = Modifier.offset(690.dp, 30.dp).size(132.dp, 40.dp),
            fontFamily = Fraunces, fontWeight = FontWeight.W600, fontSize = 27.sp,
            color = Cream, textAlign = TextAlign.End,
        )
        // arrow chip — f261 relative (742.52,24) 48x48 radius14 brown
        Box(
            modifier = Modifier.offset(832.dp, 24.dp).size(48.dp, 48.dp)
                .clip(RoundedCornerShape(14.dp)).background(brown),
            contentAlignment = Alignment.Center,
        ) {
            Text("→", fontFamily = InstrumentSans, fontSize = 30.sp, color = Cream)
        }
        // qty badge — f264 relative (65,-6) 40x40 brown circle, white Fraunces 32
        Box(
            modifier = Modifier.offset(65.dp, (-6).dp).size(40.dp, 40.dp)
                .graphicsLayer {
                    scaleX = badgeScale; scaleY = badgeScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                }
                .shadow(8.dp, CircleShape, spotColor = glowGold.copy(alpha = 0.9f * goldMix))
                .clip(CircleShape).background(brown),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                count.toString(),
                fontFamily = Fraunces, fontSize = 28.sp,
                color = if (goldMix > 0.5f) gold else Cream,
            )
        }
    }
}

/** Piecewise-linear interpolation of the CSS k-cart-pulse scale keyframes. */
private fun cartKeyScale(t: Float): Float = when {
    t < 0.22f -> 1f + 0.75f * (t / 0.22f)
    t < 0.46f -> 1.75f + (0.94f - 1.75f) * ((t - 0.22f) / 0.24f)
    t < 0.70f -> 0.94f + (1.5f - 0.94f) * ((t - 0.46f) / 0.24f)
    else -> 1.5f + (1f - 1.5f) * ((t - 0.70f) / 0.30f)
}

/** 0=white, 1=gold at the two pulse peaks. */
private fun cartKeyGold(t: Float): Float = when {
    t < 0.22f -> t / 0.22f
    t < 0.46f -> 1f - (t - 0.22f) / 0.24f
    t < 0.70f -> (t - 0.46f) / 0.24f
    else -> 1f - (t - 0.70f) / 0.30f
}

/** 0→1 ring at 30%, hold to 70%, back to 0 (HTML k-bar-pulse). */
private fun cartRing(t: Float): Float = when {
    t < 0.30f -> t / 0.30f
    t < 0.70f -> 1f
    else -> 1f - (t - 0.70f) / 0.30f
}

@Composable
internal fun CloseIcon() {
    Canvas(Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(Ink, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = stroke)
        drawLine(Ink, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = stroke)
    }
}

/** English labels are never ellipsised: step the type down instead. */
private fun chipFontSize(label: String, width: Float): TextUnit = when {
    width >= 200f -> 24.sp
    label.length > 12 -> 18.sp
    label.length > 9 -> 20.sp
    else -> 24.sp
}

/** Long product names must not be clipped in the 600 px title strip. */
private fun titleFontSize(name: String): TextUnit = when {
    name.length <= 12 -> 48.sp
    name.length <= 16 -> 40.sp
    else -> 36.sp
}

/* ---------------------------------------------------------------------------
   Android Studio previews (no device required).
   --------------------------------------------------------------------------- */
@androidx.compose.ui.tooling.preview.Preview(widthDp = 540, heightDp = 960, showBackground = true)
@Composable
private fun PreviewCustomScreen() {
    com.jinuo.kiosk.ui.theme.KioskTheme { CustomScreen() }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Caramel Macchiato · 4-step sweetness",
    widthDp = 540,
    heightDp = 960,
    showBackground = true,
)
@Composable
private fun PreviewCaramelFourStep() {
    com.jinuo.kiosk.ui.theme.KioskTheme {
        CustomScreen(initialProductId = "caramel-macchiato")
    }
}
