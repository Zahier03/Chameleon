package com.sotech.chameleon.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val calendarEvents by viewModel.calendarEvents.collectAsState(initial = emptyList())
    val timetableEntries by viewModel.timetableEntries.collectAsState(initial = emptyList())

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

    val upcomingEvents by remember {
        derivedStateOf {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

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
                .map { (event, eventTime) ->
                    EventDisplay(
                        event.title,
                        event.description,
                        formatDateTime(event.dateTime, event.hour, event.minute),
                        Color(event.color),
                        true,
                        eventTime
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
                .map { (entry, eventTime) ->
                    EventDisplay(
                        entry.title,
                        entry.description,
                        "${entry.startTime} - ${entry.endTime}",
                        Color(entry.color),
                        false,
                        eventTime
                    )
                }

            (futureCalendarEvents + todayTimetable)
                .sortedBy { it.eventTimeMillis }
        }
    }

    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    val pagerState = rememberPagerState(pageCount = { upcomingEvents.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = Calendar.getInstance()
        }
    }

    LaunchedEffect(upcomingEvents.size) {
        if (upcomingEvents.isNotEmpty()) {
            while (true) {
                delay(5000)
                val nextPage = (pagerState.currentPage + 1) % upcomingEvents.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLandscape) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = String.format("%02d", currentTime.get(Calendar.HOUR_OF_DAY)),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (80 * scaleFactor).sp,
                                    lineHeight = (90 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (80 * scaleFactor).sp,
                                    lineHeight = (90 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format("%02d", currentTime.get(Calendar.MINUTE)),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (80 * scaleFactor).sp,
                                    lineHeight = (90 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.padding(bottom = (6 * scaleFactor).dp)
                            ) {
                                Text(
                                    text = String.format("%02d", currentTime.get(Calendar.SECOND)),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (32 * scaleFactor).sp,
                                        lineHeight = (40 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(currentTime.time),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = barberChopFont,
                                fontSize = (16 * scaleFactor).sp,
                                lineHeight = (20 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                        .padding((16 * scaleFactor).dp)
                ) {
                    if (upcomingEvents.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape((24 * scaleFactor).dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((24 * scaleFactor).dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.EventNote,
                                    contentDescription = null,
                                    modifier = Modifier.size((48 * scaleFactor).dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
                                Text(
                                    text = "NO UPCOMING EVENTS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (14 * scaleFactor).sp,
                                        lineHeight = (18 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) { page ->
                                EventCardLandscape(
                                    event = upcomingEvents[page],
                                    currentTime = currentTime,
                                    scaleFactor = scaleFactor,
                                    barberChopFont = barberChopFont
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = (12 * scaleFactor).dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(upcomingEvents.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (pagerState.currentPage == index) (10 * scaleFactor).dp else (6 * scaleFactor).dp)
                                            .background(
                                                color = if (pagerState.currentPage == index)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                    )
                                    if (index < upcomingEvents.size - 1) {
                                        Spacer(modifier = Modifier.width((6 * scaleFactor).dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = String.format("%02d", currentTime.get(Calendar.HOUR_OF_DAY)),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (140 * scaleFactor).sp,
                                    lineHeight = (150 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (140 * scaleFactor).sp,
                                    lineHeight = (150 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format("%02d", currentTime.get(Calendar.MINUTE)),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (140 * scaleFactor).sp,
                                    lineHeight = (150 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.padding(bottom = (8 * scaleFactor).dp)
                            ) {
                                Text(
                                    text = String.format("%02d", currentTime.get(Calendar.SECOND)),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (48 * scaleFactor).sp,
                                        lineHeight = (56 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(currentTime.time),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = barberChopFont,
                                fontSize = (22 * scaleFactor).sp,
                                lineHeight = (28 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f)
                        .background(MaterialTheme.colorScheme.background)
                        .padding((20 * scaleFactor).dp)
                ) {
                    if (upcomingEvents.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape((24 * scaleFactor).dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((32 * scaleFactor).dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.EventNote,
                                    contentDescription = null,
                                    modifier = Modifier.size((64 * scaleFactor).dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
                                Text(
                                    text = "NO UPCOMING EVENTS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (16 * scaleFactor).sp,
                                        lineHeight = (24 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) { page ->
                                EventCardPortrait(
                                    event = upcomingEvents[page],
                                    currentTime = currentTime,
                                    scaleFactor = scaleFactor,
                                    barberChopFont = barberChopFont
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = (16 * scaleFactor).dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(upcomingEvents.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (pagerState.currentPage == index) (12 * scaleFactor).dp else (8 * scaleFactor).dp)
                                            .background(
                                                color = if (pagerState.currentPage == index)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                    )
                                    if (index < upcomingEvents.size - 1) {
                                        Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding((16 * scaleFactor).dp)
                .size((48 * scaleFactor).dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size((24 * scaleFactor).dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EventCardLandscape(
    event: EventDisplay,
    currentTime: Calendar,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    val timeUntilEvent = event.eventTimeMillis - currentTime.timeInMillis
    val countdown = remember(timeUntilEvent) {
        formatCountdown(timeUntilEvent)
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = event.color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape((24 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            event.color.copy(alpha = 0.2f),
                            event.color.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding((20 * scaleFactor).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size((48 * scaleFactor).dp)
                            .background(
                                color = event.color.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (event.isCalendar) Icons.Default.Event else Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size((28 * scaleFactor).dp),
                            tint = event.color
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                    ) {
                        Text(
                            text = if (event.isCalendar) "UPCOMING EVENT" else "TODAY'S SCHEDULE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = barberChopFont,
                                letterSpacing = (1 * scaleFactor).sp,
                                fontSize = (11 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = event.color
                        )
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = barberChopFont,
                                fontSize = (20 * scaleFactor).sp,
                                lineHeight = (26 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (event.description.isNotEmpty()) {
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = (12 * scaleFactor).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                ) {
                    if (timeUntilEvent > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = event.color.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape((12 * scaleFactor).dp)
                        ) {
                            Row(
                                modifier = Modifier.padding((10 * scaleFactor).dp),
                                horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size((16 * scaleFactor).dp),
                                    tint = event.color
                                )
                                Text(
                                    text = countdown,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (14 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text(
                        text = event.timeInfo,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (13 * scaleFactor).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EventCardPortrait(
    event: EventDisplay,
    currentTime: Calendar,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    val timeUntilEvent = event.eventTimeMillis - currentTime.timeInMillis
    val countdown = remember(timeUntilEvent) {
        formatCountdown(timeUntilEvent)
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = event.color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape((24 * scaleFactor).dp)
    ) {
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
                .padding((32 * scaleFactor).dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size((56 * scaleFactor).dp)
                            .background(
                                color = event.color.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (event.isCalendar) Icons.Default.Event else Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size((32 * scaleFactor).dp),
                            tint = event.color
                        )
                    }
                    Column {
                        Text(
                            text = if (event.isCalendar) "UPCOMING EVENT" else "TODAY'S SCHEDULE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = barberChopFont,
                                letterSpacing = (1 * scaleFactor).sp,
                                fontSize = (14 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = event.color
                        )
                        Text(
                            text = event.timeInfo,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = (16 * scaleFactor).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                ) {
                    if (timeUntilEvent > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = event.color.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape((12 * scaleFactor).dp)
                        ) {
                            Row(
                                modifier = Modifier.padding((12 * scaleFactor).dp),
                                horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size((20 * scaleFactor).dp),
                                    tint = event.color
                                )
                                Text(
                                    text = countdown,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (18 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = barberChopFont,
                            fontSize = (48 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (event.description.isNotEmpty()) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = (18 * scaleFactor).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

data class EventDisplay(
    val title: String,
    val description: String,
    val timeInfo: String,
    val color: Color,
    val isCalendar: Boolean,
    val eventTimeMillis: Long
)

fun formatCountdown(millisUntil: Long): String {
    if (millisUntil <= 0) return "Now"

    val days = TimeUnit.MILLISECONDS.toDays(millisUntil)
    val hours = TimeUnit.MILLISECONDS.toHours(millisUntil) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntil) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntil) % 60

    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun formatDateTime(timestamp: Long, hour: Int, minute: Int): String {
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = timestamp;
        set(java.util.Calendar.HOUR_OF_DAY, hour);
        set(java.util.Calendar.MINUTE, minute)
    }
    return java.text.SimpleDateFormat("MMM dd, yyyy - HH:mm", java.util.Locale.getDefault()).format(calendar.time)
}
