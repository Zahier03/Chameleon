package com.sotech.chameleon.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.sotech.chameleon.ui.MainViewModel
import com.sotech.chameleon.ui.TerminalSession
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

enum class TerminalDisplayMode { DOCKED, FLOATING, MINIMIZED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodePlaygroundScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentPlaygroundDir by viewModel.currentPlaygroundDir.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()

    val currentFile by viewModel.currentFile.collectAsState()
    val codeContent by viewModel.codeContent.collectAsState()
    val isRunning by viewModel.isPlaygroundRunning.collectAsState()

    val terminalSessions by viewModel.terminalSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    val activeSession = terminalSessions.find { it.id == activeSessionId } ?: terminalSessions.firstOrNull()

    // Notebook States
    val isColabMode by viewModel.isColabMode.collectAsState()
    val notebookCells by viewModel.notebookCells.collectAsState()
    val installedPackages by viewModel.installedPackages.collectAsState()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var menuExpandedPath by remember { mutableStateOf<String?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }

    // Terminal Visual States
    var terminalMode by remember { mutableStateOf(TerminalDisplayMode.DOCKED) }
    var dockedHeight by remember { mutableFloatStateOf(800f) }
    var floatingOffsetX by remember { mutableFloatStateOf(100f) }
    var floatingOffsetY by remember { mutableFloatStateOf(400f) }

    // Image Popup Overlay
    var popupImage by remember { mutableStateOf<Bitmap?>(null) }
    var imgOffsetX by remember { mutableFloatStateOf(0f) }
    var imgOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        viewModel.refreshFileTree()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Explorer",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val relPath = currentPlaygroundDir.absolutePath.substringAfter("playground", "")
                    Text(
                        text = "Target: /playground$relPath",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (relPath.isNotEmpty()) {
                        IconButton(onClick = { viewModel.resetTargetFolder() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Home, contentDescription = "Root", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider()

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(fileTree) { node ->
                        val isFolder = node.file.isDirectory
                        val isSelected = currentFile?.absolutePath == node.file.absolutePath

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isFolder) {
                                        viewModel.toggleFolder(node.file)
                                    } else {
                                        viewModel.selectPlaygroundFile(node.file)
                                        scope.launch { drawerState.close() }
                                    }
                                }
                                .background(if (isSelected && !isFolder) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f) else Color.Transparent)
                                .padding(start = (16 + node.depth * 16).dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isFolder) {
                                Icon(
                                    imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                            } else {
                                Spacer(Modifier.width(20.dp)) // Align with folder text
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = node.file.name,
                                fontSize = 14.sp,
                                color = if (isSelected && !isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Box {
                                IconButton(
                                    onClick = { menuExpandedPath = node.file.absolutePath },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = menuExpandedPath == node.file.absolutePath,
                                    onDismissRequest = { menuExpandedPath = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpandedPath = null
                                            fileToDelete = node.file
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { showNewFileDialog = true }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New File")
                    }
                    TextButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Folder")
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.imePadding(),
                topBar = {
                    TopAppBar(
                        title = { Text(currentFile?.name ?: "No File Selected", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // View Installed Packages
                            IconButton(onClick = { viewModel.fetchInstalledPackages() }) {
                                Icon(Icons.Default.Inventory2, contentDescription = "Packages", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            // Toggle Mode
                            IconButton(onClick = { viewModel.toggleColabMode() }) {
                                Icon(
                                    imageVector = if (isColabMode) Icons.Default.ViewHeadline else Icons.Default.ViewAgenda,
                                    contentDescription = "Toggle Colab Mode"
                                )
                            }

                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Folder, contentDescription = "Files")
                            }

                            // Run Button (Only shows in Standard Mode, Colab has per-cell run buttons)
                            if (!isColabMode) {
                                IconButton(
                                    onClick = { viewModel.runPlaygroundCode() },
                                    enabled = currentFile != null && !isRunning
                                ) {
                                    if (isRunning) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF4CAF50))
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Run Code", tint = if(currentFile != null) Color(0xFF4CAF50) else Color.Gray)
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.savePlaygroundFile() },
                                enabled = currentFile != null
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (currentFile == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                Text("Select or create a file to start coding", color = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { scope.launch { drawerState.open() } }) { Text("Open Files") }
                            }
                        }
                    } else if (isColabMode) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Jupyter/Colab Mode", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { viewModel.restartSessionAndClearOutputs() }) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restart Runtime")
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(bottom = 300.dp), // Fixes text hiding behind keyboard
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(notebookCells) { cell ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                                // Run Button
                                                IconButton(
                                                    onClick = { viewModel.runCell(cell.id) },
                                                    enabled = !cell.isRunning,
                                                    modifier = Modifier.align(Alignment.Top).size(40.dp)
                                                ) {
                                                    if (cell.isRunning) {
                                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF4CAF50))
                                                    } else {
                                                        Box(modifier = Modifier.size(32.dp).background(Color(0xFF4CAF50).copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }

                                                // Code Editor
                                                BasicTextField(
                                                    value = cell.code,
                                                    onValueChange = { viewModel.updateCellCode(cell.id, it) },
                                                    modifier = Modifier.weight(1f).padding(top = 8.dp, bottom = 8.dp).horizontalScroll(rememberScrollState()),
                                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                                )

                                                // Cell Actions
                                                Column {
                                                    IconButton(onClick = { viewModel.removeCell(cell.id) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(onClick = { viewModel.addCellBelow(cell.id) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Add, contentDescription = "Add Below", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            // Output Area
                                            if (cell.output.isNotEmpty()) {
                                                HorizontalDivider(color = Color.Gray.copy(alpha=0.2f))
                                                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E).copy(alpha=0.8f)).padding(12.dp)) {
                                                    cell.output.forEach { out ->
                                                        if (out.text.isNotBlank()) {
                                                            Text(
                                                                text = out.text,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 12.sp,
                                                                color = if (out.isError) Color(0xFFFF5252) else Color(0xFF4CAF50)
                                                            )
                                                        }
                                                        if (out.imageBitmap != null) {
                                                            Spacer(Modifier.height(8.dp))
                                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                                Image(
                                                                    bitmap = out.imageBitmap.asImageBitmap(),
                                                                    contentDescription = "Plot",
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .heightIn(max = 300.dp)
                                                                        .clickable { popupImage = out.imageBitmap },
                                                                    contentScale = ContentScale.Fit
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedButton(
                                        onClick = { viewModel.addCellBelow(notebookCells.lastOrNull()?.id ?: "") },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("+ Code")
                                    }
                                }
                            }
                        }

                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .verticalScroll(rememberScrollState())
                            ) {
                                BasicTextField(
                                    value = codeContent,
                                    onValueChange = { viewModel.updateCodeContent(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(16.dp)
                                        .padding(bottom = 300.dp), // Deep bottom padding
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        // Docked Terminal
                        if (terminalMode == TerminalDisplayMode.DOCKED) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(with(LocalDensity.current) { dockedHeight.toDp() })
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                color = Color(0xFF1E1E1E),
                                shadowElevation = 8.dp
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Draggable Handle & Toolbar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF2D2D2D))
                                            .pointerInput(Unit) {
                                                detectVerticalDragGestures { _, dragAmount ->
                                                    dockedHeight = (dockedHeight - dragAmount).coerceIn(200f, 2000f)
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TerminalTabs(
                                            sessions = terminalSessions,
                                            activeSessionId = activeSessionId,
                                            onSwitch = { viewModel.switchTerminalSession(it) },
                                            onAdd = { viewModel.createTerminalSession() },
                                            onClose = { viewModel.closeTerminalSession(it) },
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { activeSession?.let { viewModel.clearTerminal(it.id) } }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { terminalMode = TerminalDisplayMode.FLOATING }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = "Float", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { terminalMode = TerminalDisplayMode.MINIMIZED }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    // Active Terminal Content
                                    TerminalOutputView(
                                        session = activeSession,
                                        onShowImage = { popupImage = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Minimized Bubble
            if (!isColabMode && terminalMode == TerminalDisplayMode.MINIMIZED) {
                FloatingActionButton(
                    onClick = { terminalMode = TerminalDisplayMode.DOCKED },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .zIndex(50f),
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Restore Terminal")
                }
            }

            // Floating Terminal Overlay
            if (!isColabMode && terminalMode == TerminalDisplayMode.FLOATING) {
                Surface(
                    modifier = Modifier
                        .offset { IntOffset(floatingOffsetX.roundToInt(), floatingOffsetY.roundToInt()) }
                        .size(width = 350.dp, height = 300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .zIndex(50f),
                    color = Color(0xFF1E1E1E),
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D2D))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        floatingOffsetX += dragAmount.x
                                        floatingOffsetY += dragAmount.y
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Terminal (Floating)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                            Row {
                                IconButton(onClick = { terminalMode = TerminalDisplayMode.DOCKED }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Dock", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { terminalMode = TerminalDisplayMode.MINIMIZED }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Minimize", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        TerminalTabs(
                            sessions = terminalSessions,
                            activeSessionId = activeSessionId,
                            onSwitch = { viewModel.switchTerminalSession(it) },
                            onAdd = { viewModel.createTerminalSession() },
                            onClose = { viewModel.closeTerminalSession(it) },
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E))
                        )

                        TerminalOutputView(
                            session = activeSession,
                            onShowImage = { popupImage = it }
                        )
                    }
                }
            }

            // Installed Packages Overlay
            if (installedPackages != null) {
                Dialog(
                    onDismissRequest = { viewModel.dismissInstalledPackages() },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 24.dp
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Installed Python Modules", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                IconButton(onClick = { viewModel.dismissInstalledPackages() }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(installedPackages!!) { pkg ->
                                    Text(
                                        text = "• $pkg",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Image Popup Overlay with Save Button
            if (popupImage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f))) // Darker background

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(imgOffsetX.roundToInt(), imgOffsetY.roundToInt()) }
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    imgOffsetX += dragAmount.x
                                    imgOffsetY += dragAmount.y
                                }
                            },
                        shadowElevation = 12.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Generated Plot", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 8.dp))

                                Row {
                                    IconButton(onClick = { viewModel.saveImageToGallery(popupImage!!) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        popupImage = null
                                        imgOffsetX = 0f
                                        imgOffsetY = 0f
                                    }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                            Image(
                                bitmap = popupImage!!.asImageBitmap(),
                                contentDescription = "Matplotlib Plot",
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .heightIn(max = 400.dp)
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }

    if (fileToDelete != null) {
        val isFolder = fileToDelete!!.isDirectory
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(if (isFolder) "Delete Folder" else "Delete File") },
            text = { Text("Are you sure you want to delete '${fileToDelete!!.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaygroundItem(fileToDelete!!)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New Python File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name (e.g. script.py)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotBlank()) {
                        viewModel.createPlaygroundFile(newFileName)
                        showNewFileDialog = false
                        newFileName = ""
                        scope.launch { drawerState.close() }
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createPlaygroundFolder(newFolderName)
                        showNewFolderDialog = false
                        newFolderName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TerminalTabs(
    sessions: List<TerminalSession>,
    activeSessionId: String,
    onSwitch: (String) -> Unit,
    onAdd: () -> Unit,
    onClose: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = sessions.indexOfFirst { it.id == activeSessionId }.coerceAtLeast(0),
        edgePadding = 4.dp,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        divider = {},
        indicator = {},
        modifier = modifier.height(36.dp)
    ) {
        sessions.forEach { session ->
            val isSelected = session.id == activeSessionId
            Tab(
                selected = isSelected,
                onClick = { onSwitch(session.id) },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(session.name, fontSize = 12.sp, color = if (isSelected) Color(0xFF4CAF50) else Color.Gray)
                    if (sessions.size > 1) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Tab",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onClose(session.id) }
                        )
                    }
                }
            }
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Add Terminal", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun TerminalOutputView(
    session: TerminalSession?,
    onShowImage: (Bitmap) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(session?.output?.size) {
        if (session != null && session.output.isNotEmpty()) {
            listState.animateScrollToItem(session.output.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Added padding to bottom so the final line isn't cut off
    ) {
        if (session == null || session.output.isEmpty()) {
            item {
                Text("Terminal ready.", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        } else {
            items(session.output) { output ->
                if (output.text.isNotBlank()) {
                    Text(
                        text = output.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (output.isError) Color(0xFFFF5252) else Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (output.imageBitmap != null) {
                    Button(
                        onClick = { onShowImage(output.imageBitmap) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 8.dp)
                            .height(32.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View Generated Plot", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}