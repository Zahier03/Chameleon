package com.sotech.chameleon.ui.dialogs

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sotech.chameleon.R
import com.sotech.chameleon.data.AppColorScheme
import com.sotech.chameleon.data.ThemeMode
import com.sotech.chameleon.data.ThemeSettings
import com.sotech.chameleon.ui.theme.textScalePresets
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsDialog(
    currentSettings: ThemeSettings,
    onDismiss: () -> Unit,
    onSave: (ThemeSettings) -> Unit
) {
    var textScale by remember { mutableStateOf(currentSettings.textScale) }
    var useDynamicColors by remember { mutableStateOf(currentSettings.useDynamicColors) }
    var isDarkMode by remember { mutableStateOf(currentSettings.isDarkMode) }
    var colorScheme by remember { mutableStateOf(currentSettings.colorScheme) }
    var selectedPreset by remember {
        mutableStateOf(
            textScalePresets.find { it.scale == currentSettings.textScale }
                ?: textScalePresets.last()
        )
    }
    var isCustomScale by remember {
        mutableStateOf(textScalePresets.none { it.scale == currentSettings.textScale })
    }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.98f else 0.95f)
                .fillMaxHeight(if (isLandscape) 0.95f else 0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape((24 * scaleFactor).dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = (24 * scaleFactor).dp, topEnd = (24 * scaleFactor).dp))
                        .background(Color(0xFF6A1B9A))
                        .padding((24 * scaleFactor).dp)
                ) {
                    Text(
                        text = "THEME",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = barberChopFont,
                            fontSize = if (isLandscape) (40 * scaleFactor).sp else (60 * scaleFactor).sp,
                            lineHeight = if (isLandscape) (50 * scaleFactor).sp else (70 * scaleFactor).sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFBA68C8).copy(alpha = 0.3f),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SETTINGS",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = barberChopFont,
                                    fontSize = if (isLandscape) (20 * scaleFactor).sp else (28 * scaleFactor).sp,
                                    lineHeight = if (isLandscape) (26 * scaleFactor).sp else (36 * scaleFactor).sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size((40 * scaleFactor).dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size((24 * scaleFactor).dp),
                                    tint = Color.White
                                )
                            }
                        }
                        Text(
                            text = "Customize your experience",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (14 * scaleFactor).sp
                            ),
                            color = Color(0xFFE1BEE7)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues((24 * scaleFactor).dp),
                    verticalArrangement = Arrangement.spacedBy((20 * scaleFactor).dp)
                ) {
                    item {
                        AnimatedSection(
                            visible = animationStarted,
                            delayMillis = 0,
                            scaleFactor = scaleFactor
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape((16 * scaleFactor).dp))
                                    .background(Color(0xFF0288D1))
                            ) {
                                Text(
                                    text = "SIZE",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = if (isLandscape) (50 * scaleFactor).sp else (80 * scaleFactor).sp,
                                        lineHeight = if (isLandscape) (60 * scaleFactor).sp else (90 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4FC3F7).copy(alpha = 0.3f),
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding((20 * scaleFactor).dp),
                                    verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                    ) {
                                        Icon(
                                            Icons.Default.TextFields,
                                            contentDescription = null,
                                            modifier = Modifier.size((24 * scaleFactor).dp),
                                            tint = Color.White
                                        )
                                        Text(
                                            text = "TEXT SIZE",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = barberChopFont,
                                                fontSize = (16 * scaleFactor).sp,
                                                lineHeight = (24 * scaleFactor).sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                    ) {
                                        textScalePresets.dropLast(1).forEach { preset ->
                                            TextScaleOption(
                                                preset = preset,
                                                isSelected = selectedPreset.scale == preset.scale && !isCustomScale,
                                                onClick = {
                                                    selectedPreset = preset
                                                    textScale = preset.scale
                                                    isCustomScale = false
                                                },
                                                scaleFactor = scaleFactor,
                                                barberChopFont = barberChopFont
                                            )
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCustomScale) {
                                                    Color.White.copy(alpha = 0.2f)
                                                } else {
                                                    Color.White.copy(alpha = 0.1f)
                                                }
                                            ),
                                            shape = RoundedCornerShape((12 * scaleFactor).dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding((16 * scaleFactor).dp),
                                                verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { isCustomScale = true },
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "Custom",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontSize = (14 * scaleFactor).sp,
                                                                fontFamily = barberChopFont
                                                            ),
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "Set your own scale",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontSize = (12 * scaleFactor).sp
                                                            ),
                                                            color = Color.White.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                    RadioButton(
                                                        selected = isCustomScale,
                                                        onClick = { isCustomScale = true },
                                                        modifier = Modifier.size((24 * scaleFactor).dp),
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = Color.White,
                                                            unselectedColor = Color.White.copy(alpha = 0.6f)
                                                        )
                                                    )
                                                }

                                                if (isCustomScale) {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                                                    ) {
                                                        Slider(
                                                            value = textScale,
                                                            onValueChange = { textScale = it },
                                                            valueRange = 0.5f..2.5f,
                                                            steps = 39,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = Color.White,
                                                                activeTrackColor = Color.White,
                                                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                                            )
                                                        )
                                                        Text(
                                                            text = "Scale: ${String.format("%.2f", textScale)}x",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontSize = (12 * scaleFactor).sp,
                                                                fontFamily = barberChopFont
                                                            ),
                                                            color = Color.White,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape((8 * scaleFactor).dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding((12 * scaleFactor).dp),
                                            verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                                        ) {
                                            Text(
                                                text = "PREVIEW",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = (10 * textScale * scaleFactor).sp,
                                                    letterSpacing = (1 * scaleFactor).sp,
                                                    fontFamily = barberChopFont
                                                ),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "This is how text will appear",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = (14 * textScale * scaleFactor).sp
                                                ),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Adjust the scale for better readability",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = (12 * textScale * scaleFactor).sp
                                                ),
                                                color = Color.White.copy(alpha = 0.8f)
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
                            delayMillis = 100,
                            scaleFactor = scaleFactor
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape((16 * scaleFactor).dp))
                                    .background(Color(0xFF5E35B1))
                            ) {
                                Text(
                                    text = "MODE",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = if (isLandscape) (50 * scaleFactor).sp else (80 * scaleFactor).sp,
                                        lineHeight = if (isLandscape) (60 * scaleFactor).sp else (90 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF9575CD).copy(alpha = 0.3f),
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding((20 * scaleFactor).dp),
                                    verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Brightness6,
                                            contentDescription = null,
                                            modifier = Modifier.size((24 * scaleFactor).dp),
                                            tint = Color.White
                                        )
                                        Text(
                                            text = "APPEARANCE",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = barberChopFont,
                                                fontSize = (16 * scaleFactor).sp,
                                                lineHeight = (24 * scaleFactor).sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                    ) {
                                        ThemeMode.values().forEach { mode ->
                                            FilterChip(
                                                selected = isDarkMode == mode,
                                                onClick = { isDarkMode = mode },
                                                label = {
                                                    Text(
                                                        text = when (mode) {
                                                            ThemeMode.LIGHT -> "Light"
                                                            ThemeMode.DARK -> "Dark"
                                                            ThemeMode.SYSTEM -> "System"
                                                        },
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontSize = (12 * scaleFactor).sp,
                                                            fontFamily = barberChopFont
                                                        ),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                leadingIcon = if (isDarkMode == mode) {
                                                    {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size((16 * scaleFactor).dp)
                                                        )
                                                    }
                                                } else null,
                                                modifier = Modifier.weight(1f),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color.White.copy(alpha = 0.3f),
                                                    containerColor = Color.White.copy(alpha = 0.1f),
                                                    selectedLabelColor = Color.White,
                                                    labelColor = Color.White.copy(alpha = 0.7f)
                                                )
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
                            delayMillis = 200,
                            scaleFactor = scaleFactor
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape((16 * scaleFactor).dp))
                                    .background(Color(0xFFD32F2F))
                            ) {
                                Text(
                                    text = "COLOR",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = barberChopFont,
                                        fontSize = if (isLandscape) (50 * scaleFactor).sp else (80 * scaleFactor).sp,
                                        lineHeight = if (isLandscape) (60 * scaleFactor).sp else (90 * scaleFactor).sp
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE57373).copy(alpha = 0.3f),
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding((20 * scaleFactor).dp),
                                    verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Palette,
                                            contentDescription = null,
                                            modifier = Modifier.size((24 * scaleFactor).dp),
                                            tint = Color.White
                                        )
                                        Text(
                                            text = "COLOR SCHEME",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = barberChopFont,
                                                fontSize = (16 * scaleFactor).sp,
                                                lineHeight = (24 * scaleFactor).sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape((8 * scaleFactor).dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding((12 * scaleFactor).dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Dynamic Colors",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = (14 * scaleFactor).sp,
                                                        fontFamily = barberChopFont
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Use colors from wallpaper (Android 12+)",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = (12 * scaleFactor).sp
                                                    ),
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                            Switch(
                                                checked = useDynamicColors,
                                                onCheckedChange = { useDynamicColors = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color.White.copy(alpha = 0.5f),
                                                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                                                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                                )
                                            )
                                        }
                                    }

                                    if (!useDynamicColors) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                                        ) {
                                            items(AppColorScheme.values().toList()) { scheme ->
                                                ColorSchemeCard(
                                                    scheme = scheme,
                                                    isSelected = colorScheme == scheme,
                                                    onClick = { colorScheme = scheme },
                                                    scaleFactor = scaleFactor,
                                                    barberChopFont = barberChopFont
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        AnimatedSection(
                            visible = animationStarted,
                            delayMillis = 300,
                            scaleFactor = scaleFactor
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape((12 * scaleFactor).dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding((16 * scaleFactor).dp),
                                    horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size((20 * scaleFactor).dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
                                    ) {
                                        Text(
                                            text = "ACCESSIBILITY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = (11 * scaleFactor).sp,
                                                fontFamily = barberChopFont
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Larger text sizes improve readability for all ages",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = (12 * scaleFactor).sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding((24 * scaleFactor).dp),
                    horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape((12 * scaleFactor).dp)
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = (14 * scaleFactor).sp
                            )
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            onSave(
                                ThemeSettings(
                                    textScale = textScale,
                                    useDynamicColors = useDynamicColors,
                                    isDarkMode = isDarkMode,
                                    colorScheme = colorScheme
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF6A1B9A)
                        ),
                        shape = RoundedCornerShape((12 * scaleFactor).dp)
                    ) {
                        Text(
                            "Apply",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = (14 * scaleFactor).sp,
                                fontFamily = barberChopFont
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextScaleOption(
    preset: com.sotech.chameleon.ui.theme.TextScalePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color.White.copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scaleFactor).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (14 * scaleFactor).sp,
                        fontFamily = barberChopFont
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (12 * scaleFactor).sp
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.size((24 * scaleFactor).dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.White,
                    unselectedColor = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
private fun ColorSchemeCard(
    scheme: AppColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    scaleFactor: Float,
    barberChopFont: FontFamily
) {
    Card(
        modifier = Modifier
            .width((100 * scaleFactor).dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color.White.copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape((12 * scaleFactor).dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke((2 * scaleFactor).dp, Color.White)
        } else null
    ) {
        Column(
            modifier = Modifier.padding((12 * scaleFactor).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)
            ) {
                val colors = when (scheme) {
                    AppColorScheme.DEFAULT -> listOf(Color(0xFF0B57D0), Color(0xFFA8C7FA))
                    AppColorScheme.OCEAN -> listOf(Color(0xFF006493), Color(0xFF8FCDFF))
                    AppColorScheme.FOREST -> listOf(Color(0xFF2E7D32), Color(0xFF81C784))
                    AppColorScheme.SUNSET -> listOf(Color(0xFFE65100), Color(0xFFFFAB78))
                    AppColorScheme.MONOCHROME -> listOf(Color(0xFF424242), Color(0xFFBDBDBD))
                }
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size((24 * scaleFactor).dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
            Text(
                text = when (scheme) {
                    AppColorScheme.DEFAULT -> "Default"
                    AppColorScheme.OCEAN -> "Ocean"
                    AppColorScheme.FOREST -> "Forest"
                    AppColorScheme.SUNSET -> "Sunset"
                    AppColorScheme.MONOCHROME -> "Mono"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (11 * scaleFactor).sp,
                    fontFamily = barberChopFont
                ),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.White
            )
        }
    }
}

@Composable
fun AnimatedSection(
    visible: Boolean,
    delayMillis: Long,
    scaleFactor: Float,
    content: @Composable () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = delayMillis.toInt(), easing = FastOutSlowInEasing),
        label = "section_alpha"
    )

    val animatedOffset by animateFloatAsState(
        targetValue = if (visible) 0f else (20 * scaleFactor),
        animationSpec = tween(600, delayMillis = delayMillis.toInt(), easing = FastOutSlowInEasing),
        label = "section_offset"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = animatedAlpha
            translationY = animatedOffset
        }
    ) {
        content()
    }
}