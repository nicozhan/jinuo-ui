package com.jinuo.kiosk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinuo.kiosk.ui.theme.Caramel
import com.jinuo.kiosk.ui.theme.Cream
import com.jinuo.kiosk.ui.theme.Hairline
import com.jinuo.kiosk.ui.theme.Ink
import com.jinuo.kiosk.ui.theme.InstrumentSans

/**
 * Transparent sibling of [Artboard]: same 1080x1920 scaling, but no opaque
 * background / clip so an overlay can be stacked on top of the current page.
 */
@Composable
internal fun OverlayBoard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val scale = minOf(maxWidth / ART_W, maxHeight / ART_H)
        val left = (maxWidth - ART_W * scale) / 2
        val top = (maxHeight - ART_H * scale) / 2
        Box(
            modifier = Modifier
                .offset(left, top)
                .requiredSize(ART_W, ART_H)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            content = content,
        )
    }
}

/** HTML #k-scrim at rgba(26,18,11,.38). */
internal val OverlayScrim = Color(0x611A120B)

/**
 * Language dropdown matching HTML #k-lang: 360px card, "Select language"
 * header, three flag rows, current language checked in caramel. Tapping the
 * scrim dismisses. [x]/[y] mirror langPos() in kiosk.js.
 */
@Composable
internal fun LangPicker(
    x: Float,
    y: Float,
    selectedCode: String,
    onSelect: (LangOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayBoard(modifier = modifier) {
        val px = x.coerceAtMost(700f) // keep the 360px card inside the 1080px board
        val py = y.coerceAtMost(1600f)
        // dim whatever page is underneath
        Box(
            Modifier.size(ART_W, ART_H)
                .background(OverlayScrim)
                .clickable { onDismiss() },
        )
        Column(
            modifier = Modifier
                .offset(px.dp, py.dp)
                .width(360.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Cream)
                .shadow(26.dp, RoundedCornerShape(22.dp), spotColor = Color(0x331A120B))
                .padding(vertical = 10.dp),
        ) {
            Text(
                "Select language",
                modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 12.dp, bottom = 12.dp),
                fontFamily = InstrumentSans,
                fontWeight = FontWeight.W600,
                fontSize = 16.sp,
                letterSpacing = 2.24.sp,
                color = androidx.compose.ui.graphics.Color(0xFF8C7157),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            LANG_OPTIONS.forEach { lg ->
                val selected = lg.code == selectedCode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .clickable { onSelect(lg) }
                        .padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lg.flag, fontSize = 26.sp, modifier = Modifier.width(36.dp))
                    Text(
                        lg.label,
                        fontSize = 25.sp,
                        fontFamily = InstrumentSans,
                        fontWeight = if (selected) FontWeight.W600 else FontWeight.W400,
                        color = if (selected) Caramel else Ink,
                    )
                    if (selected) {
                        Text(
                            "✓",
                            fontSize = 20.sp,
                            color = Caramel,
                            modifier = Modifier.weight(1f).wrapContentWidth(Alignment.End),
                        )
                    }
                }
            }
        }
    }
}
