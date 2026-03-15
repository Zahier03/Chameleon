package com.sotech.chameleon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sotech.chameleon.data.ChatConversation
import com.sotech.chameleon.data.MindMapVersion
import com.sotech.chameleon.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    conversations: List<ChatConversation>,
    savedMindMaps: List<MindMapVersion>,
    savedNotes: List<Note>,
    onNavigateToChat: (String) -> Unit,
    onNavigateToMindMap: () -> Unit,
    onNavigateToNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // RECENT CHATS SECTION
            item {
                Column {
                    ExploreSectionHeader(title = "Recent Chats")
                    Spacer(modifier = Modifier.height(8.dp))

                    if (conversations.isEmpty()) {
                        ExploreEmptyState("No recent chats.")
                    } else {
                        // Fixed: Using lastMessageAt instead of timestamp
                        val recentChats = conversations.sortedByDescending { it.lastMessageAt }.take(5)
                        ExploreListGroup {
                            recentChats.forEachIndexed { index, chat ->
                                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(chat.lastMessageAt))
                                ExploreProjectItem(
                                    title = chat.title,
                                    subtitle = "Active $dateStr",
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    iconBgColor = Color(0xFF2EA043), // GitHub Green
                                    onClick = { onNavigateToChat(chat.id) }
                                )
                                if (index < recentChats.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // SAVED MIND MAPS SECTION
            item {
                Column {
                    ExploreSectionHeader(title = "Ideas & Mind Maps")
                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedMindMaps.isEmpty()) {
                        ExploreEmptyState("No saved mind maps.")
                    } else {
                        val recentMaps = savedMindMaps.sortedByDescending { it.timestamp }.take(5)
                        ExploreListGroup {
                            recentMaps.forEachIndexed { index, map ->
                                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(map.timestamp))
                                ExploreProjectItem(
                                    title = map.title,
                                    subtitle = "Created $dateStr",
                                    icon = Icons.Default.AccountTree,
                                    iconBgColor = Color(0xFF8957E5), // GitHub Purple
                                    onClick = onNavigateToMindMap
                                )
                                if (index < recentMaps.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // SAVED NOTES SECTION
            item {
                Column {
                    ExploreSectionHeader(title = "Saved Notes")
                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedNotes.isEmpty()) {
                        ExploreEmptyState("No saved notes.")
                    } else {
                        val recentNotes = savedNotes.sortedByDescending { it.timestamp }.take(5)
                        ExploreListGroup {
                            recentNotes.forEachIndexed { index, note ->
                                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(note.timestamp))
                                ExploreProjectItem(
                                    title = note.title,
                                    subtitle = "Edited $dateStr",
                                    icon = Icons.Default.EditNote,
                                    iconBgColor = Color(0xFFD29922), // GitHub Yellow
                                    onClick = onNavigateToNotes
                                )
                                if (index < recentNotes.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun ExploreEmptyState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun ExploreListGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ExploreProjectItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Icon Box
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}