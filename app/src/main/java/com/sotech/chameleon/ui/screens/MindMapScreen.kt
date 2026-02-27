package com.sotech.chameleon.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sotech.chameleon.data.ModelStatus
import com.sotech.chameleon.data.MindMapVersion
import com.sotech.chameleon.ui.MainViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenModelSelector: () -> Unit
) {
    // ViewModel States
    val currentModel by viewModel.currentModel.collectAsState()
    val mindMapContent by viewModel.mindMapContent.collectAsState()
    val isGenerating by viewModel.isGeneratingMindMap.collectAsState()
    val savedMaps by viewModel.savedMindMaps.collectAsState()

    // Model Status States
    val modelState by viewModel.modelState.collectAsState()
    val modelStatus = modelState.status
    val modelError = modelState.error
    val initializationProgress by viewModel.initializationProgress.collectAsState()

    var promptText by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var editableContent by remember { mutableStateOf(mindMapContent) }
    var showCodeEditor by remember { mutableStateOf(false) }

    // States for Colors, Themes, Search Dialog, Folding Input, and Menu
    var mindMapBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var mindMapTheme by remember { mutableStateOf("default") }
    var searchNodeText by remember { mutableStateOf<String?>(null) }

    // UI Fold States
    var isInputExpanded by remember { mutableStateOf(true) }
    var isStylingExpanded by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    // Version & Multi-map states
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }

    var showVersionDialog by remember { mutableStateOf(false) }
    var selectedVersions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var connectedPrintMaps by remember { mutableStateOf<List<MindMapVersion>>(emptyList()) }

    // AI Styling States
    var showAiStyleDialog by remember { mutableStateOf(false) }
    var aiStylePrompt by remember { mutableStateOf("") }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isApiModel = currentModel?.isApiModel == true
    val isModelReady = modelStatus == ModelStatus.READY

    // Keep editable content synced with viewmodel generation
    LaunchedEffect(mindMapContent) {
        editableContent = mindMapContent
    }

    val displayCodes = remember(editableContent, connectedPrintMaps) {
        if (connectedPrintMaps.isNotEmpty()) {
            listOf(editableContent) + connectedPrintMaps.map { it.content }
        } else {
            listOf(editableContent)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImages = uris
        if (uris.isNotEmpty()) isInputExpanded = true
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPdf = uri
        if (uri != null) isInputExpanded = true
    }

    val backgroundColors = listOf(
        Color.Transparent, Color(0xFFF8F9FA), Color(0xFFFFF0F5),
        Color(0xFFE6E6FA), Color(0xFFF0F8FF), Color(0xFFF5FFFA),
        Color(0xFFFFFACD), Color(0xFF1E1E1E)
    )
    val mermaidThemes = listOf("default", "dark", "forest", "neutral", "base")

    val currentShareUrl = remember(editableContent, mindMapTheme) {
        try {
            val jsonString = JSONObject().apply {
                put("code", editableContent)
                put("mermaid", "{\n  \"theme\": \"$mindMapTheme\"\n}")
                put("autoSync", true)
                put("updateDiagram", false)
            }.toString()
            val base64 = android.util.Base64.encodeToString(
                jsonString.toByteArray(Charsets.UTF_8),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            )
            "https://mermaid.live/edit#base64:$base64"
        } catch (e: Exception) {
            "https://mermaid.live"
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable { onOpenModelSelector() }
                    ) {
                        Text("Mind Map Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        when (modelStatus) {
                            ModelStatus.INITIALIZING -> {
                                Text(
                                    text = initializationProgress.ifBlank { "Initializing..." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ModelStatus.ERROR -> {
                                Text(
                                    text = modelError ?: "Error loading model",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ModelStatus.READY -> {
                                Text(
                                    text = if (isGenerating) "Generating Map..." else currentModel?.displayName ?: "Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            else -> {
                                Text(
                                    text = "No Model Selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenModelSelector) {
                        Icon(Icons.Default.Settings, contentDescription = "Model Settings")
                    }

                    // Always show the 3-dot menu if we aren't generating, so user can access History
                    if (!isGenerating) {
                        Box {
                            IconButton(onClick = { isMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false }
                            ) {
                                // Only show these if there is an active map
                                if (editableContent.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = { Text(if (showCodeEditor) "Show Map View" else "Edit Mermaid Code") },
                                        onClick = {
                                            showCodeEditor = !showCodeEditor
                                            isMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (showCodeEditor) Icons.Default.Map else Icons.Default.Code,
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Save Current Version") },
                                        onClick = {
                                            isMenuExpanded = false
                                            saveTitle = ""
                                            showSaveDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                                    )
                                }

                                // ALWAYS SHOW VERSION HISTORY
                                DropdownMenuItem(
                                    text = { Text("Version History & Connect") },
                                    onClick = {
                                        isMenuExpanded = false
                                        showVersionDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                                )

                                if (editableContent.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = { Text("Share Link (Mermaid Live)") },
                                        onClick = {
                                            isMenuExpanded = false
                                            try {
                                                val sendIntent: Intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Check out my beautiful mind map!\n\n$currentShareUrl")
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Share Mind Map")
                                                context.startActivity(shareIntent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Export to PDF (Inc. Connected)") },
                                        onClick = {
                                            isMenuExpanded = false
                                            webViewInstance?.let { webView ->
                                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                                val jobName = "MindMap_${System.currentTimeMillis()}"
                                                val printAdapter = webView.createPrintDocumentAdapter(jobName)

                                                val printAttributes = PrintAttributes.Builder()
                                                    .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE)
                                                    .build()

                                                printManager.print(jobName, printAdapter, printAttributes)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isInputExpanded,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { isInputExpanded = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Prompt")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isInputExpanded,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Create or update map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    isInputExpanded = false
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Input", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        AnimatedVisibility(visible = selectedImages.isNotEmpty() || selectedPdf != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedPdf?.let {
                                    AssistChip(
                                        onClick = { selectedPdf = null },
                                        label = { Text("PDF Attached") },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove PDF", modifier = Modifier.size(16.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.error)
                                    )
                                }
                                if (selectedImages.isNotEmpty()) {
                                    AssistChip(
                                        onClick = { selectedImages = emptyList() },
                                        label = { Text("${selectedImages.size} Image(s)") },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove Images", modifier = Modifier.size(16.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { imagePicker.launch("image/*") },
                                enabled = isModelReady && !isGenerating,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Attach Image", modifier = Modifier.size(24.dp))
                            }

                            FilledTonalIconButton(
                                onClick = { pdfPicker.launch("application/pdf") },
                                enabled = isModelReady && isApiModel && !isGenerating,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Attach PDF", modifier = Modifier.size(24.dp))
                            }

                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Topic or Prompt...") },
                                maxLines = 5,
                                enabled = isModelReady && !isGenerating,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            FilledIconButton(
                                onClick = {
                                    val bitmaps = selectedImages.mapNotNull { uri ->
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                            }
                                        } catch (e: Exception) { null }
                                    }
                                    viewModel.generateMindMap(promptText, bitmaps, selectedPdf)
                                    showCodeEditor = false
                                    keyboardController?.hide()
                                    isInputExpanded = false
                                    connectedPrintMaps = emptyList()
                                },
                                enabled = isModelReady && !isGenerating && (promptText.isNotBlank() || selectedImages.isNotEmpty() || selectedPdf != null),
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Generate", modifier = Modifier.size(20.dp))
                            }
                        }

                        if (!isApiModel && isModelReady) {
                            Text(
                                text = "PDF uploads are disabled. Please use a Gemini API model.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {

            AnimatedVisibility(visible = editableContent.isNotBlank() && !showCodeEditor && isModelReady) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .animateContentSize(animationSpec = tween(300)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isStylingExpanded = !isStylingExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Brush,
                                    contentDescription = "Style",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Design & Aesthetics",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (isStylingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Styling",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isStylingExpanded) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showAiStyleDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "AI Magic")
                                Spacer(Modifier.width(8.dp))
                                Text("Ask AI to Style Map", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Map Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                mermaidThemes.forEach { theme ->
                                    FilterChip(
                                        selected = mindMapTheme == theme,
                                        onClick = { mindMapTheme = theme },
                                        label = { Text(theme.replaceFirstChar { it.uppercase() }) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Canvas Background", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                backgroundColors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surface else color)
                                            .border(
                                                width = 2.dp,
                                                color = if (mindMapBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
                                            .clickable { mindMapBackgroundColor = color },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (color == Color.Transparent) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp), tint = Color.Gray)
                                        }
                                        if (mindMapBackgroundColor == color) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp), tint = if (color == Color.Transparent || color == Color(0xFF1E1E1E)) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (!showCodeEditor) mindMapBackgroundColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (!isModelReady) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            "Model Not Ready",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (modelStatus == ModelStatus.INITIALIZING) "Initializing model... Please wait." else "Please select and initialize an AI model to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (modelStatus != ModelStatus.INITIALIZING) {
                            Button(onClick = onOpenModelSelector) {
                                Text("Select Model")
                            }
                        }
                    }
                } else if (isGenerating) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(mindMapContent, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (editableContent.isNotBlank()) {
                    if (showCodeEditor) {
                        OutlinedTextField(
                            value = editableContent,
                            onValueChange = {
                                editableContent = it
                                viewModel.updateMindMapContent(it)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            label = { Text("Mermaid.js Structure (Edit to change map)") }
                        )
                    } else {
                        MermaidWebView(
                            mermaidCodes = displayCodes,
                            theme = mindMapTheme,
                            shareUrl = currentShareUrl,
                            onNodeClicked = { clickedText ->
                                searchNodeText = clickedText
                            },
                            modifier = Modifier.fillMaxSize(),
                            onWebViewReady = { webView ->
                                webViewInstance = webView
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountTree,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            "What shall we learn today?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Enter a prompt below to generate a beautiful map",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Version") },
            text = {
                OutlinedTextField(
                    value = saveTitle,
                    onValueChange = { saveTitle = it },
                    label = { Text("Version Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (saveTitle.isNotBlank()) {
                        viewModel.saveMindMapVersion(saveTitle, editableContent)
                        showSaveDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Version History & Connections") },
            text = {
                if (savedMaps.isEmpty()) {
                    Text(
                        "No saved versions found. Generate and save a map first!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(savedMaps) { map ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedVersions.contains(map.id)) {
                                            selectedVersions = selectedVersions - map.id
                                        } else {
                                            selectedVersions = selectedVersions + map.id
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedVersions.contains(map.id),
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(map.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(map.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    editableContent = map.content
                                    viewModel.updateMindMapContent(map.content)
                                    connectedPrintMaps = emptyList()
                                    showVersionDialog = false
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Load")
                                }
                                IconButton(onClick = { viewModel.deleteMindMapVersion(map.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedVersions.size > 1) {
                    Button(onClick = {
                        val selectedMapsList = savedMaps.filter { selectedVersions.contains(it.id) }
                        val sb = StringBuilder()
                        sb.append("graph TD\n")
                        selectedMapsList.forEach { map ->
                            val cleanContent = map.content
                                .replace("graph TD", "")
                                .replace("graph LR", "")
                                .replace("mindmap", "")
                                .trim()
                            val safeTitle = map.title.replace(Regex("[^A-Za-z0-9]"), "_")
                            sb.append("subgraph $safeTitle[${map.title}]\n")
                            sb.append(cleanContent)
                            sb.append("\nend\n")
                        }
                        for (i in 0 until selectedMapsList.size - 1) {
                            val t1 = selectedMapsList[i].title.replace(Regex("[^A-Za-z0-9]"), "_")
                            val t2 = selectedMapsList[i+1].title.replace(Regex("[^A-Za-z0-9]"), "_")
                            sb.append("$t1 --> $t2\n")
                        }
                        val combinedCode = sb.toString()

                        editableContent = combinedCode
                        viewModel.updateMindMapContent(combinedCode)
                        connectedPrintMaps = selectedMapsList
                        showVersionDialog = false
                    }) {
                        Text("Combine Maps for Print")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showVersionDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAiStyleDialog) {
        AlertDialog(
            onDismissRequest = { showAiStyleDialog = false },
            icon = {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Magic AI",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("AI Magic Stylist", color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column {
                    Text(
                        "Describe your dream aesthetic! The AI will redesign your map's structure, emojis, and colors based on your prompt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = aiStylePrompt,
                        onValueChange = { aiStylePrompt = it },
                        placeholder = { Text("e.g., Make it soft pastel pink, add cute emojis, make it look like a galaxy...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiStyleDialog = false
                        val magicPrompt = if (promptText.isNotBlank()) {
                            "$promptText\n\nCRITICAL STYLING INSTRUCTIONS: $aiStylePrompt"
                        } else {
                            "Redesign the current mind map with this aesthetic style: $aiStylePrompt. Please update node names with appropriate emojis if requested."
                        }
                        viewModel.generateMindMap(magicPrompt, emptyList(), null)
                        isStylingExpanded = false
                        connectedPrintMaps = emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Apply Magic")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiStyleDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (searchNodeText != null) {
        Dialog(
            onDismissRequest = { searchNodeText = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = searchNodeText ?: "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { searchNodeText = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        return false
                                    }
                                }
                            }
                        },
                        update = { webView ->
                            val url = "https://www.google.com/search?q=${Uri.encode(searchNodeText)}"
                            webView.loadUrl(url)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

class WebAppInterface(private val onNodeClick: (String) -> Unit) {
    @JavascriptInterface
    fun onNodeClicked(text: String) {
        onNodeClick(text)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidWebView(
    mermaidCodes: List<String>,
    theme: String,
    shareUrl: String,
    onNodeClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {}
) {
    val diagramsHtml = mermaidCodes.joinToString("\n<div class='page-break'></div>\n") { code ->
        "<div class=\"mermaid\">\n$code\n</div>"
    }

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=10.0, user-scalable=yes">
            <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap" rel="stylesheet">
            <script type="module">
                import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
                mermaid.initialize({ 
                    startOnLoad: true, 
                    theme: '$theme', 
                    securityLevel: 'loose',
                    fontFamily: "'Roboto', sans-serif",
                    flowchart: { htmlLabels: true }
                });
                
                const observer = new MutationObserver((mutations, obs) => {
                    const nodes = document.querySelectorAll('.node');
                    if (nodes.length > 0) {
                        nodes.forEach(node => {
                            node.style.cursor = 'pointer';
                            node.addEventListener('click', function(e) {
                                let text = this.textContent || this.innerText;
                                window.AndroidInterface.onNodeClicked(text.trim());
                            });
                        });
                        obs.disconnect(); 
                    }
                });
                observer.observe(document.body, { childList: true, subtree: true });
            </script>
            <style>
                body { 
                    margin: 0; 
                    padding: 24px; 
                    background-color: transparent; 
                    font-family: 'Roboto', sans-serif;
                }
                .pdf-header {
                    display: none; 
                }
                .page-break {
                    display: none;
                }
                @media print {
                    @page {
                        margin: 10mm; 
                    }
                    body {
                        background-color: white !important;
                        padding: 0;
                        margin: 0;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        -webkit-print-color-adjust: exact !important;
                        color-adjust: exact !important;
                    }
                    * {
                        -webkit-user-select: text !important;
                        user-select: text !important;
                    }
                    .pdf-header {
                        display: flex; 
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        margin-bottom: 24px;
                        padding-top: 12px;
                        width: 100%;
                    }
                    .pdf-header p {
                        margin: 0 0 8px 0;
                        color: #5f6368;
                        font-size: 13px;
                        font-weight: 500;
                    }
                    .pdf-header a {
                        display: inline-block;
                        padding: 8px 24px;
                        background-color: #f0f4f8 !important; 
                        color: #0b57d0 !important; 
                        text-decoration: none;
                        border: 1px solid #c2e7ff;
                        border-radius: 100px; 
                        font-size: 14px;
                        font-weight: 500;
                        letter-spacing: 0.25px;
                    }
                    .mermaid {
                        width: 100%;
                        display: flex;
                        justify-content: center;
                        margin-bottom: 20px;
                    }
                    .mermaid svg {
                        max-width: 100% !important;
                        height: auto !important;
                    }
                    .page-break {
                        display: block;
                        page-break-after: always;
                        height: 0;
                        border: none;
                    }
                    text, .edgeLabel, .nodeLabel {
                        font-family: 'Roboto', sans-serif !important;
                    }
                }
                .mermaid { 
                    text-align: center;
                    width: max-content; 
                    min-width: 100%;
                }
                .mermaid svg { 
                    max-width: none !important; 
                }
            </style>
        </head>
        <body>
            <div class="pdf-header">
                <p>Interactive Map Available</p>
                <a href="$shareUrl">View Interactive Map</a>
            </div>
            $diagramsHtml
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                addJavascriptInterface(WebAppInterface(onNodeClicked), "AndroidInterface")

                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                onWebViewReady(this)
            }
        },
        update = { webView ->
            val encodedHtml = android.util.Base64.encodeToString(html.toByteArray(), android.util.Base64.NO_PADDING)
            webView.loadData(encodedHtml, "text/html", "base64")
        },
        modifier = modifier
    )
}