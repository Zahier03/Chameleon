package com.sotech.chameleon.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sotech.chameleon.R
import com.sotech.chameleon.data.ImportedModel
import com.sotech.chameleon.data.MessageStatsSummary
import com.sotech.chameleon.data.StatsSettings
import com.sotech.chameleon.utils.formatNumber
import com.sotech.chameleon.utils.formatSpeed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    models: List<ImportedModel>,
    currentModel: ImportedModel?,
    isImporting: Boolean,
    statsSettings: StatsSettings,
    statsSummary: MessageStatsSummary,
    onModelSelect: (ImportedModel) -> Unit,
    onModelDelete: (ImportedModel) -> Unit,
    onModelImport: (uri: Uri, displayName: String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToMindMap: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToCode: () -> Unit,
    onNavigateToModelManager: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStatsSettingsUpdate: (StatsSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var animationStarted by remember { mutableStateOf(false) }
    var showStatsSettings by remember { mutableStateOf(false) }

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var carouselIndex by remember { mutableIntStateOf(0) }
    val uriHandler = LocalUriHandler.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val scaleFactor = remember(screenWidthDp, isLandscape) {
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

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            carouselIndex = (carouselIndex + 1) % 2
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            text = "MA",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = barberChopFont,
                                fontSize = (40 * scaleFactor).sp,
                                lineHeight = (50 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Black,
                            letterSpacing = (3 * scaleFactor).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PI",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = barberChopFont,
                                fontSize = (40 * scaleFactor).sp,
                                lineHeight = (50 * scaleFactor).sp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onSurface,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                            fontWeight = FontWeight.Black,
                            letterSpacing = (3 * scaleFactor).sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = (16 * scaleFactor).dp,
                        end = (16 * scaleFactor).dp,
                        top = (12 * scaleFactor).dp,
                        bottom = (60 * scaleFactor).dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp),
                    verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                ) {
                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 0) {
                            HeroCarousel(carouselIndex, currentTimeMs, scaleFactor, barberChopFont, uriHandler, true)
                        }
                    }

                    if (statsSettings.showStats) {
                        item {
                            AnimatedSection(visible = animationStarted, delayMillis = 100) {
                                StatsCard(models, statsSummary, scaleFactor, barberChopFont, { showStatsSettings = true }, true)
                            }
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 200) {
                            ChatToolCard(models, scaleFactor, barberChopFont, onNavigateToChat, true)
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 300) {
                            MindMapToolCard(models, currentModel, scaleFactor, barberChopFont, onNavigateToMindMap, true)
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 400) {
                            NotesToolCard(scaleFactor, barberChopFont, onNavigateToNotes, true)
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 500) {
                            CodePlaygroundToolCard(scaleFactor, barberChopFont, onNavigateToCode, true)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = (20 * scaleFactor).dp,
                        end = (20 * scaleFactor).dp,
                        top = (12 * scaleFactor).dp,
                        bottom = (60 * scaleFactor).dp
                    ),
                    verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                ) {
                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 0) {
                            HeroCarousel(carouselIndex, currentTimeMs, scaleFactor, barberChopFont, uriHandler, false)
                        }
                    }

                    if (statsSettings.showStats) {
                        item {
                            AnimatedSection(visible = animationStarted, delayMillis = 100) {
                                StatsCard(models, statsSummary, scaleFactor, barberChopFont, { showStatsSettings = true }, false)
                            }
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 200) {
                            ChatToolCard(models, scaleFactor, barberChopFont, onNavigateToChat, false)
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 300) {
                            MindMapToolCard(models, currentModel, scaleFactor, barberChopFont, onNavigateToMindMap, false)
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 400) {
                            NotesToolCard(scaleFactor, barberChopFont, onNavigateToNotes, false)
                        }
                    }

                    item {
                        AnimatedSection(visible = animationStarted, delayMillis = 500) {
                            CodePlaygroundToolCard(scaleFactor, barberChopFont, onNavigateToCode, false)
                        }
                    }
                }
            }
        }
    }

    if (showStatsSettings) {
        com.sotech.chameleon.ui.dialogs.StatsSettingsDialog(
            currentSettings = statsSettings,
            onDismiss = { showStatsSettings = false },
            onSave = { newSettings ->
                onStatsSettingsUpdate(newSettings)
                showStatsSettings = false
            }
        )
    }
}

@Composable
fun HeroCarousel(
    carouselIndex: Int,
    currentTimeMs: Long,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    isLandscape: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) (140 * scaleFactor).dp else (180 * scaleFactor).dp)
            .clip(RoundedCornerShape((12 * scaleFactor).dp))
    ) {
        AnimatedContent(
            targetState = carouselIndex,
            transitionSpec = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ) togetherWith slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            },
            label = "carousel"
        ) { index ->
            when (index) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = R.drawable.robot,
                            contentDescription = "Hero Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp)
                        ) {
                            DigitalClock(
                                timeMs = currentTimeMs,
                                font = barberChopFont,
                                scaleFactor = scaleFactor,
                                isLandscape = isLandscape
                            )
                            DateDisplay(
                                timeMs = currentTimeMs,
                                font = barberChopFont,
                                scaleFactor = scaleFactor,
                                isLandscape = isLandscape
                            )
                        }
                    }
                }
                1 -> {
                    Card(
                        onClick = {
                            uriHandler.openUri("https://huggingface.co/litert-community")
                        },
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape((12 * scaleFactor).dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "MODELS",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                                    lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF42A5F5),
                                modifier = Modifier.align(Alignment.Center)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((16 * scaleFactor).dp)
                            ) {
                                AsyncImage(
                                    model = R.drawable.litert,
                                    contentDescription = "LiteRT Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(if (isLandscape) (60 * scaleFactor).dp else (90 * scaleFactor).dp)
                                        .offset(x = (15 * scaleFactor).dp, y = (-8 * scaleFactor).dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth(0.6f),
                                    verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                ) {
                                    Text(
                                        text = "LITERT",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = barberChopFont,
                                            fontSize = (16 * scaleFactor).sp,
                                            lineHeight = (20 * scaleFactor).sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size((18 * scaleFactor).dp),
                                            tint = Color(0xFF90CAF9)
                                        )
                                        Text(
                                            text = "GET MODELS",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = barberChopFont,
                                                fontSize = (14 * scaleFactor).sp,
                                                lineHeight = (18 * scaleFactor).sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = "Browse & download AI models",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = (12 * scaleFactor).sp
                                        ),
                                        color = Color(0xFFBBDEFB)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    models: List<ImportedModel>,
    statsSummary: MessageStatsSummary,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    onSettingsClick: () -> Unit,
    isLandscape: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((12 * scaleFactor).dp))
            .background(Color(0xFFD84315))
    ) {
        Text(
            text = "STATS",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = barberChopFont,
                fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
            ),
            fontWeight = FontWeight.Black,
            color = Color(0xFFFF6E40),
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scaleFactor).dp),
            verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHAT STATS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size((28 * scaleFactor).dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size((16 * scaleFactor).dp),
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size((16 * scaleFactor).dp),
                        tint = Color(0xFFFFAB91)
                    )
                    Text(
                        text = "MODELS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp,
                            lineHeight = (18 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = models.size.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = (24 * scaleFactor).sp,
                        lineHeight = (30 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6E40)
                )
            }

            HorizontalDivider(
                color = Color(0xFFFF6E40).copy(alpha = 0.3f)
            )

            if (statsSummary.totalMessages > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Speed,
                        value = formatSpeed(statsSummary.avgDecodeSpeed),
                        label = "tok/s",
                        compact = true,
                        font = barberChopFont,
                        iconColor = Color(0xFFFFAB91),
                        textColor = Color.White,
                        labelColor = Color(0xFFFFAB91),
                        scaleFactor = scaleFactor
                    )
                    StatItem(
                        icon = Icons.AutoMirrored.Filled.Message,
                        value = statsSummary.totalMessages.toString(),
                        label = "msgs",
                        compact = true,
                        font = barberChopFont,
                        iconColor = Color(0xFFFFAB91),
                        textColor = Color.White,
                        labelColor = Color(0xFFFFAB91),
                        scaleFactor = scaleFactor
                    )
                    StatItem(
                        icon = Icons.Default.Numbers,
                        value = formatNumber(statsSummary.totalTokens),
                        label = "tokens",
                        compact = true,
                        font = barberChopFont,
                        iconColor = Color(0xFFFFAB91),
                        textColor = Color.White,
                        labelColor = Color(0xFFFFAB91),
                        scaleFactor = scaleFactor
                    )
                }
            } else {
                Text(
                    text = "No chat statistics yet",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (11 * scaleFactor).sp
                    ),
                    color = Color(0xFFFFAB91),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ChatToolCard(
    models: List<ImportedModel>,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    onNavigateToChat: () -> Unit,
    isLandscape: Boolean
) {
    Card(
        onClick = onNavigateToChat,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32)
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "TOOLS",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = barberChopFont,
                    fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                    lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Black,
                color = Color(0xFF66BB6A),
                modifier = Modifier.align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(if (isLandscape) (70 * scaleFactor).dp else (90 * scaleFactor).dp)
                    .offset(
                        x = if (isLandscape) (20 * scaleFactor).dp else (25 * scaleFactor).dp,
                        y = if (isLandscape) (-20 * scaleFactor).dp else (-25 * scaleFactor).dp
                    )
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Chat",
                    modifier = Modifier
                        .size((24 * scaleFactor).dp)
                        .offset(x = (-10 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding((16 * scaleFactor).dp),
                verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
            ) {
                Text(
                    text = "AI CHAT",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height((6 * scaleFactor).dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp),
                        tint = Color(0xFF81C784)
                    )
                    Text(
                        text = "START CHAT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp,
                            lineHeight = (18 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = if (models.isNotEmpty()) {
                        "Begin your AI conversation"
                    } else {
                        "Import a model first"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp
                    ),
                    color = Color(0xFFA5D6A7)
                )
            }
        }
    }
}

@Composable
fun MindMapToolCard(
    models: List<ImportedModel>,
    currentModel: ImportedModel?,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    onNavigateToMindMap: () -> Unit,
    isLandscape: Boolean
) {
    val isApiModel = currentModel?.isApiModel == true

    Card(
        onClick = onNavigateToMindMap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6A1B9A)
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "IDEAS",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = barberChopFont,
                    fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                    lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Black,
                color = Color(0xFFAB47BC),
                modifier = Modifier.align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(if (isLandscape) (70 * scaleFactor).dp else (90 * scaleFactor).dp)
                    .offset(
                        x = if (isLandscape) (20 * scaleFactor).dp else (25 * scaleFactor).dp,
                        y = if (isLandscape) (-20 * scaleFactor).dp else (-25 * scaleFactor).dp
                    )
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color(0xFF8E24AA),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountTree,
                    contentDescription = "Open Mind Map",
                    modifier = Modifier
                        .size((24 * scaleFactor).dp)
                        .offset(x = (-10 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding((16 * scaleFactor).dp),
                verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
            ) {
                Text(
                    text = "MIND MAP",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height((6 * scaleFactor).dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schema,
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp),
                        tint = Color(0xFFCE93D8)
                    )
                    Text(
                        text = "GENERATE MAP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp,
                            lineHeight = (18 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = if (models.isEmpty()) {
                        "Import a model first"
                    } else if (isApiModel) {
                        "Generate structure with PDF/Image/Text"
                    } else {
                        "Generate structure with Image/Text (PDF disabled for local AI)"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp
                    ),
                    color = Color(0xFFE1BEE7)
                )
            }
        }
    }
}

@Composable
fun NotesToolCard(
    scaleFactor: Float,
    barberChopFont: FontFamily,
    onNavigateToNotes: () -> Unit,
    isLandscape: Boolean
) {
    Card(
        onClick = onNavigateToNotes,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE65100)
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "WRITE",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = barberChopFont,
                    fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                    lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF9800),
                modifier = Modifier.align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(if (isLandscape) (70 * scaleFactor).dp else (90 * scaleFactor).dp)
                    .offset(
                        x = if (isLandscape) (20 * scaleFactor).dp else (25 * scaleFactor).dp,
                        y = if (isLandscape) (-20 * scaleFactor).dp else (-25 * scaleFactor).dp
                    )
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color(0xFFF57C00),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = "Open Notes",
                    modifier = Modifier
                        .size((24 * scaleFactor).dp)
                        .offset(x = (-10 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding((16 * scaleFactor).dp),
                verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
            ) {
                Text(
                    text = "NOTES & DOCS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height((6 * scaleFactor).dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Article,
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp),
                        tint = Color(0xFFFFB74D)
                    )
                    Text(
                        text = "OPEN EDITOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp,
                            lineHeight = (18 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Draft markdown notes & ideas",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp
                    ),
                    color = Color(0xFFFFE0B2)
                )
            }
        }
    }
}

@Composable
fun CodePlaygroundToolCard(
    scaleFactor: Float,
    barberChopFont: FontFamily,
    onNavigateToCode: () -> Unit,
    isLandscape: Boolean
) {
    Card(
        onClick = onNavigateToCode,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0277BD)
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "LOGIC",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = barberChopFont,
                    fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                    lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Black,
                color = Color(0xFF039BE5),
                modifier = Modifier.align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(if (isLandscape) (70 * scaleFactor).dp else (90 * scaleFactor).dp)
                    .offset(
                        x = if (isLandscape) (20 * scaleFactor).dp else (25 * scaleFactor).dp,
                        y = if (isLandscape) (-20 * scaleFactor).dp else (-25 * scaleFactor).dp
                    )
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color(0xFF0288D1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = "Open Code Playground",
                    modifier = Modifier
                        .size((24 * scaleFactor).dp)
                        .offset(x = (-10 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding((16 * scaleFactor).dp),
                verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
            ) {
                Text(
                    text = "CODE PLAYGROUND",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height((6 * scaleFactor).dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp),
                        tint = Color(0xFF4FC3F7)
                    )
                    Text(
                        text = "OPEN EDITOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp,
                            lineHeight = (18 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Write & execute Python scripts",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp
                    ),
                    color = Color(0xFFB3E5FC)
                )
            }
        }
    }
}

@Composable
fun DigitalClock(
    timeMs: Long,
    font: FontFamily,
    scaleFactor: Float,
    isLandscape: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = timeFormat.format(timeMs)

    Box {
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = font,
                fontSize = if (isLandscape) (36 * scaleFactor).sp else (54 * scaleFactor).sp,
                lineHeight = if (isLandscape) (42 * scaleFactor).sp else (60 * scaleFactor).sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = MaterialTheme.colorScheme.surface,
                    offset = Offset(0f, 0f),
                    blurRadius = 2f
                )
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.offset(x = (1.5 * scaleFactor).dp, y = (1.5 * scaleFactor).dp)
        )
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = font,
                fontSize = if (isLandscape) (36 * scaleFactor).sp else (54 * scaleFactor).sp,
                lineHeight = if (isLandscape) (42 * scaleFactor).sp else (60 * scaleFactor).sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = MaterialTheme.colorScheme.surface,
                    offset = Offset(0f, 0f),
                    blurRadius = 2f
                )
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.offset(x = (-1.5 * scaleFactor).dp, y = (-1.5 * scaleFactor).dp)
        )
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = font,
                fontSize = if (isLandscape) (36 * scaleFactor).sp else (54 * scaleFactor).sp,
                lineHeight = if (isLandscape) (42 * scaleFactor).sp else (60 * scaleFactor).sp
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DateDisplay(
    timeMs: Long,
    font: FontFamily,
    scaleFactor: Float,
    isLandscape: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val dateString = dateFormat.format(timeMs)
    val dayString = dayFormat.format(timeMs)
    val fullString = "$dateString | $dayString"

    Box {
        Text(
            text = fullString,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = font,
                fontSize = if (isLandscape) (14 * scaleFactor).sp else (18 * scaleFactor).sp,
                lineHeight = if (isLandscape) (18 * scaleFactor).sp else (24 * scaleFactor).sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = MaterialTheme.colorScheme.surface,
                    offset = Offset(0f, 0f),
                    blurRadius = 2f
                )
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.offset(x = (0.8 * scaleFactor).dp, y = (0.8 * scaleFactor).dp)
        )
        Text(
            text = fullString,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = font,
                fontSize = if (isLandscape) (14 * scaleFactor).sp else (18 * scaleFactor).sp,
                lineHeight = if (isLandscape) (18 * scaleFactor).sp else (24 * scaleFactor).sp,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = MaterialTheme.colorScheme.surface,
                    offset = Offset(0f, 0f),
                    blurRadius = 2f
                )
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.offset(x = (-0.8 * scaleFactor).dp, y = (-0.8 * scaleFactor).dp)
        )
        Text(
            text = fullString,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = font,
                fontSize = if (isLandscape) (14 * scaleFactor).sp else (18 * scaleFactor).sp,
                lineHeight = if (isLandscape) (18 * scaleFactor).sp else (24 * scaleFactor).sp
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AnimatedSection(
    visible: Boolean,
    delayMillis: Long,
    content: @Composable () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = delayMillis.toInt(), easing = FastOutSlowInEasing),
        label = "section_alpha"
    )

    val animatedOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(600, delayMillis = delayMillis.toInt(), easing = FastOutSlowInEasing),
        label = "section_offset"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = animatedAlpha
            translationY = animatedOffset
        }
    ) {
        content()
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    compact: Boolean = false,
    font: FontFamily,
    iconColor: Color = Color.White,
    textColor: Color = Color.White,
    labelColor: Color = Color.White,
    scaleFactor: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((3 * scaleFactor).dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((3 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(if (compact) (12 * scaleFactor).dp else (16 * scaleFactor).dp),
                tint = iconColor
            )
            Text(
                text = value,
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = font,
                        fontSize = (12 * scaleFactor).sp,
                        lineHeight = (16 * scaleFactor).sp
                    )
                } else {
                    MaterialTheme.typography.titleSmall.copy(
                        fontFamily = font,
                        fontSize = (14 * scaleFactor).sp,
                        lineHeight = (18 * scaleFactor).sp
                    )
                },
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Text(
            text = label,
            style = if (compact) {
                MaterialTheme.typography.labelSmall.copy(fontSize = (10 * scaleFactor).sp)
            } else {
                MaterialTheme.typography.bodySmall.copy(fontSize = (11 * scaleFactor).sp)
            },
            color = labelColor
        )
    }
}