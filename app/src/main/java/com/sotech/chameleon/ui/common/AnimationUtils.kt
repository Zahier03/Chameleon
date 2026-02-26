package com.sotech.chameleon.ui.common

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import kotlinx.coroutines.delay

/**
 * Creates a delayed animation progress that starts after an initial delay.
 */
@Composable
fun rememberDelayedAnimationProgress(
    initialDelay: Long,
    animationDurationMs: Int,
    animationLabel: String = "animation"
): Float {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(initialDelay)
        progress = 1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = animationDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = animationLabel
    )

    return animatedProgress
}

/**
 * Creates a shimmer effect modifier for loading states.
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    return this.then(
        Modifier.drawWithContent {
            drawContent()
            val shimmerWidth = size.width * 0.3f
            val shimmerOffset = (size.width + shimmerWidth) * shimmerProgress - shimmerWidth

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0f)
                    ),
                    startX = shimmerOffset,
                    endX = shimmerOffset + shimmerWidth
                ),
                blendMode = BlendMode.SrcOver
            )
        }
    )
}

/**
 * Creates a pulsating scale effect.
 */
@Composable
fun animatePulseScale(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    duration: Int = 1000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = if (enabled) minScale else 1f,
        targetValue = if (enabled) maxScale else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    return scale
}

/**
 * Staggered animation state for list items.
 */
@Composable
fun <T> rememberStaggeredAnimationState(
    items: List<T>,
    delayBetweenItems: Int = 50
): Map<Int, Float> {
    val animationStates = remember(items) {
        mutableStateMapOf<Int, Float>()
    }

    LaunchedEffect(items) {
        items.forEachIndexed { index, _ ->
            delay(index * delayBetweenItems.toLong())
            animationStates[index] = 1f
        }
    }

    return animationStates
}

/**
 * Creates a gradient animation that shifts colors over time.
 */
@Composable
fun animateGradientColors(
    colors: List<Color>,
    duration: Int = 3000
): List<Color> {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_offset"
    )

    return remember(animatedOffset, colors) {
        val shiftedColors = mutableListOf<Color>()
        val offset = (animatedOffset * colors.size).toInt()

        for (i in colors.indices) {
            val index = (i + offset) % colors.size
            shiftedColors.add(colors[index])
        }

        shiftedColors
    }
}

/**
 * Extension function for ContentDrawScope to draw a gradient overlay.
 */
fun ContentDrawScope.drawGradientOverlay(
    colors: List<Color>,
    startY: Float = 0f,
    endY: Float = size.height
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = colors,
            startY = startY,
            endY = endY
        ),
        size = Size(size.width, size.height)
    )
}

/**
 * Fade and slide animation configuration.
 */
data class FadeSlideAnimationConfig(
    val fadeInDuration: Int = 600,
    val fadeOutDuration: Int = 300,
    val slideDistance: Float = 30f,
    val delayMillis: Int = 0
)

/**
 * Creates a combined fade and slide animation state.
 */
@Composable
fun rememberFadeSlideAnimation(
    visible: Boolean,
    config: FadeSlideAnimationConfig = FadeSlideAnimationConfig()
): Pair<Float, Float> {
    var targetAlpha by remember { mutableFloatStateOf(if (visible) 0f else 1f) }
    var targetOffset by remember { mutableFloatStateOf(if (visible) config.slideDistance else 0f) }

    LaunchedEffect(visible) {
        if (config.delayMillis > 0) {
            delay(config.delayMillis.toLong())
        }
        targetAlpha = if (visible) 1f else 0f
        targetOffset = if (visible) 0f else config.slideDistance
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(
            durationMillis = if (visible) config.fadeInDuration else config.fadeOutDuration,
            easing = FastOutSlowInEasing
        ),
        label = "fade_alpha"
    )

    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = tween(
            durationMillis = if (visible) config.fadeInDuration else config.fadeOutDuration,
            easing = FastOutSlowInEasing
        ),
        label = "slide_offset"
    )

    return animatedAlpha to animatedOffset
}