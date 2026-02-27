package com.sotech.chameleon.data

import kotlinx.serialization.Serializable

@Serializable
data class ImportedModel(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val displayName: String,
    val maxTokens: Int = 2048,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val temperature: Float = 0.7f,
    val supportImage: Boolean = false,
    val supportAudio: Boolean = false,
    val useGpu: Boolean = true,
    val ragEnabled: Boolean = false,
    val ragContextFiles: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isApiModel: Boolean = false,
    val apiKey: String = "",
    val modelCode: String = "",
    val modelType: GeminiModelType = GeminiModelType.CHAT
)

enum class GeminiModelType {
    CHAT,
    CHAT_IMAGE,
    CHAT_VIDEO,
    CHAT_AUDIO,
    CHAT_PDF,
    CHAT_MULTIMODAL
}

data class GeminiModel(
    val name: String,
    val code: String,
    val description: String,
    val type: GeminiModelType,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val supportedInputs: List<String>,
    val features: List<String>
)

object GeminiModels {
    val models = listOf(
        GeminiModel(
            name = "Gemini 2.5 Pro",
            code = "gemini-2.5-pro",
            description = "Most powerful multimodal model with advanced reasoning",
            type = GeminiModelType.CHAT_MULTIMODAL,
            inputTokenLimit = 1048576,
            outputTokenLimit = 65536,
            supportedInputs = listOf("Text", "Images", "Video", "Audio", "PDF"),
            features = listOf("Thinking", "Function calling", "Code execution", "Search grounding")
        ),
        GeminiModel(
            name = "Gemini 2.5 Flash",
            code = "gemini-2.5-flash",
            description = "Fast and intelligent with thinking capabilities",
            type = GeminiModelType.CHAT_MULTIMODAL,
            inputTokenLimit = 1048576,
            outputTokenLimit = 65536,
            supportedInputs = listOf("Text", "Images", "Video", "Audio"),
            features = listOf("Thinking", "Function calling", "Code execution")
        ),
        GeminiModel(
            name = "Gemini 2.5 Flash-Lite",
            code = "gemini-2.5-flash-lite",
            description = "Ultra fast and cost-efficient",
            type = GeminiModelType.CHAT_MULTIMODAL,
            inputTokenLimit = 1048576,
            outputTokenLimit = 65536,
            supportedInputs = listOf("Text", "Images", "Video", "Audio", "PDF"),
            features = listOf("Thinking", "Code execution", "Search grounding")
        )
    )
}

data class ChatConversation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val isPinned: Boolean = false,
    val modelName: String? = null
)

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val latencyMs: Float = -1f,
    val stats: MessageStats? = null,
    val usedRAG: Boolean = false,
    val retrievedContext: String? = null,
    val conversationId: String = "default",
    val images: List<String> = emptyList()
)

data class MessageStats(
    val timeToFirstToken: Float = 0f,
    val prefillSpeed: Float = 0f,
    val decodeSpeed: Float = 0f,
    val totalLatency: Float = 0f,
    val tokenCount: Int = 0,
    val prefillTokens: Int = 0,
    val decodeTokens: Int = 0,
    val accelerator: String = ""
)

data class ModelConfig(
    val maxTokens: Int = 2048,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val temperature: Float = 0.7f,
    val useGpu: Boolean = true,
    val ragEnabled: Boolean = false
)

enum class ModelStatus {
    NOT_INITIALIZED,
    INITIALIZING,
    READY,
    ERROR
}

data class ModelState(
    val model: ImportedModel? = null,
    val status: ModelStatus = ModelStatus.NOT_INITIALIZED,
    val error: String? = null,
    val ragStatus: RAGStatus = RAGStatus.NOT_INITIALIZED
)

enum class RAGStatus {
    NOT_INITIALIZED,
    INITIALIZING,
    READY,
    ERROR,
    DISABLED
}

data class StatsSettings(
    val showStats: Boolean = true,
    val expandStatsDefault: Boolean = false,
    val showDetailedStats: Boolean = true
)

data class MessageStatsSummary(
    val totalMessages: Int = 0,
    val avgTimeToFirstToken: Float = 0f,
    val avgPrefillSpeed: Float = 0f,
    val avgDecodeSpeed: Float = 0f,
    val avgLatency: Float = 0f,
    val totalTokens: Int = 0,
    val ragUsageCount: Int = 0
)

data class ThemeSettings(
    val textScale: Float = 1.0f,
    val useDynamicColors: Boolean = false,
    val isDarkMode: ThemeMode = ThemeMode.SYSTEM,
    val colorScheme: AppColorScheme = AppColorScheme.DEFAULT
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class AppColorScheme {
    DEFAULT,
    OCEAN,
    FOREST,
    SUNSET,
    MONOCHROME
}

data class InfoCard(
    val title: String,
    val description: String,
    val icon: String,
    val actionText: String? = null,
    val actionUrl: String? = null
)

data class RAGContext(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val source: RAGContextSource,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RAGContextSource {
    FILE,
    USER_INPUT,
    WEB,
    DOCUMENT
}

data class RAGSettings(
    val enabled: Boolean = false,
    val numRetrievalDocs: Int = 3,
    val minScore: Float = 0.0f,
    val autoLoadContext: Boolean = true,
    val contextSources: List<RAGContext> = emptyList()
)

data class CalendarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dateTime: Long,
    val hour: Int = 0,
    val minute: Int = 0,
    val color: Long = 0xFF1976D2L
)

data class TimetableEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val color: Long = 0xFF388E3CL
)

data class MindMapVersion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val alignment: Int = 3,
    val fontSize: Float = 18f
)

data class TextMarker(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startIndex: Int,
    val endIndex: Int,
    val originalText: String
)