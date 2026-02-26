package com.sotech.chameleon.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sotech.chameleon.R
import com.sotech.chameleon.data.ThemeSettings
import com.sotech.chameleon.ui.theme.customColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeSettings: ThemeSettings,
    onThemeSettingsClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationStarted by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val scaleFactor = remember(screenWidthDp, screenHeightDp, isLandscape) {
        if (isLandscape) {
            (screenHeightDp.value / 411f).coerceIn(0.5f, 1f)
        } else {
            (screenWidthDp.value / 411f).coerceIn(0.7f, 1.2f)
        }
    }

    val barberChopFont = remember {
        try {
            FontFamily(Font(R.font.barberchop, FontWeight.Bold))
        } catch (e: Exception) {
            FontFamily.Default
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        animationStarted = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = barberChopFont,
                            fontSize = (40 * scaleFactor).sp,
                            lineHeight = (50 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (3 * scaleFactor).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = (16 * scaleFactor).dp,
                end = (16 * scaleFactor).dp,
                top = (16 * scaleFactor).dp,
                bottom = if (isLandscape) (60 * scaleFactor).dp else (80 * scaleFactor).dp
            ),
            verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
        ) {
            item {
                AnimatedSection(
                    visible = animationStarted,
                    delayMillis = 0
                ) {
                    HeroCard(
                        scaleFactor = scaleFactor,
                        barberChopFont = barberChopFont,
                        isLandscape = isLandscape
                    )
                }
            }

            item {
                AnimatedSection(
                    visible = animationStarted,
                    delayMillis = 100
                ) {
                    AppearanceCard(
                        themeSettings = themeSettings,
                        onClick = onThemeSettingsClick,
                        scaleFactor = scaleFactor,
                        barberChopFont = barberChopFont,
                        isLandscape = isLandscape
                    )
                }
            }

            item {
                AnimatedSection(
                    visible = animationStarted,
                    delayMillis = 200
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                    ) {
                        InfoCard(
                            modifier = Modifier.weight(1f),
                            scaleFactor = scaleFactor,
                            barberChopFont = barberChopFont,
                            isLandscape = isLandscape
                        )
                        SystemCard(
                            modifier = Modifier.weight(1f),
                            scaleFactor = scaleFactor,
                            barberChopFont = barberChopFont,
                            isLandscape = isLandscape
                        )
                    }
                }
            }

            item {
                AnimatedSection(
                    visible = animationStarted,
                    delayMillis = 300
                ) {
                    PrivacyCard(
                        scaleFactor = scaleFactor,
                        barberChopFont = barberChopFont,
                        isLandscape = isLandscape
                    )
                }
            }

            item {
                AnimatedSection(
                    visible = animationStarted,
                    delayMillis = 400
                ) {
                    OpenSourceCard(
                        scaleFactor = scaleFactor,
                        barberChopFont = barberChopFont,
                        isLandscape = isLandscape
                    )
                }
            }
        }
    }
}

@Composable
fun HeroCard(
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) (120 * scaleFactor).dp else (180 * scaleFactor).dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (4 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.chameleon),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.3f }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((16 * scaleFactor).dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size((32 * scaleFactor).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size((20 * scaleFactor).dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "CHAMELEON AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = barberChopFont,
                                fontSize = (16 * scaleFactor).sp,
                                lineHeight = (20 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Offline • Private • Powerful",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = (10 * scaleFactor).sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Text(
                    text = "Your personal AI assistant that runs completely on your device",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp,
                        lineHeight = (16 * scaleFactor).sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AppearanceCard(
    themeSettings: ThemeSettings,
    onClick: () -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF7B1FA2).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (4 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size((70 * scaleFactor).dp)
                    .offset(
                        x = (15 * scaleFactor).dp,
                        y = (-15 * scaleFactor).dp
                    )
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color(0xFF9C27B0),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier
                        .size((28 * scaleFactor).dp)
                        .offset(
                            x = (-8 * scaleFactor).dp,
                            y = (8 * scaleFactor).dp
                        ),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding((16 * scaleFactor).dp),
                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
            ) {
                Text(
                    text = "APPEARANCE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = barberChopFont,
                        fontSize = (20 * scaleFactor).sp,
                        lineHeight = (26 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Customize theme and colors",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp
                    ),
                    color = Color(0xFFE1BEE7)
                )

                Spacer(modifier = Modifier.height((4 * scaleFactor).dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape((8 * scaleFactor).dp)
                    ) {
                        Text(
                            text = "Text: ${String.format("%.1f", themeSettings.textScale)}x",
                            modifier = Modifier.padding(
                                horizontal = (12 * scaleFactor).dp,
                                vertical = (6 * scaleFactor).dp
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (10 * scaleFactor).sp,
                                fontFamily = barberChopFont
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape((8 * scaleFactor).dp)
                    ) {
                        Text(
                            text = if (themeSettings.useDynamicColors) "Dynamic" else themeSettings.colorScheme.name,
                            modifier = Modifier.padding(
                                horizontal = (12 * scaleFactor).dp,
                                vertical = (6 * scaleFactor).dp
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (10 * scaleFactor).sp,
                                fontFamily = barberChopFont
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (4 * scaleFactor).dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    Card(
        modifier = modifier.height(if (isLandscape) (120 * scaleFactor).dp else (160 * scaleFactor).dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00796B).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (4 * scaleFactor).dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding((16 * scaleFactor).dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size((32 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size((20 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
            ) {
                Text(
                    text = "VERSION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = (10 * scaleFactor).sp,
                        letterSpacing = (1 * scaleFactor).sp,
                        fontFamily = barberChopFont
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF80CBC4)
                )
                Text(
                    text = "1.0.0",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = (24 * scaleFactor).sp,
                        lineHeight = (30 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SystemCard(
    modifier: Modifier = Modifier,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    Card(
        modifier = modifier.height(if (isLandscape) (120 * scaleFactor).dp else (160 * scaleFactor).dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF57C00).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (4 * scaleFactor).dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding((16 * scaleFactor).dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size((32 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size((20 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
            ) {
                Text(
                    text = "PERFORMANCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = (10 * scaleFactor).sp,
                        letterSpacing = (1 * scaleFactor).sp,
                        fontFamily = barberChopFont
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC80)
                )
                Text(
                    text = "GPU",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = (24 * scaleFactor).sp,
                        lineHeight = (30 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Accelerated",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (10 * scaleFactor).sp
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PrivacyCard(
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1976D2).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (4 * scaleFactor).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scaleFactor).dp),
            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size((44 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size((24 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
            ) {
                Text(
                    text = "PRIVACY FIRST",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Your AI runs completely offline. No data leaves your device.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp,
                        lineHeight = (16 * scaleFactor).sp
                    ),
                    color = Color(0xFF90CAF9)
                )
            }
        }
    }
}

@Composable
fun OpenSourceCard(
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF388E3C).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (4 * scaleFactor).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scaleFactor).dp),
            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size((44 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size((24 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
            ) {
                Text(
                    text = "OPEN SOURCE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Built with MediaPipe for everyone. Free to use and modify.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp,
                        lineHeight = (16 * scaleFactor).sp
                    ),
                    color = Color(0xFFA5D6A7)
                )
            }
        }
    }
}