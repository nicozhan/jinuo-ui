package com.jinuo.kiosk.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Contactless card-tap animation, native Compose Canvas (no WebView / GIF).
 *
 * 680x680 viewBox:
 *   - static induction ellipse cx=340 cy=340 rx=230 ry=134
 *   - halo ellipse fading in/out on tap
 *   - THREE SIGNAL BARS: separate vertical strokes travelling strictly LEFT
 *     (horizontal translation only, no diagonal drift, never overlapping)
 *   - card slides in from the right, taps, slides back
 * 2.8s infinite loop.
 */
@Composable
fun TapCardAnimation(modifier: Modifier = Modifier) {
    val progress by rememberInfiniteTransition(label = "tapCard").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tapProgress",
    )

    Canvas(modifier = modifier) {
        val s = size.minDimension / 680f
        val ink = Color(0xFF141414)

        scale(s) {
            // static induction ellipse
            drawOval(
                topLeft = Offset(110f, 206f), size = Size(460f, 268f),
                color = ink, style = Stroke(width = 10f),
            )

            // halo on tap
            val haloA = haloAlpha(progress)
            if (haloA > 0f) {
                drawOval(
                    topLeft = Offset(110f, 206f), size = Size(460f, 268f),
                    color = ink.copy(alpha = haloA), style = Stroke(width = 22f),
                )
            }

            // three signals: identical leftward motion keeps the 38px pitch
            // constant; only the fade is staggered, so the bars never cross.
            val dx = barDx(progress)
            drawSignal(BAR_1, barAlpha(progress, 0) to dx, ink)
            drawSignal(BAR_2, barAlpha(progress, 1) to dx, ink)
            drawSignal(BAR_3, barAlpha(progress, 2) to dx, ink)

            // card group: 0-10% parked right, 32-60% tapped in, 86-100% away
            translate(cardOffset(progress), 0f) {
                translate(30f, 160f) {
                    rotate(-20f, Offset(450f, 140f)) {
                        drawRoundRect(
                            topLeft = Offset(395f, 104f), size = Size(108f, 72f),
                            cornerRadius = CornerRadius(9f), color = Color.White,
                        )
                        drawRoundRect(
                            topLeft = Offset(395f, 104f), size = Size(108f, 72f),
                            cornerRadius = CornerRadius(9f), color = ink,
                            style = Stroke(width = 9f),
                        )
                        drawLine(
                            color = ink, start = Offset(408f, 126f), end = Offset(438f, 126f),
                            strokeWidth = 8f, cap = StrokeCap.Round,
                        )
                    }
                    drawPath(HAND, ink, style = Stroke(11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(CUFF_1, ink, style = Stroke(11f, cap = StrokeCap.Round))
                    drawPath(CUFF_2, ink, style = Stroke(11f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

/**
 * All three bars share ONE leftward X translation (+34 -> -126 in viewBox px),
 * so their 38px pitch is invariant and they can never overlap. Y is untouched.
 */
private fun barDx(p: Float): Float = when {
    p <= .31f -> 34f
    p < .40f -> 34f - 126f * ease((p - .31f) / .09f)
    p < .55f -> -92f - 34f * ease((p - .40f) / .15f)
    else -> -126f
}

/** Only the opacity is staggered, giving a left-travelling ripple across |||. */
private fun barAlpha(p: Float, index: Int): Float {
    val hold = .30f + index * .035f
    val peak = .40f + index * .035f
    val fade = .55f + index * .035f
    return when {
        p <= hold || p >= fade -> 0f
        p < peak -> ease((p - hold) / (peak - hold))
        else -> 1f - ease((p - peak) / (fade - peak))
    }
}

private fun haloAlpha(p: Float): Float = when {
    p <= .32f || p >= .64f -> 0f
    p < .44f -> .3f * ease((p - .32f) / .12f)
    else -> .3f * (1f - ease((p - .44f) / .20f))
}

private fun cardOffset(p: Float): Float = when {
    p <= .10f -> 76f
    p < .32f -> 76f * (1f - ease((p - .10f) / .22f))
    p <= .60f -> 0f
    p < .86f -> 76f * ease((p - .60f) / .26f)
    else -> 76f
}

private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** Draw one vertical bar shifted only along X — no scale, no Y drift. */
private fun DrawScope.drawSignal(path: Path, state: Pair<Float, Float>, ink: Color) {
    val (alpha, dx) = state
    if (alpha <= 0.001f) return
    translate(dx, 0f) {
        drawPath(path, ink.copy(alpha = alpha), style = Stroke(12f, cap = StrokeCap.Round))
    }
}

/* Three separate vertical bars ("|||"), 38px apart in viewBox space. The card
   taps from the right, so the bars slide along -X into the reader. The 38px
   pitch is wider than the 12px stroke, so they can never overlap. */
private val BAR_1 = Path().apply {
    moveTo(372f, 316f); lineTo(372f, 364f)
}
private val BAR_2 = Path().apply {
    moveTo(334f, 310f); lineTo(334f, 370f)
}
private val BAR_3 = Path().apply {
    moveTo(296f, 316f); lineTo(296f, 364f)
}
private val HAND = Path().apply {
    moveTo(421f, 181f)
    cubicTo(440f, 176f, 456f, 180f, 468f, 192f)
    lineTo(498f, 222f)
    cubicTo(506f, 230f, 504f, 240f, 494f, 244f)
    lineTo(470f, 253f)
    cubicTo(460f, 257f, 450f, 253f, 445f, 244f)
    lineTo(428f, 214f)
}
private val CUFF_1 = Path().apply {
    moveTo(403f, 195f); cubicTo(419f, 191f, 431f, 195f, 441f, 205f)
}
private val CUFF_2 = Path().apply {
    moveTo(441f, 228f); lineTo(463f, 219f)
}
