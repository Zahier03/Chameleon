package com.sotech.chameleon

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.sotech.chameleon.data.ModelRepository
import com.sotech.chameleon.data.ThemeMode
import com.sotech.chameleon.data.ThemeSettings
import com.sotech.chameleon.data.AppColorScheme
import com.sotech.chameleon.ui.theme.ChameleonTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*
import kotlin.random.Random

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    @Inject
    lateinit var repository: ModelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        var isLoadingComplete = false
        var themeSettings = ThemeSettings()

        lifecycleScope.launch {
            themeSettings = repository.themeSettings.first()
        }

        setContent {
            ChameleonTheme(themeSettings = themeSettings) {
                SplashScreen(
                    themeSettings = themeSettings,
                    onLoadingComplete = {
                        isLoadingComplete = true
                    }
                )
            }
        }

        lifecycleScope.launch {
            themeSettings = repository.themeSettings.first()

            while (!isLoadingComplete) {
                delay(100)
            }

            delay(200)

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

@Composable
fun SplashScreen(
    themeSettings: ThemeSettings,
    onLoadingComplete: () -> Unit
) {
    var animationProgress by remember { mutableStateOf(0f) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var showLoading by remember { mutableStateOf(false) }

    val systemDarkTheme = isSystemInDarkTheme()

    val isDarkMode = when (themeSettings.isDarkMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDarkTheme
    }

    val targetColor = remember(isDarkMode, themeSettings.colorScheme) {
        when {
            isDarkMode -> {
                when (themeSettings.colorScheme) {
                    AppColorScheme.DEFAULT -> Color(0xFF131314)
                    AppColorScheme.OCEAN -> Color(0xFF0F1418)
                    AppColorScheme.FOREST -> Color(0xFF0E150E)
                    AppColorScheme.SUNSET -> Color(0xFF1A0F0A)
                    AppColorScheme.MONOCHROME -> Color(0xFF121212)
                }
            }
            else -> {
                when (themeSettings.colorScheme) {
                    AppColorScheme.DEFAULT -> Color(0xFFFFFBFE)
                    AppColorScheme.OCEAN -> Color(0xFFF8FAFD)
                    AppColorScheme.FOREST -> Color(0xFFF5FBF5)
                    AppColorScheme.SUNSET -> Color(0xFFFFF8F5)
                    AppColorScheme.MONOCHROME -> Color(0xFFFAFAFA)
                }
            }
        }
    }

    val barberChopFont = remember {
        try {
            FontFamily(Font(R.font.barberchop, FontWeight.Bold))
        } catch (e: Exception) {
            FontFamily.Default
        }
    }

    val randomCorner = remember {
        Random.nextInt(4)
    }

    val randomColorScheme = remember {
        Random.nextInt(5)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shineProgress by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shine_progress"
    )

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            )
        ) { value, _ ->
            animationProgress = value
        }

        delay(500)
        showLoading = true
    }

    LaunchedEffect(showLoading) {
        if (showLoading) {
            val steps = listOf(
                0.1f to 200L,
                0.25f to 300L,
                0.4f to 400L,
                0.6f to 500L,
                0.8f to 400L,
                0.95f to 300L,
                1.0f to 200L
            )

            for ((progress, stepDelay) in steps) {
                animate(
                    initialValue = loadingProgress,
                    targetValue = progress,
                    animationSpec = tween(
                        durationMillis = stepDelay.toInt(),
                        easing = FastOutSlowInEasing
                    )
                ) { value, _ ->
                    loadingProgress = value
                }
                delay(stepDelay)
            }

            loadingProgress = 1.0f
            delay(300)
            onLoadingComplete()
        }
    }

    val emoticons = listOf(
        "(˶˃⤙˂˶)",
        "(´｡• ᵕ •｡`)",
        "(˶ᵔ ᵕ ᵔ˶)",
        "(≽^•⩊•^≼)",
        "(˶ˆᗜˆ˵)",
        "(´｡• ω •｡`)"
    )

    var currentEmoticonIndex by remember { mutableStateOf(0) }

    LaunchedEffect(showLoading) {
        if (showLoading) {
            while (true) {
                delay(800)
                currentEmoticonIndex = (currentEmoticonIndex + 1) % emoticons.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawExpandingCircle(
                progress = animationProgress,
                targetColor = targetColor
            )
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawShinyGradientShadow(
                progress = shineProgress,
                corner = randomCorner,
                colorScheme = randomColorScheme,
                isDarkMode = isDarkMode
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            val textAlpha by animateFloatAsState(
                targetValue = if (animationProgress > 0.3f) 1f else 0f,
                animationSpec = tween(600),
                label = "text_alpha"
            )

            Box(
                modifier = Modifier.graphicsLayer(alpha = textAlpha)
            ) {
                Text(
                    text = "MAPI",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontSize = 42.sp,
                        fontFamily = barberChopFont
                    ),
                    color = if (isDarkMode) Color.White else Color(0xFF1C1B1F)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI Chat • Offline • Private",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = barberChopFont
                ),
                color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F),
                modifier = Modifier.graphicsLayer(alpha = textAlpha)
            )

            Spacer(modifier = Modifier.height(80.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showLoading,
                    enter = fadeIn(animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentEmoticonIndex,
                            transitionSpec = {
                                (slideInVertically(
                                    initialOffsetY = { -it },
                                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                                ) + fadeIn(
                                    animationSpec = tween(300)
                                )) togetherWith (slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                                ) + fadeOut(
                                    animationSpec = tween(300)
                                ))
                            },
                            label = "emoticon_animation"
                        ) { index ->
                            Text(
                                text = emoticons[index],
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = when {
                                loadingProgress < 0.2f -> "Initializing..."
                                loadingProgress < 0.4f -> "Loading models..."
                                loadingProgress < 0.6f -> "Setting up AI engine..."
                                loadingProgress < 0.8f -> "Preparing chat interface..."
                                loadingProgress < 0.95f -> "Almost ready..."
                                else -> "Welcome!"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontFamily = barberChopFont
                            ),
                            color = if (isDarkMode)
                                Color.White.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .padding(top = 8.dp)
                        ) {
                            FixedAsciiProgressBar(
                                progress = loadingProgress,
                                modifier = Modifier.fillMaxWidth(),
                                isDarkMode = isDarkMode
                            )
                        }

                        Text(
                            text = "${(loadingProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = barberChopFont,
                                fontSize = 14.sp
                            ),
                            color = if (isDarkMode)
                                Color.White.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FixedAsciiProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val totalBlocks = 15
    val filledBlocks = (progress * totalBlocks).toInt()
    val progressBar = buildString {
        append("[")
        repeat(filledBlocks) { append("█") }
        repeat(totalBlocks - filledBlocks) { append("░") }
        append("]")
    }

    Text(
        text = progressBar,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = if (isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        letterSpacing = 1.sp
    )
}

fun DrawScope.drawExpandingCircle(
    progress: Float,
    targetColor: Color
) {
    val maxRadius = sqrt(size.width * size.width + size.height * size.height)
    val currentRadius = maxRadius * progress

    val corners = listOf(
        Offset(0f, 0f),
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height)
    )

    corners.forEach { corner ->
        drawCircle(
            color = targetColor,
            radius = currentRadius,
            center = corner,
            alpha = progress
        )
    }

    if (progress > 0.5f) {
        drawCircle(
            color = targetColor,
            radius = currentRadius * 0.8f,
            center = size.center,
            alpha = (progress - 0.5f) * 2f
        )
    }
}

fun DrawScope.drawShinyGradientShadow(
    progress: Float,
    corner: Int,
    colorScheme: Int,
    isDarkMode: Boolean
) {
    val maxRadius = sqrt(size.width * size.width + size.height * size.height)
    val currentRadius = (maxRadius * progress).coerceAtLeast(1f)

    val gradientColors = when (colorScheme) {
        0 -> {
            if (isDarkMode) {
                listOf(
                    Color(0xFF6366F1).copy(alpha = 0.4f),
                    Color(0xFF7C7EF3).copy(alpha = 0.3f),
                    Color(0xFF9CA3F5).copy(alpha = 0.2f),
                    Color(0xFFBCC4F8).copy(alpha = 0.1f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFF4F46E5).copy(alpha = 0.3f),
                    Color(0xFF6366F1).copy(alpha = 0.2f),
                    Color(0xFF818CF8).copy(alpha = 0.15f),
                    Color(0xFFA5B4FC).copy(alpha = 0.08f),
                    Color.Transparent
                )
            }
        }
        1 -> {
            if (isDarkMode) {
                listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.4f),
                    Color(0xFF9D73F7).copy(alpha = 0.3f),
                    Color(0xFFB18BF9).copy(alpha = 0.2f),
                    Color(0xFFC5A3FB).copy(alpha = 0.1f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFF7C3AED).copy(alpha = 0.3f),
                    Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    Color(0xFFA78BFA).copy(alpha = 0.15f),
                    Color(0xFFC4B5FD).copy(alpha = 0.08f),
                    Color.Transparent
                )
            }
        }
        2 -> {
            if (isDarkMode) {
                listOf(
                    Color(0xFFEC4899).copy(alpha = 0.4f),
                    Color(0xFFEE5FA5).copy(alpha = 0.3f),
                    Color(0xFFF077B1).copy(alpha = 0.2f),
                    Color(0xFFF28EBD).copy(alpha = 0.1f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFFDB2777).copy(alpha = 0.3f),
                    Color(0xFFEC4899).copy(alpha = 0.2f),
                    Color(0xFFF472B6).copy(alpha = 0.15f),
                    Color(0xFFF9A8D4).copy(alpha = 0.08f),
                    Color.Transparent
                )
            }
        }
        3 -> {
            if (isDarkMode) {
                listOf(
                    Color(0xFF10B981).copy(alpha = 0.4f),
                    Color(0xFF20C28F).copy(alpha = 0.3f),
                    Color(0xFF30CB9D).copy(alpha = 0.2f),
                    Color(0xFF40D4AB).copy(alpha = 0.1f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFF059669).copy(alpha = 0.3f),
                    Color(0xFF10B981).copy(alpha = 0.2f),
                    Color(0xFF34D399).copy(alpha = 0.15f),
                    Color(0xFF6EE7B7).copy(alpha = 0.08f),
                    Color.Transparent
                )
            }
        }
        else -> {
            if (isDarkMode) {
                listOf(
                    Color(0xFF06B6D4).copy(alpha = 0.4f),
                    Color(0xFF16BFDB).copy(alpha = 0.3f),
                    Color(0xFF26C8E2).copy(alpha = 0.2f),
                    Color(0xFF36D1E9).copy(alpha = 0.1f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFF0891B2).copy(alpha = 0.3f),
                    Color(0xFF06B6D4).copy(alpha = 0.2f),
                    Color(0xFF22D3EE).copy(alpha = 0.15f),
                    Color(0xFF67E8F9).copy(alpha = 0.08f),
                    Color.Transparent
                )
            }
        }
    }

    val cornerOffset = when (corner) {
        0 -> Offset(0f, 0f)
        1 -> Offset(size.width, 0f)
        2 -> Offset(0f, size.height)
        else -> Offset(size.width, size.height)
    }

    if (currentRadius > 1f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = gradientColors,
                center = cornerOffset,
                radius = currentRadius
            ),
            radius = currentRadius,
            center = cornerOffset
        )
    }
}