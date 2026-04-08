package com.kumbidi.androvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumbidi.androvibe.data.local.FileSystemManager
import com.kumbidi.androvibe.ui.screens.*
import com.kumbidi.androvibe.ui.theme.AndroVibeTheme
import com.kumbidi.androvibe.ui.theme.Base
import com.kumbidi.androvibe.ui.theme.Blue
import com.kumbidi.androvibe.ui.theme.Mantle
import com.kumbidi.androvibe.ui.theme.Overlay0
import com.kumbidi.androvibe.ui.theme.Surface0
import com.kumbidi.androvibe.ui.theme.Text
import com.kumbidi.androvibe.ui.viewmodels.SetupViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroVibeTheme {
                AndroVibeNavigation()
            }
        }
    }
}

@Composable
fun AndroVibeNavigation() {
    val navController = rememberNavController()
    val setupViewModel: SetupViewModel = viewModel()

    val startDest = if (setupViewModel.isSetupComplete()) "projects" else "setup"

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = Modifier.fillMaxSize().background(Base)
    ) {
        composable("setup") {
            SetupWizardScreen(
                viewModel = setupViewModel,
                onSetupComplete = {
                    navController.navigate("projects") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable("projects") {
            ProjectListScreen(
                onProjectSelected = { projectName ->
                    navController.navigate("workspace/$projectName")
                }
            )
        }

        composable("workspace/{projectName}") { backStackEntry ->
            val projectName = backStackEntry.arguments?.getString("projectName") ?: return@composable
            WorkspaceScreen(projectName = projectName, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun WorkspaceScreen(projectName: String, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    val fsManager = remember { FileSystemManager(androidx.compose.ui.platform.LocalContext.current) }
    val projectDir = remember { File(fsManager.projectsRoot, projectName) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Mantle,
                contentColor = Text
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                    label = { Text("Editor") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Blue,
                        selectedTextColor = Blue,
                        unselectedIconColor = Overlay0,
                        unselectedTextColor = Overlay0,
                        indicatorColor = Surface0
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
                    label = { Text("Terminal") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Blue,
                        selectedTextColor = Blue,
                        unselectedIconColor = Overlay0,
                        unselectedTextColor = Overlay0,
                        indicatorColor = Surface0
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = "AI Chat") },
                    label = { Text("AI Chat") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Blue,
                        selectedTextColor = Blue,
                        unselectedIconColor = Overlay0,
                        unselectedTextColor = Overlay0,
                        indicatorColor = Surface0
                    )
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> EditorScreen(
                projectName = projectName,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> TerminalScreen(
                projectName = projectName,
                projectDir = projectDir,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> ChatScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
