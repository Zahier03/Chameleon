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

    // States for Colors, Themes, Search Dialog, Folding Input, and Menu
    var mindMapBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var mindMapTheme by remember { mutableStateOf("default") }
    var searchNodeText by remember { mutableStateOf<String?>(null) }
    var isInputExpanded by remember { mutableStateOf(true) }
    var isMenuExpanded by remember { mutableStateOf(false) } // Controls the top right menu

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isApiModel = currentModel?.isApiModel == true

    // Keep editable content synced with viewmodel generation
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
                            // Hamburger / More Options Icon
                            IconButton(onClick = { isMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            // The Dropdown Menu
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false }
                            ) {
                                // 1. Toggle Code Editor
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

                                // 2. Share to Mermaid Live
                                DropdownMenuItem(
                                    text = { Text("Share Link (Mermaid Live)") },
                                    onClick = {
                                        isMenuExpanded = false
                                        try {
                                            // Format the JSON required by Mermaid Live Editor
                                            val jsonString = JSONObject().apply {
                                                put("code", editableContent)
                                                put("mermaid", "{\n  \"theme\": \"$mindMapTheme\"\n}")
                                                put("autoSync", true)
                                                put("updateDiagram", false)
                                            }.toString()

                                            // Encode string to URL-safe Base64
                                            val base64 = android.util.Base64.encodeToString(
                                                jsonString.toByteArray(Charsets.UTF_8),
                                                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                                            )

                                            // Construct the final URL
                                            val shareUrl = "https://mermaid.live/edit#base64:$base64"

                                            // Launch Android Share Sheet
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Check out my mind map!\n\n$shareUrl")
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
            // Floating button to bring back the input area when it's folded
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
            // Animated folding for the input area
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
                        // Header with close button to manually fold the input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Create or update map",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
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

                        // Attachments preview
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
                                    isInputExpanded = false // Fold input away on generate
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

            // Customization Options (Styles & Backgrounds)
            AnimatedVisibility(visible = editableContent.isNotBlank() && !showCodeEditor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Theme (Nodes/Text Color) Picker
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Map Style:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp))
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

                    // Background Color Picker
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Background:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp))
                        backgroundColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else color)
                                    .border(
                                        width = 2.dp,
                                        color = if (mindMapBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { mindMapBackgroundColor = color }
                            ) {
                                if (color == Color.Transparent) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
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
                    .background(if (!showCodeEditor) mindMapBackgroundColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            label = { Text("Mermaid.js Structure (Edit to change map)") }
                        )
                    } else {
                        MermaidWebView(
                            mermaidCode = editableContent,
                            theme = mindMapTheme,
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
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            "Enter a prompt to generate a Mind Map",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // "Kanta Pembesar" - Google Search Overlay Dialog
    if (searchNodeText != null) {
        Dialog(
            onDismissRequest = { searchNodeText = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Dialog Header
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
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { searchNodeText = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    // Google Search WebView
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
    mermaidCode: String,
    theme: String,
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