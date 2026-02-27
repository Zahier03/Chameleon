package com.sotech.chameleon.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sotech.chameleon.data.ModelStatus
import com.sotech.chameleon.data.Note
import com.sotech.chameleon.data.TextMarker
import com.sotech.chameleon.ui.MainViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenModelSelector: () -> Unit
) {
    val notes by viewModel.savedNotes.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val modelState by viewModel.modelState.collectAsState()

    var editingNote by remember { mutableStateOf<Note?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    var currentAlign by remember { mutableIntStateOf(3) }
    var currentSize by remember { mutableFloatStateOf(18f) }
    var markers by remember { mutableStateOf<List<TextMarker>>(emptyList()) }
    var isFixing by remember { mutableStateOf(false) }

    var isFormattingExpanded by remember { mutableStateOf(false) }
    var isMarkerMode by remember { mutableStateOf(false) }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var currentStrokeOffsets by remember { mutableStateOf(mutableListOf<Int>()) }

    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    val composeTextAlign = when (currentAlign) {
        0 -> TextAlign.Left
        1 -> TextAlign.Center
        2 -> TextAlign.Right
        else -> TextAlign.Justify
    }

    fun saveAndCloseEditor() {
        if (editTitle.isNotBlank() || textFieldValue.text.isNotBlank()) {
            val finalTitle = if (editTitle.isBlank()) "Untitled Note" else editTitle
            viewModel.saveNote(
                id = if (isCreatingNew) null else editingNote?.id,
                title = finalTitle,
                content = textFieldValue.text,
                alignment = currentAlign,
                fontSize = currentSize
            )
        }
        editingNote = null
        isCreatingNew = false
        markers = emptyList()
        isMarkerMode = false
        currentPoints.clear()
        currentStrokeOffsets.clear()
    }

    fun exportToPdf() {
        val webView = WebView(context)
        val titleSafe = editTitle.replace("<", "&lt;").replace(">", "&gt;")
        val contentSafe = textFieldValue.text.replace("<", "&lt;").replace(">", "&gt;")

        val alignStr = when (currentAlign) {
            0 -> "left"
            1 -> "center"
            2 -> "right"
            else -> "justify"
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { 
                        font-family: 'Times New Roman', Times, serif; 
                        padding: 40px; 
                        line-height: 1.8; 
                        color: black; 
                        background-color: white;
                    }
                    h1 { 
                        text-align: center; 
                        font-size: 28pt; 
                        margin-bottom: 40px; 
                        font-weight: bold; 
                    }
                    .content { 
                        font-size: ${currentSize}pt; 
                        text-align: $alignStr; 
                        white-space: pre-wrap; 
                    }
                </style>
            </head>
            <body>
                <h1>${titleSafe.ifBlank { "Untitled Document" }}</h1>
                <div class="content">$contentSafe</div>
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = if (editTitle.isNotBlank()) editTitle else "Document"
                val printAdapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    BackHandler(enabled = (editingNote != null || isCreatingNew)) {
        saveAndCloseEditor()
    }

    val markerTransformation = remember(markers) {
        VisualTransformation { text ->
            val builder = AnnotatedString.Builder(text)
            for (marker in markers) {
                val start = marker.startIndex.coerceIn(0, text.length)
                val end = marker.endIndex.coerceIn(0, text.length)
                if (start < end) {
                    builder.addStyle(
                        style = SpanStyle(background = Color(0x88FFEB3B), color = Color.Black),
                        start = start,
                        end = end
                    )
                }
            }
            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    if (editingNote != null || isCreatingNew) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.clickable { onOpenModelSelector() }) {
                            Text(if (isCreatingNew) "New Note" else "Edit Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            when (modelState.status) {
                                ModelStatus.READY -> {
                                    Text(
                                        text = currentModel?.displayName ?: "Ready",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                ModelStatus.INITIALIZING -> {
                                    Text(
                                        text = "Initializing...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "No Model Selected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { saveAndCloseEditor() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconToggleButton(
                            checked = isMarkerMode,
                            onCheckedChange = {
                                isMarkerMode = it
                                if (!it) {
                                    currentPoints.clear()
                                    currentStrokeOffsets.clear()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Highlight,
                                contentDescription = null,
                                tint = if (isMarkerMode) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { isFormattingExpanded = !isFormattingExpanded }) {
                            Icon(Icons.Default.FormatSize, contentDescription = null)
                        }
                        IconButton(onClick = { exportToPdf() }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        }
                        IconButton(onClick = { saveAndCloseEditor() }) {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        if (!isCreatingNew) {
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            showDeleteConfirmDialog = editingNote?.id
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    )
                                }
                            }
                        }
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
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    AnimatedVisibility(visible = isFormattingExpanded) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { currentAlign = 0 },
                                    colors = ButtonDefaults.textButtonColors(contentColor = if (currentAlign == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                ) { Text("Left") }
                                TextButton(
                                    onClick = { currentAlign = 1 },
                                    colors = ButtonDefaults.textButtonColors(contentColor = if (currentAlign == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                ) { Text("Center") }
                                TextButton(
                                    onClick = { currentAlign = 2 },
                                    colors = ButtonDefaults.textButtonColors(contentColor = if (currentAlign == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                ) { Text("Right") }
                                TextButton(
                                    onClick = { currentAlign = 3 },
                                    colors = ButtonDefaults.textButtonColors(contentColor = if (currentAlign == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                ) { Text("Justify") }

                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { currentSize = (currentSize - 2f).coerceAtLeast(10f) }) { Text("A-") }
                                TextButton(onClick = { currentSize = (currentSize + 2f).coerceAtMost(40f) }) { Text("A+") }
                            }
                        }
                    }

                    BasicTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (editTitle.isEmpty()) {
                                    Text(
                                        text = "Title",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(state = rememberScrollState(), enabled = !isMarkerMode)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (!isMarkerMode) {
                                    if (newValue.text != textFieldValue.text) {
                                        markers = emptyList()
                                    }
                                    textFieldValue = newValue
                                }
                            },
                            readOnly = isMarkerMode,
                            onTextLayout = { textLayoutResult = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = currentSize.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = (currentSize * 1.5).sp,
                                textAlign = composeTextAlign
                            ),
                            cursorBrush = SolidColor(if (isMarkerMode) Color.Transparent else MaterialTheme.colorScheme.primary),
                            visualTransformation = markerTransformation,
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Start writing your formal paper...",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Serif,
                                                fontSize = currentSize.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                lineHeight = (currentSize * 1.5).sp,
                                                textAlign = composeTextAlign
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )

                        if (isMarkerMode) {
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentPoints.clear()
                                                currentPoints.add(offset)
                                                currentStrokeOffsets.clear()
                                                textLayoutResult?.getOffsetForPosition(offset)?.let { currentStrokeOffsets.add(it) }
                                            },
                                            onDrag = { change, _ ->
                                                currentPoints.add(change.position)
                                                textLayoutResult?.getOffsetForPosition(change.position)?.let { currentStrokeOffsets.add(it) }
                                            },
                                            onDragEnd = {
                                                if (currentStrokeOffsets.isNotEmpty()) {
                                                    val min = currentStrokeOffsets.minOrNull() ?: 0
                                                    val max = currentStrokeOffsets.maxOrNull() ?: 0
                                                    val text = textFieldValue.text

                                                    if (min <= max) {
                                                        var safeMin = min.coerceIn(0, text.length)
                                                        var safeMax = max.coerceIn(0, text.length)

                                                        while (safeMin > 0 && !text[safeMin - 1].isWhitespace()) {
                                                            safeMin--
                                                        }
                                                        while (safeMax < text.length && !text[safeMax].isWhitespace()) {
                                                            safeMax++
                                                        }

                                                        if (safeMin < safeMax) {
                                                            val markedText = text.substring(safeMin, safeMax)
                                                            markers = markers + TextMarker(
                                                                id = "M_${System.currentTimeMillis()}_${markers.size}",
                                                                startIndex = safeMin,
                                                                endIndex = safeMax,
                                                                originalText = markedText
                                                            )
                                                        }
                                                    }
                                                }
                                                currentPoints.clear()
                                                currentStrokeOffsets.clear()
                                            },
                                            onDragCancel = {
                                                currentPoints.clear()
                                                currentStrokeOffsets.clear()
                                            }
                                        )
                                    }
                            ) {
                                if (currentPoints.size > 1) {
                                    val path = Path().apply {
                                        moveTo(currentPoints.first().x, currentPoints.first().y)
                                        for (i in 1 until currentPoints.size) {
                                            lineTo(currentPoints[i].x, currentPoints[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(0xAAFFEB3B),
                                        style = Stroke(width = currentSize * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                } else if (currentPoints.size == 1) {
                                    drawCircle(
                                        color = Color(0xAAFFEB3B),
                                        radius = currentSize * 1.25f,
                                        center = currentPoints.first()
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = markers.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionButton(
                            onClick = { markers = emptyList() },
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.error,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }

                        ExtendedFloatingActionButton(
                            onClick = {
                                if (modelState.status != ModelStatus.READY) {
                                    onOpenModelSelector()
                                    return@ExtendedFloatingActionButton
                                }
                                isFixing = true
                                val prompt = "You are an expert editor. Fix the grammar, clarity, and formal tone of the following text snippets. Return ONLY a valid JSON object where keys are the IDs and values are the corrected strings. Do not wrap in markdown like ```json. Example: {\"id1\":\"fixed text\"}\n\n" + markers.joinToString("\n") { "\"${it.id}\": \"${it.originalText}\"" }
                                viewModel.fixNoteText(prompt) { response ->
                                    try {
                                        val cleanJson = response.substringAfter("{").substringBeforeLast("}")
                                        val jsonObject = JSONObject("{$cleanJson}")
                                        var newText = textFieldValue.text
                                        val sortedMarkers = markers.sortedByDescending { it.startIndex }
                                        for (marker in sortedMarkers) {
                                            if (jsonObject.has(marker.id)) {
                                                val fixed = jsonObject.getString(marker.id)
                                                val safeStart = marker.startIndex.coerceIn(0, newText.length)
                                                val safeEnd = marker.endIndex.coerceIn(0, newText.length)
                                                if (safeStart < safeEnd) {
                                                    newText = newText.replaceRange(safeStart, safeEnd, fixed)
                                                }
                                            }
                                        }
                                        textFieldValue = TextFieldValue(newText)
                                        markers = emptyList()
                                        isMarkerMode = false
                                    } catch (e: Exception) {
                                    } finally {
                                        isFixing = false
                                    }
                                }
                            },
                            icon = {
                                if (isFixing) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onTertiary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                }
                            },
                            text = { Text(if (isFixing) "Fixing..." else "Fix Marked (${markers.size})") },
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                        )

                        FloatingActionButton(
                            onClick = {
                                if (markers.isNotEmpty()) {
                                    markers = markers.dropLast(1)
                                }
                            },
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Notes", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editTitle = ""
                        textFieldValue = TextFieldValue("")
                        currentAlign = 3
                        currentSize = 18f
                        isCreatingNew = true
                        editingNote = null
                        markers = emptyList()
                        isMarkerMode = false
                        currentPoints.clear()
                        currentStrokeOffsets.clear()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        ) { paddingValues ->
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No formal notes yet.\nTap + to start writing.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    editingNote = note
                                    editTitle = note.title
                                    textFieldValue = TextFieldValue(note.content)
                                    currentAlign = note.alignment
                                    currentSize = note.fontSize
                                    isCreatingNew = false
                                    markers = emptyList()
                                    isMarkerMode = false
                                    currentPoints.clear()
                                    currentStrokeOffsets.clear()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(Date(note.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = note.content.ifBlank { "No content" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(showDeleteConfirmDialog!!)
                        showDeleteConfirmDialog = null
                        editingNote = null
                        isCreatingNew = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                onBack()
            }
        }
    }
    DisposableEffect(backDispatcher) {
        backDispatcher?.addCallback(backCallback)
        onDispose {
            backCallback.remove()
        }
    }
    LaunchedEffect(enabled) {
        backCallback.isEnabled = enabled
    }
}