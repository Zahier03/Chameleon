package com.sotech.chameleon.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.sotech.chameleon.R
import com.sotech.chameleon.data.CalendarEvent
import com.sotech.chameleon.data.TimetableEntry
import com.sotech.chameleon.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ManageCardType {
    CALENDAR,
    TIMETABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val calendarEvents by viewModel.calendarEvents.collectAsState(initial = emptyList())
    val timetableEntries by viewModel.timetableEntries.collectAsState(initial = emptyList())

    var showCardDeck by rememberSaveable { mutableStateOf(true) }
    var selectedCard by remember { mutableStateOf<ManageCardType?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showTimetableDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val referenceScreenWidth = 411.dp

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

    LaunchedEffect(selectedCard) {
        if (selectedCard != null) {
            delay(300)
            showCardDeck = false
            when (selectedCard) {
                ManageCardType.CALENDAR -> showCalendarDialog = true
                ManageCardType.TIMETABLE -> showTimetableDialog = true
                null -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MANAGE",
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        RegularManageCard(
                            type = ManageCardType.CALENDAR,
                            onClick = {
                                selectedCard = ManageCardType.CALENDAR
                            },
                            scaleFactor = scaleFactor,
                            barberChopFont = barberChopFont,
                            isLandscape = true
                        )
                    }

                    item {
                        RegularManageCard(
                            type = ManageCardType.TIMETABLE,
                            onClick = {
                                selectedCard = ManageCardType.TIMETABLE
                            },
                            scaleFactor = scaleFactor,
                            barberChopFont = barberChopFont,
                            isLandscape = true
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = (20 * scaleFactor).dp,
                        end = (20 * scaleFactor).dp,
                        top = (16 * scaleFactor).dp,
                        bottom = (80 * scaleFactor).dp
                    ),
                    verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                ) {
                    item {
                        Text(
                            text = "TOOLS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = barberChopFont,
                                fontSize = (18 * scaleFactor).sp,
                                lineHeight = (24 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = (4 * scaleFactor).dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                    }

                    item {
                        AnimatedVisibility(
                            visible = showCardDeck,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((400 * scaleFactor).dp)
                                    .offset(y = (5 * scaleFactor).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SwipeableCardDeck(
                                    cards = listOf(
                                        ManageCardType.CALENDAR,
                                        ManageCardType.TIMETABLE
                                    ),
                                    onCardSelected = { card ->
                                        selectedCard = card
                                    },
                                    scaleFactor = scaleFactor,
                                    barberChopFont = barberChopFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCalendarDialog) {
        CalendarDialog(
            events = calendarEvents,
            onDismiss = {
                showCalendarDialog = false
                selectedCard = null
                showCardDeck = true
            },
            onAddEvent = { event ->
                viewModel.addCalendarEvent(event)
            },
            onDeleteEvent = { event ->
                viewModel.deleteCalendarEvent(event.id)
            },
            scaleFactor = scaleFactor,
            barberChopFont = barberChopFont
        )
    }

    if (showTimetableDialog) {
        TimetableDialog(
            entries = timetableEntries,
            onDismiss = {
                showTimetableDialog = false
                selectedCard = null
                showCardDeck = true
            },
            onAddEntry = { entry ->
                viewModel.addTimetableEntry(entry)
            },
            onDeleteEntry = { entry ->
                viewModel.deleteTimetableEntry(entry.id)
            },
            scaleFactor = scaleFactor,
            barberChopFont = barberChopFont
        )
    }
}

@Composable
fun SwipeableCardDeck(
    cards: List<ManageCardType>,
    onCardSelected: (ManageCardType) -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    var cardOrder by remember { mutableStateOf(listOf(0, 1)) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isSwiping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        cardOrder.forEachIndexed { index, cardIndex ->
            val cardType = cards[cardIndex % cards.size]
            val isTopCard = index == cardOrder.lastIndex
            val offset = (cardOrder.lastIndex - index).coerceAtLeast(0)

            val scale = 1f - (offset * 0.05f)
            val yOffset = offset * (15 * scaleFactor)
            val alpha = 1f - (offset * 0.2f)

            ManageCard(
                cardType = cardType,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(0.7f)
                    .graphicsLayer {
                        scaleX = if (isTopCard) 1f else scale
                        scaleY = if (isTopCard) 1f else scale
                        translationX = if (isTopCard) offsetX else 0f
                        translationY = if (isTopCard) offsetY else yOffset
                        this.alpha = if (isTopCard) 1f else alpha
                        rotationZ = if (isTopCard) (offsetX / 20f).coerceIn(-15f, 15f) else 0f
                    }
                    .zIndex(index.toFloat())
                    .then(
                        if (isTopCard && !isSwiping) {
                            Modifier.pointerInput(cardOrder) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        if (!isSwiping) {
                                            scope.launch {
                                                animate(
                                                    initialValue = offsetX,
                                                    targetValue = 0f,
                                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                ) { value, _ -> offsetX = value }
                                                animate(
                                                    initialValue = offsetY,
                                                    targetValue = 0f,
                                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                ) { value, _ -> offsetY = value }
                                            }
                                        }
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        scope.launch {
                                            animate(
                                                initialValue = offsetX,
                                                targetValue = 0f,
                                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                                            ) { value, _ -> offsetX = value }
                                            animate(
                                                initialValue = offsetY,
                                                targetValue = 0f,
                                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                                            ) { value, _ -> offsetY = value }
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y

                                    val threshold = 120 * scaleFactor
                                    val distance = kotlin.math.sqrt(offsetX * offsetX + offsetY * offsetY)

                                    if (distance > threshold && !isSwiping) {
                                        isSwiping = true
                                        scope.launch {
                                            val targetX = if (offsetX > 0) 1200f else -1200f
                                            val targetY = offsetY * 2

                                            animate(
                                                initialValue = offsetX,
                                                targetValue = targetX,
                                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                                            ) { value, _ -> offsetX = value }
                                            animate(
                                                initialValue = offsetY,
                                                targetValue = targetY,
                                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                                            ) { value, _ -> offsetY = value }

                                            delay(100)
                                            val swipedCard = cardOrder.last()
                                            cardOrder = listOf(swipedCard) + cardOrder.dropLast(1)
                                            offsetX = 0f
                                            offsetY = 0f
                                            delay(100)
                                            isSwiping = false
                                        }
                                    }
                                }
                            }
                        } else Modifier
                    ),
                onClick = if (isTopCard && !isDragging && !isSwiping) {
                    { onCardSelected(cardType) }
                } else null,
                scaleFactor = scaleFactor,
                barberChopFont = barberChopFont
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCard(
    cardType: ManageCardType,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    val (title, icon, color, description) = when (cardType) {
        ManageCardType.CALENDAR -> Tuple4(
            "CALENDAR",
            Icons.Default.Event,
            Color(0xFF1976D2),
            "Schedule your events"
        )
        ManageCardType.TIMETABLE -> Tuple4(
            "TIMETABLE",
            Icons.Default.Schedule,
            Color(0xFF388E3C),
            "Weekly class schedule"
        )
    }

    Card(
        onClick = onClick ?: {},
        modifier = modifier,
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape((24 * scaleFactor).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = (8 * scaleFactor).dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = barberChopFont,
                    fontSize = (70 * scaleFactor).sp,
                    lineHeight = (90 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((24 * scaleFactor).dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size((64 * scaleFactor).dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size((36 * scaleFactor).dp),
                        tint = Color.White
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (32 * scaleFactor).sp,
                            lineHeight = (40 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (16 * scaleFactor).sp,
                            lineHeight = (24 * scaleFactor).sp
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TAP TO OPEN",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (12 * scaleFactor).sp,
                                letterSpacing = (1 * scaleFactor).sp,
                                fontFamily = barberChopFont
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size((16 * scaleFactor).dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDialog(
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onAddEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape((24 * scaleFactor).dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1976D2))
                            .padding((24 * scaleFactor).dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CALENDAR",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (28 * scaleFactor).sp,
                                        lineHeight = (36 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Manage your events",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = (14 * scaleFactor).sp
                                    ),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size((40 * scaleFactor).dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size((24 * scaleFactor).dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = (24 * scaleFactor).dp,
                                end = (24 * scaleFactor).dp,
                                top = (24 * scaleFactor).dp,
                                bottom = (90 * scaleFactor).dp
                            ),
                            verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                        ) {
                            if (events.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = (40 * scaleFactor).dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.EventNote,
                                            contentDescription = null,
                                            modifier = Modifier.size((64 * scaleFactor).dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
                                        Text(
                                            text = "No events yet",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = (16 * scaleFactor).sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            events.sortedBy {
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = it.dateTime
                                    set(Calendar.HOUR_OF_DAY, it.hour)
                                    set(Calendar.MINUTE, it.minute)
                                }
                                calendar.timeInMillis
                            }.forEach { event ->
                                item(key = event.id) {
                                    CalendarEventItem(
                                        event = event,
                                        onDelete = { onDeleteEvent(event) },
                                        scaleFactor = scaleFactor,
                                        barberChopFont = barberChopFont
                                    )
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding((24 * scaleFactor).dp)
                                .zIndex(10f),
                            containerColor = Color(0xFF1976D2)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Event",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCalendarEventDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { event ->
                onAddEvent(event)
                showAddDialog = false
            },
            scaleFactor = scaleFactor,
            barberChopFont = barberChopFont
        )
    }
}

@Composable
fun CalendarEventItem(
    event: CalendarEvent,
    onDelete: () -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(event.color).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scaleFactor).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size((40 * scaleFactor).dp)
                        .background(
                            color = Color(event.color).copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size((24 * scaleFactor).dp),
                        tint = Color(event.color)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatEventDateTime(event.dateTime, event.hour, event.minute),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (12 * scaleFactor).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (event.description.isNotEmpty()) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = (12 * scaleFactor).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size((40 * scaleFactor).dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size((20 * scaleFactor).dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarEventDialog(
    onDismiss: () -> Unit,
    onAdd: (CalendarEvent) -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "ADD EVENT",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (24 * scaleFactor).sp,
                                    lineHeight = (32 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding((24 * scaleFactor).dp)
                            .zIndex(20f),
                        horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape((12 * scaleFactor).dp)
                        ) {
                            Text("Cancel")
                        }
                        FilledTonalButton(
                            onClick = {
                                if (title.isNotBlank() && datePickerState.selectedDateMillis != null) {
                                    onAdd(
                                        CalendarEvent(
                                            title = title,
                                            description = description,
                                            dateTime = datePickerState.selectedDateMillis!!,
                                            hour = timePickerState.hour,
                                            minute = timePickerState.minute
                                        )
                                    )
                                }
                            },
                            enabled = title.isNotBlank() && datePickerState.selectedDateMillis != null,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1976D2)
                            ),
                            shape = RoundedCornerShape((12 * scaleFactor).dp)
                        ) {
                            Text("Add", color = Color.White)
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues((24 * scaleFactor).dp),
                    verticalArrangement = Arrangement.spacedBy((20 * scaleFactor).dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Event Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                    item {
                        DatePicker(
                            state = datePickerState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding((16 * scaleFactor).dp),
                                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                            ) {
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TimePicker(
                                    state = timePickerState,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableDialog(
    entries: List<TimetableEntry>,
    onDismiss: () -> Unit,
    onAddEntry: (TimetableEntry) -> Unit,
    onDeleteEntry: (TimetableEntry) -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape((24 * scaleFactor).dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF388E3C))
                            .padding((24 * scaleFactor).dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TIMETABLE",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = (28 * scaleFactor).sp,
                                        lineHeight = (36 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Weekly schedule",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = (14 * scaleFactor).sp
                                    ),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size((40 * scaleFactor).dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size((24 * scaleFactor).dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = (24 * scaleFactor).dp,
                                end = (24 * scaleFactor).dp,
                                top = (24 * scaleFactor).dp,
                                bottom = (90 * scaleFactor).dp
                            ),
                            verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                        ) {
                            if (entries.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = (40 * scaleFactor).dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size((64 * scaleFactor).dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
                                        Text(
                                            text = "No schedule entries yet",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = (16 * scaleFactor).sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            daysOfWeek.forEachIndexed { dayIndex, dayName ->
                                val dayEntries =
                                    entries.filter { it.dayOfWeek == dayIndex + 1 }.sortedBy { it.startTime }
                                if (dayEntries.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = dayName.uppercase(),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = barberChopFont,
                                                fontSize = (14 * scaleFactor).sp,
                                                letterSpacing = (1 * scaleFactor).sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = (8 * scaleFactor).dp)
                                        )
                                    }
                                    dayEntries.forEach { entry ->
                                        item(key = entry.id) {
                                            TimetableEntryItem(
                                                entry = entry,
                                                onDelete = { onDeleteEntry(entry) },
                                                scaleFactor = scaleFactor,
                                                barberChopFont = barberChopFont
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding((24 * scaleFactor).dp)
                                .zIndex(10f),
                            containerColor = Color(0xFF388E3C)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Entry",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimetableEntryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { entry ->
                onAddEntry(entry)
                showAddDialog = false
            },
            scaleFactor = scaleFactor,
            barberChopFont = barberChopFont
        )
    }
}

@Composable
fun TimetableEntryItem(
    entry: TimetableEntry,
    onDelete: () -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(entry.color).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scaleFactor).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size((40 * scaleFactor).dp)
                        .background(
                            color = Color(entry.color).copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size((24 * scaleFactor).dp),
                        tint = Color(entry.color)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${entry.startTime} - ${entry.endTime}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (12 * scaleFactor).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (entry.description.isNotEmpty()) {
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = (12 * scaleFactor).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size((40 * scaleFactor).dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size((20 * scaleFactor).dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetableEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (TimetableEntry) -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDay by remember { mutableIntStateOf(1) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var expanded by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "ADD SCHEDULE",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = (24 * scaleFactor).sp,
                                    lineHeight = (32 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding((24 * scaleFactor).dp)
                            .zIndex(20f),
                        horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape((12 * scaleFactor).dp)
                        ) {
                            Text("Cancel")
                        }
                        FilledTonalButton(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onAdd(
                                        TimetableEntry(
                                            title = title,
                                            description = description,
                                            dayOfWeek = selectedDay,
                                            startTime = startTime,
                                            endTime = endTime
                                        )
                                    )
                                }
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF388E3C)
                            ),
                            shape = RoundedCornerShape((12 * scaleFactor).dp)
                        ) {
                            Text("Add", color = Color.White)
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues((24 * scaleFactor).dp),
                    verticalArrangement = Arrangement.spacedBy((20 * scaleFactor).dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )
                    }
                    item {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = daysOfWeek[selectedDay - 1],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Day") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                daysOfWeek.forEachIndexed { index, day ->
                                    DropdownMenuItem(
                                        text = { Text(day) },
                                        onClick = {
                                            selectedDay = index + 1
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

fun formatEventDateTime(timestamp: Long, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    return sdf.format(calendar.time)
}

data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
@Composable
fun RegularManageCard(
    type: ManageCardType,
    onClick: () -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    val (backgroundColor, watermarkText, icon, title, description) = when (type) {
        ManageCardType.CALENDAR -> {
            listOf(
                Color(0xFF1976D2),
                "CALENDAR",
                Icons.Default.Event,
                "CALENDAR",
                "Schedule and manage your events"
            )
        }
        ManageCardType.TIMETABLE -> {
            listOf(
                Color(0xFF388E3C),
                "TIMETABLE",
                Icons.Default.Schedule,
                "TIMETABLE",
                "Organize your weekly schedule"
            )
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) (140 * scaleFactor).dp else (180 * scaleFactor).dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor as Color
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = watermarkText as String,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = barberChopFont,
                    fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                    lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.15f),
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
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon as androidx.compose.ui.graphics.vector.ImageVector,
                    contentDescription = null,
                    modifier = Modifier
                        .size((32 * scaleFactor).dp)
                        .offset(x = (-10 * scaleFactor).dp),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding((16 * scaleFactor).dp),
                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
            ) {
                Text(
                    text = title as String,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = barberChopFont,
                        fontSize = (16 * scaleFactor).sp,
                        lineHeight = (20 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = description as String,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * scaleFactor).sp
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size((18 * scaleFactor).dp),
                        tint = Color.White
                    )
                    Text(
                        text = "OPEN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = (14 * scaleFactor).sp,
                            lineHeight = (18 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}