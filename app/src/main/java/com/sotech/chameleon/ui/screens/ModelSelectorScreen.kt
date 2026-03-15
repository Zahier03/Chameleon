package com.sotech.chameleon.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sotech.chameleon.R
import com.sotech.chameleon.data.GeminiModelType
import com.sotech.chameleon.data.GeminiModels
import com.sotech.chameleon.data.ImportedModel
import com.sotech.chameleon.data.StatsSettings
import com.sotech.chameleon.data.MessageStatsSummary
import com.sotech.chameleon.ui.dialogs.StatsSettingsDialog
import com.sotech.chameleon.ui.components.StatsSummaryView
import com.sotech.chameleon.ui.theme.customColors
import com.sotech.chameleon.utils.formatFileSize
import com.sotech.chameleon.utils.formatDate
import com.sotech.chameleon.utils.formatSpeed
import com.sotech.chameleon.utils.formatNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorScreen(
    models: List<ImportedModel>,
    currentModel: ImportedModel?,
    isImporting: Boolean,
    importProgress: Float,
    importStatus: String,
    statsSettings: StatsSettings,
    statsSummary: MessageStatsSummary,
    onModelSelect: (ImportedModel) -> Unit,
    onModelDelete: (ImportedModel) -> Unit,
    onModelImport: (uri: android.net.Uri, displayName: String, supportImage: Boolean, supportAudio: Boolean) -> Unit,
    onModelConfigUpdate: (ImportedModel) -> Unit,
    onGeminiModelAdd: (displayName: String, apiKey: String, modelCode: String, modelType: GeminiModelType) -> Unit,
    onStatsSettingsUpdate: (StatsSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showGeminiDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var modelDisplayName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<ImportedModel?>(null) }
    var showConfigDialog by remember { mutableStateOf<ImportedModel?>(null) }
    var showStatsSettings by remember { mutableStateOf(false) }
    var showStatsSummary by remember { mutableStateOf(false) }
    var showModelTypeSelector by remember { mutableStateOf(false) }
    var animationStarted by remember { mutableStateOf(false) }

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
    val context = LocalContext.current

    val barberChopFont = remember {
        try {
            FontFamily(Font(R.font.barberchop, FontWeight.Bold))
        } catch (e: Exception) {
            FontFamily.Default
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        animationStarted = true
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            showImportDialog = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MODELS",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = barberChopFont,
                            fontSize = if (isLandscape) (24 * scaleFactor).sp else (40 * scaleFactor).sp,
                            lineHeight = if (isLandscape) (30 * scaleFactor).sp else (50 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (3 * scaleFactor).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showModelTypeSelector = true },
                        enabled = !isImporting
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Model")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (models.isEmpty()) {
                EmptyModelState(
                    onImportClick = { showModelTypeSelector = true },
                    animationStarted = animationStarted,
                    barberChopFont = barberChopFont,
                    scaleFactor = scaleFactor,
                    isLandscape = isLandscape
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (isLandscape) (16 * scaleFactor).dp else (24 * scaleFactor).dp,
                        end = if (isLandscape) (16 * scaleFactor).dp else (24 * scaleFactor).dp,
                        top = (16 * scaleFactor).dp,
                        bottom = if (isLandscape) (60 * scaleFactor).dp else (80 * scaleFactor).dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) (16 * scaleFactor).dp else (24 * scaleFactor).dp)
                ) {
                    if (statsSettings.showStats) {
                        item {
                            AnimatedSection(
                                visible = animationStarted,
                                delayMillis = 0L
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape((16 * scaleFactor).dp))
                                        .background(Color(0xFF1565C0))
                                ) {
                                    Text(
                                        text = "STATS",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = barberChopFont,
                                            fontSize = if (isLandscape) (40 * scaleFactor).sp else (80 * scaleFactor).sp,
                                            lineHeight = if (isLandscape) (50 * scaleFactor).sp else (90 * scaleFactor).sp
                                        ),
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF42A5F5),
                                        modifier = Modifier.align(Alignment.Center)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding((20 * scaleFactor).dp),
                                        verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "PERFORMANCE",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontFamily = barberChopFont,
                                                    fontSize = if (isLandscape) (18 * scaleFactor).sp else (22 * scaleFactor).sp,
                                                    lineHeight = if (isLandscape) (24 * scaleFactor).sp else (28 * scaleFactor).sp
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                            ) {
                                                IconButton(
                                                    onClick = { showStatsSummary = true },
                                                    modifier = Modifier.size((32 * scaleFactor).dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Analytics,
                                                        contentDescription = "View Details",
                                                        modifier = Modifier.size((20 * scaleFactor).dp),
                                                        tint = Color.White
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { showStatsSettings = true },
                                                    modifier = Modifier.size((32 * scaleFactor).dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = "Settings",
                                                        modifier = Modifier.size((20 * scaleFactor).dp),
                                                        tint = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFF42A5F5).copy(alpha = 0.3f))

                                        if (statsSummary.totalMessages > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                StatItem(
                                                    icon = Icons.Default.Speed,
                                                    value = formatSpeed(statsSummary.avgDecodeSpeed),
                                                    label = "tok/s",
                                                    compact = true,
                                                    font = barberChopFont,
                                                    iconColor = Color(0xFF90CAF9),
                                                    textColor = Color.White,
                                                    labelColor = Color(0xFF90CAF9),
                                                    scaleFactor = scaleFactor
                                                )
                                                StatItem(
                                                    icon = Icons.Default.Message,
                                                    value = statsSummary.totalMessages.toString(),
                                                    label = "msgs",
                                                    compact = true,
                                                    font = barberChopFont,
                                                    iconColor = Color(0xFF90CAF9),
                                                    textColor = Color.White,
                                                    labelColor = Color(0xFF90CAF9),
                                                    scaleFactor = scaleFactor
                                                )
                                                StatItem(
                                                    icon = Icons.Default.Numbers,
                                                    value = formatNumber(statsSummary.totalTokens),
                                                    label = "tokens",
                                                    compact = true,
                                                    font = barberChopFont,
                                                    iconColor = Color(0xFF90CAF9),
                                                    textColor = Color.White,
                                                    labelColor = Color(0xFF90CAF9),
                                                    scaleFactor = scaleFactor
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "No statistics yet",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize * scaleFactor),
                                                color = Color(0xFF90CAF9),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        AnimatedSection(
                            visible = animationStarted,
                            delayMillis = 100L
                        ) {
                            Text(
                                text = "YOUR MODELS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = if (isLandscape) (18 * scaleFactor).sp else (22 * scaleFactor).sp,
                                    lineHeight = if (isLandscape) (24 * scaleFactor).sp else (28 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    items(models.size) { index ->
                        val model = models[index]
                        AnimatedSection(
                            visible = animationStarted,
                            delayMillis = 200L + (index * 50L)
                        ) {
                            RetroModelCard(
                                model = model,
                                isSelected = currentModel?.fileName == model.fileName,
                                onSelect = { onModelSelect(model) },
                                onDelete = { showDeleteDialog = model },
                                onConfig = { showConfigDialog = model },
                                barberChopFont = barberChopFont,
                                scaleFactor = scaleFactor,
                                isLandscape = isLandscape
                            )
                        }
                    }
                }
            }

            if (isImporting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape((16 * scaleFactor).dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = (8 * scaleFactor).dp)
                    ) {
                        Column(
                            modifier = Modifier.padding((32 * scaleFactor).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((20 * scaleFactor).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((48 * scaleFactor).dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size((24 * scaleFactor).dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                            ) {
                                Text(
                                    "IMPORTING MODEL",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = (12 * scaleFactor).sp,
                                        letterSpacing = (1.5 * scaleFactor).sp,
                                        fontFamily = barberChopFont
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                AnimatedVisibility(visible = importStatus.isNotEmpty()) {
                                    Text(
                                        importStatus,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * scaleFactor),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                            ) {
                                if (importProgress > 0f) {
                                    LinearProgressIndicator(
                                        progress = { importProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((6 * scaleFactor).dp)
                                            .clip(RoundedCornerShape((3 * scaleFactor).dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        "${(importProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize * scaleFactor,
                                            fontFamily = barberChopFont
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size((32 * scaleFactor).dp),
                                        strokeWidth = (3 * scaleFactor).dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                "Large models may take a while to import.\nThe app remains responsive.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * scaleFactor).sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = (16 * scaleFactor).sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModelTypeSelector) {
        ModelTypeDialog(
            onDismiss = { showModelTypeSelector = false },
            onLocalModel = {
                showModelTypeSelector = false
                launcher.launch("*/*")
            },
            onGeminiModel = {
                showModelTypeSelector = false
                showGeminiDialog = true
            },
            scaleFactor = scaleFactor
        )
    }

    if (showStatsSettings) {
        StatsSettingsDialog(
            currentSettings = statsSettings,
            onDismiss = { showStatsSettings = false },
            onSave = { newSettings ->
                onStatsSettingsUpdate(newSettings)
                showStatsSettings = false
            }
        )
    }

    if (showStatsSummary) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            StatsSummaryView(
                summary = statsSummary,
                onClose = { showStatsSummary = false },
                modifier = Modifier.padding((16 * scaleFactor).dp)
            )
        }
    }

    if (showImportDialog && selectedUri != null) {
        var supportImage by remember { mutableStateOf(false) }
        var supportAudio by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                modelDisplayName = ""
                selectedUri = null
            },
            title = {
                Text(
                    "Import Model",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                ) {
                    Text(
                        "Configure your model:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * scaleFactor
                        )
                    )

                    OutlinedTextField(
                        value = modelDisplayName,
                        onValueChange = { modelDisplayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        placeholder = { Text("My Model") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = (8 * scaleFactor).dp)
                    )

                    Text(
                        "Model Capabilities:",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize * scaleFactor
                        ),
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding((12 * scaleFactor).dp),
                            verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size((18 * scaleFactor).dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Enable these if your model supports multimodal input",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (11 * scaleFactor).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { supportImage = !supportImage }
                            .clip(RoundedCornerShape((8 * scaleFactor).dp))
                            .background(
                                if (supportImage)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding((12 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size((24 * scaleFactor).dp),
                                tint = if (supportImage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    "Image Support",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize * scaleFactor
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "For vision models (e.g., Gemma 3n)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (11 * scaleFactor).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(
                            checked = supportImage,
                            onCheckedChange = { supportImage = it }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { supportAudio = !supportAudio }
                            .clip(RoundedCornerShape((8 * scaleFactor).dp))
                            .background(
                                if (supportAudio)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding((12 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                        ) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size((24 * scaleFactor).dp),
                                tint = if (supportAudio)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    "Audio Support",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize * scaleFactor
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "For audio-capable models",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (11 * scaleFactor).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(
                            checked = supportAudio,
                            onCheckedChange = { supportAudio = it }
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        selectedUri?.let { uri ->
                            onModelImport(uri, modelDisplayName, supportImage, supportAudio)
                        }
                        showImportDialog = false
                        modelDisplayName = ""
                        selectedUri = null
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        modelDisplayName = ""
                        selectedUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGeminiDialog) {
        GeminiModelDialog(
            onDismiss = { showGeminiDialog = false },
            onConfirm = { displayName, apiKey, modelCode, modelType ->
                onGeminiModelAdd(displayName, apiKey, modelCode, modelType)
                showGeminiDialog = false
            },
            scaleFactor = scaleFactor
        )
    }

    showDeleteDialog?.let { model ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Model") },
            text = { Text("Are you sure you want to delete \"${model.displayName}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onModelDelete(model)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showConfigDialog?.let { model ->
        if (model.isApiModel) {
            GeminiModelConfigDialog(
                model = model,
                onDismiss = { showConfigDialog = null },
                onSave = { updatedModel ->
                    onModelConfigUpdate(updatedModel)
                    showConfigDialog = null
                },
                scaleFactor = scaleFactor
            )
        } else {
            ModelConfigDialog(
                model = model,
                onDismiss = { showConfigDialog = null },
                onSave = { updatedModel ->
                    onModelConfigUpdate(updatedModel)
                    showConfigDialog = null
                },
                scaleFactor = scaleFactor
            )
        }
    }
}

@Composable
fun ModelTypeDialog(
    onDismiss: () -> Unit,
    onLocalModel: () -> Unit,
    onGeminiModel: () -> Unit,
    scaleFactor: Float
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Model",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
            ) {
                Card(
                    onClick = onLocalModel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding((16 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size((32 * scaleFactor).dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Import .tflite or .task file",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Card(
                    onClick = onGeminiModel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding((16 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size((32 * scaleFactor).dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini API",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Use Google Gemini models",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GeminiModelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, GeminiModelType) -> Unit,
    scaleFactor: Float
) {
    var displayName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var selectedModelIndex by remember { mutableStateOf(0) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val selectedGeminiModel = GeminiModels.models[selectedModelIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Gemini Model",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding((12 * scaleFactor).dp),
                            verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size((20 * scaleFactor).dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Get API Key",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Visit Google AI Studio to get your free API key:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open AI Studio")
                                Spacer(modifier = Modifier.width((4 * scaleFactor).dp))
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size((16 * scaleFactor).dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("My Gemini Model") }
                    )
                }

                item {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            validationError = null
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("AIza...") },
                        isError = validationError != null,
                        supportingText = validationError?.let { { Text(it) } }
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                    ) {
                        Text(
                            text = "Select Model",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )

                        GeminiModels.models.forEachIndexed { index, model ->
                            Card(
                                onClick = { selectedModelIndex = index },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedModelIndex == index) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding((12 * scaleFactor).dp),
                                    verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = model.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (selectedModelIndex == index) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size((20 * scaleFactor).dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = model.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Code: ${model.code}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Supports: ${model.supportedInputs.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (displayName.isBlank() || apiKey.isBlank()) {
                        validationError = "Please fill all fields"
                    } else {
                        onConfirm(
                            displayName.ifBlank { selectedGeminiModel.name },
                            apiKey,
                            selectedGeminiModel.code,
                            selectedGeminiModel.type
                        )
                    }
                },
                enabled = !isValidating
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size((16 * scaleFactor).dp),
                        strokeWidth = (2 * scaleFactor).dp
                    )
                    Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                }
                Text("Add Model")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isValidating) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GeminiModelConfigDialog(
    model: ImportedModel,
    onDismiss: () -> Unit,
    onSave: (ImportedModel) -> Unit,
    scaleFactor: Float
) {
    var apiKey by remember { mutableStateOf(model.apiKey) }
    var temperature by remember { mutableStateOf(model.temperature.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "GEMINI MODEL CONFIG",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = (12 * scaleFactor).sp,
                    letterSpacing = (1.5 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature") },
                    singleLine = true,
                    supportingText = { Text("0.0 - 2.0", style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize * scaleFactor)) }
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding((12 * scaleFactor).dp),
                        verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                    ) {
                        Text(
                            text = "Model: ${model.modelCode}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type: ${model.modelType.name.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val updatedModel = model.copy(
                        apiKey = apiKey,
                        temperature = temperature.toFloatOrNull() ?: model.temperature
                    )
                    onSave(updatedModel)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RetroModelCard(
    model: ImportedModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onConfig: () -> Unit,
    barberChopFont: FontFamily,
    scaleFactor: Float,
    isLandscape: Boolean
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFFFFA726)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape((16 * scaleFactor).dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) (16 * scaleFactor).dp else (20 * scaleFactor).dp),
            verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = barberChopFont,
                                fontSize = if (isLandscape) (16 * scaleFactor).sp else (18 * scaleFactor).sp,
                                lineHeight = if (isLandscape) (20 * scaleFactor).sp else (24 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Surface(
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape((4 * scaleFactor).dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    modifier = Modifier.padding(horizontal = (6 * scaleFactor).dp, vertical = (2 * scaleFactor).dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = (10 * scaleFactor).sp,
                                        letterSpacing = (0.5 * scaleFactor).sp,
                                        fontFamily = barberChopFont
                                    ),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (model.isApiModel) {
                            Surface(
                                color = if (isSelected) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape((4 * scaleFactor).dp)
                            ) {
                                Text(
                                    text = "API",
                                    modifier = Modifier.padding(horizontal = (6 * scaleFactor).dp, vertical = (2 * scaleFactor).dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = (10 * scaleFactor).sp,
                                        letterSpacing = (0.5 * scaleFactor).sp,
                                        fontFamily = barberChopFont
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = if (model.isApiModel) model.modelCode else model.fileName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * scaleFactor).sp),
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                ) {
                    IconButton(
                        onClick = onConfig,
                        modifier = Modifier.size((32 * scaleFactor).dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configure",
                            modifier = Modifier.size((18 * scaleFactor).dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size((32 * scaleFactor).dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size((18 * scaleFactor).dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(
                color = if (isSelected) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            if (model.isApiModel) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ModelInfoItem(
                        icon = Icons.Default.Cloud,
                        text = "Gemini API",
                        isSelected = isSelected,
                        scaleFactor = scaleFactor
                    )
                    ModelInfoItem(
                        icon = Icons.Default.Category,
                        text = model.modelType.name.replace("_", " "),
                        isSelected = isSelected,
                        scaleFactor = scaleFactor
                    )
                    ModelInfoItem(
                        icon = Icons.Default.TextFields,
                        text = "${model.maxTokens}",
                        isSelected = isSelected,
                        scaleFactor = scaleFactor
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ModelInfoItem(
                        icon = Icons.Default.Memory,
                        text = formatFileSize(model.fileSize),
                        isSelected = isSelected,
                        scaleFactor = scaleFactor
                    )
                    ModelInfoItem(
                        icon = if (model.useGpu) Icons.Default.Speed else Icons.Default.Computer,
                        text = if (model.useGpu) "GPU" else "CPU",
                        isSelected = isSelected,
                        scaleFactor = scaleFactor
                    )
                    ModelInfoItem(
                        icon = Icons.Default.TextFields,
                        text = "${model.maxTokens}",
                        isSelected = isSelected,
                        scaleFactor = scaleFactor
                    )
                }
            }

            Text(
                text = "Added ${formatDate(model.timestamp)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * scaleFactor).sp),
                color = if (isSelected) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ModelInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean,
    scaleFactor: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size((16 * scaleFactor).dp),
            tint = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * scaleFactor).sp),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyModelState(
    onImportClick: () -> Unit,
    animationStarted: Boolean,
    barberChopFont: FontFamily,
    scaleFactor: Float,
    isLandscape: Boolean
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "empty_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = animatedAlpha }
            .padding((32 * scaleFactor).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size((80 * scaleFactor).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size((40 * scaleFactor).dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height((24 * scaleFactor).dp))
        Text(
            "NO MODELS",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = barberChopFont,
                fontSize = if (isLandscape) (24 * scaleFactor).sp else (32 * scaleFactor).sp,
                lineHeight = if (isLandscape) (30 * scaleFactor).sp else (36 * scaleFactor).sp
            ),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
        Text(
            "Import a model to get started",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * scaleFactor),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height((32 * scaleFactor).dp))
        FilledTonalButton(
            onClick = onImportClick,
            shape = RoundedCornerShape((12 * scaleFactor).dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size((18 * scaleFactor).dp)
            )
            Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
            Text(
                "Add Model",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = barberChopFont,
                    fontSize = MaterialTheme.typography.labelLarge.fontSize * scaleFactor
                )
            )
        }
    }
}

@Composable
fun ModelConfigDialog(
    model: ImportedModel,
    onDismiss: () -> Unit,
    onSave: (ImportedModel) -> Unit,
    scaleFactor: Float
) {
    var maxTokens by remember { mutableStateOf(model.maxTokens.toString()) }
    var topK by remember { mutableStateOf(model.topK.toString()) }
    var topP by remember { mutableStateOf(model.topP.toString()) }
    var temperature by remember { mutableStateOf(model.temperature.toString()) }
    var useGpu by remember { mutableStateOf(model.useGpu) }
    var supportImage by remember { mutableStateOf(model.supportImage) }
    var supportAudio by remember { mutableStateOf(model.supportAudio) }
    var showCustomTokens by remember { mutableStateOf(false) }

    val modelSizeMB = model.fileSize / 1024 / 1024
    val isLargeModel = modelSizeMB > 2048

    val tokenPresets = listOf(128, 256, 512, 1024, 2048, 4096, 8192, 16384)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "MODEL CONFIGURATION",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = (12 * scaleFactor).sp,
                    letterSpacing = (1.5 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
            ) {
                if (isLargeModel) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape((8 * scaleFactor).dp)
                        ) {
                            Column(modifier = Modifier.padding((12 * scaleFactor).dp)) {
                                Text(
                                    text = "Large model (${modelSizeMB}MB)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize * scaleFactor
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height((4 * scaleFactor).dp))
                                Text(
                                    text = "CPU mode recommended for stability",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * scaleFactor).sp)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                    ) {
                        Text(
                            text = "Max Tokens",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = MaterialTheme.typography.labelMedium.fontSize * scaleFactor
                            ),
                            fontWeight = FontWeight.Medium
                        )

                        if (!showCustomTokens) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(tokenPresets.size) { index ->
                                    val preset = tokenPresets[index]
                                    FilterChip(
                                        selected = maxTokens == preset.toString(),
                                        onClick = { maxTokens = preset.toString() },
                                        label = {
                                            Text(
                                                preset.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * scaleFactor
                                                )
                                            )
                                        },
                                        modifier = Modifier.height((32 * scaleFactor).dp)
                                    )
                                }
                            }
                            TextButton(
                                onClick = { showCustomTokens = true },
                                modifier = Modifier.padding(top = (4 * scaleFactor).dp)
                            ) {
                                Text(
                                    "Custom value",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize * scaleFactor
                                    )
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = maxTokens,
                                onValueChange = { maxTokens = it },
                                label = { Text("Custom Max Tokens") },
                                singleLine = true,
                                supportingText = {
                                    Text(
                                        "Any value (e.g., 32768)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = MaterialTheme.typography.bodySmall.fontSize * scaleFactor
                                        )
                                    )
                                },
                                trailingIcon = {
                                    TextButton(onClick = { showCustomTokens = false }) {
                                        Text(
                                            "Presets",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = MaterialTheme.typography.labelSmall.fontSize * scaleFactor
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        OutlinedTextField(
                            value = topK,
                            onValueChange = { topK = it },
                            label = { Text("Top K") },
                            singleLine = true,
                            supportingText = {
                                Text(
                                    "20-100",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * scaleFactor
                                    )
                                )
                            }
                        )

                        OutlinedTextField(
                            value = topP,
                            onValueChange = { topP = it },
                            label = { Text("Top P") },
                            singleLine = true,
                            supportingText = {
                                Text(
                                    "0.9-0.95",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * scaleFactor
                                    )
                                )
                            }
                        )

                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temperature") },
                            singleLine = true,
                            supportingText = {
                                Text(
                                    "Higher = more creative",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * scaleFactor
                                    )
                                )
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = "Hardware & Capabilities",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize * scaleFactor
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { useGpu = !useGpu }
                            .clip(RoundedCornerShape((8 * scaleFactor).dp))
                            .background(
                                if (useGpu)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding((12 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size((24 * scaleFactor).dp)
                            )
                            Column {
                                Text(
                                    "Use GPU",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize * scaleFactor
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Faster inference, higher power usage",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (11 * scaleFactor).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(
                            checked = useGpu,
                            onCheckedChange = { useGpu = it }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { supportImage = !supportImage }
                            .clip(RoundedCornerShape((8 * scaleFactor).dp))
                            .background(
                                if (supportImage)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding((12 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size((24 * scaleFactor).dp)
                            )
                            Column {
                                Text(
                                    "Image Support",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize * scaleFactor
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Vision models like Gemma 3n",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (11 * scaleFactor).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(
                            checked = supportImage,
                            onCheckedChange = { supportImage = it }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { supportAudio = !supportAudio }
                            .clip(RoundedCornerShape((8 * scaleFactor).dp))
                            .background(
                                if (supportAudio)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding((12 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                        ) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size((24 * scaleFactor).dp)
                            )
                            Column {
                                Text(
                                    "Audio Support",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize * scaleFactor
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Audio-capable models",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (11 * scaleFactor).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(
                            checked = supportAudio,
                            onCheckedChange = { supportAudio = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val updatedModel = model.copy(
                        maxTokens = maxTokens.toIntOrNull() ?: model.maxTokens,
                        topK = topK.toIntOrNull() ?: model.topK,
                        topP = topP.toFloatOrNull() ?: model.topP,
                        temperature = temperature.toFloatOrNull() ?: model.temperature,
                        useGpu = useGpu,
                        supportImage = supportImage,
                        supportAudio = supportAudio
                    )
                    onSave(updatedModel)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AnimatedSection(
    visible: Boolean,
    delayMillis: Long,
    content: @Composable () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = delayMillis.toInt(), easing = FastOutSlowInEasing),
        label = "section_alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(600, delayMillis = delayMillis.toInt(), easing = FastOutSlowInEasing),
        label = "section_offset"
    )
    Box(modifier = Modifier.graphicsLayer { alpha = animatedAlpha; translationY = animatedOffset }) { content() }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    compact: Boolean = false,
    font: FontFamily,
    iconColor: Color = Color.White,
    textColor: Color = Color.White,
    labelColor: Color = Color.White,
    scaleFactor: Float
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((3 * scaleFactor).dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy((3 * scaleFactor).dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) (12 * scaleFactor).dp else (16 * scaleFactor).dp), tint = iconColor)
            Text(text = value, style = if (compact) MaterialTheme.typography.bodyMedium.copy(fontFamily = font, fontSize = (12 * scaleFactor).sp) else MaterialTheme.typography.titleSmall.copy(fontFamily = font, fontSize = (14 * scaleFactor).sp), fontWeight = FontWeight.Bold, color = textColor)
        }
        Text(text = label, style = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = (10 * scaleFactor).sp) else MaterialTheme.typography.bodySmall.copy(fontSize = (11 * scaleFactor).sp), color = labelColor)
    }
}