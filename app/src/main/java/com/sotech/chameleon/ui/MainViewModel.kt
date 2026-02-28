package com.sotech.chameleon.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
import java.util.UUID
import javax.inject.Inject

data class PlaygroundOutput(
    val text: String,
    val isError: Boolean = false,
    val imageBitmap: Bitmap? = null
)

data class TerminalSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val output: List<PlaygroundOutput> = emptyList()
)

data class CodeCell(
    val id: String = UUID.randomUUID().toString(),
    val code: String = "",
    val output: List<PlaygroundOutput> = emptyList(),
    val isRunning: Boolean = false
)

data class FileNode(
    val file: File,
    val depth: Int,
    val isExpanded: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
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

    // --- Playground States ---
    private val playgroundDir by lazy { File(context.filesDir, "playground").apply { mkdirs() } }

    private val _currentPlaygroundDir = MutableStateFlow<File>(playgroundDir)
    val currentPlaygroundDir: StateFlow<File> = _currentPlaygroundDir.asStateFlow()

    val isAtPlaygroundRoot: StateFlow<Boolean> = _currentPlaygroundDir.map {
        it.absolutePath == playgroundDir.absolutePath
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    private val _expandedFolders = MutableStateFlow<Set<String>>(emptySet())

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    private val _currentFile = MutableStateFlow<File?>(null)
    val currentFile: StateFlow<File?> = _currentFile.asStateFlow()

    private val _codeContent = MutableStateFlow("")
    val codeContent: StateFlow<String> = _codeContent.asStateFlow()

    private val _isPlaygroundRunning = MutableStateFlow(false)
    val isPlaygroundRunning: StateFlow<Boolean> = _isPlaygroundRunning.asStateFlow()

    // Colab-style States
    private val _isColabMode = MutableStateFlow(false)
    val isColabMode: StateFlow<Boolean> = _isColabMode.asStateFlow()

    private val _notebookCells = MutableStateFlow<List<CodeCell>>(emptyList())
    val notebookCells: StateFlow<List<CodeCell>> = _notebookCells.asStateFlow()

    private val _installedPackages = MutableStateFlow<List<String>?>(null)
    val installedPackages: StateFlow<List<String>?> = _installedPackages.asStateFlow()

    // Multi-session Terminal State
    private val _terminalSessions = MutableStateFlow<List<TerminalSession>>(listOf(TerminalSession(name = "Bash 1")))
    val terminalSessions: StateFlow<List<TerminalSession>> = _terminalSessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String>(_terminalSessions.value.first().id)
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    // --- Original States ---
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

    val savedMindMaps = repository.savedMindMaps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedNotes = repository.savedNotes.stateIn(
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

    private val _mindMapContent = MutableStateFlow("")
    val mindMapContent: StateFlow<String> = _mindMapContent.asStateFlow()

    private val _isGeneratingMindMap = MutableStateFlow(false)
    val isGeneratingMindMap: StateFlow<Boolean> = _isGeneratingMindMap.asStateFlow()

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

    // --- File System / Tree Playground Functions ---

    fun refreshFileTree() {
        val result = mutableListOf<FileNode>()
        buildTree(playgroundDir, 0, _expandedFolders.value, result)
        _fileTree.value = result
    }

    private fun buildTree(dir: File, depth: Int, expanded: Set<String>, result: MutableList<FileNode>) {
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: return
        for (file in files) {
            val isExp = expanded.contains(file.absolutePath)
            result.add(FileNode(file, depth, isExp))
            if (file.isDirectory && isExp) {
                buildTree(file, depth + 1, expanded, result)
            }
        }
    }

    fun toggleFolder(file: File) {
        val current = _expandedFolders.value.toMutableSet()
        if (current.contains(file.absolutePath)) {
            current.remove(file.absolutePath)
        } else {
            current.add(file.absolutePath)
        }
        _expandedFolders.value = current
        _currentPlaygroundDir.value = file
        refreshFileTree()
    }

    fun resetTargetFolder() {
        _currentPlaygroundDir.value = playgroundDir
    }

    fun deletePlaygroundItem(file: File) {
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
        if (_currentFile.value?.absolutePath == file.absolutePath) {
            _currentFile.value = null
            _codeContent.value = ""
        }
        if (_currentPlaygroundDir.value.absolutePath.startsWith(file.absolutePath)) {
            _currentPlaygroundDir.value = playgroundDir
        }
        refreshFileTree()
    }

    fun createPlaygroundFolder(folderName: String) {
        val folder = File(_currentPlaygroundDir.value, folderName)
        if (!folder.exists()) {
            folder.mkdirs()
            val currentExp = _expandedFolders.value.toMutableSet()
            currentExp.add(_currentPlaygroundDir.value.absolutePath)
            _expandedFolders.value = currentExp
            refreshFileTree()
        }
    }

    fun createPlaygroundFile(fileName: String) {
        var name = fileName
        if (!name.endsWith(".py") && !name.endsWith(".txt")) name += ".py"
        val file = File(_currentPlaygroundDir.value, name)
        if (!file.exists()) {
            file.createNewFile()
            if (name.endsWith(".py")) {
                file.writeText("# %%\nimport matplotlib.pyplot as plt\nimport numpy as np\n\nprint(\"Hello Chameleon Notebook!\")\n")
            }
            val currentExp = _expandedFolders.value.toMutableSet()
            currentExp.add(_currentPlaygroundDir.value.absolutePath)
            _expandedFolders.value = currentExp

            refreshFileTree()
            selectPlaygroundFile(file)
        }
    }

    fun selectPlaygroundFile(file: File) {
        savePlaygroundFile()
        _currentFile.value = file
        val content = file.readText()
        _codeContent.value = content

        if (content.contains("# %%")) {
            _isColabMode.value = true
            parseCellsFromContent(content)
        } else {
            _isColabMode.value = false
        }
    }

    fun updateCodeContent(content: String) {
        _codeContent.value = content
    }

    fun savePlaygroundFile() {
        if (_currentFile.value == null) return
        val contentToSave = if (_isColabMode.value) {
            _notebookCells.value.joinToString("\n\n# %%\n") { it.code }
        } else {
            _codeContent.value
        }
        _currentFile.value?.writeText(contentToSave)
    }

    fun saveImageToGallery(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filename = "Chameleon_Plot_${System.currentTimeMillis()}.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Chameleon")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(it, values, null, null)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- Colab Mode Functions ---
    fun toggleColabMode() {
        if (_isColabMode.value) {
            _codeContent.value = _notebookCells.value.joinToString("\n\n# %%\n") { it.code }
            _isColabMode.value = false
        } else {
            parseCellsFromContent(_codeContent.value)
            _isColabMode.value = true
        }
    }

    private fun parseCellsFromContent(content: String) {
        val blocks = content.split("# %%").map { it.trim() }.filter { it.isNotEmpty() }
        _notebookCells.value = if (blocks.isEmpty()) {
            listOf(CodeCell(code = ""))
        } else {
            blocks.map { CodeCell(code = it) }
        }
    }

    fun updateCellCode(id: String, newCode: String) {
        _notebookCells.value = _notebookCells.value.map { if (it.id == id) it.copy(code = newCode) else it }
    }

    fun addCellBelow(id: String) {
        val currentCells = _notebookCells.value.toMutableList()
        val index = currentCells.indexOfFirst { it.id == id }
        if (index != -1) {
            currentCells.add(index + 1, CodeCell())
            _notebookCells.value = currentCells
        }
    }

    fun removeCell(id: String) {
        if (_notebookCells.value.size > 1) {
            _notebookCells.value = _notebookCells.value.filter { it.id != id }
        }
    }

    fun runCell(id: String) {
        val cell = _notebookCells.value.find { it.id == id } ?: return
        if (cell.isRunning) return

        _notebookCells.value = _notebookCells.value.map {
            if (it.id == id) it.copy(isRunning = true, output = emptyList()) else it
        }

        viewModelScope.launch {
            val result = codeExecutor.execute(cell.code, "python", context, _activeSessionId.value)
            val newOutput = PlaygroundOutput(
                text = if (result.success) result.output else result.error,
                isError = !result.success,
                imageBitmap = result.imageBitmap
            )

            _notebookCells.value = _notebookCells.value.map {
                if (it.id == id) it.copy(isRunning = false, output = listOf(newOutput)) else it
            }
        }
    }

    fun restartSessionAndClearOutputs() {
        codeExecutor.resetSession(_activeSessionId.value)
        if (_isColabMode.value) {
            _notebookCells.value = _notebookCells.value.map { it.copy(output = emptyList()) }
        } else {
            clearTerminal(_activeSessionId.value)
        }
    }

    fun fetchInstalledPackages() {
        viewModelScope.launch {
            _installedPackages.value = listOf("Loading packages...")
            val packages = codeExecutor.getInstalledPackages(context)
            _installedPackages.value = packages
        }
    }

    fun dismissInstalledPackages() {
        _installedPackages.value = null
    }

    // --- Terminal Session Management ---
    fun createTerminalSession() {
        val count = _terminalSessions.value.size + 1
        val newSession = TerminalSession(name = "Bash $count")
        _terminalSessions.value = _terminalSessions.value + newSession
        _activeSessionId.value = newSession.id
    }

    fun switchTerminalSession(id: String) {
        _activeSessionId.value = id
    }

    fun closeTerminalSession(id: String) {
        val currentList = _terminalSessions.value
        if (currentList.size > 1) {
            val newList = currentList.filter { it.id != id }
            _terminalSessions.value = newList
            if (_activeSessionId.value == id) {
                _activeSessionId.value = newList.last().id
            }
        }
    }

    fun clearTerminal(sessionId: String) {
        _terminalSessions.value = _terminalSessions.value.map { session ->
            if (session.id == sessionId) session.copy(output = emptyList()) else session
        }
    }

    private fun appendTerminalOutput(sessionId: String, output: PlaygroundOutput) {
        _terminalSessions.value = _terminalSessions.value.map { session ->
            if (session.id == sessionId) session.copy(output = session.output + output) else session
        }
    }

    fun runPlaygroundCode() {
        if (_isPlaygroundRunning.value) return

        savePlaygroundFile()

        val codeToRun = _codeContent.value
        val sessionId = _activeSessionId.value
        val fileName = _currentFile.value?.name ?: "script.py"

        appendTerminalOutput(sessionId, PlaygroundOutput("> Running $fileName...", isError = false))

        _isPlaygroundRunning.value = true

        viewModelScope.launch {
            val result = codeExecutor.execute(codeToRun, "python", context, sessionId)

            val newOutput = PlaygroundOutput(
                text = if(result.success) result.output else result.error,
                isError = !result.success,
                imageBitmap = result.imageBitmap
            )
            appendTerminalOutput(sessionId, newOutput)
            _isPlaygroundRunning.value = false
        }
    }

    // --- Original Functions ---
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

    fun saveMindMapVersion(title: String, content: String) {
        viewModelScope.launch {
            repository.saveMindMapVersion(MindMapVersion(title = title, content = content))
        }
    }

    fun deleteMindMapVersion(id: String) {
        viewModelScope.launch {
            repository.deleteMindMapVersion(id)
        }
    }

    fun saveNote(id: String?, title: String, content: String, alignment: Int, fontSize: Float) {
        viewModelScope.launch {
            val noteToSave = if (id != null) {
                Note(id = id, title = title, content = content, alignment = alignment, fontSize = fontSize, timestamp = System.currentTimeMillis())
            } else {
                Note(title = title, content = content, alignment = alignment, fontSize = fontSize)
            }
            repository.saveNote(noteToSave)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun fixNoteText(prompt: String, onComplete: (String) -> Unit) {
        val currentModel = _currentModel.value
        if (currentModel == null) {
            onComplete("")
            return
        }

        viewModelScope.launch {
            try {
                if (currentModel.isApiModel) {
                    geminiHelper.generateResponse(
                        prompt = prompt,
                        model = currentModel,
                        images = emptyList(),
                        onPartialResult = {},
                        onComplete = { _ ->
                            onComplete(geminiHelper.currentResponse.value)
                        },
                        onError = { onComplete("") }
                    )
                } else {
                    llmHelper.generateResponse(
                        prompt = prompt,
                        model = currentModel,
                        images = emptyList(),
                        onPartialResult = {},
                        onComplete = { _ ->
                            onComplete(llmHelper.currentResponse.value)
                        },
                        onError = { onComplete("") }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in fixNoteText", e)
                onComplete("")
            }
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

    fun updateMindMapContent(content: String) {
        _mindMapContent.value = content
    }

    fun generateMindMap(prompt: String, images: List<Bitmap> = emptyList(), pdfUris: List<Uri> = emptyList()) {
        val currentModel = _currentModel.value ?: return

        if (prompt.isBlank() && images.isEmpty() && pdfUris.isEmpty()) {
            Log.w(TAG, "Cannot generate mind map without prompt, image, or PDF")
            return
        }

        if (pdfUris.isNotEmpty() && !currentModel.isApiModel) {
            _mindMapContent.value = "Error: Local AI models do not support PDF processing. Please select a Gemini API model or remove the PDF attachment."
            return
        }

        stopGeneration()
        _isGeneratingMindMap.value = true
        _mindMapContent.value = "Generating mind map structure..."

        currentMessageJob = viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are an expert at extracting information and creating mind maps.
                    Analyze the following input and generate a hierarchical mind map structure using Mermaid.js format (graph TD).
                    Do not include markdown code blocks, explanations, or any other text. Output strictly the raw Mermaid.js syntax starting with 'graph TD'.
                    Ensure the nodes and relationships are logically structured.
                    
                    CRITICAL SYNTAX RULE - YOU MUST FOLLOW THIS:
                    You MUST wrap EVERY node's text label in double quotes to prevent syntax errors with mathematical symbols, slashes, hyphens, and special characters.
                    
                    User Input: $prompt
                """.trimIndent()

                val finalPrompt = if (pdfUris.isNotEmpty() && currentModel.isApiModel) {
                    "$systemPrompt\n[PDF Attachments Included - Extract key concepts for mind map]"
                } else {
                    systemPrompt
                }

                val pdfBase64s = mutableListOf<String>()
                if (pdfUris.isNotEmpty() && currentModel.isApiModel) {
                    for (uri in pdfUris) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bytes = stream.readBytes()
                                pdfBase64s.add(android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to read PDF: $uri", e)
                        }
                    }
                }

                if (currentModel.isApiModel) {
                    geminiHelper.generateResponse(
                        prompt = finalPrompt,
                        model = currentModel,
                        images = images,
                        pdfBase64s = pdfBase64s,
                        onPartialResult = { partial -> },
                        onComplete = { stats ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = geminiHelper.currentResponse.value.replace("```mermaid", "").replace("```", "").trim()
                        },
                        onError = { error ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = "Error generating mind map: $error"
                        }
                    )
                } else {
                    llmHelper.generateResponse(
                        prompt = finalPrompt,
                        model = currentModel,
                        images = images,
                        onPartialResult = { partial -> },
                        onComplete = { stats ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = llmHelper.currentResponse.value.replace("```mermaid", "").replace("```", "").trim()
                        },
                        onError = { error ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = "Error generating mind map: $error"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in generateMindMap", e)
                _isGeneratingMindMap.value = false
                _mindMapContent.value = "Error: ${e.message}"
            }
        }
    }

    fun fixMindMapSyntax(brokenCode: String) {
        val currentModel = _currentModel.value ?: return

        stopGeneration()
        _isGeneratingMindMap.value = true
        _mindMapContent.value = "Fixing syntax error..."

        currentMessageJob = viewModelScope.launch {
            try {
                val systemPrompt = """
                    The following Mermaid.js graph TD code has a syntax error. 
                    Please fix it and return ONLY the valid Mermaid.js code starting with 'graph TD'.
                    Do not include markdown code blocks, explanations, or any other text.
                    CRITICAL: You MUST wrap EVERY node's text label in double quotes to prevent syntax errors (e.g., A["Label text"] --> B{"Another label"}).
                    
                    Broken Code:
                    $brokenCode
                """.trimIndent()

                if (currentModel.isApiModel) {
                    geminiHelper.generateResponse(
                        prompt = systemPrompt,
                        model = currentModel,
                        images = emptyList(),
                        pdfBase64s = emptyList(),
                        onPartialResult = { },
                        onComplete = { stats ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = geminiHelper.currentResponse.value.replace("```mermaid", "").replace("```", "").trim()
                        },
                        onError = { error ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = "Error fixing map: $error\n\nOriginal Code:\n$brokenCode"
                        }
                    )
                } else {
                    llmHelper.generateResponse(
                        prompt = systemPrompt,
                        model = currentModel,
                        images = emptyList(),
                        onPartialResult = { },
                        onComplete = { stats ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = llmHelper.currentResponse.value.replace("```mermaid", "").replace("```", "").trim()
                        },
                        onError = { error ->
                            _isGeneratingMindMap.value = false
                            _mindMapContent.value = "Error fixing map: $error\n\nOriginal Code:\n$brokenCode"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in fixMindMapSyntax", e)
                _isGeneratingMindMap.value = false
                _mindMapContent.value = "Error: ${e.message}\n\nOriginal Code:\n$brokenCode"
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

IMPORTANT: If the user asks to plot, graph, or visualize a function, you MUST provide executable Python code using matplotlib."""
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
        _isGeneratingMindMap.value = false

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