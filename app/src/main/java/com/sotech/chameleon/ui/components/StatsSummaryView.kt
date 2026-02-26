package com.sotech.chameleon.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sotech.chameleon.R
import com.sotech.chameleon.data.MessageStatsSummary
import com.sotech.chameleon.ui.theme.customColors
import com.sotech.chameleon.utils.formatNumber
import com.sotech.chameleon.utils.formatLatency
import com.sotech.chameleon.utils.formatSpeed

@Composable
fun StatsSummaryView(
    summary: MessageStatsSummary,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val barberChopFont = remember {
        try {
            FontFamily(Font(R.font.barberchop, FontWeight.Bold))
        } catch (e: Exception) {
            FontFamily.Default
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isLandscape) {
                    Modifier.fillMaxHeight(0.9f)
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = (8 * scaleFactor).dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape((24 * scaleFactor).dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = (24 * scaleFactor).dp, topEnd = (24 * scaleFactor).dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1976D2),
                                Color(0xFF7B1FA2)
                            )
                        )
                    )
                    .padding(
                        horizontal = if (isLandscape) (16 * scaleFactor).dp else (24 * scaleFactor).dp,
                        vertical = if (isLandscape) (12 * scaleFactor).dp else (24 * scaleFactor).dp
                    )
            ) {
                Text(
                    text = "STATS",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = if (isLandscape) (36 * scaleFactor).sp else (60 * scaleFactor).sp,
                        lineHeight = if (isLandscape) (44 * scaleFactor).sp else (70 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                    ) {
                        Text(
                            text = "PERFORMANCE",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = barberChopFont,
                                fontSize = if (isLandscape) (18 * scaleFactor).sp else (24 * scaleFactor).sp,
                                lineHeight = if (isLandscape) (24 * scaleFactor).sp else (32 * scaleFactor).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Summary Report",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (isLandscape) (12 * scaleFactor).sp else (14 * scaleFactor).sp
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(if (isLandscape) (32 * scaleFactor).dp else (40 * scaleFactor).dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(if (isLandscape) (20 * scaleFactor).dp else (24 * scaleFactor).dp),
                            tint = Color.White
                        )
                    }
                }
            }

            if (isLandscape) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = (16 * scaleFactor).dp,
                        vertical = (12 * scaleFactor).dp
                    ),
                    verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                        ) {
                            SummaryStatCard(
                                icon = Icons.AutoMirrored.Filled.Message,
                                value = summary.totalMessages.toString(),
                                label = "Messages",
                                color = Color(0xFF1976D2),
                                modifier = Modifier.weight(1f),
                                scaleFactor = scaleFactor,
                                barberChopFont = barberChopFont,
                                isLandscape = isLandscape
                            )

                            SummaryStatCard(
                                icon = Icons.Default.Numbers,
                                value = formatNumber(summary.totalTokens),
                                label = "Tokens",
                                color = Color(0xFF7B1FA2),
                                modifier = Modifier.weight(1f),
                                scaleFactor = scaleFactor,
                                barberChopFont = barberChopFont,
                                isLandscape = isLandscape
                            )
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape((16 * scaleFactor).dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding((16 * scaleFactor).dp),
                                verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
                            ) {
                                Text(
                                    text = "METRICS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = (12 * scaleFactor).sp,
                                        letterSpacing = (1 * scaleFactor).sp,
                                        fontFamily = barberChopFont
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                MetricRow(
                                    label = "First Token",
                                    value = formatLatency(summary.avgTimeToFirstToken),
                                    icon = Icons.Default.Timer,
                                    progress = calculateProgress(summary.avgTimeToFirstToken, 2f, inverse = true),
                                    scaleFactor = scaleFactor,
                                    isLandscape = isLandscape
                                )

                                MetricRow(
                                    label = "Prefill Speed",
                                    value = "${formatSpeed(summary.avgPrefillSpeed)} tok/s",
                                    icon = Icons.Default.Speed,
                                    progress = calculateProgress(summary.avgPrefillSpeed, 100f),
                                    scaleFactor = scaleFactor,
                                    isLandscape = isLandscape
                                )

                                MetricRow(
                                    label = "Decode Speed",
                                    value = "${formatSpeed(summary.avgDecodeSpeed)} tok/s",
                                    icon = Icons.Default.FlashOn,
                                    progress = calculateProgress(summary.avgDecodeSpeed, 100f),
                                    scaleFactor = scaleFactor,
                                    isLandscape = isLandscape
                                )

                                MetricRow(
                                    label = "Avg Latency",
                                    value = formatLatency(summary.avgLatency),
                                    icon = Icons.Default.Schedule,
                                    progress = calculateProgress(summary.avgLatency, 10f, inverse = true),
                                    scaleFactor = scaleFactor,
                                    isLandscape = isLandscape
                                )
                            }
                        }
                    }

                    if (summary.totalMessages > 0) {
                        item {
                            PerformanceGrade(
                                decodeSpeed = summary.avgDecodeSpeed,
                                scaleFactor = scaleFactor,
                                barberChopFont = barberChopFont,
                                isLandscape = isLandscape
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding((24 * scaleFactor).dp),
                    verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        SummaryStatCard(
                            icon = Icons.AutoMirrored.Filled.Message,
                            value = summary.totalMessages.toString(),
                            label = "Messages",
                            color = Color(0xFF1976D2),
                            modifier = Modifier.weight(1f),
                            scaleFactor = scaleFactor,
                            barberChopFont = barberChopFont,
                            isLandscape = isLandscape
                        )

                        SummaryStatCard(
                            icon = Icons.Default.Numbers,
                            value = formatNumber(summary.totalTokens),
                            label = "Tokens",
                            color = Color(0xFF7B1FA2),
                            modifier = Modifier.weight(1f),
                            scaleFactor = scaleFactor,
                            barberChopFont = barberChopFont,
                            isLandscape = isLandscape
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape((16 * scaleFactor).dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding((20 * scaleFactor).dp),
                            verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                        ) {
                            Text(
                                text = "METRICS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = (12 * scaleFactor).sp,
                                    letterSpacing = (1 * scaleFactor).sp,
                                    fontFamily = barberChopFont
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            MetricRow(
                                label = "First Token",
                                value = formatLatency(summary.avgTimeToFirstToken),
                                icon = Icons.Default.Timer,
                                progress = calculateProgress(summary.avgTimeToFirstToken, 2f, inverse = true),
                                scaleFactor = scaleFactor,
                                isLandscape = isLandscape
                            )

                            MetricRow(
                                label = "Prefill Speed",
                                value = "${formatSpeed(summary.avgPrefillSpeed)} tok/s",
                                icon = Icons.Default.Speed,
                                progress = calculateProgress(summary.avgPrefillSpeed, 100f),
                                scaleFactor = scaleFactor,
                                isLandscape = isLandscape
                            )

                            MetricRow(
                                label = "Decode Speed",
                                value = "${formatSpeed(summary.avgDecodeSpeed)} tok/s",
                                icon = Icons.Default.FlashOn,
                                progress = calculateProgress(summary.avgDecodeSpeed, 100f),
                                scaleFactor = scaleFactor,
                                isLandscape = isLandscape
                            )

                            MetricRow(
                                label = "Avg Latency",
                                value = formatLatency(summary.avgLatency),
                                icon = Icons.Default.Schedule,
                                progress = calculateProgress(summary.avgLatency, 10f, inverse = true),
                                scaleFactor = scaleFactor,
                                isLandscape = isLandscape
                            )
                        }
                    }

                    if (summary.totalMessages > 0) {
                        PerformanceGrade(
                            decodeSpeed = summary.avgDecodeSpeed,
                            scaleFactor = scaleFactor,
                            barberChopFont = barberChopFont,
                            isLandscape = isLandscape
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape((16 * scaleFactor).dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) (12 * scaleFactor).dp else (16 * scaleFactor).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) (6 * scaleFactor).dp else (8 * scaleFactor).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isLandscape) (36 * scaleFactor).dp else (48 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (isLandscape) (20 * scaleFactor).dp else (28 * scaleFactor).dp),
                    tint = color
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = barberChopFont,
                    fontSize = if (isLandscape) (24 * scaleFactor).sp else (32 * scaleFactor).sp,
                    lineHeight = if (isLandscape) (30 * scaleFactor).sp else (40 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = if (isLandscape) (10 * scaleFactor).sp else (12 * scaleFactor).sp,
                    fontFamily = barberChopFont
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: Float,
    scaleFactor: Float,
    isLandscape: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) (6 * scaleFactor).dp else (8 * scaleFactor).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) (6 * scaleFactor).dp else (8 * scaleFactor).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (isLandscape) (16 * scaleFactor).dp else (20 * scaleFactor).dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (isLandscape) (12 * scaleFactor).sp else (14 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isLandscape) (12 * scaleFactor).sp else (14 * scaleFactor).sp
                ),
                fontWeight = FontWeight.Bold,
                color = getProgressColor(progress)
            )
        }

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) (4 * scaleFactor).dp else (6 * scaleFactor).dp)
                .clip(RoundedCornerShape(if (isLandscape) (2 * scaleFactor).dp else (3 * scaleFactor).dp)),
            color = getProgressColor(progress),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun PerformanceGrade(
    decodeSpeed: Float,
    scaleFactor: Float,
    barberChopFont: FontFamily,
    isLandscape: Boolean
) {
    val (grade, gradeColor, gradeText) = when {
        decodeSpeed >= 50 -> Triple("A+", Color(0xFF4CAF50), "Excellent")
        decodeSpeed >= 40 -> Triple("A", Color(0xFF66BB6A), "Great")
        decodeSpeed >= 30 -> Triple("B+", Color(0xFF81C784), "Good")
        decodeSpeed >= 20 -> Triple("B", Color(0xFFFFC107), "Average")
        decodeSpeed >= 10 -> Triple("C", Color(0xFFFF9800), "Below Average")
        else -> Triple("D", Color(0xFFFF5722), "Poor")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = gradeColor.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape((16 * scaleFactor).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) (16 * scaleFactor).dp else (20 * scaleFactor).dp),
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) (12 * scaleFactor).dp else (16 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isLandscape) (48 * scaleFactor).dp else (64 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grade,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = barberChopFont,
                        fontSize = if (isLandscape) (24 * scaleFactor).sp else (32 * scaleFactor).sp,
                        lineHeight = if (isLandscape) (30 * scaleFactor).sp else (40 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = gradeColor
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
            ) {
                Text(
                    text = "GRADE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = if (isLandscape) (10 * scaleFactor).sp else (11 * scaleFactor).sp,
                        letterSpacing = (1 * scaleFactor).sp,
                        fontFamily = barberChopFont
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = gradeText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = barberChopFont,
                        fontSize = if (isLandscape) (20 * scaleFactor).sp else (24 * scaleFactor).sp,
                        lineHeight = if (isLandscape) (26 * scaleFactor).sp else (32 * scaleFactor).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = gradeColor
                )
            }
        }
    }
}

private fun calculateProgress(value: Float, max: Float, inverse: Boolean = false): Float {
    val progress = if (max > 0) (value / max).coerceIn(0f, 1f) else 0f
    return if (inverse) 1f - progress else progress
}

private fun getProgressColor(progress: Float): Color {
    return when {
        progress >= 0.7f -> Color(0xFF4CAF50)
        progress >= 0.4f -> Color(0xFFFFC107)
        else -> Color(0xFFFF5722)
    }
}