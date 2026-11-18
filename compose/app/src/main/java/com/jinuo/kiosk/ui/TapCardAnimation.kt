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
 *   - THREE WAVE ARCS: all three share one strictly-horizontal LEFT translation
 *     (+34 -> -126 in viewBox px); each arc grows from 1x to 2x as it travels
 *     and the three fade in/out in sequence so they never smear into one blob
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

            // three waves: one shared leftward offset keeps the 56px pitch
            // fixed; the per-wave scale grows 1x -> 2x and fades are staggered.
            val dx = waveDx(progress)
            drawWave(WAVE_1, waveAlpha(progress, 0), dx, waveScale(progress, 0), ink)
            drawWave(WAVE_2, waveAlpha(progress, 1), dx, waveScale(progress, 1), ink)
            drawWave(WAVE_3, waveAlpha(progress, 2), dx, waveScale(progress, 2), ink)

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
 * One shared, purely horizontal translation for all three waves. Y is never
 * touched, and a common dx keeps their 56px pitch constant while scaling.
 */
private fun waveDx(p: Float): Float = when {
    p <= .30f -> 34f
    p < .40f -> 34f - 126f * ease((p - .30f) / .10f)
    p < .55f -> -92f - 34f * ease((p - .40f) / .15f)
    else -> -126f
}

/** Staggered fade, matching the reference animation's w1/w2/w3 timing. */
private fun waveAlpha(p: Float, index: Int): Float {
    val hold = .30f + index * .06f
    val peak = .40f + index * .06f
    val fade = .55f + index * .06f
    return when {
        p <= hold || p >= fade -> 0f
        p < peak -> ease((p - hold) / (peak - hold))
        else -> 1f - ease((p - peak) / (fade - peak))
    }
}

/**
 * Each wave grows from 1x at fade-in to exactly 2x at its brightest moment,
 * then holds 2x while fading out, so the final size is clearly visible.
 */
private fun waveScale(p: Float, index: Int): Float {
    val hold = .30f + index * .06f
    val peak = .40f + index * .06f
    if (p <= hold) return 1f
    return 1f + ease((p - hold) / (peak - hold))
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

/** Draw one arc: translate along X only, then scale about the arc's own centre. */
private fun DrawScope.drawWave(
    path: Path,
    alpha: Float,
    dx: Float,
    scaleFactor: Float,
    ink: Color,
) {
    if (alpha <= 0.001f) return
    translate(dx, 0f) {
        val centre = path.getBounds().center
        scale(scaleFactor, pivot = centre) {
            drawPath(path, ink.copy(alpha = alpha), style = Stroke(10f, cap = StrokeCap.Round))
        }
    }
}

/* Three right-bulging arcs (")"), symmetric about y=340, 56px apart. The card
   taps from the right and the waves slide LEFT into the reader while growing
   to 2x. The 56px pitch exceeds their expanded half-widths, so they stay
   legible even at full scale. */
private val WAVE_1 = Path().apply {
    moveTo(382f, 328f); quadraticBezierTo(400f, 340f, 382f, 352f)
}
private val WAVE_2 = Path().apply {
    moveTo(322f, 316f); quadraticBezierTo(348f, 340f, 322f, 364f)
}
private val WAVE_3 = Path().apply {
    moveTo(262f, 304f); quadraticBezierTo(296f, 340f, 262f, 376f)
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
