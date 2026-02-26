package com.sotech.chameleon.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.sotech.chameleon.data.ChatConversation
import com.sotech.chameleon.data.ChatMessage
import com.sotech.chameleon.data.ImportedModel
import com.sotech.chameleon.data.ModelStatus
import com.sotech.chameleon.utils.formatDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    conversations: List<ChatConversation>,
    currentConversation: ChatConversation?,
    currentModel: ImportedModel?,
    modelStatus: ModelStatus,
    modelError: String?,
    initializationProgress: String?,
    isGenerating: Boolean,
    currentResponse: String,
    currentInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String, List<Bitmap>) -> Unit,
    onStopGeneration: () -> Unit,
    onClearChat: () -> Unit,
    onOpenModelSelector: () -> Unit,
    onCreateNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDeleteConversations: (List<String>) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onPinConversation: (String, Boolean) -> Unit,
    onEditMessage: ((Long, String) -> Unit)? = null,
    onRegenerateFrom: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    var userHasScrolledUp by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val bitmaps = uris.mapNotNull { uri ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                null
            }
        }
        selectedImages = bitmaps
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            userHasScrolledUp = lastVisibleIndex < totalItems - 1
        }
    }

    LaunchedEffect(messages.size, isGenerating) {
        if (!userHasScrolledUp && messages.isNotEmpty()) {
            delay(100)
            scope.launch {
                listState.animateScrollToItem(
                    index = if (isGenerating) messages.size else messages.size - 1
                )
            }
        }
    }

    LaunchedEffect(isGenerating) {
        if (!isGenerating && !userHasScrolledUp && messages.isNotEmpty()) {
            delay(100)
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = conversations,
                currentConversationId = currentConversation?.id,
                onCreateNew = {
                    scope.launch {
                        onCreateNewConversation()
                        drawerState.close()
                    }
                },
                onSelectConversation = { id ->
                    scope.launch {
                        onSelectConversation(id)
                        drawerState.close()
                    }
                },
                onDeleteConversation = onDeleteConversation,
                onDeleteConversations = onDeleteConversations,
                onRenameConversation = onRenameConversation,
                onPinConversation = onPinConversation
            )
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.clickable {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentConversation?.title ?: "New Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = "Open conversations",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            when (modelStatus) {
                                ModelStatus.INITIALIZING -> {
                                    Text(
                                        text = initializationProgress ?: "Initializing...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                ModelStatus.ERROR -> {
                                    Text(
                                        text = modelError ?: "Error",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                ModelStatus.READY -> {
                                    Text(
                                        text = if (isGenerating) "Generating..." else currentModel?.displayName ?: "Ready",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isGenerating)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                else -> {}
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open conversations"
                            )
                        }
                    },
                    actions = {
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearChat,
                                enabled = !isGenerating
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Chat"
                                )
                            }
                        }
                        IconButton(onClick = onOpenModelSelector) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Model Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        if (selectedImages.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedImages) { bitmap ->
                                    Box {
                                        AsyncImage(
                                            model = bitmap,
                                            contentDescription = "Selected image",
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = {
                                                selectedImages = selectedImages.filter { it != bitmap }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                enabled = currentModel != null &&
                                        modelStatus == ModelStatus.READY &&
                                        !isGenerating,
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (selectedImages.isNotEmpty()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Add Image",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            OutlinedTextField(
                                value = currentInput,
                                onValueChange = onInputChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                placeholder = {
                                    Text(
                                        "Type a message...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                enabled = currentModel != null &&
                                        modelStatus == ModelStatus.READY &&
                                        !isGenerating,
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = if (currentInput.isNotBlank()) ImeAction.Send else ImeAction.Default
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (currentInput.isNotBlank() || selectedImages.isNotEmpty()) {
                                            onSendMessage(currentInput, selectedImages)
                                            onInputChange("")
                                            selectedImages = emptyList()
                                            keyboardController?.hide()
                                            userHasScrolledUp = false
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            if (isGenerating) {
                                FilledIconButton(
                                    onClick = onStopGeneration,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        if (currentInput.isNotBlank() || selectedImages.isNotEmpty()) {
                                            onSendMessage(currentInput, selectedImages)
                                            onInputChange("")
                                            selectedImages = emptyList()
                                            keyboardController?.hide()
                                            userHasScrolledUp = false
                                        }
                                    },
                                    enabled = currentModel != null &&
                                            modelStatus == ModelStatus.READY &&
                                            (currentInput.isNotBlank() || selectedImages.isNotEmpty()),
                                    modifier = Modifier.size(48.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (currentModel != null &&
                                            modelStatus == ModelStatus.READY &&
                                            (currentInput.isNotBlank() || selectedImages.isNotEmpty())) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (messages.isEmpty() && !isGenerating) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Start a conversation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Type a message below to begin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        reverseLayout = false
                    ) {
                        items(
                            items = messages,
                            key = { message -> "${message.timestamp}_${message.isUser}" }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                onEdit = if (message.isUser && onEditMessage != null) {
                                    { editedText -> onEditMessage(message.timestamp, editedText) }
                                } else null,
                                onRegenerate = if (!message.isUser && onRegenerateFrom != null) {
                                    { onRegenerateFrom(message.timestamp) }
                                } else null,
                                isGenerationActive = isGenerating
                            )
                        }

                        if (isGenerating && currentResponse.isNotEmpty()) {
                            item(key = "generating_ai_response") {
                                MessageBubble(
                                    message = ChatMessage(
                                        content = currentResponse,
                                        isUser = false,
                                        timestamp = Long.MAX_VALUE
                                    ),
                                    isGenerating = true,
                                    isGenerationActive = true
                                )
                            }
                        }

                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = userHasScrolledUp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                    ) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    userHasScrolledUp = false
                                    listState.animateScrollToItem(
                                        if (isGenerating) messages.size else messages.size - 1
                                    )
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Scroll to bottom"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDrawer(
    conversations: List<ChatConversation>,
    currentConversationId: String?,
    onCreateNew: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDeleteConversations: (List<String>) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onPinConversation: (String, Boolean) -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Conversations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalIconButton(
                    onClick = onCreateNew,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New conversation",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                items(
                    items = conversations,
                    key = { it.id }
                ) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        isSelected = conversation.id == currentConversationId,
                        onSelect = { onSelectConversation(conversation.id) },
                        onDelete = { onDeleteConversation(conversation.id) },
                        onRename = { newTitle -> onRenameConversation(conversation.id, newTitle) },
                        onPin = { onPinConversation(conversation.id, !conversation.isPinned) }
                    )
                }
            }

            if (conversations.isNotEmpty()) {
                HorizontalDivider()
                TextButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Conversations")
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        var selectedIds by remember { mutableStateOf(conversations.map { it.id }.toSet()) }
        var selectAll by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete Conversations") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectAll = !selectAll
                                selectedIds = if (selectAll) conversations.map { it.id }.toSet() else emptySet()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = selectAll,
                            onCheckedChange = { checked ->
                                selectAll = checked
                                selectedIds = if (checked) conversations.map { it.id }.toSet() else emptySet()
                            }
                        )
                        Text("Select All", style = MaterialTheme.typography.bodyLarge)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(conversations) { conversation ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (conversation.id in selectedIds) {
                                            selectedIds - conversation.id
                                        } else {
                                            selectedIds + conversation.id
                                        }
                                        selectAll = selectedIds.size == conversations.size
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = conversation.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + conversation.id
                                        } else {
                                            selectedIds - conversation.id
                                        }
                                        selectAll = selectedIds.size == conversations.size
                                    }
                                )
                                Text(
                                    conversation.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    if (selectedIds.isNotEmpty()) {
                        Text(
                            "This will delete ${selectedIds.size} conversations and all their messages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedIds.isNotEmpty()) {
                            onDeleteConversations(selectedIds.toList())
                            showDeleteAllDialog = false
                        }
                    },
                    enabled = selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: ChatConversation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onPin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { showMenu = true }
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (conversation.isPinned) Icons.Default.PushPin else Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (conversation.messageCount > 0) {
                        Text(
                            text = "${conversation.messageCount} messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (conversation.isPinned) "Unpin" else "Pin") },
                    onClick = {
                        showMenu = false
                        onPin()
                    },
                    leadingIcon = {
                        Icon(
                            if (conversation.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showMenu = false
                        showRenameDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }

    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(conversation.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRename(newTitle)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isGenerating: Boolean = false,
    onEdit: ((String) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    isGenerationActive: Boolean = false
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(message.content) }
    var showContextMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            if (!isGenerationActive && !isGenerating) {
                                showContextMenu = true
                            }
                        },
                        enabled = !isGenerationActive && !isGenerating
                    ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (message.images.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(message.images) { imagePath ->
                                AsyncImage(
                                    model = imagePath,
                                    contentDescription = "Message image",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    if (message.isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        RichText(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Markdown(content = message.content)
                        }
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!message.isUser && message.stats != null && !isGenerating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Speed: ${String.format("%.1f", message.stats.decodeSpeed)} tok/s | " +
                                    "Time: ${String.format("%.1f", message.stats.totalLatency)}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    if (onEdit != null || onRegenerate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (onEdit != null) {
                                FilledTonalIconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.size(32.dp),
                                    enabled = !isGenerationActive && !isGenerating
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (onRegenerate != null) {
                                FilledTonalIconButton(
                                    onClick = onRegenerate,
                                    modifier = Modifier.size(32.dp),
                                    enabled = !isGenerationActive && !isGenerating
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Regenerate",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = DpOffset(0.dp, (-8).dp)
            ) {
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showContextMenu = false
                            showEditDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                }
                if (onRegenerate != null) {
                    DropdownMenuItem(
                        text = { Text("Regenerate") },
                        onClick = {
                            showContextMenu = false
                            onRegenerate()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        showContextMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("message", message.content)
                        clipboard.setPrimaryClip(clip)
                    },
                    leadingIcon = @Composable {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    }
                )
            }
        }
    }

    if (showEditDialog && onEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 10,
                    label = { Text("Message") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedText.isNotBlank()) {
                            onEdit(editedText)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save & Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}