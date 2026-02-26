package com.sotech.chameleon.llm

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.sotech.chameleon.data.ImportedModel
import com.sotech.chameleon.data.MessageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class LlmHelper @Inject constructor() {
    private val TAG = "LlmHelper"

    private var llmInference: LlmInference? = null
    private var llmSession: LlmInferenceSession? = null
    private var currentModel: ImportedModel? = null
    private var currentContext: Context? = null
    private var currentBackend: LlmInference.Backend? = null

    private val sessionMutex = Mutex()
    private var isSessionValid = false

    private var currentGenerationJob: Job? = null
    private val isGenerationCancelled = AtomicBoolean(false)
    private val generationMutex = Mutex()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _initializationProgress = MutableStateFlow("")
    val initializationProgress: StateFlow<String> = _initializationProgress.asStateFlow()

    private val _currentStats = MutableStateFlow<MessageStats?>(null)
    val currentStats: StateFlow<MessageStats?> = _currentStats.asStateFlow()

    suspend fun initialize(
        context: Context,
        model: ImportedModel,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            try {
                currentContext = context
                currentModel = model

                cleanup()

                val runtime = Runtime.getRuntime()
                val maxMemory = runtime.maxMemory() / 1024 / 1024
                val totalMemory = runtime.totalMemory() / 1024 / 1024
                val freeMemory = runtime.freeMemory() / 1024 / 1024
                val usedMemory = totalMemory - freeMemory
                val modelSizeMB = model.fileSize / 1024 / 1024

                Log.d(TAG, "Memory before loading - Max: ${maxMemory}MB, Used: ${usedMemory}MB, Free: ${freeMemory}MB")
                Log.d(TAG, "Model size: ${modelSizeMB}MB")
                Log.d(TAG, "Model supports - Image: ${model.supportImage}, Audio: ${model.supportAudio}")

                if (modelSizeMB > 1000) {
                    Log.w(TAG, "Large model detected (${modelSizeMB}MB). Optimizing for stability...")
                    _initializationProgress.value = "Large model detected. Optimizing..."
                }

                _initializationProgress.value = "Loading model..."

                val preferredBackend = if (model.useGpu && modelSizeMB < 2000) {
                    LlmInference.Backend.GPU
                } else {
                    LlmInference.Backend.CPU
                }

                Log.d(TAG, "Attempting to initialize with backend: $preferredBackend")
                _initializationProgress.value = "Creating inference engine (${model.maxTokens} max tokens)..."

                System.gc()
                delay(500)

                var initializationSuccess = false
                var backendUsed = preferredBackend

                for (attempt in 0..2) {
                    try {
                        when (attempt) {
                            0 -> {
                                backendUsed = preferredBackend
                                _initializationProgress.value = "Trying ${if (backendUsed == LlmInference.Backend.GPU) "GPU" else "CPU"} mode..."
                            }
                            1 -> {
                                if (preferredBackend == LlmInference.Backend.GPU) {
                                    backendUsed = LlmInference.Backend.CPU
                                    Log.w(TAG, "GPU initialization failed, falling back to CPU")
                                    _initializationProgress.value = "GPU failed, trying CPU..."
                                } else {
                                    break
                                }
                            }
                            2 -> {
                                backendUsed = LlmInference.Backend.CPU
                                val reducedTokens = (model.maxTokens * 0.5).toInt().coerceAtLeast(128)
                                Log.w(TAG, "Standard initialization failed, trying CPU with reduced tokens: $reducedTokens")
                                _initializationProgress.value = "Retrying with optimized settings..."
                            }
                        }

                        val maxTokensToUse = if (attempt == 2) {
                            (model.maxTokens * 0.5).toInt().coerceAtLeast(128)
                        } else {
                            model.maxTokens
                        }

                        val optionsBuilder = LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(model.filePath)
                            .setMaxTokens(maxTokensToUse)
                            .setPreferredBackend(backendUsed)

                        if (model.supportImage) {
                            optionsBuilder.setMaxNumImages(10)
                            Log.d(TAG, "Image support enabled with max 10 images")
                        }

                        val options = optionsBuilder.build()

                        Log.d(TAG, "Creating LlmInference with backend: $backendUsed, maxTokens: $maxTokensToUse")
                        _initializationProgress.value = "Loading model into memory..."

                        llmInference = withContext(Dispatchers.IO) {
                            LlmInference.createFromOptions(context, options)
                        }

                        if (llmInference != null) {
                            currentBackend = backendUsed
                            Log.d(TAG, "LlmInference created successfully with backend: $backendUsed")
                            initializationSuccess = true
                            break
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Attempt $attempt failed with backend $backendUsed", e)
                        llmInference?.close()
                        llmInference = null

                        if (attempt == 2) {
                            throw e
                        }

                        System.gc()
                        delay(500)
                    }
                }

                if (!initializationSuccess || llmInference == null) {
                    _initializationProgress.value = "Failed to create inference"
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Failed to initialize model. Try reducing max tokens or use a smaller model.")
                    }
                    return@withContext
                }

                if (createSession(model)) {
                    _isInitialized.value = true

                    val modeInfo = when (currentBackend) {
                        LlmInference.Backend.GPU -> "GPU"
                        LlmInference.Backend.CPU -> "CPU"
                        else -> "Unknown"
                    }

                    _initializationProgress.value = "Ready! ($modeInfo)"
                    isSessionValid = true

                    val usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                    Log.d(TAG, "Memory after loading - Used: ${usedMemoryAfter}MB (increased by ${usedMemoryAfter - usedMemory}MB)")
                    Log.d(TAG, "Model initialized successfully: ${model.displayName} with backend: $currentBackend")

                    withContext(Dispatchers.Main) {
                        onComplete(true, null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Failed to create session")
                    }
                }

            } catch (oom: OutOfMemoryError) {
                Log.e(TAG, "OutOfMemoryError during initialization", oom)
                _initializationProgress.value = "Out of memory"
                cleanup()
                System.gc()
                withContext(Dispatchers.Main) {
                    onComplete(false, "Out of memory. Please close other apps and try again.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize model", e)
                _initializationProgress.value = "Error: ${e.message}"
                cleanup()
                withContext(Dispatchers.Main) {
                    onComplete(false, "Initialization failed: ${e.message}")
                }
            }
        }
    }

    private fun createSession(model: ImportedModel): Boolean {
        return try {
            _initializationProgress.value = "Creating session..."
            Log.d(TAG, "Creating new session with image support: ${model.supportImage}, audio support: ${model.supportAudio}")

            val sessionOptionsBuilder = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(model.topK)
                .setTopP(model.topP)
                .setTemperature(model.temperature)

            val graphOptionsBuilder = GraphOptions.builder()

            if (model.supportImage) {
                graphOptionsBuilder.setEnableVisionModality(true)
                Log.d(TAG, "Vision modality enabled in session")
            }

            if (model.supportAudio) {
                Log.w(TAG, "Audio support requested but not fully configured - skipping audio modality")
            }

            sessionOptionsBuilder.setGraphOptions(graphOptionsBuilder.build())

            llmSession?.close()
            llmSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptionsBuilder.build())
            isSessionValid = true
            Log.d(TAG, "Session created successfully with persistent conversation history")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session", e)
            _initializationProgress.value = "Error creating session"
            llmSession = null
            isSessionValid = false
            false
        }
    }

    private suspend fun ensureValidSession(): Boolean {
        return sessionMutex.withLock {
            if (!isSessionValid || llmSession == null) {
                Log.d(TAG, "Session invalid, attempting to recreate...")
                currentModel?.let { model ->
                    if (createSession(model)) {
                        Log.d(TAG, "Session recreated successfully")
                        return@withLock true
                    }
                }
                Log.e(TAG, "Failed to recreate session")
                return@withLock false
            }
            true
        }
    }

    fun generateResponse(
        prompt: String,
        model: ImportedModel,
        images: List<Bitmap> = emptyList(),
        onPartialResult: (String) -> Unit,
        onComplete: (MessageStats?) -> Unit,
        onError: (String) -> Unit
    ) {
        stopGeneration()

        currentGenerationJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                isGenerationCancelled.set(false)
                generateWithImages(prompt, model, images, onPartialResult, onComplete, onError)
            } catch (e: CancellationException) {
                Log.d(TAG, "Generation cancelled")
                _isGenerating.value = false
                _currentResponse.value = ""
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate response", e)
                _isGenerating.value = false
                _currentResponse.value = ""
                onError("Failed to generate response: ${e.message}")
            }
        }
    }

    private suspend fun generateWithImages(
        prompt: String,
        model: ImportedModel,
        images: List<Bitmap>,
        onPartialResult: (String) -> Unit,
        onComplete: (MessageStats?) -> Unit,
        onError: (String) -> Unit
    ) {
        generationMutex.withLock {
            try {
                if (isGenerationCancelled.get()) {
                    Log.d(TAG, "Generation cancelled before start")
                    return
                }

                if (!ensureValidSession()) {
                    onError("Session not available. Please reinitialize the model.")
                    return
                }

                val session = llmSession
                if (session == null) {
                    onError("Model not initialized")
                    return
                }

                _isGenerating.value = true
                _currentResponse.value = ""
                _currentStats.value = null

                var firstRun = true
                var timeToFirstToken = 0f
                var firstTokenTs = 0L
                var decodeTokens = 0
                var prefillTokens = estimateTokenCount(prompt) + (images.size * 257)
                var prefillSpeed = 0f
                var decodeSpeed: Float
                val startTime = System.currentTimeMillis()
                val accelerator = when (currentBackend) {
                    LlmInference.Backend.GPU -> "GPU"
                    LlmInference.Backend.CPU -> "CPU"
                    else -> "Unknown"
                }
                var generationError = false
                var fullResponse = ""
                var generationComplete = false

                try {
                    Log.d(TAG, "Adding query with ${images.size} images - IMPORTANT ORDER: text first, then images")

                    session.addQueryChunk(prompt)
                    Log.d(TAG, "Text prompt added to session")

                    if (images.isNotEmpty() && model.supportImage) {
                        Log.d(TAG, "Now adding ${images.size} images to session after text prompt")
                        for ((index, bitmap) in images.withIndex()) {
                            try {
                                val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
                                session.addImage(mpImage)
                                Log.d(TAG, "Image ${index + 1}/${images.size} added successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to add image ${index + 1}", e)
                                onError("Failed to process image ${index + 1}: ${e.message}")
                                _isGenerating.value = false
                                return
                            }
                        }
                        Log.d(TAG, "All ${images.size} images added successfully")
                    } else if (images.isNotEmpty() && !model.supportImage) {
                        Log.w(TAG, "Images provided but model does not support images. Images will be ignored.")
                    }

                    session.generateResponseAsync { partialResult, done ->
                        try {
                            if (isGenerationCancelled.get()) {
                                Log.d(TAG, "Generation cancelled during streaming")
                                return@generateResponseAsync
                            }

                            val currentTime = System.currentTimeMillis()

                            if (firstRun) {
                                firstTokenTs = currentTime
                                timeToFirstToken = (firstTokenTs - startTime) / 1000f
                                prefillSpeed = if (timeToFirstToken > 0) prefillTokens / timeToFirstToken else 0f
                                firstRun = false
                            } else {
                                decodeTokens++
                            }

                            fullResponse += partialResult
                            _currentResponse.value = fullResponse
                            onPartialResult(partialResult)

                            if (done) {
                                generationComplete = true
                                _isGenerating.value = false

                                val totalTime = (currentTime - startTime) / 1000f
                                val decodeTime = if (firstTokenTs > 0) (currentTime - firstTokenTs) / 1000f else 0f
                                decodeSpeed = if (decodeTime > 0) decodeTokens / decodeTime else 0f

                                val stats = MessageStats(
                                    timeToFirstToken = timeToFirstToken,
                                    prefillSpeed = prefillSpeed,
                                    decodeSpeed = decodeSpeed,
                                    totalLatency = totalTime,
                                    tokenCount = prefillTokens + decodeTokens,
                                    prefillTokens = prefillTokens,
                                    decodeTokens = decodeTokens,
                                    accelerator = accelerator
                                )

                                _currentStats.value = stats
                                onComplete(stats)

                                Log.d(TAG, "Generation completed. Response length: ${fullResponse.length}")
                                Log.d(TAG, "Generation stats: $stats")
                                Log.d(TAG, "Conversation history maintained in session")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during generation callback", e)
                            generationError = true
                        }
                    }

                    while (_isGenerating.value && !generationComplete && !isGenerationCancelled.get()) {
                        delay(100)
                    }

                    if (isGenerationCancelled.get()) {
                        Log.d(TAG, "Generation was cancelled")
                        _isGenerating.value = false
                        _currentResponse.value = ""
                        return
                    }

                } catch (e: CancellationException) {
                    Log.d(TAG, "Generation coroutine cancelled")
                    _isGenerating.value = false
                    _currentResponse.value = ""
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Generation failed", e)
                    generationError = true

                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        Log.d(TAG, "Attempting to recover from generation error...")
                        if (ensureValidSession()) {
                            Log.d(TAG, "Session recovered successfully")
                        }
                    }

                    throw e
                }

                if (generationError) {
                    _currentResponse.value = ""
                    onError("Generation error occurred. Please try again.")
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Generation mutex operation cancelled")
                _isGenerating.value = false
                _currentResponse.value = ""
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate response", e)
                _isGenerating.value = false
                _currentResponse.value = ""
                onError("Failed to generate response. Please try again.")
            }
        }
    }

    fun stopGeneration() {
        try {
            Log.d(TAG, "Stopping generation...")
            isGenerationCancelled.set(true)

            currentGenerationJob?.cancel()
            currentGenerationJob = null

            try {
                llmSession?.cancelGenerateResponseAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling session generation", e)
            }

            _isGenerating.value = false
            Log.d(TAG, "Generation stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop generation", e)
            _isGenerating.value = false
        }
    }

    fun resetSession(model: ImportedModel) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            sessionMutex.withLock {
                try {
                    stopGeneration()

                    Log.d(TAG, "Resetting session - this will clear conversation history")
                    llmSession?.close()
                    llmSession = null
                    isSessionValid = false
                    _currentResponse.value = ""
                    _currentStats.value = null

                    if (llmInference != null) {
                        if (createSession(model)) {
                            Log.d(TAG, "Session reset successfully - new conversation started")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reset session", e)
                }
            }
        }
    }

    fun cleanup() {
        try {
            stopGeneration()

            llmSession?.close()
            llmInference?.close()
            llmSession = null
            llmInference = null
            currentModel = null
            currentContext = null
            currentBackend = null
            _isInitialized.value = false
            _isGenerating.value = false
            _currentResponse.value = ""
            _currentStats.value = null
            isSessionValid = false
            currentGenerationJob = null
            Log.d(TAG, "Cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup", e)
        }
    }

    private fun estimateTokenCount(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }
}