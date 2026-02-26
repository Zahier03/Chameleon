package com.sotech.chameleon.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sotech.chameleon.data.*
import com.sotech.chameleon.llm.LlmHelper
import com.sotech.chameleon.llm.GeminiHelper
import com.sotech.chameleon.execution.CodeExecutor
import com.sotech.chameleon.execution.CodeParser
import com.sotech.chameleon.execution.GraphGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlin.collections.mapIndexed

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ModelRepository,
    private val llmHelper: LlmHelper,
    private val geminiHelper: GeminiHelper,
    private val codeExecutor: CodeExecutor,
    private val codeParser: CodeParser,
    private val graphGenerator: GraphGenerator
) : ViewModel() {
    private val TAG = "MainViewModel"

    val importedModels = repository.importedModels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val conversations = repository.conversations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentConversationId = MutableStateFlow("default")
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    val currentConversation: Flow<ChatConversation?> = currentConversationId.flatMapLatest { id ->
        flow {
            conversations.collect { convos ->
                emit(convos.find { it.id == id })
            }
        }
    }

    val chatMessages: Flow<List<ChatMessage>> = currentConversationId.flatMapLatest { id ->
        repository.getMessagesForConversation(id)
    }

    val calendarEvents = repository.calendarEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val timetableEntries = repository.timetableEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentModel = MutableStateFlow<ImportedModel?>(null)
    val currentModel: StateFlow<ImportedModel?> = _currentModel.asStateFlow()

    private val _modelState = MutableStateFlow(ModelState())
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _importStatus = MutableStateFlow("")
    val importStatus: StateFlow<String> = _importStatus.asStateFlow()

    private val _statsSettings = MutableStateFlow(StatsSettings())
    val statsSettings: StateFlow<StatsSettings> = _statsSettings.asStateFlow()

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    private var currentMessageJob: Job? = null

    val isGenerating: StateFlow<Boolean>
        get() = if (_currentModel.value?.isApiModel == true) geminiHelper.isGenerating else llmHelper.isGenerating

    val currentResponse: StateFlow<String>
        get() = if (_currentModel.value?.isApiModel == true) geminiHelper.currentResponse else llmHelper.currentResponse

    val initializationProgress = llmHelper.initializationProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    data class ModelState(
        val model: ImportedModel? = null,
        val status: ModelStatus = ModelStatus.NOT_INITIALIZED,
        val error: String? = null
    )

    init {
        viewModelScope.launch {
            importedModels.collect { models ->
                if (models.isNotEmpty() && _currentModel.value == null) {
                    selectModel(models.first())
                }
            }
        }

        viewModelScope.launch {
            repository.currentConversationId.collect { id ->
                _currentConversationId.value = id
            }
        }

        viewModelScope.launch {
            repository.statsSettings.collect { settings ->
                _statsSettings.value = settings
            }
        }

        viewModelScope.launch {
            repository.themeSettings.collect { settings ->
                _themeSettings.value = settings
            }
        }

        viewModelScope.launch {
            conversations.collect { convos ->
                if (convos.isEmpty()) {
                    createNewConversation("New Chat")
                }
            }
        }
    }

    fun createNewConversation(title: String = "New Chat") {
        viewModelScope.launch {
            val newConvo = repository.createConversation(
                title = title,
                modelName = _currentModel.value?.displayName
            )
            setCurrentConversation(newConvo.id)
        }
    }

    fun setCurrentConversation(conversationId: String) {
        viewModelScope.launch {
            repository.setCurrentConversation(conversationId)
            _currentConversationId.value = conversationId
        }
    }

    fun updateConversationTitle(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateConversationTitle(conversationId, newTitle)
        }
    }

    fun pinConversation(conversationId: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.pinConversation(conversationId, isPinned)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
        }
    }

    fun deleteConversations(conversationIds: List<String>) {
        viewModelScope.launch {
            repository.deleteConversations(conversationIds)
        }
    }

    fun addCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.saveCalendarEvent(event)
        }
    }

    fun deleteCalendarEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(eventId)
        }
    }

    fun addTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.saveTimetableEntry(entry)
        }
    }

    fun deleteTimetableEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteTimetableEntry(entryId)
        }
    }

    fun getStatsSummary(conversationId: String? = null): Flow<MessageStatsSummary> = flow {
        val targetId = conversationId ?: _currentConversationId.value
        repository.getMessagesForConversation(targetId).collect { messages ->
            emit(calculateStatsSummary(messages))
        }
    }

    private fun calculateStatsSummary(messages: List<ChatMessage>): MessageStatsSummary {
        val aiMessages = messages.filter { !it.isUser && it.stats != null }

        if (aiMessages.isEmpty()) {
            return MessageStatsSummary()
        }

        val totalMessages = aiMessages.size
        val avgTimeToFirstToken = aiMessages.mapNotNull { it.stats?.timeToFirstToken }.average().toFloat()
        val avgPrefillSpeed = aiMessages.mapNotNull { it.stats?.prefillSpeed }.average().toFloat()
        val avgDecodeSpeed = aiMessages.mapNotNull { it.stats?.decodeSpeed }.average().toFloat()
        val avgLatency = aiMessages.mapNotNull { it.stats?.totalLatency }.average().toFloat()
        val totalTokens = aiMessages.sumOf { it.stats?.tokenCount ?: 0 }

        return MessageStatsSummary(
            totalMessages = totalMessages,
            avgTimeToFirstToken = avgTimeToFirstToken,
            avgPrefillSpeed = avgPrefillSpeed,
            avgDecodeSpeed = avgDecodeSpeed,
            avgLatency = avgLatency,
            totalTokens = totalTokens
        )
    }

    private fun initializeModel(model: ImportedModel) {
        viewModelScope.launch {
            if (model.isApiModel) {
                _modelState.value = ModelState(
                    model = model,
                    status = ModelStatus.READY
                )
                Log.d(TAG, "Gemini API model ready: ${model.displayName}")
            } else {
                _modelState.value = ModelState(
                    model = model,
                    status = ModelStatus.INITIALIZING
                )

                llmHelper.initialize(
                    context = context,
                    model = model,
                    onComplete = { success, error ->
                        if (success) {
                            _modelState.value = ModelState(
                                model = model,
                                status = ModelStatus.READY
                            )
                            Log.d(TAG, "Model initialized: ${model.displayName}")
                        } else {
                            _modelState.value = ModelState(
                                model = model,
                                status = ModelStatus.ERROR,
                                error = error ?: "Unknown error"
                            )
                        }
                    }
                )
            }
        }
    }

    fun sendMessage(message: String, images: List<Bitmap> = emptyList()) {
        val currentModel = _currentModel.value ?: return

        if (message.isBlank() && images.isEmpty()) {
            Log.w(TAG, "Cannot send empty message without images")
            return
        }

        stopGeneration()

        currentMessageJob = viewModelScope.launch {
            try {
                val conversationId = _currentConversationId.value

                val imagePaths = images.mapIndexed { index, bitmap ->
                    val fileName = "image_${System.currentTimeMillis()}_$index.png"
                    val file = File(context.filesDir, fileName)
                    withContext(Dispatchers.IO) {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    file.absolutePath
                }

                repository.saveMessage(
                    ChatMessage(
                        content = message,
                        isUser = true,
                        conversationId = conversationId,
                        images = imagePaths
                    )
                )

                delay(10)

                val enhancedMessage = enhanceMessageWithSystemPrompt(message)

                var hasCompleted = false
                var hasError = false

                Log.d(TAG, "Sending message: $message with ${images.size} images")

                if (currentModel.isApiModel) {
                    geminiHelper.generateResponse(
                        prompt = enhancedMessage,
                        model = currentModel,
                        images = images,
                        onPartialResult = { },
                        onComplete = { stats ->
                            if (!hasCompleted && !hasError) {
                                hasCompleted = true

                                viewModelScope.launch {
                                    val finalResponse = geminiHelper.currentResponse.value

                                    if (finalResponse.isNotEmpty()) {
                                        val processedResponse = processResponseWithCodeExecution(finalResponse)
                                        delay(10)

                                        repository.saveMessage(
                                            ChatMessage(
                                                content = processedResponse,
                                                isUser = false,
                                                latencyMs = (stats?.totalLatency ?: 0f) * 1000f,
                                                stats = stats,
                                                conversationId = conversationId
                                            )
                                        )
                                        Log.d(TAG, "AI response saved successfully: ${processedResponse.length} chars")
                                    }
                                }
                            }
                        },
                        onError = { error ->
                            if (!hasCompleted && !hasError) {
                                hasError = true
                                Log.e(TAG, "Generation error: $error")
                                viewModelScope.launch {
                                    delay(10)

                                    val partialResponse = geminiHelper.currentResponse.value
                                    if (partialResponse.isNotEmpty()) {
                                        repository.saveMessage(
                                            ChatMessage(
                                                content = partialResponse,
                                                isUser = false,
                                                conversationId = conversationId
                                            )
                                        )
                                    } else {
                                        repository.saveMessage(
                                            ChatMessage(
                                                content = "Error: $error",
                                                isUser = false,
                                                conversationId = conversationId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                } else {
                    llmHelper.generateResponse(
                        prompt = enhancedMessage,
                        model = currentModel,
                        images = images,
                        onPartialResult = { },
                        onComplete = { stats ->
                            if (!hasCompleted && !hasError) {
                                hasCompleted = true

                                viewModelScope.launch {
                                    val finalResponse = llmHelper.currentResponse.value

                                    if (finalResponse.isNotEmpty()) {
                                        val processedResponse = processResponseWithCodeExecution(finalResponse)
                                        delay(10)

                                        repository.saveMessage(
                                            ChatMessage(
                                                content = processedResponse,
                                                isUser = false,
                                                latencyMs = (stats?.totalLatency ?: 0f) * 1000f,
                                                stats = stats,
                                                conversationId = conversationId
                                            )
                                        )
                                        Log.d(TAG, "AI response saved successfully: ${processedResponse.length} chars")
                                    }
                                }
                            }
                        },
                        onError = { error ->
                            if (!hasCompleted && !hasError) {
                                hasError = true
                                Log.e(TAG, "Generation error: $error")
                                viewModelScope.launch {
                                    delay(10)

                                    val partialResponse = llmHelper.currentResponse.value
                                    if (partialResponse.isNotEmpty()) {
                                        repository.saveMessage(
                                            ChatMessage(
                                                content = partialResponse,
                                                isUser = false,
                                                conversationId = conversationId
                                            )
                                        )
                                    } else {
                                        repository.saveMessage(
                                            ChatMessage(
                                                content = "Error: $error",
                                                isUser = false,
                                                conversationId = conversationId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
            }
        }
    }

    private fun enhanceMessageWithSystemPrompt(message: String): String {
        val lowerMessage = message.lowercase()
        val needsEnhancement = lowerMessage.contains("plot") ||
                lowerMessage.contains("graph") ||
                lowerMessage.contains("visualize") ||
                lowerMessage.contains("chart") ||
                lowerMessage.contains("draw") ||
                lowerMessage.contains("calculate") ||
                lowerMessage.contains("compute") ||
                lowerMessage.contains("execute") ||
                lowerMessage.contains("run")

        return if (needsEnhancement) {
            """${com.sotech.chameleon.execution.SystemPromptHelper.CODE_EXECUTION_PROMPT}

User Request: $message

IMPORTANT: If the user asks to plot, graph, or visualize a function, you MUST provide executable Python code using matplotlib. Do not just describe what the graph would look like. Provide the actual code that will be executed to generate the graph."""
        } else {
            message
        }
    }

    private suspend fun processResponseWithCodeExecution(response: String): String = withContext(Dispatchers.IO) {
        try {
            val codeBlocks = codeParser.parseCodeBlocks(response)
            if (codeBlocks.isEmpty()) return@withContext response

            var processedResponse = response
            val results = mutableListOf<String>()

            for (block in codeBlocks) {
                val result = codeExecutor.execute(block.code, block.language, context)
                if (result.success && result.output.isNotEmpty()) {
                    results.add("\n\n**Execution Result:**\n```\n${result.output}\n```")
                } else if (!result.success && result.error.isNotEmpty()) {
                    results.add("\n\n**Execution Error:**\n```\n${result.error}\n```")
                }
            }

            if (results.isNotEmpty()) {
                processedResponse += results.joinToString("")
            }

            processedResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error processing code execution", e)
            response
        }
    }

    fun stopGeneration() {
        Log.d(TAG, "Stopping current generation...")

        val currentModel = _currentModel.value
        val isCurrentlyGenerating = if (currentModel?.isApiModel == true) {
            geminiHelper.isGenerating.value
        } else {
            llmHelper.isGenerating.value
        }

        val partialResponse = if (currentModel?.isApiModel == true) {
            geminiHelper.currentResponse.value
        } else {
            llmHelper.currentResponse.value
        }

        currentMessageJob?.cancel()
        currentMessageJob = null

        if (currentModel?.isApiModel == true) {
            geminiHelper.stopGeneration()
        } else {
            llmHelper.stopGeneration()
        }

        if (isCurrentlyGenerating && partialResponse.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    delay(10)

                    repository.saveMessage(
                        ChatMessage(
                            content = partialResponse,
                            isUser = false,
                            latencyMs = 0f,
                            conversationId = _currentConversationId.value
                        )
                    )
                    Log.d(TAG, "Partial response saved: ${partialResponse.length} chars")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save partial response", e)
                }
            }
        }

        Log.d(TAG, "Generation stopped")
    }

    fun clearChat() {
        viewModelScope.launch {
            stopGeneration()

            repository.clearMessages(_currentConversationId.value)

            _currentModel.value?.let { model ->
                if (!model.isApiModel) {
                    llmHelper.resetSession(model)
                }
            }

            Log.d(TAG, "Chat cleared successfully")
        }
    }

    fun deleteMessageAndRegenerate(messageTimestamp: Long) {
        viewModelScope.launch {
            val conversationId = _currentConversationId.value

            repository.getMessagesForConversation(conversationId).collect { msgList ->
                val messageIndex = msgList.indexOfFirst { it.timestamp == messageTimestamp }

                if (messageIndex == -1) {
                    Log.w(TAG, "Message not found for regeneration")
                    return@collect
                }

                val messageToDelete = msgList[messageIndex]

                if (messageToDelete.isUser) {
                    Log.w(TAG, "Cannot regenerate user messages")
                    return@collect
                }

                val userMessage = if (messageIndex > 0 && msgList[messageIndex - 1].isUser) {
                    msgList[messageIndex - 1]
                } else {
                    Log.w(TAG, "No user message found before AI response")
                    return@collect
                }

                repository.deleteMessagesFrom(userMessage.timestamp, conversationId)
                delay(100)

                val imageBitmaps = userMessage.images.mapNotNull { imagePath ->
                    try {
                        android.graphics.BitmapFactory.decodeFile(imagePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load image for regeneration: $imagePath", e)
                        null
                    }
                }

                sendMessage(userMessage.content, imageBitmaps)

                Log.d(TAG, "Regenerating from message at index $messageIndex with ${imageBitmaps.size} images")
                return@collect
            }
        }
    }

    fun editMessageAndRegenerate(messageTimestamp: Long, newContent: String) {
        viewModelScope.launch {
            val conversationId = _currentConversationId.value

            repository.getMessagesForConversation(conversationId).collect { msgList ->
                val messageIndex = msgList.indexOfFirst { it.timestamp == messageTimestamp }

                if (messageIndex == -1) {
                    Log.w(TAG, "Message not found for editing")
                    return@collect
                }

                val messageToEdit = msgList[messageIndex]

                if (!messageToEdit.isUser) {
                    Log.w(TAG, "Can only edit user messages")
                    return@collect
                }

                repository.deleteMessagesFrom(messageTimestamp, conversationId)
                delay(100)

                val imageBitmaps = messageToEdit.images.mapNotNull { imagePath ->
                    try {
                        android.graphics.BitmapFactory.decodeFile(imagePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load image for editing: $imagePath", e)
                        null
                    }
                }

                sendMessage(newContent, imageBitmaps)

                Log.d(TAG, "Message edited and regenerating with ${imageBitmaps.size} images")
                return@collect
            }
        }
    }

    fun updateInput(input: String) {
        _currentInput.value = input
    }

    fun importModel(uri: Uri, displayName: String, supportImage: Boolean = false, supportAudio: Boolean = false) {
        viewModelScope.launch {
            try {
                _isImporting.value = true
                _importProgress.value = 0f
                _importStatus.value = "Starting import..."

                _modelState.value = ModelState(
                    status = ModelStatus.INITIALIZING,
                    error = null
                )

                val fileName = getFileName(uri)
                val fileSize = getFileSize(uri)
                val destFile = File(context.filesDir, fileName)

                Log.d(TAG, "Importing model: $fileName (${fileSize / 1024 / 1024}MB) - Image: $supportImage, Audio: $supportAudio")
                _importStatus.value = "Copying model file..."

                val success = withContext(Dispatchers.IO) {
                    copyFileWithProgress(uri, destFile, fileSize)
                }

                if (success) {
                    _importStatus.value = "Finalizing..."
                    _importProgress.value = 1f

                    val model = ImportedModel(
                        displayName = displayName.ifBlank { fileName },
                        fileName = fileName,
                        filePath = destFile.absolutePath,
                        fileSize = fileSize,
                        timestamp = System.currentTimeMillis(),
                        isApiModel = false,
                        supportImage = supportImage,
                        supportAudio = supportAudio
                    )

                    repository.saveModel(model)
                    selectModel(model)

                    _modelState.value = ModelState(
                        model = model,
                        status = ModelStatus.READY
                    )

                    _importStatus.value = "Import complete!"
                    delay(500)

                    Log.d(TAG, "Model imported successfully: ${model.displayName} (Image: $supportImage, Audio: $supportAudio)")
                } else {
                    _modelState.value = ModelState(
                        status = ModelStatus.ERROR,
                        error = "Failed to copy model file"
                    )
                    _importStatus.value = "Import failed"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import model", e)
                _modelState.value = ModelState(
                    status = ModelStatus.ERROR,
                    error = e.message ?: "Unknown error"
                )
                _importStatus.value = "Error: ${e.message}"
            } finally {
                delay(1000)
                _isImporting.value = false
                _importProgress.value = 0f
                _importStatus.value = ""
            }
        }
    }

    fun addGeminiModel(displayName: String, apiKey: String, modelCode: String, modelType: GeminiModelType) {
        viewModelScope.launch {
            try {
                val model = ImportedModel(
                    displayName = displayName,
                    fileName = "gemini_$modelCode",
                    filePath = "",
                    fileSize = 0,
                    timestamp = System.currentTimeMillis(),
                    isApiModel = true,
                    apiKey = apiKey,
                    modelCode = modelCode,
                    modelType = modelType,
                    maxTokens = 65536
                )

                repository.saveModel(model)
                selectModel(model)

                Log.d(TAG, "Gemini model added successfully: ${model.displayName}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add Gemini model", e)
            }
        }
    }

    private suspend fun copyFileWithProgress(sourceUri: Uri, destFile: File, totalSize: Long): Boolean = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
            outputStream = FileOutputStream(destFile)

            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream")
                return@withContext false
            }

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!isActive) {
                    Log.d(TAG, "File copy cancelled")
                    return@withContext false
                }
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val progress = if (totalSize > 0) {
                    (totalBytesRead.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                _importProgress.value = progress
                _importStatus.value = "Copying: ${(progress * 100).toInt()}%"

                yield()
            }

            outputStream.flush()
            Log.d(TAG, "File copied successfully. Total bytes: $totalBytesRead")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error during file copy", e)
            try {
                destFile.delete()
            } catch (deleteError: Exception) {
                Log.e(TAG, "Failed to delete partial file", deleteError)
            }
            return@withContext false
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing input stream", e)
            }
            try {
                outputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing output stream", e)
            }
        }
    }

    fun selectModel(model: ImportedModel) {
        stopGeneration()

        _currentModel.value = model
        _modelState.value = ModelState(
            model = model,
            status = ModelStatus.NOT_INITIALIZED
        )
        initializeModel(model)
    }

    fun deleteModel(model: ImportedModel) {
        viewModelScope.launch {
            try {
                if (_currentModel.value?.fileName == model.fileName) {
                    stopGeneration()
                }

                if (!model.isApiModel) {
                    withContext(Dispatchers.IO) {
                        val file = File(model.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }

                repository.deleteModel(model.fileName)

                if (_currentModel.value?.fileName == model.fileName) {
                    if (!model.isApiModel) {
                        llmHelper.cleanup()
                    } else {
                        geminiHelper.cleanup()
                    }
                    _currentModel.value = null
                    _modelState.value = ModelState()
                }

                Log.d(TAG, "Model deleted: ${model.fileName}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete model", e)
            }
        }
    }

    fun updateModelConfig(model: ImportedModel) {
        viewModelScope.launch {
            repository.updateModel(model)
            if (_currentModel.value?.fileName == model.fileName) {
                _currentModel.value = model
                if (_modelState.value.status == ModelStatus.READY && !model.isApiModel) {
                    initializeModel(model)
                }
            }
        }
    }

    fun updateStatsSettings(settings: StatsSettings) {
        viewModelScope.launch {
            repository.updateStatsSettings(settings)
        }
    }

    fun updateThemeSettings(settings: ThemeSettings) {
        viewModelScope.launch {
            repository.updateThemeSettings(settings)
        }
    }

    private fun getFileName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                it.getString(nameIndex)
            } else {
                null
            }
        } ?: "model_${System.currentTimeMillis()}.bin"
    }

    private fun getFileSize(uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && it.moveToFirst()) {
                it.getLong(sizeIndex)
            } else {
                0L
            }
        } ?: 0L
    }

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
        llmHelper.cleanup()
        geminiHelper.cleanup()
        Log.d(TAG, "ViewModel cleared")
    }
}