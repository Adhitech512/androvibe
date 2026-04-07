package com.kumbidi.androvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kumbidi.androvibe.ui.screens.ChatScreen
import com.kumbidi.androvibe.ui.screens.EditorScreen
import com.kumbidi.androvibe.ui.screens.TerminalScreen
import com.kumbidi.androvibe.ui.theme.AndroVibeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroVibeTheme {
                AndroVibeApp()
            }
        }
    }
}

@Composable
fun AndroVibeApp() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Editor,
        Screen.Terminal,
        Screen.Chat
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Editor.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Editor.route) { EditorScreen() }
            composable(Screen.Terminal.route) { TerminalScreen() }
            composable(Screen.Chat.route) { ChatScreen() }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Editor : Screen("editor", "Editor", Icons.Filled.Code)
    object Terminal : Screen("terminal", "Terminal", Icons.Filled.Terminal)
    object Chat : Screen("chat", "AI Chat", Icons.Filled.Chat)
}
