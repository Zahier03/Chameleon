package com.sotech.chameleon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sotech.chameleon.data.CalendarEvent
import com.sotech.chameleon.data.TimetableEntry
import com.sotech.chameleon.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit,
    onNavigateToMindMap: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToCode: () -> Unit,
    onNavigateToModelManager: () -> Unit,
    onNavigateToDeck: () -> Unit
) {
    val calendarEvents by viewModel.calendarEvents.collectAsState(initial = emptyList())
    val timetableEntries by viewModel.timetableEntries.collectAsState(initial = emptyList())

    var showCalendarDialog by remember { mutableStateOf(false) }
    var showTimetableDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // SECTION: TOOLS
            item {
                Column {
                    SectionHeader(title = "Tools")
                    Spacer(modifier = Modifier.height(8.dp))

                    GithubListGroup {
                        GithubListItem(
                            title = "AI Chat",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            iconBgColor = Color(0xFF2EA043), // GitHub Green
                            onClick = onNavigateToChat
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        GithubListItem(
                            title = "Ideas & Mind Map",
                            icon = Icons.Default.AccountTree,
                            iconBgColor = Color(0xFF8957E5), // GitHub Purple
                            onClick = onNavigateToMindMap
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        GithubListItem(
                            title = "Notes & Editor",
                            icon = Icons.Default.EditNote,
                            iconBgColor = Color(0xFFD29922), // GitHub Yellow
                            onClick = onNavigateToNotes
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        GithubListItem(
                            title = "Code Playground",
                            icon = Icons.Default.Code,
                            iconBgColor = Color(0xFF2F81F7), // GitHub Blue
                            onClick = onNavigateToCode
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        GithubListItem(
                            title = "Manage Models",
                            icon = Icons.Default.Storage,
                            iconBgColor = Color(0xFF6E7681), // GitHub Gray
                            onClick = onNavigateToModelManager
                        )
                    }
                }
            }

            // SECTION: SCHEDULE
            item {
                Column {
                    SectionHeader(title = "Schedule")
                    Spacer(modifier = Modifier.height(8.dp))

                    GithubListGroup {
                        GithubListItem(
                            title = "Deck",
                            icon = Icons.Default.Fullscreen,
                            iconBgColor = Color(0xFF00ADB5), // Cyan
                            onClick = onNavigateToDeck
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        GithubListItem(
                            title = "Calendar",
                            icon = Icons.Default.Event,
                            iconBgColor = Color(0xFFDA3633), // GitHub Red
                            onClick = { showCalendarDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        GithubListItem(
                            title = "Timetable",
                            icon = Icons.Default.Schedule,
                            iconBgColor = Color(0xFFBF8700), // Dark Gold
                            onClick = { showTimetableDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showCalendarDialog) {
        CalendarDialog(
            events = calendarEvents,
            onDismiss = { showCalendarDialog = false },
            onAddEvent = { viewModel.addCalendarEvent(it) },
            onDeleteEvent = { viewModel.deleteCalendarEvent(it.id) }
        )
    }

    if (showTimetableDialog) {
        TimetableDialog(
            entries = timetableEntries,
            onDismiss = { showTimetableDialog = false },
            onAddEntry = { viewModel.addTimetableEntry(it) },
            onDeleteEntry = { viewModel.deleteTimetableEntry(it.id) }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
fun GithubListGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun GithubListItem(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Icon Box
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ================= Dialog Functions =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDialog(events: List<CalendarEvent>, onDismiss: () -> Unit, onAddEvent: (CalendarEvent) -> Unit, onDeleteEvent: (CalendarEvent) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f).align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFDA3633)).padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "CALENDAR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            events.sortedBy { Calendar.getInstance().apply { timeInMillis = it.dateTime; set(Calendar.HOUR_OF_DAY, it.hour); set(Calendar.MINUTE, it.minute) }.timeInMillis }.forEach { event ->
                                item(key = event.id) {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(event.color).copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                val calendar = Calendar.getInstance().apply { timeInMillis = event.dateTime; set(Calendar.HOUR_OF_DAY, event.hour); set(Calendar.MINUTE, event.minute) }
                                                val timeStr = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(calendar.time)
                                                Text(text = timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                            IconButton(onClick = { onDeleteEvent(event) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }
                        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp), containerColor = Color(0xFFDA3633)) {
                            Icon(Icons.Default.Add, contentDescription = "Add Event", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        val timePickerState = rememberTimePickerState(initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY), initialMinute = Calendar.getInstance().get(Calendar.MINUTE))

        Dialog(onDismissRequest = { showAddDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Add Event") }, navigationIcon = { IconButton(onClick = { showAddDialog = false }) { Icon(Icons.Default.Close, "") } }) },
                bottomBar = {
                    Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        FilledTonalButton(onClick = { if (title.isNotBlank() && datePickerState.selectedDateMillis != null) { onAddEvent(CalendarEvent(title = title, description = description, dateTime = datePickerState.selectedDateMillis!!, hour = timePickerState.hour, minute = timePickerState.minute)); showAddDialog = false } }, enabled = title.isNotBlank() && datePickerState.selectedDateMillis != null, modifier = Modifier.weight(1f), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFDA3633))) { Text("Add", color = Color.White) }
                    }
                }
            ) { paddingValues ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
                    item { DatePicker(state = datePickerState, modifier = Modifier.fillMaxWidth()) }
                    item { TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableDialog(entries: List<TimetableEntry>, onDismiss: () -> Unit, onAddEntry: (TimetableEntry) -> Unit, onDeleteEntry: (TimetableEntry) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f).align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFBF8700)).padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "TIMETABLE", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEachIndexed { dayIndex, dayName ->
                                val dayEntries = entries.filter { it.dayOfWeek == dayIndex + 1 }.sortedBy { it.startTime }
                                if (dayEntries.isNotEmpty()) {
                                    item { Text(text = dayName.uppercase(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                                    dayEntries.forEach { entry ->
                                        item(key = entry.id) {
                                            Card(colors = CardDefaults.cardColors(containerColor = Color(entry.color).copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                        Text(text = "${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                    }
                                                    IconButton(onClick = { onDeleteEntry(entry) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp), containerColor = Color(0xFFBF8700)) {
                            Icon(Icons.Default.Add, contentDescription = "Add Entry", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var selectedDay by remember { mutableIntStateOf(1) }
        var startTime by remember { mutableStateOf("09:00") }
        var endTime by remember { mutableStateOf("10:00") }
        var expanded by remember { mutableStateOf(false) }
        val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        Dialog(onDismissRequest = { showAddDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Add Schedule") }, navigationIcon = { IconButton(onClick = { showAddDialog = false }) { Icon(Icons.Default.Close, "") } }) },
                bottomBar = {
                    Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        FilledTonalButton(onClick = { if (title.isNotBlank()) { onAddEntry(TimetableEntry(title = title, description = description, dayOfWeek = selectedDay, startTime = startTime, endTime = endTime)); showAddDialog = false } }, enabled = title.isNotBlank(), modifier = Modifier.weight(1f), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFBF8700))) { Text("Add", color = Color.White) }
                    }
                }
            ) { paddingValues ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
                    item {
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(value = daysOfWeek[selectedDay - 1], onValueChange = {}, readOnly = true, label = { Text("Day") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                daysOfWeek.forEachIndexed { index, day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = index + 1; expanded = false }) }
                            }
                        }
                    }
                    item { OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start Time (HH:mm)") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End Time (HH:mm)") }, modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}