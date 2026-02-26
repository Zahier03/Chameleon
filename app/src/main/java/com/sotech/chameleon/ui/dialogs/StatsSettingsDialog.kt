package com.sotech.chameleon.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sotech.chameleon.data.StatsSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSettingsDialog(
    currentSettings: StatsSettings,
    onDismiss: () -> Unit,
    onSave: (StatsSettings) -> Unit
) {
    var showStats by remember { mutableStateOf(currentSettings.showStats) }
    var expandDefault by remember { mutableStateOf(currentSettings.expandStatsDefault) }
    var showDetailed by remember { mutableStateOf(currentSettings.showDetailedStats) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "STATS SETTINGS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Show Stats Toggle
                SettingItem(
                    title = "Show Statistics",
                    description = "Display performance metrics",
                    checked = showStats,
                    onCheckedChange = { showStats = it }
                )

                // Auto-expand Toggle
                SettingItem(
                    title = "Auto-expand Stats",
                    description = "Automatically show detailed stats",
                    checked = expandDefault,
                    onCheckedChange = { expandDefault = it },
                    enabled = showStats
                )

                // Detailed Metrics Toggle
                SettingItem(
                    title = "Detailed Metrics",
                    description = "Show token counts and speed breakdowns",
                    checked = showDetailed,
                    onCheckedChange = { showDetailed = it },
                    enabled = showStats
                )

                // Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "METRICS EXPLAINED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MetricExplanation(
                                metric = "Time to First Token",
                                description = "Response speed"
                            )
                            MetricExplanation(
                                metric = "Prefill Speed",
                                description = "Context processing rate"
                            )
                            MetricExplanation(
                                metric = "Decode Speed",
                                description = "Token generation rate"
                            )
                            MetricExplanation(
                                metric = "Total Latency",
                                description = "Complete response time"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    onSave(
                        StatsSettings(
                            showStats = showStats,
                            expandStatsDefault = expandDefault,
                            showDetailedStats = showDetailed
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
private fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun MetricExplanation(
    metric: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$metric:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}