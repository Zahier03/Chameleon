package com.sotech.chameleon.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sotech.chameleon.data.ImportedModel
import com.sotech.chameleon.ui.MainViewModel

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

    val context = LocalContext.current
    val isApiModel = currentModel?.isApiModel == true

    // Keep editable content synced with viewmodel generation
    LaunchedEffect(mindMapContent) {
        editableContent = mindMapContent
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImages = uris
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPdf = uri
    }

    Scaffold(
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
                        TextButton(onClick = { showCodeEditor = !showCodeEditor }) {
                            Text(if (showCodeEditor) "Show Map" else "Edit Structure")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color(0xFF8E24AA)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Display Area (Visual Mind Map or Code Editor)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (isGenerating) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8E24AA))
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
                            tint = Color(0xFFCE93D8)
                        )
                        Text(
                            "Enter a prompt to generate a Mind Map",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attachments preview
                AnimatedVisibility(visible = selectedImages.isNotEmpty() || selectedPdf != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedPdf?.let {
                            AssistChip(
                                onClick = { selectedPdf = null },
                                label = { Text("PDF Attached") },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove PDF", modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = Color.Red)
                            )
                        }
                        if (selectedImages.isNotEmpty()) {
                            AssistChip(
                                onClick = { selectedImages = emptyList() },
                                label = { Text("${selectedImages.size} Image(s)") },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove Images", modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = Color.Blue)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Image Picker
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Attach Image", tint = Color(0xFF8E24AA))
                    }

                    // PDF Picker (Disabled if not API Model)
                    IconButton(
                        onClick = { pdfPicker.launch("application/pdf") },
                        enabled = isApiModel
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Attach PDF",
                            tint = if (isApiModel) Color(0xFF8E24AA) else Color.Gray
                        )
                    }

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Generate a mind map about...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8E24AA),
                            focusedLabelColor = Color(0xFF8E24AA)
                        )
                    )

                    FloatingActionButton(
                        onClick = {
                            val bitmaps = selectedImages.mapNotNull { uri ->
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            viewModel.generateMindMap(promptText, bitmaps, selectedPdf)
                            showCodeEditor = false
                        },
                        containerColor = Color(0xFF8E24AA),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Generate")
                    }
                }

                if (!isApiModel) {
                    Text(
                        text = "PDF uploads are disabled. Please use a Gemini API model for PDF analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MermaidWebView(mermaidCode: String, modifier: Modifier = Modifier) {
    // Basic HTML wrapper to load and render Mermaid JS
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=yes">
            <script type="module">
                import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
                mermaid.initialize({ startOnLoad: true, theme: 'default', securityLevel: 'loose' });
            </script>
            <style>
                body { margin: 0; padding: 16px; display: flex; justify-content: center; align-items: flex-start; background-color: transparent; }
                .mermaid { width: 100%; overflow: auto; text-align: center; }
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
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
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