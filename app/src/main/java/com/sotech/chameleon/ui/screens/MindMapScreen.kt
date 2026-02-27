package com.sotech.chameleon.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sotech.chameleon.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentModel by viewModel.currentModel.collectAsState()
    val mindMapContent by viewModel.mindMapContent.collectAsState()
    val isGenerating by viewModel.isGeneratingMindMap.collectAsState()

    var promptText by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var editableContent by remember { mutableStateOf(mindMapContent) }
    var showCodeEditor by remember { mutableStateOf(false) }

    // UI Fold States
    var isInputExpanded by remember { mutableStateOf(true) }
    var isCustomizationExpanded by remember { mutableStateOf(false) } // Foldable Customization Menu
    var isMenuExpanded by remember { mutableStateOf(false) }
    var searchNodeText by remember { mutableStateOf<String?>(null) }

    // Map Styling States
    var mindMapBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var mindMapTheme by remember { mutableStateOf("default") }

    // Custom Background States
    var showCustomBgDialog by remember { mutableStateOf(false) }
    var isCustomBgEnabled by remember { mutableStateOf(false) }
    var customCss by remember { mutableStateOf("") }
    var customHtml by remember { mutableStateOf("<div class=\"container\"></div>") }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isApiModel = currentModel?.isApiModel == true
    val scope = rememberCoroutineScope()

    LaunchedEffect(mindMapContent) {
        editableContent = mindMapContent
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
        Color.Transparent, Color(0xFFF5F5F5), Color(0xFFFFF9C4),
        Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFCE4EC), Color(0xFF1E1E1E)
    )
    val mermaidThemes = listOf("default", "dark", "forest", "neutral")

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Mind Map Generator", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (editableContent.isNotBlank() && !isGenerating) {
                        Box {
                            IconButton(onClick = { isMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (showCodeEditor) "Show Map View" else "Edit Mermaid Code") },
                                    onClick = {
                                        showCodeEditor = !showCodeEditor
                                        isMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = if (showCodeEditor) Icons.Default.Map else Icons.Default.Code, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Share Link (Mermaid Live)") },
                                    onClick = {
                                        isMenuExpanded = false
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
                                            val shareUrl = "https://mermaid.live/edit#base64:$base64"

                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Check out my mind map!\n\n$shareUrl")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Mind Map"))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                )
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
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Create or update map", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            IconButton(
                                onClick = {
                                    isInputExpanded = false
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Input")
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
                            FilledIconButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (selectedImages.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Attach Image", modifier = Modifier.size(24.dp))
                            }

                            FilledIconButton(
                                onClick = { pdfPicker.launch("application/pdf") },
                                enabled = isApiModel,
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (selectedPdf != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Attach PDF", modifier = Modifier.size(24.dp))
                            }

                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Topic or Prompt...") },
                                maxLines = 5,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                                },
                                enabled = !isGenerating && (promptText.isNotBlank() || selectedImages.isNotEmpty() || selectedPdf != null),
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (promptText.isNotBlank() || selectedImages.isNotEmpty() || selectedPdf != null)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Generate", modifier = Modifier.size(24.dp))
                            }
                        }

                        if (!isApiModel) {
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

            // --- FOLDABLE CUSTOMIZATION MENU ---
            AnimatedVisibility(visible = editableContent.isNotBlank() && !showCodeEditor) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Toggle Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCustomizationExpanded = !isCustomizationExpanded }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Customize Map Design", style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(
                            imageVector = if (isCustomizationExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Customization"
                        )
                    }

                    // Foldable Content
                    AnimatedVisibility(visible = isCustomizationExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            // Map Theme
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Theme:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp))
                                mermaidThemes.forEach { theme ->
                                    FilterChip(
                                        selected = mindMapTheme == theme,
                                        onClick = { mindMapTheme = theme },
                                        label = { Text(theme.replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            // Backgrounds
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Background:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp))

                                // Standard Colors
                                backgroundColors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else color)
                                            .border(
                                                width = 2.dp,
                                                color = if (!isCustomBgEnabled && mindMapBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                mindMapBackgroundColor = color
                                                isCustomBgEnabled = false
                                            }
                                    ) {
                                        if (color == Color.Transparent) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
                                        }
                                    }
                                }

                                // Custom Code / AI Generator Button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            width = 2.dp,
                                            color = if (isCustomBgEnabled) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { showCustomBgDialog = true }
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = "Custom CSS", modifier = Modifier.align(Alignment.Center).size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            // Display Area (Visual Mind Map or Code Editor)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!showCodeEditor && !isCustomBgEnabled) mindMapBackgroundColor else if (!showCodeEditor && isCustomBgEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (isGenerating) {
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
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            label = { Text("Mermaid.js Structure (Edit to change map)") }
                        )
                    } else {
                        MermaidWebView(
                            mermaidCode = editableContent,
                            theme = mindMapTheme,
                            customCss = if (isCustomBgEnabled) customCss else "",
                            customHtml = if (isCustomBgEnabled) customHtml else "",
                            onNodeClicked = { clickedText ->
                                searchNodeText = clickedText
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        Text("Enter a prompt to generate a Mind Map", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // --- CUSTOM BACKGROUND HTML/CSS DIALOG ---
    if (showCustomBgDialog) {
        var aiPrompt by remember { mutableStateOf("") }
        var isAiGenerating by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showCustomBgDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f).clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Custom Map Background", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Switch(
                            checked = isCustomBgEnabled,
                            onCheckedChange = { isCustomBgEnabled = it }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Ask AI Section
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF8E24AA))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ask AI to generate a pattern", style = MaterialTheme.typography.titleSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = aiPrompt,
                                        onValueChange = { aiPrompt = it },
                                        placeholder = { Text("e.g., A futuristic blue cyber grid...") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    if (isAiGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        IconButton(
                                            onClick = {
                                                // Simulated AI Generation (You can replace this with actual ViewModel API call later)
                                                scope.launch {
                                                    isAiGenerating = true
                                                    delay(1500) // Fake loading time
                                                    customCss = ".container {\n  width: 100%;\n  height: 100%;\n  background-color: #0b0f19;\n  background-image: \n    radial-gradient(circle at 50% 50%, #1a233a 0%, #0b0f19 100%), \n    linear-gradient(0deg, rgba(0, 255, 128, 0.1) 1px, transparent 1px), \n    linear-gradient(90deg, rgba(0, 255, 128, 0.1) 1px, transparent 1px);\n  background-size: 100% 100%, 40px 40px, 40px 40px;\n}"
                                                    customHtml = "<div class=\"container\"></div>"
                                                    isCustomBgEnabled = true
                                                    isAiGenerating = false
                                                    aiPrompt = ""
                                                }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Generate")
                                        }
                                    }
                                }
                            }
                        }

                        // 1-Tap Presets
                        Text("1-Tap Presets:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Button(onClick = {
                                customCss = ".container {\n  width: 100%;\n  height: 100%;\n  --color: rgba(114, 114, 114, 0.3);\n  background-color: #191a1a;\n  background-image: linear-gradient(0deg, transparent 24%, var(--color) 25%, var(--color) 26%, transparent 27%,transparent 74%, var(--color) 75%, var(--color) 76%, transparent 77%,transparent),\n      linear-gradient(90deg, transparent 24%, var(--color) 25%, var(--color) 26%, transparent 27%,transparent 74%, var(--color) 75%, var(--color) 76%, transparent 77%,transparent);\n  background-size: 55px 55px;\n}"
                                customHtml = "<div class=\"container\"></div>"
                                isCustomBgEnabled = true
                            }) { Text("Grid Line") }

                            Button(onClick = {
                                customCss = ".container {\n  width: 100%;\n  height: 100%;\n  background: rgba(29, 31, 32, 0.904)\n    radial-gradient(rgba(255, 255, 255, 0.712) 10%, transparent 1%);\n  background-size: 11px 11px;\n}"
                                customHtml = "<div class=\"container\"></div>"
                                isCustomBgEnabled = true
                            }) { Text("Dots") }

                            Button(onClick = {
                                customCss = ""
                                customHtml = ""
                                isCustomBgEnabled = false
                            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Clear") }
                        }

                        // Manual Code Editors
                        OutlinedTextField(
                            value = customCss,
                            onValueChange = { customCss = it },
                            label = { Text("CSS (e.g. .container { ... })") },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        OutlinedTextField(
                            value = customHtml,
                            onValueChange = { customHtml = it },
                            label = { Text("HTML (e.g. <div class=\"container\"></div>)") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    // Close Button
                    Button(
                        onClick = { showCustomBgDialog = false },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Save & Close")
                    }
                }
            }
        }
    }

    // --- "KANTA PEMBESAR" GOOGLE SEARCH DIALOG ---
    if (searchNodeText != null) {
        Dialog(
            onDismissRequest = { searchNodeText = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f).clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(searchNodeText ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                        }
                        IconButton(onClick = { searchNodeText = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean { return false }
                                }
                            }
                        },
                        update = { webView ->
                            webView.loadUrl("https://www.google.com/search?q=${Uri.encode(searchNodeText)}")
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
    mermaidCode: String,
    theme: String,
    customCss: String = "",
    customHtml: String = "",
    onNodeClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=10.0, user-scalable=yes">
            <script type="module">
                import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
                mermaid.initialize({ startOnLoad: true, theme: '$theme', securityLevel: 'loose' });
                
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
                }
                /* Custom Injected Background CSS */
                $customCss
                
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
            <div id="custom-bg-wrapper" style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: -1; pointer-events: none;">
                $customHtml
            </div>
            
            <div class="mermaid">
                $mermaidCode
            </div>
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
            }
        },
        update = { webView ->
            val encodedHtml = android.util.Base64.encodeToString(html.toByteArray(), android.util.Base64.NO_PADDING)
            webView.loadData(encodedHtml, "text/html", "base64")
        },
        modifier = modifier
    )
}