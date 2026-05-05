package com.example.epubtoaudiobook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.epubtoaudiobook.ui.screens.AudiobookLibraryScreen
import com.example.epubtoaudiobook.ui.screens.EpubLibraryScreen
import com.example.epubtoaudiobook.ui.screens.PlayerScreen
import com.example.epubtoaudiobook.ui.screens.SettingsScreen
import com.example.epubtoaudiobook.ui.theme.EpubToAudiobookTheme

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Epub : BottomNavItem("epub", "EPUB", Icons.Default.Book)
    object Audiobooks : BottomNavItem("audiobooks", "Audiobooks", Icons.Default.Headphones)
    object Settings : BottomNavItem("settings", "Paramètres", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EpubToAudiobookTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val bottomItems = listOf(
        BottomNavItem.Epub,
        BottomNavItem.Audiobooks,
        BottomNavItem.Settings
    )

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val showBottomBar = bottomItems.any { it.route == currentDestination?.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Epub.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Epub.route) {
                EpubLibraryScreen()
            }
            composable(BottomNavItem.Audiobooks.route) {
                AudiobookLibraryScreen(
                    onOpenPlayer = { id -> navController.navigate("player/$id") }
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
            composable("player/{audiobookId}") { backStack ->
                val id = backStack.arguments?.getString("audiobookId")?.toLongOrNull() ?: return@composable
                PlayerScreen(
                    audiobookId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}