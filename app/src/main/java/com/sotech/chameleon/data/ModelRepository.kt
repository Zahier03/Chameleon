package com.sotech.chameleon.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chameleon_prefs")

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val MODELS_KEY = stringPreferencesKey("imported_models")
    private val CONVERSATIONS_KEY = stringPreferencesKey("chat_conversations")
    private val MESSAGES_KEY = stringPreferencesKey("chat_messages")
    private val CURRENT_CONVERSATION_KEY = stringPreferencesKey("current_conversation_id")
    private val STATS_SETTINGS_KEY = stringPreferencesKey("stats_settings")
    private val THEME_SETTINGS_KEY = stringPreferencesKey("theme_settings")
    private val CALENDAR_EVENTS_KEY = stringPreferencesKey("calendar_events")
    private val TIMETABLE_ENTRIES_KEY = stringPreferencesKey("timetable_entries")
    private val MIND_MAP_VERSIONS_KEY = stringPreferencesKey("mind_map_versions")
    private val NOTES_KEY = stringPreferencesKey("saved_notes")

    val importedModels: Flow<List<ImportedModel>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[MODELS_KEY] ?: "[]"
            val type = object : TypeToken<List<ImportedModel>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    val conversations: Flow<List<ChatConversation>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[CONVERSATIONS_KEY] ?: "[]"
            val type = object : TypeToken<List<ChatConversation>>() {}.type
            val convos = gson.fromJson<List<ChatConversation>>(json, type) ?: emptyList()
            if (convos.isEmpty()) {
                listOf(ChatConversation(id = "default", title = "New Chat"))
            } else {
                convos.sortedWith(compareByDescending<ChatConversation> { it.isPinned }.thenByDescending { it.lastMessageAt })
            }
        }

    val currentConversationId: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENT_CONVERSATION_KEY] ?: "default"
        }

    val chatMessages: Flow<List<ChatMessage>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[MESSAGES_KEY] ?: "[]"
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    val calendarEvents: Flow<List<CalendarEvent>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[CALENDAR_EVENTS_KEY] ?: "[]"
            val type = object : TypeToken<List<CalendarEvent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    val timetableEntries: Flow<List<TimetableEntry>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[TIMETABLE_ENTRIES_KEY] ?: "[]"
            val type = object : TypeToken<List<TimetableEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    val savedMindMaps: Flow<List<MindMapVersion>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[MIND_MAP_VERSIONS_KEY] ?: "[]"
            val type = object : TypeToken<List<MindMapVersion>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    val savedNotes: Flow<List<Note>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[NOTES_KEY] ?: "[]"
            val type = object : TypeToken<List<Note>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> =
        chatMessages.map { messages ->
            messages.filter { it.conversationId == conversationId }
                .sortedBy { it.timestamp }
        }

    val statsSettings: Flow<StatsSettings> = context.dataStore.data
        .map { preferences ->
            val json = preferences[STATS_SETTINGS_KEY] ?: "{}"
            gson.fromJson(json, StatsSettings::class.java) ?: StatsSettings()
        }

    val themeSettings: Flow<ThemeSettings> = context.dataStore.data
        .map { preferences ->
            val json = preferences[THEME_SETTINGS_KEY] ?: "{}"
            gson.fromJson(json, ThemeSettings::class.java) ?: ThemeSettings()
        }

    suspend fun createConversation(title: String, modelName: String?): ChatConversation {
        val newConversation = ChatConversation(
            title = title,
            modelName = modelName
        )

        context.dataStore.edit { preferences ->
            val currentConversations = getConversationsFromPrefs(preferences).toMutableList()
            currentConversations.add(0, newConversation)
            preferences[CONVERSATIONS_KEY] = gson.toJson(currentConversations)
        }

        return newConversation
    }

    suspend fun updateConversation(conversation: ChatConversation) {
        context.dataStore.edit { preferences ->
            val currentConversations = getConversationsFromPrefs(preferences).toMutableList()
            val index = currentConversations.indexOfFirst { it.id == conversation.id }
            if (index != -1) {
                currentConversations[index] = conversation
                preferences[CONVERSATIONS_KEY] = gson.toJson(currentConversations)
            }
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        context.dataStore.edit { preferences ->
            val currentConversations = getConversationsFromPrefs(preferences).toMutableList()
            currentConversations.removeAll { it.id == conversationId }
            preferences[CONVERSATIONS_KEY] = gson.toJson(currentConversations)

            val currentMessages = getMessagesFromPrefs(preferences).toMutableList()
            currentMessages.removeAll { it.conversationId == conversationId }
            preferences[MESSAGES_KEY] = gson.toJson(currentMessages)

            if (preferences[CURRENT_CONVERSATION_KEY] == conversationId) {
                preferences[CURRENT_CONVERSATION_KEY] = currentConversations.firstOrNull()?.id ?: "default"
            }
        }
    }

    suspend fun deleteConversations(conversationIds: List<String>) {
        context.dataStore.edit { preferences ->
            val currentConversations = getConversationsFromPrefs(preferences).toMutableList()
            currentConversations.removeAll { it.id in conversationIds }
            preferences[CONVERSATIONS_KEY] = gson.toJson(currentConversations)

            val currentMessages = getMessagesFromPrefs(preferences).toMutableList()
            currentMessages.removeAll { it.conversationId in conversationIds }
            preferences[MESSAGES_KEY] = gson.toJson(currentMessages)

            val currentId = preferences[CURRENT_CONVERSATION_KEY]
            if (currentId in conversationIds) {
                preferences[CURRENT_CONVERSATION_KEY] = currentConversations.firstOrNull()?.id ?: "default"
            }
        }
    }

    suspend fun setCurrentConversation(conversationId: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_CONVERSATION_KEY] = conversationId
        }
    }

    suspend fun updateConversationTitle(conversationId: String, newTitle: String) {
        context.dataStore.edit { preferences ->
            val currentConversations = getConversationsFromPrefs(preferences).toMutableList()
            val index = currentConversations.indexOfFirst { it.id == conversationId }
            if (index != -1) {
                currentConversations[index] = currentConversations[index].copy(title = newTitle)
                preferences[CONVERSATIONS_KEY] = gson.toJson(currentConversations)
            }
        }
    }

    suspend fun pinConversation(conversationId: String, isPinned: Boolean) {
        context.dataStore.edit { preferences ->
            val currentConversations = getConversationsFromPrefs(preferences).toMutableList()
            val index = currentConversations.indexOfFirst { it.id == conversationId }
            if (index != -1) {
                currentConversations[index] = currentConversations[index].copy(isPinned = isPinned)
                preferences[CONVERSATIONS_KEY] = gson.toJson(currentConversations)
            }
        }
    }

    suspend fun saveModel(model: ImportedModel) {
        context.dataStore.edit { preferences ->
            val currentModels = getModelsFromPrefs(preferences).toMutableList()
            currentModels.add(model)
            preferences[MODELS_KEY] = gson.toJson(currentModels)
        }
    }

    suspend fun deleteModel(fileName: String) {
        context.dataStore.edit { preferences ->
            val currentModels = getModelsFromPrefs(preferences).toMutableList()
            currentModels.removeAll { it.fileName == fileName }
            preferences[MODELS_KEY] = gson.toJson(currentModels)
        }
    }

    suspend fun updateModel(model: ImportedModel) {
        context.dataStore.edit { preferences ->
            val currentModels = getModelsFromPrefs(preferences).toMutableList()
            val index = currentModels.indexOfFirst { it.fileName == model.fileName }
            if (index != -1) {
                currentModels[index] = model
                preferences[MODELS_KEY] = gson.toJson(currentModels)
            }
        }
    }

    suspend fun saveMessage(message: ChatMessage) {
        context.dataStore.edit { preferences ->
            val currentMessages = getMessagesFromPrefs(preferences).toMutableList()
            currentMessages.add(message)

            if (currentMessages.size > 1000) {
                val groupedByConversation = currentMessages.groupBy { it.conversationId }
                val trimmedMessages = mutableListOf<ChatMessage>()

                groupedByConversation.forEach { (_, messages) ->
                    trimmedMessages.addAll(messages.takeLast(100))
                }

                preferences[MESSAGES_KEY] = gson.toJson(trimmedMessages)
            } else {
                preferences[MESSAGES_KEY] = gson.toJson(currentMessages)
            }

            val conversations = getConversationsFromPrefs(preferences).toMutableList()
            val conversationIndex = conversations.indexOfFirst { it.id == message.conversationId }
            if (conversationIndex != -1) {
                val conversation = conversations[conversationIndex]
                val messagesInConvo = currentMessages.count { it.conversationId == message.conversationId }

                val updatedTitle = if (conversation.title == "New Chat" && !message.isUser) {
                    message.content.take(30).trim().ifEmpty { "New Chat" }
                } else {
                    conversation.title
                }

                conversations[conversationIndex] = conversation.copy(
                    lastMessageAt = message.timestamp,
                    messageCount = messagesInConvo,
                    title = updatedTitle
                )
                preferences[CONVERSATIONS_KEY] = gson.toJson(conversations)
            }
        }
    }

    suspend fun clearMessages(conversationId: String) {
        context.dataStore.edit { preferences ->
            val currentMessages = getMessagesFromPrefs(preferences).toMutableList()
            currentMessages.removeAll { it.conversationId == conversationId }
            preferences[MESSAGES_KEY] = gson.toJson(currentMessages)

            val conversations = getConversationsFromPrefs(preferences).toMutableList()
            val conversationIndex = conversations.indexOfFirst { it.id == conversationId }
            if (conversationIndex != -1) {
                conversations[conversationIndex] = conversations[conversationIndex].copy(
                    messageCount = 0,
                    lastMessageAt = System.currentTimeMillis()
                )
                preferences[CONVERSATIONS_KEY] = gson.toJson(conversations)
            }
        }
    }

    suspend fun deleteMessagesFrom(timestamp: Long, conversationId: String) {
        context.dataStore.edit { preferences ->
            val currentMessages = getMessagesFromPrefs(preferences).toMutableList()
            currentMessages.removeAll { it.timestamp >= timestamp && it.conversationId == conversationId }
            preferences[MESSAGES_KEY] = gson.toJson(currentMessages)

            val conversations = getConversationsFromPrefs(preferences).toMutableList()
            val conversationIndex = conversations.indexOfFirst { it.id == conversationId }
            if (conversationIndex != -1) {
                val messagesInConvo = currentMessages.count { it.conversationId == conversationId }
                conversations[conversationIndex] = conversations[conversationIndex].copy(
                    messageCount = messagesInConvo
                )
                preferences[CONVERSATIONS_KEY] = gson.toJson(conversations)
            }
        }
    }

    suspend fun updateStatsSettings(settings: StatsSettings) {
        context.dataStore.edit { preferences ->
            preferences[STATS_SETTINGS_KEY] = gson.toJson(settings)
        }
    }

    suspend fun updateThemeSettings(settings: ThemeSettings) {
        context.dataStore.edit { preferences ->
            preferences[THEME_SETTINGS_KEY] = gson.toJson(settings)
        }
    }

    suspend fun saveCalendarEvent(event: CalendarEvent) {
        context.dataStore.edit { preferences ->
            val currentEvents = getCalendarEventsFromPrefs(preferences).toMutableList()
            currentEvents.add(event)
            preferences[CALENDAR_EVENTS_KEY] = gson.toJson(currentEvents)
        }
    }

    suspend fun deleteCalendarEvent(eventId: String) {
        context.dataStore.edit { preferences ->
            val currentEvents = getCalendarEventsFromPrefs(preferences).toMutableList()
            currentEvents.removeAll { it.id == eventId }
            preferences[CALENDAR_EVENTS_KEY] = gson.toJson(currentEvents)
        }
    }

    suspend fun saveTimetableEntry(entry: TimetableEntry) {
        context.dataStore.edit { preferences ->
            val currentEntries = getTimetableEntriesFromPrefs(preferences).toMutableList()
            currentEntries.add(entry)
            preferences[TIMETABLE_ENTRIES_KEY] = gson.toJson(currentEntries)
        }
    }

    suspend fun deleteTimetableEntry(entryId: String) {
        context.dataStore.edit { preferences ->
            val currentEntries = getTimetableEntriesFromPrefs(preferences).toMutableList()
            currentEntries.removeAll { it.id == entryId }
            preferences[TIMETABLE_ENTRIES_KEY] = gson.toJson(currentEntries)
        }
    }

    suspend fun saveMindMapVersion(version: MindMapVersion) {
        context.dataStore.edit { preferences ->
            val current = getMindMapVersionsFromPrefs(preferences).toMutableList()
            current.add(version)
            preferences[MIND_MAP_VERSIONS_KEY] = gson.toJson(current)
        }
    }

    suspend fun deleteMindMapVersion(id: String) {
        context.dataStore.edit { preferences ->
            val current = getMindMapVersionsFromPrefs(preferences).toMutableList()
            current.removeAll { it.id == id }
            preferences[MIND_MAP_VERSIONS_KEY] = gson.toJson(current)
        }
    }

    suspend fun saveNote(note: Note) {
        context.dataStore.edit { preferences ->
            val currentNotes = getNotesFromPrefs(preferences).toMutableList()
            val existingIndex = currentNotes.indexOfFirst { it.id == note.id }

            if (existingIndex != -1) {
                currentNotes[existingIndex] = note
            } else {
                currentNotes.add(0, note)
            }
            preferences[NOTES_KEY] = gson.toJson(currentNotes)
        }
    }

    suspend fun deleteNote(id: String) {
        context.dataStore.edit { preferences ->
            val currentNotes = getNotesFromPrefs(preferences).toMutableList()
            currentNotes.removeAll { it.id == id }
            preferences[NOTES_KEY] = gson.toJson(currentNotes)
        }
    }

    private fun getModelsFromPrefs(preferences: Preferences): List<ImportedModel> {
        val json = preferences[MODELS_KEY] ?: "[]"
        val type = object : TypeToken<List<ImportedModel>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun getConversationsFromPrefs(preferences: Preferences): List<ChatConversation> {
        val json = preferences[CONVERSATIONS_KEY] ?: "[]"
        val type = object : TypeToken<List<ChatConversation>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun getMessagesFromPrefs(preferences: Preferences): List<ChatMessage> {
        val json = preferences[MESSAGES_KEY] ?: "[]"
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun getCalendarEventsFromPrefs(preferences: Preferences): List<CalendarEvent> {
        val json = preferences[CALENDAR_EVENTS_KEY] ?: "[]"
        val type = object : TypeToken<List<CalendarEvent>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun getTimetableEntriesFromPrefs(preferences: Preferences): List<TimetableEntry> {
        val json = preferences[TIMETABLE_ENTRIES_KEY] ?: "[]"
        val type = object : TypeToken<List<TimetableEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun getMindMapVersionsFromPrefs(preferences: Preferences): List<MindMapVersion> {
        val json = preferences[MIND_MAP_VERSIONS_KEY] ?: "[]"
        val type = object : TypeToken<List<MindMapVersion>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun getNotesFromPrefs(preferences: Preferences): List<Note> {
        val json = preferences[NOTES_KEY] ?: "[]"
        val type = object : TypeToken<List<Note>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getModelFile(model: ImportedModel): File {
        return File(model.filePath)
    }
}