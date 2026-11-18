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
 *   - THREE SIGNAL ARCS that travel strictly LEFT (horizontal translation,
 *     no diagonal drift) with staggered fade in/out
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

            // three signals: each slides LEFT along the X axis only.
            drawSignal(WAVE_1, signalState(progress, .31f, .40f, .55f), ink)
            drawSignal(WAVE_2, signalState(progress, .37f, .46f, .61f), ink)
            drawSignal(WAVE_3, signalState(progress, .43f, .52f, .67f), ink)

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
 * [alpha, leftward dx] for one signal. dx is a pure X translation in viewBox
 * px: +34 (start, near the card) -> -92 (end, left of centre). Y is untouched,
 * so the arc never drifts up-left.
 */
private fun signalState(p: Float, hold: Float, peak: Float, fade: Float): Pair<Float, Float> = when {
    p <= hold -> 0f to 34f
    p < peak -> {
        val t = ease((p - hold) / (peak - hold))
        t to (34f - 126f * t)
    }
    p < fade -> {
        val t = ease((p - peak) / (fade - peak))
        (1f - t) to (-92f - 34f * t)
    }
    else -> 0f to -126f
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

/** Draw one arc shifted only along X — no scale, no Y drift. */
private fun DrawScope.drawSignal(path: Path, state: Pair<Float, Float>, ink: Color) {
    val (alpha, dx) = state
    if (alpha <= 0.001f) return
    translate(dx, 0f) {
        drawPath(path, ink.copy(alpha = alpha), style = Stroke(10f, cap = StrokeCap.Round))
    }
}

/* Three arcs bulging LEFT ("("), symmetric about y=340. The card taps from
   the right, so a left-facing arc sliding along -X reads as a signal travelling
   leftward into the reader. No Y component anywhere in the animation. */
private val WAVE_1 = Path().apply {
    moveTo(372f, 314f); quadraticBezierTo(352f, 340f, 372f, 366f)
}
private val WAVE_2 = Path().apply {
    moveTo(344f, 320f); quadraticBezierTo(326f, 340f, 344f, 360f)
}
private val WAVE_3 = Path().apply {
    moveTo(318f, 324f); quadraticBezierTo(302f, 340f, 318f, 356f)
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
