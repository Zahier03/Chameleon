package com.sotech.chameleon.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sotech.chameleon.R
import com.sotech.chameleon.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DashboardEventDisplay(
    val title: String,
    val description: String,
    val timeInfo: String,
    val color: Color,
    val isCalendar: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit,
    onNavigateToDeck: () -> Unit
) {
    val calendarEvents by viewModel.calendarEvents.collectAsState(initial = emptyList())
    val timetableEntries by viewModel.timetableEntries.collectAsState(initial = emptyList())
    var animationStarted by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val referenceScreenWidth = 411.dp

    val textScaleFactor = remember(screenWidthDp, screenHeightDp, isLandscape) {
        if (isLandscape) {
            (screenHeightDp.value / 411f).coerceIn(0.5f, 1f)
        } else {
            if (screenWidthDp < referenceScreenWidth) screenWidthDp / referenceScreenWidth else 1f
        }
    }

    val barberChopFont = remember {
        try {
            FontFamily(Font(R.font.barberchop, FontWeight.Bold))
        } catch (e: Exception) {
            FontFamily.Default
        }
    }

    val upcomingEvents by remember {
        derivedStateOf {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

            val futureCalendarEvents = calendarEvents
                .map { event ->
                    val eventCalendar = Calendar.getInstance().apply {
                        timeInMillis = event.dateTime
                        set(Calendar.HOUR_OF_DAY, event.hour)
                        set(Calendar.MINUTE, event.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    event to eventCalendar.timeInMillis
                }
                .filter { (_, eventTime) -> eventTime > now }
                .sortedBy { (_, eventTime) -> eventTime }
                .take(5)
                .map { (event, _) ->
                    DashboardEventDisplay(
                        event.title,
                        event.description,
                        formatDateTime(event.dateTime, event.hour, event.minute),
                        Color(event.color),
                        true
                    )
                }

            val todayTimetable = timetableEntries
                .filter { it.dayOfWeek == dayOfWeek }
                .mapNotNull { entry ->
                    try {
                        val timeParts = entry.startTime.split(":")
                        val entryCalendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                            set(Calendar.MINUTE, timeParts[1].toInt())
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (entryCalendar.timeInMillis > now) {
                            entry to entryCalendar.timeInMillis
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { (_, time) -> time }
                .take(3)
                .map { (entry, _) ->
                    DashboardEventDisplay(
                        entry.title,
                        entry.description,
                        "${entry.startTime} - ${entry.endTime}",
                        Color(entry.color),
                        false
                    )
                }

            (futureCalendarEvents + todayTimetable)
                .sortedBy { event ->
                    if (event.isCalendar) 0 else 1
                }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        animationStarted = true
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DASHBOARD",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = barberChopFont,
                            fontSize = (40 * textScaleFactor).sp,
                            lineHeight = (50 * textScaleFactor).sp
                        ),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (3 * textScaleFactor).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(
                        start = (24 * textScaleFactor).dp,
                        end = (24 * textScaleFactor).dp,
                        top = (16 * textScaleFactor).dp,
                        bottom = (80 * textScaleFactor).dp
                    ),
                horizontalArrangement = Arrangement.spacedBy((24 * textScaleFactor).dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy((12 * textScaleFactor).dp)
                ) {
                    Card(
                        onClick = onNavigateToChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape((16 * textScaleFactor).dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (60 * textScaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF66BB6A).copy(alpha = 0.3f),
                                modifier = Modifier.align(Alignment.Center)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((16 * textScaleFactor).dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size((32 * textScaleFactor).dp),
                                    tint = Color.White
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy((8 * textScaleFactor).dp)
                                ) {
                                    Text(
                                        text = "AI CHAT",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = barberChopFont,
                                            fontSize = (20 * textScaleFactor).sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Start conversation",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = (12 * textScaleFactor).sp
                                        ),
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        onClick = onNavigateToDeck,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape((16 * textScaleFactor).dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "TIME",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (50 * textScaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF64B5F6).copy(alpha = 0.3f),
                                modifier = Modifier.align(Alignment.Center)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((16 * textScaleFactor).dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size((32 * textScaleFactor).dp),
                                    tint = Color.White
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy((8 * textScaleFactor).dp)
                                ) {
                                    Text(
                                        text = "DECK",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = barberChopFont,
                                            fontSize = (20 * textScaleFactor).sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "View fullscreen",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = (12 * textScaleFactor).sp
                                        ),
                                        color = Color(0xFF90CAF9)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    EventCarousel(
                        events = upcomingEvents,
                        scaleFactor = textScaleFactor,
                        barberChopFont = barberChopFont
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = (24 * textScaleFactor).dp,
                    end = (24 * textScaleFactor).dp,
                    top = (16 * textScaleFactor).dp,
                    bottom = (80 * textScaleFactor).dp
                ),
                verticalArrangement = Arrangement.spacedBy((24 * textScaleFactor).dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((12 * textScaleFactor).dp)
                    ) {
                        Card(
                            onClick = onNavigateToChat,
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape((16 * textScaleFactor).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((200 * textScaleFactor).dp)
                            ) {
                                Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (60 * textScaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF66BB6A).copy(alpha = 0.3f),
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding((16 * textScaleFactor).dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size((32 * textScaleFactor).dp),
                                        tint = Color.White
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy((8 * textScaleFactor).dp)
                                    ) {
                                        Text(
                                            text = "AI CHAT",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = barberChopFont,
                                                fontSize = (20 * textScaleFactor).sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Start conversation",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = (12 * textScaleFactor).sp
                                            ),
                                            color = Color(0xFFA5D6A7)
                                        )
                                    }
                                }
                            }
                        }

                        Card(
                            onClick = onNavigateToDeck,
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1976D2)
                            ),
                            shape = RoundedCornerShape((16 * textScaleFactor).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((200 * textScaleFactor).dp)
                            ) {
                                Text(
                                    text = "TIME",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (50 * textScaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF64B5F6).copy(alpha = 0.3f),
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding((16 * textScaleFactor).dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size((32 * textScaleFactor).dp),
                                        tint = Color.White
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy((8 * textScaleFactor).dp)
                                    ) {
                                        Text(
                                            text = "DECK",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = barberChopFont,
                                                fontSize = (20 * textScaleFactor).sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "View fullscreen",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = (12 * textScaleFactor).sp
                                            ),
                                            color = Color(0xFF90CAF9)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    EventCarousel(
                        events = upcomingEvents,
                        scaleFactor = textScaleFactor,
                        barberChopFont = barberChopFont
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCarousel(
    events: List<DashboardEventDisplay>,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    val pagerState = rememberPagerState(pageCount = { events.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            while (true) {
                delay(5000)
                val nextPage = (pagerState.currentPage + 1) % events.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height((240 * scaleFactor).dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (4 * scaleFactor).dp)
    ) {
        if (events.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((20 * scaleFactor).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.EventNote,
                    contentDescription = null,
                    modifier = Modifier.size((48 * scaleFactor).dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
                Text(
                    text = "NO UPCOMING EVENTS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val event = events[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        event.color.copy(alpha = 0.2f),
                                        event.color.copy(alpha = 0.4f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding((20 * scaleFactor).dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((40 * scaleFactor).dp)
                                        .background(
                                            color = event.color.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (event.isCalendar) Icons.Default.Event else Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size((24 * scaleFactor).dp),
                                        tint = event.color
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (event.isCalendar) "UPCOMING EVENT" else "TODAY'S SCHEDULE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = (11 * scaleFactor).sp,
                                            fontFamily = barberChopFont
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = event.color
                                    )
                                    Text(
                                        text = event.timeInfo,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = (12 * scaleFactor).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                            ) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (24 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (event.description.isNotEmpty()) {
                                    Text(
                                        text = event.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = (14 * scaleFactor).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding((16 * scaleFactor).dp),
                    horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                ) {
                    repeat(events.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (pagerState.currentPage == index) (24 * scaleFactor).dp else (8 * scaleFactor).dp,
                                    height = (8 * scaleFactor).dp
                                )
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = (8 * scaleFactor).dp)
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val prevPage = if (pagerState.currentPage == 0) events.size - 1 else pagerState.currentPage - 1
                                pagerState.animateScrollToPage(prevPage)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            modifier = Modifier.size((32 * scaleFactor).dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = (8 * scaleFactor).dp)
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val nextPage = (pagerState.currentPage + 1) % events.size
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            modifier = Modifier.size((32 * scaleFactor).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSection(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis.toLong())
            isVisible = true
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut()
    ) {
        content()
    }
}

fun formatDateTime(timestamp: Long, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    return sdf.format(calendar.time)
}