package com.sotech.chameleon.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sotech.chameleon.ui.screens.ChatScreen
import com.sotech.chameleon.ui.screens.HomeScreen
import com.sotech.chameleon.ui.screens.ManageScreen
import com.sotech.chameleon.ui.screens.DashboardScreen
import com.sotech.chameleon.ui.screens.DeckScreen
import com.sotech.chameleon.ui.screens.MindMapScreen
import com.sotech.chameleon.ui.screens.ModelSelectorScreen
import com.sotech.chameleon.ui.screens.NotesScreen
import com.sotech.chameleon.ui.screens.CodePlaygroundScreen
import com.sotech.chameleon.ui.screens.SettingsScreen
import com.sotech.chameleon.ui.dialogs.ThemeSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChameleonApp(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val importedModels by viewModel.importedModels.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val currentConversation by viewModel.currentConversation.collectAsState(initial = null)
    val chatMessages by viewModel.chatMessages.collectAsState(initial = emptyList())
    val currentModel by viewModel.currentModel.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val currentInput by viewModel.currentInput.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentResponse by viewModel.currentResponse.collectAsState()
    val initializationProgress by viewModel.initializationProgress.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    val statsSettings by viewModel.statsSettings.collectAsState()
    val statsSummary by viewModel.getStatsSummary().collectAsState(initial = com.sotech.chameleon.data.MessageStatsSummary())
    val themeSettings by viewModel.themeSettings.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val showBottomNav = when (currentRoute) {
        "dashboard", "home", "manage", "settings" -> true
        else -> false
    }

    if (isLandscape && showBottomNav) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                NavigationRailItem(
                    selected = currentRoute == "dashboard",
                    onClick = {
                        if (currentRoute != "dashboard") {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.ViewModule, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationRailItem(
                    selected = currentRoute == "home",
                    onClick = {
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo("dashboard") { inclusive = false }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationRailItem(
                    selected = currentRoute == "manage",
                    onClick = {
                        if (currentRoute != "manage") {
                            navController.navigate("manage") {
                                popUpTo("dashboard") { inclusive = false }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Event, contentDescription = "Manage") },
                    label = { Text("Manage") }
                )
                NavigationRailItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings") {
                                popUpTo("dashboard") { inclusive = false }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }

            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToChat = {
                            navController.navigate("chat")
                        },
                        onNavigateToDeck = {
                            navController.navigate("deck")
                        }
                    )
                }

                composable("deck") {
                    DeckScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        models = importedModels,
                        currentModel = currentModel,
                        isImporting = isImporting,
                        statsSettings = statsSettings,
                        statsSummary = statsSummary,
                        onModelSelect = viewModel::selectModel,
                        onModelDelete = viewModel::deleteModel,
                        onModelImport = viewModel::importModel,
                        onNavigateToChat = {
                            navController.navigate("chat")
                        },
                        onNavigateToMindMap = {
                            navController.navigate("mindmap")
                        },
                        onNavigateToNotes = {
                            navController.navigate("notes")
                        },
                        onNavigateToCode = {
                            navController.navigate("code")
                        },
                        onNavigateToModelManager = {
                            navController.navigate("models")
                        },
                        onNavigateToSettings = {
                            showThemeDialog = true
                        },
                        onStatsSettingsUpdate = viewModel::updateStatsSettings
                    )
                }

                composable("manage") {
                    ManageScreen(
                        viewModel = viewModel
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        themeSettings = themeSettings,
                        onThemeSettingsClick = {
                            showThemeDialog = true
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("mindmap") {
                    MindMapScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onOpenModelSelector = {
                            navController.navigate("models?fromChat=true")
                        }
                    )
                }

                composable("notes") {
                    NotesScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onOpenModelSelector = {
                            navController.navigate("models?fromChat=true")
                        }
                    )
                }

                composable("code") {
                    CodePlaygroundScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("chat") {
                    ChatScreen(
                        messages = chatMessages,
                        conversations = conversations,
                        currentConversation = currentConversation,
                        currentModel = currentModel,
                        modelStatus = modelState.status,
                        modelError = modelState.error,
                        initializationProgress = initializationProgress,
                        isGenerating = isGenerating,
                        currentResponse = currentResponse,
                        currentInput = currentInput,
                        onInputChange = viewModel::updateInput,
                        onSendMessage = viewModel::sendMessage,
                        onStopGeneration = viewModel::stopGeneration,
                        onClearChat = viewModel::clearChat,
                        onOpenModelSelector = {
                            navController.navigate("models?fromChat=true")
                        },
                        onCreateNewConversation = viewModel::createNewConversation,
                        onSelectConversation = viewModel::setCurrentConversation,
                        onDeleteConversation = viewModel::deleteConversation,
                        onDeleteConversations = viewModel::deleteConversations,
                        onRenameConversation = viewModel::updateConversationTitle,
                        onPinConversation = viewModel::pinConversation,
                        onEditMessage = viewModel::editMessageAndRegenerate,
                        onRegenerateFrom = viewModel::deleteMessageAndRegenerate
                    )
                }

                composable(
                    "models?fromChat={fromChat}",
                    arguments = listOf(
                        navArgument("fromChat") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val fromChat = backStackEntry.arguments?.getBoolean("fromChat") ?: false

                    ModelSelectorScreen(
                        models = importedModels,
                        currentModel = currentModel,
                        isImporting = isImporting,
                        importProgress = importProgress,
                        importStatus = importStatus,
                        statsSettings = statsSettings,
                        statsSummary = statsSummary,
                        onModelSelect = { model ->
                            viewModel.selectModel(model)
                            if (fromChat) {
                                navController.popBackStack()
                            } else {
                                navController.navigate("chat") {
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            }
                        },
                        onModelDelete = viewModel::deleteModel,
                        onModelImport = viewModel::importModel,
                        onModelConfigUpdate = viewModel::updateModelConfig,
                        onGeminiModelAdd = viewModel::addGeminiModel,
                        onStatsSettingsUpdate = viewModel::updateStatsSettings,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            modifier = modifier,
            bottomBar = {
                if (showBottomNav) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "dashboard",
                            onClick = {
                                if (currentRoute != "dashboard") {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = false }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.ViewModule, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                if (currentRoute != "home") {
                                    navController.navigate("home") {
                                        popUpTo("dashboard") { inclusive = false }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "manage",
                            onClick = {
                                if (currentRoute != "manage") {
                                    navController.navigate("manage") {
                                        popUpTo("dashboard") { inclusive = false }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.Event, contentDescription = "Manage") },
                            label = { Text("Manage") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                if (currentRoute != "settings") {
                                    navController.navigate("settings") {
                                        popUpTo("dashboard") { inclusive = false }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToChat = {
                            navController.navigate("chat")
                        },
                        onNavigateToDeck = {
                            navController.navigate("deck")
                        }
                    )
                }

                composable("deck") {
                    DeckScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        models = importedModels,
                        currentModel = currentModel,
                        isImporting = isImporting,
                        statsSettings = statsSettings,
                        statsSummary = statsSummary,
                        onModelSelect = viewModel::selectModel,
                        onModelDelete = viewModel::deleteModel,
                        onModelImport = viewModel::importModel,
                        onNavigateToChat = {
                            navController.navigate("chat")
                        },
                        onNavigateToMindMap = {
                            navController.navigate("mindmap")
                        },
                        onNavigateToNotes = {
                            navController.navigate("notes")
                        },
                        onNavigateToCode = {
                            navController.navigate("code")
                        },
                        onNavigateToModelManager = {
                            navController.navigate("models")
                        },
                        onNavigateToSettings = {
                            showThemeDialog = true
                        },
                        onStatsSettingsUpdate = viewModel::updateStatsSettings
                    )
                }

                composable("manage") {
                    ManageScreen(
                        viewModel = viewModel
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        themeSettings = themeSettings,
                        onThemeSettingsClick = {
                            showThemeDialog = true
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("mindmap") {
                    MindMapScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onOpenModelSelector = {
                            navController.navigate("models?fromChat=true")
                        }
                    )
                }

                composable("notes") {
                    NotesScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onOpenModelSelector = {
                            navController.navigate("models?fromChat=true")
                        }
                    )
                }

                composable("code") {
                    CodePlaygroundScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("chat") {
                    ChatScreen(
                        messages = chatMessages,
                        conversations = conversations,
                        currentConversation = currentConversation,
                        currentModel = currentModel,
                        modelStatus = modelState.status,
                        modelError = modelState.error,
                        initializationProgress = initializationProgress,
                        isGenerating = isGenerating,
                        currentResponse = currentResponse,
                        currentInput = currentInput,
                        onInputChange = viewModel::updateInput,
                        onSendMessage = viewModel::sendMessage,
                        onStopGeneration = viewModel::stopGeneration,
                        onClearChat = viewModel::clearChat,
                        onOpenModelSelector = {
                            navController.navigate("models?fromChat=true")
                        },
                        onCreateNewConversation = viewModel::createNewConversation,
                        onSelectConversation = viewModel::setCurrentConversation,
                        onDeleteConversation = viewModel::deleteConversation,
                        onDeleteConversations = viewModel::deleteConversations,
                        onRenameConversation = viewModel::updateConversationTitle,
                        onPinConversation = viewModel::pinConversation,
                        onEditMessage = viewModel::editMessageAndRegenerate,
                        onRegenerateFrom = viewModel::deleteMessageAndRegenerate
                    )
                }

                composable(
                    "models?fromChat={fromChat}",
                    arguments = listOf(
                        navArgument("fromChat") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val fromChat = backStackEntry.arguments?.getBoolean("fromChat") ?: false

                    ModelSelectorScreen(
                        models = importedModels,
                        currentModel = currentModel,
                        isImporting = isImporting,
                        importProgress = importProgress,
                        importStatus = importStatus,
                        statsSettings = statsSettings,
                        statsSummary = statsSummary,
                        onModelSelect = { model ->
                            viewModel.selectModel(model)
                            if (fromChat) {
                                navController.popBackStack()
                            } else {
                                navController.navigate("chat") {
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            }
                        },
                        onModelDelete = viewModel::deleteModel,
                        onModelImport = viewModel::importModel,
                        onModelConfigUpdate = viewModel::updateModelConfig,
                        onGeminiModelAdd = viewModel::addGeminiModel,
                        onStatsSettingsUpdate = viewModel::updateStatsSettings,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSettingsDialog(
            currentSettings = themeSettings,
            onDismiss = {
                showThemeDialog = false
            },
            onSave = { newSettings ->
                viewModel.updateThemeSettings(newSettings)
                showThemeDialog = false
            }
        )
    }
}