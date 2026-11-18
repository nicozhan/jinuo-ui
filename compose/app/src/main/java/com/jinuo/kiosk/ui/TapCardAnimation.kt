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
 * Contactless card-tap animation — a strict 1:1 native Compose Canvas port of
 * the reference `刷卡动画.html` (pure SVG + CSS), no WebView / GIF.
 *
 * 680x680 viewBox, transparent background. Exact mapping:
 *   - static induction ellipse cx=340 cy=340 rx=230 ry=134, stroke 10
 *   - halo ellipse (same geometry) stroke 22, opacity 0 -> .3 -> 0 @ 32/44/64%
 *   - THREE right-bulging arcs with the reference's verbatim path coords.
 *     They are FIXED at the centre — CSS only animates `transform: scale()` and
 *     `opacity`, with `transform-box: fill-box; transform-origin: center`.
 *     So each arc scales .7 -> 1 -> 1.25 about its OWN centre, staggered by 6%
 *     (w1 31/40/55, w2 37/46/61, w3 43/52/67). NO horizontal wave movement.
 *   - card group is what slides: translateX(76 -> 0 -> 76) @ 0-10/32-60/86-100%
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
            // static induction ellipse: <ellipse cx=340 cy=340 rx=230 ry=134 stroke=10>
            drawOval(
                topLeft = Offset(110f, 206f), size = Size(460f, 268f),
                color = ink, style = Stroke(width = 10f),
            )

            // halo ellipse: same geometry, stroke 22, opacity 0 -> .3 -> 0
            val haloA = haloAlpha(progress)
            if (haloA > 0f) {
                drawOval(
                    topLeft = Offset(110f, 206f), size = Size(460f, 268f),
                    color = ink.copy(alpha = haloA), style = Stroke(width = 22f),
                )
            }

            // three waves: FIXED position. Only scale about own centre + staggered alpha.
            drawWave(WAVE_1, waveAlpha(progress, 0), waveScale(progress, 0), ink)
            drawWave(WAVE_2, waveAlpha(progress, 1), waveScale(progress, 1), ink)
            drawWave(WAVE_3, waveAlpha(progress, 2), waveScale(progress, 2), ink)

            // card group slides in from the right: translateX(76 -> 0 -> 76)
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

/** CSS @keyframes w1/w2/w3: opacity 0 at hold, 1 at peak, 0 at fade. */
private fun waveAlpha(p: Float, index: Int): Float {
    val hold = .31f + index * .06f
    val peak = .40f + index * .06f
    val fade = .55f + index * .06f
    return when {
        p <= hold || p >= fade -> 0f
        p < peak -> ease((p - hold) / (peak - hold))
        else -> 1f - ease((p - peak) / (fade - peak))
    }
}

/** CSS @keyframes w1/w2/w3: scale .7 -> 1 -> 1.25 (NOT 2x, and no translation). */
private fun waveScale(p: Float, index: Int): Float {
    val hold = .31f + index * .06f
    val peak = .40f + index * .06f
    val fade = .55f + index * .06f
    return when {
        p <= hold -> .7f
        p < peak -> .7f + .3f * ease((p - hold) / (peak - hold))
        p < fade -> 1f + .25f * ease((p - peak) / (fade - peak))
        else -> 1.25f
    }
}

/** CSS @keyframes halo: 0 -> .3 -> 0 at 32/44/64%. */
private fun haloAlpha(p: Float): Float = when {
    p <= .32f || p >= .64f -> 0f
    p < .44f -> .3f * ease((p - .32f) / .12f)
    else -> .3f * (1f - ease((p - .44f) / .20f))
}

/** CSS @keyframes tap: 76px parked -> 0 tapped -> 76px away. */
private fun cardOffset(p: Float): Float = when {
    p <= .10f -> 76f
    p < .32f -> 76f * (1f - ease((p - .10f) / .22f))
    p <= .60f -> 0f
    p < .86f -> 76f * ease((p - .60f) / .26f)
    else -> 76f
}

/** smoothstep — close approximation of CSS's default `ease` timing. */
private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** Draw one arc scaled about its OWN bounds centre (transform-box: fill-box). */
private fun DrawScope.drawWave(
    path: Path,
    alpha: Float,
    scaleFactor: Float,
    ink: Color,
) {
    if (alpha <= 0.001f) return
    val centre = path.getBounds().center
    scale(scaleFactor, pivot = centre) {
        drawPath(path, ink.copy(alpha = alpha), style = Stroke(10f, cap = StrokeCap.Round))
    }
}

/* Verbatim arc paths from the reference SVG (right-bulging ")"), symmetric about
   y=340. w3 is the largest/leftmost, w1 the smallest/rightmost — matching the
   screenshot where arcs read ")))". Fixed in place, never translated. */
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
