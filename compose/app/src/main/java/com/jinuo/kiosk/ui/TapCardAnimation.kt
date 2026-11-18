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
 * Contactless card-tap animation, ported 1:1 from the reference SVG/CSS
 * (刷卡动画.html) into a native Compose Canvas — no WebView, no static image.
 *
 * The source uses a 680×680 viewBox:
 *   - ellipse  cx=340 cy=340 rx=230 ry=134  (the reader induction zone)
 *   - halo     same ellipse, thick stroke fading in/out
 *   - w1/w2/w3 three "wave" arcs scaling out from the ellipse centre
 *   - card     a tilted card + hand group sliding in from x=+76 to x=0
 * The whole loop is 2.8s and repeats forever, exactly like the HTML version.
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

        // The SVG viewBox is 680×680; scale everything into the canvas box.
        scale(s) {
            // static induction ellipse
            drawOval(
                topLeft = Offset(110f, 206f), size = Size(460f, 268f),
                color = ink, style = Stroke(width = 10f),
            )

            // halo: 0→32% invisible, 44% alpha .3, 64%→ invisible
            val haloA = haloAlpha(progress)
            if (haloA > 0f) {
                drawOval(
                    topLeft = Offset(110f, 206f), size = Size(460f, 268f),
                    color = ink.copy(alpha = haloA), style = Stroke(width = 22f),
                )
            }

            // three waves, each with its own delayed start / peak / fade
            drawWave(WAVE_1, 366.5f, 340f, waveState(progress, .31f, .40f, .55f), ink)
            drawWave(WAVE_2, 340.5f, 340f, waveState(progress, .37f, .46f, .61f), ink)
            drawWave(WAVE_3, 314.5f, 340f, waveState(progress, .43f, .52f, .67f), ink)

            // card group: 0–10% parked right, 32–60% tapped in, 86–100% away
            translate(cardOffset(progress), 0f) {
                translate(30f, 160f) {
                    // rotated plastic card (rotate -20° around 450,140)
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
                    // hand / tap gesture strokes
                    drawPath(HAND, ink, style = Stroke(11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(CUFF_1, ink, style = Stroke(11f, cap = StrokeCap.Round))
                    drawPath(CUFF_2, ink, style = Stroke(11f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

/** returns [alpha, scale] for one wave at the current loop progress */
private fun waveState(p: Float, hold: Float, peak: Float, fade: Float): Pair<Float, Float> = when {
    p <= hold -> 0f to .7f
    p < peak -> {
        val t = ease((p - hold) / (peak - hold))
        t to (.7f + .3f * t)
    }
    p < fade -> {
        val t = ease((p - peak) / (fade - peak))
        (1f - t) to (1f + .25f * t)
    }
    else -> 0f to 1.25f
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

/** smoothstep — close to the CSS cubic-bezier(.45,0,.25,1) used for the card */
private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun DrawScope.drawWave(path: Path, cx: Float, cy: Float, state: Pair<Float, Float>, ink: Color) {
    val (alpha, sc) = state
    if (alpha <= 0.001f) return
    translate(cx, cy) {
        scale(sc) {
            translate(-cx, -cy) {
                drawPath(path, ink.copy(alpha = alpha), style = Stroke(10f, cap = StrokeCap.Round))
            }
        }
    }
}

private val WAVE_1 = Path().apply {
    moveTo(362f, 328f); quadraticBezierTo(380f, 340f, 362f, 352f)
}
private val WAVE_2 = Path().apply {
    moveTo(334f, 316f); quadraticBezierTo(360f, 340f, 334f, 364f)
}
private val WAVE_3 = Path().apply {
    moveTo(306f, 304f); quadraticBezierTo(340f, 340f, 306f, 376f)
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
